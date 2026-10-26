package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlayerColor
import com.example.data.PlayerSeat
import com.example.data.RoomStatus
import com.example.data.TurnPhase
import com.example.domain.LudoRulesEngine
import com.example.ui.board3d.Interactive3DDiceCube
import com.example.ui.board3d.Interactive3DLudoBoard
import com.example.ui.board3d.paletteForColor
import com.example.ui.theme.BrushedBrass
import com.example.ui.viewmodel.LudoGameViewModel
import com.example.ui.viewmodel.LudoUiState

private val QUICK_EMOJIS = listOf("🎲", "🔥", "👏", "😮", "😅", "👑")

@Composable
fun GameArenaScreen(
  uiState: LudoUiState,
  viewModel: LudoGameViewModel
) {
  BackHandler {
    viewModel.leaveToLobby()
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
      val isExpandedLayout = maxWidth >= 720.dp

      if (isExpandedLayout) {
        // Canonical Supporting Pane + 3D Viewport layout for Tablets/Foldables/Landscape
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Column(
            modifier = Modifier
              .width(300.dp)
              .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            ArenaTopBar(uiState = uiState, viewModel = viewModel)
            PlayerSeatsGrid(uiState = uiState)
            EmojiReactionStrip(
              activeEmoji = uiState.activeReactionEmoji,
              activeSender = uiState.activeReactionSender,
              onEmojiSelected = { viewModel.sendQuickEmojiReaction(it) }
            )
            DiceAndTurnControlDock(uiState = uiState, viewModel = viewModel)
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          ) {
            BoardViewportWithBanner(uiState = uiState, viewModel = viewModel)
          }
        }
      } else {
        // Portrait Mobile Layout
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          ArenaTopBar(uiState = uiState, viewModel = viewModel)

          Spacer(modifier = Modifier.height(6.dp))

          PlayerSeatsGrid(uiState = uiState)

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
          ) {
            BoardViewportWithBanner(uiState = uiState, viewModel = viewModel)
          }

          EmojiReactionStrip(
            activeEmoji = uiState.activeReactionEmoji,
            activeSender = uiState.activeReactionSender,
            onEmojiSelected = { viewModel.sendQuickEmojiReaction(it) }
          )

          Spacer(modifier = Modifier.height(6.dp))

          DiceAndTurnControlDock(uiState = uiState, viewModel = viewModel)
        }
      }

      // Victory Podium Overlay when match finishes
      if (uiState.roomStatus == RoomStatus.FINISHED || uiState.turnPhase == TurnPhase.GAME_OVER) {
        VictoryPodiumOverlay(
          uiState = uiState,
          onPlayAgain = {
            if (uiState.isOnlineMatch) {
              viewModel.leaveToLobby()
            } else {
              viewModel.startOfflineGame()
            }
          },
          onReturnLobby = { viewModel.leaveToLobby() }
        )
      }
    }
  }

  if (uiState.showRulesModal) {
    LiveLudoRulesDialog(onDismiss = { viewModel.toggleRulesModal(false) })
  }
}

