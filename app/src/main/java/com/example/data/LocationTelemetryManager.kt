package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.model.TelemetryState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class LocationTelemetryManager(
    context: Context,
    private val scope: CoroutineScope
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private var lastLocation: Location? = null
    private var isListeningGps = false
    private var demoJob: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            if (_telemetry.value.isDemoDriveActive) return

            val previous = lastLocation
            val deltaMeters = if (previous != null) {
                val dist = previous.distanceTo(location).toDouble()
                if (dist in 0.5..500.0) dist else 0.0
            } else {
                0.0
            }
            lastLocation = location

            val rawSpeedMps = if (location.hasSpeed()) location.speed else 0f
            val cleanSpeedMps = if (rawSpeedMps < 0.4f) 0f else rawSpeedMps

            _telemetry.update { current ->
                current.copy(
                    speedMps = cleanSpeedMps,
                    maxSpeedMps = maxOf(current.maxSpeedMps, cleanSpeedMps),
                    tripDistanceMeters = current.tripDistanceMeters + deltaMeters,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    bearing = if (location.hasBearing()) location.bearing else current.bearing,
                    hasGpsFix = true
                )
            }
        }
    }

    fun onPermissionStatusChanged(granted: Boolean) {
        _telemetry.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            startGpsUpdates()
        } else {
            stopGpsUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    fun startGpsUpdates() {
        if (isListeningGps || !_telemetry.value.hasLocationPermission) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()

        try {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null && !_telemetry.value.isDemoDriveActive) {
                    lastLocation = location
                    _telemetry.update {
                        it.copy(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            bearing = if (location.hasBearing()) location.bearing else it.bearing,
                            hasGpsFix = true
                        )
                    }
                }
            }
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            isListeningGps = true
        } catch (_: SecurityException) {
            _telemetry.update { it.copy(hasLocationPermission = false) }
        }
    }

    fun stopGpsUpdates() {
        if (!isListeningGps) return
        fusedClient.removeLocationUpdates(locationCallback)
        isListeningGps = false
    }

    fun toggleDemoDrive() {
        if (_telemetry.value.isDemoDriveActive) {
            stopDemoDrive()
        } else {
            startDemoDrive()
        }
    }

    private fun startDemoDrive() {
        demoJob?.cancel()
        _telemetry.update { it.copy(isDemoDriveActive = true) }
        demoJob = scope.launch {
            var tick = 0
            var simLat = _telemetry.value.latitude
            var simLng = _telemetry.value.longitude
            while (isActive) {
                // Smooth highway/arterial speed curve between 18 mph and 68 mph
                val speedMps = (16.5f + 12.0f * sin(tick * 0.08) + 2.5f * cos(tick * 0.21)).toFloat()
                    .coerceAtLeast(0f)
                val bearing = ((tick * 1.4f) % 360f)
                val stepMeters = speedMps * 0.5
                val rad = Math.toRadians(bearing.toDouble())
                simLat += (stepMeters * cos(rad)) / 111320.0
                simLng += (stepMeters * sin(rad)) / (111320.0 * abs(cos(Math.toRadians(simLat))).coerceAtLeast(0.2))

                _telemetry.update { current ->
                    current.copy(
                        speedMps = speedMps,
                        maxSpeedMps = maxOf(current.maxSpeedMps, speedMps),
                        tripDistanceMeters = current.tripDistanceMeters + stepMeters,
                        latitude = simLat,
                        longitude = simLng,
                        bearing = bearing
                    )
                }
                tick++
                delay(500L)
            }
        }
    }

    private fun stopDemoDrive() {
        demoJob?.cancel()
        demoJob = null
        _telemetry.update {
            it.copy(
                isDemoDriveActive = false,
                speedMps = 0f
            )
        }
    }

    fun resetTripStats() {
        _telemetry.update {
            it.copy(maxSpeedMps = 0f, tripDistanceMeters = 0.0)
        }
    }

    fun release() {
        stopDemoDrive()
        stopGpsUpdates()
    }
}
