package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

private val Context.dataStore by preferencesDataStore(name = "mathpad_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val TTS_VOICE = stringPreferencesKey("tts_voice")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    val isOfflineMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.OFFLINE_MODE] ?: false
    }

    val ttsVoice: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.TTS_VOICE] ?: "Kore"
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setOfflineMode(offline: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OFFLINE_MODE] = offline
        }
    }

    suspend fun setTtsVoice(voiceName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TTS_VOICE] = voiceName
        }
    }
}
