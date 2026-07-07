package com.example.ui.screens.security.components

import androidx.compose.material3.MaterialTheme
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.AppSettings
import com.example.domain.HashUtils
import com.example.domain.DatabaseSecurityGuard

@Composable
fun SecurityActivePanel(
    currentSettings: AppSettings,
    isVerified: Boolean,
    onVerifySuccess: () -> Unit,
    onStartChangePasscode: () -> Unit,
    onCopyRecoveryPhrase: () -> Unit,
    onDeactivateSecurity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isDark = com.example.ui.theme.LocalIsDark.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isVerified) {
                // VERIFICATION FLOW (تأكيد الهوية)
                var verifyMethod by remember { mutableIntStateOf(0) } // 0 = PIN, 1 = Recovery Phrase
                var pinInput by remember { mutableStateOf("") }
                var phraseInput by remember { mutableStateOf("") }
                var pinVisible by remember { mutableStateOf(false) }

                val pinFocusRequester = remember { FocusRequester() }
                val phraseFocusRequester = remember { FocusRequester() }

                // Auto-focus based on selected method
                LaunchedEffect(verifyMethod) {
                    if (verifyMethod == 0) {
                        pinFocusRequester.requestFocus()
                    } else {
                        phraseFocusRequester.requestFocus()
                    }
                }

                Text(
                    text = "تأكيد الهوية لإدارة الأمان",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.End)
                )

                // Tab-like Selection Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Method 1: Recovery Phrase
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (verifyMethod == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { verifyMethod = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "مفتاح الاسترداد",
                            color = if (verifyMethod == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Method 0: PIN
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (verifyMethod == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { verifyMethod = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "رمز القفل الحالي",
                            color = if (verifyMethod == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                if (verifyMethod == 0) {
                    // PIN VERIFICATION FIELD
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pinInput = it
                                if (it.length == 4) {
                                    focusManager.clearFocus()
                                    val hashed = HashUtils.hashString(it)
                                    val isMatch = DatabaseSecurityGuard.secureEqual(hashed, currentSettings.passcodeHash.orEmpty())
                                    if (isMatch) {
                                        onVerifySuccess()
                                        Toast.makeText(context, "تم التحقق بنجاح ✅", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "رمز القفل غير صحيح! ❌", Toast.LENGTH_SHORT).show()
                                        pinInput = ""
                                    }
                                }
                            }
                        },
                        label = { Text("أدخل رمز القفل الحالي") },
                        placeholder = { Text("٤ أرقام") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { pinVisible = !pinVisible }) {
                                Icon(
                                    imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp
                        ),
                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .focusRequester(pinFocusRequester)
                    )
                } else {
                    // RECOVERY PHRASE VERIFICATION FIELD
                    OutlinedTextField(
                        value = phraseInput,
                        onValueChange = { phraseInput = it },
                        label = { Text("أدخل مفتاح الأمان (الاسترداد)") },
                        placeholder = { Text("مثال: فاطمة أو أحمد") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Right,
                            fontSize = 14.sp
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (phraseInput.isNotBlank()) {
                                val hashed = HashUtils.hashString(phraseInput.trim())
                                val isMatch = DatabaseSecurityGuard.secureEqual(hashed, currentSettings.recoveryPhraseHash.orEmpty())
                                if (isMatch) {
                                    onVerifySuccess()
                                    Toast.makeText(context, "تم التحقق بنجاح ✅", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "مفتاح الأمان غير صحيح! ❌", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(phraseFocusRequester)
                    )

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (phraseInput.isNotBlank()) {
                                val hashed = HashUtils.hashString(phraseInput.trim())
                                val isMatch = DatabaseSecurityGuard.secureEqual(hashed, currentSettings.recoveryPhraseHash.orEmpty())
                                if (isMatch) {
                                    onVerifySuccess()
                                    Toast.makeText(context, "تم التحقق بنجاح ✅", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "مفتاح الأمان غير صحيح! ❌", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = phraseInput.isNotBlank()
                    ) {
                        Text(
                            text = "تحقق وتأكيد",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // VERIFIED STATE: Options to Change PIN or Disable Lock
                val shieldBg = if (isDark) Color(0xFF0D2818) else Color(0xFFE8F5E9)
                val shieldBorder = if (isDark) Color(0xFF2E7D32) else Color(0xFF81C784)
                val shieldTint = if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32)

                // Big glowing shield indicator
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(shieldBg, CircleShape)
                        .border(width = 1.5.dp, color = shieldBorder, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = shieldTint,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "تم تأكيد الهوية بنجاح ✅",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = shieldTint
                )

                Text(
                    text = "تم فك قفل لوحة التحكم بالأمان. يمكنك الآن تعديل رمز القفل أو إيقاف التفعيل تماماً.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // OPTION 1: CHANGE PIN
                Button(
                    onClick = onStartChangePasscode,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تغيير رمز القفل الجديد",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // OPTION 2: DEACTIVATE LOCK
                OutlinedButton(
                    onClick = onDeactivateSecurity,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تعطيل قفل التطبيق والأمان",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                // OPTION 3: COPY RECOVERY PHRASE
                TextButton(
                    onClick = onCopyRecoveryPhrase,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.sec_btn_copy),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
