package com.example.ui.screens.habayeb.components.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    showFilterMenu: Boolean,
    onDismissRequest: () -> Unit,
    dateFilterMode: Int,
    onDateFilterModeChange: (Int) -> Unit,
    customStartDate: Long?,
    onCustomStartDateChange: (Long?) -> Unit,
    customEndDate: Long?,
    onCustomEndDateChange: (Long?) -> Unit,
    typeFilterMode: Int,
    onTypeFilterModeChange: (Int) -> Unit,
    activeThemeColor: Color,
    onShowDatePicker: (initialTime: Long?, onDateSelected: (Long) -> Unit) -> Unit
) {
    if (showFilterMenu) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.habayeb_smart_filter),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Date Filter Segment
                Text(
                    text = stringResource(id = R.string.habayeb_filter_date),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val dateModes = listOf(
                    0 to stringResource(id = R.string.habayeb_filter_all_time),
                    1 to stringResource(id = R.string.habayeb_filter_today),
                    2 to stringResource(id = R.string.habayeb_filter_month),
                    3 to stringResource(id = R.string.habayeb_filter_custom)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dateModes.forEach { (mode, label) ->
                        val isSelected = dateFilterMode == mode
                        val chipBg = if (isSelected) activeThemeColor else MaterialTheme.colorScheme.outlineVariant
                        val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .clickable {
                                    onDateFilterModeChange(mode)
                                    if (mode == 3 && customStartDate == null) {
                                        onShowDatePicker(customStartDate) { start ->
                                            onCustomStartDateChange(start)
                                            onShowDatePicker(customEndDate ?: start) { end ->
                                                onCustomEndDateChange(end)
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = chipText)
                        }
                    }
                }

                if (dateFilterMode == 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val startStr = customStartDate?.let { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(it)) } ?: "..."
                        val endStr = customEndDate?.let { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(it)) } ?: "..."
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .clickable { onShowDatePicker(customStartDate) { onCustomStartDateChange(it) } }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = activeThemeColor, modifier = Modifier.size(14.dp))
                                Text(startStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Text(stringResource(id = R.string.habayeb_to_text), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant)
                                .clickable { onShowDatePicker(customEndDate ?: customStartDate) { onCustomEndDateChange(it) } }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = activeThemeColor, modifier = Modifier.size(14.dp))
                                Text(endStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Type Filter Segment
                Text(
                    text = stringResource(id = R.string.habayeb_filter_by_type),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val typeModes = listOf(
                    0 to stringResource(id = R.string.habayeb_filter_all),
                    1 to stringResource(id = R.string.habayeb_filter_type_debts),
                    2 to stringResource(id = R.string.habayeb_filter_type_payments)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    typeModes.forEach { (mode, label) ->
                        val isSelected = typeFilterMode == mode
                        val chipBg = if (isSelected) activeThemeColor else MaterialTheme.colorScheme.outlineVariant
                        val chipText = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .clickable { onTypeFilterModeChange(mode) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = chipText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteCustomerConfirmDialog(
    showDialog: Boolean,
    customerName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(id = R.string.habayeb_delete_account_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(text = stringResource(id = R.string.habayeb_delete_account_confirm, customerName))
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(id = R.string.habayeb_delete_yes), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.habayeb_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun EditCustomerDialog(
    showDialog: Boolean,
    nameValue: String,
    onNameChange: (String) -> Unit,
    phoneValue: String,
    onPhoneChange: (String) -> Unit,
    activeThemeColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        val editNameFocusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            editNameFocusRequester.requestFocus()
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(stringResource(id = R.string.habayeb_edit_name_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nameValue,
                        onValueChange = onNameChange,
                        label = { Text(stringResource(id = R.string.habayeb_account_name)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(editNameFocusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeThemeColor,
                            focusedLabelColor = activeThemeColor,
                            cursorColor = activeThemeColor
                        )
                    )

                    OutlinedTextField(
                        value = phoneValue,
                        onValueChange = onPhoneChange,
                        label = { Text(stringResource(id = R.string.habayeb_phone_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeThemeColor,
                            focusedLabelColor = activeThemeColor,
                            cursorColor = activeThemeColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(id = R.string.habayeb_save_edit))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.habayeb_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun DeleteBulkTransactionsConfirmDialog(
    showDialog: Boolean,
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(id = R.string.habayeb_confirm_delete_txs),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(text = stringResource(id = R.string.habayeb_confirm_delete_txs_msg, selectedCount))
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.habayeb_delete),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = R.string.habayeb_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
