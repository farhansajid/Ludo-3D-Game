package com.example.ui.board3d

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.example.data.ActivePawnEmoteEffect
import com.example.data.PawnEmoteItem
import com.example.data.PawnEmoteVisualStyle
import com.example.data.PlayerColor
import com.example.data.PlayerSeat
import com.example.data.Tutorial3DBeaconSpec
import com.example.data.TutorialHighlightZoneType
import com.example.domain.LudoRulesEngine
import com.example.ui.theme.AegeanTeal
import com.example.ui.theme.AegeanTealDark
import com.example.ui.theme.AegeanTealSoft
import com.example.ui.theme.AlabasterLinen
import com.example.ui.theme.BrassHighlight
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.CoralAmber
import com.example.ui.theme.CoralAmberDark
import com.example.ui.theme.CoralAmberSoft
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.EucalyptusSageDark
import com.example.ui.theme.EucalyptusSoft
import com.example.ui.theme.NordicSlateBlue
import com.example.ui.theme.NordicSlateDark
import com.example.ui.theme.NordicSlateSoft
import com.example.ui.theme.Obsidianink
import com.example.ui.theme.RoseQuartz
import com.example.ui.theme.RoseQuartzDark
import com.example.ui.theme.RoseQuartzSoft
import com.example.ui.theme.RoyalAmethyst
import com.example.ui.theme.RoyalAmethystDark
import com.example.ui.theme.RoyalAmethystSoft
import com.example.ui.theme.StoneBorderLight
import com.example.ui.theme.TerracottaRed
import com.example.ui.theme.TerracottaRedDark
import com.example.ui.theme.TerracottaSoft
import com.example.ui.theme.TravertineStone
import com.example.ui.theme.WarmOchreDark
import com.example.ui.theme.WarmOchreGold
import com.example.ui.theme.WarmOchreSoft
import com.example.ui.theme.WarmSandCard
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

data class Vec3(val x: Float, val y: Float, val z: Float)

data class ProjectedPoint(
  val screenX: Float,
  val screenY: Float,
  val depth: Float,
  val scale: Float
)

data class AnimatedTokenState(
  val tokenIndex: Int = -1,
  val fromPos: Int = -1,
  val toPos: Int = -1,
  val progress: Float = 0f,
  val trailPathPositions: List<Int> = emptyList(),
  val fadingWakeAlpha: Float = 1f
) {
  val tokenColor: PlayerColor
    get() = if (tokenIndex >= 0) LudoRulesEngine.colorForTokenIndex(tokenIndex) else PlayerColor.RED
}

data class PawnTrailSample3D(
  val worldPos: Vec3,
  val groundPos: Vec3,
  val intensity: Float // 0f (oldest tail tip) .. 1f (leading pawn head)
)

data class Tutorial3DBoardOverlay(
  val beacons: List<Tutorial3DBeaconSpec> = emptyList(),
  val trajectoryFromWorld: Vec3? = null,
  val trajectoryToWorld: Vec3? = null,
  val showTrajectoryArc: Boolean = false,
  val activeTargetTokenIndex: Int = -1
)

enum class DayNightPhase(val label: String, val badgeSymbol: String) {
  DAWN("Golden Dawn", "🌅"),
  MORNING("Morning Sun", "🌤️"),
  MIDDAY("Zenith Noon", "☀️"),
  GOLDEN_DUSK("Amber Dusk", "🌇"),
  NIGHT("Moonlit Night", "🌙")
}

enum class DayNightCycleMode(val shortLabel: String, val fixedFractionalHour: Float?) {
  REAL_TIME_LOCAL("Live Local", null),
  DAWN_PREVIEW("Dawn", 6.5f),
  MIDDAY_PREVIEW("Midday", 12.5f),
  DUSK_PREVIEW("Dusk", 18.0f),
  NIGHT_PREVIEW("Night", 2.0f);

  fun nextMode(): DayNightCycleMode {
    val all = entries
    return all[(ordinal + 1) % all.size]
  }
}

data class SceneLightingState3D(
  val phase: DayNightPhase,
  val fractionalHour: Float,
  val formattedLocalTime: String,
  val lightAzimuthDeg: Float,
  val lightElevationDeg: Float,
  val shadowDirX: Float,
  val shadowDirY: Float,
  val shadowStretchFactor: Float,
  val shadowColor: Color,
  val ambientWashColor: Color,
  val skyHorizonGlowColor: Color,
  val plinthTopColor: Color,
  val plinthSideColorA: Color,
  val plinthSideColorB: Color,
  val specularHighlightColor: Color,
  val lanternGlowIntensity: Float // 0f during bright day .. 0.95f at night
)

object DayNightLightingEngine {
  private data class LightingKeyframe(
    val hour: Float,
    val phase: DayNightPhase,
    val azimuthDeg: Float,
    val elevationDeg: Float,
    val shadowDirX: Float,
    val shadowDirY: Float,
    val shadowStretch: Float,
    val shadowColor: Color,
    val ambientWash: Color,
    val horizonGlow: Color,
    val plinthTop: Color,
    val plinthSideA: Color,
    val plinthSideB: Color,
    val specularHighlight: Color,
    val lanternGlow: Float
  )

  private val KEYFRAMES = listOf(
    // 00:00 Deep Moonlit Night (Celestial moonlight from NE, long cool SW shadows, warm board lanterns)
    LightingKeyframe(
      hour = 0f,
      phase = DayNightPhase.NIGHT,
      azimuthDeg = 128f,
      elevationDeg = 32f,
      shadowDirX = 0.25f,
      shadowDirY = -0.19f,
      shadowStretch = 1.48f,
      shadowColor = Color(0xFF0A1022).copy(alpha = 0.42f),
      ambientWash = Color(0xFF162447).copy(alpha = 0.16f),
      horizonGlow = Color(0xFF3E5C9A).copy(alpha = 0.18f),
      plinthTop = Color(0xFFE5E2DA),
      plinthSideA = Color(0xFFB8B3A8),
      plinthSideB = Color(0xFF9E998E),
      specularHighlight = Color(0xFFDCE8FF),
      lanternGlow = 0.94f
    ),
    // 05:00 Pre-Dawn Twilight
    LightingKeyframe(
      hour = 5.0f,
      phase = DayNightPhase.NIGHT,
      azimuthDeg = -82f,
      elevationDeg = 24f,
      shadowDirX = -0.30f,
      shadowDirY = 0.16f,
      shadowStretch = 1.62f,
      shadowColor = Color(0xFF141830).copy(alpha = 0.38f),
      ambientWash = Color(0xFF2B2D5C).copy(alpha = 0.13f),
      horizonGlow = Color(0xFFD88373).copy(alpha = 0.16f),
      plinthTop = Color(0xFFEAE3DA),
      plinthSideA = Color(0xFFC4B9AE),
      plinthSideB = Color(0xFFACA196),
      specularHighlight = Color(0xFFF6D9CE),
      lanternGlow = 0.72f
    ),
    // 06:45 Rose-Gold Dawn (Low eastern sun casting long westward shadows)
    LightingKeyframe(
      hour = 6.75f,
      phase = DayNightPhase.DAWN,
      azimuthDeg = -66f,
      elevationDeg = 26f,
      shadowDirX = -0.35f,
      shadowDirY = 0.14f,
      shadowStretch = 1.75f,
      shadowColor = Color(0xFF281C32).copy(alpha = 0.33f),
      ambientWash = Color(0xFFF2A679).copy(alpha = 0.11f),
      horizonGlow = Color(0xFFFFB88C).copy(alpha = 0.20f),
      plinthTop = Color(0xFFF6EDE2),
      plinthSideA = Color(0xFFDCCAB8),
      plinthSideB = Color(0xFFC4B19E),
      specularHighlight = Color(0xFFFFE5CC),
      lanternGlow = 0.25f
    ),
    // 09:30 Crisp Morning Sun
    LightingKeyframe(
      hour = 9.5f,
      phase = DayNightPhase.MORNING,
      azimuthDeg = -34f,
      elevationDeg = 52f,
      shadowDirX = -0.17f,
      shadowDirY = 0.10f,
      shadowStretch = 1.24f,
      shadowColor = Color(0xFF1E1A17).copy(alpha = 0.28f),
      ambientWash = Color(0xFFFFF6E5).copy(alpha = 0.05f),
      horizonGlow = Color(0xFFFFEAB5).copy(alpha = 0.12f),
      plinthTop = AlabasterLinen,
      plinthSideA = Color(0xFFD8CFC0),
      plinthSideB = Color(0xFFC2B8A4),
      specularHighlight = Color(0xFFFFFBF2),
      lanternGlow = 0.0f
    ),
    // 13:00 High Zenith Midday (Compact tight shadows directly beneath pawns & plinth)
    LightingKeyframe(
      hour = 13.0f,
      phase = DayNightPhase.MIDDAY,
      azimuthDeg = 6f,
      elevationDeg = 76f,
      shadowDirX = 0.04f,
      shadowDirY = 0.05f,
      shadowStretch = 0.95f,
      shadowColor = Color(0xFF141210).copy(alpha = 0.26f),
      ambientWash = Color(0xFFFFFFFF).copy(alpha = 0.02f),
      horizonGlow = Color(0xFFFFF3D1).copy(alpha = 0.09f),
      plinthTop = AlabasterLinen,
      plinthSideA = Color(0xFFD5CCB8),
      plinthSideB = Color(0xFFBEB49E),
      specularHighlight = Color(0xFFFFFFFF),
      lanternGlow = 0.0f
    ),
    // 16:30 Late Afternoon Warmth
    LightingKeyframe(
      hour = 16.5f,
      phase = DayNightPhase.GOLDEN_DUSK,
      azimuthDeg = 46f,
      elevationDeg = 42f,
      shadowDirX = 0.24f,
      shadowDirY = 0.12f,
      shadowStretch = 1.42f,
      shadowColor = Color(0xFF261A14).copy(alpha = 0.31f),
      ambientWash = Color(0xFFF29F58).copy(alpha = 0.09f),
      horizonGlow = Color(0xFFFFB36B).copy(alpha = 0.16f),
      plinthTop = Color(0xFFF6ECDD),
      plinthSideA = Color(0xFFD8C4AA),
      plinthSideB = Color(0xFFBFAB92),
      specularHighlight = Color(0xFFFFE0B5),
      lanternGlow = 0.28f
    ),
    // 18:30 Golden Hour Sunset (Low western sun casting long eastward shadows & warm amber glaze)
    LightingKeyframe(
      hour = 18.5f,
      phase = DayNightPhase.GOLDEN_DUSK,
      azimuthDeg = 68f,
      elevationDeg = 22f,
      shadowDirX = 0.38f,
      shadowDirY = 0.16f,
      shadowStretch = 1.82f,
      shadowColor = Color(0xFF241628).copy(alpha = 0.36f),
      ambientWash = Color(0xFFE87A48).copy(alpha = 0.13f),
      horizonGlow = Color(0xFFFF9A62).copy(alpha = 0.22f),
      plinthTop = Color(0xFFF2E4D4),
      plinthSideA = Color(0xFFD1BBA6),
      plinthSideB = Color(0xFFB59F8B),
      specularHighlight = Color(0xFFFFCFA3),
      lanternGlow = 0.65f
    ),
    // 20:30 Evening Nocturne Transition
    LightingKeyframe(
      hour = 20.5f,
      phase = DayNightPhase.NIGHT,
      azimuthDeg = 108f,
      elevationDeg = 28f,
      shadowDirX = 0.27f,
      shadowDirY = -0.14f,
      shadowStretch = 1.52f,
      shadowColor = Color(0xFF0C1226).copy(alpha = 0.40f),
      ambientWash = Color(0xFF19274E).copy(alpha = 0.15f),
      horizonGlow = Color(0xFF43619E).copy(alpha = 0.17f),
      plinthTop = Color(0xFFE7E3DB),
      plinthSideA = Color(0xFFBCB6AA),
      plinthSideB = Color(0xFFA29C90),
      specularHighlight = Color(0xFFDCE8FF),
      lanternGlow = 0.90f
    )
  )

  fun computeFractionalHourFromMillis(
    timeMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault()
  ): Float {
    val cal = Calendar.getInstance(timeZone).apply {
      this.timeInMillis = timeMillis
    }
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    val s = cal.get(Calendar.SECOND)
    return (h + (m / 60f) + (s / 3600f)).coerceIn(0f, 23.999f)
  }

  fun formatHourMinute(fractionalHour: Float): String {
    val wrapped = ((fractionalHour % 24f) + 24f) % 24f
    val totalMinutes = (wrapped * 60f).toInt()
    val h24 = (totalMinutes / 60) % 24
    val min = totalMinutes % 60
    return String.format(Locale.US, "%02d:%02d", h24, min)
  }

  fun phaseForHour(fractionalHour: Float): DayNightPhase {
    val h = ((fractionalHour % 24f) + 24f) % 24f
    return when {
      h in 5.25f..8.0f -> DayNightPhase.DAWN
      h in 8.0f..11.5f -> DayNightPhase.MORNING
      h in 11.5f..16.0f -> DayNightPhase.MIDDAY
      h in 16.0f..19.5f -> DayNightPhase.GOLDEN_DUSK
      else -> DayNightPhase.NIGHT
    }
  }

  fun computeLightingForFractionalHour(
    fractionalHour: Float,
    formattedTimeOverride: String? = null
  ): SceneLightingState3D {
    val h = ((fractionalHour % 24f) + 24f) % 24f
    val count = KEYFRAMES.size
    var idxA = count - 1
    var idxB = 0

    for (i in 0 until count) {
      if (KEYFRAMES[i].hour <= h) {
        idxA = i
        idxB = (i + 1) % count
      }
    }

    val kfA = KEYFRAMES[idxA]
    val kfB = KEYFRAMES[idxB]
    val startH = kfA.hour
    val endH = if (idxB == 0) 24f else kfB.hour
    val currentH = if (h < startH) h + 24f else h
    val span = (endH - startH).coerceAtLeast(0.01f)
    val t = ((currentH - startH) / span).coerceIn(0f, 1f)

    fun lerpF(a: Float, b: Float): Float = a + (b - a) * t

    return SceneLightingState3D(
      phase = phaseForHour(h),
      fractionalHour = h,
      formattedLocalTime = formattedTimeOverride ?: formatHourMinute(h),
      lightAzimuthDeg = lerpF(kfA.azimuthDeg, kfB.azimuthDeg),
      lightElevationDeg = lerpF(kfA.elevationDeg, kfB.elevationDeg),
      shadowDirX = lerpF(kfA.shadowDirX, kfB.shadowDirX),
      shadowDirY = lerpF(kfA.shadowDirY, kfB.shadowDirY),
      shadowStretchFactor = lerpF(kfA.shadowStretch, kfB.shadowStretch),
      shadowColor = lerp(kfA.shadowColor, kfB.shadowColor, t),
      ambientWashColor = lerp(kfA.ambientWash, kfB.ambientWash, t),
      skyHorizonGlowColor = lerp(kfA.horizonGlow, kfB.horizonGlow, t),
      plinthTopColor = lerp(kfA.plinthTop, kfB.plinthTop, t),
      plinthSideColorA = lerp(kfA.plinthSideA, kfB.plinthSideA, t),
      plinthSideColorB = lerp(kfA.plinthSideB, kfB.plinthSideB, t),
      specularHighlightColor = lerp(kfA.specularHighlight, kfB.specularHighlight, t),
      lanternGlowIntensity = lerpF(kfA.lanternGlow, kfB.lanternGlow).coerceIn(0f, 1f)
    )
  }

