package com.example.axolotls.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleCalendarApiService {

    /**
     * Fetches upcoming events from a public Google Calendar.
     * No OAuth needed for public calendars -- just an API key.
     */
    @GET("calendars/{calendarId}/events")
    suspend fun getEvents(
        @Path("calendarId") calendarId: String,
        @Query("key") apiKey: String,
        @Query("timeMin") timeMin: String,
        @Query("maxResults") maxResults: Int = 25,
        @Query("singleEvents") singleEvents: Boolean = true,
        @Query("orderBy") orderBy: String = "startTime"
    ): GoogleCalendarResponse

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/calendar/v3/"

        fun create(): GoogleCalendarApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GoogleCalendarApiService::class.java)
        }
    }
}
