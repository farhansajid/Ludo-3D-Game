package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AudioCallMode
import com.example.data.PlayerColor
import com.example.data.PlayerSeat
import com.example.data.RoomStatus
import com.example.data.TurnPhase
import com.example.domain.LudoRulesEngine
import com.example.ui.audio.AudioCallUiState
import com.example.ui.audio.HapticFeedbackManager
import com.example.ui.board3d.Interactive3DDiceCube
import com.example.ui.board3d.Interactive3DLudoBoard
import com.example.ui.board3d.ParticleSystemOverlay
import com.example.ui.board3d.paletteForColor
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.TerracottaRed
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

  val context = LocalContext.current
  val localView = LocalView.current
  val audioCallState by viewModel.audioCallState.collectAsStateWithLifecycle()
  var pendingCallAction by remember { mutableStateOf<(() -> Unit)?>(null) }

  LaunchedEffect(uiState.hapticPulseSequence) {
    val spec = uiState.activeHapticSpec
    if (uiState.hapticPulseSequence > 0L && uiState.isHapticsEnabled && spec != null) {
      HapticFeedbackManager.performHardwareHaptic(context, localView, spec)
    }
  }

  val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    viewModel.onMicPermissionResult(granted)
    pendingCallAction?.invoke()
    pendingCallAction = null
  }

  fun runWithMicPermission(action: () -> Unit) {
    val hasPerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    if (hasPerm) {
      viewModel.onMicPermissionResult(true)
      action()
    } else {
      pendingCallAction = action
      recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
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
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Column(
            modifier = Modifier
              .width(320.dp)
              .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            ArenaTopBar(uiState = uiState, viewModel = viewModel)
            if (uiState.isSpectating) {
              SpectatorModeBanner(
                uiState = uiState,
                onToggleAutoFollow = { viewModel.toggleAutoFocusTurnCamera() },
                onExitSpectate = { viewModel.leaveToLobby() }
              )
            } else {
              AudioCallControlStrip(
                audioCallState = audioCallState,
                onStartGroupCall = { runWithMicPermission { viewModel.startGroupAudioCall() } },
                onEndCall = { viewModel.endAudioCall() },
                onToggleMic = { viewModel.toggleMicMute() },
                onToggleSpeaker = { viewModel.toggleSpeaker() }
              )
            }
            PlayerSeatsGrid(
              uiState = uiState,
              audioCallState = audioCallState,
              currentUserId = viewModel.currentUserId,
              onStartDirectCall = { seat ->
                runWithMicPermission { viewModel.startOneToOneAudioCall(seat.uid, seat.name) }
              },
              onToggleMutePeer = { seat -> viewModel.toggleMutePeer(seat.uid) }
            )
            EmojiReactionStrip(
              isSpectating = uiState.isSpectating,
              equippedEmoteSymbol = uiState.equippedEmoteForMoment(uiState.selectedEmoteMomentSlot).stickerSymbol,
              onTrigger3DPawnEmote = { viewModel.triggerManualPawnEmote() },
              onOpenEmoteGallery = { viewModel.openEmoteGalleryScreen() },
              onEmojiSelected = { viewModel.sendQuickEmojiReaction(it) }
            )
            DualDiceAndTurnControlDock(uiState = uiState, viewModel = viewModel)
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
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          ArenaTopBar(uiState = uiState, viewModel = viewModel)

          Spacer(modifier = Modifier.height(4.dp))

          if (uiState.isSpectating) {
            SpectatorModeBanner(
              uiState = uiState,
              onToggleAutoFollow = { viewModel.toggleAutoFocusTurnCamera() },
              onExitSpectate = { viewModel.leaveToLobby() }
            )
          } else {
            AudioCallControlStrip(
              audioCallState = audioCallState,
              onStartGroupCall = { runWithMicPermission { viewModel.startGroupAudioCall() } },
              onEndCall = { viewModel.endAudioCall() },
              onToggleMic = { viewModel.toggleMicMute() },
              onToggleSpeaker = { viewModel.toggleSpeaker() }
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          PlayerSeatsGrid(
            uiState = uiState,
            audioCallState = audioCallState,
            currentUserId = viewModel.currentUserId,
            onStartDirectCall = { seat ->
              runWithMicPermission { viewModel.startOneToOneAudioCall(seat.uid, seat.name) }
            },
            onToggleMutePeer = { seat -> viewModel.toggleMutePeer(seat.uid) }
          )

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
          ) {
            BoardViewportWithBanner(uiState = uiState, viewModel = viewModel)
          }

          EmojiReactionStrip(
            isSpectating = uiState.isSpectating,
            equippedEmoteSymbol = uiState.equippedEmoteForMoment(uiState.selectedEmoteMomentSlot).stickerSymbol,
            onTrigger3DPawnEmote = { viewModel.triggerManualPawnEmote() },
            onOpenEmoteGallery = { viewModel.openEmoteGalleryScreen() },
            onEmojiSelected = { viewModel.sendQuickEmojiReaction(it) }
          )

          AnimatedVisibility(
            visible = uiState.isRoomChatOpen,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            Column {
              Spacer(modifier = Modifier.height(4.dp))
              PrivateRoomLiveChatCard(
                roomCode = uiState.roomCode.ifBlank { "PRV409" },
                isPublicRoom = uiState.isPublicRoom,
                messages = uiState.roomChatMessages,
                draftText = uiState.chatDraftText,
                profanityNotice = uiState.lastProfanityFilterNotice,
                currentUserId = viewModel.currentUserId,
                onDraftChanged = { viewModel.updateChatDraftText(it) },
                onSendClicked = { viewModel.sendRoomChatMessage() },
                onQuickReplyClicked = { viewModel.sendQuickReplyChat(it) },
                onDismissProfanityNotice = { viewModel.clearProfanityNotice() },
                onClosePanel = { viewModel.toggleRoomChat(false) },
                modifier = Modifier.testTag("private_room_arena_chat_panel")
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          DualDiceAndTurnControlDock(uiState = uiState, viewModel = viewModel)
        }
      }

      if (uiState.roomStatus == RoomStatus.FINISHED || uiState.turnPhase == TurnPhase.GAME_OVER) {
        VictoryPodiumOverlay(
          uiState = uiState,
          onPlayCelebrationFanfare = { viewModel.playVictoryCelebrationFanfare() },
          onWatchReplay = { viewModel.openReplayStudioWithCurrentMatch() },
          onPlayAgain = {
            if (uiState.isTournamentMatchActive) {
              viewModel.openWeeklyTournamentScreen()
            } else if (uiState.isOnlineMatch) {
              viewModel.leaveToLobby()
            } else {
              viewModel.startOfflineGame()
            }
          },
          onReturnLobby = {
            if (uiState.isTournamentMatchActive) {
              viewModel.openWeeklyTournamentScreen()
            } else {
              viewModel.leaveToLobby()
            }
          }
        )
      }
    }
  }

  if (uiState.showRulesModal) {
    LiveLudoRulesDialog(
      onDismiss = { viewModel.toggleRulesModal(false) },
      onStartInteractiveTutorial = { viewModel.openTutorialScreen() }
    )
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
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      IconButton(
        onClick = {
          if (uiState.isTournamentMatchActive) {
            viewModel.openWeeklyTournamentScreen()
          } else {
            viewModel.leaveToLobby()
          }
        },
        modifier = Modifier.testTag("back_to_lobby_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Leave Match"
        )
      }
      Column {
        Text(
          text = when {
            uiState.isSpectating -> "👁️ Spectating #${uiState.roomCode} (${uiState.seats.size}P)"
            uiState.isTournamentMatchActive -> "🏆 Sovereign Cup • ${uiState.tournamentCurrentRound.title}"
            uiState.isOnlineMatch -> "Room #${uiState.roomCode} (${uiState.seats.size}P)"
            else -> "${uiState.seats.size}P 3D Tabletop"
          },
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = when {
            uiState.isSpectating ->
              "View-Only Mode • Watching ${uiState.spectatingTargetName.ifBlank { "Live Match" }} • Emoji Reactions Enabled"
            uiState.isTournamentMatchActive ->
              "Ends in ${com.example.data.WeeklyTournamentCatalog.formatRemainingTime(uiState.tournamentRemainingSeconds)} • Ladder Match"
            else -> "2-Dice Sequential • Drag Orbit • Pinch Zoom"
          },
          style = MaterialTheme.typography.labelSmall,
          color = if (uiState.isSpectating || uiState.isTournamentMatchActive) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      // Pawn Movement Speed Selector Chip
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, BrushedBrass, RoundedCornerShape(50))
          .clickable { viewModel.cyclePawnMoveSpeed() }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("cycle_pawn_speed_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Speed,
          contentDescription = stringResource(R.string.speed_control_label),
          tint = BrushedBrass,
          modifier = Modifier.size(15.dp)
        )
        Text(
          text = uiState.pawnMoveSpeed.label,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      // Private Room Live Chat Toggle Button + Unread Badge
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(
            if (uiState.isRoomChatOpen) BrushedBrass.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surface
          )
          .border(1.dp, BrushedBrass, RoundedCornerShape(50))
          .clickable { viewModel.toggleRoomChat() }
          .padding(horizontal = 9.dp, vertical = 6.dp)
          .testTag("arena_chat_toggle_button"),
        contentAlignment = Alignment.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = "Toggle Private Room Chat",
            tint = BrushedBrass,
            modifier = Modifier.size(15.dp)
          )
          Text(
            text = if (uiState.unreadChatCount > 0) "Chat (${uiState.unreadChatCount})" else "Chat",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (uiState.isRoomChatOpen || uiState.unreadChatCount > 0) {
              BrushedBrass
            } else {
              MaterialTheme.colorScheme.onSurface
            }
          )
        }
      }

      // In-Game HUD Overlay Toggle Chip
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(
            if (uiState.isHudOverlayVisible) BrushedBrass.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surface
          )
          .border(1.dp, BrushedBrass, RoundedCornerShape(50))
          .clickable { viewModel.toggleHudOverlayVisibility() }
          .padding(horizontal = 9.dp, vertical = 6.dp)
          .testTag("arena_hud_toggle_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = if (uiState.isHudOverlayVisible) "HUD ON" else "HUD OFF",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (uiState.isHudOverlayVisible) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      IconButton(
        onClick = { viewModel.toggleSoundEnabled() },
        modifier = Modifier.testTag("arena_sound_toggle_button")
      ) {
        Icon(
          imageVector = if (uiState.isSoundEnabled) {
            Icons.AutoMirrored.Filled.VolumeUp
          } else {
            Icons.AutoMirrored.Filled.VolumeOff
          },
          contentDescription = stringResource(R.string.sound_toggle_label),
          tint = if (uiState.isSoundEnabled) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      IconButton(
        onClick = { viewModel.toggleHapticsEnabled() },
        modifier = Modifier.testTag("arena_haptics_toggle_button")
      ) {
        Icon(
          imageVector = Icons.Default.Vibration,
          contentDescription = if (uiState.isHapticsEnabled) "Haptic Feedback ON" else "Haptic Feedback OFF",
          tint = if (uiState.isHapticsEnabled) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

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
        onClick = { viewModel.openReplayStudioWithCurrentMatch() },
        modifier = Modifier.testTag("arena_open_replay_button")
      ) {
        Icon(
          imageVector = Icons.Default.VideoLibrary,
          contentDescription = "Open 3D Match Replay Studio",
          tint = BrushedBrass
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
private fun SpectatorModeBanner(
  uiState: LudoUiState,
  onToggleAutoFollow: () -> Unit,
  onExitSpectate: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.5.dp, BrushedBrass, RoundedCornerShape(14.dp))
      .padding(horizontal = 12.dp, vertical = 7.dp)
      .testTag("spectator_mode_banner"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .size(30.dp)
          .clip(CircleShape)
          .background(BrushedBrass.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Visibility,
          contentDescription = null,
          tint = BrushedBrass,
          modifier = Modifier.size(16.dp)
        )
      }
      Column {
        val roomPrivacyBadge = if (uiState.isPublicRoom) "PUBLIC ROOM" else "PRIVATE ROOM"
        val syncBadge = if (uiState.isOnlineMatch) "FIRESTORE LIVE SYNC" else "LIVE 3D SYNC"
        Text(
          text = "$roomPrivacyBadge OBSERVER (VIEW-ONLY) • $syncBadge",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.ExtraBold,
          color = BrushedBrass,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "Observing ${uiState.spectatingTargetName.ifBlank { "Room #${uiState.roomCode}" }} (#${uiState.roomCode}) • Turn ${uiState.turnNumber} • Board Locked",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(
            if (uiState.autoFocusTurnCamera) EucalyptusSage.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surfaceVariant
          )
          .border(
            1.dp,
            if (uiState.autoFocusTurnCamera) EucalyptusSage else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            RoundedCornerShape(50)
          )
          .clickable { onToggleAutoFollow() }
          .padding(horizontal = 8.dp, vertical = 5.dp)
          .testTag("spectator_autofollow_toggle"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = if (uiState.autoFocusTurnCamera) "Auto-Cam ON" else "Free Cam",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (uiState.autoFocusTurnCamera) EucalyptusSage else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(TerracottaRed.copy(alpha = 0.16f))
          .border(1.dp, TerracottaRed, RoundedCornerShape(50))
          .clickable { onExitSpectate() }
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .testTag("exit_spectator_mode_button"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Exit",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
          color = TerracottaRed
        )
      }
    }
  }
}

@Composable
private fun AudioCallControlStrip(
  audioCallState: AudioCallUiState,
  onStartGroupCall: () -> Unit,
  onEndCall: () -> Unit,
  onToggleMic: () -> Unit,
  onToggleSpeaker: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(
        width = 1.dp,
        color = if (audioCallState.isInCall) EucalyptusSage else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp)
      )
      .padding(horizontal = 12.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f)
    ) {
      Icon(
        imageVector = if (audioCallState.isInCall) Icons.Default.PhoneInTalk else Icons.Default.Call,
        contentDescription = null,
        tint = if (audioCallState.isInCall) EucalyptusSage else BrushedBrass,
        modifier = Modifier.size(18.dp)
      )
      Column {
        Text(
          text = when {
            !audioCallState.isInCall -> "Voice Chat Ready (Group or 1-on-1)"
            audioCallState.callMode == AudioCallMode.DIRECT ->
              "1-on-1 Call: ${audioCallState.targetPeerName}"
            else -> "Live Group Table Call"
          },
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = if (audioCallState.isInCall) {
            if (audioCallState.isMicMuted) "Mic Muted • Tap seat to mute/call peer" else "Mic Live • Tap any seat for 1-on-1 call"
          } else {
            "Join Group Voice or tap a player's phone icon for 1-on-1"
          },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (!audioCallState.isInCall) {
      Button(
        onClick = onStartGroupCall,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = EucalyptusSage,
          contentColor = Color.White
        ),
        modifier = Modifier
          .height(36.dp)
          .testTag("start_group_call_button")
      ) {
        Icon(
          imageVector = Icons.Default.Call,
          contentDescription = null,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = stringResource(R.string.audio_call_group), style = MaterialTheme.typography.labelMedium)
      }
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        IconButton(
          onClick = onToggleMic,
          modifier = Modifier
            .size(36.dp)
            .testTag("toggle_mic_mute_button")
        ) {
          Icon(
            imageVector = if (audioCallState.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = if (audioCallState.isMicMuted) "Unmute Microphone" else "Mute Microphone",
            tint = if (audioCallState.isMicMuted) TerracottaRed else EucalyptusSage,
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(
          onClick = onToggleSpeaker,
          modifier = Modifier
            .size(36.dp)
            .testTag("toggle_speaker_button")
        ) {
          Icon(
            imageVector = if (audioCallState.isSpeakerEnabled) {
              Icons.AutoMirrored.Filled.VolumeUp
            } else {
              Icons.AutoMirrored.Filled.VolumeOff
            },
            contentDescription = "Toggle Speaker",
            tint = if (audioCallState.isSpeakerEnabled) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(
          onClick = onEndCall,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(TerracottaRed.copy(alpha = 0.18f))
            .testTag("end_audio_call_button")
        ) {
          Icon(
            imageVector = Icons.Default.CallEnd,
            contentDescription = "End Call",
            tint = TerracottaRed,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun PlayerSeatsGrid(
  uiState: LudoUiState,
  audioCallState: AudioCallUiState,
  currentUserId: String?,
  onStartDirectCall: (PlayerSeat) -> Unit,
  onToggleMutePeer: (PlayerSeat) -> Unit
) {
  val seats = uiState.seats
  val columnsPerRow = if (seats.size > 4) 4 else 2
  val goalPos = LudoRulesEngine.homeGoalPos(uiState.boardArmCount)

  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    val chunked = seats.chunked(columnsPerRow)
    chunked.forEach { rowSeats ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        rowSeats.forEach { seat ->
          val isSpeaking = seat.uid in audioCallState.speakingUids
          val isPeerMuted = seat.uid in audioCallState.mutedPeerUids
          val isSelf = seat.uid == currentUserId || seat.uid.contains("LOCAL-0")
          PlayerCornerHudCard(
            seat = seat,
            isCompact = seats.size > 4,
            isCurrentTurn = uiState.currentTurnColor == seat.color && uiState.turnPhase != TurnPhase.GAME_OVER,
            tokens = uiState.tokens,
            goalPos = goalPos,
            consecutiveSixes = if (uiState.currentTurnColor == seat.color) uiState.consecutiveSixes else 0,
            turnSecondsRemaining = uiState.turnSecondsRemaining,
            isSpeaking = isSpeaking,
            isPeerMuted = isPeerMuted,
            canCallOrMutePeer = !uiState.isSpectating && !isSelf && !seat.isBot,
            onCallPeerClicked = { onStartDirectCall(seat) },
            onMutePeerClicked = { onToggleMutePeer(seat) },
            modifier = Modifier.weight(1f)
          )
        }
        repeat(columnsPerRow - rowSeats.size) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}

@Composable
private fun PlayerCornerHudCard(
  seat: PlayerSeat,
  isCompact: Boolean,
  isCurrentTurn: Boolean,
  tokens: List<Int>,
  goalPos: Int,
  consecutiveSixes: Int,
  turnSecondsRemaining: Int,
  isSpeaking: Boolean,
  isPeerMuted: Boolean,
  canCallOrMutePeer: Boolean,
  onCallPeerClicked: () -> Unit,
  onMutePeerClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  val (primary, _, softTone) = paletteForColor(seat.color)
  val tokenRange = LudoRulesEngine.tokenIndicesForColor(seat.color)
  val homeCount = tokenRange.count { tokens.getOrElse(it) { -1 } == goalPos }
  val activeCount = tokenRange.count { tokens.getOrElse(it) { -1 } in 0 until goalPos }

  Card(
    modifier = modifier
      .border(
        width = if (isSpeaking) 2.5.dp else if (isCurrentTurn) 2.dp else 1.dp,
        color = when {
          isSpeaking -> EucalyptusSage
          isCurrentTurn -> primary
          else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(12.dp)
      ),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCurrentTurn) softTone.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = if (isCompact) 6.dp else 10.dp, vertical = if (isCompact) 5.dp else 7.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(if (isCompact) 8.dp else 10.dp)
              .clip(CircleShape)
              .background(primary)
          )
          Text(
            text = seat.name,
            style = if (isCompact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        if (canCallOrMutePeer && !isCompact) {
          Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(EucalyptusSage.copy(alpha = 0.16f))
                .clickable { onCallPeerClicked() }
                .testTag("direct_call_${seat.color.name}"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "1-to-1 Call ${seat.name}",
                tint = EucalyptusSage,
                modifier = Modifier.size(12.dp)
              )
            }
            Box(
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onMutePeerClicked() },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isPeerMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Mute ${seat.name}",
                tint = if (isPeerMuted) TerracottaRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
              )
            }
          }
        } else {
          Text(
            text = "$homeCount/4",
            style = MaterialTheme.typography.labelSmall,
            color = primary,
            fontWeight = FontWeight.Bold
          )
        }
      }

      if (!isCompact) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "$homeCount/4 Home • $activeCount Track",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (isCurrentTurn) {
            Text(
              text = if (consecutiveSixes > 0) "${consecutiveSixes}×6 • ${turnSecondsRemaining}s" else "${turnSecondsRemaining}s",
              style = MaterialTheme.typography.labelSmall,
              color = primary,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      if (isCurrentTurn) {
        LinearProgressIndicator(
          progress = {
            (turnSecondsRemaining.toFloat() / LudoUiState.TURN_DURATION_SECONDS.toFloat()).coerceIn(0f, 1f)
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(2.5.dp)
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
      seats = uiState.seats,
      armCount = uiState.boardArmCount,
      legalMoveTokenIndices = if (uiState.isSpectating) emptyList() else uiState.legalMoveTokenIndices,
      activeBlockades = uiState.activeBlockades,
      animatedToken = uiState.animatedToken,
      yawDegrees = uiState.cameraYaw,
      pitchDegrees = uiState.cameraPitch,
      zoom = uiState.cameraZoom,
      onCameraTransform = { dYaw, dPitch, zoomFactor ->
        viewModel.adjustCamera(dYaw, dPitch, zoomFactor)
      },
      onTokenTapped = { tokenIndex ->
        if (!uiState.isSpectating) {
          viewModel.onTokenSelected(tokenIndex)
        }
      },
      onBoardCenterTapped = {
        if (!uiState.isSpectating) {
          viewModel.onRollDiceClicked()
        }
      },
      activePawnEmote = uiState.activePawnEmote,
      modifier = Modifier.fillMaxSize()
    )

    // 3D Particle System & Fireworks Overlay on top of the board
    ParticleSystemOverlay(
      event = uiState.activeParticleBurst,
      onAnimationFinished = { viewModel.clearParticleBurst() },
      modifier = Modifier.fillMaxSize()
    )

    // In-Game HUD Overlay (Current Turn Indicator, Active Pawns on Board, Remaining Moves & Player Stats)
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      AnimatedVisibility(
        visible = uiState.isHudOverlayVisible,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        InGamePlayerStatsHudOverlay(
          uiState = uiState,
          onToggleExpanded = { viewModel.toggleHudOverlayExpanded() }
        )
      }

      // Architectural status toast pill below HUD
      val (turnColorPrimary, _, _) = paletteForColor(uiState.currentTurnColor)
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
          .border(1.dp, turnColorPrimary.copy(alpha = 0.6f), RoundedCornerShape(50))
          .padding(horizontal = 14.dp, vertical = 4.dp)
          .testTag("arena_status_banner_pill")
      ) {
        Text(
          text = uiState.statusBanner,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
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
private fun InGamePlayerStatsHudOverlay(
  uiState: LudoUiState,
  onToggleExpanded: () -> Unit
) {
  val telemetry = uiState.hudTelemetry
  val activeStats = telemetry.activePlayerStats
  val (turnPrimary, _, turnSoft) = paletteForColor(telemetry.currentTurnColor)
  val timerFraction = (uiState.turnSecondsRemaining.toFloat() / LudoUiState.TURN_DURATION_SECONDS.toFloat())
    .coerceIn(0f, 1f)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.2.dp, turnPrimary.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
      .testTag("in_game_stats_hud_overlay"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f)
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 7.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Top Row: Current Turn Indicator + Phase Badge + Timer + Expand Toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("hud_current_turn_indicator"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(7.dp),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(12.dp)
              .clip(CircleShape)
              .background(turnPrimary)
              .border(1.5.dp, BrushedBrass, CircleShape)
          )
          Text(
            text = "TURN #${telemetry.turnNumber} • ${telemetry.currentTurnPlayerName.uppercase()}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = turnPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = turnSoft.copy(alpha = 0.32f),
            modifier = Modifier.border(1.dp, turnPrimary.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
          ) {
            Text(
              text = telemetry.turnPhaseBadge,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = turnPrimary,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "${uiState.turnSecondsRemaining}s",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (uiState.turnSecondsRemaining <= 5) TerracottaRed else MaterialTheme.colorScheme.onSurfaceVariant
          )
          Surface(
            onClick = onToggleExpanded,
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .border(1.dp, BrushedBrass.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
              .testTag("hud_toggle_details_button")
          ) {
            Text(
              text = if (uiState.isHudOverlayExpanded) "Hide Table ▲" else "All Players ▼",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = BrushedBrass,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }
      }

      if (uiState.isHapticsEnabled && uiState.lastTriggeredHapticTitle.isNotBlank()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("hud_last_haptic_badge"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Vibration,
            contentDescription = null,
            tint = BrushedBrass,
            modifier = Modifier.size(12.dp)
          )
          Text(
            text = "Haptic Pulse: ${uiState.lastTriggeredHapticTitle}",
            style = MaterialTheme.typography.labelSmall,
            color = BrushedBrass,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Live Turn Countdown Bar
      LinearProgressIndicator(
        progress = { timerFraction },
        modifier = Modifier
          .fillMaxWidth()
          .height(3.dp)
          .clip(RoundedCornerShape(2.dp)),
        color = if (uiState.turnSecondsRemaining <= 5) TerracottaRed else turnPrimary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant
      )

      // 3-Column Tactical Metrics Strip: Active Pawns on Board | Remaining Moves | Victory Distance
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // 1. Active Pawns on Board Metric
        HudMetricPill(
          label = "ACTIVE PAWNS",
          primaryValue = "${activeStats.activePawnsOnBoard}/4 On Board",
          subValue = "${activeStats.pawnsInYard} Yard • ${activeStats.pawnsAtHomeGoal}/4 Home",
          accentColor = turnPrimary,
          modifier = Modifier
            .weight(1f)
            .testTag("hud_active_pawns_metric")
        )

        // 2. Remaining Moves Metric
        val remainingMovesHeadline = when (telemetry.turnPhase) {
          TurnPhase.ROLL -> "2 Dice to Roll"
          TurnPhase.MOVE -> "${telemetry.remainingDiceMovesInTurn} Move${if (telemetry.remainingDiceMovesInTurn == 1) "" else "s"} Left (+${telemetry.remainingDicePipsInTurn})"
          TurnPhase.GAME_OVER -> "0 Moves Left"
        }
        val remainingMovesSub = when (telemetry.turnPhase) {
          TurnPhase.MOVE -> "${telemetry.legalMovablePawnCount} Legal Pawn${if (telemetry.legalMovablePawnCount == 1) "" else "s"} • ${telemetry.activeDieLabel}"
          TurnPhase.ROLL -> "Tap 3D Dice to Roll"
          TurnPhase.GAME_OVER -> "Match Complete"
        }
        HudMetricPill(
          label = "REMAINING MOVES",
          primaryValue = remainingMovesHeadline,
          subValue = remainingMovesSub,
          accentColor = BrushedBrass,
          modifier = Modifier
            .weight(1f)
            .testTag("hud_remaining_moves_metric")
        )

        // 3. Remaining Tiles to Victory & Progress Metric
        HudMetricPill(
          label = "VICTORY PROGRESS",
          primaryValue = "${activeStats.raceProgressPercent}% Complete",
          subValue = "${activeStats.totalRemainingTilesToVictory} Tiles Left",
          accentColor = EucalyptusSage,
          modifier = Modifier
            .weight(1f)
            .testTag("hud_victory_distance_metric")
        )
      }

      // Expandable All-Players Live Telemetry Breakdown
      AnimatedVisibility(
        visible = uiState.isHudOverlayExpanded,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(8.dp)
            .testTag("hud_all_players_breakdown_panel"),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          telemetry.allPlayerStats.forEach { playerStat ->
            val (seatPrimary, _, _) = paletteForColor(playerStat.color)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                  if (playerStat.isCurrentTurn) seatPrimary.copy(alpha = 0.14f)
                  else Color.Transparent
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1.1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(seatPrimary)
                )
                Text(
                  text = if (playerStat.isCurrentTurn) "▶ ${playerStat.playerName}" else playerStat.playerName,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (playerStat.isCurrentTurn) FontWeight.ExtraBold else FontWeight.Medium,
                  color = if (playerStat.isCurrentTurn) seatPrimary else MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }

              Text(
                text = "Active: ${playerStat.activePawnsOnBoard}/4 • Home: ${playerStat.pawnsAtHomeGoal}/4",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1.3f),
                textAlign = TextAlign.Center
              )

              Text(
                text = "${playerStat.totalRemainingTilesToVictory} tiles (${playerStat.raceProgressPercent}%)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = seatPrimary,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HudMetricPill(
  label: String,
  primaryValue: String,
  subValue: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
      .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
      .padding(horizontal = 7.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(1.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = accentColor,
      fontWeight = FontWeight.ExtraBold,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = primaryValue,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = subValue,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun EmojiReactionStrip(
  isSpectating: Boolean = false,
  equippedEmoteSymbol: String,
  onTrigger3DPawnEmote: () -> Unit,
  onOpenEmoteGallery: () -> Unit,
  onEmojiSelected: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(
        width = if (isSpectating) 1.dp else 0.dp,
        color = if (isSpectating) BrushedBrass.copy(alpha = 0.65f) else Color.Transparent,
        shape = RoundedCornerShape(14.dp)
      )
      .padding(horizontal = 8.dp, vertical = 3.dp)
      .testTag("emoji_reaction_strip"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 3D Pawn Emote Trigger Pill + Gallery Button
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(BrushedBrass.copy(alpha = 0.16f))
          .border(1.dp, BrushedBrass, RoundedCornerShape(50))
          .clickable { onTrigger3DPawnEmote() }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("trigger_3d_pawn_emote_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = equippedEmoteSymbol,
          style = MaterialTheme.typography.labelLarge
        )
        Text(
          text = if (isSpectating) "Cheer 3D" else "3D Emote",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = BrushedBrass
        )
      }

      if (!isSpectating) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onOpenEmoteGallery() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("arena_open_emote_gallery_button"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "Loadout",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    QUICK_EMOJIS.take(if (isSpectating) 5 else 4).forEach { emoji ->
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .clickable { onEmojiSelected(emoji) }
          .testTag("emoji_reaction_$emoji"),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = emoji,
          style = MaterialTheme.typography.titleMedium
        )
      }
    }
  }
}

@Composable
private fun DualDiceAndTurnControlDock(
  uiState: LudoUiState,
  viewModel: LudoGameViewModel
) {
  val canAct = viewModel.canCurrentUserAct()
  val canRoll = canAct && uiState.turnPhase == TurnPhase.ROLL
  val canSelectDie = canAct && uiState.turnPhase == TurnPhase.MOVE
  val (turnColor, _, _) = paletteForColor(uiState.currentTurnColor)
  val activeSeat = uiState.seats.firstOrNull { it.color == uiState.currentTurnColor }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.5.dp, turnColor.copy(alpha = 0.75f), RoundedCornerShape(22.dp)),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Sequential Dual-Dice Step & Firestore Sync Header Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .testTag("dual_dice_sequence_step_badge"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Casino,
            contentDescription = null,
            tint = BrushedBrass,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = uiState.sequentialMoveStepLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = BrushedBrass,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Interactive Die #1 / Die #2 Sequential Selector Chips
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val die1Selected = uiState.turnPhase == TurnPhase.MOVE && uiState.selectedDieIndex == 0 && !uiState.dice1Used
          val die2Selected = uiState.turnPhase == TurnPhase.MOVE && uiState.selectedDieIndex == 1 && !uiState.dice2Used

          Surface(
            onClick = {
              if (uiState.turnPhase == TurnPhase.ROLL) {
                viewModel.onRollDiceClicked()
              } else {
                viewModel.onSelectDie(0)
              }
            },
            enabled = canRoll || (canSelectDie && !uiState.dice1Used),
            shape = RoundedCornerShape(999.dp),
            color = when {
              uiState.turnPhase == TurnPhase.MOVE && uiState.dice1Used ->
                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
              die1Selected -> BrushedBrass.copy(alpha = 0.24f)
              else -> MaterialTheme.colorScheme.surface
            },
            modifier = Modifier
              .border(
                width = if (die1Selected) 1.5.dp else 1.dp,
                color = if (die1Selected) BrushedBrass else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(999.dp)
              )
              .testTag("select_die_1_chip")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              if (die1Selected) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = BrushedBrass,
                  modifier = Modifier.size(11.dp)
                )
              }
              Text(
                text = if (uiState.turnPhase == TurnPhase.MOVE && uiState.dice1Used) {
                  "Die #1 (Used)"
                } else {
                  "Die #1 (+${uiState.diceValue})"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (die1Selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (die1Selected) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Surface(
            onClick = {
              if (uiState.turnPhase == TurnPhase.ROLL) {
                viewModel.onRollDiceClicked()
              } else {
                viewModel.onSelectDie(1)
              }
            },
            enabled = canRoll || (canSelectDie && !uiState.dice2Used),
            shape = RoundedCornerShape(999.dp),
            color = when {
              uiState.turnPhase == TurnPhase.MOVE && uiState.dice2Used ->
                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
              die2Selected -> BrushedBrass.copy(alpha = 0.24f)
              else -> MaterialTheme.colorScheme.surface
            },
            modifier = Modifier
              .border(
                width = if (die2Selected) 1.5.dp else 1.dp,
                color = if (die2Selected) BrushedBrass else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = RoundedCornerShape(999.dp)
              )
              .testTag("select_die_2_chip")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              if (die2Selected) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = BrushedBrass,
                  modifier = Modifier.size(11.dp)
                )
              }
              Text(
                text = if (uiState.turnPhase == TurnPhase.MOVE && uiState.dice2Used) {
                  "Die #2 (Used)"
                } else {
                  "Die #2 (+${uiState.diceValue2})"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (die2Selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (die2Selected) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Dual 3D Dice Tray (Die 1 and Die 2 side by side)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Interactive3DDiceCube(
              diceValue = uiState.diceValue,
              isRolling = uiState.isDiceRolling,
              activeColor = uiState.currentTurnColor,
              enabled = canRoll || (canSelectDie && !uiState.dice1Used),
              isSelected = uiState.turnPhase == TurnPhase.MOVE && uiState.selectedDieIndex == 0 && !uiState.dice1Used,
              isUsed = uiState.turnPhase == TurnPhase.MOVE && uiState.dice1Used,
              onRollOrSelectClicked = {
                if (uiState.turnPhase == TurnPhase.ROLL) {
                  viewModel.onRollDiceClicked()
                } else {
                  viewModel.onSelectDie(0)
                }
              },
              sizeDp = 58.dp,
              testTagName = "dice_3d_cube_1"
            )
            Text(
              text = when {
                uiState.turnPhase == TurnPhase.MOVE && uiState.dice1Used -> "USED"
                uiState.turnPhase == TurnPhase.MOVE && uiState.selectedDieIndex == 0 -> "DIE 1 (+${uiState.diceValue})"
                else -> "DIE 1"
              },
              style = MaterialTheme.typography.labelSmall,
              color = if (uiState.turnPhase == TurnPhase.MOVE && uiState.selectedDieIndex == 0 && !uiState.dice1Used) {
                BrushedBrass
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              fontWeight = FontWeight.Bold
            )
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Interactive3DDiceCube(
              diceValue = uiState.diceValue2,
              isRolling = uiState.isDiceRolling,
              activeColor = uiState.currentTurnColor,
              enabled = canRoll || (canSelectDie && !uiState.dice2Used),
              isSelected = uiState.turnPhase == TurnPhase.MOVE && uiState.selectedDieIndex == 1 && !uiState.dice2Used,
              isUsed = uiState.turnPhase == TurnPhase.MOVE && uiState.dice2Used,
              onRollOrSelectClicked = {
                if (uiState.turnPhase == TurnPhase.ROLL) {
                  viewModel.onRollDiceClicked()
                } else {
                  viewModel.onSelectDie(1)
                }
              },
              sizeDp = 58.dp,
              testTagName = "dice_3d_cube_2"
            )
            Text(
              text = when {
                uiState.turnPhase == TurnPhase.MOVE && uiState.dice2Used -> "USED"
                uiState.turnPhase == TurnPhase.MOVE && uiState.selectedDieIndex == 1 -> "DIE 2 (+${uiState.diceValue2})"
                else -> "DIE 2"
              },
              style = MaterialTheme.typography.labelSmall,
              color = if (uiState.turnPhase == TurnPhase.MOVE && uiState.selectedDieIndex == 1 && !uiState.dice2Used) {
                BrushedBrass
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Text(
            text = activeSeat?.name ?: uiState.currentTurnColor.label,
            style = MaterialTheme.typography.titleSmall,
            color = turnColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = when {
              uiState.isSpectating -> {
                val roomKind = if (uiState.isPublicRoom) "Public" else "Private"
                "👁️ Observing $roomKind Room #${uiState.roomCode} • ${uiState.lastSyncedDieChoiceSummary}"
              }
              uiState.isDiceRolling -> "Tumbling 2 3D dice..."
              uiState.turnPhase == TurnPhase.MOVE ->
                uiState.lastSyncedDieChoiceSummary
              canRoll -> "Tap dice or button to roll 2 dice"
              else -> "Waiting for ${activeSeat?.name ?: "player"}..."
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }

      Spacer(modifier = Modifier.width(8.dp))

      if (uiState.isSpectating) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = BrushedBrass.copy(alpha = 0.16f),
          modifier = Modifier
            .height(48.dp)
            .border(1.dp, BrushedBrass, RoundedCornerShape(14.dp))
            .testTag("spectator_view_only_badge")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = null,
              tint = BrushedBrass,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "VIEW-ONLY",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.ExtraBold,
              color = BrushedBrass
            )
          }
        }
      } else if (uiState.turnPhase == TurnPhase.MOVE && canAct && uiState.legalMoveTokenIndices.isNotEmpty()) {
        Button(
          onClick = {
            val best = LudoRulesEngine.chooseBestMove(
              tokens = uiState.tokens,
              color = uiState.currentTurnColor,
              diceValue = uiState.activeDieValue,
              seats = uiState.seats,
              armCount = uiState.boardArmCount
            ) ?: uiState.legalMoveTokenIndices.first()
            viewModel.onTokenSelected(best)
          },
          modifier = Modifier
            .height(48.dp)
            .testTag("move_pawn_action_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = turnColor,
            contentColor = Color.White
          )
        ) {
          Text(
            text = "MOVE +${uiState.activeDieValue}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
        }
      } else {
        Button(
          onClick = { viewModel.onRollDiceClicked() },
          enabled = canRoll,
          modifier = Modifier
            .height(48.dp)
            .testTag("roll_dice_action_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = turnColor,
            contentColor = Color.White
          )
        ) {
          Text(
            text = stringResource(R.string.roll_dice_action),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
          )
        }
      }
      }
    }
  }
}

@Composable
private fun VictoryPodiumOverlay(
  uiState: LudoUiState,
  onPlayCelebrationFanfare: () -> Unit,
  onWatchReplay: () -> Unit,
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
          text = "+100 Win Bonus • +${uiState.matchCapturesByMe * 25} Capture Pts • +${uiState.matchHomesByMe * 15} Home Pts",
          style = MaterialTheme.typography.labelLarge,
          color = BrushedBrass,
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
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onWatchReplay,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("podium_watch_replay_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VideoLibrary,
              contentDescription = null,
              tint = BrushedBrass,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("3D Replay", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }

          OutlinedButton(
            onClick = onPlayCelebrationFanfare,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("podium_play_celebration_sound_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = null,
              tint = BrushedBrass,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Fanfare", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
