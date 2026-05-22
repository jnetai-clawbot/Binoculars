package com.jnetaol.binoculars.engine

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "binoculars_settings")

object SettingsManager {
    private val KEY_SHOW_DISTANCE = booleanPreferencesKey("show_distance_overlay")
    private val KEY_NIGHT_VISION = booleanPreferencesKey("night_vision_mode")
    private val KEY_SHOW_GRID = booleanPreferencesKey("show_grid")
    private val KEY_DISTANCE_MULTIPLIER = floatPreferencesKey("distance_multiplier")

    fun showDistanceOverlay(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SHOW_DISTANCE] ?: true }

    fun nightVisionMode(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_NIGHT_VISION] ?: false }

    fun showGrid(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SHOW_GRID] ?: true }

    fun distanceMultiplier(context: Context): Flow<Float> =
        context.dataStore.data.map { it[KEY_DISTANCE_MULTIPLIER] ?: 1.0f }

    suspend fun setShowDistanceOverlay(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_DISTANCE] = value }
    }

    suspend fun setNightVisionMode(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_NIGHT_VISION] = value }
    }

    suspend fun setShowGrid(context: Context, value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_GRID] = value }
    }

    suspend fun setDistanceMultiplier(context: Context, value: Float) {
        context.dataStore.edit { it[KEY_DISTANCE_MULTIPLIER] = value }
    }
}
