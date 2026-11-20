package com.example.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.AudioTrack
import com.example.model.MediaState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalMediaManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState: StateFlow<MediaState> = _mediaState.asStateFlow()

    private var progressJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _mediaState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startProgressLoop() else stopProgressLoop()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = exoPlayer.currentMediaItemIndex
                val duration = exoPlayer.duration.takeIf { it > 0 }
                    ?: _mediaState.value.playlist.getOrNull(index)?.durationMs
                    ?: 0L
                _mediaState.update {
                    it.copy(
                        currentIndex = index.coerceAtLeast(0),
                        positionMs = 0L,
                        durationMs = duration
                    )
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val duration = exoPlayer.duration.takeIf { it > 0 }
                        ?: _mediaState.value.currentTrack?.durationMs
                        ?: 0L
                    _mediaState.update { it.copy(durationMs = duration) }
                }
            }
        })
    }

    fun addAudioUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        scope.launch {
            val newTracks = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri -> extractTrackMetadata(uri) }
            }
            if (newTracks.isEmpty()) return@launch

            val existing = _mediaState.value.playlist
            val merged = (existing + newTracks).distinctBy { it.uriString }
            val wasEmpty = existing.isEmpty()

            _mediaState.update {
                it.copy(
                    playlist = merged,
                    durationMs = if (wasEmpty) merged.first().durationMs else it.durationMs
                )
            }

            withContext(Dispatchers.Main) {
                if (wasEmpty) {
                    exoPlayer.setMediaItems(merged.map { MediaItem.fromUri(it.uriString) })
                    exoPlayer.prepare()
                    exoPlayer.play()
                } else {
                    val existingUris = existing.map { it.uriString }.toSet()
                    val added = newTracks.filter { it.uriString !in existingUris }
                    if (added.isNotEmpty()) {
                        exoPlayer.addMediaItems(added.map { MediaItem.fromUri(it.uriString) })
                    }
                }
            }
        }
    }

    private fun extractTrackMetadata(uri: Uri): AudioTrack? {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
            // Non-persistable URI is still playable during the active session
        }

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }
                ?: queryDisplayName(uri)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: "Local Audio"
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val art = retriever.embeddedPicture

            AudioTrack(
                uriString = uri.toString(),
                title = title,
                artist = artist,
                durationMs = durationMs,
                artworkBytes = art
            )
        } catch (_: Exception) {
            AudioTrack(
                uriString = uri.toString(),
                title = queryDisplayName(uri),
                artist = "Local Audio",
                durationMs = 0L,
                artworkBytes = null
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    val raw = cursor.getString(index)
                    if (!raw.isNullOrBlank()) {
                        return raw.substringBeforeLast('.')
                    }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Audio Track"
    }

    fun togglePlayPause() {
        if (_mediaState.value.playlist.isEmpty()) return
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        }
    }

    fun play() {
        if (_mediaState.value.playlist.isEmpty()) return
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.play()
    }

    fun pause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    fun next() {
        if (_mediaState.value.playlist.isEmpty()) return
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
            exoPlayer.play()
        } else {
            exoPlayer.seekTo(0, 0L)
            exoPlayer.play()
        }
    }

    fun previous() {
        if (_mediaState.value.playlist.isEmpty()) return
        if (exoPlayer.currentPosition > 3000L) {
            exoPlayer.seekTo(0L)
        } else if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
            exoPlayer.play()
        } else {
            exoPlayer.seekTo(0L)
        }
    }

    fun selectTrack(index: Int) {
        if (index !in _mediaState.value.playlist.indices) return
        exoPlayer.seekTo(index, 0L)
        exoPlayer.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
        _mediaState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun toggleShuffle() {
        val nextShuffle = !_mediaState.value.isShuffle
        exoPlayer.shuffleModeEnabled = nextShuffle
        _mediaState.update { it.copy(isShuffle = nextShuffle) }
    }

    fun toggleRepeat() {
        val nextRepeat = !_mediaState.value.isRepeat
        exoPlayer.repeatMode = if (nextRepeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        _mediaState.update { it.copy(isRepeat = nextRepeat) }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _mediaState.update { state ->
                    val dur = exoPlayer.duration.takeIf { it > 0 } ?: state.durationMs
                    state.copy(
                        positionMs = exoPlayer.currentPosition.coerceAtLeast(0L),
                        durationMs = dur
                    )
                }
                delay(400L)
            }
        }
    }

    private fun stopProgressLoop() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressLoop()
        exoPlayer.release()
    }
}
