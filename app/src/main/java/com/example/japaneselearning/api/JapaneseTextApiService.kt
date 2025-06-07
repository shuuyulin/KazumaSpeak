package com.example.japaneselearning.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface JapaneseTextApiService {
    @GET("v1/to/hiragana")
    suspend fun convertToHiragana(@Query("q") text: String): Response<ConversionResponse>
    
    @GET("v1/to/katakana")
    suspend fun convertToKatakana(@Query("q") text: String): Response<ConversionResponse>
    
    @GET("v1/to/romaji")
    suspend fun convertToRomaji(@Query("q") text: String): Response<ConversionResponse>
    
    @GET("v1/to/kana")
    suspend fun convertToMixedKana(@Query("q") text: String): Response<ConversionResponse>
    
    @GET("v1/is/japanese")
    suspend fun validateJapanese(@Query("q") text: String): Response<ValidationResponse>
}

data class ConversionResponse(val a: String)
data class ValidationResponse(val result: Boolean)