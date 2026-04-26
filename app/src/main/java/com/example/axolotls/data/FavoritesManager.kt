package com.example.axolotls.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages favorite event IDs using SharedPreferences.
 * Stores community favorites (String IDs) and nearby favorites (Int IDs) separately.
 *
 * This is a temporary local storage solution. Will be replaced with a
 * Room database + user accounts in a future update.
 */
class FavoritesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "axolotls_favorites"
        private const val KEY_COMMUNITY = "community_favorite_ids"
        private const val KEY_NEARBY = "nearby_favorite_ids"
    }

    // ── Community Favorites (String IDs) ──

    fun getCommunityFavorites(): Set<String> {
        return prefs.getStringSet(KEY_COMMUNITY, emptySet()) ?: emptySet()
    }

    fun saveCommunityFavorites(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_COMMUNITY, ids).apply()
    }

    fun toggleCommunityFavorite(id: String): Set<String> {
        val current = getCommunityFavorites().toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        saveCommunityFavorites(current)
        return current
    }

    // ── Nearby Favorites (Int IDs stored as strings) ──

    fun getNearbyFavorites(): Set<Int> {
        val stored = prefs.getStringSet(KEY_NEARBY, emptySet()) ?: emptySet()
        return stored.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun saveNearbyFavorites(ids: Set<Int>) {
        prefs.edit().putStringSet(KEY_NEARBY, ids.map { it.toString() }.toSet()).apply()
    }

    fun toggleNearbyFavorite(id: Int): Set<Int> {
        val current = getNearbyFavorites().toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        saveNearbyFavorites(current)
        return current
    }
}
