package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MatchReplayRecord
import com.example.data.ReplayCameraPreset
import com.example.data.ReplayFrame
import com.example.ui.board3d.Interactive3DLudoBoard
import com.example.ui.board3d.paletteForColor
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.TerracottaRed
import com.example.ui.viewmodel.LudoGameViewModel
import com.example.ui.viewmodel.LudoUiState
import kotlin.math.roundToInt

@Composable
fun ReplayStudioScreen(
  uiState: LudoUiState,
  viewModel: LudoGameViewModel
) {
  BackHandler {
    viewModel.leaveToLobby()
  }

  val replay = uiState.selectedReplay
  val currentFrame = uiState.currentReplayFrame
  val frames = replay.frames
  val maxFrameIndex = (frames.size - 1).coerceAtLeast(0)
  val highlightedTokens = if (currentFrame.movedTokenIndex >= 0) {
    listOf(currentFrame.movedTokenIndex)
  } else {
    emptyList()
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 680.dp)
          .fillMaxSize()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // 1. Top Header Bar with Recording Status Toggle
        ReplayStudioTopBar(
          uiState = uiState,
          onBack = { viewModel.leaveToLobby() },
          onToggleRecording = { viewModel.toggleMatchRecordingEnabled() },
          onResetCamera = {
            viewModel.selectReplayCameraPreset(ReplayCameraPreset.ISOMETRIC_BROADCAST)
          }
        )

        // 2. Past Matches & Tactical Replays Selector Strip
        PastMatchesSelectorStrip(
          savedReplays = uiState.savedReplays,
          selectedReplayId = replay.id,
          onSelectReplay = { viewModel.selectReplayRecord(it) }
        )

        // 3. Multi-Angle 3D Camera Preset Selector Bar + Manual Orbit Quick Buttons
        CameraAnglePresetStrip(
          selectedPreset = uiState.selectedReplayCameraPreset,
          cameraYaw = uiState.cameraYaw,
          cameraPitch = uiState.cameraPitch,
          cameraZoom = uiState.cameraZoom,
          onSelectPreset = { viewModel.selectReplayCameraPreset(it) },
          onOrbitDelta = { dYaw, dPitch -> viewModel.adjustCamera(dYaw, dPitch, 1f) }
        )

        // 4. Interactive 3D Board Viewport showing historical frame from chosen camera angle
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
              width = 1.dp,
              color = BrushedBrass.copy(alpha = 0.55f),
              shape = RoundedCornerShape(20.dp)
            )
        ) {
          val replayAnimatedTrail = if (
            currentFrame.movedTokenIndex >= 0 &&
            currentFrame.fromStep != currentFrame.toStep
          ) {
            val stepHistory = if (
              currentFrame.fromStep >= 0 &&
              currentFrame.toStep > currentFrame.fromStep &&
              (currentFrame.toStep - currentFrame.fromStep) <= 12
            ) {
              (currentFrame.fromStep until currentFrame.toStep).toList()
            } else {
              listOf(currentFrame.fromStep)
            }
            com.example.ui.board3d.AnimatedTokenState(
              tokenIndex = currentFrame.movedTokenIndex,
              fromPos = currentFrame.fromStep,
              toPos = currentFrame.toStep,
              progress = 1f,
              trailPathPositions = stepHistory,
              fadingWakeAlpha = 0.78f
            )
          } else {
            null
          }

          Interactive3DLudoBoard(
            tokens = currentFrame.tokens,
            seats = replay.seats,
            armCount = uiState.replayBoardArmCount,
            legalMoveTokenIndices = highlightedTokens,
            activeBlockades = uiState.replayActiveBlockades,
            animatedToken = replayAnimatedTrail,
            yawDegrees = uiState.cameraYaw,
            pitchDegrees = uiState.cameraPitch,
            zoom = uiState.cameraZoom,
            onCameraTransform = { dYaw, dPitch, zoomFactor ->
              viewModel.adjustCamera(dYaw, dPitch, zoomFactor)
            },
            onTokenTapped = {},
            onBoardCenterTapped = { viewModel.toggleReplayPlayback() },
            modifier = Modifier.fillMaxSize().testTag("replay_3d_board_viewport")
          )

          // Top-left Camera Telemetry Badge inside 3D Viewport
          Row(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp)
              .clip(RoundedCornerShape(50))
              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
              .border(1.dp, BrushedBrass.copy(alpha = 0.6f), RoundedCornerShape(50))
              .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Cameraswitch,
              contentDescription = null,
              tint = BrushedBrass,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = "${uiState.selectedReplayCameraPreset.label} • Yaw ${uiState.cameraYaw.roundToInt()}° • Pitch ${uiState.cameraPitch.roundToInt()}°",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Top-right Match Summary Pill
          Row(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(8.dp)
              .clip(RoundedCornerShape(50))
              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
              .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "${replay.seats.size}P • ${replay.totalCaptures} Captures • ${replay.totalBlockades} Blockades",
              style = MaterialTheme.typography.labelSmall,
              color = BrushedBrass,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // 5. Strategy Breakdown & Move Telemetry Card
        ReplayTacticalAnalysisCard(
          frame = currentFrame,
          totalFrames = frames.size
        )

        // 6. Interactive Timeline Scrubber, Frame Chips & Playback Controls Dock
        ReplayPlaybackControlDock(
          uiState = uiState,
          frames = frames,
          maxFrameIndex = maxFrameIndex,
          onSeekFrame = { viewModel.seekReplayFrame(it) },
          onStepBack = { viewModel.stepReplayBackward() },
          onTogglePlay = { viewModel.toggleReplayPlayback() },
          onStepForward = { viewModel.stepReplayForward() },
          onCycleSpeed = { viewModel.cycleReplaySpeed() }
        )
      }
    }
  }
}

