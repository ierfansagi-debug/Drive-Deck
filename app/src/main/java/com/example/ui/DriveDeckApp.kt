package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.HeadunitUiState
import com.example.model.HeadunitView
import com.example.ui.components.AudioRadioWidgetCard
import com.example.ui.components.ClockWeatherWidgetCard
import com.example.ui.components.DriverBottomBar
import com.example.ui.components.DriverSideRail
import com.example.ui.components.MapWidgetCard
import com.example.ui.components.SpeedometerWidgetCard
import com.example.ui.screens.AudioFocusScreen
import com.example.ui.screens.ClusterFocusScreen
import com.example.ui.screens.FullMapScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DriveDeckTheme

@Composable
fun DriveDeckApp(viewModel: HeadunitViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(fine || coarse)
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.addAudioUris(uris)
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            viewModel.onLocationPermissionResult(true)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val requestLocation = {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    val pickAudio = {
        audioPickerLauncher.launch(arrayOf("audio/*"))
    }

    DriveDeckTheme(darkTheme = state.effectiveDarkTheme) {
        BackHandler(enabled = state.activeView == HeadunitView.DASHBOARD_SPLIT) {
            // Home launcher root screen stays active on Back press
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscapeCockpit = maxWidth >= 500.dp || maxWidth >= maxHeight

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isLandscapeCockpit) {
                        DriverBottomBar(
                            state = state,
                            onNavigate = viewModel::navigateTo
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isLandscapeCockpit) {
                        DriverSideRail(
                            state = state,
                            onNavigate = viewModel::navigateTo,
                            onCycleTheme = viewModel::cycleThemeMode
                        )
                    }

                    AnimatedContent(
                        targetState = state.activeView,
                        label = "headunit_view_transition",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) { view ->
                        when (view) {
                            HeadunitView.DASHBOARD_SPLIT -> {
                                if (isLandscapeCockpit) {
                                    LandscapeSplitDashboard(
                                        state = state,
                                        viewModel = viewModel,
                                        onRequestLocation = requestLocation,
                                        onPickAudio = pickAudio
                                    )
                                } else {
                                    PortraitStackedDashboard(
                                        state = state,
                                        viewModel = viewModel,
                                        onRequestLocation = requestLocation,
                                        onPickAudio = pickAudio
                                    )
                                }
                            }

                            HeadunitView.MAP_FULL -> FullMapScreen(
                                state = state,
                                onBack = viewModel::navigateBackToDashboard,
                                onToggleFollow = viewModel::setFollowVehicleOnMap,
                                onRequestLocationPermission = requestLocation,
                                onTogglePlayPause = viewModel::togglePlayPause
                            )

                            HeadunitView.AUDIO_FOCUS -> AudioFocusScreen(
                                state = state,
                                onBack = viewModel::navigateBackToDashboard,
                                onSelectSource = viewModel::setAudioSource,
                                onPickLocalAudio = pickAudio,
                                onSelectTrack = viewModel::selectTrack,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onNextTrack = viewModel::nextTrack,
                                onPreviousTrack = viewModel::previousTrack,
                                onSeekMedia = viewModel::seekMedia,
                                onToggleShuffle = viewModel::toggleShuffle,
                                onToggleRepeat = viewModel::toggleRepeat,
                                onTuneFm = viewModel::tuneFmFrequency,
                                onStepFm = viewModel::stepFmFrequency,
                                onSeekFmPreset = viewModel::seekFmPreset,
                                onSelectPreset = viewModel::selectFmPreset,
                                onSavePreset = viewModel::saveFmPreset
                            )

                            HeadunitView.CLUSTER_FOCUS -> ClusterFocusScreen(
                                state = state,
                                onBack = viewModel::navigateBackToDashboard,
                                onToggleSpeedUnit = viewModel::toggleSpeedUnit,
                                onToggleDemoDrive = viewModel::toggleDemoDrive,
                                onResetTripStats = viewModel::resetTripStats,
                                onRefreshWeather = viewModel::refreshWeather
                            )

                            HeadunitView.SETTINGS -> SettingsScreen(
                                state = state,
                                onBack = viewModel::navigateBackToDashboard,
                                onSelectSpeedUnit = viewModel::setSpeedUnit,
                                onSelectTempUnit = viewModel::setTempUnit,
                                onSelectThemeMode = viewModel::setThemeMode,
                                onToggle24HourClock = viewModel::set24HourClock,
                                onToggleDemoDrive = viewModel::toggleDemoDrive,
                                onResetTripStats = viewModel::resetTripStats
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeSplitDashboard(
    state: HeadunitUiState,
    viewModel: HeadunitViewModel,
    onRequestLocation: () -> Unit,
    onPickAudio: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left-Center Primary Google Maps Pane (~54%)
        MapWidgetCard(
            telemetry = state.telemetry,
            isDarkTheme = state.effectiveDarkTheme,
            followVehicle = state.followVehicleOnMap,
            showExpandButton = true,
            onToggleFollow = viewModel::setFollowVehicleOnMap,
            onRequestLocationPermission = onRequestLocation,
            onExpandMap = { viewModel.navigateTo(HeadunitView.MAP_FULL) },
            modifier = Modifier
                .weight(0.54f)
                .fillMaxHeight()
        )

        // Right Side Instrumentation & Audio Widget Deck (~46%)
        Column(
            modifier = Modifier
                .weight(0.46f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Speedometer + Clock/Weather
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.48f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpeedometerWidgetCard(
                    telemetry = state.telemetry,
                    speedUnit = state.speedUnit,
                    onToggleSpeedUnit = viewModel::toggleSpeedUnit,
                    onToggleDemoDrive = viewModel::toggleDemoDrive,
                    onOpenCluster = { viewModel.navigateTo(HeadunitView.CLUSTER_FOCUS) },
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight()
                )

                ClockWeatherWidgetCard(
                    clock = state.clock,
                    weather = state.weather,
                    tempUnit = state.tempUnit,
                    speedUnit = state.speedUnit,
                    onRefreshWeather = viewModel::refreshWeather,
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                )
            }

            // Bottom Row: Media3 Local Player + FM Preset Tuner
            AudioRadioWidgetCard(
                activeSource = state.activeAudioSource,
                mediaState = state.media,
                radioState = state.radio,
                onSelectSource = viewModel::setAudioSource,
                onPickLocalAudio = onPickAudio,
                onTogglePlayPause = viewModel::togglePlayPause,
                onNextTrack = viewModel::nextTrack,
                onPreviousTrack = viewModel::previousTrack,
                onSeekMedia = viewModel::seekMedia,
                onToggleShuffle = viewModel::toggleShuffle,
                onToggleRepeat = viewModel::toggleRepeat,
                onTuneFm = viewModel::tuneFmFrequency,
                onStepFm = viewModel::stepFmFrequency,
                onSeekFmPreset = viewModel::seekFmPreset,
                onSelectPreset = viewModel::selectFmPreset,
                onSavePreset = viewModel::saveFmPreset,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.52f)
            )
        }
    }
}

@Composable
private fun PortraitStackedDashboard(
    state: HeadunitUiState,
    viewModel: HeadunitViewModel,
    onRequestLocation: () -> Unit,
    onPickAudio: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MapWidgetCard(
            telemetry = state.telemetry,
            isDarkTheme = state.effectiveDarkTheme,
            followVehicle = state.followVehicleOnMap,
            showExpandButton = true,
            onToggleFollow = viewModel::setFollowVehicleOnMap,
            onRequestLocationPermission = onRequestLocation,
            onExpandMap = { viewModel.navigateTo(HeadunitView.MAP_FULL) },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SpeedometerWidgetCard(
                telemetry = state.telemetry,
                speedUnit = state.speedUnit,
                onToggleSpeedUnit = viewModel::toggleSpeedUnit,
                onToggleDemoDrive = viewModel::toggleDemoDrive,
                onOpenCluster = { viewModel.navigateTo(HeadunitView.CLUSTER_FOCUS) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            ClockWeatherWidgetCard(
                clock = state.clock,
                weather = state.weather,
                tempUnit = state.tempUnit,
                speedUnit = state.speedUnit,
                onRefreshWeather = viewModel::refreshWeather,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        AudioRadioWidgetCard(
            activeSource = state.activeAudioSource,
            mediaState = state.media,
            radioState = state.radio,
            onSelectSource = viewModel::setAudioSource,
            onPickLocalAudio = onPickAudio,
            onTogglePlayPause = viewModel::togglePlayPause,
            onNextTrack = viewModel::nextTrack,
            onPreviousTrack = viewModel::previousTrack,
            onSeekMedia = viewModel::seekMedia,
            onToggleShuffle = viewModel::toggleShuffle,
            onToggleRepeat = viewModel::toggleRepeat,
            onTuneFm = viewModel::tuneFmFrequency,
            onStepFm = viewModel::stepFmFrequency,
            onSeekFmPreset = viewModel::seekFmPreset,
            onSelectPreset = viewModel::selectFmPreset,
            onSavePreset = viewModel::saveFmPreset,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        )
    }
}
