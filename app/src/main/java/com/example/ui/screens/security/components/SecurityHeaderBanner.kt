package com.example.ui.screens.security.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldPrimary

@Composable
fun SecurityHeaderBanner(
    isAlreadyPasscodeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(20.dp))
    ) {
        val isDark = com.example.ui.theme.LocalIsDark.current
        val iconBg = if (isDark) {
            if (isAlreadyPasscodeEnabled) Color(0xFF064E3B) else Color(0xFF1E293B)
        } else {
            if (isAlreadyPasscodeEnabled) Color(0xFFECFDF5) else Color(0xFFEFF6FF)
        }
        val iconTint = if (isDark) {
            if (isAlreadyPasscodeEnabled) Color(0xFF34D399) else MaterialTheme.colorScheme.primary
        } else {
            if (isAlreadyPasscodeEnabled) Color(0xFF059669) else EmeraldPrimary
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = iconBg,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAlreadyPasscodeEnabled) Icons.Default.VerifiedUser else Icons.Default.Security,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Text(
                    text = if (isAlreadyPasscodeEnabled) stringResource(id = R.string.sec_status_active) else stringResource(id = R.string.sec_status_inactive),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAlreadyPasscodeEnabled) stringResource(id = R.string.sec_desc_active) else stringResource(id = R.string.sec_desc_inactive),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}
