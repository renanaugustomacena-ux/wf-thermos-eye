package com.alexcupsa.wifithermal.heatmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alexcupsa.wifithermal.core.engine.heatmap.HeatmapColors
import com.alexcupsa.wifithermal.core.model.HeatmapGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeatmapScreen(
    onBack: () -> Unit,
    viewModel: HeatmapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.survey?.name ?: "Heatmap") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ModeSelector(state, viewModel)

            if (state.mode == HeatmapMode.BSSID) {
                BssidSelector(state, viewModel)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.generating -> CircularProgressIndicator()
                    state.errorMessage != null -> Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                    state.grid != null -> HeatmapCanvas(state.grid!!, state.config.colorScheme)
                    else -> Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            state.grid?.let { grid ->
                HeatmapLegend(grid, state.config.colorScheme)
            }
        }
    }
}

@Composable
private fun ModeSelector(state: HeatmapUiState, viewModel: HeatmapViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = state.mode == HeatmapMode.BAND_2_4,
            onClick = { viewModel.setMode(HeatmapMode.BAND_2_4) },
            label = { Text("2.4 GHz") },
        )
        FilterChip(
            selected = state.mode == HeatmapMode.BAND_5,
            onClick = { viewModel.setMode(HeatmapMode.BAND_5) },
            label = { Text("5 GHz") },
        )
        FilterChip(
            selected = state.mode == HeatmapMode.BSSID,
            onClick = { viewModel.setMode(HeatmapMode.BSSID) },
            label = { Text("Per AP") },
        )
    }
}

@Composable
private fun BssidSelector(state: HeatmapUiState, viewModel: HeatmapViewModel) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(state.apSummaries) { ap ->
            FilterChip(
                selected = state.selectedBssid == ap.bssid,
                onClick = { viewModel.selectBssid(ap.bssid) },
                label = {
                    Text(
                        text = ap.ssid.ifEmpty { ap.bssid.takeLast(8) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun HeatmapCanvas(
    grid: HeatmapGrid,
    colorScheme: com.alexcupsa.wifithermal.core.model.ColorScheme,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY,
            )
            .transformable(transformState),
    ) {
        val cellW = size.width / grid.width
        val cellH = size.height / grid.height

        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                val value = grid.data[y * grid.width + x]
                if (!value.isNaN()) {
                    val argb = HeatmapColors.rssiToColor(
                        rssi = value,
                        scheme = colorScheme,
                        minRssi = grid.minValue,
                        maxRssi = grid.maxValue,
                        alpha = 200,
                    )
                    drawRect(
                        color = Color(argb),
                        topLeft = Offset(x * cellW, y * cellH),
                        size = Size(cellW + 1f, cellH + 1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend(grid: HeatmapGrid, colorScheme: com.alexcupsa.wifithermal.core.model.ColorScheme) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(20.dp),
        ) {
            val steps = 50
            val stepWidth = size.width / steps
            for (i in 0 until steps) {
                val t = i.toDouble() / (steps - 1)
                val rssi = grid.maxValue - t * (grid.maxValue - grid.minValue)
                val argb = HeatmapColors.rssiToColor(rssi, colorScheme, grid.minValue, grid.maxValue)
                drawRect(
                    color = Color(argb),
                    topLeft = Offset(i * stepWidth, 0f),
                    size = Size(stepWidth + 1f, size.height),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = "${"%.0f".format(grid.maxValue)} dBm",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "${"%.0f".format(grid.minValue)} dBm",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${grid.generationTimeMs}ms",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
