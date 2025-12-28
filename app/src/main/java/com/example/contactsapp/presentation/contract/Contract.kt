package com.example.contactsapp.presentation.contract

import android.content.Context
import android.net.Uri
import com.example.contactsapp.domain.model.Contact

data class ContactState(
    val contacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val previousSearches: List<String> = emptyList(),
    val isSearchActive: Boolean = false,
    val firstNameInput: String = "",
    val lastNameInput: String = "",
    val phoneNumberInput: String = "",
    val selectedImageUri: Uri? = null,
    val isImagePickerBottomSheetOpen: Boolean = false,
    val isContactSaved: Boolean = false,
    val selectedContact: Contact? = null,
    val isEditMode: Boolean = false,
    val isMenuExpanded: Boolean = false,
    val successMessage: String? = null,
    val shouldNavigateBack: Boolean = false,
    val globalSuccessMessage: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface ContactEvent {
    data class OnSearchQueryChanged(val query: String) : ContactEvent
    data class OnSearchFocusChanged(val isFocused: Boolean) : ContactEvent
    data class OnRemoveSearchHistoryItem(val query: String) : ContactEvent
    data object OnClearSearchHistory : ContactEvent
    data class OnFirstNameChanged(val value: String) : ContactEvent
    data class OnLastNameChanged(val value: String) : ContactEvent
    data class OnPhoneNumberChanged(val value: String) : ContactEvent
    data object OnAddPhotoClicked : ContactEvent
    data object OnDismissImagePickerBottomSheet : ContactEvent
    data class OnImageUriSelected(val uri: Uri?) : ContactEvent
    data object OnSaveContact : ContactEvent
    data object OnResetSaveState : ContactEvent
    data class OnContactSelected(val contact: Contact) : ContactEvent
    data object OnAddNewContact : ContactEvent
    data object OnToggleEditMode : ContactEvent
    data object OnUpdateContact : ContactEvent
    data object OnDeleteContact : ContactEvent
    data class OnSaveToLocalPhone(val context: Context) : ContactEvent
    data object OnClearSuccessMessage : ContactEvent
    data class OnMenuExpandedChanged(val isExpanded: Boolean) : ContactEvent
    data object OnCheckLocalContactStatus : ContactEvent
    data object OnClearGlobalSuccessMessage : ContactEvent
}