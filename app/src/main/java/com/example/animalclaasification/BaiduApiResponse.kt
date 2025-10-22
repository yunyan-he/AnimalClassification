package com.example.animalclaasification
data class BaiduResponse(
    val log_id: Long,
    val result: List<RecognitionResult>
)

data class RecognitionResult(
    val score: String,
    val name: String,
    val baike_info: BaikeInfo?
)

data class BaikeInfo(
    val baike_url: String?,
    val description: String?,
    val image_url: String?
)
