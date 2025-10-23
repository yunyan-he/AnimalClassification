package com.example.animalclaasification

import com.google.gson.annotations.SerializedName

/**
 * Data model for the response from Wikipedia's page/summary API.
 * We only care about the 'extract' field.
 */
data class WikiIntroResponse(
        @SerializedName("extract") val extract: String?
)