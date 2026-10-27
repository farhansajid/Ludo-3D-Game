package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LudoRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createProfile_claimDailyMissionAndLeaderboards_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = LudoMultiplayerRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.ensureOrUpdateProfile("Alice Royal", preferredCountryCode = "US")
    }
    assertTrue("Expected profile creation to succeed: ${result.exceptionOrNull()}", result.isSuccess)
    val profile = result.getOrThrow()
    assertEquals(uid, profile.userId)
    assertEquals("Alice Royal", profile.displayName)
    assertEquals("US", profile.countryCode)
    assertEquals(250L, profile.coins)

    // Claim a Daily Challenge Mission (+150 Coins, +120 XP)
    val claimResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.claimDailyMissionReward("mission_win_1", coinReward = 150L, xpReward = 120L)
    }
    assertTrue("Expected daily mission claim to succeed: ${claimResult.exceptionOrNull()}", claimResult.isSuccess)

    // Record a win with 2 captures and 4 home pawns (+100 + 50 + 60 = +210 pts)
    val statResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.recordMatchStats(won = true, capturesInMatch = 2, homesReachedInMatch = 4)
    }
    assertTrue("Expected recordMatchStats to succeed", statResult.isSuccess)

    // Save Admin App Config
    val adminResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.saveAdminConfig(
        AdminAppConfig(
          updatedByUid = uid,
          missionRewardMultiplier = 2L,
          announcementBanner = "Royal Tournament Live!"
        )
      )
    }
    assertTrue("Expected saveAdminConfig to succeed: ${adminResult.exceptionOrNull()}", adminResult.isSuccess)

    // Verify Global Leaderboard emits Alice
    val globalLeaders = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeGlobalLeaderboard().first { list -> list.any { it.userId == uid } }
    }
    val aliceRank = globalLeaders.first { it.userId == uid }
    assertEquals(310L, aliceRank.score)
    assertTrue(aliceRank.claimedMissionIds.contains("mission_win_1"))

    // Verify Country Leaderboard for US emits Alice
    val countryLeaders = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeCountryLeaderboard("US").first { list -> list.any { it.userId == uid } }
    }
    assertTrue(countryLeaders.any { it.userId == uid })
  }

  @Test
  fun create8PlayerRoom_selectColorAndVoiceSignal_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = LudoMultiplayerRepository(firestore)
    val dynamicCode = "RM" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createRoom(
        roomCode = dynamicCode,
        hostName = "Alice",
        isPublic = true,
        maxPlayers = 8,
        hostColor = PlayerColor.PURPLE
      )
    }
    assertTrue("Expected 8P room creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

    // Change color in waiting room
    val colorChange = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.changePlayerColorInRoom(dynamicCode, PlayerColor.TEAL)
    }
    assertTrue("Expected color change to succeed: ${colorChange.exceptionOrNull()}", colorChange.isSuccess)

    // Send voice signal inside room
    val voiceSignal = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.sendVoiceSignal(
        roomCode = dynamicCode,
        senderName = "Alice",
        targetUid = "ALL",
        signalType = "AUDIO_FRAME",
        payload = "AAECAw=="
      )
    }
    assertTrue("Expected voice signal to succeed: ${voiceSignal.exceptionOrNull()}", voiceSignal.isSuccess)

    val emittedRooms = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeUserRooms(aliceUid).first { rooms ->
        rooms.any { it.roomCode == dynamicCode }
      }
    }
    assertTrue(emittedRooms.any { it.roomCode == dynamicCode })
  }

  @Test
  fun getActivePlayingRoom_nonMemberAccess_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = LudoMultiplayerRepository(firestore)
    val dynamicCode = "PL" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createRoom(
        roomCode = dynamicCode,
        hostName = "Alice",
        isPublic = false,
        maxPlayers = 6,
        initialStatus = RoomStatus.PLAYING
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = LudoMultiplayerRepository(firestore)
    val bobRead = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getRoomByCode(dynamicCode)
    }
    assertTrue("Expected non-member read on PLAYING room to fail", bobRead.isFailure)
    val fsException = bobRead.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
  }

  @Test
  fun spectatePublicPlayingRoom_andSendEmojiReaction_succeedsWhileGameplayMutationFails() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = LudoMultiplayerRepository(firestore)
    val dynamicCode = "SP" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createRoom(
        roomCode = dynamicCode,
        hostName = "Alice",
        isPublic = true,
        maxPlayers = 4,
        initialStatus = RoomStatus.PLAYING
      ).getOrThrow()
    }

    // Bob (non-member friend) spectates the public PLAYING room in view-only mode
    signInTestUser(BOB_EMAIL)
    val bobRepo = LudoMultiplayerRepository(firestore)
    val spectateRead = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getRoomByCode(dynamicCode)
    }
    assertTrue("Expected spectator read on public PLAYING room to succeed: ${spectateRead.exceptionOrNull()}", spectateRead.isSuccess)

    // Bob can send an in-game emoji reaction while spectating
    val reactionResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.sendEmojiReaction(dynamicCode, "👏", "Bob (Spectator)")
    }
    assertTrue("Expected spectator emoji reaction to succeed: ${reactionResult.exceptionOrNull()}", reactionResult.isSuccess)

    // Bob cannot interfere with gameplay state (e.g. pushing tokens or dice)
    val illegalGameMutation = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.pushRoomGameState(
        roomCode = dynamicCode,
        status = RoomStatus.PLAYING,
        tokens = List(32) { 0 },
        currentTurnColor = PlayerColor.RED,
        turnPhase = TurnPhase.MOVE,
        diceValue = 6,
        diceValue2 = 6,
        dice1Used = false,
        dice2Used = false,
        selectedDieIndex = 0,
        consecutiveSixes = 1,
        turnNumber = 2,
        lastEvent = "Spectator trying to move tokens",
        winnerColor = "NONE",
        winnerOrder = emptyList()
      )
    }
    assertTrue("Expected spectator gameplay mutation to fail", illegalGameMutation.isFailure)
    val fsException = illegalGameMutation.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
  }

  @Test
  fun observeUserRooms_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = LudoMultiplayerRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserRooms("unauth_uid").first()
      }
      fail("Expected unauthenticated observeUserRooms to throw Exception")
    } catch (e: Exception) {
      val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }
  }

  @Test
  fun sendAndAcceptFriendRequest_andSearchPlayersByUsername_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = LudoMultiplayerRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureOrUpdateProfile("Alice Sovereign", preferredCountryCode = "US").getOrThrow()
    }

    val bobUid = signInTestUser(BOB_EMAIL)
    val bobRepo = LudoMultiplayerRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.ensureOrUpdateProfile("Bob Archon", preferredCountryCode = "GB").getOrThrow()
    }

    // Bob searches for "Alice" by username
    val searchResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.searchPlayersByUsername("Alice")
    }
    assertTrue("Expected player search to succeed: ${searchResult.exceptionOrNull()}", searchResult.isSuccess)
    assertTrue("Expected Alice in search results", searchResult.getOrThrow().any { it.userId == aliceUid })

    // Bob sends friend request to Alice
    val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.sendFriendRequest(
        targetUserId = aliceUid,
        targetDisplayName = "Alice Sovereign",
        targetCountryCode = "US",
        senderName = "Bob Archon",
        senderCountryCode = "GB"
      )
    }
    assertTrue("Expected sendFriendRequest to succeed: ${sendResult.exceptionOrNull()}", sendResult.isSuccess)
    val request = sendResult.getOrThrow()
    assertEquals(FriendRequestStatus.PENDING.name, request.status)

    // Switch back to Alice to observe and accept the incoming request
    signInTestUser(ALICE_EMAIL)
    val incomingRequests = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeFriendRequests(aliceUid).first { list -> list.any { it.id == request.id } }
    }
    assertTrue(incomingRequests.any { it.fromUid == bobUid && it.status == FriendRequestStatus.PENDING.name })

    val acceptResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.respondToFriendRequest(request.id, accept = true)
    }
    assertTrue("Expected respondToFriendRequest to succeed: ${acceptResult.exceptionOrNull()}", acceptResult.isSuccess)

    val acceptedList = withTimeout(FLOW_TIMEOUT_MS) {
      aliceRepo.observeFriendRequests(aliceUid).first { list ->
        list.any { it.id == request.id && it.status == FriendRequestStatus.ACCEPTED.name }
      }
    }
    assertTrue(acceptedList.any { it.id == request.id && it.status == FriendRequestStatus.ACCEPTED.name })

    val removeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.removeFriendOrRequest(request.id)
    }
    assertTrue("Expected removeFriendOrRequest to succeed: ${removeResult.exceptionOrNull()}", removeResult.isSuccess)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@ludo.test"
    const val BOB_EMAIL = "bob@ludo.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
