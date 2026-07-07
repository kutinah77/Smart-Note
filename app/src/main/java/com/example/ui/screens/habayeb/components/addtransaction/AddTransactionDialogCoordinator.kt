package com.example.ui.screens.habayeb.components.addtransaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.CalculatorDialog

@Composable
fun AddTransactionDialogCoordinator(
    showCalculator: Boolean,
    onDismissCalculator: () -> Unit,
    onValueConfirmed: (Double) -> Unit,
    activeThemeColor: Color,
    activeSubColor: Color
) {
    if (showCalculator) {
        CalculatorDialog(
            onDismiss = onDismissCalculator,
            onValueConfirmed = onValueConfirmed,
            activeThemeColor = activeThemeColor,
            activeSubColor = activeSubColor
        )
    }
}
