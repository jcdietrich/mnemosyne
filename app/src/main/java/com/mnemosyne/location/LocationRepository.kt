package com.mnemosyne.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class Coordinates(val latitude: Double, val longitude: Double)

@Singleton
open class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Obtains the last known location or current fine location fix.
     */
    @SuppressLint("MissingPermission")
    open suspend fun getCurrentLocation(): Coordinates? = suspendCancellableCoroutine { continuation ->
        try {
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(Coordinates(location.latitude, location.longitude))
                    } else {
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { lastLoc ->
                                if (lastLoc != null) {
                                    continuation.resume(Coordinates(lastLoc.latitude, lastLoc.longitude))
                                } else {
                                    continuation.resume(null)
                                }
                            }
                            .addOnFailureListener {
                                continuation.resume(null)
                            }
                    }
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }

            continuation.invokeOnCancellation {
                cts.cancel()
            }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    /**
     * Reverse-geocodes GPS coordinates into a readable location string (e.g. "Seattle, WA").
     */
    open suspend fun getAddressDescription(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        if (latitude == 0.0 && longitude == 0.0) return@withContext ""
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val address = addresses?.firstOrNull() ?: return@withContext ""

            val locality = address.locality ?: address.subLocality ?: address.featureName ?: ""
            val adminArea = address.adminArea ?: address.countryName ?: ""

            listOf(locality, adminArea).filter { it.isNotBlank() }.joinToString(", ")
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Calculates distance between two GPS coordinates in kilometers using the Haversine formula.
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if ((lat1 == 0.0 && lon1 == 0.0) || (lat2 == 0.0 && lon2 == 0.0)) return Double.MAX_VALUE
        val r = 6371.0 // Radius of Earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
