package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActivePawnEmoteEffect
import com.example.data.AdminAppConfig
import com.example.data.AudioCallMode
import com.example.data.CountryCatalog
import com.example.data.CustomAvatarOption
import com.example.data.DailyMission
import com.example.data.DailyMissionCatalog
import com.example.data.EmoteCategory
import com.example.data.EmoteGalleryCatalog
import com.example.data.EmoteMomentTrigger
import com.example.data.FriendEntryUiModel
import com.example.data.FriendPresenceState
import com.example.data.FriendRequest
import com.example.data.FriendRequestStatus
import com.example.data.FriendsStudioCatalog
import com.example.data.HowToPlayTutorialCatalog
import com.example.data.LudoMultiplayerRepository
import com.example.data.LudoRoom
import com.example.data.MatchOutcomeType
import com.example.data.MatchReplayRecord
import com.example.data.MissionType
import com.example.data.PawnEmoteItem
import com.example.data.PawnMoveSpeed
import com.example.data.PlayerColor
import com.example.data.PlayerProfile
import com.example.data.PlayerSeat
import com.example.data.PrivateRoomChatCatalog
import com.example.data.ProfileCatalog
import com.example.data.ProfileMatchHistoryEntry
import com.example.data.QuickReplyChatOption
import com.example.data.ReplayCameraPreset
import com.example.data.ReplayFrame
import com.example.data.RoomChatMessage
import com.example.data.RoomStatus
import com.example.data.SampleReplayCatalog
import com.example.data.StoreItem
import com.example.data.StoreItemType
import com.example.data.TournamentBracketMatch
import com.example.data.TournamentCosmeticReward
import com.example.data.TournamentRoundTier
import com.example.data.TurnPhase
import com.example.data.Tutorial3DBeaconSpec
import com.example.data.TutorialHighlightZoneType
import com.example.data.TutorialInteractiveActionType
import com.example.data.TutorialLessonStep
import com.example.data.WeeklyTournamentCatalog
import com.example.domain.InGameHudTelemetry
import com.example.domain.InGamePlayerHudStats
import com.example.domain.LudoRulesEngine
import com.example.ui.audio.AudioCallManager
import com.example.ui.audio.AudioCallUiState
import com.example.ui.audio.DefaultHapticEffectRepository
import com.example.ui.audio.DefaultSoundEffectRepository
import com.example.ui.audio.HapticEffectRepository
import com.example.ui.audio.HapticFeedbackManager
import com.example.ui.audio.HapticPatternSpec
import com.example.ui.audio.LudoHapticEvent
import com.example.ui.audio.LudoSoundEvent
import com.example.ui.audio.SoundEffectRepository
import com.example.ui.audio.SoundEffectsManager
import com.example.ui.board3d.AnimatedTokenState
import com.example.ui.board3d.ParticleBurstEvent
import com.example.ui.board3d.ParticleEffectType
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
  LOBBY,
  WAITING_ROOM,
  GAME_ARENA,
  LEADERBOARD,
  STORE,
  ADMIN_PANEL,
  REPLAY_STUDIO,
  EMOTE_GALLERY,
  WEEKLY_TOURNAMENT,
  PROFILE,
  FRIENDS,
  HOW_TO_PLAY_TUTORIAL
}

data class OfflineSeatDraft(
  val seatIndex: Int,
  val color: PlayerColor,
  val name: String,
  val isEnabled: Boolean,
  val isBot: Boolean
)

data class LudoUiState(
  val currentScreen: AppScreen = AppScreen.LOBBY,
  val isOnlineMatch: Boolean = false,
  val isSpectating: Boolean = false,
  val spectatingTargetName: String = "",
  val spectatorCount: Int = 1,
  val roomCode: String = "",
  val isHost: Boolean = true,
  val isPublicRoom: Boolean = true,
  val roomStatus: RoomStatus = RoomStatus.WAITING,
  val maxPlayers: Int = 4,
  val preferredOnlineColor: PlayerColor = PlayerColor.RED,
  val seats: List<PlayerSeat> = listOf(
    PlayerSeat(PlayerColor.RED, "HUMAN_0", "You", isBot = false),
    PlayerSeat(PlayerColor.GREEN, "BOT_1", "Atelier Bot", isBot = true),
    PlayerSeat(PlayerColor.YELLOW, "BOT_2", "Tactical AI", isBot = true),
    PlayerSeat(PlayerColor.BLUE, "BOT_3", "Archon AI", isBot = true)
  ),
  val offlinePlayerCount: Int = 4,
  val offlineDraftSeats: List<OfflineSeatDraft> = buildDefaultOfflineSeats(4),
  val tokens: List<Int> = List(32) { -1 },
  val currentTurnColor: PlayerColor = PlayerColor.RED,
  val turnPhase: TurnPhase = TurnPhase.ROLL,
  // Dual-dice state
  val diceValue: Int = 1,
  val diceValue2: Int = 6,
  val dice1Used: Boolean = false,
  val dice2Used: Boolean = false,
  val selectedDieIndex: Int = 0,
  val isDiceRolling: Boolean = false,
  val consecutiveSixes: Int = 0,
  val turnBonusPending: Boolean = false,
  val turnNumber: Int = 1,
  val legalMoveTokenIndices: List<Int> = emptyList(),
  val animatedToken: AnimatedTokenState? = null,
  val pawnMoveSpeed: PawnMoveSpeed = PawnMoveSpeed.FAST,
  val isSoundEnabled: Boolean = true,
  val soundVolume: Float = 1.0f,
  val lastPlayedSoundClipTitle: String = "",
  val isHapticsEnabled: Boolean = true,
  val lastTriggeredHapticTitle: String = "",
  val activeHapticSpec: HapticPatternSpec? = null,
  val hapticPulseSequence: Long = 0L,
  val activeParticleBurst: ParticleBurstEvent? = null,
  val turnSecondsRemaining: Int = TURN_DURATION_SECONDS,
  val statusBanner: String = "Roll 2 dice — any 6 deploys a pawn onto the board",
  val winnerColor: String = "NONE",
  val winnerOrder: List<String> = emptyList(),
  val matchCapturesByMe: Int = 0,
  val matchHomesByMe: Int = 0,
  val activeReactionEmoji: String = "",
  val activeReactionSender: String = "",
  // Private Room Real-Time Text Chat & Dual-Dice Sequential Sync state
  val roomChatMessages: List<RoomChatMessage> = PrivateRoomChatCatalog.buildInitialShowcaseMessages("PRV409"),
  val chatDraftText: String = "",
  val isRoomChatOpen: Boolean = false,
  val unreadChatCount: Int = 0,
  val lastProfanityFilterNotice: String? = null,
  val lastSyncedDieChoiceSummary: String = "Step 1/2 • Select Die #1 or Die #2 to preview legal 3D pawn paths",
  // Daily Mission & Currency/XP state (combines session + Firestore profile)
  val sessionWinsCount: Int = 0,
  val sessionLossesCount: Int = 0,
  val sessionSixesRolledCount: Int = 0,
  val sessionCapturesCount: Int = 0,
  val sessionHomesCount: Int = 0,
  val sessionOpensCount: Int = 0,
  val localGuestCoins: Long = 250L,
  val localGuestXp: Long = 0L,
  val localClaimedMissionIds: List<String> = emptyList(),
  val localUnlockedItems: List<String> = emptyList(),
  val localMatchHistory: List<ProfileMatchHistoryEntry> = ProfileCatalog.DEFAULT_SAMPLE_HISTORY,
  val localDisplayName: String = "Royal Player",
  val selectedAvatarId: String = ProfileCatalog.DEFAULT_AVATAR_ID,
  val firebaseAuthPhotoUri: String = "ludo-avatar://${ProfileCatalog.DEFAULT_AVATAR_ID}",
  // Voice call room metadata synced from Firestore
  val roomActiveVoiceUids: List<String> = emptyList(),
  val roomCallMode: AudioCallMode = AudioCallMode.NONE,
  val roomCallInitiatorUid: String = "",
  val roomCallTargetUid: String = "",
  // Leaderboard filter state
  val selectedCountryFilter: String = CountryCatalog.detectDefaultCountry().code,
  // 3D Camera state
  val cameraYaw: Float = -28f,
  val cameraPitch: Float = 52f,
  val cameraZoom: Float = 1.04f,
  val autoFocusTurnCamera: Boolean = false,
  // Match Recording & Replay Studio state
  val isMatchRecordingEnabled: Boolean = true,
  val activeMatchId: String = "LIVE-MATCH-1",
  val activeMatchRecordedFrames: List<ReplayFrame> = emptyList(),
  val savedReplays: List<MatchReplayRecord> = SampleReplayCatalog.SHOWCASE_REPLAYS,
  val selectedReplayId: String = SampleReplayCatalog.SHOWCASE_REPLAYS.first().id,
  val replayFrameIndex: Int = 0,
  val isReplayPlaying: Boolean = false,
  val replaySpeedMultiplier: Float = 1f,
  val selectedReplayCameraPreset: ReplayCameraPreset = ReplayCameraPreset.ISOMETRIC_BROADCAST,
  // Emote Gallery & 3D Pawn Emotes state
  val selectedEmoteIdForPreview: String = EmoteGalleryCatalog.ALL_EMOTES.first().id,
  val selectedEmoteMomentSlot: EmoteMomentTrigger = EmoteMomentTrigger.OPPONENT_CAPTURE,
  val selectedEmoteCategoryFilter: EmoteCategory? = null,
  val equippedEmotesByMoment: Map<EmoteMomentTrigger, String> = EmoteGalleryCatalog.DEFAULT_EQUIPPED_LOADOUT,
  val activePawnEmote: ActivePawnEmoteEffect? = null,
  // Weekly Tournament Ladder Bracket & Live Updates state
  val tournamentSeasonTitle: String = WeeklyTournamentCatalog.CURRENT_SEASON_TITLE,
  val tournamentRemainingSeconds: Long = WeeklyTournamentCatalog.DEFAULT_REMAINING_SECONDS,
  val tournamentCurrentRound: TournamentRoundTier = TournamentRoundTier.QUARTERFINALS,
  val tournamentWins: Int = 0,
  val tournamentLadderPoints: Int = 120,
  val tournamentBracketMatches: List<TournamentBracketMatch> = WeeklyTournamentCatalog.buildInitialBracket(),
  val tournamentClaimedRewardIds: List<String> = emptyList(),
  val equippedTournamentCosmeticId: String = WeeklyTournamentCatalog.EXCLUSIVE_REWARDS.first().id,
  val isTournamentMatchActive: Boolean = false,
  val tournamentLiveFeedMessage: String = "LIVE BRACKET • Quarterfinals underway — win 3 bracket matches to claim the Sovereign Cup!",
  // Friends System state (search by username, friend requests, online status & active rooms)
  val selectedFriendsTab: Int = 0, // 0 = My Friends, 1 = Requests, 2 = Find Players
  val friendsSearchQuery: String = "",
  val friendsSearchResults: List<PlayerProfile> = FriendsStudioCatalog.STUDIO_SEARCHABLE_DIRECTORY,
  val isSearchingPlayers: Boolean = false,
  val localStudioFriends: List<FriendEntryUiModel> = FriendsStudioCatalog.STUDIO_SHOWCASE_FRIENDS,
  val localIncomingRequests: List<FriendRequest> = listOf(
    FriendRequest(
      id = "demo_req_incoming_kenji",
      fromUid = "demo_uid_kenji",
      fromName = "Kenji Takahashi",
      fromCountry = "JP",
      toUid = "LOCAL_PLAYER",
      toName = "You",
      toCountry = "US",
      status = FriendRequestStatus.PENDING.name,
      memberUids = listOf("demo_uid_kenji", "LOCAL_PLAYER")
    )
  ),
  val localSentRequests: List<FriendRequest> = emptyList(),
  // Interactive 'How to Play' 3D Tutorial state
  val tutorialStepIndex: Int = 0,
  val tutorialActionState: TutorialInteractiveActionType = HowToPlayTutorialCatalog.LESSONS.first().initialActionType,
  val tutorialTokens: List<Int> = HowToPlayTutorialCatalog.LESSONS.first().initialTokens,
  val tutorialDice1: Int = HowToPlayTutorialCatalog.LESSONS.first().scriptedDie1,
  val tutorialDice2: Int = HowToPlayTutorialCatalog.LESSONS.first().scriptedDie2,
  val tutorialSelectedDieIndex: Int = 0,
  val tutorialDiceRolledInStep: Boolean = false,
  val tutorialIsRollingDice: Boolean = false,
  val tutorialAnimatedToken: AnimatedTokenState? = null,
  val tutorialCompletedStepIds: Set<String> = emptySet(),
  val tutorialGraduationRewardClaimed: Boolean = false,
  val tutorialCoachBanner: String = HowToPlayTutorialCatalog.LESSONS.first().coachInstruction,
  // Modals, HUD Overlay & notifications
  val isHudOverlayVisible: Boolean = true,
  val isHudOverlayExpanded: Boolean = false,
  val showRulesModal: Boolean = false,
  val isLoadingAction: Boolean = false,
  val errorMessage: String? = null,
  val toastMessage: String? = null
) {
  val boardArmCount: Int
    get() = LudoRulesEngine.boardArmCountForPlayers(maxOf(maxPlayers, seats.size))

  val activeBlockades: Map<Int, Set<PlayerColor>>
    get() = LudoRulesEngine.getActiveBlockades(tokens, seats, boardArmCount)

  val activeDieValue: Int
    get() = if (selectedDieIndex == 0) diceValue else diceValue2

  val sequentialMoveStepNumber: Int
    get() = if (dice1Used xor dice2Used) 2 else 1

  val sequentialMoveStepLabel: String
    get() = when {
      turnPhase != TurnPhase.MOVE -> "DUAL DICE • ROLL BOTH DICE TO BEGIN SEQUENTIAL MOVE"
      sequentialMoveStepNumber == 1 -> "STEP 1 OF 2 • CHOOSE DIE #1 (+$diceValue) OR DIE #2 (+$diceValue2)"
      else -> "STEP 2 OF 2 • MOVE WITH REMAINING DIE #${selectedDieIndex + 1} (+$activeDieValue)"
    }

  val hudTelemetry: InGameHudTelemetry
    get() = LudoRulesEngine.computeHudTelemetry(
      tokens = tokens,
      seats = seats,
      currentTurnColor = currentTurnColor,
      turnPhase = turnPhase,
      turnNumber = turnNumber,
      diceValue = diceValue,
      diceValue2 = diceValue2,
      dice1Used = dice1Used,
      dice2Used = dice2Used,
      selectedDieIndex = selectedDieIndex,
      legalMoveTokenIndices = legalMoveTokenIndices,
      armCount = boardArmCount
    )

  val previewEmote: PawnEmoteItem
    get() = EmoteGalleryCatalog.findById(selectedEmoteIdForPreview)

  val selectedAvatar: CustomAvatarOption
    get() = ProfileCatalog.findAvatarById(selectedAvatarId)

  val activeTournamentMatch: TournamentBracketMatch?
    get() = tournamentBracketMatches.firstOrNull {
      it.roundTier == tournamentCurrentRound && it.isPlayerMatch
    } ?: tournamentBracketMatches.firstOrNull { it.roundTier == tournamentCurrentRound }

  fun equippedEmoteForMoment(moment: EmoteMomentTrigger): PawnEmoteItem {
    val id = equippedEmotesByMoment[moment]
      ?: EmoteGalleryCatalog.DEFAULT_EQUIPPED_LOADOUT[moment]
      ?: EmoteGalleryCatalog.ALL_EMOTES.first().id
    return EmoteGalleryCatalog.findById(id)
  }

  val selectedReplay: MatchReplayRecord
    get() = savedReplays.firstOrNull { it.id == selectedReplayId } ?: savedReplays.first()

  val currentReplayFrame: ReplayFrame
    get() {
      val frames = selectedReplay.frames
      val safeIdx = replayFrameIndex.coerceIn(0, (frames.size - 1).coerceAtLeast(0))
      return frames.getOrElse(safeIdx) {
        ReplayFrame(
          frameIndex = 0,
          turnNumber = 1,
          actorColor = PlayerColor.RED,
          actorName = "Player 1",
          diceValue = 1,
          diceValue2 = 6,
          usedDieValue = 0,
          tokens = List(32) { -1 },
          eventSummary = "Replay ready",
          tacticalNote = "Scrub the timeline or switch 3D camera angles to analyze moves."
        )
      }
    }

  val replayBoardArmCount: Int
    get() = LudoRulesEngine.boardArmCountForPlayers(maxOf(selectedReplay.maxPlayers, selectedReplay.seats.size))

  val replayActiveBlockades: Map<Int, Set<PlayerColor>>
    get() = LudoRulesEngine.getActiveBlockades(currentReplayFrame.tokens, selectedReplay.seats, replayBoardArmCount)

  val currentTutorialLesson: TutorialLessonStep
    get() = HowToPlayTutorialCatalog.findStepByIndex(tutorialStepIndex)

  val tutorialActiveBlockades: Map<Int, Set<PlayerColor>>
    get() = LudoRulesEngine.getActiveBlockades(tutorialTokens, HowToPlayTutorialCatalog.TUTORIAL_SEATS, 4)

  val tutorialLegalMoveTokens: List<Int>
    get() = if (
      tutorialActionState == TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN &&
      currentTutorialLesson.targetTokenIndex >= 0
    ) {
      listOf(currentTutorialLesson.targetTokenIndex)
    } else {
      emptyList()
    }

  companion object {
    const val TURN_DURATION_SECONDS = 22

    fun buildDefaultOfflineSeats(playerCount: Int): List<OfflineSeatDraft> {
      val botNames = listOf(
        "Player 1", "Atelier Bot", "Tactical AI", "Archon AI",
        "Vanguard AI", "Citadel AI", "Sovereign AI", "Sentinel AI"
      )
      return PlayerColor.entries.mapIndexed { idx, color ->
        OfflineSeatDraft(
          seatIndex = idx,
          color = color,
          name = botNames.getOrElse(idx) { "Player ${idx + 1}" },
          isEnabled = idx < playerCount,
          isBot = idx != 0
        )
      }
    }
  }
}

sealed interface DataStreamState<out T> {
  data object Loading : DataStreamState<Nothing>
  data class Success<T>(val data: T) : DataStreamState<T>
  data class Error(val message: String) : DataStreamState<Nothing>
}

