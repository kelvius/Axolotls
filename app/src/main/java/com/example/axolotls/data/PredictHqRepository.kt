package com.example.axolotls.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Repository that fetches school holidays, public holidays, and severe weather
 * events from the PredictHQ API based on user location.
 */
class PredictHqRepository(
    private val api: PredictHqApiService = PredictHqApiService.create(API_TOKEN)
) {
    companion object {
        const val API_TOKEN = "5HAEvPUYoK573hXpQACXnCc2OzCyvI_qDkL-BpXC"

        /** Categories we care about */
        const val CATEGORIES = "school-holidays,public-holidays,severe-weather,observances"

        /** Default location: Winnipeg, MB */
        const val DEFAULT_LAT = 49.8951
        const val DEFAULT_LNG = -97.1384

        /** Search radius in km */
        const val RADIUS_KM = 100

        /** How many months ahead to search */
        const val MONTHS_AHEAD = 5

        /** Map PredictHQ category to a display-friendly name */
        fun categoryDisplayName(category: String): String = when (category) {
            "school-holidays" -> "School Holidays"
            "public-holidays" -> "Public Holidays"
            "severe-weather" -> "Severe Weather"
            "observances" -> "Observances"
            else -> category.replaceFirstChar { it.uppercase() }
        }

        /** Map PredictHQ category to emoji-free icon label for the badge */
        fun categoryBadge(category: String): String = when (category) {
            "school-holidays" -> "School Holiday"
            "public-holidays" -> "Public Holiday"
            "severe-weather" -> "Weather Alert"
            "observances" -> "Observance"
            else -> category.replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Fetch upcoming events near the given location.
     * Returns them as CommunityEvent objects ready for display.
     */
    suspend fun getEvents(
        lat: Double = DEFAULT_LAT,
        lng: Double = DEFAULT_LNG
    ): List<CommunityEvent> = withContext(Dispatchers.IO) {
        try {
            val now = todayIso()
            val endDate = monthsFromNowIso(MONTHS_AHEAD)
            val within = "${RADIUS_KM}km@$lat,$lng"

            val response = api.searchEvents(
                category = CATEGORIES,
                within = within,
                activeGte = now,
                activeLte = endDate,
                sort = "start",
                limit = 50
            )

            response.results?.map { event ->
                CommunityEvent(
                    id = "phq_${event.id}",
                    title = event.title,
                    description = event.description.ifBlank {
                        buildDescription(event)
                    },
                    location = buildLocation(event),
                    startTime = formatPhqDate(event.start),
                    endTime = formatPhqDate(event.end),
                    calendarName = categoryBadge(event.category),
                    isAllDay = isAllDayEvent(event),
                    htmlLink = null
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun buildDescription(event: PredictHqEvent): String {
        val parts = mutableListOf<String>()
        parts.add(categoryDisplayName(event.category))
        if (event.rank > 0) {
            parts.add("Impact rank: ${event.rank}/100")
        }
        event.labels?.takeIf { it.isNotEmpty() }?.let {
            parts.add(it.joinToString(", ") { label ->
                label.replaceFirstChar { c -> c.uppercase() }
            })
        }
        if (event.scope != null) {
            parts.add("Scope: ${event.scope}")
        }
        return parts.joinToString(" • ")
    }

    private fun buildLocation(event: PredictHqEvent): String {
        val parts = mutableListOf<String>()
        event.state?.let { parts.add(it) }
        if (event.country.isNotBlank()) {
            parts.add(event.country)
        }
        return if (parts.isEmpty()) "Winnipeg, MB" else parts.joinToString(", ")
    }

    private fun isAllDayEvent(event: PredictHqEvent): Boolean {
        // If duration is a multiple of 86400 seconds (full days), treat as all-day
        return event.duration > 0 && event.duration % 86400 == 0L
    }

    /**
     * Format a PredictHQ ISO 8601 datetime string to a readable format.
     * Input: "2026-05-01T00:00:00Z" or "2026-05-01"
     */
    private fun formatPhqDate(dateStr: String): String {
        if (dateStr.isBlank()) return ""
        return try {
            val months = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            // Try parsing full ISO datetime
            val datePart = dateStr.substring(0, 10)
            val parts = datePart.split("-")
            val monthStr = months[parts[1].toInt() - 1]
            val day = parts[2].toInt()
            val year = parts[0]

            if (dateStr.length > 10 && !dateStr.contains("T00:00:00")) {
                // Has a specific time
                val timePart = dateStr.substring(11, 16)
                val hour = timePart.substring(0, 2).toInt()
                val minute = timePart.substring(3, 5)
                val amPm = if (hour < 12) "AM" else "PM"
                val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                "$monthStr $day, $year at $hour12:$minute $amPm"
            } else {
                "$monthStr $day, $year"
            }
        } catch (_: Exception) {
            dateStr.take(10)
        }
    }

    private fun todayIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun monthsFromNowIso(months: Int): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.add(Calendar.MONTH, months)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(cal.time)
    }
}
