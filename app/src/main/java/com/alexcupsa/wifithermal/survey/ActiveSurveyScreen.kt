package com.alexcupsa.wifithermal.survey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alexcupsa.wifithermal.core.model.MeasurementPoint
import com.alexcupsa.wifithermal.core.model.Position
import com.alexcupsa.wifithermal.core.ui.theme.SignalExcellent
import com.alexcupsa.wifithermal.core.ui.theme.SignalFair
import com.alexcupsa.wifithermal.core.ui.theme.SignalGood

@Composable
fun ActiveSurveyScreen(
    onComplete: () -> Unit,
    viewModel: ActiveSurveyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SurveyHeader(state)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                FloorPlanView(
                    measurements = state.measurements,
                    tapPosition = state.tapPosition,
                    onTap = { x, y -> viewModel.onFloorPlanTap(x, y) },
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.tapPosition != null && state.currentScanResults.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = { viewModel.recordMeasurement() },
                            containerColor = SignalExcellent,
                        ) {
                            Icon(Icons.Default.Add, "Record measurement")
                        }
                    }
                }
            }

            BottomControls(
                state = state,
                onStartRecording = viewModel::startRecording,
                onStopRecording = viewModel::stopRecording,
                onComplete = {
                    viewModel.completeSurvey()
                    onComplete()
                },
            )
        }
    }
}

@Composable
private fun SurveyHeader(state: ActiveSurveyUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = state.survey?.name ?: "Survey",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${state.measurements.size} measurement points",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatChip("APs", "${state.currentScanResults.size}")
                if (state.isRecording) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("REC", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FloorPlanView(
    measurements: List<MeasurementPoint>,
    tapPosition: Position?,
    onTap: (Double, Double) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY,
            )
            .transformable(transformState)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val normX = (offset.x - offsetX) / (size.width * scale)
                    val normY = (offset.y - offsetY) / (size.height * scale)
                    onTap(normX.toDouble().coerceIn(0.0, 1.0), normY.toDouble().coerceIn(0.0, 1.0))
                }
            },
    ) {
        GridOverlay()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            for (point in measurements) {
                val px = (point.position.x * w).toFloat()
                val py = (point.position.y * h).toFloat()
                drawCircle(
                    color = SignalGood,
                    radius = 8f,
                    center = Offset(px, py),
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(px, py),
                )
            }

            tapPosition?.let { pos ->
                val px = (pos.x * w).toFloat()
                val py = (pos.y * h).toFloat()
                drawCircle(
                    color = SignalFair,
                    radius = 14f,
                    center = Offset(px, py),
                    alpha = 0.4f,
                )
                drawCircle(
                    color = SignalFair,
                    radius = 6f,
                    center = Offset(px, py),
                )
            }
        }
    }
}

@Composable
private fun GridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 40f
        val lineColor = Color.White.copy(alpha = 0.05f)

        var x = 0f
        while (x < size.width) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += gridSize
        }
        var y = 0f
        while (y < size.height) {
            drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += gridSize
        }
    }
}

@Composable
private fun BottomControls(
    state: ActiveSurveyUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onComplete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!state.isRecording) {
                Button(
                    onClick = onStartRecording,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.RadioButtonChecked, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Scanning")
                }
            } else {
                Button(
                    onClick = onStopRecording,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Stop")
                }
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SignalGood,
                ),
                enabled = state.measurements.isNotEmpty(),
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Finish")
            }
        }
    }
}
