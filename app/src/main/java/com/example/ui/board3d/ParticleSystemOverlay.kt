package com.example.ui.board3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.PlayerColor
import com.example.ui.theme.AlabasterLinen
import com.example.ui.theme.BrassHighlight
import com.example.ui.theme.BrushedBrass
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ParticleEffectType {
  FIREWORKS,
  CAPTURE_SHOCKWAVE,
  GOLDEN_AURA
}

data class ParticleBurstEvent(
  val id: Long = System.currentTimeMillis(),
  val type: ParticleEffectType,
  val primaryColor: PlayerColor,
  val secondaryColor: PlayerColor? = null,
  val subtitle: String = ""
)

private data class SimParticle(
  val originXRatio: Float,
  val originYRatio: Float,
  val vx: Float,
  val vy: Float,
  val gravity: Float,
  val sizeDp: Float,
  val color: Color,
  val delayFraction: Float = 0f
)

@Composable
fun ParticleSystemOverlay(
  event: ParticleBurstEvent?,
  onAnimationFinished: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (event == null) return

  val progress = remember(event.id) { Animatable(0f) }

  val durationMs = when (event.type) {
    ParticleEffectType.FIREWORKS -> 2400
    ParticleEffectType.CAPTURE_SHOCKWAVE -> 1350
    ParticleEffectType.GOLDEN_AURA -> 1650
  }

  val particles = remember(event.id) {
    buildParticlesForEvent(event)
  }

  LaunchedEffect(event.id) {
    progress.snapTo(0f)
    progress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
    )
    onAnimationFinished()
  }

  val t = progress.value

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    val minDim = size.minDimension

    val (primaryTone, _, softTone) = paletteForColor(event.primaryColor)
    val secondaryTone = event.secondaryColor?.let { paletteForColor(it).first } ?: BrassHighlight

    when (event.type) {
      ParticleEffectType.CAPTURE_SHOCKWAVE -> {
        // Double expanding shockwave rings at board center/capture zone
        val cx = w * 0.5f
        val cy = h * 0.5f
        val ringRadius1 = minDim * (0.08f + t * 0.42f)
        val alpha1 = (1f - t).coerceIn(0f, 1f)
        drawCircle(
          color = primaryTone.copy(alpha = alpha1 * 0.85f),
          radius = ringRadius1,
          center = Offset(cx, cy),
          style = Stroke(width = (10f * (1f - t * 0.6f)).coerceAtLeast(2f))
        )

        val t2 = ((t - 0.12f) / 0.88f).coerceIn(0f, 1f)
        if (t > 0.12f) {
          val ringRadius2 = minDim * (0.05f + t2 * 0.34f)
          drawCircle(
            color = secondaryTone.copy(alpha = (1f - t2) * 0.75f),
            radius = ringRadius2,
            center = Offset(cx, cy),
            style = Stroke(width = (6f * (1f - t2 * 0.5f)).coerceAtLeast(1.5f))
          )
        }
      }

      ParticleEffectType.GOLDEN_AURA -> {
        // Warm golden aura rings for Blockade or Double 6s
        val cx = w * 0.5f
        val cy = h * 0.5f
        val pulseAlpha = sin(t * PI).toFloat().coerceIn(0f, 1f)
        drawCircle(
          color = BrassHighlight.copy(alpha = pulseAlpha * 0.24f),
          radius = minDim * (0.16f + t * 0.18f),
          center = Offset(cx, cy)
        )
        drawCircle(
          color = BrushedBrass.copy(alpha = pulseAlpha * 0.85f),
          radius = minDim * (0.14f + t * 0.22f),
          center = Offset(cx, cy),
          style = Stroke(width = 5f)
        )
      }

      ParticleEffectType.FIREWORKS -> {
        // Multi-center flash glow on firework bursts
        val centers = listOf(
          0.5f to 0.42f,
          0.28f to 0.34f,
          0.72f to 0.36f
        )
        centers.forEachIndexed { idx, (rx, ry) ->
          val delay = idx * 0.18f
          val localT = ((t - delay) / (1f - delay)).coerceIn(0f, 1f)
          if (localT > 0f && localT < 0.45f) {
            val flashAlpha = (1f - localT / 0.45f) * 0.38f
            drawCircle(
              color = if (idx % 2 == 0) primaryTone.copy(alpha = flashAlpha) else BrassHighlight.copy(alpha = flashAlpha),
              radius = minDim * (0.06f + localT * 0.25f),
              center = Offset(w * rx, h * ry)
            )
          }
        }
      }
    }

    // Render all physics particles
    for (p in particles) {
      if (t < p.delayFraction) continue
      val localT = ((t - p.delayFraction) / (1f - p.delayFraction)).coerceIn(0f, 1f)
      val fade = (1f - localT * localT).coerceIn(0f, 1f)

      val px = w * p.originXRatio + p.vx * minDim * localT
      val py = h * p.originYRatio + p.vy * minDim * localT + 0.5f * p.gravity * minDim * localT * localT

      // Particle tail trail
      val prevT = (localT - 0.05f).coerceAtLeast(0f)
      val prevX = w * p.originXRatio + p.vx * minDim * prevT
      val prevY = h * p.originYRatio + p.vy * minDim * prevT + 0.5f * p.gravity * minDim * prevT * prevT

      drawLine(
        color = p.color.copy(alpha = fade * 0.55f),
        start = Offset(prevX, prevY),
        end = Offset(px, py),
        strokeWidth = p.sizeDp * 0.85f
      )

      drawCircle(
        color = p.color.copy(alpha = fade),
        radius = p.sizeDp * (0.45f + 0.55f * fade),
        center = Offset(px, py)
      )

      if (fade > 0.5f) {
        drawCircle(
          color = softTone.copy(alpha = fade * 0.7f),
          radius = p.sizeDp * 0.32f,
          center = Offset(px, py)
        )
      }
    }
  }
}

