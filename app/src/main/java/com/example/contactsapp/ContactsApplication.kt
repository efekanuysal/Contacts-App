package com.example.contactsapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.contactsapp.data.remote.RetrofitClient

class ContactsApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // 3. Force Coil to use our Unsafe OkHttp Client
            .okHttpClient { RetrofitClient.unsafeOkHttpClient }
            .crossfade(true)
            .build()
    }
}