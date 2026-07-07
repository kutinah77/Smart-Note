package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.api.GoogleDriveApiService
import com.example.data.local.AppDatabase
import com.example.data.repository.GoogleAuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed class CloudSyncState {
    object Idle : CloudSyncState()
    object Authenticating : CloudSyncState()
    data class Authenticated(val email: String) : CloudSyncState()
    object Syncing : CloudSyncState()
    object Success : CloudSyncState()
    data class Error(val message: String) : CloudSyncState()
    object SessionExpired : CloudSyncState()
}

data class CloudBackupFile(
    val id: String,
    val name: String,
    val size: Long,
    val createdTime: String
)

class GoogleDriveSyncHelper(private val context: Context) : CloudSyncProvider {

    companion object {
        private const val TAG = "GoogleDriveSyncHelper"
    }

    private val apiService = GoogleDriveApiService.create()
    private val authRepository = GoogleAuthRepository(context, apiService)

    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    override val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Safe check during startup
        val email = authRepository.getStoredEmail()
        val refreshToken = authRepository.getStoredRefreshToken()
        if (!email.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
            _syncState.value = CloudSyncState.Authenticated(email)
        }

        // Safe async check without GlobalScope
        serviceScope.launch {
            try {
                if (authRepository.isUserTrulySignedIn(context)) {
                    val db = AppDatabase.getDatabase(context)
                    val settings = db.settingsDao().getSettingsDirect()
                    if (authRepository.getStoredRefreshToken().isNullOrEmpty() && (settings == null || !settings.isCloudSyncEnabled)) {
                        Log.d(TAG, "Force Reset On Reinstall Mismatch detected. Executing silent logout.")
                        authRepository.getGoogleSignInClient(context).signOut()
                        _syncState.value = CloudSyncState.Idle
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve reinstall mismatch in init", e)
            }
        }
    }

    override fun isUserTrulySignedIn(context: Context): Boolean {
        return authRepository.isUserTrulySignedIn(context)
    }

    override fun getStoredRefreshToken(): String? {
        return authRepository.getStoredRefreshToken()
    }

    override fun getStoredEmail(): String? {
        return authRepository.getStoredEmail()
    }

    override fun getAuthUrl(): String {
        return authRepository.getAuthUrl()
    }

    override fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        return authRepository.getGoogleSignInClient(context)
    }

