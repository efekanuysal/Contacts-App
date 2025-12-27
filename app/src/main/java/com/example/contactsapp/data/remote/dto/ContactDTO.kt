package com.example.contactsapp.data.remote.dto

data class ContactRequest(
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val profileImageUrl: String
)

// Generic wrapper to match the server response structure
data class BaseResponse<T>(
    val success: Boolean,
    val messages: List<String>?,
    val data: T?,
    val status: Int
)

// The actual data inside the "data" field
data class ImageUploadData(
    val imageUrl: String
)

data class UserListData(
    val users: List<UserDto>
)

data class UserDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val profileImageUrl: String?
)