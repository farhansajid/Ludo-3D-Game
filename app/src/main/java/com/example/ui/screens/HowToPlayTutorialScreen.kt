package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.HowToPlayTutorialCatalog
import com.example.data.PlayerColor
import com.example.data.TutorialInteractiveActionType
import com.example.data.TutorialLessonStep
import com.example.ui.board3d.Interactive3DDiceCube
import com.example.ui.board3d.Interactive3DLudoBoard
import com.example.ui.board3d.LudoBoardCoordinates
import com.example.ui.board3d.ParticleSystemOverlay
import com.example.ui.board3d.Tutorial3DBoardOverlay
import com.example.ui.board3d.Vec3
import com.example.ui.theme.AlabasterLinen
import com.example.ui.theme.BrassHighlight
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.Obsidianink
import com.example.ui.theme.TerracottaRed
import com.example.ui.viewmodel.LudoGameViewModel
import com.example.ui.viewmodel.LudoUiState
import kotlin.math.sin

@Composable
fun HowToPlayTutorialScreen(
  uiState: LudoUiState,
  viewModel: LudoGameViewModel
) {
  BackHandler {
    viewModel.leaveToLobby()
  }

  val lesson = uiState.currentTutorialLesson
  val totalLessons = HowToPlayTutorialCatalog.LESSONS.size
  val completedCount = uiState.tutorialCompletedStepIds.size

  val infiniteTransition = rememberInfiniteTransition(label = "tutorial_ui_pulse")
  val spotlightPulse by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 6.2831855f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "spotlight_pulse_phase"
  )
  val pulseAlpha = (0.65f + 0.35f * sin(spotlightPulse)).coerceIn(0.35f, 1f)

  // Compute 3D trajectory world endpoints for the active lesson
  val trajectoryOverlay = buildTutorial3DOverlayForLesson(
    lesson = lesson,
    actionState = uiState.tutorialActionState
  )

  Surface(
    modifier = Modifier
      .fillMaxSize()
      .testTag("how_to_play_tutorial_screen"),
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
              .width(360.dp)
              .fillMaxHeight()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            TutorialTopBar(
              lessonNumber = lesson.stepNumber,
              totalLessons = totalLessons,
              completedCount = completedCount,
              onBack = { viewModel.leaveToLobby() },
              onResetLesson = { viewModel.resetCurrentTutorialLesson() },
              onResetCamera = {
                viewModel.selectTutorialLesson(uiState.tutorialStepIndex)
              }
            )

            TutorialChapterStepperStrip(
              lessons = HowToPlayTutorialCatalog.LESSONS,
              activeIndex = uiState.tutorialStepIndex,
              completedIds = uiState.tutorialCompletedStepIds,
              onSelectLesson = { viewModel.selectTutorialLesson(it) }
            )

            TutorialCoachInstructionCard(
              lesson = lesson,
              uiState = uiState,
              pulseAlpha = pulseAlpha
            )

            TutorialDualDiceAndActionDock(
              lesson = lesson,
              uiState = uiState,
              pulseAlpha = pulseAlpha,
              viewModel = viewModel
            )
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          ) {
            Tutorial3DBoardViewport(
              uiState = uiState,
              lesson = lesson,
              tutorialOverlay = trajectoryOverlay,
              viewModel = viewModel
            )
          }
        }
      } else {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TutorialTopBar(
            lessonNumber = lesson.stepNumber,
            totalLessons = totalLessons,
            completedCount = completedCount,
            onBack = { viewModel.leaveToLobby() },
            onResetLesson = { viewModel.resetCurrentTutorialLesson() },
            onResetCamera = {
              viewModel.selectTutorialLesson(uiState.tutorialStepIndex)
            }
          )

          TutorialChapterStepperStrip(
            lessons = HowToPlayTutorialCatalog.LESSONS,
            activeIndex = uiState.tutorialStepIndex,
            completedIds = uiState.tutorialCompletedStepIds,
            onSelectLesson = { viewModel.selectTutorialLesson(it) }
          )

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
          ) {
            Tutorial3DBoardViewport(
              uiState = uiState,
              lesson = lesson,
              tutorialOverlay = trajectoryOverlay,
              viewModel = viewModel
            )
          }

          TutorialCoachInstructionCard(
            lesson = lesson,
            uiState = uiState,
            pulseAlpha = pulseAlpha
          )

          TutorialDualDiceAndActionDock(
            lesson = lesson,
            uiState = uiState,
            pulseAlpha = pulseAlpha,
            viewModel = viewModel
          )
        }
      }
    }
  }
}

