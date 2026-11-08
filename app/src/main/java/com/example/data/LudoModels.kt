package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

enum class PlayerColor(val label: String, val startTrackIndex: Int) {
  RED("Terracotta Red", 0),
  GREEN("Sage Green", 13),
  YELLOW("Ochre Gold", 26),
  BLUE("Slate Blue", 39),
  PURPLE("Royal Amethyst", 52),
  ORANGE("Coral Amber", 65),
  TEAL("Aegean Lagoon", 78),
  ROSE("Rose Quartz", 91);

  companion object {
    fun fromString(value: String): PlayerColor {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: RED
    }

    fun defaultColorsForCount(maxPlayers: Int): List<PlayerColor> {
      return when (maxPlayers) {
        2 -> listOf(RED, YELLOW)
        6 -> listOf(RED, GREEN, YELLOW, BLUE, PURPLE, ORANGE)
        8 -> entries.toList()
        else -> listOf(RED, GREEN, YELLOW, BLUE)
      }
    }
  }
}

/**
 * High-responsiveness 3D pawn movement speed presets.
 * Default is FAST (2x) so pawn hops feel snappy and fluid.
 */
enum class PawnMoveSpeed(val label: String, val stepDelayMs: Long, val framesPerStep: Int) {
  NORMAL("1x", 8L, 4),
  FAST("2x", 4L, 3),
  TURBO("4x", 2L, 2),
  INSTANT("Instant", 0L, 1);
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

enum class AudioCallMode {
  NONE,
  GROUP,
  DIRECT;

  companion object {
    fun fromString(value: String): AudioCallMode {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
    }
  }
}

data class CountryInfo(
  val code: String,
  val name: String,
  val flagEmoji: String,
  val currencyCode: String = "USD",
  val currencySymbol: String = "$",
  val usdExchangeRate: Double = 1.0
)

object CountryCatalog {
  val COUNTRIES: List<CountryInfo> = listOf(
    CountryInfo("US", "United States", "🇺🇸", "USD", "$", 1.0),
    CountryInfo("IN", "India", "🇮🇳", "INR", "₹", 84.0),
    CountryInfo("PK", "Pakistan", "🇵🇰", "PKR", "Rs ", 278.0),
    CountryInfo("GB", "United Kingdom", "🇬🇧", "GBP", "£", 0.79),
    CountryInfo("CA", "Canada", "🇨🇦", "CAD", "CA$", 1.38),
    CountryInfo("AU", "Australia", "🇦🇺", "AUD", "A$", 1.52),
    CountryInfo("AE", "United Arab Emirates", "🇦🇪", "AED", "AED ", 3.67),
    CountryInfo("SA", "Saudi Arabia", "🇸🇦", "SAR", "SAR ", 3.75),
    CountryInfo("DE", "Germany", "🇩🇪", "EUR", "€", 0.92),
    CountryInfo("FR", "France", "🇫🇷", "EUR", "€", 0.92),
    CountryInfo("BR", "Brazil", "🇧🇷", "BRL", "R$", 5.05),
    CountryInfo("JP", "Japan", "🇯🇵", "JPY", "¥", 152.0),
    CountryInfo("KR", "South Korea", "🇰🇷", "KRW", "₩", 1380.0),
    CountryInfo("SG", "Singapore", "🇸🇬", "SGD", "S$", 1.34),
    CountryInfo("MY", "Malaysia", "🇲🇾", "MYR", "RM", 4.45),
    CountryInfo("TR", "Turkey", "🇹🇷", "TRY", "₺", 34.2),
    CountryInfo("NG", "Nigeria", "🇳🇬", "NGN", "₦", 1600.0),
    CountryInfo("ZA", "South Africa", "🇿🇦", "ZAR", "R", 18.0),
    CountryInfo("MX", "Mexico", "🇲🇽", "MXN", "MX$", 19.8),
    CountryInfo("ES", "Spain", "🇪🇸", "EUR", "€", 0.92),
    CountryInfo("IT", "Italy", "🇮🇹", "EUR", "€", 0.92),
    CountryInfo("BD", "Bangladesh", "🇧🇩", "BDT", "৳", 120.0),
    CountryInfo("ID", "Indonesia", "🇮🇩", "IDR", "Rp ", 15800.0),
    CountryInfo("PH", "Philippines", "🇵🇭", "PHP", "₱", 58.0)
  )

  fun detectDefaultCountry(): CountryInfo {
    val localeCountry = Locale.getDefault().country.uppercase()
    if (localeCountry.length == 2) {
      return fromCode(localeCountry)
    }
    return COUNTRIES.first()
  }

  fun fromCode(code: String): CountryInfo {
    val upper = code.trim().uppercase()
    val existing = COUNTRIES.firstOrNull { it.code == upper }
    if (existing != null) return existing

    val safeCode = if (upper.length == 2) upper else "US"
    val locale = Locale("", safeCode)
    val displayName = locale.displayCountry.ifBlank { safeCode }
    val javaCurrency = runCatching { java.util.Currency.getInstance(locale) }.getOrNull()
    val currCode = javaCurrency?.currencyCode ?: "USD"
    val currSymbol = javaCurrency?.getSymbol(locale)?.takeIf { it.isNotBlank() } ?: "$"
    return CountryInfo(
      code = safeCode,
      name = displayName,
      flagEmoji = flagEmojiForCountryCode(safeCode),
      currencyCode = currCode,
      currencySymbol = currSymbol,
      usdExchangeRate = 1.0
    )
  }

  fun formatUsdPriceForCountry(usdAmount: Double, countryCode: String): String {
    val info = fromCode(countryCode)
    val converted = usdAmount * info.usdExchangeRate
    return if (info.usdExchangeRate >= 25.0) {
      // Whole-number rounding for high-denomination currencies (PKR, INR, JPY, KRW, NGN, IDR, PHP, BDT, TRY)
      val rounded = ((converted + 2.0) / 5.0).toInt().coerceAtLeast(1) * 5
      "${info.currencySymbol}%,d".format(Locale.US, rounded)
    } else {
      "${info.currencySymbol}%.2f".format(Locale.US, converted)
    }
  }

  fun formatCoinsEquivalent(coins: Long, countryCode: String): String {
    val info = fromCode(countryCode)
    val usdEquivalent = coins.toDouble() / 500.0
    val converted = usdEquivalent * info.usdExchangeRate
    return if (info.usdExchangeRate >= 25.0) {
      "${info.currencySymbol}%,d ${info.currencyCode}".format(Locale.US, converted.toLong())
    } else {
      "${info.currencySymbol}%.2f ${info.currencyCode}".format(Locale.US, converted)
    }
  }

