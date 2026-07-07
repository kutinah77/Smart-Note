package com.example.ui.screens.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SoftRed

@Composable
fun DangerZoneSection(
    onResetClick: () -> Unit
) {
    OutlinedButton(
        onClick = onResetClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRed),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = SoftRed, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.backup_btn_delete_all), color = SoftRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