@Composable
private fun ReplayStudioTopBar(
  uiState: LudoUiState,
  onBack: () -> Unit,
  onToggleRecording: () -> Unit,
  onResetCamera: () -> Unit
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
        onClick = onBack,
        modifier = Modifier.testTag("replay_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back to Lobby"
        )
      }
      Column {
        Text(
          text = "3D Match Replay Studio",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
        Text(
          text = "Multi-Angle Camera • Strategy Analysis • Drag to Orbit",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Auto-Record Toggle Pill
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(
            if (uiState.isMatchRecordingEnabled) {
              TerracottaRed.copy(alpha = 0.15f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant
            }
          )
          .border(
            width = 1.dp,
            color = if (uiState.isMatchRecordingEnabled) TerracottaRed else MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(50)
          )
          .clickable { onToggleRecording() }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("toggle_match_recording_button"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.FiberManualRecord,
          contentDescription = "Match Recording Status",
          tint = if (uiState.isMatchRecordingEnabled) TerracottaRed else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(12.dp)
        )
        Text(
          text = if (uiState.isMatchRecordingEnabled) "REC ON" else "REC OFF",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (uiState.isMatchRecordingEnabled) TerracottaRed else MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      IconButton(
        onClick = onResetCamera,
        modifier = Modifier
          .size(36.dp)
          .testTag("replay_reset_camera_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Reset 3D Camera",
          tint = BrushedBrass
        )
      }
    }
  }
}

@Composable
private fun PastMatchesSelectorStrip(
  savedReplays: List<MatchReplayRecord>,
  selectedReplayId: String,
  onSelectReplay: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    savedReplays.forEach { item ->
      val isSelected = item.id == selectedReplayId
      Card(
        modifier = Modifier
          .widthIn(min = 190.dp, max = 260.dp)
          .clickable { onSelectReplay(item.id) }
          .border(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) BrushedBrass else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            shape = RoundedCornerShape(14.dp)
          )
          .testTag("select_replay_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isSelected) {
            BrushedBrass.copy(alpha = 0.14f)
          } else {
            MaterialTheme.colorScheme.surface
          }
        )
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Icon(
              imageVector = Icons.Default.VideoLibrary,
              contentDescription = null,
              tint = if (isSelected) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = item.recordedAtLabel,
              style = MaterialTheme.typography.labelSmall,
              color = if (isSelected) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold
            )
          }
          Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${item.frames.size} Moves • ${item.seats.size}P Arena",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
          )
        }
      }
    }
  }
}

