package com.mnemosyne.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val driveClient: DriveClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Retrieve Google Drive token if user is signed in
            val sharedPrefs = applicationContext.getSharedPreferences("mnemosyne_auth", Context.MODE_PRIVATE)
            val accessToken = sharedPrefs.getString("drive_access_token", null) ?: return@withContext Result.success()

            val encryptedBackup = backupRepository.createEncryptedBackupPayload()
            val stream = ByteArrayInputStream(encryptedBackup)

            driveClient.uploadBackup(accessToken, stream, encryptedBackup.size.toLong())
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
