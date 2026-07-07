package com.example.ui.screens.habayeb.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.utils.CurrencyConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomerTransactionRow(
    tx: HabayebTransaction,
    currencySymbol: String,
    initialType: String,
    isSelected: Boolean,
    isTxMultiSelectActive: Boolean,
    hasActiveRecurring: Boolean,
    txSeqNo: Int,
    parentTxSeq: Int?,
    currentHistBalance: Double,
    activeThemeColor: Color,
    onSelectToggle: () -> Unit,
    onLongClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onExchangeRateClick: () -> Unit
) {
    val parsedCurrencyInfo = remember(tx.description) {
        CurrencyConfig.parseTransactionCurrency(tx.description, currencySymbol)
    }
    val txCurrencySymbol = if (tx.is_foreign) tx.currency_code else parsedCurrencyInfo.first
    val cleanDescription = parsedCurrencyInfo.second

    val formattedDate = remember(tx.timestamp) {
        val df = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
        val tf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val d = Date(tx.timestamp * 1000)
        df.format(d) + " | " + tf.format(d)
    }

    val isPositiveSign = tx.type == "PAYMENT_BY_THEM" || tx.type == "OWED_TO_THEM"
    val isDebt = tx.type == "OWED_BY_THEM" || tx.type == "OWED_TO_THEM"
    val isLeh = tx.type == "PAYMENT_BY_THEM" || tx.type == "OWED_TO_THEM"
    val indicatorColor = if (isLeh) Color(0xFF10B981) else Color(0xFFEF4444)
    val rowBgColor = if (isSelected) activeThemeColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) activeThemeColor else MaterialTheme.colorScheme.outlineVariant

    val formattedHistBal = try { String.format(Locale.ENGLISH, "%,.0f", kotlin.math.abs(currentHistBalance)) } catch (e: Exception) { kotlin.math.abs(currentHistBalance).toString() }
    val amountToFormat = if (tx.is_foreign) tx.foreign_amount else tx.amount
    val formattedAmount = try { String.format(Locale.ENGLISH, "%,.0f", amountToFormat) } catch (e: Exception) { amountToFormat.toString() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .combinedClickable(
                onClick = {
                    if (isTxMultiSelectActive) {
                        onSelectToggle()
                    } else {
                        onOptionsClick()
                    }
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = rowBgColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(indicatorColor, indicatorColor.copy(alpha = 0.6f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Date/Time (Rightmost)
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "#$txSeqNo",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = activeThemeColor,
                            modifier = Modifier
                                .background(activeThemeColor.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                        if (hasActiveRecurring) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(activeThemeColor.copy(alpha = 0.12f), CircleShape)
                                    .clickable { onScheduleClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = stringResource(id = R.string.habayeb_recurring_source),
                                    tint = activeThemeColor,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val d = remember(tx.timestamp) { Date(tx.timestamp * 1000) }
                    val dateStr = remember(d) { SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(d) }
                    val timeStr = remember(d) { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(d) }

                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    
                    Spacer(modifier = Modifier.height(1.dp))

                    Text(
                        text = timeStr,
                        fontSize = 8.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                // 2. Details (Middle-Right)
                Column(
                    modifier = Modifier.weight(1.8f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val typeStr = when (tx.type) {
                        "OWED_BY_THEM" -> stringResource(id = R.string.habayeb_pdf_tx_owed_by)
                        "PAYMENT_BY_THEM" -> stringResource(id = R.string.habayeb_pdf_tx_payment_by)
                        "OWED_TO_THEM" -> stringResource(id = R.string.habayeb_pdf_tx_owed_to)
                        "PAYMENT_TO_THEM" -> stringResource(id = R.string.habayeb_pdf_tx_payment_to)
                        else -> stringResource(id = R.string.habayeb_pdf_tx_generic)
                    }
                    Text(
                        text = typeStr,
                        fontSize = 9.sp,
                        color = indicatorColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    // Fixed max lines: 2 and proper ellipsize
                    Text(
                        text = cleanDescription.ifEmpty { stringResource(id = R.string.habayeb_no_notes) },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (hasActiveRecurring) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                .border(0.5.dp, Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.habayeb_recurring_source),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    } else if (tx.linkedMainTxId != null) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(6.dp))
                                .border(0.5.dp, Color(0xFF3B82F6), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.habayeb_auto_generated_sub, parentTxSeq ?: 0),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D4ED8)
                            )
                        }
                    }

                    if (tx.is_foreign) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (tx.is_rate_calculated) Color(0xFFE6F4EA) else Color(0xFFF1F3F4))
                                .border(0.5.dp, if (tx.is_rate_calculated) Color(0xFF137333) else Color(0xFF9AA0A6), RoundedCornerShape(6.dp))
                                .clickable { onExchangeRateClick() }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (tx.is_rate_calculated) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (tx.is_rate_calculated) Color(0xFF137333) else Color(0xFFD93025),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (tx.is_rate_calculated) stringResource(id = R.string.habayeb_rate_active, tx.exchange_rate.toString()) else stringResource(id = R.string.habayeb_rate_inactive),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.is_rate_calculated) Color(0xFF137333) else Color(0xFF5F6368)
                            )
                        }
                    }
                }

                // 3. Amount with colorful indicator arrow (Middle-Left)
                Column(
                    modifier = Modifier.weight(1.2f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val txArrow = if (isDebt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward
                        Icon(
                            imageVector = txArrow,
                            contentDescription = null,
                            tint = indicatorColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // AutoSizing amount text
                        AutoSizeText(
                            text = "$formattedAmount $txCurrencySymbol",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = indicatorColor
                        )
                    }
                    if (tx.is_foreign && tx.is_rate_calculated) {
                        val formattedEquiv = try { String.format(Locale.ENGLISH, "%,.0f", tx.equivalent_amount) } catch (e: Exception) { tx.equivalent_amount.toString() }
                        AutoSizeText(
                            text = "($formattedEquiv $currencySymbol)",
                            fontSize = 9.sp,
                            color = indicatorColor.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