private fun buildParticlesForEvent(event: ParticleBurstEvent): List<SimParticle> {
  val rng = Random(event.id)
  val (primary, _, soft) = paletteForColor(event.primaryColor)
  val secondary = event.secondaryColor?.let { paletteForColor(it).first } ?: BrassHighlight
  val palette = listOf(primary, secondary, BrushedBrass, BrassHighlight, soft, AlabasterLinen)

  val list = mutableListOf<SimParticle>()
  when (event.type) {
    ParticleEffectType.FIREWORKS -> {
      val burstOrigins = listOf(
        Triple(0.50f, 0.44f, 0.0f),
        Triple(0.26f, 0.34f, 0.16f),
        Triple(0.74f, 0.35f, 0.28f),
        Triple(0.50f, 0.26f, 0.42f)
      )
      for ((ox, oy, delay) in burstOrigins) {
        val count = 38
        for (i in 0 until count) {
          val angle = (i.toDouble() / count.toDouble()) * 2.0 * PI + rng.nextDouble(-0.08, 0.08)
          val speed = rng.nextFloat() * 0.38f + 0.14f
          list.add(
            SimParticle(
              originXRatio = ox,
              originYRatio = oy,
              vx = (cos(angle) * speed).toFloat(),
              vy = (sin(angle) * speed).toFloat() - 0.06f,
              gravity = 0.48f,
              sizeDp = rng.nextFloat() * 5.5f + 4.0f,
              color = palette[i % palette.size],
              delayFraction = delay
            )
          )
        }
      }
    }

    ParticleEffectType.CAPTURE_SHOCKWAVE -> {
      val count = 48
      for (i in 0 until count) {
        val angle = (i.toDouble() / count.toDouble()) * 2.0 * PI + rng.nextDouble(-0.1, 0.1)
        val speed = rng.nextFloat() * 0.42f + 0.12f
        list.add(
          SimParticle(
            originXRatio = 0.5f,
            originYRatio = 0.5f,
            vx = (cos(angle) * speed).toFloat(),
            vy = (sin(angle) * speed).toFloat(),
            gravity = 0.18f,
            sizeDp = rng.nextFloat() * 5.0f + 3.5f,
            color = if (i % 2 == 0) primary else secondary,
            delayFraction = 0f
          )
        )
      }
    }

    ParticleEffectType.GOLDEN_AURA -> {
      val count = 42
      for (i in 0 until count) {
        val angle = (i.toDouble() / count.toDouble()) * 2.0 * PI
        val radiusOffset = rng.nextFloat() * 0.18f + 0.03f
        list.add(
          SimParticle(
            originXRatio = 0.5f + (cos(angle) * radiusOffset).toFloat(),
            originYRatio = 0.52f + (sin(angle) * radiusOffset).toFloat(),
            vx = (cos(angle) * 0.12f).toFloat(),
            vy = -(rng.nextFloat() * 0.32f + 0.10f),
            gravity = -0.08f,
            sizeDp = rng.nextFloat() * 5.0f + 3.5f,
            color = if (i % 3 == 0) BrassHighlight else if (i % 3 == 1) BrushedBrass else primary,
            delayFraction = rng.nextFloat() * 0.22f
          )
        )
      }
    }
  }
  return list
}