  fun computeLightingForRealTime(
    timeMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
  ): SceneLightingState3D {
    val fractional = computeFractionalHourFromMillis(timeMillis, timeZone)
    return computeLightingForFractionalHour(fractional)
  }
}

private data class ProjectedTutorialBeaconAnchor(
  val spec: Tutorial3DBeaconSpec,
  val screenX: Float,
  val screenY: Float,
  val scale: Float
)

private data class ProjectedTokenHitTarget(
  val tokenIndex: Int,
  val screenX: Float,
  val screenY: Float,
  val hitRadiusPx: Float,
  val depth: Float
)

object LudoBoardCoordinates {
  // 52 perimeter track squares in clockwise order starting at Arm 0 Start (1, 6) for 4-arm board
  val MAIN_TRACK_COORDS: List<Pair<Float, Float>> = listOf(
    1f to 6f, 2f to 6f, 3f to 6f, 4f to 6f, 5f to 6f,
    6f to 5f, 6f to 4f, 6f to 3f, 6f to 2f, 6f to 1f, 6f to 0f,
    7f to 0f,
    8f to 0f, 8f to 1f, 8f to 2f, 8f to 3f, 8f to 4f, 8f to 5f,
    9f to 6f, 10f to 6f, 11f to 6f, 12f to 6f, 13f to 6f, 14f to 6f,
    14f to 7f,
    14f to 8f, 13f to 8f, 12f to 8f, 11f to 8f, 10f to 8f, 9f to 8f,
    8f to 9f, 8f to 10f, 8f to 11f, 8f to 12f, 8f to 13f, 8f to 14f,
    7f to 14f,
    6f to 14f, 6f to 13f, 6f to 12f, 6f to 11f, 6f to 10f, 6f to 9f,
    5f to 8f, 4f to 8f, 3f to 8f, 2f to 8f, 1f to 8f, 0f to 8f,
    0f to 7f,
    0f to 6f
  )

  fun yardPedestalGridCoordForArm(armIndex: Int, slotIndex: Int): Pair<Float, Float> {
    val baseCol = when (armIndex % 4) {
      0, 3 -> 1.5f
      else -> 10.5f
    }
    val baseRow = when (armIndex % 4) {
      0, 1 -> 1.5f
      else -> 10.5f
    }
    val colOffset = if (slotIndex % 2 == 0) 0.35f else 2.65f
    val rowOffset = if (slotIndex / 2 == 0) 0.35f else 2.65f
    return (baseCol + colOffset) to (baseRow + rowOffset)
  }

  fun homeStretchGridCoordForArm(armIndex: Int, stepIndex: Int): Pair<Float, Float> {
    val s = (stepIndex + 1).coerceIn(1, 5).toFloat()
    return when (armIndex % 4) {
      0 -> s to 7f
      1 -> 7f to s
      2 -> (14f - s) to 7f
      else -> 7f to (14f - s)
    }
  }

  fun homeGoalGridCoordForArm(armIndex: Int, slotIndex: Int): Pair<Float, Float> {
    val spread = (slotIndex - 1.5f) * 0.28f
    return when (armIndex % 4) {
      0 -> 6.32f to (7.0f + spread)
      1 -> (7.0f + spread) to 6.32f
      2 -> 7.68f to (7.0f + spread)
      else -> (7.0f + spread) to 7.68f
    }
  }

  /**
   * Radial 3D coordinate generator for 6-player (Hexagonal) and 8-player (Octagonal) boards.
   * Returns (worldX, worldY) in [-7.6, 7.6] space.
   */
  fun radialArmAngleRad(armIndex: Int, armCount: Int): Double {
    return PI + (armIndex.toDouble() / armCount.toDouble()) * 2.0 * PI
  }

  fun radialTrackWorldCoord(globalIdx: Int, armCount: Int): Pair<Float, Float> {
    val armIdx = (globalIdx / 13) % armCount
    val offset = globalIdx % 13
    // Each arm has 13 tiles:
    // 0..4: entry lane moving inward from r=6.6 to r=2.9
    // 5..10: next arm's outward lane moving from r=2.9 to r=6.9
    // 11: tip middle turn at next arm's outer tip
    // 12: next arm's outer corner before start
    val targetArm = if (offset in 0..4) armIdx else (armIdx + 1) % armCount
    val angle = radialArmAngleRad(targetArm, armCount)
    val dirX = cos(angle).toFloat()
    val dirY = sin(angle).toFloat()
    // Perpendicular clockwise vector
    val perpX = -dirY
    val perpY = dirX

    val laneHalfWidth = if (armCount == 6) 0.56f else 0.44f
    val (radius, sideSign) = when (offset) {
      in 0..4 -> (6.5f - offset * 0.82f) to 1f
      in 5..10 -> (3.22f + (offset - 5) * 0.72f) to -1f
      11 -> 6.95f to 0f
      else -> 6.95f to 1f
    }

    val wx = dirX * radius + perpX * (laneHalfWidth * sideSign)
    val wy = dirY * radius + perpY * (laneHalfWidth * sideSign)
    return wx to wy
  }

  fun radialHomeStretchWorldCoord(armIndex: Int, stepIndex: Int, armCount: Int): Pair<Float, Float> {
    val angle = radialArmAngleRad(armIndex, armCount)
    val radius = 5.9f - stepIndex.coerceIn(0, 4) * 0.85f
    return (cos(angle).toFloat() * radius) to (sin(angle).toFloat() * radius)
  }

  fun radialHomeGoalWorldCoord(armIndex: Int, slotIndex: Int, armCount: Int): Pair<Float, Float> {
    val angle = radialArmAngleRad(armIndex, armCount)
    val dirX = cos(angle).toFloat()
    val dirY = sin(angle).toFloat()
    val perpX = -dirY
    val perpY = dirX
    val spread = (slotIndex - 1.5f) * 0.22f
    val radius = 1.25f
    return (dirX * radius + perpX * spread) to (dirY * radius + perpY * spread)
  }

  fun radialYardCenterWorldCoord(armIndex: Int, armCount: Int): Pair<Float, Float> {
    // Yard sits in the sector between armIndex and (armIndex + 1)
    val midAngle = radialArmAngleRad(armIndex, armCount) + (PI / armCount.toDouble())
    val radius = if (armCount == 6) 5.55f else 5.85f
    return (cos(midAngle).toFloat() * radius) to (sin(midAngle).toFloat() * radius)
  }

  fun radialYardPedestalWorldCoord(armIndex: Int, slotIndex: Int, armCount: Int): Pair<Float, Float> {
    val (cx, cy) = radialYardCenterWorldCoord(armIndex, armCount)
    val spread = if (armCount == 6) 0.44f else 0.36f
    val dx = if (slotIndex % 2 == 0) -spread else spread
    val dy = if (slotIndex / 2 == 0) -spread else spread
    return (cx + dx) to (cy + dy)
  }

  fun tokenWorldXY(
    color: PlayerColor,
    slotIndex: Int,
    relativePos: Int,
    seats: List<PlayerSeat>,
    armCount: Int
  ): Pair<Float, Float> {
    val armIndex = LudoRulesEngine.armIndexForColor(color, seats, armCount)
    val maxMainRel = LudoRulesEngine.maxMainTrackRel(armCount)
    val goalPos = LudoRulesEngine.homeGoalPos(armCount)

    return if (armCount <= 4) {
      val (col, row) = when {
        relativePos < 0 -> yardPedestalGridCoordForArm(armIndex, slotIndex)
        relativePos in 0..maxMainRel -> {
          val globalIdx = (armIndex * 13 + relativePos) % 52
          MAIN_TRACK_COORDS[globalIdx]
        }
        relativePos in (maxMainRel + 1) until goalPos ->
          homeStretchGridCoordForArm(armIndex, relativePos - (maxMainRel + 1))
        else -> homeGoalGridCoordForArm(armIndex, slotIndex)
      }
      (col - 7.0f) to (row - 7.0f)
    } else {
      when {
        relativePos < 0 -> radialYardPedestalWorldCoord(armIndex, slotIndex, armCount)
        relativePos in 0..maxMainRel -> {
          val globalIdx = (armIndex * 13 + relativePos) % (armCount * 13)
          radialTrackWorldCoord(globalIdx, armCount)
        }
        relativePos in (maxMainRel + 1) until goalPos ->
          radialHomeStretchWorldCoord(armIndex, relativePos - (maxMainRel + 1), armCount)
        else -> radialHomeGoalWorldCoord(armIndex, slotIndex, armCount)
      }
    }
  }

  /**
   * Generates 3D world-space trail samples behind a moving pawn for the player-colored
   * glowing motion trail. Combines recently traversed step positions ([AnimatedTokenState.trailPathPositions])
   * with the active parabolic hop arc from [AnimatedTokenState.fromPos] to [AnimatedTokenState.toPos].
   */
  fun buildPawnTrailSamples3D(
    animatedToken: AnimatedTokenState,
    seats: List<PlayerSeat>,
    armCount: Int,
    subdivisionsPerSegment: Int = 8
  ): List<PawnTrailSample3D> {
    if (animatedToken.tokenIndex < 0) return emptyList()
    val color = animatedToken.tokenColor
    val slotIndex = animatedToken.tokenIndex % 4
    val goalPos = LudoRulesEngine.homeGoalPos(armCount)
    val clampedProgress = animatedToken.progress.coerceIn(0.05f, 1f)
    val wakeAlpha = animatedToken.fadingWakeAlpha.coerceIn(0f, 1f)
    if (wakeAlpha <= 0.01f) return emptyList()

    fun baseZForPos(pos: Int): Float = when {
      pos < 0 -> 0.30f
      pos >= goalPos -> 0.44f
      else -> 0.22f
    }

    val rawPoints = mutableListOf<Pair<Vec3, Vec3>>()

    // 1. Historical multi-step trail segments (up to last 4 visited positions)
    val history = animatedToken.trailPathPositions.takeLast(5)
    if (history.size >= 2) {
      for (segIdx in 0 until (history.size - 1)) {
        val segFrom = history[segIdx]
        val segTo = history[segIdx + 1]
        val (fx, fy) = tokenWorldXY(color, slotIndex, segFrom, seats, armCount)
        val (tx, ty) = tokenWorldXY(color, slotIndex, segTo, seats, armCount)
        val fz = baseZForPos(segFrom)
        val tz = baseZForPos(segTo)
        val steps = (subdivisionsPerSegment / 2).coerceAtLeast(3)
        for (s in 0 until steps) {
          val u = s.toFloat() / steps.toFloat()
          val wx = fx + (tx - fx) * u
          val wy = fy + (ty - fy) * u
          // Subtle lingering parabolic wake above historical tiles
          val hopArc = sin(u * PI).toFloat() * 0.38f
          val wz = fz + (tz - fz) * u + hopArc
          rawPoints.add(Vec3(wx, wy, wz) to Vec3(wx, wy, 0.21f))
        }
      }
    }

    // 2. Active step parabolic 3D hop from fromPos -> current interpolated head position
    val (fromX, fromY) = tokenWorldXY(color, slotIndex, animatedToken.fromPos, seats, armCount)
    val (toX, toY) = tokenWorldXY(color, slotIndex, animatedToken.toPos, seats, armCount)
    val fromZ = baseZForPos(animatedToken.fromPos)
    val toZ = baseZForPos(animatedToken.toPos)

    val activeSteps = subdivisionsPerSegment.coerceAtLeast(4)
    for (s in 0..activeSteps) {
      val frac = (s.toFloat() / activeSteps.toFloat()) * clampedProgress
      val wx = fromX + (toX - fromX) * frac
      val wy = fromY + (toY - fromY) * frac
      val hopHeight = sin(frac * PI).toFloat() * 1.35f
      val wz = fromZ + (toZ - fromZ) * frac + hopHeight
      rawPoints.add(Vec3(wx, wy, wz) to Vec3(wx, wy, 0.21f))
    }

    if (rawPoints.size < 2) return emptyList()

    val lastIdx = (rawPoints.size - 1).coerceAtLeast(1)
    return rawPoints.mapIndexed { idx, (worldPt, groundPt) ->
      val normalized = idx.toFloat() / lastIdx.toFloat()
      // Smooth quadratic ease-in from tail tip (0f) to pawn head (1f), scaled by fadingWakeAlpha
      val intensity = (normalized * normalized * wakeAlpha).coerceIn(0f, 1f)
      PawnTrailSample3D(
        worldPos = worldPt,
        groundPos = groundPt,
        intensity = intensity
      )
    }
  }
}

