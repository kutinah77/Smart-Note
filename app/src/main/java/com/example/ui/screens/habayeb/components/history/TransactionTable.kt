package com.example.ui.screens.habayeb.components.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.HabayebTransaction
import com.example.ui.screens.habayeb.components.CustomerTransactionRow
import com.example.ui.theme.LocalIsDark

@Composable
fun TransactionTable(
    displayedTxs: List<HabayebTransaction>,
    txSearchQuery: String,
    listState: LazyListState,
    contentPadding: PaddingValues,
    runningBalances: Map<String, Double>,
    activeRecurringTxIds: Set<String>,
    txSequenceNumbers: Map<String, Int>,
    selectedTxIds: SnapshotStateList<String>,
    isTxMultiSelectActive: Boolean,
    onMultiSelectActiveChange: (Boolean) -> Unit,
    activeThemeColor: Color,
    currencySymbol: String,
    initialType: String,
    onDeleteBulkClick: () -> Unit,
    onOptionsClick: (HabayebTransaction) -> Unit,
    onScheduleClick: (HabayebTransaction) -> Unit,
    onExchangeRateClick: (HabayebTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDark.current
    val haptic = LocalHapticFeedback.current

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Table Header Row
            Surface(
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.habayeb_col_date),
                        modifier = Modifier.weight(1.2f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.habayeb_col_details),
                        modifier = Modifier.weight(1.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.habayeb_col_amount),
                        modifier = Modifier.weight(1.2f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (displayedTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (txSearchQuery.isEmpty()) stringResource(id = R.string.habayeb_no_tx_recorded) else stringResource(id = R.string.habayeb_no_search_results),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Top,
                    contentPadding = PaddingValues(
                        top = 2.dp,
                        bottom = contentPadding.calculateBottomPadding() + 80.dp
                    )
                ) {
                    items(displayedTxs, key = { it.id }) { tx ->
                        val isSelected = selectedTxIds.contains(tx.id)
                        val currentHistBalance = runningBalances[tx.id] ?: 0.0
                        val hasActiveRecurring = tx.id in activeRecurringTxIds
                        val txSeqNo = txSequenceNumbers[tx.id] ?: 0
                        val parentTxSeq = remember(tx.linkedMainTxId, txSequenceNumbers) {
                            if (tx.linkedMainTxId != null) {
                                txSequenceNumbers[tx.linkedMainTxId]
                            } else null
                        }

                        CustomerTransactionRow(
                            tx = tx,
                            currencySymbol = currencySymbol,
                            initialType = initialType,
                            isSelected = isSelected,
                            isTxMultiSelectActive = isTxMultiSelectActive,
                            hasActiveRecurring = hasActiveRecurring,
                            txSeqNo = txSeqNo,
                            parentTxSeq = parentTxSeq,
                            currentHistBalance = currentHistBalance,
                            activeThemeColor = activeThemeColor,
                            onSelectToggle = {
                                if (isSelected) selectedTxIds.remove(tx.id)
                                else selectedTxIds.add(tx.id)
                                if (selectedTxIds.isEmpty()) {
                                    onMultiSelectActiveChange(false)
                                }
                            },
                            onLongClick = {
                                if (!isTxMultiSelectActive) {
                                    onMultiSelectActiveChange(true)
                                    selectedTxIds.add(tx.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            onOptionsClick = { onOptionsClick(tx) },
                            onScheduleClick = { onScheduleClick(tx) },
                            onExchangeRateClick = { onExchangeRateClick(tx) }
                        )
                    }
                }
            }
        }

        // Multi-Select Floating Bar
        AnimatedVisibility(
            visible = isTxMultiSelectActive,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .shadow(16.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(alpha = 0.1f))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(30.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Cancel Button
                IconButton(
                    onClick = {
                        onMultiSelectActiveChange(false)
                        selectedTxIds.clear()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.habayeb_cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Selection Info & Select All
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            val allSelected = displayedTxs.isNotEmpty() && displayedTxs.all { selectedTxIds.contains(it.id) }
                            if (allSelected) {
                                selectedTxIds.clear()
                            } else {
                                displayedTxs.forEach { if (!selectedTxIds.contains(it.id)) selectedTxIds.add(it.id) }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val allSelected = displayedTxs.isNotEmpty() && displayedTxs.all { selectedTxIds.contains(it.id) }
                    Icon(
                        imageVector = if (allSelected) Icons.Default.Check else Icons.Default.List,
                        contentDescription = stringResource(id = R.string.habayeb_all_selected),
                        tint = if (allSelected) activeThemeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (allSelected) stringResource(id = R.string.habayeb_all_selected) else stringResource(id = R.string.habayeb_items_selected, selectedTxIds.size),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete Button
                IconButton(
                    onClick = {
                        if (selectedTxIds.isNotEmpty()) {
                            onDeleteBulkClick()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (isDark) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.habayeb_delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
