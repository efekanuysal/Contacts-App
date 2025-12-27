package com.example.contactsapp.domain.model

import android.net.Uri

data class Contact(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val photoUri: Uri?,
    val isSavedLocally: Boolean = false
)