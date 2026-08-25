package com.mnemosyne.backup

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class DriveClient @Inject constructor() {

    companion object {
        const val BACKUP_PREFIX = "mnemosyne_backup_"
        const val BACKUP_EXTENSION = ".bin"
        const val LEGACY_BACKUP_FILENAME = "mnemosyne_backup.bin"
        const val APP_DATA_FOLDER = "appDataFolder"
        const val MAX_ROLLING_BACKUPS = 7
    }

    private fun getDriveService(accessTokenString: String): Drive {
        val credentials = GoogleCredentials.create(
            AccessToken(accessTokenString, Date(System.currentTimeMillis() + 3600 * 1000))
        ).createScoped(listOf(DriveScopes.DRIVE_APPDATA))

        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        return Drive.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
            .setApplicationName("Mnemosyne")
            .build()
    }

    /**
     * Uploads a timestamped snapshot (e.g. mnemosyne_backup_2026-08-24.bin) and prunes
     * older backups to maintain a rolling 7-day snapshot history.
     */
    open suspend fun uploadBackup(
        accessToken: String,
        encryptedInputStream: InputStream,
        contentLength: Long
    ): String = withContext(Dispatchers.IO) {
        val driveService = getDriveService(accessToken)

        val dateSuffix = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayFilename = "$BACKUP_PREFIX$dateSuffix$BACKUP_EXTENSION"

        // Check if a backup for today already exists
        val todayFiles = driveService.files().list()
            .setSpaces(APP_DATA_FOLDER)
            .setQ("name = '$todayFilename' and trashed = false")
            .setFields("files(id, name)")
            .execute()
            .files

        val mediaContent = InputStreamContent("application/octet-stream", encryptedInputStream).apply {
            length = contentLength
        }

        val uploadedId = if (!todayFiles.isNullOrEmpty()) {
            val fileId = todayFiles[0].id
            val updated = driveService.files().update(fileId, null, mediaContent).execute()
            updated.id
        } else {
            val fileMetadata = File().apply {
                name = todayFilename
                parents = listOf(APP_DATA_FOLDER)
            }
            val created = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            created.id
        }

        // Prune older backups, keeping only the 7 most recent snapshots
        pruneOldBackups(driveService)

        uploadedId
    }

    /**
     * Downloads the most recent backup snapshot available in Google Drive appDataFolder.
     */
    open suspend fun downloadBackup(accessToken: String): InputStream? = withContext(Dispatchers.IO) {
        val driveService = getDriveService(accessToken)

        val allBackups = driveService.files().list()
            .setSpaces(APP_DATA_FOLDER)
            .setQ("trashed = false")
            .setFields("files(id, name, createdTime)")
            .setOrderBy("createdTime desc")
            .execute()
            .files

        if (allBackups.isNullOrEmpty()) return@withContext null

        // Pick latest timestamped backup or legacy backup
        val latestBackup = allBackups.firstOrNull { it.name.startsWith(BACKUP_PREFIX) }
            ?: allBackups.firstOrNull { it.name == LEGACY_BACKUP_FILENAME }
            ?: allBackups.first()

        driveService.files().get(latestBackup.id).executeMediaAsInputStream()
    }

    private fun pruneOldBackups(driveService: Drive) {
        try {
            val files = driveService.files().list()
                .setSpaces(APP_DATA_FOLDER)
                .setQ("trashed = false")
                .setFields("files(id, name, createdTime)")
                .setOrderBy("createdTime desc")
                .execute()
                .files

            if (files.isNullOrEmpty()) return

            val snapshotFiles = files.filter { it.name.startsWith(BACKUP_PREFIX) }
            if (snapshotFiles.size > MAX_ROLLING_BACKUPS) {
                val filesToDelete = snapshotFiles.drop(MAX_ROLLING_BACKUPS)
                for (file in filesToDelete) {
                    driveService.files().delete(file.id).execute()
                    android.util.Log.i("DriveClient", "Pruned old backup snapshot: ${file.name}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DriveClient", "Failed to prune old backups", e)
        }
    }
}