class Camera3DProjector(
  val yawDegrees: Float,
  val pitchDegrees: Float,
  val zoom: Float,
  val viewportWidth: Float,
  val viewportHeight: Float
) {
  private val yawRad = (yawDegrees * PI / 180.0).toFloat()
  private val pitchRad = (pitchDegrees * PI / 180.0).toFloat()
  private val cosYaw = cos(yawRad)
  private val sinYaw = sin(yawRad)
  private val cosPitch = cos(pitchRad)
  private val sinPitch = sin(pitchRad)

  private val baseScale = minOf(viewportWidth, viewportHeight) * 0.056f * zoom
  private val cameraDist = 28f
  private val centerX = viewportWidth * 0.5f
  private val centerY = viewportHeight * 0.51f

  fun project(world: Vec3): ProjectedPoint {
    val rx = world.x * cosYaw - world.y * sinYaw
    val ry = world.x * sinYaw + world.y * cosYaw
    val rz = world.z

    val camX = rx
    val camY = ry * sinPitch - rz * cosPitch
    val camZ = ry * cosPitch + rz * sinPitch

    val perspective = cameraDist / (cameraDist + camZ * 0.42f).coerceAtLeast(8f)
    val sx = centerX + camX * baseScale * perspective
    val sy = centerY + camY * baseScale * perspective
    return ProjectedPoint(
      screenX = sx,
      screenY = sy,
      depth = camZ,
      scale = baseScale * perspective
    )
  }

  fun gridToWorld(col: Float, row: Float, z: Float = 0f): Vec3 {
    return Vec3(col - 7.0f, row - 7.0f, z)
  }
}

@Composable
fun Interactive3DLudoBoard(
  tokens: List<Int>,
  seats: List<PlayerSeat>,
  armCount: Int,
  legalMoveTokenIndices: List<Int>,
  activeBlockades: Map<Int, Set<PlayerColor>>,
  animatedToken: AnimatedTokenState?,
  yawDegrees: Float,
  pitchDegrees: Float,
  zoom: Float,
  onCameraTransform: (deltaYaw: Float, deltaPitch: Float, zoomFactor: Float) -> Unit,
  onTokenTapped: (tokenIndex: Int) -> Unit,
  onBoardCenterTapped: () -> Unit,
  activePawnEmote: ActivePawnEmoteEffect? = null,
  tutorialOverlay: Tutorial3DBoardOverlay? = null,
  onTutorialBeaconTapped: ((Tutorial3DBeaconSpec) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "ludo3d_pulse")
  val pulsePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "halo_pulse"
  )

  var hitTargets by remember { mutableStateOf<List<ProjectedTokenHitTarget>>(emptyList()) }
  var centerGoalScreenPoint by remember { mutableStateOf(Offset.Zero) }
  var emoteScreenAnchor by remember { mutableStateOf<Offset?>(null) }
  var tutorialBeaconAnchors by remember { mutableStateOf<List<ProjectedTutorialBeaconAnchor>>(emptyList()) }

  // Real-time local environment clock ticker (updates automatically as local time passes)
  var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var lightingCycleMode by remember { mutableStateOf(DayNightCycleMode.REAL_TIME_LOCAL) }

  LaunchedEffect(lightingCycleMode) {
    if (lightingCycleMode == DayNightCycleMode.REAL_TIME_LOCAL) {
      while (true) {
        currentTimeMillis = System.currentTimeMillis()
        delay(15_000L)
      }
    }
  }

  val sceneLighting = remember(currentTimeMillis, lightingCycleMode) {
    val fixedHour = lightingCycleMode.fixedFractionalHour
    if (fixedHour != null) {
      DayNightLightingEngine.computeLightingForFractionalHour(fixedHour)
    } else {
      DayNightLightingEngine.computeLightingForRealTime(currentTimeMillis)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("interactive_3d_ludo_board")
      .pointerInput(legalMoveTokenIndices, tutorialOverlay) {
        detectTapGestures { tapOffset ->
          val legalTapped = hitTargets
            .filter { it.tokenIndex in legalMoveTokenIndices }
            .sortedBy { it.depth }
            .firstOrNull { target ->
              hypot(tapOffset.x - target.screenX, tapOffset.y - target.screenY) <= target.hitRadiusPx * 1.45f
            }

          if (legalTapped != null) {
            onTokenTapped(legalTapped.tokenIndex)
            return@detectTapGestures
          }

          val anyTapped = hitTargets
            .sortedBy { it.depth }
            .firstOrNull { target ->
              hypot(tapOffset.x - target.screenX, target.screenY - tapOffset.y) <= target.hitRadiusPx
            }
          if (anyTapped != null) {
            onTokenTapped(anyTapped.tokenIndex)
            return@detectTapGestures
          }

          val tappedBeacon = tutorialBeaconAnchors.firstOrNull { anchor ->
            hypot(tapOffset.x - anchor.screenX, tapOffset.y - anchor.screenY) <= (anchor.scale * 1.15f).coerceAtLeast(42f)
          }
          if (tappedBeacon != null) {
            onTutorialBeaconTapped?.invoke(tappedBeacon.spec)
            return@detectTapGestures
          }

          if (hypot(tapOffset.x - centerGoalScreenPoint.x, tapOffset.y - centerGoalScreenPoint.y) <= 68f) {
            onBoardCenterTapped()
          }
        }
      }
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, gestureZoom, _ ->
          val dYaw = pan.x * 0.28f
          val dPitch = -pan.y * 0.20f
          onCameraTransform(dYaw, dPitch, gestureZoom)
        }
      }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val projector = Camera3DProjector(
        yawDegrees = yawDegrees,
        pitchDegrees = pitchDegrees,
        zoom = zoom,
        viewportWidth = size.width,
        viewportHeight = size.height
      )

      val armColors = Array(armCount) { armIdx ->
        seats.firstOrNull { LudoRulesEngine.armIndexForColor(it.color, seats, armCount) == armIdx }?.color
          ?: PlayerColor.entries[armIdx % PlayerColor.entries.size]
      }

      // 1. Directional horizon sky-glow behind the 3D board synced to local solar/lunar position
      drawAmbientSkyHorizonBackdrop3D(
        projector = projector,
        lighting = sceneLighting
      )

      if (armCount <= 4) {
        drawBoardPlinth3D(projector, sceneLighting)
        drawCornerYard3D(projector, armColors[0], armIndex = 0, startCol = 0f, startRow = 0f, lighting = sceneLighting)
        drawCornerYard3D(projector, armColors[1], armIndex = 1, startCol = 9f, startRow = 0f, lighting = sceneLighting)
        drawCornerYard3D(projector, armColors[2], armIndex = 2, startCol = 9f, startRow = 9f, lighting = sceneLighting)
        drawCornerYard3D(projector, armColors[3], armIndex = 3, startCol = 0f, startRow = 9f, lighting = sceneLighting)
        drawTrackTiles4Arm3D(projector, armColors, activeBlockades, sceneLighting)
        val centerProjected = projector.project(projector.gridToWorld(7f, 7f, 0.45f))
        centerGoalScreenPoint = Offset(centerProjected.screenX, centerProjected.screenY)
        drawCenterGoalPyramid4Arm3D(projector, armColors, sceneLighting)
      } else {
        drawPolygonalBoard3D(projector, armCount, armColors, activeBlockades, sceneLighting)
        val centerProjected = projector.project(Vec3(0f, 0f, 0.55f))
        centerGoalScreenPoint = Offset(centerProjected.screenX, centerProjected.screenY)
      }

      // 2. Subtle nocturnal/dusk lantern illumination & directional sun/moon light glaze over the board
      drawBoardAtmosphericLightingOverlay3D(
        projector = projector,
        armCount = armCount,
        lighting = sceneLighting,
        pulsePhase = pulsePhase
      )

      val tokenWorldList = computeTokenWorldPositions(
        tokens = tokens,
        seats = seats,
        armCount = armCount,
        animatedToken = animatedToken,
        projector = projector
      )

      val sortedTokens = tokenWorldList.sortedByDescending { it.projected.depth }
      val newHitTargets = mutableListOf<ProjectedTokenHitTarget>()

      // Subtle 3D glowing movement trail behind the actively moving pawn, tinted in its PlayerColor
      if (animatedToken != null && animatedToken.tokenIndex >= 0) {
        drawPawnMovementGlowTrail3D(
          projector = projector,
          animatedToken = animatedToken,
          seats = seats,
          armCount = armCount,
          pulsePhase = pulsePhase
        )
      }

      for (item in sortedTokens) {
        val isLegal = item.tokenIndex in legalMoveTokenIndices ||
          (tutorialOverlay != null && item.tokenIndex == tutorialOverlay.activeTargetTokenIndex)
        val isMovingPawn = animatedToken != null &&
          animatedToken.tokenIndex == item.tokenIndex &&
          animatedToken.fadingWakeAlpha > 0.1f
        drawSculptedPawn3D(
          projector = projector,
          worldPos = item.worldPos,
          color = item.color,
          isLegalMove = isLegal,
          pulsePhase = pulsePhase,
          tokenNumber = (item.tokenIndex % 4) + 1,
          isMoving = isMovingPawn,
          lighting = sceneLighting
        )
        newHitTargets.add(
          ProjectedTokenHitTarget(
            tokenIndex = item.tokenIndex,
            screenX = item.projected.screenX,
            screenY = item.projected.screenY - item.projected.scale * 0.42f,
            hitRadiusPx = (item.projected.scale * 0.68f).coerceAtLeast(32f),
            depth = item.projected.depth
          )
        )
      }
      hitTargets = newHitTargets

      // Highlighted 3D Tutorial Overlays (Trajectory Arc + Volumetric Beacons)
      if (tutorialOverlay != null) {
        if (
          tutorialOverlay.showTrajectoryArc &&
          tutorialOverlay.trajectoryFromWorld != null &&
          tutorialOverlay.trajectoryToWorld != null
        ) {
          drawTutorial3DTrajectoryArc(
            projector = projector,
            fromWorld = tutorialOverlay.trajectoryFromWorld,
            toWorld = tutorialOverlay.trajectoryToWorld,
            phase = pulsePhase
          )
        }

        val newBeaconAnchors = mutableListOf<ProjectedTutorialBeaconAnchor>()
        for (beacon in tutorialOverlay.beacons) {
          val topAnchor = drawTutorial3DBeacon(
            projector = projector,
            beacon = beacon,
            phase = pulsePhase
          )
          newBeaconAnchors.add(
            ProjectedTutorialBeaconAnchor(
              spec = beacon,
              screenX = topAnchor.screenX,
              screenY = topAnchor.screenY,
              scale = topAnchor.scale
            )
          )
        }
        tutorialBeaconAnchors = newBeaconAnchors
      } else {
        tutorialBeaconAnchors = emptyList()
      }

      if (activePawnEmote != null) {
        val targetEntry = sortedTokens.firstOrNull { it.tokenIndex == activePawnEmote.tokenIndex }
          ?: sortedTokens.firstOrNull { it.color == activePawnEmote.pawnColor && tokens.getOrElse(it.tokenIndex) { -1 } >= 0 }
          ?: sortedTokens.firstOrNull { it.color == activePawnEmote.pawnColor }

        if (targetEntry != null) {
          val badgeTop = drawPawnEmote3DAbovePawn(
            projector = projector,
            pawnWorldPos = targetEntry.worldPos,
            pawnColor = activePawnEmote.pawnColor,
            emote = activePawnEmote.emote,
            phase = pulsePhase
          )
          emoteScreenAnchor = Offset(badgeTop.screenX, badgeTop.screenY)
        } else {
          emoteScreenAnchor = null
        }
      } else {
        emoteScreenAnchor = null
      }
    }

    // Floating 3D Callout Overlay Pills for Tutorial Beacons
    if (tutorialOverlay != null && tutorialBeaconAnchors.isNotEmpty()) {
      val density = LocalDensity.current
      val calloutWidthPx = with(density) { 144.dp.toPx() }
      val calloutHeightPx = with(density) { 44.dp.toPx() }

      tutorialBeaconAnchors.forEach { anchorItem ->
        val (primaryTone, _, _) = paletteForColor(anchorItem.spec.accentColor)
        val isSpotlight = anchorItem.spec.zoneType == TutorialHighlightZoneType.PAWN_SPOTLIGHT ||
          anchorItem.spec.zoneType == TutorialHighlightZoneType.CAPTURE_TARGET

        Box(
          modifier = Modifier
            .offset {
              IntOffset(
                x = (anchorItem.screenX - calloutWidthPx * 0.5f).toInt().coerceAtLeast(4),
                y = (anchorItem.screenY - calloutHeightPx - 8f).toInt().coerceAtLeast(4)
              )
            }
            .clip(RoundedCornerShape(14.dp))
            .background(Obsidianink.copy(alpha = 0.92f))
            .border(
              width = if (isSpotlight) 2.dp else 1.2.dp,
              color = if (isSpotlight) BrassHighlight else BrushedBrass.copy(alpha = 0.85f),
              shape = RoundedCornerShape(14.dp)
            )
            .clickable {
              onTutorialBeaconTapped?.invoke(anchorItem.spec)
            }
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag("tutorial_3d_callout_${anchorItem.spec.id}")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = anchorItem.spec.badgeIcon,
              style = MaterialTheme.typography.titleSmall
            )
            Column {
              Text(
                text = anchorItem.spec.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = BrassHighlight
              )
              Text(
                text = anchorItem.spec.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = primaryTone
              )
            }
          }
        }
      }
    }

    val anchor = emoteScreenAnchor
    if (activePawnEmote != null && anchor != null) {
      val density = LocalDensity.current
      val pillWidthPx = with(density) { 148.dp.toPx() }
      val pillHeightPx = with(density) { 46.dp.toPx() }
      val (primaryTone, _, _) = paletteForColor(activePawnEmote.pawnColor)

      Box(
        modifier = Modifier
          .offset {
            IntOffset(
              x = (anchor.x - pillWidthPx * 0.5f).toInt(),
              y = (anchor.y - pillHeightPx - 6f).toInt().coerceAtLeast(4)
            )
          }
          .clip(RoundedCornerShape(50))
          .background(Obsidianink.copy(alpha = 0.90f))
          .border(1.5.dp, BrushedBrass, RoundedCornerShape(50))
          .padding(horizontal = 10.dp, vertical = 4.dp)
          .testTag("active_pawn_emote_3d_badge")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = activePawnEmote.emote.stickerSymbol,
            style = MaterialTheme.typography.titleMedium
          )
          Column {
            Text(
              text = activePawnEmote.emote.calloutText,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = BrassHighlight
            )
            Text(
              text = "${activePawnEmote.actorName} • ${activePawnEmote.triggerMoment.shortTag}",
              style = MaterialTheme.typography.labelSmall,
              color = primaryTone
            )
          }
        }
      }
    }

    // Subtle Real-Time Day/Night Lighting Sync Indicator & Cycle Pill in Bottom-Start of 3D Viewport
    Box(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(8.dp)
        .clip(RoundedCornerShape(50))
        .background(Obsidianink.copy(alpha = 0.76f))
        .border(
          width = 1.dp,
          color = if (lightingCycleMode == DayNightCycleMode.REAL_TIME_LOCAL) {
            BrushedBrass.copy(alpha = 0.65f)
          } else {
            BrassHighlight
          },
          shape = RoundedCornerShape(50)
        )
        .clickable { lightingCycleMode = lightingCycleMode.nextMode() }
        .padding(horizontal = 10.dp, vertical = 4.dp)
        .testTag("day_night_lighting_badge")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        Text(
          text = sceneLighting.phase.badgeSymbol,
          style = MaterialTheme.typography.labelSmall
        )
        Text(
          text = if (lightingCycleMode == DayNightCycleMode.REAL_TIME_LOCAL) {
            "${sceneLighting.phase.label} • ${sceneLighting.formattedLocalTime}"
          } else {
            "${sceneLighting.phase.label} (${lightingCycleMode.shortLabel})"
          },
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = AlabasterLinen
        )
      }
    }
  }
}

