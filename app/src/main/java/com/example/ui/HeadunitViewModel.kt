package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HeadunitSettings
import com.example.data.LocalMediaManager
import com.example.data.LocationTelemetryManager
import com.example.data.SettingsRepository
import com.example.data.WeatherRepository
import com.example.model.AudioSource
import com.example.model.ClockState
import com.example.model.FmPreset
import com.example.model.HeadunitUiState
import com.example.model.HeadunitView
import com.example.model.RadioState
import com.example.model.SpeedUnit
import com.example.model.TempUnit
import com.example.model.ThemeMode
import com.example.model.WeatherState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

class HeadunitViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val weatherRepository = WeatherRepository(application)
    private val locationManager = LocationTelemetryManager(application, viewModelScope)
    private val mediaManager = LocalMediaManager(application, viewModelScope)

    private val activeView = MutableStateFlow(HeadunitView.DASHBOARD_SPLIT)
    private val activeAudioSource = MutableStateFlow(AudioSource.MEDIA)
    private val followVehicleOnMap = MutableStateFlow(true)
    private val isScanningFm = MutableStateFlow(false)
    private val weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    private val clockTick = MutableStateFlow(System.currentTimeMillis())

    private var lastWeatherLat: Double? = null
    private var lastWeatherLon: Double? = null

    private val clockAndWeatherFlow = combine(
        clockTick,
        weatherState,
        settingsRepository.settingsFlow
    ) { millis, weather, settings ->
        Triple(buildClockState(millis, settings.is24HourClock), weather, settings)
    }

    private val audioControlFlow = combine(
        activeAudioSource,
        mediaManager.mediaState,
        isScanningFm
    ) { source, media, scanning ->
        Triple(source, media, scanning)
    }

    val uiState: StateFlow<HeadunitUiState> = combine(
        activeView,
        followVehicleOnMap,
        locationManager.telemetry,
        clockAndWeatherFlow,
        audioControlFlow
    ) { view, followMap, telemetry, clockWeatherSettings, audioTriple ->
        val (clock, weather, settings) = clockWeatherSettings
        val (audioSource, media, scanning) = audioTriple

        val effectiveDark = when (settings.themeMode) {
            ThemeMode.NIGHT -> true
            ThemeMode.DAY -> false
            ThemeMode.AUTO -> {
                if (weather is WeatherState.Ready) {
                    !weather.isDay
                } else {
                    clock.isNightHour
                }
            }
        }

        val matchingPreset = settings.fmPresets.firstOrNull {
            abs(it.frequencyMhz - settings.currentFmFrequency) < 0.06f
        }

        val radio = RadioState(
            currentFrequencyMhz = settings.currentFmFrequency,
            presets = settings.fmPresets,
            isScanning = scanning,
            signalStrength = if (matchingPreset != null) 5 else 3,
            rdsLabel = matchingPreset?.stationName ?: "FM STEREO"
        )

        HeadunitUiState(
            activeView = view,
            themeMode = settings.themeMode,
            effectiveDarkTheme = effectiveDark,
            speedUnit = settings.speedUnit,
            tempUnit = settings.tempUnit,
            is24HourClock = settings.is24HourClock,
            activeAudioSource = audioSource,
            followVehicleOnMap = followMap,
            telemetry = telemetry,
            weather = weather,
            clock = clock,
            media = media,
            radio = radio
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HeadunitUiState()
    )

    init {
        startClockTicker()
        observeLocationForWeather()
        refreshWeather()
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                clockTick.value = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }

    private fun observeLocationForWeather() {
        viewModelScope.launch {
            locationManager.telemetry.collect { t ->
                if (t.hasGpsFix && !t.isDemoDriveActive) {
                    val prevLat = lastWeatherLat
                    val prevLon = lastWeatherLon
                    val movedFar = prevLat == null || prevLon == null ||
                        abs(t.latitude - prevLat) > 0.05 || abs(t.longitude - prevLon) > 0.05
                    if (movedFar) {
                        fetchWeatherFor(t.latitude, t.longitude)
                    }
                }
            }
        }
    }

    fun refreshWeather() {
        val t = locationManager.telemetry.value
        fetchWeatherFor(t.latitude, t.longitude)
    }

    private fun fetchWeatherFor(lat: Double, lon: Double) {
        lastWeatherLat = lat
        lastWeatherLon = lon
        viewModelScope.launch {
            weatherState.value = WeatherState.Loading
            weatherState.value = weatherRepository.fetchWeather(lat, lon)
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        locationManager.onPermissionStatusChanged(granted)
        if (granted) {
            refreshWeather()
        }
    }

    fun navigateTo(view: HeadunitView) {
        activeView.value = view
    }

    fun navigateBackToDashboard() {
        activeView.value = HeadunitView.DASHBOARD_SPLIT
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun cycleThemeMode() {
        val modes = ThemeMode.entries
        val next = modes[(uiState.value.themeMode.ordinal + 1) % modes.size]
        setThemeMode(next)
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        viewModelScope.launch { settingsRepository.setSpeedUnit(unit) }
    }

    fun toggleSpeedUnit() {
        val next = if (uiState.value.speedUnit == SpeedUnit.MPH) SpeedUnit.KMH else SpeedUnit.MPH
        setSpeedUnit(next)
    }

    fun setTempUnit(unit: TempUnit) {
        viewModelScope.launch { settingsRepository.setTempUnit(unit) }
    }

    fun set24HourClock(is24Hour: Boolean) {
        viewModelScope.launch { settingsRepository.set24HourClock(is24Hour) }
    }

    fun setFollowVehicleOnMap(follow: Boolean) {
        followVehicleOnMap.value = follow
    }

    fun toggleDemoDrive() {
        locationManager.toggleDemoDrive()
    }

    fun resetTripStats() {
        locationManager.resetTripStats()
    }

    fun setAudioSource(source: AudioSource) {
        activeAudioSource.value = source
        if (source == AudioSource.FM_RADIO) {
            mediaManager.pause()
        }
    }

    // Local Media3 Controls
    fun addAudioUris(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            activeAudioSource.value = AudioSource.MEDIA
            mediaManager.addAudioUris(uris)
        }
    }

    fun togglePlayPause() {
        activeAudioSource.value = AudioSource.MEDIA
        mediaManager.togglePlayPause()
    }

    fun nextTrack() {
        activeAudioSource.value = AudioSource.MEDIA
        mediaManager.next()
    }

    fun previousTrack() {
        activeAudioSource.value = AudioSource.MEDIA
        mediaManager.previous()
    }

    fun selectTrack(index: Int) {
        activeAudioSource.value = AudioSource.MEDIA
        mediaManager.selectTrack(index)
    }

    fun seekMedia(positionMs: Long) {
        mediaManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        mediaManager.toggleShuffle()
    }

    fun toggleRepeat() {
        mediaManager.toggleRepeat()
    }

    // FM Preset Tuner Controls
    fun tuneFmFrequency(frequencyMhz: Float) {
        val rounded = ((frequencyMhz.coerceIn(87.5f, 108.0f)) * 10f).roundToInt() / 10f
        activeAudioSource.value = AudioSource.FM_RADIO
        mediaManager.pause()
        viewModelScope.launch {
            settingsRepository.setCurrentFmFrequency(rounded)
        }
    }

    fun stepFmFrequency(deltaMhz: Float) {
        tuneFmFrequency(uiState.value.radio.currentFrequencyMhz + deltaMhz)
    }

    fun seekFmPreset(forward: Boolean) {
        val presets = uiState.value.radio.presets.sortedBy { it.frequencyMhz }
        if (presets.isEmpty()) return
        val current = uiState.value.radio.currentFrequencyMhz
        val target = if (forward) {
            presets.firstOrNull { it.frequencyMhz > current + 0.06f } ?: presets.first()
        } else {
            presets.lastOrNull { it.frequencyMhz < current - 0.06f } ?: presets.last()
        }

        activeAudioSource.value = AudioSource.FM_RADIO
        mediaManager.pause()
        viewModelScope.launch {
            isScanningFm.value = true
            delay(350L)
            settingsRepository.setCurrentFmFrequency(target.frequencyMhz)
            isScanningFm.value = false
        }
    }

    fun selectFmPreset(preset: FmPreset) {
        tuneFmFrequency(preset.frequencyMhz)
    }

    fun saveFmPreset(index: Int, frequencyMhz: Float, stationName: String) {
        viewModelScope.launch {
            settingsRepository.saveFmPreset(index, frequencyMhz, stationName)
        }
    }

    private fun buildClockState(millis: Long, is24Hour: Boolean): ClockState {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val isNight = hour24 < 6 || hour24 >= 19 || (hour24 == 6 && minute < 30) || (hour24 == 18 && minute >= 30)

        val date = cal.time
        val timePattern = if (is24Hour) "HH:mm" else "h:mm"
        val timeFmt = SimpleDateFormat(timePattern, Locale.getDefault()).format(date)
        val secondsFmt = SimpleDateFormat("ss", Locale.getDefault()).format(date)
        val amPmFmt = if (is24Hour) "" else SimpleDateFormat("a", Locale.getDefault()).format(date).uppercase(Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date)

        return ClockState(
            timeFormatted = timeFmt,
            secondsFormatted = secondsFmt,
            amPm = amPmFmt,
            dateFormatted = dateFmt,
            isNightHour = isNight
        )
    }

    override fun onCleared() {
        super.onCleared()
        locationManager.release()
        mediaManager.release()
    }
}
