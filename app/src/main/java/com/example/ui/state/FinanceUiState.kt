package com.example.ui.state

import com.example.data.CloudBackupFile
import java.io.File

data class FinanceUiState(
    val cloudBackupsList: List<CloudBackupFile> = emptyList(),
    val isFetchingCloudBackups: Boolean = false,
    val localBackups: List<File> = emptyList(),
    val isPrivacyModeEnabled: Boolean = true,
    val tabOrder: String = "HABAYEB,LEDGER,MAKHZAN",
    val defaultStartDestination: String = "HABAYEB",
    val linkHabayebDebts: Boolean = false,
    val searchQuery: String = ""
)
