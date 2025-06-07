package com.example.japaneselearning.utils

sealed class LoadingState<T> {
    class Loading<T> : LoadingState<T>()
    data class Success<T>(val data: T) : LoadingState<T>()
    data class Error<T>(val message: String, val fallback: T? = null) : LoadingState<T>()
}