package com.mnemosyne.data

import com.mnemosyne.crypto.CryptoManager
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.query.OrderFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class MemoryRepository @Inject constructor(
    private val boxStore: BoxStore,
    private val cryptoManager: CryptoManager
) {
    private val box: Box<Memory> = boxStore.boxFor(Memory::class.java)

    private val _memoriesFlow = MutableStateFlow<List<Memory>>(emptyList())
    val memoriesFlow: Flow<List<Memory>> = _memoriesFlow.asStateFlow()

    init {
        refreshFlow()
    }

    private fun refreshFlow() {
        _memoriesFlow.value = getAllSync()
    }

    /**
     * Saves a Memory entity, encrypting the plaintext transcript before persistence.
     */
    open suspend fun save(memory: Memory): Long = withContext(Dispatchers.IO) {
        val encryptedTranscript = if (memory.transcript.isNotEmpty()) {
            cryptoManager.encryptString(memory.transcript)
        } else {
            ""
        }
        val entityToSave = memory.copy(transcript = encryptedTranscript)
        val id = box.put(entityToSave)
        refreshFlow()
        id
    }

    /**
     * Retrieves all memories sorted reverse-chronologically with decrypted transcripts.
     */
    open suspend fun getAll(): List<Memory> = withContext(Dispatchers.IO) {
        getAllSync()
    }

    /**
     * Retrieves a paginated slice of memories sorted reverse-chronologically.
     */
    open suspend fun getPaged(offset: Long, limit: Long): List<Memory> = withContext(Dispatchers.IO) {
        val rawEntities = box.query()
            .order(Memory_.timestampUtcMs, OrderFlags.DESCENDING)
            .build()
            .find(offset, limit)

        rawEntities.map { decryptMemory(it) }
    }

    private fun getAllSync(): List<Memory> {
        val rawEntities = box.query()
            .order(Memory_.timestampUtcMs, OrderFlags.DESCENDING)
            .build()
            .find(0, 50) // Default initial chunk

        return rawEntities.map { decryptMemory(it) }
    }

    /**
     * Performs approximate nearest neighbor (ANN) vector search via ObjectBox HNSW index.
     */
    open suspend fun searchByVector(queryVector: FloatArray, limit: Long = 20): List<Memory> = withContext(Dispatchers.IO) {
        if (queryVector.isEmpty() || queryVector.all { it == 0.0f }) {
            android.util.Log.w("MemoryRepository", "Query vector empty or all zeros, returning all")
            return@withContext getAllSync()
        }

        try {
            val query = box.query(
                Memory_.embeddingVector.nearestNeighbors(queryVector, limit.toInt())
            ).build()

            val results = query.findWithScores()
            query.close()
            android.util.Log.i("MemoryRepository", "ObjectBox vector search found ${results.size} neighbors")

            results.map { result ->
                decryptMemory(result.get())
            }
        } catch (e: Exception) {
            android.util.Log.e("MemoryRepository", "Vector search query failed", e)
            getAllSync()
        }
    }

    /**
     * Deletes a memory by ID.
     */
    open suspend fun delete(id: Long): Boolean = withContext(Dispatchers.IO) {
        val deleted = box.remove(id)
        if (deleted) refreshFlow()
        deleted
    }

    /**
     * Deletes all memories (used during full restore).
     */
    open suspend fun deleteAll(): Unit = withContext(Dispatchers.IO) {
        box.removeAll()
        refreshFlow()
    }

    private fun decryptMemory(raw: Memory): Memory {
        val decryptedTranscript = if (raw.transcript.isNotEmpty()) {
            try {
                cryptoManager.decryptString(raw.transcript)
            } catch (e: Exception) {
                "[Decryption Error]"
            }
        } else {
            ""
        }
        return raw.copy(transcript = decryptedTranscript)
    }
}
