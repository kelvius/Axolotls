package com.example.axolotls.data

import com.example.axolotls.ui.screens.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class EventRepository(
    private val api: WinnipegApiService = WinnipegApiService.create()
) {
    /**
     * Fetches recent permits and optionally sorts them by distance from [userLat]/[userLng].
     * If user location is null, events are returned in default order (most recent first).
     */
    suspend fun getRecentEvents(
        userLat: Double? = null,
        userLng: Double? = null
    ): Result<List<Event>> = withContext(Dispatchers.IO) {
        try {
            val permits = api.getRecentPermits()
            var events = permits.mapIndexed { index, permit ->
                val lat = permit.point?.latitude
                val lng = permit.point?.longitude
                val distance = if (userLat != null && userLng != null && lat != null && lng != null) {
                    haversineKm(userLat, userLng, lat, lng)
                } else {
                    null
                }
                Event(
                    id = index + 1000,
                    title = buildTitle(permit),
                    date = formatDate(permit.issueDate),
                    location = permit.address ?: "Winnipeg",
                    description = buildDescription(permit),
                    latitude = lat,
                    longitude = lng,
                    distanceKm = distance
                )
            }
            // Sort by distance (nearest first) when location is available
            if (userLat != null && userLng != null) {
                events = events.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
            }
            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildTitle(permit: WinnipegPermit): String {
        val workType = permit.workType ?: "Development"
        val subType = permit.subType ?: permit.permitType ?: "Project"
        return "$workType - $subType"
    }

    private fun buildDescription(permit: WinnipegPermit): String {
        val parts = mutableListOf<String>()
        permit.permitGroup?.let { parts.add(it) }
        permit.neighbourhoodName?.let { parts.add(it) }
        permit.community?.let { parts.add("Community: $it") }
        if (permit.majorProject == "Yes") parts.add("Major Project")
        permit.applicantBusinessName?.let { parts.add("By: $it") }
        return parts.joinToString(" | ").ifEmpty { "City of Winnipeg permit" }
    }

    private fun formatDate(isoDate: String?): String {
        if (isoDate == null) return "Date TBD"
        return try {
            val parts = isoDate.split("T")[0].split("-")
            val months = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            val month = months[parts[1].toInt() - 1]
            "$month ${parts[2].toInt()}, ${parts[0]}"
        } catch (_: Exception) {
            isoDate.take(10)
        }
    }

    /**
     * Haversine formula to calculate the distance in km between two lat/lng points.
     */
    private fun haversineKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
