package com.example.ui.screens.habayeb.sections

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.habayeb.components.CustomerItemRow
import com.example.ui.state.CustomerUiState

@Composable
fun HabayebListSection(
    filteredCustomers: List<CustomerUiState>,
    isScreenReady: Boolean,
    isPrivacyMode: Boolean,
    isMultiSelectActive: Boolean,
    onMultiSelectActiveChanged: (Boolean) -> Unit,
    selectedCustomerIds: List<String>,
    onSelectedCustomerIdsChanged: (List<String>) -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color,
    currencySymbol: String,
    haptic: HapticFeedback,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    selectedFilterTab: Int,
    contentPadding: PaddingValues,
    onCustomerClick: (CustomerUiState) -> Unit,
    onQuickAdd: (CustomerUiState) -> Unit,
    onDeleteSelectedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = com.example.ui.theme.LocalIsDark.current

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + 80.dp
            )
        ) {
            if (!isScreenReady) {
                item(key = "loading_skeleton") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = com.example.ui.theme.EmeraldPrimary,
                            strokeWidth = 3.dp
                        )
                    }
                }
            } else if (filteredCustomers.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillParentMaxHeight(0.6f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🤝", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when (selectedFilterTab) {
                                    1 -> stringResource(id = R.string.habayeb_no_debtors)
                                    2 -> stringResource(id = R.string.habayeb_no_creditors)
                                    else -> stringResource(id = R.string.habayeb_empty_list)
                                },
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            } else {
                items(filteredCustomers, key = { it.id }) { customer ->
                    val isSelected = selectedCustomerIds.contains(customer.id)

                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                        CustomerItemRow(
                            isPrivacyMode = isPrivacyMode,
                            customer = customer,
                            isSelected = isSelected,
                            isMultiSelectActive = isMultiSelectActive,
                            activeThemeColor = activeThemeColor,
                            activeSubColor = activeSubColor,
                            currencySymbol = currencySymbol,
                            haptic = haptic,
                            onCustomerClick = {
                                if (isMultiSelectActive) {
                                    val newList = selectedCustomerIds.toMutableList()
                                    if (isSelected) {
                                        newList.remove(customer.id)
                                        if (newList.isEmpty()) {
                                            onMultiSelectActiveChanged(false)
                                        }
                                    } else {
                                        newList.add(customer.id)
                                    }
                                    onSelectedCustomerIdsChanged(newList)
                                } else {
                                    onCustomerClick(customer)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onCustomerLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onMultiSelectActiveChanged(true)
                                if (!isSelected) {
                                    val newList = selectedCustomerIds.toMutableList()
                                    newList.add(customer.id)
                                    onSelectedCustomerIdsChanged(newList)
                                }
                            },
                            onQuickAdd = {
                                onQuickAdd(customer)
                            }
                        )
                    }
                }
            }
        }

        // --- Multi-Select Floating Bar ---
        AnimatedVisibility(
            visible = isMultiSelectActive,
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
                        onMultiSelectActiveChanged(false)
                        onSelectedCustomerIdsChanged(emptyList())
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(id = R.string.desc_deselect),
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
                            val allSelected = filteredCustomers.isNotEmpty() && filteredCustomers.all { selectedCustomerIds.contains(it.id) }
                            val newList = if (allSelected) {
                                emptyList()
                            } else {
                                filteredCustomers.map { it.id }
                            }
                            onSelectedCustomerIdsChanged(newList)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val allSelected = filteredCustomers.isNotEmpty() && filteredCustomers.all { selectedCustomerIds.contains(it.id) }
                    Icon(
                        imageVector = if (allSelected) Icons.Default.Check else Icons.Default.List,
                        contentDescription = stringResource(id = R.string.desc_select_all),
                        tint = if (allSelected) activeThemeColor else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (allSelected) stringResource(id = R.string.text_selected_all) else stringResource(id = R.string.text_selected_count, selectedCustomerIds.size),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete Button
                IconButton(
                    onClick = {
                        if (selectedCustomerIds.isNotEmpty()) {
                            onDeleteSelectedClick()
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (isDark) Color(0xFF3E1F1F) else Color(0xFFFEF2F2), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.desc_delete_selected),
                        tint = if (isDark) Color(0xFFEF5350) else Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
