package com.example.ui.screens.habayeb.components.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.habayeb.components.CustomerHistoryTopBar

@Composable
fun HistoryHeader(
    customerName: String,
    customerPhone: String,
    isSearchActive: Boolean,
    txSearchQuery: String,
    activeThemeColor: Color,
    isPdfExporting: Boolean,
    isPhoneAvailable: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchClose: () -> Unit,
    onSearchOpen: () -> Unit,
    onPdfExportClick: () -> Unit,
    onCsvExportClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onFilterClick: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomerHistoryTopBar(
        customerName = customerName,
        customerPhone = customerPhone,
        isSearchActive = isSearchActive,
        txSearchQuery = txSearchQuery,
        activeThemeColor = activeThemeColor,
        isPdfExporting = isPdfExporting,
        isPhoneAvailable = isPhoneAvailable,
        onSearchQueryChange = onSearchQueryChange,
        onSearchClose = onSearchClose,
        onSearchOpen = onSearchOpen,
        onPdfExportClick = onPdfExportClick,
        onCsvExportClick = onCsvExportClick,
        onWhatsAppClick = onWhatsAppClick,
        onDeleteClick = onDeleteClick,
        onEditClick = onEditClick,
        onFilterClick = onFilterClick,
        onDismiss = onDismiss
    )
}
