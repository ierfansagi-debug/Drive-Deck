package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.WeatherRepository
import com.example.model.ClockState
import com.example.model.SpeedUnit
import com.example.model.TempUnit
import com.example.model.WeatherState
import com.example.ui.theme.LocalDriveDeckColors
import kotlin.math.roundToInt

@Composable
fun ClockWeatherWidgetCard(
    clock: ClockState,
    weather: WeatherState,
    tempUnit: TempUnit,
    speedUnit: SpeedUnit,
    onRefreshWeather: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDriveDeckColors.current

    InstrumentCard(
        modifier = modifier.testTag("clock_weather_card"),
        contentPadding = 12.dp
    ) {
        // Digital Clock Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = clock.timeFormatted,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ":${clock.secondsFormatted}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (clock.amPm.isNotEmpty()) {
                        Text(
                            text = clock.amPm,
                            style = MaterialTheme.typography.labelMedium,
                            color = ext.mutedText,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }
                Text(
                    text = clock.dateFormatted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.mutedText
                )
            }

            IconButton(
                onClick = onRefreshWeather,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("weather_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh Weather",
                    tint = ext.mutedText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = ext.bezelBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Open-Meteo Weather Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            when (weather) {
                is WeatherState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Syncing weather...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.mutedText
                        )
                    }
                }

                is WeatherState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Weather Unavailable",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = weather.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.mutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                is WeatherState.Ready -> {
                    val tempValue = when (tempUnit) {
                        TempUnit.FAHRENHEIT -> (weather.tempCelsius * 9.0 / 5.0 + 32.0).roundToInt()
                        TempUnit.CELSIUS -> weather.tempCelsius.roundToInt()
                    }
                    val windText = when (speedUnit) {
                        SpeedUnit.MPH -> "${(weather.windSpeedKmh * 0.621371).roundToInt()} mph"
                        SpeedUnit.KMH -> "${weather.windSpeedKmh.roundToInt()} km/h"
                    }
                    val icon = wmoWeatherIcon(weather.weatherCode, weather.isDay)
                    val iconTint = if (weather.weatherCode in listOf(0, 1) && weather.isDay) {
                        ext.amberAccent
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = WeatherRepository.describeWmoCode(weather.weatherCode),
                                    tint = iconTint,
                                    modifier = Modifier.size(34.dp)
                                )
                                Column {
                                    Text(
                                        text = "$tempValue${tempUnit.symbol}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = WeatherRepository.describeWmoCode(weather.weatherCode),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ext.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ext.panelElevated)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = weather.cityName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Air,
                                    contentDescription = "Wind Speed",
                                    tint = ext.mutedText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = windText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.mutedText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun wmoWeatherIcon(code: Int, isDay: Boolean): ImageVector {
    return when (code) {
        0, 1 -> if (isDay) Icons.Filled.WbSunny else Icons.Filled.NightsStay
        2, 3, 45, 48 -> Icons.Filled.Cloud
        in 51..67, in 80..82 -> Icons.Filled.WaterDrop
        in 71..77, 85, 86 -> Icons.Filled.AcUnit
        in 95..99 -> Icons.Filled.Thunderstorm
        else -> Icons.Filled.Cloud
    }
}
