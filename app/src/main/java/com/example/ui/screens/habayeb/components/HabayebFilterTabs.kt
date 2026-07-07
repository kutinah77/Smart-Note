package com.example.ui.screens.habayeb.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import com.example.R

@Composable
fun HabayebFilterTabs(
    selectedFilterTab: Int,
    onFilterTabSelected: (Int) -> Unit,
    totalOwedByThem: Double,
    totalOwedToThem: Double,
    currencySymbol: String,
    isPrivacyMode: Boolean = false,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    // صف التصفية المتناسق والمنسق بلمسة جمالية واحترافية وارتفاع مريح
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. كبسولة لي عند الناس (المدينين) المتناسقة والمريحة للعين
        val isOwedByThemSelected = selectedFilterTab == 1
        val formattedOwedByThem = String.format(java.util.Locale.ENGLISH, "%,.0f", totalOwedByThem)
        val isDark = com.example.ui.theme.LocalIsDark.current
        val owedByThemBg = if (isDark) {
            if (isOwedByThemSelected) Color(0xFF3E1F1F) else MaterialTheme.colorScheme.surface
        } else {
            if (isOwedByThemSelected) Color(0xFFFFF5F5) else MaterialTheme.colorScheme.surface
        }
        val owedByThemBorder = if (isDark) {
            if (isOwedByThemSelected) Color(0xFFEF5350) else MaterialTheme.colorScheme.outlineVariant
        } else {
            if (isOwedByThemSelected) Color(0xFFEF4444) else Color(0xFFE2E8F0)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .shadow(
                    elevation = if (isOwedByThemSelected) 2.dp else 0.dp,
                    shape = RoundedCornerShape(14.dp)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(owedByThemBg)
                .border(
                    BorderStroke(
                        width = if (isOwedByThemSelected) 1.5.dp else 1.dp,
                        color = owedByThemBorder
                    ),
                    RoundedCornerShape(14.dp)
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFilterTabSelected(if (isOwedByThemSelected) 0 else 1)
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(id = R.string.habayeb_filter_owed_by),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isOwedByThemSelected) (if (isDark) Color(0xFFFF8A80) else Color(0xFFB91C1C)) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "*****" else "$formattedOwedByThem $currencySymbol",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOwedByThemSelected) (if (isDark) Color(0xFFFF8A80) else Color(0xFF991B1B)) else Color(0xFFEF4444)
                    )
                }
            }
        }

        // 2. كبسولة علي للناس (الدائنين) المتناسقة والمريحة للعين
        val isOwedToThemSelected = selectedFilterTab == 2
        val formattedOwedToThem = String.format(java.util.Locale.ENGLISH, "%,.0f", totalOwedToThem)
        val owedToThemBg = if (isDark) {
            if (isOwedToThemSelected) Color(0xFF1B3B2B) else MaterialTheme.colorScheme.surface
        } else {
            if (isOwedToThemSelected) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surface
        }
        val owedToThemBorder = if (isDark) {
            if (isOwedToThemSelected) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
        } else {
            if (isOwedToThemSelected) Color(0xFF10B981) else Color(0xFFE2E8F0)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .shadow(
                    elevation = if (isOwedToThemSelected) 2.dp else 0.dp,
                    shape = RoundedCornerShape(14.dp)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(owedToThemBg)
                .border(
                    BorderStroke(
                        width = if (isOwedToThemSelected) 1.5.dp else 1.dp,
                        color = owedToThemBorder
                    ),
                    RoundedCornerShape(14.dp)
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFilterTabSelected(if (isOwedToThemSelected) 0 else 2)
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(id = R.string.habayeb_filter_owed_to),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isOwedToThemSelected) (if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPrivacyMode) "*****" else "$formattedOwedToThem $currencySymbol",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOwedToThemSelected) (if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)) else Color(0xFF10B981)
                    )
                }
            }
        }
    }
}
