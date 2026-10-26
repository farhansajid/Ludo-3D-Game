package com.example.domain

import com.example.data.PlayerColor
import com.example.data.PlayerSeat
import com.example.data.TurnPhase

data class MoveOutcome(
  val updatedTokens: List<Int>,
  val pathSteps: List<Int>,
  val capturedTokenIndices: List<Int>,
  val reachedHome: Boolean,
  val playerFinishedAll: Boolean,
  val bonusRollGranted: Boolean,
  val nextTurnColor: PlayerColor,
  val nextTurnPhase: TurnPhase,
  val nextConsecutiveSixes: Int,
  val winnerColor: String,
  val updatedWinnerOrder: List<String>,
  val eventDescription: String
)

object LudoRulesEngine {

  const val YARD_POS = -1
  const val MAX_MAIN_TRACK_REL = 50
  const val HOME_GOAL_POS = 56
  const val OUTER_TRACK_SIZE = 52

  val SAFE_GLOBAL_INDICES = setOf(0, 8, 13, 21, 26, 34, 39, 47)
  val STAR_GLOBAL_INDICES = setOf(8, 21, 34, 47)

  fun tokenIndicesForColor(color: PlayerColor): IntRange {
    val start = color.ordinal * 4
    return start until (start + 4)
  }

  fun colorForTokenIndex(tokenIndex: Int): PlayerColor {
    return PlayerColor.entries[(tokenIndex / 4).coerceIn(0, 3)]
  }

  fun toGlobalTrackIndex(color: PlayerColor, relativePos: Int): Int? {
    if (relativePos !in 0..MAX_MAIN_TRACK_REL) return null
    return (color.startTrackIndex + relativePos) % OUTER_TRACK_SIZE
  }

  /**
   * Returns a map of global track square (0..51) -> set of PlayerColors that have >= 2 tokens
   * stacked on that square (forming an impassable blockade for other colors).
   */
  fun getActiveBlockades(tokens: List<Int>): Map<Int, Set<PlayerColor>> {
    val countsBySquareAndColor = mutableMapOf<Int, MutableMap<PlayerColor, Int>>()
    for (tokenIndex in 0 until 16) {
      val pos = tokens.getOrElse(tokenIndex) { YARD_POS }
      val color = colorForTokenIndex(tokenIndex)
      val globalIdx = toGlobalTrackIndex(color, pos) ?: continue
      val colorMap = countsBySquareAndColor.getOrPut(globalIdx) { mutableMapOf() }
      colorMap[color] = (colorMap[color] ?: 0) + 1
    }

    val blockades = mutableMapOf<Int, Set<PlayerColor>>()
    for ((globalIdx, colorCounts) in countsBySquareAndColor) {
      val blockingColors = colorCounts.filterValues { it >= 2 }.keys
      if (blockingColors.isNotEmpty()) {
        blockades[globalIdx] = blockingColors
      }
    }
    return blockades
  }

  /**
   * Checks whether [tokenIndex] belonging to [color] can legally move by [diceValue].
   * Rules enforced:
   * - Rolling 6 is required to exit the Home Yard (-1 -> 0).
   * - Exact roll is required to land in the center Home Triangle (56).
   * - Two same-color opponent tokens on any outer-track square form an impassable blockade
   *   that cannot be passed through or landed upon.
   */
  fun canTokenMove(
    tokens: List<Int>,
    tokenIndex: Int,
    diceValue: Int
  ): Boolean {
    if (diceValue !in 1..6) return false
    val color = colorForTokenIndex(tokenIndex)
    val currentPos = tokens.getOrElse(tokenIndex) { YARD_POS }

    if (currentPos >= HOME_GOAL_POS) return false

    val blockades = getActiveBlockades(tokens)

    if (currentPos == YARD_POS) {
      if (diceValue != 6) return false
      val startGlobal = color.startTrackIndex
      val blockingColors = blockades[startGlobal].orEmpty()
      return blockingColors.none { it != color }
    }

    val targetPos = currentPos + diceValue
    if (targetPos > HOME_GOAL_POS) return false

    // Check every step along the path for opponent blockades on the outer track
    for (stepPos in (currentPos + 1)..targetPos) {
      val globalStep = toGlobalTrackIndex(color, stepPos)
      if (globalStep != null) {
        val blockingColors = blockades[globalStep].orEmpty()
        if (blockingColors.any { it != color }) {
          return false
        }
      }
    }

    return true
  }

