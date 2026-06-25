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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val FIREWORK_COLORS = listOf(
    Color(0xFFFFD700), // gold
    Color(0xFFFF3D6E), // hot pink
    Color(0xFF34E89E), // green
    Color(0xFF4DA6FF), // sky blue
    Color(0xFFB388FF), // lavender
    Color(0xFFFFFFFF), // white
    Color(0xFFFF8C42), // orange
    Color(0xFF18FFFF), // cyan
)

private enum class Shape { Ring, Burst, Chrysanthemum, Willow }

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    var life: Float = 1f,
    val decay: Float,
    val size: Float,
    val trail: ArrayDeque<Offset> = ArrayDeque(),
    val sparkle: Boolean,
)

/**
 * Fireworks particle overlay. [speedMultiplier] scales particle velocity
 * and [durationMs] controls how long the animation runs (bursts are
 * spread evenly across the window).
 *
 * Bursts pick a random shape (ring / burst / chrysanthemum / willow),
 * carry short fading trails behind each particle, and overdraw with
 * Plus blend mode so overlapping particles glow hot. Bursts size scales
 * by a uniform random factor so the screen isn't full of same-size
 * bursts.
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
        val burstInterval = 900L
        val maxBursts = (durationMs / burstInterval).toInt().coerceAtLeast(4)
        var burstCount = 0
        var elapsed = 0L

        while (isActive && elapsed < durationMs) {
            val due = burstCount == 0 || elapsed >= burstCount * burstInterval
            if (burstCount < maxBursts && due) {
                spawnBurst(particles, rng, speedMultiplier)
                burstCount++
                // Occasionally double up so it doesn't feel metronomic.
                if (rng.nextFloat() < 0.35f && burstCount < maxBursts) {
                    spawnBurst(particles, rng, speedMultiplier)
                    burstCount++
                }
            }

            delay(frameDelay)
            elapsed += frameDelay

            val iter = particles.listIterator()
            while (iter.hasNext()) {
                val p = iter.next()
                // Tuck the previous position into the trail before moving.
                p.trail.addFirst(Offset(p.x, p.y))
                if (p.trail.size > MAX_TRAIL) p.trail.removeLast()
                p.x += p.vx
                p.y += p.vy
                p.vy += 0.00012f * speedMultiplier  // gravity
                p.vx *= 0.985f
                p.vy *= 0.985f
                p.life -= p.decay
                if (p.life <= 0f) iter.remove()
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (p in particles) {
            val alpha = p.life.coerceIn(0f, 1f)
            // Trail: progressively smaller + dimmer dots behind the head.
            // Plain alpha blending so the color of the streak reads true
            // instead of clipping to white.
            var step = 0
            for (t in p.trail) {
                step++
                val trailAlpha = alpha * (1f - step.toFloat() / MAX_TRAIL) * 0.45f
                if (trailAlpha <= 0.02f) continue
                drawCircle(
                    color = p.color.copy(alpha = trailAlpha),
                    radius = (p.size * p.life) *
                        (1.0f - step.toFloat() / (MAX_TRAIL + 2)),
                    center = Offset(t.x * w, t.y * h),
                )
            }
            // Head: a soft outer halo at lower alpha plus a solid core.
            // Two layers, both using the particle's actual color — keeps
            // the burst bead-shaped and the color readable.
            val cx = p.x * w
            val cy = p.y * h
            drawCircle(
                color = p.color.copy(alpha = alpha * 0.35f),
                radius = p.size * p.life * 1.8f,
                center = Offset(cx, cy),
            )
            drawCircle(
                color = p.color.copy(alpha = alpha),
                radius = p.size * p.life,
                center = Offset(cx, cy),
            )
            if (p.sparkle) {
                // A small bright dot — same color, just brighter — so
                // we don't wash everything out toward pure white.
                drawCircle(
                    color = p.color.copy(alpha = (alpha + 0.2f).coerceAtMost(1f)),
                    radius = (p.size * p.life) * 0.4f,
                    center = Offset(cx, cy),
                )
            }
        }
    }
}

private const val MAX_TRAIL = 8

private fun spawnBurst(
    particles: MutableList<Particle>,
    rng: Random,
    speedMultiplier: Float,
) {
    val cx = 0.1f + rng.nextFloat() * 0.8f
    val cy = 0.08f + rng.nextFloat() * 0.55f
    val burstColor = FIREWORK_COLORS[rng.nextInt(FIREWORK_COLORS.size)]
    val secondaryColor = FIREWORK_COLORS[rng.nextInt(FIREWORK_COLORS.size)]
    // Vary burst scale so the show feels less uniform.
    val sizeFactor = 0.7f + rng.nextFloat() * 1.1f
    val count = (80 + rng.nextInt(80) * sizeFactor).toInt()
    val baseSpeedFloor = 0.0035f * sizeFactor
    val baseSpeedRange = 0.0085f * sizeFactor

    when (Shape.entries[rng.nextInt(Shape.entries.size)]) {
        Shape.Ring -> {
            // Even ring at a single radius — the classic "O".
            val ringSpeed = baseSpeedFloor + baseSpeedRange * 0.7f
            for (i in 0 until count) {
                val angle = (i.toFloat() / count) * 2f * PI.toFloat()
                particles += newParticle(
                    cx, cy,
                    cos(angle) * ringSpeed * speedMultiplier,
                    sin(angle) * ringSpeed * speedMultiplier,
                    pickColor(rng, burstColor, secondaryColor),
                    decay = 0.0035f + rng.nextFloat() * 0.004f,
                    size = (7f + rng.nextFloat() * 5f) * sizeFactor,
                    sparkle = rng.nextFloat() < 0.25f,
                )
            }
        }
        Shape.Burst -> {
            // Random angle + speed — chaotic spray.
            for (i in 0 until count) {
                val angle = rng.nextFloat() * 2f * PI.toFloat()
                val speed = baseSpeedFloor + rng.nextFloat() * baseSpeedRange
                particles += newParticle(
                    cx, cy,
                    cos(angle) * speed * speedMultiplier,
                    sin(angle) * speed * speedMultiplier,
                    pickColor(rng, burstColor, secondaryColor),
                    decay = 0.004f + rng.nextFloat() * 0.006f,
                    size = (6.5f + rng.nextFloat() * 6f) * sizeFactor,
                    sparkle = rng.nextFloat() < 0.3f,
                )
            }
        }
        Shape.Chrysanthemum -> {
            // Dense layered ring — two speeds, brighter core.
            for (i in 0 until count) {
                val angle = (i.toFloat() / count) * 2f * PI.toFloat() +
                    (rng.nextFloat() - 0.5f) * 0.08f
                val outerLayer = i % 2 == 0
                val speed = if (outerLayer) {
                    baseSpeedFloor + baseSpeedRange * 0.8f
                } else {
                    baseSpeedFloor + baseSpeedRange * 0.45f
                }
                particles += newParticle(
                    cx, cy,
                    cos(angle) * speed * speedMultiplier,
                    sin(angle) * speed * speedMultiplier,
                    pickColor(rng, burstColor, secondaryColor),
                    decay = 0.003f + rng.nextFloat() * 0.004f,
                    size = (4.5f + rng.nextFloat() * 5f) * sizeFactor,
                    sparkle = rng.nextFloat() < 0.35f,
                )
            }
        }
        Shape.Willow -> {
            // Heavy downward-trailing burst — short upward arc, long fall.
            for (i in 0 until count) {
                val angle = rng.nextFloat() * 2f * PI.toFloat()
                val speed = baseSpeedFloor * 0.7f + rng.nextFloat() * baseSpeedRange * 0.6f
                particles += newParticle(
                    cx, cy,
                    cos(angle) * speed * speedMultiplier,
                    sin(angle) * speed * speedMultiplier - 0.0015f * speedMultiplier,
                    pickColor(rng, burstColor, secondaryColor),
                    decay = 0.0025f + rng.nextFloat() * 0.0035f,
                    size = (6f + rng.nextFloat() * 5f) * sizeFactor,
                    sparkle = rng.nextFloat() < 0.4f,
                )
            }
        }
    }
}

private fun pickColor(rng: Random, primary: Color, secondary: Color): Color = when {
    // Keep white rare so colors stay readable. Most particles take the
    // burst's primary color, a chunk take its secondary, a few are white.
    rng.nextFloat() < 0.06f -> Color.White
    rng.nextFloat() < 0.30f -> secondary
    else -> primary
}

private fun newParticle(
    x: Float, y: Float,
    vx: Float, vy: Float,
    color: Color,
    decay: Float,
    size: Float,
    sparkle: Boolean,
): Particle = Particle(
    x = x, y = y,
    vx = vx, vy = vy,
    color = color,
    decay = decay,
    size = size,
    sparkle = sparkle,
)
