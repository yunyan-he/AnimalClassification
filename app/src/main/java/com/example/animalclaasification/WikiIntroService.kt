
package com.example.animalclaasification

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface WikiIntroService {
    /**
     * Fetches the summary for a given page title (animal name).
     * Example URL: https://en.wikipedia.org/api/rest_v1/page/summary/Lion
     */
    @GET("page/summary/{animalName}")
    fun fetchSummary(
        @Path("animalName") animalName: String // The name of the animal to query
    ): Call<WikiIntroResponse>
}