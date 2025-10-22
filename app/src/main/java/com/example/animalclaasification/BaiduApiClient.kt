package com.example.animalclaasification


import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BaiduApiClient {
    private const val BASE_URL = "https://aip.baidubce.com/"

    val api: BaiduApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BaiduApiService::class.java)
    }
}
