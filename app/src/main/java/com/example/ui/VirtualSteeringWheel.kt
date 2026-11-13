package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SunsetGold

@Composable
fun VirtualSteeringWheel(
    size: Dp,
    onAnalogSteerChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val desc = stringResource(R.string.virtual_wheel_desc)
    var targetSteer by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedSteer by animateFloatAsState(
        targetValue = targetSteer,
        animationSpec = if (isDragging) spring(stiffness = 1800f) else spring(dampingRatio = 0.72f, stiffness = 420f),
        label = "wheel_steer"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .testTag("virtual_steering_wheel")
            .semantics { contentDescription = desc }
            .pointerInput(Unit) {
                var cumulativeDx = 0f
                val maxTravelPx = this.size.width * 0.65f
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        cumulativeDx = 0f
                    },
                    onDragEnd = {
                        isDragging = false
                        targetSteer = 0f
                        onAnalogSteerChanged(0f)
                    },
                    onDragCancel = {
                        isDragging = false
                        targetSteer = 0f
                        onAnalogSteerChanged(0f)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        cumulativeDx += dragAmount.x
                        // Dragging left (negative dx) => steerLeft (+1.0); dragging right (positive dx) => steerRight (-1.0)
                        val steer = (-cumulativeDx / maxTravelPx).coerceIn(-1f, 1f)
                        targetSteer = steer
                        onAnalogSteerChanged(steer)
                    }
                )
            }
    ) {
        val center = Offset(this.size.width * 0.5f, this.size.height * 0.5f)
        val radius = this.size.minDimension * 0.42f
        val rimStroke = this.size.minDimension * 0.105f

        // Subtle translucent disc backing
        drawCircle(
            color = ObsidianCard,
            radius = radius + rimStroke * 0.48f,
            center = center
        )

        // Rotate wheel clockwise for right turn (-steer), counter-clockwise for left turn (+steer)
        rotate(degrees = -animatedSteer * 115f, pivot = center) {
            // Carbon rim
            drawCircle(
                color = Color(0xFF1A243B),
                radius = radius,
                center = center,
                style = Stroke(width = rimStroke)
            )
            // Outer rim highlight
            drawCircle(
                color = if (isDragging) ElectricCyan.copy(alpha = 0.85f) else SunsetGold.copy(alpha = 0.65f),
                radius = radius + rimStroke * 0.35f,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // 3 sport spokes
            val spokeCol = Color(0xFF283552)
            val spokeWidth = rimStroke * 0.65f
            drawLine(
                color = spokeCol,
                start = Offset(center.x - radius * 0.90f, center.y),
                end = Offset(center.x + radius * 0.90f, center.y),
                strokeWidth = spokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = spokeCol,
                start = center,
                end = Offset(center.x, center.y + radius * 0.90f),
                strokeWidth = spokeWidth,
                cap = StrokeCap.Round
            )

            // Top center gold racing stripe marker
            drawLine(
                color = SunsetGold,
                start = Offset(center.x, center.y - radius - rimStroke * 0.42f),
                end = Offset(center.x, center.y - radius + rimStroke * 0.42f),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center hub badge
            drawCircle(
                color = Color(0xFF0E1628),
                radius = radius * 0.28f,
                center = center
            )
            drawCircle(
                color = SunsetGold,
                radius = radius * 0.28f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}
