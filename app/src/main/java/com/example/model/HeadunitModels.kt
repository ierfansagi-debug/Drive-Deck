package com.example.model

enum class HeadunitView {
    DASHBOARD_SPLIT,
    MAP_FULL,
    AUDIO_FOCUS,
    CLUSTER_FOCUS,
    SETTINGS
}

enum class ThemeMode(val label: String) {
    AUTO("Auto (Day/Night)"),
    DAY("Daylight"),
    NIGHT("Night Cockpit")
}

enum class SpeedUnit(val label: String, val unitSuffix: String) {
    MPH("Miles per Hour", "MPH"),
    KMH("Kilometers per Hour", "KM/H")
}

enum class TempUnit(val label: String, val symbol: String) {
    FAHRENHEIT("Fahrenheit", "°F"),
    CELSIUS("Celsius", "°C")
}

enum class AudioSource {
    MEDIA,
    FM_RADIO
}

data class FmPreset(
    val index: Int,
    val frequencyMhz: Float,
    val stationName: String
)

data class AudioTrack(
    val uriString: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artworkBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioTrack) return false
        return uriString == other.uriString &&
            title == other.title &&
            artist == other.artist &&
            durationMs == other.durationMs
    }

    override fun hashCode(): Int {
        var result = uriString.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + durationMs.hashCode()
        return result
    }
}

sealed interface WeatherState {
    data object Loading : WeatherState
    data class Ready(
        val tempCelsius: Double,
        val weatherCode: Int,
        val windSpeedKmh: Double,
        val humidityPercent: Int,
        val cityName: String,
        val isDay: Boolean,
        val updatedTimeLabel: String
    ) : WeatherState
    data class Error(val message: String) : WeatherState
}

data class TelemetryState(
    val speedMps: Float = 0f,
    val maxSpeedMps: Float = 0f,
    val tripDistanceMeters: Double = 0.0,
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val bearing: Float = 0f,
    val hasLocationPermission: Boolean = false,
    val hasGpsFix: Boolean = false,
    val isDemoDriveActive: Boolean = false
) {
    fun formattedSpeed(unit: SpeedUnit): Int {
        val converted = when (unit) {
            SpeedUnit.MPH -> speedMps * 2.23694f
            SpeedUnit.KMH -> speedMps * 3.6f
        }
        return converted.toInt().coerceAtLeast(0)
    }

    fun formattedMaxSpeed(unit: SpeedUnit): Int {
        val converted = when (unit) {
            SpeedUnit.MPH -> maxSpeedMps * 2.23694f
            SpeedUnit.KMH -> maxSpeedMps * 3.6f
        }
        return converted.toInt().coerceAtLeast(0)
    }

    fun formattedTripDistance(unit: SpeedUnit): String {
        return when (unit) {
            SpeedUnit.MPH -> String.format(java.util.Locale.US, "%.1f mi", tripDistanceMeters / 1609.344)
            SpeedUnit.KMH -> String.format(java.util.Locale.US, "%.1f km", tripDistanceMeters / 1000.0)
        }
    }
}

data class ClockState(
    val timeFormatted: String = "--:--",
    val secondsFormatted: String = "00",
    val amPm: String = "",
    val dateFormatted: String = "",
    val isNightHour: Boolean = true
)

data class MediaState(
    val playlist: List<AudioTrack> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false
) {
    val currentTrack: AudioTrack?
        get() = playlist.getOrNull(currentIndex)
}

data class RadioState(
    val currentFrequencyMhz: Float = 94.7f,
    val presets: List<FmPreset> = DefaultFmPresets,
    val isScanning: Boolean = false,
    val signalStrength: Int = 4,
    val rdsLabel: String = "BAY ALT"
)

data class HeadunitUiState(
    val activeView: HeadunitView = HeadunitView.DASHBOARD_SPLIT,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val effectiveDarkTheme: Boolean = true,
    val speedUnit: SpeedUnit = SpeedUnit.MPH,
    val tempUnit: TempUnit = TempUnit.FAHRENHEIT,
    val is24HourClock: Boolean = false,
    val activeAudioSource: AudioSource = AudioSource.MEDIA,
    val followVehicleOnMap: Boolean = true,
    val telemetry: TelemetryState = TelemetryState(),
    val weather: WeatherState = WeatherState.Loading,
    val clock: ClockState = ClockState(),
    val media: MediaState = MediaState(),
    val radio: RadioState = RadioState()
)

val DefaultFmPresets = listOf(
    FmPreset(0, 88.5f, "KQED FM"),
    FmPreset(1, 91.3f, "NPR NEWS"),
    FmPreset(2, 94.7f, "BAY ALT"),
    FmPreset(3, 98.1f, "CHILL"),
    FmPreset(4, 101.9f, "DRIVE HITS"),
    FmPreset(5, 106.5f, "ROCK FM")
)