private data class TokenRenderEntry(
  val tokenIndex: Int,
  val color: PlayerColor,
  val worldPos: Vec3,
  val projected: ProjectedPoint
)

private fun computeTokenWorldPositions(
  tokens: List<Int>,
  seats: List<PlayerSeat>,
  armCount: Int,
  animatedToken: AnimatedTokenState?,
  projector: Camera3DProjector
): List<TokenRenderEntry> {
  val activeColorSet = if (seats.isNotEmpty()) {
    seats.map { it.color }.toSet()
  } else {
    PlayerColor.entries.take(armCount).toSet()
  }

  val activeTokenIndices = PlayerColor.entries
    .filter { it in activeColorSet }
    .flatMap { LudoRulesEngine.tokenIndicesForColor(it) }
    .filter { it < tokens.size }

  val rawWorldXY = mutableMapOf<Int, Pair<Float, Float>>()
  for (idx in activeTokenIndices) {
    val color = LudoRulesEngine.colorForTokenIndex(idx)
    val slotIdx = idx % 4
    val pos = tokens.getOrElse(idx) { -1 }
    rawWorldXY[idx] = LudoBoardCoordinates.tokenWorldXY(color, slotIdx, pos, seats, armCount)
  }

  val coordGroups = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()
  for (idx in activeTokenIndices) {
    val xy = rawWorldXY[idx] ?: continue
    val key = (xy.first * 10).toInt() to (xy.second * 10).toInt()
    coordGroups.getOrPut(key) { mutableListOf() }.add(idx)
  }

  val entries = mutableListOf<TokenRenderEntry>()
  for (idx in activeTokenIndices) {
    val color = LudoRulesEngine.colorForTokenIndex(idx)
    val slotIdx = idx % 4

    val world = if (animatedToken != null && animatedToken.tokenIndex == idx) {
      val fromXY = LudoBoardCoordinates.tokenWorldXY(color, slotIdx, animatedToken.fromPos, seats, armCount)
      val toXY = LudoBoardCoordinates.tokenWorldXY(color, slotIdx, animatedToken.toPos, seats, armCount)
      val t = animatedToken.progress.coerceIn(0f, 1f)
      val interpX = fromXY.first + (toXY.first - fromXY.first) * t
      val interpY = fromXY.second + (toXY.second - fromXY.second) * t
      val hopZ = 0.22f + sin(t * PI).toFloat() * 1.35f
      Vec3(interpX, interpY, hopZ)
    } else {
      val baseXY = rawWorldXY[idx] ?: (0f to 0f)
      val key = (baseXY.first * 10).toInt() to (baseXY.second * 10).toInt()
      val group = coordGroups[key].orEmpty()
      val (offsetX, offsetY) = if (group.size > 1) {
        val rank = group.indexOf(idx)
        val angle = (rank.toDouble() / group.size.toDouble()) * 2.0 * PI + PI / 4.0
        val radius = 0.21f
        (cos(angle).toFloat() * radius) to (sin(angle).toFloat() * radius)
      } else {
        0f to 0f
      }
      Vec3(baseXY.first + offsetX, baseXY.second + offsetY, 0.22f)
    }

    entries.add(
      TokenRenderEntry(
        tokenIndex = idx,
        color = color,
        worldPos = world,
        projected = projector.project(world)
      )
    )
  }
  return entries
}

private fun DrawScope.drawAmbientSkyHorizonBackdrop3D(
  projector: Camera3DProjector,
  lighting: SceneLightingState3D
) {
  // Project a subtle atmospheric light pool originating from the sun/moon azimuth direction
  val lightSourceWorld = Vec3(
    x = -lighting.shadowDirX * 16f,
    y = -lighting.shadowDirY * 16f,
    z = 1.2f
  )
  val sourceProj = projector.project(lightSourceWorld)
  val boardCenterProj = projector.project(Vec3(0f, 0f, 0f))
  val radiusPx = (minOf(size.width, size.height) * 0.68f).coerceAtLeast(120f)

  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        lighting.skyHorizonGlowColor,
        lighting.ambientWashColor,
        Color.Transparent
      ),
      center = Offset(sourceProj.screenX, sourceProj.screenY),
      radius = radiusPx
    ),
    radius = radiusPx,
    center = Offset(boardCenterProj.screenX, boardCenterProj.screenY)
  )
}

private fun DrawScope.drawBoardAtmosphericLightingOverlay3D(
  projector: Camera3DProjector,
  armCount: Int,
  lighting: SceneLightingState3D,
  pulsePhase: Float
) {
  // 1. Subtle ambient tone wash across the board top surface
  if (lighting.ambientWashColor.alpha > 0.01f) {
    val boardExtent = if (armCount <= 4) 7.65f else 7.4f
    val washCorners = listOf(
      projector.project(Vec3(-boardExtent, -boardExtent, 0.20f)),
      projector.project(Vec3(boardExtent, -boardExtent, 0.20f)),
      projector.project(Vec3(boardExtent, boardExtent, 0.20f)),
      projector.project(Vec3(-boardExtent, boardExtent, 0.20f))
    )
    drawPolygon(washCorners, lighting.ambientWashColor)
  }

  // 2. Warm nocturnal / twilight architectural lantern glow pools when dusk or night sets in
  val lantern = lighting.lanternGlowIntensity
  if (lantern > 0.04f) {
    val flicker = 0.94f + 0.06f * sin(pulsePhase * 1.5f)
    val lanternCore = BrassHighlight.copy(alpha = (0.22f * lantern * flicker).coerceIn(0f, 0.28f))
    val lanternWarm = CoralAmberSoft.copy(alpha = (0.14f * lantern * flicker).coerceIn(0f, 0.20f))

    // Central goal pyramid lantern beacon
    val centerPt = projector.project(Vec3(0f, 0f, 0.32f))
    drawCircle(
      color = lanternWarm,
      radius = centerPt.scale * 2.15f,
      center = Offset(centerPt.screenX, centerPt.screenY)
    )
    drawCircle(
      color = lanternCore,
      radius = centerPt.scale * 1.15f,
      center = Offset(centerPt.screenX, centerPt.screenY)
    )

    // Corner yard sanctuary lanterns
    if (armCount <= 4) {
      val yardCenters = listOf(
        projector.gridToWorld(2.5f, 2.5f, 0.24f),
        projector.gridToWorld(11.5f, 2.5f, 0.24f),
        projector.gridToWorld(11.5f, 11.5f, 0.24f),
        projector.gridToWorld(2.5f, 11.5f, 0.24f)
      )
      for (yc in yardCenters) {
        val yp = projector.project(yc)
        drawCircle(
          color = lanternWarm,
          radius = yp.scale * 1.85f,
          center = Offset(yp.screenX, yp.screenY)
        )
        drawCircle(
          color = lanternCore,
          radius = yp.scale * 0.92f,
          center = Offset(yp.screenX, yp.screenY)
        )
      }
    } else {
      for (arm in 0 until armCount) {
        val (ycx, ycy) = LudoBoardCoordinates.radialYardCenterWorldCoord(arm, armCount)
        val yp = projector.project(Vec3(ycx, ycy, 0.24f))
        drawCircle(
          color = lanternWarm,
          radius = yp.scale * 1.45f,
          center = Offset(yp.screenX, yp.screenY)
        )
      }
    }
  }
}

private fun DrawScope.drawPolygonalBoard3D(
  projector: Camera3DProjector,
  armCount: Int,
  armColors: Array<PlayerColor>,
  activeBlockades: Map<Int, Set<PlayerColor>>,
  lighting: SceneLightingState3D
) {
  val outerRadius = 7.95f
  val sides = armCount * 2
  val shadowShiftX = lighting.shadowDirX * 1.35f * lighting.shadowStretchFactor
  val shadowShiftY = lighting.shadowDirY * 1.35f * lighting.shadowStretchFactor
  val shadowPts = (0 until sides).map { i ->
    val a = (i.toDouble() / sides.toDouble()) * 2.0 * PI
    projector.project(
      Vec3(
        x = (cos(a) * (outerRadius + 0.45f)).toFloat() + shadowShiftX,
        y = (sin(a) * (outerRadius + 0.45f)).toFloat() + shadowShiftY,
        z = -0.7f
      )
    )
  }
  drawPolygon(shadowPts, lighting.shadowColor)

  val bottomPts = (0 until sides).map { i ->
    val a = (i.toDouble() / sides.toDouble()) * 2.0 * PI
    projector.project(Vec3((cos(a) * outerRadius).toFloat(), (sin(a) * outerRadius).toFloat(), -0.52f))
  }
  val topPts = (0 until sides).map { i ->
    val a = (i.toDouble() / sides.toDouble()) * 2.0 * PI
    projector.project(Vec3((cos(a) * outerRadius).toFloat(), (sin(a) * outerRadius).toFloat(), 0.0f))
  }

  for (i in 0 until sides) {
    val next = (i + 1) % sides
    val wall = listOf(bottomPts[i], bottomPts[next], topPts[next], topPts[i])
    drawPolygon(wall, if (i % 2 == 0) lighting.plinthSideColorA else lighting.plinthSideColorB)
  }
  drawPolygon(topPts, lighting.plinthTopColor)
  drawPolygonStroke(topPts, StoneBorderLight, 2.2f)

  // Draw radial yards for each arm
  for (arm in 0 until armCount) {
    val color = armColors[arm]
    val (primary, darkTone, softTone) = paletteForColor(color)
    val (ycx, ycy) = LudoBoardCoordinates.radialYardCenterWorldCoord(arm, armCount)
    val yardSize = if (armCount == 6) 1.05f else 0.86f

    drawExtrudedBox3D(
      projector = projector,
      minX = ycx - yardSize,
      maxX = ycx + yardSize,
      minY = ycy - yardSize,
      maxY = ycy + yardSize,
      bottomZ = 0.0f,
      topZ = 0.18f,
      topColor = softTone,
      sideColorA = primary,
      sideColorB = darkTone,
      strokeColor = primary
    )

    for (slot in 0 until 4) {
      val (px, py) = LudoBoardCoordinates.radialYardPedestalWorldCoord(arm, slot, armCount)
      val pProj = projector.project(Vec3(px, py, 0.22f))
      val r = pProj.scale * (if (armCount == 6) 0.28f else 0.24f)
      drawCircle(color = lighting.plinthTopColor, radius = r, center = Offset(pProj.screenX, pProj.screenY))
      drawCircle(
        color = primary,
        radius = r * 0.78f,
        center = Offset(pProj.screenX, pProj.screenY),
        style = Stroke(width = 2f)
      )
    }
  }

  // Draw radial outer track tiles
  val totalOuter = armCount * 13
  val starIndices = LudoRulesEngine.starGlobalIndices(armCount)
  val tileHalf = if (armCount == 6) 0.34f else 0.28f

  for (globalIdx in 0 until totalOuter) {
    val (wx, wy) = LudoBoardCoordinates.radialTrackWorldCoord(globalIdx, armCount)
    val isStart = globalIdx % 13 == 0
    val startArm = globalIdx / 13
    val isStar = globalIdx in starIndices

    val tileColor = when {
      isStart -> paletteForColor(armColors[startArm % armCount]).first
      isStar -> lerp(Color(0xFFF3E6C8), BrassHighlight, lighting.lanternGlowIntensity * 0.22f)
      else -> lerp(TravertineStone, lighting.plinthTopColor, 0.35f)
    }
    val borderColor = when {
      isStart -> paletteForColor(armColors[startArm % armCount]).second
      isStar -> BrushedBrass
      else -> StoneBorderLight
    }

    drawFlatTile3D(
      projector = projector,
      minX = wx - tileHalf,
      maxX = wx + tileHalf,
      minY = wy - tileHalf,
      maxY = wy + tileHalf,
      z = 0.12f,
      fillColor = tileColor,
      strokeColor = borderColor
    )

    if (isStart || isStar) {
      val center = projector.project(Vec3(wx, wy, 0.14f))
      drawStarInlay(
        center = Offset(center.screenX, center.screenY),
        radius = center.scale * 0.20f,
        color = if (isStart) AlabasterLinen else BrushedBrass
      )
    }

    val blockingColors = activeBlockades[globalIdx]
    if (!blockingColors.isNullOrEmpty()) {
      val center = projector.project(Vec3(wx, wy, 0.18f))
      drawCircle(
        color = BrushedBrass,
        radius = center.scale * 0.36f,
        center = Offset(center.screenX, center.screenY),
        style = Stroke(width = 2.5f)
      )
    }
  }

  // Draw radial home stretch tiles for each arm
  for (arm in 0 until armCount) {
    val color = armColors[arm]
    val (primary, _, softTone) = paletteForColor(color)
    for (step in 0 until 5) {
      val (hx, hy) = LudoBoardCoordinates.radialHomeStretchWorldCoord(arm, step, armCount)
      drawFlatTile3D(
        projector = projector,
        minX = hx - tileHalf * 0.9f,
        maxX = hx + tileHalf * 0.9f,
        minY = hy - tileHalf * 0.9f,
        maxY = hy + tileHalf * 0.9f,
        z = 0.15f,
        fillColor = softTone,
        strokeColor = primary
      )
    }
  }

  // Draw Center 3D N-Gon Pyramid Goal with directional apex shadow
  val apexShadow = projector.project(
    Vec3(
      x = lighting.shadowDirX * 1.1f * lighting.shadowStretchFactor,
      y = lighting.shadowDirY * 1.1f * lighting.shadowStretchFactor,
      z = 0.16f
    )
  )
  val apex = projector.project(Vec3(0f, 0f, 0.68f))
  val goalRadius = 1.85f
  val ringPts = (0 until armCount).map { arm ->
    val a = LudoBoardCoordinates.radialArmAngleRad(arm, armCount) - (PI / armCount)
    projector.project(Vec3((cos(a) * goalRadius).toFloat(), (sin(a) * goalRadius).toFloat(), 0.16f))
  }
  drawCircle(
    color = lighting.shadowColor.copy(alpha = (lighting.shadowColor.alpha * 0.55f).coerceIn(0f, 0.32f)),
    radius = apexShadow.scale * 0.85f,
    center = Offset(apexShadow.screenX, apexShadow.screenY)
  )
  for (arm in 0 until armCount) {
    val next = (arm + 1) % armCount
    val (primary, _, _) = paletteForColor(armColors[arm])
    drawPolygon(listOf(ringPts[arm], ringPts[next], apex), primary)
    drawLine(
      color = BrushedBrass,
      start = Offset(ringPts[arm].screenX, ringPts[arm].screenY),
      end = Offset(apex.screenX, apex.screenY),
      strokeWidth = 2.2f
    )
  }
  drawCircle(
    color = lerp(BrassHighlight, lighting.specularHighlightColor, 0.35f),
    radius = apex.scale * 0.20f,
    center = Offset(apex.screenX, apex.screenY)
  )
}

