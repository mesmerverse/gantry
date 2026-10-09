package com.mesmerverse.gantry.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class MediaState(
    val title: String,
    val artist: String?,
    val playing: Boolean,
    val packageName: String,
    val art: ImageBitmap?,
)

/**
 * Currently playing media via MediaSessionManager. Requires the user to
 * grant notification access (opt-in from Settings); emits null without it,
 * or when no session exists. A paused session is kept — apps often
 * deactivate their session on pause, but the controller stays usable until
 * the session is destroyed (playback shut down or the app killed).
 */
class MediaRepository(private val context: Context) {

    private val listenerComponent =
        ComponentName(context, MediaNotificationListener::class.java)

    private var controller: MediaController? = null

    val state: Flow<MediaState?> = callbackFlow {
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val handler = Handler(Looper.getMainLooper())
        var attached: MediaController? = null

        fun read(): MediaState? {
            val current = attached ?: return null
            val metadata = current.metadata ?: return null
            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: return null
            val art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            return MediaState(
                title = title,
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                playing = current.playbackState?.state == PlaybackState.STATE_PLAYING,
                packageName = current.packageName,
                art = art?.asImageBitmap(),
            )
        }

        var reattach: () -> Unit = {}
        val controllerCallback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                trySend(read())
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                trySend(read())
            }

            // Some apps tear down the session (or die) without the sessions
            // listener firing; re-query so the widget clears immediately.
            override fun onSessionDestroyed() {
                reattach()
            }
        }

        fun attach(controllers: List<MediaController>?, dropAttached: Boolean = false) {
            if (dropAttached) {
                attached?.unregisterCallback(controllerCallback)
                attached = null
            }
            // Prefer whatever is actually playing; otherwise hold on to the
            // current session (pausing often deactivates it, dropping it from
            // the active list while the controller still works) before falling
            // back to any idle session with metadata.
            val candidate = controllers?.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: attached ?: controllers?.firstOrNull { it.metadata != null }
            if (candidate !== attached) {
                attached?.unregisterCallback(controllerCallback)
                attached = candidate
                attached?.registerCallback(controllerCallback, handler)
            }
            controller = attached
            trySend(read())
        }

        reattach = {
            // The destroyed session must not be held onto — dropAttached lets
            // the re-query land on a live session or clear the widget.
            runCatching { attach(manager.getActiveSessions(listenerComponent), dropAttached = true) }
                .onFailure { trySend(null) }
        }

        val sessionsListener =
            MediaSessionManager.OnActiveSessionsChangedListener { attach(it) }
        val hasAccess = runCatching {
            manager.addOnActiveSessionsChangedListener(
                sessionsListener, listenerComponent, handler
            )
            attach(manager.getActiveSessions(listenerComponent))
        }.isSuccess
        if (!hasAccess) trySend(null)

        awaitClose {
            runCatching { manager.removeOnActiveSessionsChangedListener(sessionsListener) }
            attached?.unregisterCallback(controllerCallback)
            controller = null
        }
    }

    fun playPause() {
        val current = controller ?: return
        if (current.playbackState?.state == PlaybackState.STATE_PLAYING) {
            current.transportControls.pause()
        } else {
            current.transportControls.play()
        }
    }

    fun next() {
        controller?.transportControls?.skipToNext()
    }

    fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    /** Jumps back 15 s — an exact seek when supported, the app's own rewind otherwise. */
    fun rewind() = seekBy(-15_000L)

    /** Jumps ahead 30 s — an exact seek when supported, the app's own fast-forward otherwise. */
    fun fastForward() = seekBy(30_000L)

    private fun seekBy(deltaMs: Long) {
        val current = controller ?: return
        val state = current.playbackState ?: return
        if (state.actions and PlaybackState.ACTION_SEEK_TO == 0L) {
            if (deltaMs < 0) {
                current.transportControls.rewind()
            } else {
                current.transportControls.fastForward()
            }
            return
        }
        // state.position is a snapshot; extrapolate while playing.
        val position = if (state.state == PlaybackState.STATE_PLAYING) {
            state.position +
                ((SystemClock.elapsedRealtime() - state.lastPositionUpdateTime) * state.playbackSpeed).toLong()
        } else {
            state.position
        }
        current.transportControls.seekTo((position + deltaMs).coerceAtLeast(0L))
    }
}
