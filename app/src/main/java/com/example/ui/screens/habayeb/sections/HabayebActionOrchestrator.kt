package com.example.ui.screens.habayeb.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.components.AddCustomerPopup
import com.example.ui.screens.habayeb.components.CustomerHistoryOverlay
import com.example.ui.screens.habayeb.components.AddTransactionPopup
import com.example.ui.screens.habayeb.components.DeleteConfirmDialog
import com.example.ui.screens.habayeb.components.EditCustomerDialog
import com.example.ui.state.CustomerUiState
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@Composable
fun HabayebActionOrchestrator(
    showAddCustomerDialog: Boolean,
    onShowAddCustomerDialogChanged: (Boolean) -> Unit,
    activeCustomerForHistory: HabayebCustomer?,
    onActiveCustomerForHistoryChanged: (HabayebCustomer?) -> Unit,
    stableCustomer: HabayebCustomer?,
    onStableCustomerChanged: (HabayebCustomer?) -> Unit,
    showAddTransactionDialogForCustomer: HabayebCustomer?,
    onShowAddTransactionDialogForCustomerChanged: (HabayebCustomer?) -> Unit,
    defaultTransactionTypeForDialog: String,
    onDefaultTransactionTypeForDialogChanged: (String) -> Unit,
    editingTransactionForDialog: HabayebTransaction?,
    onEditingTransactionForDialogChanged: (HabayebTransaction?) -> Unit,
    showDeleteConfirmDialog: Boolean,
    onShowDeleteConfirmDialogChanged: (Boolean) -> Unit,
    showEditCustomerDialog: Boolean,
    onShowEditCustomerDialogChanged: (Boolean) -> Unit,
    editingCustomerForDialog: HabayebCustomer?,
    onEditingCustomerForDialogChanged: (HabayebCustomer?) -> Unit,
    selectedCustomerIds: List<String>,
    onSelectedCustomerIdsCleared: () -> Unit,
    onMultiSelectActiveChanged: (Boolean) -> Unit,
    viewModel: FinanceViewModel,
    activeThemeColor: Color,
    activeSubColor: Color,
    currencySymbol: String,
    contentPadding: PaddingValues,
    filteredCustomers: List<CustomerUiState>,
    listState: LazyListState
) {
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activeCustomerForHistory) {
        if (activeCustomerForHistory != null) {
            onStableCustomerChanged(activeCustomerForHistory)
        }
    }

    // 1. ADD NEW CUSTOMER DIALOG
    if (showAddCustomerDialog) {
        AddCustomerPopup(
            viewModel = viewModel,
            onDismiss = { onShowAddCustomerDialogChanged(false) },
            onCustomerAdded = { newCustomerId ->
                coroutineScope.launch {
                    kotlinx.coroutines.delay(300) // allow UI to reflect state
                    val index = filteredCustomers.indexOfFirst { it.id == newCustomerId }
                    if (index >= 0) {
                        try {
                            listState.animateScrollToItem(index)
                        } catch (e: Exception) {}
                    } else {
                        try {
                            listState.animateScrollToItem(0)
                        } catch (e: Exception) {}
                    }
                }
            },
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor
        )
    }

    // 2. DETAILED CUSTOMER DEBT TRANSACTION HISTORY OVERLAY
    AnimatedVisibility(
        visible = activeCustomerForHistory != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.zIndex(10f)
    ) {
        stableCustomer?.let { customer ->
            CustomerHistoryOverlay(
                customer = customer,
                viewModel = viewModel,
                onDismiss = { onActiveCustomerForHistoryChanged(null) },
                onAddTransaction = { c, type ->
                    onDefaultTransactionTypeForDialogChanged(type)
                    onShowAddTransactionDialogForCustomerChanged(c)
                },
                activeThemeColor = activeThemeColor,
                activeSubColor = activeSubColor,
                currencySymbol = currencySymbol,
                contentPadding = contentPadding
            )
        }
    }

    // 3. ADD/EDIT DEBT TRANSACTION POPUP
    if (showAddTransactionDialogForCustomer != null) {
        AddTransactionPopup(
            customer = showAddTransactionDialogForCustomer,
            viewModel = viewModel,
            initialSelectedType = defaultTransactionTypeForDialog,
            editingTransaction = editingTransactionForDialog,
            onDismiss = {
                onShowAddTransactionDialogForCustomerChanged(null)
                onEditingTransactionForDialogChanged(null)
            },
            onTransactionAdded = { customerId ->
                coroutineScope.launch {
                    kotlinx.coroutines.delay(300)
                    val index = filteredCustomers.indexOfFirst { it.id == customerId }
                    if (index >= 0) {
                        try {
                            listState.animateScrollToItem(index)
                        } catch (e: Exception) {}
                    }
                }
            },
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor
        )
    }

    // 4. MULTI-DELETE / SINGLE DELETE CONFIRMATION DIALOG
    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            selectedCustomerIds = selectedCustomerIds,
            viewModel = viewModel,
            onDismiss = {
                onShowDeleteConfirmDialogChanged(false)
            },
            onSuccessBulkDelete = {
                onSelectedCustomerIdsCleared()
                onMultiSelectActiveChanged(false)
            }
        )
    }

    // 5. EDIT CUSTOMER DIALOG
    if (showEditCustomerDialog && editingCustomerForDialog != null) {
        EditCustomerDialog(
            customer = editingCustomerForDialog,
            viewModel = viewModel,
            activeThemeColor = activeThemeColor,
            onDismiss = { onShowEditCustomerDialogChanged(false) }
        )
    }
}
