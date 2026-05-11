package com.example.axolotls.data

import com.example.axolotls.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Fetches nearby parks, cafes, and landmarks from Google Places API (New)
 * and maps them into CommunityEvent objects for the unified event feed.
 */
class PlacesRepository(
    private val api: PlacesApiService = PlacesApiService.create()
) {
    companion object {
        private val API_KEY: String get() = BuildConfig.MAPS_API_KEY

        /** Search radius in meters (5km) */
        private const val SEARCH_RADIUS = 5000.0

        /** Max results per place type query */
        private const val MAX_RESULTS = 10

        /** Place types to search for parks and outdoor spots */
        private val PARK_TYPES = listOf("park", "national_park")

        /** Place types to search for cafes and coffee shops */
        private val CAFE_TYPES = listOf("cafe", "coffee_shop")

        /**
         * Map a place primaryType to a user-friendly source label.
         */
        private fun sourceLabel(primaryType: String?): String {
            return when (primaryType) {
                "park", "national_park" -> "Nearby Park"
                "cafe", "coffee_shop" -> "Nearby Cafe"
                else -> "Google Places"
            }
        }

        /**
         * Build a short description from place details.
         */
        private fun buildDescription(place: PlaceResult): String {
            val parts = mutableListOf<String>()

            // Editorial summary if available
            place.editorialSummary?.text?.let { parts.add(it) }

            // Rating info
            if (place.rating != null && place.userRatingCount != null) {
                parts.add("Rated ${place.rating}/5 (${place.userRatingCount} reviews)")
            }

            // Open now status
            val openNow = place.currentOpeningHours?.openNow
            if (openNow == true) {
                parts.add("Currently open")
            } else if (openNow == false) {
                parts.add("Currently closed")
            }

            return parts.joinToString(" - ").ifEmpty {
                "Discover this spot nearby"
            }
        }

        /**
         * Build opening hours string for display as startTime.
         */
        private fun buildTimeDisplay(place: PlaceResult): String {
            // Show today's hours if available
            val hours = place.currentOpeningHours ?: place.regularOpeningHours
            val today = Calendar.getInstance()
            val dayOfWeek = today.get(Calendar.DAY_OF_WEEK) // Sunday = 1, Saturday = 7
            // weekdayDescriptions is Monday-indexed (0=Monday)
            val descIndex = when (dayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
            val todayHours = hours?.weekdayDescriptions?.getOrNull(descIndex)
            if (todayHours != null) {
                return todayHours
            }

            // Fallback to today's date
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US)
            return sdf.format(today.time)
        }
    }

    /**
     * Fetches nearby parks and cafes around the given coordinates.
     * Returns them as CommunityEvent objects ready to merge into the feed.
     */
    suspend fun getNearbySpots(
        lat: Double,
        lng: Double,
        radiusMeters: Double = SEARCH_RADIUS
    ): List<CommunityEvent> = withContext(Dispatchers.IO) {
        try {
            val center = LatLng(latitude = lat, longitude = lng)

            // Fetch parks and cafes in parallel
            val parksDeferred = async {
                fetchPlaces(center, PARK_TYPES, radiusMeters)
            }
            val cafesDeferred = async {
                fetchPlaces(center, CAFE_TYPES, radiusMeters)
            }

            val parks = parksDeferred.await()
            val cafes = cafesDeferred.await()

            (parks + cafes)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Executes a single Nearby Search request for the given place types.
     */
    private suspend fun fetchPlaces(
        center: LatLng,
        types: List<String>,
        radiusMeters: Double
    ): List<CommunityEvent> {
        return try {
            val request = NearbySearchRequest(
                includedTypes = types,
                maxResultCount = MAX_RESULTS,
                locationRestriction = LocationRestriction(
                    circle = CircleRestriction(
                        center = center,
                        radius = radiusMeters
                    )
                )
            )

            val response = api.searchNearby(
                apiKey = API_KEY,
                request = request
            )

            response.places?.mapNotNull { place ->
                val name = place.displayName?.text ?: return@mapNotNull null
                val placeId = place.id ?: return@mapNotNull null

                CommunityEvent(
                    id = "places_$placeId",
                    title = name,
                    description = buildDescription(place),
                    location = place.shortFormattedAddress
                        ?: place.formattedAddress
                        ?: "",
                    startTime = buildTimeDisplay(place),
                    endTime = "",
                    calendarName = sourceLabel(place.primaryType),
                    isAllDay = false,
                    htmlLink = place.googleMapsUri,
                    latitude = place.location?.latitude,
                    longitude = place.location?.longitude
                )
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
