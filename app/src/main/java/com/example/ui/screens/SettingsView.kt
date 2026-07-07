package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CloudSyncState
import com.example.data.local.entities.AppSettings
import com.example.ui.helper.PermissionHelper
import com.example.ui.helper.rememberBackupPermissionHelper
import com.example.ui.screens.habayeb.components.ExchangeRateSetupDialog
import com.example.ui.screens.settings.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsView(
    viewModel: FinanceViewModel,
    settings: AppSettings,
    googleAuthManager: com.example.ui.screens.settings.GoogleAuthManager,
    storageManager: com.example.ui.screens.settings.StorageManager,
    onNavigateToSecurity: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current

    val isDark = when (settings.themeMode) {
        1 -> false
        2 -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    val localBackups by viewModel.localBackups.collectAsStateWithLifecycle()
    val googleCloudSyncState by viewModel.googleDriveSyncState.collectAsStateWithLifecycle()
    var showBackupPermissionExplanationDialog by remember { mutableStateOf(false) }
    var onPermissionGrantedCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    var currencySymbol by remember { mutableStateOf(settings.currencySymbol) }
    var currenciesToSetup by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentSetupIndex by remember { mutableStateOf(0) }
    var showSetupDialog by remember { mutableStateOf(false) }
    var isAutoBackupEnabled by remember { mutableStateOf(settings.isAutoBackupEnabled) }

    LaunchedEffect(settings.currencySymbol) {
        currencySymbol = settings.currencySymbol
    }

    val permissionHelper = rememberBackupPermissionHelper(context) {
        onPermissionGrantedCallback?.invoke()
    }

    LaunchedEffect(googleCloudSyncState) {
        if (googleCloudSyncState is CloudSyncState.SessionExpired) {
            Toast.makeText(context, context.getString(R.string.toast_reconnect_cloud), Toast.LENGTH_LONG).show()
            googleAuthManager.signIn()
        } else if (googleCloudSyncState is CloudSyncState.Success) {
            Toast.makeText(context, context.getString(R.string.settings_gdrive_sync_success), Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 40.dp)
    ) {
        // App Settings Header Card (Visual Title Card)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 1. Preferences Card (Currency & Theme Settings)
        item {
            PreferencesSection(
                settings = settings,
                currencySymbol = currencySymbol,
                onCurrencySymbolChange = { newSymbol ->
                    currencySymbol = newSymbol
                    viewModel.migrateBaseCurrency(newSymbol) {
                        // Identify other currencies needing rates setup after base migration completes
                        val otherCurrencies = listOf(
                            context.getString(R.string.currency_yer),
                            context.getString(R.string.currency_usd),
                            context.getString(R.string.currency_sar)
                        ).filter { it != newSymbol }
                        val missingRates = otherCurrencies.filter { other ->
                            !com.example.ui.screens.habayeb.utils.ExchangeRateHelper.hasRate(settings.exchangeRatesJson, newSymbol, other)
                        }
                        if (missingRates.isNotEmpty()) {
                            currenciesToSetup = missingRates
                            currentSetupIndex = 0
                            showSetupDialog = true
                        }
                    }
                },
                onThemeModeChange = { modeIndex ->
                    viewModel.saveSettings(settings.copy(themeMode = modeIndex))
                }
            )
        }

        // Custom Signature Fingerprint Card
        item {
            SignatureCard()
        }

        // 2. Security & Protection Action Button
        item {
            OutlinedButton(
                onClick = onNavigateToSecurity,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBackIosNew,
                        contentDescription = stringResource(id = R.string.desc_login),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.drawer_security_label),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = stringResource(id = R.string.desc_security),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 3. Backup & Cloud Synchronization Card (Quad-Backup Section)
        item {
            BackupSection(
                viewModel = viewModel,
                localBackups = localBackups,
                googleCloudSyncState = googleCloudSyncState,
                isDark = isDark,
                onBackupRestoreSuccess = { restoredSettings ->
                    currencySymbol = restoredSettings.currencySymbol
                },
                onSafExportRequested = { filename ->
                    storageManager.exportBackup(filename)
                },
                onSafRestoreRequested = {
                    storageManager.restoreBackup()
                },
                onPermissionExplanationRequested = { onGranted ->
                    onPermissionGrantedCallback = onGranted
                    showBackupPermissionExplanationDialog = true
                },
                checkBackupPermissionsGranted = {
                    PermissionHelper.isBackupPermissionGranted(context)
                }
            )
        }

        // 3.5 Auto Backup Background Card
        item {
            SettingsAutoBackupCard(
                isAutoBackupEnabled = isAutoBackupEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        val enableAutoBackup = {
                            isAutoBackupEnabled = true
                            viewModel.saveSettings(settings.copy(isAutoBackupEnabled = true))
                            com.example.AutoBackupWorker.scheduleDailyBackupWorker(context)
                            Toast.makeText(context, context.getString(R.string.settings_toast_auto_backup_enabled), Toast.LENGTH_SHORT).show()
                        }
                        if (PermissionHelper.isBackupPermissionGranted(context)) {
                            enableAutoBackup()
                        } else {
                            onPermissionGrantedCallback = enableAutoBackup
                            showBackupPermissionExplanationDialog = true
                        }
                    } else {
                        isAutoBackupEnabled = false
                        viewModel.saveSettings(settings.copy(isAutoBackupEnabled = false))
                        com.example.AutoBackupWorker.cancelDailyBackupWorker(context)
                        Toast.makeText(context, context.getString(R.string.settings_toast_auto_backup_disabled), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // 4. Danger Zone Card
        item {
            DangerZone(viewModel = viewModel)
        }

        // 5. Developer Info Footer Card
        item {
            SettingsDeveloperFooter(context = context)
        }
    }

    if (showBackupPermissionExplanationDialog) {
        BackupPermissionExplanationDialog(
            onDismiss = { showBackupPermissionExplanationDialog = false },
            onGrantPermissions = {
                showBackupPermissionExplanationDialog = false
                permissionHelper.requestPermissions()
            },
            onUseInternalStorage = {
                showBackupPermissionExplanationDialog = false
                onPermissionGrantedCallback?.invoke()
            }
        )
    }

    if (showSetupDialog && currentSetupIndex < currenciesToSetup.size) {
        val targetCurrency = currenciesToSetup[currentSetupIndex]
        ExchangeRateSetupDialog(
            currencySymbol = currencySymbol,
            selectedCurrency = targetCurrency,
            initialRateStr = "",
            activeThemeColor = MaterialTheme.colorScheme.primary,
            onDismiss = {
                if (currentSetupIndex + 1 < currenciesToSetup.size) {
                    currentSetupIndex++
                } else {
                    showSetupDialog = false
                    currenciesToSetup = emptyList()
                }
            },
            onConfirm = { newRate ->
                val currentSettings = viewModel.settingsState.value
                val updatedSettings = currentSettings.copy(
                    exchangeRatesJson = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.setRate(currentSettings.exchangeRatesJson, currencySymbol, targetCurrency, newRate)
                )
                viewModel.saveSettings(updatedSettings)
                if (currentSetupIndex + 1 < currenciesToSetup.size) {
                    currentSetupIndex++
                } else {
                    showSetupDialog = false
                    currenciesToSetup = emptyList()
                }
            }
        )
    }
}
