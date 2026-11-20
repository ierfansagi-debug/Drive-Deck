package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class DriveDeckExtendedColors(
    val bezelBorder: Color,
    val panelElevated: Color,
    val mutedText: Color,
    val telemetryAccent: Color,
    val amberAccent: Color,
    val gpsActive: Color,
    val simulatedBadge: Color,
    val alertRed: Color,
    val isDark: Boolean
)

val LocalDriveDeckColors = staticCompositionLocalOf {
    DriveDeckExtendedColors(
        bezelBorder = NightBorder,
        panelElevated = NightSurfaceElevated,
        mutedText = NightOnSurfaceMuted,
        telemetryAccent = NightPrimary,
        amberAccent = NightSecondary,
        gpsActive = GpsLockedGreen,
        simulatedBadge = SimulatedWarningOrange,
        alertRed = AlertRed,
        isDark = true
    )
}

private val NightColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    secondary = NightSecondary,
    onSecondary = NightOnSecondary,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceElevated,
    onSurfaceVariant = NightOnSurfaceMuted,
    outline = NightBorder,
    error = AlertRed
)

private val DayColorScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = DayOnPrimary,
    secondary = DaySecondary,
    onSecondary = DayOnSecondary,
    background = DayBackground,
    onBackground = DayOnSurface,
    surface = DaySurface,
    onSurface = DayOnSurface,
    surfaceVariant = DaySurfaceElevated,
    onSurfaceVariant = DayOnSurfaceMuted,
    outline = DayBorder,
    error = AlertRed
)

val DriveDeckShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun DriveDeckTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NightColorScheme else DayColorScheme
    val extendedColors = if (darkTheme) {
        DriveDeckExtendedColors(
            bezelBorder = NightBorder,
            panelElevated = NightSurfaceElevated,
            mutedText = NightOnSurfaceMuted,
            telemetryAccent = NightPrimary,
            amberAccent = NightSecondary,
            gpsActive = GpsLockedGreen,
            simulatedBadge = SimulatedWarningOrange,
            alertRed = AlertRed,
            isDark = true
        )
    } else {
        DriveDeckExtendedColors(
            bezelBorder = DayBorder,
            panelElevated = DaySurfaceElevated,
            mutedText = DayOnSurfaceMuted,
            telemetryAccent = DayPrimary,
            amberAccent = DaySecondary,
            gpsActive = GpsLockedGreen,
            simulatedBadge = SimulatedWarningOrange,
            alertRed = AlertRed,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalDriveDeckColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DriveDeckTypography,
            shapes = DriveDeckShapes,
            content = content
        )
    }
}
