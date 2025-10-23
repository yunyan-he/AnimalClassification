package com.example.animalclaasification

/**
 * Data class for building the request body to send to Gemini.
 */
data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

data class Part(
    // Either text or inlineData will be present
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String = "image/jpeg", // Or "image/png"
    val data: String // Base64 encoded image string
)

