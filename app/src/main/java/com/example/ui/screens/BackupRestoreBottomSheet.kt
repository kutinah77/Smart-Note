package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CloudSyncState
import com.example.data.local.entities.AppSettings
import com.example.ui.screens.settings.components.*
import com.example.ui.screens.settings.rememberGoogleAuthManager
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SoftRed
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Dialog states unified into a single sealed class
sealed class BackupUiDialog {
    object None : BackupUiDialog()
    object PasteBase64 : BackupUiDialog()
    object ResetConfirm1 : BackupUiDialog()
    object ResetConfirm2 : BackupUiDialog()
    data class RestoreConfirm(val onConfirm: () -> Unit) : BackupUiDialog()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreBottomSheet(
    settings: AppSettings,
    viewModel: FinanceViewModel,
    onExportMzd: () -> Unit,
    onImportMzd: () -> Unit,
    onImportBase64: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = com.example.ui.theme.LocalIsDark.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val googleSyncState by viewModel.googleDriveSyncState.collectAsStateWithLifecycle()
    val storedEmail = remember(googleSyncState) { viewModel.googleDriveSyncHelper.getStoredEmail() }
    val isConnected = !storedEmail.isNullOrEmpty() || googleSyncState is CloudSyncState.Authenticated || googleSyncState is CloudSyncState.Success

    var showCloudBackupsSheet by remember { mutableStateOf(false) }
    var isSyncLoggingOut by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<BackupUiDialog>(BackupUiDialog.None) }
    var pasteText by remember { mutableStateOf("") }

    val googleAuthManager = rememberGoogleAuthManager(viewModel, context)

