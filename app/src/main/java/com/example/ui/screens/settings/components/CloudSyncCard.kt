package com.example.ui.screens.settings.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CloudSyncState
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SoftRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncCard(
    googleSyncState: CloudSyncState,
    storedEmail: String?,
    isDark: Boolean,
    onSignInClick: () -> Unit,
    onAuthUrlClick: () -> Unit,
    onFallbackConfirm: (String) -> Unit,
    onUploadBackupClick: () -> Unit,
    onRestoreBackupClick: () -> Unit,
    onBrowseArchivesClick: () -> Unit,
    onLogoutClick: () -> Unit,
    isSyncLoggingOut: Boolean,
    onOpenGoogleDriveApp: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.backup_cloud_linking_title),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            when (val state = googleSyncState) {
                is CloudSyncState.Idle, is CloudSyncState.Error, is CloudSyncState.SessionExpired -> {
                    var showWebFallback by remember { mutableStateOf(false) }
                    var pastedWebCode by remember { mutableStateOf("") }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state is CloudSyncState.Error) {
                            Text(
                                text = "⚠️ " + state.message,
                                fontSize = 11.sp,
                                color = SoftRed,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Button(
                            onClick = onSignInClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(stringResource(R.string.backup_btn_gdrive_quick), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = { showWebFallback = !showWebFallback },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = if (showWebFallback) stringResource(R.string.backup_btn_gdrive_fallback_hide) else stringResource(R.string.backup_btn_gdrive_fallback_show),
                                color = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
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
                                        text = stringResource(R.string.backup_gdrive_fallback_steps),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    
                                    Button(
                                        onClick = onAuthUrlClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Text(stringResource(R.string.backup_btn_gdrive_open_browser), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Text(
                                        text = stringResource(R.string.backup_gdrive_fallback_desc),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = pastedWebCode,
                                        onValueChange = { pastedWebCode = it },
                                        placeholder = { Text(stringResource(R.string.backup_placeholder_oauth_code), fontSize = 11.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (isDark) Color(0xFF34D399) else EmeraldPrimary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            onFallbackConfirm(pastedWebCode)
                                        },
                                        enabled = pastedWebCode.trim().isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Text(stringResource(R.string.backup_btn_oauth_confirm), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    TextButton(
                                        onClick = onOpenGoogleDriveApp,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.backup_btn_gdrive_open_app),
                                            color = Color(0xFF3B82F6),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is CloudSyncState.Authenticating -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_toast_cloud_auth), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                is CloudSyncState.Syncing -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF10B981))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_toast_cloud_syncing), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                is CloudSyncState.Success, is CloudSyncState.Authenticated -> {
                    val email = if (state is CloudSyncState.Authenticated) state.email else (storedEmail ?: stringResource(R.string.cloud_google_connected))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val successBg = if (isDark) Color(0xFF1B3B2B) else Color(0xFFDCFCE7)
                        val successText = if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D)
                        val warningBg = if (isDark) Color(0xFF3E1F1F) else Color(0xFFFEF2F2)
                        val warningText = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(successBg, RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Text(
                                text = stringResource(R.string.backup_gdrive_linked_pattern, email),
                                color = successText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = stringResource(R.string.backup_gdrive_linked_warning),
                            fontSize = 10.sp,
                            color = warningText,
                            lineHeight = 14.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(warningBg, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onUploadBackupClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text(stringResource(R.string.backup_btn_upload_backup), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onRestoreBackupClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text(stringResource(id = R.string.btn_cloud_restore), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onBrowseArchivesClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("open_cloud_backups_archive_sheet")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.BackupTable, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text(stringResource(R.string.backup_btn_browse_archives), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onLogoutClick,
                            enabled = !isSyncLoggingOut,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, disabledContainerColor = MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSyncLoggingOut) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onErrorContainer, strokeWidth = 2.dp)
                                    Text(stringResource(R.string.backup_gdrive_logging_out), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                    Text(stringResource(R.string.backup_btn_gdrive_logout), fontSize = 10.sp, color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
