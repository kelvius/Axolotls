package com.example.axolotls.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface WinnipegApiService {

    @GET("resource/it4w-cpf4.json")
    suspend fun getRecentPermits(
        @Query("\$limit") limit: Int = 20,
        @Query("\$order") order: String = "issue_date DESC",
        @Query("\$where") where: String = "issue_date IS NOT NULL"
    ): List<WinnipegPermit>

    companion object {
        private const val BASE_URL = "https://data.winnipeg.ca/"

        fun create(): WinnipegApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WinnipegApiService::class.java)
        }
    }
}
