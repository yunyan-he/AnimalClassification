package com.example.animalclaasification

/**
 * Data class for parsing the response from Gemini.
 * We only care about the text in the first candidate's first part.
 */
data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)
// We can reuse the 'Content' and 'Part' classes from the Request
