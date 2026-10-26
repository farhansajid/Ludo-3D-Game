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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.PlayerColor
import com.example.domain.LudoRulesEngine
import com.example.ui.theme.AlabasterLinen
import com.example.ui.theme.BrassHighlight
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.EucalyptusSageDark
import com.example.ui.theme.EucalyptusSoft
import com.example.ui.theme.NordicSlateBlue
import com.example.ui.theme.NordicSlateDark
import com.example.ui.theme.NordicSlateSoft
import com.example.ui.theme.Obsidianink
import com.example.ui.theme.StoneBorderLight
import com.example.ui.theme.TerracottaRed
import com.example.ui.theme.TerracottaRedDark
import com.example.ui.theme.TerracottaSoft
import com.example.ui.theme.TravertineStone
import com.example.ui.theme.WarmOchreDark
import com.example.ui.theme.WarmOchreGold
import com.example.ui.theme.WarmOchreSoft
import com.example.ui.theme.WarmSandCard
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
  val progress: Float = 0f
)

private data class ProjectedTokenHitTarget(
  val tokenIndex: Int,
  val screenX: Float,
  val screenY: Float,
  val hitRadiusPx: Float,
  val depth: Float
)

object LudoBoardCoordinates {
  // 52 perimeter track squares in clockwise order starting at Red Start (1, 6)
  val MAIN_TRACK_COORDS: List<Pair<Float, Float>> = listOf(
    // 0..4: Left arm top row moving right
    1f to 6f, 2f to 6f, 3f to 6f, 4f to 6f, 5f to 6f,
    // 5..10: Top arm left col moving up
    6f to 5f, 6f to 4f, 6f to 3f, 6f to 2f, 6f to 1f, 6f to 0f,
    // 11: Top middle turn
    7f to 0f,
    // 12..17: Top arm right col moving down (13 is Green start)
    8f to 0f, 8f to 1f, 8f to 2f, 8f to 3f, 8f to 4f, 8f to 5f,
    // 18..23: Right arm top row moving right
    9f to 6f, 10f to 6f, 11f to 6f, 12f to 6f, 13f to 6f, 14f to 6f,
    // 24: Right middle turn
    14f to 7f,
    // 25..30: Right arm bottom row moving left (26 is Yellow start)
    14f to 8f, 13f to 8f, 12f to 8f, 11f to 8f, 10f to 8f, 9f to 8f,
    // 31..36: Bottom arm right col moving down
    8f to 9f, 8f to 10f, 8f to 11f, 8f to 12f, 8f to 13f, 8f to 14f,
    // 37: Bottom middle turn
    7f to 14f,
    // 38..43: Bottom arm left col moving up (39 is Blue start)
    6f to 14f, 6f to 13f, 6f to 12f, 6f to 11f, 6f to 10f, 6f to 9f,
    // 44..49: Left arm bottom row moving left
    5f to 8f, 4f to 8f, 3f to 8f, 2f to 8f, 1f to 8f, 0f to 8f,
    // 50: Left middle turn
    0f to 7f,
    // 51: Left arm top-left corner before Red start
    0f to 6f
  )

  fun yardPedestalGridCoord(color: PlayerColor, slotIndex: Int): Pair<Float, Float> {
    val baseCol = when (color) {
      PlayerColor.RED, PlayerColor.BLUE -> 1.5f
      PlayerColor.GREEN, PlayerColor.YELLOW -> 10.5f
    }
    val baseRow = when (color) {
      PlayerColor.RED, PlayerColor.GREEN -> 1.5f
      PlayerColor.YELLOW, PlayerColor.BLUE -> 10.5f
    }
    val colOffset = if (slotIndex % 2 == 0) 0.35f else 2.65f
    val rowOffset = if (slotIndex / 2 == 0) 0.35f else 2.65f
    return (baseCol + colOffset) to (baseRow + rowOffset)
  }