private fun buildTutorial3DOverlayForLesson(
  lesson: TutorialLessonStep,
  actionState: TutorialInteractiveActionType
): Tutorial3DBoardOverlay {
  val showTrajectory = actionState == TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN &&
    lesson.targetTokenIndex >= 0

  val fromWorld = if (lesson.targetTokenIndex >= 0) {
    val slotIdx = lesson.targetTokenIndex % 4
    val (wx, wy) = LudoBoardCoordinates.tokenWorldXY(
      color = PlayerColor.RED,
      slotIndex = slotIdx,
      relativePos = lesson.fromRelativePos,
      seats = HowToPlayTutorialCatalog.TUTORIAL_SEATS,
      armCount = 4
    )
    Vec3(wx, wy, 0.35f)
  } else {
    null
  }

  val toWorld = if (lesson.targetTokenIndex >= 0) {
    val slotIdx = lesson.targetTokenIndex % 4
    val (wx, wy) = LudoBoardCoordinates.tokenWorldXY(
      color = PlayerColor.RED,
      slotIndex = slotIdx,
      relativePos = lesson.toRelativePos,
      seats = HowToPlayTutorialCatalog.TUTORIAL_SEATS,
      armCount = 4
    )
    Vec3(wx, wy, if (lesson.toRelativePos >= 56) 0.55f else 0.32f)
  } else {
    null
  }

  return Tutorial3DBoardOverlay(
    beacons = lesson.beacons,
    trajectoryFromWorld = fromWorld,
    trajectoryToWorld = toWorld,
    showTrajectoryArc = showTrajectory,
    activeTargetTokenIndex = if (showTrajectory) lesson.targetTokenIndex else -1
  )
}

@Composable
private fun TutorialTopBar(
  lessonNumber: Int,
  totalLessons: Int,
  completedCount: Int,
  onBack: () -> Unit,
  onResetLesson: () -> Unit,
  onResetCamera: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("tutorial_back_to_lobby_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Lobby"
          )
        }
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = null,
              tint = BrushedBrass,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "3D Royal Academy • How to Play",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold
            )
          }
          Text(
            text = "Lesson $lessonNumber of $totalLessons • $completedCount/$totalLessons Mastered (+150 Coins Reward)",
            style = MaterialTheme.typography.labelSmall,
            color = BrushedBrass
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        IconButton(
          onClick = onResetLesson,
          modifier = Modifier.testTag("tutorial_reset_step_button")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Replay Current Lesson",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(
          onClick = onResetCamera,
          modifier = Modifier.testTag("tutorial_reset_camera_button")
        ) {
          Icon(
            imageVector = Icons.Default.CenterFocusStrong,
            contentDescription = "Reset 3D Camera Angle",
            tint = BrushedBrass
          )
        }
      }
    }

    LinearProgressIndicator(
      progress = { (completedCount.toFloat() / totalLessons.toFloat()).coerceIn(0.08f, 1f) },
      modifier = Modifier
        .fillMaxWidth()
        .height(5.dp)
        .clip(RoundedCornerShape(50)),
      color = BrushedBrass,
      trackColor = MaterialTheme.colorScheme.surfaceVariant
    )
  }
}

@Composable
private fun TutorialChapterStepperStrip(
  lessons: List<TutorialLessonStep>,
  activeIndex: Int,
  completedIds: Set<String>,
  onSelectLesson: (Int) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    lessons.forEachIndexed { idx, step ->
      val isSelected = idx == activeIndex
      val isCompleted = step.id in completedIds

      val containerColor = when {
        isSelected -> BrushedBrass
        isCompleted -> EucalyptusSage.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.surfaceVariant
      }
      val contentColor = when {
        isSelected -> Obsidianink
        isCompleted -> EucalyptusSage
        else -> MaterialTheme.colorScheme.onSurfaceVariant
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(50))
          .background(containerColor)
          .border(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) BrassHighlight else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            shape = RoundedCornerShape(50)
          )
          .clickable { onSelectLesson(idx) }
          .padding(horizontal = 12.dp, vertical = 6.dp)
          .testTag("tutorial_step_chip_${step.stepNumber}")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
          if (isCompleted) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Completed",
              modifier = Modifier.size(14.dp),
              tint = if (isSelected) Obsidianink else EucalyptusSage
            )
          }
          Text(
            text = "${step.stepNumber}. ${
              when (step.stepNumber) {
                1 -> "3D Board"
                2 -> "Deploy (6)"
                3 -> "Dice Choice"
                4 -> "Captures"
                5 -> "Blockades"
                else -> "Home Goal"
              }
            }",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = contentColor
          )
        }
      }
    }
  }
}