  fun getLegalMoveTokenIndices(
    tokens: List<Int>,
    color: PlayerColor,
    diceValue: Int
  ): List<Int> {
    return tokenIndicesForColor(color).filter { idx ->
      canTokenMove(tokens, idx, diceValue)
    }
  }

  /**
   * Executes a move for [tokenIndex] by [diceValue] and computes captures, bonus rolls,
   * turn transitions, and win state.
   */
  fun executeMove(
    tokens: List<Int>,
    seats: List<PlayerSeat>,
    currentTurnColor: PlayerColor,
    tokenIndex: Int,
    diceValue: Int,
    consecutiveSixes: Int,
    currentWinnerColor: String,
    currentWinnerOrder: List<String>
  ): MoveOutcome {
    val mutableTokens = tokens.toMutableList()
    val startPos = mutableTokens[tokenIndex]
    val targetPos = if (startPos == YARD_POS) 0 else (startPos + diceValue).coerceAtMost(HOME_GOAL_POS)

    val pathSteps = if (startPos == YARD_POS) {
      listOf(0)
    } else {
      ((startPos + 1)..targetPos).toList()
    }

    mutableTokens[tokenIndex] = targetPos

    // Check for captures on the destination square (only on outer track and non-safe squares)
    val capturedIndices = mutableListOf<Int>()
    val destGlobal = toGlobalTrackIndex(currentTurnColor, targetPos)
    if (destGlobal != null && destGlobal !in SAFE_GLOBAL_INDICES) {
      for (otherIdx in 0 until 16) {
        val otherColor = colorForTokenIndex(otherIdx)
        if (otherColor != currentTurnColor) {
          val otherPos = mutableTokens[otherIdx]
          val otherGlobal = toGlobalTrackIndex(otherColor, otherPos)
          if (otherGlobal == destGlobal) {
            capturedIndices.add(otherIdx)
          }
        }
      }
      // If opponent had only 1 token (not a blockade), capture it back to yard
      if (capturedIndices.size == 1) {
        for (capIdx in capturedIndices) {
          mutableTokens[capIdx] = YARD_POS
        }
      } else {
        capturedIndices.clear()
      }
    }

    val reachedHome = targetPos == HOME_GOAL_POS
    val playerTokens = tokenIndicesForColor(currentTurnColor).map { mutableTokens[it] }
    val playerFinishedAll = playerTokens.all { it == HOME_GOAL_POS }

    val updatedWinners = currentWinnerOrder.toMutableList()
    if (playerFinishedAll && currentTurnColor.name !in updatedWinners) {
      updatedWinners.add(currentTurnColor.name)
    }
    val updatedFirstWinner = if (currentWinnerColor == "NONE" && updatedWinners.isNotEmpty()) {
      updatedWinners.first()
    } else {
      currentWinnerColor
    }

    val activeColors = if (seats.isNotEmpty()) {
      seats.map { it.color }
    } else {
      PlayerColor.entries
    }
    val unfinishedColors = activeColors.filter { c ->
      c.name !in updatedWinners && tokenIndicesForColor(c).any { mutableTokens[it] < HOME_GOAL_POS }
    }

    val isGameOver = unfinishedColors.size <= 1 && activeColors.size >= 2 && updatedWinners.isNotEmpty()
    if (isGameOver) {
      for (remaining in unfinishedColors) {
        if (remaining.name !in updatedWinners) {
          updatedWinners.add(remaining.name)
        }
      }
    }

    val bonusRoll = !isGameOver && !playerFinishedAll && (
      diceValue == 6 || capturedIndices.isNotEmpty() || reachedHome
    )

    val nextColor = if (bonusRoll) {
      currentTurnColor
    } else {
      nextActiveColor(currentTurnColor, activeColors, updatedWinners)
    }

    val nextPhase = if (isGameOver) TurnPhase.GAME_OVER else TurnPhase.ROLL
    val nextSixes = if (bonusRoll && diceValue == 6) consecutiveSixes else 0

    val playerLabel = seats.firstOrNull { it.color == currentTurnColor }?.name ?: currentTurnColor.label
    val eventText = when {
      isGameOver -> "$playerLabel wins the match!"
      playerFinishedAll -> "$playerLabel finished all 4 tokens!"
      capturedIndices.isNotEmpty() -> {
        val capturedColor = colorForTokenIndex(capturedIndices.first())
        "$playerLabel captured ${capturedColor.label}! Bonus roll"
      }
      reachedHome -> "$playerLabel guided a token Home! Bonus roll"
      startPos == YARD_POS -> "$playerLabel deployed a token onto the board! Bonus roll"
      diceValue == 6 -> "$playerLabel rolled a 6! Bonus roll"
      else -> "$playerLabel advanced $diceValue ${if (diceValue == 1) "step" else "steps"}"
    }

    return MoveOutcome(
      updatedTokens = mutableTokens,
      pathSteps = pathSteps,
      capturedTokenIndices = capturedIndices,
      reachedHome = reachedHome,
      playerFinishedAll = playerFinishedAll,
      bonusRollGranted = bonusRoll,
      nextTurnColor = nextColor,
      nextTurnPhase = nextPhase,
      nextConsecutiveSixes = nextSixes,
      winnerColor = updatedFirstWinner,
      updatedWinnerOrder = updatedWinners,
      eventDescription = eventText
    )
  }

