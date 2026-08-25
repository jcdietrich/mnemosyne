package com.mnemosyne.embedding

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates 512-dimensional text embeddings on-device using MediaPipe Tasks Text (USE model).
 * Governs R3, KD1, KTD2.
 */
@Singleton
open class EmbeddingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val EMBEDDING_DIMENSION = 100
    }

    private var embedder: TextEmbedder? = null

    private fun getOrCreateEmbedder(): TextEmbedder {
        return embedder ?: synchronized(this) {
            embedder ?: run {
                val modelFile = File(context.filesDir, "models/embedding/universal_sentence_encoder.tflite")
                val baseOptions = if (modelFile.exists()) {
                    BaseOptions.builder()
                        .setModelAssetPath(modelFile.absolutePath)
                        .build()
                } else {
                    // Fallback to bundled asset if present
                    BaseOptions.builder()
                        .setModelAssetPath("models/embedding/universal_sentence_encoder.tflite")
                        .build()
                }

                val options = TextEmbedderOptions.builder()
                    .setBaseOptions(baseOptions)
                    .build()

                TextEmbedder.createFromOptions(context, options).also { embedder = it }
            }
        }
    }

    /**
     * Computes on-device text embedding vector.
     */
    open suspend fun embed(text: String): FloatArray = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext FloatArray(EMBEDDING_DIMENSION)

        try {
            val embedderInstance = getOrCreateEmbedder()
            val result = embedderInstance.embed(text)
            val embedding = result.embeddingResult().embeddings().firstOrNull()
            val floats = embedding?.floatEmbedding() ?: FloatArray(EMBEDDING_DIMENSION)
            val nonZero = floats.count { it != 0.0f }
            android.util.Log.i("EmbeddingRepository", "Embedded '$text' -> ${floats.size} dims ($nonZero non-zero)")
            floats
        } catch (e: Exception) {
            android.util.Log.e("EmbeddingRepository", "Failed to embed text: '$text'", e)
            FloatArray(EMBEDDING_DIMENSION)
        }
    }
}
