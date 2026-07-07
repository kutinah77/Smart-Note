package com.example.ui.screens.ledger.sections

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.data.local.entities.FixedCommitment
import com.example.domain.usecase.MonthLedger
import com.example.domain.usecase.DayLedger
import com.example.ui.screens.ledger.components.*
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.FinanceViewModel
import java.math.BigDecimal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LedgerListSection(
    lazyListState: LazyListState,
    viewModel: FinanceViewModel,
    settings: AppSettings,
    collapseFraction: Float,
    isDaySelectionMode: Boolean,
    onDaySelectionModeChange: (Boolean) -> Unit,
    selectedDayKeys: List<String>,
    onSelectedDayKeysChange: (List<String>) -> Unit,
    onDeleteSelectedDays: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    monthlyLedger: List<MonthLedger>,
    commitments: List<FixedCommitment>,
    computedCommitments: List<Triple<FixedCommitment, Double, Double>>,
    linkHabayebDebts: Boolean,
    isScreenReady: Boolean,
    collapsedMonths: Set<String>,
    onCollapsedMonthsChange: (Set<String>) -> Unit,
    isSelectionMode: Boolean,
    haptic: HapticFeedback,
    bottomPadding: Dp,
    onActiveDayKeyChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCash by viewModel.totalCashState.collectAsStateWithLifecycle()

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = bottomPadding + 110.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Compact Header + Total Cash + Coverage Ratio
        item(key = "header_total_cash") {
            val isPrivacyMode by viewModel.isPrivacyModeEnabled.collectAsStateWithLifecycle()
            val allKeys = remember(monthlyLedger) {
                monthlyLedger.flatMap { ml -> ml.days.map { "${ml.monthKey}_${it.dayNumber}" } }
            }
            val selectedDayKeysCountText = when (selectedDayKeys.size) {
                1 -> stringResource(id = R.string.ledger_selected_days_count_1)
                2 -> stringResource(id = R.string.ledger_selected_days_count_2)
                else -> stringResource(id = R.string.ledger_selected_days_count_more, selectedDayKeys.size)
            }
            val isSelectAllChecked = selectedDayKeys.size == allKeys.size && allKeys.isNotEmpty()

            MainLedgerHeader(
                collapseFraction = collapseFraction,
                isDaySelectionMode = isDaySelectionMode,
                selectedDayKeys = selectedDayKeys,
                onCancelDaySelection = {
                    onDaySelectionModeChange(false)
                    onSelectedDayKeysChange(emptyList())
                },
                onSelectAllDays = {
                    if (selectedDayKeys.size == allKeys.size) {
                        onSelectedDayKeysChange(emptyList())
                    } else {
                        onSelectedDayKeysChange(allKeys)
                    }
                },
                onDeleteSelectedDays = onDeleteSelectedDays,
                onMenuClick = onMenuClick,
                onSearchClick = onSearchClick,
                totalCash = totalCash,
                isPrivacyMode = isPrivacyMode,
                onTogglePrivacyMode = { viewModel.togglePrivacyMode() },
                currencySymbol = settings.currencySymbol,
                formatCurrency = { value, sym -> viewModel.formatCurrency(value, sym) },
                commitments = commitments,
                computedCommitments = computedCommitments,
                linkHabayebDebts = linkHabayebDebts,
                onLinkHabayebDebtsChange = { viewModel.toggleLinkHabayebDebts(it) },
                monthlyLedger = monthlyLedger,
                selectedDayKeysCountText = selectedDayKeysCountText,
                isSelectAllChecked = isSelectAllChecked
            )
        }

        // Commitments Summary Cards - Row 1 of the 2x2 Grid block
        item(key = "commitments_summary") {
            CommitmentsSummaryCards(
                commitments = commitments,
                computedCommitments = computedCommitments,
                totalCash = totalCash,
                currencySymbol = settings.currencySymbol,
                formatCurrency = { value, sym -> viewModel.formatCurrency(value, sym) }
            )
        }

        if (!isScreenReady) {
            item(key = "loading_skeleton") {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = EmeraldPrimary,
                        strokeWidth = 3.dp
                    )
                }
            }
        } else if (monthlyLedger.isEmpty()) {
            item(key = "empty_state") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📓", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(id = R.string.ledger_empty_state_msg),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            // Ledger Month-by-month list
            monthlyLedger.forEachIndexed { monthIdx, monthLedger ->
                val isCollapsed = collapsedMonths.contains(monthLedger.monthKey)

                // Month Header
                item(key = "header_${monthLedger.monthKey}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val newCollapsed = if (isCollapsed) {
                                    collapsedMonths - monthLedger.monthKey
                                } else {
                                    collapsedMonths + monthLedger.monthKey
                                }
                                onCollapsedMonthsChange(newCollapsed)
                            }
                            .padding(start = 14.dp, end = 14.dp, top = if (monthIdx == 0) 2.dp else 12.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp).padding(end = 4.dp)
                            )
                            Text(
                                text = if (monthIdx == 0) stringResource(id = R.string.ledger_daily_record) else stringResource(id = R.string.ledger_monthly_record),
                                color = EmeraldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = monthLedger.monthName,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.weight(1f).padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    }
                }

                if (!isCollapsed) {
                    // Days list inside this month
                    items(monthLedger.days, key = { dayLedger -> "${monthLedger.monthKey}_${dayLedger.dayNumber}" }) { dayLedger ->
                        val dayKey = "${monthLedger.monthKey}_${dayLedger.dayNumber}"
                        val isDaySelected = selectedDayKeys.contains(dayKey)

                        DayCard(
                            dayLedger = dayLedger,
                            dayKey = dayKey,
                            isDaySelected = isDaySelected,
                            isDaySelectionMode = isDaySelectionMode,
                            haptic = haptic,
                            currencySymbol = settings.currencySymbol,
                            formatCurrency = { amt, sym -> viewModel.formatCurrency(amt, sym) },
                            onDayClick = {
                                if (isDaySelectionMode) {
                                    val newList = selectedDayKeys.toMutableList()
                                    if (newList.contains(it)) {
                                        newList.remove(it)
                                        if (newList.isEmpty()) {
                                            onDaySelectionModeChange(false)
                                        }
                                    } else {
                                        newList.add(it)
                                    }
                                    onSelectedDayKeysChange(newList)
                                } else {
                                    onActiveDayKeyChange(it)
                                }
                            },
                            onDayLongClick = {
                                if (!isDaySelectionMode && !isSelectionMode) {
                                    onDaySelectionModeChange(true)
                                    onSelectedDayKeysChange(listOf(it))
                                } else if (isDaySelectionMode) {
                                    val newList = selectedDayKeys.toMutableList()
                                    if (newList.contains(it)) {
                                        newList.remove(it)
                                        if (newList.isEmpty()) {
                                            onDaySelectionModeChange(false)
                                        }
                                    } else {
                                        newList.add(it)
                                    }
                                    onSelectedDayKeysChange(newList)
                                }
                            }
                        )
                    }
                }

                // Month Transition Separator
                if (monthIdx < monthlyLedger.size - 1) {
                    item(key = "transition_${monthLedger.monthKey}") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MonthTransitionLine()
                        }
                    }
                }
            }
        }
    }
}
