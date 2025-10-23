package com.example.animalclaasification

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BaikeIntroClient {
    private const val BASE_URL = "https://cn.apihz.cn/api/zici/"
    val api: BaikeIntroService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BaikeIntroService::class.java)
    }
}
