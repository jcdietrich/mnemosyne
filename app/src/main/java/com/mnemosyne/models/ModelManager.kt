package com.mnemosyne.models

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ModelStatus {
    Ready,
    NotReady,
    Error
}

@Singleton
open class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {
    companion object {
        // Official ONNX Zipformer Chinese-English speech recognition model asset bundle
        const val STT_MODEL_URL = "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20.tar.bz2"
        // Official MediaPipe Universal Sentence Encoder Lite model
        const val EMBEDDING_MODEL_URL = "https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/latest/universal_sentence_encoder.tflite"
    }

    private val sttDir by lazy { File(context.filesDir, "models/stt") }
    private val embeddingDir by lazy { File(context.filesDir, "models/embedding") }

    open fun areModelsReady(): Boolean {
        val encoder = File(sttDir, "encoder.onnx")
        val decoder = File(sttDir, "decoder.onnx")
        val tokens = File(sttDir, "tokens.txt")
        val filesDirReady = encoder.exists() && decoder.exists() && tokens.exists()

        val assetReady = try {
            context.assets.list("models/stt")?.contains("tokens.txt") == true
        } catch (e: Exception) {
            false
        }

        return filesDirReady || assetReady
    }

    open suspend fun ensureModels(
        onProgress: (progressPercent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _, _ -> }
    ): ModelStatus = withContext(Dispatchers.IO) {
        if (areModelsReady()) return@withContext ModelStatus.Ready

        try {
            sttDir.mkdirs()
            embeddingDir.mkdirs()

            val targetEmbeddingFile = File(embeddingDir, "universal_sentence_encoder.tflite")
            if (!targetEmbeddingFile.exists()) {
                downloadFile(EMBEDDING_MODEL_URL, targetEmbeddingFile, onProgress)
            }

            ModelStatus.Ready
        } catch (e: Exception) {
            ModelStatus.Error
        }
    }

    private fun downloadFile(
        url: String,
        target: File,
        onProgress: (progressPercent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ) {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) throw IllegalStateException("Download failed with code ${response.code}")

        val body = response.body ?: throw IllegalStateException("Empty response body")
        val totalLength = body.contentLength()
        val tempFile = File(target.parentFile, "${target.name}.tmp")

        body.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytesCopied = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesCopied += read
                    val progress = if (totalLength > 0) ((bytesCopied * 100) / totalLength).toInt() else 0
                    onProgress(progress, bytesCopied, totalLength)
                }
            }
        }

        if (tempFile.renameTo(target)) {
            tempFile.delete()
        }
    }
}
