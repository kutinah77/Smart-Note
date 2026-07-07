package com.example.ui.screens

import androidx.compose.material3.MaterialTheme

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.ui.screens.security.components.SecurityActivePanel
import com.example.ui.screens.security.components.SecurityHeaderBanner
import com.example.ui.screens.security.components.SecuritySetupForm
import com.example.ui.viewmodel.FinanceViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    settings: AppSettings,
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Safely collect reactive app settings with lifecycle-awareness
    val currentSettings by viewModel.settingsState.collectAsStateWithLifecycle()
    val isAlreadyPasscodeEnabled = currentSettings.isPasscodeEnabled

    // State inputs for setup form
    var passcode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var recoveryPhrase by remember { mutableStateOf("") }
    var recoveryHint by remember { mutableStateOf("") }
    var checkAcknowledged by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Verification states
    var isVerified by remember { mutableStateOf(false) }
    var isChangingPasscode by remember { mutableStateOf(false) }

    // Reset states when lock is disabled or status changes
    LaunchedEffect(isAlreadyPasscodeEnabled) {
        if (!isAlreadyPasscodeEnabled) {
            isVerified = false
            isChangingPasscode = false
        }
    }

    Scaffold(
        modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.sec_title),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding() // CRITICAL: Keyboard dynamically pushes fields and buttons up perfectly!
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp), // Reduced vertical padding to raise inputs
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp) // Tighter spacing to keep fields visible
        ) {

            // REQUIREMENT: Hide top banner completely in setup/change-password modes to raise input fields up
            if (isAlreadyPasscodeEnabled && !isChangingPasscode && !isVerified) {
                // Header decorative security banner (Decomposed Component)
                SecurityHeaderBanner(
                    isAlreadyPasscodeEnabled = isAlreadyPasscodeEnabled
                )
            }

            if (!isAlreadyPasscodeEnabled || isChangingPasscode) {
                // SETUP SECURITY FORM (Decomposed Component)
                SecuritySetupForm(
                    passcode = passcode,
                    onPasscodeChange = { passcode = it },
                    confirmPasscode = confirmPasscode,
                    onConfirmPasscodeChange = { confirmPasscode = it },
                    recoveryPhrase = recoveryPhrase,
                    onRecoveryPhraseChange = { recoveryPhrase = it },
                    recoveryHint = recoveryHint,
                    onRecoveryHintChange = { recoveryHint = it },
                    checkAcknowledged = checkAcknowledged,
                    onCheckAcknowledgedChange = { checkAcknowledged = it },
                    isSaving = isSaving,
                    onSave = {
                        val isValid = passcode.length == 4 &&
                                confirmPasscode == passcode &&
                                recoveryPhrase.isNotBlank() &&
                                checkAcknowledged &&
                                !isSaving
                        if (isValid) {
                            isSaving = true
                            coroutineScope.launch(Dispatchers.Default) {
                                val pHash = com.example.domain.HashUtils.hashString(passcode)
                                val rHash = com.example.domain.HashUtils.hashString(recoveryPhrase.trim())
                                val updated = currentSettings.copy(
                                    isPasscodeEnabled = true,
                                    passcodeHash = pHash,
                                    recoveryPhraseHash = rHash,
                                    recoveryHint = recoveryHint.trim().takeIf { it.isNotBlank() }
                                )
                                viewModel.saveSettings(updated)

                                withContext(Dispatchers.Main) {
                                    isSaving = false
                                    Toast.makeText(context, context.getString(R.string.sec_toast_enabled_success), Toast.LENGTH_SHORT).show()
                                    isVerified = false
                                    isChangingPasscode = false
                                    passcode = ""
                                    confirmPasscode = ""
                                    recoveryPhrase = ""
                                    recoveryHint = ""
                                    checkAcknowledged = false
                                    onBack()
                                }
                            }
                        }
                    }
                )
            } else {
                // SECURITY ALREADY ACTIVE VIEW (Decomposed Component)
                SecurityActivePanel(
                    currentSettings = currentSettings,
                    isVerified = isVerified,
                    onVerifySuccess = {
                        isVerified = true
                    },
                    onStartChangePasscode = {
                        isChangingPasscode = true
                    },
                    onCopyRecoveryPhrase = {
                        if (!currentSettings.recoveryPhraseHash.isNullOrBlank()) {
                            clipboardManager.setText(AnnotatedString(currentSettings.recoveryPhraseHash!!))
                            Toast.makeText(context, context.getString(R.string.sec_toast_copied), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeactivateSecurity = {
                        val updated = currentSettings.copy(
                            isPasscodeEnabled = false,
                            passcodeHash = null,
                            recoveryPhraseHash = null,
                            recoveryHint = null
                        )
                        viewModel.saveSettings(updated)
                        Toast.makeText(context, context.getString(R.string.sec_toast_disabled), Toast.LENGTH_SHORT).show()
                        isVerified = false
                        isChangingPasscode = false
                    }
                )
            }
        }
    }
}
