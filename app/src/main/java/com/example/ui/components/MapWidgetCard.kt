package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.model.TelemetryState
import com.example.ui.theme.LocalDriveDeckColors
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Point
import java.util.Locale

private const val STYLE_OPENFREEMAP_DARK = "https://tiles.openfreemap.org/styles/dark"
private const val STYLE_OPENFREEMAP_LIGHT = "https://tiles.openfreemap.org/styles/liberty"
private const val STYLE_MAPLIBRE_FALLBACK = "https://demotiles.maplibre.org/style.json"

private const val VEHICLE_SOURCE_ID = "drivedeck-vehicle-source"
private const val VEHICLE_HALO_LAYER_ID = "drivedeck-vehicle-halo"
private const val VEHICLE_CORE_LAYER_ID = "drivedeck-vehicle-core"

@Composable
fun MapWidgetCard(
    telemetry: TelemetryState,
    isDarkTheme: Boolean,
    followVehicle: Boolean,
    showExpandButton: Boolean,
    onToggleFollow: (Boolean) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onExpandMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDriveDeckColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val primaryArgb = MaterialTheme.colorScheme.primary.toArgb()

    val currentTelemetry by rememberUpdatedState(telemetry)
    val currentOnToggleFollow by rememberUpdatedState(onToggleFollow)

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply {
            onCreate(null)
        }
    }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var activeStyleUri by remember { mutableStateOf<String?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var preferredZoom by remember { mutableStateOf(15.0) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val failListener = MapView.OnDidFailLoadingMapListener {
            val map = mapLibreMap ?: return@OnDidFailLoadingMapListener
            if (activeStyleUri != STYLE_MAPLIBRE_FALLBACK) {
                activeStyleUri = STYLE_MAPLIBRE_FALLBACK
                styleReady = false
                map.setStyle(Style.Builder().fromUri(STYLE_MAPLIBRE_FALLBACK)) { style ->
                    applyVehicleMarkerLayers(style, currentTelemetry, primaryArgb)
                    styleReady = true
                }
            }
        }
        mapView.addOnDidFailLoadingMapListener(failListener)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.removeOnDidFailLoadingMapListener(failListener)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    val targetStyleUri = if (isDarkTheme) STYLE_OPENFREEMAP_DARK else STYLE_OPENFREEMAP_LIGHT

    LaunchedEffect(mapLibreMap, targetStyleUri) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (activeStyleUri != targetStyleUri) {
            activeStyleUri = targetStyleUri
            styleReady = false
            map.setStyle(Style.Builder().fromUri(targetStyleUri)) { style ->
                applyVehicleMarkerLayers(style, currentTelemetry, primaryArgb)
                styleReady = true
            }
        }
    }

    LaunchedEffect(mapLibreMap, styleReady, telemetry.latitude, telemetry.longitude, telemetry.bearing, followVehicle) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (styleReady) {
            map.style?.getSourceAs<GeoJsonSource>(VEHICLE_SOURCE_ID)?.setGeoJson(
                Point.fromLngLat(telemetry.longitude, telemetry.latitude)
            )
        }
        if (followVehicle) {
            val targetBearing = if (telemetry.speedMps > 2f) {
                telemetry.bearing.toDouble()
            } else {
                map.cameraPosition.bearing
            }
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(LatLng(telemetry.latitude, telemetry.longitude))
                        .zoom(preferredZoom)
                        .bearing(targetBearing)
                        .tilt(35.0)
                        .build()
                ),
                650
            )
        }
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.bezelBorder),
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("maplibre_map_view"),
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            mapLibreMap = map
                            map.uiSettings.apply {
                                isCompassEnabled = true
                                isRotateGesturesEnabled = true
                                isTiltGesturesEnabled = true
                                isZoomGesturesEnabled = true
                                isScrollGesturesEnabled = true
                                isAttributionEnabled = true
                                isLogoEnabled = false
                            }
                            map.moveCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(LatLng(currentTelemetry.latitude, currentTelemetry.longitude))
                                        .zoom(15.0)
                                        .tilt(35.0)
                                        .build()
                                )
                            )
                            map.addOnCameraMoveStartedListener { reason ->
                                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                                    currentOnToggleFollow(false)
                                }
                            }
                            map.addOnCameraIdleListener {
                                val camZoom = map.cameraPosition.zoom
                                if (camZoom in 1.0..20.0) {
                                    preferredZoom = camZoom
                                }
                            }
                        }
                    }
                }
            )

            // Top-left Coordinates Pill + GPS Enable button
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, ext.bezelBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Navigation,
                            contentDescription = "Navigation Coordinates",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.4f, %.4f",
                                telemetry.latitude,
                                telemetry.longitude
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!telemetry.hasLocationPermission) {
                    Button(
                        onClick = onRequestLocationPermission,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("map_enable_gps_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.grant_location),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Right-side MapLibre driving control cluster (large >= 56dp touch targets)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (showExpandButton) {
                    Surface(
                        onClick = onExpandMap,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("map_expand_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, ext.bezelBorder),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Expand Map View",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Large Zoom In & Zoom Out driving buttons
                Surface(
                    onClick = {
                        val map = mapLibreMap ?: return@Surface
                        val nextZoom = (map.cameraPosition.zoom + 1.0).coerceAtMost(20.0)
                        preferredZoom = nextZoom
                        map.animateCamera(CameraUpdateFactory.zoomTo(nextZoom), 250)
                    },
                    modifier = Modifier
                        .size(58.dp)
                        .testTag("map_zoom_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, ext.bezelBorder),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Zoom In Map",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        val map = mapLibreMap ?: return@Surface
                        val nextZoom = (map.cameraPosition.zoom - 1.0).coerceAtLeast(1.0)
                        preferredZoom = nextZoom
                        map.animateCamera(CameraUpdateFactory.zoomTo(nextZoom), 250)
                    },
                    modifier = Modifier
                        .size(58.dp)
                        .testTag("map_zoom_out_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, ext.bezelBorder),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Zoom Out Map",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Surface(
                    onClick = {
                        preferredZoom = 16.0
                        onToggleFollow(true)
                        mapLibreMap?.animateCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.Builder()
                                    .target(LatLng(telemetry.latitude, telemetry.longitude))
                                    .zoom(16.0)
                                    .tilt(35.0)
                                    .build()
                            ),
                            500
                        )
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("map_recenter_button"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (followVehicle) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (followVehicle) MaterialTheme.colorScheme.primary else ext.bezelBorder
                    ),
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (followVehicle) Icons.Filled.GpsFixed else Icons.Filled.MyLocation,
                            contentDescription = "Recenter Vehicle on Map",
                            tint = if (followVehicle) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Bottom-left Follow Mode Badge (padded above MapLibre attribution)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 34.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, ext.bezelBorder)
            ) {
                Text(
                    text = if (followVehicle) "AUTO-FOLLOW" else "FREE PAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (followVehicle) MaterialTheme.colorScheme.primary else ext.mutedText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

private fun applyVehicleMarkerLayers(
    style: Style,
    telemetry: TelemetryState,
    accentArgb: Int
) {
    val point = Point.fromLngLat(telemetry.longitude, telemetry.latitude)
    if (style.getSource(VEHICLE_SOURCE_ID) == null) {
        style.addSource(GeoJsonSource(VEHICLE_SOURCE_ID, point))
    }
    if (style.getLayer(VEHICLE_HALO_LAYER_ID) == null) {
        val haloLayer = CircleLayer(VEHICLE_HALO_LAYER_ID, VEHICLE_SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(18f),
            PropertyFactory.circleColor(accentArgb),
            PropertyFactory.circleOpacity(0.28f)
        )
        style.addLayer(haloLayer)
    }
    if (style.getLayer(VEHICLE_CORE_LAYER_ID) == null) {
        val coreLayer = CircleLayer(VEHICLE_CORE_LAYER_ID, VEHICLE_SOURCE_ID).withProperties(
            PropertyFactory.circleRadius(8.5f),
            PropertyFactory.circleColor(accentArgb),
            PropertyFactory.circleStrokeWidth(3f),
            PropertyFactory.circleStrokeColor(AndroidColor.WHITE)
        )
        style.addLayer(coreLayer)
    }
}
