package com.example.ui.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SoftRed
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun DangerZone(
    viewModel: FinanceViewModel
) {
    var showTrapDialog by remember { mutableStateOf(false) }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SoftRed.copy(alpha = 0.03f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DangerDeleteButton {
                showTrapDialog = true
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.settings_danger_desc),
                fontSize = 11.sp,
                color = SoftRed.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 15.sp
            )
        }
    }

    if (showTrapDialog) {
        ResetTrapDialog(
            onDismiss = { showTrapDialog = false },
            onConfirmDelete = {
                viewModel.deleteAllData()
                showTrapDialog = false
            }
        )
    }
}
