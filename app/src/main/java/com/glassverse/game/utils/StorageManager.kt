// app/src/main/java/com/glassverse/game/utils/StorageManager.kt
package com.glassverse.game.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class StorageManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "glassverse_game_data"
        private const val KEY_GAME_STATE = "game_state"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_LAUNCH_COUNT = "launch_count"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Track launches
        val count = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        prefs.edit { putInt(KEY_LAUNCH_COUNT, count + 1) }
        if (!prefs.contains(KEY_FIRST_LAUNCH)) {
            prefs.edit { putLong(KEY_FIRST_LAUNCH, System.currentTimeMillis()) }
        }
    }

    fun saveGameState(json: String) {
        prefs.edit(commit = true) {
            putString(KEY_GAME_STATE, json)
            putLong("last_save", System.currentTimeMillis())
        }
    }

    fun loadGameState(): String {
        return prefs.getString(KEY_GAME_STATE, "") ?: ""
    }

    fun clearAll() {
        prefs.edit(commit = true) { clear() }
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getInt(KEY_LAUNCH_COUNT, 0) <= 1
    }

    fun getLaunchCount(): Int {
        return prefs.getInt(KEY_LAUNCH_COUNT, 0)
    }

    fun getLastSaveTime(): Long {
        return prefs.getLong("last_save", 0)
    }
}