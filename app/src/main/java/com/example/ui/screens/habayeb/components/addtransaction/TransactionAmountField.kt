package com.example.ui.screens.habayeb.components.addtransaction

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun TransactionAmountField(
    amountStr: String,
    onAmountChange: (String) -> Unit,
    amountFocusRequester: FocusRequester,
    descFocusRequester: FocusRequester,
    dynamicThemeColor: Color,
    selectedTransactionCurrency: String,
    onShowCalculator: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = com.example.ui.theme.LocalIsDark.current

    OutlinedTextField(
        value = amountStr,
        onValueChange = onAmountChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(amountFocusRequester),
        placeholder = {
            Text(
                text = stringResource(id = R.string.habayeb_amount) + " *",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { descFocusRequester.requestFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
            unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White,
            focusedBorderColor = dynamicThemeColor,
            unfocusedBorderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.LightGray.copy(alpha = 0.6f),
            cursorColor = dynamicThemeColor
        ),
        singleLine = true,
        textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.Bold),
        leadingIcon = {
            IconButton(onClick = onShowCalculator) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = stringResource(id = R.string.habayeb_calculator),
                    tint = dynamicThemeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingIcon = {
            Text(
                text = selectedTransactionCurrency,
                color = dynamicThemeColor,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 14.sp
            )
        },
        shape = RoundedCornerShape(8.dp)
    )
}
