package com.example.ui.screens.ledger.sections

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.ui.components.CircularRevealShape
import com.example.ui.screens.HabayebScreen
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun LedgerOverlayManager(
    isHabayebActive: Boolean,
    onClose: () -> Unit,
    habayebButtonCenter: Offset,
    viewModel: FinanceViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(isHabayebActive) {
        if (isHabayebActive) {
            animProgress.animateTo(1f, animationSpec = tween(450, easing = FastOutSlowInEasing))
        } else {
            animProgress.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        }
    }

    if (animProgress.value > 0f) {
        val revealCenter = if (habayebButtonCenter != Offset.Zero) {
            habayebButtonCenter
        } else {
            Offset(250f, 400f) // comfortable default
        }
        val isRelativeReveal = (habayebButtonCenter == Offset.Zero)

        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        val pivotX = if (isRelativeReveal) 0.5f else (revealCenter.x / screenWidthPx).coerceIn(0f, 1f)
        val pivotY = if (isRelativeReveal) 0.5f else (revealCenter.y / screenHeightPx).coerceIn(0f, 1f)

        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = animProgress.value
                    scaleX = animProgress.value
                    scaleY = animProgress.value
                    transformOrigin = TransformOrigin(pivotX, pivotY)
                }
                .clip(CircularRevealShape(animProgress.value, revealCenter, isRelative = isRelativeReveal))
        ) {
            HabayebScreen(
                viewModel = viewModel,
                onMenuClick = onMenuClick,
                onClose = onClose
            )
        }
    }
}