class LudoGameViewModel(
  private val repository: LudoMultiplayerRepository,
  val currentUserId: String?,
  initialDisplayName: String,
  val currentUserEmail: String? = null,
  val soundEffectRepository: SoundEffectRepository = DefaultSoundEffectRepository(),
  val hapticEffectRepository: HapticEffectRepository = DefaultHapticEffectRepository()
) : ViewModel() {

  val isAuthenticated: Boolean
    get() = !currentUserId.isNullOrBlank()

  private val _uiState = MutableStateFlow(LudoUiState())
  val uiState: StateFlow<LudoUiState> = _uiState.asStateFlow()

  private val _countryFilterFlow = MutableStateFlow(CountryCatalog.detectDefaultCountry().code)

  val soundManager = SoundEffectsManager(soundEffectRepository)
  val hapticManager = HapticFeedbackManager(hapticEffectRepository)

  private val audioCallManager = AudioCallManager(
    repository = repository,
    scope = viewModelScope,
    currentUserId = currentUserId
  )
  val audioCallState: StateFlow<AudioCallUiState> = audioCallManager.callState

  private var roomListenerJob: Job? = null
  private var chatListenerJob: Job? = null
  private var turnTimerJob: Job? = null
  private var botActionJob: Job? = null
  private var replayPlaybackJob: Job? = null
  private var pawnEmoteJob: Job? = null
  private var tournamentTickerJob: Job? = null
  private var tournamentLiveTickCount: Int = 0
  private var isAnimatingMove: Boolean = false
  private var lastProcessedTurnKey: String = ""
  private var statsRecordedForRoom: String = ""
  private var recordedMatchCounter: Int = 1

  // Strictly auth-gated: only observes Firestore when currentUserId != null
  val profileState: StateFlow<DataStreamState<PlayerProfile?>> =
    if (currentUserId != null) {
      repository
        .observeProfile(currentUserId)
        .map<PlayerProfile?, DataStreamState<PlayerProfile?>> { profile ->
          if (profile != null) {
            if (profile.countryCode.isNotBlank()) {
              _countryFilterFlow.value = profile.countryCode
            }
            val syncedLoadout = profile.parsedEquippedEmoteLoadout()
            val syncedAvatarId = ProfileCatalog.decodeAvatarFromUnlockedItems(profile.unlockedItems)
              ?: repository.currentAuthAvatarId()
            val syncedAvatarOption = ProfileCatalog.findAvatarById(syncedAvatarId)
            _uiState.update { state ->
              state.copy(
                selectedCountryFilter = profile.countryCode.ifBlank { state.selectedCountryFilter },
                equippedEmotesByMoment = syncedLoadout,
                localDisplayName = profile.displayName.ifBlank { state.localDisplayName },
                selectedAvatarId = syncedAvatarOption.id,
                firebaseAuthPhotoUri = syncedAvatarOption.authPhotoUriString,
                sessionWinsCount = maxOf(
                  state.sessionWinsCount,
                  profile.missionProgressForType(MissionType.WIN_MATCHES)
                ),
                sessionSixesRolledCount = maxOf(
                  state.sessionSixesRolledCount,
                  profile.missionProgressForType(MissionType.ROLL_SIXES)
                ),
                sessionCapturesCount = maxOf(
                  state.sessionCapturesCount,
                  profile.missionProgressForType(MissionType.CAPTURE_PAWNS)
                ),
                sessionHomesCount = maxOf(
                  state.sessionHomesCount,
                  profile.missionProgressForType(MissionType.REACH_HOME)
                ),
                sessionOpensCount = maxOf(
                  state.sessionOpensCount,
                  profile.missionProgressForType(MissionType.OPEN_FROM_YARD)
                )
              )
            }
          }
          DataStreamState.Success(profile)
        }
        .catch { error ->
          Log.w(TAG, "Error observing profile", error)
          emit(DataStreamState.Error(error.message ?: "Failed to load profile"))
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = DataStreamState.Loading
        )
    } else {
      flowOf<DataStreamState<PlayerProfile?>>(DataStreamState.Success(null))
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = DataStreamState.Success(null)
        )
    }

  // Strictly auth-gated: observes live AdminAppConfig when currentUserId != null
  val adminConfigState: StateFlow<AdminAppConfig> =
    if (currentUserId != null) {
      repository
        .observeAdminConfig()
        .map { it ?: AdminAppConfig() }
        .catch { emit(AdminAppConfig()) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = AdminAppConfig()
        )
    } else {
      flowOf(AdminAppConfig())
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = AdminAppConfig()
        )
    }

  // Strictly auth-gated: only observes open public rooms in Firestore when currentUserId != null
  val openPublicRoomsState: StateFlow<DataStreamState<List<LudoRoom>>> =
    if (currentUserId != null) {
      repository
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
    } else {
      flowOf<DataStreamState<List<LudoRoom>>>(DataStreamState.Success(emptyList()))
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = DataStreamState.Success(emptyList())
        )
    }

  val globalLeaderboardState: StateFlow<DataStreamState<List<PlayerProfile>>> =
    if (currentUserId != null) {
      repository
        .observeGlobalLeaderboard(limit = 50L)
        .map<List<PlayerProfile>, DataStreamState<List<PlayerProfile>>> { DataStreamState.Success(it) }
        .catch { error ->
          Log.w(TAG, "Error observing global leaderboard", error)
          emit(DataStreamState.Error(error.message ?: "Failed to load global leaderboard"))
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = DataStreamState.Loading
        )
    } else {
      flowOf<DataStreamState<List<PlayerProfile>>>(DataStreamState.Success(emptyList()))
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = DataStreamState.Success(emptyList())
        )
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  val countryLeaderboardState: StateFlow<DataStreamState<List<PlayerProfile>>> =
    if (currentUserId != null) {
      _countryFilterFlow
        .flatMapLatest { countryCode ->
          repository
            .observeCountryLeaderboard(countryCode = countryCode, limit = 50L)
            .map<List<PlayerProfile>, DataStreamState<List<PlayerProfile>>> { DataStreamState.Success(it) }
            .catch { error ->
              Log.w(TAG, "Error observing country leaderboard", error)
              emit(DataStreamState.Error(error.message ?: "Failed to load country leaderboard"))
            }
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = DataStreamState.Loading
        )
    } else {
      flowOf<DataStreamState<List<PlayerProfile>>>(DataStreamState.Success(emptyList()))
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = DataStreamState.Success(emptyList())
        )
    }

  val friendRequestsState: StateFlow<DataStreamState<List<FriendRequest>>> =
    if (currentUserId != null) {
      repository
        .observeFriendRequests(currentUserId)
        .map<List<FriendRequest>, DataStreamState<List<FriendRequest>>> { DataStreamState.Success(it) }
        .catch { error ->
          Log.w(TAG, "Error observing friend requests", error)
          emit(DataStreamState.Error(error.message ?: "Failed to load friend requests"))
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = DataStreamState.Loading
        )
    } else {
      flowOf<DataStreamState<List<FriendRequest>>>(DataStreamState.Success(emptyList()))
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = DataStreamState.Success(emptyList())
        )
    }

  val waitingRoomsForFriendsState: StateFlow<List<LudoRoom>> =
    if (currentUserId != null) {
      repository
        .observeWaitingRoomsForFriends()
        .catch { emit(emptyList()) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = emptyList()
        )
    } else {
      flowOf(emptyList<LudoRoom>())
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = emptyList()
        )
    }

  val spectatablePublicPlayingRoomsState: StateFlow<List<LudoRoom>> =
    if (currentUserId != null) {
      repository
        .observeSpectatablePublicPlayingRooms()
        .catch { emit(emptyList()) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = emptyList()
        )
    } else {
      flowOf(emptyList<LudoRoom>())
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000L),
          initialValue = emptyList()
        )
    }

  init {
    val initialAuthAvatarId = repository.currentAuthAvatarId()
    val initialAvatarOption = ProfileCatalog.findAvatarById(initialAuthAvatarId)
    val initialNameClean = initialDisplayName.trim().ifBlank {
      repository.currentAuthDisplayName()?.trim()?.takeIf { it.isNotBlank() } ?: "Royal Player"
    }
    _uiState.update {
      it.copy(
        localDisplayName = initialNameClean,
        selectedAvatarId = initialAvatarOption.id,
        firebaseAuthPhotoUri = initialAvatarOption.authPhotoUriString
      )
    }
    if (currentUserId != null) {
      viewModelScope.launch {
        repository.ensureOrUpdateProfile(
          displayName = initialNameClean,
          avatarId = initialAvatarOption.id
        )
      }
    }
    startWeeklyTournamentTicker()
  }

  private fun startWeeklyTournamentTicker() {
    tournamentTickerJob?.cancel()
    tournamentTickerJob = viewModelScope.launch {
      while (true) {
        delay(1000L)
        tournamentLiveTickCount += 1
        _uiState.update { state ->
          val nextRemaining = if (state.tournamentRemainingSeconds > 0L) {
            state.tournamentRemainingSeconds - 1L
          } else {
            WeeklyTournamentCatalog.DEFAULT_REMAINING_SECONDS
          }
          if (tournamentLiveTickCount % 4 == 0) {
            val (updatedMatches, nextFeed) = WeeklyTournamentCatalog.tickLiveContenderMatches(
              matches = state.tournamentBracketMatches,
              tickCount = tournamentLiveTickCount / 4
            )
            state.copy(
              tournamentRemainingSeconds = nextRemaining,
              tournamentBracketMatches = updatedMatches,
              tournamentLiveFeedMessage = nextFeed
            )
          } else {
            state.copy(tournamentRemainingSeconds = nextRemaining)
          }
        }
      }
    }
  }

  private fun currentDisplayName(): String {
    val profile = (profileState.value as? DataStreamState.Success)?.data
    return profile?.displayName?.ifBlank { _uiState.value.localDisplayName }
      ?: _uiState.value.localDisplayName.ifBlank { "Royal Player" }
  }

  fun updateDisplayName(newName: String) {
    val trimmed = newName.trim().take(40)
    if (trimmed.isBlank()) return
    _uiState.update {
      it.copy(
        localDisplayName = trimmed,
        toastMessage = "Display name updated to \"$trimmed\" • Synced with Firebase Auth"
      )
    }
    if (!isAuthenticated) return
    viewModelScope.launch {
      repository.ensureOrUpdateProfile(
        displayName = trimmed,
        avatarId = _uiState.value.selectedAvatarId
      )
    }
  }

  fun selectCustomAvatar(avatarId: String) {
    val option = ProfileCatalog.findAvatarById(avatarId)
    _uiState.update { state ->
      state.copy(
        selectedAvatarId = option.id,
        firebaseAuthPhotoUri = option.authPhotoUriString,
        localUnlockedItems = ProfileCatalog.encodeAvatarIntoUnlockedItems(state.localUnlockedItems, option.id),
        toastMessage = "Equipped ${option.title} (${option.badgeSymbol}) • Saved to Firebase Auth"
      )
    }
    if (isAuthenticated) {
      viewModelScope.launch {
        repository.updateUserAvatar(option.id)
      }
    }
  }

  fun updateUserCountry(countryCode: String) {
    val clean = countryCode.trim().uppercase()
    _countryFilterFlow.value = clean
    _uiState.update { it.copy(selectedCountryFilter = clean) }
    if (isAuthenticated) {
      viewModelScope.launch {
        repository.updateUserCountry(clean)
      }
    }
  }

  fun selectCountryLeaderboardFilter(countryCode: String) {
    val clean = countryCode.trim().uppercase()
    _countryFilterFlow.value = clean
    _uiState.update { it.copy(selectedCountryFilter = clean) }
  }

  fun openLeaderboardScreen() {
    _uiState.update { it.copy(currentScreen = AppScreen.LEADERBOARD) }
  }

  fun openStoreScreen() {
    _uiState.update { it.copy(currentScreen = AppScreen.STORE) }
  }

  fun openEmoteGalleryScreen() {
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.EMOTE_GALLERY,
        errorMessage = null,
        toastMessage = null
      )
    }
  }

  fun openWeeklyTournamentScreen() {
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.WEEKLY_TOURNAMENT,
        errorMessage = null,
        toastMessage = null
      )
    }
  }

  fun openProfileScreen() {
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.PROFILE,
        errorMessage = null,
        toastMessage = null
      )
    }
  }

  fun openFriendsScreen(initialTab: Int = 0) {
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.FRIENDS,
        selectedFriendsTab = initialTab.coerceIn(0, 2),
        errorMessage = null,
        toastMessage = null
      )
    }
    if (isAuthenticated) {
      viewModelScope.launch {
        repository.touchPresenceHeartbeat()
      }
      executePlayerSearch(_uiState.value.friendsSearchQuery)
    }
  }

  fun selectFriendsTab(tabIndex: Int) {
    _uiState.update { it.copy(selectedFriendsTab = tabIndex.coerceIn(0, 2)) }
  }

  fun updateFriendsSearchQuery(query: String) {
    val clean = query.take(40)
    _uiState.update { it.copy(friendsSearchQuery = clean) }
    executePlayerSearch(clean)
  }

  fun executePlayerSearch(queryOverride: String? = null) {
    val query = (queryOverride ?: _uiState.value.friendsSearchQuery).trim()
    val cleanLower = query.lowercase()
    val localMatches = FriendsStudioCatalog.STUDIO_SEARCHABLE_DIRECTORY.filter { p ->
      cleanLower.isBlank() ||
        p.displayName.lowercase().contains(cleanLower) ||
        p.countryName.lowercase().contains(cleanLower) ||
        p.countryCode.lowercase().contains(cleanLower)
    }

    if (!isAuthenticated) {
      _uiState.update {
        it.copy(
          friendsSearchResults = localMatches,
          isSearchingPlayers = false
        )
      }
      return
    }

    _uiState.update { it.copy(isSearchingPlayers = true) }
    viewModelScope.launch {
      val result = repository.searchPlayersByUsername(query, limit = 80L)
      result.fold(
        onSuccess = { cloudPlayers ->
          val combined = (cloudPlayers + localMatches).distinctBy { it.userId.ifBlank { it.id } }
          _uiState.update {
            it.copy(
              friendsSearchResults = combined,
              isSearchingPlayers = false
            )
          }
        },
        onFailure = {
          _uiState.update {
            it.copy(
              friendsSearchResults = localMatches,
              isSearchingPlayers = false
            )
          }
        }
      )
    }
  }

  fun sendFriendRequestToPlayer(targetProfile: PlayerProfile) {
    val targetUid = targetProfile.userId.ifBlank { targetProfile.id }
    val targetName = targetProfile.displayName.ifBlank { "Royal Player" }
    val targetCountry = targetProfile.countryCode.ifBlank { "US" }

    if (targetUid.startsWith("demo_uid_") || !isAuthenticated) {
      val demoReq = FriendRequest(
        id = "demo_sent_${targetUid}",
        fromUid = currentUserId ?: "LOCAL_PLAYER",
        fromName = currentDisplayName(),
        fromCountry = _uiState.value.selectedCountryFilter,
        toUid = targetUid,
        toName = targetName,
        toCountry = targetCountry,
        status = FriendRequestStatus.PENDING.name,
        memberUids = listOf(currentUserId ?: "LOCAL_PLAYER", targetUid)
      )
      _uiState.update { state ->
        val updatedSent = (state.localSentRequests.filterNot { it.toUid == targetUid } + demoReq)
        state.copy(
          localSentRequests = updatedSent,
          toastMessage = "Friend request sent to $targetName!"
        )
      }
      return
    }

    val myProfile = (profileState.value as? DataStreamState.Success)?.data
    val senderName = myProfile?.displayName?.ifBlank { currentDisplayName() } ?: currentDisplayName()
    val senderCountry = myProfile?.countryCode?.ifBlank { _uiState.value.selectedCountryFilter }
      ?: _uiState.value.selectedCountryFilter

    _uiState.update { it.copy(isLoadingAction = true, errorMessage = null) }
    viewModelScope.launch {
      val res = repository.sendFriendRequest(
        targetUserId = targetUid,
        targetDisplayName = targetName,
        targetCountryCode = targetCountry,
        senderName = senderName,
        senderCountryCode = senderCountry
      )
      res.fold(
        onSuccess = { saved ->
          val msg = if (saved.status == FriendRequestStatus.ACCEPTED.name) {
            "You and $targetName are now friends!"
          } else {
            "Friend request sent to $targetName!"
          }
          _uiState.update {
            it.copy(
              isLoadingAction = false,
              toastMessage = msg
            )
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isLoadingAction = false,
              errorMessage = err.message ?: "Could not send friend request"
            )
          }
        }
      )
    }
  }

  fun respondToFriendRequest(request: FriendRequest, accept: Boolean) {
    val myUid = currentUserId ?: "LOCAL_PLAYER"
    val senderName = request.otherName(myUid)

    if (request.id.startsWith("demo_") || !isAuthenticated) {
      _uiState.update { state ->
        val remainingIncoming = state.localIncomingRequests.filterNot { it.id == request.id }
        val updatedFriends = if (accept) {
          val newFriend = FriendEntryUiModel(
            requestId = request.id,
            friendUid = request.fromUid,
            displayName = request.fromName,
            countryCode = request.fromCountry,
            countryName = CountryCatalog.fromCode(request.fromCountry).name,
            score = 860L,
            rankTierTitle = "Diamond Archon",
            wins = 22L,
            gamesPlayed = 31L,
            capturesTotal = 58L,
            presenceState = FriendPresenceState.IN_WAITING_ROOM,
            statusSubtitle = "Waiting in Room #KNJ208 (1/4 Players) — Tap to Join!",
            activeRoomCode = "KNJ208",
            activeRoomStatus = RoomStatus.WAITING.name,
            activeRoomPlayersCount = 1,
            activeRoomMaxPlayers = 4,
            activeRoomTurnNumber = 1,
            isDemoFriend = true
          )
          (listOf(newFriend) + state.localStudioFriends).distinctBy { it.friendUid }
        } else {
          state.localStudioFriends
        }
        state.copy(
          localIncomingRequests = remainingIncoming,
          localStudioFriends = updatedFriends,
          toastMessage = if (accept) "Accepted $senderName's friend request!" else "Declined friend request from $senderName"
        )
      }
      return
    }

    _uiState.update { it.copy(isLoadingAction = true, errorMessage = null) }
    viewModelScope.launch {
      val res = repository.respondToFriendRequest(request.id, accept = accept)
      res.fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isLoadingAction = false,
              toastMessage = if (accept) "Accepted $senderName's friend request!" else "Declined friend request from $senderName"
            )
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isLoadingAction = false,
              errorMessage = err.message ?: "Failed to update friend request"
            )
          }
        }
      )
    }
  }

  fun removeFriendOrCancelRequest(requestId: String, labelName: String) {
    if (requestId.startsWith("demo_") || !isAuthenticated) {
      _uiState.update { state ->
        state.copy(
          localStudioFriends = state.localStudioFriends.filterNot { it.requestId == requestId },
          localSentRequests = state.localSentRequests.filterNot { it.id == requestId },
          localIncomingRequests = state.localIncomingRequests.filterNot { it.id == requestId },
          toastMessage = "Removed $labelName"
        )
      }
      return
    }

    _uiState.update { it.copy(isLoadingAction = true, errorMessage = null) }
    viewModelScope.launch {
      val res = repository.removeFriendOrRequest(requestId)
      res.fold(
        onSuccess = {
          _uiState.update {
            it.copy(
              isLoadingAction = false,
              toastMessage = "Removed $labelName"
            )
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isLoadingAction = false,
              errorMessage = err.message ?: "Could not remove friend"
            )
          }
        }
      )
    }
  }

  fun joinFriendActiveRoom(friend: FriendEntryUiModel) {
    val code = friend.activeRoomCode?.trim()?.uppercase().orEmpty()
    if (code.isBlank()) {
      _uiState.update { it.copy(toastMessage = "${friend.displayName} is not currently in an open room.") }
      return
    }

    if (!friend.isDemoFriend && isAuthenticated) {
      joinOnlineRoomByCode(code)
      return
    }

    // Studio showcase friend room: launch a live 3D game table seated directly with this friend!
    turnTimerJob?.cancel()
    botActionJob?.cancel()
    roomListenerJob?.cancel()
    endAudioCall()
    isAnimatingMove = false

    val targetPlayers = friend.activeRoomMaxPlayers.coerceIn(2, 8)
    val myColor = _uiState.value.preferredOnlineColor
    val colorPool = (PlayerColor.defaultColorsForCount(targetPlayers) + PlayerColor.entries).distinct()
    val friendColor = colorPool.firstOrNull { it != myColor } ?: PlayerColor.GREEN
    val remainingColors = colorPool.filter { it != myColor && it != friendColor }

    val seats = mutableListOf(
      PlayerSeat(color = myColor, uid = currentUserId ?: "HUMAN_0", name = currentDisplayName(), isBot = false),
      PlayerSeat(color = friendColor, uid = friend.friendUid, name = friend.displayName, isBot = true)
    )
    val botNames = listOf("Atelier AI", "Tactical AI", "Archon AI", "Vanguard AI", "Citadel AI", "Sovereign AI")
    remainingColors.take((targetPlayers - 2).coerceAtLeast(0)).forEachIndexed { idx, c ->
      seats.add(PlayerSeat(color = c, uid = "BOT_${idx + 2}", name = botNames.getOrElse(idx) { "Bot ${idx + 2}" }, isBot = true))
    }

    val initialTokens = List(32) { -1 }
    val turnSeconds = adminConfigState.value.turnDurationSeconds.toInt().coerceIn(8, 120)
    val matchId = "FRIEND-ROOM-$code-${++recordedMatchCounter}"
    val kickoffFrame = ReplayFrame(
      frameIndex = 0,
      turnNumber = 1,
      actorColor = myColor,
      actorName = currentDisplayName(),
      diceValue = 1,
      diceValue2 = 6,
      usedDieValue = 0,
      tokens = initialTokens,
      eventSummary = "Joined ${friend.displayName}'s Room #$code (${seats.size}P Arena)",
      tacticalNote = "Friend Room Match: Roll a 6 on either die to deploy your opening runner."
    )

    soundManager.play(LudoSoundEvent.OPEN)
    _uiState.update { state ->
      state.copy(
        currentScreen = AppScreen.GAME_ARENA,
        isOnlineMatch = false,
        isSpectating = false,
        spectatingTargetName = "",
        isTournamentMatchActive = false,
        roomCode = code,
        isHost = false,
        roomStatus = RoomStatus.PLAYING,
        maxPlayers = seats.size,
        seats = seats,
        tokens = initialTokens,
        currentTurnColor = myColor,
        turnPhase = TurnPhase.ROLL,
        diceValue = 1,
        diceValue2 = 6,
        dice1Used = false,
        dice2Used = false,
        selectedDieIndex = 0,
        isDiceRolling = false,
        consecutiveSixes = 0,
        turnBonusPending = false,
        turnNumber = 1,
        legalMoveTokenIndices = emptyList(),
        animatedToken = null,
        activeParticleBurst = null,
        turnSecondsRemaining = turnSeconds,
        statusBanner = "Joined ${friend.displayName}'s Room #$code — Your turn to roll 2 dice!",
        winnerColor = "NONE",
        winnerOrder = emptyList(),
        matchCapturesByMe = 0,
        matchHomesByMe = 0,
        activeMatchId = matchId,
        activeMatchRecordedFrames = listOf(kickoffFrame),
        errorMessage = null,
        toastMessage = "Joined ${friend.displayName} in Room #$code!"
      )
    }
    onNewTurnReady()
  }

  fun spectateFriendActiveRoom(friend: FriendEntryUiModel) {
    val code = friend.activeRoomCode?.trim()?.uppercase().orEmpty()
    if (code.isBlank()) {
      _uiState.update {
        it.copy(toastMessage = "${friend.displayName} is not currently in an active game room to spectate.")
      }
      return
    }

    if (!friend.isDemoFriend && isAuthenticated) {
      spectateOnlineRoomByCode(code, friendNameHint = friend.displayName)
      return
    }

    // Launch live view-only 3D spectator arena for friend's active match (all seats autonomous, zero interference)
    turnTimerJob?.cancel()
    botActionJob?.cancel()
    roomListenerJob?.cancel()
    endAudioCall()
    isAnimatingMove = false

    val targetPlayers = friend.activeRoomMaxPlayers.coerceIn(2, 8)
    val colorPool = PlayerColor.defaultColorsForCount(targetPlayers)
    val friendColor = colorPool.firstOrNull() ?: PlayerColor.RED
    val rivalColors = colorPool.drop(1)
    val rivalNames = listOf(
      "Archon AI", "Vanguard AI", "Citadel AI", "Sovereign AI",
      "Atelier AI", "Tactical AI", "Sentinel AI"
    )

    val seats = mutableListOf(
      PlayerSeat(
        color = friendColor,
        uid = friend.friendUid,
        name = friend.displayName,
        isBot = true
      )
    )
    rivalColors.take((targetPlayers - 1).coerceAtLeast(1)).forEachIndexed { idx, c ->
      seats.add(
        PlayerSeat(
          color = c,
          uid = "SPECTATED_PLAYER_${idx + 1}",
          name = rivalNames.getOrElse(idx) { "Challenger ${idx + 2}" },
          isBot = true
        )
      )
    }

    val armCount = LudoRulesEngine.boardArmCountForPlayers(seats.size)
    val initialZoom = when (armCount) {
      8 -> 0.90f
      6 -> 0.95f
      else -> 1.04f
    }

    // Seed mid-game token positions so the spectator enters an active match already in progress
    val midGameTokens = MutableList(32) { -1 }
    seats.forEachIndexed { seatIdx, seat ->
      val indices = LudoRulesEngine.tokenIndicesForColor(seat.color).toList()
      if (indices.size >= 4) {
        midGameTokens[indices[0]] = (8 + seatIdx * 5) % 36
        midGameTokens[indices[1]] = (19 + seatIdx * 3) % 36
        if (seatIdx % 2 == 0) {
          midGameTokens[indices[2]] = 0 // Safe starting star
        }
      }
    }

    val startTurnNumber = friend.activeRoomTurnNumber.coerceAtLeast(6)
    val turnSeconds = adminConfigState.value.turnDurationSeconds.toInt().coerceIn(8, 120)
    val matchId = "SPECTATE-$code-${++recordedMatchCounter}"
    val kickoffFrame = ReplayFrame(
      frameIndex = 0,
      turnNumber = startTurnNumber,
      actorColor = friendColor,
      actorName = friend.displayName,
      diceValue = 4,
      diceValue2 = 6,
      usedDieValue = 0,
      tokens = midGameTokens,
      eventSummary = "Spectating ${friend.displayName} in Room #$code (${seats.size}P Live Match)",
      tacticalNote = "View-Only Spectator Mode: Watch live 3D turns and send Emoji/Emote reactions."
    )

    val roomKindLabel = if (friend.activeRoomIsPublic) "Room" else "Private Room"
    soundManager.play(LudoSoundEvent.SAFE)
    _uiState.update { state ->
      state.copy(
        currentScreen = AppScreen.GAME_ARENA,
        isOnlineMatch = false,
        isSpectating = true,
        isPublicRoom = friend.activeRoomIsPublic,
        spectatingTargetName = friend.displayName,
        spectatorCount = (friend.activeRoomPlayersCount.coerceAtLeast(2) + 1),
        isTournamentMatchActive = false,
        roomCode = code,
        isHost = false,
        roomStatus = RoomStatus.PLAYING,
        maxPlayers = seats.size,
        seats = seats,
        tokens = midGameTokens,
        currentTurnColor = friendColor,
        turnPhase = TurnPhase.ROLL,
        diceValue = 4,
        diceValue2 = 6,
        dice1Used = false,
        dice2Used = false,
        selectedDieIndex = 0,
        isDiceRolling = false,
        consecutiveSixes = 0,
        turnBonusPending = false,
        turnNumber = startTurnNumber,
        legalMoveTokenIndices = emptyList(),
        animatedToken = null,
        activeParticleBurst = null,
        turnSecondsRemaining = turnSeconds,
        autoFocusTurnCamera = true,
        cameraYaw = yawForColor(friendColor, seats, armCount),
        cameraZoom = initialZoom,
        statusBanner = "👁️ Observing $roomKindLabel #$code (${friend.displayName}) • View-Only Board Lock",
        winnerColor = "NONE",
        winnerOrder = emptyList(),
        matchCapturesByMe = 0,
        matchHomesByMe = 0,
        activeMatchId = matchId,
        activeMatchRecordedFrames = listOf(kickoffFrame),
        roomChatMessages = PrivateRoomChatCatalog.buildInitialShowcaseMessages(code),
        unreadChatCount = 0,
        errorMessage = null,
        toastMessage = "Joined $roomKindLabel #$code as View-Only Observer!"
      )
    }
    onNewTurnReady()
  }

  fun spectateOnlineRoomByCode(codeInput: String, friendNameHint: String = "") {
    val cleanCode = codeInput.trim().uppercase()
    if (cleanCode.length < 4) {
      _uiState.update { it.copy(errorMessage = "Please enter a valid room code to spectate.") }
      return
    }

    // Check if this code matches a local Studio Showcase friend room or ongoing private room preset
    val matchingDemoFriend = _uiState.value.localStudioFriends.firstOrNull {
      it.activeRoomCode?.equals(cleanCode, ignoreCase = true) == true && it.isDemoFriend
    }
    val matchingPrivatePreset = FriendsStudioCatalog.ONGOING_PRIVATE_ROOMS_SHOWCASE.firstOrNull {
      it.roomCode.equals(cleanCode, ignoreCase = true)
    }
    if (matchingDemoFriend != null || matchingPrivatePreset != null || !isAuthenticated) {
      val fallbackFriend = matchingDemoFriend ?: FriendEntryUiModel(
        requestId = "spectate_$cleanCode",
        friendUid = "demo_spectate_$cleanCode",
        displayName = matchingPrivatePreset?.hostName
          ?: friendNameHint.ifBlank { "Private Room #$cleanCode Host" },
        countryCode = _uiState.value.selectedCountryFilter,
        countryName = CountryCatalog.fromCode(_uiState.value.selectedCountryFilter).name,
        score = 1200L,
        rankTierTitle = "Grandmaster",
        wins = 28L,
        gamesPlayed = 40L,
        capturesTotal = 74L,
        presenceState = FriendPresenceState.IN_ACTIVE_MATCH,
        statusSubtitle = matchingPrivatePreset?.summary ?: "Playing in Private Room #$cleanCode",
        activeRoomCode = cleanCode,
        activeRoomStatus = RoomStatus.PLAYING.name,
        activeRoomIsPublic = false,
        activeRoomPlayersCount = matchingPrivatePreset?.maxPlayers ?: 4,
        activeRoomMaxPlayers = matchingPrivatePreset?.maxPlayers ?: 4,
        activeRoomTurnNumber = matchingPrivatePreset?.turnNumber ?: 8,
        isDemoFriend = true
      )
      spectateFriendActiveRoom(fallbackFriend)
      return
    }

    _uiState.update { it.copy(isLoadingAction = true, errorMessage = null) }
    viewModelScope.launch {
      val result = repository.spectateRoomByCode(cleanCode)
      result.onSuccess { room ->
        turnTimerJob?.cancel()
        botActionJob?.cancel()
        endAudioCall()
        isAnimatingMove = false

        val hostOrFriendName = friendNameHint.ifBlank {
          room.playerNames.firstOrNull() ?: "Room #${room.roomCode}"
        }
        val roomKind = if (room.isPublic) "Public Room" else "Private Room"
        soundManager.play(LudoSoundEvent.SAFE)
        _uiState.update {
          it.copy(
            isLoadingAction = false,
            currentScreen = AppScreen.GAME_ARENA,
            isOnlineMatch = true,
            isSpectating = true,
            spectatingTargetName = hostOrFriendName,
            spectatorCount = (room.memberUids.size.coerceAtLeast(1) + 1),
            isTournamentMatchActive = false,
            roomCode = room.roomCode,
            isHost = false,
            isPublicRoom = room.isPublic,
            maxPlayers = room.maxPlayers.toInt(),
            legalMoveTokenIndices = emptyList(),
            toastMessage = "Observing $roomKind #${room.roomCode} ($hostOrFriendName) • Live Firestore Sync"
          )
        }
        applyRemoteRoomSnapshot(room)
        subscribeToOnlineRoom(room.roomCode)
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isLoadingAction = false,
            errorMessage = err.localizedMessage ?: "Could not spectate room $cleanCode"
          )
        }
      }
    }
  }

  fun buildResolvedFriendsList(
    cloudRequests: List<FriendRequest>,
    leaderboardProfiles: List<PlayerProfile>,
    waitingRooms: List<LudoRoom>,
    openPublicRooms: List<LudoRoom>,
    spectatablePlayingRooms: List<LudoRoom> = emptyList()
  ): List<FriendEntryUiModel> {
    val myUid = currentUserId ?: "LOCAL_PLAYER"
    val allActiveRooms = (waitingRooms + openPublicRooms + spectatablePlayingRooms).distinctBy { it.roomCode }
    val profileByUid = (leaderboardProfiles + _uiState.value.friendsSearchResults)
      .associateBy { it.userId.ifBlank { it.id } }

    val acceptedCloudFriends = cloudRequests
      .filter { it.status == FriendRequestStatus.ACCEPTED.name }
      .map { req ->
        val friendUid = req.otherUid(myUid)
        FriendsStudioCatalog.buildFriendEntry(
          request = req,
          myUid = myUid,
          profile = profileByUid[friendUid],
          activeRooms = allActiveRooms
        )
      }

    return (acceptedCloudFriends + _uiState.value.localStudioFriends)
      .distinctBy { it.friendUid }
      .sortedWith(
        compareBy<FriendEntryUiModel> { it.presenceState.sortPriority }
          .thenByDescending { it.score }
      )
  }

  fun computeGlobalRank(profile: PlayerProfile?, globalLeaders: List<PlayerProfile>): Int {
    if (profile != null && profile.userId.isNotBlank()) {
      val exactIdx = globalLeaders.indexOfFirst { it.userId == profile.userId }
      if (exactIdx >= 0) return exactIdx + 1
    }
    val myScore = profile?.score ?: 100L
    val higherCount = globalLeaders.count { it.score > myScore }
    return (higherCount + 1).coerceAtLeast(1)
  }

  fun computeCountryRank(profile: PlayerProfile?, countryLeaders: List<PlayerProfile>): Int {
    if (profile != null && profile.userId.isNotBlank()) {
      val exactIdx = countryLeaders.indexOfFirst { it.userId == profile.userId }
      if (exactIdx >= 0) return exactIdx + 1
    }
    val myScore = profile?.score ?: 100L
    val higherCount = countryLeaders.count { it.score > myScore }
    return (higherCount + 1).coerceAtLeast(1)
  }

  fun effectiveMatchHistory(profile: PlayerProfile?): List<ProfileMatchHistoryEntry> {
    val firestoreHistory = profile?.parsedMatchHistory().orEmpty()
    val localNonSample = _uiState.value.localMatchHistory.filterNot { it.id.startsWith("sample_hist_") }
    val combined = (localNonSample + firestoreHistory).distinctBy {
      "${it.outcome.code}:${it.modeLabel}:${it.captures}:${it.homesReached}:${it.scoreDelta}"
    }
    return if (combined.isNotEmpty()) {
      combined.take(12)
    } else {
      _uiState.value.localMatchHistory.take(8)
    }
  }

  fun openAdminPanelScreen() {
    _uiState.update { it.copy(currentScreen = AppScreen.ADMIN_PANEL) }
  }

  fun openReplayStudioScreen(replayId: String? = null) {
    replayPlaybackJob?.cancel()
    _uiState.update { state ->
      val targetId = replayId ?: state.selectedReplayId
      val preset = state.selectedReplayCameraPreset
      state.copy(
        currentScreen = AppScreen.REPLAY_STUDIO,
        selectedReplayId = targetId,
        replayFrameIndex = 0,
        isReplayPlaying = false,
        cameraYaw = preset.yaw,
        cameraPitch = preset.pitch,
        cameraZoom = preset.zoom,
        errorMessage = null
      )
    }
  }

  fun openReplayStudioWithCurrentMatch() {
    saveActiveMatchRecordingToLibrary()
    val latestMatchId = _uiState.value.activeMatchId
    val targetId = if (_uiState.value.savedReplays.any { it.id == latestMatchId }) {
      latestMatchId
    } else {
      _uiState.value.savedReplays.first().id
    }
    openReplayStudioScreen(targetId)
  }

  fun toggleMatchRecordingEnabled() {
    _uiState.update { state ->
      val next = !state.isMatchRecordingEnabled
      state.copy(
        isMatchRecordingEnabled = next,
        statusBanner = if (next) "Match Recording ON — capturing moves for 3D Replay" else "Match Recording paused"
      )
    }
  }

  fun selectReplayRecord(replayId: String) {
    replayPlaybackJob?.cancel()
    _uiState.update { state ->
      val record = state.savedReplays.firstOrNull { it.id == replayId } ?: return@update state
      val firstFrame = record.frames.firstOrNull()
      val armCount = LudoRulesEngine.boardArmCountForPlayers(maxOf(record.maxPlayers, record.seats.size))
      val preset = state.selectedReplayCameraPreset
      val targetYaw = if (preset.autoFollowActiveColor && firstFrame != null) {
        yawForColor(firstFrame.actorColor, record.seats, armCount)
      } else {
        preset.yaw
      }
      state.copy(
        selectedReplayId = replayId,
        replayFrameIndex = 0,
        isReplayPlaying = false,
        cameraYaw = targetYaw,
        cameraPitch = preset.pitch,
        cameraZoom = preset.zoom
      )
    }
  }

  fun seekReplayFrame(frameIndex: Int) {
    _uiState.update { state ->
      val frames = state.selectedReplay.frames
      if (frames.isEmpty()) return@update state
      val clamped = frameIndex.coerceIn(0, frames.lastIndex)
      val targetFrame = frames[clamped]
      val preset = state.selectedReplayCameraPreset
      val nextYaw = if (preset.autoFollowActiveColor) {
        yawForColor(targetFrame.actorColor, state.selectedReplay.seats, state.replayBoardArmCount)
      } else {
        state.cameraYaw
      }
      state.copy(
        replayFrameIndex = clamped,
        cameraYaw = nextYaw
      )
    }
  }

  fun stepReplayForward() {
    replayPlaybackJob?.cancel()
    _uiState.update { it.copy(isReplayPlaying = false) }
    seekReplayFrame(_uiState.value.replayFrameIndex + 1)
  }

  fun stepReplayBackward() {
    replayPlaybackJob?.cancel()
    _uiState.update { it.copy(isReplayPlaying = false) }
    seekReplayFrame(_uiState.value.replayFrameIndex - 1)
  }

  fun toggleReplayPlayback() {
    val currentlyPlaying = _uiState.value.isReplayPlaying
    if (currentlyPlaying) {
      replayPlaybackJob?.cancel()
      _uiState.update { it.copy(isReplayPlaying = false) }
      return
    }
    val frames = _uiState.value.selectedReplay.frames
    if (frames.isEmpty()) return
    if (_uiState.value.replayFrameIndex >= frames.lastIndex) {
      seekReplayFrame(0)
    }
    _uiState.update { it.copy(isReplayPlaying = true) }
    replayPlaybackJob?.cancel()
    replayPlaybackJob = viewModelScope.launch {
      while (_uiState.value.isReplayPlaying && _uiState.value.currentScreen == AppScreen.REPLAY_STUDIO) {
        val delayMs = (1300L / _uiState.value.replaySpeedMultiplier).toLong().coerceIn(240L, 2800L)
        delay(delayMs)
        val s = _uiState.value
        val maxIdx = s.selectedReplay.frames.lastIndex
        if (s.replayFrameIndex < maxIdx) {
          seekReplayFrame(s.replayFrameIndex + 1)
          soundManager.play(LudoSoundEvent.MOVE)
        } else {
          _uiState.update { it.copy(isReplayPlaying = false) }
          break
        }
      }
    }
  }

  fun cycleReplaySpeed() {
    _uiState.update { state ->
      val nextSpeed = when (state.replaySpeedMultiplier) {
        0.5f -> 1f
        1f -> 2f
        2f -> 4f
        else -> 0.5f
      }
      state.copy(replaySpeedMultiplier = nextSpeed)
    }
  }

  fun selectReplayCameraPreset(preset: ReplayCameraPreset) {
    _uiState.update { state ->
      val frame = state.currentReplayFrame
      val targetYaw = if (preset.autoFollowActiveColor) {
        yawForColor(frame.actorColor, state.selectedReplay.seats, state.replayBoardArmCount)
      } else {
        preset.yaw
      }
      state.copy(
        selectedReplayCameraPreset = preset,
        cameraYaw = targetYaw,
        cameraPitch = preset.pitch,
        cameraZoom = preset.zoom
      )
    }
  }

  private fun saveActiveMatchRecordingToLibrary() {
    val state = _uiState.value
    val frames = state.activeMatchRecordedFrames
    if (frames.isEmpty()) return
    val totalCaptures = frames.sumOf { it.capturedCount }
    val totalBlockades = frames.count { it.formedBlockade }
    val matchLabel = if (state.isOnlineMatch) {
      "Online Room #${state.roomCode} (${state.seats.size}P)"
    } else {
      "Recorded ${state.seats.size}P 3D Match #$recordedMatchCounter"
    }
    val winnerLabel = if (state.winnerColor != "NONE") {
      val winSeat = state.seats.firstOrNull { it.color.name == state.winnerColor }
      "Winner: ${winSeat?.name ?: state.winnerColor}"
    } else {
      "In-Progress Study (${frames.size} frames)"
    }
    val record = MatchReplayRecord(
      id = state.activeMatchId,
      title = matchLabel,
      subtitle = "$winnerLabel • ${frames.size} Moves • $totalCaptures Captures • $totalBlockades Blockades",
      isOnlineMatch = state.isOnlineMatch,
      maxPlayers = state.maxPlayers,
      seats = state.seats,
      frames = frames,
      winnerColor = state.winnerColor,
      recordedAtLabel = "Recorded Match",
      totalCaptures = totalCaptures,
      totalBlockades = totalBlockades
    )
    _uiState.update { current ->
      val filtered = current.savedReplays.filterNot { it.id == record.id }
      current.copy(
        savedReplays = listOf(record) + filtered,
        selectedReplayId = record.id
      )
    }
  }

  private fun buildTacticalNote(
    seatName: String,
    usedDieValue: Int,
    openedFromYard: Boolean,
    capturedCount: Int,
    formedBlockade: Boolean,
    landedOnSafe: Boolean,
    reachedHome: Boolean,
    toStep: Int,
    armCount: Int
  ): String {
    val homeStretchStart = LudoRulesEngine.maxMainTrackRel(armCount) + 1
    return when {
      reachedHome -> "Home Goal (+15 pts): $seatName guided a pawn into the central crown with exact roll (+$usedDieValue)."
      capturedCount > 0 && formedBlockade -> "Pincher Ambush: $seatName captured $capturedCount opponent pawn(s) AND stacked a blockade shield!"
      capturedCount > 0 -> "Tactical Capture (+${capturedCount * 25} pts): $seatName eliminated $capturedCount exposed opponent pawn(s) with +$usedDieValue."
      formedBlockade -> "Blockade Shield: $seatName stacked two friendly pawns at step $toStep to block trailing rivals."
      openedFromYard -> "Yard Deployment (+6): $seatName deployed a new runner onto the starting safe star."
      landedOnSafe -> "Safe Star Anchor: $seatName secured an uncapturable star tile using +$usedDieValue."
      toStep >= homeStretchStart -> "Home Stretch Entry: $seatName entered the protected inner corridor (step $toStep)."
      else -> "Track Advance (+$usedDieValue): $seatName repositioned to step $toStep."
    }
  }

  fun toggleRulesModal(show: Boolean) {
    _uiState.update { it.copy(showRulesModal = show) }
  }

  fun clearErrorMessage() {
    _uiState.update { it.copy(errorMessage = null, toastMessage = null) }
  }

  fun clearParticleBurst() {
    _uiState.update { it.copy(activeParticleBurst = null) }
  }

  // --- SOUND EFFECTS & HAPTIC FEEDBACK CONTROLS ---

  private fun emitHapticEvent(event: LudoHapticEvent): HapticPatternSpec? {
    val spec = hapticManager.trigger(event)
    if (spec != null) {
      _uiState.update {
        it.copy(
          lastTriggeredHapticTitle = spec.title,
          activeHapticSpec = spec,
          hapticPulseSequence = it.hapticPulseSequence + 1L
        )
      }
    }
    return spec
  }

  fun toggleHapticsEnabled() {
    val enabled = hapticManager.toggleHaptics()
    val latestSpec = hapticManager.lastTriggeredPattern.value
    _uiState.update {
      it.copy(
        isHapticsEnabled = enabled,
        lastTriggeredHapticTitle = latestSpec?.title ?: it.lastTriggeredHapticTitle,
        activeHapticSpec = if (enabled) latestSpec else null,
        hapticPulseSequence = if (enabled) it.hapticPulseSequence + 1L else it.hapticPulseSequence,
        statusBanner = if (enabled) "Tactile Haptic Feedback turned ON" else "Tactile Haptic Feedback turned OFF"
      )
    }
  }

  fun toggleSoundEnabled() {
    val enabled = soundManager.toggleSound()
    _uiState.update {
      it.copy(
        isSoundEnabled = enabled,
        lastPlayedSoundClipTitle = soundManager.lastPlayedClip.value?.title ?: it.lastPlayedSoundClipTitle,
        statusBanner = if (enabled) "Game Sound Effects turned ON" else "Game Sound Effects turned OFF"
      )
    }
  }

  fun setSoundEffectsVolume(volume: Float) {
    soundManager.setVolume(volume)
    _uiState.update {
      it.copy(soundVolume = soundManager.volume.value)
    }
  }

  fun playVictoryCelebrationFanfare() {
    val clip = soundManager.playGameCelebration()
    _uiState.update {
      it.copy(
        lastPlayedSoundClipTitle = clip?.title ?: it.lastPlayedSoundClipTitle,
        statusBanner = "🎺 Playing Royal Victory Celebration Fanfare!"
      )
    }
  }

  // --- DAILY MISSIONS, CURRENCY & XP, IN-APP PURCHASES & ADMIN CONFIG ---

  fun missionProgressCount(mission: DailyMission, profile: PlayerProfile?): Int {
    val state = _uiState.value
    val cloudCount = profile?.missionProgressForType(mission.type) ?: 0
    return when (mission.type) {
      MissionType.WIN_MATCHES -> maxOf(state.sessionWinsCount, cloudCount)
      MissionType.ROLL_SIXES -> maxOf(state.sessionSixesRolledCount, cloudCount)
      MissionType.CAPTURE_PAWNS -> maxOf(state.sessionCapturesCount, cloudCount)
      MissionType.REACH_HOME -> maxOf(state.sessionHomesCount, cloudCount)
      MissionType.OPEN_FROM_YARD -> maxOf(state.sessionOpensCount, cloudCount)
    }
  }

  fun isMissionClaimed(missionId: String, profile: PlayerProfile?): Boolean {
    return missionId in _uiState.value.localClaimedMissionIds ||
      (profile?.claimedMissionIds?.contains(missionId) == true)
  }

  fun claimDailyMission(mission: DailyMission, profile: PlayerProfile?) {
    val progress = missionProgressCount(mission, profile)
    if (progress < mission.targetCount) {
      _uiState.update {
        it.copy(errorMessage = "Complete ${mission.title} ($progress/${mission.targetCount}) before claiming.")
      }
      return
    }
    if (isMissionClaimed(mission.id, profile)) return

    val multiplier = adminConfigState.value.missionRewardMultiplier.coerceIn(1L, 10L)
    val totalCoins = mission.rewardCoins * multiplier
    val totalXp = mission.rewardXp * multiplier

    soundManager.play(LudoSoundEvent.HOME)
    _uiState.update { state ->
      state.copy(
        localGuestCoins = state.localGuestCoins + totalCoins,
        localGuestXp = state.localGuestXp + totalXp,
        localClaimedMissionIds = (state.localClaimedMissionIds + mission.id).distinct(),
        toastMessage = "Claimed ${mission.title}: +$totalCoins Coins & +$totalXp XP!"
      )
    }

    if (isAuthenticated) {
      viewModelScope.launch {
        repository.claimDailyMissionReward(
          missionId = mission.id,
          coinReward = totalCoins,
          xpReward = totalXp
        )
      }
    }
  }

  fun claimAllReadyDailyMissions(profile: PlayerProfile?) {
    val readyMissions = DailyMissionCatalog.MISSIONS.filter { mission ->
      missionProgressCount(mission, profile) >= mission.targetCount &&
        !isMissionClaimed(mission.id, profile)
    }
    if (readyMissions.isEmpty()) return
    readyMissions.forEach { mission ->
      claimDailyMission(mission, profile)
    }
  }

  fun purchaseOrUnlockStoreItem(item: StoreItem, profile: PlayerProfile?) {
    when (item.type) {
      StoreItemType.IAP_COIN_BUNDLE -> {
        soundManager.play(LudoSoundEvent.HOME)
        _uiState.update { state ->
          state.copy(
            localGuestCoins = state.localGuestCoins + item.coinsGranted,
            localGuestXp = state.localGuestXp + item.bonusXp,
            localUnlockedItems = (state.localUnlockedItems + item.id).distinct(),
            toastMessage = "Purchased ${item.title}: +${item.coinsGranted} Royal Coins & +${item.bonusXp} XP credited!"
          )
        }
        if (isAuthenticated) {
          viewModelScope.launch {
            repository.purchaseIapCoinBundle(
              bundleId = item.id,
              coinsToAdd = item.coinsGranted,
              bonusXp = item.bonusXp
            )
          }
        }
      }
      StoreItemType.COSMETIC_UNLOCK -> {
        val currentCoins = if (isAuthenticated && profile != null) profile.coins else _uiState.value.localGuestCoins
        val unlockedList = if (isAuthenticated && profile != null) {
          profile.unlockedItems + _uiState.value.localUnlockedItems
        } else {
          _uiState.value.localUnlockedItems
        }
        if (item.id in unlockedList) {
          _uiState.update { it.copy(toastMessage = "${item.title} is already equipped!") }
          return
        }
        if (currentCoins < item.coinCost) {
          _uiState.update {
            it.copy(errorMessage = "Need ${item.coinCost} Royal Coins to unlock ${item.title} (You have $currentCoins).")
          }
          return
        }

        soundManager.play(LudoSoundEvent.OPEN)
        _uiState.update { state ->
          state.copy(
            localGuestCoins = (state.localGuestCoins - item.coinCost).coerceAtLeast(0L),
            localGuestXp = state.localGuestXp + item.bonusXp,
            localUnlockedItems = (state.localUnlockedItems + item.id).distinct(),
            toastMessage = "Unlocked ${item.title} (+${item.bonusXp} XP)!"
          )
        }
        if (isAuthenticated) {
          viewModelScope.launch {
            repository.unlockStoreItemWithCoins(
              itemId = item.id,
              coinCost = item.coinCost,
              bonusXp = item.bonusXp
            )
          }
        }
      }
    }
  }

  // --- EMOTE GALLERY & 3D PAWN EMOTES ---

  fun isEmoteUnlocked(emote: PawnEmoteItem, profile: PlayerProfile?): Boolean {
    if (emote.unlockedByDefault) return true
    if (emote.id in EmoteGalleryCatalog.DEFAULT_UNLOCKED_IDS) return true
    if (emote.id in _uiState.value.localUnlockedItems) return true
    return profile?.unlockedItems?.contains(emote.id) == true
  }

  fun selectEmoteForPreview(emoteId: String) {
    val item = EmoteGalleryCatalog.findById(emoteId)
    soundManager.play(LudoSoundEvent.MOVE)
    _uiState.update {
      it.copy(
        selectedEmoteIdForPreview = item.id,
        selectedEmoteMomentSlot = item.recommendedTrigger,
        errorMessage = null
      )
    }
  }

  fun selectEmoteMomentSlot(moment: EmoteMomentTrigger) {
    _uiState.update { state ->
      val equippedForSlot = state.equippedEmoteForMoment(moment)
      state.copy(
        selectedEmoteMomentSlot = moment,
        selectedEmoteIdForPreview = equippedForSlot.id,
        errorMessage = null
      )
    }
  }

  fun setEmoteCategoryFilter(category: EmoteCategory?) {
    _uiState.update { it.copy(selectedEmoteCategoryFilter = category) }
  }

  fun unlockOrEquipPawnEmote(
    emote: PawnEmoteItem,
    targetMoment: EmoteMomentTrigger = _uiState.value.selectedEmoteMomentSlot,
    profile: PlayerProfile?
  ) {
    val alreadyUnlocked = isEmoteUnlocked(emote, profile)
    if (alreadyUnlocked) {
      soundManager.play(LudoSoundEvent.SAFE)
      val updatedLoadout = _uiState.value.equippedEmotesByMoment.toMutableMap().apply {
        put(targetMoment, emote.id)
      }
      _uiState.update { state ->
        state.copy(
          selectedEmoteIdForPreview = emote.id,
          selectedEmoteMomentSlot = targetMoment,
          equippedEmotesByMoment = updatedLoadout,
          toastMessage = "Equipped '${emote.title}' for ${targetMoment.label}!",
          errorMessage = null
        )
      }
      if (isAuthenticated) {
        viewModelScope.launch {
          repository.saveEquippedEmoteLoadout(loadout = updatedLoadout)
        }
      }
      return
    }

    val currentCoins = if (isAuthenticated && profile != null) {
      profile.coins
    } else {
      _uiState.value.localGuestCoins
    }

    if (currentCoins < emote.coinCost) {
      _uiState.update {
        it.copy(
          errorMessage = "Need ${emote.coinCost} Royal Coins to unlock ${emote.title} (You have $currentCoins)."
        )
      }
      return
    }

    soundManager.play(LudoSoundEvent.HOME)
    val updatedLoadout = _uiState.value.equippedEmotesByMoment.toMutableMap().apply {
      put(targetMoment, emote.id)
    }
    _uiState.update { state ->
      state.copy(
        localGuestCoins = (state.localGuestCoins - emote.coinCost).coerceAtLeast(0L),
        localGuestXp = state.localGuestXp + emote.bonusXp,
        localUnlockedItems = (state.localUnlockedItems + emote.id).distinct(),
        selectedEmoteIdForPreview = emote.id,
        selectedEmoteMomentSlot = targetMoment,
        equippedEmotesByMoment = updatedLoadout,
        toastMessage = "Unlocked & Equipped '${emote.title}' for ${targetMoment.label} (+${emote.bonusXp} XP)!",
        errorMessage = null
      )
    }

    if (isAuthenticated) {
      viewModelScope.launch {
        repository.saveEquippedEmoteLoadout(
          loadout = updatedLoadout,
          newlyUnlockedEmoteId = emote.id,
          coinCost = emote.coinCost,
          bonusXp = emote.bonusXp
        )
      }
    }
  }

  fun triggerPawnEmoteForSpecialMoment(
    tokenIndex: Int,
    pawnColor: PlayerColor,
    actorName: String,
    moment: EmoteMomentTrigger
  ) {
    val equippedEmote = _uiState.value.equippedEmoteForMoment(moment)
    val effect = ActivePawnEmoteEffect(
      tokenIndex = tokenIndex,
      pawnColor = pawnColor,
      emote = equippedEmote,
      triggerMoment = moment,
      actorName = actorName
    )
    pawnEmoteJob?.cancel()
    _uiState.update { it.copy(activePawnEmote = effect) }
    pawnEmoteJob = viewModelScope.launch {
      delay(3600L)
      _uiState.update { state ->
        if (state.activePawnEmote?.createdAtMillis == effect.createdAtMillis) {
          state.copy(activePawnEmote = null)
        } else {
          state
        }
      }
    }
  }

  fun triggerManualPawnEmote(emoteId: String? = null) {
    val state = _uiState.value
    val targetSeat = if (state.isSpectating) {
      state.seats.firstOrNull { it.name.equals(state.spectatingTargetName, ignoreCase = true) }
        ?: state.seats.firstOrNull { it.color == state.currentTurnColor }
        ?: state.seats.firstOrNull()
    } else {
      state.seats.firstOrNull { !it.isBot && (it.uid == currentUserId || !state.isOnlineMatch) }
        ?: state.seats.firstOrNull { it.color == state.currentTurnColor }
        ?: state.seats.firstOrNull()
    } ?: return

    val colorIndices = LudoRulesEngine.tokenIndicesForColor(targetSeat.color)
    val leadTokenIndex = colorIndices
      .maxByOrNull { idx -> state.tokens.getOrElse(idx) { -1 } }
      ?: colorIndices.first()

    val chosenEmote = if (emoteId != null) {
      EmoteGalleryCatalog.findById(emoteId)
    } else {
      state.equippedEmoteForMoment(state.selectedEmoteMomentSlot)
    }

    val actorLabel = if (state.isSpectating) {
      "${currentDisplayName()} (Spectator)"
    } else {
      targetSeat.name
    }

    soundManager.play(LudoSoundEvent.SAFE)
    val effect = ActivePawnEmoteEffect(
      tokenIndex = leadTokenIndex,
      pawnColor = targetSeat.color,
      emote = chosenEmote,
      triggerMoment = chosenEmote.recommendedTrigger,
      actorName = actorLabel
    )
    pawnEmoteJob?.cancel()
    _uiState.update {
      it.copy(
        activePawnEmote = effect,
        activeReactionEmoji = chosenEmote.stickerSymbol,
        activeReactionSender = actorLabel,
        statusBanner = "$actorLabel cheered ${chosenEmote.stickerSymbol} ${chosenEmote.title}: ${chosenEmote.calloutText}"
      )
    }
    if (state.isSpectating && isAuthenticated && state.isOnlineMatch && state.roomCode.isNotBlank()) {
      viewModelScope.launch {
        repository.sendEmojiReaction(state.roomCode, chosenEmote.stickerSymbol, actorLabel)
      }
    }
    pawnEmoteJob = viewModelScope.launch {
      delay(3600L)
      _uiState.update { s ->
        if (s.activePawnEmote?.createdAtMillis == effect.createdAtMillis) {
          s.copy(activePawnEmote = null)
        } else {
          s
        }
      }
    }
  }

  // --- WEEKLY TOURNAMENT LADDER BRACKETS & EXCLUSIVE COSMETICS ---

  fun isTournamentRewardUnlocked(
    reward: TournamentCosmeticReward,
    profile: PlayerProfile?
  ): Boolean {
    val state = _uiState.value
    if (state.tournamentWins >= reward.winsRequired) return true
    if (reward.id in state.localUnlockedItems) return true
    return profile?.unlockedItems?.contains(reward.id) == true
  }

  fun isTournamentRewardClaimed(
    reward: TournamentCosmeticReward,
    profile: PlayerProfile?
  ): Boolean {
    val state = _uiState.value
    if (reward.id in state.tournamentClaimedRewardIds) return true
    if (reward.id in state.localUnlockedItems) return true
    return profile?.unlockedItems?.contains(reward.id) == true
  }

  fun claimOrEquipTournamentReward(
    reward: TournamentCosmeticReward,
    profile: PlayerProfile?
  ) {
    if (!isTournamentRewardUnlocked(reward, profile)) {
      _uiState.update {
        it.copy(
          errorMessage = "Reach ${reward.requiredRound.title} (${reward.winsRequired} Bracket Win${if (reward.winsRequired == 1) "" else "s"}) to unlock ${reward.title}!"
        )
      }
      return
    }

    val alreadyClaimed = isTournamentRewardClaimed(reward, profile)
    if (alreadyClaimed) {
      soundManager.play(LudoSoundEvent.SAFE)
      _uiState.update {
        it.copy(
          equippedTournamentCosmeticId = reward.id,
          toastMessage = "Equipped Tournament Exclusive '${reward.title}'!",
          errorMessage = null
        )
      }
      return
    }

    soundManager.play(LudoSoundEvent.HOME)
    _uiState.update { state ->
      state.copy(
        localGuestCoins = state.localGuestCoins + reward.bonusCoins,
        localGuestXp = state.localGuestXp + reward.bonusXp,
        localUnlockedItems = (state.localUnlockedItems + reward.id).distinct(),
        tournamentClaimedRewardIds = (state.tournamentClaimedRewardIds + reward.id).distinct(),
        equippedTournamentCosmeticId = reward.id,
        toastMessage = "Claimed & Equipped '${reward.title}' (+${reward.bonusCoins} Coins & +${reward.bonusXp} XP)!",
        errorMessage = null
      )
    }

    if (isAuthenticated) {
      viewModelScope.launch {
        repository.claimDailyMissionReward(
          missionId = reward.id,
          coinReward = reward.bonusCoins,
          xpReward = reward.bonusXp
        )
        repository.unlockStoreItemWithCoins(
          itemId = reward.id,
          coinCost = 0L,
          bonusXp = 0L
        )
      }
    }
  }

  fun startTournamentBracketMatch() {
    val state = _uiState.value
    if (state.tournamentCurrentRound == TournamentRoundTier.CHAMPION) {
      _uiState.update {
        it.copy(
          toastMessage = "You are already the Weekly Sovereign Cup Champion! Reset bracket to start a new defense run."
        )
      }
      return
    }

    val activeMatch = state.activeTournamentMatch ?: return
    val playerSeatColor = activeMatch.player1Color
    val opponentColor = activeMatch.player2Color
    val localOwnerId = currentUserId ?: "LOCAL_GUEST"
    val playerName = currentDisplayName().takeIf { it != "Royal Player" } ?: activeMatch.player1Name

    val seats = listOf(
      PlayerSeat(
        color = playerSeatColor,
        uid = "$localOwnerId-TOURNEY-0",
        name = playerName,
        isBot = false
      ),
      PlayerSeat(
        color = opponentColor,
        uid = "BOT_TOURNEY_1",
        name = activeMatch.player2Name,
        isBot = true
      )
    )

    roomListenerJob?.cancel()
    val turnSeconds = adminConfigState.value.turnDurationSeconds.toInt().coerceIn(8, 120)
    val matchId = "TOURNEY-${activeMatch.matchId}-${++recordedMatchCounter}"
    val roundTitle = state.tournamentCurrentRound.title
    val kickoffFrame = ReplayFrame(
      frameIndex = 0,
      turnNumber = 1,
      actorColor = playerSeatColor,
      actorName = playerName,
      diceValue = 1,
      diceValue2 = 6,
      usedDieValue = 0,
      tokens = List(32) { -1 },
      eventSummary = "$roundTitle Bracket Match: $playerName vs ${activeMatch.player2Name}",
      tacticalNote = "Weekly Tournament ($roundTitle): Win to advance up the Sovereign Cup ladder!"
    )

    soundManager.play(LudoSoundEvent.OPEN)
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.GAME_ARENA,
        isOnlineMatch = false,
        isSpectating = false,
        spectatingTargetName = "",
        isTournamentMatchActive = true,
        roomCode = "CUP-${activeMatch.matchId}",
        isHost = true,
        roomStatus = RoomStatus.PLAYING,
        maxPlayers = 2,
        seats = seats,
        tokens = List(32) { -1 },
        currentTurnColor = playerSeatColor,
        turnPhase = TurnPhase.ROLL,
        diceValue = 1,
        diceValue2 = 6,
        dice1Used = false,
        dice2Used = false,
        selectedDieIndex = 0,
        isDiceRolling = false,
        consecutiveSixes = 0,
        turnBonusPending = false,
        turnNumber = 1,
        legalMoveTokenIndices = emptyList(),
        animatedToken = null,
        activeParticleBurst = null,
        turnSecondsRemaining = turnSeconds,
        statusBanner = "🏆 $roundTitle Duel: $playerName vs ${activeMatch.player2Name} — Roll 2 dice!",
        winnerColor = "NONE",
        winnerOrder = emptyList(),
        matchCapturesByMe = 0,
        matchHomesByMe = 0,
        activeMatchId = matchId,
        activeMatchRecordedFrames = if (it.isMatchRecordingEnabled) listOf(kickoffFrame) else emptyList(),
        cameraZoom = 1.04f,
        errorMessage = null,
        toastMessage = null
      )
    }
    onNewTurnReady()
  }

  fun advanceTournamentBracketRoundOnVictory(profile: PlayerProfile? = null) {
    val state = _uiState.value
    if (state.tournamentCurrentRound == TournamentRoundTier.CHAMPION) {
      _uiState.update {
        it.copy(toastMessage = "You have already conquered the Grand Finals as Sovereign Cup Champion!")
      }
      return
    }

    val currentRound = state.tournamentCurrentRound
    val playerName = (profile?.displayName?.takeIf { it.isNotBlank() } ?: currentDisplayName())
      .takeIf { it != "Royal Player" } ?: "You"
    val playerCountry = profile?.countryCode?.takeIf { it.isNotBlank() } ?: state.selectedCountryFilter
    val playerColor = state.activeTournamentMatch?.player1Color ?: PlayerColor.RED

    val (updatedMatches, nextRound) = WeeklyTournamentCatalog.advanceBracketOnPlayerWin(
      matches = state.tournamentBracketMatches,
      currentRound = currentRound,
      playerName = playerName,
      playerCountry = playerCountry,
      playerColor = playerColor,
      playerScore = 165 + state.tournamentWins * 20,
      opponentScore = 95
    )

    val newWins = (state.tournamentWins + 1).coerceAtMost(3)
    val newlyUnlockedReward = WeeklyTournamentCatalog.EXCLUSIVE_REWARDS.firstOrNull {
      it.winsRequired == newWins
    }
    val bonusCoins = newlyUnlockedReward?.bonusCoins ?: 150L
    val bonusXp = newlyUnlockedReward?.bonusXp ?: 80L
    val unlockedIdList = if (newlyUnlockedReward != null) {
      (state.localUnlockedItems + newlyUnlockedReward.id).distinct()
    } else {
      state.localUnlockedItems
    }
    val claimedRewardIds = if (newlyUnlockedReward != null) {
      (state.tournamentClaimedRewardIds + newlyUnlockedReward.id).distinct()
    } else {
      state.tournamentClaimedRewardIds
    }

    soundManager.playGameCelebration()
    _uiState.update { s ->
      s.copy(
        tournamentCurrentRound = nextRound,
        tournamentWins = newWins,
        tournamentLadderPoints = s.tournamentLadderPoints + 150,
        tournamentBracketMatches = updatedMatches,
        localGuestCoins = s.localGuestCoins + bonusCoins,
        localGuestXp = s.localGuestXp + bonusXp,
        localUnlockedItems = unlockedIdList,
        tournamentClaimedRewardIds = claimedRewardIds,
        equippedTournamentCosmeticId = newlyUnlockedReward?.id ?: s.equippedTournamentCosmeticId,
        sessionWinsCount = s.sessionWinsCount + 1,
        tournamentLiveFeedMessage = if (nextRound == TournamentRoundTier.CHAMPION) {
          "🏆 CHAMPION CROWNED • $playerName conquered the Grand Finals & won the Sovereign Cup!"
        } else {
          "LIVE BRACKET • $playerName won ${currentRound.title} and advanced to ${nextRound.title}!"
        },
        toastMessage = if (newlyUnlockedReward != null) {
          "Won ${currentRound.title}! Advanced to ${nextRound.title} & unlocked '${newlyUnlockedReward.title}' (+$bonusCoins Coins, +$bonusXp XP)!"
        } else {
          "Won ${currentRound.title}! Advanced to ${nextRound.title}!"
        },
        errorMessage = null
      )
    }

    if (isAuthenticated && newlyUnlockedReward != null) {
      viewModelScope.launch {
        repository.claimDailyMissionReward(
          missionId = newlyUnlockedReward.id,
          coinReward = bonusCoins,
          xpReward = bonusXp
        )
        repository.unlockStoreItemWithCoins(
          itemId = newlyUnlockedReward.id,
          coinCost = 0L,
          bonusXp = 0L
        )
      }
    }
  }

  fun resetTournamentBracketRun() {
    val playerName = currentDisplayName().takeIf { it != "Royal Player" } ?: "You"
    val playerCountry = _uiState.value.selectedCountryFilter
    soundManager.play(LudoSoundEvent.OPEN)
    _uiState.update { state ->
      state.copy(
        tournamentCurrentRound = TournamentRoundTier.QUARTERFINALS,
        tournamentWins = 0,
        tournamentBracketMatches = WeeklyTournamentCatalog.buildInitialBracket(
          playerName = playerName,
          playerCountry = playerCountry,
          playerColor = state.preferredOnlineColor
        ),
        tournamentLiveFeedMessage = "LIVE BRACKET • Fresh Weekly Bracket seeded — Quarterfinals ready for kickoff!",
        toastMessage = "Entered a new Sovereign Cup Weekly Bracket run!",
        errorMessage = null
      )
    }
  }

  fun saveAdminConfiguration(config: AdminAppConfig) {
    soundManager.play(LudoSoundEvent.SAFE)
    if (!isAuthenticated) {
      _uiState.update {
        it.copy(toastMessage = "Admin config applied locally. Sign in with Google to publish to Firebase Cloud.")
      }
      return
    }
    viewModelScope.launch {
      val result = repository.saveAdminConfig(config)
      result.onSuccess {
        _uiState.update {
          it.copy(toastMessage = "Firebase & Admin configuration saved to /appConfig/global")
        }
      }.onFailure { err ->
        _uiState.update {
          it.copy(errorMessage = err.localizedMessage ?: "Failed to save admin config")
        }
      }
    }
  }

  // --- PAWN MOVEMENT SPEED CONTROL ---

  fun setPawnMoveSpeed(speed: PawnMoveSpeed) {
    _uiState.update { it.copy(pawnMoveSpeed = speed) }
  }

  fun cyclePawnMoveSpeed() {
    val entries = PawnMoveSpeed.entries
    val current = _uiState.value.pawnMoveSpeed
    val next = entries[(entries.indexOf(current) + 1) % entries.size]
    _uiState.update {
      it.copy(
        pawnMoveSpeed = next,
        statusBanner = "Pawn move speed set to ${next.label}"
      )
    }
  }

  // --- 1-TO-1 & GROUP AUDIO CALL CONTROLS ---

  fun onMicPermissionResult(granted: Boolean) {
    audioCallManager.onMicPermissionResult(granted)
  }

  fun startGroupAudioCall() {
    val state = _uiState.value
    audioCallManager.startGroupCall(
      roomCode = state.roomCode,
      displayName = currentDisplayName(),
      currentActiveVoiceUids = state.roomActiveVoiceUids
    )
  }

  fun startOneToOneAudioCall(peerUid: String, peerName: String) {
    val state = _uiState.value
    audioCallManager.startOneToOneCall(
      roomCode = state.roomCode,
      displayName = currentDisplayName(),
      peerUid = peerUid,
      peerName = peerName,
      currentActiveVoiceUids = state.roomActiveVoiceUids
    )
  }

  fun endAudioCall() {
    val state = _uiState.value
    audioCallManager.endCall(
      roomCode = state.roomCode,
      displayName = currentDisplayName(),
      currentActiveVoiceUids = state.roomActiveVoiceUids
    )
  }

  fun toggleMicMute() {
    audioCallManager.toggleMicMute()
  }

  fun toggleSpeaker() {
    audioCallManager.toggleSpeaker()
  }

  fun toggleMutePeer(peerUid: String) {
    audioCallManager.toggleMutePeer(peerUid)
  }

  // --- 3D CAMERA CONTROLS ---

  fun adjustCamera(deltaYaw: Float, deltaPitch: Float, zoomFactor: Float) {
    _uiState.update { state ->
      state.copy(
        cameraYaw = (state.cameraYaw + deltaYaw) % 360f,
        cameraPitch = (state.cameraPitch + deltaPitch).coerceIn(25f, 82f),
        cameraZoom = (state.cameraZoom * zoomFactor).coerceIn(0.68f, 1.95f)
      )
    }
  }

  fun setCameraPreset(yaw: Float, pitch: Float, zoom: Float = 1.04f) {
    _uiState.update {
      it.copy(
        cameraYaw = yaw,
        cameraPitch = pitch.coerceIn(25f, 82f),
        cameraZoom = zoom.coerceIn(0.68f, 1.95f)
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
      val targetYaw = if (next) yawForColor(state.currentTurnColor, state.seats, state.boardArmCount) else state.cameraYaw
      state.copy(autoFocusTurnCamera = next, cameraYaw = targetYaw)
    }
  }

  fun toggleHudOverlayVisibility(visible: Boolean? = null) {
    _uiState.update { state ->
      val nextVisible = visible ?: !state.isHudOverlayVisible
      state.copy(isHudOverlayVisible = nextVisible)
    }
  }

  fun toggleHudOverlayExpanded(expanded: Boolean? = null) {
    _uiState.update { state ->
      val nextExpanded = expanded ?: !state.isHudOverlayExpanded
      state.copy(
        isHudOverlayVisible = true,
        isHudOverlayExpanded = nextExpanded
      )
    }
  }

  private fun yawForColor(
    color: PlayerColor,
    seats: List<PlayerSeat>,
    armCount: Int
  ): Float {
    val arm = LudoRulesEngine.armIndexForColor(color, seats, armCount)
    return if (armCount <= 4) {
      when (arm % 4) {
        0 -> -35f
        1 -> -125f
        2 -> 145f
        else -> 55f
      }
    } else {
      (-90f - (arm.toFloat() / armCount.toFloat()) * 360f) % 360f
    }
  }

  // --- OFFLINE STUDIO SETUP & CUSTOM COLOR SELECTION ---

  fun setOfflinePlayerCount(count: Int) {
    val validCount = when {
      count <= 2 -> 2
      count <= 4 -> 4
      count <= 6 -> 6
      else -> 8
    }
    _uiState.update { state ->
      val defaultColors = PlayerColor.defaultColorsForCount(validCount)
      val updatedDrafts = state.offlineDraftSeats.mapIndexed { idx, draft ->
        val enabled = idx < validCount
        val assignedColor = defaultColors.getOrElse(idx) {
          PlayerColor.entries[idx % PlayerColor.entries.size]
        }
        draft.copy(
          isEnabled = enabled,
          color = if (enabled) assignedColor else draft.color
        )
      }
      state.copy(
        offlinePlayerCount = validCount,
        offlineDraftSeats = updatedDrafts
      )
    }
  }

  fun updateOfflineSeatDraft(
    seatIndex: Int,
    isEnabled: Boolean? = null,
    isBot: Boolean? = null,
    name: String? = null,
    newColor: PlayerColor? = null
  ) {
    _uiState.update { state ->
      val list = state.offlineDraftSeats.toMutableList()
      val target = list.getOrNull(seatIndex) ?: return@update state
      val nextBot = isBot ?: target.isBot
      val nextColor = newColor ?: target.color
      val defaultName = if (nextBot) "${nextColor.label.substringBefore(" ")} AI" else "Player ${seatIndex + 1}"

      if (newColor != null) {
        val otherIdx = list.indexOfFirst { it.seatIndex != seatIndex && it.color == newColor }
        if (otherIdx >= 0) {
          list[otherIdx] = list[otherIdx].copy(color = target.color)
        }
      }

      list[seatIndex] = target.copy(
        color = nextColor,
        isEnabled = isEnabled ?: target.isEnabled,
        isBot = nextBot,
        name = name ?: if (isBot != null || newColor != null) defaultName else target.name
      )

      val enabledCount = list.count { it.isEnabled }
      if (enabledCount < 2) state else state.copy(offlineDraftSeats = list)
    }
  }

  fun setPreferredOnlineColor(color: PlayerColor) {
    _uiState.update { it.copy(preferredOnlineColor = color) }
  }

  fun selectOnlineSeatColor(newColor: PlayerColor) {
    val state = _uiState.value
    if (!isAuthenticated || !state.isOnlineMatch || state.roomCode.isBlank()) return
    viewModelScope.launch {
      val result = repository.changePlayerColorInRoom(state.roomCode, newColor)
      result.onFailure { err ->
        _uiState.update { it.copy(errorMessage = err.localizedMessage ?: "Color already taken") }
      }
    }
  }

  fun startOfflineGame() {
    roomListenerJob?.cancel()
    val localOwnerId = currentUserId ?: "LOCAL_GUEST"
    val drafts = _uiState.value.offlineDraftSeats.filter { it.isEnabled }
    val seats = drafts.mapIndexed { idx, draft ->
      PlayerSeat(
        color = draft.color,
        uid = if (draft.isBot) "BOT_$idx" else "$localOwnerId-LOCAL-$idx",
        name = draft.name.ifBlank { draft.color.label },
        isBot = draft.isBot
      )
    }
    val firstColor = seats.first().color
    val armCount = LudoRulesEngine.boardArmCountForPlayers(seats.size)
    val initialZoom = when (armCount) {
      8 -> 0.90f
      6 -> 0.95f
      else -> 1.04f
    }
    val turnSeconds = adminConfigState.value.turnDurationSeconds.toInt().coerceIn(8, 120)
    val matchId = "REC-${++recordedMatchCounter}-${System.currentTimeMillis() % 100000}"
    val kickoffFrame = ReplayFrame(
      frameIndex = 0,
      turnNumber = 1,
      actorColor = firstColor,
      actorName = seats.first().name,
      diceValue = 1,
      diceValue2 = 6,
      usedDieValue = 0,
      tokens = List(32) { -1 },
      eventSummary = "Match started! ${seats.first().name}'s turn to roll 2 dice.",
      tacticalNote = "Kickoff Frame: All ${seats.size * 4} pawns in starting yards across $armCount board arms."
    )
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.GAME_ARENA,
        isOnlineMatch = false,
        isSpectating = false,
        spectatingTargetName = "",
        isTournamentMatchActive = false,
        roomCode = "LOCAL-3D",
        isHost = true,
        roomStatus = RoomStatus.PLAYING,
        maxPlayers = seats.size,
        seats = seats,
        tokens = List(32) { -1 },
        currentTurnColor = firstColor,
        turnPhase = TurnPhase.ROLL,
        diceValue = 1,
        diceValue2 = 6,
        dice1Used = false,
        dice2Used = false,
        selectedDieIndex = 0,
        isDiceRolling = false,
        consecutiveSixes = 0,
        turnBonusPending = false,
        turnNumber = 1,
        legalMoveTokenIndices = emptyList(),
        animatedToken = null,
        activeParticleBurst = null,
        turnSecondsRemaining = turnSeconds,
        statusBanner = "Match started! ${seats.first().name}'s turn to roll 2 dice.",
        winnerColor = "NONE",
        winnerOrder = emptyList(),
        matchCapturesByMe = 0,
        matchHomesByMe = 0,
        activeMatchId = matchId,
        activeMatchRecordedFrames = if (it.isMatchRecordingEnabled) listOf(kickoffFrame) else emptyList(),
        cameraZoom = initialZoom,
        errorMessage = null
      )
    }
    onNewTurnReady()
  }

  // --- ONLINE MULTIPLAYER LOBBY & ROOMS ---

  fun createOnlinePrivateRoom(maxPlayers: Int = 4, isPublic: Boolean = false) {
    if (!isAuthenticated) return
    val code = LudoMultiplayerRepository.generateRoomCode()
    val preferredColor = _uiState.value.preferredOnlineColor
    _uiState.update { it.copy(isLoadingAction = true, errorMessage = null) }
    viewModelScope.launch {
      val result = repository.createRoom(
        roomCode = code,
        hostName = currentDisplayName(),
        isPublic = isPublic,
        maxPlayers = maxPlayers,
        hostColor = preferredColor
      )
      result.onSuccess { room ->
        _uiState.update {
          it.copy(
            isLoadingAction = false,
            currentScreen = AppScreen.WAITING_ROOM,
            isOnlineMatch = true,
            isSpectating = false,
            spectatingTargetName = "",
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
    // Check if the code matches a showcase ongoing private room when offline/demo
    val matchingOngoingPrivate = FriendsStudioCatalog.ONGOING_PRIVATE_ROOMS_SHOWCASE.firstOrNull {
      it.roomCode.equals(cleanCode, ignoreCase = true)
    }
    if (!isAuthenticated || matchingOngoingPrivate != null) {
      spectateOnlineRoomByCode(cleanCode)
      return
    }
    val preferredColor = _uiState.value.preferredOnlineColor
    _uiState.update { it.copy(isLoadingAction = true, errorMessage = null) }
    viewModelScope.launch {
      val result = repository.joinRoomByCode(cleanCode, currentDisplayName(), preferredColor)
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
            isSpectating = false,
            spectatingTargetName = "",
            roomCode = room.roomCode,
            isHost = room.hostUid == currentUserId,
            isPublicRoom = room.isPublic,
            maxPlayers = room.maxPlayers.toInt()
          )
        }
        subscribeToOnlineRoom(room.roomCode)
      }.onFailure { err ->
        val msg = err.localizedMessage.orEmpty()
        if (msg.contains("already started", ignoreCase = true) || msg.contains("is full", ignoreCase = true)) {
          // Ongoing private/public room -> automatically join as a view-only observer synced via Firestore!
          spectateOnlineRoomByCode(cleanCode)
        } else {
          _uiState.update {
            it.copy(
              isLoadingAction = false,
              errorMessage = msg.ifBlank { "Could not join room $cleanCode" }
            )
          }
        }
      }
    }
  }

  fun startQuickOnlineMatch(maxPlayers: Int = 4) {
    if (!isAuthenticated) return
    val preferredColor = _uiState.value.preferredOnlineColor
    _uiState.update { it.copy(isLoadingAction = true, errorMessage = null) }
    viewModelScope.launch {
      val result = repository.findOrCreateQuickMatch(
        playerName = currentDisplayName(),
        maxPlayers = maxPlayers,
        preferredColor = preferredColor
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
            isSpectating = false,
            spectatingTargetName = "",
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
    if (!isAuthenticated) return
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
    if (!isAuthenticated) return
    roomListenerJob?.cancel()
    chatListenerJob?.cancel()
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
    chatListenerJob = viewModelScope.launch {
      repository.observeRoomChatMessages(roomCode)
        .catch { err ->
          Log.w(TAG, "Room chat stream error", err)
        }
        .collect { cloudMessages ->
          _uiState.update { state ->
            val resolvedMessages = if (cloudMessages.isEmpty()) {
              PrivateRoomChatCatalog.buildInitialShowcaseMessages(roomCode)
            } else {
              cloudMessages
            }
            val addedCount = (resolvedMessages.size - state.roomChatMessages.size).coerceAtLeast(0)
            val nextUnread = if (state.isRoomChatOpen || state.currentScreen == AppScreen.WAITING_ROOM) {
              0
            } else {
              state.unreadChatCount + addedCount
            }
            state.copy(
              roomChatMessages = resolvedMessages,
              unreadChatCount = nextUnread
            )
          }
        }
    }
  }

  internal fun applyRemoteRoomSnapshot(room: LudoRoom) {
    val status = RoomStatus.fromString(room.status)
    val turnColor = PlayerColor.fromString(room.currentTurnColor)
    val phase = TurnPhase.fromString(room.turnPhase)
    val remoteTokens = room.tokenPositions
    val remoteSeats = room.seats
    val armCount = LudoRulesEngine.boardArmCountForPlayers(maxOf(room.maxPlayers.toInt(), remoteSeats.size))

    val die1 = room.diceValue.toInt().coerceIn(0, 6)
    val die2 = room.diceValue2.toInt().coerceIn(0, 6)
    val d1Used = room.dice1Used
    val d2Used = room.dice2Used
    val selIdx = room.selectedDieIndex.toInt().coerceIn(0, 1)
    val activeDie = if (selIdx == 0) die1 else die2

    val isObserver = _uiState.value.isSpectating ||
      (currentUserId != null && room.memberUids.isNotEmpty() && currentUserId !in room.memberUids)

    val legalIndices = if (!isObserver && phase == TurnPhase.MOVE && activeDie in 1..6) {
      LudoRulesEngine.getLegalMoveTokenIndices(remoteTokens, turnColor, activeDie, remoteSeats, armCount)
    } else {
      emptyList()
    }

    val targetScreen = when {
      isObserver -> AppScreen.GAME_ARENA
      status == RoomStatus.WAITING && _uiState.value.currentScreen == AppScreen.WAITING_ROOM -> AppScreen.WAITING_ROOM
      status == RoomStatus.PLAYING || status == RoomStatus.FINISHED -> AppScreen.GAME_ARENA
      else -> _uiState.value.currentScreen
    }

    _uiState.update { state ->
      val nextYaw = if (state.autoFocusTurnCamera) yawForColor(turnColor, remoteSeats, armCount) else state.cameraYaw
      val observerTarget = state.spectatingTargetName.ifBlank {
        room.playerNames.firstOrNull() ?: "Room #${room.roomCode}"
      }
      val resolvedBanner = if (isObserver) {
        val roomKind = if (room.isPublic) "Public" else "Private"
        "👁️ Observing $roomKind #${room.roomCode} • ${room.lastEvent}"
      } else {
        room.lastEvent
      }
      state.copy(
        currentScreen = targetScreen,
        isOnlineMatch = true,
        isSpectating = isObserver,
        spectatingTargetName = if (isObserver) observerTarget else "",
        roomCode = room.roomCode,
        isHost = !isObserver && room.hostUid == currentUserId,
        isPublicRoom = room.isPublic,
        roomStatus = status,
        maxPlayers = room.maxPlayers.toInt(),
        seats = remoteSeats,
        tokens = if (isAnimatingMove) state.tokens else remoteTokens,
        currentTurnColor = turnColor,
        turnPhase = phase,
        diceValue = if (die1 == 0) 1 else die1,
        diceValue2 = if (die2 == 0) 6 else die2,
        dice1Used = d1Used,
        dice2Used = d2Used,
        selectedDieIndex = selIdx,
        consecutiveSixes = room.consecutiveSixes.toInt(),
        turnNumber = room.turnNumber.toInt(),
        legalMoveTokenIndices = legalIndices,
        statusBanner = resolvedBanner,
        winnerColor = room.winnerColor,
        winnerOrder = room.winnerOrder,
        activeReactionEmoji = room.reactionEmoji,
        activeReactionSender = room.reactionSender,
        roomActiveVoiceUids = room.activeVoiceUids,
        roomCallMode = AudioCallMode.fromString(room.callMode),
        roomCallInitiatorUid = room.callInitiatorUid,
        roomCallTargetUid = room.callTargetUid,
        lastSyncedDieChoiceSummary = "Firestore Synced • Die #${selIdx + 1} (+$activeDie) selected",
        cameraYaw = nextYaw
      )
    }

    if (status == RoomStatus.FINISHED && statsRecordedForRoom != room.roomCode) {
      soundManager.playGameCelebration()
      if (isAuthenticated && !isObserver) {
        statsRecordedForRoom = room.roomCode
        val mySeat = remoteSeats.firstOrNull { it.uid == currentUserId }
        if (mySeat != null) {
          val iWon = room.winnerColor == mySeat.color.name
          val captures = _uiState.value.matchCapturesByMe
          val homes = _uiState.value.matchHomesByMe
          val delta = LudoRulesEngine.calculateMatchScoreDelta(
            won = iWon,
            forfeited = false,
            capturesInMatch = captures,
            homesReachedInMatch = homes
          )
          val entry = ProfileMatchHistoryEntry(
            id = "online_${room.roomCode}_${System.currentTimeMillis()}",
            outcome = if (iWon) MatchOutcomeType.WIN else MatchOutcomeType.LOSS,
            modeLabel = "Online Room #${room.roomCode} (${remoteSeats.size}P)",
            captures = captures,
            homesReached = homes,
            scoreDelta = delta,
            timestampLabel = "Just Now"
          )
          _uiState.update {
            it.copy(
              sessionWinsCount = if (iWon) it.sessionWinsCount + 1 else it.sessionWinsCount,
              sessionLossesCount = if (!iWon) it.sessionLossesCount + 1 else it.sessionLossesCount,
              localMatchHistory = listOf(entry) + it.localMatchHistory
            )
          }
          viewModelScope.launch {
            repository.recordMatchStats(
              won = iWon,
              capturesInMatch = captures,
              homesReachedInMatch = homes,
              forfeited = false,
              modeLabel = "Online ${remoteSeats.size}P #${room.roomCode}"
            )
          }
        }
      } else {
        statsRecordedForRoom = room.roomCode
      }
    }

    val turnKey = "${room.roomCode}-${room.turnNumber}-${room.currentTurnColor}-${room.turnPhase}-${room.dice1Used}-${room.dice2Used}"
    if (status == RoomStatus.PLAYING && turnKey != lastProcessedTurnKey) {
      lastProcessedTurnKey = turnKey
      onNewTurnReady()
    }
  }

  // --- DUAL-DICE SELECTION, TURN EXECUTION, AUTO-MOVE, AND TIMERS ---

  fun canCurrentUserAct(): Boolean {
    val state = _uiState.value
    if (state.isSpectating) return false
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
    if (state.isSpectating && state.isOnlineMatch) return false
    val activeSeat = state.seats.firstOrNull { it.color == state.currentTurnColor } ?: return false
    return if (!state.isOnlineMatch) {
      true
    } else if (activeSeat.isBot) {
      state.isHost
    } else {
      activeSeat.uid == currentUserId
    }
  }

  fun onSelectDie(dieIndex: Int) {
    val state = _uiState.value
    if (state.isSpectating) {
      _uiState.update {
        it.copy(statusBanner = "👁️ Spectator Mode (View-Only) — Use the Emoji, Chat & 3D Emote bar to cheer!")
      }
      return
    }
    if (state.turnPhase != TurnPhase.MOVE || state.isDiceRolling || isAnimatingMove) return
    if (!canCurrentUserAct()) return
    val idx = dieIndex.coerceIn(0, 1)
    val isUsed = if (idx == 0) state.dice1Used else state.dice2Used
    if (isUsed) return

    val dieVal = if (idx == 0) state.diceValue else state.diceValue2
    val legal = LudoRulesEngine.getLegalMoveTokenIndices(
      tokens = state.tokens,
      color = state.currentTurnColor,
      diceValue = dieVal,
      seats = state.seats,
      armCount = state.boardArmCount
    )
    val stepNum = if (state.dice1Used xor state.dice2Used) 2 else 1
    val actorName = state.seats.firstOrNull { it.color == state.currentTurnColor }?.name ?: state.currentTurnColor.label
    val bannerMsg = if (legal.isNotEmpty()) {
      "Step $stepNum/2: $actorName selected Die #${idx + 1} (+$dieVal) — tap a glowing 3D pawn"
    } else {
      "Step $stepNum/2: Die #${idx + 1} (+$dieVal) has no legal moves — switch to the other die"
    }
    val syncSummary = if (isAuthenticated && state.isOnlineMatch && state.roomCode.isNotBlank()) {
      "Step $stepNum/2 • Die #${idx + 1} (+$dieVal) synced to Firestore #${state.roomCode}"
    } else {
      "Step $stepNum/2 • Die #${idx + 1} (+$dieVal) selected (${legal.size} legal pawns)"
    }
    emitHapticEvent(LudoHapticEvent.DIE_SELECT)
    _uiState.update {
      it.copy(
        selectedDieIndex = idx,
        legalMoveTokenIndices = legal,
        statusBanner = bannerMsg,
        lastSyncedDieChoiceSummary = syncSummary
      )
    }
    if (isAuthenticated && state.isOnlineMatch && !state.isSpectating && state.roomCode.isNotBlank()) {
      viewModelScope.launch {
        repository.syncSelectedDieChoice(
          roomCode = state.roomCode,
          selectedDieIndex = idx,
          lastEvent = bannerMsg
        )
      }
    }
  }

  private fun onNewTurnReady() {
    turnTimerJob?.cancel()
    botActionJob?.cancel()

    val state = _uiState.value
    if (state.roomStatus != RoomStatus.PLAYING || state.turnPhase == TurnPhase.GAME_OVER) return

    // Trigger haptic feedback when a player's turn begins (TurnPhase.ROLL)
    if (state.turnPhase == TurnPhase.ROLL) {
      emitHapticEvent(LudoHapticEvent.TURN_START)
    }

    val configuredTurnDuration = adminConfigState.value.turnDurationSeconds.toInt().coerceIn(8, 120)

    turnTimerJob = viewModelScope.launch {
      for (remaining in configuredTurnDuration downTo 1) {
        _uiState.update { it.copy(turnSecondsRemaining = remaining) }
        delay(1000L)
      }
      _uiState.update { it.copy(turnSecondsRemaining = 0) }
      if (isCurrentTurnControlledLocally()) {
        when (_uiState.value.turnPhase) {
          TurnPhase.ROLL -> onRollDiceClicked(triggeredByBotOrTimer = true)
          TurnPhase.MOVE -> executeBotOrTimerBestDualMove()
          TurnPhase.GAME_OVER -> {}
        }
      }
    }

    val activeSeat = state.seats.firstOrNull { it.color == state.currentTurnColor }
    val shouldRunBot = activeSeat?.isBot == true && (!state.isOnlineMatch || state.isHost)

    if (shouldRunBot && state.turnPhase == TurnPhase.ROLL) {
      botActionJob = viewModelScope.launch {
        delay(420L)
        onRollDiceClicked(triggeredByBotOrTimer = true)
      }
    } else if (state.turnPhase == TurnPhase.MOVE && isCurrentTurnControlledLocally() && shouldRunBot) {
      botActionJob = viewModelScope.launch {
        delay(320L)
        executeBotOrTimerBestDualMove()
      }
    }
  }

  private fun executeBotOrTimerBestDualMove() {
    val s = _uiState.value
    val bestPair = LudoRulesEngine.chooseBestDieAndMove(
      tokens = s.tokens,
      color = s.currentTurnColor,
      diceValues = listOf(s.diceValue, s.diceValue2),
      diceUsed = listOf(s.dice1Used, s.dice2Used),
      seats = s.seats,
      armCount = s.boardArmCount
    )
    if (bestPair != null) {
      val (dieIdx, tokIdx) = bestPair
      val dieVal = if (dieIdx == 0) s.diceValue else s.diceValue2
      val legal = LudoRulesEngine.getLegalMoveTokenIndices(
        tokens = s.tokens,
        color = s.currentTurnColor,
        diceValue = dieVal,
        seats = s.seats,
        armCount = s.boardArmCount
      )
      _uiState.update {
        it.copy(
          selectedDieIndex = dieIdx,
          legalMoveTokenIndices = legal
        )
      }
      onTokenSelected(tokIdx, triggeredByBotOrAuto = true)
    }
  }

  fun onRollDiceClicked(triggeredByBotOrTimer: Boolean = false) {
    val state = _uiState.value
    if (!triggeredByBotOrTimer && state.isSpectating) {
      _uiState.update {
        it.copy(statusBanner = "👁️ Spectator Mode (View-Only) — Use the Emoji & 3D Emote bar below to cheer!")
      }
      return
    }
    if (state.turnPhase != TurnPhase.ROLL || state.isDiceRolling || isAnimatingMove) return
    if (!triggeredByBotOrTimer && !canCurrentUserAct()) return

    val rollClip = soundManager.playDiceRoll()
    emitHapticEvent(LudoHapticEvent.DICE_ROLL)
    _uiState.update {
      it.copy(lastPlayedSoundClipTitle = rollClip?.title ?: it.lastPlayedSoundClipTitle)
    }

    viewModelScope.launch {
      _uiState.update { it.copy(isDiceRolling = true) }
      delay(340L)

      val currentState = _uiState.value
      val die1 = Random.nextInt(1, 7)
      val die2 = Random.nextInt(1, 7)
      val currentColor = currentState.currentTurnColor
      val activeSeatName = currentState.seats.firstOrNull { it.color == currentColor }?.name ?: currentColor.label
      val hasAnySix = die1 == 6 || die2 == 6
      val isDoubleSix = die1 == 6 && die2 == 6
      val newSixes = if (hasAnySix) currentState.consecutiveSixes + 1 else 0
      val sixesRolledByMe = if (canUserClaimStats(currentColor)) {
        (if (die1 == 6) 1 else 0) + (if (die2 == 6) 1 else 0)
      } else {
        0
      }
      if (sixesRolledByMe > 0) {
        _uiState.update {
          it.copy(sessionSixesRolledCount = it.sessionSixesRolledCount + sixesRolledByMe)
        }
        if (isAuthenticated) {
          viewModelScope.launch {
            repository.recordDailyMissionProgress(mapOf(MissionType.ROLL_SIXES to sixesRolledByMe))
          }
        }
      }

      val doubleSixBurst = if (isDoubleSix) {
        ParticleBurstEvent(
          type = ParticleEffectType.GOLDEN_AURA,
          primaryColor = currentColor,
          subtitle = "Double 6s Golden Aura!"
        )
      } else {
        null
      }

      if (newSixes >= 3) {
        val activeColors = currentState.seats.map { it.color }
        val nextColor = LudoRulesEngine.nextActiveColor(
          current = currentColor,
          activeSeats = activeColors,
          finishedColors = currentState.winnerOrder
        )
        val forfeitMsg = "$activeSeatName rolled three 6s in a row — turn forfeited!"
        _uiState.update {
          it.copy(
            diceValue = die1,
            diceValue2 = die2,
            dice1Used = true,
            dice2Used = true,
            selectedDieIndex = 0,
            isDiceRolling = false,
            consecutiveSixes = 0,
            turnBonusPending = false,
            currentTurnColor = nextColor,
            turnPhase = TurnPhase.ROLL,
            turnNumber = it.turnNumber + 1,
            legalMoveTokenIndices = emptyList(),
            statusBanner = forfeitMsg,
            cameraYaw = if (it.autoFocusTurnCamera) yawForColor(nextColor, it.seats, it.boardArmCount) else it.cameraYaw
          )
        }
        syncStateIfOnline()
        onNewTurnReady()
        return@launch
      }

      val legal1 = LudoRulesEngine.getLegalMoveTokenIndices(
        tokens = currentState.tokens,
        color = currentColor,
        diceValue = die1,
        seats = currentState.seats,
        armCount = currentState.boardArmCount
      )
      val legal2 = LudoRulesEngine.getLegalMoveTokenIndices(
        tokens = currentState.tokens,
        color = currentColor,
        diceValue = die2,
        seats = currentState.seats,
        armCount = currentState.boardArmCount
      )

      if (legal1.isEmpty() && legal2.isEmpty()) {
        val noMoveMsg = "$activeSeatName rolled [$die1, $die2] — no legal moves available"
        _uiState.update {
          it.copy(
            diceValue = die1,
            diceValue2 = die2,
            dice1Used = true,
            dice2Used = true,
            isDiceRolling = false,
            activeParticleBurst = doubleSixBurst ?: it.activeParticleBurst,
            legalMoveTokenIndices = emptyList(),
            statusBanner = noMoveMsg
          )
        }
        delay(560L)
        val activeColors = _uiState.value.seats.map { it.color }
        val nextColor = if (hasAnySix) {
          currentColor
        } else {
          LudoRulesEngine.nextActiveColor(
            current = currentColor,
            activeSeats = activeColors,
            finishedColors = _uiState.value.winnerOrder
          )
        }
        _uiState.update {
          it.copy(
            currentTurnColor = nextColor,
            turnPhase = TurnPhase.ROLL,
            consecutiveSixes = if (hasAnySix) newSixes else 0,
            turnBonusPending = false,
            turnNumber = it.turnNumber + 1,
            statusBanner = if (hasAnySix) "$noMoveMsg • Rolled a 6: Bonus Roll!" else "$noMoveMsg • Next: ${nextColor.label}",
            cameraYaw = if (it.autoFocusTurnCamera) yawForColor(nextColor, it.seats, it.boardArmCount) else it.cameraYaw
          )
        }
        syncStateIfOnline()
        onNewTurnReady()
      } else {
        val initialSelectedIdx = when {
          legal1.isEmpty() -> 1
          legal2.isEmpty() -> 0
          die2 == 6 && die1 != 6 -> 1
          else -> 0
        }
        val initialLegal = if (initialSelectedIdx == 0) legal1 else legal2
        val activeDieVal = if (initialSelectedIdx == 0) die1 else die2

        val movePrompt = "Step 1/2: $activeSeatName rolled [$die1, $die2] — choose Die #1 or Die #2 (+$activeDieVal active)"
        _uiState.update {
          it.copy(
            diceValue = die1,
            diceValue2 = die2,
            dice1Used = false,
            dice2Used = false,
            selectedDieIndex = initialSelectedIdx,
            isDiceRolling = false,
            consecutiveSixes = newSixes,
            turnBonusPending = hasAnySix,
            turnPhase = TurnPhase.MOVE,
            legalMoveTokenIndices = initialLegal,
            activeParticleBurst = doubleSixBurst ?: it.activeParticleBurst,
            statusBanner = movePrompt,
            lastSyncedDieChoiceSummary = "Step 1/2 • Die #${initialSelectedIdx + 1} (+$activeDieVal) active"
          )
        }
        syncStateIfOnline()
        onNewTurnReady()
      }
    }
  }

  fun onTokenSelected(tokenIndex: Int, triggeredByBotOrAuto: Boolean = false) {
    val state = _uiState.value
    if (!triggeredByBotOrAuto && state.isSpectating) {
      _uiState.update {
        it.copy(statusBanner = "👁️ Spectator Mode (View-Only) — Use the Emoji & 3D Emote bar below to cheer!")
      }
      return
    }
    if (state.turnPhase != TurnPhase.MOVE || isAnimatingMove || state.isDiceRolling) return
    if (tokenIndex !in state.legalMoveTokenIndices) return
    if (!triggeredByBotOrAuto && !canCurrentUserAct()) return

    isAnimatingMove = true
    turnTimerJob?.cancel()
    botActionJob?.cancel()

    viewModelScope.launch {
      val startTokens = state.tokens
      val startPos = startTokens.getOrElse(tokenIndex) { -1 }
      val usedDieIndex = state.selectedDieIndex
      val usedDieValue = state.activeDieValue
      val armCount = state.boardArmCount

      val outcome = LudoRulesEngine.executeMove(
        tokens = startTokens,
        seats = state.seats,
        currentTurnColor = state.currentTurnColor,
        tokenIndex = tokenIndex,
        diceValue = usedDieValue,
        consecutiveSixes = state.consecutiveSixes,
        currentWinnerColor = state.winnerColor,
        currentWinnerOrder = state.winnerOrder,
        armCount = armCount
      )

      // Animate step-by-step according to user's selected PawnMoveSpeed, with a 3D glowing trail
      val speed = _uiState.value.pawnMoveSpeed
      val visitedTrailPositions = mutableListOf(startPos)
      if (speed != PawnMoveSpeed.INSTANT) {
        var stepFrom = startPos
        val workingTokens = startTokens.toMutableList()
        for ((stepIdx, stepTo) in outcome.pathSteps.withIndex()) {
          // Play subtle step click sound during movement (throttled so fast moves sound clean)
          if (stepIdx == 0 || stepIdx == outcome.pathSteps.lastIndex) {
            val moveClip = soundManager.playPawnMove()
            _uiState.update {
              it.copy(lastPlayedSoundClipTitle = moveClip?.title ?: it.lastPlayedSoundClipTitle)
            }
          }
          val historySnapshot = visitedTrailPositions.toList()
          val frames = speed.framesPerStep
          for (frame in 1..frames) {
            val progress = frame.toFloat() / frames.toFloat()
            _uiState.update {
              it.copy(
                animatedToken = AnimatedTokenState(
                  tokenIndex = tokenIndex,
                  fromPos = stepFrom,
                  toPos = stepTo,
                  progress = progress,
                  trailPathPositions = historySnapshot,
                  fadingWakeAlpha = 1f
                )
              )
            }
            delay(speed.stepDelayMs)
          }
          visitedTrailPositions.add(stepTo)
          workingTokens[tokenIndex] = stepTo
          stepFrom = stepTo
        }
        // Smooth landing afterglow wake so the 3D trail dissolves gracefully behind the pawn
        val finalPos = outcome.pathSteps.lastOrNull() ?: startPos
        val prevPos = if (visitedTrailPositions.size >= 2) {
          visitedTrailPositions[visitedTrailPositions.size - 2]
        } else {
          startPos
        }
        val fadeHistory = visitedTrailPositions.dropLast(1)
        for (wakeAlpha in listOf(0.68f, 0.36f)) {
          _uiState.update {
            it.copy(
              tokens = workingTokens.toList(),
              animatedToken = AnimatedTokenState(
                tokenIndex = tokenIndex,
                fromPos = prevPos,
                toPos = finalPos,
                progress = 1f,
                trailPathPositions = fadeHistory,
                fadingWakeAlpha = wakeAlpha
              )
            )
          }
          delay(22L)
        }
        _uiState.update { it.copy(animatedToken = null) }
      } else {
        val moveClip = soundManager.playPawnMove()
        _uiState.update {
          it.copy(lastPlayedSoundClipTitle = moveClip?.title ?: it.lastPlayedSoundClipTitle)
        }
      }

      // Play distinct outcome sound effects: GAME_CELEBRATION, HOME, CAPTURE, OPEN, SAFE
      val safeIndices = LudoRulesEngine.safeGlobalIndices(armCount)
      val landedOnSafe = outcome.destinationGlobalIndex != null && outcome.destinationGlobalIndex in safeIndices
      val openedFromYard = startPos == LudoRulesEngine.YARD_POS

      val outcomeClip = when {
        outcome.nextTurnPhase == TurnPhase.GAME_OVER -> {
          emitHapticEvent(LudoHapticEvent.HOME_OR_VICTORY)
          soundManager.playGameCelebration()
        }
        outcome.reachedHome -> {
          emitHapticEvent(LudoHapticEvent.HOME_OR_VICTORY)
          soundManager.play(LudoSoundEvent.HOME)
        }
        outcome.capturedTokenIndices.isNotEmpty() -> {
          emitHapticEvent(LudoHapticEvent.PAWN_CAPTURE)
          soundManager.play(LudoSoundEvent.CAPTURE)
        }
        openedFromYard -> {
          emitHapticEvent(LudoHapticEvent.PAWN_MOVE)
          soundManager.play(LudoSoundEvent.OPEN)
        }
        landedOnSafe || outcome.formedBlockade -> {
          emitHapticEvent(LudoHapticEvent.PAWN_MOVE)
          soundManager.play(LudoSoundEvent.SAFE)
        }
        else -> {
          emitHapticEvent(LudoHapticEvent.PAWN_MOVE)
          null
        }
      }
      if (outcomeClip != null) {
        _uiState.update {
          it.copy(lastPlayedSoundClipTitle = outcomeClip.title)
        }
      }

      // Check particle/fireworks triggers for this move
      val particleEvent: ParticleBurstEvent? = when {
        outcome.nextTurnPhase == TurnPhase.GAME_OVER || outcome.reachedHome -> {
          ParticleBurstEvent(
            type = ParticleEffectType.FIREWORKS,
            primaryColor = state.currentTurnColor,
            subtitle = if (outcome.nextTurnPhase == TurnPhase.GAME_OVER) "Victory Fireworks!" else "Home Goal Fireworks!"
          )
        }
        outcome.capturedTokenIndices.isNotEmpty() -> {
          val capturedColor = LudoRulesEngine.colorForTokenIndex(outcome.capturedTokenIndices.first())
          ParticleBurstEvent(
            type = ParticleEffectType.CAPTURE_SHOCKWAVE,
            primaryColor = state.currentTurnColor,
            secondaryColor = capturedColor,
            subtitle = "Capture Shockwave!"
          )
        }
        outcome.formedBlockade -> {
          ParticleBurstEvent(
            type = ParticleEffectType.GOLDEN_AURA,
            primaryColor = state.currentTurnColor,
            subtitle = "Blockade Shield Formed!"
          )
        }
        else -> null
      }

      val isGameOver = outcome.nextTurnPhase == TurnPhase.GAME_OVER
      val nextRoomStatus = if (isGameOver) RoomStatus.FINISHED else RoomStatus.PLAYING
      val isMyTurnForStats = canUserClaimStats(state.currentTurnColor)
      val myCapturesDelta = if (outcome.capturedTokenIndices.isNotEmpty() && isMyTurnForStats) {
        outcome.capturedTokenIndices.size
      } else {
        0
      }
      val myHomesDelta = if (outcome.reachedHome && isMyTurnForStats) 1 else 0
      val myOpensDelta = if (openedFromYard && isMyTurnForStats) 1 else 0
      if (isAuthenticated && (myCapturesDelta > 0 || myHomesDelta > 0 || myOpensDelta > 0)) {
        val moveMissionDeltas = buildMap<MissionType, Int> {
          if (myCapturesDelta > 0) put(MissionType.CAPTURE_PAWNS, myCapturesDelta)
          if (myHomesDelta > 0) put(MissionType.REACH_HOME, myHomesDelta)
          if (myOpensDelta > 0) put(MissionType.OPEN_FROM_YARD, myOpensDelta)
        }
        viewModelScope.launch {
          repository.recordDailyMissionProgress(moveMissionDeltas)
        }
      }

      val newDice1Used = if (usedDieIndex == 0) true else state.dice1Used
      val newDice2Used = if (usedDieIndex == 1) true else state.dice2Used
      val earnedBonusSoFar = state.turnBonusPending || outcome.bonusRollGranted

      // Check if the other die is still unused and has legal moves on the updated board
      val otherDieIndex = if (usedDieIndex == 0) 1 else 0
      val otherDieUsed = if (otherDieIndex == 0) newDice1Used else newDice2Used
      val otherDieValue = if (otherDieIndex == 0) state.diceValue else state.diceValue2

      val otherLegalMoves = if (!isGameOver && !outcome.playerFinishedAll && !otherDieUsed) {
        LudoRulesEngine.getLegalMoveTokenIndices(
          tokens = outcome.updatedTokens,
          color = state.currentTurnColor,
          diceValue = otherDieValue,
          seats = state.seats,
          armCount = armCount
        )
      } else {
        emptyList()
      }

      isAnimatingMove = false

      val actorSeatName = state.seats.firstOrNull { it.color == state.currentTurnColor }?.name ?: state.currentTurnColor.label
      val destStep = outcome.updatedTokens.getOrElse(tokenIndex) { startPos }

      // Trigger equipped 3D Pawn Emote / Visual Sticker above the moved pawn on special game moments!
      val specialEmoteMoment: EmoteMomentTrigger? = when {
        outcome.nextTurnPhase == TurnPhase.GAME_OVER || outcome.reachedHome -> EmoteMomentTrigger.HOME_GOAL
        outcome.capturedTokenIndices.isNotEmpty() -> EmoteMomentTrigger.OPPONENT_CAPTURE
        outcome.formedBlockade -> EmoteMomentTrigger.BLOCKADE_SHIELD
        openedFromYard -> EmoteMomentTrigger.YARD_OPEN
        landedOnSafe -> EmoteMomentTrigger.SAFE_STAR
        else -> null
      }
      if (specialEmoteMoment != null) {
        triggerPawnEmoteForSpecialMoment(
          tokenIndex = tokenIndex,
          pawnColor = state.currentTurnColor,
          actorName = actorSeatName,
          moment = specialEmoteMoment
        )
      }

      val tacticalNote = buildTacticalNote(
        seatName = actorSeatName,
        usedDieValue = usedDieValue,
        openedFromYard = openedFromYard,
        capturedCount = outcome.capturedTokenIndices.size,
        formedBlockade = outcome.formedBlockade,
        landedOnSafe = landedOnSafe,
        reachedHome = outcome.reachedHome,
        toStep = destStep,
        armCount = armCount
      )

      if (otherLegalMoves.isNotEmpty()) {
        // Sequential 2nd Die Phase: stay on current player's turn and switch to the remaining die!
        val seatName = actorSeatName
        _uiState.update {
          val updatedFrames = if (it.isMatchRecordingEnabled) {
            it.activeMatchRecordedFrames + ReplayFrame(
              frameIndex = it.activeMatchRecordedFrames.size,
              turnNumber = it.turnNumber,
              actorColor = state.currentTurnColor,
              actorName = seatName,
              diceValue = state.diceValue,
              diceValue2 = state.diceValue2,
              usedDieValue = usedDieValue,
              movedTokenIndex = tokenIndex,
              fromStep = startPos,
              toStep = destStep,
              tokens = outcome.updatedTokens,
              eventSummary = outcome.eventDescription,
              tacticalNote = tacticalNote,
              capturedCount = outcome.capturedTokenIndices.size,
              formedBlockade = outcome.formedBlockade,
              reachedHome = outcome.reachedHome
            )
          } else {
            it.activeMatchRecordedFrames
          }
          it.copy(
            roomStatus = nextRoomStatus,
            tokens = outcome.updatedTokens,
            turnPhase = TurnPhase.MOVE,
            dice1Used = newDice1Used,
            dice2Used = newDice2Used,
            selectedDieIndex = otherDieIndex,
            turnBonusPending = earnedBonusSoFar,
            legalMoveTokenIndices = otherLegalMoves,
            animatedToken = null,
            activeParticleBurst = particleEvent ?: it.activeParticleBurst,
            statusBanner = "Step 2/2: $seatName played +$usedDieValue • Now move +$otherDieValue with Die #${otherDieIndex + 1}",
            lastSyncedDieChoiceSummary = "Step 2/2 • Remaining Die #${otherDieIndex + 1} (+$otherDieValue) active",
            winnerColor = outcome.winnerColor,
            winnerOrder = outcome.updatedWinnerOrder,
            matchCapturesByMe = it.matchCapturesByMe + myCapturesDelta,
            matchHomesByMe = it.matchHomesByMe + myHomesDelta,
            sessionCapturesCount = it.sessionCapturesCount + myCapturesDelta,
            sessionHomesCount = it.sessionHomesCount + myHomesDelta,
            sessionOpensCount = it.sessionOpensCount + myOpensDelta,
            activeMatchRecordedFrames = updatedFrames
          )
        }
        saveActiveMatchRecordingToLibrary()
        syncStateIfOnline()
        onNewTurnReady()
      } else {
        // Both dice completed (or 2nd die has no legal moves) -> transition to Bonus Roll or Next Player
        val grantBonusRoll = !isGameOver && !outcome.playerFinishedAll && earnedBonusSoFar
        val activeColors = state.seats.map { it.color }
        val finalNextColor = if (grantBonusRoll) {
          state.currentTurnColor
        } else {
          LudoRulesEngine.nextActiveColor(
            current = state.currentTurnColor,
            activeSeats = activeColors,
            finishedColors = outcome.updatedWinnerOrder
          )
        }
        val finalPhase = if (isGameOver) TurnPhase.GAME_OVER else TurnPhase.ROLL
        val finalSixes = if (grantBonusRoll && (state.diceValue == 6 || state.diceValue2 == 6)) {
          state.consecutiveSixes
        } else {
          0
        }

        val bannerText = if (grantBonusRoll && !outcome.eventDescription.contains("Bonus roll", ignoreCase = true)) {
          "${outcome.eventDescription} • Bonus roll!"
        } else {
          outcome.eventDescription
        }

        val humanSeat = state.seats.firstOrNull { !it.isBot }
        val humanWon = isGameOver && humanSeat != null && outcome.winnerColor == humanSeat.color.name
        val humanLost = isGameOver && humanSeat != null && outcome.winnerColor != humanSeat.color.name
        val totalMyCaptures = state.matchCapturesByMe + myCapturesDelta
        val totalMyHomes = state.matchHomesByMe + myHomesDelta
        val matchModeDesc = when {
          state.isTournamentMatchActive -> "Weekly Cup ${state.tournamentCurrentRound.shortLabel}"
          state.isOnlineMatch -> "Online ${state.seats.size}P #${state.roomCode}"
          else -> "3D Studio ${state.seats.size}P Match"
        }

        _uiState.update {
          val updatedFrames = if (it.isMatchRecordingEnabled) {
            it.activeMatchRecordedFrames + ReplayFrame(
              frameIndex = it.activeMatchRecordedFrames.size,
              turnNumber = it.turnNumber,
              actorColor = state.currentTurnColor,
              actorName = actorSeatName,
              diceValue = state.diceValue,
              diceValue2 = state.diceValue2,
              usedDieValue = usedDieValue,
              movedTokenIndex = tokenIndex,
              fromStep = startPos,
              toStep = destStep,
              tokens = outcome.updatedTokens,
              eventSummary = bannerText,
              tacticalNote = tacticalNote,
              capturedCount = outcome.capturedTokenIndices.size,
              formedBlockade = outcome.formedBlockade,
              reachedHome = outcome.reachedHome
            )
          } else {
            it.activeMatchRecordedFrames
          }
          val updatedHistory = if (isGameOver && !state.isOnlineMatch && humanSeat != null) {
            val delta = LudoRulesEngine.calculateMatchScoreDelta(
              won = humanWon,
              forfeited = false,
              capturesInMatch = totalMyCaptures,
              homesReachedInMatch = totalMyHomes
            )
            val newEntry = ProfileMatchHistoryEntry(
              id = "local_${state.activeMatchId}",
              outcome = if (humanWon) MatchOutcomeType.WIN else MatchOutcomeType.LOSS,
              modeLabel = matchModeDesc,
              captures = totalMyCaptures,
              homesReached = totalMyHomes,
              scoreDelta = delta,
              timestampLabel = "Just Now"
            )
            listOf(newEntry) + it.localMatchHistory
          } else {
            it.localMatchHistory
          }
          it.copy(
            roomStatus = nextRoomStatus,
            tokens = outcome.updatedTokens,
            currentTurnColor = finalNextColor,
            turnPhase = finalPhase,
            dice1Used = true,
            dice2Used = true,
            turnBonusPending = false,
            consecutiveSixes = finalSixes,
            turnNumber = it.turnNumber + 1,
            legalMoveTokenIndices = emptyList(),
            animatedToken = null,
            activeParticleBurst = particleEvent ?: it.activeParticleBurst,
            statusBanner = bannerText,
            winnerColor = outcome.winnerColor,
            winnerOrder = outcome.updatedWinnerOrder,
            matchCapturesByMe = totalMyCaptures,
            matchHomesByMe = totalMyHomes,
            sessionCapturesCount = it.sessionCapturesCount + myCapturesDelta,
            sessionHomesCount = it.sessionHomesCount + myHomesDelta,
            sessionOpensCount = it.sessionOpensCount + myOpensDelta,
            sessionWinsCount = if (humanWon) it.sessionWinsCount + 1 else it.sessionWinsCount,
            sessionLossesCount = if (humanLost) it.sessionLossesCount + 1 else it.sessionLossesCount,
            localMatchHistory = updatedHistory,
            activeMatchRecordedFrames = updatedFrames,
            cameraYaw = if (it.autoFocusTurnCamera) yawForColor(finalNextColor, it.seats, armCount) else it.cameraYaw
          )
        }
        saveActiveMatchRecordingToLibrary()

        if (humanWon && state.isTournamentMatchActive) {
          val profile = (profileState.value as? DataStreamState.Success)?.data
          advanceTournamentBracketRoundOnVictory(profile)
        }

        if (isAuthenticated && isGameOver && !state.isOnlineMatch && humanSeat != null) {
          repository.recordMatchStats(
            won = outcome.winnerColor == humanSeat.color.name,
            capturesInMatch = _uiState.value.matchCapturesByMe,
            homesReachedInMatch = _uiState.value.matchHomesByMe,
            forfeited = false,
            modeLabel = matchModeDesc
          )
        }

        syncStateIfOnline()
        onNewTurnReady()
      }
    }
  }

  private fun canUserClaimStats(color: PlayerColor): Boolean {
    val state = _uiState.value
    if (state.isSpectating) return false
    val seat = state.seats.firstOrNull { it.color == color } ?: return false
    return !seat.isBot && (seat.uid == currentUserId || !state.isOnlineMatch)
  }

  private suspend fun syncStateIfOnline() {
    val state = _uiState.value
    if (state.isSpectating) return
    if (!isAuthenticated || !state.isOnlineMatch || state.roomCode.isBlank()) return
    repository.pushRoomGameState(
      roomCode = state.roomCode,
      status = state.roomStatus,
      tokens = state.tokens,
      currentTurnColor = state.currentTurnColor,
      turnPhase = state.turnPhase,
      diceValue = state.diceValue,
      diceValue2 = state.diceValue2,
      dice1Used = state.dice1Used,
      dice2Used = state.dice2Used,
      selectedDieIndex = state.selectedDieIndex,
      consecutiveSixes = state.consecutiveSixes,
      turnNumber = state.turnNumber,
      lastEvent = state.statusBanner,
      winnerColor = state.winnerColor,
      winnerOrder = state.winnerOrder
    )
  }

  fun sendQuickEmojiReaction(emoji: String) {
    val state = _uiState.value
    val sender = if (state.isSpectating) {
      "${currentDisplayName()} (Spectator)"
    } else {
      currentDisplayName()
    }
    soundManager.play(LudoSoundEvent.MOVE)
    _uiState.update {
      it.copy(
        activeReactionEmoji = emoji,
        activeReactionSender = sender,
        statusBanner = if (state.isSpectating) {
          "👁️ Spectator Cheer: $sender sent $emoji to the arena!"
        } else {
          it.statusBanner
        }
      )
    }
    if (isAuthenticated && state.isOnlineMatch && state.roomCode.isNotBlank()) {
      viewModelScope.launch {
        repository.sendEmojiReaction(state.roomCode, emoji, sender)
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Private Room Real-Time Text Chat (with Profanity Filter & Quick-Reply Emojis)
  // ---------------------------------------------------------------------------

  fun toggleRoomChat(open: Boolean? = null) {
    _uiState.update { state ->
      val nextOpen = open ?: !state.isRoomChatOpen
      state.copy(
        isRoomChatOpen = nextOpen,
        unreadChatCount = if (nextOpen) 0 else state.unreadChatCount,
        lastProfanityFilterNotice = if (!nextOpen) null else state.lastProfanityFilterNotice
      )
    }
  }

  fun updateChatDraftText(text: String) {
    _uiState.update { it.copy(chatDraftText = text.take(160)) }
  }

  fun clearProfanityNotice() {
    _uiState.update { it.copy(lastProfanityFilterNotice = null) }
  }

  fun sendQuickReplyChat(option: QuickReplyChatOption) {
    sendRoomChatMessage(
      rawInput = "${option.emoji} ${option.phrase}",
      quickEmoji = option.emoji
    )
    sendQuickEmojiReaction(option.emoji)
  }

  fun sendRoomChatMessage(
    rawInput: String = _uiState.value.chatDraftText,
    quickEmoji: String = ""
  ) {
    val state = _uiState.value
    val trimmed = rawInput.trim()
    if (trimmed.isBlank() && quickEmoji.isBlank()) return

    val sanitizeResult = PrivateRoomChatCatalog.sanitizeMessage(
      if (trimmed.isNotBlank()) trimmed else quickEmoji
    )
    if (sanitizeResult.cleanedText.isBlank()) return

    val mySeat = state.seats.firstOrNull { !it.isBot && (it.uid == currentUserId || !state.isOnlineMatch) }
    val senderColorName = when {
      state.isSpectating -> "SPECTATOR"
      mySeat != null -> mySeat.color.name
      else -> state.preferredOnlineColor.name
    }
    val senderDisplay = if (state.isSpectating) {
      "${currentDisplayName()} (Spectator)"
    } else {
      currentDisplayName()
    }
    val effectiveRoomCode = state.roomCode.ifBlank { "PRV409" }
    val localMsg = RoomChatMessage(
      id = "msg_${System.currentTimeMillis()}",
      roomCode = effectiveRoomCode,
      senderUid = currentUserId ?: "LOCAL_PLAYER",
      senderName = senderDisplay,
      senderColor = senderColorName,
      messageText = sanitizeResult.cleanedText,
      quickEmoji = quickEmoji.take(16),
      wasFiltered = sanitizeResult.wasFiltered
    )

    val filterWarning = if (sanitizeResult.wasFiltered) {
      "🛡️ Profanity Filter Active: Replaced ${sanitizeResult.filteredWordCount} restricted word(s) with ***"
    } else {
      null
    }

    soundManager.play(LudoSoundEvent.MOVE)
    _uiState.update { current ->
      current.copy(
        roomChatMessages = (current.roomChatMessages + localMsg).takeLast(50),
        chatDraftText = if (quickEmoji.isBlank()) "" else current.chatDraftText,
        lastProfanityFilterNotice = filterWarning ?: current.lastProfanityFilterNotice
      )
    }

    if (isAuthenticated && state.isOnlineMatch && state.roomCode.isNotBlank()) {
      viewModelScope.launch {
        repository.sendRoomChatMessage(
          roomCode = state.roomCode,
          senderName = senderDisplay,
          senderColor = senderColorName,
          rawMessageText = trimmed.ifBlank { quickEmoji },
          quickEmoji = quickEmoji
        )
      }
    }
  }

  fun leaveToLobby() {
    val state = _uiState.value
    // If leaving an active match after turns have progressed (and not spectating), record forfeit rank penalty
    if (!state.isSpectating &&
      state.currentScreen == AppScreen.GAME_ARENA &&
      state.roomStatus == RoomStatus.PLAYING &&
      state.turnNumber > 2
    ) {
      val forfeitDelta = LudoRulesEngine.calculateMatchScoreDelta(
        won = false,
        forfeited = true,
        capturesInMatch = state.matchCapturesByMe,
        homesReachedInMatch = state.matchHomesByMe
      )
      val modeDesc = if (state.isOnlineMatch) {
        "Online ${state.seats.size}P #${state.roomCode}"
      } else {
        "3D Studio ${state.seats.size}P Match"
      }
      val forfeitEntry = ProfileMatchHistoryEntry(
        id = "forfeit_${System.currentTimeMillis()}",
        outcome = MatchOutcomeType.FORFEIT,
        modeLabel = modeDesc,
        captures = state.matchCapturesByMe,
        homesReached = state.matchHomesByMe,
        scoreDelta = forfeitDelta,
        timestampLabel = "Forfeited"
      )
      _uiState.update {
        it.copy(
          sessionLossesCount = it.sessionLossesCount + 1,
          localMatchHistory = listOf(forfeitEntry) + it.localMatchHistory
        )
      }
      if (isAuthenticated) {
        viewModelScope.launch {
          repository.recordMatchStats(
            won = false,
            capturesInMatch = state.matchCapturesByMe,
            homesReachedInMatch = state.matchHomesByMe,
            forfeited = true,
            modeLabel = modeDesc
          )
        }
      }
    }

    if (audioCallState.value.isInCall) {
      endAudioCall()
    }

    roomListenerJob?.cancel()
    chatListenerJob?.cancel()
    turnTimerJob?.cancel()
    botActionJob?.cancel()
    replayPlaybackJob?.cancel()
    isAnimatingMove = false
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.LOBBY,
        isOnlineMatch = false,
        isSpectating = false,
        spectatingTargetName = "",
        isTournamentMatchActive = false,
        roomCode = "",
        isRoomChatOpen = false,
        unreadChatCount = 0,
        isReplayPlaying = false,
        legalMoveTokenIndices = emptyList(),
        animatedToken = null,
        activeParticleBurst = null,
        errorMessage = null
      )
    }
  }

  // ---------------------------------------------------------------------------
  // Interactive 'How to Play' 3D Tutorial Mode
  // ---------------------------------------------------------------------------

  fun openTutorialScreen(startStepIndex: Int = 0) {
    val safeIndex = startStepIndex.coerceIn(0, HowToPlayTutorialCatalog.LESSONS.lastIndex)
    val lesson = HowToPlayTutorialCatalog.findStepByIndex(safeIndex)
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.HOW_TO_PLAY_TUTORIAL,
        showRulesModal = false,
        tutorialStepIndex = safeIndex,
        tutorialActionState = lesson.initialActionType,
        tutorialTokens = lesson.initialTokens,
        tutorialDice1 = lesson.scriptedDie1,
        tutorialDice2 = lesson.scriptedDie2,
        tutorialSelectedDieIndex = if (lesson.initialActionType == TutorialInteractiveActionType.SELECT_DIE_SLOT) 0 else lesson.requiredDieSelectionIndex,
        tutorialDiceRolledInStep = lesson.initialActionType != TutorialInteractiveActionType.ROLL_DUAL_DICE,
        tutorialIsRollingDice = false,
        tutorialAnimatedToken = null,
        tutorialCoachBanner = lesson.coachInstruction,
        cameraYaw = lesson.cameraYaw,
        cameraPitch = lesson.cameraPitch,
        cameraZoom = lesson.cameraZoom,
        activeParticleBurst = null,
        activePawnEmote = null,
        errorMessage = null
      )
    }
  }

  fun selectTutorialLesson(stepIndex: Int) {
    val safeIndex = stepIndex.coerceIn(0, HowToPlayTutorialCatalog.LESSONS.lastIndex)
    val lesson = HowToPlayTutorialCatalog.findStepByIndex(safeIndex)
    _uiState.update {
      it.copy(
        tutorialStepIndex = safeIndex,
        tutorialActionState = lesson.initialActionType,
        tutorialTokens = lesson.initialTokens,
        tutorialDice1 = lesson.scriptedDie1,
        tutorialDice2 = lesson.scriptedDie2,
        tutorialSelectedDieIndex = if (lesson.initialActionType == TutorialInteractiveActionType.SELECT_DIE_SLOT) 0 else lesson.requiredDieSelectionIndex,
        tutorialDiceRolledInStep = lesson.initialActionType != TutorialInteractiveActionType.ROLL_DUAL_DICE,
        tutorialIsRollingDice = false,
        tutorialAnimatedToken = null,
        tutorialCoachBanner = lesson.coachInstruction,
        cameraYaw = lesson.cameraYaw,
        cameraPitch = lesson.cameraPitch,
        cameraZoom = lesson.cameraZoom,
        activeParticleBurst = null,
        activePawnEmote = null
      )
    }
  }

  fun resetCurrentTutorialLesson() {
    selectTutorialLesson(_uiState.value.tutorialStepIndex)
  }

  fun performTutorialRollDice() {
    val state = _uiState.value
    val lesson = state.currentTutorialLesson
    if (state.tutorialIsRollingDice) return

    soundManager.playDiceRoll()

    if (state.tutorialActionState != TutorialInteractiveActionType.ROLL_DUAL_DICE) {
      if (state.tutorialActionState == TutorialInteractiveActionType.INSPECT_BOARD) {
        performTutorialBoardOrPawnAction()
      }
      return
    }

    viewModelScope.launch {
      _uiState.update { it.copy(tutorialIsRollingDice = true) }
      delay(300L)
      _uiState.update { current ->
        current.copy(
          tutorialIsRollingDice = false,
          tutorialDiceRolledInStep = true,
          tutorialDice1 = lesson.scriptedDie1,
          tutorialDice2 = lesson.scriptedDie2,
          tutorialSelectedDieIndex = lesson.requiredDieSelectionIndex,
          tutorialActionState = TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN,
          tutorialCoachBanner = lesson.secondaryPromptAfterSubAction.ifBlank { lesson.coachInstruction }
        )
      }
    }
  }

  fun performTutorialSelectDie(dieIndex: Int) {
    val state = _uiState.value
    val lesson = state.currentTutorialLesson
    val safeDie = dieIndex.coerceIn(0, 1)
    soundManager.play(LudoSoundEvent.MOVE)

    if (state.tutorialActionState == TutorialInteractiveActionType.SELECT_DIE_SLOT) {
      if (safeDie == lesson.requiredDieSelectionIndex) {
        _uiState.update {
          it.copy(
            tutorialSelectedDieIndex = safeDie,
            tutorialActionState = TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN,
            tutorialCoachBanner = lesson.secondaryPromptAfterSubAction.ifBlank { lesson.coachInstruction }
          )
        }
      } else {
        val targetValue = if (lesson.requiredDieSelectionIndex == 0) lesson.scriptedDie1 else lesson.scriptedDie2
        _uiState.update {
          it.copy(
            tutorialSelectedDieIndex = safeDie,
            tutorialCoachBanner = "Tip: Tap Die #${lesson.requiredDieSelectionIndex + 1} [$targetValue] so your Red Pawn lands directly on the Safe Star (#8)!"
          )
        }
      }
    } else {
      _uiState.update { it.copy(tutorialSelectedDieIndex = safeDie) }
    }
  }

  fun performTutorialBoardOrPawnAction(tappedTokenIndex: Int = -1) {
    val state = _uiState.value
    val lesson = state.currentTutorialLesson

    when (state.tutorialActionState) {
      TutorialInteractiveActionType.INSPECT_BOARD -> {
        soundManager.play(LudoSoundEvent.SAFE)
        _uiState.update {
          it.copy(
            tutorialActionState = TutorialInteractiveActionType.LESSON_COMPLETED,
            tutorialTokens = lesson.afterActionTokens,
            tutorialCompletedStepIds = it.tutorialCompletedStepIds + lesson.id,
            tutorialCoachBanner = lesson.completedFeedback,
            activeParticleBurst = ParticleBurstEvent(
              id = System.nanoTime(),
              type = ParticleEffectType.GOLDEN_AURA,
              primaryColor = PlayerColor.YELLOW
            )
          )
        }
      }

      TutorialInteractiveActionType.ROLL_DUAL_DICE -> {
        performTutorialRollDice()
      }

      TutorialInteractiveActionType.SELECT_DIE_SLOT -> {
        performTutorialSelectDie(lesson.requiredDieSelectionIndex)
      }

      TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN -> {
        if (tappedTokenIndex >= 0 && lesson.targetTokenIndex >= 0 && tappedTokenIndex != lesson.targetTokenIndex) {
          _uiState.update {
            it.copy(
              tutorialCoachBanner = "Tap the highlighted Red Pawn #${(lesson.targetTokenIndex % 4) + 1} (marked with the golden 3D spotlight) to complete this move!"
            )
          }
          return
        }

        val tokenIdx = lesson.targetTokenIndex.coerceAtLeast(0)
        val fromPos = lesson.fromRelativePos
        val toPos = lesson.toRelativePos

        viewModelScope.launch {
          // Build intermediate step path for multi-tile tutorial moves so the 3D trail follows the track
          val stepPath = if (fromPos >= 0 && toPos > fromPos && (toPos - fromPos) <= 8) {
            (fromPos..toPos).toList()
          } else {
            listOf(fromPos, toPos)
          }
          // Smooth 3D step animation from fromPos to toPos with player-colored 3D glow trail
          val frameCount = 6
          for (step in 1..frameCount) {
            val progress = step.toFloat() / frameCount.toFloat()
            _uiState.update {
              it.copy(
                tutorialAnimatedToken = AnimatedTokenState(
                  tokenIndex = tokenIdx,
                  fromPos = fromPos,
                  toPos = toPos,
                  progress = progress,
                  trailPathPositions = stepPath.dropLast(1),
                  fadingWakeAlpha = 1f
                )
              )
            }
            delay(32L)
          }

          val soundEvent = when (lesson.stepNumber) {
            2 -> LudoSoundEvent.OPEN
            3 -> LudoSoundEvent.SAFE
            4 -> LudoSoundEvent.CAPTURE
            5 -> LudoSoundEvent.SAFE
            6 -> LudoSoundEvent.HOME
            else -> LudoSoundEvent.MOVE
          }
          soundManager.play(soundEvent)

          val particleType = when (lesson.stepNumber) {
            4 -> ParticleEffectType.CAPTURE_SHOCKWAVE
            6 -> ParticleEffectType.FIREWORKS
            else -> ParticleEffectType.GOLDEN_AURA
          }

          val emoteMoment = when (lesson.stepNumber) {
            2 -> EmoteMomentTrigger.YARD_OPEN
            3 -> EmoteMomentTrigger.SAFE_STAR
            4 -> EmoteMomentTrigger.OPPONENT_CAPTURE
            5 -> EmoteMomentTrigger.BLOCKADE_SHIELD
            6 -> EmoteMomentTrigger.HOME_GOAL
            else -> null
          }

          val activeEmote = emoteMoment?.let { moment ->
            ActivePawnEmoteEffect(
              tokenIndex = tokenIdx,
              pawnColor = PlayerColor.RED,
              emote = _uiState.value.equippedEmoteForMoment(moment),
              triggerMoment = moment,
              actorName = "You"
            )
          }

          _uiState.update { current ->
            current.copy(
              tutorialAnimatedToken = null,
              tutorialTokens = lesson.afterActionTokens,
              tutorialActionState = TutorialInteractiveActionType.LESSON_COMPLETED,
              tutorialCompletedStepIds = current.tutorialCompletedStepIds + lesson.id,
              tutorialCoachBanner = lesson.completedFeedback,
              activeParticleBurst = ParticleBurstEvent(
                id = System.nanoTime(),
                type = particleType,
                primaryColor = PlayerColor.RED
              ),
              activePawnEmote = activeEmote
            )
          }
        }
      }

      TutorialInteractiveActionType.LESSON_COMPLETED -> {
        advanceTutorialLesson()
      }
    }
  }

  fun advanceTutorialLesson() {
    val state = _uiState.value
    if (state.tutorialStepIndex < HowToPlayTutorialCatalog.LESSONS.lastIndex) {
      selectTutorialLesson(state.tutorialStepIndex + 1)
    } else {
      claimTutorialGraduationReward()
    }
  }

  fun claimTutorialGraduationReward() {
    val state = _uiState.value
    if (state.tutorialGraduationRewardClaimed) {
      _uiState.update {
        it.copy(
          toastMessage = "You've already graduated from the 3D Academy! Launching Lobby..."
        )
      }
      leaveToLobby()
      return
    }

    val coinBonus = HowToPlayTutorialCatalog.TUTORIAL_GRADUATION_COIN_REWARD
    val xpBonus = HowToPlayTutorialCatalog.TUTORIAL_GRADUATION_XP_REWARD
    soundManager.playGameCelebration()

    _uiState.update {
      it.copy(
        tutorialGraduationRewardClaimed = true,
        localGuestCoins = it.localGuestCoins + coinBonus,
        localGuestXp = it.localGuestXp + xpBonus,
        toastMessage = "3D Academy Graduated! Earned +$coinBonus Royal Coins & +$xpBonus XP!"
      )
    }

    if (isAuthenticated) {
      viewModelScope.launch {
        repository.claimDailyMissionReward(
          missionId = "tutorial_academy_graduation",
          coinReward = coinBonus,
          xpReward = xpBonus
        )
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    roomListenerJob?.cancel()
    chatListenerJob?.cancel()
    turnTimerJob?.cancel()
    botActionJob?.cancel()
    replayPlaybackJob?.cancel()
    tournamentTickerJob?.cancel()
    audioCallManager.cleanup()
  }

  private companion object {
    const val TAG = "LudoGameVM"
  }
}
