package com.example.ui.screens.habayeb.components.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun CurrencySelectionGrid(
    selectedTransactionCurrency: String,
    onCurrencySelected: (String) -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val famousCurrencies = listOf(
            Pair(stringResource(R.string.currency_yer), stringResource(R.string.currency_label_yer)),
            Pair(stringResource(R.string.currency_usd), stringResource(R.string.currency_label_usd)),
            Pair(stringResource(R.string.currency_sar), stringResource(R.string.currency_label_sar))
        )
        famousCurrencies.forEachIndexed { index, (sym, label) ->
            val isSelected = selectedTransactionCurrency == sym
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (selectedTransactionCurrency != sym) {
                            onCurrencySelected(sym)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFFE91E63) else Color.DarkGray
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Custom Radio Button Circle
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (isSelected) Color(0xFFE91E63) else Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE91E63))
                        )
                    }
                }
            }
            if (index < famousCurrencies.size - 1) {
                Spacer(modifier = Modifier.width(12.dp))
            }
        }
    }
}