private fun DrawScope.drawBoardPlinth3D(
  projector: Camera3DProjector,
  lighting: SceneLightingState3D
) {
  val shiftX = lighting.shadowDirX * 1.45f * lighting.shadowStretchFactor
  val shiftY = lighting.shadowDirY * 1.45f * lighting.shadowStretchFactor
  val shadowCorners = listOf(
    projector.project(Vec3(-8.2f + shiftX, -8.2f + shiftY, -0.75f)),
    projector.project(Vec3(8.2f + shiftX, -8.2f + shiftY, -0.75f)),
    projector.project(Vec3(8.2f + shiftX, 8.2f + shiftY, -0.75f)),
    projector.project(Vec3(-8.2f + shiftX, 8.2f + shiftY, -0.75f))
  )
  drawPolygon(shadowCorners, lighting.shadowColor)

  drawExtrudedBox3D(
    projector = projector,
    minX = -7.75f,
    maxX = 7.75f,
    minY = -7.75f,
    maxY = 7.75f,
    bottomZ = -0.55f,
    topZ = 0.0f,
    topColor = lighting.plinthTopColor,
    sideColorA = lighting.plinthSideColorA,
    sideColorB = lighting.plinthSideColorB,
    strokeColor = StoneBorderLight
  )
}

private fun DrawScope.drawCornerYard3D(
  projector: Camera3DProjector,
  color: PlayerColor,
  armIndex: Int,
  startCol: Float,
  startRow: Float,
  lighting: SceneLightingState3D
) {
  val (primary, darkTone, softTone) = paletteForColor(color)
  val minX = startCol - 7.5f
  val maxX = startCol + 6f - 7.5f
  val minY = startRow - 7.5f
  val maxY = startRow + 6f - 7.5f

  // Subtle directional shadow cast by the raised 3D yard onto the board plinth
  val yardShadowDx = lighting.shadowDirX * 0.42f * lighting.shadowStretchFactor
  val yardShadowDy = lighting.shadowDirY * 0.42f * lighting.shadowStretchFactor
  val yardShadowCorners = listOf(
    projector.project(Vec3(minX + 0.08f + yardShadowDx, minY + 0.08f + yardShadowDy, 0.02f)),
    projector.project(Vec3(maxX - 0.08f + yardShadowDx, minY + 0.08f + yardShadowDy, 0.02f)),
    projector.project(Vec3(maxX - 0.08f + yardShadowDx, maxY - 0.08f + yardShadowDy, 0.02f)),
    projector.project(Vec3(minX + 0.08f + yardShadowDx, maxY - 0.08f + yardShadowDy, 0.02f))
  )
  drawPolygon(
    yardShadowCorners,
    lighting.shadowColor.copy(alpha = (lighting.shadowColor.alpha * 0.65f).coerceIn(0f, 0.30f))
  )

  drawExtrudedBox3D(
    projector = projector,
    minX = minX + 0.08f,
    maxX = maxX - 0.08f,
    minY = minY + 0.08f,
    maxY = maxY - 0.08f,
    bottomZ = 0.0f,
    topZ = 0.18f,
    topColor = lerp(primary, lighting.ambientWashColor, 0.06f),
    sideColorA = darkTone,
    sideColorB = darkTone.copy(alpha = 0.88f),
    strokeColor = darkTone
  )

  drawExtrudedBox3D(
    projector = projector,
    minX = minX + 0.85f,
    maxX = maxX - 0.85f,
    minY = minY + 0.85f,
    maxY = maxY - 0.85f,
    bottomZ = 0.18f,
    topZ = 0.22f,
    topColor = lerp(softTone, lighting.plinthTopColor, 0.18f),
    sideColorA = WarmSandCard,
    sideColorB = StoneBorderLight,
    strokeColor = darkTone.copy(alpha = 0.35f)
  )

  for (slot in 0 until 4) {
    val (pCol, pRow) = LudoBoardCoordinates.yardPedestalGridCoordForArm(armIndex, slot)
    val center = projector.project(projector.gridToWorld(pCol, pRow, 0.24f))
    val r = center.scale * 0.42f
    drawCircle(
      color = darkTone.copy(alpha = 0.28f),
      radius = r * 1.08f,
      center = Offset(
        center.screenX + r * lighting.shadowDirX * 0.45f,
        center.screenY + r * (0.10f + lighting.shadowDirY * 0.45f)
      )
    )
    drawCircle(
      color = lighting.plinthTopColor,
      radius = r,
      center = Offset(center.screenX, center.screenY)
    )
    drawCircle(
      color = primary,
      radius = r * 0.72f,
      center = Offset(center.screenX, center.screenY),
      style = Stroke(width = (r * 0.16f).coerceAtLeast(2f))
    )
  }
}

private fun DrawScope.drawTrackTiles4Arm3D(
  projector: Camera3DProjector,
  armColors: Array<PlayerColor>,
  activeBlockades: Map<Int, Set<PlayerColor>>,
  lighting: SceneLightingState3D
) {
  LudoBoardCoordinates.MAIN_TRACK_COORDS.forEachIndexed { globalIdx, (col, row) ->
    val isStart = globalIdx % 13 == 0
    val startOwner = if (isStart) armColors[(globalIdx / 13) % 4] else null
    val isStar = globalIdx in LudoRulesEngine.STAR_GLOBAL_INDICES

    val tileColor = when {
      startOwner != null -> paletteForColor(startOwner).first
      isStar -> lerp(Color(0xFFF3E6C8), BrassHighlight, lighting.lanternGlowIntensity * 0.24f)
      else -> lerp(TravertineStone, lighting.plinthTopColor, 0.38f)
    }
    val borderColor = when {
      startOwner != null -> paletteForColor(startOwner).second
      isStar -> BrushedBrass
      else -> StoneBorderLight
    }

    val minX = col - 7.5f + 0.04f
    val maxX = col - 6.5f - 0.04f
    val minY = row - 7.5f + 0.04f
    val maxY = row - 6.5f - 0.04f

    drawFlatTile3D(
      projector = projector,
      minX = minX,
      maxX = maxX,
      minY = minY,
      maxY = maxY,
      z = 0.12f,
      fillColor = tileColor,
      strokeColor = borderColor
    )

    if (isStar || startOwner != null) {
      val center = projector.project(projector.gridToWorld(col, row, 0.14f))
      val starColor = if (startOwner != null) AlabasterLinen else BrushedBrass
      drawStarInlay(
        center = Offset(center.screenX, center.screenY),
        radius = center.scale * 0.28f,
        color = starColor
      )
    }

    val blockingColors = activeBlockades[globalIdx]
    if (!blockingColors.isNullOrEmpty()) {
      val center = projector.project(projector.gridToWorld(col, row, 0.18f))
      val shieldColor = paletteForColor(blockingColors.first()).first
      drawCircle(
        color = BrushedBrass,
        radius = center.scale * 0.46f,
        center = Offset(center.screenX, center.screenY),
        style = Stroke(width = (center.scale * 0.09f).coerceAtLeast(2.5f))
      )
      drawCircle(
        color = shieldColor.copy(alpha = 0.25f),
        radius = center.scale * 0.44f,
        center = Offset(center.screenX, center.screenY)
      )
    }
  }

  for (arm in 0 until 4) {
    val color = armColors[arm]
    val (primary, _, softTone) = paletteForColor(color)
    for (step in 0 until 5) {
      val (col, row) = LudoBoardCoordinates.homeStretchGridCoordForArm(arm, step)
      drawFlatTile3D(
        projector = projector,
        minX = col - 7.5f + 0.05f,
        maxX = col - 6.5f - 0.05f,
        minY = row - 7.5f + 0.05f,
        maxY = row - 6.5f - 0.05f,
        z = 0.15f,
        fillColor = lerp(softTone, lighting.plinthTopColor, 0.14f),
        strokeColor = primary
      )
      val dotCenter = projector.project(projector.gridToWorld(col, row, 0.16f))
      drawCircle(
        color = primary,
        radius = dotCenter.scale * 0.16f,
        center = Offset(dotCenter.screenX, dotCenter.screenY)
      )
    }
  }
}

private fun DrawScope.drawCenterGoalPyramid4Arm3D(
  projector: Camera3DProjector,
  armColors: Array<PlayerColor>,
  lighting: SceneLightingState3D
) {
  // Subtle directional shadow cast by the 3D central pyramid apex
  val apexShadow = projector.project(
    Vec3(
      x = lighting.shadowDirX * 1.15f * lighting.shadowStretchFactor,
      y = lighting.shadowDirY * 1.15f * lighting.shadowStretchFactor,
      z = 0.16f
    )
  )
  val apex = projector.project(Vec3(0f, 0f, 0.65f))
  val nw = projector.project(Vec3(-1.5f, -1.5f, 0.16f))
  val ne = projector.project(Vec3(1.5f, -1.5f, 0.16f))
  val se = projector.project(Vec3(1.5f, 1.5f, 0.16f))
  val sw = projector.project(Vec3(-1.5f, 1.5f, 0.16f))

  drawCircle(
    color = lighting.shadowColor.copy(alpha = (lighting.shadowColor.alpha * 0.55f).coerceIn(0f, 0.30f)),
    radius = apexShadow.scale * 0.78f,
    center = Offset(apexShadow.screenX, apexShadow.screenY)
  )

  drawPolygon(listOf(nw, sw, apex), paletteForColor(armColors[0]).first)
  drawPolygon(listOf(nw, ne, apex), paletteForColor(armColors[1]).first)
  drawPolygon(listOf(ne, se, apex), paletteForColor(armColors[2]).first)
  drawPolygon(listOf(sw, se, apex), paletteForColor(armColors[3]).first)

  val ridgeColor = BrushedBrass
  listOf(nw, ne, se, sw).forEach { corner ->
    drawLine(
      color = ridgeColor,
      start = Offset(corner.screenX, corner.screenY),
      end = Offset(apex.screenX, apex.screenY),
      strokeWidth = 2.5f
    )
  }
  drawCircle(
    color = lerp(BrassHighlight, lighting.specularHighlightColor, 0.35f),
    radius = apex.scale * 0.18f,
    center = Offset(apex.screenX, apex.screenY)
  )
  drawCircle(
    color = Obsidianink,
    radius = apex.scale * 0.18f,
    center = Offset(apex.screenX, apex.screenY),
    style = Stroke(width = 2f)
  )
}

