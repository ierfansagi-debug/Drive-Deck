package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AudioSource
import com.example.model.FmPreset
import com.example.model.MediaState
import com.example.model.RadioState
import com.example.ui.theme.LocalDriveDeckColors
import java.util.Locale
import kotlin.math.abs

@Composable
fun AudioRadioWidgetCard(
    activeSource: AudioSource,
    mediaState: MediaState,
    radioState: RadioState,
    onSelectSource: (AudioSource) -> Unit,
    onPickLocalAudio: () -> Unit,
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
    onSavePreset: (Int, Float, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDriveDeckColors.current

    InstrumentCard(
        modifier = modifier.testTag("audio_radio_card"),
        contentPadding = 12.dp
    ) {
        // Source Switcher Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.panelElevated)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SourceTabChip(
                    label = "LOCAL MEDIA",
                    icon = Icons.Filled.LibraryMusic,
                    selected = activeSource == AudioSource.MEDIA,
                    onClick = { onSelectSource(AudioSource.MEDIA) },
                    testTag = "tab_local_media"
                )
                SourceTabChip(
                    label = "FM TUNER",
                    icon = Icons.Filled.Radio,
                    selected = activeSource == AudioSource.FM_RADIO,
                    onClick = { onSelectSource(AudioSource.FM_RADIO) },
                    testTag = "tab_fm_tuner"
                )
            }

            if (activeSource == AudioSource.MEDIA) {
                Button(
                    onClick = onPickLocalAudio,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_local_audio_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.load_audio_tracks),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.load_audio_tracks),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                // Signal bars for FM
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    for (bar in 1..5) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height((6 + bar * 3).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (bar <= radioState.signalStrength) ext.amberAccent
                                    else ext.bezelBorder
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedContent(
            targetState = activeSource,
            label = "audio_source_switch",
            modifier = Modifier.weight(1f)
        ) { source ->
            when (source) {
                AudioSource.MEDIA -> LocalMediaPanel(
                    mediaState = mediaState,
                    onPickLocalAudio = onPickLocalAudio,
                    onTogglePlayPause = onTogglePlayPause,
                    onNextTrack = onNextTrack,
                    onPreviousTrack = onPreviousTrack,
                    onSeekMedia = onSeekMedia,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat
                )

                AudioSource.FM_RADIO -> FmRadioPanel(
                    radioState = radioState,
                    onTuneFm = onTuneFm,
                    onStepFm = onStepFm,
                    onSeekFmPreset = onSeekFmPreset,
                    onSelectPreset = onSelectPreset,
                    onSavePreset = onSavePreset
                )
            }
        }
    }
}

@Composable
private fun SourceTabChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val ext = LocalDriveDeckColors.current
    Surface(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else ext.panelElevated,
        border = if (selected) BorderStroke(1.dp, ext.bezelBorder) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else ext.mutedText,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onSurface else ext.mutedText
            )
        }
    }
}

