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
  fun createAndGetProfile_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = LudoMultiplayerRepository(firestore)

    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.ensureOrUpdateProfile("Alice Royal")
    }
    assertTrue("Expected profile creation to succeed: ${result.exceptionOrNull()}", result.isSuccess)
    val profile = result.getOrThrow()
    assertEquals(uid, profile.userId)
    assertEquals("Alice Royal", profile.displayName)
  }

  @Test
  fun getProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = LudoMultiplayerRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.ensureOrUpdateProfile("Alice").getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = LudoMultiplayerRepository(firestore)
    val bobReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getProfile(aliceUid)
    }
    assertTrue("Expected cross-user profile read to fail", bobReadResult.isFailure)
    val fsException = bobReadResult.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsException?.code)
  }

  @Test
  fun createAndObserveRoom_authenticatedMember_emitsRealtimeUpdates() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = LudoMultiplayerRepository(firestore)
    val dynamicCode = "RM" + UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createRoom(
        roomCode = dynamicCode,
        hostName = "Alice",
        isPublic = true,
        maxPlayers = 4
      )
    }
    assertTrue("Expected room creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)

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
        maxPlayers = 2,
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

  private companion object {
    const val ALICE_EMAIL = "alice@ludo.test"
    const val BOB_EMAIL = "bob@ludo.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