@Composable
private fun CameraAnglePresetStrip(
  selectedPreset: ReplayCameraPreset,
  cameraYaw: Float,
  cameraPitch: Float,
  cameraZoom: Float,
  onSelectPreset: (ReplayCameraPreset) -> Unit,
  onOrbitDelta: (Float, Float) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      ReplayCameraPreset.entries.forEach { preset ->
        val selected = preset == selectedPreset
        FilterChip(
          selected = selected,
          onClick = { onSelectPreset(preset) },
          label = {
            Text(
              text = preset.label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
          },
          leadingIcon = {
            Icon(
              imageVector = if (preset.autoFollowActiveColor) {
                Icons.Default.CenterFocusStrong
              } else {
                Icons.Default.ViewInAr
              },
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BrushedBrass.copy(alpha = 0.22f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
            selectedLeadingIconColor = BrushedBrass
          ),
          modifier = Modifier.testTag("replay_camera_preset_${preset.id}")
        )
      }

      // Quick 45° Orbit Left / Right buttons for fine manual angle inspection
      FilterChip(
        selected = false,
        onClick = { onOrbitDelta(-45f, 0f) },
        label = { Text("Orbit -45°", style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.RotateLeft,
            contentDescription = "Orbit Left 45 degrees",
            modifier = Modifier.size(14.dp)
          )
        },
        modifier = Modifier.testTag("replay_orbit_left_button")
      )

      FilterChip(
        selected = false,
        onClick = { onOrbitDelta(45f, 0f) },
        label = { Text("Orbit +45°", style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.RotateRight,
            contentDescription = "Orbit Right 45 degrees",
            modifier = Modifier.size(14.dp)
          )
        },
        modifier = Modifier.testTag("replay_orbit_right_button")
      )
    }
  }
}

@Composable
private fun ReplayTacticalAnalysisCard(
  frame: ReplayFrame,
  totalFrames: Int
) {
  val (actorPrimary, _, actorSoft) = paletteForColor(frame.actorColor)

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, actorPrimary.copy(alpha = 0.65f), RoundedCornerShape(16.dp)),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
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
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(actorPrimary)
          )
          Text(
            text = "Turn #${frame.turnNumber} • ${frame.actorName}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = actorPrimary
          )
          if (frame.usedDieValue > 0) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(actorSoft.copy(alpha = 0.35f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "Dice [${frame.diceValue}, ${frame.diceValue2}] → Used +${frame.usedDieValue}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (frame.capturedCount > 0) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(TerracottaRed.copy(alpha = 0.18f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
              Text(
                text = "⚔️ Capture",
                style = MaterialTheme.typography.labelSmall,
                color = TerracottaRed,
                fontWeight = FontWeight.Bold
              )
            }
          }
          if (frame.formedBlockade) {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(BrushedBrass.copy(alpha = 0.2f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = BrushedBrass,
                modifier = Modifier.size(11.dp)
              )
              Text(
                text = "Blockade",
                style = MaterialTheme.typography.labelSmall,
                color = BrushedBrass,
                fontWeight = FontWeight.Bold
              )
            }
          }
          if (frame.reachedHome) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(EucalyptusSage.copy(alpha = 0.2f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
              Text(
                text = "👑 Home Goal",
                style = MaterialTheme.typography.labelSmall,
                color = EucalyptusSage,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Text(
            text = "Step ${frame.frameIndex + 1}/$totalFrames",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Text(
        text = frame.eventSummary,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        text = "💡 ${frame.tacticalNote}",
        style = MaterialTheme.typography.labelSmall,
        color = BrushedBrass,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun ReplayPlaybackControlDock(
  uiState: LudoUiState,
  frames: List<ReplayFrame>,
  maxFrameIndex: Int,
  onSeekFrame: (Int) -> Unit,
  onStepBack: () -> Unit,
  onTogglePlay: () -> Unit,
  onStepForward: () -> Unit,
  onCycleSpeed: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, BrushedBrass.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Quick Jump Move Log Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        frames.forEachIndexed { idx, f ->
          val isCurrent = idx == uiState.replayFrameIndex
          val (chipColor, _, _) = paletteForColor(f.actorColor)
          val badge = when {
            f.reachedHome -> "👑"
            f.capturedCount > 0 -> "⚔️"
            f.formedBlockade -> "🛡️"
            idx == 0 -> "🏁"
            else -> "+${f.usedDieValue}"
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(
                if (isCurrent) chipColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant
              )
              .border(
                width = if (isCurrent) 1.5.dp else 0.5.dp,
                color = if (isCurrent) chipColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp)
              )
              .clickable { onSeekFrame(idx) }
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("replay_frame_chip_$idx")
          ) {
            Text(
              text = "#${idx + 1} $badge",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
              color = if (isCurrent) chipColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      // Timeline Scrubber Slider + Transport Controls
      if (maxFrameIndex > 0) {
        Slider(
          value = uiState.replayFrameIndex.toFloat(),
          onValueChange = { onSeekFrame(it.roundToInt()) },
          valueRange = 0f..maxFrameIndex.toFloat(),
          steps = (maxFrameIndex - 1).coerceAtLeast(0),
          colors = SliderDefaults.colors(
            thumbColor = BrushedBrass,
            activeTrackColor = BrushedBrass
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .testTag("replay_timeline_slider")
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Frame ${uiState.replayFrameIndex + 1} of ${frames.size}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontWeight = FontWeight.SemiBold
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          IconButton(
            onClick = onStepBack,
            enabled = uiState.replayFrameIndex > 0,
            modifier = Modifier
              .size(40.dp)
              .testTag("replay_step_back_button")
          ) {
            Icon(
              imageVector = Icons.Default.FastRewind,
              contentDescription = "Previous Move"
            )
          }

          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(BrushedBrass)
              .clickable { onTogglePlay() }
              .testTag("replay_play_pause_button"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (uiState.isReplayPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (uiState.isReplayPlaying) "Pause Replay" else "Play Replay",
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }

          IconButton(
            onClick = onStepForward,
            enabled = uiState.replayFrameIndex < maxFrameIndex,
            modifier = Modifier
              .size(40.dp)
              .testTag("replay_step_forward_button")
          ) {
            Icon(
              imageVector = Icons.Default.FastForward,
              contentDescription = "Next Move"
            )
          }
        }

        // Speed Multiplier Selector Pill (0.5x, 1x, 2x, 4x)
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, BrushedBrass, RoundedCornerShape(50))
            .clickable { onCycleSpeed() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("replay_speed_button"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Speed,
            contentDescription = "Playback Speed",
            tint = BrushedBrass,
            modifier = Modifier.size(14.dp)
          )
          val speedLabel = if (uiState.replaySpeedMultiplier == 0.5f) {
            "0.5x"
          } else {
            "${uiState.replaySpeedMultiplier.toInt()}x"
          }
          Text(
            text = speedLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}