@Composable
private fun Tutorial3DBoardViewport(
  uiState: LudoUiState,
  lesson: TutorialLessonStep,
  tutorialOverlay: Tutorial3DBoardOverlay,
  viewModel: LudoGameViewModel
) {
  Card(
    modifier = Modifier
      .fillMaxSize()
      .border(1.5.dp, BrushedBrass.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1714))
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Interactive3DLudoBoard(
        tokens = uiState.tutorialTokens,
        seats = HowToPlayTutorialCatalog.TUTORIAL_SEATS,
        armCount = 4,
        legalMoveTokenIndices = uiState.tutorialLegalMoveTokens,
        activeBlockades = uiState.tutorialActiveBlockades,
        animatedToken = uiState.tutorialAnimatedToken,
        yawDegrees = uiState.cameraYaw,
        pitchDegrees = uiState.cameraPitch,
        zoom = uiState.cameraZoom,
        onCameraTransform = { dYaw, dPitch, zoomFactor ->
          viewModel.adjustCamera(dYaw, dPitch, zoomFactor)
        },
        onTokenTapped = { tokenIdx ->
          viewModel.performTutorialBoardOrPawnAction(tappedTokenIndex = tokenIdx)
        },
        onBoardCenterTapped = {
          viewModel.performTutorialBoardOrPawnAction()
        },
        activePawnEmote = uiState.activePawnEmote,
        tutorialOverlay = tutorialOverlay,
        onTutorialBeaconTapped = { _ ->
          viewModel.performTutorialBoardOrPawnAction(tappedTokenIndex = lesson.targetTokenIndex)
        },
        modifier = Modifier.fillMaxSize()
      )

      ParticleSystemOverlay(
        event = uiState.activeParticleBurst,
        onAnimationFinished = { viewModel.clearParticleBurst() },
        modifier = Modifier.fillMaxSize()
      )

      // Top-left 3D Overlay Mode Badge
      Row(
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(10.dp)
          .clip(RoundedCornerShape(50))
          .background(Obsidianink.copy(alpha = 0.86f))
          .border(1.dp, BrushedBrass.copy(alpha = 0.75f), RoundedCornerShape(50))
          .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.ViewInAr,
          contentDescription = null,
          tint = BrassHighlight,
          modifier = Modifier.size(15.dp)
        )
        Text(
          text = "3D Spotlight Overlays Active • Drag to Orbit",
          style = MaterialTheme.typography.labelSmall,
          color = AlabasterLinen,
          fontWeight = FontWeight.SemiBold
        )
      }

      // Bottom-right quick 3D Camera Angle presets for studying the board
      Row(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(10.dp)
          .clip(RoundedCornerShape(50))
          .background(Obsidianink.copy(alpha = 0.86f))
          .border(1.dp, BrushedBrass.copy(alpha = 0.65f), RoundedCornerShape(50))
          .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "3D View:",
          style = MaterialTheme.typography.labelSmall,
          color = BrassHighlight
        )
        Text(
          text = "Iso",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = AlabasterLinen,
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable {
              viewModel.setCameraPreset(
                yaw = lesson.cameraYaw,
                pitch = lesson.cameraPitch,
                zoom = lesson.cameraZoom
              )
            }
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("tutorial_camera_iso_button")
        )
        Text(
          text = "Top",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = AlabasterLinen,
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable {
              viewModel.setCameraPreset(
                yaw = 0f,
                pitch = 68f,
                zoom = 1.05f
              )
            }
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("tutorial_camera_top_button")
        )
        Text(
          text = "Close-Up",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = AlabasterLinen,
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable {
              viewModel.setCameraPreset(
                yaw = -15f,
                pitch = 36f,
                zoom = 1.32f
              )
            }
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag("tutorial_camera_closeup_button")
        )
      }
    }
  }
}

