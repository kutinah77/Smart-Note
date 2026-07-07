package com.example.ui.screens.habayeb.components

import androidx.compose.material3.MaterialTheme
 
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
 
@Composable
fun HabayebFilterToolbar(
    filteredCustomersCount: Int,
    financialSortMode: Int,
    onFinancialSortModeChanged: (Int) -> Unit,
    historicalSortMode: Int,
    onHistoricalSortModeChanged: (Int) -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color,
    haptic: HapticFeedback,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = com.example.ui.theme.LocalIsDark.current
    val primaryColor = activeThemeColor
    val backgroundLight = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF0F3FC)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val neutralWhite = MaterialTheme.colorScheme.surface

    var isMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1st Element in Row (RTL Right side): Customer Count Badge (Very compact and elegant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
            Text(
                text = stringResource(id = R.string.habayeb_customers_count, filteredCustomersCount),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }

        // 2nd Element in Row (RTL Left side): Smart Sorting Dropdown Menu Button
        Box {
            val isSorted = financialSortMode != 0 || historicalSortMode != 1 // default is historicalSortMode == 1
            val currentSortText = when {
                financialSortMode == 1 -> stringResource(id = R.string.filter_sort_largest)
                financialSortMode == 2 -> stringResource(id = R.string.filter_sort_smallest)
                historicalSortMode == 1 && financialSortMode == 0 -> stringResource(id = R.string.filter_sort_default)
                historicalSortMode == 2 -> stringResource(id = R.string.filter_sort_oldest)
                else -> stringResource(id = R.string.filter_sort_default)
            }

            Row(
                modifier = Modifier
                    .height(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSorted) primaryColor.copy(alpha = 0.15f) else backgroundLight)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isMenuExpanded = true
                    }
                    .padding(horizontal = 10.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentSortText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSorted) primaryColor else textPrimary
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier.background(neutralWhite)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.filter_sort_default),
                            fontSize = 12.sp,
                            fontWeight = if (financialSortMode == 0 && historicalSortMode == 1) FontWeight.Bold else FontWeight.Normal,
                            color = textPrimary
                        )
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFinancialSortModeChanged(0)
                        onHistoricalSortModeChanged(1)
                        isMenuExpanded = false
                        onScrollToTop()
                    }
                )
                HorizontalDivider(color = backgroundLight)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.filter_sort_largest),
                            fontSize = 12.sp,
                            fontWeight = if (financialSortMode == 1) FontWeight.Bold else FontWeight.Normal,
                            color = textPrimary
                        )
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onHistoricalSortModeChanged(0)
                        onFinancialSortModeChanged(1)
                        isMenuExpanded = false
                        onScrollToTop()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.filter_sort_smallest),
                            fontSize = 12.sp,
                            fontWeight = if (financialSortMode == 2) FontWeight.Bold else FontWeight.Normal,
                            color = textPrimary
                        )
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onHistoricalSortModeChanged(0)
                        onFinancialSortModeChanged(2)
                        isMenuExpanded = false
                        onScrollToTop()
                    }
                )
                HorizontalDivider(color = backgroundLight)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.filter_sort_oldest),
                            fontSize = 12.sp,
                            fontWeight = if (historicalSortMode == 2) FontWeight.Bold else FontWeight.Normal,
                            color = textPrimary
                        )
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFinancialSortModeChanged(0)
                        onHistoricalSortModeChanged(2)
                        isMenuExpanded = false
                        onScrollToTop()
                    }
                )
            }
        }
    }
}
