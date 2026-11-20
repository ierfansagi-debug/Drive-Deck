package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.SpeedUnit
import com.example.model.TelemetryState
import com.example.ui.theme.LocalDriveDeckColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerWidgetCard(
    telemetry: TelemetryState,
    speedUnit: SpeedUnit,
    onToggleSpeedUnit: () -> Unit,
    onToggleDemoDrive: () -> Unit,
    onOpenCluster: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDriveDeckColors.current
    val currentSpeed = telemetry.formattedSpeed(speedUnit)
    val maxGaugeScale = if (speedUnit == SpeedUnit.MPH) 140f else 220f
    val targetFraction = (currentSpeed / maxGaugeScale).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "speed_arc_fraction"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = ext.amberAccent
    val trackColor = ext.bezelBorder
    val mutedColor = ext.mutedText

    InstrumentCard(
        modifier = modifier
            .clickable { onOpenCluster() }
            .testTag("speedometer_card"),
        contentPadding = 12.dp
    ) {
        // Header row: Speed label + Simulated indicator / Demo Drive toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "SPEED",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor
                )
                if (telemetry.isDemoDriveActive) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ext.simulatedBadge.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, ext.simulatedBadge)
                    ) {
                        Text(
                            text = stringResource(R.string.simulated_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.simulatedBadge,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Demo Drive toggle pill
            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleDemoDrive() }
                    .testTag("speedo_demo_toggle"),
                shape = RoundedCornerShape(8.dp),
                color = if (telemetry.isDemoDriveActive) {
                    ext.simulatedBadge.copy(alpha = 0.16f)
                } else {
                    ext.panelElevated
                },
                border = BorderStroke(
                    1.dp,
                    if (telemetry.isDemoDriveActive) ext.simulatedBadge else ext.bezelBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (telemetry.isDemoDriveActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.demo_drive),
                        tint = if (telemetry.isDemoDriveActive) ext.simulatedBadge else primaryColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "DEMO",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (telemetry.isDemoDriveActive) ext.simulatedBadge else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Radial Speed Gauge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = (size.minDimension * 0.085f).coerceAtLeast(8f)
                    val inset = strokeWidth / 2f + 4f
                    val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                    val topLeft = Offset(inset, inset)
                    val startAngle = 150f
                    val totalSweep = 240f

                    // Background track arc
                    drawArc(
                        color = trackColor.copy(alpha = 0.55f),
                        startAngle = startAngle,
                        sweepAngle = totalSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Tick marks
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerRadius = (size.minDimension / 2f) - inset - strokeWidth * 0.9f
                    val tickCount = 14
                    for (i in 0..tickCount) {
                        val frac = i / tickCount.toFloat()
                        val angleRad = Math.toRadians((startAngle + totalSweep * frac).toDouble())
                        val innerLen = if (i % 2 == 0) 10f else 6f
                        val p1 = Offset(
                            center.x + (outerRadius - innerLen) * cos(angleRad).toFloat(),
                            center.y + (outerRadius - innerLen) * sin(angleRad).toFloat()
                        )
                        val p2 = Offset(
                            center.x + outerRadius * cos(angleRad).toFloat(),
                            center.y + outerRadius * sin(angleRad).toFloat()
                        )
                        drawLine(
                            color = if (frac <= animatedFraction) primaryColor else mutedColor.copy(alpha = 0.45f),
                            start = p1,
                            end = p2,
                            strokeWidth = if (i % 2 == 0) 3f else 1.8f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Active Speed Sweep Arc
                    if (animatedFraction > 0.005f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to primaryColor,
                                0.7f to primaryColor,
                                1.0f to secondaryColor
                            ),
                            startAngle = startAngle,
                            sweepAngle = totalSweep * animatedFraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentSpeed.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleSpeedUnit() }
                            .testTag("speedometer_unit_pill"),
                        shape = RoundedCornerShape(8.dp),
                        color = ext.panelElevated,
                        border = BorderStroke(1.dp, ext.bezelBorder)
                    ) {
                        Text(
                            text = speedUnit.unitSuffix,
                            style = MaterialTheme.typography.labelSmall,
                            color = primaryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // Footer Trip & Max Stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ext.panelElevated)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TRIP",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor
                )
                Text(
                    text = telemetry.formattedTripDistance(speedUnit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "MAX",
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedColor
                )
                Text(
                    text = "${telemetry.formattedMaxSpeed(speedUnit)} ${speedUnit.unitSuffix}",
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryColor
                )
            }
        }
    }
}
