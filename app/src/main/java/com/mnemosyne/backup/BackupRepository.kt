package com.mnemosyne.backup

import android.content.Context
import androidx.work.*
import com.mnemosyne.crypto.CryptoManager
import com.mnemosyne.data.Memory
import com.mnemosyne.data.MemoryRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryRepository: MemoryRepository,
    private val cryptoManager: CryptoManager,
    private val driveClient: DriveClient
) {
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val memoryAdapter = moshi.adapter(Memory::class.java)

    fun schedulePeriodicBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only (R13)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "MnemosyneBackupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }

    open suspend fun createEncryptedBackupPayload(): ByteArray = withContext(Dispatchers.IO) {
        val allMemories = memoryRepository.getAll()
        val jsonLines = buildString {
            for (memory in allMemories) {
                append(memoryAdapter.toJson(memory))
                append("\n")
            }
        }
        cryptoManager.encrypt(jsonLines.toByteArray(Charsets.UTF_8))
    }

    open suspend fun restoreFromEncryptedPayload(encryptedBytes: ByteArray): Int = withContext(Dispatchers.IO) {
        val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
        val jsonString = String(decryptedBytes, Charsets.UTF_8)

        val lines = jsonString.lines().filter { it.isNotBlank() }
        memoryRepository.deleteAll()

        var importedCount = 0
        for (line in lines) {
            val memory = memoryAdapter.fromJson(line)
            if (memory != null) {
                memoryRepository.save(memory)
                importedCount++
            }
        }
        importedCount
    }
}
