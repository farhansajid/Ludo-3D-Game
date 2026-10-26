package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PlayerColor(val label: String, val startTrackIndex: Int) {
  RED("Terracotta Red", 0),
  GREEN("Sage Green", 13),
  YELLOW("Ochre Gold", 26),
  BLUE("Slate Blue", 39);

  companion object {
    fun fromString(value: String): PlayerColor {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: RED
    }
  }
}

enum class TurnPhase {
  ROLL,
  MOVE,
  GAME_OVER;

  companion object {
    fun fromString(value: String): TurnPhase {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ROLL
    }
  }
}

enum class RoomStatus {
  WAITING,
  PLAYING,
  FINISHED;

  companion object {
    fun fromString(value: String): RoomStatus {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: WAITING
    }
  }
}

data class PlayerSeat(
  val color: PlayerColor,
  val uid: String,
  val name: String,
  val isBot: Boolean = uid.startsWith("BOT_")
)

@IgnoreExtraProperties
data class PlayerProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "Royal Player",
  val gamesPlayed: Long = 0L,
  val gamesWon: Long = 0L,
  val capturesTotal: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LudoRoom(
  @DocumentId val id: String = "",
  val hostUid: String = "",
  val roomCode: String = "",
  val isPublic: Boolean = true,
  val status: String = RoomStatus.WAITING.name,
  val maxPlayers: Long = 4L,
  val memberUids: List<String> = emptyList(),
  val playerNames: List<String> = emptyList(),
  val playerColors: List<String> = emptyList(),
  val playerUids: List<String> = emptyList(),
  val tokens: List<Long> = List(16) { -1L },
  val currentTurnColor: String = PlayerColor.RED.name,
  val turnPhase: String = TurnPhase.ROLL.name,
  val diceValue: Long = 0L,
  val consecutiveSixes: Long = 0L,
  val turnNumber: Long = 1L,
  val lastEvent: String = "Waiting for players",
  val reactionEmoji: String = "",
  val reactionSender: String = "",
  val winnerColor: String = "NONE",
  val winnerOrder: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val tokenPositions: List<Int>
    get() = if (tokens.size == 16) tokens.map { (it as Number).toInt() } else List(16) { -1 }

  val seats: List<PlayerSeat>
    get() = playerColors.mapIndexed { index, colorStr ->
      val uid = playerUids.getOrElse(index) { "BOT_$index" }
      val name = playerNames.getOrElse(index) { "Player ${index + 1}" }
      PlayerSeat(
        color = PlayerColor.fromString(colorStr),
        uid = uid,
        name = name,
        isBot = uid.startsWith("BOT_")
      )
    }
}
