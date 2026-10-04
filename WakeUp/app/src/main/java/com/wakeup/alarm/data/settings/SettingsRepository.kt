package com.wakeup.alarm.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wakeup.alarm.domain.model.Alarm
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** App-wide preferences stored with Preferences DataStore. Corrupt or unknown values fall back to defaults. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> prefs.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[KEY_THEME] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = edit { it[KEY_DYNAMIC_COLOR] = enabled }

    suspend fun setTimeFormat(format: TimeFormatPreference) = edit { it[KEY_TIME_FORMAT] = format.name }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) =
        edit { it[KEY_SNOOZE] = minutes.coerceIn(1, Alarm.MAX_SNOOZE_MINUTES) }

    suspend fun setDefaultVibrate(enabled: Boolean) = edit { it[KEY_VIBRATE] = enabled }

    suspend fun setDefaultSoundUri(uri: String) = edit { it[KEY_SOUND] = uri }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        try {
            dataStore.edit { block(it) }
        } catch (_: IOException) {
            // Disk problem: the setting simply isn't saved. Never crash for a preference.
        }
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        themeMode = enumOrDefault(this[KEY_THEME], ThemeMode.SYSTEM),
        dynamicColor = this[KEY_DYNAMIC_COLOR] ?: false,
        timeFormat = enumOrDefault(this[KEY_TIME_FORMAT], TimeFormatPreference.SYSTEM),
        defaultSnoozeMinutes = (this[KEY_SNOOZE] ?: Alarm.DEFAULT_SNOOZE_MINUTES).coerceIn(1, Alarm.MAX_SNOOZE_MINUTES),
        defaultVibrate = this[KEY_VIBRATE] ?: true,
        defaultSoundUri = this[KEY_SOUND] ?: "",
    )

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_TIME_FORMAT = stringPreferencesKey("time_format")
        val KEY_SNOOZE = intPreferencesKey("default_snooze_minutes")
        val KEY_VIBRATE = booleanPreferencesKey("default_vibrate")
        val KEY_SOUND = stringPreferencesKey("default_sound_uri")
    }
}
