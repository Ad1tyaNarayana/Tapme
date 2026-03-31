package com.nfcupi.pay.ui.screens.receive.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nfcupi.pay.ui.theme.TapmeOrange

@Composable
fun PulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")

    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 0, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "a1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 180, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "a2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.22f, targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, delayMillis = 360, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "a3"
    )

    Canvas(modifier = Modifier.size(width = 160.dp, height = 80.dp)) {
        val cx = size.width / 2f
        val dotY = size.height - 4.dp.toPx()

        // Arc 1 — innermost, strongest
        val arc1 = Path().apply {
            moveTo(cx - 28.dp.toPx(), dotY - 32.dp.toPx())
            quadraticTo(cx, dotY - 52.dp.toPx(), cx + 28.dp.toPx(), dotY - 32.dp.toPx())
        }
        drawPath(arc1, color = TapmeOrange, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round), alpha = alpha1)

        // Arc 2 — middle
        val arc2 = Path().apply {
            moveTo(cx - 40.dp.toPx(), dotY - 18.dp.toPx())
            quadraticTo(cx, dotY - 42.dp.toPx(), cx + 40.dp.toPx(), dotY - 18.dp.toPx())
        }
        drawPath(arc2, color = TapmeOrange, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round), alpha = alpha2)

        // Arc 3 — outermost, faintest
        val arc3 = Path().apply {
            moveTo(cx - 54.dp.toPx(), dotY - 4.dp.toPx())
            quadraticTo(cx, dotY - 30.dp.toPx(), cx + 54.dp.toPx(), dotY - 4.dp.toPx())
        }
        drawPath(arc3, color = TapmeOrange, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round), alpha = alpha3)

        // Dot glow halo
        drawCircle(color = TapmeOrange, radius = 9.dp.toPx(), center = Offset(cx, dotY), alpha = 0.12f)
        // Dot core
        drawCircle(color = TapmeOrange, radius = 3.5.dp.toPx(), center = Offset(cx, dotY), alpha = 0.9f)
    }
}
