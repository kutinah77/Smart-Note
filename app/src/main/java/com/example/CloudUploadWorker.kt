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
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Data
import com.example.data.GoogleDriveSyncHelper
import java.io.File

class CloudUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "CloudUploadWorker"
        const val KEY_FILE_PATH = "backup_file_path"
        const val KEY_FILE_NAME = "backup_file_name"

        fun enqueueUpload(context: Context, filePath: String, fileName: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = Data.Builder()
                .putString(KEY_FILE_PATH, filePath)
                .putString(KEY_FILE_NAME, fileName)
                .build()

            val uploadWorkRequest = OneTimeWorkRequestBuilder<CloudUploadWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(uploadWorkRequest)
            Log.d(TAG, "Enqueued cloud upload for $fileName to trigger when internet is connected")
        }
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        val filePath = inputData.getString(KEY_FILE_PATH) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: return Result.failure()

        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "Backup file not found at path: $filePath")
                return Result.failure()
            }

            val syncHelper = GoogleDriveSyncHelper(context)
            val isLinked = !syncHelper.getStoredRefreshToken().isNullOrEmpty()
            if (!isLinked) {
                Log.d(TAG, "Google Drive not linked. Skipping cloud upload.")
                return Result.success()
            }

            val jsonStr = file.readText()
            val success = syncHelper.uploadBackupToDriveWithFilename(fileName, jsonStr)
            if (success) {
                Log.d(TAG, "Successfully uploaded backup $fileName to Google Drive in background")
                sendCloudUploadNotification(context, true)
                return Result.success()
            } else {
                Log.e(TAG, "Cloud upload failed, retrying...")
                if (runAttemptCount >= 2) {
                    sendCloudUploadNotification(context, false)
                }
                return Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during cloud upload", e)
            if (runAttemptCount >= 2) {
                sendCloudUploadNotification(context, false)
            }
            return Result.retry()
        }
    }

    private fun sendCloudUploadNotification(context: Context, success: Boolean) {
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

        val title = if (success) {
            context.getString(com.example.R.string.autobackup_notification_title_cloud)
        } else {
            context.getString(com.example.R.string.autobackup_notification_title_cloud_failed)
        }

        val text = if (success) {
            context.getString(com.example.R.string.autobackup_notification_text_cloud)
        } else {
            context.getString(com.example.R.string.autobackup_notification_text_cloud_failed)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_upload_done else android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(if (success) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
