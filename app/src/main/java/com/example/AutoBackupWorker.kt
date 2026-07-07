package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AppSettings
import com.example.data.GoogleDriveSyncHelper
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AutoBackupWorker"

        fun scheduleDailyBackupWorker(context: Context) {
            val workManager = WorkManager.getInstance(context)
            
            // Unrestricted network constraint so local daily backup always runs offline!
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)                // Save device battery health
                .build()

            // Calculate precise initial delay to 11:59 PM (23:59:00)
            val currentDate = Calendar.getInstance()
            val dueDate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (dueDate.before(currentDate)) {
                dueDate.add(Calendar.DAY_OF_YEAR, 1)
            }
            val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

            Log.d(TAG, "Scheduling daily backup worker. Initial delay: ${initialDelay / 1000 / 60} minutes (runs at 11:59 PM).")

            val dailyWorkRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "MizanDailyBackup",
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWorkRequest
            )
        }
        
        fun cancelDailyBackupWorker(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("MizanDailyBackup")
        }
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        try {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettingsDirect() ?: AppSettings()
            
            if (!settings.isAutoBackupEnabled) {
                Log.d(TAG, "Auto-backup feature is disabled in user settings.")
                return Result.success()
            }
            
            val commitments = db.commitmentDao().getAllCommitmentsFlow().first()
            val transactions = db.transactionDao().getAllTransactionsFlow().first()
            val deletedItems = db.deletedItemDao().getAllDeletedItemsDirect()

            val habayebCustomers = db.habayebDao().getAllCustomersDirect()
            val habayebTransactions = db.habayebDao().getAllTransactionsDirect()

            val jsonStr = com.example.data.serialization.MzdBackupSerializer.exportBackupToJson(
                settings = settings,
                commitments = commitments,
                transactions = transactions,
                habayebCustomers = habayebCustomers,
                habayebTransactions = habayebTransactions,
                deletedItems = deletedItems
            )

            val folderName = "الدفتر الذكي"
            val documentsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
            var mainDir = File(documentsDir, folderName)
            try {
                if (!mainDir.exists()) {
                    mainDir.mkdirs()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create public Documents directory, falling back to private storage", e)
            }

            if (!mainDir.exists()) {
                val appPrivateDir = context.getExternalFilesDir(null) ?: context.filesDir
                mainDir = File(appPrivateDir, folderName)
                if (!mainDir.exists()) {
                    mainDir.mkdirs()
                }
            }

            if (!mainDir.exists()) {
                Log.e(TAG, "Could not create local backup directories.")
                sendBackupFailureNotification(context, false)
                return Result.failure()
            }

            // Create month directory inside "الدفتر الذكي"
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.US)
            val monthStr = sdfMonth.format(Date())
            val monthDir = File(mainDir, monthStr)
            try {
                if (!monthDir.exists()) {
                    monthDir.mkdirs()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create month directory in local backup", e)
            }

            val finalMonthDir = if (monthDir.exists()) monthDir else mainDir

            val sdfName = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
            val dateStr = sdfName.format(Date())
            val fileName = "Mizan_$dateStr.mzd"
            val file = File(finalMonthDir, fileName)

            var localWrittenSuccessfully = false
            try {
                // Safeguarded Buffered writer to ensure clean closing of streams
                file.bufferedWriter().use { writer ->
                    writer.write(jsonStr)
                }
                if (file.exists() && file.length() > 500) { // Real IO validation
                    localWrittenSuccessfully = true
                }
            } catch (writeEx: Exception) {
                Log.e(TAG, "Error writing data payload to local storage path", writeEx)
            }

            if (localWrittenSuccessfully) {
                // Dual Cloud Sync
                val syncHelper = GoogleDriveSyncHelper(context)
                val isLinked = !syncHelper.getStoredRefreshToken().isNullOrEmpty()
                var cloudSynced = false
                var isOffline = false
                
                if (isLinked) {
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    val hasNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val nw = connectivityManager.activeNetwork
                        val actNw = connectivityManager.getNetworkCapabilities(nw)
                        actNw != null && actNw.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    } else {
                        val nwInfo = connectivityManager.activeNetworkInfo
                        nwInfo != null && nwInfo.isConnected
                    }

                    if (hasNetwork) {
                        // Upload this specific file!
                        cloudSynced = syncHelper.uploadBackupToDriveWithFilename(fileName, jsonStr)
                    } else {
                        isOffline = true
                    }
                    
                    if (!cloudSynced) {
                        Log.w(TAG, "Google Cloud synchronization failed or offline. Enqueueing background CloudUploadWorker.")
                        // Enqueue guaranteed upload for as soon as we connect to internet
                        CloudUploadWorker.enqueueUpload(context, file.absolutePath, fileName)
                    }
                }
                
                // Absolutely last step: send success notification!
                sendBackupNotification(context, isLinked, cloudSynced, isOffline, file)
                return Result.success()
            } else {
                Log.e(TAG, "Local file write verification failed.")
                sendBackupFailureNotification(context, false)
                return Result.retry() // Retry locally as well since write could be transient
            }

        } catch (e: Exception) {
            Log.e(TAG, "Defensive rescue: unexpected background execution error in AutoBackupWorker", e)
            sendBackupFailureNotification(context, false)
            
            // Check if failure is related to network or file system IO
            return if (e is IOException) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun sendBackupNotification(context: Context, isLinked: Boolean, cloudSynced: Boolean, isOffline: Boolean, backupFile: File) {
        val channelId = "mizan_backup_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(com.example.R.string.autobackup_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(com.example.R.string.autobackup_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val text: String = backupFile.absolutePath
        val icon: Int
        val priority: Int

        if (isLinked) {
            if (cloudSynced) {
                title = "تم حفظ نسخة احتياطية ومزامنتها سحابياً ✅"
                icon = android.R.drawable.stat_sys_upload_done
                priority = NotificationCompat.PRIORITY_DEFAULT
            } else if (isOffline) {
                title = "تم حفظ نسخة احتياطية محلياً (المزامنة معلقة) ⚠️"
                icon = android.R.drawable.stat_sys_warning
                priority = NotificationCompat.PRIORITY_DEFAULT
            } else {
                title = "تم حفظ نسخة احتياطية محلياً (فشل المزامنة) ⚠️"
                icon = android.R.drawable.stat_sys_warning
                priority = NotificationCompat.PRIORITY_HIGH
            }
        } else {
            title = "تم حفظ نسخة احتياطية بنجاح 🏠📁"
            icon = android.R.drawable.stat_sys_upload_done
            priority = NotificationCompat.PRIORITY_DEFAULT
        }

        // Trigger premium physical success vibration!
        com.example.ui.helper.VibrationHelper.vibrateSuccess(context)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun sendBackupFailureNotification(context: Context, isPermissionIssue: Boolean) {
        val channelId = "mizan_backup_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(com.example.R.string.autobackup_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(com.example.R.string.autobackup_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(com.example.R.string.autobackup_notification_title_failure)
        val text = if (isPermissionIssue) {
            context.getString(com.example.R.string.autobackup_notification_text_permission)
        } else {
            context.getString(com.example.R.string.autobackup_notification_text_failure)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }
}
