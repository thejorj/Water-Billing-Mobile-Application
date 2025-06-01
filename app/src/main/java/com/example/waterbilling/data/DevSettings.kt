package com.example.waterbilling.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.devSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "dev_settings")

class DevSettings(private val context: Context) {
    private val devModeKey = booleanPreferencesKey("dev_mode_enabled")

    val isDevModeEnabled: Flow<Boolean> = context.devSettingsDataStore.data
        .map { preferences ->
            preferences[devModeKey] ?: false
        }

    suspend fun setDevModeEnabled(enabled: Boolean) {
        context.devSettingsDataStore.edit { preferences ->
            preferences[devModeKey] = enabled
        }
    }

    companion object {
        const val DEV_MODE_PASSWORD = "DALOY2024" // You can change this to any secure password
    }
} 