  fun nextActiveColor(
    current: PlayerColor,
    activeSeats: List<PlayerColor>,
    finishedColors: List<String>
  ): PlayerColor {
    val order = PlayerColor.entries
    val startIdx = order.indexOf(current)
    for (offset in 1..4) {
      val candidate = order[(startIdx + offset) % 4]
      if (candidate in activeSeats && candidate.name !in finishedColors) {
        return candidate
      }
    }
    return current
  }

  /**
   * Tactical AI evaluator that scores legal moves based on:
   * 1. Capturing an opponent token (+120)
   * 2. Reaching the exact Home Triangle (+100)
   * 3. Exiting the Home Yard on a 6 (+85)
   * 4. Landing on a Safe Star / Start square or forming a same-color Blockade (+65)
   * 5. Escaping danger when an opponent is 1..6 tiles behind (+50)
   * 6. Advancing the furthest-ahead token closer to home (+progress)
   */
  fun chooseBestMove(
    tokens: List<Int>,
    color: PlayerColor,
    diceValue: Int
  ): Int? {
    val legalIndices = getLegalMoveTokenIndices(tokens, color, diceValue)
    if (legalIndices.isEmpty()) return null
    if (legalIndices.size == 1) return legalIndices.first()

    return legalIndices.maxByOrNull { idx ->
      scoreMove(tokens, color, idx, diceValue)
    }
  }

  private fun scoreMove(
    tokens: List<Int>,
    color: PlayerColor,
    tokenIndex: Int,
    diceValue: Int
  ): Int {
    val currentPos = tokens[tokenIndex]
    val targetPos = if (currentPos == YARD_POS) 0 else currentPos + diceValue
    var score = 0

    if (currentPos == YARD_POS) {
      score += 85
    }
    if (targetPos == HOME_GOAL_POS) {
      score += 110
    }
    if (targetPos in 51..55 && currentPos <= MAX_MAIN_TRACK_REL) {
      score += 75 // Entering safe home stretch
    }

    val targetGlobal = toGlobalTrackIndex(color, targetPos)
    if (targetGlobal != null) {
      // Check if captures an opponent
      if (targetGlobal !in SAFE_GLOBAL_INDICES) {
        val opponentCount = (0 until 16).count { otherIdx ->
          val otherColor = colorForTokenIndex(otherIdx)
          otherColor != color && toGlobalTrackIndex(otherColor, tokens[otherIdx]) == targetGlobal
        }
        if (opponentCount == 1) {
          score += 130
        }
      }

      // Check if lands on safe star/start square
      if (targetGlobal in SAFE_GLOBAL_INDICES) {
        score += 60
      }

      // Check if forms a same-color blockade
      val friendlyOnTarget = tokenIndicesForColor(color).count { friendIdx ->
        friendIdx != tokenIndex && toGlobalTrackIndex(color, tokens[friendIdx]) == targetGlobal
      }
      if (friendlyOnTarget >= 1) {
        score += 55
      }
    }

    // Prefer advancing tokens closer to home
    score += targetPos.coerceAtLeast(0)
    return score
  }
}
