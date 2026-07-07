package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.BuildConfig
import com.example.data.api.GoogleDriveApiService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

class GoogleAuthRepository(
    private val context: Context,
    private val apiService: GoogleDriveApiService
) {

    companion object {
        private const val TAG = "GoogleAuthRepository"
    }

    val clientId: String
        get() {
            val configId = try { BuildConfig.GOOGLE_CLIENT_ID } catch (e: Exception) { "" }
            return if (configId.isNullOrBlank() || configId == "GOOGLE_CLIENT_ID_PLACEHOLDER") "" else configId
        }

    val clientSecret: String
        get() {
            val configSecret = try { BuildConfig.GOOGLE_CLIENT_SECRET } catch (e: Exception) { "" }
            return if (configSecret.isNullOrBlank() || configSecret == "GOOGLE_CLIENT_SECRET_PLACEHOLDER") "" else configSecret
        }

    val scope = "https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email"

    private val sharedPrefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "secure_google_drive_sync_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating EncryptedSharedPreferences, using fallback SharedPreferences.", e)
            context.getSharedPreferences("google_drive_sync_prefs", Context.MODE_PRIVATE)
        }
    }

    fun isUserTrulySignedIn(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val requiredScope = Scope("https://www.googleapis.com/auth/drive.file")
        return account != null && GoogleSignIn.hasPermissions(account, requiredScope)
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope("https://www.googleapis.com/auth/drive.appdata"),
                Scope("https://www.googleapis.com/auth/drive.file")
            )
            .requestServerAuthCode(clientId, true)
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getAuthUrl(): String {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=${URLEncoder.encode(clientId, "UTF-8")}" +
                "&redirect_uri=${URLEncoder.encode("http://localhost/oauth2callback", "UTF-8")}" +
                "&response_type=code" +
                "&scope=${URLEncoder.encode(scope, "UTF-8")}" +
                "&prompt=consent" +
                "&access_type=offline"
    }

    fun logout() {
        try {
            val signInClient = getGoogleSignInClient(context)
            signInClient.revokeAccess()
            signInClient.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error during Google Sign-In silent logout/revoke", e)
        }
        sharedPrefs.edit().clear().apply()
    }

    fun logoutAsync(onComplete: () -> Unit) {
        try {
            val signInClient = getGoogleSignInClient(context)
            signInClient.revokeAccess().addOnCompleteListener {
                signInClient.signOut().addOnCompleteListener {
                    sharedPrefs.edit().clear().apply()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during deep revokeAccess and signOut on logoutAsync", e)
            sharedPrefs.edit().clear().apply()
            onComplete()
        }
    }

    fun storeTokens(accessToken: String, refreshToken: String?, expiresInSec: Long) {
        val editor = sharedPrefs.edit()
        editor.putString("access_token", accessToken)
        if (refreshToken != null) {
            editor.putString("refresh_token", refreshToken)
        }
        editor.putLong("token_expiry", System.currentTimeMillis() + (expiresInSec * 1000))
        editor.apply()
    }

    fun storeEmail(email: String) {
        sharedPrefs.edit().putString("email", email).apply()
    }

    fun getStoredAccessToken(): String? = sharedPrefs.getString("access_token", null)
    fun getStoredRefreshToken(): String? = sharedPrefs.getString("refresh_token", null)
    fun getStoredEmail(): String? = sharedPrefs.getString("email", null)

    fun isTokenExpired(): Boolean {
        val expiry = sharedPrefs.getLong("token_expiry", 0)
        return System.currentTimeMillis() >= (expiry - 300_000)
    }

    suspend fun handleAuthorizationCode(code: String, inputEmail: String?, redirectUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.exchangeCode(
                code = code,
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUri = redirectUri
            )

            if (response.isSuccessful) {
                val rawBody = response.body()?.string() ?: ""
                val json = JSONObject(rawBody)
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", "").takeIf { it.isNotEmpty() } ?: getStoredRefreshToken()
                val expiresIn = json.optLong("expires_in", 3600L)

                storeTokens(accessToken, refreshToken, expiresIn)

                val email = inputEmail ?: fetchUserEmail(accessToken) ?: "account@google.com"
                storeEmail(email)
                email
            } else {
                Log.e(TAG, "Authorization code exchange failed: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during authorization code exchange", e)
            null
        }
    }

    suspend fun fetchUserEmail(accessToken: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.fetchUserInfo("Bearer $accessToken")
            if (response.isSuccessful) {
                val rawBody = response.body()?.string() ?: ""
                val json = JSONObject(rawBody)
                json.optString("email", null)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed retrieving user account info safely", e)
            null
        }
    }

    suspend fun refreshAccessTokenIfNeeded(): String? = withContext(Dispatchers.IO) {
        val refreshToken = getStoredRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            return@withContext null
        }

        if (!isTokenExpired()) {
            val currentToken = getStoredAccessToken()
            if (!currentToken.isNullOrEmpty()) {
                return@withContext currentToken
            }
        }

        try {
            val response = apiService.refreshToken(
                clientId = clientId,
                clientSecret = clientSecret,
                refreshToken = refreshToken
            )

            if (response.isSuccessful) {
                val rawBody = response.body()?.string() ?: ""
                val json = JSONObject(rawBody)
                val accessToken = json.getString("access_token")
                val expiresIn = json.optLong("expires_in", 3600L)

                storeTokens(accessToken, refreshToken, expiresIn)
                accessToken
            } else {
                Log.e(TAG, "AccessToken refresh failed: status code ${response.code()}")
                // Clear invalid credentials locally
                sharedPrefs.edit()
                    .remove("access_token")
                    .remove("refresh_token")
                    .remove("token_expiry")
                    .remove("email")
                    .apply()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing credentials or renewing access tokens", e)
            null
        }
    }
}
