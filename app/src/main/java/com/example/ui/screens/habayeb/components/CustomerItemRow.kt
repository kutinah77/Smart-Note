package com.example.ui.screens.habayeb.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.helper.AutoScaleText
import com.example.ui.helper.formatCurrency
import com.example.ui.helper.getInitialColor
import androidx.compose.material3.MaterialTheme


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomerItemRow(
    customer: com.example.ui.state.CustomerUiState,
    isSelected: Boolean,
    isMultiSelectActive: Boolean,
    activeThemeColor: Color,
    activeSubColor: Color,
    currencySymbol: String,
    isPrivacyMode: Boolean = false,
    haptic: HapticFeedback,
    onCustomerClick: () -> Unit,
    onCustomerLongClick: () -> Unit,
    onQuickAdd: () -> Unit
) {
    val lastTxTime = customer.lastTransactionTimestamp
    val onSurfaceTextColor = MaterialTheme.colorScheme.onBackground
    val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val sdf = remember { java.text.SimpleDateFormat("yyyy/MM/dd hh:mm a", java.util.Locale("ar")) }
    val formattedDate = remember(lastTxTime) {
        sdf.format(java.util.Date(lastTxTime * 1000))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        border = null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeSubColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 200))
            .combinedClickable(
                onClick = onCustomerClick,
                onLongClick = onCustomerLongClick
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val firstLetter = remember(customer.name) { customer.name.trim().firstOrNull()?.toString()?.uppercase() ?: "؟" }
                    val avatarColor = remember(customer.name) { getInitialColor(customer.name) }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onQuickAdd() },
                        contentAlignment = Alignment.Center
                    ) {
                        // Main Avatar circle in the center
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(avatarColor.copy(alpha = 0.12f))
                                .border(0.5.dp, avatarColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstLetter,
                                color = avatarColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Floating Badge in the bottom-end corner
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .background(activeThemeColor, CircleShape)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.habayeb_add_tx_button).replace(" ➕",""),
                                tint = Color.White,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(id = R.string.ledger_done_btn),
                                    tint = activeThemeColor,
                                    modifier = Modifier.size(14.dp).padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = customer.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.habayeb_last_modified, formattedDate),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondaryColor
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 6.dp)
                ) {
                    val netDebtVal = customer.displayNetDebt
                    val netDebtDecimal = com.example.ui.helper.HabayebMathHelper.toBigDecimal(netDebtVal)
                    val isZero = netDebtDecimal.compareTo(java.math.BigDecimal.ZERO) == 0
                    val isPositive = netDebtDecimal.compareTo(java.math.BigDecimal.ZERO) > 0
                    val isNegative = netDebtDecimal.compareTo(java.math.BigDecimal.ZERO) < 0
                    val itemCurrencySymbol = customer.displayCurrencySymbol
                    val initialType = customer.originalCustomer.initialType
                    
                    val (debtColor, statusText, isOwedByThem) = when (initialType) {
                        "OWED_BY_THEM" -> {
                            if (isPositive) Triple(Color(0xFFDC2626), stringResource(id = R.string.habayeb_owed), true)
                            else if (isNegative) Triple(Color(0xFF16A34A), stringResource(id = R.string.status_overpaid_to_customer), false)
                            else Triple(textSecondaryColor, stringResource(id = R.string.habayeb_balanced), null as Boolean?)
                        }
                        "OWED_TO_THEM" -> {
                            if (isNegative) Triple(Color(0xFF16A34A), stringResource(id = R.string.habayeb_to_them), false)
                            else if (isPositive) Triple(Color(0xFFDC2626), stringResource(id = R.string.status_overpaid_to_supplier), true)
                            else Triple(textSecondaryColor, stringResource(id = R.string.habayeb_balanced), null as Boolean?)
                        }
                        else -> Triple(textSecondaryColor, stringResource(id = R.string.habayeb_balanced), null as Boolean?)
                    }

                    if (!isZero) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            if (isOwedByThem != null) {
                                Text(
                                    text = if (isOwedByThem) "▼" else "▲",
                                    color = debtColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                            }
                            AutoScaleText(
                                text = formatCurrency(netDebtDecimal.abs().toDouble(), itemCurrencySymbol),
                                baseFontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = debtColor
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondaryColor
                        )
                    } else {
                        Text(
                            text = stringResource(id = R.string.habayeb_status_balanced_short),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondaryColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.habayeb_status_balanced),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondaryColor
                        )
                    }
                }
            }

        }
    }
}
