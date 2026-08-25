package com.mnemosyne.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mnemosyne.feed.HomeScreen
import com.mnemosyne.models.ModelDownloadScreen
import com.mnemosyne.models.ModelDownloadViewModel
import com.mnemosyne.permissions.PermissionScreen
import com.mnemosyne.permissions.PermissionState
import com.mnemosyne.permissions.PermissionViewModel
import com.mnemosyne.settings.SettingsScreen

const val ROUTE_PERMISSIONS = "permissions"
const val ROUTE_MODEL_DOWNLOAD = "model_download"
const val ROUTE_HOME = "home"
const val ROUTE_SETTINGS = "settings"

@Composable
fun MnemosyneNavHost() {
    val navController = rememberNavController()
    val permissionVm: PermissionViewModel = hiltViewModel()
    val permissionState by permissionVm.state.collectAsState()

    val startDestination = if (permissionState == PermissionState.Granted) {
        ROUTE_MODEL_DOWNLOAD
    } else {
        ROUTE_PERMISSIONS
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_PERMISSIONS) {
            PermissionScreen(
                onPermissionsGranted = {
                    navController.navigate(ROUTE_MODEL_DOWNLOAD) {
                        popUpTo(ROUTE_PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_MODEL_DOWNLOAD) {
            val modelVm: ModelDownloadViewModel = hiltViewModel()
            ModelDownloadScreen(
                viewModel = modelVm,
                onModelsReady = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(ROUTE_MODEL_DOWNLOAD) { inclusive = true }
                    }
                }
            )
        }
        composable(ROUTE_HOME) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(ROUTE_SETTINGS)
                }
            )
        }
        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
