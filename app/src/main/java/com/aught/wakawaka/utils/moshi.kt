package com.aught.wakawaka.utils

import com.squareup.moshi.Moshi

fun getMoshi(): Moshi {
    // Create and return a Moshi instance with the JSONDateAdapter and KotlinJsonAdapterFactory
    return Moshi.Builder()
        .add(JSONDateAdapter())
        .build()
}
