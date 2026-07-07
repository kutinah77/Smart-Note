package com.example.ui.screens.ledger.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.entities.FixedCommitment
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SoftRed
import kotlinx.coroutines.delay

@Composable
fun CommitmentEditDialog(
    showCommitmentDialog: Boolean,
    editingCommitment: FixedCommitment?,
    onDismissRequest: () -> Unit,
    onSaveCommitment: (name: String, targetAmount: Double, currentProgress: Double) -> Unit,
    onDeleteCommitment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!showCommitmentDialog) return

    val nameFocus = remember { FocusRequester() }
    val targetFocus = remember { FocusRequester() }
    val progressFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val initialName = editingCommitment?.name ?: ""
    val initialTarget = editingCommitment?.targetAmount?.let { if (it > 0) it.toInt().toString() else "" } ?: ""
    val initialProgress = editingCommitment?.currentProgress?.let { if (it > 0) it.toInt().toString() else "" } ?: ""

    var obligationName by rememberSaveable(editingCommitment) { mutableStateOf(initialName) }
    var targetAmtStr by rememberSaveable(editingCommitment) { mutableStateOf(initialTarget) }
    var progressAmtStr by rememberSaveable(editingCommitment) { mutableStateOf(initialProgress) }

    LaunchedEffect(Unit) {
        delay(250)
        try {
            if (editingCommitment == null) {
                nameFocus.requestFocus()
            } else {
                targetFocus.requestFocus()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.98f)
                .clip(RoundedCornerShape(24.dp)),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sleek Header with subtle styling
                Text(
                    text = if (editingCommitment != null) stringResource(id = R.string.ledger_edit_commitment_title) else stringResource(id = R.string.ledger_add_commitment_title),
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input fields
                OutlinedTextField(
                    value = obligationName,
                    onValueChange = { if (editingCommitment == null) obligationName = it },
                    enabled = (editingCommitment == null),
                    label = { Text(stringResource(id = R.string.ledger_commitment_name_label), fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        disabledBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(nameFocus),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { targetFocus.requestFocus() }),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetAmtStr,
                    onValueChange = { targetAmtStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { progressFocus.requestFocus() }),
                    label = { Text(stringResource(id = R.string.ledger_commitment_target_amount_label), fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(targetFocus),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = progressAmtStr,
                    onValueChange = { progressAmtStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    label = { Text(stringResource(id = R.string.ledger_commitment_current_progress_label), fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(progressFocus),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Row (Highly polished and proportioned)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (editingCommitment != null) {
                        OutlinedButton(
                            onClick = { onDeleteCommitment(editingCommitment.name) },
                            border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRed),
                            modifier = Modifier
                                .weight(0.7f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.ledger_commitment_delete),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onDismissRequest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.common_cancel),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            val tar = targetAmtStr.toDoubleOrNull() ?: 0.0
                            val prg = progressAmtStr.toDoubleOrNull() ?: 0.0
                            if (obligationName.isNotBlank() && tar > 0) {
                                onSaveCommitment(obligationName, tar, prg)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.ledger_save_commitment_btn),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
