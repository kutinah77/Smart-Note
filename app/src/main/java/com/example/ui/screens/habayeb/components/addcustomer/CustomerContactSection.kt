package com.example.ui.screens.habayeb.components.addcustomer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.StringUtils.getContactDetails

@Composable
fun CustomerContactSection(
    nameStr: String,
    onNameChange: (String) -> Unit,
    focusRequester: FocusRequester,
    initialAmountFocusRequester: FocusRequester,
    activeThemeColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. الاسم (Account Name Input)
        OutlinedTextField(
            value = nameStr,
            onValueChange = onNameChange,
            label = { Text(stringResource(id = R.string.hint_account_name), fontSize = 10.sp) },
            placeholder = { Text(stringResource(id = R.string.habayeb_edit_name_desc), fontSize = 10.sp) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { initialAmountFocusRequester.requestFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = activeThemeColor,
                focusedLabelColor = activeThemeColor,
                cursorColor = activeThemeColor,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun CustomerPhoneSection(
    phoneStr: String,
    onPhoneChange: (String) -> Unit,
    nameStr: String,
    onNameChange: (String) -> Unit,
    phoneFocusRequester: FocusRequester,
    activeThemeColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { contactUri ->
        contactUri?.let { uri ->
            val details = getContactDetails(context, uri)
            if (details != null) {
                onNameChange(details.first)
                onPhoneChange(details.second)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.habayeb_toast_storage_permission),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 5. رقم الهاتف (Phone Input)
        OutlinedTextField(
            value = phoneStr,
            onValueChange = onPhoneChange,
            label = { Text(stringResource(id = R.string.habayeb_phone_label), fontSize = 10.sp) },
            placeholder = { Text(stringResource(id = R.string.habayeb_contact_picker), fontSize = 10.sp) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(phoneFocusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = activeThemeColor,
                focusedLabelColor = activeThemeColor,
                cursorColor = activeThemeColor,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            trailingIcon = {
                IconButton(onClick = {
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_CONTACTS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        contactPickerLauncher.launch(null)
                    } else {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = stringResource(id = R.string.habayeb_contact_picker),
                        tint = activeThemeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        )
    }
}
