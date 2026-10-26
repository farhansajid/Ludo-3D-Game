package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LudoMultiplayerRepository
import com.example.data.LudoRoom
import com.example.data.PlayerColor
import com.example.data.PlayerProfile
import com.example.data.PlayerSeat
import com.example.data.RoomStatus
import com.example.data.TurnPhase
import com.example.domain.LudoRulesEngine
import com.example.ui.board3d.AnimatedTokenState
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
  LOBBY,
  WAITING_ROOM,
  GAME_ARENA
}

data class OfflineSeatDraft(
  val color: PlayerColor,
  val name: String,
  val isEnabled: Boolean,
  val isBot: Boolean
)

data class LudoUiState(
  val currentScreen: AppScreen = AppScreen.LOBBY,
  val isOnlineMatch: Boolean = false,
  val roomCode: String = "",
  val isHost: Boolean = true,
  val isPublicRoom: Boolean = true,
  val roomStatus: RoomStatus = RoomStatus.WAITING,
  val maxPlayers: Int = 4,
  val seats: List<PlayerSeat> = listOf(
    PlayerSeat(PlayerColor.RED, "HUMAN_0", "You", isBot = false),
    PlayerSeat(PlayerColor.GREEN, "BOT_1", "Atelier Bot", isBot = true),
    PlayerSeat(PlayerColor.YELLOW, "BOT_2", "Tactical AI", isBot = true),
    PlayerSeat(PlayerColor.BLUE, "BOT_3", "Archon AI", isBot = true)
  ),
  val offlineDraftSeats: List<OfflineSeatDraft> = listOf(
    OfflineSeatDraft(PlayerColor.RED, "Player 1", isEnabled = true, isBot = false),
    OfflineSeatDraft(PlayerColor.GREEN, "Atelier Bot", isEnabled = true, isBot = true),
    OfflineSeatDraft(PlayerColor.YELLOW, "Tactical AI", isEnabled = true, isBot = true),
    OfflineSeatDraft(PlayerColor.BLUE, "Archon AI", isEnabled = true, isBot = true)
  ),
  val tokens: List<Int> = List(16) { -1 },
  val currentTurnColor: PlayerColor = PlayerColor.RED,
  val turnPhase: TurnPhase = TurnPhase.ROLL,
  val diceValue: Int = 1,
  val isDiceRolling: Boolean = false,
  val consecutiveSixes: Int = 0,
  val turnNumber: Int = 1,
  val legalMoveTokenIndices: List<Int> = emptyList(),
  val animatedToken: AnimatedTokenState? = null,
  val turnSecondsRemaining: Int = TURN_DURATION_SECONDS,
  val statusBanner: String = "Roll a 6 to deploy a pawn onto the board",
  val winnerColor: String = "NONE",
  val winnerOrder: List<String> = emptyList(),
  val matchCapturesByMe: Int = 0,
  val activeReactionEmoji: String = "",
  val activeReactionSender: String = "",
  // 3D Camera state
  val cameraYaw: Float = -28f,
  val cameraPitch: Float = 52f,
  val cameraZoom: Float = 1.04f,
  val autoFocusTurnCamera: Boolean = false,
  // Modals & notifications
  val showRulesModal: Boolean = false,
  val isLoadingAction: Boolean = false,
  val errorMessage: String? = null
) {
  val activeBlockades: Map<Int, Set<PlayerColor>>
    get() = LudoRulesEngine.getActiveBlockades(tokens)

  companion object {
    const val TURN_DURATION_SECONDS = 20
  }
}

sealed interface DataStreamState<out T> {
  data object Loading : DataStreamState<Nothing>
  data class Success<T>(val data: T) : DataStreamState<T>
  data class Error(val message: String) : DataStreamState<Nothing>
}

