package com.mnemosyne.permissions

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

enum class PermissionState { Denied, Granted }

@HiltViewModel
class PermissionViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(computeState())
    val state: StateFlow<PermissionState> = _state

    fun onPermissionsResult(granted: Map<String, Boolean>) {
        if (granted.values.all { it }) {
            _state.value = PermissionState.Granted
        }
    }

    fun refresh() {
        _state.value = computeState()
    }

    private fun computeState(): PermissionState {
        val ctx = getApplication<Application>()
        val audioGranted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val locationGranted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return if (audioGranted && locationGranted) PermissionState.Granted else PermissionState.Denied
    }
}
