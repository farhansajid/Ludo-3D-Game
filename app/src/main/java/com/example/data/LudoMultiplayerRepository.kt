package com.example.data

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.domain.LudoRulesEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import java.util.UUID
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

  fun currentAuthDisplayName(): String? = auth.currentUser?.displayName

  fun currentAuthPhotoUriString(): String? = auth.currentUser?.photoUrl?.toString()

  fun currentAuthAvatarId(): String {
    return ProfileCatalog.parseAvatarIdFromAuthPhotoUri(currentAuthPhotoUriString())
      ?: ProfileCatalog.DEFAULT_AVATAR_ID
  }

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  suspend fun ensureOrUpdateProfile(
    displayName: String,
    preferredCountryCode: String? = null,
    avatarId: String? = null
  ): Result<PlayerProfile> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      val safeName = displayName.trim().ifBlank {
        auth.currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "Royal Player"
      }.take(60)
      val detectedCountry = if (!preferredCountryCode.isNullOrBlank()) {
        CountryCatalog.fromCode(preferredCountryCode)
      } else {
        CountryCatalog.detectDefaultCountry()
      }

      // Sync DisplayName and Custom Avatar photoUri into Firebase Auth user profile
      val targetAvatarId = avatarId?.takeIf { it.isNotBlank() }
        ?: ProfileCatalog.parseAvatarIdFromAuthPhotoUri(auth.currentUser?.photoUrl?.toString())
      val targetAvatarOption = targetAvatarId?.let { ProfileCatalog.findAvatarById(it) }

      runCatching {
        val requestBuilder = UserProfileChangeRequest.Builder()
          .setDisplayName(safeName)
        if (targetAvatarOption != null) {
          requestBuilder.setPhotoUri(Uri.parse(targetAvatarOption.authPhotoUriString))
        }
        auth.currentUser?.updateProfile(requestBuilder.build())?.await()
      }

      if (!snapshot.exists()) {
        val initialUnlocked = if (targetAvatarOption != null) {
          ProfileCatalog.encodeAvatarIntoUnlockedItems(emptyList(), targetAvatarOption.id)
        } else {
          emptyList()
        }
        val payload = mapOf(
          "userId" to uid,
          "displayName" to safeName,
          "countryCode" to detectedCountry.code,
          "countryName" to detectedCountry.name.take(60),
          "score" to 100L, // Starting rank points
          "gamesPlayed" to 0L,
          "gamesWon" to 0L,
          "capturesTotal" to 0L,
          "homesReachedTotal" to 0L,
          "coins" to 250L,
          "xp" to 0L,
          "claimedMissionIds" to emptyList<String>(),
          "unlockedItems" to initialUnlocked,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
      } else {
        val updates = mutableMapOf<String, Any>()
        if (safeName.isNotBlank() && snapshot.getString("displayName") != safeName) {
          updates["displayName"] = safeName
        }
        if (!preferredCountryCode.isNullOrBlank() && snapshot.getString("countryCode") != detectedCountry.code) {
          updates["countryCode"] = detectedCountry.code
          updates["countryName"] = detectedCountry.name.take(60)
        }
        if (snapshot.getLong("coins") == null) updates["coins"] = 250L
        if (snapshot.getLong("xp") == null) updates["xp"] = 0L
        if (snapshot.get("claimedMissionIds") == null) updates["claimedMissionIds"] = emptyList<String>()
        @Suppress("UNCHECKED_CAST")
        val existingUnlocked = (snapshot.get("unlockedItems") as? List<*>)
          ?.filterIsInstance<String>()
          ?: emptyList()
        if (snapshot.get("unlockedItems") == null) {
          updates["unlockedItems"] = emptyList<String>()
        }
        if (targetAvatarOption != null) {
          val updatedUnlocked = ProfileCatalog.encodeAvatarIntoUnlockedItems(existingUnlocked, targetAvatarOption.id)
          if (updatedUnlocked != existingUnlocked) {
            updates["unlockedItems"] = updatedUnlocked
          }
        }
        if (updates.isNotEmpty()) {
          updates["updatedAt"] = FieldValue.serverTimestamp()
          docRef.update(updates).await()
        }
      }
      val updated = docRef.get().await()
        .toObject(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: PlayerProfile(
          id = uid,
          userId = uid,
          displayName = safeName,
          countryCode = detectedCountry.code,
          countryName = detectedCountry.name,
          score = 100L,
          coins = 250L,
          xp = 0L
        )
      Result.success(updated)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateUserAvatar(avatarId: String): Result<CustomAvatarOption> {
    val avatarOption = ProfileCatalog.findAvatarById(avatarId)
    // 1. Update Firebase Auth user profile photoUri
    runCatching {
      val profileUpdates = UserProfileChangeRequest.Builder()
        .setPhotoUri(Uri.parse(avatarOption.authPhotoUriString))
        .build()
      auth.currentUser?.updateProfile(profileUpdates)?.await()
    }

    // 2. Sync custom avatar into Firestore /users/{uid} if authenticated
    val uid = auth.currentUser?.uid ?: return Result.success(avatarOption)
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        ensureOrUpdateProfile(
          displayName = auth.currentUser?.displayName ?: "Royal Player",
          avatarId = avatarOption.id
        )
      } else {
        @Suppress("UNCHECKED_CAST")
        val existingUnlocked = (snapshot.get("unlockedItems") as? List<*>)
          ?.filterIsInstance<String>()
          ?: emptyList()
        val updatedUnlocked = ProfileCatalog.encodeAvatarIntoUnlockedItems(existingUnlocked, avatarOption.id)
        docRef.update(
          mapOf(
            "unlockedItems" to updatedUnlocked,
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(avatarOption)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateUserCountry(countryCode: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val info = CountryCatalog.fromCode(countryCode)
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        ensureOrUpdateProfile(
          displayName = auth.currentUser?.displayName ?: "Royal Player",
          preferredCountryCode = info.code
        )
      } else {
        docRef.update(
          mapOf(
            "countryCode" to info.code,
            "countryName" to info.name.take(60),
            "updatedAt" to FieldValue.serverTimestamp()
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
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

  fun observeGlobalLeaderboard(limit: Long = 50L): Flow<List<PlayerProfile>> {
    val path = "users"
    return db.collection(path)
      .orderBy("score", Query.Direction.DESCENDING)
      .limit(limit)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          PlayerProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).sortedWith(
          compareByDescending<PlayerProfile> { it.score }
            .thenByDescending { it.gamesWon }
            .thenByDescending { it.capturesTotal }
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeCountryLeaderboard(countryCode: String, limit: Long = 50L): Flow<List<PlayerProfile>> {
    val path = "users"
    val cleanCode = countryCode.trim().uppercase().ifBlank { "US" }
    return db.collection(path)
      .whereEqualTo("countryCode", cleanCode)
      .limit(limit)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          PlayerProfile::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).sortedWith(
          compareByDescending<PlayerProfile> { it.score }
            .thenByDescending { it.gamesWon }
            .thenByDescending { it.capturesTotal }
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  suspend fun recordMatchStats(
    won: Boolean,
    capturesInMatch: Int,
    homesReachedInMatch: Int = 0,
    forfeited: Boolean = false,
    modeLabel: String = "3D Studio Match"
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      var snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        ensureOrUpdateProfile(auth.currentUser?.displayName ?: "Royal Player")
        snapshot = docRef.get().await()
      }
      val profile = snapshot.toObject(
        PlayerProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: PlayerProfile(id = uid, userId = uid)

      val existingScore = snapshot.getLong("score") ?: 100L
      val existingPlayed = snapshot.getLong("gamesPlayed") ?: 0L
      val existingWon = snapshot.getLong("gamesWon") ?: 0L
      val existingCaptures = snapshot.getLong("capturesTotal") ?: 0L
      val existingHomes = snapshot.getLong("homesReachedTotal") ?: 0L
      val existingCoins = snapshot.getLong("coins") ?: 250L
      val existingXp = snapshot.getLong("xp") ?: 0L

      val delta = LudoRulesEngine.calculateMatchScoreDelta(
        won = won,
        forfeited = forfeited,
        capturesInMatch = capturesInMatch,
        homesReachedInMatch = homesReachedInMatch
      )
      val newScore = (existingScore + delta.toLong()).coerceIn(0L, 100000000L)
      val matchCoinsEarned = (if (won && !forfeited) 75L else 15L) +
        (capturesInMatch.coerceAtLeast(0) * 10L) +
        (homesReachedInMatch.coerceAtLeast(0) * 10L)
      val matchXpEarned = (if (won && !forfeited) 60L else 20L) +
        (capturesInMatch.coerceAtLeast(0) * 15L) +
        (homesReachedInMatch.coerceAtLeast(0) * 10L)

      val outcomeType = when {
        forfeited -> MatchOutcomeType.FORFEIT
        won -> MatchOutcomeType.WIN
        else -> MatchOutcomeType.LOSS
      }
      val historyEntry = ProfileMatchHistoryEntry(
        id = "hist_${existingPlayed + 1L}",
        outcome = outcomeType,
        modeLabel = modeLabel,
        captures = capturesInMatch.coerceAtLeast(0),
        homesReached = homesReachedInMatch.coerceAtLeast(0),
        scoreDelta = delta
      )
      val updatedClaimedWithHistory = ProfileCatalog.encodeMatchHistoryIntoClaimedList(
        existingClaimed = profile.claimedMissionIds,
        entry = historyEntry
      )
      val missionDeltas = buildMap<MissionType, Int> {
        if (won && !forfeited) put(MissionType.WIN_MATCHES, 1)
        if (capturesInMatch > 0) put(MissionType.CAPTURE_PAWNS, capturesInMatch)
        if (homesReachedInMatch > 0) put(MissionType.REACH_HOME, homesReachedInMatch)
      }
      val updatedClaimedWithHistoryAndMissions = ProfileCatalog.incrementMissionProgressInClaimedList(
        existingClaimed = updatedClaimedWithHistory,
        deltas = missionDeltas
      )

      val updates = mutableMapOf<String, Any>(
        "score" to newScore,
        "gamesPlayed" to (existingPlayed + 1L),
        "capturesTotal" to (existingCaptures + capturesInMatch.coerceAtLeast(0).toLong()),
        "homesReachedTotal" to (existingHomes + homesReachedInMatch.coerceAtLeast(0).toLong()),
        "coins" to (existingCoins + matchCoinsEarned).coerceIn(0L, 100000000L),
        "xp" to (existingXp + matchXpEarned).coerceIn(0L, 100000000L),
        "claimedMissionIds" to updatedClaimedWithHistoryAndMissions,
        "updatedAt" to FieldValue.serverTimestamp()
      )
      if (won && !forfeited) {
        updates["gamesWon"] = existingWon + 1L
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

  suspend fun recordDailyMissionProgress(
    deltas: Map<MissionType, Int>
  ): Result<Map<MissionType, Int>> {
    val nonZeroDeltas = deltas.filterValues { it > 0 }
    if (nonZeroDeltas.isEmpty()) return Result.success(emptyMap())
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      var snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        ensureOrUpdateProfile(auth.currentUser?.displayName ?: "Royal Player")
        snapshot = docRef.get().await()
      }
      val profile = snapshot.toObject(
        PlayerProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: PlayerProfile(id = uid, userId = uid)

      val updatedClaimed = ProfileCatalog.incrementMissionProgressInClaimedList(
        existingClaimed = profile.claimedMissionIds,
        deltas = nonZeroDeltas
      )
      docRef.update(
        mapOf(
          "claimedMissionIds" to updatedClaimed,
          "updatedAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(ProfileCatalog.decodeMissionProgressFromClaimedList(updatedClaimed))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun claimDailyMissionReward(
    missionId: String,
    coinReward: Long,
    xpReward: Long
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      var snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        ensureOrUpdateProfile(auth.currentUser?.displayName ?: "Royal Player")
        snapshot = docRef.get().await()
      }
      val profile = snapshot.toObject(
        PlayerProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: PlayerProfile(id = uid, userId = uid)

      if (missionId in profile.claimedMissionIds) {
        return Result.failure(IllegalStateException("Mission reward already claimed today."))
      }

      val updatedClaimed = (profile.claimedMissionIds + missionId).distinct().take(80)
      val updatedCoins = (profile.coins + coinReward.coerceAtLeast(0L)).coerceIn(0L, 100000000L)
      val updatedXp = (profile.xp + xpReward.coerceAtLeast(0L)).coerceIn(0L, 100000000L)

      docRef.update(
        mapOf(
          "coins" to updatedCoins,
          "xp" to updatedXp,
          "claimedMissionIds" to updatedClaimed,
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

  suspend fun purchaseIapCoinBundle(
    bundleId: String,
    coinsToAdd: Long,
    bonusXp: Long
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      var snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        ensureOrUpdateProfile(auth.currentUser?.displayName ?: "Royal Player")
        snapshot = docRef.get().await()
      }
      val profile = snapshot.toObject(
        PlayerProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: PlayerProfile(id = uid, userId = uid)

      val updatedCoins = (profile.coins + coinsToAdd.coerceAtLeast(0L)).coerceIn(0L, 100000000L)
      val updatedXp = (profile.xp + bonusXp.coerceAtLeast(0L)).coerceIn(0L, 100000000L)
      val updatedUnlocked = (profile.unlockedItems + bundleId).distinct().take(48)

      docRef.update(
        mapOf(
          "coins" to updatedCoins,
          "xp" to updatedXp,
          "unlockedItems" to updatedUnlocked,
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

  suspend fun unlockStoreItemWithCoins(
    itemId: String,
    coinCost: Long,
    bonusXp: Long
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      var snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        ensureOrUpdateProfile(auth.currentUser?.displayName ?: "Royal Player")
        snapshot = docRef.get().await()
      }
      val profile = snapshot.toObject(
        PlayerProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: PlayerProfile(id = uid, userId = uid)

      if (itemId in profile.unlockedItems) {
        return Result.success(Unit)
      }
      if (profile.coins < coinCost) {
        return Result.failure(IllegalArgumentException("Not enough Royal Coins (need $coinCost)."))
      }

      val updatedCoins = (profile.coins - coinCost).coerceIn(0L, 100000000L)
      val updatedXp = (profile.xp + bonusXp.coerceAtLeast(0L)).coerceIn(0L, 100000000L)
      val updatedUnlocked = (profile.unlockedItems + itemId).distinct().take(48)

      docRef.update(
        mapOf(
          "coins" to updatedCoins,
          "xp" to updatedXp,
          "unlockedItems" to updatedUnlocked,
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

  suspend fun saveEquippedEmoteLoadout(
    loadout: Map<EmoteMomentTrigger, String>,
    newlyUnlockedEmoteId: String? = null,
    coinCost: Long = 0L,
    bonusXp: Long = 0L
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      var snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        ensureOrUpdateProfile(auth.currentUser?.displayName ?: "Royal Player")
        snapshot = docRef.get().await()
      }
      val profile = snapshot.toObject(
        PlayerProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: PlayerProfile(id = uid, userId = uid)

      val isNewUnlock = !newlyUnlockedEmoteId.isNullOrBlank() && newlyUnlockedEmoteId !in profile.unlockedItems
      if (isNewUnlock && profile.coins < coinCost) {
        return Result.failure(IllegalArgumentException("Not enough Royal Coins (need $coinCost)."))
      }

      val updatedCoins = if (isNewUnlock) {
        (profile.coins - coinCost.coerceAtLeast(0L)).coerceIn(0L, 100000000L)
      } else {
        profile.coins
      }
      val updatedXp = if (isNewUnlock) {
        (profile.xp + bonusXp.coerceAtLeast(0L)).coerceIn(0L, 100000000L)
      } else {
        profile.xp
      }
      val updatedUnlocked = ProfileCatalog.encodeEquippedLoadoutIntoUnlockedItems(
        existingUnlockedItems = profile.unlockedItems,
        loadout = loadout,
        additionalUnlockedItemId = newlyUnlockedEmoteId
      )

      docRef.update(
        mapOf(
          "coins" to updatedCoins,
          "xp" to updatedXp,
          "unlockedItems" to updatedUnlocked,
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

  fun observeAdminConfig(): Flow<AdminAppConfig?> {
    val docRef = db.collection("appConfig").document("global")
    return docRef.snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(
            AdminAppConfig::class.java,
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

  suspend fun saveAdminConfig(config: AdminAppConfig): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("appConfig").document("global")
    return try {
      val payload = mapOf(
        "updatedByUid" to uid,
        "ownerEmail" to config.ownerEmail.trim().ifBlank { "farhanSajid@gmail.com" }.take(120),
        "customFirebaseDatabaseId" to config.customFirebaseDatabaseId.trim()
          .ifBlank { "ai-studio-android-royallud-d389e848-893a-41af-b823-e38eded48b8c" }
          .take(128),
        "turnDurationSeconds" to config.turnDurationSeconds.coerceIn(8L, 120L),
        "winPoints" to config.winPoints.coerceIn(10L, 1000L),
        "capturePoints" to config.capturePoints.coerceIn(5L, 500L),
        "homePoints" to config.homePoints.coerceIn(5L, 500L),
        "missionRewardMultiplier" to config.missionRewardMultiplier.coerceIn(1L, 10L),
        "announcementBanner" to config.announcementBanner.trim().take(200),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createRoom(
    roomCode: String,
    hostName: String,
    isPublic: Boolean,
    maxPlayers: Int = 4,
    hostColor: PlayerColor = PlayerColor.RED,
    initialStatus: RoomStatus = RoomStatus.WAITING
  ): Result<LudoRoom> {
    val uid = requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      val safeName = hostName.trim().ifBlank { "Host Player" }.take(60)
      val validMax = when {
        maxPlayers <= 2 -> 2
        maxPlayers <= 4 -> 4
        maxPlayers <= 6 -> 6
        else -> 8
      }
      val payload = mapOf(
        "hostUid" to uid,
        "roomCode" to normalizedCode,
        "isPublic" to isPublic,
        "status" to initialStatus.name,
        "maxPlayers" to validMax.toLong(),
        "memberUids" to listOf(uid),
        "playerNames" to listOf(safeName),
        "playerColors" to listOf(hostColor.name),
        "playerUids" to listOf(uid),
        "tokens" to List(32) { -1L },
        "currentTurnColor" to hostColor.name,
        "turnPhase" to TurnPhase.ROLL.name,
        "diceValue" to 0L,
        "diceValue2" to 0L,
        "dice1Used" to false,
        "dice2Used" to false,
        "selectedDieIndex" to 0L,
        "consecutiveSixes" to 0L,
        "turnNumber" to 1L,
        "lastEvent" to "$safeName created ${validMax}-player room $normalizedCode",
        "reactionEmoji" to "",
        "reactionSender" to "",
        "winnerColor" to "NONE",
        "winnerOrder" to emptyList<String>(),
        "activeVoiceUids" to emptyList<String>(),
        "callMode" to AudioCallMode.NONE.name,
        "callInitiatorUid" to "",
        "callTargetUid" to "",
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

  suspend fun joinRoomByCode(
    roomCode: String,
    playerName: String,
    preferredColor: PlayerColor? = null
  ): Result<LudoRoom> {
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

      val defaultOrder = PlayerColor.defaultColorsForCount(room.maxPlayers.toInt()).map { it.name } +
        PlayerColor.entries.map { it.name }
      val nextColor = if (preferredColor != null && preferredColor.name !in room.playerColors) {
        preferredColor.name
      } else {
        defaultOrder.firstOrNull { it !in room.playerColors } ?: PlayerColor.YELLOW.name
      }
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

  suspend fun changePlayerColorInRoom(roomCode: String, newColor: PlayerColor): Result<Unit> {
    val uid = requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      val snapshot = docRef.get().await()
      val room = snapshot.toObject(
        LudoRoom::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: return Result.failure(IllegalStateException("Room not found"))

      if (room.status != RoomStatus.WAITING.name) {
        return Result.failure(IllegalStateException("Cannot change color after match started"))
      }
      if (newColor.name in room.playerColors) {
        return Result.failure(IllegalArgumentException("${newColor.label} is already taken"))
      }
      val seatIdx = room.playerUids.indexOf(uid)
      if (seatIdx < 0) {
        return Result.failure(IllegalStateException("Player not seated in room"))
      }

      val updatedColors = room.playerColors.toMutableList()
      updatedColors[seatIdx] = newColor.name
      val updatedTurnColor = if (seatIdx == 0) newColor.name else room.currentTurnColor

      docRef.update(
        mapOf(
          "playerColors" to updatedColors,
          "currentTurnColor" to updatedTurnColor,
          "lastEvent" to "${room.playerNames.getOrElse(seatIdx) { "Player" }} selected ${newColor.label}",
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

  suspend fun findOrCreateQuickMatch(
    playerName: String,
    maxPlayers: Int = 4,
    preferredColor: PlayerColor = PlayerColor.RED
  ): Result<LudoRoom> {
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
        (uid in room.memberUids || room.memberUids.size < room.maxPlayers.toInt()) &&
          room.maxPlayers.toInt() == maxPlayers
      }

      if (openRoom != null) {
        joinRoomByCode(openRoom.roomCode, playerName, preferredColor)
      } else {
        val generatedCode = generateRoomCode()
        createRoom(
          roomCode = generatedCode,
          hostName = playerName,
          isPublic = true,
          maxPlayers = maxPlayers,
          hostColor = preferredColor
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

      val targetSeats = room.maxPlayers.toInt().coerceIn(2, 8)
      val colorPool = (PlayerColor.defaultColorsForCount(targetSeats) + PlayerColor.entries)
        .map { it.name }
        .distinct()

      if (fillWithBots || updatedColors.size < 2) {
        val botNames = listOf(
          "Atelier Bot", "Tactical AI", "Archon AI", "Vanguard AI",
          "Citadel AI", "Sovereign AI", "Sentinel AI"
        )
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

      val firstTurnColor = updatedColors.firstOrNull() ?: PlayerColor.RED.name
      docRef.update(
        mapOf(
          "status" to RoomStatus.PLAYING.name,
          "playerNames" to updatedNames,
          "playerColors" to updatedColors,
          "playerUids" to updatedUids,
          "currentTurnColor" to firstTurnColor,
          "lastEvent" to "Match started! ${PlayerColor.fromString(firstTurnColor).label}'s turn to roll 2 dice.",
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
    diceValue2: Int,
    dice1Used: Boolean,
    dice2Used: Boolean,
    selectedDieIndex: Int,
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
      val paddedTokens = if (tokens.size == 32) {
        tokens
      } else {
        (tokens + List(32) { -1 }).take(32)
      }
      docRef.update(
        mapOf(
          "status" to status.name,
          "tokens" to paddedTokens.map { it.toLong() },
          "currentTurnColor" to currentTurnColor.name,
          "turnPhase" to turnPhase.name,
          "diceValue" to diceValue.coerceIn(0, 6).toLong(),
          "diceValue2" to diceValue2.coerceIn(0, 6).toLong(),
          "dice1Used" to dice1Used,
          "dice2Used" to dice2Used,
          "selectedDieIndex" to selectedDieIndex.coerceIn(0, 1).toLong(),
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

  suspend fun updateVoiceCallState(
    roomCode: String,
    activeVoiceUids: List<String>,
    callMode: AudioCallMode,
    callInitiatorUid: String,
    callTargetUid: String
  ): Result<Unit> {
    requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      docRef.update(
        mapOf(
          "activeVoiceUids" to activeVoiceUids.distinct().take(8),
          "callMode" to callMode.name,
          "callInitiatorUid" to callInitiatorUid.take(128),
          "callTargetUid" to callTargetUid.take(128),
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

  suspend fun sendVoiceSignal(
    roomCode: String,
    senderName: String,
    targetUid: String,
    signalType: String,
    payload: String
  ): Result<Unit> {
    val uid = requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val signalId = "sig_" + UUID.randomUUID().toString().replace("-", "").take(16)
    val docRef = db.collection("rooms").document(normalizedCode)
      .collection("voiceSignals").document(signalId)
    return try {
      docRef.set(
        mapOf(
          "senderUid" to uid,
          "senderName" to senderName.take(60).ifBlank { "Player" },
          "targetUid" to targetUid.take(128).ifBlank { "ALL" },
          "signalType" to signalType,
          "payload" to payload.take(32000),
          "createdAt" to FieldValue.serverTimestamp()
        )
      ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeVoiceSignals(roomCode: String): Flow<List<VoiceSignal>> {
    val normalizedCode = roomCode.trim().uppercase()
    val path = "rooms/$normalizedCode/voiceSignals"
    return db.collection("rooms").document(normalizedCode)
      .collection("voiceSignals")
      .limit(25)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          VoiceSignal::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
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

  suspend fun touchPresenceHeartbeat(): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        docRef.update(mapOf("updatedAt" to FieldValue.serverTimestamp())).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun searchPlayersByUsername(
    query: String,
    limit: Long = 80L
  ): Result<List<PlayerProfile>> {
    val uid = requireUserId()
    val path = "users"
    return try {
      val snapshot = db.collection(path)
        .orderBy("score", Query.Direction.DESCENDING)
        .limit(limit)
        .get()
        .await()

      val cleanQuery = query.trim().lowercase()
      val matches = snapshot.toObjects(
        PlayerProfile::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ).filter { profile ->
        profile.userId.isNotBlank() &&
          profile.userId != uid &&
          (cleanQuery.isBlank() ||
            profile.displayName.lowercase().contains(cleanQuery) ||
            profile.countryName.lowercase().contains(cleanQuery) ||
            profile.countryCode.lowercase().contains(cleanQuery))
      }
      Result.success(matches)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      Result.failure(e)
    }
  }

  suspend fun sendFriendRequest(
    targetUserId: String,
    targetDisplayName: String,
    targetCountryCode: String = "US",
    senderName: String = "Royal Player",
    senderCountryCode: String = "US"
  ): Result<FriendRequest> {
    val uid = requireUserId()
    val cleanTargetUid = targetUserId.trim()
    if (cleanTargetUid.isBlank() || cleanTargetUid == uid) {
      return Result.failure(IllegalArgumentException("Cannot send a friend request to yourself."))
    }

    val requestId = FriendsStudioCatalog.buildFriendRequestId(uid, cleanTargetUid)
    val docRef = db.collection("friendRequests").document(requestId)

    return try {
      val safeSenderName = senderName.trim().ifBlank { "Royal Player" }.take(60)
      val safeSenderCountry = senderCountryCode.trim().uppercase().ifBlank { "US" }.take(8)
      val safeTargetName = targetDisplayName.trim().ifBlank { "Royal Friend" }.take(60)
      val safeTargetCountry = targetCountryCode.trim().uppercase().ifBlank { "US" }.take(8)

      val payload = mapOf(
        "fromUid" to uid,
        "fromName" to safeSenderName,
        "fromCountry" to safeSenderCountry,
        "toUid" to cleanTargetUid,
        "toName" to safeTargetName,
        "toCountry" to safeTargetCountry,
        "status" to FriendRequestStatus.PENDING.name,
        "memberUids" to listOf(uid, cleanTargetUid),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
      )
      docRef.set(payload).await()

      val saved = FriendRequest(
        id = requestId,
        fromUid = uid,
        fromName = safeSenderName,
        fromCountry = safeSenderCountry,
        toUid = cleanTargetUid,
        toName = safeTargetName,
        toCountry = safeTargetCountry,
        status = FriendRequestStatus.PENDING.name,
        memberUids = listOf(uid, cleanTargetUid)
      )
      Result.success(saved)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun respondToFriendRequest(
    requestId: String,
    accept: Boolean
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("friendRequests").document(requestId)
    return try {
      val newStatus = if (accept) {
        FriendRequestStatus.ACCEPTED.name
      } else {
        FriendRequestStatus.DECLINED.name
      }
      docRef.update(
        mapOf(
          "status" to newStatus,
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

  suspend fun removeFriendOrRequest(requestId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection("friendRequests").document(requestId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeFriendRequests(userId: String): Flow<List<FriendRequest>> {
    val path = "friendRequests"
    return db.collection(path)
      .whereArrayContains("memberUids", userId)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          FriendRequest::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeActivePublicRooms(): Flow<List<LudoRoom>> {
    val path = "rooms"
    return db.collection(path)
      .whereEqualTo("isPublic", true)
      .snapshots()
      .map { snapshot ->
        snapshot.toObjects(
          LudoRoom::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ).filter { it.status == RoomStatus.WAITING.name || it.status == RoomStatus.PLAYING.name }
      }
      .catch { error ->
        if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
        throw error
      }
  }

  fun observeWaitingRoomsForFriends(): Flow<List<LudoRoom>> {
    val path = "rooms"
    return db.collection(path)
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

  suspend fun spectateRoomByCode(roomCode: String): Result<LudoRoom> {
    requireUserId()
    val normalizedCode = roomCode.trim().uppercase()
    val docRef = db.collection("rooms").document(normalizedCode)
    return try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        return Result.failure(NoSuchElementException("Room $normalizedCode not found"))
      }
      val room = snapshot.toObject(
        LudoRoom::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
      ) ?: return Result.failure(IllegalStateException("Unable to read room $normalizedCode"))
      Result.success(room)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  fun observeSpectatablePublicPlayingRooms(): Flow<List<LudoRoom>> {
    val path = "rooms"
    return db.collection(path)
      .whereEqualTo("status", RoomStatus.PLAYING.name)
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

  fun observeOngoingPrivateRooms(): Flow<List<LudoRoom>> {
    val path = "rooms"
    return db.collection(path)
      .whereEqualTo("isPublic", false)
      .whereEqualTo("status", RoomStatus.PLAYING.name)
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
