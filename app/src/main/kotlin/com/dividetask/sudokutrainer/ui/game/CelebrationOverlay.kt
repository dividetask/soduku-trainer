package com.dividetask.sudokutrainer.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val FIREWORK_COLORS = listOf(
    Color(0xFFFFD700),
    Color(0xFFFF4444),
    Color(0xFF44FF44),
    Color(0xFF4488FF),
    Color(0xFFFF44FF),
    Color(0xFFFFFFFF),
    Color(0xFFFF8800),
    Color(0xFF44FFFF),
)

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var life: Float = 1f,
    val decay: Float,
    val size: Float,
)

/**
 * Fireworks particle overlay. [speedMultiplier] scales particle velocity
 * and [durationMs] controls how long the animation runs (bursts are
 * spread evenly across the window).
 */
@Composable
fun CelebrationOverlay(
    speedMultiplier: Float,
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    val particles = remember { mutableStateListOf<Particle>() }

    LaunchedEffect(Unit) {
        val rng = Random.Default
        val frameDelay = 16L
        val burstInterval = 1500L
        val maxBursts = (durationMs / burstInterval).toInt().coerceAtLeast(4)
        var burstCount = 0
        var elapsed = 0L

        while (isActive && elapsed < durationMs) {
            if (burstCount < maxBursts && (burstCount == 0 || elapsed >= burstCount * burstInterval)) {
                val cx = 0.1f + rng.nextFloat() * 0.8f
                val cy = 0.1f + rng.nextFloat() * 0.5f
                val burstColor = FIREWORK_COLORS[rng.nextInt(FIREWORK_COLORS.size)]
                val count = 50 + rng.nextInt(40)
                for (i in 0 until count) {
                    val angle = rng.nextFloat() * 2f * Math.PI.toFloat()
                    val baseSpeed = 0.002f + rng.nextFloat() * 0.006f
                    particles += Particle(
                        x = cx,
                        y = cy,
                        vx = cos(angle) * baseSpeed * speedMultiplier,
                        vy = sin(angle) * baseSpeed * speedMultiplier,
                        color = if (rng.nextFloat() < 0.3f) Color.White else burstColor,
                        decay = 0.004f + rng.nextFloat() * 0.008f,
                        size = 3f + rng.nextFloat() * 5f,
                    )
                }
                burstCount++
            }

            delay(frameDelay)
            elapsed += frameDelay

            val iter = particles.listIterator()
            while (iter.hasNext()) {
                val p = iter.next()
                p.x += p.vx
                p.y += p.vy
                p.vy += 0.00008f * speedMultiplier
                p.vx *= 0.99f
                p.vy *= 0.99f
                p.life -= p.decay
                if (p.life <= 0f) iter.remove()
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        for (p in particles) {
            drawCircle(
                color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                radius = p.size * p.life,
                center = Offset(p.x * size.width, p.y * size.height),
            )
        }
    }
}
