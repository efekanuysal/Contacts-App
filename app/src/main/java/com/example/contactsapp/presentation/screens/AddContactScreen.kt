package com.example.contactsapp.presentation.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.contactsapp.presentation.components.ContactInputField
import com.example.contactsapp.presentation.components.ImageSourceDialog
import com.example.contactsapp.presentation.components.ProfileImagePicker
import com.example.contactsapp.presentation.contract.ContactEvent
import com.example.contactsapp.presentation.contract.ContactState
import com.example.contactsapp.presentation.components.SuccessMessageOverlay
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    state: ContactState,
    onEvent: (ContactEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateSuccess: () -> Unit
) {
    val context = LocalContext.current
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> onEvent(ContactEvent.OnImageUriSelected(uri)) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) onEvent(ContactEvent.OnImageUriSelected(tempPhotoUri)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createTempPhotoUri(context)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    if (state.isContactSaved) {
        onNavigateSuccess()
        onEvent(ContactEvent.OnResetSaveState)
    }

    // USE THE REUSABLE DIALOG HERE
    if (state.isImagePickerBottomSheetOpen) {
        ImageSourceDialog(
            onDismissRequest = { onEvent(ContactEvent.OnDismissImagePickerBottomSheet) },
            onCameraClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onGalleryClick = { galleryLauncher.launch("image/*") }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    color = Color(0xFF007AFF),
                    fontSize = 16.sp,
                    modifier = Modifier.clickable { onNavigateBack() }
                )
                Text(
                    text = "New Contact",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Done",
                    color = if (state.firstNameInput.isNotBlank()) Color(0xFF007AFF) else Color.LightGray,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = state.firstNameInput.isNotBlank()) {
                        onEvent(ContactEvent.OnSaveContact)
                    }
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onEvent(ContactEvent.OnAddPhotoClicked) }
                ) {
                    ProfileImagePicker(
                        imageUri = state.selectedImageUri,
                        onClick = { onEvent(ContactEvent.OnAddPhotoClicked) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (state.selectedImageUri == null) "Add Photo" else "Change Photo",
                        color = Color(0xFF007AFF),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Error: ${state.errorMessage}",
                        color = Color.Red,
                        fontSize = 14.sp
                    )
                }
            Spacer(modifier = Modifier.height(32.dp))
            ContactInputField(
                value = state.firstNameInput,
                onValueChange = { onEvent(ContactEvent.OnFirstNameChanged(it)) },
                placeholder = "First Name"
            )
            Spacer(modifier = Modifier.height(16.dp))
            ContactInputField(
                value = state.lastNameInput,
                onValueChange = { onEvent(ContactEvent.OnLastNameChanged(it)) },
                placeholder = "Last Name"
            )
            Spacer(modifier = Modifier.height(16.dp))
            ContactInputField(
                value = state.phoneNumberInput,
                onValueChange = { onEvent(ContactEvent.OnPhoneNumberChanged(it)) },
                placeholder = "Phone Number",
                keyboardType = KeyboardType.Phone
            )
        }
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                }
            }
            if (state.globalSuccessMessage != null) {
                SuccessMessageOverlay(
                    message = state.globalSuccessMessage,
                    onDismiss = {
                        onEvent(ContactEvent.OnClearGlobalSuccessMessage)
                        onNavigateSuccess()
                    }
                )
            }
        }
    }
}

@Composable
fun ImageSourceBottomSheetContent(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            BottomSheetButton(
                text = "Camera",
                icon = Icons.Outlined.CameraAlt,
                onClick = onCameraClick
            )
            BottomSheetButton(
                text = "Gallery",
                icon = Icons.Outlined.Image,
                onClick = onGalleryClick
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onCancelClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text("Cancel", color = Color(0xFF007AFF), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun BottomSheetButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF007AFF))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = Color(0xFF007AFF), fontSize = 18.sp)
        }
    }
}

fun createTempPhotoUri(context: Context): Uri {
    val tempFile = File.createTempFile(
        "temp_image_${UUID.randomUUID()}",
        ".jpg",
        context.externalCacheDir
    )
    // Ensure you use the same authority as defined in AndroidManifest.xml
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
}