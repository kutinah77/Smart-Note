package com.example.ui.screens.settings.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CloudSyncState
import com.example.ui.screens.CloudBackupsBottomSheet
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SoftRed
import com.example.ui.viewmodel.FinanceViewModel
import java.io.File

@Composable
fun BackupSection(
    viewModel: FinanceViewModel,
    localBackups: List<File>,
    googleCloudSyncState: CloudSyncState,
    isDark: Boolean,
    onBackupRestoreSuccess: (com.example.data.local.entities.AppSettings) -> Unit,
    onSafExportRequested: (String) -> Unit,
    onSafRestoreRequested: () -> Unit,
    onPermissionExplanationRequested: (() -> Unit) -> Unit,
    checkBackupPermissionsGranted: () -> Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showCloudBackupsSheet by remember { mutableStateOf(false) }
    var isSyncLoggingOut by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pastedBackupText by remember { mutableStateOf("") }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = stringResource(R.string.settings_quad_backup_title),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_quad_backup_subtitle),
                fontSize = 11.sp,
                color = Color(0xFF5A625E),
                textAlign = TextAlign.Right
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. الخيار الأول: تصدير واستيراد ملفات (.mzd)
                QuadBackupItem(
                    title = stringResource(R.string.settings_backup_portable_title),
                    description = stringResource(R.string.settings_backup_portable_desc),
                    accentColor = EmeraldPrimary,
                    icon = { Icon(Icons.Default.Save, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp)) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val runBackup = {
                                    viewModel.createLocalBackup(context) { file ->
                                        if (file != null) {
                                            com.example.ui.helper.shareBackupFile(context, file)
                                        }
                                    }
                                }
                                if (checkBackupPermissionsGranted()) {
                                    runBackup()
                                } else {
                                    onPermissionExplanationRequested(runBackup)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(stringResource(R.string.settings_export_mzd), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onSafRestoreRequested()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(stringResource(R.string.settings_import_mzd), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. الخيار الثاني: الاستنساخ النصي المشفر
                QuadBackupItem(
                    title = stringResource(R.string.settings_backup_base64_title),
                    description = stringResource(R.string.settings_backup_base64_desc),
                    accentColor = Color(0xFF6366F1),
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp)) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.getBackupJsonForClipboard { jsonStr ->
                                    clipboardManager.setText(AnnotatedString(jsonStr))
                                    Toast.makeText(context, context.getString(R.string.settings_toast_base64_copied), Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(stringResource(R.string.settings_copy_encrypted), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                pastedBackupText = ""
                                showPasteDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Text(stringResource(R.string.settings_restore_paste), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3. الخيار الثالث: التمرير السحابي اليدوي
                QuadBackupItem(
                    title = stringResource(R.string.settings_backup_gdrive_title),
                    description = stringResource(R.string.settings_backup_gdrive_desc),
                    accentColor = Color(0xFF0EA5E9),
                    icon = { Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color(0xFF0EA5E9), modifier = Modifier.size(18.dp)) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = {
                                val sdfName = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.US)
                                val dateStr = sdfName.format(java.util.Date())
                                onSafExportRequested("Mzd_$dateStr.mzd")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text(stringResource(R.string.settings_backup_gdrive_btn), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_backup_gdrive_note),
                            fontSize = 9.sp,
                            color = Color.LightGray.copy(alpha = 0.7f),
                            textAlign = TextAlign.Right
                        )
                    }
                }

                // 4. الخيار الرابع: المزامنة السحابية المباشرة (REST Client with WebView Auth)
                QuadBackupItem(
                    title = stringResource(R.string.settings_backup_cloud_title),
                    description = stringResource(R.string.settings_backup_cloud_desc),
                    accentColor = Color(0xFF10B981),
                    icon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp)) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        val googleSignInClient = remember(context) {
                            viewModel.googleDriveSyncHelper.getGoogleSignInClient(context)
                        }
                        val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                        ) { result ->
                            val intent = result.data
                            if (result.resultCode == android.app.Activity.RESULT_OK && intent != null) {
                                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(intent)
                                try {
                                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
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
                                    android.util.Log.e("BackupSection", "Google sign in failed", e)
                                    Toast.makeText(context, context.getString(R.string.settings_gdrive_link_failed_error_pattern, e.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.settings_gdrive_link_cancelled), Toast.LENGTH_SHORT).show()
                            }
                        }

                        when (val state = googleCloudSyncState) {
                            is CloudSyncState.Idle, is CloudSyncState.Error, is CloudSyncState.SessionExpired -> {
                                var showWebFallback by remember { mutableStateOf(false) }
                                var pastedWebCode by remember { mutableStateOf("") }
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Button(
                                        onClick = {
                                            try {
                                                val playServicesAvailable = run {
                                                    try {
                                                        val gmsClass = Class.forName("com.google.android.gms.common.GoogleApiAvailability")
                                                        val instanceMethod = gmsClass.getMethod("getInstance")
                                                        val gmsInstance = instanceMethod.invoke(null)
                                                        val isAvailableMethod = gmsClass.getMethod("isGooglePlayServicesAvailable", Context::class.java)
                                                        val status = isAvailableMethod.invoke(gmsInstance, context) as Int
                                                        status == 0
                                                    } catch (e: Exception) {
                                                        false
                                                    }
                                                }

                                                if (!playServicesAvailable) {
                                                    Toast.makeText(context, context.getString(R.string.settings_toast_google_missing), Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }

                                                googleSignInClient.signOut().addOnCompleteListener {
                                                    googleSignInClient.revokeAccess().addOnCompleteListener {
                                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, context.getString(R.string.settings_toast_no_network), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    ) {
                                        Text(stringResource(R.string.settings_btn_gdrive_quick), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    TextButton(
                                        onClick = { showWebFallback = !showWebFallback },
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text(
                                            text = if (showWebFallback) stringResource(R.string.settings_btn_gdrive_fallback_hide) else stringResource(R.string.settings_btn_gdrive_fallback_show),
                                            color = Color(0xFF10B981),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (showWebFallback) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.settings_gdrive_fallback_steps),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(viewModel.googleDriveSyncHelper.getAuthUrl()))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, context.getString(R.string.settings_toast_browser_failed), Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                                ) {
                                                    Text(stringResource(R.string.settings_btn_gdrive_open_browser), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Text(
                                                    text = stringResource(R.string.settings_gdrive_fallback_desc),
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 14.sp,
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                OutlinedTextField(
                                                    value = pastedWebCode,
                                                    onValueChange = { pastedWebCode = it },
                                                    placeholder = { Text(stringResource(R.string.settings_placeholder_oauth_code), fontSize = 11.sp) },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = Color(0xFF10B981),
                                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                                    )
                                                )

                                                Button(
                                                    onClick = {
                                                        val rawCode = pastedWebCode.trim()
                                                        if (rawCode.isNotEmpty()) {
                                                            val finalCode = if (rawCode.startsWith("http://") || rawCode.startsWith("https://") || rawCode.contains("code=")) {
                                                                var extracted = ""
                                                                try {
                                                                    val parsedUri = android.net.Uri.parse(rawCode)
                                                                    extracted = parsedUri.getQueryParameter("code") ?: ""
                                                                } catch (e: Exception) {}
                                                                if (extracted.isEmpty()) {
                                                                    val idx = rawCode.indexOf("code=")
                                                                    if (idx != -1) {
                                                                        val start = idx + 5
                                                                        val end = rawCode.indexOf("&", start).let { if (it == -1) rawCode.length else it }
                                                                        extracted = rawCode.substring(start, end)
                                                                    }
                                                                }
                                                                extracted.takeIf { it.isNotEmpty() } ?: rawCode
                                                            } else {
                                                                rawCode
                                                            }

                                                            viewModel.handleGoogleOAuthCode(finalCode, null, "http://localhost/oauth2callback") { success ->
                                                                if (success) {
                                                                    Toast.makeText(context, context.getString(R.string.settings_toast_oauth_success), Toast.LENGTH_LONG).show()
                                                                } else {
                                                                    Toast.makeText(context, context.getString(R.string.settings_toast_oauth_failed), Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    enabled = pastedWebCode.trim().isNotEmpty(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().height(38.dp)
                                                ) {
                                                    Text(stringResource(R.string.settings_btn_oauth_confirm), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                TextButton(
                                                    onClick = { com.example.ui.helper.openGoogleDriveApp(context) },
                                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.settings_btn_gdrive_open_app),
                                                        color = Color(0xFF3B82F6),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (state is CloudSyncState.Error) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = stringResource(R.string.settings_state_prefix) + state.message,
                                            color = SoftRed,
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                            }
                            is CloudSyncState.Authenticating -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF10B981))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = stringResource(R.string.settings_toast_cloud_auth), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            is CloudSyncState.Syncing -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF10B981))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = stringResource(R.string.settings_toast_cloud_syncing), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            is CloudSyncState.Success, is CloudSyncState.Authenticated -> {
                                val storedEmail = viewModel.googleDriveSyncHelper.getStoredEmail()
                                val email = if (state is CloudSyncState.Authenticated) state.email else (storedEmail ?: stringResource(R.string.cloud_google_connected))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isDark) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = if (isDark) Color(0xFF81C784) else Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                        Text(
                                            text = stringResource(R.string.settings_gdrive_linked_pattern, email),
                                            color = if (isDark) Color(0xFF81C784) else Color(0xFF15803D),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Text(
                                        text = stringResource(R.string.settings_gdrive_linked_warning),
                                        fontSize = 10.sp,
                                        color = if (isDark) Color(0xFFEF9A9A) else Color(0xFF991B1B),
                                        lineHeight = 14.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isDark) Color(0xFFC62828).copy(alpha = 0.2f) else Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.uploadBackupToGoogleDrive { success -> }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).height(40.dp)
                                        ) {
                                            Text(stringResource(R.string.settings_btn_upload_backup), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.restoreFromGoogleDriveDirect(context) { success ->
                                                    if (success) {
                                                        Toast.makeText(context, context.getString(R.string.settings_toast_gdrive_restore_success), Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, context.getString(R.string.settings_toast_gdrive_restore_failed), Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f).height(40.dp)
                                        ) {
                                            Text(stringResource(R.string.settings_btn_download_backup), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            showCloudBackupsSheet = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(40.dp).testTag("open_cloud_backups_archive_button")
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.BackupTable, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Text(stringResource(R.string.settings_btn_browse_archives), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            isSyncLoggingOut = true
                                            viewModel.googleDriveLogout {
                                                isSyncLoggingOut = false
                                                Toast.makeText(context, context.getString(R.string.settings_toast_gdrive_logout_success), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = !isSyncLoggingOut,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, disabledContainerColor = MaterialTheme.colorScheme.outlineVariant),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSyncLoggingOut) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onErrorContainer, strokeWidth = 2.dp)
                                                Text(stringResource(R.string.settings_gdrive_logging_out), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                                Text(stringResource(R.string.settings_btn_gdrive_logout), fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showCloudBackupsSheet) {
                CloudBackupsBottomSheet(
                    viewModel = viewModel,
                    onDismiss = { showCloudBackupsSheet = false }
                )
            }

            if (showPasteDialog) {
                AlertDialog(
                    onDismissRequest = { showPasteDialog = false },
                    title = {
                        Text(stringResource(R.string.settings_dialog_restore_paste_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    },
                    text = {
                        Column {
                            Text(stringResource(R.string.settings_dialog_restore_paste_desc), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pastedBackupText,
                                onValueChange = { pastedBackupText = it },
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                placeholder = { Text(stringResource(R.string.settings_placeholder_paste_encoded)) }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (pastedBackupText.isNotBlank()) {
                                viewModel.executeMasterRestore(pastedBackupText, context) { success, restoredSettings ->
                                    if (success && restoredSettings != null) {
                                        onBackupRestoreSuccess(restoredSettings)
                                        pastedBackupText = ""
                                        showPasteDialog = false
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.settings_toast_paste_failed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }) {
                            Text(stringResource(R.string.settings_btn_restore_now), color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPasteDialog = false }) {
                            Text(stringResource(R.string.settings_btn_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.settings_discovered_backups_title),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (localBackups.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_no_local_backups),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                ) {
                    localBackups.forEach { file ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.restoreFromLocalFile(file, context) { success, restoredSettings ->
                                        if (success && restoredSettings != null) {
                                            onBackupRestoreSuccess(restoredSettings)
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = stringResource(R.string.settings_desc_restore), tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = file.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Right,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
