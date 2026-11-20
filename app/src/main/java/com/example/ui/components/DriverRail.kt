package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.HeadunitUiState
import com.example.model.HeadunitView
import com.example.model.ThemeMode
import com.example.ui.theme.LocalDriveDeckColors

private data class RailDestination(
    val view: HeadunitView,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val RailDestinations = listOf(
    RailDestination(HeadunitView.DASHBOARD_SPLIT, R.string.nav_cockpit, Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_cockpit"),
    RailDestination(HeadunitView.MAP_FULL, R.string.nav_map, Icons.Filled.Map, Icons.Outlined.Map, "nav_map"),
    RailDestination(HeadunitView.AUDIO_FOCUS, R.string.nav_audio, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_audio"),
    RailDestination(HeadunitView.CLUSTER_FOCUS, R.string.nav_cluster, Icons.Filled.Speed, Icons.Outlined.Speed, "nav_cluster"),
    RailDestination(HeadunitView.SETTINGS, R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun DriverSideRail(
    state: HeadunitUiState,
    onNavigate: (HeadunitView) -> Unit,
    onCycleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDriveDeckColors.current

    Surface(
        modifier = modifier
            .width(84.dp)
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, ext.bezelBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top mini telemetry status (Time + GPS status)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = state.clock.timeFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                val gpsColor = when {
                    state.telemetry.isDemoDriveActive -> ext.simulatedBadge
                    state.telemetry.hasGpsFix -> ext.gpsActive
                    else -> ext.mutedText
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(gpsColor.copy(alpha = 0.14f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = if (state.telemetry.hasGpsFix || state.telemetry.isDemoDriveActive) {
                            Icons.Filled.GpsFixed
                        } else {
                            Icons.Filled.GpsNotFixed
                        },
                        contentDescription = "GPS Signal Status",
                        tint = gpsColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (state.telemetry.isDemoDriveActive) "SIM" else if (state.telemetry.hasGpsFix) "GPS" else "NO GPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = gpsColor
                    )
                }
            }

            // Center navigation icons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RailDestinations.forEach { dest ->
                    val selected = state.activeView == dest.view
                    val label = stringResource(dest.labelRes)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (selected) 1.dp else 0.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onNavigate(dest.view) }
                            .testTag(dest.testTag)
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = label,
                                tint = if (selected) MaterialTheme.colorScheme.primary else ext.mutedText,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.primary else ext.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Bottom Day/Night Auto quick cycle button
            val themeIcon = when (state.themeMode) {
                ThemeMode.AUTO -> Icons.Filled.BrightnessAuto
                ThemeMode.DAY -> Icons.Filled.WbSunny
                ThemeMode.NIGHT -> Icons.Filled.DarkMode
            }
            val themeShort = when (state.themeMode) {
                ThemeMode.AUTO -> "AUTO"
                ThemeMode.DAY -> "DAY"
                ThemeMode.NIGHT -> "NIGHT"
            }
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(ext.panelElevated)
                    .border(1.dp, ext.bezelBorder, CircleShape)
                    .clickable { onCycleTheme() }
                    .testTag("rail_theme_cycle_button")
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = themeIcon,
                        contentDescription = "Cycle Theme Mode: $themeShort",
                        tint = ext.amberAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = themeShort,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun DriverBottomBar(
    state: HeadunitUiState,
    onNavigate: (HeadunitView) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDriveDeckColors.current
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        RailDestinations.forEach { dest ->
            val selected = state.activeView == dest.view
            val label = stringResource(dest.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(dest.view) },
                modifier = Modifier.testTag("${dest.testTag}_bottom"),
                icon = {
                    Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    unselectedIconColor = ext.mutedText,
                    unselectedTextColor = ext.mutedText
                )
            )
        }
    }
}
