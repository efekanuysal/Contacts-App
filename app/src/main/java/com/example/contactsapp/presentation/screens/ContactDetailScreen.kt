package com.example.contactsapp.presentation.screens

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contactsapp.presentation.components.ContactInputField
import com.example.contactsapp.presentation.components.ImageSourceDialog
import com.example.contactsapp.presentation.components.ProfileImagePicker
import com.example.contactsapp.presentation.contract.ContactEvent
import com.example.contactsapp.presentation.contract.ContactState
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    state: ContactState,
    onEvent: (ContactEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            // FIX: Call the specific check event, NOT OnContactSelected
            onEvent(ContactEvent.OnCheckLocalContactStatus)
        }
    }
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
            val uri = createTempPhotoUri(context) // Use the same utility function
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            // If we were trying to save, save now.
            // Also triggers a check implicitly via the save function logic
            onEvent(ContactEvent.OnSaveToLocalPhone(context))
        }
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
        containerColor = Color.White,
        topBar = {
            if (state.isEditMode) {
                EditModeTopBar(
                    onCancel = { onEvent(ContactEvent.OnContactSelected(state.selectedContact!!)) }, // Revert
                    onDone = { onEvent(ContactEvent.OnUpdateContact) }
                )
            } else {
                ViewModeTopBar(
                    onBack = onNavigateBack,
                    isMenuExpanded = state.isMenuExpanded,
                    onMenuToggle = { onEvent(ContactEvent.OnMenuExpandedChanged(it)) },
                    onEditClick = { onEvent(ContactEvent.OnToggleEditMode) },
                    onDeleteClick = { onEvent(ContactEvent.OnDeleteContact) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Profile Image
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(
                    enabled = state.isEditMode,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onEvent(ContactEvent.OnAddPhotoClicked) }
            ) {
                ProfileImagePicker(
                    imageUri = state.selectedImageUri,
                    onClick = { if (state.isEditMode) onEvent(ContactEvent.OnAddPhotoClicked) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Change Photo",
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(enabled = state.isEditMode) {
                        if (state.isEditMode) onEvent(ContactEvent.OnAddPhotoClicked)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Fields
            if (state.isEditMode) {
                // Edit Mode Fields
                ContactInputField(value = state.firstNameInput, onValueChange = { onEvent(ContactEvent.OnFirstNameChanged(it)) }, placeholder = "First Name")
                Spacer(modifier = Modifier.height(16.dp))
                ContactInputField(value = state.lastNameInput, onValueChange = { onEvent(ContactEvent.OnLastNameChanged(it)) }, placeholder = "Last Name")
                Spacer(modifier = Modifier.height(16.dp))
                ContactInputField(value = state.phoneNumberInput, onValueChange = { onEvent(ContactEvent.OnPhoneNumberChanged(it)) }, placeholder = "Phone Number", keyboardType = KeyboardType.Phone)
            } else {
                // View Mode Fields (Read Only)
                ReadOnlyField(text = state.firstNameInput)
                Spacer(modifier = Modifier.height(16.dp))
                ReadOnlyField(text = state.lastNameInput)
                Spacer(modifier = Modifier.height(16.dp))
                ReadOnlyField(text = state.phoneNumberInput)

                Spacer(modifier = Modifier.height(48.dp))
                val isButtonEnabled = !state.isContactSaved
                // Save to Phone Button
                OutlinedButton(
                    onClick = {
                        // If already saved, do nothing (though button is disabled)
                        if (!state.isContactSaved) {
                            contactPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS
                                )
                            )
                        }
                    },
                    enabled = isButtonEnabled, // Locks button visually
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(30.dp),
                    border = if (isButtonEnabled) androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.Black
                    ) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isButtonEnabled) Color.Black else Color.LightGray
                    )
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isContactSaved) "Saved to Phone" else "Save to My Phone Contact",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // MESSAGE LOGIC
                if (state.isContactSaved) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "This contact is already saved your phone.",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            if (state.globalSuccessMessage != null) {
                SuccessMessageOverlay(
                    message = state.globalSuccessMessage,
                    onDismiss = { onEvent(ContactEvent.OnClearGlobalSuccessMessage) }
                )
            }
        }
    }
}

@Composable
fun ReadOnlyField(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(1.dp, Color(0xFFE5E5EA), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text, fontSize = 16.sp, color = Color.Black)
    }
}

@Composable
fun ViewModeTopBar(
    onBack: () -> Unit,
    isMenuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
        }

        Box {
            IconButton(onClick = { onMenuToggle(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.Black)
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { onMenuToggle(false) },
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        onEditClick()
                        onMenuToggle(false)
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = Color.Red) },
                    onClick = {
                        onDeleteClick()
                        onMenuToggle(false)
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                )
            }
        }
    }
}

@Composable
fun EditModeTopBar(onCancel: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cancel",
            color = Color(0xFF007AFF),
            fontSize = 16.sp,
            modifier = Modifier.clickable { onCancel() }
        )
        Text(
            text = "Edit Contact",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Done",
            color = Color(0xFF007AFF),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onDone() }
        )
    }
}

@Composable
fun SuccessMessageOverlay(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        delay(2000)
        onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00C853)) // Green
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    color = Color(0xFF00C853),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}