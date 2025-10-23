package com.example.animalclaasification

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.animalclaasification.BuildConfig


interface BaikeIntroService {
    @GET("baikebaidu.php")
    fun fetchBaikeIntro(
        @Query("id") id: String = "10002800",  // 固定 ID
        @Query("key") key: String = BuildConfig.BAIKE_API_KEY,  // API Key
        @Query("words") animalType: String // 需要查询的动物类型
    ): Call<BaikeIntroResponse>
}
