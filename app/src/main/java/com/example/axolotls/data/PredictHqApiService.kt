package com.example.axolotls.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for the PredictHQ Events API.
 * Docs: https://docs.predicthq.com/api/events/search-events
 */
interface PredictHqApiService {

    /**
     * Search for events by category and location radius.
     *
     * @param category  Comma-separated categories (e.g. "school-holidays,public-holidays,severe-weather")
     * @param within    Geo-radius filter (e.g. "50km@49.8951,-97.1384")
     * @param activeGte Start of date range (ISO 8601, e.g. "2026-04-26")
     * @param activeLte End of date range (ISO 8601, e.g. "2026-09-30")
     * @param sort      Sort order (e.g. "start")
     * @param limit     Max results per page (max 500)
     */
    @GET("v1/events/")
    suspend fun searchEvents(
        @Query("category") category: String,
        @Query("within") within: String,
        @Query("active.gte") activeGte: String,
        @Query("active.lte") activeLte: String,
        @Query("sort") sort: String = "start",
        @Query("limit") limit: Int = 50
    ): PredictHqResponse

    companion object {
        private const val BASE_URL = "https://api.predicthq.com/"

        fun create(apiToken: String): PredictHqApiService {
            val authInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $apiToken")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PredictHqApiService::class.java)
        }
    }
}
