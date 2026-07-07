package com.example.ui.screens.habayeb.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.unit.dp
import com.example.ui.screens.habayeb.components.HabayebFilterTabs
import com.example.ui.screens.habayeb.components.HabayebHeaderTopBar

@Composable
fun HabayebStatsHeader(
    isSearchActive: Boolean,
    onSearchActiveChanged: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onMenuClick: () -> Unit,
    haptic: HapticFeedback,
    totalOwedByThem: Double,
    totalOwedToThem: Double,
    isPrivacyMode: Boolean,
    onTogglePrivacy: () -> Unit,
    currencySymbol: String,
    selectedFilterTab: Int,
    onFilterTabSelected: (Int) -> Unit,
    activeThemeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = activeThemeColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            HabayebHeaderTopBar(
                isSearchActive = isSearchActive,
                onSearchActiveChanged = onSearchActiveChanged,
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                onMenuClick = onMenuClick,
                haptic = haptic,
                netDebt = totalOwedByThem - totalOwedToThem,
                isPrivacyMode = isPrivacyMode,
                onTogglePrivacy = onTogglePrivacy,
                currencySymbol = currencySymbol
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        HabayebFilterTabs(
            selectedFilterTab = selectedFilterTab,
            onFilterTabSelected = onFilterTabSelected,
            totalOwedByThem = totalOwedByThem,
            totalOwedToThem = totalOwedToThem,
            currencySymbol = currencySymbol,
            isPrivacyMode = isPrivacyMode,
            haptic = haptic
        )
    }
}
