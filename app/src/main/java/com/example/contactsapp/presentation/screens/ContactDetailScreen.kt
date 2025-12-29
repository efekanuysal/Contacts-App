package com.example.contactsapp.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.contactsapp.presentation.components.ContactInputField
import com.example.contactsapp.presentation.components.ImageSourceDialog
import com.example.contactsapp.presentation.components.ProfileImagePicker
import com.example.contactsapp.presentation.components.SuccessMessageOverlay
import com.example.contactsapp.presentation.components.rememberDominantColor
import com.example.contactsapp.presentation.contract.ContactEvent
import com.example.contactsapp.presentation.contract.ContactState

@Composable
fun ContactDetailScreen(
    state: ContactState,
    onEvent: (ContactEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isDeleteConfirmationVisible by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Fix: Navigate back when shouldNavigateBack is true
    LaunchedEffect(state.shouldNavigateBack) {
        if (state.shouldNavigateBack) {
            onNavigateBack()
        }
    }

    val dominantColorState = rememberDominantColor(
        context = context,
        imageUri = state.selectedImageUri,
        defaultColor = Color.Transparent
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onEvent(ContactEvent.OnCheckLocalContactStatus)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onEvent(ContactEvent.OnImageUriSelected(it)) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) tempPhotoUri?.let { onEvent(ContactEvent.OnImageUriSelected(it)) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createTempPhotoUri(context)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            onEvent(ContactEvent.OnSaveToLocalPhone(context))
        }
    }

    if (state.isImagePickerBottomSheetOpen) {
        ImageSourceDialog(
            onDismissRequest = { onEvent(ContactEvent.OnDismissImagePickerBottomSheet) },
            onCameraClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onGalleryClick = { galleryLauncher.launch("image/*") }
        )
    }

    if (isDeleteConfirmationVisible) {
        DeleteConfirmationSheet(
            onDismiss = { isDeleteConfirmationVisible = false },
            onConfirm = {
                isDeleteConfirmationVisible = false
                onEvent(ContactEvent.OnDeleteContact)
            }
        )
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            if (state.isEditMode) {
                EditModeTopBar(
                    onCancel = { state.selectedContact?.let { onEvent(ContactEvent.OnContactSelected(it)) } },
                    onDone = { onEvent(ContactEvent.OnUpdateContact) }
                )
            } else {
                ViewModeTopBar(
                    onBack = onNavigateBack,
                    isMenuExpanded = state.isMenuExpanded,
                    onMenuToggle = { onEvent(ContactEvent.OnMenuExpandedChanged(it)) },
                    onEditClick = { onEvent(ContactEvent.OnToggleEditMode) },
                    onRequestDelete = { isDeleteConfirmationVisible = true }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                val showGradient = state.selectedImageUri != null && dominantColorState.value != Color.Transparent

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .then(
                            if (showGradient) {
                                Modifier.background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            dominantColorState.value.copy(alpha = 0.65f),
                                            dominantColorState.value.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                )
                            } else Modifier
                        )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onEvent(ContactEvent.OnAddPhotoClicked) }
                    ) {
                        ProfileImagePicker(
                            imageUri = state.selectedImageUri,
                            onClick = { onEvent(ContactEvent.OnAddPhotoClicked) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Change Photo",
                    color = Color(0xFF007AFF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onEvent(ContactEvent.OnAddPhotoClicked) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (state.isEditMode) {
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
                } else {
                    ReadOnlyField(text = state.firstNameInput)
                    Spacer(modifier = Modifier.height(16.dp))
                    ReadOnlyField(text = state.lastNameInput)
                    Spacer(modifier = Modifier.height(16.dp))
                    ReadOnlyField(text = state.phoneNumberInput)

                    Spacer(modifier = Modifier.height(48.dp))

                    val isButtonEnabled = !state.isContactSaved
                    OutlinedButton(
                        onClick = {
                            if (!state.isContactSaved) {
                                contactPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CONTACTS,
                                        Manifest.permission.WRITE_CONTACTS
                                    )
                                )
                            }
                        },
                        enabled = isButtonEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(30.dp),
                        border = if (isButtonEnabled) BorderStroke(
                            1.dp,
                            Color.Black
                        ) else BorderStroke(1.dp, Color.LightGray),
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
            }

            if (state.globalSuccessMessage != null) {
                SuccessMessageOverlay(
                    message = state.globalSuccessMessage!!,
                    onDismiss = { onEvent(ContactEvent.OnClearGlobalSuccessMessage) }
                )
            }
        }
    }
}

// ... Rest of the helper composables (ViewModeTopBar, etc.) remain unchanged from previous version ...
@Composable
fun ViewModeTopBar(
    onBack: () -> Unit,
    isMenuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
        }
        Box {
            IconButton(onClick = { onMenuToggle(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.Black)
            }
            StyledDropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { onMenuToggle(false) },
                onEditClick = {
                    onMenuToggle(false)
                    onEditClick()
                },
                onDeleteClick = {
                    onMenuToggle(false)
                    onRequestDelete()
                }
            )
        }
    }
}

@Composable
fun StyledDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(16.dp))
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            offset = DpOffset(x = (-8).dp, y = 0.dp),
            modifier = Modifier
                .background(Color.White)
                .width(180.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Edit", fontSize = 16.sp) },
                onClick = onEditClick,
                trailingIcon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.Black
                    )
                },
                colors = MenuDefaults.itemColors(
                    textColor = Color.Black,
                    trailingIconColor = Color.Black
                )
            )
            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            DropdownMenuItem(
                text = { Text("Delete", color = Color(0xFFFF3B30), fontSize = 16.sp) },
                onClick = onDeleteClick,
                trailingIcon = {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFFF3B30)
                    )
                },
                colors = MenuDefaults.itemColors(
                    textColor = Color(0xFFFF3B30),
                    trailingIconColor = Color(0xFFFF3B30)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmationSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.LightGray)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Delete Contact",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Are you sure you want to delete this contact?",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, Color.Black),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text(text = "No", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1C1C1E),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Yes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EditModeTopBar(onCancel: () -> Unit, onDone: () -> Unit) {
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