class LudoGameViewModel(
  private val repository: LudoMultiplayerRepository,
  val currentUserId: String,
  initialDisplayName: String
) : ViewModel() {

  private val _uiState = MutableStateFlow(LudoUiState())
  val uiState: StateFlow<LudoUiState> = _uiState.asStateFlow()

  private var roomListenerJob: Job? = null
  private var turnTimerJob: Job? = null
  private var botActionJob: Job? = null
  private var isAnimatingMove: Boolean = false
  private var lastProcessedTurnKey: String = ""
  private var statsRecordedForRoom: String = ""

  // Real-time stream of player's profile
  val profileState: StateFlow<DataStreamState<PlayerProfile?>> = repository
    .observeProfile(currentUserId)
    .map<PlayerProfile?, DataStreamState<PlayerProfile?>> { DataStreamState.Success(it) }
    .catch { error ->
      Log.w(TAG, "Error observing profile", error)
      emit(DataStreamState.Error(error.message ?: "Failed to load profile"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = DataStreamState.Loading
    )

  // Real-time stream of open public rooms in the lobby
  val openPublicRoomsState: StateFlow<DataStreamState<List<LudoRoom>>> = repository
    .observeOpenPublicRooms()
    .map<List<LudoRoom>, DataStreamState<List<LudoRoom>>> { DataStreamState.Success(it) }
    .catch { error ->
      Log.w(TAG, "Error observing public rooms", error)
      emit(DataStreamState.Error(error.message ?: "Failed to load open rooms"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = DataStreamState.Loading
    )

  init {
    viewModelScope.launch {
      repository.ensureOrUpdateProfile(initialDisplayName)
    }
  }

  private fun currentDisplayName(): String {
    val profile = (profileState.value as? DataStreamState.Success)?.data
    return profile?.displayName?.ifBlank { "Royal Player" } ?: "Royal Player"
  }

  fun updateDisplayName(newName: String) {
    val trimmed = newName.trim().take(40)
    if (trimmed.isBlank()) return
    viewModelScope.launch {
      repository.ensureOrUpdateProfile(trimmed)
    }
  }

  fun toggleRulesModal(show: Boolean) {
    _uiState.update { it.copy(showRulesModal = show) }
  }

  fun clearErrorMessage() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  // --- 3D CAMERA CONTROLS ---

  fun adjustCamera(deltaYaw: Float, deltaPitch: Float, zoomFactor: Float) {
    _uiState.update { state ->
      state.copy(
        cameraYaw = (state.cameraYaw + deltaYaw) % 360f,
        cameraPitch = (state.cameraPitch + deltaPitch).coerceIn(25f, 82f),
        cameraZoom = (state.cameraZoom * zoomFactor).coerceIn(0.75f, 1.85f)
      )
    }
  }

  fun setCameraPreset(yaw: Float, pitch: Float, zoom: Float = 1.04f) {
    _uiState.update {
      it.copy(
        cameraYaw = yaw,
        cameraPitch = pitch.coerceIn(25f, 82f),
        cameraZoom = zoom.coerceIn(0.75f, 1.85f)
      )
    }
  }

  fun cycleCameraPerspective() {
    val currentPitch = _uiState.value.cameraPitch
    when {
      currentPitch < 42f -> setCameraPreset(yaw = -28f, pitch = 54f, zoom = 1.04f)
      currentPitch < 68f -> setCameraPreset(yaw = 0f, pitch = 78f, zoom = 1.08f)
      else -> setCameraPreset(yaw = -42f, pitch = 33f, zoom = 1.12f)
    }
  }

  fun toggleAutoFocusTurnCamera() {
    _uiState.update { state ->
      val next = !state.autoFocusTurnCamera
      val targetYaw = if (next) yawForColor(state.currentTurnColor) else state.cameraYaw
      state.copy(autoFocusTurnCamera = next, cameraYaw = targetYaw)
    }
  }

  private fun yawForColor(color: PlayerColor): Float {
    return when (color) {
      PlayerColor.RED -> -35f
      PlayerColor.GREEN -> -125f
      PlayerColor.YELLOW -> 145f
      PlayerColor.BLUE -> 55f
    }
  }

  // --- OFFLINE STUDIO SETUP ---

  fun updateOfflineSeatDraft(color: PlayerColor, isEnabled: Boolean? = null, isBot: Boolean? = null, name: String? = null) {
    _uiState.update { state ->
      val updated = state.offlineDraftSeats.map { seat ->
        if (seat.color == color) {
          val nextBot = isBot ?: seat.isBot
          val defaultName = if (nextBot) "${color.label.substringBefore(" ")} AI" else "Player ${color.ordinal + 1}"
          seat.copy(
            isEnabled = isEnabled ?: seat.isEnabled,
            isBot = nextBot,
            name = name ?: if (isBot != null) defaultName else seat.name
          )
        } else {
          seat
        }
      }
      // Guarantee at least 2 enabled seats
      val enabledCount = updated.count { it.isEnabled }
      if (enabledCount < 2) state else state.copy(offlineDraftSeats = updated)
    }
  }

  fun startOfflineGame() {
    roomListenerJob?.cancel()
    val drafts = _uiState.value.offlineDraftSeats.filter { it.isEnabled }
    val seats = drafts.mapIndexed { idx, draft ->
      PlayerSeat(
        color = draft.color,
        uid = if (draft.isBot) "BOT_$idx" else "$currentUserId-LOCAL-$idx",
        name = draft.name.ifBlank { draft.color.label },
        isBot = draft.isBot
      )
    }
    val firstColor = seats.first().color
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.GAME_ARENA,
        isOnlineMatch = false,
        roomCode = "LOCAL-3D",
        isHost = true,
        roomStatus = RoomStatus.PLAYING,
        maxPlayers = seats.size,
        seats = seats,
        tokens = List(16) { -1 },
        currentTurnColor = firstColor,
        turnPhase = TurnPhase.ROLL,
        diceValue = 1,
        isDiceRolling = false,
        consecutiveSixes = 0,
        turnNumber = 1,
        legalMoveTokenIndices = emptyList(),
        animatedToken = null,
        turnSecondsRemaining = LudoUiState.TURN_DURATION_SECONDS,
        statusBanner = "Match started! ${seats.first().name}'s turn to roll.",
        winnerColor = "NONE",
        winnerOrder = emptyList(),
        matchCapturesByMe = 0,
        errorMessage = null
      )
    }
    onNewTurnReady()
  }

  // --- ONLINE MULTIPLAYER LOBBY & ROOMS ---

  fun createOnlinePrivateRoom(maxPlayers: Int = 4, isPublic: Boolean = false) {
    val code = LudoMultiplayerRepository.generateRoomCode()
    _uiState.update { it.copy(isLoadingAction = true, errorMessage = null) }
    viewModelScope.launch {
      val result = repository.createRoom(
        roomCode = code,
        hostName = currentDisplayName(),
        isPublic = isPublic,
        maxPlayers = maxPlayers
      )
      result.onSuccess { room ->
        _uiState.update {
          it.copy(
            isLoadingAction = false,
            currentScreen = AppScreen.WAITING_ROOM,
            isOnlineMatch = true,
            roomCode = room.roomCode,
            isHost = true,
            isPublicRoom = room.isPublic,
            maxPlayers = room.maxPlayers.toInt()
          )
        }
        subscribeToOnlineRoom(room.roomCode)
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isLoadingAction = false,
            errorMessage = err.localizedMessage ?: "Failed to create room"
          )
        }
      }
    }
  }

  fun joinOnlineRoomByCode(codeInput: String) {
    val cleanCode = codeInput.trim().uppercase()
    if (cleanCode.length < 4) {
      _uiState.update { it.copy(errorMessage = "Please enter a valid 6-character room code.") }
      return
    }
    _uiState.update { it.copy(isLoadingAction = true, errorMessage = null) }
    viewModelScope.launch {
      val result = repository.joinRoomByCode(cleanCode, currentDisplayName())
      result.onSuccess { room ->
        val nextScreen = if (room.status == RoomStatus.PLAYING.name) {
          AppScreen.GAME_ARENA
        } else {
          AppScreen.WAITING_ROOM
        }
        _uiState.update {
          it.copy(
            isLoadingAction = false,
            currentScreen = nextScreen,
            isOnlineMatch = true,
            roomCode = room.roomCode,
            isHost = room.hostUid == currentUserId,
            isPublicRoom = room.isPublic,
            maxPlayers = room.maxPlayers.toInt()
          )
        }
        subscribeToOnlineRoom(room.roomCode)
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isLoadingAction = false,
            errorMessage = err.localizedMessage ?: "Could not join room $cleanCode"
          )
        }
      }
    }
  }

  fun startQuickOnlineMatch(maxPlayers: Int = 2) {
    _uiState.update { it.copy(isLoadingAction = true, errorMessage = null) }
    viewModelScope.launch {
      val result = repository.findOrCreateQuickMatch(
        playerName = currentDisplayName(),
        maxPlayers = maxPlayers
      )
      result.onSuccess { room ->
        val nextScreen = if (room.status == RoomStatus.PLAYING.name) {
          AppScreen.GAME_ARENA
        } else {
          AppScreen.WAITING_ROOM
        }
        _uiState.update {
          it.copy(
            isLoadingAction = false,
            currentScreen = nextScreen,
            isOnlineMatch = true,
            roomCode = room.roomCode,
            isHost = room.hostUid == currentUserId,
            isPublicRoom = true,
            maxPlayers = room.maxPlayers.toInt()
          )
        }
        subscribeToOnlineRoom(room.roomCode)
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isLoadingAction = false,
            errorMessage = err.localizedMessage ?: "Quick matchmaking failed"
          )
        }
      }
    }
  }

  fun hostStartOnlineMatch(fillWithBots: Boolean) {
    val code = _uiState.value.roomCode
    if (code.isBlank()) return
    _uiState.update { it.copy(isLoadingAction = true) }
    viewModelScope.launch {
      val result = repository.startMatchWithOptionalBots(code, fillWithBots)
      result.onSuccess {
        _uiState.update { it.copy(isLoadingAction = false, currentScreen = AppScreen.GAME_ARENA) }
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isLoadingAction = false,
            errorMessage = err.localizedMessage ?: "Could not start match"
          )
        }
      }
    }
  }

  private fun subscribeToOnlineRoom(roomCode: String) {
    roomListenerJob?.cancel()
    lastProcessedTurnKey = ""
    roomListenerJob = viewModelScope.launch {
      repository.observeRoom(roomCode)
        .catch { err ->
          Log.w(TAG, "Room stream error", err)
          _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Lost connection to room") }
        }
        .collect { room ->
          if (room == null) return@collect
          applyRemoteRoomSnapshot(room)
        }
    }
  }

  private fun applyRemoteRoomSnapshot(room: LudoRoom) {
    val status = RoomStatus.fromString(room.status)
    val turnColor = PlayerColor.fromString(room.currentTurnColor)
    val phase = TurnPhase.fromString(room.turnPhase)
    val remoteTokens = room.tokenPositions
    val remoteDice = room.diceValue.toInt().coerceIn(0, 6)
    val legalIndices = if (phase == TurnPhase.MOVE && remoteDice in 1..6) {
      LudoRulesEngine.getLegalMoveTokenIndices(remoteTokens, turnColor, remoteDice)
    } else {
      emptyList()
    }

    val targetScreen = when {
      status == RoomStatus.WAITING && _uiState.value.currentScreen == AppScreen.WAITING_ROOM -> AppScreen.WAITING_ROOM
      status == RoomStatus.PLAYING || status == RoomStatus.FINISHED -> AppScreen.GAME_ARENA
      else -> _uiState.value.currentScreen
    }

    _uiState.update { state ->
      val nextYaw = if (state.autoFocusTurnCamera) yawForColor(turnColor) else state.cameraYaw
      state.copy(
        currentScreen = targetScreen,
        roomCode = room.roomCode,
        isHost = room.hostUid == currentUserId,
        isPublicRoom = room.isPublic,
        roomStatus = status,
        maxPlayers = room.maxPlayers.toInt(),
        seats = room.seats,
        tokens = if (isAnimatingMove) state.tokens else remoteTokens,
        currentTurnColor = turnColor,
        turnPhase = phase,
        diceValue = if (remoteDice == 0) 1 else remoteDice,
        consecutiveSixes = room.consecutiveSixes.toInt(),
        turnNumber = room.turnNumber.toInt(),
        legalMoveTokenIndices = legalIndices,
        statusBanner = room.lastEvent,
        winnerColor = room.winnerColor,
        winnerOrder = room.winnerOrder,
        activeReactionEmoji = room.reactionEmoji,
        activeReactionSender = room.reactionSender,
        cameraYaw = nextYaw
      )
    }

    if (status == RoomStatus.FINISHED && statsRecordedForRoom != room.roomCode) {
      statsRecordedForRoom = room.roomCode
      val mySeat = room.seats.firstOrNull { it.uid == currentUserId }
      if (mySeat != null) {
        val iWon = room.winnerColor == mySeat.color.name
        viewModelScope.launch {
          repository.recordMatchStats(won = iWon, capturesInMatch = _uiState.value.matchCapturesByMe)
        }
      }
    }

    val turnKey = "${room.roomCode}-${room.turnNumber}-${room.currentTurnColor}-${room.turnPhase}-${room.consecutiveSixes}"
    if (status == RoomStatus.PLAYING && turnKey != lastProcessedTurnKey) {
      lastProcessedTurnKey = turnKey
      onNewTurnReady()
    }
  }

  // --- TURN EXECUTION, DICE ROLL, AUTO-MOVE, AND TIMERS ---

  fun canCurrentUserAct(): Boolean {
    val state = _uiState.value
    if (state.roomStatus != RoomStatus.PLAYING || state.turnPhase == TurnPhase.GAME_OVER) return false
    if (state.isDiceRolling || isAnimatingMove) return false
    val activeSeat = state.seats.firstOrNull { it.color == state.currentTurnColor } ?: return false
    if (activeSeat.isBot) return false
    return if (state.isOnlineMatch) {
      activeSeat.uid == currentUserId
    } else {
      true
    }
  }

  private fun isCurrentTurnControlledLocally(): Boolean {
    val state = _uiState.value
    if (state.roomStatus != RoomStatus.PLAYING || state.turnPhase == TurnPhase.GAME_OVER) return false
    val activeSeat = state.seats.firstOrNull { it.color == state.currentTurnColor } ?: return false
    return if (!state.isOnlineMatch) {
      true
    } else if (activeSeat.isBot) {
      state.isHost // Host executes bot turns in online matches
    } else {
      activeSeat.uid == currentUserId
    }
  }

  private fun onNewTurnReady() {
    turnTimerJob?.cancel()
    botActionJob?.cancel()

    val state = _uiState.value
    if (state.roomStatus != RoomStatus.PLAYING || state.turnPhase == TurnPhase.GAME_OVER) return

    // Start 20-second turn countdown timer
    turnTimerJob = viewModelScope.launch {
      for (remaining in LudoUiState.TURN_DURATION_SECONDS downTo 1) {
        _uiState.update { it.copy(turnSecondsRemaining = remaining) }
        delay(1000L)
      }
      _uiState.update { it.copy(turnSecondsRemaining = 0) }
      if (isCurrentTurnControlledLocally()) {
        when (_uiState.value.turnPhase) {
          TurnPhase.ROLL -> onRollDiceClicked(triggeredByBotOrTimer = true)
          TurnPhase.MOVE -> {
            val bestIdx = LudoRulesEngine.chooseBestMove(
              tokens = _uiState.value.tokens,
              color = _uiState.value.currentTurnColor,
              diceValue = _uiState.value.diceValue
            )
            if (bestIdx != null) {
              onTokenSelected(bestIdx, triggeredByBotOrAuto = true)
            }
          }
          TurnPhase.GAME_OVER -> {}
        }
      }
    }

    val activeSeat = state.seats.firstOrNull { it.color == state.currentTurnColor }
    val shouldRunBot = activeSeat?.isBot == true && (!state.isOnlineMatch || state.isHost)

    if (shouldRunBot && state.turnPhase == TurnPhase.ROLL) {
      botActionJob = viewModelScope.launch {
        delay(650L)
        onRollDiceClicked(triggeredByBotOrTimer = true)
      }
    } else if (state.turnPhase == TurnPhase.MOVE && isCurrentTurnControlledLocally()) {
      // Smart Auto-Move when only 1 legal move exists (or if bot is in MOVE phase)
      val legal = state.legalMoveTokenIndices
      if (legal.size == 1 || shouldRunBot) {
        botActionJob = viewModelScope.launch {
          delay(420L)
          val chosen = if (legal.size == 1) {
            legal.first()
          } else {
            LudoRulesEngine.chooseBestMove(state.tokens, state.currentTurnColor, state.diceValue)
          }
          if (chosen != null) {
            onTokenSelected(chosen, triggeredByBotOrAuto = true)
          }
        }
      }
    }
  }

  fun onRollDiceClicked(triggeredByBotOrTimer: Boolean = false) {
    val state = _uiState.value
    if (state.turnPhase != TurnPhase.ROLL || state.isDiceRolling || isAnimatingMove) return
    if (!triggeredByBotOrTimer && !canCurrentUserAct()) return

    viewModelScope.launch {
      _uiState.update { it.copy(isDiceRolling = true) }
      delay(460L)

      val rolled = Random.nextInt(1, 7)
      val currentColor = _uiState.value.currentTurnColor
      val activeSeatName = _uiState.value.seats.firstOrNull { it.color == currentColor }?.name ?: currentColor.label
      val newSixes = if (rolled == 6) _uiState.value.consecutiveSixes + 1 else 0

      // Rule: Three consecutive 6s forfeit the current turn immediately
      if (newSixes >= 3) {
        val activeColors = _uiState.value.seats.map { it.color }
        val nextColor = LudoRulesEngine.nextActiveColor(
          current = currentColor,
          activeSeats = activeColors,
          finishedColors = _uiState.value.winnerOrder
        )
        val forfeitMsg = "$activeSeatName rolled three 6s in a row — turn forfeited!"
        _uiState.update {
          it.copy(
            diceValue = 6,
            isDiceRolling = false,
            consecutiveSixes = 0,
            currentTurnColor = nextColor,
            turnPhase = TurnPhase.ROLL,
            turnNumber = it.turnNumber + 1,
            legalMoveTokenIndices = emptyList(),
            statusBanner = forfeitMsg,
            cameraYaw = if (it.autoFocusTurnCamera) yawForColor(nextColor) else it.cameraYaw
          )
        }
        syncStateIfOnline()
        onNewTurnReady()
        return@launch
      }

      val legalIndices = LudoRulesEngine.getLegalMoveTokenIndices(
        tokens = _uiState.value.tokens,
        color = currentColor,
        diceValue = rolled
      )

      if (legalIndices.isEmpty()) {
        // No legal moves: brief pause, then advance turn
        val noMoveMsg = "$activeSeatName rolled $rolled — no valid moves available"
        _uiState.update {
          it.copy(
            diceValue = rolled,
            isDiceRolling = false,
            legalMoveTokenIndices = emptyList(),
            statusBanner = noMoveMsg
          )
        }
        delay(780L)
        val activeColors = _uiState.value.seats.map { it.color }
        val nextColor = LudoRulesEngine.nextActiveColor(
          current = currentColor,
          activeSeats = activeColors,
          finishedColors = _uiState.value.winnerOrder
        )
        _uiState.update {
          it.copy(
            currentTurnColor = nextColor,
            turnPhase = TurnPhase.ROLL,
            consecutiveSixes = 0,
            turnNumber = it.turnNumber + 1,
            statusBanner = "$noMoveMsg. Next: ${nextColor.label}",
            cameraYaw = if (it.autoFocusTurnCamera) yawForColor(nextColor) else it.cameraYaw
          )
        }
        syncStateIfOnline()
        onNewTurnReady()
      } else {
        val movePrompt = if (legalIndices.size == 1) {
          "$activeSeatName rolled $rolled — Smart Auto-Move advancing pawn"
        } else {
          "$activeSeatName rolled $rolled — tap a glowing 3D pawn to move"
        }
        _uiState.update {
          it.copy(
            diceValue = rolled,
            isDiceRolling = false,
            consecutiveSixes = newSixes,
            turnPhase = TurnPhase.MOVE,
            legalMoveTokenIndices = legalIndices,
            statusBanner = movePrompt
          )
        }
        syncStateIfOnline()
        onNewTurnReady()
      }
    }
  }

  fun onTokenSelected(tokenIndex: Int, triggeredByBotOrAuto: Boolean = false) {
    val state = _uiState.value
    if (state.turnPhase != TurnPhase.MOVE || isAnimatingMove || state.isDiceRolling) return
    if (tokenIndex !in state.legalMoveTokenIndices) return
    if (!triggeredByBotOrAuto && !canCurrentUserAct()) return

    isAnimatingMove = true
    turnTimerJob?.cancel()
    botActionJob?.cancel()

    viewModelScope.launch {
      val startTokens = state.tokens
      val startPos = startTokens[tokenIndex]
      val outcome = LudoRulesEngine.executeMove(
        tokens = startTokens,
        seats = state.seats,
        currentTurnColor = state.currentTurnColor,
        tokenIndex = tokenIndex,
        diceValue = state.diceValue,
        consecutiveSixes = state.consecutiveSixes,
        currentWinnerColor = state.winnerColor,
        currentWinnerOrder = state.winnerOrder
      )

      // Animate token step-by-step along parabolic 3D arcs
      var stepFrom = startPos
      val workingTokens = startTokens.toMutableList()
      for (stepTo in outcome.pathSteps) {
        val frames = 7
        for (frame in 1..frames) {
          val progress = frame.toFloat() / frames.toFloat()
          _uiState.update {
            it.copy(
              animatedToken = AnimatedTokenState(
                tokenIndex = tokenIndex,
                fromPos = stepFrom,
                toPos = stepTo,
                progress = progress
              )
            )
          }
          delay(22L)
        }
        workingTokens[tokenIndex] = stepTo
        _uiState.update {
          it.copy(
            tokens = workingTokens.toList(),
            animatedToken = null
          )
        }
        stepFrom = stepTo
      }

      val isGameOver = outcome.nextTurnPhase == TurnPhase.GAME_OVER
      val nextRoomStatus = if (isGameOver) RoomStatus.FINISHED else RoomStatus.PLAYING
      val myCapturesDelta = if (outcome.capturedTokenIndices.isNotEmpty() && canUserClaimStats(state.currentTurnColor)) {
        outcome.capturedTokenIndices.size
      } else {
        0
      }

      isAnimatingMove = false
      _uiState.update {
        it.copy(
          roomStatus = nextRoomStatus,
          tokens = outcome.updatedTokens,
          currentTurnColor = outcome.nextTurnColor,
          turnPhase = outcome.nextTurnPhase,
          consecutiveSixes = outcome.nextConsecutiveSixes,
          turnNumber = it.turnNumber + 1,
          legalMoveTokenIndices = emptyList(),
          animatedToken = null,
          statusBanner = outcome.eventDescription,
          winnerColor = outcome.winnerColor,
          winnerOrder = outcome.updatedWinnerOrder,
          matchCapturesByMe = it.matchCapturesByMe + myCapturesDelta,
          cameraYaw = if (it.autoFocusTurnCamera) yawForColor(outcome.nextTurnColor) else it.cameraYaw
        )
      }

      if (isGameOver && !state.isOnlineMatch) {
        val humanSeat = state.seats.firstOrNull { !it.isBot }
        if (humanSeat != null) {
          repository.recordMatchStats(
            won = outcome.winnerColor == humanSeat.color.name,
            capturesInMatch = _uiState.value.matchCapturesByMe
          )
        }
      }

      syncStateIfOnline()
      onNewTurnReady()
    }
  }

  private fun canUserClaimStats(color: PlayerColor): Boolean {
    val seat = _uiState.value.seats.firstOrNull { it.color == color } ?: return false
    return !seat.isBot && (seat.uid == currentUserId || !_uiState.value.isOnlineMatch)
  }

  private suspend fun syncStateIfOnline() {
    val state = _uiState.value
    if (!state.isOnlineMatch || state.roomCode.isBlank()) return
    repository.pushRoomGameState(
      roomCode = state.roomCode,
      status = state.roomStatus,
      tokens = state.tokens,
      currentTurnColor = state.currentTurnColor,
      turnPhase = state.turnPhase,
      diceValue = state.diceValue,
      consecutiveSixes = state.consecutiveSixes,
      turnNumber = state.turnNumber,
      lastEvent = state.statusBanner,
      winnerColor = state.winnerColor,
      winnerOrder = state.winnerOrder
    )
  }

  fun sendQuickEmojiReaction(emoji: String) {
    val state = _uiState.value
    val sender = currentDisplayName()
    _uiState.update {
      it.copy(
        activeReactionEmoji = emoji,
        activeReactionSender = sender
      )
    }
    if (state.isOnlineMatch && state.roomCode.isNotBlank()) {
      viewModelScope.launch {
        repository.sendEmojiReaction(state.roomCode, emoji, sender)
      }
    }
  }

  fun leaveToLobby() {
    roomListenerJob?.cancel()
    turnTimerJob?.cancel()
    botActionJob?.cancel()
    isAnimatingMove = false
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.LOBBY,
        isOnlineMatch = false,
        roomCode = "",
        legalMoveTokenIndices = emptyList(),
        animatedToken = null,
        errorMessage = null
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    roomListenerJob?.cancel()
    turnTimerJob?.cancel()
    botActionJob?.cancel()
  }

  private companion object {
    const val TAG = "LudoGameVM"
  }
}
