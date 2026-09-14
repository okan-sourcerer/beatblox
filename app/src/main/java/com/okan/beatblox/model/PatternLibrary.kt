package com.okan.beatblox.model

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SavedPattern(
    val id: String = newId(),
    val name: String,
    val root: Chain,
    val cpm: Double,
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Named patterns the user has explicitly saved. Separate from [PatternStore],
 * which autosaves whatever is on screen: the library only changes on Save.
 */
class PatternLibrary(context: Context) {

    private val prefs = context.getSharedPreferences("library", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<SavedPattern> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<SavedPattern>>(raw) }
            .onFailure { Log.w(TAG, "could not read library", it) }
            .getOrDefault(emptyList())
    }

    fun save(patterns: List<SavedPattern>) {
        prefs.edit().putString(KEY, json.encodeToString(patterns)).apply()
    }

    private companion object {
        const val TAG = "PatternLibrary"
        const val KEY = "patterns"
    }
}