@Composable
private fun TutorialCoachInstructionCard(
  lesson: TutorialLessonStep,
  uiState: LudoUiState,
  pulseAlpha: Float
) {
  val isCompleted = uiState.tutorialActionState == TutorialInteractiveActionType.LESSON_COMPLETED
  val borderColor = if (isCompleted) {
    EucalyptusSage
  } else {
    BrushedBrass.copy(alpha = pulseAlpha)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.8.dp, borderColor, RoundedCornerShape(18.dp))
      .testTag("tutorial_coach_card"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isCompleted) {
        EucalyptusSage.copy(alpha = 0.14f)
      } else {
        MaterialTheme.colorScheme.surface
      }
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = lesson.chapterTag,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
          color = if (isCompleted) EucalyptusSage else BrushedBrass
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.TouchApp,
            contentDescription = null,
            tint = if (isCompleted) EucalyptusSage else TerracottaRed,
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = uiState.tutorialActionState.actionLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCompleted) EucalyptusSage else TerracottaRed,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Text(
        text = lesson.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = uiState.tutorialCoachBanner,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
          .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Lightbulb,
          contentDescription = null,
          tint = BrushedBrass,
          modifier = Modifier.size(15.dp)
        )
        Text(
          text = "Pro Tip: ${lesson.proTip}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun TutorialDualDiceAndActionDock(
  lesson: TutorialLessonStep,
  uiState: LudoUiState,
  pulseAlpha: Float,
  viewModel: LudoGameViewModel
) {
  val actionState = uiState.tutorialActionState
  val isRollStep = actionState == TutorialInteractiveActionType.ROLL_DUAL_DICE
  val isSelectDieStep = actionState == TutorialInteractiveActionType.SELECT_DIE_SLOT
  val isPawnStep = actionState == TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN
  val isLessonCompleted = actionState == TutorialInteractiveActionType.LESSON_COMPLETED

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (isRollStep || isSelectDieStep) 2.dp else 1.dp,
        color = if (isRollStep || isSelectDieStep) {
          BrushedBrass.copy(alpha = pulseAlpha)
        } else {
          MaterialTheme.colorScheme.outline
        },
        shape = RoundedCornerShape(20.dp)
      ),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Dual 3D Dice Selector with interactive spotlight ring
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Die #1 Slot
          val die1Highlighted = isSelectDieStep && lesson.requiredDieSelectionIndex == 0
          val die1Selected = uiState.tutorialSelectedDieIndex == 0 && uiState.tutorialDiceRolledInStep
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .background(
                if (die1Selected) BrushedBrass.copy(alpha = 0.18f) else Color.Transparent
              )
              .border(
                width = if (die1Highlighted || die1Selected) 2.dp else 1.dp,
                color = when {
                  die1Highlighted -> TerracottaRed.copy(alpha = pulseAlpha)
                  die1Selected -> BrushedBrass
                  else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(14.dp)
              )
              .clickable { viewModel.performTutorialSelectDie(0) }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("tutorial_die_slot_0")
          ) {
            Interactive3DDiceCube(
              diceValue = uiState.tutorialDice1,
              isRolling = uiState.tutorialIsRollingDice,
              activeColor = PlayerColor.RED,
              enabled = true,
              isSelected = die1Selected,
              isUsed = false,
              onRollOrSelectClicked = { viewModel.performTutorialSelectDie(0) },
              sizeDp = 46.dp,
              testTagName = "tutorial_dice_3d_cube_1"
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (die1Selected) "Die #1 [${uiState.tutorialDice1}] ✓" else "Die #1 [${uiState.tutorialDice1}]",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (die1Selected) FontWeight.ExtraBold else FontWeight.Medium,
              color = if (die1Selected) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Die #2 Slot
          val die2Highlighted = isSelectDieStep && lesson.requiredDieSelectionIndex == 1
          val die2Selected = uiState.tutorialSelectedDieIndex == 1 && uiState.tutorialDiceRolledInStep
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .background(
                if (die2Highlighted) {
                  TerracottaRed.copy(alpha = 0.16f)
                } else if (die2Selected) {
                  BrushedBrass.copy(alpha = 0.18f)
                } else {
                  Color.Transparent
                }
              )
              .border(
                width = if (die2Highlighted || die2Selected) 2.dp else 1.dp,
                color = when {
                  die2Highlighted -> TerracottaRed.copy(alpha = pulseAlpha)
                  die2Selected -> BrushedBrass
                  else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                },
                shape = RoundedCornerShape(14.dp)
              )
              .clickable { viewModel.performTutorialSelectDie(1) }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("tutorial_die_slot_1")
          ) {
            Interactive3DDiceCube(
              diceValue = uiState.tutorialDice2,
              isRolling = uiState.tutorialIsRollingDice,
              activeColor = PlayerColor.RED,
              enabled = true,
              isSelected = die2Selected,
              isUsed = false,
              onRollOrSelectClicked = { viewModel.performTutorialSelectDie(1) },
              sizeDp = 46.dp,
              testTagName = "tutorial_dice_3d_cube_2"
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = when {
                die2Highlighted -> "TAP DIE #2 [${uiState.tutorialDice2}]"
                die2Selected -> "Die #2 [${uiState.tutorialDice2}] ✓"
                else -> "Die #2 [${uiState.tutorialDice2}]"
              },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (die2Highlighted || die2Selected) FontWeight.ExtraBold else FontWeight.Medium,
              color = when {
                die2Highlighted -> TerracottaRed
                die2Selected -> BrushedBrass
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
        }

        // Primary Step Action CTA Button
        val isLastLesson = uiState.tutorialStepIndex == HowToPlayTutorialCatalog.LESSONS.lastIndex
        Button(
          onClick = {
            when (actionState) {
              TutorialInteractiveActionType.INSPECT_BOARD -> viewModel.performTutorialBoardOrPawnAction()
              TutorialInteractiveActionType.ROLL_DUAL_DICE -> viewModel.performTutorialRollDice()
              TutorialInteractiveActionType.SELECT_DIE_SLOT -> viewModel.performTutorialSelectDie(lesson.requiredDieSelectionIndex)
              TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN -> viewModel.performTutorialBoardOrPawnAction(lesson.targetTokenIndex)
              TutorialInteractiveActionType.LESSON_COMPLETED -> viewModel.advanceTutorialLesson()
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = when {
              isLessonCompleted -> EucalyptusSage
              isRollStep || isSelectDieStep || isPawnStep -> TerracottaRed
              else -> BrushedBrass
            },
            contentColor = if (isLessonCompleted || isRollStep || isSelectDieStep || isPawnStep) {
              AlabasterLinen
            } else {
              Obsidianink
            }
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .height(52.dp)
            .weight(1f)
            .padding(start = 10.dp)
            .testTag("tutorial_primary_action_button")
        ) {
          val ctaLabel = when (actionState) {
            TutorialInteractiveActionType.INSPECT_BOARD -> "Complete Overview"
            TutorialInteractiveActionType.ROLL_DUAL_DICE -> "Roll 3D Dice (6 & 4)"
            TutorialInteractiveActionType.SELECT_DIE_SLOT -> "Select Die #2 [${lesson.scriptedDie2}]"
            TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN -> {
              when (lesson.stepNumber) {
                2 -> "Deploy Pawn #1"
                3 -> "Move +5 to Star"
                4 -> "Capture Green Pawn"
                5 -> "Form 3D Blockade"
                6 -> "Ascend Goal Pyramid"
                else -> "Move Highlighted Pawn"
              }
            }
            TutorialInteractiveActionType.LESSON_COMPLETED -> {
              if (isLastLesson) {
                if (uiState.tutorialGraduationRewardClaimed) "Finish Academy" else "Claim +150 Coins"
              } else {
                "Next Lesson (${lesson.stepNumber + 1}/6)"
              }
            }
          }

          Icon(
            imageVector = when {
              isLessonCompleted && isLastLesson -> Icons.Default.EmojiEvents
              isLessonCompleted -> Icons.AutoMirrored.Filled.ArrowForward
              else -> Icons.Default.AutoAwesome
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = ctaLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}
