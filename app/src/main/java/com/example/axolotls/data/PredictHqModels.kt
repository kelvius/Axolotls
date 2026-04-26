package com.example.axolotls.data

import com.google.gson.annotations.SerializedName

/**
 * PredictHQ Events API response models.
 * Used for school holidays, public holidays, and severe weather events.
 * API Docs: https://docs.predicthq.com/api/events/search-events
 */

data class PredictHqResponse(
    @SerializedName("count") val count: Int = 0,
    @SerializedName("results") val results: List<PredictHqEvent>? = null,
    @SerializedName("next") val next: String? = null
)

data class PredictHqEvent(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("labels") val labels: List<String>? = null,
    @SerializedName("start") val start: String = "",        // ISO 8601 datetime
    @SerializedName("end") val end: String = "",             // ISO 8601 datetime
    @SerializedName("duration") val duration: Long = 0,      // seconds
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("location") val location: List<Double>? = null, // [longitude, latitude]
    @SerializedName("country") val country: String = "",
    @SerializedName("state") val state: String? = null,
    @SerializedName("rank") val rank: Int = 0,               // importance 0-100
    @SerializedName("scope") val scope: String? = null,
    @SerializedName("entities") val entities: List<PredictHqEntity>? = null
)

data class PredictHqEntity(
    @SerializedName("entity_id") val entityId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("type") val type: String = ""
)
