package com.example.ui.screens.habayeb.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.HabayebCustomer
import com.example.ui.helper.formatCurrency
import java.math.BigDecimal

@Composable
fun AutoSizeText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    var fontSizeState by remember(text, fontSize) { mutableStateOf(fontSize) }
    var readyToDraw by remember(text, fontSize) { mutableStateOf(false) }

    Text(
        text = text,
        style = TextStyle(fontSize = fontSizeState, fontWeight = fontWeight, color = color),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        softWrap = false,
        modifier = modifier.drawWithContent {
            if (readyToDraw) {
                drawContent()
            }
        },
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                val currentSize = fontSizeState.value
                if (currentSize > 8f) {
                    fontSizeState = (currentSize - 0.5f).sp
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
fun BalanceCompactChip(
    amount: Double,
    currencyCode: String,
    initialType: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isZero = try {
        BigDecimal.valueOf(amount).compareTo(BigDecimal.ZERO) == 0
    } catch (e: Exception) {
        amount == 0.0
    }

    val (chipColor, stateLabel) = when {
        isZero -> Pair(Color(0xFF757575), stringResource(id = R.string.status_account_cleared))
        initialType == "OWED_BY_THEM" -> { // Customer
            if (amount > 0.0) Pair(Color(0xFFEF4444), stringResource(id = R.string.status_remaining_on_him))
            else Pair(Color(0xFF10B981), stringResource(id = R.string.status_remaining_for_him) + " (+)")
        }
        initialType == "OWED_TO_THEM" -> { // Supplier
            if (amount < 0.0) Pair(Color(0xFF10B981), stringResource(id = R.string.status_remaining_for_him))
            else Pair(Color(0xFFEF4444), stringResource(id = R.string.status_remaining_with_him) + " (+)")
        }
        else -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, stringResource(id = R.string.status_account_cleared))
    }

    val formattedAmountStr = formatCurrency(kotlin.math.abs(amount), currencyCode)

    val bgColor = if (isSelected) chipColor.copy(alpha = 0.12f) else chipColor.copy(alpha = 0.04f)
    val borderColor = if (isSelected) chipColor else chipColor.copy(alpha = 0.25f)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stateLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isZero) chipColor else chipColor.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        AutoSizeText(
            text = formattedAmountStr,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = chipColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CustomerSummaryCard(
    activeCustomer: HabayebCustomer,
    currencySymbol: String,
    netDebtMap: Map<String, Double>,
    selectedCurrencyFilter: String? = null,
    onCurrencyFilterSelected: (String?) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val foreignCurrencies = netDebtMap.keys.filter { it != currencySymbol && (netDebtMap[it] ?: 0.0) != 0.0 }.sorted()
        val allCurrencies = listOf(currencySymbol) + foreignCurrencies

        if (allCurrencies.size <= 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (curr in allCurrencies) {
                    val netDebtVal = netDebtMap[curr] ?: 0.0
                    BalanceCompactChip(
                        amount = netDebtVal,
                        currencyCode = curr,
                        initialType = activeCustomer.initialType,
                        isSelected = selectedCurrencyFilter == curr,
                        onSelect = {
                            if (selectedCurrencyFilter == curr) {
                                onCurrencyFilterSelected(null)
                            } else {
                                onCurrencyFilterSelected(curr)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (curr in allCurrencies) {
                    val netDebtVal = netDebtMap[curr] ?: 0.0
                    BalanceCompactChip(
                        amount = netDebtVal,
                        currencyCode = curr,
                        initialType = activeCustomer.initialType,
                        isSelected = selectedCurrencyFilter == curr,
                        onSelect = {
                            if (selectedCurrencyFilter == curr) {
                                onCurrencyFilterSelected(null)
                            } else {
                                onCurrencyFilterSelected(curr)
                            }
                        },
                        modifier = Modifier.widthIn(min = 100.dp)
                    )
                }
            }
        }
    }
}
