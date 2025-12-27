package com.example.contactsapp.data.remote

import com.example.contactsapp.data.remote.dto.BaseResponse
import com.example.contactsapp.data.remote.dto.ContactRequest
import com.example.contactsapp.data.remote.dto.ImageUploadData
import com.example.contactsapp.data.remote.dto.UserListData
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ContactApi {
    @GET("api/User/GetAll")
    suspend fun getContacts(): Response<BaseResponse<UserListData>>

    @Multipart
    @POST("api/User/UploadImage")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<BaseResponse<ImageUploadData>>

    @POST("api/User")
    suspend fun saveContact(
        @Body request: ContactRequest
    ): Response<Unit>

    @PUT("api/User/{id}")
    suspend fun updateContact(
        @Path("id") id: String,
        @Body request: ContactRequest
    ): Response<BaseResponse<Any>>

    @DELETE("api/User/{id}")
    suspend fun deleteContact(
        @Path("id") id: String
    ): Response<BaseResponse<Any>>
}