  fun flagEmojiForCountryCode(code: String): String {
    val clean = code.trim().uppercase()
    if (clean.length != 2 || !clean[0].isLetter() || !clean[1].isLetter()) return "🌍"
    val first = Character.codePointAt(clean, 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(clean, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
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
  val countryCode: String = "US",
  val countryName: String = "United States",
  val score: Long = 100L,
  val gamesPlayed: Long = 0L,
  val gamesWon: Long = 0L,
  val capturesTotal: Long = 0L,
  val homesReachedTotal: Long = 0L,
  val coins: Long = 250L,
  val xp: Long = 0L,
  val claimedMissionIds: List<String> = emptyList(),
  val unlockedItems: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val playerLevel: Int
    get() = ((xp / 200L) + 1L).toInt().coerceAtLeast(1)

  val xpIntoCurrentLevel: Int
    get() = (xp % 200L).toInt()

  val gamesLost: Long
    get() = (gamesPlayed - gamesWon).coerceAtLeast(0L)

  val winRatePercent: Int
    get() = if (gamesPlayed > 0L) {
      ((gamesWon * 100L) / gamesPlayed).toInt().coerceIn(0, 100)
    } else {
      0
    }

  val winLossRatio: Double
    get() = if (gamesLost > 0L) {
      gamesWon.toDouble() / gamesLost.toDouble()
    } else {
      gamesWon.toDouble()
    }

  val winLossRatioFormatted: String
    get() = String.format(Locale.US, "%.2f", winLossRatio)

  val capturesPerMatchAvg: String
    get() = if (gamesPlayed > 0L) {
      String.format(Locale.US, "%.1f", capturesTotal.toDouble() / gamesPlayed.toDouble())
    } else {
      "0.0"
    }

  val rankTierTitle: String
    get() = when {
      score >= 1200L -> "Sovereign Grandmaster"
      score >= 750L -> "Diamond Archon"
      score >= 400L -> "Golden Tactician"
      score >= 200L -> "Emerald Knight"
      else -> "Royal Contender"
    }

  fun parsedAvatarId(fallbackAvatarId: String = ProfileCatalog.DEFAULT_AVATAR_ID): String {
    return ProfileCatalog.decodeAvatarFromUnlockedItems(unlockedItems) ?: fallbackAvatarId
  }

  fun parsedEquippedEmoteLoadout(): Map<EmoteMomentTrigger, String> {
    return ProfileCatalog.decodeEquippedLoadoutFromUnlockedItems(unlockedItems)
  }

  fun parsedMatchHistory(): List<ProfileMatchHistoryEntry> {
    return ProfileCatalog.decodeMatchHistoryFromClaimedList(claimedMissionIds)
  }

  fun parsedMissionProgress(): Map<MissionType, Int> {
    return ProfileCatalog.decodeMissionProgressFromClaimedList(claimedMissionIds)
  }

  fun missionProgressForType(type: MissionType): Int {
    val encodedCount = parsedMissionProgress()[type] ?: 0
    return when (type) {
      MissionType.WIN_MATCHES -> maxOf(encodedCount, gamesWon.toInt())
      MissionType.ROLL_SIXES -> encodedCount
      MissionType.CAPTURE_PAWNS -> maxOf(encodedCount, capturesTotal.toInt())
      MissionType.REACH_HOME -> maxOf(encodedCount, homesReachedTotal.toInt())
      MissionType.OPEN_FROM_YARD -> encodedCount
    }
  }
}

data class CustomAvatarOption(
  val id: String,
  val title: String,
  val subtitle: String,
  val badgeSymbol: String,
  val primaryColorHex: Long,
  val ringColorHex: Long
) {
  val authPhotoUriString: String
    get() = "ludo-avatar://$id"
}

enum class MatchOutcomeType(val code: String, val label: String) {
  WIN("W", "VICTORY"),
  LOSS("L", "DEFEAT"),
  FORFEIT("F", "FORFEIT");

  companion object {
    fun fromCode(code: String): MatchOutcomeType {
      return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: LOSS
    }
  }
}

data class ProfileMatchHistoryEntry(
  val id: String,
  val outcome: MatchOutcomeType,
  val modeLabel: String,
  val captures: Int,
  val homesReached: Int,
  val scoreDelta: Int,
  val timestampLabel: String = "Recorded Match"
)

object ProfileCatalog {
  private const val EQUIP_PREFIX = "EQUIP:"
  private const val HIST_PREFIX = "HIST:"
  private const val AVATAR_PREFIX = "AVATAR:"
  private const val MISSION_PROGRESS_PREFIX = "MPROG:"

  const val DEFAULT_AVATAR_ID = "avatar_sovereign_crown"

  val AVATAR_OPTIONS: List<CustomAvatarOption> = listOf(
    CustomAvatarOption(
      id = "avatar_sovereign_crown",
      title = "Sovereign Crown",
      subtitle = "Imperial Gold Crest",
      badgeSymbol = "👑",
      primaryColorHex = 0xFFD4AF37,
      ringColorHex = 0xFFFFE082
    ),
    CustomAvatarOption(
      id = "avatar_emerald_dragon",
      title = "Jade Dragon",
      subtitle = "Sage Guardian",
      badgeSymbol = "🐉",
      primaryColorHex = 0xFF3D6B58,
      ringColorHex = 0xFF81C784
    ),
    CustomAvatarOption(
      id = "avatar_crimson_knight",
      title = "Terracotta Knight",
      subtitle = "Vanguard Striker",
      badgeSymbol = "⚔️",
      primaryColorHex = 0xFFC84B31,
      ringColorHex = 0xFFFF8A65
    ),
    CustomAvatarOption(
      id = "avatar_sapphire_archon",
      title = "Sapphire Archon",
      subtitle = "Grand Strategist",
      badgeSymbol = "🛡️",
      primaryColorHex = 0xFF345B87,
      ringColorHex = 0xFF64B5F6
    ),
    CustomAvatarOption(
      id = "avatar_golden_dice",
      title = "Dual-Dice Master",
      subtitle = "High Roller Crest",
      badgeSymbol = "🎲",
      primaryColorHex = 0xFFD99B26,
      ringColorHex = 0xFFFFD54F
    ),
    CustomAvatarOption(
      id = "avatar_amethyst_monarch",
      title = "Amethyst Monarch",
      subtitle = "Royal Mystic",
      badgeSymbol = "🔮",
      primaryColorHex = 0xFF7B4B94,
      ringColorHex = 0xFFCE93D8
    ),
    CustomAvatarOption(
      id = "avatar_phoenix_blazer",
      title = "Solar Phoenix",
      subtitle = "Undefeated Flame",
      badgeSymbol = "🔥",
      primaryColorHex = 0xFFD96B27,
      ringColorHex = 0xFFFFB74D
    ),
    CustomAvatarOption(
      id = "avatar_celestial_star",
      title = "Celestial Star",
      subtitle = "Starlight Champion",
      badgeSymbol = "🌟",
      primaryColorHex = 0xFF2A7B76,
      ringColorHex = 0xFF4DB6AC
    )
  )

  fun findAvatarById(avatarId: String?): CustomAvatarOption {
    if (avatarId.isNullOrBlank()) return AVATAR_OPTIONS.first()
    return AVATAR_OPTIONS.firstOrNull { it.id == avatarId } ?: AVATAR_OPTIONS.first()
  }

  fun parseAvatarIdFromAuthPhotoUri(photoUriString: String?): String? {
    if (photoUriString.isNullOrBlank()) return null
    val trimmed = photoUriString.trim()
    val candidate = when {
      trimmed.startsWith("ludo-avatar://") -> trimmed.removePrefix("ludo-avatar://")
      trimmed.startsWith(AVATAR_PREFIX) -> trimmed.removePrefix(AVATAR_PREFIX)
      else -> trimmed
    }
    return AVATAR_OPTIONS.firstOrNull { it.id == candidate }?.id
  }

  fun encodeAvatarIntoUnlockedItems(
    existingUnlockedItems: List<String>,
    avatarId: String
  ): List<String> {
    val validAvatar = findAvatarById(avatarId)
    val withoutAvatar = existingUnlockedItems
      .filterNot { it.startsWith(AVATAR_PREFIX) }
      .distinct()
      .take(46)
    return (withoutAvatar + "$AVATAR_PREFIX${validAvatar.id}").distinct().take(48)
  }

  fun decodeAvatarFromUnlockedItems(unlockedItems: List<String>): String? {
    val raw = unlockedItems.lastOrNull { it.startsWith(AVATAR_PREFIX) } ?: return null
    val avatarId = raw.removePrefix(AVATAR_PREFIX).trim()
    return AVATAR_OPTIONS.firstOrNull { it.id == avatarId }?.id
  }

  val DEFAULT_SAMPLE_HISTORY: List<ProfileMatchHistoryEntry> = listOf(
    ProfileMatchHistoryEntry(
      id = "sample_hist_1",
      outcome = MatchOutcomeType.WIN,
      modeLabel = "Sovereign Cup Quarterfinal (2P)",
      captures = 3,
      homesReached = 4,
      scoreDelta = 235,
      timestampLabel = "Recent Match"
    ),
    ProfileMatchHistoryEntry(
      id = "sample_hist_2",
      outcome = MatchOutcomeType.WIN,
      modeLabel = "Classic 4P Dual-Dice Studio",
      captures = 2,
      homesReached = 4,
      scoreDelta = 210,
      timestampLabel = "Earlier Today"
    ),
    ProfileMatchHistoryEntry(
      id = "sample_hist_3",
      outcome = MatchOutcomeType.LOSS,
      modeLabel = "6P Royal Hex Showdown",
      captures = 2,
      homesReached = 2,
      scoreDelta = 65,
      timestampLabel = "Yesterday"
    )
  )

  fun encodeEquippedLoadoutIntoUnlockedItems(
    existingUnlockedItems: List<String>,
    loadout: Map<EmoteMomentTrigger, String>,
    additionalUnlockedItemId: String? = null
  ): List<String> {
    val baseUnlocked = existingUnlockedItems
      .filterNot { it.startsWith(EQUIP_PREFIX) }
      .let { list ->
        if (!additionalUnlockedItemId.isNullOrBlank()) list + additionalUnlockedItemId else list
      }
      .distinct()
      .take(38)

    val equipTokens = EmoteMomentTrigger.entries.mapNotNull { moment ->
      val emoteId = loadout[moment] ?: EmoteGalleryCatalog.DEFAULT_EQUIPPED_LOADOUT[moment]
      if (!emoteId.isNullOrBlank()) {
        "$EQUIP_PREFIX${moment.id}:$emoteId"
      } else {
        null
      }
    }
    return (baseUnlocked + equipTokens).distinct().take(48)
  }

  fun decodeEquippedLoadoutFromUnlockedItems(
    unlockedItems: List<String>
  ): Map<EmoteMomentTrigger, String> {
    val result = EmoteGalleryCatalog.DEFAULT_EQUIPPED_LOADOUT.toMutableMap()
    unlockedItems
      .filter { it.startsWith(EQUIP_PREFIX) }
      .forEach { raw ->
        val parts = raw.removePrefix(EQUIP_PREFIX).split(":")
        if (parts.size >= 2) {
          val momentId = parts[0]
          val emoteId = parts[1]
          val trigger = EmoteMomentTrigger.entries.firstOrNull { it.id == momentId }
          val validEmote = EmoteGalleryCatalog.ALL_EMOTES.any { it.id == emoteId }
          if (trigger != null && validEmote) {
            result[trigger] = emoteId
          }
        }
      }
    return result
  }

  fun encodeMatchHistoryIntoClaimedList(
    existingClaimed: List<String>,
    entry: ProfileMatchHistoryEntry
  ): List<String> {
    val nonHistoryIds = existingClaimed.filterNot { it.startsWith(HIST_PREFIX) }.distinct().take(50)
    val existingHistoryRaw = existingClaimed.filter { it.startsWith(HIST_PREFIX) }
    val safeMode = entry.modeLabel
      .replace(":", "-")
      .replace("|", "-")
      .trim()
      .ifBlank { "3D Match" }
      .take(28)
    val encoded = "$HIST_PREFIX${entry.outcome.code}:$safeMode:${entry.captures}:${entry.homesReached}:${entry.scoreDelta}"
    val updatedHistory = (listOf(encoded) + existingHistoryRaw).take(15)
    return (nonHistoryIds + updatedHistory).take(80)
  }

  fun decodeMatchHistoryFromClaimedList(
    claimedList: List<String>
  ): List<ProfileMatchHistoryEntry> {
    return claimedList
      .filter { it.startsWith(HIST_PREFIX) }
      .mapIndexedNotNull { idx, raw ->
        val parts = raw.removePrefix(HIST_PREFIX).split(":")
        if (parts.size >= 5) {
          val outcome = MatchOutcomeType.fromCode(parts[0])
          val mode = parts[1].ifBlank { "3D Match" }
          val captures = parts[2].toIntOrNull() ?: 0
          val homes = parts[3].toIntOrNull() ?: 0
          val delta = parts[4].toIntOrNull() ?: 0
          ProfileMatchHistoryEntry(
            id = "fs_hist_$idx",
            outcome = outcome,
            modeLabel = mode,
            captures = captures,
            homesReached = homes,
            scoreDelta = delta,
            timestampLabel = if (idx == 0) "Latest Cloud Match" else "Match #${idx + 1}"
          )
        } else {
          null
        }
      }
  }

  fun decodeMissionProgressFromClaimedList(
    claimedList: List<String>
  ): Map<MissionType, Int> {
    val progressMap = mutableMapOf<MissionType, Int>()
    claimedList
      .filter { it.startsWith(MISSION_PROGRESS_PREFIX) }
      .forEach { raw ->
        val parts = raw.removePrefix(MISSION_PROGRESS_PREFIX).split(":")
        if (parts.size >= 2) {
          val type = MissionType.entries.firstOrNull { it.name == parts[0] }
          val count = parts[1].toIntOrNull()?.coerceAtLeast(0) ?: 0
          if (type != null) {
            progressMap[type] = maxOf(progressMap[type] ?: 0, count)
          }
        }
      }
    return progressMap
  }

  fun encodeMissionProgressIntoClaimedList(
    existingClaimed: List<String>,
    progressByType: Map<MissionType, Int>
  ): List<String> {
    val nonProgressEntries = existingClaimed
      .filterNot { it.startsWith(MISSION_PROGRESS_PREFIX) }
      .distinct()
      .take(65)
    val progressTokens = MissionType.entries.mapNotNull { type ->
      val count = (progressByType[type] ?: 0).coerceIn(0, 999999)
      if (count > 0) {
        "$MISSION_PROGRESS_PREFIX${type.name}:$count"
      } else {
        null
      }
    }
    return (nonProgressEntries + progressTokens).distinct().take(80)
  }

  fun incrementMissionProgressInClaimedList(
    existingClaimed: List<String>,
    deltas: Map<MissionType, Int>
  ): List<String> {
    val current = decodeMissionProgressFromClaimedList(existingClaimed).toMutableMap()
    deltas.forEach { (type, delta) ->
      if (delta > 0) {
        current[type] = ((current[type] ?: 0) + delta).coerceIn(0, 999999)
      }
    }
    return encodeMissionProgressIntoClaimedList(existingClaimed, current)
  }
}


enum class MissionType {
  WIN_MATCHES,
  ROLL_SIXES,
  CAPTURE_PAWNS,
  REACH_HOME,
  OPEN_FROM_YARD
}

data class DailyMission(
  val id: String,
  val title: String,
  val description: String,
  val type: MissionType,
  val targetCount: Int,
  val rewardCoins: Long,
  val rewardXp: Long
)

object DailyMissionCatalog {
  val MISSIONS: List<DailyMission> = listOf(
    DailyMission(
      id = "mission_win_3",
      title = "Triple Crown Champion",
      description = "Win 3 games (Online or Offline 3D Studio)",
      type = MissionType.WIN_MATCHES,
      targetCount = 3,
      rewardCoins = 300L,
      rewardXp = 220L
    ),
    DailyMission(
      id = "mission_roll_six_5",
      title = "High Roller Sixes",
      description = "Roll a 6 five times on the dual 3D dice",
      type = MissionType.ROLL_SIXES,
      targetCount = 5,
      rewardCoins = 180L,
      rewardXp = 130L
    ),
    DailyMission(
      id = "mission_win_1",
      title = "Royal Victor",
      description = "Win 1 Ludo match (Online or Offline 3D Studio)",
      type = MissionType.WIN_MATCHES,
      targetCount = 1,
      rewardCoins = 150L,
      rewardXp = 120L
    ),
    DailyMission(
      id = "mission_capture_2",
      title = "Tactical Hunter",
      description = "Capture 2 opponent pawns on the outer track",
      type = MissionType.CAPTURE_PAWNS,
      targetCount = 2,
      rewardCoins = 120L,
      rewardXp = 90L
    ),
    DailyMission(
      id = "mission_home_3",
      title = "Sanctuary Escort",
      description = "Guide 3 pawns safely into the center Home Goal",
      type = MissionType.REACH_HOME,
      targetCount = 3,
      rewardCoins = 140L,
      rewardXp = 100L
    ),
    DailyMission(
      id = "mission_open_4",
      title = "Dual-Dice Vanguard",
      description = "Open 4 pawns from your yard by rolling a 6",
      type = MissionType.OPEN_FROM_YARD,
      targetCount = 4,
      rewardCoins = 100L,
      rewardXp = 80L
    )
  )
}

enum class StoreItemType {
  IAP_COIN_BUNDLE,
  COSMETIC_UNLOCK
}

data class StoreItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val type: StoreItemType,
  val priceLabel: String = "",
  val basePriceUsd: Double = 0.0,
  val coinCost: Long = 0L,
  val coinsGranted: Long = 0L,
  val bonusXp: Long = 0L,
  val badgeTag: String = ""
) {
  fun localizedPrice(countryCode: String): String {
    return if (basePriceUsd > 0.0) {
      CountryCatalog.formatUsdPriceForCountry(basePriceUsd, countryCode)
    } else {
      priceLabel
    }
  }
}

object RoyalStoreCatalog {
  val IAP_BUNDLES: List<StoreItem> = listOf(
    StoreItem(
      id = "iap_pouch_500",
      title = "Squire's Coin Pouch",
      subtitle = "+500 Royal Coins & +50 XP",
      type = StoreItemType.IAP_COIN_BUNDLE,
      priceLabel = "$0.99",
      basePriceUsd = 0.99,
      coinsGranted = 500L,
      bonusXp = 50L,
      badgeTag = "STARTER"
    ),
    StoreItem(
      id = "iap_chest_2000",
      title = "Archon's Brass Chest",
      subtitle = "+2,000 Royal Coins & +250 XP",
      type = StoreItemType.IAP_COIN_BUNDLE,
      priceLabel = "$2.99",
      basePriceUsd = 2.99,
      coinsGranted = 2000L,
      bonusXp = 250L,
      badgeTag = "POPULAR"
    ),
    StoreItem(
      id = "iap_vault_6500",
      title = "Sovereign Treasury Vault",
      subtitle = "+6,500 Royal Coins, +800 XP & VIP Crown",
      type = StoreItemType.IAP_COIN_BUNDLE,
      priceLabel = "$7.99",
      basePriceUsd = 7.99,
      coinsGranted = 6500L,
      bonusXp = 800L,
      badgeTag = "BEST VALUE"
    )
  )

  val COSMETIC_ITEMS: List<StoreItem> = listOf(
    StoreItem(
      id = "cosmetic_brass_dice",
      title = "Gilded Brass 3D Dice",
      subtitle = "Hand-crafted architectural brass dice finish",
      type = StoreItemType.COSMETIC_UNLOCK,
      coinCost = 300L,
      bonusXp = 60L,
      badgeTag = "3D DICE"
    ),
    StoreItem(
      id = "cosmetic_marble_pawns",
      title = "Carrara Marble Pawns",
      subtitle = "Polished Italian travertine & marble pawn sheen",
      type = StoreItemType.COSMETIC_UNLOCK,
      coinCost = 450L,
      bonusXp = 90L,
      badgeTag = "3D PAWNS"
    ),
    StoreItem(
      id = "cosmetic_royal_fireworks",
      title = "Imperial Gold Fireworks",
      subtitle = "Enhanced golden particle trails on capture & victory",
      type = StoreItemType.COSMETIC_UNLOCK,
      coinCost = 600L,
      bonusXp = 120L,
      badgeTag = "FX PACK"
    ),
    StoreItem(
      id = "cosmetic_vip_patron",
      title = "Royal Patron Crest",
      subtitle = "Exclusive sovereign emblem on your profile card",
      type = StoreItemType.COSMETIC_UNLOCK,
      coinCost = 1000L,
      bonusXp = 250L,
      badgeTag = "VIP CREST"
    )
  )
}

@IgnoreExtraProperties
data class AdminAppConfig(
  @DocumentId val id: String = "global",
  val updatedByUid: String = "",
  val ownerEmail: String = "farhanSajid@gmail.com",
  val customFirebaseDatabaseId: String = "ai-studio-android-royallud-d389e848-893a-41af-b823-e38eded48b8c",
  val turnDurationSeconds: Long = 22L,
  val winPoints: Long = 100L,
  val capturePoints: Long = 25L,
  val homePoints: Long = 15L,
  val missionRewardMultiplier: Long = 1L,
  val announcementBanner: String = "Welcome to Royal Ludo 3D Studio!",
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
  val tokens: List<Long> = List(32) { -1L },
  val currentTurnColor: String = PlayerColor.RED.name,
  val turnPhase: String = TurnPhase.ROLL.name,
  val diceValue: Long = 0L,
  val diceValue2: Long = 0L,
  val dice1Used: Boolean = false,
  val dice2Used: Boolean = false,
  val selectedDieIndex: Long = 0L,
  val consecutiveSixes: Long = 0L,
  val turnNumber: Long = 1L,
  val lastEvent: String = "Waiting for players",
  val reactionEmoji: String = "",
  val reactionSender: String = "",
  val winnerColor: String = "NONE",
  val winnerOrder: List<String> = emptyList(),
  val activeVoiceUids: List<String> = emptyList(),
  val callMode: String = AudioCallMode.NONE.name,
  val callInitiatorUid: String = "",
  val callTargetUid: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  val tokenPositions: List<Int>
    get() = if (tokens.size in setOf(16, 24, 32)) {
      val list = tokens.map { (it as Number).toInt() }
      if (list.size < 32) list + List(32 - list.size) { -1 } else list
    } else {
      List(32) { -1 }
    }

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

@IgnoreExtraProperties
data class VoiceSignal(
  @DocumentId val id: String = "",
  val senderUid: String = "",
  val senderName: String = "",
  val targetUid: String = "ALL",
  val signalType: String = "AUDIO_FRAME",
  val payload: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)

enum class ReplayCameraPreset(
  val id: String,
  val label: String,
  val subtitle: String,
  val yaw: Float,
  val pitch: Float,
  val zoom: Float,
  val autoFollowActiveColor: Boolean = false
) {
  ISOMETRIC_BROADCAST(
    id = "isometric",
    label = "Isometric 3D",
    subtitle = "Balanced 52° broadcast tabletop view",
    yaw = -28f,
    pitch = 52f,
    zoom = 1.04f
  ),
  TOP_DOWN_TACTICAL(
    id = "top_down",
    label = "Top-Down 90°",
    subtitle = "Overhead strategy & blockade map",
    yaw = 0f,
    pitch = 81f,
    zoom = 1.08f
  ),
  LOW_PAWN_POV(
    id = "low_pov",
    label = "Pawn Eye-Level",
    subtitle = "Dramatic 28° low-angle horizon view",
    yaw = -42f,
    pitch = 28f,
    zoom = 1.18f
  ),
  FOLLOW_ACTIVE_TURN(
    id = "follow_turn",
    label = "Auto-Follow Turn",
    subtitle = "Orbits dynamically to the moving player's seat",
    yaw = -35f,
    pitch = 48f,
    zoom = 1.06f,
    autoFollowActiveColor = true
  ),
  SOUTH_SEAT_POV(
    id = "south_seat",
    label = "South Seat POV",
    subtitle = "Red / Primary player's home corner angle",
    yaw = -35f,
    pitch = 46f,
    zoom = 1.05f
  ),
  WEST_SEAT_POV(
    id = "west_seat",
    label = "West Seat POV",
    subtitle = "Green / Left flank tactical angle",
    yaw = -125f,
    pitch = 46f,
    zoom = 1.05f
  ),
  NORTH_SEAT_POV(
    id = "north_seat",
    label = "North Seat POV",
    subtitle = "Yellow / Opposite table perspective",
    yaw = 145f,
    pitch = 46f,
    zoom = 1.05f
  ),
  EAST_SEAT_POV(
    id = "east_seat",
    label = "East Seat POV",
    subtitle = "Blue / Right flank tactical angle",
    yaw = 55f,
    pitch = 46f,
    zoom = 1.05f
  )
}

data class ReplayFrame(
  val frameIndex: Int,
  val turnNumber: Int,
  val actorColor: PlayerColor,
  val actorName: String,
  val diceValue: Int,
  val diceValue2: Int,
  val usedDieValue: Int,
  val movedTokenIndex: Int = -1,
  val fromStep: Int = -1,
  val toStep: Int = -1,
  val tokens: List<Int>,
  val eventSummary: String,
  val tacticalNote: String,
  val capturedCount: Int = 0,
  val formedBlockade: Boolean = false,
  val reachedHome: Boolean = false
)

data class MatchReplayRecord(
  val id: String,
  val title: String,
  val subtitle: String,
  val isOnlineMatch: Boolean,
  val maxPlayers: Int,
  val seats: List<PlayerSeat>,
  val frames: List<ReplayFrame>,
  val winnerColor: String,
  val recordedAtLabel: String,
  val totalCaptures: Int,
  val totalBlockades: Int
)

object SampleReplayCatalog {
  private fun tokensWith(overrides: Map<Int, Int>): List<Int> {
    return List(32) { idx -> overrides[idx] ?: -1 }
  }

  val SHOWCASE_REPLAYS: List<MatchReplayRecord> = listOf(
    MatchReplayRecord(
      id = "showcase_4p_tactical_trap",
      title = "4P Grandmaster: Blockade & Pincher Trap",
      subtitle = "Study how Terracotta Red uses dual-dice splits to form a safe-star blockade and capture Cobalt Blue",
      isOnlineMatch = false,
      maxPlayers = 4,
      seats = listOf(
        PlayerSeat(color = PlayerColor.RED, uid = "P1", name = "Archon Red", isBot = false),
        PlayerSeat(color = PlayerColor.GREEN, uid = "P2", name = "Sage AI", isBot = true),
        PlayerSeat(color = PlayerColor.YELLOW, uid = "P3", name = "Ochre AI", isBot = true),
        PlayerSeat(color = PlayerColor.BLUE, uid = "P4", name = "Cobalt AI", isBot = true)
      ),
      winnerColor = PlayerColor.RED.name,
      recordedAtLabel = "Pro Strategy Study #1",
      totalCaptures = 2,
      totalBlockades = 2,
      frames = listOf(
        ReplayFrame(
          frameIndex = 0,
          turnNumber = 1,
          actorColor = PlayerColor.RED,
          actorName = "Archon Red",
          diceValue = 6,
          diceValue2 = 4,
          usedDieValue = 0,
          tokens = tokensWith(emptyMap()),
          eventSummary = "Match Kickoff — All 16 pawns in starting yards",
          tacticalNote = "Opening tip: Prioritize rolling a 6 to deploy at least two runners early."
        ),
        ReplayFrame(
          frameIndex = 1,
          turnNumber = 1,
          actorColor = PlayerColor.RED,
          actorName = "Archon Red",
          diceValue = 6,
          diceValue2 = 4,
          usedDieValue = 6,
          movedTokenIndex = 0,
          fromStep = -1,
          toStep = 0,
          tokens = tokensWith(mapOf(0 to 0)),
          eventSummary = "Archon Red rolled [6, 4] and deployed Pawn #1 from yard",
          tacticalNote = "Yard Exit (+6): Opens the primary Red track at step 0 (Safe Star)."
        ),
        ReplayFrame(
          frameIndex = 2,
          turnNumber = 1,
          actorColor = PlayerColor.RED,
          actorName = "Archon Red",
          diceValue = 6,
          diceValue2 = 4,
          usedDieValue = 4,
          movedTokenIndex = 0,
          fromStep = 0,
          toStep = 4,
          tokens = tokensWith(mapOf(0 to 4)),
          eventSummary = "Archon Red used 2nd die (+4) to advance Pawn #1 to step 4",
          tacticalNote = "Sequential 2nd Die: Advancing within striking distance of the star tile at step 8."
        ),
        ReplayFrame(
          frameIndex = 3,
          turnNumber = 2,
          actorColor = PlayerColor.RED,
          actorName = "Archon Red",
          diceValue = 6,
          diceValue2 = 4,
          usedDieValue = 6,
          movedTokenIndex = 1,
          fromStep = -1,
          toStep = 0,
          tokens = tokensWith(mapOf(0 to 8, 1 to 0, 4 to 2, 8 to 5, 12 to 1)),
          eventSummary = "Bonus Roll! Archon Red deployed 2nd pawn & secured Safe Star at step 8",
          tacticalNote = "Anchor Strategy: Securing the step-8 star creates an uncapturable forward outpost."
        ),
        ReplayFrame(
          frameIndex = 4,
          turnNumber = 4,
          actorColor = PlayerColor.GREEN,
          actorName = "Sage AI",
          diceValue = 6,
          diceValue2 = 5,
          usedDieValue = 5,
          movedTokenIndex = 4,
          fromStep = 2,
          toStep = 7,
          tokens = tokensWith(mapOf(0 to 8, 1 to 8, 4 to 7, 5 to 0, 8 to 11, 12 to 6)),
          eventSummary = "Archon Red formed a Twin Blockade Shield at step 8!",
          tacticalNote = "Blockade Shield: Two Red pawns stacked at step 8 block opponents from passing.",
          formedBlockade = true
        ),
        ReplayFrame(
          frameIndex = 5,
          turnNumber = 6,
          actorColor = PlayerColor.RED,
          actorName = "Archon Red",
          diceValue = 6,
          diceValue2 = 6,
          usedDieValue = 6,
          movedTokenIndex = 0,
          fromStep = 8,
          toStep = 14,
          tokens = tokensWith(mapOf(0 to 14, 1 to 8, 2 to 0, 4 to 10, 8 to 16, 12 to 12)),
          eventSummary = "Double 6s! Archon Red split the blockade to ambush Sage AI at step 14!",
          tacticalNote = "Tactical Ambush (+25 pts): Breaking a blockade with high dice to capture an exposed runner.",
          capturedCount = 1
        ),
        ReplayFrame(
          frameIndex = 6,
          turnNumber = 9,
          actorColor = PlayerColor.BLUE,
          actorName = "Cobalt AI",
          diceValue = 5,
          diceValue2 = 3,
          usedDieValue = 5,
          movedTokenIndex = 12,
          fromStep = 12,
          toStep = 20,
          tokens = tokensWith(mapOf(0 to 27, 1 to 21, 2 to 8, 4 to 0, 8 to 24, 12 to 20)),
          eventSummary = "Cobalt AI pushed deep into the Northern quadrant (step 20)",
          tacticalNote = "Camera Tip: Switch to 'North Seat POV' or 'Top-Down 90°' to inspect quadrant spacing."
        ),
        ReplayFrame(
          frameIndex = 7,
          turnNumber = 12,
          actorColor = PlayerColor.RED,
          actorName = "Archon Red",
          diceValue = 6,
          diceValue2 = 5,
          usedDieValue = 5,
          movedTokenIndex = 0,
          fromStep = 34,
          toStep = 39,
          tokens = tokensWith(mapOf(0 to 39, 1 to 39, 2 to 22, 3 to 13, 8 to 31, 12 to 28)),
          eventSummary = "Archon Red captured Cobalt AI on the Eastern flank and formed a 2nd blockade!",
          tacticalNote = "Pincher Combo: Capturing + stacking on the same turn denies counter-attacks.",
          capturedCount = 1,
          formedBlockade = true
        ),
        ReplayFrame(
          frameIndex = 8,
          turnNumber = 16,
          actorColor = PlayerColor.RED,
          actorName = "Archon Red",
          diceValue = 6,
          diceValue2 = 3,
          usedDieValue = 3,
          movedTokenIndex = 0,
          fromStep = 53,
          toStep = 56,
          tokens = tokensWith(mapOf(0 to 56, 1 to 56, 2 to 54, 3 to 52, 8 to 44, 12 to 35)),
          eventSummary = "Archon Red's lead pawns entered the crimson home lane & reached Center Home!",
          tacticalNote = "Endgame Precision: Save small die values (1–3) for exact home-triangle entry.",
          reachedHome = true
        ),
        ReplayFrame(
          frameIndex = 9,
          turnNumber = 19,
          actorColor = PlayerColor.RED,
          actorName = "Archon Red",
          diceValue = 2,
          diceValue2 = 4,
          usedDieValue = 4,
          movedTokenIndex = 3,
          fromStep = 52,
          toStep = 56,
          tokens = tokensWith(mapOf(0 to 56, 1 to 56, 2 to 56, 3 to 56, 8 to 49, 12 to 41)),
          eventSummary = "Victory! Archon Red brought all 4 pawns home to claim the championship!",
          tacticalNote = "Full Sweep (+100 Win Pts): Zero Red pawns lost thanks to Safe-Star discipline.",
          reachedHome = true
        )
      )
    ),
    MatchReplayRecord(
      id = "showcase_6p_hex_clash",
      title = "6P Hexagonal Blitz: Cross-Board Siege",
      subtitle = "6-Arm radial board study featuring Amethyst Purple & Royal Teal multi-lane maneuvers",
      isOnlineMatch = true,
      maxPlayers = 6,
      seats = listOf(
        PlayerSeat(color = PlayerColor.RED, uid = "U1", name = "Sultan Red", isBot = false),
        PlayerSeat(color = PlayerColor.GREEN, uid = "U2", name = "Emerald Knight", isBot = true),
        PlayerSeat(color = PlayerColor.YELLOW, uid = "U3", name = "Solar Monarch", isBot = true),
        PlayerSeat(color = PlayerColor.BLUE, uid = "U4", name = "Azure Duke", isBot = true),
        PlayerSeat(color = PlayerColor.PURPLE, uid = "U5", name = "Amethyst Queen", isBot = true),
        PlayerSeat(color = PlayerColor.TEAL, uid = "U6", name = "Teal Tactician", isBot = true)
      ),
      winnerColor = PlayerColor.PURPLE.name,
      recordedAtLabel = "Pro Strategy Study #2",
      totalCaptures = 3,
      totalBlockades = 1,
      frames = listOf(
        ReplayFrame(
          frameIndex = 0,
          turnNumber = 1,
          actorColor = PlayerColor.RED,
          actorName = "Sultan Red",
          diceValue = 6,
          diceValue2 = 5,
          usedDieValue = 6,
          movedTokenIndex = 0,
          fromStep = -1,
          toStep = 0,
          tokens = tokensWith(mapOf(0 to 0, 4 to 0, 8 to 0, 12 to 0, 16 to 0, 20 to 0)),
          eventSummary = "6-Player Hex Arena — All 6 royal houses deploy opening runners",
          tacticalNote = "6P Geometry: With 78 outer track steps, safe stars every 13 steps are vital."
        ),
        ReplayFrame(
          frameIndex = 1,
          turnNumber = 5,
          actorColor = PlayerColor.PURPLE,
          actorName = "Amethyst Queen",
          diceValue = 6,
          diceValue2 = 4,
          usedDieValue = 6,
          movedTokenIndex = 16,
          fromStep = 8,
          toStep = 14,
          tokens = tokensWith(mapOf(0 to 11, 4 to 8, 8 to 14, 12 to 6, 16 to 14, 17 to 8, 20 to 13)),
          eventSummary = "Amethyst Queen surged across Arm 5 to capture Solar Monarch!",
          tacticalNote = "Radial Sightlines: Use 'Auto-Follow Turn' camera to track 6-arm intersections.",
          capturedCount = 1
        ),
        ReplayFrame(
          frameIndex = 2,
          turnNumber = 9,
          actorColor = PlayerColor.TEAL,
          actorName = "Teal Tactician",
          diceValue = 5,
          diceValue2 = 5,
          usedDieValue = 5,
          movedTokenIndex = 20,
          fromStep = 13,
          toStep = 18,
          tokens = tokensWith(mapOf(0 to 21, 4 to 16, 12 to 19, 16 to 29, 17 to 29, 20 to 18, 21 to 18)),
          eventSummary = "Amethyst Queen & Teal Tactician both established mid-board blockades",
          tacticalNote = "Dual Blockade Standoff: High traffic in 6P games makes safe-tile stacking essential.",
          formedBlockade = true
        ),
        ReplayFrame(
          frameIndex = 3,
          turnNumber = 14,
          actorColor = PlayerColor.PURPLE,
          actorName = "Amethyst Queen",
          diceValue = 6,
          diceValue2 = 5,
          usedDieValue = 6,
          movedTokenIndex = 16,
          fromStep = 60,
          toStep = 66,
          tokens = tokensWith(mapOf(0 to 48, 4 to 39, 12 to 44, 16 to 66, 17 to 61, 18 to 52, 20 to 41)),
          eventSummary = "Amethyst Queen executed a double capture before entering the Purple home stretch!",
          tacticalNote = "Momentum Swing: Two captures in a single round grant massive rank score bonuses.",
          capturedCount = 2
        ),
        ReplayFrame(
          frameIndex = 4,
          turnNumber = 18,
          actorColor = PlayerColor.PURPLE,
          actorName = "Amethyst Queen",
          diceValue = 4,
          diceValue2 = 2,
          usedDieValue = 4,
          movedTokenIndex = 19,
          fromStep = 78,
          toStep = 82,
          tokens = tokensWith(mapOf(0 to 65, 4 to 54, 12 to 58, 16 to 82, 17 to 82, 18 to 82, 19 to 82, 20 to 63)),
          eventSummary = "Amethyst Queen completed all 4 home goals on the 6P Hex Board!",
          tacticalNote = "Hex Victory: Use 'Pawn Eye-Level' camera to inspect the central 6-arm home crown.",
          reachedHome = true
        )
      )
    )
  )
}

enum class EmoteMomentTrigger(
  val id: String,
  val label: String,
  val shortTag: String,
  val description: String
) {
  YARD_OPEN(
    id = "yard_open",
    label = "Yard Exit (Roll 6)",
    shortTag = "ON OPEN (6)",
    description = "Appears above your pawn when deploying from the yard with a 6"
  ),
  OPPONENT_CAPTURE(
    id = "opponent_capture",
    label = "Opponent Capture",
    shortTag = "ON CAPTURE",
    description = "Appears above your pawn when eliminating a rival pawn"
  ),
  BLOCKADE_SHIELD(
    id = "blockade_shield",
    label = "Blockade Formation",
    shortTag = "ON BLOCKADE",
    description = "Appears above your twin stacked pawns when forming a blockade"
  ),
  SAFE_STAR(
    id = "safe_star",
    label = "Safe Star Landing",
    shortTag = "ON SAFE STAR",
    description = "Appears above your pawn when anchoring on a protected star tile"
  ),
  HOME_GOAL(
    id = "home_goal",
    label = "Home Goal & Victory",
    shortTag = "ON HOME / WIN",
    description = "Appears above your pawn when entering the central home triangle"
  )
}

enum class EmoteCategory(val label: String) {
  ANIMATION_3D("3D Animation"),
  VISUAL_STICKER("Visual Sticker")
}

enum class PawnEmoteVisualStyle {
  SOVEREIGN_CROWN_3D,
  THUNDER_STRIKE_3D,
  AEGIS_FORTRESS_3D,
  SUPERNOVA_STAR_3D,
  CELESTIAL_HELIX_3D,
  DRAGON_FLAME_3D,
  STICKER_DEAL_WITH_IT,
  STICKER_ROYAL_SALUTE,
  STICKER_CALCULATED,
  STICKER_SHIELD_WALL
}

data class PawnEmoteItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val category: EmoteCategory,
  val visualStyle: PawnEmoteVisualStyle,
  val recommendedTrigger: EmoteMomentTrigger,
  val stickerSymbol: String,
  val calloutText: String,
  val coinCost: Long,
  val bonusXp: Long,
  val rarityLabel: String,
  val unlockedByDefault: Boolean = false
)

data class ActivePawnEmoteEffect(
  val tokenIndex: Int,
  val pawnColor: PlayerColor,
  val emote: PawnEmoteItem,
  val triggerMoment: EmoteMomentTrigger,
  val actorName: String,
  val createdAtMillis: Long = System.currentTimeMillis()
)

object EmoteGalleryCatalog {
  val ALL_EMOTES: List<PawnEmoteItem> = listOf(
    PawnEmoteItem(
      id = "emote_3d_sovereign_crown",
      title = "Sovereign 3D Crown",
      subtitle = "Levitating gilded 3D crown with orbiting brass rings above your pawn",
      category = EmoteCategory.ANIMATION_3D,
      visualStyle = PawnEmoteVisualStyle.SOVEREIGN_CROWN_3D,
      recommendedTrigger = EmoteMomentTrigger.HOME_GOAL,
      stickerSymbol = "👑",
      calloutText = "ROYAL ASCENSION!",
      coinCost = 0L,
      bonusXp = 0L,
      rarityLabel = "STARTER",
      unlockedByDefault = true
    ),
    PawnEmoteItem(
      id = "emote_3d_supernova_star",
      title = "Astral Supernova 3D",
      subtitle = "Multi-axis 3D spinning star compass & golden energy pulse on safe tiles",
      category = EmoteCategory.ANIMATION_3D,
      visualStyle = PawnEmoteVisualStyle.SUPERNOVA_STAR_3D,
      recommendedTrigger = EmoteMomentTrigger.SAFE_STAR,
      stickerSymbol = "🌟",
      calloutText = "STAR SANCTUARY!",
      coinCost = 0L,
      bonusXp = 0L,
      rarityLabel = "STARTER",
      unlockedByDefault = true
    ),
    PawnEmoteItem(
      id = "emote_sticker_royal_salute",
      title = "Royal Salute Sticker",
      subtitle = "Holographic floating crest sticker when deploying a runner on a 6",
      category = EmoteCategory.VISUAL_STICKER,
      visualStyle = PawnEmoteVisualStyle.STICKER_ROYAL_SALUTE,
      recommendedTrigger = EmoteMomentTrigger.YARD_OPEN,
      stickerSymbol = "🫡",
      calloutText = "REPORTING FOR DUTY!",
      coinCost = 0L,
      bonusXp = 0L,
      rarityLabel = "STARTER",
      unlockedByDefault = true
    ),
    PawnEmoteItem(
      id = "emote_3d_thunder_strike",
      title = "Thunderbolt Ambush 3D",
      subtitle = "High-voltage 3D plasma lightning bolts & shockwave rings on pawn capture",
      category = EmoteCategory.ANIMATION_3D,
      visualStyle = PawnEmoteVisualStyle.THUNDER_STRIKE_3D,
      recommendedTrigger = EmoteMomentTrigger.OPPONENT_CAPTURE,
      stickerSymbol = "⚡",
      calloutText = "THUNDER AMBUSH!",
      coinCost = 150L,
      bonusXp = 45L,
      rarityLabel = "RARE"
    ),
    PawnEmoteItem(
      id = "emote_3d_aegis_fortress",
      title = "Aegis Bastion 3D",
      subtitle = "Rotating 3D hexagonal forcefield dome above twin blockade pawns",
      category = EmoteCategory.ANIMATION_3D,
      visualStyle = PawnEmoteVisualStyle.AEGIS_FORTRESS_3D,
      recommendedTrigger = EmoteMomentTrigger.BLOCKADE_SHIELD,
      stickerSymbol = "🛡️",
      calloutText = "IMPENETRABLE!",
      coinCost = 200L,
      bonusXp = 60L,
      rarityLabel = "RARE"
    ),
    PawnEmoteItem(
      id = "emote_sticker_deal_with_it",
      title = "Sunglasses Flex Sticker",
      subtitle = "Floating 'DEAL WITH IT' obsidian & brass visor badge after a capture",
      category = EmoteCategory.VISUAL_STICKER,
      visualStyle = PawnEmoteVisualStyle.STICKER_DEAL_WITH_IT,
      recommendedTrigger = EmoteMomentTrigger.OPPONENT_CAPTURE,
      stickerSymbol = "😎",
      calloutText = "DEAL WITH IT!",
      coinCost = 180L,
      bonusXp = 50L,
      rarityLabel = "RARE"
    ),
    PawnEmoteItem(
      id = "emote_sticker_calculated",
      title = "200 IQ Geometry Sticker",
      subtitle = "Floating architectural compass & '200 IQ MOVE' tactical sticker",
      category = EmoteCategory.VISUAL_STICKER,
      visualStyle = PawnEmoteVisualStyle.STICKER_CALCULATED,
      recommendedTrigger = EmoteMomentTrigger.SAFE_STAR,
      stickerSymbol = "📐",
      calloutText = "200 IQ CALCULATED!",
      coinCost = 220L,
      bonusXp = 65L,
      rarityLabel = "EPIC"
    ),
    PawnEmoteItem(
      id = "emote_3d_dragon_flame",
      title = "Crimson Vortex 3D",
      subtitle = "Ascending 3D fire tornado & ember rings when charging out of the yard",
      category = EmoteCategory.ANIMATION_3D,
      visualStyle = PawnEmoteVisualStyle.DRAGON_FLAME_3D,
      recommendedTrigger = EmoteMomentTrigger.YARD_OPEN,
      stickerSymbol = "🔥",
      calloutText = "BLAZING VANGUARD!",
      coinCost = 280L,
      bonusXp = 85L,
      rarityLabel = "EPIC"
    ),
    PawnEmoteItem(
      id = "emote_sticker_shield_wall",
      title = "Citadel Gate Sticker",
      subtitle = "Floating 'NONE SHALL PASS' fortress sticker above blockade stacks",
      category = EmoteCategory.VISUAL_STICKER,
      visualStyle = PawnEmoteVisualStyle.STICKER_SHIELD_WALL,
      recommendedTrigger = EmoteMomentTrigger.BLOCKADE_SHIELD,
      stickerSymbol = "🏰",
      calloutText = "NONE SHALL PASS!",
      coinCost = 240L,
      bonusXp = 70L,
      rarityLabel = "EPIC"
    ),
    PawnEmoteItem(
      id = "emote_3d_celestial_helix",
      title = "Celestial Helix 3D",
      subtitle = "Dual spiraling 3D orbital DNA & starlight pillars ascending to the heavens",
      category = EmoteCategory.ANIMATION_3D,
      visualStyle = PawnEmoteVisualStyle.CELESTIAL_HELIX_3D,
      recommendedTrigger = EmoteMomentTrigger.HOME_GOAL,
      stickerSymbol = "🌌",
      calloutText = "COSMIC SOVEREIGN!",
      coinCost = 400L,
      bonusXp = 120L,
      rarityLabel = "LEGENDARY"
    )
  )

  val DEFAULT_UNLOCKED_IDS: Set<String> = ALL_EMOTES
    .filter { it.unlockedByDefault }
    .map { it.id }
    .toSet()

  val DEFAULT_EQUIPPED_LOADOUT: Map<EmoteMomentTrigger, String> = mapOf(
    EmoteMomentTrigger.YARD_OPEN to "emote_sticker_royal_salute",
    EmoteMomentTrigger.OPPONENT_CAPTURE to "emote_3d_sovereign_crown",
    EmoteMomentTrigger.BLOCKADE_SHIELD to "emote_3d_supernova_star",
    EmoteMomentTrigger.SAFE_STAR to "emote_3d_supernova_star",
    EmoteMomentTrigger.HOME_GOAL to "emote_3d_sovereign_crown"
  )

  fun findById(id: String): PawnEmoteItem {
    return ALL_EMOTES.firstOrNull { it.id == id } ?: ALL_EMOTES.first()
  }
}

// --- WEEKLY TOURNAMENT LADDER BRACKETS & EXCLUSIVE COSMETIC REWARDS ---

enum class TournamentRoundTier(
  val roundIndex: Int,
  val title: String,
  val shortLabel: String,
  val matchFormatLabel: String
) {
  QUARTERFINALS(
    roundIndex = 0,
    title = "Quarterfinals",
    shortLabel = "QF",
    matchFormatLabel = "Round of 8 • Dual-Dice Knockout"
  ),
  SEMIFINALS(
    roundIndex = 1,
    title = "Semifinals",
    shortLabel = "SF",
    matchFormatLabel = "Final 4 • High-Stakes Studio Duel"
  ),
  GRAND_FINALS(
    roundIndex = 2,
    title = "Grand Finals",
    shortLabel = "GF",
    matchFormatLabel = "Championship Match • Sovereign Cup"
  ),
  CHAMPION(
    roundIndex = 3,
    title = "Sovereign Champion",
    shortLabel = "CHAMP",
    matchFormatLabel = "Undefeated Weekly Bracket Champion"
  )
}

data class TournamentCosmeticReward(
  val id: String,
  val title: String,
  val subtitle: String,
  val categoryLabel: String,
  val iconSymbol: String,
  val rarityLabel: String,
  val requiredRound: TournamentRoundTier,
  val winsRequired: Int,
  val bonusCoins: Long,
  val bonusXp: Long
)

data class TournamentBracketMatch(
  val matchId: String,
  val roundTier: TournamentRoundTier,
  val slotIndex: Int,
  val player1Name: String,
  val player1Country: String,
  val player1Color: PlayerColor,
  val player1Seed: Int,
  val player1Score: Int,
  val player2Name: String,
  val player2Country: String,
  val player2Color: PlayerColor,
  val player2Seed: Int,
  val player2Score: Int,
  val winnerName: String? = null,
  val isPlayerMatch: Boolean = false,
  val isLive: Boolean = false,
  val liveStatusText: String = ""
)

data class TournamentCountdownParts(
  val days: Long,
  val hours: Long,
  val minutes: Long,
  val seconds: Long
)

object WeeklyTournamentCatalog {
  const val CURRENT_SEASON_TITLE = "Sovereign Cup • Weekly Ladder"
  const val DEFAULT_REMAINING_SECONDS = 3L * 86_400L + 14L * 3_600L + 28L * 60L + 42L

  val EXCLUSIVE_REWARDS: List<TournamentCosmeticReward> = listOf(
    TournamentCosmeticReward(
      id = "tourney_cosmetic_contender_banner",
      title = "Quarterfinalist Brass Crest",
      subtitle = "Exclusive gilded bracket qualifier emblem & tabletop trim",
      categoryLabel = "BRACKET EMBLEM",
      iconSymbol = "🛡️",
      rarityLabel = "TOURNAMENT RARE",
      requiredRound = TournamentRoundTier.QUARTERFINALS,
      winsRequired = 0,
      bonusCoins = 100L,
      bonusXp = 50L
    ),
    TournamentCosmeticReward(
      id = "tourney_cosmetic_semifinal_dice",
      title = "Semifinalist Chronos Dice",
      subtitle = "Precision clockwork brass & emerald dual-dice skin with golden pips",
      categoryLabel = "EXCLUSIVE 3D DICE",
      iconSymbol = "🎲",
      rarityLabel = "TOURNAMENT EPIC",
      requiredRound = TournamentRoundTier.SEMIFINALS,
      winsRequired = 1,
      bonusCoins = 250L,
      bonusXp = 120L
    ),
    TournamentCosmeticReward(
      id = "tourney_cosmetic_finalist_aura",
      title = "Grand Finalist Astral Trail",
      subtitle = "Radiant starlight halo & shockwave trail beneath all 4 runners",
      categoryLabel = "3D PAWN TRAIL",
      iconSymbol = "✨",
      rarityLabel = "TOURNAMENT MYTHIC",
      requiredRound = TournamentRoundTier.GRAND_FINALS,
      winsRequired = 2,
      bonusCoins = 500L,
      bonusXp = 250L
    ),
    TournamentCosmeticReward(
      id = "tourney_cosmetic_champion_crown_board",
      title = "Sovereign Champion Crown & Board",
      subtitle = "Weekly Champion's obsidian-gold 3D arena inlay & royal sovereign crown",
      categoryLabel = "CHAMPIONSHIP TROPHY",
      iconSymbol = "👑",
      rarityLabel = "WEEKLY EXCLUSIVE",
      requiredRound = TournamentRoundTier.CHAMPION,
      winsRequired = 3,
      bonusCoins = 1000L,
      bonusXp = 500L
    )
  )

  fun splitRemainingTime(totalSeconds: Long): TournamentCountdownParts {
    val clamped = totalSeconds.coerceAtLeast(0L)
    val days = clamped / 86_400L
    val hours = (clamped % 86_400L) / 3_600L
    val minutes = (clamped % 3_600L) / 60L
    val seconds = clamped % 60L
    return TournamentCountdownParts(days, hours, minutes, seconds)
  }

  fun formatRemainingTime(totalSeconds: Long): String {
    val parts = splitRemainingTime(totalSeconds)
    return "%02dd %02dh %02dm %02ds".format(parts.days, parts.hours, parts.minutes, parts.seconds)
  }

  fun buildInitialBracket(
    playerName: String = "You",
    playerCountry: String = "US",
    playerColor: PlayerColor = PlayerColor.RED
  ): List<TournamentBracketMatch> {
    val cleanName = playerName.ifBlank { "You" }
    return listOf(
      // Quarterfinals (4 matches, 8 seeded contenders)
      TournamentBracketMatch(
        matchId = "QF-1",
        roundTier = TournamentRoundTier.QUARTERFINALS,
        slotIndex = 0,
        player1Name = cleanName,
        player1Country = playerCountry,
        player1Color = playerColor,
        player1Seed = 1,
        player1Score = 0,
        player2Name = "Grandmaster Vance",
        player2Country = "GB",
        player2Color = if (playerColor == PlayerColor.GREEN) PlayerColor.BLUE else PlayerColor.GREEN,
        player2Seed = 8,
        player2Score = 0,
        winnerName = null,
        isPlayerMatch = true,
        isLive = true,
        liveStatusText = "Ready for Kickoff • Your Quarterfinal Duel"
      ),
      TournamentBracketMatch(
        matchId = "QF-2",
        roundTier = TournamentRoundTier.QUARTERFINALS,
        slotIndex = 1,
        player1Name = "Duchess Katerina",
        player1Country = "DE",
        player1Color = PlayerColor.YELLOW,
        player1Seed = 4,
        player1Score = 95,
        player2Name = "Kenji Takahashi",
        player2Country = "JP",
        player2Color = PlayerColor.PURPLE,
        player2Seed = 5,
        player2Score = 80,
        winnerName = null,
        isPlayerMatch = false,
        isLive = true,
        liveStatusText = "LIVE • Turn 14: Katerina leads 3-2 in Home Pawns"
      ),
      TournamentBracketMatch(
        matchId = "QF-3",
        roundTier = TournamentRoundTier.QUARTERFINALS,
        slotIndex = 2,
        player1Name = "Aarav Sharma",
        player1Country = "IN",
        player1Color = PlayerColor.ORANGE,
        player1Seed = 2,
        player1Score = 140,
        player2Name = "Lucas Oliveira",
        player2Country = "BR",
        player2Color = PlayerColor.TEAL,
        player2Seed = 7,
        player2Score = 105,
        winnerName = "Aarav Sharma",
        isPlayerMatch = false,
        isLive = false,
        liveStatusText = "FINAL • Aarav Sharma won via Double-6 Home Sprint"
      ),
      TournamentBracketMatch(
        matchId = "QF-4",
        roundTier = TournamentRoundTier.QUARTERFINALS,
        slotIndex = 3,
        player1Name = "Tariq Al-Mansoor",
        player1Country = "AE",
        player1Color = PlayerColor.BLUE,
        player1Seed = 3,
        player1Score = 110,
        player2Name = "Chloe Laurent",
        player2Country = "FR",
        player2Color = PlayerColor.ROSE,
        player2Seed = 6,
        player2Score = 90,
        winnerName = null,
        isPlayerMatch = false,
        isLive = true,
        liveStatusText = "LIVE • Turn 18: Tariq formed a central blockade"
      ),
      // Semifinals (2 matches)
      TournamentBracketMatch(
        matchId = "SF-1",
        roundTier = TournamentRoundTier.SEMIFINALS,
        slotIndex = 0,
        player1Name = "Winner of QF-1",
        player1Country = playerCountry,
        player1Color = playerColor,
        player1Seed = 1,
        player1Score = 0,
        player2Name = "Duchess Katerina",
        player2Country = "DE",
        player2Color = PlayerColor.YELLOW,
        player2Seed = 4,
        player2Score = 0,
        winnerName = null,
        isPlayerMatch = false,
        isLive = false,
        liveStatusText = "Awaiting Quarterfinal #1 & #2 Victors"
      ),
      TournamentBracketMatch(
        matchId = "SF-2",
        roundTier = TournamentRoundTier.SEMIFINALS,
        slotIndex = 1,
        player1Name = "Aarav Sharma",
        player1Country = "IN",
        player1Color = PlayerColor.ORANGE,
        player1Seed = 2,
        player1Score = 115,
        player2Name = "Tariq Al-Mansoor",
        player2Country = "AE",
        player2Color = PlayerColor.BLUE,
        player2Seed = 3,
        player2Score = 95,
        winnerName = null,
        isPlayerMatch = false,
        isLive = true,
        liveStatusText = "LIVE • Semifinal B: Aarav & Tariq trading captures"
      ),
      // Grand Finals (1 championship match)
      TournamentBracketMatch(
        matchId = "GF-1",
        roundTier = TournamentRoundTier.GRAND_FINALS,
        slotIndex = 0,
        player1Name = "Semifinal 1 Victor",
        player1Country = playerCountry,
        player1Color = playerColor,
        player1Seed = 1,
        player1Score = 0,
        player2Name = "Aarav Sharma",
        player2Country = "IN",
        player2Color = PlayerColor.ORANGE,
        player2Seed = 2,
        player2Score = 0,
        winnerName = null,
        isPlayerMatch = false,
        isLive = false,
        liveStatusText = "Sovereign Cup Championship Table"
      )
    )
  }

  fun advanceBracketOnPlayerWin(
    matches: List<TournamentBracketMatch>,
    currentRound: TournamentRoundTier,
    playerName: String,
    playerCountry: String,
    playerColor: PlayerColor,
    playerScore: Int = 160,
    opponentScore: Int = 85
  ): Pair<List<TournamentBracketMatch>, TournamentRoundTier> {
    val cleanName = playerName.ifBlank { "You" }
    val nextRound = when (currentRound) {
      TournamentRoundTier.QUARTERFINALS -> TournamentRoundTier.SEMIFINALS
      TournamentRoundTier.SEMIFINALS -> TournamentRoundTier.GRAND_FINALS
      TournamentRoundTier.GRAND_FINALS -> TournamentRoundTier.CHAMPION
      TournamentRoundTier.CHAMPION -> TournamentRoundTier.CHAMPION
    }

    val updated = matches.map { match ->
      when {
        currentRound == TournamentRoundTier.QUARTERFINALS && match.matchId == "QF-1" -> {
          match.copy(
            player1Name = cleanName,
            player1Country = playerCountry,
            player1Color = playerColor,
            player1Score = playerScore,
            player2Score = opponentScore,
            winnerName = cleanName,
            isLive = false,
            liveStatusText = "VICTORY • $cleanName advanced to Semifinals!"
          )
        }
        currentRound == TournamentRoundTier.QUARTERFINALS && match.matchId == "QF-2" -> {
          match.copy(
            player1Score = 150,
            player2Score = 110,
            winnerName = match.player1Name,
            isLive = false,
            liveStatusText = "FINAL • ${match.player1Name} advanced to Semifinals"
          )
        }
        currentRound == TournamentRoundTier.QUARTERFINALS && match.matchId == "SF-1" -> {
          match.copy(
            player1Name = cleanName,
            player1Country = playerCountry,
            player1Color = playerColor,
            player2Name = "Duchess Katerina",
            player2Country = "DE",
            player2Color = if (playerColor == PlayerColor.YELLOW) PlayerColor.BLUE else PlayerColor.YELLOW,
            isPlayerMatch = true,
            isLive = true,
            liveStatusText = "Ready for Kickoff • Your Semifinal Showdown"
          )
        }
        currentRound == TournamentRoundTier.SEMIFINALS && match.matchId == "SF-1" -> {
          match.copy(
            player1Name = cleanName,
            player1Country = playerCountry,
            player1Color = playerColor,
            player1Score = playerScore,
            player2Score = opponentScore,
            winnerName = cleanName,
            isLive = false,
            liveStatusText = "VICTORY • $cleanName clinched Grand Finals berth!"
          )
        }
        currentRound == TournamentRoundTier.SEMIFINALS && match.matchId == "SF-2" -> {
          match.copy(
            player1Score = 165,
            player2Score = 130,
            winnerName = match.player1Name,
            isLive = false,
            liveStatusText = "FINAL • ${match.player1Name} advanced to Grand Finals"
          )
        }
        currentRound == TournamentRoundTier.SEMIFINALS && match.matchId == "GF-1" -> {
          match.copy(
            player1Name = cleanName,
            player1Country = playerCountry,
            player1Color = playerColor,
            player2Name = "Aarav Sharma",
            player2Country = "IN",
            player2Color = if (playerColor == PlayerColor.ORANGE) PlayerColor.BLUE else PlayerColor.ORANGE,
            isPlayerMatch = true,
            isLive = true,
            liveStatusText = "LIVE CHAMPIONSHIP • Grand Finals Ready for Kickoff!"
          )
        }
        currentRound == TournamentRoundTier.GRAND_FINALS && match.matchId == "GF-1" -> {
          match.copy(
            player1Name = cleanName,
            player1Country = playerCountry,
            player1Color = playerColor,
            player1Score = playerScore,
            player2Score = opponentScore,
            winnerName = cleanName,
            isLive = false,
            liveStatusText = "CHAMPION • $cleanName won the Sovereign Cup!"
          )
        }
        else -> match
      }
    }

    return updated to nextRound
  }

  fun tickLiveContenderMatches(
    matches: List<TournamentBracketMatch>,
    tickCount: Int
  ): Pair<List<TournamentBracketMatch>, String> {
    val feedMessages = listOf(
      "LIVE BRACKET • Duchess Katerina rolled Double 6s in Quarterfinal #2 (+25 Capture Pts)",
      "LIVE BRACKET • Aarav Sharma secured a twin-pawn blockade at Star Tile #19",
      "LIVE BRACKET • Tariq Al-Mansoor guided his 3rd runner into the Home Triangle (+15 Pts)",
      "LIVE BRACKET • Grandmaster Vance is warming up at Table #1 for your bracket duel",
      "LIVE BRACKET • 1,420 contenders competing across regional Sovereign Cup ladders"
    )
    val nextFeed = feedMessages[tickCount % feedMessages.size]
    val updated = matches.map { match ->
      if (!match.isPlayerMatch && match.isLive && match.winnerName == null) {
        val delta1 = if (tickCount % 2 == 0) 5 else 0
        val delta2 = if (tickCount % 2 == 1) 5 else 0
        match.copy(
          player1Score = (match.player1Score + delta1).coerceAtMost(145),
          player2Score = (match.player2Score + delta2).coerceAtMost(140)
        )
      } else {
        match
      }
    }
    return updated to nextFeed
  }
}

// --- FRIENDS SYSTEM: USERNAME SEARCH, FRIEND REQUESTS, ONLINE STATUS & ACTIVE ROOMS ---

enum class FriendRequestStatus(val label: String) {
  PENDING("Pending"),
  ACCEPTED("Friends"),
  DECLINED("Declined");

  companion object {
    fun fromString(value: String): FriendRequestStatus {
      return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
  }
}

@IgnoreExtraProperties
data class FriendRequest(
  @DocumentId val id: String = "",
  val fromUid: String = "",
  val fromName: String = "",
  val fromCountry: String = "US",
  val toUid: String = "",
  val toName: String = "",
  val toCountry: String = "US",
  val status: String = FriendRequestStatus.PENDING.name,
  val memberUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  fun otherUid(myUid: String): String = if (fromUid == myUid) toUid else fromUid
  fun otherName(myUid: String): String = if (fromUid == myUid) toName else fromName
  fun otherCountry(myUid: String): String = if (fromUid == myUid) toCountry else fromCountry
}

enum class FriendPresenceState(
  val label: String,
  val badgeTag: String,
  val sortPriority: Int
) {
  IN_WAITING_ROOM("In Room Lobby • Joinable", "JOIN ROOM", 0),
  IN_ACTIVE_MATCH("In 3D Match", "IN MATCH", 1),
  ONLINE_LOBBY("Online in Studio", "ONLINE", 2),
  OFFLINE("Offline", "OFFLINE", 3)
}

data class FriendEntryUiModel(
  val requestId: String,
  val friendUid: String,
  val displayName: String,
  val countryCode: String,
  val countryName: String,
  val score: Long,
  val rankTierTitle: String,
  val wins: Long,
  val gamesPlayed: Long,
  val capturesTotal: Long,
  val presenceState: FriendPresenceState,
  val statusSubtitle: String,
  val activeRoomCode: String? = null,
  val activeRoomStatus: String? = null,
  val activeRoomIsPublic: Boolean = true,
  val activeRoomPlayersCount: Int = 0,
  val activeRoomMaxPlayers: Int = 4,
  val activeRoomTurnNumber: Int = 1,
  val isDemoFriend: Boolean = false
)

data class OngoingPrivateRoomPreset(
  val roomCode: String,
  val hostName: String,
  val maxPlayers: Int,
  val turnNumber: Int,
  val summary: String
)

object FriendsStudioCatalog {
  private const val ONLINE_WINDOW_MILLIS = 15L * 60L * 1000L // 15 minutes active presence window

  val ONGOING_PRIVATE_ROOMS_SHOWCASE: List<OngoingPrivateRoomPreset> = listOf(
    OngoingPrivateRoomPreset(
      roomCode = "PRV409",
      hostName = "Tariq Al-Mansoor",
      maxPlayers = 4,
      turnNumber = 9,
      summary = "Private 4P Table • Turn 9 • Live Firestore Sync"
    ),
    OngoingPrivateRoomPreset(
      roomCode = "SOV6P8",
      hostName = "Aarav Sharma",
      maxPlayers = 6,
      turnNumber = 14,
      summary = "Private 6P Hex Arena • Turn 14 • Live Firestore Sync"
    ),
    OngoingPrivateRoomPreset(
      roomCode = "VLT802",
      hostName = "Grandmaster Vance",
      maxPlayers = 8,
      turnNumber = 18,
      summary = "Private 8P Octa Table • Turn 18 • Live Firestore Sync"
    )
  )

  fun buildFriendRequestId(uidA: String, uidB: String): String {
    val cleanA = uidA.trim().replace("/", "_").take(48)
    val cleanB = uidB.trim().replace("/", "_").take(48)
    val sorted = listOf(cleanA, cleanB).sorted()
    return "fr_${sorted[0]}_${sorted[1]}".take(120)
  }

  fun buildFriendEntry(
    request: FriendRequest,
    myUid: String,
    profile: PlayerProfile?,
    activeRooms: List<LudoRoom>,
    nowMillis: Long = System.currentTimeMillis()
  ): FriendEntryUiModel {
    val friendUid = request.otherUid(myUid)
    val resolvedName = profile?.displayName?.ifBlank { request.otherName(myUid) }
      ?: request.otherName(myUid).ifBlank { "Royal Friend" }
    val resolvedCountryCode = profile?.countryCode?.ifBlank { request.otherCountry(myUid) }
      ?: request.otherCountry(myUid).ifBlank { "US" }
    val resolvedCountryName = profile?.countryName?.ifBlank {
      CountryCatalog.fromCode(resolvedCountryCode).name
    } ?: CountryCatalog.fromCode(resolvedCountryCode).name
    val score = profile?.score ?: 320L
    val tier = profile?.rankTierTitle ?: "Emerald Knight"
    val wins = profile?.gamesWon ?: 4L
    val played = profile?.gamesPlayed ?: 6L
    val captures = profile?.capturesTotal ?: 9L

    val activeRoom = activeRooms
      .filter { it.status == RoomStatus.WAITING.name || it.status == RoomStatus.PLAYING.name }
      .sortedBy { if (it.status == RoomStatus.WAITING.name) 0 else 1 }
      .firstOrNull { room ->
        friendUid in room.memberUids || room.hostUid == friendUid
      }

    val updatedAtMillis = profile?.updatedAt?.toDate()?.time
      ?: request.updatedAt?.toDate()?.time
      ?: 0L
    val isRecentlyActive = updatedAtMillis > 0L && (nowMillis - updatedAtMillis) <= ONLINE_WINDOW_MILLIS

    val presence = when {
      activeRoom != null && activeRoom.status == RoomStatus.WAITING.name ->
        FriendPresenceState.IN_WAITING_ROOM
      activeRoom != null && activeRoom.status == RoomStatus.PLAYING.name ->
        FriendPresenceState.IN_ACTIVE_MATCH
      isRecentlyActive ->
        FriendPresenceState.ONLINE_LOBBY
      else ->
        FriendPresenceState.OFFLINE
    }

    val isRoomPublic = activeRoom?.isPublic ?: true
    val roomVisibilityLabel = if (isRoomPublic) "Room" else "Private Room"

    val subtitle = when (presence) {
      FriendPresenceState.IN_WAITING_ROOM -> {
        val code = activeRoom?.roomCode ?: ""
        val seats = "${activeRoom?.memberUids?.size ?: 1}/${activeRoom?.maxPlayers ?: 4}"
        "Waiting in $roomVisibilityLabel #$code ($seats Players) — Tap to Join!"
      }
      FriendPresenceState.IN_ACTIVE_MATCH -> {
        val code = activeRoom?.roomCode ?: ""
        val turn = activeRoom?.turnNumber ?: 1L
        "Playing in $roomVisibilityLabel #$code • Turn $turn (${activeRoom?.maxPlayers ?: 4}P Arena)"
      }
      FriendPresenceState.ONLINE_LOBBY -> {
        "Online now • Ready for Dual-Dice Match"
      }
      FriendPresenceState.OFFLINE -> {
        if (updatedAtMillis > 0L) {
          val minsAgo = ((nowMillis - updatedAtMillis) / 60_000L).coerceAtLeast(1L)
          if (minsAgo < 60L) "Last seen ${minsAgo}m ago"
          else if (minsAgo < 1440L) "Last seen ${minsAgo / 60L}h ago"
          else "Last seen ${minsAgo / 1440L}d ago"
        } else {
          "Offline • Sovereign Ludo Club"
        }
      }
    }

    return FriendEntryUiModel(
      requestId = request.id,
      friendUid = friendUid,
      displayName = resolvedName,
      countryCode = resolvedCountryCode,
      countryName = resolvedCountryName,
      score = score,
      rankTierTitle = tier,
      wins = wins,
      gamesPlayed = played,
      capturesTotal = captures,
      presenceState = presence,
      statusSubtitle = subtitle,
      activeRoomCode = activeRoom?.roomCode,
      activeRoomStatus = activeRoom?.status,
      activeRoomIsPublic = isRoomPublic,
      activeRoomPlayersCount = activeRoom?.memberUids?.size ?: 0,
      activeRoomMaxPlayers = activeRoom?.maxPlayers?.toInt() ?: 4,
      activeRoomTurnNumber = activeRoom?.turnNumber?.toInt() ?: 1,
      isDemoFriend = false
    )
  }

  val STUDIO_SHOWCASE_FRIENDS: List<FriendEntryUiModel> = listOf(
    FriendEntryUiModel(
      requestId = "demo_fr_katerina",
      friendUid = "demo_uid_katerina",
      displayName = "Duchess Katerina",
      countryCode = "DE",
      countryName = "Germany",
      score = 940L,
      rankTierTitle = "Diamond Archon",
      wins = 28L,
      gamesPlayed = 36L,
      capturesTotal = 64L,
      presenceState = FriendPresenceState.IN_WAITING_ROOM,
      statusSubtitle = "Waiting in Private Room #KTRN04 (2/4 Players) — Tap to Join or Watch!",
      activeRoomCode = "KTRN04",
      activeRoomStatus = RoomStatus.WAITING.name,
      activeRoomIsPublic = false,
      activeRoomPlayersCount = 2,
      activeRoomMaxPlayers = 4,
      activeRoomTurnNumber = 1,
      isDemoFriend = true
    ),
    FriendEntryUiModel(
      requestId = "demo_fr_aarav",
      friendUid = "demo_uid_aarav",
      displayName = "Aarav Sharma",
      countryCode = "IN",
      countryName = "India",
      score = 1280L,
      rankTierTitle = "Sovereign Grandmaster",
      wins = 42L,
      gamesPlayed = 51L,
      capturesTotal = 108L,
      presenceState = FriendPresenceState.IN_ACTIVE_MATCH,
      statusSubtitle = "Playing in Private Room #SOV6P8 • Turn 14 (6P Hex Arena)",
      activeRoomCode = "SOV6P8",
      activeRoomStatus = RoomStatus.PLAYING.name,
      activeRoomIsPublic = false,
      activeRoomPlayersCount = 6,
      activeRoomMaxPlayers = 6,
      activeRoomTurnNumber = 14,
      isDemoFriend = true
    ),
    FriendEntryUiModel(
      requestId = "demo_fr_tariq",
      friendUid = "demo_uid_tariq",
      displayName = "Tariq Al-Mansoor",
      countryCode = "AE",
      countryName = "United Arab Emirates",
      score = 780L,
      rankTierTitle = "Diamond Archon",
      wins = 19L,
      gamesPlayed = 27L,
      capturesTotal = 47L,
      presenceState = FriendPresenceState.IN_ACTIVE_MATCH,
      statusSubtitle = "Playing in Private Room #PRV409 • Turn 9 (4P Classic Arena)",
      activeRoomCode = "PRV409",
      activeRoomStatus = RoomStatus.PLAYING.name,
      activeRoomIsPublic = false,
      activeRoomPlayersCount = 4,
      activeRoomMaxPlayers = 4,
      activeRoomTurnNumber = 9,
      isDemoFriend = true
    ),
    FriendEntryUiModel(
      requestId = "demo_fr_vance",
      friendUid = "demo_uid_vance",
      displayName = "Grandmaster Vance",
      countryCode = "GB",
      countryName = "United Kingdom",
      score = 610L,
      rankTierTitle = "Golden Tactician",
      wins = 15L,
      gamesPlayed = 24L,
      capturesTotal = 39L,
      presenceState = FriendPresenceState.OFFLINE,
      statusSubtitle = "Last seen 2h ago • Sovereign Cup Seed #8",
      activeRoomCode = null,
      activeRoomStatus = null,
      isDemoFriend = true
    )
  )

  val STUDIO_SEARCHABLE_DIRECTORY: List<PlayerProfile> = listOf(
    PlayerProfile(
      id = "demo_uid_kenji",
      userId = "demo_uid_kenji",
      displayName = "Kenji Takahashi",
      countryCode = "JP",
      countryName = "Japan",
      score = 860L,
      gamesPlayed = 31L,
      gamesWon = 22L,
      capturesTotal = 58L,
      homesReachedTotal = 88L
    ),
    PlayerProfile(
      id = "demo_uid_chloe",
      userId = "demo_uid_chloe",
      displayName = "Chloe Laurent",
      countryCode = "FR",
      countryName = "France",
      score = 710L,
      gamesPlayed = 25L,
      gamesWon = 17L,
      capturesTotal = 41L,
      homesReachedTotal = 68L
    ),
    PlayerProfile(
      id = "demo_uid_lucas",
      userId = "demo_uid_lucas",
      displayName = "Lucas Oliveira",
      countryCode = "BR",
      countryName = "Brazil",
      score = 540L,
      gamesPlayed = 20L,
      gamesWon = 12L,
      capturesTotal = 34L,
      homesReachedTotal = 52L
    ),
    PlayerProfile(
      id = "demo_uid_zainab",
      userId = "demo_uid_zainab",
      displayName = "Zainab Malik",
      countryCode = "PK",
      countryName = "Pakistan",
      score = 1120L,
      gamesPlayed = 40L,
      gamesWon = 32L,
      capturesTotal = 89L,
      homesReachedTotal = 128L
    ),
    PlayerProfile(
      id = "demo_uid_marcus",
      userId = "demo_uid_marcus",
      displayName = "Marcus Sterling",
      countryCode = "US",
      countryName = "United States",
      score = 490L,
      gamesPlayed = 18L,
      gamesWon = 11L,
      capturesTotal = 29L,
      homesReachedTotal = 44L
    )
  )
}

/**
 * Interactive action expected from the player during a 'How to Play' 3D tutorial lesson.
 */
enum class TutorialInteractiveActionType(val actionLabel: String) {
  INSPECT_BOARD("Tap Center Pyramid or 'Complete Overview'"),
  ROLL_DUAL_DICE("Tap 'Roll 3D Dice' in the Dice Dock"),
  SELECT_DIE_SLOT("Tap Die #2 [5] in the Dual-Dice Selector"),
  TAP_HIGHLIGHTED_PAWN("Tap the Highlighted 3D Red Pawn on the Board"),
  LESSON_COMPLETED("Lesson Mastered — Ready for Next Step")
}

/**
 * Visual style for a 3D highlight beacon rendered on the 3D Ludo board during the tutorial.
 */
enum class TutorialHighlightZoneType {
  YARD_PEDESTAL,
  START_SQUARE,
  SAFE_STAR,
  CAPTURE_TARGET,
  BLOCKADE_SHIELD,
  HOME_GOAL_PYRAMID,
  PAWN_SPOTLIGHT
}

data class Tutorial3DBeaconSpec(
  val id: String,
  val title: String,
  val subtitle: String,
  val badgeIcon: String,
  val worldX: Float,
  val worldY: Float,
  val worldZ: Float = 0.26f,
  val zoneType: TutorialHighlightZoneType,
  val accentColor: PlayerColor = PlayerColor.RED
)

data class TutorialLessonStep(
  val stepNumber: Int,
  val id: String,
  val chapterTag: String,
  val title: String,
  val subtitle: String,
  val coachInstruction: String,
  val secondaryPromptAfterSubAction: String = "",
  val completedFeedback: String,
  val proTip: String,
  val initialActionType: TutorialInteractiveActionType,
  val requiredDieSelectionIndex: Int = 0,
  val scriptedDie1: Int,
  val scriptedDie2: Int,
  val initialTokens: List<Int>,
  val afterActionTokens: List<Int>,
  val targetTokenIndex: Int = -1,
  val fromRelativePos: Int = -1,
  val toRelativePos: Int = -1,
  val capturedTokenIndex: Int = -1,
  val cameraYaw: Float,
  val cameraPitch: Float,
  val cameraZoom: Float,
  val beacons: List<Tutorial3DBeaconSpec>
)

object HowToPlayTutorialCatalog {
  const val TUTORIAL_GRADUATION_COIN_REWARD: Long = 150L
  const val TUTORIAL_GRADUATION_XP_REWARD: Long = 100L

  val TUTORIAL_SEATS: List<PlayerSeat> = listOf(
    PlayerSeat(PlayerColor.RED, "TUTORIAL_HERO", "You (Terracotta)", isBot = false),
    PlayerSeat(PlayerColor.GREEN, "TUTORIAL_BOT_1", "Atelier Mentor", isBot = true),
    PlayerSeat(PlayerColor.YELLOW, "TUTORIAL_BOT_2", "Tactical AI", isBot = true),
    PlayerSeat(PlayerColor.BLUE, "TUTORIAL_BOT_3", "Archon AI", isBot = true)
  )

  private fun buildTokens(overrides: Map<Int, Int>): List<Int> {
    return List(32) { idx -> overrides[idx] ?: -1 }
  }

  val LESSONS: List<TutorialLessonStep> = listOf(
    // Lesson 1: 3D Board Architecture & Zones
    TutorialLessonStep(
      stepNumber = 1,
      id = "tutorial_step_1_board_mechanics",
      chapterTag = "CHAPTER 1 OF 6 • BOARD MECHANICS",
      title = "The 3D Royal Board Architecture",
      subtitle = "Corner Yards, Starting Tiles, Safe Stars & the Central Goal Pyramid",
      coachInstruction = "Drag on the 3D board to orbit the camera, or inspect the 4 highlighted 3D beacons showing your Corner Yard, Start Tile, Safe Star, and Central Goal Pyramid. Tap the Center Pyramid or 'Complete Step' when ready!",
      completedFeedback = "Board Architecture Mastered! You know where pawns deploy, where they are protected from capture, and where they ascend to win.",
      proTip = "Pinch to zoom and drag with one finger anytime during a live match to inspect crowded tiles in 3D.",
      initialActionType = TutorialInteractiveActionType.INSPECT_BOARD,
      requiredDieSelectionIndex = 0,
      scriptedDie1 = 6,
      scriptedDie2 = 4,
      initialTokens = buildTokens(
        mapOf(
          0 to -1, 1 to -1, 2 to -1, 3 to -1,
          4 to 0,   // Green pawn on Green Start
          8 to 8    // Yellow pawn on Yellow Safe Star
        )
      ),
      afterActionTokens = buildTokens(
        mapOf(
          0 to 0, 1 to -1, 2 to -1, 3 to -1,
          4 to 0,
          8 to 8
        )
      ),
      targetTokenIndex = -1,
      fromRelativePos = -1,
      toRelativePos = -1,
      cameraYaw = -26f,
      cameraPitch = 50f,
      cameraZoom = 1.06f,
      beacons = listOf(
        Tutorial3DBeaconSpec(
          id = "beacon_yard",
          title = "Corner Yard",
          subtitle = "Roll a 6 to deploy",
          badgeIcon = "🏰",
          worldX = -4.2f,
          worldY = -4.2f,
          worldZ = 0.36f,
          zoneType = TutorialHighlightZoneType.YARD_PEDESTAL,
          accentColor = PlayerColor.RED
        ),
        Tutorial3DBeaconSpec(
          id = "beacon_start",
          title = "Start Square",
          subtitle = "Safe entry tile (#0)",
          badgeIcon = "🛡️",
          worldX = -6.0f,
          worldY = -1.0f,
          worldZ = 0.28f,
          zoneType = TutorialHighlightZoneType.START_SQUARE,
          accentColor = PlayerColor.RED
        ),
        Tutorial3DBeaconSpec(
          id = "beacon_star",
          title = "Brushed-Brass Star",
          subtitle = "100% Capture Immune",
          badgeIcon = "⭐",
          worldX = -1.0f,
          worldY = -5.0f,
          worldZ = 0.28f,
          zoneType = TutorialHighlightZoneType.SAFE_STAR,
          accentColor = PlayerColor.YELLOW
        ),
        Tutorial3DBeaconSpec(
          id = "beacon_goal",
          title = "3D Goal Pyramid",
          subtitle = "Get all 4 pawns home",
          badgeIcon = "👑",
          worldX = 0.0f,
          worldY = 0.0f,
          worldZ = 0.58f,
          zoneType = TutorialHighlightZoneType.HOME_GOAL_PYRAMID,
          accentColor = PlayerColor.YELLOW
        )
      )
    ),

    // Lesson 2: Rolling Dual 3D Dice & Yard Deployment
    TutorialLessonStep(
      stepNumber = 2,
      id = "tutorial_step_2_yard_deployment",
      chapterTag = "CHAPTER 2 OF 6 • YARD DEPLOYMENT",
      title = "Roll a 6 to Deploy from Your Yard",
      subtitle = "Dual 3D Dice give you two chances every turn to roll a 6",
      coachInstruction = "First, tap the highlighted 'Roll Tutorial Dice' button in the 3D Dice Dock below to roll a 6 and a 4.",
      secondaryPromptAfterSubAction = "Awesome roll! Die #1 landed on 6. Now tap the glowing Red Pawn #1 inside your Corner Yard to deploy it onto your Starting Square!",
      completedFeedback = "Pawn Deployed! Rolling a 6 deploys a pawn onto your safe Starting Square and earns a Bonus Roll after both dice are resolved.",
      proTip = "Deploying multiple pawns early gives you tactical flexibility across the board.",
      initialActionType = TutorialInteractiveActionType.ROLL_DUAL_DICE,
      requiredDieSelectionIndex = 0,
      scriptedDie1 = 6,
      scriptedDie2 = 4,
      initialTokens = buildTokens(
        mapOf(
          0 to -1, 1 to -1, 2 to -1, 3 to -1,
          4 to 4
        )
      ),
      afterActionTokens = buildTokens(
        mapOf(
          0 to 0, 1 to -1, 2 to -1, 3 to -1,
          4 to 4
        )
      ),
      targetTokenIndex = 0,
      fromRelativePos = -1,
      toRelativePos = 0,
      cameraYaw = -18f,
      cameraPitch = 46f,
      cameraZoom = 1.15f,
      beacons = listOf(
        Tutorial3DBeaconSpec(
          id = "beacon_deploy_pawn",
          title = "Red Pawn #1",
          subtitle = "Tap with [6] to Deploy",
          badgeIcon = "♟️",
          worldX = -5.15f,
          worldY = -5.15f,
          worldZ = 0.42f,
          zoneType = TutorialHighlightZoneType.PAWN_SPOTLIGHT,
          accentColor = PlayerColor.RED
        ),
        Tutorial3DBeaconSpec(
          id = "beacon_deploy_target",
          title = "Start Tile (Pos 0)",
          subtitle = "Landing Zone",
          badgeIcon = "🎯",
          worldX = -6.0f,
          worldY = -1.0f,
          worldZ = 0.28f,
          zoneType = TutorialHighlightZoneType.START_SQUARE,
          accentColor = PlayerColor.RED
        )
      )
    ),

    // Lesson 3: Dual-Dice Selection & Sequential Pawn Movement
    TutorialLessonStep(
      stepNumber = 3,
      id = "tutorial_step_3_dice_selection",
      chapterTag = "CHAPTER 3 OF 6 • DICE SELECTION",
      title = "Strategic Dual-Dice Selection",
      subtitle = "Choose which die to play first to land on high-value tiles",
      coachInstruction = "You rolled [3] and [5]. Red Pawn #1 is at Tile #3 — exactly 5 steps away from the Safe Star at Tile #8! Tap Die #2 [5] in the Dice Dock to select it first.",
      secondaryPromptAfterSubAction = "Die #2 [5] selected! Follow the golden 3D trajectory arc and tap Red Pawn #1 on the board to sprint 5 steps onto the Brushed-Brass Safe Star!",
      completedFeedback = "Tactical Precision! By selecting Die #2 [5] first, your pawn reached immunity on the Safe Star before moving another pawn.",
      proTip = "You can split your two dice across two different pawns or combine both dice on the same pawn sequentially!",
      initialActionType = TutorialInteractiveActionType.SELECT_DIE_SLOT,
      requiredDieSelectionIndex = 1,
      scriptedDie1 = 3,
      scriptedDie2 = 5,
      initialTokens = buildTokens(
        mapOf(
          0 to 3,  // Red Pawn #1 at pos 3
          1 to 0,  // Red Pawn #2 at Start
          4 to 6
        )
      ),
      afterActionTokens = buildTokens(
        mapOf(
          0 to 8,  // Red Pawn #1 lands on Safe Star pos 8
          1 to 0,
          4 to 6
        )
      ),
      targetTokenIndex = 0,
      fromRelativePos = 3,
      toRelativePos = 8,
      cameraYaw = -22f,
      cameraPitch = 48f,
      cameraZoom = 1.14f,
      beacons = listOf(
        Tutorial3DBeaconSpec(
          id = "beacon_pawn_pos3",
          title = "Red Pawn #1",
          subtitle = "At Tile #3 • Tap to Move +5",
          badgeIcon = "⚡",
          worldX = -3.0f,
          worldY = -1.0f,
          worldZ = 0.40f,
          zoneType = TutorialHighlightZoneType.PAWN_SPOTLIGHT,
          accentColor = PlayerColor.RED
        ),
        Tutorial3DBeaconSpec(
          id = "beacon_safe_star_8",
          title = "Safe Star (#8)",
          subtitle = "+5 Steps • Safe Zone",
          badgeIcon = "⭐",
          worldX = -1.0f,
          worldY = -5.0f,
          worldZ = 0.30f,
          zoneType = TutorialHighlightZoneType.SAFE_STAR,
          accentColor = PlayerColor.YELLOW
        )
      )
    ),

    // Lesson 4: Tactical Capture & Bonus Roll
    TutorialLessonStep(
      stepNumber = 4,
      id = "tutorial_step_4_tactical_capture",
      chapterTag = "CHAPTER 4 OF 6 • CAPTURING PAWNS",
      title = "Capturing Opponent Pawns",
      subtitle = "Strike unprotected rivals for +25 Score & an instant Bonus Roll",
      coachInstruction = "An opponent Green Pawn is unprotected 4 steps ahead at Tile #14! Die #1 [4] is active — tap your highlighted Red Pawn #1 at Tile #10 to capture the Green Pawn!",
      completedFeedback = "Direct Capture! The Green Pawn was sent back to its Corner Yard, and you earned +25 Leaderboard Points plus a Bonus Roll!",
      proTip = "Opponents on colored Starting Squares or Brushed-Brass Star tiles cannot be captured.",
      initialActionType = TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN,
      requiredDieSelectionIndex = 0,
      scriptedDie1 = 4,
      scriptedDie2 = 2,
      initialTokens = buildTokens(
        mapOf(
          0 to 10, // Red Pawn #1 at globalIdx 10
          4 to 1   // Green Pawn #1 at Green rel 1 -> globalIdx (13 + 1) = 14!
        )
      ),
      afterActionTokens = buildTokens(
        mapOf(
          0 to 14, // Red Pawn #1 captures at globalIdx 14
          4 to -1  // Green Pawn #1 sent back to Yard!
        )
      ),
      targetTokenIndex = 0,
      fromRelativePos = 10,
      toRelativePos = 14,
      capturedTokenIndex = 4,
      cameraYaw = -12f,
      cameraPitch = 45f,
      cameraZoom = 1.18f,
      beacons = listOf(
        Tutorial3DBeaconSpec(
          id = "beacon_attacker",
          title = "Attacker (#10)",
          subtitle = "Tap to Strike (+4)",
          badgeIcon = "⚔️",
          worldX = -1.0f,
          worldY = -7.0f,
          worldZ = 0.40f,
          zoneType = TutorialHighlightZoneType.PAWN_SPOTLIGHT,
          accentColor = PlayerColor.RED
        ),
        Tutorial3DBeaconSpec(
          id = "beacon_capture_victim",
          title = "Rival Green Pawn",
          subtitle = "Vulnerable Target (#14)",
          badgeIcon = "🎯",
          worldX = 1.0f,
          worldY = -6.0f,
          worldZ = 0.40f,
          zoneType = TutorialHighlightZoneType.CAPTURE_TARGET,
          accentColor = PlayerColor.GREEN
        )
      )
    ),

    // Lesson 5: Safe Stars & Golden Blockade Shields
    TutorialLessonStep(
      stepNumber = 5,
      id = "tutorial_step_5_golden_blockade",
      chapterTag = "CHAPTER 5 OF 6 • GOLDEN BLOCKADES",
      title = "Forging a 3D Golden Blockade Shield",
      subtitle = "Stack two friendly pawns on the same tile to block all rivals",
      coachInstruction = "Red Pawn #1 is holding Tile #21. Your Red Pawn #2 is 3 steps behind at Tile #18 with Die #1 [3] selected. Tap Red Pawn #2 to join Pawn #1 and forge an impassable Golden Blockade!",
      completedFeedback = "Golden Blockade Forged! Notice the rotating 3D golden shield ring around Tile #21 — no opponent pawn can pass or land on this square!",
      proTip = "Forming a blockade near an opponent's exit lane can trap their pawns while you advance toward Home.",
      initialActionType = TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN,
      requiredDieSelectionIndex = 0,
      scriptedDie1 = 3,
      scriptedDie2 = 1,
      initialTokens = buildTokens(
        mapOf(
          0 to 21, // Red Pawn #1 at pos 21
          1 to 18, // Red Pawn #2 at pos 18
          8 to 5   // Yellow Pawn nearby
        )
      ),
      afterActionTokens = buildTokens(
        mapOf(
          0 to 21, // Both Red Pawns stacked at pos 21 -> active Blockade!
          1 to 21,
          8 to 5
        )
      ),
      targetTokenIndex = 1,
      fromRelativePos = 18,
      toRelativePos = 21,
      cameraYaw = 15f,
      cameraPitch = 47f,
      cameraZoom = 1.16f,
      beacons = listOf(
        Tutorial3DBeaconSpec(
          id = "beacon_pawn2_pos18",
          title = "Red Pawn #2",
          subtitle = "Tap to Advance +3",
          badgeIcon = "♟️",
          worldX = 2.0f,
          worldY = -1.0f,
          worldZ = 0.40f,
          zoneType = TutorialHighlightZoneType.PAWN_SPOTLIGHT,
          accentColor = PlayerColor.RED
        ),
        Tutorial3DBeaconSpec(
          id = "beacon_blockade_pos21",
          title = "Blockade Anchor (#21)",
          subtitle = "Stack 2 Friendly Pawns",
          badgeIcon = "🛡️",
          worldX = 5.0f,
          worldY = -1.0f,
          worldZ = 0.42f,
          zoneType = TutorialHighlightZoneType.BLOCKADE_SHIELD,
          accentColor = PlayerColor.YELLOW
        )
      )
    ),

    // Lesson 6: Home Stretch & Exact Goal Entry
    TutorialLessonStep(
      stepNumber = 6,
      id = "tutorial_step_6_home_goal",
      chapterTag = "CHAPTER 6 OF 6 • VICTORY & HOME GOAL",
      title = "Ascending the 3D Goal Pyramid",
      subtitle = "Enter your colored Home Stretch and roll the exact count to finish",
      coachInstruction = "Your Red Pawn #1 is inside the Red Home Stretch at Tile #53 — just 3 steps from the Central 3D Goal Pyramid (#56)! With Die #1 [3] active, tap Red Pawn #1 to score Home!",
      completedFeedback = "Royal Graduation Complete! Your pawn ascended the 3D Goal Pyramid (+15 Score & Bonus Roll). Claim your +150 Royal Coins & +100 XP Graduation Reward!",
      proTip = "First player to guide all 4 pawns into the Central 3D Goal Pyramid wins the match (+100 Rank Score)!",
      initialActionType = TutorialInteractiveActionType.TAP_HIGHLIGHTED_PAWN,
      requiredDieSelectionIndex = 0,
      scriptedDie1 = 3,
      scriptedDie2 = 6,
      initialTokens = buildTokens(
        mapOf(
          0 to 53, // Red Pawn #1 in Home Stretch (3 steps from 56)
          1 to 56, // Red Pawn #2 already in Goal
          2 to 56, // Red Pawn #3 already in Goal
          3 to 56  // Red Pawn #4 already in Goal
        )
      ),
      afterActionTokens = buildTokens(
        mapOf(
          0 to 56,
          1 to 56,
          2 to 56,
          3 to 56
        )
      ),
      targetTokenIndex = 0,
      fromRelativePos = 53,
      toRelativePos = 56,
      cameraYaw = -30f,
      cameraPitch = 44f,
      cameraZoom = 1.22f,
      beacons = listOf(
        Tutorial3DBeaconSpec(
          id = "beacon_home_stretch_53",
          title = "Home Stretch (#53)",
          subtitle = "Tap Pawn • Exact Roll [3]",
          badgeIcon = "🚀",
          worldX = -4.0f,
          worldY = 0.0f,
          worldZ = 0.40f,
          zoneType = TutorialHighlightZoneType.PAWN_SPOTLIGHT,
          accentColor = PlayerColor.RED
        ),
        Tutorial3DBeaconSpec(
          id = "beacon_central_pyramid",
          title = "Central Goal (#56)",
          subtitle = "Victory Crown Ascent",
          badgeIcon = "👑",
          worldX = 0.0f,
          worldY = 0.0f,
          worldZ = 0.62f,
          zoneType = TutorialHighlightZoneType.HOME_GOAL_PYRAMID,
          accentColor = PlayerColor.YELLOW
        )
      )
    )
  )

  fun findStepByIndex(index: Int): TutorialLessonStep {
    return LESSONS.getOrElse(index.coerceIn(0, LESSONS.lastIndex)) { LESSONS.first() }
  }
}

// --- PRIVATE ROOM REAL-TIME TEXT CHAT, PROFANITY FILTER & QUICK-REPLY EMOJIS ---

@IgnoreExtraProperties
data class RoomChatMessage(
  @DocumentId val id: String = "",
  val roomCode: String = "",
  val senderUid: String = "",
  val senderName: String = "",
  val senderColor: String = "RED",
  val messageText: String = "",
  val quickEmoji: String = "",
  val wasFiltered: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null
)

data class QuickReplyChatOption(
  val id: String,
  val emoji: String,
  val shortLabel: String,
  val messageText: String
) {
  val phrase: String
    get() = shortLabel
}

data class ChatSanitizeResult(
  val sanitizedText: String,
  val wasFiltered: Boolean,
  val matchedWordsCount: Int
) {
  val cleanedText: String
    get() = sanitizedText
  val filteredWordCount: Int
    get() = matchedWordsCount
}

object PrivateRoomChatCatalog {
  private val PROFANITY_PATTERNS: List<Regex> = listOf(
    Regex("""\bf+[\W_]*[u@*0]+[\W_]*c+[\W_]*k+(?:i*n*g*|e*r*|s*)?\b""", RegexOption.IGNORE_CASE),
    Regex("""\bs+[\W_]*h+[\W_]*[i1!*]+[\W_]*t+(?:t*y*|s*)?\b""", RegexOption.IGNORE_CASE),
    Regex("""\bb+[\W_]*u+[\W_]*l+[\W_]*s+[\W_]*h+[\W_]*[i1!*]+[\W_]*t+\b""", RegexOption.IGNORE_CASE),
    Regex("""\bb+[\W_]*[i1!*]+[\W_]*t+[\W_]*c+[\W_]*h+(?:e*s*)?\b""", RegexOption.IGNORE_CASE),
    Regex("""\ba+[\W_]*s+[\W_]*s+[\W_]*h+[\W_]*o+[\W_]*l+[\W_]*e+(?:s*)?\b""", RegexOption.IGNORE_CASE),
    Regex("""\bb+[\W_]*a+[\W_]*s+[\W_]*t+[\W_]*a+[\W_]*r+[\W_]*d+(?:s*)?\b""", RegexOption.IGNORE_CASE),
    Regex("""\bd+[\W_]*[i1!*]+[\W_]*c+[\W_]*k+(?:s*)?\b""", RegexOption.IGNORE_CASE),
    Regex("""\bd+[\W_]*a+[\W_]*m+[\W_]*n+\b""", RegexOption.IGNORE_CASE),
    Regex("""\bc+[\W_]*r+[\W_]*a+[\W_]*p+\b""", RegexOption.IGNORE_CASE),
    Regex("""\bi+[\W_]*d+[\W_]*i+[\W_]*o+[\W_]*t+(?:s*)?\b""", RegexOption.IGNORE_CASE),
    Regex("""\bs+[\W_]*t+[\W_]*u+[\W_]*p+[\W_]*i+[\W_]*d+\b""", RegexOption.IGNORE_CASE),
    Regex("""\bn+[\W_]*o+[\W_]*o+[\W_]*b+(?:s*)?\b""", RegexOption.IGNORE_CASE),
    Regex("""\bl+[\W_]*o+[\W_]*s+[\W_]*e+[\W_]*r+(?:s*)?\b""", RegexOption.IGNORE_CASE),
    Regex("""\bw+[\W_]*t+[\W_]*f+\b""", RegexOption.IGNORE_CASE),
    Regex("""\bs+[\W_]*t+[\W_]*f+[\W_]*u+\b""", RegexOption.IGNORE_CASE)
  )

  val QUICK_REPLY_OPTIONS: List<QuickReplyChatOption> = listOf(
    QuickReplyChatOption(
      id = "qr_good_roll",
      emoji = "🎲",
      shortLabel = "Good Roll!",
      messageText = "🎲 Good roll! Dual-dice luck is on fire!"
    ),
    QuickReplyChatOption(
      id = "qr_what_a_move",
      emoji = "🔥",
      shortLabel = "Great Move!",
      messageText = "🔥 What a tactical move!"
    ),
    QuickReplyChatOption(
      id = "qr_blockade",
      emoji = "🛡️",
      shortLabel = "Blockade!",
      messageText = "🛡️ Nice Golden Blockade shield!"
    ),
    QuickReplyChatOption(
      id = "qr_close_call",
      emoji = "😅",
      shortLabel = "Close Call!",
      messageText = "😅 Phew, that was a close call on the outer track!"
    ),
    QuickReplyChatOption(
      id = "qr_roll_six",
      emoji = "⚡",
      shortLabel = "Need a 6!",
      messageText = "⚡ Manifesting a 6 on Die #1 or Die #2!"
    ),
    QuickReplyChatOption(
      id = "qr_gg",
      emoji = "👑",
      shortLabel = "GG WP!",
      messageText = "👑 GG Well Played everyone!"
    )
  )

  fun sanitizeMessage(rawInput: String): ChatSanitizeResult {
    var working = rawInput.trim().take(240)
    if (working.isBlank()) {
      return ChatSanitizeResult(sanitizedText = "", wasFiltered = false, matchedWordsCount = 0)
    }
    var matchCount = 0
    for (pattern in PROFANITY_PATTERNS) {
      val matches = pattern.findAll(working).count()
      if (matches > 0) {
        matchCount += matches
        working = pattern.replace(working) { matchResult ->
          "*".repeat(matchResult.value.length.coerceIn(3, 6))
        }
      }
    }
    return ChatSanitizeResult(
      sanitizedText = working,
      wasFiltered = matchCount > 0,
      matchedWordsCount = matchCount
    )
  }

  fun buildInitialShowcaseMessages(roomCode: String, hostName: String = "Tariq Al-Mansoor"): List<RoomChatMessage> {
    val cleanCode = roomCode.trim().uppercase().ifBlank { "PRV409" }
    return listOf(
      RoomChatMessage(
        id = "seed_${cleanCode}_1",
        roomCode = cleanCode,
        senderUid = "host_seed_1",
        senderName = hostName,
        senderColor = "RED",
        messageText = "Welcome to Private Room #$cleanCode! SafeChat profanity filter is active.",
        quickEmoji = "👑",
        wasFiltered = false
      ),
      RoomChatMessage(
        id = "seed_${cleanCode}_2",
        roomCode = cleanCode,
        senderUid = "peer_seed_2",
        senderName = "Duchess Katerina",
        senderColor = "GREEN",
        messageText = "🎲 Good roll! Choosing Die #2 first to land on the Safe Star!",
        quickEmoji = "🎲",
        wasFiltered = false
      ),
      RoomChatMessage(
        id = "seed_${cleanCode}_3",
        roomCode = cleanCode,
        senderUid = "peer_seed_3",
        senderName = "Archon AI",
        senderColor = "YELLOW",
        messageText = "🛡️ Nice Golden Blockade shield! Let's see your sequential 2nd die move.",
        quickEmoji = "🛡️",
        wasFiltered = false
      )
    )
  }
}






