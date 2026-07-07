package com.example.ui.screens.habayeb.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.screens.CalculatorDialog
import com.example.ui.screens.habayeb.components.addcustomer.CustomerContactSection
import com.example.ui.screens.habayeb.components.addcustomer.CustomerPhoneSection
import com.example.ui.screens.habayeb.components.addcustomer.CurrencyPickerRow
import com.example.ui.screens.habayeb.components.addcustomer.InitialBalanceSection
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

data class AddCustomerUiState(
    val nameStr: String = "",
    val phoneStr: String = "",
    val notesStr: String = "",
    val initialAmountStr: String = "",
    val initialType: String = "OWED_BY_THEM",
    val selectedTransactionCurrency: String = "",
    val applyExchangeRate: Boolean = false,
    val showRateSetupOverlay: Boolean = false,
    val tempRateStr: String = "",
    val showCalculator: Boolean = false,
    val isSavingCustomer: Boolean = false,
    val selectedCalendar: Calendar = Calendar.getInstance()
)

@Composable
fun AddCustomerPopup(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    onCustomerAdded: (String) -> Unit = {},
    activeThemeColor: Color,
    activeSubColor: Color
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val currencySymbol = settings.currencySymbol

    var uiState by remember { mutableStateOf(AddCustomerUiState()) }

    LaunchedEffect(currencySymbol) {
        if (uiState.selectedTransactionCurrency.isEmpty()) {
            uiState = uiState.copy(selectedTransactionCurrency = currencySymbol)
        }
    }

    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val phoneFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val initialAmountFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val notesFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .widthIn(max = 350.dp)
                    .fillMaxWidth(0.94f)
                    .imePadding()
                    .padding(2.dp)
            ) {
                Crossfade(targetState = uiState.showRateSetupOverlay, label = "CustomerFormTransition") { isSetup ->
                    if (isSetup) {
                        BackHandler {
                            uiState = uiState.copy(showRateSetupOverlay = false, applyExchangeRate = false)
                        }
                        ExchangeRateSetupContent(
                            currencySymbol = currencySymbol,
                            selectedCurrency = uiState.selectedTransactionCurrency,
                            initialRateStr = uiState.tempRateStr,
                            activeThemeColor = activeThemeColor,
                            onDismiss = {
                                uiState = uiState.copy(showRateSetupOverlay = false, applyExchangeRate = false)
                            },
                            onConfirm = { newRate ->
                                val newSettings = settings.copy(
                                    exchangeRatesJson = viewModel.currencyMigrationService.setRate(
                                        settings.exchangeRatesJson,
                                        currencySymbol,
                                        uiState.selectedTransactionCurrency,
                                        newRate
                                    )
                                )
                                viewModel.saveSettings(newSettings)
                                uiState = uiState.copy(applyExchangeRate = true, showRateSetupOverlay = false)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.habayeb_cancel),
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.dialog_title_add_account),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = activeThemeColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.size(24.dp))
                            }

                            // 1. Customer Contact Section (Name only)
                            CustomerContactSection(
                                nameStr = uiState.nameStr,
                                onNameChange = { uiState = uiState.copy(nameStr = it) },
                                focusRequester = focusRequester,
                                initialAmountFocusRequester = initialAmountFocusRequester,
                                activeThemeColor = activeThemeColor,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 2. Initial Balance Section (Amount & Notes/Statement)
                            InitialBalanceSection(
                                initialAmountStr = uiState.initialAmountStr,
                                onInitialAmountChange = { uiState = uiState.copy(initialAmountStr = it) },
                                notesStr = uiState.notesStr,
                                onNotesChange = { uiState = uiState.copy(notesStr = it) },
                                selectedTransactionCurrency = uiState.selectedTransactionCurrency,
                                initialAmountFocusRequester = initialAmountFocusRequester,
                                notesFocusRequester = notesFocusRequester,
                                phoneFocusRequester = phoneFocusRequester,
                                activeThemeColor = activeThemeColor,
                                onShowCalculator = { uiState = uiState.copy(showCalculator = true) },
                                selectedCalendar = uiState.selectedCalendar,
                                onDateSelected = { uiState = uiState.copy(selectedCalendar = it) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 3. Customer Phone Section (Last Input Field)
                            CustomerPhoneSection(
                                phoneStr = uiState.phoneStr,
                                onPhoneChange = { uiState = uiState.copy(phoneStr = it) },
                                nameStr = uiState.nameStr,
                                onNameChange = { uiState = uiState.copy(nameStr = it) },
                                phoneFocusRequester = phoneFocusRequester,
                                activeThemeColor = activeThemeColor,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 3. Currency Picker Row
                            CurrencyPickerRow(
                                selectedTransactionCurrency = uiState.selectedTransactionCurrency,
                                onCurrencyChange = { uiState = uiState.copy(selectedTransactionCurrency = it) },
                                currencySymbol = currencySymbol,
                                settings = settings,
                                applyExchangeRate = uiState.applyExchangeRate,
                                onApplyExchangeRateChange = { uiState = uiState.copy(applyExchangeRate = it) },
                                onShowRateSetupOverlay = { uiState = uiState.copy(tempRateStr = "", showRateSetupOverlay = true) },
                                activeThemeColor = activeThemeColor,
                                currencyMigrationService = viewModel.currencyMigrationService,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Type selection row: OWED_BY_THEM / OWED_TO_THEM
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // "عليه"
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                uiState = uiState.copy(initialType = "OWED_BY_THEM")
                                            }
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.habayeb_owed),
                                            fontSize = 10.sp,
                                            fontWeight = if (uiState.initialType == "OWED_BY_THEM") FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.initialType == "OWED_BY_THEM") Color(0xFFDC2626) else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    1.5.dp,
                                                    if (uiState.initialType == "OWED_BY_THEM") Color(0xFFDC2626) else Color.LightGray,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (uiState.initialType == "OWED_BY_THEM") {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFDC2626))
                                                )
                                            }
                                        }
                                    }

                                    // "له"
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                uiState = uiState.copy(initialType = "OWED_TO_THEM")
                                            }
                                            .padding(horizontal = 2.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.habayeb_to_them),
                                            fontSize = 10.sp,
                                            fontWeight = if (uiState.initialType == "OWED_TO_THEM") FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.initialType == "OWED_TO_THEM") Color(0xFF10B981) else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    1.5.dp,
                                                    if (uiState.initialType == "OWED_TO_THEM") Color(0xFF10B981) else Color.LightGray,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (uiState.initialType == "OWED_TO_THEM") {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF10B981))
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Save Button
                                Button(
                                    enabled = !uiState.isSavingCustomer,
                                    onClick = {
                                        if (uiState.nameStr.trim().isBlank()) {
                                            Toast.makeText(context, context.getString(R.string.habayeb_toast_enter_name), Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val cleanAmountStr = CurrencyConfig.normalizeDigits(uiState.initialAmountStr).trim()
                                        val actualInitialAmount = cleanAmountStr.toDoubleOrNull() ?: 0.0

                                        if (actualInitialAmount < 0.0) {
                                            Toast.makeText(context, context.getString(R.string.habayeb_toast_initial_amount_negative), Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val isForeignSelected = uiState.selectedTransactionCurrency != currencySymbol
                                        if (isForeignSelected && uiState.applyExchangeRate) {
                                            val hasStoredRate = viewModel.currencyMigrationService.hasRate(
                                                settings.exchangeRatesJson,
                                                currencySymbol,
                                                uiState.selectedTransactionCurrency
                                            )
                                            if (!hasStoredRate) {
                                                uiState = uiState.copy(tempRateStr = "", showRateSetupOverlay = true)
                                                return@Button
                                            }
                                        }

                                        uiState = uiState.copy(isSavingCustomer = true)

                                        val settingsRate = viewModel.currencyMigrationService.getSettingsRate(
                                            uiState.selectedTransactionCurrency,
                                            settings,
                                            context.getString(R.string.currency_sar),
                                            context.getString(R.string.currency_usd),
                                            context.getString(R.string.currency_yer)
                                        )

                                        coroutineScope.launch {
                                            val (success, newCustomerId) = viewModel.createHabayebCustomerUseCase.execute(
                                                name = uiState.nameStr,
                                                phone = uiState.phoneStr,
                                                notes = uiState.notesStr,
                                                initialAmount = actualInitialAmount,
                                                initialType = uiState.initialType,
                                                selectedCurrency = uiState.selectedTransactionCurrency,
                                                currencySymbol = currencySymbol,
                                                applyExchangeRate = uiState.applyExchangeRate,
                                                settingsRate = settingsRate,
                                                customTimestamp = uiState.selectedCalendar.timeInMillis / 1000,
                                                defaultOpeningDetails = context.getString(R.string.habayeb_opening_balance_default_desc),
                                                formatDescriptionWithCurrency = { details, currency ->
                                                    CurrencyConfig.formatDescriptionWithCurrency(details, currency)
                                                }
                                            )
                                            if (success && newCustomerId != null) {
                                                com.example.ui.helper.VibrationHelper.vibrateSuccess(context)
                                                Toast.makeText(context, context.getString(R.string.habayeb_toast_save_success), Toast.LENGTH_SHORT).show()
                                                onCustomerAdded(newCustomerId)
                                                onDismiss()
                                            } else {
                                                uiState = uiState.copy(isSavingCustomer = false)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = activeThemeColor,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.btn_save),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showCalculator) {
        CalculatorDialog(
            onDismiss = { uiState = uiState.copy(showCalculator = false) },
            onValueConfirmed = { value ->
                val amountText = if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
                uiState = uiState.copy(initialAmountStr = amountText, showCalculator = false)
            },
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor
        )
    }
}
