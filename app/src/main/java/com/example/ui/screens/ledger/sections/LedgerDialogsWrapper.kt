package com.example.ui.screens.ledger.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.FixedCommitment
import com.example.data.local.entities.TransactionDb
import com.example.domain.usecase.MonthLedger
import com.example.domain.usecase.DayLedger
import com.example.ui.screens.ledger.components.*
import com.example.ui.viewmodel.FinanceViewModel
import java.math.BigDecimal

@Composable
fun LedgerDialogsWrapper(
    viewModel: FinanceViewModel,
    settings: AppSettings,
    activeThemeColor: Color,
    activeSubColor: Color,

    // Day Selection / Delete
    showDeleteDaysDialog: Boolean,
    onShowDeleteDaysDialogChange: (Boolean) -> Unit,
    selectedDayKeys: List<String>,
    onClearSelectedDayKeys: () -> Unit,
    onDaySelectionModeChange: (Boolean) -> Unit,
    monthlyLedger: List<MonthLedger>,

    // Transaction Record
    showTxDialog: Boolean,
    onShowTxDialogChange: (Boolean) -> Unit,
    txDialogType: String,
    onTxDialogTypeChange: (String) -> Unit,
    editingTransaction: TransactionDb?,
    onEditingTransactionChange: (TransactionDb?) -> Unit,

    // Search
    showSearch: Boolean,
    onShowSearchChange: (Boolean) -> Unit,

    // Commitments List
    showCommitmentsListSheet: Boolean,
    onShowCommitmentsListSheetChange: (Boolean) -> Unit,
    reorderCommitmentTarget: FixedCommitment?,
    onReorderCommitmentTargetChange: (FixedCommitment?) -> Unit,
    showCommitmentDialog: Boolean,
    onShowCommitmentDialogChange: (Boolean) -> Unit,
    editingCommitment: FixedCommitment?,
    onEditingCommitmentChange: (FixedCommitment?) -> Unit,
    commitments: List<FixedCommitment>,
    computedCommitments: List<Triple<FixedCommitment, Double, Double>>,
    totalCash: BigDecimal,

    // Activation
    showActivationDialog: Boolean,
    onShowActivationDialogChange: (Boolean) -> Unit,
    deviceId: String,

    // Active Day transactions popup
    activeDayKey: String?,
    onActiveDayKeyChange: (String?) -> Unit,
    activeDayLedger: DayLedger?
) {
    // DeleteDaysConfirmDialog
    DeleteDaysConfirmDialog(
        showDeleteDaysDialog = showDeleteDaysDialog,
        onDismiss = { onShowDeleteDaysDialogChange(false) },
        monthlyLedger = monthlyLedger,
        selectedDayKeys = selectedDayKeys.toMutableList(),
        viewModel = viewModel,
        scope = androidx.compose.runtime.rememberCoroutineScope(),
        context = androidx.compose.ui.platform.LocalContext.current,
        onSuccess = {
            onClearSelectedDayKeys()
            onDaySelectionModeChange(false)
            onShowDeleteDaysDialogChange(false)
        }
    )

    // TransactionRecordDialog
    TransactionRecordDialog(
        showTxDialog = showTxDialog,
        txDialogType = txDialogType,
        editingTransaction = editingTransaction,
        currencySymbol = settings.currencySymbol,
        onDismiss = { onShowTxDialogChange(false) },
        onSave = { _, type, category, amount, description ->
            if (editingTransaction != null) {
                val tx = editingTransaction.copy(
                    amount = amount,
                    description = description,
                    category = category
                )
                viewModel.updateTransaction(tx)
            } else {
                viewModel.addTransaction(
                    type = type,
                    category = category,
                    amount = amount,
                    description = description
                )
            }
            onShowTxDialogChange(false)
        }
    )

    // Search Dialog
    if (showSearch) {
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val searchResults by viewModel.searchResultsState.collectAsStateWithLifecycle()
        SearchLedgerDialog(
            query = searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            results = searchResults,
            formatCurrency = { amt -> viewModel.formatCurrency(BigDecimal.valueOf(amt), settings.currencySymbol) },
            onDismiss = { onShowSearchChange(false) }
        )
    }

    // Commitments List Popup Dialog
    CommitmentsListDialog(
        showCommitmentsListSheet = showCommitmentsListSheet,
        commitments = commitments,
        computedCommitments = computedCommitments,
        totalCash = totalCash,
        currencySymbol = settings.currencySymbol,
        formatCurrency = { amt, sym -> viewModel.formatCurrency(amt, sym) },
        formatDoubleCurrency = { amt, sym -> viewModel.formatDoubleCurrency(amt, sym) },
        onDismissRequest = { onShowCommitmentsListSheetChange(false) },
        onAddCommitmentClick = {
            onEditingCommitmentChange(null)
            onShowCommitmentDialogChange(true)
        },
        onEditCommitmentClick = { fc ->
            onEditingCommitmentChange(fc)
            onShowCommitmentDialogChange(true)
        },
        onDeleteCommitment = { name -> viewModel.deleteCommitment(name) },
        onReorderCommitment = { fc, pos -> viewModel.reorderCommitment(fc, pos) },
        onCheckedChange = { fc, checked ->
            viewModel.saveCommitment(fc.name, fc.targetAmount, if (checked) fc.targetAmount else 0.0)
        },
        onSetReorderTarget = { onReorderCommitmentTargetChange(it) }
    )

    // Commitment Add/Edit Dialog
    CommitmentEditDialog(
        showCommitmentDialog = showCommitmentDialog,
        editingCommitment = editingCommitment,
        onDismissRequest = {
            onShowCommitmentDialogChange(false)
            onEditingCommitmentChange(null)
        },
        onSaveCommitment = { name, targetAmount, currentProgress ->
            viewModel.saveCommitment(name, targetAmount, currentProgress)
            onShowCommitmentDialogChange(false)
            onEditingCommitmentChange(null)
        },
        onDeleteCommitment = { name ->
            viewModel.deleteCommitment(name)
            onShowCommitmentDialogChange(false)
            onEditingCommitmentChange(null)
        }
    )

    ReorderCommitmentDialog(
        reorderCommitmentTarget = reorderCommitmentTarget,
        commitmentsSize = commitments.size,
        onDismiss = { onReorderCommitmentTargetChange(null) },
        onApplyReorder = { target, position ->
            viewModel.reorderCommitment(target, position)
            onReorderCommitmentTargetChange(null)
        },
        context = androidx.compose.ui.platform.LocalContext.current
    )

    if (showActivationDialog) {
        DeviceActivationDialog(
            deviceId = deviceId,
            viewModel = viewModel,
            onDismiss = { onShowActivationDialogChange(false) }
        )
    }

    // Active Day Transactions Dialog
    ActiveDayTransactionsDialog(
        activeDayKey = activeDayKey,
        activeDayLedger = activeDayLedger,
        currencySymbol = settings.currencySymbol,
        onDismiss = { onActiveDayKeyChange(null) },
        onDeleteTransaction = { txId -> viewModel.deleteTransactionById(txId) },
        onEditTransaction = { tx ->
            onEditingTransactionChange(tx)
            onTxDialogTypeChange(tx.type)
            onShowTxDialogChange(true)
        },
        formatDoubleCurrency = { amt, sym -> viewModel.formatDoubleCurrency(amt, sym) },
        formatCurrency = { amt, sym -> viewModel.formatCurrency(amt, sym) }
    )
}
