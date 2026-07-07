package com.example.ui.screens.habayeb.components.addtransaction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun ActionButtonsRow(
    isSaving: Boolean,
    isLendOperationSelected: Boolean,
    onDebtClick: () -> Unit,
    onPayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val debtInteractionSource = remember { MutableInteractionSource() }
    val isDebtPressed by debtInteractionSource.collectIsPressedAsState()
    val debtScale by animateFloatAsState(
        targetValue = if (isDebtPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 1500f
        ),
        label = "DebtBtnScale"
    )

    val payInteractionSource = remember { MutableInteractionSource() }
    val isPayPressed by payInteractionSource.collectIsPressedAsState()
    val payScale by animateFloatAsState(
        targetValue = if (isPayPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 1500f
        ),
        label = "PayBtnScale"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            enabled = !isSaving,
            onClick = onDebtClick,
            interactionSource = debtInteractionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEF4444), // Red
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .graphicsLayer {
                    scaleX = debtScale
                    scaleY = debtScale
                }
        ) {
            Text(
                text = stringResource(id = R.string.btn_new_debt),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Button(
            enabled = !isSaving,
            onClick = onPayClick,
            interactionSource = payInteractionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981), // Green
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .graphicsLayer {
                    scaleX = payScale
                    scaleY = payScale
                }
        ) {
            Text(
                text = if (isLendOperationSelected) stringResource(id = R.string.btn_receive) else stringResource(id = R.string.btn_pay),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
