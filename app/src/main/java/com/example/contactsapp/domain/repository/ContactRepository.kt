package com.example.contactsapp.domain.repository

import android.net.Uri
import com.example.contactsapp.domain.model.Contact



interface ContactRepository {
    suspend fun getContacts(): Result<List<Contact>>
    suspend fun saveContact(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        imageUri: Uri?
    ): Result<Unit>
    suspend fun updateContact(id: String, firstName: String, lastName: String, phoneNumber: String, imageUri: Uri?): Result<Unit>
    suspend fun deleteContact(id: String): Result<Unit>

    fun isContactStoredInPhone(phoneNumber: String): Boolean
}