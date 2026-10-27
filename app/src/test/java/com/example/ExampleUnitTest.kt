package com.example

import com.example.data.CountryCatalog
import com.example.data.EmoteCategory
import com.example.data.EmoteGalleryCatalog
import com.example.data.EmoteMomentTrigger
import com.example.data.PlayerColor
import com.example.data.PlayerSeat
import com.example.data.ReplayCameraPreset
import com.example.data.RoyalStoreCatalog
import com.example.data.SampleReplayCatalog
import com.example.domain.LudoRulesEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    val initialTokens = List(32) { -1 }
    assertTrue(LudoRulesEngine.getLegalMoveTokenIndices(initialTokens, PlayerColor.RED, 5).isEmpty())
    assertEquals(listOf(0, 1, 2, 3), LudoRulesEngine.getLegalMoveTokenIndices(initialTokens, PlayerColor.RED, 6))
  }

  @Test
  fun blockade_twoSameColorOpponentTokensBlockPath() {
    val tokens = MutableList(32) { -1 }
    tokens[0] = 10
    tokens[4] = 0
    tokens[5] = 0

    assertFalse(LudoRulesEngine.canTokenMove(tokens, tokenIndex = 0, diceValue = 4))
    assertTrue(LudoRulesEngine.canTokenMove(tokens, tokenIndex = 0, diceValue = 2))
  }

  @Test
  fun captureAndSafeStar_capturesOnNormalTileButNotOnSafeStar() {
    val tokens = MutableList(32) { -1 }
    tokens[0] = 2
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

  @Test
  fun dualDiceAndEightPlayerBoard_evaluatesMovesAndScoresCorrectly() {
    val eightPlayerSeats = PlayerColor.entries.mapIndexed { idx, c ->
      PlayerSeat(c, "u$idx", c.label)
    }
    val tokens = MutableList(32) { -1 }
    // Roll [4, 6]: Die 2 (value 6, index 1) should be selected to exit the yard for ROSE (ordinal 7 -> tokens 28..31)
    val bestChoice = LudoRulesEngine.chooseBestDieAndMove(
      tokens = tokens,
      color = PlayerColor.ROSE,
      diceValues = listOf(4, 6),
      diceUsed = listOf(false, false),
      seats = eightPlayerSeats,
      armCount = 8
    )
    assertNotNull(bestChoice)
    assertEquals(1, bestChoice?.first)
    assertTrue(bestChoice?.second in 28..31)

    // Verify leaderboard scoring formula (+100 win + 2*25 captures + 4*15 home = 210)
    val scoreDelta = LudoRulesEngine.calculateMatchScoreDelta(
      won = true,
      forfeited = false,
      capturesInMatch = 2,
      homesReachedInMatch = 4
    )
    assertEquals(210, scoreDelta)
  }

  @Test
  fun locationCurrency_formatsRegionalStorePricesAndEquivalents() {
    val starterPack = RoyalStoreCatalog.IAP_BUNDLES.first()
    assertEquals("$0.99", starterPack.localizedPrice("US"))
    assertTrue(starterPack.localizedPrice("PK").startsWith("Rs "))
    assertTrue(starterPack.localizedPrice("IN").startsWith("₹"))
    assertTrue(starterPack.localizedPrice("GB").startsWith("£"))
    assertTrue(CountryCatalog.formatCoinsEquivalent(500L, "PK").contains("PKR"))
    assertTrue(CountryCatalog.formatCoinsEquivalent(500L, "US").contains("USD"))
  }

  @Test
  fun matchReplayCatalogAndCameraPresets_provideValidFramesAndAngles() {
    val replays = SampleReplayCatalog.SHOWCASE_REPLAYS
    assertTrue(replays.size >= 2)
    replays.forEach { record ->
      assertTrue(record.frames.isNotEmpty())
      record.frames.forEach { frame ->
        assertEquals(32, frame.tokens.size)
        assertTrue(frame.tacticalNote.isNotBlank())
      }
    }
    assertEquals(8, ReplayCameraPreset.entries.size)
    assertTrue(ReplayCameraPreset.FOLLOW_ACTIVE_TURN.autoFollowActiveColor)
    assertTrue(ReplayCameraPreset.TOP_DOWN_TACTICAL.pitch > 75f)
  }

  @Test
  fun emoteGalleryAndSpecialMomentLoadout_provides3DAnimationsAndStickers() {
    val allEmotes = EmoteGalleryCatalog.ALL_EMOTES
    assertEquals(10, allEmotes.size)
    assertTrue(allEmotes.any { it.category == EmoteCategory.ANIMATION_3D })
    assertTrue(allEmotes.any { it.category == EmoteCategory.VISUAL_STICKER })
    assertEquals(3, EmoteGalleryCatalog.DEFAULT_UNLOCKED_IDS.size)

    EmoteMomentTrigger.entries.forEach { moment ->
      val defaultId = EmoteGalleryCatalog.DEFAULT_EQUIPPED_LOADOUT[moment]
      assertNotNull(defaultId)
      val resolved = EmoteGalleryCatalog.findById(defaultId!!)
      assertTrue(resolved.unlockedByDefault)
      assertTrue(resolved.calloutText.isNotBlank())
    }
  }

  @Test
  fun weeklyTournamentLadderAndRewards_advancesRoundsAndFormatsCountdown() {
    val initialBracket = com.example.data.WeeklyTournamentCatalog.buildInitialBracket(
      playerName = "Aria",
      playerCountry = "US",
      playerColor = PlayerColor.RED
    )
    assertEquals(7, initialBracket.size)
    assertEquals(4, initialBracket.count { it.roundTier == com.example.data.TournamentRoundTier.QUARTERFINALS })
    assertEquals(2, initialBracket.count { it.roundTier == com.example.data.TournamentRoundTier.SEMIFINALS })
    assertEquals(1, initialBracket.count { it.roundTier == com.example.data.TournamentRoundTier.GRAND_FINALS })

    // Advance from Quarterfinals -> Semifinals -> Grand Finals -> Champion
    val (afterQf, round2) = com.example.data.WeeklyTournamentCatalog.advanceBracketOnPlayerWin(
      matches = initialBracket,
      currentRound = com.example.data.TournamentRoundTier.QUARTERFINALS,
      playerName = "Aria",
      playerCountry = "US",
      playerColor = PlayerColor.RED
    )
    assertEquals(com.example.data.TournamentRoundTier.SEMIFINALS, round2)
    assertEquals("Aria", afterQf.first { it.matchId == "QF-1" }.winnerName)
    assertTrue(afterQf.first { it.matchId == "SF-1" }.isPlayerMatch)

    val (afterSf, round3) = com.example.data.WeeklyTournamentCatalog.advanceBracketOnPlayerWin(
      matches = afterQf,
      currentRound = round2,
      playerName = "Aria",
      playerCountry = "US",
      playerColor = PlayerColor.RED
    )
    assertEquals(com.example.data.TournamentRoundTier.GRAND_FINALS, round3)
    assertEquals("Aria", afterSf.first { it.matchId == "SF-1" }.winnerName)
    assertTrue(afterSf.first { it.matchId == "GF-1" }.isPlayerMatch)

    val (afterGf, round4) = com.example.data.WeeklyTournamentCatalog.advanceBracketOnPlayerWin(
      matches = afterSf,
      currentRound = round3,
      playerName = "Aria",
      playerCountry = "US",
      playerColor = PlayerColor.RED
    )
    assertEquals(com.example.data.TournamentRoundTier.CHAMPION, round4)
    assertEquals("Aria", afterGf.first { it.matchId == "GF-1" }.winnerName)

    // Verify exclusive rewards and countdown formatting
    assertEquals(4, com.example.data.WeeklyTournamentCatalog.EXCLUSIVE_REWARDS.size)
    val formatted = com.example.data.WeeklyTournamentCatalog.formatRemainingTime(90_061L)
    assertEquals("01d 01h 01m 01s", formatted)
  }

  @Test
  fun profileStatisticsAndFirestoreLoadoutEncoding_computesWinLossRankAndEquippedEmotes() {
    val customLoadout = EmoteGalleryCatalog.DEFAULT_EQUIPPED_LOADOUT.toMutableMap().apply {
      put(EmoteMomentTrigger.OPPONENT_CAPTURE, "emote_3d_thunder_strike")
      put(EmoteMomentTrigger.HOME_GOAL, "emote_3d_celestial_helix")
    }
    val encodedUnlocked = com.example.data.ProfileCatalog.encodeEquippedLoadoutIntoUnlockedItems(
      existingUnlockedItems = listOf("emote_3d_thunder_strike"),
      loadout = customLoadout,
      additionalUnlockedItemId = "emote_3d_celestial_helix"
    )
    assertTrue(encodedUnlocked.size <= 48)

    val matchEntry = com.example.data.ProfileMatchHistoryEntry(
      id = "hist_1",
      outcome = com.example.data.MatchOutcomeType.WIN,
      modeLabel = "Online 4P #ROYAL1",
      captures = 4,
      homesReached = 4,
      scoreDelta = 260
    )
    val encodedClaimed = com.example.data.ProfileCatalog.encodeMatchHistoryIntoClaimedList(
      existingClaimed = listOf("mission_win_1"),
      entry = matchEntry
    )

    val profile = com.example.data.PlayerProfile(
      id = "u_aria",
      userId = "u_aria",
      displayName = "Aria",
      countryCode = "US",
      countryName = "United States",
      score = 1280L,
      gamesPlayed = 20L,
      gamesWon = 15L,
      capturesTotal = 45L,
      homesReachedTotal = 60L,
      claimedMissionIds = encodedClaimed,
      unlockedItems = encodedUnlocked
    )

    assertEquals(5L, profile.gamesLost)
    assertEquals(75, profile.winRatePercent)
    assertEquals("2.3", profile.capturesPerMatchAvg)
    assertEquals("Sovereign Grandmaster", profile.rankTierTitle)

    val decodedLoadout = profile.parsedEquippedEmoteLoadout()
    assertEquals("emote_3d_thunder_strike", decodedLoadout[EmoteMomentTrigger.OPPONENT_CAPTURE])
    assertEquals("emote_3d_celestial_helix", decodedLoadout[EmoteMomentTrigger.HOME_GOAL])

    val decodedHistory = profile.parsedMatchHistory()
    assertEquals(1, decodedHistory.size)
    assertEquals(com.example.data.MatchOutcomeType.WIN, decodedHistory.first().outcome)
    assertEquals(4, decodedHistory.first().captures)
    assertEquals(260, decodedHistory.first().scoreDelta)
  }

  @Test
  fun friendsSystemCatalog_resolvesPresenceAndActiveRoomsCorrectly() {
    val reqId1 = com.example.data.FriendsStudioCatalog.buildFriendRequestId("alice_1", "bob_2")
    val reqId2 = com.example.data.FriendsStudioCatalog.buildFriendRequestId("bob_2", "alice_1")
    assertEquals(reqId1, reqId2)

    val request = com.example.data.FriendRequest(
      id = reqId1,
      fromUid = "alice_1",
      fromName = "Alice",
      fromCountry = "US",
      toUid = "bob_2",
      toName = "Bob",
      toCountry = "GB",
      status = com.example.data.FriendRequestStatus.ACCEPTED.name,
      memberUids = listOf("alice_1", "bob_2")
    )

    val bobWaitingRoom = com.example.data.LudoRoom(
      id = "ROOM99",
      hostUid = "bob_2",
      roomCode = "ROOM99",
      isPublic = true,
      status = com.example.data.RoomStatus.WAITING.name,
      maxPlayers = 4L,
      memberUids = listOf("bob_2", "charlie_3")
    )

    val resolvedEntry = com.example.data.FriendsStudioCatalog.buildFriendEntry(
      request = request,
      myUid = "alice_1",
      profile = null,
      activeRooms = listOf(bobWaitingRoom)
    )

    assertEquals("bob_2", resolvedEntry.friendUid)
    assertEquals("Bob", resolvedEntry.displayName)
    assertEquals(com.example.data.FriendPresenceState.IN_WAITING_ROOM, resolvedEntry.presenceState)
    assertEquals("ROOM99", resolvedEntry.activeRoomCode)
    assertEquals(2, resolvedEntry.activeRoomPlayersCount)
  }

  @Test
  fun howToPlayTutorialCatalog_providesSixInteractive3DLessonsAndValidTransitions() {
    val lessons = com.example.data.HowToPlayTutorialCatalog.LESSONS
    assertEquals(6, lessons.size)

    // Lesson 1: Board Mechanics & 4 3D Beacons
    val lesson1 = lessons[0]
    assertEquals(com.example.data.TutorialInteractiveActionType.INSPECT_BOARD, lesson1.initialActionType)
    assertEquals(4, lesson1.beacons.size)

    // Lesson 2: Rolling Dual 3D Dice (6 & 4) & Deploying Pawn #1 from Yard (-1 -> 0)
    val lesson2 = lessons[1]
    assertEquals(com.example.data.TutorialInteractiveActionType.ROLL_DUAL_DICE, lesson2.initialActionType)
    assertEquals(6, lesson2.scriptedDie1)
    assertEquals(-1, lesson2.initialTokens[0])
    assertEquals(0, lesson2.afterActionTokens[0])

    // Lesson 3: Dual-Dice Selection (Die #2 [5] to advance from pos 3 to Safe Star pos 8)
    val lesson3 = lessons[2]
    assertEquals(com.example.data.TutorialInteractiveActionType.SELECT_DIE_SLOT, lesson3.initialActionType)
    assertEquals(1, lesson3.requiredDieSelectionIndex)
    assertEquals(5, lesson3.scriptedDie2)
    assertEquals(3, lesson3.fromRelativePos)
    assertEquals(8, lesson3.toRelativePos)
    assertEquals(8, lesson3.afterActionTokens[0])

    // Lesson 4: Tactical Capture (Red Pawn #1 captures Green Pawn #1 at globalIdx 14)
    val lesson4 = lessons[3]
    assertEquals(4, lesson4.capturedTokenIndex)
    assertEquals(1, lesson4.initialTokens[4])
    assertEquals(-1, lesson4.afterActionTokens[4])
    assertEquals(14, lesson4.afterActionTokens[0])

    // Lesson 5: Golden Blockade Shield (stacking Red Pawn #1 & #2 on pos 21)
    val lesson5 = lessons[4]
    val blockadesAfter = LudoRulesEngine.getActiveBlockades(
      tokens = lesson5.afterActionTokens,
      seats = com.example.data.HowToPlayTutorialCatalog.TUTORIAL_SEATS,
      armCount = 4
    )
    assertTrue(blockadesAfter[21]?.contains(PlayerColor.RED) == true)

    // Lesson 6: Ascending Central 3D Goal Pyramid (pos 53 -> 56)
    val lesson6 = lessons[5]
    assertEquals(53, lesson6.fromRelativePos)
    assertEquals(56, lesson6.toRelativePos)
    assertTrue((0..3).all { lesson6.afterActionTokens[it] == 56 })
  }

  @Test
  fun pawnMovementGlowTrail3D_generatesColorMatched3DSamplesAndTaperedIntensity() {
    val seats = com.example.data.HowToPlayTutorialCatalog.TUTORIAL_SEATS
    // Token index 12 belongs to BLUE (arm 3)
    val blueMovingState = com.example.ui.board3d.AnimatedTokenState(
      tokenIndex = 12,
      fromPos = 4,
      toPos = 5,
      progress = 0.65f,
      trailPathPositions = listOf(1, 2, 3, 4),
      fadingWakeAlpha = 1f
    )
    assertEquals(PlayerColor.BLUE, blueMovingState.tokenColor)

    val (primary, darkTone, softTone) = com.example.ui.board3d.paletteForColor(blueMovingState.tokenColor)
    assertTrue(primary != darkTone)
    assertTrue(primary != softTone)

    val samples = com.example.ui.board3d.LudoBoardCoordinates.buildPawnTrailSamples3D(
      animatedToken = blueMovingState,
      seats = seats,
      armCount = 4
    )
    assertTrue(samples.size >= 10)
    // Tail starts at 0f intensity and tapers smoothly upward toward the leading pawn head
    assertEquals(0f, samples.first().intensity, 0.001f)
    assertTrue(samples.last().intensity > samples[samples.size / 2].intensity)
    assertEquals(1f, samples.last().intensity, 0.001f)
    // Parabolic 3D hop lifts worldPos.z above the ground plane z (0.21f)
    assertTrue(samples.any { it.worldPos.z > it.groundPos.z + 0.35f })
  }

  @Test
  fun dayNightLightingCycle3D_syncsWithLocalTimeAndCastsDirectionalShadows() {
    val dawnLighting = com.example.ui.board3d.DayNightLightingEngine.computeLightingForFractionalHour(6.75f)
    val middayLighting = com.example.ui.board3d.DayNightLightingEngine.computeLightingForFractionalHour(13.0f)
    val duskLighting = com.example.ui.board3d.DayNightLightingEngine.computeLightingForFractionalHour(18.5f)
    val nightLighting = com.example.ui.board3d.DayNightLightingEngine.computeLightingForFractionalHour(2.0f)

    assertEquals(com.example.ui.board3d.DayNightPhase.DAWN, dawnLighting.phase)
    assertEquals(com.example.ui.board3d.DayNightPhase.MIDDAY, middayLighting.phase)
    assertEquals(com.example.ui.board3d.DayNightPhase.GOLDEN_DUSK, duskLighting.phase)
    assertEquals(com.example.ui.board3d.DayNightPhase.NIGHT, nightLighting.phase)

    // Dawn sun in the East casts westward shadows (negative shadowDirX), Dusk sun in the West casts eastward shadows (positive shadowDirX)
    assertTrue(dawnLighting.shadowDirX < -0.20f)
    assertTrue(duskLighting.shadowDirX > 0.25f)

    // Midday overhead sun casts compact shadows compared to Golden Dusk elongated shadows
    assertTrue(middayLighting.shadowStretchFactor < duskLighting.shadowStretchFactor)

    // Nocturnal lantern glow is off at Midday (0f) and strongly active during Moonlit Night (> 0.8f)
    assertEquals(0f, middayLighting.lanternGlowIntensity, 0.01f)
    assertTrue(nightLighting.lanternGlowIntensity > 0.80f)
    assertEquals("02:00", nightLighting.formattedLocalTime)
  }

  @Test
  fun spectatingSystem_resolvesActivePlayingRoomAndEnforcesViewOnlyState() {
    val request = com.example.data.FriendRequest(
      id = com.example.data.FriendsStudioCatalog.buildFriendRequestId("alice_1", "zainab_2"),
      fromUid = "alice_1",
      fromName = "Alice",
      fromCountry = "US",
      toUid = "zainab_2",
      toName = "Zainab",
      toCountry = "AE",
      status = com.example.data.FriendRequestStatus.ACCEPTED.name,
      memberUids = listOf("alice_1", "zainab_2")
    )

    val livePlayingRoom = com.example.data.LudoRoom(
      id = "SOV6P8",
      hostUid = "zainab_2",
      roomCode = "SOV6P8",
      isPublic = true,
      status = com.example.data.RoomStatus.PLAYING.name,
      maxPlayers = 6L,
      turnNumber = 14L,
      memberUids = listOf("zainab_2", "p2", "p3", "p4", "p5", "p6")
    )

    val resolvedFriend = com.example.data.FriendsStudioCatalog.buildFriendEntry(
      request = request,
      myUid = "alice_1",
      profile = null,
      activeRooms = listOf(livePlayingRoom)
    )

    assertEquals(com.example.data.FriendPresenceState.IN_ACTIVE_MATCH, resolvedFriend.presenceState)
    assertEquals("SOV6P8", resolvedFriend.activeRoomCode)
    assertEquals("PLAYING", resolvedFriend.activeRoomStatus)
    assertEquals(14, resolvedFriend.activeRoomTurnNumber)

    val spectatorUiState = com.example.ui.viewmodel.LudoUiState(
      currentScreen = com.example.ui.viewmodel.AppScreen.GAME_ARENA,
      isSpectating = true,
      spectatingTargetName = resolvedFriend.displayName,
      spectatorCount = 4,
      roomCode = "SOV6P8",
      roomStatus = com.example.data.RoomStatus.PLAYING
    )
    assertTrue(spectatorUiState.isSpectating)
    assertEquals("Zainab", spectatorUiState.spectatingTargetName)
    assertEquals(4, spectatorUiState.spectatorCount)
  }

  @Test
  fun soundEffectsManagerAndRepository_playsClipsForDiceRollPawnMovementAndGameCelebrations() {
    val repository = com.example.ui.audio.DefaultSoundEffectRepository(enableHardwareAudio = false)
    val manager = com.example.ui.audio.SoundEffectsManager(repository)

    // Verify all 7 clips exist in the repository catalog and synthesize valid non-empty PCM buffers
    val allClips = repository.getAllClips()
    assertEquals(7, allClips.size)
    allClips.forEach { clip ->
      assertTrue(clip.totalDurationMs > 0)
      val pcm = repository.getSynthesizedPcmBuffer(clip.event)
      assertTrue(pcm.isNotEmpty())
    }

    // 1. Dice rolling clip playback
    val diceClip = manager.playDiceRoll()
    assertNotNull(diceClip)
    assertEquals(com.example.ui.audio.LudoSoundEvent.DICE_ROLL, diceClip?.event)
    assertEquals(diceClip, manager.lastPlayedClip.value)

    // 2. Pawn movement clip playback
    val moveClip = manager.playPawnMove()
    assertNotNull(moveClip)
    assertEquals(com.example.ui.audio.LudoSoundEvent.MOVE, moveClip?.event)
    assertEquals(moveClip, manager.lastPlayedClip.value)

    // 3. Game-ending celebration fanfare clip playback
    val celebrationClip = manager.playGameCelebration()
    assertNotNull(celebrationClip)
    assertEquals(com.example.ui.audio.LudoSoundEvent.GAME_CELEBRATION, celebrationClip?.event)
    assertEquals(celebrationClip, manager.lastPlayedClip.value)
    assertTrue(celebrationClip!!.totalDurationMs > moveClip!!.totalDurationMs)

    assertEquals(
      listOf(
        com.example.ui.audio.LudoSoundEvent.DICE_ROLL,
        com.example.ui.audio.LudoSoundEvent.MOVE,
        com.example.ui.audio.LudoSoundEvent.GAME_CELEBRATION
      ),
      manager.playedClipsHistory.value
    )

    // Mute and unmute behavior
    val enabledAfterOff = manager.toggleSound()
    assertFalse(enabledAfterOff)
    assertEquals(null, manager.playDiceRoll())
    assertEquals(3, manager.playedClipsHistory.value.size)

    val enabledAfterOn = manager.toggleSound()
    assertTrue(enabledAfterOn)
    assertEquals(com.example.ui.audio.LudoSoundEvent.SAFE, manager.lastPlayedClip.value?.event)
  }

  @Test
  fun dailyMissionSystem_tracksWinThreeGamesAndRollASixFiveTimesInFirestoreClaimedList() {
    val missions = com.example.data.DailyMissionCatalog.MISSIONS
    assertTrue(missions.size >= 6)

    // Verify 'Win 3 games' and 'Roll a 6 five times' objectives exist with currency rewards
    val win3Mission = missions.firstOrNull { it.id == "mission_win_3" }
    assertNotNull(win3Mission)
    assertEquals(com.example.data.MissionType.WIN_MATCHES, win3Mission?.type)
    assertEquals(3, win3Mission?.targetCount)
    assertTrue((win3Mission?.rewardCoins ?: 0L) > 0L)

    val rollSix5Mission = missions.firstOrNull { it.id == "mission_roll_six_5" }
    assertNotNull(rollSix5Mission)
    assertEquals(com.example.data.MissionType.ROLL_SIXES, rollSix5Mission?.type)
    assertEquals(5, rollSix5Mission?.targetCount)
    assertTrue((rollSix5Mission?.rewardCoins ?: 0L) > 0L)

    // Verify incremental Firestore progress encoding and decoding
    var claimedList = listOf("mission_win_1")
    claimedList = com.example.data.ProfileCatalog.incrementMissionProgressInClaimedList(
      existingClaimed = claimedList,
      deltas = mapOf(
        com.example.data.MissionType.ROLL_SIXES to 3,
        com.example.data.MissionType.WIN_MATCHES to 2
      )
    )
    claimedList = com.example.data.ProfileCatalog.incrementMissionProgressInClaimedList(
      existingClaimed = claimedList,
      deltas = mapOf(
        com.example.data.MissionType.ROLL_SIXES to 2,
        com.example.data.MissionType.WIN_MATCHES to 1,
        com.example.data.MissionType.OPEN_FROM_YARD to 4
      )
    )

    val profile = com.example.data.PlayerProfile(
      userId = "u_missions",
      gamesPlayed = 4L,
      gamesWon = 3L,
      claimedMissionIds = claimedList
    )
    assertEquals(3, profile.missionProgressForType(com.example.data.MissionType.WIN_MATCHES))
    assertEquals(5, profile.missionProgressForType(com.example.data.MissionType.ROLL_SIXES))
    assertEquals(4, profile.missionProgressForType(com.example.data.MissionType.OPEN_FROM_YARD))
    assertTrue("mission_win_1" in profile.claimedMissionIds)
  }

  @Test
  fun profileStatsAndCustomAvatarSelection_computesWinLossRatioAndEncodesFirebaseAuthAvatar() {
    val avatars = com.example.data.ProfileCatalog.AVATAR_OPTIONS
    assertEquals(8, avatars.size)

    val dragonAvatar = com.example.data.ProfileCatalog.findAvatarById("avatar_emerald_dragon")
    assertEquals("ludo-avatar://avatar_emerald_dragon", dragonAvatar.authPhotoUriString)
    assertEquals(
      "avatar_emerald_dragon",
      com.example.data.ProfileCatalog.parseAvatarIdFromAuthPhotoUri(dragonAvatar.authPhotoUriString)
    )

    val unlockedWithAvatar = com.example.data.ProfileCatalog.encodeAvatarIntoUnlockedItems(
      existingUnlockedItems = listOf("emote_crown_spin"),
      avatarId = dragonAvatar.id
    )
    val profile = com.example.data.PlayerProfile(
      userId = "u_profile",
      displayName = "Grand Archon",
      gamesPlayed = 20L,
      gamesWon = 15L,
      unlockedItems = unlockedWithAvatar
    )
    assertEquals(5L, profile.gamesLost)
    assertEquals(75, profile.winRatePercent)
    assertEquals("3.00", profile.winLossRatioFormatted)
    assertEquals("avatar_emerald_dragon", profile.parsedAvatarId())
  }

  @Test
  fun ongoingPrivateRoomSpectating_resolvesPrivateRoomObserverStateAndSyncsRealTimeFirestoreRoom() {
    val privatePresets = com.example.data.FriendsStudioCatalog.ONGOING_PRIVATE_ROOMS_SHOWCASE
    assertTrue(privatePresets.isNotEmpty())
    assertTrue(privatePresets.any { it.roomCode == "PRV409" && it.maxPlayers == 4 })
    assertTrue(privatePresets.any { it.roomCode == "SOV6P8" && it.maxPlayers == 6 })

    val friendRequest = com.example.data.FriendRequest(
      id = "fr_alice_bob",
      fromUid = "alice_1",
      fromName = "Alice",
      toUid = "bob_2",
      toName = "Bob",
      status = com.example.data.FriendRequestStatus.ACCEPTED.name,
      memberUids = listOf("alice_1", "bob_2")
    )
    val ongoingPrivateRoom = com.example.data.LudoRoom(
      id = "PRV409",
      hostUid = "bob_2",
      roomCode = "PRV409",
      isPublic = false,
      status = com.example.data.RoomStatus.PLAYING.name,
      maxPlayers = 4L,
      memberUids = listOf("bob_2", "charlie_3"),
      playerNames = listOf("Bob", "Charlie"),
      playerColors = listOf("RED", "GREEN"),
      playerUids = listOf("bob_2", "charlie_3"),
      tokens = List(32) { idx -> if (idx == 0) 8L else if (idx == 4) 14L else -1L },
      currentTurnColor = "GREEN",
      turnPhase = com.example.data.TurnPhase.MOVE.name,
      diceValue = 6L,
      diceValue2 = 4L,
      selectedDieIndex = 0L,
      turnNumber = 9L,
      lastEvent = "Charlie rolled [6, 4]"
    )

    val entry = com.example.data.FriendsStudioCatalog.buildFriendEntry(
      request = friendRequest,
      myUid = "alice_1",
      profile = null,
      activeRooms = listOf(ongoingPrivateRoom)
    )
    assertEquals(com.example.data.FriendPresenceState.IN_ACTIVE_MATCH, entry.presenceState)
    assertEquals("PRV409", entry.activeRoomCode)
    assertFalse(entry.activeRoomIsPublic)
    assertTrue(entry.statusSubtitle.contains("Private Room #PRV409"))
    assertEquals(8, ongoingPrivateRoom.tokenPositions[0])
    assertEquals(14, ongoingPrivateRoom.tokenPositions[4])
    assertEquals(2, ongoingPrivateRoom.seats.size)
  }
}
