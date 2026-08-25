package com.mnemosyne.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mnemosyne.backup.BackupRepository
import com.mnemosyne.backup.DriveClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import javax.inject.Inject

sealed interface BackupUiState {
    data object Idle : BackupUiState
    data class Working(val message: String) : BackupUiState
    data class Success(val message: String) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val backupRepository: BackupRepository,
    private val driveClient: DriveClient
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("mnemosyne_auth", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    val userEmail: String?
        get() = sharedPrefs.getString("user_email", null)

    fun saveAuthToken(email: String, token: String) {
        sharedPrefs.edit()
            .putString("user_email", email)
            .putString("drive_access_token", token)
            .apply()
        backupRepository.schedulePeriodicBackup()
    }

    fun clearAuth() {
        sharedPrefs.edit().clear().apply()
    }

    fun backupNow() {
        val token = sharedPrefs.getString("drive_access_token", null)
        if (token == null) {
            _uiState.value = BackupUiState.Error("Please sign in with Google first.")
            return
        }

        viewModelScope.launch {
            _uiState.value = BackupUiState.Working("Encrypting and backing up memories...")
            try {
                val payload = backupRepository.createEncryptedBackupPayload()
                driveClient.uploadBackup(token, ByteArrayInputStream(payload), payload.size.toLong())
                _uiState.value = BackupUiState.Success("Backup successfully uploaded to Google Drive.")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Backup failed: ${e.localizedMessage ?: "Network error"}")
            }
        }
    }

    fun restoreNow() {
        val token = sharedPrefs.getString("drive_access_token", null)
        if (token == null) {
            _uiState.value = BackupUiState.Error("Please sign in with Google first.")
            return
        }

        viewModelScope.launch {
            _uiState.value = BackupUiState.Working("Downloading and restoring memories...")
            try {
                val inputStream = driveClient.downloadBackup(token)
                if (inputStream == null) {
                    _uiState.value = BackupUiState.Error("No backup found in Google Drive.")
                    return@launch
                }
                val bytes = inputStream.readBytes()
                val count = backupRepository.restoreFromEncryptedPayload(bytes)
                _uiState.value = BackupUiState.Success("Restored $count memories successfully.")
            } catch (e: Exception) {
                _uiState.value = BackupUiState.Error("Restore failed: ${e.localizedMessage ?: "Decryption or network error"}")
            }
        }
    }

    fun resetState() {
        _uiState.value = BackupUiState.Idle
    }
}
