package com.example.axolotls.data

import com.google.gson.annotations.SerializedName

/**
 * Models for Google Calendar API v3 Events list response.
 * Only the fields we need are mapped; the rest are ignored by Gson.
 */
data class GoogleCalendarResponse(
    @SerializedName("items") val items: List<GoogleCalendarEvent>? = null,
    @SerializedName("summary") val calendarName: String? = null
)

data class GoogleCalendarEvent(
    @SerializedName("id") val id: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("start") val start: GoogleDateTime? = null,
    @SerializedName("end") val end: GoogleDateTime? = null,
    @SerializedName("htmlLink") val htmlLink: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("organizer") val organizer: GoogleOrganizer? = null
)

data class GoogleDateTime(
    /** For timed events */
    @SerializedName("dateTime") val dateTime: String? = null,
    /** For all-day events */
    @SerializedName("date") val date: String? = null,
    @SerializedName("timeZone") val timeZone: String? = null
) {
    /** Returns whichever is available: dateTime or date */
    val raw: String? get() = dateTime ?: date
}

data class GoogleOrganizer(
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("email") val email: String? = null
)
