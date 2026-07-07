package com.example.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.R
import com.example.ui.viewmodel.FinanceViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException

class GoogleAuthManager(
    val signInClient: GoogleSignInClient,
    private val onSignInClick: () -> Unit
) {
    fun signIn() {
        onSignInClick()
    }
}

@Composable
fun rememberGoogleAuthManager(
    viewModel: FinanceViewModel,
    context: Context
): GoogleAuthManager {
    val googleSignInClient = remember(context) {
        viewModel.googleDriveSyncHelper.getGoogleSignInClient(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intent = result.data
        if (result.resultCode == Activity.RESULT_OK && intent != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            try {
                val account = task.getResult(ApiException::class.java)
                val authCode = account?.serverAuthCode
                val email = account?.email ?: "account@google.com"
                if (authCode != null) {
                    viewModel.handleGoogleOAuthCode(authCode, email) { success ->
                        if (success) {
                            Toast.makeText(context, context.getString(R.string.settings_gdrive_link_success_pattern, email), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, context.getString(R.string.settings_gdrive_link_failed_network), Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.settings_gdrive_link_failed_invalid_code), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("GoogleAuthManager", "Google sign in failed", e)
                Toast.makeText(context, context.getString(R.string.settings_gdrive_link_failed_error_pattern, e.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
            }
        } else {
            var handledError = false
            if (intent != null) {
                try {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                    task.getResult(ApiException::class.java)
                } catch (e: ApiException) {
                    val sc = e.statusCode
                    Log.e("GoogleAuthManager", "Sign in failed with code $sc", e)
                    Toast.makeText(context, context.getString(R.string.settings_gdrive_link_failed_api_code_pattern, sc), Toast.LENGTH_LONG).show()
                    handledError = true
                } catch (e: Exception) {
                    Log.e("GoogleAuthManager", "Sign in task exception", e)
                }
            }
            if (!handledError) {
                Toast.makeText(context, context.getString(R.string.settings_gdrive_link_cancelled), Toast.LENGTH_SHORT).show()
            }
        }
    }

    return remember(googleSignInClient) {
        GoogleAuthManager(googleSignInClient) {
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInClient.revokeAccess().addOnCompleteListener {
                    launcher.launch(googleSignInClient.signInIntent)
                }
            }
        }
    }
}
