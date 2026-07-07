package com.example.ui.screens.business.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun DynamicPhoneList(
    phoneList: List<String>,
    onPhoneChange: (index: Int, newValue: String) -> Unit,
    onAddPhone: () -> Unit,
    onRemovePhone: (index: Int) -> Unit,
    activeThemeColor: Color
) {
    val focusManager = LocalFocusManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.biz_phones_section),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("biz_phones_section")
            )

            phoneList.forEachIndexed { index, phone ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { newVal -> onPhoneChange(index, newVal) },
                        label = { 
                            Text(
                                text = if (index == 0) {
                                    stringResource(id = R.string.biz_label_primary_phone)
                                } else {
                                    stringResource(id = R.string.biz_label_secondary_phone, index + 1)
                                }, 
                                fontSize = 13.sp
                            ) 
                        },
                        placeholder = { Text(text = stringResource(id = R.string.biz_placeholder_phone), fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = activeThemeColor,
                            focusedLabelColor = activeThemeColor,
                            cursorColor = activeThemeColor
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right)
                    )

                    if (index > 0) {
                        IconButton(
                            onClick = { onRemovePhone(index) },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.biz_desc_delete_phone)
                            )
                        }
                    }
                    
                    if (index == phoneList.lastIndex && phoneList.size < 3) {
                        IconButton(
                            onClick = onAddPhone,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = activeThemeColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = stringResource(id = R.string.biz_btn_add_phone)
                            )
                        }
                    }
                }
            }
        }
    }
}
