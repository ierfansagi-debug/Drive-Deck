package com.example.ui.screens

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AudioSource
import com.example.model.FmPreset
import com.example.model.HeadunitUiState
import com.example.model.SpeedUnit
import com.example.model.TempUnit
import com.example.model.ThemeMode
import com.example.ui.components.AudioRadioWidgetCard
import com.example.ui.components.ClockWeatherWidgetCard
import com.example.ui.components.FmRadioPanel
import com.example.ui.components.InstrumentCard
import com.example.ui.components.MapWidgetCard
import com.example.ui.components.SpeedometerWidgetCard
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.LocalDriveDeckColors
import java.util.Locale

@Composable
fun FullMapScreen(
    state: HeadunitUiState,
    onBack: () -> Unit,
    onToggleFollow: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onTogglePlayPause: () -> Unit
) {
    BackHandler(onBack = onBack)
    val ext = LocalDriveDeckColors.current

    Box(modifier = Modifier.fillMaxSize()) {
        MapWidgetCard(
            telemetry = state.telemetry,
            isDarkTheme = state.effectiveDarkTheme,
            followVehicle = state.followVehicleOnMap,
            showExpandButton = false,
            onToggleFollow = onToggleFollow,
            onRequestLocationPermission = onRequestLocationPermission,
            onExpandMap = {},
            modifier = Modifier.fillMaxSize()
        )

        // Floating Bottom Driving HUD Pill
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp, start = 16.dp, end = 76.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, ext.bezelBorder),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Speed Pill
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = state.telemetry.formattedSpeed(state.speedUnit).toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.speedUnit.unitSuffix,
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.mutedText,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                // Mini Audio / Radio readout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (state.activeAudioSource == AudioSource.FM_RADIO) {
                        Icon(
                            imageVector = Icons.Filled.Radio,
                            contentDescription = "FM Radio",
                            tint = ext.amberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f MHz • %s", state.radio.currentFrequencyMhz, state.radio.rdsLabel),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        val track = state.media.currentTrack
                        FilledIconButton(
                            onClick = onTogglePlayPause,
                            enabled = track != null,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("fullmap_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (state.media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Toggle Playback"
                            )
                        }
                        Text(
                            text = track?.title ?: "No Media",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Collapse back to Split Cockpit
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fullmap_exit_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloseFullscreen,
                        contentDescription = "Return to Split Cockpit"
                    )
                }
            }
        }
    }
}