    LaunchedEffect(googleSyncState) {
        if (googleSyncState is CloudSyncState.SessionExpired) {
            googleAuthManager.signIn()
        } else if (googleSyncState is CloudSyncState.Success) {
            Toast.makeText(context, context.getString(R.string.backup_toast_sync_success), Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = stringResource(R.string.backup_sheet_title),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                val connectedBg = if (isDark) Color(0xFF1B3B2B) else Color(0xFFDCFCE7)
                val connectedText = if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isConnected) connectedBg else MaterialTheme.colorScheme.outlineVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = if (isConnected) stringResource(R.string.backup_status_connected) else stringResource(R.string.backup_status_local),
                            fontSize = 9.sp,
                            color = if (isConnected) connectedText else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // 1. Cloud Sync Component (Google Drive)
            CloudSyncCard(
                googleSyncState = googleSyncState,
                storedEmail = storedEmail,
                isDark = isDark,
                onSignInClick = { googleAuthManager.signIn() },
                onAuthUrlClick = {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(viewModel.googleDriveSyncHelper.getAuthUrl()))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.backup_toast_browser_failed), Toast.LENGTH_SHORT).show()
                    }
                },
                onFallbackConfirm = { rawCode ->
                    val cleanCode = rawCode.trim()
                    if (cleanCode.isNotEmpty()) {
                        val finalCode = if (cleanCode.startsWith("http://") || cleanCode.startsWith("https://") || cleanCode.contains("code=")) {
                            var extracted = ""
                            try {
                                val parsedUri = android.net.Uri.parse(cleanCode)
                                extracted = parsedUri.getQueryParameter("code") ?: ""
                            } catch (e: Exception) {}
                            if (extracted.isEmpty()) {
                                val idx = cleanCode.indexOf("code=")
                                if (idx != -1) {
                                    val start = idx + 5
                                    val end = cleanCode.indexOf("&", start).let { if (it == -1) cleanCode.length else it }
                                    extracted = cleanCode.substring(start, end)
                                }
                            }
                            extracted.takeIf { it.isNotEmpty() } ?: cleanCode
                        } else {
                            cleanCode
                        }

                        viewModel.handleGoogleOAuthCode(finalCode, null, "http://localhost/oauth2callback") { success ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.backup_toast_oauth_success), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, context.getString(R.string.backup_toast_oauth_failed), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                onUploadBackupClick = {
                    viewModel.uploadBackupToGoogleDrive { }
                },
                onRestoreBackupClick = {
                    activeDialog = BackupUiDialog.RestoreConfirm {
                        viewModel.restoreFromGoogleDriveDirect(context) { success ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.toast_cloud_restore_success), Toast.LENGTH_SHORT).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, context.getString(R.string.toast_cloud_restore_failed_or_missing), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                onBrowseArchivesClick = {
                    showCloudBackupsSheet = true
                },
                onLogoutClick = {
                    isSyncLoggingOut = true
                    viewModel.googleDriveLogout {
                        isSyncLoggingOut = false
                        Toast.makeText(context, context.getString(R.string.backup_toast_gdrive_logout_success), Toast.LENGTH_SHORT).show()
                    }
                },
                isSyncLoggingOut = isSyncLoggingOut,
                onOpenGoogleDriveApp = {
                    com.example.ui.helper.openGoogleDriveApp(context)
                }
            )

            // 2. Local Action Component
            LocalActionsSection(
                onExportMzd = onExportMzd,
                onCopyBase64 = {
                    viewModel.getBackupJsonForClipboard { json ->
                        coroutineScope.launch(Dispatchers.Default) {
                            try {
                                val base64 = com.example.data.serialization.MzdBackupSerializer.encodeToBase64(json)
                                withContext(Dispatchers.Main) {
                                    clipboardManager.setText(AnnotatedString(base64))
                                    Toast.makeText(context, context.getString(R.string.backup_toast_copied_success), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("BackupRestoreBottomSheet", "Failed to encode copy to clipboard", e)
                            }
                        }
                    }
                },
                onShareBackup = {
                    viewModel.getBackupJsonForClipboard { json ->
                        try {
                            val cacheDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
                            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US).format(java.util.Date())
                            val file = File(cacheDir, "Mizan_$dateStr.mzd").apply { writeText(json) }
                            com.example.ui.helper.shareBackupFile(context, file)
                        } catch (e: Exception) {
                            Toast.makeText(context, context.getString(R.string.export_backup_failed, e.localizedMessage ?: ""), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onImportMzd = {
                    activeDialog = BackupUiDialog.RestoreConfirm {
                        onImportMzd()
                    }
                },
                onPasteBase64Click = {
                    activeDialog = BackupUiDialog.PasteBase64
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Danger Zone Action Component
            DangerZoneSection(
                onResetClick = {
                    activeDialog = BackupUiDialog.ResetConfirm1
                }
            )
        }
    }

    // Unified Dialog Management
    when (val dialog = activeDialog) {
        is BackupUiDialog.None -> {}
        is BackupUiDialog.PasteBase64 -> {
            AlertDialog(
                onDismissRequest = { activeDialog = BackupUiDialog.None },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                title = { Text(stringResource(R.string.backup_dialog_restore_paste_title), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        placeholder = { Text(stringResource(R.string.backup_dialog_restore_paste_desc)) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Left, fontSize = 11.sp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = if (isDark) Color(0xFF34D399) else EmeraldPrimary)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val decodedJson = com.example.data.serialization.MzdBackupSerializer.decodeFromBase64(pasteText.trim())
                                activeDialog = BackupUiDialog.RestoreConfirm {
                                    onImportBase64(decodedJson)
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.backup_toast_paste_failed), Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.backup_btn_restore_now), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = BackupUiDialog.None }) {
                        Text(stringResource(R.string.backup_btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
        is BackupUiDialog.ResetConfirm1 -> {
            AlertDialog(
                onDismissRequest = { activeDialog = BackupUiDialog.None },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                title = { Text(stringResource(R.string.backup_reset1_title), color = SoftRed, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text(
                        stringResource(R.string.backup_reset1_desc),
                        color = Color.DarkGray, fontSize = 12.sp, lineHeight = 20.sp, textAlign = TextAlign.Right
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            activeDialog = BackupUiDialog.ResetConfirm2
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.backup_reset_confirm_btn), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = BackupUiDialog.None }) {
                        Text(stringResource(R.string.backup_reset_cancel_btn), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
        is BackupUiDialog.ResetConfirm2 -> {
            AlertDialog(
                onDismissRequest = { activeDialog = BackupUiDialog.None },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                title = { Text(stringResource(R.string.backup_reset2_title), color = SoftRed, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text(
                        stringResource(R.string.backup_reset2_desc),
                        color = Color.DarkGray, fontSize = 12.sp, lineHeight = 20.sp, textAlign = TextAlign.Right
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearLocalCopyAndWipeMemory(context)
                            Toast.makeText(context, context.getString(R.string.backup_toast_reset_success), Toast.LENGTH_LONG).show()
                            activeDialog = BackupUiDialog.None
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.backup_reset_final_btn), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeDialog = BackupUiDialog.None }) {
                        Text(stringResource(R.string.backup_reset_final_cancel_btn), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
        is BackupUiDialog.RestoreConfirm -> {
            AlertDialog(
                onDismissRequest = { activeDialog = BackupUiDialog.None },
                icon = {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.backup_restore_warn_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.backup_restore_warn_desc),
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Right
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            dialog.onConfirm()
                            activeDialog = BackupUiDialog.None
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.backup_restore_confirm_btn), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { activeDialog = BackupUiDialog.None },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.backup_reset_cancel_btn), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }

    if (showCloudBackupsSheet) {
        CloudBackupsBottomSheet(
            viewModel = viewModel,
            onDismiss = { showCloudBackupsSheet = false }
        )
    }
}
