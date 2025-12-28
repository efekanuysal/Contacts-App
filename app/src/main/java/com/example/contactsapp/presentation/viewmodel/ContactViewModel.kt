package com.example.contactsapp.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.contactsapp.data.remote.RetrofitClient
import com.example.contactsapp.data.repository.ContactRepositoryImpl
import com.example.contactsapp.domain.repository.ContactRepository
import com.example.contactsapp.presentation.contract.ContactEvent
import com.example.contactsapp.presentation.contract.ContactState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ContactRepository = ContactRepositoryImpl(
        api = RetrofitClient.api,
        context = application.applicationContext
    )

    private val prefs: SharedPreferences = application.getSharedPreferences("contacts_app_prefs", Context.MODE_PRIVATE)
    private val HISTORY_KEY = "search_history"

    private val _state = MutableStateFlow(ContactState())
    val state: StateFlow<ContactState> = _state.asStateFlow()

    init {
        loadSearchHistory()
        fetchContacts()
    }

    private fun loadSearchHistory() {
        val historySet = prefs.getStringSet(HISTORY_KEY, emptySet()) ?: emptySet()
        _state.update { it.copy(previousSearches = historySet.toList().sorted()) }
    }

    private fun saveSearchHistory(history: List<String>) {
        prefs.edit().putStringSet(HISTORY_KEY, history.toSet()).apply()
        _state.update { it.copy(previousSearches = history) }
    }

    private fun fetchContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.getContacts()
            if (result.isSuccess) {
                _state.update { it.copy(contacts = result.getOrDefault(emptyList())) }
            }
        }
    }

    fun onEvent(event: ContactEvent) {
        when (event) {
            is ContactEvent.OnSearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
            }
            is ContactEvent.OnSearchFocusChanged -> {
                _state.update { it.copy(isSearchActive = event.isFocused) }
            }
            is ContactEvent.OnRemoveSearchHistoryItem -> {
                val current = _state.value.previousSearches.toMutableList()
                current.remove(event.query)
                saveSearchHistory(current)
            }
            ContactEvent.OnClearSearchHistory -> {
                saveSearchHistory(emptyList())
            }
            is ContactEvent.OnFirstNameChanged -> {
                _state.update { it.copy(firstNameInput = event.value) }
            }
            is ContactEvent.OnLastNameChanged -> {
                _state.update { it.copy(lastNameInput = event.value) }
            }
            is ContactEvent.OnPhoneNumberChanged -> {
                _state.update { it.copy(phoneNumberInput = event.value) }
            }
            ContactEvent.OnAddPhotoClicked -> {
                _state.update { it.copy(isImagePickerBottomSheetOpen = true) }
            }
            ContactEvent.OnDismissImagePickerBottomSheet -> {
                _state.update { it.copy(isImagePickerBottomSheetOpen = false) }
            }
            is ContactEvent.OnImageUriSelected -> {
                _state.update {
                    it.copy(
                        selectedImageUri = event.uri,
                        isImagePickerBottomSheetOpen = false
                    )
                }
                if (_state.value.selectedContact != null) {
                    updateContact()
                }
            }
            ContactEvent.OnSaveContact -> {
                saveContact()
            }
            ContactEvent.OnResetSaveState -> {
                _state.update { it.copy(isContactSaved = false) }
            }
            is ContactEvent.OnContactSelected -> {
                if (_state.value.isSearchActive && _state.value.searchQuery.isNotBlank()) {
                    val currentHistory = _state.value.previousSearches.toMutableList()
                    val query = _state.value.searchQuery.trim()
                    if (!currentHistory.contains(query)) {
                        currentHistory.add(query)
                        saveSearchHistory(currentHistory)
                    }
                }

                _state.update {
                    it.copy(
                        selectedContact = event.contact,
                        firstNameInput = event.contact.firstName,
                        lastNameInput = event.contact.lastName,
                        phoneNumberInput = event.contact.phoneNumber,
                        selectedImageUri = event.contact.photoUri,
                        isEditMode = false,
                        successMessage = null,
                        isMenuExpanded = false,
                        isContactSaved = false
                    )
                }
            }
            ContactEvent.OnAddNewContact -> {
                _state.update {
                    it.copy(
                        selectedContact = null,
                        firstNameInput = "",
                        lastNameInput = "",
                        phoneNumberInput = "",
                        selectedImageUri = null,
                        isEditMode = false,
                        isContactSaved = false,
                        errorMessage = null,
                        successMessage = null
                    )
                }
            }
            ContactEvent.OnCheckLocalContactStatus -> {
                val phoneNumber = _state.value.selectedContact?.phoneNumber ?: return
                viewModelScope.launch(Dispatchers.IO) {
                    val exists = repository.isContactStoredInPhone(phoneNumber)
                    _state.update { it.copy(isContactSaved = exists) }
                }
            }
            is ContactEvent.OnToggleEditMode -> {
                _state.update { it.copy(isEditMode = true, isMenuExpanded = false) }
            }
            is ContactEvent.OnMenuExpandedChanged -> {
                _state.update { it.copy(isMenuExpanded = event.isExpanded) }
            }
            ContactEvent.OnUpdateContact -> {
                updateContact()
            }
            ContactEvent.OnDeleteContact -> {
                deleteContact()
            }
            is ContactEvent.OnSaveToLocalPhone -> {
                saveToLocalPhone(event.context)
            }
            ContactEvent.OnClearSuccessMessage -> {
                _state.update { it.copy(successMessage = null) }
            }
            ContactEvent.OnClearGlobalSuccessMessage -> {
                _state.update { it.copy(globalSuccessMessage = null) }
            }
            else -> handleOtherEvents(event)
        }
    }

    private fun handleOtherEvents(event: ContactEvent) {
        when (event) {
            ContactEvent.OnDismissImagePickerBottomSheet -> {
                _state.update { it.copy(isImagePickerBottomSheetOpen = false) }
            }
            else -> {}
        }
    }

    private fun saveContact() {
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        val currentState = _state.value

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.saveContact(
                    firstName = currentState.firstNameInput,
                    lastName = currentState.lastNameInput,
                    phoneNumber = currentState.phoneNumberInput,
                    imageUri = currentState.selectedImageUri
                )

                if (result.isSuccess) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isContactSaved = true,
                            firstNameInput = "",
                            lastNameInput = "",
                            phoneNumberInput = "",
                            selectedImageUri = null,
                            globalSuccessMessage = "New contact saved \uD83C\uDF89"
                        )
                    }
                    fetchContacts()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown Error"
                    Log.e("API_ERROR", "Request Failed: $error")
                    _state.update { it.copy(isLoading = false, errorMessage = error) }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Crash: ${e.message}")
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun updateContact() {
        val contact = _state.value.selectedContact ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.updateContact(
                id = contact.id,
                firstName = _state.value.firstNameInput,
                lastName = _state.value.lastNameInput,
                phoneNumber = _state.value.phoneNumberInput,
                imageUri = _state.value.selectedImageUri
            )
            if (result.isSuccess) {
                _state.update { it.copy(isEditMode = false, successMessage = "User is Updated!") }
                fetchContacts()
            }
        }
    }

    private fun deleteContact() {
        val contact = _state.value.selectedContact ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.deleteContact(contact.id)
            if (result.isSuccess) {
                _state.update { it.copy(shouldNavigateBack = true, globalSuccessMessage = "Contact deleted!") }
                fetchContacts()
            }
        }
    }

    private fun saveToLocalPhone(context: Context) {
        val contact = _state.value.selectedContact ?: return
        try {
            val ops = ArrayList<android.content.ContentProviderOperation>()

            ops.add(android.content.ContentProviderOperation.newInsert(android.provider.ContactsContract.RawContacts.CONTENT_URI)
                .withValue(android.provider.ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(android.provider.ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build())

            ops.add(android.content.ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(android.provider.ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(android.provider.ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contact.firstName)
                .withValue(android.provider.ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contact.lastName)
                .build())

            ops.add(android.content.ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(android.provider.ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phoneNumber)
                .withValue(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE, android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build())

            context.contentResolver.applyBatch(android.provider.ContactsContract.AUTHORITY, ops)
            _state.update { it.copy(globalSuccessMessage = "User is added to your phone!", isContactSaved = true) }
            fetchContacts()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}