@Composable
fun AudioFocusScreen(
    state: HeadunitUiState,
    onBack: () -> Unit,
    onSelectSource: (AudioSource) -> Unit,
    onPickLocalAudio: () -> Unit,
    onSelectTrack: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onSeekMedia: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onTuneFm: (Float) -> Unit,
    onStepFm: (Float) -> Unit,
    onSeekFmPreset: (Boolean) -> Unit,
    onSelectPreset: (FmPreset) -> Unit,
    onSavePreset: (Int, Float, String) -> Unit
) {
    BackHandler(onBack = onBack)
    val ext = LocalDriveDeckColors.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenTopHeader(
            title = "Audio & FM Radio Console",
            onBack = onBack
        )

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWide = maxWidth >= 500.dp || maxWidth >= maxHeight
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AudioRadioWidgetCard(
                        activeSource = state.activeAudioSource,
                        mediaState = state.media,
                        radioState = state.radio,
                        onSelectSource = onSelectSource,
                        onPickLocalAudio = onPickLocalAudio,
                        onTogglePlayPause = onTogglePlayPause,
                        onNextTrack = onNextTrack,
                        onPreviousTrack = onPreviousTrack,
                        onSeekMedia = onSeekMedia,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onTuneFm = onTuneFm,
                        onStepFm = onStepFm,
                        onSeekFmPreset = onSeekFmPreset,
                        onSelectPreset = onSelectPreset,
                        onSavePreset = onSavePreset,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    PlaylistQueueCard(
                        state = state,
                        onPickLocalAudio = onPickLocalAudio,
                        onSelectTrack = onSelectTrack,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AudioRadioWidgetCard(
                        activeSource = state.activeAudioSource,
                        mediaState = state.media,
                        radioState = state.radio,
                        onSelectSource = onSelectSource,
                        onPickLocalAudio = onPickLocalAudio,
                        onTogglePlayPause = onTogglePlayPause,
                        onNextTrack = onNextTrack,
                        onPreviousTrack = onPreviousTrack,
                        onSeekMedia = onSeekMedia,
                        onToggleShuffle = onToggleShuffle,
                        onToggleRepeat = onToggleRepeat,
                        onTuneFm = onTuneFm,
                        onStepFm = onStepFm,
                        onSeekFmPreset = onSeekFmPreset,
                        onSelectPreset = onSelectPreset,
                        onSavePreset = onSavePreset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp)
                    )
                    PlaylistQueueCard(
                        state = state,
                        onPickLocalAudio = onPickLocalAudio,
                        onSelectTrack = onSelectTrack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistQueueCard(
    state: HeadunitUiState,
    onPickLocalAudio: () -> Unit,
    onSelectTrack: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDriveDeckColors.current
    InstrumentCard(modifier = modifier, contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOCAL QUEUE (${state.media.playlist.size})",
                style = MaterialTheme.typography.labelMedium,
                color = ext.mutedText
            )
            Button(
                onClick = onPickLocalAudio,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("queue_add_audio_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.load_audio_tracks))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (state.media.playlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.panelElevated),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_tracks_loaded),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.no_tracks_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.mutedText
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(state.media.playlist) { index, track ->
                    val isCurrent = index == state.media.currentIndex
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectTrack(index) }
                            .testTag("playlist_item_$index"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else ext.panelElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary else ext.bezelBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = formatDurationMs(track.durationMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.mutedText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClusterFocusScreen(
    state: HeadunitUiState,
    onBack: () -> Unit,
    onToggleSpeedUnit: () -> Unit,
    onToggleDemoDrive: () -> Unit,
    onResetTripStats: () -> Unit,
    onRefreshWeather: () -> Unit
) {
    BackHandler(onBack = onBack)
    val ext = LocalDriveDeckColors.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenTopHeader(
            title = "Instrument Cluster",
            onBack = onBack
        )

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isWide = maxWidth >= 500.dp || maxWidth >= maxHeight
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpeedometerWidgetCard(
                        telemetry = state.telemetry,
                        speedUnit = state.speedUnit,
                        onToggleSpeedUnit = onToggleSpeedUnit,
                        onToggleDemoDrive = onToggleDemoDrive,
                        onOpenCluster = {},
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClockWeatherWidgetCard(
                            clock = state.clock,
                            weather = state.weather,
                            tempUnit = state.tempUnit,
                            speedUnit = state.speedUnit,
                            onRefreshWeather = onRefreshWeather,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

                        TripComputerCard(
                            state = state,
                            onResetTripStats = onResetTripStats,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpeedometerWidgetCard(
                        telemetry = state.telemetry,
                        speedUnit = state.speedUnit,
                        onToggleSpeedUnit = onToggleSpeedUnit,
                        onToggleDemoDrive = onToggleDemoDrive,
                        onOpenCluster = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                    )
                    TripComputerCard(
                        state = state,
                        onResetTripStats = onResetTripStats,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ClockWeatherWidgetCard(
                        clock = state.clock,
                        weather = state.weather,
                        tempUnit = state.tempUnit,
                        speedUnit = state.speedUnit,
                        onRefreshWeather = onRefreshWeather,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TripComputerCard(
    state: HeadunitUiState,
    onResetTripStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDriveDeckColors.current
    InstrumentCard(modifier = modifier, contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Explore,
                    contentDescription = "Heading",
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "HEADING & TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.mutedText
                    )
                    Text(
                        text = String.format(Locale.US, "%03.0f° %s", state.telemetry.bearing, bearingToCardinal(state.telemetry.bearing)),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            OutlinedButton(
                onClick = onResetTripStats,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cluster_reset_trip_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.RestartAlt,
                    contentDescription = "Reset Trip",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Trip")
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: HeadunitUiState,
    onBack: () -> Unit,
    onSelectSpeedUnit: (SpeedUnit) -> Unit,
    onSelectTempUnit: (TempUnit) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggle24HourClock: (Boolean) -> Unit,
    onToggleDemoDrive: () -> Unit,
    onResetTripStats: () -> Unit
) {
    BackHandler(onBack = onBack)
    val ext = LocalDriveDeckColors.current
    val context = LocalContext.current
    var isDefaultHome by remember { mutableStateOf(isDriveDeckDefaultHome(context)) }

    val homeRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultHome = isDriveDeckDefaultHome(context)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenTopHeader(
            title = "Cockpit Settings",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Default Home Launcher Setup
            InstrumentCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HEADUNIT HOME LAUNCHER",
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.mutedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isDefaultHome) "DriveDeck is your default Home app" else "Set DriveDeck as Default Home",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pressing the system Home button will return straight to this cockpit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.mutedText
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val roleManager = context.getSystemService(RoleManager::class.java)
                                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                                    roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                                } else {
                                    Intent(Settings.ACTION_HOME_SETTINGS)
                                }
                            } else {
                                Intent(Settings.ACTION_HOME_SETTINGS)
                            }
                            try {
                                homeRoleLauncher.launch(intent)
                            } catch (_: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("settings_set_default_home_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDefaultHome) ext.gpsActive else MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = if (isDefaultHome) "Default Active" else "Choose Home")
                    }
                }
            }

            // Speedometer & Distance Units
            InstrumentCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SPEEDOMETER & DISTANCE UNITS",
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.mutedText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpeedUnit.entries.forEach { unit ->
                        val selected = state.speedUnit == unit
                        Button(
                            onClick = { onSelectSpeedUnit(unit) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("settings_speed_${unit.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else ext.panelElevated,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = "${unit.unitSuffix} (${unit.label})")
                        }
                    }
                }
            }

            // Day / Night Theme Mode
            InstrumentCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DISPLAY & NIGHT COCKPIT THEME",
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.mutedText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = state.themeMode == mode
                        Button(
                            onClick = { onSelectThemeMode(mode) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("settings_theme_${mode.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else ext.panelElevated,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = mode.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Temperature & Clock Format
            InstrumentCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "WEATHER TEMPERATURE & CLOCK",
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.mutedText
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TempUnit.entries.forEach { unit ->
                        val selected = state.tempUnit == unit
                        Button(
                            onClick = { onSelectTempUnit(unit) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("settings_temp_${unit.name.lowercase()}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else ext.panelElevated,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text(text = "${unit.symbol} (${unit.label})")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "24-Hour Digital Clock",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Switch between 12h AM/PM and 24h military time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.mutedText
                        )
                    }
                    Switch(
                        checked = state.is24HourClock,
                        onCheckedChange = onToggle24HourClock,
                        modifier = Modifier.testTag("settings_24h_switch")
                    )
                }
            }

            // GPS Simulation & Trip Controls
            InstrumentCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Simulated Demo Drive",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Test speedometer needle & map movement indoors",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.mutedText
                        )
                    }
                    Switch(
                        checked = state.telemetry.isDemoDriveActive,
                        onCheckedChange = { onToggleDemoDrive() },
                        modifier = Modifier.testTag("settings_demo_drive_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onResetTripStats,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("settings_reset_trip_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Trip Distance & Max Speed")
                }
            }
        }
    }
}

@Composable
private fun ScreenTopHeader(
    title: String,
    onBack: () -> Unit
) {
    val ext = LocalDriveDeckColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalIconButton(
            onClick = onBack,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("screen_back_button"),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Cockpit"
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun isDriveDeckDefaultHome(context: Context): Boolean {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.resolveActivity(
            homeIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    return resolved?.activityInfo?.packageName == context.packageName
}

private fun bearingToCardinal(bearing: Float): String {
    val normalized = ((bearing % 360f) + 360f) % 360f
    val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val index = ((normalized + 22.5f) / 45f).toInt() % 8
    return dirs[index]
}
