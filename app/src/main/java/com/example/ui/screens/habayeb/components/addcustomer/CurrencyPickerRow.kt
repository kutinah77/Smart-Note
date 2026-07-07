package com.example.ui.screens.habayeb.components.addcustomer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.domain.service.CurrencyMigrationService

@Composable
fun CurrencyPickerRow(
    selectedTransactionCurrency: String,
    onCurrencyChange: (String) -> Unit,
    currencySymbol: String,
    settings: AppSettings,
    applyExchangeRate: Boolean,
    onApplyExchangeRateChange: (Boolean) -> Unit,
    onShowRateSetupOverlay: () -> Unit,
    activeThemeColor: Color,
    currencyMigrationService: CurrencyMigrationService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val currencySar = context.getString(R.string.currency_sar)
    val currencyUsd = context.getString(R.string.currency_usd)
    val currencyYer = context.getString(R.string.currency_yer)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Currency selection buttons
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val famousCurrencies = listOf(
                Pair(currencyYer, context.getString(R.string.currency_label_yer)),
                Pair(currencyUsd, context.getString(R.string.currency_label_usd)),
                Pair(currencySar, context.getString(R.string.currency_label_sar))
            )
            famousCurrencies.forEachIndexed { index, (sym, label) ->
                val isSelected = selectedTransactionCurrency == sym
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (selectedTransactionCurrency != sym) {
                                onCurrencyChange(sym)
                                onApplyExchangeRateChange(false)
                            }
                            if (sym == currencySymbol) {
                                onApplyExchangeRateChange(false)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color(0xFFE91E63) else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSelected) Color(0xFFE91E63) else Color.Gray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE91E63))
                            )
                        }
                    }
                }
                if (index < famousCurrencies.size - 1) {
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
        }

        // Optional currency exchange rate option
        if (selectedTransactionCurrency != currencySymbol) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val hasStoredRate = currencyMigrationService.hasRate(
                            settings.exchangeRatesJson,
                            currencySymbol,
                            selectedTransactionCurrency
                        )
                        if (!applyExchangeRate) {
                            if (hasStoredRate) {
                                onApplyExchangeRateChange(true)
                            } else {
                                onShowRateSetupOverlay()
                            }
                        } else {
                            onApplyExchangeRateChange(false)
                        }
                    }
                    .padding(vertical = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .border(1.5.dp, activeThemeColor, RoundedCornerShape(3.dp))
                        .background(if (applyExchangeRate) activeThemeColor else Color.Transparent, RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (applyExchangeRate) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.habayeb_add_with_rate_question),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.DarkGray
                )
            }
        }
    }
}
