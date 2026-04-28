package com.example.axolotls.data

import com.example.axolotls.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents a community event from any source.
 */
data class CommunityEvent(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val startTime: String,
    val endTime: String,
    val calendarName: String,
    val isAllDay: Boolean,
    val htmlLink: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

/**
 * A public calendar source to fetch events from.
 */
data class CalendarSource(
    val calendarId: String,
    val displayName: String
)

class CalendarRepository(
    private val api: GoogleCalendarApiService = GoogleCalendarApiService.create(),
    private val predictHqRepo: PredictHqRepository = PredictHqRepository()
) {
    companion object {
        val API_KEY: String get() = BuildConfig.MAPS_API_KEY

        // Winnipeg coordinates for proximity check
        private const val WINNIPEG_LAT = 49.8951
        private const val WINNIPEG_LNG = -97.1384
        private const val WINNIPEG_RADIUS_KM = 100.0

        /**
         * Google Calendar public holiday calendar IDs by country.
         * Format: "en.{country}#holiday@group.v.calendar.google.com"
         * Full list: https://gist.github.com/nickvdyck/07f810a99e923a8dbd18
         */
        private val HOLIDAY_CALENDARS = mapOf(
            "CA" to CalendarSource("en.canadian#holiday@group.v.calendar.google.com", "Canadian Holidays"),
            "US" to CalendarSource("en.usa#holiday@group.v.calendar.google.com", "US Holidays"),
            "PH" to CalendarSource("en.philippines#holiday@group.v.calendar.google.com", "Philippine Holidays"),
            "GB" to CalendarSource("en.uk#holiday@group.v.calendar.google.com", "UK Holidays"),
            "AU" to CalendarSource("en.australian#holiday@group.v.calendar.google.com", "Australian Holidays"),
            "NZ" to CalendarSource("en.new_zealand#holiday@group.v.calendar.google.com", "New Zealand Holidays"),
            "IN" to CalendarSource("en.indian#holiday@group.v.calendar.google.com", "Indian Holidays"),
            "DE" to CalendarSource("en.german#holiday@group.v.calendar.google.com", "German Holidays"),
            "FR" to CalendarSource("en.french#holiday@group.v.calendar.google.com", "French Holidays"),
            "JP" to CalendarSource("en.japanese#holiday@group.v.calendar.google.com", "Japanese Holidays"),
            "KR" to CalendarSource("en.south_korea#holiday@group.v.calendar.google.com", "South Korean Holidays"),
            "MX" to CalendarSource("en.mexican#holiday@group.v.calendar.google.com", "Mexican Holidays"),
            "BR" to CalendarSource("en.brazilian#holiday@group.v.calendar.google.com", "Brazilian Holidays"),
            "IE" to CalendarSource("en.irish#holiday@group.v.calendar.google.com", "Irish Holidays"),
            "SG" to CalendarSource("en.singapore#holiday@group.v.calendar.google.com", "Singaporean Holidays"),
            "MY" to CalendarSource("en.malaysia#holiday@group.v.calendar.google.com", "Malaysian Holidays")
        )

        /** Always include these calendars regardless of location */
        private val UNIVERSAL_CALENDARS = listOf(
            CalendarSource("en.christian#holiday@group.v.calendar.google.com", "Christian Holidays")
        )

        /**
         * Rough country detection from lat/lng using bounding boxes.
         * Not precise, but good enough for picking the right holiday calendar.
         */
        fun detectCountry(lat: Double, lng: Double): String {
            return when {
                // Canada: lat 42-83, lng -141 to -52
                lat in 42.0..83.0 && lng in -141.0..-52.0 -> "CA"
                // USA (continental): lat 24-49, lng -125 to -66
                lat in 24.0..49.0 && lng in -125.0..-66.0 -> "US"
                // Alaska
                lat in 51.0..72.0 && lng in -180.0..-130.0 -> "US"
                // Hawaii
                lat in 18.0..23.0 && lng in -161.0..-154.0 -> "US"
                // Philippines: lat 4.5-21, lng 116-127
                lat in 4.5..21.0 && lng in 116.0..127.0 -> "PH"
                // UK: lat 49-61, lng -8 to 2
                lat in 49.0..61.0 && lng in -8.0..2.0 -> "GB"
                // Ireland: lat 51-56, lng -11 to -5.5
                lat in 51.0..56.0 && lng in -11.0..-5.5 -> "IE"
                // Australia: lat -44 to -10, lng 112-155
                lat in -44.0..-10.0 && lng in 112.0..155.0 -> "AU"
                // New Zealand: lat -47 to -34, lng 166-179
                lat in -47.0..-34.0 && lng in 166.0..179.0 -> "NZ"
                // India: lat 6-36, lng 68-97
                lat in 6.0..36.0 && lng in 68.0..97.0 -> "IN"
                // Japan: lat 24-46, lng 122-146
                lat in 24.0..46.0 && lng in 122.0..146.0 -> "JP"
                // South Korea: lat 33-39, lng 124-132
                lat in 33.0..39.0 && lng in 124.0..132.0 -> "KR"
                // Germany: lat 47-55, lng 5.5-15.5
                lat in 47.0..55.0 && lng in 5.5..15.5 -> "DE"
                // France: lat 41-51, lng -5.5 to 10
                lat in 41.0..51.0 && lng in -5.5..10.0 -> "FR"
                // Mexico: lat 14-33, lng -118 to -86
                lat in 14.0..33.0 && lng in -118.0..-86.0 -> "MX"
                // Brazil: lat -34 to 5, lng -74 to -34
                lat in -34.0..5.0 && lng in -74.0..-34.0 -> "BR"
                // Singapore: lat 1-2, lng 103-104
                lat in 1.0..2.0 && lng in 103.0..104.5 -> "SG"
                // Malaysia: lat 0-8, lng 99-120
                lat in 0.0..8.0 && lng in 99.0..120.0 -> "MY"
                // Default to US if we can't determine
                else -> "US"
            }
        }

        /** Haversine distance in km */
        fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLng / 2) * sin(dLng / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }

    /**
     * Gets the appropriate Google Calendar sources for the user's location.
     */
    private fun getCalendarSources(lat: Double, lng: Double): List<CalendarSource> {
        val country = detectCountry(lat, lng)
        val sources = mutableListOf<CalendarSource>()

        // Add country-specific holiday calendar
        HOLIDAY_CALENDARS[country]?.let { sources.add(it) }

        // Always add universal calendars
        sources.addAll(UNIVERSAL_CALENDARS)

        return sources
    }

    /**
     * Only include curated Winnipeg events if user is near Winnipeg.
     */
    private fun getCuratedEvents(lat: Double, lng: Double): List<CommunityEvent> {
        val distanceToWinnipeg = haversineKm(lat, lng, WINNIPEG_LAT, WINNIPEG_LNG)
        return if (distanceToWinnipeg <= WINNIPEG_RADIUS_KM) {
            getWinnipegEvents()
        } else {
            emptyList()
        }
    }

    /**
     * Curated Winnipeg community events (real events from Tourism Winnipeg & local orgs).
     * Only shown when user is within 100km of Winnipeg.
     */
    private fun getWinnipegEvents(): List<CommunityEvent> = listOf(
        CommunityEvent(
            id = "wpg_001",
            title = "Crying Over Spilt Tea - Exhibition",
            description = "Inspired by Black drag culture and British tea traditions, this exhibit explores identity, ritual, and storytelling through art at the Winnipeg Art Gallery.",
            location = "WAG-Qaumajuq, 300 Memorial Blvd, Winnipeg",
            startTime = "Apr 27, 2026",
            endTime = "Jul 5, 2026",
            calendarName = "Winnipeg Art Gallery",
            isAllDay = true
        ),
        CommunityEvent(
            id = "wpg_002",
            title = "Toddler Tuesdays at The Leaf",
            description = "Fun and educational activities for toddlers and parents at The Leaf in Assiniboine Park. Activities include nature exploration, sensory play, and guided tours.",
            location = "The Leaf, Assiniboine Park, Winnipeg",
            startTime = "Apr 28, 2026",
            endTime = "May 26, 2026",
            calendarName = "Assiniboine Park",
            isAllDay = true
        ),
        CommunityEvent(
            id = "wpg_003",
            title = "WAG Wednesday Nights",
            description = "After-hours adults-only experience at WAG-Qaumajuq with creativity, cocktails, and curated gallery access every Wednesday night.",
            location = "WAG-Qaumajuq, 300 Memorial Blvd, Winnipeg",
            startTime = "Apr 29, 2026 at 7:00 PM",
            endTime = "Apr 29, 2026 at 10:00 PM",
            calendarName = "Winnipeg Art Gallery",
            isAllDay = false
        ),
        CommunityEvent(
            id = "wpg_004",
            title = "Those Who Made The Winter - Walking Tour",
            description = "Indigenous-led walking tour at The Forks exploring winter survival traditions. Enjoy Labrador Tea while learning about the history of the land.",
            location = "The Forks, Winnipeg",
            startTime = "Apr 30, 2026 at 10:00 AM",
            endTime = "Apr 30, 2026 at 12:00 PM",
            calendarName = "The Forks",
            isAllDay = false
        ),
        CommunityEvent(
            id = "wpg_005",
            title = "St. Norbert Farmers' Market",
            description = "Manitoba's largest farmers' market with over 100 vendors offering local produce, baked goods, crafts, and artisan products. Open Saturdays.",
            location = "3514 Pembina Hwy, Winnipeg",
            startTime = "May 2, 2026 at 8:00 AM",
            endTime = "May 2, 2026 at 3:00 PM",
            calendarName = "St. Norbert Farmers' Market",
            isAllDay = false
        ),
        CommunityEvent(
            id = "wpg_006",
            title = "The Leaf Unplugged",
            description = "A calming adults-only experience at The Leaf. Reconnect with nature and discover the healing power of plants in a serene, phone-free environment.",
            location = "The Leaf, Assiniboine Park, Winnipeg",
            startTime = "May 6, 2026 at 6:00 PM",
            endTime = "May 6, 2026 at 9:00 PM",
            calendarName = "Assiniboine Park",
            isAllDay = false
        ),
        CommunityEvent(
            id = "wpg_007",
            title = "Storytime with a Pilot",
            description = "Bring your kids to the Royal Aviation Museum for stories read by real pilots! Interactive activities and cockpit tours included.",
            location = "Royal Aviation Museum, 2088 Wellington Ave, Winnipeg",
            startTime = "May 7, 2026 at 10:30 AM",
            endTime = "May 7, 2026 at 11:30 AM",
            calendarName = "Royal Aviation Museum",
            isAllDay = false
        ),
        CommunityEvent(
            id = "wpg_008",
            title = "Candlelight Concert: Tribute to The Weeknd",
            description = "Experience The Weeknd's greatest hits performed by a string quartet in a stunning candlelit venue. A magical multi-sensory musical experience.",
            location = "Winnipeg, MB",
            startTime = "May 17, 2026 at 6:30 PM",
            endTime = "May 17, 2026 at 9:00 PM",
            calendarName = "Candlelight Concerts",
            isAllDay = false
        ),
        CommunityEvent(
            id = "wpg_009",
            title = "Victoria Day",
            description = "National statutory holiday in Canada. Many businesses closed. Fireworks displays across Winnipeg parks.",
            location = "Winnipeg, MB",
            startTime = "May 18, 2026",
            endTime = "May 18, 2026",
            calendarName = "Canadian Holidays",
            isAllDay = true
        ),
        CommunityEvent(
            id = "wpg_010",
            title = "The Four Seasons & Other Works - RWB",
            description = "Royal Winnipeg Ballet presents a dynamic trio of ballets featuring James Kudelka's The Four Seasons, Cameron Fraser-Monroe's debut, and Dwight Rhoden's world premiere.",
            location = "Centennial Concert Hall, 555 Main St, Winnipeg",
            startTime = "May 21, 2026 at 7:30 PM",
            endTime = "May 24, 2026 at 9:30 PM",
            calendarName = "Royal Winnipeg Ballet",
            isAllDay = false
        ),
        CommunityEvent(
            id = "wpg_011",
            title = "Winnipeg International Jazz Festival",
            description = "Annual jazz festival featuring world-class musicians performing across multiple venues in the Exchange District and downtown Winnipeg.",
            location = "Exchange District, Winnipeg",
            startTime = "Jun 13, 2026 at 5:00 PM",
            endTime = "Jun 22, 2026 at 11:00 PM",
            calendarName = "Winnipeg Jazz Festival",
            isAllDay = false
        ),
        CommunityEvent(
            id = "wpg_012",
            title = "National Indigenous Peoples Day",
            description = "Celebrate the heritage, diverse cultures, and contributions of First Nations, Inuit, and Métis peoples. Events at The Forks and across the city.",
            location = "The Forks, Winnipeg",
            startTime = "Jun 21, 2026",
            endTime = "Jun 21, 2026",
            calendarName = "Canadian Holidays",
            isAllDay = true
        ),
        CommunityEvent(
            id = "wpg_013",
            title = "Canada Day Celebrations",
            description = "Winnipeg's largest Canada Day celebration with live music, fireworks, family activities, and cultural performances at The Forks.",
            location = "The Forks, Winnipeg",
            startTime = "Jul 1, 2026",
            endTime = "Jul 1, 2026",
            calendarName = "Canadian Holidays",
            isAllDay = true
        ),
        CommunityEvent(
            id = "wpg_014",
            title = "Winnipeg Folk Festival",
            description = "Iconic multi-day outdoor music festival at Birds Hill Provincial Park featuring folk, roots, and world music from hundreds of artists.",
            location = "Birds Hill Provincial Park, MB",
            startTime = "Jul 9, 2026 at 12:00 PM",
            endTime = "Jul 12, 2026 at 11:00 PM",
            calendarName = "Winnipeg Folk Festival",
            isAllDay = false
        ),
        CommunityEvent(
            id = "wpg_015",
            title = "Winnipeg Fringe Theatre Festival",
            description = "North America's second-largest fringe festival with hundreds of performances in the Exchange District. Theatre, comedy, dance, and more.",
            location = "Exchange District, Winnipeg",
            startTime = "Jul 15, 2026",
            endTime = "Jul 26, 2026",
            calendarName = "Winnipeg Fringe Festival",
            isAllDay = true
        ),
        CommunityEvent(
            id = "wpg_016",
            title = "Folklorama - Multicultural Festival",
            description = "The world's largest and longest-running multicultural festival. Visit cultural pavilions across Winnipeg showcasing food, dance, and traditions from 40+ cultures.",
            location = "Various Venues, Winnipeg",
            startTime = "Aug 2, 2026",
            endTime = "Aug 15, 2026",
            calendarName = "Folklorama",
            isAllDay = true
        ),
        CommunityEvent(
            id = "wpg_017",
            title = "Manitoba Civic Holiday (Terry Fox Day)",
            description = "Provincial holiday in Manitoba. Parks and recreation facilities host special events and activities.",
            location = "Winnipeg, MB",
            startTime = "Aug 3, 2026",
            endTime = "Aug 3, 2026",
            calendarName = "Canadian Holidays",
            isAllDay = true
        ),
        CommunityEvent(
            id = "wpg_018",
            title = "Manito Ahbee Festival",
            description = "Indigenous celebration featuring one of North America's largest pow wows, an Indigenous music awards program, a marketplace, and educational events.",
            location = "RBC Convention Centre, Winnipeg",
            startTime = "Aug 14, 2026",
            endTime = "Aug 16, 2026",
            calendarName = "Manito Ahbee",
            isAllDay = true
        ),
        CommunityEvent(
            id = "wpg_019",
            title = "Labour Day",
            description = "National statutory holiday celebrating workers and the labour movement. The unofficial end of summer.",
            location = "Winnipeg, MB",
            startTime = "Sep 7, 2026",
            endTime = "Sep 7, 2026",
            calendarName = "Canadian Holidays",
            isAllDay = true
        ),
        CommunityEvent(
            id = "wpg_020",
            title = "Culture Days Winnipeg",
            description = "Free arts and culture activities across Winnipeg. Galleries, studios, and cultural spaces open their doors with workshops, tours, and performances.",
            location = "Various Venues, Winnipeg",
            startTime = "Sep 25, 2026",
            endTime = "Sep 27, 2026",
            calendarName = "Culture Days",
            isAllDay = true
        )
    )

    /**
     * Fetches upcoming events from all sources based on user location:
     * 1. Google Calendar API (country-specific holiday calendar)
     * 2. PredictHQ (school holidays, public holidays, severe weather near user)
     * 3. Curated Winnipeg events (only if user is within 100km of Winnipeg)
     * Merges, deduplicates, and sorts by start time.
     */
    suspend fun getCommunityEvents(
        lat: Double = WINNIPEG_LAT,
        lng: Double = WINNIPEG_LNG
    ): Result<List<CommunityEvent>> = withContext(Dispatchers.IO) {
        try {
            val now = nowIso8601()
            val calendarSources = getCalendarSources(lat, lng)

            // Fetch from Google Calendar API (best effort, location-aware)
            val googleEvents = async {
                try {
                    calendarSources.map { source ->
                        async {
                            try {
                                val response = api.getEvents(
                                    calendarId = source.calendarId,
                                    apiKey = API_KEY,
                                    timeMin = now
                                )
                                response.items?.mapNotNull { event ->
                                    if (event.summary.isNullOrBlank() || event.status == "cancelled") {
                                        null
                                    } else {
                                        CommunityEvent(
                                            id = event.id ?: "",
                                            title = event.summary,
                                            description = event.description?.take(200)?.replace(Regex("<[^>]*>"), "") ?: "",
                                            location = event.location ?: "",
                                            startTime = formatDateTime(event.start),
                                            endTime = formatDateTime(event.end),
                                            calendarName = response.calendarName ?: source.displayName,
                                            isAllDay = event.start?.date != null,
                                            htmlLink = event.htmlLink
                                        )
                                    }
                                } ?: emptyList()
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten()
                } catch (_: Exception) {
                    emptyList()
                }
            }

            // Fetch from PredictHQ (best effort, uses actual user coordinates)
            val predictHqEvents = async {
                try {
                    predictHqRepo.getEvents(lat, lng)
                } catch (_: Exception) {
                    emptyList()
                }
            }

            // Curated events only if near Winnipeg
            val curatedEvents = getCuratedEvents(lat, lng)

            val allEvents = (googleEvents.await() + predictHqEvents.await() + curatedEvents)
                .distinctBy { it.title.lowercase().trim() }
                .sortedBy { it.startTime }

            Result.success(allEvents)
        } catch (e: Exception) {
            // Fallback: curated events if near Winnipeg, otherwise empty
            val fallback = getCuratedEvents(lat, lng)
            Result.success(fallback)
        }
    }

    private fun nowIso8601(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun formatDateTime(dt: GoogleDateTime?): String {
        if (dt == null) return ""
        dt.date?.let { date ->
            return try {
                val parts = date.split("-")
                val months = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                "${months[parts[1].toInt() - 1]} ${parts[2].toInt()}, ${parts[0]}"
            } catch (_: Exception) {
                date
            }
        }
        dt.dateTime?.let { dateTime ->
            return try {
                val datePart = dateTime.substring(0, 10)
                val timePart = dateTime.substring(11, 16)
                val parts = datePart.split("-")
                val months = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                val hour = timePart.substring(0, 2).toInt()
                val minute = timePart.substring(3, 5)
                val amPm = if (hour < 12) "AM" else "PM"
                val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                "${months[parts[1].toInt() - 1]} ${parts[2].toInt()}, ${parts[0]} at $hour12:$minute $amPm"
            } catch (_: Exception) {
                dateTime.take(16)
            }
        }
        return ""
    }
}
