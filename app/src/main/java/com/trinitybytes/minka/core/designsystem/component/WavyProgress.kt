package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * Figma Wavy progress (`331:3040`) for multi-step flows: a primary wave up to [progress], then a
 * flat surfaceContainerHighest track. M3 1.4.0 has no LinearWavyProgressIndicator, so this draws
 * the Figma geometry: 12 dp tall, 4 dp round stroke, 12 dp wavelength, 8 dp gap before the track.
 */
@Composable
fun MinkaWavyProgress(progress: Float, modifier: Modifier = Modifier) {
    val waveColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val fraction = progress.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f) },
    ) {
        val stroke = 4.dp.toPx()
        val halfStroke = stroke / 2
        val centerY = size.height / 2
        val amplitude = 2.5.dp.toPx()
        val halfWave = 6.dp.toPx()
        val gap = 8.dp.toPx()
        val waveEnd = halfStroke + (size.width - stroke) * fraction
        if (fraction > 0f) {
            val path = Path().apply {
                moveTo(halfStroke, centerY)
                var x = halfStroke
                var up = true
                while (x < waveEnd) {
                    val next = minOf(x + halfWave, waveEnd)
                    val peak = if (up) centerY - amplitude * 2 else centerY + amplitude * 2
                    // A quadratic curve whose control point sits at twice the amplitude peaks at the amplitude.
                    quadraticTo((x + next) / 2, peak, next, centerY)
                    x = next
                    up = !up
                }
            }
            drawPath(path, waveColor, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        val trackStart = if (fraction > 0f) waveEnd + gap else halfStroke
        if (trackStart < size.width - halfStroke) {
            drawLine(
                color = trackColor,
                start = Offset(trackStart, centerY),
                end = Offset(size.width - halfStroke, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Preview
@Composable
private fun MinkaWavyProgressPreview() {
    MinkaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(MinkaSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                listOf(1f / 3, 2f / 3, 1f).forEach { MinkaWavyProgress(progress = it) }
            }
        }
    }
}