@Composable
private fun ArenaTopBar(
  uiState: LudoUiState,
  viewModel: LudoGameViewModel
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      IconButton(
        onClick = { viewModel.leaveToLobby() },
        modifier = Modifier.testTag("back_to_lobby_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Leave Match"
        )
      }
      Column {
        Text(
          text = if (uiState.isOnlineMatch) "Room #${uiState.roomCode}" else "3D Tabletop Studio",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "Drag to Orbit 3D • Pinch to Zoom",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      IconButton(
        onClick = { viewModel.cycleCameraPerspective() },
        modifier = Modifier.testTag("cycle_camera_angle_button")
      ) {
        Icon(
          imageVector = Icons.Default.ViewInAr,
          contentDescription = "Cycle 3D Tilt Angle",
          tint = BrushedBrass
        )
      }

      IconButton(
        onClick = { viewModel.toggleAutoFocusTurnCamera() },
        modifier = Modifier.testTag("toggle_autofocus_camera_button")
      ) {
        Icon(
          imageVector = Icons.Default.CenterFocusStrong,
          contentDescription = "Auto-Focus Active Turn Camera",
          tint = if (uiState.autoFocusTurnCamera) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      IconButton(
        onClick = { viewModel.setCameraPreset(yaw = -28f, pitch = 52f, zoom = 1.04f) },
        modifier = Modifier.testTag("reset_camera_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = stringResource(R.string.reset_camera_desc),
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      IconButton(
        onClick = { viewModel.toggleRulesModal(true) },
        modifier = Modifier.testTag("arena_rules_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.MenuBook,
          contentDescription = stringResource(R.string.rules_button_label),
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun PlayerSeatsGrid(uiState: LudoUiState) {
  val seats = uiState.seats
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    val chunked = seats.chunked(2)
    chunked.forEach { rowSeats ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        rowSeats.forEach { seat ->
          PlayerCornerHudCard(
            seat = seat,
            isCurrentTurn = uiState.currentTurnColor == seat.color && uiState.turnPhase != TurnPhase.GAME_OVER,
            tokens = uiState.tokens,
            consecutiveSixes = if (uiState.currentTurnColor == seat.color) uiState.consecutiveSixes else 0,
            turnSecondsRemaining = uiState.turnSecondsRemaining,
            modifier = Modifier.weight(1f)
          )
        }
        if (rowSeats.size == 1) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun PlayerCornerHudCard(
  seat: PlayerSeat,
  isCurrentTurn: Boolean,
  tokens: List<Int>,
  consecutiveSixes: Int,
  turnSecondsRemaining: Int,
  modifier: Modifier = Modifier
) {
  val (primary, _, softTone) = paletteForColor(seat.color)
  val tokenRange = LudoRulesEngine.tokenIndicesForColor(seat.color)
  val homeCount = tokenRange.count { tokens.getOrElse(it) { -1 } == LudoRulesEngine.HOME_GOAL_POS }
  val activeCount = tokenRange.count { tokens.getOrElse(it) { -1 } in 0..55 }

  Card(
    modifier = modifier
      .border(
        width = if (isCurrentTurn) 2.dp else 1.dp,
        color = if (isCurrentTurn) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp)
      ),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCurrentTurn) softTone.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(primary)
          )
          Text(
            text = seat.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Text(
          text = "$homeCount/4 Home",
          style = MaterialTheme.typography.labelMedium,
          color = primary,
          fontWeight = FontWeight.Bold
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (seat.isBot) "AI • $activeCount on track" else "Active: $activeCount",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isCurrentTurn) {
          Text(
            text = if (consecutiveSixes > 0) "${consecutiveSixes}×6 • ${turnSecondsRemaining}s" else "${turnSecondsRemaining}s",
            style = MaterialTheme.typography.labelMedium,
            color = primary,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      if (isCurrentTurn) {
        LinearProgressIndicator(
          progress = {
            (turnSecondsRemaining.toFloat() / LudoUiState.TURN_DURATION_SECONDS.toFloat()).coerceIn(0f, 1f)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp)),
          color = primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
      }
    }
  }
}

@Composable
private fun BoardViewportWithBanner(
  uiState: LudoUiState,
  viewModel: LudoGameViewModel
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Interactive3DLudoBoard(
      tokens = uiState.tokens,
      legalMoveTokenIndices = uiState.legalMoveTokenIndices,
      activeBlockades = uiState.activeBlockades,
      animatedToken = uiState.animatedToken,
      yawDegrees = uiState.cameraYaw,
      pitchDegrees = uiState.cameraPitch,
      zoom = uiState.cameraZoom,
      onCameraTransform = { dYaw, dPitch, zoomFactor ->
        viewModel.adjustCamera(dYaw, dPitch, zoomFactor)
      },
      onTokenTapped = { tokenIndex ->
        viewModel.onTokenSelected(tokenIndex)
      },
      onBoardCenterTapped = {
        viewModel.onRollDiceClicked()
      },
      modifier = Modifier.fillMaxSize()
    )

    // Architectural status toast pill at top of 3D viewport
    val (turnColorPrimary, _, _) = paletteForColor(uiState.currentTurnColor)
    Box(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 6.dp)
        .clip(RoundedCornerShape(50))
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
        .border(1.dp, turnColorPrimary.copy(alpha = 0.6f), RoundedCornerShape(50))
        .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
      Text(
        text = uiState.statusBanner,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    // Floating Emoji Reaction Bubble
    AnimatedVisibility(
      visible = uiState.activeReactionEmoji.isNotBlank(),
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, BrushedBrass, RoundedCornerShape(50))
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(text = uiState.activeReactionEmoji, style = MaterialTheme.typography.titleLarge)
        Text(
          text = uiState.activeReactionSender,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun EmojiReactionStrip(
  activeEmoji: String,
  activeSender: String,
  onEmojiSelected: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surface)
      .padding(horizontal = 10.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    QUICK_EMOJIS.forEach { emoji ->
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .clickable { onEmojiSelected(emoji) }
          .testTag("emoji_reaction_$emoji"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = emoji,
          style = MaterialTheme.typography.titleLarge
        )
      }
    }
  }
}

@Composable
private fun DiceAndTurnControlDock(
  uiState: LudoUiState,
  viewModel: LudoGameViewModel
) {
  val canAct = viewModel.canCurrentUserAct()
  val canRoll = canAct && uiState.turnPhase == TurnPhase.ROLL
  val (turnColor, _, _) = paletteForColor(uiState.currentTurnColor)
  val activeSeat = uiState.seats.firstOrNull { it.color == uiState.currentTurnColor }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.5.dp, turnColor.copy(alpha = 0.75f), RoundedCornerShape(22.dp)),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Interactive3DDiceCube(
        diceValue = uiState.diceValue,
        isRolling = uiState.isDiceRolling,
        activeColor = uiState.currentTurnColor,
        enabled = canRoll,
        onRollClicked = { viewModel.onRollDiceClicked() },
        sizeDp = 72.dp
      )

      Spacer(modifier = Modifier.width(14.dp))

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = "${activeSeat?.name ?: uiState.currentTurnColor.label} (${uiState.currentTurnColor.name})",
          style = MaterialTheme.typography.titleMedium,
          color = turnColor,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = when {
            uiState.isDiceRolling -> "Tumbling 3D die..."
            uiState.turnPhase == TurnPhase.MOVE && uiState.legalMoveTokenIndices.isNotEmpty() ->
              stringResource(R.string.select_token_prompt)
            canRoll -> "Tap the 3D die or button to roll"
            else -> "Waiting for ${activeSeat?.name ?: "player"}..."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      if (uiState.turnPhase == TurnPhase.MOVE && canAct && uiState.legalMoveTokenIndices.isNotEmpty()) {
        Button(
          onClick = {
            val best = LudoRulesEngine.chooseBestMove(
              tokens = uiState.tokens,
              color = uiState.currentTurnColor,
              diceValue = uiState.diceValue
            ) ?: uiState.legalMoveTokenIndices.first()
            viewModel.onTokenSelected(best)
          },
          modifier = Modifier
            .height(50.dp)
            .testTag("move_pawn_action_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = turnColor,
            contentColor = Color.White
          )
        ) {
          Text(
            text = "MOVE +${uiState.diceValue}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
        }
      } else {
        Button(
          onClick = { viewModel.onRollDiceClicked() },
          enabled = canRoll,
          modifier = Modifier
            .height(50.dp)
            .testTag("roll_dice_action_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = turnColor,
            contentColor = Color.White
          )
        ) {
          Text(
            text = stringResource(R.string.roll_dice_action),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun VictoryPodiumOverlay(
  uiState: LudoUiState,
  onPlayAgain: () -> Unit,
  onReturnLobby: () -> Unit
) {
  val winnerColor = PlayerColor.fromString(uiState.winnerColor)
  val (primary, _, _) = paletteForColor(winnerColor)
  val winnerSeat = uiState.seats.firstOrNull { it.color == winnerColor }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.65f))
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .border(2.dp, BrushedBrass, RoundedCornerShape(28.dp)),
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(BrushedBrass.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "Victory Trophy",
            tint = BrushedBrass,
            modifier = Modifier.size(36.dp)
          )
        }

        Text(
          text = "${winnerSeat?.name ?: winnerColor.label} Wins!",
          style = MaterialTheme.typography.headlineLarge,
          color = primary,
          textAlign = TextAlign.Center
        )

        Text(
          text = "All 4 pawns reached the center 3D Home Pyramid.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center
        )

        if (uiState.winnerOrder.isNotEmpty()) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            uiState.winnerOrder.forEachIndexed { idx, colorName ->
              val c = PlayerColor.fromString(colorName)
              val seatName = uiState.seats.firstOrNull { it.color == c }?.name ?: c.label
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "#${idx + 1} $seatName",
                  style = MaterialTheme.typography.titleSmall
                )
                Text(
                  text = c.label,
                  style = MaterialTheme.typography.labelMedium,
                  color = paletteForColor(c).first
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = onReturnLobby,
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("podium_return_lobby_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("Lobby")
          }

          Button(
            onClick = onPlayAgain,
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .testTag("podium_play_again_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text(if (uiState.isOnlineMatch) "New Match" else "Rematch")
          }
        }
      }
    }
  }
}
