package com.example.ui.screens.habayeb.components.addcustomer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Calendar

@Composable
fun InitialBalanceSection(
    initialAmountStr: String,
    onInitialAmountChange: (String) -> Unit,
    notesStr: String,
    onNotesChange: (String) -> Unit,
    selectedTransactionCurrency: String,
    initialAmountFocusRequester: FocusRequester,
    notesFocusRequester: FocusRequester,
    phoneFocusRequester: FocusRequester,
    activeThemeColor: Color,
    onShowCalculator: () -> Unit,
    selectedCalendar: Calendar,
    onDateSelected: (Calendar) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 2. المبلغ (Initial Amount Input)
        OutlinedTextField(
            value = initialAmountStr,
            onValueChange = onInitialAmountChange,
            label = { Text(stringResource(id = R.string.hint_opening_balance), fontSize = 10.sp) },
            placeholder = { Text("0", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { notesFocusRequester.requestFocus() }),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(initialAmountFocusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = activeThemeColor,
                focusedLabelColor = activeThemeColor,
                cursorColor = activeThemeColor,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            leadingIcon = {
                IconButton(onClick = onShowCalculator, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = stringResource(id = R.string.habayeb_calculator),
                        tint = activeThemeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            trailingIcon = {
                Text(
                    text = selectedTransactionCurrency,
                    fontSize = 10.sp,
                    color = activeThemeColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        )

        // 4. بيان العملية (Details/Statement field - notesStr)
        OutlinedTextField(
            value = notesStr,
            onValueChange = onNotesChange,
            label = { Text(stringResource(id = R.string.hint_description), fontSize = 10.sp) },
            placeholder = { Text(stringResource(id = R.string.hint_description), fontSize = 10.sp) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(notesFocusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { phoneFocusRequester.requestFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = activeThemeColor,
                focusedLabelColor = activeThemeColor,
                cursorColor = activeThemeColor,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        val year = selectedCalendar.get(Calendar.YEAR)
                        val month = selectedCalendar.get(Calendar.MONTH)
                        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)
                        android.app.DatePickerDialog(
                            context,
                            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                                val newCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, selectedYear)
                                    set(Calendar.MONTH, selectedMonth)
                                    set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)
                                    set(Calendar.HOUR_OF_DAY, 12)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onDateSelected(newCal)
                            },
                            year,
                            month,
                            day
                        ).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = stringResource(id = R.string.habayeb_tx_date),
                        tint = activeThemeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )
    }
}
