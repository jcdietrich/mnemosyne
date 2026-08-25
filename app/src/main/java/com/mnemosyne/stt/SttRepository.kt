package com.mnemosyne.stt

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device speech-to-text using sherpa-onnx.
 * Models are expected at [filesDir]/models/stt/ after the download step (U3).
 * No audio or transcript leaves the device (R1, KD1).
 */
@Singleton
class SttRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SAMPLE_RATE = 16_000
    }

    // Lazily initialized; will throw if models not yet downloaded
    private val recognizer: OfflineRecognizer by lazy { buildRecognizer() }

    /**
     * Transcribe a raw 16 kHz PCM audio buffer.
     * Returns the transcript string, or empty string if nothing was recognized.
     * Runs on [Dispatchers.Default] — caller need not switch dispatchers.
     */
    suspend fun transcribe(audio: ShortArray): String = withContext(Dispatchers.Default) {
        android.util.Log.i("SttRepository", "Transcribing audio buffer of ${audio.size} samples (${audio.size / 16000f}s)...")
        if (audio.isEmpty()) return@withContext ""

        // Calculate max amplitude to determine if digital gain boost/normalization is needed
        var maxAmp = 0
        for (sample in audio) {
            val abs = kotlin.math.abs(sample.toInt())
            if (abs > maxAmp) maxAmp = abs
        }

        // Apply automatic gain scaling if recorded below 12000 peak (target ~24000)
        val scaleFactor = if (maxAmp in 1..12000) {
            (24000f / maxAmp).coerceAtMost(6.0f)
        } else {
            1.0f
        }
        android.util.Log.i("SttRepository", "Peak amplitude=$maxAmp, applied gain scale=$scaleFactor")

        val stream: OfflineStream = recognizer.createStream()
        // Convert ShortArray to FloatArray normalized to [-1, 1] with gain scaling
        val floats = FloatArray(audio.size) { i ->
            val boosted = (audio[i] * scaleFactor).coerceIn(-32768f, 32767f)
            boosted / 32768f
        }
        stream.acceptWaveform(floats, sampleRate = SAMPLE_RATE)
        recognizer.decode(stream)
        val result = recognizer.getResult(stream).text.trim()
        stream.release()
        android.util.Log.i("SttRepository", "Transcription result: '$result'")
        result
    }

    private fun buildRecognizer(): OfflineRecognizer {
        val modelDir = File(context.filesDir, "models/stt")
        modelDir.mkdirs()

        val encoderFile = File(modelDir, "encoder.onnx")
        val decoderFile = File(modelDir, "decoder.onnx")
        val tokensFile = File(modelDir, "tokens.txt")

        // If models are bundled in assets, copy them to filesDir if not already present
        if (!encoderFile.exists() || !decoderFile.exists() || !tokensFile.exists()) {
            copyAssetFile("models/stt/encoder.onnx", encoderFile)
            copyAssetFile("models/stt/decoder.onnx", decoderFile)
            copyAssetFile("models/stt/tokens.txt", tokensFile)
        }

        val config = OfflineRecognizerConfig(
            modelConfig = com.k2fsa.sherpa.onnx.OfflineModelConfig(
                whisper = com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig(
                    encoder = encoderFile.absolutePath,
                    decoder = decoderFile.absolutePath,
                    language = "en",
                    task = "transcribe"
                ),
                tokens = tokensFile.absolutePath,
                numThreads = 2,
                debug = false,
                provider = "cpu"
            )
        )
        return OfflineRecognizer(assetManager = null, config = config)
    }

    private fun copyAssetFile(assetPath: String, destFile: File) {
        try {
            context.assets.open(assetPath).use { input ->
                java.io.FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            // Ignored if asset not present
        }
    }
}
