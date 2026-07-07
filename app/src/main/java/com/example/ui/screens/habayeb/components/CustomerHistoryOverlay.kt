package com.example.ui.screens.habayeb.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.domain.usecase.CalculateRunningBalanceUseCase
import com.example.domain.usecase.FilterCustomerHistoryUseCase
import com.example.data.serialization.PdfReportGenerator
import com.example.ui.helper.VibrationHelper
import com.example.ui.screens.habayeb.utils.HabayebRecurringManager
import com.example.ui.screens.habayeb.utils.CommunicationHelper
import com.example.ui.screens.habayeb.components.history.HistoryHeader
import com.example.ui.screens.habayeb.components.history.BalanceSummary
import com.example.ui.screens.habayeb.components.history.TransactionTable
import com.example.ui.screens.habayeb.components.history.DeleteCustomerConfirmDialog
import com.example.ui.screens.habayeb.components.history.EditCustomerDialog
import com.example.ui.screens.habayeb.components.history.DeleteBulkTransactionsConfirmDialog
import com.example.ui.screens.habayeb.components.history.FilterBottomSheet
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@Composable
fun CustomerHistoryOverlay(
    customer: HabayebCustomer,
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onAddTransaction: (HabayebCustomer, String) -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color,
    currencySymbol: String,
    contentPadding: PaddingValues = PaddingValues()
) {
    val customers by viewModel.habayebCustomersState.collectAsStateWithLifecycle()
    val activeCustomer = customers.find { it.id == customer.id } ?: customer
    val transactions by viewModel.habayebTransactionsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isPdfExporting by remember { mutableStateOf(false) }
    var showRateModifyDialog by remember { mutableStateOf(false) }
    var exchangeTxToModify by remember { mutableStateOf<HabayebTransaction?>(null) }
    var showRateSetupOverlay by remember { mutableStateOf(false) }
    var setupOverlayCurrency by remember { mutableStateOf("") }
    var setupOverlayInitialRate by remember { mutableStateOf("") }

    // Search and filter states
    var txSearchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var dateFilterMode by remember { mutableStateOf(0) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    var typeFilterMode by remember { mutableStateOf(0) }
    var selectedCurrencyFilter by remember { mutableStateOf<String?>(null) }

    // Date picker utility helper
    val showDatePicker = { initialTime: Long?, onDateSelected: (Long) -> Unit ->
        val calendar = java.util.Calendar.getInstance().apply { initialTime?.let { timeInMillis = it } }
        android.app.DatePickerDialog(context, { _, y, m, d ->
            onDateSelected(java.util.Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }.timeInMillis)
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }

    // Filter transactions and sequential running balance via UseCases with derivedStateOf optimization
    val allCustomerTxs = remember(transactions, activeCustomer) {
        transactions.filter { it.customerId == activeCustomer.id }.sortedBy { it.timestamp }
    }

    val filterUseCase = remember { FilterCustomerHistoryUseCase() }
    val searchDebtStr = stringResource(R.string.customer_history_search_debt)
    val searchPaymentStr = stringResource(R.string.customer_history_search_payment)
    val displayedTxs by remember(allCustomerTxs, txSearchQuery, dateFilterMode, customStartDate, customEndDate, typeFilterMode, selectedCurrencyFilter) {
        derivedStateOf {
            filterUseCase.execute(allCustomerTxs, txSearchQuery, dateFilterMode, customStartDate, customEndDate, typeFilterMode, selectedCurrencyFilter, currencySymbol, searchDebtStr, searchPaymentStr)
        }
    }

    val calculateRunningBalanceUseCase = remember { CalculateRunningBalanceUseCase() }
    val runningBalances by remember(allCustomerTxs, currencySymbol) {
        derivedStateOf { calculateRunningBalanceUseCase.execute(allCustomerTxs, currencySymbol) }
    }

    val currencyKeys = remember(allCustomerTxs, currencySymbol) {
        (listOf(currencySymbol) + allCustomerTxs.map { com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(it, currencySymbol).first }).distinct()
    }

    val netDebtMap = remember(allCustomerTxs, currencyKeys, currencySymbol) {
        currencyKeys.associateWith { curr ->
            val currencyTxs = allCustomerTxs.filter { tx ->
                val (txCurrency, _) = com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(tx, currencySymbol)
                txCurrency == curr
            }
            val owedByThem = currencyTxs.filter { it.type == "OWED_BY_THEM" }.sumOf { com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(it, currencySymbol).second }
            val paymentByThem = currencyTxs.filter { it.type == "PAYMENT_BY_THEM" }.sumOf { com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(it, currencySymbol).second }
            val owedToThem = currencyTxs.filter { it.type == "OWED_TO_THEM" }.sumOf { com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(it, currencySymbol).second }
            val paymentToThem = currencyTxs.filter { it.type == "PAYMENT_TO_THEM" }.sumOf { com.example.ui.screens.habayeb.utils.CurrencyConfig.getTransactionCurrencyAndAmount(it, currencySymbol).second }
            owedByThem - paymentByThem - owedToThem + paymentToThem
        }
    }

    val primaryDisplayCurrency = if (currencyKeys.contains(currencySymbol)) currencySymbol else currencyKeys.firstOrNull() ?: currencySymbol
    val netDebt = netDebtMap[primaryDisplayCurrency] ?: 0.0

    val txSequenceNumbers = remember(allCustomerTxs) {
        allCustomerTxs.sortedBy { it.timestamp }.mapIndexed { idx, tx -> tx.id to (idx + 1) }.toMap()
    }

    val listState = rememberLazyListState()
    var isHistoryListInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(allCustomerTxs.size) {
        if (isHistoryListInitialized) {
            if (displayedTxs.isNotEmpty()) try { listState.animateScrollToItem(displayedTxs.size - 1) } catch (e: Exception) {}
        } else { isHistoryListInitialized = true }
    }

    // Dialogs & Back handler state
    var editingTransactionForDialog by remember { mutableStateOf<HabayebTransaction?>(null) }
    var showAddTransactionDialogFromHistory by remember { mutableStateOf<HabayebCustomer?>(null) }
    var defaultTransactionTypeFromHistory by remember { mutableStateOf("OWED_BY_THEM") }
    var transactionForOptionsDialog by remember { mutableStateOf<HabayebTransaction?>(null) }
    var transactionForAutoRepeatDialog by remember { mutableStateOf<HabayebTransaction?>(null) }
    var isTxMultiSelectActive by remember { mutableStateOf(false) }
    val selectedTxIds = remember { mutableStateListOf<String>() }
    var showDeleteBulkTxConfirmDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (isTxMultiSelectActive) { isTxMultiSelectActive = false; selectedTxIds.clear() }
        else if (isSearchActive) { isSearchActive = false; txSearchQuery = "" }
        else onDismiss()
    }

    var refreshRecurringTrigger by remember { mutableStateOf(0) }
    val activeRecurringTxIds = remember(activeCustomer.id, refreshRecurringTrigger, allCustomerTxs) {
        HabayebRecurringManager.getAllConfigs(context).filter { it.isActive && it.customerId == activeCustomer.id }.map { it.originalTxId }.toSet()
    }

    LaunchedEffect(activeCustomer.id) {
        HabayebRecurringManager.checkAndExecuteRecurring(context, viewModel) { count ->
            Toast.makeText(context, context.getString(R.string.customer_history_toast_recurring_added, count, activeCustomer.name), Toast.LENGTH_LONG).show()
        }
    }

    var confirmDeleteCust by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedNameStr by remember(activeCustomer.name) { mutableStateOf(activeCustomer.name) }
    var editedPhoneStr by remember(activeCustomer.phone) { mutableStateOf(activeCustomer.phone) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.fillMaxSize()) {
                HistoryHeader(
                    customerName = activeCustomer.name, customerPhone = activeCustomer.phone, isSearchActive = isSearchActive,
                    txSearchQuery = txSearchQuery, activeThemeColor = activeThemeColor, isPdfExporting = isPdfExporting,
                    isPhoneAvailable = activeCustomer.phone.isNotBlank(), onSearchQueryChange = { txSearchQuery = it },
                    onSearchClose = { isSearchActive = false; txSearchQuery = "" }, onSearchOpen = { isSearchActive = true },
                    onPdfExportClick = {
                        isPdfExporting = true
                        PdfReportGenerator.generateAndHandleCustomerPdfReportAsync(context, coroutineScope, activeCustomer, netDebt, allCustomerTxs, "SHARE", onFinished = { isPdfExporting = false })
                    },
                    onCsvExportClick = { com.example.data.serialization.CsvReportGenerator.generateAndShareCsvReport(context, coroutineScope, activeCustomer, allCustomerTxs, currencySymbol) },
                    onWhatsAppClick = { CommunicationHelper.sendWhatsAppStatement(context, activeCustomer, netDebt, currencySymbol) },
                    onDeleteClick = { confirmDeleteCust = true }, onEditClick = { showEditNameDialog = true },
                    onFilterClick = { showFilterMenu = true }, onDismiss = onDismiss
                )

                if (!isSearchActive) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    BalanceSummary(activeCustomer = activeCustomer, currencySymbol = currencySymbol, netDebtMap = netDebtMap, selectedCurrencyFilter = selectedCurrencyFilter, onCurrencyFilterSelected = { selectedCurrencyFilter = it })
                }

                TransactionTable(
                    displayedTxs = displayedTxs, txSearchQuery = txSearchQuery, listState = listState, contentPadding = contentPadding,
                    runningBalances = runningBalances, activeRecurringTxIds = activeRecurringTxIds, txSequenceNumbers = txSequenceNumbers,
                    selectedTxIds = selectedTxIds, isTxMultiSelectActive = isTxMultiSelectActive, onMultiSelectActiveChange = { isTxMultiSelectActive = it },
                    activeThemeColor = activeThemeColor, currencySymbol = currencySymbol, initialType = activeCustomer.initialType,
                    onDeleteBulkClick = { if (selectedTxIds.isNotEmpty()) showDeleteBulkTxConfirmDialog = true },
                    onOptionsClick = { transactionForOptionsDialog = it }, onScheduleClick = { transactionForAutoRepeatDialog = it },
                    onExchangeRateClick = { exchangeTxToModify = it; showRateModifyDialog = true }, modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = !isTxMultiSelectActive, enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = contentPadding.calculateBottomPadding() + 16.dp, start = 20.dp)
            ) {
                FloatingActionButton(
                    onClick = { onAddTransaction(activeCustomer, if (netDebt >= 0.0) "OWED_BY_THEM" else "OWED_TO_THEM") },
                    containerColor = activeThemeColor, contentColor = Color.White, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(id = R.string.habayeb_add_tx_desc), modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    // Modal bottom filters and confirmation dialogs delegation
    FilterBottomSheet(
        showFilterMenu = showFilterMenu, onDismissRequest = { showFilterMenu = false }, dateFilterMode = dateFilterMode,
        onDateFilterModeChange = { dateFilterMode = it }, customStartDate = customStartDate, onCustomStartDateChange = { customStartDate = it },
        customEndDate = customEndDate, onCustomEndDateChange = { customEndDate = it }, typeFilterMode = typeFilterMode,
        onTypeFilterModeChange = { typeFilterMode = it }, activeThemeColor = activeThemeColor, onShowDatePicker = showDatePicker
    )

    DeleteCustomerConfirmDialog(showDialog = confirmDeleteCust, customerName = activeCustomer.name, onDismiss = { confirmDeleteCust = false }, onConfirm = {
        viewModel.deleteHabayebCustomer(activeCustomer.id)
        Toast.makeText(context, context.getString(R.string.habayeb_toast_delete_success), Toast.LENGTH_SHORT).show()
        confirmDeleteCust = false
        onDismiss()
    })

    EditCustomerDialog(
        showDialog = showEditNameDialog, nameValue = editedNameStr, onNameChange = { editedNameStr = it },
        phoneValue = editedPhoneStr, onPhoneChange = { editedPhoneStr = it }, activeThemeColor = activeThemeColor,
        onDismiss = { showEditNameDialog = false }, onConfirm = {
            if (editedNameStr.isNotBlank()) {
                viewModel.updateHabayebCustomer(activeCustomer.copy(name = editedNameStr.trim(), phone = editedPhoneStr.trim()))
                Toast.makeText(context, context.getString(R.string.habayeb_toast_update_success), Toast.LENGTH_SHORT).show()
            }
            showEditNameDialog = false
        }
    )

    DeleteBulkTransactionsConfirmDialog(showDialog = showDeleteBulkTxConfirmDialog, selectedCount = selectedTxIds.size, onDismiss = { showDeleteBulkTxConfirmDialog = false }, onConfirm = {
        selectedTxIds.toList().forEach { txId ->
            viewModel.deleteHabayebTransaction(txId)
            HabayebRecurringManager.deleteConfigForTransaction(context, txId)
        }
        Toast.makeText(context, context.getString(R.string.habayeb_toast_delete_bulk_success), Toast.LENGTH_SHORT).show()
        selectedTxIds.clear()
        isTxMultiSelectActive = false
        refreshRecurringTrigger++
        showDeleteBulkTxConfirmDialog = false
    })

    // Popups for adding / repeating / options / exchange rate
    if (showAddTransactionDialogFromHistory != null) {
        AddTransactionPopup(
            customer = showAddTransactionDialogFromHistory!!, viewModel = viewModel, initialSelectedType = defaultTransactionTypeFromHistory,
            editingTransaction = editingTransactionForDialog, onDismiss = { showAddTransactionDialogFromHistory = null; editingTransactionForDialog = null },
            activeThemeColor = activeThemeColor, activeSubColor = activeSubColor
        )
    }

    if (transactionForOptionsDialog != null) {
        val optTx = transactionForOptionsDialog!!
        TransactionOptionsDialog(
            transaction = optTx, customerName = activeCustomer.name, onDismiss = { transactionForOptionsDialog = null },
            onEdit = {
                editingTransactionForDialog = transactionForOptionsDialog
                defaultTransactionTypeFromHistory = transactionForOptionsDialog!!.type
                showAddTransactionDialogFromHistory = activeCustomer
                transactionForOptionsDialog = null
            },
            onDelete = {
                coroutineScope.launch {
                    val success = viewModel.deleteHabayebTransactionSuspended(optTx.id)
                    if (success) {
                        VibrationHelper.vibrateSuccess(context)
                        HabayebRecurringManager.deleteConfigForTransaction(context, optTx.id)
                        Toast.makeText(context, context.getString(R.string.habayeb_toast_delete_tx_success), Toast.LENGTH_SHORT).show()
                        refreshRecurringTrigger++
                    } else { Toast.makeText(context, context.getString(R.string.toast_delete_failed), Toast.LENGTH_SHORT).show() }
                }
                transactionForOptionsDialog = null
            },
            onAutoRepeat = { transactionForAutoRepeatDialog = transactionForOptionsDialog; transactionForOptionsDialog = null },
            activeThemeColor = activeThemeColor, activeSubColor = activeSubColor, isRecurringOriginal = optTx.id in activeRecurringTxIds,
            onDeleteAutoRepeat = {
                HabayebRecurringManager.deleteConfigForTransaction(context, optTx.id)
                Toast.makeText(context, context.getString(R.string.habayeb_toast_stop_recurring_success), Toast.LENGTH_SHORT).show()
                refreshRecurringTrigger++
                transactionForOptionsDialog = null
            },
            parentSeqNumber = if (optTx.linkedMainTxId != null) txSequenceNumbers[optTx.linkedMainTxId] else null
        )
    }

    if (transactionForAutoRepeatDialog != null) {
        RecurringTransactionPopup(transaction = transactionForAutoRepeatDialog!!, customerName = activeCustomer.name, onDismiss = { transactionForAutoRepeatDialog = null; refreshRecurringTrigger++ }, activeThemeColor = activeThemeColor, activeSubColor = activeSubColor)
    }

    if (showRateModifyDialog && exchangeTxToModify != null) {
        val tx = exchangeTxToModify!!
        androidx.compose.ui.window.Dialog(onDismissRequest = { showRateModifyDialog = false; showRateSetupOverlay = false }) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.width(260.dp).padding(8.dp)) {
                    androidx.compose.animation.Crossfade(targetState = showRateSetupOverlay, label = "RateModifyTransition") { isSetup ->
                        if (isSetup) {
                            androidx.activity.compose.BackHandler { showRateSetupOverlay = false }
                            ExchangeRateSetupContent(
                                currencySymbol = currencySymbol, selectedCurrency = setupOverlayCurrency, initialRateStr = setupOverlayInitialRate, activeThemeColor = activeThemeColor,
                                onDismiss = { showRateSetupOverlay = false },
                                onConfirm = { newRate ->
                                    val settings = viewModel.settingsState.value
                                    viewModel.saveSettings(settings.copy(exchangeRatesJson = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.setRate(settings.exchangeRatesJson, currencySymbol, setupOverlayCurrency, newRate)))
                                    viewModel.updateTransactionExchangeRate(tx.id, newRate, true)
                                    showRateSetupOverlay = false; showRateModifyDialog = false; exchangeTxToModify = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        val settings = viewModel.settingsState.value
                                        if (com.example.ui.screens.habayeb.utils.ExchangeRateHelper.hasRate(settings.exchangeRatesJson, currencySymbol, tx.currency_code)) {
                                            viewModel.updateTransactionExchangeRate(tx.id, com.example.ui.screens.habayeb.utils.ExchangeRateHelper.getRate(settings.exchangeRatesJson, currencySymbol, tx.currency_code), true)
                                            showRateModifyDialog = false; exchangeTxToModify = null
                                        } else { setupOverlayCurrency = tx.currency_code; setupOverlayInitialRate = ""; showRateSetupOverlay = true }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1.2f).height(38.dp), contentPadding = PaddingValues(0.dp)
                                ) { Text(stringResource(id = R.string.habayeb_activate_exchange), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                                Button(onClick = { viewModel.updateTransactionExchangeRate(tx.id, tx.exchange_rate, false); showRateModifyDialog = false; exchangeTxToModify = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f).height(38.dp), contentPadding = PaddingValues(0.dp)) { Text(stringResource(id = R.string.habayeb_deactivate_exchange), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                            }
                        }
                    }
                }
            }
        }
    }
}
