package com.example.contactsapp.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.example.contactsapp.data.remote.ContactApi
import com.example.contactsapp.data.remote.dto.ContactRequest
import com.example.contactsapp.domain.model.Contact
import com.example.contactsapp.domain.repository.ContactRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ContactRepositoryImpl(
    private val api: ContactApi,
    private val context: Context
) : ContactRepository {

    override suspend fun getContacts(): Result<List<Contact>> {
        return try {
            val response = api.getContacts()
            if (response.isSuccessful && response.body()?.success == true) {
                val userDtos = response.body()?.data?.users ?: emptyList()
                val contacts = userDtos.map { dto ->
                    Contact(
                        id = dto.id,
                        firstName = dto.firstName,
                        lastName = dto.lastName,
                        phoneNumber = dto.phoneNumber,
                        photoUri = if (!dto.profileImageUrl.isNullOrEmpty()) Uri.parse(dto.profileImageUrl) else null,
                        // Check local storage for each contact
                        isSavedLocally = isContactStoredInPhone(dto.phoneNumber)
                    )
                }
                Result.success(contacts)
            } else {
                Result.failure(Exception("Failed to fetch contacts: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveContact(
        firstName: String,
        lastName: String,
        phoneNumber: String,
        imageUri: Uri?
    ): Result<Unit> {
        return try {
            val imageUrl = if (imageUri != null) {
                uploadImage(imageUri) ?: return Result.failure(Exception("Image upload failed or returned empty URL"))
            } else {
                ""
            }

            val request = ContactRequest(
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                profileImageUrl = imageUrl
            )

            val response = api.saveContact(request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to save contact: ${response.code()}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun uploadImage(uri: Uri): String? {
        val mimeType = getMimeType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        val file = uriToFile(uri, extension) ?: return null

        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        try {
            val response = api.uploadImage(body)

            // Check HTTP success AND API logical success
            if (response.isSuccessful && response.body()?.success == true) {
                return response.body()?.data?.imageUrl
            } else {
                println("Upload failed: HTTP ${response.code()} / API Success: ${response.body()?.success}")
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun uriToFile(uri: Uri, extension: String): File? {
        val contentResolver = context.contentResolver
        val tempFile = File.createTempFile("upload_${System.currentTimeMillis()}", ".$extension", context.cacheDir)

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getMimeType(uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "image/jpeg"
    }
    override suspend fun updateContact(
        id: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        imageUri: Uri?
    ): Result<Unit> {
        return try {
            // Logic: If URI is http, it's an existing remote URL, reuse it.
            // If it's a file/content URI, upload it first.
            val imageUrl = if (imageUri != null && imageUri.scheme?.startsWith("http") == false) {
                uploadImage(imageUri) ?: return Result.failure(Exception("Image upload failed"))
            } else {
                imageUri?.toString() ?: ""
            }

            val request = ContactRequest(firstName, lastName, phoneNumber, imageUrl)
            val response = api.updateContact(id, request)

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Update failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteContact(id: String): Result<Unit> {
        return try {
            val response = api.deleteContact(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isContactStoredInPhone(phoneNumber: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false
        }

        // Use Uri.encode to handle special characters like '#' or '+' correctly
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup._ID)

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                return cursor.count > 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}