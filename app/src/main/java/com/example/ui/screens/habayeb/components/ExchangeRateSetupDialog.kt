package com.example.ui.screens.habayeb.components

import androidx.compose.material3.MaterialTheme

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import androidx.compose.ui.res.stringResource
import com.example.ui.screens.habayeb.utils.CurrencyConfig

@Composable
fun ExchangeRateSetupContent(
    currencySymbol: String,
    selectedCurrency: String,
    initialRateStr: String,
    activeThemeColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var rateStr by remember { mutableStateOf(initialRateStr) }
    var isChecked by remember { mutableStateOf(false) }
    var showUncheckedError by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val isDark = com.example.ui.theme.LocalIsDark.current

    val statusColor by animateColorAsState(
        targetValue = when {
            isChecked -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
            showUncheckedError -> if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F)
            else -> activeThemeColor.copy(alpha = 0.85f)
        },
        animationSpec = tween(durationMillis = 180),
        label = "statusColor"
    )

    val inputBorderColor by animateColorAsState(
        targetValue = if (isFocused) activeThemeColor else if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.LightGray.copy(alpha = 0.4f),
        label = "inputBorder"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val currencyLabel = when (selectedCurrency) {
                context.getString(R.string.currency_usd) -> context.getString(R.string.habayeb_enter_exchange_rate_usd)
                context.getString(R.string.currency_sar) -> context.getString(R.string.habayeb_enter_exchange_rate_sar)
                else -> context.getString(R.string.habayeb_enter_exchange_rate_generic, selectedCurrency)
            }

            Text(
                text = currencyLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = activeThemeColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .border(1.dp, inputBorderColor, RoundedCornerShape(4.dp))
                    .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFAFBFD), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = rateStr,
                    onValueChange = { input ->
                        val cleaned = CurrencyConfig.normalizeDigits(input)
                        if (cleaned.isEmpty() || cleaned.toDoubleOrNull() != null || cleaned.last() == '.') {
                            rateStr = cleaned
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeThemeColor
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rateStr.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.habayeb_exchange_rate_placeholder),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        isChecked = !isChecked
                        if (isChecked) {
                            showUncheckedError = false
                        }
                    }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(
                            width = 1.dp,
                            color = statusColor,
                            shape = RoundedCornerShape(3.dp)
                        )
                        .background(
                            color = if (isChecked) statusColor else Color.Transparent,
                            shape = RoundedCornerShape(3.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(6.dp))
                
                Text(
                    text = context.getString(R.string.habayeb_confirm_exchange_rate_question),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    textAlign = TextAlign.Start
                )
            }

            Text(
                text = context.getString(R.string.habayeb_exchange_rate_hint_text),
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 10.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                    ),
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Text(stringResource(id = R.string.habayeb_cancel), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val doubleRate = rateStr.trim().toDoubleOrNull()
                        if (doubleRate == null || doubleRate <= 0.0) {
                            Toast.makeText(context, context.getString(R.string.habayeb_toast_enter_valid_rate), Toast.LENGTH_SHORT).show()
                        } else if (!isChecked) {
                            showUncheckedError = true
                            Toast.makeText(context, context.getString(R.string.habayeb_toast_confirm_rate_first), Toast.LENGTH_SHORT).show()
                        } else {
                            onConfirm(doubleRate)
                        }
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(32.dp),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = statusColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(id = R.string.habayeb_save), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExchangeRateSetupDialog(
    currencySymbol: String,
    selectedCurrency: String,
    initialRateStr: String,
    activeThemeColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val isDark = com.example.ui.theme.LocalIsDark.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .width(260.dp)
                .wrapContentHeight()
                .shadow(8.dp, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(1.dp, activeThemeColor.copy(alpha = 0.12f))
        ) {
            ExchangeRateSetupContent(
                currencySymbol = currencySymbol,
                selectedCurrency = selectedCurrency,
                initialRateStr = initialRateStr,
                activeThemeColor = activeThemeColor,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