@Composable
private fun LocalMediaPanel(
    mediaState: MediaState,
    onPickLocalAudio: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextTrack: () -> Unit,
    onPreviousTrack: () -> Unit,
    onSeekMedia: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    val ext = LocalDriveDeckColors.current
    val track = mediaState.currentTrack

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val bitmap = remember(track?.artworkBytes) {
                track?.artworkBytes?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Track Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.img_audio_deck_art_1795182218558),
                    contentDescription = "DriveDeck Audio Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track?.title ?: stringResource(R.string.no_tracks_loaded),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track?.artist ?: stringResource(R.string.no_tracks_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.mutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Progress Slider
        Column(modifier = Modifier.fillMaxWidth()) {
            val duration = mediaState.durationMs.coerceAtLeast(1L)
            val progressFraction = (mediaState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            Slider(
                value = progressFraction,
                onValueChange = { frac -> onSeekMedia((frac * duration).toLong()) },
                enabled = track != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .testTag("media_seek_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = ext.bezelBorder
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDurationMs(mediaState.positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.mutedText
                )
                Text(
                    text = if (track != null) formatDurationMs(mediaState.durationMs) else "--:--",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.mutedText
                )
            }
        }

        // Transport Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("media_shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (mediaState.isShuffle) MaterialTheme.colorScheme.primary else ext.mutedText
                )
            }

            FilledTonalIconButton(
                onClick = onPreviousTrack,
                enabled = track != null,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("media_prev_button")
            ) {
                Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous Track")
            }

            FilledIconButton(
                onClick = {
                    if (track == null) onPickLocalAudio() else onTogglePlayPause()
                },
                modifier = Modifier
                    .size(54.dp)
                    .testTag("media_play_pause_button"),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (mediaState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (mediaState.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(28.dp)
                )
            }

            FilledTonalIconButton(
                onClick = onNextTrack,
                enabled = track != null,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("media_next_button")
            ) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next Track")
            }

            IconButton(
                onClick = onToggleRepeat,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("media_repeat_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (mediaState.isRepeat) MaterialTheme.colorScheme.primary else ext.mutedText
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FmRadioPanel(
    radioState: RadioState,
    onTuneFm: (Float) -> Unit,
    onStepFm: (Float) -> Unit,
    onSeekFmPreset: (Boolean) -> Unit,
    onSelectPreset: (FmPreset) -> Unit,
    onSavePreset: (Int, Float, String) -> Unit
) {
    val ext = LocalDriveDeckColors.current
    var editingPreset by remember { mutableStateOf<FmPreset?>(null) }
    var editStationName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Frequency Readout & Fine-Tune Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", radioState.currentFrequencyMhz),
                        style = MaterialTheme.typography.displaySmall,
                        color = ext.amberAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MHz",
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.mutedText,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                Text(
                    text = if (radioState.isScanning) "SCANNING..." else "RDS: ${radioState.rdsLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalIconButton(
                    onClick = { onSeekFmPreset(false) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fm_seek_prev_button")
                ) {
                    Icon(imageVector = Icons.Filled.FastRewind, contentDescription = "Seek Previous FM Preset")
                }
                FilledTonalIconButton(
                    onClick = { onStepFm(-0.1f) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fm_step_down_button")
                ) {
                    Text("-0.1", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalIconButton(
                    onClick = { onStepFm(0.1f) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fm_step_up_button")
                ) {
                    Text("+0.1", style = MaterialTheme.typography.labelSmall)
                }
                FilledTonalIconButton(
                    onClick = { onSeekFmPreset(true) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fm_seek_next_button")
                ) {
                    Icon(imageVector = Icons.Filled.FastForward, contentDescription = "Seek Next FM Preset")
                }
            }
        }

        // Interactive Canvas Frequency Scale (87.5 .. 108.0 MHz)
        val amberColor = ext.amberAccent
        val borderColor = ext.bezelBorder
        val mutedColor = ext.mutedText
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ext.panelElevated)
                .pointerInput(radioState.currentFrequencyMhz) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        val mhzPerPx = 20.5f / size.width.coerceAtLeast(1)
                        onTuneFm(radioState.currentFrequencyMhz + dragAmount * mhzPerPx)
                    }
                }
                .testTag("fm_tuner_scale")
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val minFreq = 87.5f
                val maxFreq = 108.0f
                val span = maxFreq - minFreq
                val tickSteps = 41 // every 0.5 MHz
                for (i in 0..tickSteps) {
                    val x = size.width * (i / tickSteps.toFloat())
                    val isMajor = i % 4 == 0
                    val topY = if (isMajor) size.height * 0.20f else size.height * 0.45f
                    drawLine(
                        color = if (isMajor) mutedColor else borderColor,
                        start = Offset(x, topY),
                        end = Offset(x, size.height),
                        strokeWidth = if (isMajor) 2.5f else 1.5f,
                        cap = StrokeCap.Round
                    )
                }

                val needleFrac = ((radioState.currentFrequencyMhz - minFreq) / span).coerceIn(0f, 1f)
                val needleX = size.width * needleFrac
                drawLine(
                    color = amberColor,
                    start = Offset(needleX, 0f),
                    end = Offset(needleX, size.height),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 6 Persistent FM Preset Chips (Tap to tune, Long-press to save current MHz)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val rows = radioState.presets.chunked(3)
            rows.forEach { rowPresets ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowPresets.forEach { preset ->
                        val isActive = abs(preset.frequencyMhz - radioState.currentFrequencyMhz) < 0.06f
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .combinedClickable(
                                    onClick = { onSelectPreset(preset) },
                                    onLongClick = {
                                        editingPreset = preset
                                        editStationName = preset.stationName
                                    }
                                )
                                .testTag("fm_preset_${preset.index}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isActive) ext.amberAccent.copy(alpha = 0.18f) else ext.panelElevated,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isActive) ext.amberAccent else ext.bezelBorder
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", preset.frequencyMhz),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isActive) ext.amberAccent else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = preset.stationName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editingPreset?.let { target ->
        AlertDialog(
            onDismissRequest = { editingPreset = null },
            title = {
                Text(
                    text = "Save ${String.format(Locale.US, "%.1f MHz", radioState.currentFrequencyMhz)} to Preset ${target.index + 1}"
                )
            },
            text = {
                OutlinedTextField(
                    value = editStationName,
                    onValueChange = { editStationName = it },
                    label = { Text("Station Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSavePreset(target.index, radioState.currentFrequencyMhz, editStationName)
                        editingPreset = null
                    }
                ) {
                    Text("Save Preset")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPreset = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