    private suspend fun disableCloudSyncInSettings() {
        try {
            val db = AppDatabase.getDatabase(context)
            val settings = db.settingsDao().getSettingsDirect()
            if (settings != null && settings.isCloudSyncEnabled) {
                db.settingsDao().insertOrUpdateSettings(settings.copy(isCloudSyncEnabled = false))
                Log.d(TAG, "Successfully deactivated isCloudSyncEnabled in DB due to Security/Auth failure.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed disabling cloud sync from helper", e)
        }
    }

    override suspend fun handleAuthorizationCode(code: String, inputEmail: String?, redirectUri: String): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = CloudSyncState.Authenticating
        val email = authRepository.handleAuthorizationCode(code, inputEmail, redirectUri)
        if (email != null) {
            _syncState.value = CloudSyncState.Authenticated(email)
            true
        } else {
            _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_link_failed))
            false
        }
    }

    override fun logout() {
        authRepository.logout()
        _syncState.value = CloudSyncState.Idle
    }

    override fun logoutAsync(onComplete: () -> Unit) {
        authRepository.logoutAsync {
            _syncState.value = CloudSyncState.Idle
            onComplete()
        }
    }

    private suspend fun handleAuthError() {
        _syncState.value = CloudSyncState.SessionExpired
        disableCloudSyncInSettings()
        logout()
    }

    override suspend fun uploadBackupToDrive(backupJsonContent: String): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = CloudSyncState.Syncing
        val accessToken = authRepository.refreshAccessTokenIfNeeded()
        val email = authRepository.getStoredEmail()

        if (accessToken == null) {
            _syncState.value = CloudSyncState.SessionExpired
            return@withContext false
        }

        // Keep local mirror matching
        try {
            val mirrorFile = File(context.filesDir, "google_drive_mirror.mzd")
            mirrorFile.bufferedWriter().use { writer ->
                writer.write(backupJsonContent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing local cache mirror file securely", e)
        }

        try {
            val q = "name contains 'Mzd_' and name contains '.mzd' and trashed = false"
            val response = apiService.searchFiles(
                authHeader = "Bearer $accessToken",
                spaces = "appDataFolder",
                orderBy = "createdTime desc",
                query = q,
                fields = "files(id,name,size,createdTime)"
            )

            var existingFileId: String? = null
            var searchSuccess = false
            var isAuthError = false

            if (response.isSuccessful) {
                val rawBody = response.body()?.string() ?: ""
                val searchResult = JSONObject(rawBody)
                val files = searchResult.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    existingFileId = files.getJSONObject(0).getString("id")
                }
                searchSuccess = true
            } else {
                if (response.code() == 401 || response.code() == 403) {
                    isAuthError = true
                }
            }

            if (!searchSuccess) {
                if (isAuthError) {
                    handleAuthError()
                } else {
                    _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed))
                }
                return@withContext false
            }

            val sdfName = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US)
            val dateStr = sdfName.format(java.util.Date())
            val newFileName = "Mzd_$dateStr.mzd"

            var success = false
            if (existingFileId != null) {
                val mediaBody = backupJsonContent.toRequestBody("application/json; charset=utf-8".toMediaType())
                val uploadResponse = apiService.uploadFileMedia(
                    url = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media",
                    authHeader = "Bearer $accessToken",
                    content = mediaBody
                )

                success = uploadResponse.isSuccessful
                if (!success) {
                    Log.e(TAG, "Failed patching file on Google Drive.")
                } else {
                    val metaJson = JSONObject().apply { put("name", newFileName) }
                    val metaBody = metaJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    apiService.updateFileMetadata(
                        url = "https://www.googleapis.com/drive/v3/files/$existingFileId",
                        authHeader = "Bearer $accessToken",
                        metadata = metaBody
                    )
                }
            } else {
                val metaJson = JSONObject().apply {
                    put("name", newFileName)
                    put("parents", JSONArray().put("appDataFolder"))
                    put("mimeType", "application/octet-stream")
                }
                val metaBody = metaJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val createResponse = apiService.createFileMetadata(
                    authHeader = "Bearer $accessToken",
                    metadata = metaBody
                )

                if (createResponse.isSuccessful) {
                    val rawBody = createResponse.body()?.string() ?: ""
                    val createdFile = JSONObject(rawBody)
                    val newFileId = createdFile.getString("id")

                    val fileBody = backupJsonContent.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val uploadResponse = apiService.uploadFileMedia(
                        url = "https://www.googleapis.com/upload/drive/v3/files/$newFileId?uploadType=media",
                        authHeader = "Bearer $accessToken",
                        content = fileBody
                    )
                    success = uploadResponse.isSuccessful
                } else {
                    Log.e(TAG, "Failed creating upload metadata structure on Drive.")
                }
            }

            if (success) {
                _syncState.value = CloudSyncState.Success
                delay(1200)
                _syncState.value = CloudSyncState.Authenticated(email ?: "account@google.com")
                true
            } else {
                _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed))
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Uncaught networking error during background cloud synchronization", e)
            _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed))
            false
        }
    }

    override suspend fun downloadBackupFromDrive(): String? = withContext(Dispatchers.IO) {
        _syncState.value = CloudSyncState.Syncing
        val accessToken = authRepository.refreshAccessTokenIfNeeded()
        val email = authRepository.getStoredEmail()

        if (accessToken == null) {
            _syncState.value = CloudSyncState.SessionExpired
            return@withContext null
        }

        try {
            val q = "name contains 'Mzd_' and name contains '.mzd' and trashed = false"
            val response = apiService.searchFiles(
                authHeader = "Bearer $accessToken",
                spaces = "appDataFolder",
                orderBy = "createdTime desc",
                query = q,
                fields = "files(id,name,size,createdTime)"
            )

            var existingFileId: String? = null
            var searchSuccess = false
            var isAuthError = false

            if (response.isSuccessful) {
                val rawBody = response.body()?.string() ?: ""
                val searchResult = JSONObject(rawBody)
                val files = searchResult.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    existingFileId = files.getJSONObject(0).getString("id")
                }
                searchSuccess = true
            } else {
                if (response.code() == 401 || response.code() == 403) {
                    isAuthError = true
                }
            }

            if (!searchSuccess) {
                if (isAuthError) {
                    handleAuthError()
                } else {
                    _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed))
                }
                return@withContext null
            }

            if (existingFileId == null) {
                _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_backups_not_found))
                return@withContext null
            }

            val downloadResponse = apiService.downloadFile(
                url = "https://www.googleapis.com/drive/v3/files/$existingFileId?alt=media",
                authHeader = "Bearer $accessToken"
            )

            if (downloadResponse.isSuccessful) {
                val content = downloadResponse.body()?.string()
                _syncState.value = CloudSyncState.Authenticated(email ?: "account@google.com")
                content
            } else {
                if (downloadResponse.code() == 401 || downloadResponse.code() == 403) {
                    handleAuthError()
                } else {
                    _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed))
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Uncaught networking error during download processing", e)
            _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed))
            null
        }
    }

    override suspend fun listCloudBackups(): List<CloudBackupFile> = withContext(Dispatchers.IO) {
        val accessToken = authRepository.refreshAccessTokenIfNeeded() ?: return@withContext emptyList()
        try {
            val response = apiService.searchFiles(
                authHeader = "Bearer $accessToken",
                spaces = "appDataFolder",
                orderBy = "createdTime desc",
                query = "name contains '.mzd' and trashed = false",
                fields = "files(id,name,size,createdTime)"
            )

            if (response.isSuccessful) {
                val rawBody = response.body()?.string() ?: ""
                val json = JSONObject(rawBody)
                val filesArray = json.optJSONArray("files") ?: return@withContext emptyList()
                val list = mutableListOf<CloudBackupFile>()
                for (i in 0 until filesArray.length()) {
                    val obj = filesArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val size = obj.optLong("size", 0L)
                    val createdTime = obj.optString("createdTime", "")
                    list.add(CloudBackupFile(id, name, size, createdTime))
                }
                list.sortedByDescending { it.name }
            } else {
                if (response.code() == 401 || response.code() == 403) {
                    handleAuthError()
                }
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing remote cloud backups", e)
            emptyList()
        }
    }

    override suspend fun uploadBackupToDriveWithFilename(filename: String, backupJsonContent: String): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = CloudSyncState.Syncing
        val accessToken = authRepository.refreshAccessTokenIfNeeded()
        val email = authRepository.getStoredEmail()

        if (accessToken == null) {
            _syncState.value = CloudSyncState.SessionExpired
            return@withContext false
        }

        try {
            val metaJson = JSONObject().apply {
                put("name", filename)
                put("parents", JSONArray().put("appDataFolder"))
                put("mimeType", "application/octet-stream")
            }
            val metaBody = metaJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val createResponse = apiService.createFileMetadata(
                authHeader = "Bearer $accessToken",
                metadata = metaBody
            )

            var success = false
            if (createResponse.isSuccessful) {
                val rawBody = createResponse.body()?.string() ?: ""
                val createdFile = JSONObject(rawBody)
                val newFileId = createdFile.getString("id")

                val fileBody = backupJsonContent.toRequestBody("application/json; charset=utf-8".toMediaType())
                val uploadResponse = apiService.uploadFileMedia(
                    url = "https://www.googleapis.com/upload/drive/v3/files/$newFileId?uploadType=media",
                    authHeader = "Bearer $accessToken",
                    content = fileBody
                )
                success = uploadResponse.isSuccessful
            } else {
                Log.e(TAG, "Failed creating file metadata on Drive.")
            }

            if (success) {
                _syncState.value = CloudSyncState.Success
                delay(1200)
                _syncState.value = CloudSyncState.Authenticated(email ?: "account@google.com")
                true
            } else {
                _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed))
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing file creation with specialized filename on Drive", e)
            _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed))
            false
        }
    }

    override suspend fun downloadBackupFromDriveById(fileId: String): String? = withContext(Dispatchers.IO) {
        _syncState.value = CloudSyncState.Syncing
        val accessToken = authRepository.refreshAccessTokenIfNeeded()
        val email = authRepository.getStoredEmail()

        if (accessToken == null) {
            _syncState.value = CloudSyncState.SessionExpired
            return@withContext null
        }

        try {
            val downloadResponse = apiService.downloadFile(
                url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media",
                authHeader = "Bearer $accessToken"
            )

            if (downloadResponse.isSuccessful) {
                val content = downloadResponse.body()?.string()
                _syncState.value = CloudSyncState.Authenticated(email ?: "account@google.com")
                content
            } else {
                if (downloadResponse.code() == 401 || downloadResponse.code() == 403) {
                    handleAuthError()
                } else {
                    _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed))
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading custom file content by database ID", e)
            _syncState.value = CloudSyncState.Error(context.getString(com.example.R.string.gdrive_error_server_failed))
            null
        }
    }

    override suspend fun deleteBackupFromDriveById(fileId: String): Boolean = withContext(Dispatchers.IO) {
        val accessToken = authRepository.refreshAccessTokenIfNeeded() ?: return@withContext false
        try {
            val response = apiService.deleteFile(
                url = "https://www.googleapis.com/drive/v3/files/$fileId",
                authHeader = "Bearer $accessToken"
            )
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error removing file from remote drive folder", e)
            false
        }
    }
}
