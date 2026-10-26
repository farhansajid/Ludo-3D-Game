package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LudoMultiplayerRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  suspend fun ensureOrUpdateProfile(displayName: String): Result<PlayerProfile> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      val safeName = displayName.trim().ifBlank { "Royal Player" }.take(60)
      if (!snapshot.exists()) {
        val payload = mapOf(
          "userId" to uid,
          "displayName" to safeName,
          "gamesPlayed" to 0L,
          "gamesWon" to 0L,
          "capturesTotal" to 0L,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
      } else if (safeName.isNotBlank() && snapshot.getString("displayName") != safeName) {
        docRef.update(
          mapOf(
            "displayName" to safeName,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      val updated = docRef.get().await()
        .toObject(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: PlayerProfile(id = uid, userId = uid, displayName = safeName)
      Result.success(updated)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getProfile(userId: String): Result<PlayerProfile> {
    val docRef = db.collection("users").document(userId)
    return try {
      val snapshot = docRef.get().await()
      val profile = snapshot.toObject(
        PlayerProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: throw NoSuchElementException("Profile not found for $userId")
      Result.success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  fun observeProfile(userId: String): Flow<PlayerProfile?> {
    val docRef = db.collection("users").document(userId)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(
            PlayerProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
        throw error
      }
  }

  suspend fun recordMatchStats(won: Boolean, capturesInMatch: Int): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        ensureOrUpdateProfile(auth.currentUser?.displayName ?: "Royal Player")
      }
      val updates = mutableMapOf<String, Any>(
        "gamesPlayed" to FieldValue.increment(1L),
        "capturesTotal" to FieldValue.increment(capturesInMatch.coerceAtLeast(0).toLong()),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (won) {
        updates["gamesWon"] = FieldValue.increment(1L)
      }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createRoom(
    roomCode: String,
    hostName: String,
    isPublic: Boolean,
    maxPlayers: Int = 4,
    initialStatus: RoomStatus = RoomStatus.WAITING
  ): Result<LudoRoom> {
    val uid = requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      val safeName = hostName.trim().ifBlank { "Host Player" }.take(60)
      val payload = mapOf(
        "hostUid" to uid,
        "roomCode" to normalizedCode,
        "isPublic" to isPublic,
        "status" to initialStatus.name,
        "maxPlayers" to maxPlayers.coerceIn(2, 4).toLong(),
        "memberUids" to listOf(uid),
        "playerNames" to listOf(safeName),
        "playerColors" to listOf(PlayerColor.RED.name),
        "playerUids" to listOf(uid),
        "tokens" to List(16) { -1L },
        "currentTurnColor" to PlayerColor.RED.name,
        "turnPhase" to TurnPhase.ROLL.name,
        "diceValue" to 0L,
        "consecutiveSixes" to 0L,
        "turnNumber" to 1L,
        "lastEvent" to "$safeName created room $normalizedCode",
        "reactionEmoji" to "",
        "reactionSender" to "",
        "winnerColor" to "NONE",
        "winnerOrder" to emptyList<String>(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      val created = docRef.get().await()
        .toObject(LudoRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: LudoRoom(id = normalizedCode, hostUid = uid, roomCode = normalizedCode)
      Result.success(created)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getRoomByCode(roomCode: String): Result<LudoRoom> {
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        throw NoSuchElementException("Room $normalizedCode not found")
      }
      val room = snapshot.toObject(
        LudoRoom::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: throw NoSuchElementException("Unable to read room $normalizedCode")
      Result.success(room)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun joinRoomByCode(roomCode: String, playerName: String): Result<LudoRoom> {
    val uid = requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        return Result.failure(IllegalArgumentException("Room $normalizedCode does not exist."))
      }
      val room = snapshot.toObject(
        LudoRoom::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: return Result.failure(IllegalStateException("Invalid room data."))

      if (uid in room.memberUids) {
        return Result.success(room)
      }
      if (room.status != RoomStatus.WAITING.name) {
        return Result.failure(IllegalStateException("Room $normalizedCode has already started."))
      }
      if (room.memberUids.size >= room.maxPlayers.toInt()) {
        return Result.failure(IllegalStateException("Room $normalizedCode is full."))
      }

      val colorAssignmentOrder = if (room.maxPlayers.toInt() == 2) {
        listOf(PlayerColor.RED.name, PlayerColor.YELLOW.name)
      } else {
        listOf(PlayerColor.RED.name, PlayerColor.GREEN.name, PlayerColor.YELLOW.name, PlayerColor.BLUE.name)
      }
      val nextColor = colorAssignmentOrder.firstOrNull { it !in room.playerColors }
        ?: PlayerColor.YELLOW.name
      val safeName = playerName.trim().ifBlank { "Challenger" }.take(60)

      val updatedMembers = room.memberUids + uid
      val updatedNames = room.playerNames + safeName
      val updatedColors = room.playerColors + nextColor
      val updatedUids = room.playerUids + uid
      val newStatus = if (updatedMembers.size >= room.maxPlayers.toInt()) {
        RoomStatus.PLAYING.name
      } else {
        RoomStatus.WAITING.name
      }

      docRef.update(
        mapOf(
          "memberUids" to updatedMembers,
          "playerNames" to updatedNames,
          "playerColors" to updatedColors,
          "playerUids" to updatedUids,
          "status" to newStatus,
          "lastEvent" to "$safeName joined as ${PlayerColor.fromString(nextColor).label}",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()

      val updated = docRef.get().await()
        .toObject(LudoRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: room
      Result.success(updated)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun findOrCreateQuickMatch(playerName: String, maxPlayers: Int = 2): Result<LudoRoom> {
    val uid = requireUserId()
    return try {
      val querySnapshot = db.collection("rooms")
        .whereEqualTo("isPublic", true)
        .whereEqualTo("status", RoomStatus.WAITING.name)
        .limit(10)
        .get()
        .await()

      val openRoom = querySnapshot.toObjects(
        LudoRoom::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ).firstOrNull { room ->
        uid in room.memberUids || room.memberUids.size < room.maxPlayers.toInt()
      }

      if (openRoom != null) {
        joinRoomByCode(openRoom.roomCode, playerName)
      } else {
        val generatedCode = generateRoomCode()
        createRoom(
          roomCode = generatedCode,
          hostName = playerName,
          isPublic = true,
          maxPlayers = maxPlayers
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "rooms")
      Result.failure(e)
    }
  }

  suspend fun startMatchWithOptionalBots(roomCode: String, fillWithBots: Boolean): Result<Unit> {
    requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      val snapshot = docRef.get().await()
      val room = snapshot.toObject(
        LudoRoom::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: return Result.failure(IllegalStateException("Room not found"))

      val updatedNames = room.playerNames.toMutableList()
      val updatedColors = room.playerColors.toMutableList()
      val updatedUids = room.playerUids.toMutableList()

      val targetSeats = room.maxPlayers.toInt().coerceIn(2, 4)
      val colorPool = if (targetSeats == 2) {
        listOf(PlayerColor.RED.name, PlayerColor.YELLOW.name)
      } else {
        listOf(PlayerColor.RED.name, PlayerColor.GREEN.name, PlayerColor.YELLOW.name, PlayerColor.BLUE.name)
      }

      if (fillWithBots || updatedColors.size < 2) {
        val botNames = listOf("Atelier Bot", "Tactical AI", "Archon AI")
        var botIndex = 1
        for (color in colorPool) {
          if (updatedColors.size >= targetSeats) break
          if (color !in updatedColors) {
            updatedColors.add(color)
            updatedNames.add(botNames.getOrElse(botIndex - 1) { "Bot $botIndex" })
            updatedUids.add("BOT_$botIndex")
            botIndex++
          }
        }
      }

      docRef.update(
        mapOf(
          "status" to RoomStatus.PLAYING.name,
          "playerNames" to updatedNames,
          "playerColors" to updatedColors,
          "playerUids" to updatedUids,
          "lastEvent" to "Match started! ${PlayerColor.fromString(room.currentTurnColor).label}'s turn to roll.",
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun pushRoomGameState(
    roomCode: String,
    status: RoomStatus,
    tokens: List<Int>,
    currentTurnColor: PlayerColor,
    turnPhase: TurnPhase,
    diceValue: Int,
    consecutiveSixes: Int,
    turnNumber: Int,
    lastEvent: String,
    winnerColor: String,
    winnerOrder: List<String>
  ): Result<Unit> {
    requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      docRef.update(
        mapOf(
          "status" to status.name,
          "tokens" to tokens.map { it.toLong() },
          "currentTurnColor" to currentTurnColor.name,
          "turnPhase" to turnPhase.name,
          "diceValue" to diceValue.toLong(),
          "consecutiveSixes" to consecutiveSixes.toLong(),
          "turnNumber" to turnNumber.toLong(),
          "lastEvent" to lastEvent.take(160),
          "winnerColor" to winnerColor,
          "winnerOrder" to winnerOrder,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun sendEmojiReaction(roomCode: String, emoji: String, senderName: String): Result<Unit> {
    requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      docRef.update(
        mapOf(
          "reactionEmoji" to emoji.take(16),
          "reactionSender" to senderName.take(60),
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeRoom(roomCode: String): Flow<LudoRoom?> {
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(
            LudoRoom::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          )
        } else {
          null
        }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
        throw error
      }
  }

  fun observeUserRooms(userId: String): Flow<List<LudoRoom>> {
    val path = "rooms"
    return db.collection(path)
      .whereArrayContains("memberUids", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          LudoRoom::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeOpenPublicRooms(): Flow<List<LudoRoom>> {
    val path = "rooms"
    return db.collection(path)
      .whereEqualTo("isPublic", true)
      .whereEqualTo("status", RoomStatus.WAITING.name)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          LudoRoom::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  companion object {
    private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generateRoomCode(length: Int = 6): String {
      return (1..length)
        .map { CODE_CHARS.random() }
        .joinToString("")
    }
  }
}
