package com.kidslanguage.companion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kidslanguage.companion.ui.theme.AccentOrange
import com.kidslanguage.companion.ui.theme.AccentYellow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Six decorative stars expand once around the pose, then fade; no interaction or looping. */
@Composable
internal fun SuccessStars(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (progress <= 0f || progress >= 1f) return@Canvas
        val opacity = minOf(progress / 0.12f, (1f - progress) / 0.6f).coerceIn(0f, 1f)
        val distance = 0.38f + progress * 0.07f
        for (index in 0 until 6) {
            val angle = (-150 + index * 60) * PI / 180
            val centerX = center.x + cos(angle).toFloat() * size.width * distance
            val centerY = center.y + sin(angle).toFloat() * size.height * distance
            val radius = size.minDimension * (if (index % 2 == 0) 0.042f else 0.033f)
            val star = Path()
            for (point in 0 until 10) {
                val direction = -PI / 2 + point * PI / 5
                val length = if (point % 2 == 0) radius else radius * 0.45f
                val x = centerX + cos(direction).toFloat() * length
                val y = centerY + sin(direction).toFloat() * length
                if (point == 0) star.moveTo(x, y) else star.lineTo(x, y)
            }
            star.close()
            drawPath(star, AccentYellow, alpha = opacity)
            drawPath(star, AccentOrange, alpha = opacity, style = Stroke(radius * 0.1f))
        }
    }
}
