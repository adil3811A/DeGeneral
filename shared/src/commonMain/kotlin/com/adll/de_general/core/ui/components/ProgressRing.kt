package com.adll.de_general.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The circular indicator both screens use — a compatibility verdict on one, download progress on
 * the other.
 *
 * The number in the middle is always something measured. On the welcome screen it is how many
 * checks passed out of how many ran; on the install screen it is the fraction of bytes on disk.
 * Neither is a score the app made up.
 */
@Composable
fun ProgressRing(
    fraction: Float,
    primaryLabel: String,
    secondaryLabel: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 148.dp,
    thickness: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        label = "ring",
    )

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = Stroke(width = thickness.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            val inset = thickness.toPx() / 2
            val arcSize = androidx.compose.ui.geometry.Size(
                size.width - thickness.toPx(),
                size.height - thickness.toPx(),
            )
            val offset = androidx.compose.ui.geometry.Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = offset,
                size = arcSize,
                style = stroke,
            )
            if (animated > 0f) {
                drawArc(
                    color = color,
                    // Start at twelve o'clock; a ring that starts at three reads as a pie chart.
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = offset,
                    size = arcSize,
                    style = stroke,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                primaryLabel,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                secondaryLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
