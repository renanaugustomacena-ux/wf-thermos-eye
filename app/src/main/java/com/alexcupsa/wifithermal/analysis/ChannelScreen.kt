package com.alexcupsa.wifithermal.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alexcupsa.wifithermal.core.model.ChannelAnalysisResult
import com.alexcupsa.wifithermal.core.model.ChannelInfo
import com.alexcupsa.wifithermal.core.ui.theme.SignalExcellent
import com.alexcupsa.wifithermal.core.ui.theme.SignalFair
import com.alexcupsa.wifithermal.core.ui.theme.SignalGood
import com.alexcupsa.wifithermal.core.ui.theme.SignalUnusable
import com.alexcupsa.wifithermal.core.ui.theme.SignalWeak
import com.alexcupsa.wifithermal.scan.ScanViewModel

@Composable
fun ChannelScreen(
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val analysis = state.channelAnalysis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Channel Analysis",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${state.results.size} access points analyzed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (analysis == null) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Start a scan to see channel analysis",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        Spacer(Modifier.height(16.dp))

        RecommendationCard(analysis)

        Spacer(Modifier.height(16.dp))
        Text("2.4 GHz Channels", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ChannelBarChart(analysis.channels2g, analysis.recommended2g)

        Spacer(Modifier.height(24.dp))
        Text("5 GHz Channels", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ChannelBarChart(analysis.channels5g, analysis.recommended5g)
    }
}

@Composable
private fun RecommendationCard(analysis: ChannelAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recommended Channels", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${analysis.recommended2g}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.Monospace,
                        color = SignalExcellent,
                    )
                    Text("2.4 GHz", style = MaterialTheme.typography.labelMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${analysis.recommended5g}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.Monospace,
                        color = SignalExcellent,
                    )
                    Text("5 GHz", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ChannelBarChart(channels: List<ChannelInfo>, recommended: Int) {
    if (channels.isEmpty()) return

    val maxAps = channels.maxOf { it.apCount }.coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            ) {
                val barWidth = size.width / channels.size
                val maxBarHeight = size.height - 20f

                channels.forEachIndexed { i, ch ->
                    val barHeight = if (ch.apCount > 0) {
                        (ch.apCount.toFloat() / maxAps) * maxBarHeight
                    } else {
                        2f
                    }

                    val color = when {
                        ch.channel == recommended -> SignalExcellent
                        ch.congestionScore < 0.2 -> SignalGood
                        ch.congestionScore < 0.5 -> SignalFair
                        ch.congestionScore < 0.8 -> SignalWeak
                        else -> SignalUnusable
                    }

                    drawRect(
                        color = color.copy(alpha = 0.8f),
                        topLeft = Offset(i * barWidth + 2f, size.height - barHeight),
                        size = Size(barWidth - 4f, barHeight),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                channels.forEach { ch ->
                    Text(
                        text = "${ch.channel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (ch.channel == recommended) FontWeight.Bold else FontWeight.Normal,
                        color = if (ch.channel == recommended) SignalExcellent else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val withAps = channels.filter { it.apCount > 0 }
                if (withAps.isNotEmpty()) {
                    val busiest = withAps.maxBy { it.congestionScore }
                    Text(
                        text = "Busiest: Ch ${busiest.channel} (${busiest.apCount} APs, ${"%.0f".format(busiest.congestionScore * 100)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
