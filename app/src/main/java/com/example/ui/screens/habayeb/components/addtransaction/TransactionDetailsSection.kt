package com.example.ui.screens.habayeb.components.addtransaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailsSection(
    descStr: String,
    onDescChange: (String) -> Unit,
    dateMillis: Long,
    onDateChange: (Long) -> Unit,
    descFocusRequester: FocusRequester,
    dynamicThemeColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isDark = com.example.ui.theme.LocalIsDark.current

    val formattedSelectedDate = remember(dateMillis) {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))
        sdf.format(Date(dateMillis))
    }

    OutlinedTextField(
        value = descStr,
        onValueChange = onDescChange,
        placeholder = {
            Text(
                text = stringResource(id = R.string.habayeb_tx_desc_optional),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
            focusedBorderColor = dynamicThemeColor,
            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.LightGray.copy(alpha = 0.6f),
            cursorColor = dynamicThemeColor
        ),
        textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 13.sp),
        leadingIcon = {
            Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedSelectedDate,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        DatePickerDialog(
                            context,
                            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                                calendar.set(Calendar.YEAR, selectedYear)
                                calendar.set(Calendar.MONTH, selectedMonth)
                                calendar.set(Calendar.DAY_OF_MONTH, selectedDayOfMonth)

                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)
                                        onDateChange(calendar.timeInMillis)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = stringResource(id = R.string.habayeb_tx_date),
                        tint = dynamicThemeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        },
        shape = RoundedCornerShape(8.dp),
        singleLine = false,
        maxLines = 2,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp, max = 56.dp)
            .focusRequester(descFocusRequester),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = { focusManager.clearFocus() }
        )
    )
}
