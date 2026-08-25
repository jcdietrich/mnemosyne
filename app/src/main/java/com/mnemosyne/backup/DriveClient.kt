package com.mnemosyne.backup

import android.content.Context
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
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class DriveClient @Inject constructor() {

    companion object {
        const val BACKUP_FILENAME = "mnemosyne_backup.bin"
        const val APP_DATA_FOLDER = "appDataFolder"
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

    open suspend fun uploadBackup(
        accessToken: String,
        encryptedInputStream: InputStream,
        contentLength: Long
    ): String = withContext(Dispatchers.IO) {
        val driveService = getDriveService(accessToken)

        // Check if an existing backup file exists in appDataFolder
        val existingFiles = driveService.files().list()
            .setSpaces(APP_DATA_FOLDER)
            .setQ("name = '$BACKUP_FILENAME' and trashed = false")
            .setFields("files(id, name)")
            .execute()
            .files

        val fileMetadata = File().apply {
            name = BACKUP_FILENAME
            parents = listOf(APP_DATA_FOLDER)
        }

        val mediaContent = InputStreamContent("application/octet-stream", encryptedInputStream).apply {
            length = contentLength
        }

        if (!existingFiles.isNullOrEmpty()) {
            val fileId = existingFiles[0].id
            val updated = driveService.files().update(fileId, null, mediaContent).execute()
            updated.id
        } else {
            val created = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            created.id
        }
    }

    open suspend fun downloadBackup(accessToken: String): InputStream? = withContext(Dispatchers.IO) {
        val driveService = getDriveService(accessToken)

        val existingFiles = driveService.files().list()
            .setSpaces(APP_DATA_FOLDER)
            .setQ("name = '$BACKUP_FILENAME' and trashed = false")
            .setFields("files(id, name)")
            .execute()
            .files

        if (existingFiles.isNullOrEmpty()) return@withContext null

        val fileId = existingFiles[0].id
        driveService.files().get(fileId).executeMediaAsInputStream()
    }
}
