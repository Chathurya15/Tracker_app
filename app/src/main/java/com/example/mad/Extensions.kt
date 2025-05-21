package com.example.mad

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <T, K, R> LiveData<T>.map(transform: (T) -> R): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) { value -> result.value = transform(value) }
    return result
}