private fun DrawScope.drawPawnMovementGlowTrail3D(
  projector: Camera3DProjector,
  animatedToken: AnimatedTokenState,
  seats: List<PlayerSeat>,
  armCount: Int,
  pulsePhase: Float
) {
  val samples = LudoBoardCoordinates.buildPawnTrailSamples3D(
    animatedToken = animatedToken,
    seats = seats,
    armCount = armCount
  )
  if (samples.size < 2) return

  val (primary, darkTone, softTone) = paletteForColor(animatedToken.tokenColor)
  val wakeAlpha = animatedToken.fadingWakeAlpha.coerceIn(0f, 1f)

  val projectedArc = samples.map { projector.project(it.worldPos) }
  val projectedGround = samples.map { projector.project(it.groundPos) }

  // 1. Subtle departure ripple ring on the origin tile surface
  val originGround = projectedGround.first()
  val rippleExpand = 0.24f + (1f - wakeAlpha * 0.5f) * 0.18f
  drawCircle(
    color = primary.copy(alpha = (0.22f * wakeAlpha).coerceIn(0f, 0.35f)),
    radius = (originGround.scale * rippleExpand).coerceAtLeast(4f),
    center = Offset(originGround.screenX, originGround.screenY),
    style = Stroke(width = (originGround.scale * 0.05f).coerceAtLeast(1.5f))
  )

  // 2. Soft surface reflection glow along the board track directly beneath the 3D hop arc
  for (i in 0 until (samples.size - 1)) {
    val segIntensity = samples[i + 1].intensity
    if (segIntensity <= 0.02f) continue
    val g0 = projectedGround[i]
    val g1 = projectedGround[i + 1]
    val avgScale = (g0.scale + g1.scale) * 0.5f

    drawLine(
      color = primary.copy(alpha = (0.16f * segIntensity).coerceIn(0f, 0.25f)),
      start = Offset(g0.screenX, g0.screenY),
      end = Offset(g1.screenX, g1.screenY),
      strokeWidth = (avgScale * 0.28f * (0.35f + 0.65f * segIntensity)).coerceAtLeast(2.5f),
      cap = StrokeCap.Round
    )
  }

  // 3. Translucent 3D vertical light-curtain filaments connecting the airborne trail to the board
  for (i in 1 until samples.size step 2) {
    val sample = samples[i]
    if (sample.intensity <= 0.08f) continue
    val arcPt = projectedArc[i]
    val groundPt = projectedGround[i]
    drawLine(
      color = primary.copy(alpha = (0.11f * sample.intensity).coerceIn(0f, 0.18f)),
      start = Offset(groundPt.screenX, groundPt.screenY),
      end = Offset(arcPt.screenX, arcPt.screenY),
      strokeWidth = (arcPt.scale * 0.07f * sample.intensity).coerceAtLeast(1.2f),
      cap = StrokeCap.Round
    )
  }

  // 4. Multi-layered 3D tapered comet ribbon following the pawn's airborne trajectory
  for (i in 0 until (samples.size - 1)) {
    val s0 = samples[i]
    val s1 = samples[i + 1]
    val segIntensity = (s0.intensity + s1.intensity) * 0.5f
    if (segIntensity <= 0.015f) continue

    val p0 = projectedArc[i]
    val p1 = projectedArc[i + 1]
    val avgScale = (p0.scale + p1.scale) * 0.5f
    val taper = 0.20f + 0.80f * segIntensity

    // Layer A: Wide atmospheric outer bloom (PlayerColor primary, subtle alpha)
    drawLine(
      color = primary.copy(alpha = (0.20f * segIntensity).coerceIn(0f, 0.28f)),
      start = Offset(p0.screenX, p0.screenY),
      end = Offset(p1.screenX, p1.screenY),
      strokeWidth = (avgScale * 0.42f * taper).coerceAtLeast(3.5f),
      cap = StrokeCap.Round
    )

    // Layer B: Rich mid-core ribbon (PlayerColor primary + darkTone depth)
    drawLine(
      color = primary.copy(alpha = (0.46f * segIntensity).coerceIn(0f, 0.58f)),
      start = Offset(p0.screenX, p0.screenY),
      end = Offset(p1.screenX, p1.screenY),
      strokeWidth = (avgScale * 0.23f * taper).coerceAtLeast(2.2f),
      cap = StrokeCap.Round
    )

    // Layer C: Luminous inner filament (PlayerColor softTone highlight)
    drawLine(
      color = softTone.copy(alpha = (0.72f * segIntensity).coerceIn(0f, 0.85f)),
      start = Offset(p0.screenX, p0.screenY),
      end = Offset(p1.screenX, p1.screenY),
      strokeWidth = (avgScale * 0.095f * taper).coerceAtLeast(1.2f),
      cap = StrokeCap.Round
    )
  }

  // 5. Subtle glowing 3D afterimage motes suspended along the wake
  for (i in 1 until samples.size step 2) {
    val sample = samples[i]
    if (sample.intensity <= 0.06f) continue
    val pt = projectedArc[i]
    val shimmer = 0.88f + 0.12f * sin(pulsePhase * 2f + i * 0.9f)
    val moteRadius = (pt.scale * 0.085f * sample.intensity * shimmer).coerceAtLeast(1.6f)

    drawCircle(
      color = darkTone.copy(alpha = (0.24f * sample.intensity).coerceIn(0f, 0.35f)),
      radius = moteRadius * 1.65f,
      center = Offset(pt.screenX, pt.screenY)
    )
    drawCircle(
      color = primary.copy(alpha = (0.48f * sample.intensity).coerceIn(0f, 0.62f)),
      radius = moteRadius * 1.2f,
      center = Offset(pt.screenX, pt.screenY)
    )
    drawCircle(
      color = softTone.copy(alpha = (0.78f * sample.intensity).coerceIn(0f, 0.88f)),
      radius = moteRadius * 0.62f,
      center = Offset(pt.screenX, pt.screenY)
    )
  }
}

private fun DrawScope.drawSculptedPawn3D(
  projector: Camera3DProjector,
  worldPos: Vec3,
  color: PlayerColor,
  isLegalMove: Boolean,
  pulsePhase: Float,
  tokenNumber: Int,
  isMoving: Boolean = false,
  lighting: SceneLightingState3D = DayNightLightingEngine.computeLightingForRealTime()
) {
  val (primary, darkTone, softTone) = paletteForColor(color)
  val pulseOffsetZ = if (isLegalMove) (sin(pulsePhase) * 0.12f + 0.12f) else 0f

  val groundProj = projector.project(Vec3(worldPos.x, worldPos.y, 0.18f))
  val baseProj = projector.project(Vec3(worldPos.x, worldPos.y, worldPos.z + pulseOffsetZ))
  val topProj = projector.project(Vec3(worldPos.x, worldPos.y, worldPos.z + pulseOffsetZ + 0.62f))

  val s = baseProj.scale

  if (isMoving) {
    // Subtle player-colored 3D motion aura enveloping the moving pawn
    drawCircle(
      color = primary.copy(alpha = 0.24f),
      radius = s * 0.44f,
      center = Offset(baseProj.screenX, (baseProj.screenY + topProj.screenY) * 0.5f)
    )
    drawCircle(
      color = softTone.copy(alpha = 0.36f),
      radius = s * 0.33f,
      center = Offset(baseProj.screenX, baseProj.screenY),
      style = Stroke(width = (s * 0.045f).coerceAtLeast(1.6f))
    )
  }

  if (isLegalMove) {
    val haloScale = 0.48f + sin(pulsePhase) * 0.08f
    drawCircle(
      color = BrassHighlight.copy(alpha = 0.85f),
      radius = groundProj.scale * haloScale,
      center = Offset(groundProj.screenX, groundProj.screenY),
      style = Stroke(width = (groundProj.scale * 0.10f).coerceAtLeast(3f))
    )
    drawCircle(
      color = primary.copy(alpha = 0.28f),
      radius = groundProj.scale * haloScale * 0.9f,
      center = Offset(groundProj.screenX, groundProj.screenY)
    )
  }

  // Real-time directional 3D cast shadow on the board plane + contact shadow beneath pawn
  val heightAboveGround = (worldPos.z + pulseOffsetZ - 0.22f).coerceAtLeast(0f)
  val shadowScale = (1f - heightAboveGround * 0.20f).coerceIn(0.45f, 1f)
  val pawnHeightWorld = 0.68f + heightAboveGround * 0.45f
  val shadowTipWorld = Vec3(
    x = worldPos.x + lighting.shadowDirX * pawnHeightWorld * lighting.shadowStretchFactor,
    y = worldPos.y + lighting.shadowDirY * pawnHeightWorld * lighting.shadowStretchFactor,
    z = 0.18f
  )
  val shadowTipProj = projector.project(shadowTipWorld)

  // Elongated directional cast shadow lobe
  drawLine(
    color = lighting.shadowColor.copy(alpha = (lighting.shadowColor.alpha * 0.78f * shadowScale).coerceIn(0.08f, 0.38f)),
    start = Offset(groundProj.screenX, groundProj.screenY),
    end = Offset(shadowTipProj.screenX, shadowTipProj.screenY),
    strokeWidth = (groundProj.scale * 0.42f * shadowScale).coerceAtLeast(3f),
    cap = StrokeCap.Round
  )
  // Ambient occlusion contact shadow directly under the pawn base
  drawCircle(
    color = lighting.shadowColor.copy(alpha = (lighting.shadowColor.alpha * 0.92f * shadowScale).coerceIn(0.12f, 0.42f)),
    radius = groundProj.scale * 0.29f * shadowScale,
    center = Offset(groundProj.screenX + s * 0.03f, groundProj.screenY + s * 0.04f)
  )

  val baseRadius = s * 0.29f
  drawCircle(
    color = darkTone,
    radius = baseRadius,
    center = Offset(baseProj.screenX, baseProj.screenY + s * 0.04f)
  )
  drawCircle(
    color = primary,
    radius = baseRadius * 0.92f,
    center = Offset(baseProj.screenX, baseProj.screenY)
  )

  val topRadius = topProj.scale * 0.17f
  val bodyPath = Path().apply {
    moveTo(baseProj.screenX - baseRadius * 0.78f, baseProj.screenY)
    lineTo(topProj.screenX - topRadius * 0.72f, topProj.screenY + topRadius * 0.3f)
    lineTo(topProj.screenX + topRadius * 0.72f, topProj.screenY + topRadius * 0.3f)
    lineTo(baseProj.screenX + baseRadius * 0.78f, baseProj.screenY)
    close()
  }
  drawPath(path = bodyPath, color = primary, style = Fill)
  drawPath(
    path = bodyPath,
    color = darkTone,
    style = Stroke(width = 1.8f, join = StrokeJoin.Round)
  )

  drawCircle(
    color = BrushedBrass,
    radius = topRadius * 0.88f,
    center = Offset(topProj.screenX, topProj.screenY + topRadius * 0.35f)
  )

  val headRadius = topProj.scale * 0.21f
  drawCircle(
    color = darkTone,
    radius = headRadius * 1.05f,
    center = Offset(topProj.screenX, topProj.screenY)
  )
  drawCircle(
    color = primary,
    radius = headRadius,
    center = Offset(topProj.screenX, topProj.screenY)
  )
  // Specular highlight shifted subtly toward the current sun/moon light source direction
  val specShiftX = (-lighting.shadowDirX).coerceIn(-0.42f, 0.42f)
  val specShiftY = (-lighting.shadowDirY).coerceIn(-0.42f, 0.42f)
  drawCircle(
    color = lerp(softTone, lighting.specularHighlightColor, 0.35f),
    radius = headRadius * 0.38f,
    center = Offset(
      topProj.screenX + headRadius * (specShiftX * 0.75f - 0.12f),
      topProj.screenY + headRadius * (specShiftY * 0.75f - 0.18f)
    )
  )

  if (tokenNumber in 1..4) {
    drawCircle(
      color = AlabasterLinen,
      radius = (headRadius * 0.16f).coerceAtLeast(1.8f),
      center = Offset(topProj.screenX, topProj.screenY + headRadius * 0.15f)
    )
  }
}

private fun DrawScope.drawFlatTile3D(
  projector: Camera3DProjector,
  minX: Float,
  maxX: Float,
  minY: Float,
  maxY: Float,
  z: Float,
  fillColor: Color,
  strokeColor: Color
) {
  val corners = listOf(
    projector.project(Vec3(minX, minY, z)),
    projector.project(Vec3(maxX, minY, z)),
    projector.project(Vec3(maxX, maxY, z)),
    projector.project(Vec3(minX, maxY, z))
  )
  drawPolygon(corners, fillColor)
  drawPolygonStroke(corners, strokeColor, strokeWidth = 1.4f)
}

private fun DrawScope.drawExtrudedBox3D(
  projector: Camera3DProjector,
  minX: Float,
  maxX: Float,
  minY: Float,
  maxY: Float,
  bottomZ: Float,
  topZ: Float,
  topColor: Color,
  sideColorA: Color,
  sideColorB: Color,
  strokeColor: Color
) {
  val b0 = projector.project(Vec3(minX, minY, bottomZ))
  val b1 = projector.project(Vec3(maxX, minY, bottomZ))
  val b2 = projector.project(Vec3(maxX, maxY, bottomZ))
  val b3 = projector.project(Vec3(minX, maxY, bottomZ))

  val t0 = projector.project(Vec3(minX, minY, topZ))
  val t1 = projector.project(Vec3(maxX, minY, topZ))
  val t2 = projector.project(Vec3(maxX, maxY, topZ))
  val t3 = projector.project(Vec3(minX, maxY, topZ))

  data class SideWall(val pts: List<ProjectedPoint>, val color: Color) {
    val avgDepth: Float = pts.map { it.depth }.average().toFloat()
  }

  val walls = listOf(
    SideWall(listOf(b0, b1, t1, t0), sideColorA),
    SideWall(listOf(b1, b2, t2, t1), sideColorB),
    SideWall(listOf(b2, b3, t3, t2), sideColorA),
    SideWall(listOf(b3, b0, t0, t3), sideColorB)
  ).sortedByDescending { it.avgDepth }

  for (wall in walls) {
    drawPolygon(wall.pts, wall.color)
    drawPolygonStroke(wall.pts, strokeColor.copy(alpha = 0.5f), 1.2f)
  }

  val topFace = listOf(t0, t1, t2, t3)
  drawPolygon(topFace, topColor)
  drawPolygonStroke(topFace, strokeColor, 1.8f)
}

private fun DrawScope.drawPolygon(points: List<ProjectedPoint>, color: Color) {
  if (points.isEmpty()) return
  val path = Path().apply {
    moveTo(points[0].screenX, points[0].screenY)
    for (i in 1 until points.size) {
      lineTo(points[i].screenX, points[i].screenY)
    }
    close()
  }
  drawPath(path = path, color = color, style = Fill)
}

private fun DrawScope.drawPolygonStroke(
  points: List<ProjectedPoint>,
  color: Color,
  strokeWidth: Float
) {
  if (points.isEmpty()) return
  val path = Path().apply {
    moveTo(points[0].screenX, points[0].screenY)
    for (i in 1 until points.size) {
      lineTo(points[i].screenX, points[i].screenY)
    }
    close()
  }
  drawPath(
    path = path,
    color = color,
    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
  )
}

private fun DrawScope.drawStarInlay(
  center: Offset,
  radius: Float,
  color: Color
) {
  val path = Path()
  val points = 10
  for (i in 0 until points) {
    val r = if (i % 2 == 0) radius else radius * 0.44f
    val angle = (i * PI / 5.0) - PI / 2.0
    val px = center.x + (cos(angle) * r).toFloat()
    val py = center.y + (sin(angle) * r).toFloat()
    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
  }
  path.close()
  drawPath(path = path, color = color, style = Fill)
}

fun paletteForColor(color: PlayerColor): Triple<Color, Color, Color> {
  return when (color) {
    PlayerColor.RED -> Triple(TerracottaRed, TerracottaRedDark, TerracottaSoft)
    PlayerColor.GREEN -> Triple(EucalyptusSage, EucalyptusSageDark, EucalyptusSoft)
    PlayerColor.YELLOW -> Triple(WarmOchreGold, WarmOchreDark, WarmOchreSoft)
    PlayerColor.BLUE -> Triple(NordicSlateBlue, NordicSlateDark, NordicSlateSoft)
    PlayerColor.PURPLE -> Triple(RoyalAmethyst, RoyalAmethystDark, RoyalAmethystSoft)
    PlayerColor.ORANGE -> Triple(CoralAmber, CoralAmberDark, CoralAmberSoft)
    PlayerColor.TEAL -> Triple(AegeanTeal, AegeanTealDark, AegeanTealSoft)
    PlayerColor.ROSE -> Triple(RoseQuartz, RoseQuartzDark, RoseQuartzSoft)
  }
}

/**
 * True 3D multi-axis rotating Cube Die rendered on Canvas with 3D face normals,
 * Lambertian shading, selection halo for dual-dice play, and projected pips for values 1..6.
 */
