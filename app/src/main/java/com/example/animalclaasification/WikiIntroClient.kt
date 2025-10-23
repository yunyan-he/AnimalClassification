package com.example.animalclaasification

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WikiIntroClient {
    // The base URL for the Wikipedia REST API
    private const val BASE_URL = "https://en.wikipedia.org/api/rest_v1/"

    // Create an OkHttpClient that adds the required User-Agent header
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            // Get the original request
            val originalRequest = chain.request()

            // Build a new request and add the User-Agent header
            val requestWithUserAgent = originalRequest.newBuilder()
                // We use your app's logical name and your provided contact email
                .header(
                    "User-Agent",
                    "AnimalClassificationApp/1.0 (3122863554@qq.com)"
                )
                .build()

            // Proceed with the new request
            chain.proceed(requestWithUserAgent)
        }
        .build()

    // Build Retrofit and tell it to use your custom OkHttpClient
    val api: WikiIntroService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // <-- This applies the User-Agent to all requests
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WikiIntroService::class.java)
    }
}
