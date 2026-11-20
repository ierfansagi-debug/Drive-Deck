package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalDriveDeckColors

@Composable
fun InstrumentCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    contentPadding: Dp = 14.dp,
    tonalElevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalDriveDeckColors.current
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = tonalElevation,
        border = BorderStroke(1.dp, ext.bezelBorder)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}
