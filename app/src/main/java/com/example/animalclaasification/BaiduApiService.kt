package com.example.animalclaasification
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface BaiduApiService {
    @FormUrlEncoded
    @POST("rest/2.0/image-classify/v1/animal")
    fun recognizeAnimal(
        @Field("access_token") accessToken: String,
        @Field("image") imageBase64: String,
        @Field("top_num") topNum: Int = 1,
        @Field("baike_num") baikeNum: Int = 1
    ): Call<BaiduResponse>
}
