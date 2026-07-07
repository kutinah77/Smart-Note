package com.example.data

import android.content.Context
import android.os.Environment
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.repository.FinanceRepository
import com.example.data.serialization.MzdBackupSerializer
import androidx.room.withTransaction
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupService(private val context: Context, private val repository: FinanceRepository) {

    private val applicationContext = context.applicationContext
    private val silentBackupMutex = kotlinx.coroutines.sync.Mutex()
    private var silentBackupJob: kotlinx.coroutines.Job? = null

    fun getBaseBackupDirectory(): File {
        val folderName = applicationContext.getString(R.string.backup_folder_name)
        val publicDocDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val mainDir = File(publicDocDir, folderName)
        try {
            if (!mainDir.exists()) {
                mainDir.mkdirs()
            }
            if (mainDir.exists()) {
                return mainDir
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val appExternalDocsDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: applicationContext.getExternalFilesDir(null)
        val fallbackMainDir = File(appExternalDocsDir, folderName)
        if (!fallbackMainDir.exists()) {
            fallbackMainDir.mkdirs()
        }
        return fallbackMainDir
    }

    fun getBackupDirectory(): File {
        val baseDir = getBaseBackupDirectory()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthStr = sdf.format(Date())
        val targetDir = File(baseDir, monthStr)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }
        return targetDir
    }

    fun getAllMzdFilesRecursively(rootDir: File): List<File> {
        val result = mutableListOf<File>()
        val files = rootDir.listFiles() ?: return emptyList()
        for (f in files) {
            if (f.isDirectory) {
                result.addAll(getAllMzdFilesRecursively(f))
            } else if (f.name.endsWith(".mzd")) {
                result.add(f)
            }
        }
        return result
    }

    fun scanLocalBackups(): List<File> {
        val baseDir = getBaseBackupDirectory()
        val files = getAllMzdFilesRecursively(baseDir)
        return files.sortedByDescending { it.lastModified() }
    }

    suspend fun createLocalBackup(
        settings: AppSettings,
        commitments: List<FixedCommitment>,
        transactions: List<TransactionDb>,
        habayebCusts: List<HabayebCustomer>,
        habayebTxs: List<HabayebTransaction>,
        deletedItems: List<DeletedItemEntity>,
        isSilent: Boolean = false
    ): File? = withContext(Dispatchers.IO) {
        try {
            val jsonStr = MzdBackupSerializer.exportBackupToJson(
                settings, commitments, transactions, habayebCusts, habayebTxs, deletedItems
            )
            val dir = getBackupDirectory()
            val fileName = if (isSilent) {
                "Mizan_Silent_Backup.mzd"
            } else {
                val sdfName = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
                val dateStr = sdfName.format(Date())
                "Mizan_$dateStr.mzd"
            }
            val file = File(dir, fileName)
            file.writeText(jsonStr)

            if (file.exists() && file.length() > 0) {
                file
            } else {
                throw IOException("Backup file verification failed.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun triggerSilentLocalBackup(
        scope: kotlinx.coroutines.CoroutineScope,
        settings: AppSettings,
        commitments: List<FixedCommitment>,
        transactions: List<TransactionDb>,
        deletedItems: List<DeletedItemEntity>,
        onComplete: (() -> Unit)? = null
    ) {
        silentBackupJob?.cancel()
        silentBackupJob = scope.launch(Dispatchers.IO) {
            try {
                kotlinx.coroutines.delay(5000)
                silentBackupMutex.withLock {
                    val file = createLocalBackup(
                        settings, commitments, transactions,
                        repository.getAllCustomersDirect(), repository.getAllTransactionsDirect(), deletedItems,
                        isSilent = true
                    )
                    if (file != null) {
                        onComplete?.invoke()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getBackupJsonForClipboard(
        settings: AppSettings,
        commitments: List<FixedCommitment>,
        transactions: List<TransactionDb>,
        deletedItems: List<DeletedItemEntity>
    ): String = withContext(Dispatchers.IO) {
        MzdBackupSerializer.exportBackupToJson(
            settings, commitments, transactions,
            repository.getAllCustomersDirect(), repository.getAllTransactionsDirect(), deletedItems
        )
    }

    suspend fun backupToGoogleDriveDirect(
        googleDriveSyncHelper: CloudSyncProvider,
        settings: AppSettings,
        commitments: List<FixedCommitment>,
        transactions: List<TransactionDb>,
        deletedItems: List<DeletedItemEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val isTrulySignedIn = googleDriveSyncHelper.isUserTrulySignedIn(applicationContext)
            val refreshToken = googleDriveSyncHelper.getStoredRefreshToken()
            if (!isTrulySignedIn && refreshToken.isNullOrEmpty()) {
                return@withContext false
            }
            val jsonStr = getBackupJsonForClipboard(settings, commitments, transactions, deletedItems)
            googleDriveSyncHelper.uploadBackupToDrive(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun uploadBackupToGoogleDriveWithFilename(
        googleDriveSyncHelper: CloudSyncProvider,
        filename: String,
        settings: AppSettings,
        commitments: List<FixedCommitment>,
        transactions: List<TransactionDb>,
        deletedItems: List<DeletedItemEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val isTrulySignedIn = googleDriveSyncHelper.isUserTrulySignedIn(applicationContext)
            val refreshToken = googleDriveSyncHelper.getStoredRefreshToken()
            if (!isTrulySignedIn && refreshToken.isNullOrEmpty()) {
                return@withContext false
            }
            val jsonStr = getBackupJsonForClipboard(settings, commitments, transactions, deletedItems)
            googleDriveSyncHelper.uploadBackupToDriveWithFilename(filename, jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromGoogleDriveDirect(
        googleDriveSyncHelper: CloudSyncProvider,
        context: Context
    ): Pair<Boolean, AppSettings?> = withContext(Dispatchers.IO) {
        try {
            val isTrulySignedIn = googleDriveSyncHelper.isUserTrulySignedIn(context)
            val refreshToken = googleDriveSyncHelper.getStoredRefreshToken()
            if (!isTrulySignedIn && refreshToken.isNullOrEmpty()) {
                return@withContext Pair(false, null)
            }
            val jsonStr = googleDriveSyncHelper.downloadBackupFromDrive()
            if (jsonStr != null) {
                executeMasterRestore(jsonStr)
            } else {
                Pair(false, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, null)
        }
    }

    suspend fun restoreFromGoogleDriveById(
        googleDriveSyncHelper: CloudSyncProvider,
        context: Context,
        fileId: String
    ): Pair<Boolean, AppSettings?> = withContext(Dispatchers.IO) {
        try {
            val isTrulySignedIn = googleDriveSyncHelper.isUserTrulySignedIn(context)
            val refreshToken = googleDriveSyncHelper.getStoredRefreshToken()
            if (!isTrulySignedIn && refreshToken.isNullOrEmpty()) {
                return@withContext Pair(false, null)
            }
            val jsonStr = googleDriveSyncHelper.downloadBackupFromDriveById(fileId)
            if (jsonStr != null) {
                executeMasterRestore(jsonStr)
            } else {
                Pair(false, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, null)
        }
    }

    suspend fun deleteCloudBackupById(
        googleDriveSyncHelper: CloudSyncProvider,
        fileId: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            googleDriveSyncHelper.deleteBackupFromDriveById(fileId)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun executeMasterRestore(rawJsonString: String): Pair<Boolean, AppSettings?> = withContext(Dispatchers.IO) {
        var restoreSuccessful = false
        var restoredSettings: AppSettings? = null
        val appDb = AppDatabase.getDatabase(applicationContext)

        try {
            val root = JSONObject(rawJsonString)
            val currentLocalSettings = repository.settingsFlow.firstOrNull() ?: AppSettings()
            val data = MzdBackupSerializer.importBackupFromJson(rawJsonString, applicationContext)
            val restoredSettingsUnmerged = data.first
            restoredSettings = restoredSettingsUnmerged.copy(
                isPasscodeEnabled = currentLocalSettings.isPasscodeEnabled,
                passcodeHash = currentLocalSettings.passcodeHash,
                recoveryPhraseHash = currentLocalSettings.recoveryPhraseHash,
                recoveryHint = currentLocalSettings.recoveryHint,
                tempPart = currentLocalSettings.tempPart,
                permPart = currentLocalSettings.permPart,
                unifiedDeviceId = currentLocalSettings.unifiedDeviceId,
                isFirstLaunch = currentLocalSettings.isFirstLaunch
            )
            val restoredCommitments = data.second
            val restoredTransactions = data.third

            // Safe execute inside try-finally as requested to prevent corrupt/partial database states
            appDb.withTransaction {
                repository.clearTransactions()
                repository.clearCommitments()
                repository.clearCustomCategories()
                repository.clearDeletedItems()

                repository.saveSettings(restoredSettings)
                for (fc in restoredCommitments) {
                    repository.saveCommitment(fc)
                }
                for (tx in restoredTransactions) {
                    repository.saveTransaction(tx)
                }

                if (root.has("deleted_items") && !root.isNull("deleted_items")) {
                    val deletedItemsArr = root.optJSONArray("deleted_items")
                    if (deletedItemsArr != null) {
                        for (i in 0 until deletedItemsArr.length()) {
                            val obj = deletedItemsArr.getJSONObject(i)
                            val item = DeletedItemEntity(
                                id = obj.getString("id"),
                                sourceSystem = obj.getString("sourceSystem"),
                                originalTableName = obj.getString("originalTableName"),
                                jsonData = obj.getString("jsonData"),
                                deletedAt = obj.getLong("deletedAt")
                            )
                            repository.saveDeletedItem(item)
                        }
                    }
                }

                repository.clearAllCustomers()
                repository.clearAllTransactions()

                val jsonHabayebObj = root.optJSONObject("habayeb_debts")
                val legacyHabayebDb = root.optJSONObject("habayeb_debts_db")

                val custArr = jsonHabayebObj?.optJSONArray("customers")
                    ?: legacyHabayebDb?.optJSONArray("habayeb_customers")

                if (custArr != null) {
                    for (i in 0 until custArr.length()) {
                        val obj = custArr.getJSONObject(i)
                        val customer = HabayebCustomer(
                            id = obj.optString("id", obj.optString("customer_id", "")),
                            name = obj.getString("name"),
                            phone = obj.optString("phone", ""),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("created_at", obj.optLong("createdAt", System.currentTimeMillis() / 1000))
                        )
                        repository.insertCustomer(customer)
                    }
                }

                val txArr = jsonHabayebObj?.optJSONArray("debt_transactions")
                    ?: legacyHabayebDb?.optJSONArray("habayeb_transactions")

                if (txArr != null) {
                    for (i in 0 until txArr.length()) {
                        val obj = txArr.getJSONObject(i)
                        val transaction = HabayebTransaction(
                            id = obj.getString("id"),
                            customerId = obj.optString("customer_id", obj.optString("customerId", "")),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            timestamp = obj.getLong("timestamp"),
                            description = obj.optString("description", ""),
                            linkedMainTxId = obj.optString("linked_main_tx_id", obj.optString("linkedMainTxId", "")).takeIf { it.isNotEmpty() }
                        )
                        repository.insertHabayebTransaction(transaction)
                    }
                }
            }
            restoreSuccessful = true
        } catch (e: Exception) {
            e.printStackTrace()
            restoreSuccessful = false
        } finally {
            // Log or execute extra verification after try block completes/fails
        }

        return@withContext Pair(restoreSuccessful, restoredSettings)
    }
}
