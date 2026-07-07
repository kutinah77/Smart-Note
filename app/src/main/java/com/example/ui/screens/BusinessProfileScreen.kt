package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.screens.business.components.BusinessInfoForm
import com.example.ui.screens.business.components.DynamicPhoneList
import com.example.ui.screens.business.components.LogoPickerSection
import com.example.ui.screens.business.components.PhotoEditorDialog
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    val activeThemeColor = MaterialTheme.colorScheme.primary

    val profileState by viewModel.businessProfile.collectAsStateWithLifecycle()

    var bizName by remember { mutableStateOf("") }
    var bizDesc by remember { mutableStateOf("") }
    var logoPath by remember { mutableStateOf("") }
    val phoneList = remember { mutableStateListOf<String>() }

    // Sync state when repository emits new values
    LaunchedEffect(profileState) {
        bizName = profileState.name
        bizDesc = profileState.description
        logoPath = profileState.logoPath
        phoneList.clear()
        phoneList.addAll(profileState.phones)
        if (phoneList.isEmpty()) {
            phoneList.add("")
        }
    }

    var logoBitmapState by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(logoPath) {
        if (logoPath.isNotEmpty()) {
            try {
                val file = java.io.File(logoPath)
                if (file.exists()) {
                    logoBitmapState = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            logoBitmapState = null
        }
    }

    var pendingImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var editingBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            pendingImageUri = uri
            val original = com.example.domain.ImageProcessor.uriToBitmap(context, uri)
            if (original != null) {
                editingBitmap = com.example.domain.ImageProcessor.scaleBitmap(original, 800)
            } else {
                Toast.makeText(context, context.getString(R.string.biz_toast_logo_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.biz_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("biz_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.biz_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = activeThemeColor
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogoPickerSection(
                logoBitmap = logoBitmapState,
                activeThemeColor = activeThemeColor,
                onLogoClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemoveLogoClick = {
                    logoPath = ""
                    logoBitmapState = null
                }
            )

            BusinessInfoForm(
                name = bizName,
                onNameChange = { bizName = it },
                description = bizDesc,
                onDescriptionChange = { bizDesc = it },
                activeThemeColor = activeThemeColor
            )

            DynamicPhoneList(
                phoneList = phoneList,
                onPhoneChange = { index, newVal ->
                    phoneList[index] = newVal
                },
                onAddPhone = {
                    if (phoneList.size < 3) {
                        phoneList.add("")
                    }
                },
                onRemovePhone = { index ->
                    if (phoneList.size > 1) {
                        phoneList.removeAt(index)
                    }
                },
                activeThemeColor = activeThemeColor
            )

            Button(
                onClick = {
                    if (bizName.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.biz_toast_err_empty_name), Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val updatedProfile = com.example.data.repository.BusinessProfile(
                        name = bizName.trim(),
                        description = bizDesc.trim(),
                        logoPath = logoPath,
                        phones = phoneList.filter { it.isNotBlank() }.map { it.trim() }
                    )
                    viewModel.saveBusinessProfile(updatedProfile)

                    Toast.makeText(context, context.getString(R.string.biz_toast_save_success), Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("biz_save_button"),
                colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        text = stringResource(id = R.string.biz_btn_save),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (editingBitmap != null) {
        PhotoEditorDialog(
            editingBitmap = editingBitmap!!,
            activeThemeColor = activeThemeColor,
            onDismiss = {
                editingBitmap = null
                pendingImageUri = null
            },
            onApply = { croppedResult ->
                val scaledResult = com.example.domain.ImageProcessor.scaleBitmap(croppedResult, 400)
                val localPath = com.example.domain.ImageProcessor.saveBitmapToInternalStorage(context, scaledResult)
                if (localPath != null) {
                    logoPath = localPath
                    logoBitmapState = scaledResult
                    Toast.makeText(context, context.getString(R.string.biz_toast_logo_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.biz_toast_logo_save_err), Toast.LENGTH_SHORT).show()
                }
                editingBitmap = null
                pendingImageUri = null
            }
        )
    }
}
