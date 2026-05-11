package com.example.axolotls.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit service for the Google Places API (New) - Nearby Search.
 * Docs: https://developers.google.com/maps/documentation/places/web-service/nearby-search
 *
 * Uses POST with a JSON body and API key + field mask in headers.
 */
interface PlacesApiService {

    @POST("v1/places:searchNearby")
    suspend fun searchNearby(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String = FIELD_MASK,
        @Body request: NearbySearchRequest
    ): NearbySearchResponse

    companion object {
        private const val BASE_URL = "https://places.googleapis.com/"

        /**
         * Fields to request from the API.
         * Each field adds to billing, so we only request what we need.
         */
        private const val FIELD_MASK = "places.id," +
                "places.displayName," +
                "places.formattedAddress," +
                "places.shortFormattedAddress," +
                "places.location," +
                "places.types," +
                "places.primaryType," +
                "places.editorialSummary," +
                "places.currentOpeningHours," +
                "places.regularOpeningHours," +
                "places.rating," +
                "places.userRatingCount," +
                "places.googleMapsUri"

        fun create(): PlacesApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlacesApiService::class.java)
        }
    }
}