@Composable
fun Interactive3DDiceCube(
  diceValue: Int,
  isRolling: Boolean,
  activeColor: PlayerColor,
  enabled: Boolean,
  isSelected: Boolean = false,
  isUsed: Boolean = false,
  onRollOrSelectClicked: () -> Unit,
  sizeDp: Dp = 64.dp,
  testTagName: String = "dice_3d_cube",
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "dice3d_spin")
  val tumbleAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 520, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "tumble_angle"
  )

  val (targetRotX, targetRotY) = remember(diceValue) {
    when (diceValue) {
      1 -> 22f to -28f
      2 -> -68f to 22f
      3 -> 22f to 62f
      4 -> 22f to -118f
      5 -> 112f to 22f
      6 -> 202f to -28f
      else -> 28f to -35f
    }
  }

  val rotX = if (isRolling) tumbleAngle * 1.4f else targetRotX
  val rotY = if (isRolling) tumbleAngle * 1.1f else targetRotY

  val (accentColor, darkAccent, _) = paletteForColor(activeColor)
  val alphaFactor = if (isUsed) 0.38f else 1f

  Canvas(
    modifier = modifier
      .size(sizeDp)
      .testTag(testTagName)
      .pointerInput(enabled) {
        detectTapGestures {
          if (enabled) onRollOrSelectClicked()
        }
      }
  ) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val halfSize = size.minDimension * 0.30f

    val radX = (rotX * PI / 180.0).toFloat()
    val radY = (rotY * PI / 180.0).toFloat()
    val cosX = cos(radX)
    val sinX = sin(radX)
    val cosY = cos(radY)
    val sinY = sin(radY)

    fun transform3D(v: Vec3): Vec3 {
      val x1 = v.x * cosY + v.z * sinY
      val z1 = -v.x * sinY + v.z * cosY
      val y2 = v.y * cosX - z1 * sinX
      val z2 = v.y * sinX + z1 * cosX
      return Vec3(x1, y2, z2)
    }

    fun toScreen(v: Vec3): Offset {
      val perspective = 4.2f / (4.2f - v.z * 0.35f)
      return Offset(
        x = cx + v.x * halfSize * perspective,
        y = cy + v.y * halfSize * perspective
      )
    }

    // Selection halo for active die in dual-dice mode
    if (isSelected && !isUsed && !isRolling) {
      drawCircle(
        color = BrassHighlight.copy(alpha = 0.35f),
        radius = halfSize * 1.52f,
        center = Offset(cx, cy)
      )
      drawCircle(
        color = BrushedBrass,
        radius = halfSize * 1.48f,
        center = Offset(cx, cy),
        style = Stroke(width = 3.2f)
      )
    }

    drawCircle(
      color = Color.Black.copy(alpha = 0.20f * alphaFactor),
      radius = halfSize * 1.15f,
      center = Offset(cx, cy + halfSize * 0.90f)
    )

    data class CubeFace(
      val value: Int,
      val corners: List<Vec3>,
      val normal: Vec3,
      val uAxis: Vec3,
      val vAxis: Vec3
    )

    val faces = listOf(
      CubeFace(
        1,
        listOf(Vec3(-1f, -1f, 1f), Vec3(1f, -1f, 1f), Vec3(1f, 1f, 1f), Vec3(-1f, 1f, 1f)),
        Vec3(0f, 0f, 1f), Vec3(1f, 0f, 0f), Vec3(0f, 1f, 0f)
      ),
      CubeFace(
        6,
        listOf(Vec3(1f, -1f, -1f), Vec3(-1f, -1f, -1f), Vec3(-1f, 1f, -1f), Vec3(1f, 1f, -1f)),
        Vec3(0f, 0f, -1f), Vec3(-1f, 0f, 0f), Vec3(0f, 1f, 0f)
      ),
      CubeFace(
        4,
        listOf(Vec3(1f, -1f, 1f), Vec3(1f, -1f, -1f), Vec3(1f, 1f, -1f), Vec3(1f, 1f, 1f)),
        Vec3(1f, 0f, 0f), Vec3(0f, 0f, -1f), Vec3(0f, 1f, 0f)
      ),
      CubeFace(
        3,
        listOf(Vec3(-1f, -1f, -1f), Vec3(-1f, -1f, 1f), Vec3(-1f, 1f, 1f), Vec3(-1f, 1f, -1f)),
        Vec3(-1f, 0f, 0f), Vec3(0f, 0f, 1f), Vec3(0f, 1f, 0f)
      ),
      CubeFace(
        2,
        listOf(Vec3(-1f, -1f, -1f), Vec3(1f, -1f, -1f), Vec3(1f, -1f, 1f), Vec3(-1f, -1f, 1f)),
        Vec3(0f, -1f, 0f), Vec3(1f, 0f, 0f), Vec3(0f, 0f, 1f)
      ),
      CubeFace(
        5,
        listOf(Vec3(-1f, 1f, 1f), Vec3(1f, 1f, 1f), Vec3(1f, 1f, -1f), Vec3(-1f, 1f, -1f)),
        Vec3(0f, 1f, 0f), Vec3(1f, 0f, 0f), Vec3(0f, 0f, -1f)
      )
    )

    val sortedFaces = faces
      .map { face ->
        val tCorners = face.corners.map(::transform3D)
        val tNormal = transform3D(face.normal)
        val avgZ = tCorners.map { it.z }.average().toFloat()
        Triple(face, tCorners, avgZ to tNormal.z)
      }
      .filter { it.third.second > -0.15f }
      .sortedBy { it.third.first }

    for ((face, tCorners, zPair) in sortedFaces) {
      val light = (0.68f + zPair.second * 0.32f).coerceIn(0.52f, 1f)
      val faceColor = Color(
        red = AlabasterLinen.red * light,
        green = AlabasterLinen.green * light,
        blue = AlabasterLinen.blue * light,
        alpha = alphaFactor
      )
      val path = Path().apply {
        val p0 = toScreen(tCorners[0])
        moveTo(p0.x, p0.y)
        for (i in 1 until tCorners.size) {
          val p = toScreen(tCorners[i])
          lineTo(p.x, p.y)
        }
        close()
      }
      drawPath(path = path, color = faceColor, style = Fill)
      drawPath(
        path = path,
        color = (if (isSelected && !isUsed) BrushedBrass else if (enabled && !isUsed) accentColor else StoneBorderLight)
          .copy(alpha = alphaFactor),
        style = Stroke(width = if (isSelected && !isUsed) 3.0f else 2.0f, join = StrokeJoin.Round)
      )

      val pipOffsets: List<Pair<Float, Float>> = when (face.value) {
        1 -> listOf(0f to 0f)
        2 -> listOf(-0.45f to -0.45f, 0.45f to 0.45f)
        3 -> listOf(-0.48f to -0.48f, 0f to 0f, 0.48f to 0.48f)
        4 -> listOf(-0.45f to -0.45f, 0.45f to -0.45f, -0.45f to 0.45f, 0.45f to 0.45f)
        5 -> listOf(-0.48f to -0.48f, 0.48f to -0.48f, 0f to 0f, -0.48f to 0.48f, 0.48f to 0.48f)
        else -> listOf(
          -0.45f to -0.52f, 0.45f to -0.52f,
          -0.45f to 0f, 0.45f to 0f,
          -0.45f to 0.52f, 0.45f to 0.52f
        )
      }

      for ((u, v) in pipOffsets) {
        val worldPip = Vec3(
          x = face.normal.x + face.uAxis.x * u + face.vAxis.x * v,
          y = face.normal.y + face.uAxis.y * u + face.vAxis.y * v,
          z = face.normal.z + face.uAxis.z * u + face.vAxis.z * v
        )
        val screenPip = toScreen(transform3D(worldPip))
        val pipColor = (if (face.value == 1 || face.value == 6) accentColor else Obsidianink)
          .copy(alpha = alphaFactor)
        val pipRadius = if (face.value == 1) halfSize * 0.20f else halfSize * 0.13f
        drawCircle(
          color = pipColor,
          radius = pipRadius,
          center = screenPip
        )
      }
    }

    if (enabled && !isRolling && !isUsed && !isSelected) {
      drawCircle(
        color = darkAccent.copy(alpha = 0.22f),
        radius = halfSize * 1.40f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.0f)
      )
    }
  }
}

/**
 * Renders custom 3D hologram animations and 3D sticker medallions directly above a pawn in 3D world space.
 * Returns the top-most ProjectedPoint above the pawn so a floating callout badge can anchor to it.
 */
fun DrawScope.drawPawnEmote3DAbovePawn(
  projector: Camera3DProjector,
  pawnWorldPos: Vec3,
  pawnColor: PlayerColor,
  emote: PawnEmoteItem,
  phase: Float
): ProjectedPoint {
  val (primary, darkTone, softTone) = paletteForColor(pawnColor)
  val hoverOffset = sin(phase) * 0.14f
  val baseEmoteZ = pawnWorldPos.z + 0.85f + hoverOffset
  val centerProj = projector.project(Vec3(pawnWorldPos.x, pawnWorldPos.y, baseEmoteZ + 0.45f))
  val topAnchorProj = projector.project(Vec3(pawnWorldPos.x, pawnWorldPos.y, baseEmoteZ + 1.05f))

  // Vertical 3D energy tether linking pawn head to floating 3D emote
  val pawnHeadProj = projector.project(Vec3(pawnWorldPos.x, pawnWorldPos.y, pawnWorldPos.z + 0.65f))
  drawLine(
    color = BrassHighlight.copy(alpha = 0.55f),
    start = Offset(pawnHeadProj.screenX, pawnHeadProj.screenY),
    end = Offset(centerProj.screenX, centerProj.screenY),
    strokeWidth = (centerProj.scale * 0.08f).coerceAtLeast(2f)
  )

  when (emote.visualStyle) {
    PawnEmoteVisualStyle.SOVEREIGN_CROWN_3D -> {
      // Orbiting 3D brass ring + 5-point 3D royal crown above the pawn
      val ringRadius = 0.52f
      val crownBaseZ = baseEmoteZ + 0.22f
      val crownPeakZ = baseEmoteZ + 0.72f
      val points = 10
      val crownPts = (0 until points).map { idx ->
        val angle = phase + (idx.toDouble() / points.toDouble()) * 2.0 * PI
        val r = if (idx % 2 == 0) ringRadius else ringRadius * 0.76f
        val z = if (idx % 2 == 0) crownPeakZ else crownBaseZ
        projector.project(
          Vec3(
            pawnWorldPos.x + (cos(angle) * r).toFloat(),
            pawnWorldPos.y + (sin(angle) * r).toFloat(),
            z
          )
        )
      }
      drawPolygon(crownPts, BrushedBrass.copy(alpha = 0.65f))
      drawPolygonStroke(crownPts, BrassHighlight, strokeWidth = 2.8f)
      crownPts.filterIndexed { index, _ -> index % 2 == 0 }.forEach { tip ->
        drawCircle(
          color = primary,
          radius = (tip.scale * 0.11f).coerceAtLeast(3f),
          center = Offset(tip.screenX, tip.screenY)
        )
      }
    }

    PawnEmoteVisualStyle.THUNDER_STRIKE_3D -> {
      // 3D shockwave ring on ground + 3 jagged lightning bolts descending to pawn
      val ringPts = (0 until 12).map { i ->
        val a = -phase * 1.5f + (i.toDouble() / 12.0) * 2.0 * PI
        val r = 0.68f + sin(phase * 2f) * 0.10f
        projector.project(
          Vec3(
            pawnWorldPos.x + (cos(a) * r).toFloat(),
            pawnWorldPos.y + (sin(a) * r).toFloat(),
            pawnWorldPos.z + 0.15f
          )
        )
      }
      drawPolygonStroke(ringPts, BrassHighlight, strokeWidth = 2.8f)

      for (boltIdx in 0 until 3) {
        val boltAngle = phase + boltIdx * (2.0 * PI / 3.0)
        val topBolt = projector.project(
          Vec3(
            pawnWorldPos.x + (cos(boltAngle) * 0.55f).toFloat(),
            pawnWorldPos.y + (sin(boltAngle) * 0.55f).toFloat(),
            baseEmoteZ + 0.92f
          )
        )
        val midBolt = projector.project(
          Vec3(
            pawnWorldPos.x + (cos(boltAngle + 0.45) * 0.25f).toFloat(),
            pawnWorldPos.y + (sin(boltAngle + 0.45) * 0.25f).toFloat(),
            baseEmoteZ + 0.35f
          )
        )
        drawLine(
          color = BrassHighlight,
          start = Offset(topBolt.screenX, topBolt.screenY),
          end = Offset(midBolt.screenX, midBolt.screenY),
          strokeWidth = 3.2f
        )
        drawLine(
          color = primary,
          start = Offset(midBolt.screenX, midBolt.screenY),
          end = Offset(pawnHeadProj.screenX, pawnHeadProj.screenY),
          strokeWidth = 3.2f
        )
      }
    }

    PawnEmoteVisualStyle.AEGIS_FORTRESS_3D -> {
      // Rotating 3D hexagonal shield cage around and above the pawn
      val hexSides = 6
      val lowerHex = (0 until hexSides).map { i ->
        val a = phase * 0.8f + (i.toDouble() / hexSides) * 2.0 * PI
        projector.project(
          Vec3(
            pawnWorldPos.x + (cos(a) * 0.64f).toFloat(),
            pawnWorldPos.y + (sin(a) * 0.64f).toFloat(),
            pawnWorldPos.z + 0.25f
          )
        )
      }
      val upperHex = (0 until hexSides).map { i ->
        val a = phase * 0.8f + (i.toDouble() / hexSides) * 2.0 * PI
        projector.project(
          Vec3(
            pawnWorldPos.x + (cos(a) * 0.50f).toFloat(),
            pawnWorldPos.y + (sin(a) * 0.50f).toFloat(),
            baseEmoteZ + 0.55f
          )
        )
      }
      drawPolygon(upperHex, primary.copy(alpha = 0.28f))
      drawPolygonStroke(lowerHex, BrushedBrass, strokeWidth = 2.4f)
      drawPolygonStroke(upperHex, BrassHighlight, strokeWidth = 2.8f)
      for (i in 0 until hexSides) {
        drawLine(
          color = BrushedBrass.copy(alpha = 0.75f),
          start = Offset(lowerHex[i].screenX, lowerHex[i].screenY),
          end = Offset(upperHex[i].screenX, upperHex[i].screenY),
          strokeWidth = 2f
        )
      }
    }

    PawnEmoteVisualStyle.SUPERNOVA_STAR_3D -> {
      // Multi-axis 3D spinning star burst above the pawn
      val starPts = (0 until 8).map { i ->
        val a = phase * 1.2f + (i.toDouble() / 8.0) * 2.0 * PI
        val r = if (i % 2 == 0) 0.68f else 0.26f
        projector.project(
          Vec3(
            pawnWorldPos.x + (cos(a) * r).toFloat(),
            pawnWorldPos.y + (sin(a) * r).toFloat(),
            baseEmoteZ + 0.42f + (if (i % 2 == 0) 0.14f else -0.10f)
          )
        )
      }
      drawPolygon(starPts, BrassHighlight.copy(alpha = 0.55f))
      drawPolygonStroke(starPts, primary, strokeWidth = 2.5f)
    }

    PawnEmoteVisualStyle.CELESTIAL_HELIX_3D -> {
      // Dual ascending 3D spiral strands orbiting up from pawn base to crown
      val strandSteps = 8
      for (s in 0 until strandSteps) {
        val frac = s.toFloat() / (strandSteps - 1).toFloat()
        val z = pawnWorldPos.z + 0.20f + frac * 1.35f
        val angleA = phase * 1.6f + frac * (2.0 * PI)
        val angleB = angleA + PI
        val radius = 0.48f * (1f - frac * 0.25f)
        val ptA = projector.project(
          Vec3(
            pawnWorldPos.x + (cos(angleA) * radius).toFloat(),
            pawnWorldPos.y + (sin(angleA) * radius).toFloat(),
            z
          )
        )
        val ptB = projector.project(
          Vec3(
            pawnWorldPos.x + (cos(angleB) * radius).toFloat(),
            pawnWorldPos.y + (sin(angleB) * radius).toFloat(),
            z
          )
        )
        drawLine(
          color = BrushedBrass.copy(alpha = 0.55f),
          start = Offset(ptA.screenX, ptA.screenY),
          end = Offset(ptB.screenX, ptB.screenY),
          strokeWidth = 1.8f
        )
        drawCircle(
          color = BrassHighlight,
          radius = (ptA.scale * 0.10f).coerceAtLeast(2.8f),
          center = Offset(ptA.screenX, ptA.screenY)
        )
        drawCircle(
          color = primary,
          radius = (ptB.scale * 0.10f).coerceAtLeast(2.8f),
          center = Offset(ptB.screenX, ptB.screenY)
        )
      }
    }

    PawnEmoteVisualStyle.DRAGON_FLAME_3D -> {
      // 3 ascending 3D ember vortex rings around the pawn
      for (ringIdx in 0 until 3) {
        val ringZ = pawnWorldPos.z + 0.30f + ringIdx * 0.36f + (hoverOffset * 0.5f)
        val radius = 0.35f + ringIdx * 0.14f
        val ringPts = (0 until 8).map { i ->
          val a = phase * (1.4f + ringIdx * 0.3f) + (i.toDouble() / 8.0) * 2.0 * PI
          projector.project(
            Vec3(
              pawnWorldPos.x + (cos(a) * radius).toFloat(),
              pawnWorldPos.y + (sin(a) * radius).toFloat(),
              ringZ
            )
          )
        }
        drawPolygonStroke(
          ringPts,
          if (ringIdx % 2 == 0) TerracottaRed else BrassHighlight,
          strokeWidth = 2.6f
        )
      }
    }

    PawnEmoteVisualStyle.STICKER_DEAL_WITH_IT,
    PawnEmoteVisualStyle.STICKER_ROYAL_SALUTE,
    PawnEmoteVisualStyle.STICKER_CALCULATED,
    PawnEmoteVisualStyle.STICKER_SHIELD_WALL -> {
      // 3D Holographic Sticker Medallion Disc floating above the pawn
      val discRadius = (centerProj.scale * 0.44f).coerceAtLeast(14f)
      drawCircle(
        color = darkTone.copy(alpha = 0.85f),
        radius = discRadius * 1.12f,
        center = Offset(centerProj.screenX, centerProj.screenY)
      )
      drawCircle(
        color = softTone,
        radius = discRadius * 0.92f,
        center = Offset(centerProj.screenX, centerProj.screenY)
      )
      drawCircle(
        color = BrushedBrass,
        radius = discRadius,
        center = Offset(centerProj.screenX, centerProj.screenY),
        style = Stroke(width = 3f)
      )
      drawStarInlay(
        center = Offset(centerProj.screenX, centerProj.screenY),
        radius = discRadius * 0.58f,
        color = primary.copy(alpha = 0.55f)
      )
    }
  }

  return topAnchorProj
}

