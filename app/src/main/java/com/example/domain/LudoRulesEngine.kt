package com.example.domain

import com.example.data.PlayerColor
import com.example.data.PlayerSeat
import com.example.data.TurnPhase

data class MoveOutcome(
  val updatedTokens: List<Int>,
  val pathSteps: List<Int>,
  val capturedTokenIndices: List<Int>,
  val reachedHome: Boolean,
  val formedBlockade: Boolean,
  val destinationGlobalIndex: Int?,
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
  const val TILES_PER_ARM = 13
  const val MAX_MAIN_TRACK_REL = 50
  const val HOME_GOAL_POS = 56
  const val OUTER_TRACK_SIZE = 52

  val SAFE_GLOBAL_INDICES = setOf(0, 8, 13, 21, 26, 34, 39, 47)
  val STAR_GLOBAL_INDICES = setOf(8, 21, 34, 47)

  fun boardArmCountForPlayers(maxPlayersOrSeats: Int): Int {
    return when {
      maxPlayersOrSeats <= 4 -> 4
      maxPlayersOrSeats <= 6 -> 6
      else -> 8
    }
  }

  fun outerTrackSize(armCount: Int = 4): Int = armCount.coerceIn(4, 8) * TILES_PER_ARM

  fun maxMainTrackRel(armCount: Int = 4): Int = outerTrackSize(armCount) - 2

  fun homeGoalPos(armCount: Int = 4): Int = maxMainTrackRel(armCount) + 6

  fun safeGlobalIndices(armCount: Int = 4): Set<Int> {
    val arms = armCount.coerceIn(4, 8)
    val set = mutableSetOf<Int>()
    for (arm in 0 until arms) {
      set.add(arm * TILES_PER_ARM)
      set.add(arm * TILES_PER_ARM + 8)
    }
    return set
  }

  fun starGlobalIndices(armCount: Int = 4): Set<Int> {
    val arms = armCount.coerceIn(4, 8)
    return (0 until arms).map { it * TILES_PER_ARM + 8 }.toSet()
  }

  fun armIndexForColor(
    color: PlayerColor,
    seats: List<PlayerSeat> = emptyList(),
    armCount: Int = 4
  ): Int {
    val arms = armCount.coerceIn(4, 8)
    if (seats.isEmpty()) {
      return color.ordinal % arms
    }
    val allWithinStandardRange = seats.all { it.color.ordinal < arms }
    if (allWithinStandardRange && seats.map { it.color }.distinct().size == seats.size) {
      return color.ordinal % arms
    }
    val seatIdx = seats.indexOfFirst { it.color == color }
    if (seatIdx < 0) return color.ordinal % arms
    return if (seats.size == 2 && arms == 4) {
      if (seatIdx == 0) 0 else 2
    } else {
      seatIdx % arms
    }
  }

  fun startTrackIndexForColor(
    color: PlayerColor,
    seats: List<PlayerSeat> = emptyList(),
    armCount: Int = 4
  ): Int {
    return armIndexForColor(color, seats, armCount) * TILES_PER_ARM
  }

  fun tokenIndicesForColor(color: PlayerColor): IntRange {
    val start = color.ordinal * 4
    return start until (start + 4)
  }

  fun colorForTokenIndex(tokenIndex: Int): PlayerColor {
    return PlayerColor.entries[(tokenIndex / 4).coerceIn(0, PlayerColor.entries.size - 1)]
  }

  fun toGlobalTrackIndex(
    color: PlayerColor,
    relativePos: Int,
    seats: List<PlayerSeat> = emptyList(),
    armCount: Int = 4
  ): Int? {
    val maxRel = maxMainTrackRel(armCount)
    if (relativePos !in 0..maxRel) return null
    val startIdx = startTrackIndexForColor(color, seats, armCount)
    return (startIdx + relativePos) % outerTrackSize(armCount)
  }

  /**
   * Returns a map of global track square -> set of PlayerColors that have >= 2 tokens
   * stacked on that square (forming an impassable blockade for other colors).
   */
  fun getActiveBlockades(
    tokens: List<Int>,
    seats: List<PlayerSeat> = emptyList(),
    armCount: Int = 4
  ): Map<Int, Set<PlayerColor>> {
    val countsBySquareAndColor = mutableMapOf<Int, MutableMap<PlayerColor, Int>>()
    val maxTokenIdx = tokens.size.coerceAtMost(32)
    for (tokenIndex in 0 until maxTokenIdx) {
      val pos = tokens.getOrElse(tokenIndex) { YARD_POS }
      val color = colorForTokenIndex(tokenIndex)
      val globalIdx = toGlobalTrackIndex(color, pos, seats, armCount) ?: continue
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
   */
  fun canTokenMove(
    tokens: List<Int>,
    tokenIndex: Int,
    diceValue: Int,
    seats: List<PlayerSeat> = emptyList(),
    armCount: Int = 4
  ): Boolean {
    if (diceValue !in 1..6) return false
    val color = colorForTokenIndex(tokenIndex)
    val currentPos = tokens.getOrElse(tokenIndex) { YARD_POS }
    val goalPos = homeGoalPos(armCount)

    if (currentPos >= goalPos) return false

    val blockades = getActiveBlockades(tokens, seats, armCount)

    if (currentPos == YARD_POS) {
      if (diceValue != 6) return false
      val startGlobal = startTrackIndexForColor(color, seats, armCount)
      val blockingColors = blockades[startGlobal].orEmpty()
      return blockingColors.none { it != color }
    }

    val targetPos = currentPos + diceValue
    if (targetPos > goalPos) return false

    // Check every step along the path for opponent blockades on the outer track
    for (stepPos in (currentPos + 1)..targetPos) {
      val globalStep = toGlobalTrackIndex(color, stepPos, seats, armCount)
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
    diceValue: Int,
    seats: List<PlayerSeat> = emptyList(),
    armCount: Int = 4
  ): List<Int> {
    return tokenIndicesForColor(color).filter { idx ->
      idx < tokens.size && canTokenMove(tokens, idx, diceValue, seats, armCount)
    }
  }

  /**
   * Executes a move for [tokenIndex] by [diceValue] and computes captures, blockades,
   * bonus rolls, turn transitions, and win state.
   */
  fun executeMove(
    tokens: List<Int>,
    seats: List<PlayerSeat>,
    currentTurnColor: PlayerColor,
    tokenIndex: Int,
    diceValue: Int,
    consecutiveSixes: Int,
    currentWinnerColor: String,
    currentWinnerOrder: List<String>,
    armCount: Int = boardArmCountForPlayers(seats.size.coerceAtLeast(4))
  ): MoveOutcome {
    val goalPos = homeGoalPos(armCount)
    val safeIndices = safeGlobalIndices(armCount)
    val mutableTokens = tokens.toMutableList()
    val startPos = mutableTokens.getOrElse(tokenIndex) { YARD_POS }
    val targetPos = if (startPos == YARD_POS) 0 else (startPos + diceValue).coerceAtMost(goalPos)

    val pathSteps = if (startPos == YARD_POS) {
      listOf(0)
    } else {
      ((startPos + 1)..targetPos).toList()
    }

    mutableTokens[tokenIndex] = targetPos

    // Check for captures on the destination square (only on outer track and non-safe squares)
    val capturedIndices = mutableListOf<Int>()
    val destGlobal = toGlobalTrackIndex(currentTurnColor, targetPos, seats, armCount)
    if (destGlobal != null && destGlobal !in safeIndices) {
      for (otherIdx in mutableTokens.indices) {
        val otherColor = colorForTokenIndex(otherIdx)
        if (otherColor != currentTurnColor) {
          val otherPos = mutableTokens[otherIdx]
          val otherGlobal = toGlobalTrackIndex(otherColor, otherPos, seats, armCount)
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

    // Check if this move formed a same-color blockade on the destination outer track square
    val formedBlockade = if (destGlobal != null) {
      val friendlyOnDest = tokenIndicesForColor(currentTurnColor).count { idx ->
        idx < mutableTokens.size && toGlobalTrackIndex(currentTurnColor, mutableTokens[idx], seats, armCount) == destGlobal
      }
      friendlyOnDest >= 2
    } else {
      false
    }

    val reachedHome = targetPos == goalPos
    val playerTokens = tokenIndicesForColor(currentTurnColor).map { mutableTokens.getOrElse(it) { YARD_POS } }
    val playerFinishedAll = playerTokens.all { it == goalPos }

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
      PlayerColor.entries.take(4)
    }
    val unfinishedColors = activeColors.filter { c ->
      c.name !in updatedWinners && tokenIndicesForColor(c).any { mutableTokens.getOrElse(it) { YARD_POS } < goalPos }
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
      playerFinishedAll -> "$playerLabel finished all 4 pawns!"
      capturedIndices.isNotEmpty() -> {
        val capturedColor = colorForTokenIndex(capturedIndices.first())
        "$playerLabel captured ${capturedColor.label}! +25 pts & Bonus roll"
      }
      reachedHome -> "$playerLabel guided a pawn Home! +15 pts & Bonus roll"
      formedBlockade -> "$playerLabel formed a golden Blockade shield!"
      startPos == YARD_POS -> "$playerLabel deployed a pawn onto the board!"
      diceValue == 6 -> "$playerLabel moved 6 steps! Bonus roll"
      else -> "$playerLabel advanced $diceValue ${if (diceValue == 1) "step" else "steps"}"
    }

    return MoveOutcome(
      updatedTokens = mutableTokens,
      pathSteps = pathSteps,
      capturedTokenIndices = capturedIndices,
      reachedHome = reachedHome,
      formedBlockade = formedBlockade,
      destinationGlobalIndex = destGlobal,
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
    if (activeSeats.isEmpty()) return current
    val currentIdx = activeSeats.indexOf(current)
    if (currentIdx >= 0) {
      for (offset in 1..activeSeats.size) {
        val candidate = activeSeats[(currentIdx + offset) % activeSeats.size]
        if (candidate.name !in finishedColors) {
          return candidate
        }
      }
    }
    val allEntries = PlayerColor.entries
    val startOrd = allEntries.indexOf(current).coerceAtLeast(0)
    for (offset in 1..allEntries.size) {
      val candidate = allEntries[(startOrd + offset) % allEntries.size]
      if (candidate in activeSeats && candidate.name !in finishedColors) {
        return candidate
      }
    }
    return current
  }

  /**
   * Calculates leaderboard rank points earned or deducted for a match:
   * - Win: +100 pts
   * - Capture: +25 pts each
   * - Pawn reaching Home: +15 pts each
   * - Loss: -15 pts
   * - Forfeit: -25 pts
   */
  fun calculateMatchScoreDelta(
    won: Boolean,
    forfeited: Boolean = false,
    capturesInMatch: Int = 0,
    homesReachedInMatch: Int = 0
  ): Int {
    val winOrLossPts = when {
      forfeited -> -25
      won -> 100
      else -> -15
    }
    val capturePts = capturesInMatch.coerceAtLeast(0) * 25
    val homePts = homesReachedInMatch.coerceAtLeast(0) * 15
    return winOrLossPts + capturePts + homePts
  }

  /**
   * Tactical AI evaluator for a single die.
   */
  fun chooseBestMove(
    tokens: List<Int>,
    color: PlayerColor,
    diceValue: Int,
    seats: List<PlayerSeat> = emptyList(),
    armCount: Int = 4
  ): Int? {
    val legalIndices = getLegalMoveTokenIndices(tokens, color, diceValue, seats, armCount)
    if (legalIndices.isEmpty()) return null
    if (legalIndices.size == 1) return legalIndices.first()

    return legalIndices.maxByOrNull { idx ->
      scoreMove(tokens, color, idx, diceValue, seats, armCount)
    }
  }

  /**
   * Tactical AI evaluator for dual-dice: selects which available die (0 or 1) and token
   * produces the highest tactical value.
   */
  fun chooseBestDieAndMove(
    tokens: List<Int>,
    color: PlayerColor,
    diceValues: List<Int>,
    diceUsed: List<Boolean>,
    seats: List<PlayerSeat> = emptyList(),
    armCount: Int = 4
  ): Pair<Int, Int>? {
    var bestPair: Pair<Int, Int>? = null
    var bestScore = Int.MIN_VALUE

    for (dieIdx in 0..1) {
      if (diceUsed.getOrElse(dieIdx) { true }) continue
      val dVal = diceValues.getOrElse(dieIdx) { 0 }
      val legal = getLegalMoveTokenIndices(tokens, color, dVal, seats, armCount)
      for (tokIdx in legal) {
        // Prioritize playing a 6 first if pawns are in the yard so the 2nd die can move the deployed pawn
        val s = scoreMove(tokens, color, tokIdx, dVal, seats, armCount) +
          (if (dVal == 6 && tokens.getOrElse(tokIdx) { YARD_POS } == YARD_POS) 40 else 0)
        if (s > bestScore) {
          bestScore = s
          bestPair = dieIdx to tokIdx
        }
      }
    }
    return bestPair
  }

  private fun scoreMove(
    tokens: List<Int>,
    color: PlayerColor,
    tokenIndex: Int,
    diceValue: Int,
    seats: List<PlayerSeat>,
    armCount: Int
  ): Int {
    val goalPos = homeGoalPos(armCount)
    val maxMainRel = maxMainTrackRel(armCount)
    val safeIndices = safeGlobalIndices(armCount)
    val currentPos = tokens.getOrElse(tokenIndex) { YARD_POS }
    val targetPos = if (currentPos == YARD_POS) 0 else currentPos + diceValue
    var score = 0

    if (currentPos == YARD_POS) {
      score += 85
    }
    if (targetPos == goalPos) {
      score += 110
    }
    if (targetPos in (maxMainRel + 1) until goalPos && currentPos <= maxMainRel) {
      score += 75
    }

    val targetGlobal = toGlobalTrackIndex(color, targetPos, seats, armCount)
    if (targetGlobal != null) {
      if (targetGlobal !in safeIndices) {
        val opponentCount = tokens.indices.count { otherIdx ->
          val otherColor = colorForTokenIndex(otherIdx)
          otherColor != color && toGlobalTrackIndex(otherColor, tokens[otherIdx], seats, armCount) == targetGlobal
        }
        if (opponentCount == 1) {
          score += 130
        }
      }

      if (targetGlobal in safeIndices) {
        score += 60
      }

      val friendlyOnTarget = tokenIndicesForColor(color).count { friendIdx ->
        friendIdx != tokenIndex &&
          friendIdx < tokens.size &&
          toGlobalTrackIndex(color, tokens[friendIdx], seats, armCount) == targetGlobal
      }
      if (friendlyOnTarget >= 1) {
        score += 55
      }
    }

    score += targetPos.coerceAtLeast(0)
    return score
  }

  fun computePlayerHudStats(
    tokens: List<Int>,
    seat: PlayerSeat,
    currentTurnColor: PlayerColor,
    armCount: Int = 4
  ): InGamePlayerHudStats {
    val goalPos = homeGoalPos(armCount)
    val maxMainRel = maxMainTrackRel(armCount)
    val maxTilesPerPawn = goalPos + 1 // 1 step to exit yard onto 0 + goalPos steps to Home
    val totalPossibleTiles = maxTilesPerPawn * 4

    val tokenIndices = tokenIndicesForColor(seat.color)
    var inYard = 0
    var activeOnBoard = 0
    var inHomeStretch = 0
    var atHomeGoal = 0
    var remainingTiles = 0

    for (idx in tokenIndices) {
      val pos = tokens.getOrElse(idx) { YARD_POS }
      when {
        pos == YARD_POS -> {
          inYard += 1
          remainingTiles += maxTilesPerPawn
        }
        pos >= goalPos -> {
          atHomeGoal += 1
        }
        pos > maxMainRel -> {
          activeOnBoard += 1
          inHomeStretch += 1
          remainingTiles += (goalPos - pos).coerceAtLeast(0)
        }
        else -> {
          activeOnBoard += 1
          remainingTiles += (goalPos - pos).coerceAtLeast(0)
        }
      }
    }

    val completedTiles = (totalPossibleTiles - remainingTiles).coerceIn(0, totalPossibleTiles)
    val progressPct = if (totalPossibleTiles > 0) {
      ((completedTiles * 100f) / totalPossibleTiles.toFloat()).toInt().coerceIn(0, 100)
    } else {
      0
    }

    return InGamePlayerHudStats(
      color = seat.color,
      playerName = seat.name,
      isBot = seat.isBot,
      isCurrentTurn = seat.color == currentTurnColor,
      pawnsInYard = inYard,
      activePawnsOnBoard = activeOnBoard,
      pawnsInHomeStretch = inHomeStretch,
      pawnsAtHomeGoal = atHomeGoal,
      totalRemainingTilesToVictory = remainingTiles,
      raceProgressPercent = progressPct
    )
  }

  fun computeHudTelemetry(
    tokens: List<Int>,
    seats: List<PlayerSeat>,
    currentTurnColor: PlayerColor,
    turnPhase: TurnPhase,
    turnNumber: Int,
    diceValue: Int,
    diceValue2: Int,
    dice1Used: Boolean,
    dice2Used: Boolean,
    selectedDieIndex: Int,
    legalMoveTokenIndices: List<Int>,
    armCount: Int = 4
  ): InGameHudTelemetry {
    val resolvedSeats = seats.ifEmpty {
      listOf(PlayerSeat(currentTurnColor, "HUMAN_0", currentTurnColor.label, isBot = false))
    }
    val allStats = resolvedSeats.map { seat ->
      computePlayerHudStats(
        tokens = tokens,
        seat = seat,
        currentTurnColor = currentTurnColor,
        armCount = armCount
      )
    }
    val activeStats = allStats.firstOrNull { it.color == currentTurnColor } ?: allStats.first()

    val remainingDiceMoves = when (turnPhase) {
      TurnPhase.ROLL -> 2
      TurnPhase.MOVE -> (if (!dice1Used) 1 else 0) + (if (!dice2Used) 1 else 0)
      TurnPhase.GAME_OVER -> 0
    }

    val remainingDicePips = when (turnPhase) {
      TurnPhase.MOVE -> (if (!dice1Used) diceValue else 0) + (if (!dice2Used) diceValue2 else 0)
      else -> 0
    }

    val phaseBadge = when (turnPhase) {
      TurnPhase.ROLL -> "ROLL 2 DICE"
      TurnPhase.MOVE -> if (remainingDiceMoves >= 2) "MOVE 1/2" else "MOVE 2/2"
      TurnPhase.GAME_OVER -> "MATCH FINISHED"
    }

    val activeDieVal = if (selectedDieIndex == 0) diceValue else diceValue2
    val activeDieLabel = when (turnPhase) {
      TurnPhase.ROLL -> "Awaiting Dual-Dice Roll"
      TurnPhase.MOVE -> "Die #${selectedDieIndex + 1} (+$activeDieVal)"
      TurnPhase.GAME_OVER -> "Victory Complete"
    }

    return InGameHudTelemetry(
      currentTurnColor = currentTurnColor,
      currentTurnPlayerName = activeStats.playerName,
      turnNumber = turnNumber,
      turnPhase = turnPhase,
      turnPhaseBadge = phaseBadge,
      activePlayerStats = activeStats,
      allPlayerStats = allStats,
      remainingDiceMovesInTurn = remainingDiceMoves,
      remainingDicePipsInTurn = remainingDicePips,
      legalMovablePawnCount = legalMoveTokenIndices.size,
      activeDieLabel = activeDieLabel
    )
  }
}

data class InGamePlayerHudStats(
  val color: PlayerColor,
  val playerName: String,
  val isBot: Boolean,
  val isCurrentTurn: Boolean,
  val pawnsInYard: Int,
  val activePawnsOnBoard: Int,
  val pawnsInHomeStretch: Int,
  val pawnsAtHomeGoal: Int,
  val totalRemainingTilesToVictory: Int,
  val raceProgressPercent: Int
)

data class InGameHudTelemetry(
  val currentTurnColor: PlayerColor,
  val currentTurnPlayerName: String,
  val turnNumber: Int,
  val turnPhase: TurnPhase,
  val turnPhaseBadge: String,
  val activePlayerStats: InGamePlayerHudStats,
  val allPlayerStats: List<InGamePlayerHudStats>,
  val remainingDiceMovesInTurn: Int,
  val remainingDicePipsInTurn: Int,
  val legalMovablePawnCount: Int,
  val activeDieLabel: String
)

