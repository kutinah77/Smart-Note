package com.example.ui.screens.habayeb.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
class HabayebScrollState(
    val maxScrollPx: Float,
    initialOffset: Float = 0f
) {
    var headerOffsetHeightPx by mutableStateOf(initialOffset)

    fun nestedScrollConnection(isSearchActive: Boolean): NestedScrollConnection {
        return object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isSearchActive) return Offset.Zero
                val delta = available.y
                val newOffset = headerOffsetHeightPx + delta
                headerOffsetHeightPx = newOffset.coerceIn(-maxScrollPx, 0f)
                return Offset.Zero
            }
        }
    }

    fun reset() {
        headerOffsetHeightPx = 0f
    }
}

@Composable
fun rememberHabayebScrollState(
    expandedHeaderHeight: Dp = 220.dp,
    collapsedHeaderHeight: Dp = 56.dp
): HabayebScrollState {
    val density = LocalDensity.current
    val maxScrollPx = with(density) { (expandedHeaderHeight - collapsedHeaderHeight).toPx() }
    return remember(maxScrollPx) {
        HabayebScrollState(maxScrollPx)
    }
}
