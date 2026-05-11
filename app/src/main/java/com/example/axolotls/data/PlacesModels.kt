package com.example.axolotls.data

import com.google.gson.annotations.SerializedName

/**
 * Request body for Places API (New) Nearby Search.
 * Docs: https://developers.google.com/maps/documentation/places/web-service/nearby-search
 */
data class NearbySearchRequest(
    @SerializedName("includedTypes") val includedTypes: List<String>,
    @SerializedName("maxResultCount") val maxResultCount: Int = 10,
    @SerializedName("locationRestriction") val locationRestriction: LocationRestriction
)

data class LocationRestriction(
    @SerializedName("circle") val circle: CircleRestriction
)

data class CircleRestriction(
    @SerializedName("center") val center: LatLng,
    @SerializedName("radius") val radius: Double
)

data class LatLng(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

/**
 * Response from Places API (New) Nearby Search.
 */
data class NearbySearchResponse(
    @SerializedName("places") val places: List<PlaceResult>? = null
)

data class PlaceResult(
    @SerializedName("id") val id: String? = null,
    @SerializedName("displayName") val displayName: PlaceDisplayName? = null,
    @SerializedName("formattedAddress") val formattedAddress: String? = null,
    @SerializedName("shortFormattedAddress") val shortFormattedAddress: String? = null,
    @SerializedName("location") val location: PlaceLocation? = null,
    @SerializedName("types") val types: List<String>? = null,
    @SerializedName("primaryType") val primaryType: String? = null,
    @SerializedName("editorialSummary") val editorialSummary: PlaceEditorialSummary? = null,
    @SerializedName("currentOpeningHours") val currentOpeningHours: PlaceOpeningHours? = null,
    @SerializedName("regularOpeningHours") val regularOpeningHours: PlaceOpeningHours? = null,
    @SerializedName("rating") val rating: Double? = null,
    @SerializedName("userRatingCount") val userRatingCount: Int? = null,
    @SerializedName("googleMapsUri") val googleMapsUri: String? = null
)

data class PlaceDisplayName(
    @SerializedName("text") val text: String? = null,
    @SerializedName("languageCode") val languageCode: String? = null
)

data class PlaceLocation(
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class PlaceEditorialSummary(
    @SerializedName("text") val text: String? = null
)

data class PlaceOpeningHours(
    @SerializedName("openNow") val openNow: Boolean? = null,
    @SerializedName("weekdayDescriptions") val weekdayDescriptions: List<String>? = null
)
