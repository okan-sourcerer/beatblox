package com.okan.strudelmobile.model

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json

/**
 * Persists the current pattern (and tempo) so it survives process death.
 * One JSON blob in SharedPreferences is plenty for a single working pattern.
 */
class PatternStore(context: Context) {

    private val prefs = context.getSharedPreferences("pattern", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): Chain? {
        val raw = prefs.getString(KEY_TREE, null) ?: return null
        return runCatching { json.decodeFromString<Chain>(raw) }
            .onFailure { Log.w(TAG, "could not restore pattern, starting fresh", it) }
            .getOrNull()
    }

    fun save(root: Chain) {
        prefs.edit().putString(KEY_TREE, json.encodeToString(root)).apply()
    }

    fun loadCpm(default: Double): Double = prefs.getFloat(KEY_CPM, default.toFloat()).toDouble()

    fun saveCpm(cpm: Double) {
        prefs.edit().putFloat(KEY_CPM, cpm.toFloat()).apply()
    }

    private companion object {
        const val TAG = "PatternStore"
        const val KEY_TREE = "tree"
        const val KEY_CPM = "cpm"
    }
}
