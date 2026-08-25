package com.mnemosyne.stt

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Captures raw PCM audio at 16 kHz, 16-bit mono — the format sherpa-onnx expects.
 * Call [start] to begin recording (returns immediately), then [stop] to flush and
 * return the captured [ShortArray].
 */
@Singleton
class AudioRecorder @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private val buffer = mutableListOf<Short>()
    private var recording = false

    /** Begin recording. Must be called with RECORD_AUDIO permission granted. */
    fun start() {
        if (recording) return
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            minBuffer * 4
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            android.util.Log.e("AudioRecorder", "AudioRecord initialization failed, state=${record.state}")
            return
        }
        audioRecord = record
        buffer.clear()
        recording = true
        record.startRecording()
        android.util.Log.i("AudioRecorder", "Recording started successfully with VOICE_RECOGNITION source, minBuffer=$minBuffer")

        // Read on a background thread until stopped
        Thread {
            val chunk = ShortArray(minBuffer)
            while (recording) {
                val read = record.read(chunk, 0, chunk.size)
                if (read > 0) {
                    synchronized(buffer) { buffer.addAll(chunk.take(read)) }
                }
            }
        }.start()
    }

    suspend fun stop(): ShortArray = withContext(Dispatchers.IO) {
        recording = false
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            android.util.Log.e("AudioRecorder", "Error stopping AudioRecord", e)
        }
        audioRecord?.release()
        audioRecord = null
        Thread.sleep(80)
        val result = synchronized(buffer) { buffer.toShortArray() }
        android.util.Log.i("AudioRecorder", "Recording stopped, captured ${result.size} samples (${result.size / 16000f}s)")
        result
    }
}
