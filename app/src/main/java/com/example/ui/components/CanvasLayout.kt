package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.database.CardEntity
import com.example.data.database.ConnectionEntity
import com.example.ui.BrainViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CanvasBackground() {
    // Elegant dotted grid matching modern boards (Miro/Heptabase)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val dotRadius = 1.5f
        val dotSpacing = 40f
        val color = Color(0x22FFFFFF) // Faint dot grid in dark theme

        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(color, radius = dotRadius, center = Offset(x, y))
                y += dotSpacing
            }
            x += dotSpacing
        }
    }
}

@Composable
fun ConnectionsCanvas(
    cards: List<CardEntity>,
    connections: List<ConnectionEntity>,
    onConnectionTap: (Long) -> Unit
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        connections.forEach { conn ->
            val sourceCard = cards.find { it.id == conn.sourceCardId }
            val targetCard = cards.find { it.id == conn.targetCardId }

            if (sourceCard != null && targetCard != null) {
                // Find card centers
                val start = Offset(
                    sourceCard.x + sourceCard.width / 2,
                    sourceCard.y + sourceCard.height / 2
                )
                val end = Offset(
                    targetCard.x + targetCard.width / 2,
                    targetCard.y + targetCard.height / 2
                )

                val lineColor = Color(android.graphics.Color.parseColor(conn.color))
                val pathStyle = when (conn.lineStyle) {
                    "DOTTED" -> PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    else -> null
                }

                if (conn.lineStyle == "STRAIGHT" || conn.lineStyle == "DOTTED" || conn.lineStyle == "ARROW") {
                    // Straight connector
                    drawLine(
                        color = lineColor,
                        start = start,
                        end = end,
                        strokeWidth = conn.thickness,
                        pathEffect = pathStyle
                    )
                    // Draw arrow head at target end
                    if (conn.lineStyle == "ARROW" || conn.lineStyle == "BIDIRECTIONAL") {
                        drawArrowHead(start, end, lineColor, conn.thickness)
                    }
                } else {
                    // Curved Bezier line - High-fidelity Cubic Bezier S-curve
                    val dx = end.x - start.x
                    val dy = end.y - start.y

                    val control1: Offset
                    val control2: Offset

                    if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                        // Horizontal dominance: S-curve horizontally
                        control1 = Offset(start.x + dx * 0.5f, start.y)
                        control2 = Offset(end.x - dx * 0.5f, end.y)
                    } else {
                        // Vertical dominance: S-curve vertically
                        control1 = Offset(start.x, start.y + dy * 0.5f)
                        control2 = Offset(end.x, end.y - dy * 0.5f)
                    }

                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        cubicTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
                    }

                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = conn.thickness, pathEffect = pathStyle)
                    )

                    // Draw arrow head on curved end
                    if (conn.lineStyle == "BIDIRECTIONAL" || conn.lineStyle == "CURVED") {
                        drawArrowHead(control2, end, lineColor, conn.thickness)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrowHead(
    start: Offset,
    end: Offset,
    color: Color,
    thickness: Float
) {
    val angle = atan2(end.y - start.y, end.x - start.x)
    val arrowLength = 22f
    val arrowAngle = Math.PI / 6 // 30 degrees

    // Let's retract the arrow slightly so it rests perfectly on card boundary (approx. 80dp retraction)
    val retractDistance = 60f
    val rx = end.x - retractDistance * cos(angle)
    val ry = end.y - retractDistance * sin(angle)
    val retractEnd = Offset(rx, ry)

    val p1 = Offset(
        retractEnd.x - arrowLength * cos(angle - arrowAngle).toFloat(),
        retractEnd.y - arrowLength * sin(angle - arrowAngle).toFloat()
    )
    val p2 = Offset(
        retractEnd.x - arrowLength * cos(angle + arrowAngle).toFloat(),
        retractEnd.y - arrowLength * sin(angle + arrowAngle).toFloat()
    )

    val path = Path().apply {
        moveTo(retractEnd.x, retractEnd.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path, color)
}
