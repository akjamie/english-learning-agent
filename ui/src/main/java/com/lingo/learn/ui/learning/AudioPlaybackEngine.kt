package org.akj.lingo.learn.ui.learning

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstracts platform audio playback so [AudioPlayerController] can drive real
 * TTS audio files while remaining unit-testable without Android [MediaPlayer].
 *
 * Implementations invoke all callbacks on the main thread.
 */
interface AudioPlaybackEngine {
    /**
     * Starts playing [file] at [speed]. Calls [onPrepared] once the duration is
     * known, [onPositionUpdate] periodically with the current position in ms,
     * and [onCompletion] when playback finishes naturally.
     */
    fun play(
        file: File,
        speed: Float,
        onPrepared: (durationMs: Long) -> Unit,
        onPositionUpdate: (positionMs: Long) -> Unit,
        onCompletion: () -> Unit
    )

    /** Pauses the currently playing track. */
    fun pause()

    /** Resumes the paused track from where it left off. */
    fun resume()

    /** Stops playback and releases resources. */
    fun stop()

    /** Updates the playback speed of the currently playing track. */
    fun setSpeed(speed: Float)
}

/**
 * Production [AudioPlaybackEngine] backed by Android [MediaPlayer].
 *
 * Uses a [Handler] on the main looper to poll position updates (every 50ms)
 * since MediaPlayer does not offer a streaming position callback.
 */
@Singleton
class MediaPlayerAudioEngine @Inject constructor() : AudioPlaybackEngine {

    private var player: MediaPlayer? = null
    private var handler: Handler? = null
    private var positionRunnable: Runnable? = null
    private var positionCallback: ((Long) -> Unit)? = null

    override fun play(
        file: File,
        speed: Float,
        onPrepared: (durationMs: Long) -> Unit,
        onPositionUpdate: (positionMs: Long) -> Unit,
        onCompletion: () -> Unit
    ) {
        stop()
        positionCallback = onPositionUpdate
        handler = Handler(Looper.getMainLooper())

        val mp = MediaPlayer()
        player = mp
        try {
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener { player ->
                player.playbackParams = player.playbackParams.setSpeed(speed.coerceAtLeast(0.25f))
                player.start()
                onPrepared(player.duration.toLong())
                startPositionUpdates()
            }
            mp.setOnCompletionListener { player ->
                player.release()
                if (this.player === player) this.player = null
                stopPositionUpdates()
                onCompletion()
            }
            mp.setOnErrorListener { player, _, _ ->
                player.release()
                if (this.player === player) this.player = null
                stopPositionUpdates()
                onCompletion()
                true
            }
            mp.prepareAsync()
        } catch (_: Exception) {
            mp.release()
            player = null
            stopPositionUpdates()
            onCompletion()
        }
    }

    private fun startPositionUpdates() {
        val r = object : Runnable {
            override fun run() {
                val p = player
                if (p != null) {
                    try {
                        if (p.isPlaying) {
                            positionCallback?.invoke(p.currentPosition.toLong())
                        }
                    } catch (_: Exception) {
                        // IllegalStateException if player is in an invalid state; ignore
                    }
                }
                handler?.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
        positionRunnable = r
        handler?.post(r)
    }

    private fun stopPositionUpdates() {
        positionRunnable?.let { handler?.removeCallbacks(it) }
        positionRunnable = null
        positionCallback = null
    }

    override fun pause() {
        try {
            player?.pause()
        } catch (_: Exception) {
            // Ignore state errors
        }
        stopPositionUpdates()
    }

    override fun resume() {
        try {
            player?.start()
            startPositionUpdates()
        } catch (_: Exception) {
            // Ignore state errors
        }
    }

    override fun stop() {
        stopPositionUpdates()
        try {
            player?.let {
                it.stop()
                it.release()
            }
        } catch (_: Exception) {
            // Ignore state errors during cleanup
        }
        player = null
    }

    override fun setSpeed(speed: Float) {
        try {
            player?.let {
                it.playbackParams = it.playbackParams.setSpeed(speed.coerceAtLeast(0.25f))
            }
        } catch (_: Exception) {
            // Ignore if player is not in a state that supports speed changes
        }
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 50L
    }
}
