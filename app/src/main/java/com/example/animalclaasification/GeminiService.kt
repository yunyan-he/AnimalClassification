package com.example.animalclaasification

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiService {

    /**
     * This is the explicit call to the free model.
     * The model name 'gemini-2.5-flash-preview-09-2025' is part of the URL.
     */
    @POST("v1beta/models/gemini-2.5-flash-preview-09-2025:generateContent")
    suspend fun generateContent(
        @Body body: GeminiRequest,
        @Query("key") apiKey: String // Your API key from Google AI Studio
    ): GeminiResponse
}
