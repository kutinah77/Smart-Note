package com.example.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.flow.StateFlow

interface CloudSyncProvider {
    val syncState: StateFlow<CloudSyncState>
    fun isUserTrulySignedIn(context: Context): Boolean
    fun getStoredRefreshToken(): String?
    fun getStoredEmail(): String?
    fun getAuthUrl(): String
    fun getGoogleSignInClient(context: Context): GoogleSignInClient
    suspend fun handleAuthorizationCode(code: String, inputEmail: String? = null, redirectUri: String = ""): Boolean
    suspend fun uploadBackupToDrive(backupJsonContent: String): Boolean
    suspend fun downloadBackupFromDrive(): String?
    suspend fun listCloudBackups(): List<CloudBackupFile>
    suspend fun uploadBackupToDriveWithFilename(filename: String, backupJsonContent: String): Boolean
    suspend fun downloadBackupFromDriveById(fileId: String): String?
    suspend fun deleteBackupFromDriveById(fileId: String): Boolean
    fun logout()
    fun logoutAsync(onComplete: () -> Unit)
}
