package com.example.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StorageManager(
    private val onExportClick: (String) -> Unit,
    private val onRestoreClick: () -> Unit
) {
    fun exportBackup(filename: String) {
        onExportClick(filename)
    }

    fun restoreBackup() {
        onRestoreClick()
    }
}

@Composable
fun rememberStorageManager(
    viewModel: FinanceViewModel,
    context: Context,
    onRestoreSuccess: (AppSettings) -> Unit
): StorageManager {
    val coroutineScope = rememberCoroutineScope()

    val safExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.getBackupJsonForClipboard { jsonStr ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonStr.toByteArray())
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, context.getString(R.string.settings_toast_synced_desc), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, context.getString(R.string.toast_backup_export_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val safRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonText = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (jsonText.isNotBlank()) {
                    viewModel.executeMasterRestore(jsonText, context) { success, restoredSettings ->
                        if (success && restoredSettings != null) {
                            onRestoreSuccess(restoredSettings)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    return remember {
        StorageManager(
            onExportClick = { filename ->
                safExportLauncher.launch(filename)
            },
            onRestoreClick = {
                safRestoreLauncher.launch(arrayOf("application/*"))
            }
        )
    }
}
