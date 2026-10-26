package com.example

import com.example.data.PlayerColor
import com.example.data.PlayerSeat
import com.example.domain.LudoRulesEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  private val defaultSeats = listOf(
    PlayerSeat(PlayerColor.RED, "u1", "Red"),
    PlayerSeat(PlayerColor.GREEN, "u2", "Green"),
    PlayerSeat(PlayerColor.YELLOW, "u3", "Yellow"),
    PlayerSeat(PlayerColor.BLUE, "u4", "Blue")
  )

  @Test
  fun yardExit_requiresRollingSix() {
    val initialTokens = List(16) { -1 }
    assertTrue(LudoRulesEngine.getLegalMoveTokenIndices(initialTokens, PlayerColor.RED, 5).isEmpty())
    assertEquals(listOf(0, 1, 2, 3), LudoRulesEngine.getLegalMoveTokenIndices(initialTokens, PlayerColor.RED, 6))
  }

  @Test
  fun blockade_twoSameColorOpponentTokensBlockPath() {
    val tokens = MutableList(16) { -1 }
    // Place Red token 0 at relative pos 10 (global track 10)
    tokens[0] = 10
    // Place two Green tokens at relative pos 0 (global track 13, Green's start)
    tokens[4] = 0
    tokens[5] = 0

    // Rolling 4 from pos 10 would try to pass through global track 13 (blocked by 2 Green tokens)
    assertFalse(LudoRulesEngine.canTokenMove(tokens, tokenIndex = 0, diceValue = 4))
    // Rolling 2 from pos 10 lands on global track 12 (before blockade) -> legal
    assertTrue(LudoRulesEngine.canTokenMove(tokens, tokenIndex = 0, diceValue = 2))
  }

  @Test
  fun captureAndSafeStar_capturesOnNormalTileButNotOnSafeStar() {
    val tokens = MutableList(16) { -1 }
    // Red token 0 at relative pos 2 (global 2)
    tokens[0] = 2
    // Blue token 12 at relative pos 17 -> global (39 + 17) % 52 = 4 (non-safe square)
    tokens[12] = 17

    val outcome = LudoRulesEngine.executeMove(
      tokens = tokens,
      seats = defaultSeats,
      currentTurnColor = PlayerColor.RED,
      tokenIndex = 0,
      diceValue = 2,
      consecutiveSixes = 0,
      currentWinnerColor = "NONE",
      currentWinnerOrder = emptyList()
    )

    assertEquals(listOf(12), outcome.capturedTokenIndices)
    assertEquals(-1, outcome.updatedTokens[12])
    assertTrue(outcome.bonusRollGranted)
    assertEquals(PlayerColor.RED, outcome.nextTurnColor)
  }
}
