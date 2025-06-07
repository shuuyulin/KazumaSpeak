package com.example.japaneselearning.api

import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {
    // CORRECT: Based on official documentation
    private const val BASE_URL = "https://api.romaji2kana.com/"
    
    fun createApiService(cacheDir: File): JapaneseTextApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // Reduced logging for production
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS) // Reduced timeout
            .readTimeout(15, TimeUnit.SECONDS)    // Reduced timeout
            .writeTimeout(15, TimeUnit.SECONDS)   // Reduced timeout
            .addInterceptor(logging)
            .cache(Cache(File(cacheDir, "http_cache"), 10L * 1024L * 1024L)) // 10MB cache
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(JapaneseTextApiService::class.java)
    }
}