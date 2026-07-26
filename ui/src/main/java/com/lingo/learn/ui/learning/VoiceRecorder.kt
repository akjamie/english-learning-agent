package com.lingo.learn.ui.learning

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android [MediaRecorder] to capture the child's voice into a temporary .wav/.m4a file
 * for ASR pronunciation evaluation. Falls back gracefully if the recorder is unavailable.
 */
@Singleton
class VoiceRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    /**
     * Starts recording audio to a temp file. Returns the output file path or null on failure.
     */
    fun startRecording(): File? {
        return try {
            val dir = File(context.cacheDir, "recordings").apply { if (!exists()) mkdirs() }
            val file = File(dir, "recording_${System.currentTimeMillis()}.m4a")
            outputFile = file

            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            file
        } catch (e: Exception) {
            recorder = null
            outputFile = null
            null
        }
    }

    /**
     * Stops recording and returns the saved audio file, or null if recording failed.
     */
    fun stopRecording(): File? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            outputFile
        } catch (e: Exception) {
            recorder = null
            outputFile
        }
    }

    /**
     * Cancels the current recording and deletes the temp file.
     */
    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}
