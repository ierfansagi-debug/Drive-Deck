package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DefaultFmPresets
import com.example.model.FmPreset
import com.example.model.SpeedUnit
import com.example.model.TempUnit
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.headunitDataStore: DataStore<Preferences> by preferencesDataStore(name = "drivedeck_settings")

data class HeadunitSettings(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val speedUnit: SpeedUnit = SpeedUnit.MPH,
    val tempUnit: TempUnit = TempUnit.FAHRENHEIT,
    val is24HourClock: Boolean = false,
    val currentFmFrequency: Float = 94.7f,
    val fmPresets: List<FmPreset> = DefaultFmPresets
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val CLOCK_24H = booleanPreferencesKey("clock_24h")
        val FM_FREQUENCY = floatPreferencesKey("fm_frequency")

        fun presetFreq(index: Int) = floatPreferencesKey("fm_preset_freq_$index")
        fun presetName(index: Int) = stringPreferencesKey("fm_preset_name_$index")
    }

    val settingsFlow: Flow<HeadunitSettings> = context.headunitDataStore.data.map { prefs ->
        val themeMode = prefs[Keys.THEME_MODE]?.let { name ->
            ThemeMode.entries.firstOrNull { it.name == name }
        } ?: ThemeMode.AUTO

        val speedUnit = prefs[Keys.SPEED_UNIT]?.let { name ->
            SpeedUnit.entries.firstOrNull { it.name == name }
        } ?: SpeedUnit.MPH

        val tempUnit = prefs[Keys.TEMP_UNIT]?.let { name ->
            TempUnit.entries.firstOrNull { it.name == name }
        } ?: TempUnit.FAHRENHEIT

        val is24Hour = prefs[Keys.CLOCK_24H] ?: false
        val currentFreq = prefs[Keys.FM_FREQUENCY] ?: 94.7f

        val presets = DefaultFmPresets.map { defaultPreset ->
            val savedFreq = prefs[Keys.presetFreq(defaultPreset.index)] ?: defaultPreset.frequencyMhz
            val savedName = prefs[Keys.presetName(defaultPreset.index)] ?: defaultPreset.stationName
            FmPreset(
                index = defaultPreset.index,
                frequencyMhz = savedFreq,
                stationName = savedName
            )
        }

        HeadunitSettings(
            themeMode = themeMode,
            speedUnit = speedUnit,
            tempUnit = tempUnit,
            is24HourClock = is24Hour,
            currentFmFrequency = currentFreq,
            fmPresets = presets
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.headunitDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setSpeedUnit(unit: SpeedUnit) {
        context.headunitDataStore.edit { it[Keys.SPEED_UNIT] = unit.name }
    }

    suspend fun setTempUnit(unit: TempUnit) {
        context.headunitDataStore.edit { it[Keys.TEMP_UNIT] = unit.name }
    }

    suspend fun set24HourClock(is24Hour: Boolean) {
        context.headunitDataStore.edit { it[Keys.CLOCK_24H] = is24Hour }
    }

    suspend fun setCurrentFmFrequency(frequencyMhz: Float) {
        val clamped = frequencyMhz.coerceIn(87.5f, 108.0f)
        context.headunitDataStore.edit { it[Keys.FM_FREQUENCY] = clamped }
    }

    suspend fun saveFmPreset(index: Int, frequencyMhz: Float, stationName: String) {
        val clamped = frequencyMhz.coerceIn(87.5f, 108.0f)
        val cleanName = stationName.trim().ifEmpty {
            String.format(java.util.Locale.US, "%.1f FM", clamped)
        }
        context.headunitDataStore.edit { prefs ->
            prefs[Keys.presetFreq(index)] = clamped
            prefs[Keys.presetName(index)] = cleanName
        }
    }
}