  fun homeStretchGridCoord(color: PlayerColor, stepIndex: Int): Pair<Float, Float> {
    // stepIndex in 0..4 corresponds to relative pos 51..55
    val s = (stepIndex + 1).coerceIn(1, 5).toFloat()
    return when (color) {
      PlayerColor.RED -> s to 7f
      PlayerColor.GREEN -> 7f to s
      PlayerColor.YELLOW -> (14f - s) to 7f
      PlayerColor.BLUE -> 7f to (14f - s)
    }
  }

  fun homeGoalGridCoord(color: PlayerColor, slotIndex: Int): Pair<Float, Float> {
    val spread = (slotIndex - 1.5f) * 0.28f
    return when (color) {
      PlayerColor.RED -> 6.32f to (7.0f + spread)
      PlayerColor.GREEN -> (7.0f + spread) to 6.32f
      PlayerColor.YELLOW -> 7.68f to (7.0f + spread)
      PlayerColor.BLUE -> (7.0f + spread) to 7.68f
    }
  }

  fun tokenBoardCoord(color: PlayerColor, slotIndex: Int, relativePos: Int): Pair<Float, Float> {
    return when {
      relativePos < 0 -> yardPedestalGridCoord(color, slotIndex)
      relativePos in 0..50 -> {
        val globalIdx = (color.startTrackIndex + relativePos) % 52
        MAIN_TRACK_COORDS[globalIdx]
      }
      relativePos in 51..55 -> homeStretchGridCoord(color, relativePos - 51)
      else -> homeGoalGridCoord(color, slotIndex)
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
    // World coordinates: x in [-7.5, 7.5], y in [-7.5, 7.5], z >= 0 is height above board
    val rx = world.x * cosYaw - world.y * sinYaw
    val ry = world.x * sinYaw + world.y * cosYaw
    val rz = world.z

    // Tilt around X axis by pitchDegrees (90 deg = top-down, 45 deg = isometric)
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
  legalMoveTokenIndices: List<Int>,
  activeBlockades: Map<Int, Set<PlayerColor>>,
  animatedToken: AnimatedTokenState?,
  yawDegrees: Float,
  pitchDegrees: Float,
  zoom: Float,
  onCameraTransform: (deltaYaw: Float, deltaPitch: Float, zoomFactor: Float) -> Unit,
  onTokenTapped: (tokenIndex: Int) -> Unit,
  onBoardCenterTapped: () -> Unit,
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

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("interactive_3d_ludo_board")
      .pointerInput(legalMoveTokenIndices) {
        detectTapGestures { tapOffset ->
          // Check legal movable tokens first, sorted closest to camera
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

          // Or check any token tap
          val anyTapped = hitTargets
            .sortedBy { it.depth }
            .firstOrNull { target ->
              hypot(tapOffset.x - target.screenX, tapOffset.y - target.screenY) <= target.hitRadiusPx
            }
          if (anyTapped != null) {
            onTokenTapped(anyTapped.tokenIndex)
            return@detectTapGestures
          }

          // Check center home prism tap
          if (hypot(tapOffset.x - centerGoalScreenPoint.x, tapOffset.y - centerGoalScreenPoint.y) <= 64f) {
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

      // 1. Draw 3D Extruded Travertine Plinth Base & Drop Shadow
      drawBoardPlinth3D(projector)

      // 2. Draw 4 Corner Yards (6x6 each) with 3D Beveled Walls & 4 Pedestal Rings
      drawCornerYard3D(projector, PlayerColor.RED, startCol = 0f, startRow = 0f)
      drawCornerYard3D(projector, PlayerColor.GREEN, startCol = 9f, startRow = 0f)
      drawCornerYard3D(projector, PlayerColor.YELLOW, startCol = 9f, startRow = 9f)
      drawCornerYard3D(projector, PlayerColor.BLUE, startCol = 0f, startRow = 9f)

      // 3. Draw 52 Main Track Tiles + 20 Home Column Tiles
      drawTrackTiles3D(projector, activeBlockades)

      // 4. Draw Center 3x3 4-Color 3D Pyramid Goal
      val centerProjected = projector.project(projector.gridToWorld(7f, 7f, 0.45f))
      centerGoalScreenPoint = Offset(centerProjected.screenX, centerProjected.screenY)
      drawCenterGoalPyramid3D(projector)

      // 5. Compute 3D positions of all 16 tokens (with micro-stack offsets & parabolic jump)
      val tokenWorldList = computeTokenWorldPositions(
        tokens = tokens,
        animatedToken = animatedToken,
        projector = projector
      )

      // Sort tokens back-to-front by camera depth (Painter's Algorithm)
      val sortedTokens = tokenWorldList.sortedByDescending { it.projected.depth }
      val newHitTargets = mutableListOf<ProjectedTokenHitTarget>()

      for (item in sortedTokens) {
        val isLegal = item.tokenIndex in legalMoveTokenIndices
        drawSculptedPawn3D(
          projector = projector,
          worldPos = item.worldPos,
          color = item.color,
          isLegalMove = isLegal,
          pulsePhase = pulsePhase,
          tokenNumber = (item.tokenIndex % 4) + 1
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
  animatedToken: AnimatedTokenState?,
  projector: Camera3DProjector
): List<TokenRenderEntry> {
  // Group non-animating tokens by their board coordinate so stacked tokens spread neatly
  val rawCoords = Array(16) { idx ->
    val color = LudoRulesEngine.colorForTokenIndex(idx)
    val slotIdx = idx % 4
    val pos = tokens.getOrElse(idx) { -1 }
    LudoBoardCoordinates.tokenBoardCoord(color, slotIdx, pos)
  }

  val coordGroups = mutableMapOf<Pair<Int, Int>, MutableList<Int>>()
  for (idx in 0 until 16) {
    val key = (rawCoords[idx].first * 10).toInt() to (rawCoords[idx].second * 10).toInt()
    coordGroups.getOrPut(key) { mutableListOf() }.add(idx)
  }

  val entries = mutableListOf<TokenRenderEntry>()
  for (idx in 0 until 16) {
    val color = LudoRulesEngine.colorForTokenIndex(idx)
    val slotIdx = idx % 4

    val world = if (animatedToken != null && animatedToken.tokenIndex == idx) {
      val fromCoord = LudoBoardCoordinates.tokenBoardCoord(color, slotIdx, animatedToken.fromPos)
      val toCoord = LudoBoardCoordinates.tokenBoardCoord(color, slotIdx, animatedToken.toPos)
      val t = animatedToken.progress.coerceIn(0f, 1f)
      val interpCol = fromCoord.first + (toCoord.first - fromCoord.first) * t
      val interpRow = fromCoord.second + (toCoord.second - fromCoord.second) * t
      val hopZ = 0.22f + sin(t * PI).toFloat() * 1.35f
      projector.gridToWorld(interpCol, interpRow, hopZ)
    } else {
      val baseCoord = rawCoords[idx]
      val key = (baseCoord.first * 10).toInt() to (baseCoord.second * 10).toInt()
      val group = coordGroups[key].orEmpty()
      val (offsetCol, offsetRow) = if (group.size > 1) {
        val rank = group.indexOf(idx)
        val angle = (rank.toDouble() / group.size.toDouble()) * 2.0 * PI + PI / 4.0
        val radius = 0.23f
        (cos(angle).toFloat() * radius) to (sin(angle).toFloat() * radius)
      } else {
        0f to 0f
      }
      projector.gridToWorld(baseCoord.first + offsetCol, baseCoord.second + offsetRow, 0.22f)
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

private fun DrawScope.drawBoardPlinth3D(projector: Camera3DProjector) {
  // Ambient soft shadow beneath the board
  val shadowCorners = listOf(
    projector.project(Vec3(-8.2f, -8.2f, -0.75f)),
    projector.project(Vec3(8.2f, -8.2f, -0.75f)),
    projector.project(Vec3(8.2f, 8.2f, -0.75f)),
    projector.project(Vec3(-8.2f, 8.2f, -0.75f))
  )
  drawPolygon(shadowCorners, Color.Black.copy(alpha = 0.26f))

  // 3D Board Plinth Box (-7.75 to 7.75 in X and Y, z from -0.55 to 0.0)
  drawExtrudedBox3D(
    projector = projector,
    minX = -7.75f,
    maxX = 7.75f,
    minY = -7.75f,
    maxY = 7.75f,
    bottomZ = -0.55f,
    topZ = 0.0f,
    topColor = AlabasterLinen,
    sideColorA = Color(0xFFD5CCB8),
    sideColorB = Color(0xFFBEB49E),
    strokeColor = StoneBorderLight
  )
}

private fun DrawScope.drawCornerYard3D(
  projector: Camera3DProjector,
  color: PlayerColor,
  startCol: Float,
  startRow: Float
) {
  val (primary, darkTone, softTone) = paletteForColor(color)
  val minX = startCol - 7.5f
  val maxX = startCol + 6f - 7.5f
  val minY = startRow - 7.5f
  val maxY = startRow + 6f - 7.5f

  // Outer colored 3D yard block
  drawExtrudedBox3D(
    projector = projector,
    minX = minX + 0.08f,
    maxX = maxX - 0.08f,
    minY = minY + 0.08f,
    maxY = maxY - 0.08f,
    bottomZ = 0.0f,
    topZ = 0.18f,
    topColor = primary,
    sideColorA = darkTone,
    sideColorB = darkTone.copy(alpha = 0.88f),
    strokeColor = darkTone
  )

  // Inner recessed alabaster court
  drawExtrudedBox3D(
    projector = projector,
    minX = minX + 0.85f,
    maxX = maxX - 0.85f,
    minY = minY + 0.85f,
    maxY = maxY - 0.85f,
    bottomZ = 0.18f,
    topZ = 0.22f,
    topColor = softTone,
    sideColorA = WarmSandCard,
    sideColorB = StoneBorderLight,
    strokeColor = darkTone.copy(alpha = 0.35f)
  )

  // Four 3D token pedestals inside the yard
  for (slot in 0 until 4) {
    val (pCol, pRow) = LudoBoardCoordinates.yardPedestalGridCoord(color, slot)
    val center = projector.project(projector.gridToWorld(pCol, pRow, 0.24f))
    val r = center.scale * 0.42f
    drawCircle(
      color = darkTone.copy(alpha = 0.28f),
      radius = r * 1.08f,
      center = Offset(center.screenX, center.screenY + r * 0.12f)
    )
    drawCircle(
      color = AlabasterLinen,
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

private fun DrawScope.drawTrackTiles3D(
  projector: Camera3DProjector,
  activeBlockades: Map<Int, Set<PlayerColor>>
) {
  // Draw 52 main track tiles
  LudoBoardCoordinates.MAIN_TRACK_COORDS.forEachIndexed { globalIdx, (col, row) ->
    val startOwner = PlayerColor.entries.firstOrNull { it.startTrackIndex == globalIdx }
    val isStar = globalIdx in LudoRulesEngine.STAR_GLOBAL_INDICES

    val tileColor = when {
      startOwner != null -> paletteForColor(startOwner).first
      isStar -> Color(0xFFF3E6C8)
      else -> TravertineStone
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

    // Safe Star or Start marker
    if (isStar || startOwner != null) {
      val center = projector.project(projector.gridToWorld(col, row, 0.14f))
      val starColor = if (startOwner != null) AlabasterLinen else BrushedBrass
      drawStarInlay(
        center = Offset(center.screenX, center.screenY),
        radius = center.scale * 0.28f,
        color = starColor
      )
    }

    // Active Blockade shield ring on this global square
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

  // Draw 5 colored home stretch tiles for each of the 4 colors
  for (color in PlayerColor.entries) {
    val (primary, darkTone, softTone) = paletteForColor(color)
    for (step in 0 until 5) {
      val (col, row) = LudoBoardCoordinates.homeStretchGridCoord(color, step)
      drawFlatTile3D(
        projector = projector,
        minX = col - 7.5f + 0.05f,
        maxX = col - 6.5f - 0.05f,
        minY = row - 7.5f + 0.05f,
        maxY = row - 6.5f - 0.05f,
        z = 0.15f,
        fillColor = softTone,
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

private fun DrawScope.drawCenterGoalPyramid3D(projector: Camera3DProjector) {
  val apex = projector.project(Vec3(0f, 0f, 0.65f))
  val nw = projector.project(Vec3(-1.5f, -1.5f, 0.16f))
  val ne = projector.project(Vec3(1.5f, -1.5f, 0.16f))
  val se = projector.project(Vec3(1.5f, 1.5f, 0.16f))
  val sw = projector.project(Vec3(-1.5f, 1.5f, 0.16f))

  // Left Triangle: RED
  drawPolygon(listOf(nw, sw, apex), TerracottaRed)
  // Top Triangle: GREEN
  drawPolygon(listOf(nw, ne, apex), EucalyptusSage)
  // Right Triangle: YELLOW
  drawPolygon(listOf(ne, se, apex), WarmOchreGold)
  // Bottom Triangle: BLUE
  drawPolygon(listOf(sw, se, apex), NordicSlateBlue)

  // Golden ridge lines & crown cap
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
    color = BrassHighlight,
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

private fun DrawScope.drawSculptedPawn3D(
  projector: Camera3DProjector,
  worldPos: Vec3,
  color: PlayerColor,
  isLegalMove: Boolean,
  pulsePhase: Float,
  tokenNumber: Int
) {
  val (primary, darkTone, softTone) = paletteForColor(color)
  val pulseOffsetZ = if (isLegalMove) (sin(pulsePhase) * 0.12f + 0.12f) else 0f

  val groundProj = projector.project(Vec3(worldPos.x, worldPos.y, 0.18f))
  val baseProj = projector.project(Vec3(worldPos.x, worldPos.y, worldPos.z + pulseOffsetZ))
  val topProj = projector.project(Vec3(worldPos.x, worldPos.y, worldPos.z + pulseOffsetZ + 0.62f))

  val s = baseProj.scale

  // 1. Pulsing legal-move ground halo ring
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

  // 2. Ground drop shadow (scales down slightly as token jumps higher)
  val heightAboveGround = (worldPos.z - 0.22f).coerceAtLeast(0f)
  val shadowScale = (1f - heightAboveGround * 0.22f).coerceIn(0.45f, 1f)
  drawCircle(
    color = Color.Black.copy(alpha = 0.28f * shadowScale),
    radius = groundProj.scale * 0.30f * shadowScale,
    center = Offset(groundProj.screenX + s * 0.05f, groundProj.screenY + s * 0.06f)
  )

  // 3. Pawn pedestal base disk
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

  // 4. Tapered 3D cone body connecting base to head
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

  // 5. Brass collar ring
  drawCircle(
    color = BrushedBrass,
    radius = topRadius * 0.88f,
    center = Offset(topProj.screenX, topProj.screenY + topRadius * 0.35f)
  )

  // 6. Spherical 3D finial head with specular highlight
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
  drawCircle(
    color = softTone,
    radius = headRadius * 0.38f,
    center = Offset(topProj.screenX - headRadius * 0.28f, topProj.screenY - headRadius * 0.28f)
  )

  // Subtle token number pip indicators (1..4) on the collar
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

  // Sort the 4 side walls by average depth so only front-facing walls show on top
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
  }
}

/**
 * True 3D multi-axis rotating Cube Die rendered on Canvas with 3D face normals,
 * Lambertian shading, and projected pips for values 1..6.
 */
@Composable
fun Interactive3DDiceCube(
  diceValue: Int,
  isRolling: Boolean,
  activeColor: PlayerColor,
  enabled: Boolean,
  onRollClicked: () -> Unit,
  sizeDp: Dp = 76.dp,
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

  Canvas(
    modifier = modifier
      .size(sizeDp)
      .testTag("dice_3d_cube")
      .pointerInput(enabled) {
        detectTapGestures {
          if (enabled) onRollClicked()
        }
      }
  ) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val halfSize = size.minDimension * 0.31f

    val radX = (rotX * PI / 180.0).toFloat()
    val radY = (rotY * PI / 180.0).toFloat()
    val cosX = cos(radX)
    val sinX = sin(radX)
    val cosY = cos(radY)
    val sinY = sin(radY)

    fun transform3D(v: Vec3): Vec3 {
      // Rotate Y then X
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

    // Soft shadow under die
    drawCircle(
      color = Color.Black.copy(alpha = 0.22f),
      radius = halfSize * 1.18f,
      center = Offset(cx, cy + halfSize * 0.92f)
    )

    data class CubeFace(
      val value: Int,
      val corners: List<Vec3>,
      val normal: Vec3,
      val uAxis: Vec3,
      val vAxis: Vec3
    )

    val faces = listOf(
      // Front (Z = +1): Value 1
      CubeFace(
        1,
        listOf(Vec3(-1f, -1f, 1f), Vec3(1f, -1f, 1f), Vec3(1f, 1f, 1f), Vec3(-1f, 1f, 1f)),
        Vec3(0f, 0f, 1f), Vec3(1f, 0f, 0f), Vec3(0f, 1f, 0f)
      ),
      // Back (Z = -1): Value 6
      CubeFace(
        6,
        listOf(Vec3(1f, -1f, -1f), Vec3(-1f, -1f, -1f), Vec3(-1f, 1f, -1f), Vec3(1f, 1f, -1f)),
        Vec3(0f, 0f, -1f), Vec3(-1f, 0f, 0f), Vec3(0f, 1f, 0f)
      ),
      // Right (X = +1): Value 4
      CubeFace(
        4,
        listOf(Vec3(1f, -1f, 1f), Vec3(1f, -1f, -1f), Vec3(1f, 1f, -1f), Vec3(1f, 1f, 1f)),
        Vec3(1f, 0f, 0f), Vec3(0f, 0f, -1f), Vec3(0f, 1f, 0f)
      ),
      // Left (X = -1): Value 3
      CubeFace(
        3,
        listOf(Vec3(-1f, -1f, -1f), Vec3(-1f, -1f, 1f), Vec3(-1f, 1f, 1f), Vec3(-1f, 1f, -1f)),
        Vec3(-1f, 0f, 0f), Vec3(0f, 0f, 1f), Vec3(0f, 1f, 0f)
      ),
      // Top (Y = -1): Value 2
      CubeFace(
        2,
        listOf(Vec3(-1f, -1f, -1f), Vec3(1f, -1f, -1f), Vec3(1f, -1f, 1f), Vec3(-1f, -1f, 1f)),
        Vec3(0f, -1f, 0f), Vec3(1f, 0f, 0f), Vec3(0f, 0f, 1f)
      ),
      // Bottom (Y = +1): Value 5
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
        alpha = 1f
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
        color = if (enabled) accentColor else StoneBorderLight,
        style = Stroke(width = if (enabled) 2.6f else 1.8f, join = StrokeJoin.Round)
      )

      // Draw 3D projected pips on the face
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
        val pipColor = if (face.value == 1 || face.value == 6) accentColor else Obsidianink
        val pipRadius = if (face.value == 1) halfSize * 0.20f else halfSize * 0.13f
        drawCircle(
          color = pipColor,
          radius = pipRadius,
          center = screenPip
        )
      }
    }

    if (enabled && !isRolling) {
      drawCircle(
        color = darkAccent.copy(alpha = 0.25f),
        radius = halfSize * 1.42f,
        center = Offset(cx, cy),
        style = Stroke(width = 2.5f)
      )
    }
  }
}