/**
 * Interactive 3D Pawn Emote & Sticker Preview Stage used inside the Emote Gallery screen.
 */
@Composable
fun PawnEmote3DPreviewStage(
  emote: PawnEmoteItem,
  pawnColor: PlayerColor,
  triggerMomentLabel: String,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "emote_preview_3d")
  val orbitYaw by infiniteTransition.animateFloat(
    initialValue = -35f,
    targetValue = 325f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 9000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "preview_orbit_yaw"
  )
  val pulsePhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2f * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1350, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "preview_pulse_phase"
  )

  var badgeAnchor by remember { mutableStateOf<Offset?>(null) }
  val (primaryTone, _, _) = paletteForColor(pawnColor)

  Box(
    modifier = modifier
      .testTag("emote_gallery_3d_preview_stage"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val projector = Camera3DProjector(
        yawDegrees = orbitYaw,
        pitchDegrees = 38f,
        zoom = 3.2f,
        viewportWidth = size.width,
        viewportHeight = size.height * 1.18f
      )

      // Draw a sculpted 3D marble & brass showcase plinth
      drawExtrudedBox3D(
        projector = projector,
        minX = -1.35f,
        maxX = 1.35f,
        minY = -1.35f,
        maxY = 1.35f,
        bottomZ = -0.35f,
        topZ = 0.16f,
        topColor = AlabasterLinen,
        sideColorA = Color(0xFFD5CCB8),
        sideColorB = Color(0xFFBEB49E),
        strokeColor = BrushedBrass
      )

      val pedestalTop = projector.project(Vec3(0f, 0f, 0.18f))
      drawStarInlay(
        center = Offset(pedestalTop.screenX, pedestalTop.screenY),
        radius = pedestalTop.scale * 0.72f,
        color = BrushedBrass.copy(alpha = 0.45f)
      )

      val pawnWorld = Vec3(0f, 0f, 0.22f)
      drawSculptedPawn3D(
        projector = projector,
        worldPos = pawnWorld,
        color = pawnColor,
        isLegalMove = true,
        pulsePhase = pulsePhase,
        tokenNumber = 1
      )

      val topProj = drawPawnEmote3DAbovePawn(
        projector = projector,
        pawnWorldPos = pawnWorld,
        pawnColor = pawnColor,
        emote = emote,
        phase = pulsePhase
      )
      badgeAnchor = Offset(topProj.screenX, topProj.screenY)
    }

    // Floating 3D Sticker & Callout Pill above the preview pawn
    val anchor = badgeAnchor
    if (anchor != null) {
      Row(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 12.dp)
          .clip(RoundedCornerShape(50))
          .background(Obsidianink.copy(alpha = 0.92f))
          .border(1.5.dp, BrushedBrass, RoundedCornerShape(50))
          .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = emote.stickerSymbol,
          style = MaterialTheme.typography.headlineSmall
        )
        Column {
          Text(
            text = emote.calloutText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = BrassHighlight
          )
          Text(
            text = "$triggerMomentLabel • ${emote.category.label}",
            style = MaterialTheme.typography.labelSmall,
            color = primaryTone
          )
        }
      }
    }
  }
}

/**
 * Draws an animated 3D parabolic trajectory arc from [fromWorld] to [toWorld] with
 * marching golden spheres and a landing target reticle on the 3D board surface.
 */
private fun DrawScope.drawTutorial3DTrajectoryArc(
  projector: Camera3DProjector,
  fromWorld: Vec3,
  toWorld: Vec3,
  phase: Float
) {
  val segments = 22
  val arcPoints = ArrayList<ProjectedPoint>(segments + 1)
  val groundPoints = ArrayList<ProjectedPoint>(segments + 1)

  for (i in 0..segments) {
    val t = i.toFloat() / segments.toFloat()
    val wx = fromWorld.x + (toWorld.x - fromWorld.x) * t
    val wy = fromWorld.y + (toWorld.y - fromWorld.y) * t
    val baseZ = fromWorld.z + (toWorld.z - fromWorld.z) * t
    val liftZ = (4f * t * (1f - t)) * 1.45f
    arcPoints.add(projector.project(Vec3(wx, wy, baseZ + liftZ)))
    groundPoints.add(projector.project(Vec3(wx, wy, 0.23f)))
  }

  // Ground shadow path on the board
  for (i in 0 until segments) {
    val a = groundPoints[i]
    val b = groundPoints[i + 1]
    drawLine(
      color = Obsidianink.copy(alpha = 0.28f),
      start = Offset(a.screenX, a.screenY),
      end = Offset(b.screenX, b.screenY),
      strokeWidth = 3.5f,
      cap = StrokeCap.Round
    )
  }

  // Outer golden glow + inner bright core for the 3D trajectory ribbon
  val arcPath = Path().apply {
    moveTo(arcPoints[0].screenX, arcPoints[0].screenY)
    for (i in 1..segments) {
      lineTo(arcPoints[i].screenX, arcPoints[i].screenY)
    }
  }
  drawPath(
    path = arcPath,
    color = BrushedBrass.copy(alpha = 0.45f),
    style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
  )
  drawPath(
    path = arcPath,
    color = BrassHighlight,
    style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
  )

  // Animated marching golden energy pulses along the 3D trajectory
  val normalizedPhase = (phase / (2f * PI.toFloat())) % 1f
  for (pulseIdx in 0 until 3) {
    val t = (normalizedPhase + pulseIdx / 3f) % 1f
    val wx = fromWorld.x + (toWorld.x - fromWorld.x) * t
    val wy = fromWorld.y + (toWorld.y - fromWorld.y) * t
    val baseZ = fromWorld.z + (toWorld.z - fromWorld.z) * t
    val liftZ = (4f * t * (1f - t)) * 1.45f
    val p = projector.project(Vec3(wx, wy, baseZ + liftZ))
    drawCircle(
      color = BrassHighlight.copy(alpha = 0.40f),
      radius = (p.scale * 0.22f).coerceAtLeast(6f),
      center = Offset(p.screenX, p.screenY)
    )
    drawCircle(
      color = AlabasterLinen,
      radius = (p.scale * 0.11f).coerceAtLeast(3.2f),
      center = Offset(p.screenX, p.screenY)
    )
  }
}

/**
 * Draws a 3D volumetric spotlight ring, vertical light pillar, and floating 3D diamond
 * indicator for a [Tutorial3DBeaconSpec], returning the projected screen anchor for its callout pill.
 */
private fun DrawScope.drawTutorial3DBeacon(
  projector: Camera3DProjector,
  beacon: Tutorial3DBeaconSpec,
  phase: Float
): ProjectedPoint {
  val (primary, _, _) = paletteForColor(beacon.accentColor)
  val pulseFactor = (0.5f + 0.5f * sin(phase)).toFloat()
  val hoverZ = (sin(phase * 1.3f) * 0.14f).toFloat()

  // 1. 3D Perspective Spotlight Rings on the board surface
  val ringRadiusOuter = 0.68f + pulseFactor * 0.18f
  val ringRadiusInner = 0.44f
  val ringSegments = 16

  val outerRingPts = (0 until ringSegments).map { i ->
    val angle = (i.toDouble() / ringSegments.toDouble()) * 2.0 * PI + phase * 0.4f
    projector.project(
      Vec3(
        beacon.worldX + (cos(angle) * ringRadiusOuter).toFloat(),
        beacon.worldY + (sin(angle) * ringRadiusOuter).toFloat(),
        beacon.worldZ
      )
    )
  }
  val innerRingPts = (0 until ringSegments).map { i ->
    val angle = (i.toDouble() / ringSegments.toDouble()) * 2.0 * PI - phase * 0.6f
    projector.project(
      Vec3(
        beacon.worldX + (cos(angle) * ringRadiusInner).toFloat(),
        beacon.worldY + (sin(angle) * ringRadiusInner).toFloat(),
        beacon.worldZ + 0.03f
      )
    )
  }

  drawPolygon(outerRingPts, primary.copy(alpha = 0.16f + pulseFactor * 0.10f))
  drawPolygonStroke(outerRingPts, BrassHighlight.copy(alpha = 0.85f), strokeWidth = 2.6f)
  drawPolygonStroke(innerRingPts, primary, strokeWidth = 2.2f)

  // 2. Vertical 3D Light Beam & Floating 3D Pointer Crystal
  val baseProj = projector.project(Vec3(beacon.worldX, beacon.worldY, beacon.worldZ + 0.08f))
  val tipZ = beacon.worldZ + 0.58f + hoverZ
  val topZ = beacon.worldZ + 1.05f + hoverZ
  val crystalTip = projector.project(Vec3(beacon.worldX, beacon.worldY, tipZ))
  val crystalTop = projector.project(Vec3(beacon.worldX, beacon.worldY, topZ))

  // Glowing vertical beam
  drawLine(
    color = BrassHighlight.copy(alpha = 0.55f),
    start = Offset(baseProj.screenX, baseProj.screenY),
    end = Offset(crystalTop.screenX, crystalTop.screenY),
    strokeWidth = 4.5f,
    cap = StrokeCap.Round
  )

  // 3D Octahedral Pointer Crystal hovering above the target
  val midZ = (tipZ + topZ) * 0.5f
  val crystalRadius = 0.24f
  val diamondRing = (0 until 4).map { i ->
    val a = phase * 1.4f + i * (PI / 2.0)
    projector.project(
      Vec3(
        beacon.worldX + (cos(a) * crystalRadius).toFloat(),
        beacon.worldY + (sin(a) * crystalRadius).toFloat(),
        midZ
      )
    )
  }
  for (i in 0 until 4) {
    val next = (i + 1) % 4
    drawPolygon(listOf(crystalTip, diamondRing[i], diamondRing[next]), primary.copy(alpha = 0.78f))
    drawPolygon(listOf(crystalTop, diamondRing[i], diamondRing[next]), BrassHighlight.copy(alpha = 0.85f))
  }
  drawPolygonStroke(diamondRing, AlabasterLinen, strokeWidth = 1.8f)

  return projector.project(Vec3(beacon.worldX, beacon.worldY, topZ + 0.28f))
}

