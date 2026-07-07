package com.example.ui.screens.habayeb.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.components.addtransaction.ActionButtonsRow
import com.example.ui.screens.habayeb.components.addtransaction.AddTransactionDialogCoordinator
import com.example.ui.screens.habayeb.components.addtransaction.CurrencySelectionGrid
import com.example.ui.screens.habayeb.components.addtransaction.TransactionAmountField
import com.example.ui.screens.habayeb.components.addtransaction.TransactionDetailsSection
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@Composable
fun AddTransactionPopup(
    customer: HabayebCustomer,
    viewModel: FinanceViewModel,
    initialSelectedType: String = "OWED_BY_THEM",
    editingTransaction: HabayebTransaction? = null,
    onDismiss: () -> Unit,
    onTransactionAdded: (String) -> Unit = {},
    activeThemeColor: Color,
    activeSubColor: Color
) {
    val context = LocalContext.current
    val isDark = com.example.ui.theme.LocalIsDark.current

    val customersUiState by viewModel.customersUiState.collectAsStateWithLifecycle()
    val customerState = customersUiState.customers.find { it.id == customer.id }
    val netDebt = customerState?.netDebt ?: 0.0
    val isOwedByThem = netDebt >= 0.0

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val currencySymbol = settings.currencySymbol

    val initialCurrencyAndDesc = remember(editingTransaction) {
        if (editingTransaction != null) {
            com.example.ui.screens.habayeb.utils.CurrencyConfig.parseTransactionCurrency(
                editingTransaction.description,
                currencySymbol
            )
        } else {
            Pair(currencySymbol, "")
        }
    }

    var selectedTransactionCurrency by rememberSaveable {
        mutableStateOf(editingTransaction?.currency_code?.let { if (it == "DEFAULT") currencySymbol else it } ?: initialCurrencyAndDesc.first)
    }

    val isForeignSelected = selectedTransactionCurrency != currencySymbol
    var applyExchangeRate by rememberSaveable { mutableStateOf(editingTransaction?.is_rate_calculated ?: false) }
    
    val currentRateVal = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.getRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
    val settingsRate = if (currentRateVal <= 0.0) 1.0 else currentRateVal

    var amountStr by rememberSaveable {
        mutableStateOf(
            editingTransaction?.let {
                if (it.is_foreign) {
                    if (it.foreign_amount % 1.0 == 0.0) it.foreign_amount.toInt().toString() else it.foreign_amount.toString()
                } else {
                    if (it.amount % 1.0 == 0.0) it.amount.toInt().toString() else it.amount.toString()
                }
            } ?: ""
        )
    }
    var descStr by rememberSaveable { mutableStateOf(if (editingTransaction != null) initialCurrencyAndDesc.second else "") }
    
    val amountFocusRequester = remember { FocusRequester() }
    val descFocusRequester = remember { FocusRequester() }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        try {
            amountFocusRequester.requestFocus()
            softwareKeyboardController?.show()
        } catch(e: Exception) {}
    }

    val isLendOperationSelected = customer.initialType == "OWED_BY_THEM"
    var dateMillis by rememberSaveable { mutableStateOf(editingTransaction?.timestamp?.let { it * 1000 } ?: System.currentTimeMillis()) }
    var showCalculator by rememberSaveable { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showRateSetupOverlay by rememberSaveable { mutableStateOf(false) }
    var tempRateStr by rememberSaveable { mutableStateOf("") }

    val dynamicThemeColor = if (isLendOperationSelected) Color(0xFFEF4444) else Color(0xFF10B981)

    val handleActionClick = { type: String ->
        focusManager.clearFocus()
        softwareKeyboardController?.hide()
        
        if (!isSaving) {
            val hasStoredRate = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.hasRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
            if (isForeignSelected && applyExchangeRate && !hasStoredRate) {
                tempRateStr = ""
                showRateSetupOverlay = true
            } else {
                isSaving = true
                coroutineScope.launch {
                    val result = viewModel.processHabayebTransactionSuspended(
                        customerId = customer.id,
                        type = type,
                        amountStr = amountStr,
                        descStr = descStr,
                        dateMillis = dateMillis,
                        editingTransaction = editingTransaction,
                        selectedTransactionCurrency = selectedTransactionCurrency,
                        currencySymbol = currencySymbol,
                        applyExchangeRate = applyExchangeRate,
                        settingsRate = settingsRate
                    )
                    when (result) {
                        is com.example.domain.usecase.ProcessHabayebTransactionUseCase.ProcessResult.Success -> {
                            com.example.ui.helper.VibrationHelper.vibrateSuccess(context)
                            Toast.makeText(context, context.getString(R.string.habayeb_toast_tx_save_success), Toast.LENGTH_SHORT).show()
                            onTransactionAdded(customer.id)
                            onDismiss()
                        }
                        is com.example.domain.usecase.ProcessHabayebTransactionUseCase.ProcessResult.ValidationError -> {
                            Toast.makeText(context, context.getString(result.messageResId), Toast.LENGTH_SHORT).show()
                            isSaving = false
                        }
                        is com.example.domain.usecase.ProcessHabayebTransactionUseCase.ProcessResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            isSaving = false
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Crossfade(targetState = showRateSetupOverlay, label = "FormTransition") { isSetup ->
                    if (isSetup) {
                        BackHandler {
                            showRateSetupOverlay = false
                            applyExchangeRate = false
                        }
                        ExchangeRateSetupContent(
                            currencySymbol = currencySymbol,
                            selectedCurrency = selectedTransactionCurrency,
                            initialRateStr = tempRateStr,
                            activeThemeColor = activeThemeColor,
                            onDismiss = {
                                showRateSetupOverlay = false
                                applyExchangeRate = false
                            },
                            onConfirm = { newRate ->
                                val newSettings = settings.copy(
                                    exchangeRatesJson = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.setRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency, newRate)
                                )
                                viewModel.saveSettings(newSettings)
                                applyExchangeRate = true
                                showRateSetupOverlay = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Header
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).padding(bottom = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (editingTransaction != null) stringResource(id = R.string.add_transaction_title_edit) else stringResource(id = R.string.add_transaction_title_new),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = activeThemeColor
                                    )
                                    Text(
                                        text = stringResource(id = R.string.add_transaction_account_label, "${customer.name.take(15)}${if (customer.name.length > 15) ".." else ""}"),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .size(24.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(id = R.string.habayeb_go_back),
                                        tint = activeThemeColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Amount Field
                            TransactionAmountField(
                                amountStr = amountStr,
                                onAmountChange = { amountStr = it },
                                amountFocusRequester = amountFocusRequester,
                                descFocusRequester = descFocusRequester,
                                dynamicThemeColor = dynamicThemeColor,
                                selectedTransactionCurrency = selectedTransactionCurrency,
                                onShowCalculator = { showCalculator = true }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Description and Date section
                            TransactionDetailsSection(
                                descStr = descStr,
                                onDescChange = { descStr = it },
                                dateMillis = dateMillis,
                                onDateChange = { dateMillis = it },
                                descFocusRequester = descFocusRequester,
                                dynamicThemeColor = dynamicThemeColor
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            Spacer(modifier = Modifier.height(4.dp))

                            // Currency Selection
                            CurrencySelectionGrid(
                                selectedTransactionCurrency = selectedTransactionCurrency,
                                onCurrencySelected = {
                                    selectedTransactionCurrency = it
                                    applyExchangeRate = false
                                },
                                currencySymbol = currencySymbol
                            )

                            // Exchange Rate toggle if foreign currency
                            if (isForeignSelected) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val hasStoredRate = com.example.ui.screens.habayeb.utils.ExchangeRateHelper.hasRate(settings.exchangeRatesJson, currencySymbol, selectedTransactionCurrency)
                                            if (!applyExchangeRate) {
                                                if (hasStoredRate) {
                                                    applyExchangeRate = true
                                                } else {
                                                    tempRateStr = ""
                                                    showRateSetupOverlay = true
                                                }
                                            } else {
                                                applyExchangeRate = false
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .border(1.dp, activeThemeColor, RoundedCornerShape(4.dp))
                                            .background(if (applyExchangeRate) activeThemeColor else Color.Transparent, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (applyExchangeRate) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(id = R.string.add_transaction_exchange_rate_prompt),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Buttons
                            ActionButtonsRow(
                                isSaving = isSaving,
                                isLendOperationSelected = isLendOperationSelected,
                                onDebtClick = {
                                    handleActionClick(if (isLendOperationSelected) "OWED_BY_THEM" else "OWED_TO_THEM")
                                },
                                onPayClick = {
                                    handleActionClick(if (isLendOperationSelected) "PAYMENT_BY_THEM" else "PAYMENT_TO_THEM")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog Coordinator for Calculator
    AddTransactionDialogCoordinator(
        showCalculator = showCalculator,
        onDismissCalculator = { showCalculator = false },
        onValueConfirmed = { value ->
            amountStr = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
            showCalculator = false
        },
        activeThemeColor = activeThemeColor,
        activeSubColor = activeSubColor
    )
}
