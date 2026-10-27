package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.CountryCatalog
import com.example.data.DailyMissionCatalog
import com.example.data.EmoteCategory
import com.example.data.EmoteMomentTrigger
import com.example.data.MatchOutcomeType
import com.example.data.PlayerProfile
import com.example.data.ProfileCatalog
import com.example.data.ProfileMatchHistoryEntry
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.TerracottaRed
import com.example.ui.viewmodel.DataStreamState
import com.example.ui.viewmodel.LudoGameViewModel
import com.example.ui.viewmodel.LudoUiState
import java.util.Locale

@Composable
fun ProfileScreen(
  uiState: LudoUiState,
  profileState: DataStreamState<PlayerProfile?>,
  globalLeaderboardState: DataStreamState<List<PlayerProfile>>,
  countryLeaderboardState: DataStreamState<List<PlayerProfile>>,
  viewModel: LudoGameViewModel
) {
  BackHandler {
    viewModel.leaveToLobby()
  }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var showEditNameDialog by remember { mutableStateOf(false) }
  var showCountryDialog by remember { mutableStateOf(false) }
  var authErrorText by remember { mutableStateOf<String?>(null) }

  val profile = (profileState as? DataStreamState.Success)?.data
  val globalLeaders = (globalLeaderboardState as? DataStreamState.Success)?.data.orEmpty()
  val countryLeaders = (countryLeaderboardState as? DataStreamState.Success)?.data.orEmpty()

  val activeCountryCode = profile?.countryCode ?: uiState.selectedCountryFilter
  val countryInfo = CountryCatalog.fromCode(activeCountryCode)
  val displayName = if (viewModel.isAuthenticated && profile != null) {
    profile.displayName.ifBlank { uiState.localDisplayName }
  } else {
    uiState.localDisplayName.ifBlank { "Royal Contender" }
  }

  val selectedAvatarId = profile?.parsedAvatarId(uiState.selectedAvatarId) ?: uiState.selectedAvatarId
  val selectedAvatar = ProfileCatalog.findAvatarById(selectedAvatarId)

  val totalWins = maxOf((profile?.gamesWon ?: 0L).toInt(), uiState.sessionWinsCount)
  val totalLosses = maxOf((profile?.gamesLost ?: 0L).toInt(), uiState.sessionLossesCount)
  val totalMatches = maxOf((profile?.gamesPlayed ?: 0L).toInt(), totalWins + totalLosses)
  val winRatePercent = if (totalMatches > 0) {
    ((totalWins * 100) / totalMatches).coerceIn(0, 100)
  } else {
    0
  }
  val winLossRatioValue = if (totalLosses > 0) {
    totalWins.toDouble() / totalLosses.toDouble()
  } else {
    totalWins.toDouble()
  }
  val winLossRatioFormatted = String.format(Locale.US, "%.2f", winLossRatioValue)

  val totalCaptures = maxOf((profile?.capturesTotal ?: 0L).toInt(), uiState.sessionCapturesCount)
  val totalHomes = maxOf((profile?.homesReachedTotal ?: 0L).toInt(), uiState.sessionHomesCount)
  val avgCaptures = if (totalMatches > 0) {
    String.format(Locale.US, "%.1f", totalCaptures.toDouble() / totalMatches.toDouble())
  } else {
    "0.0"
  }
  val rankScore = profile?.score ?: (100L + totalWins * 100L + totalCaptures * 25L + totalHomes * 15L)
  val coins = if (viewModel.isAuthenticated && profile != null) profile.coins else uiState.localGuestCoins
  val xp = if (viewModel.isAuthenticated && profile != null) profile.xp else uiState.localGuestXp
  val level = ((xp / 200L) + 1L).toInt().coerceAtLeast(1)
  val xpProgress = ((xp % 200L).toFloat() / 200f).coerceIn(0f, 1f)
  val rankTierTitle = profile?.rankTierTitle ?: when {
    rankScore >= 1200L -> "Sovereign Grandmaster"
    rankScore >= 750L -> "Diamond Archon"
    rankScore >= 400L -> "Golden Tactician"
    rankScore >= 200L -> "Emerald Knight"
    else -> "Royal Contender"
  }

  val globalRank = viewModel.computeGlobalRank(profile, globalLeaders)
  val countryRank = viewModel.computeCountryRank(profile, countryLeaders)
  val matchHistory = viewModel.effectiveMatchHistory(profile)

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 640.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Top Header Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            IconButton(
              onClick = { viewModel.leaveToLobby() },
              modifier = Modifier.testTag("profile_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Lobby"
              )
            }
            Column {
              Text(
                text = "Royal Player Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = "Firebase Auth & Firestore • Win/Loss Ratio • Custom Avatar",
                style = MaterialTheme.typography.labelMedium,
                color = BrushedBrass
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = { viewModel.openFriendsScreen() },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("profile_open_friends_button")
            ) {
              Text("Friends")
            }
            OutlinedButton(
              onClick = { showCountryDialog = true },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("profile_country_button")
            ) {
              Text("${countryInfo.flagEmoji} ${countryInfo.code}")
            }
            IconButton(
              onClick = { showEditNameDialog = true },
              modifier = Modifier.testTag("profile_edit_name_button")
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Display Name",
                tint = BrushedBrass
              )
            }
          }
        }

        // Toast or Error Banner
        val bannerMsg = uiState.toastMessage ?: uiState.errorMessage ?: authErrorText
        if (bannerMsg != null) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
              containerColor = EucalyptusSage.copy(alpha = 0.16f)
            )
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = bannerMsg,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
              )
              TextButton(
                onClick = {
                  viewModel.clearErrorMessage()
                  authErrorText = null
                }
              ) {
                Text("OK")
              }
            }
          }
        }

        // 1. Player Identity, Custom Avatar Crest, Firebase Auth & Cloud Sync Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, BrushedBrass, RoundedCornerShape(22.dp))
            .testTag("profile_identity_card"),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
              ) {
                Box(
                  modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color(selectedAvatar.primaryColorHex).copy(alpha = 0.25f))
                    .border(2.dp, Color(selectedAvatar.ringColorHex), CircleShape)
                    .testTag("profile_selected_avatar_badge"),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = selectedAvatar.badgeSymbol,
                    style = MaterialTheme.typography.headlineSmall
                  )
                }
                Column {
                  Text(
                    text = "$displayName ${countryInfo.flagEmoji}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "${selectedAvatar.title} • $rankTierTitle • Lv.$level",
                    style = MaterialTheme.typography.labelLarge,
                    color = BrushedBrass,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "${countryInfo.name} • ${countryInfo.currencyCode} (${countryInfo.currencySymbol.trim()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              // Cloud Sync Badge
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .background(
                    if (viewModel.isAuthenticated) EucalyptusSage.copy(alpha = 0.2f)
                    else BrushedBrass.copy(alpha = 0.16f)
                  )
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = if (viewModel.isAuthenticated) Icons.Default.CloudDone else Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (viewModel.isAuthenticated) EucalyptusSage else BrushedBrass,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = if (viewModel.isAuthenticated) "FIREBASE AUTH + DB" else "STUDIO PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (viewModel.isAuthenticated) EucalyptusSage else BrushedBrass
                  )
                }
              }
            }

            // Firebase Auth Storage Telemetry Strip
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("profile_firebase_auth_info"),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (viewModel.isAuthenticated) {
                    "Firebase Auth User: ${viewModel.currentUserEmail ?: viewModel.currentUserId?.take(12) ?: "Signed In"}"
                  } else {
                    "Firebase Auth Profile: Ready for Google Sign-In Sync"
                  },
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Auth Avatar URI: ${selectedAvatar.authPhotoUriString}",
                  style = MaterialTheme.typography.labelSmall,
                  color = BrushedBrass
                )
              }
              TextButton(
                onClick = { showEditNameDialog = true },
                modifier = Modifier.testTag("profile_inline_edit_name_button")
              ) {
                Text("Edit Name", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }

            // XP Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Level $level Progress (${xp % 200L} / 200 XP)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "$coins Royal Coins • $xp Total XP",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = BrushedBrass
                )
              }
              LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(999.dp)),
                color = BrushedBrass,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
              )
            }

            if (!viewModel.isAuthenticated) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Sign in with Google to store your Custom Avatar, Display Name & Win/Loss stats in Firebase Auth and Firestore.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                  onClick = {
                    onGoogleSignInClicked(
                      context = context,
                      credentialManager = credentialManager,
                      onAuthSuccess = {},
                      onAuthError = { authErrorText = it },
                      scope = scope
                    )
                  },
                  modifier = Modifier.testTag("profile_google_sign_in_button"),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = BrushedBrass,
                    contentColor = Color.Black
                  )
                ) {
                  Text("Sign In", fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        // 1B. Custom Avatar Selection Card (Persisted to Firebase Auth photoUri & Firestore)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .testTag("profile_avatar_selector_section"),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = BrushedBrass
                )
                Column {
                  Text(
                    text = "Custom Royal Avatar Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Tap any crest to equip • Stored in Firebase Auth (photoUri) & Firestore",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(BrushedBrass.copy(alpha = 0.2f))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "${selectedAvatar.badgeSymbol} ${selectedAvatar.title}",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = BrushedBrass
                )
              }
            }

            ProfileCatalog.AVATAR_OPTIONS.chunked(4).forEach { rowAvatars ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                rowAvatars.forEach { avatarOption ->
                  val isEquipped = avatarOption.id == selectedAvatar.id
                  val primaryAccent = Color(avatarOption.primaryColorHex)
                  val ringAccent = Color(avatarOption.ringColorHex)
                  Column(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(16.dp))
                      .background(
                        if (isEquipped) primaryAccent.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                      )
                      .border(
                        width = if (isEquipped) 2.dp else 1.dp,
                        color = if (isEquipped) ringAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                      )
                      .clickable { viewModel.selectCustomAvatar(avatarOption.id) }
                      .padding(horizontal = 6.dp, vertical = 10.dp)
                      .testTag("profile_avatar_option_${avatarOption.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(primaryAccent.copy(alpha = 0.28f))
                        .border(1.5.dp, ringAccent, CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = avatarOption.badgeSymbol,
                        style = MaterialTheme.typography.titleMedium
                      )
                    }
                    Text(
                      text = avatarOption.title,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = if (isEquipped) FontWeight.ExtraBold else FontWeight.SemiBold,
                      color = if (isEquipped) BrushedBrass else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = if (isEquipped) "EQUIPPED" else avatarOption.subtitle,
                      style = MaterialTheme.typography.labelSmall,
                      color = if (isEquipped) EucalyptusSage else MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
            }
          }
        }

        // 2. Global Rank & Regional Standing Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .testTag("profile_global_rank_card"),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Leaderboard,
                  contentDescription = null,
                  tint = BrushedBrass
                )
                Text(
                  text = "Global & Regional Rank Standing",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
              }
              OutlinedButton(
                onClick = { viewModel.openLeaderboardScreen() },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("profile_open_leaderboard_button")
              ) {
                Text("Leaderboard", style = MaterialTheme.typography.labelSmall)
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              ProfileRankBadgeCard(
                label = "GLOBAL RANK",
                value = "#$globalRank",
                subtitle = "Worldwide Standing",
                accentColor = BrushedBrass,
                modifier = Modifier
                  .weight(1f)
                  .testTag("profile_global_rank_value")
              )
              ProfileRankBadgeCard(
                label = "${countryInfo.flagEmoji} ${countryInfo.code} RANK",
                value = "#$countryRank",
                subtitle = countryInfo.name,
                accentColor = EucalyptusSage,
                modifier = Modifier
                  .weight(1f)
                  .testTag("profile_country_rank_value")
              )
              ProfileRankBadgeCard(
                label = "RANK RATING",
                value = "$rankScore",
                subtitle = "Royal Rank PTS",
                accentColor = BrushedBrass,
                modifier = Modifier
                  .weight(1f)
                  .testTag("profile_rank_score_value")
              )
            }
          }
        }

        // 3. Win / Loss Ratio, Total Games Played & Combat Telemetry Grid
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .testTag("profile_stats_grid"),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = BrushedBrass
              )
              Column {
                Text(
                  text = "Win-Loss Ratio, Total Games & Career Telemetry",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Synced via Firebase Auth & Firestore (/users/{uid})",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Headline 3-Column Row: Total Games Played, Win-Loss Ratio, Win Rate %
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              ProfileRankBadgeCard(
                label = "TOTAL GAMES",
                value = "$totalMatches",
                subtitle = "Matches Played",
                accentColor = BrushedBrass,
                modifier = Modifier
                  .weight(1f)
                  .testTag("profile_total_games_tile")
              )
              ProfileRankBadgeCard(
                label = "WIN-LOSS RATIO",
                value = "$winLossRatioFormatted",
                subtitle = "${totalWins}W : ${totalLosses}L",
                accentColor = EucalyptusSage,
                modifier = Modifier
                  .weight(1f)
                  .testTag("profile_win_loss_ratio_tile")
              )
              ProfileRankBadgeCard(
                label = "WIN RATE",
                value = "$winRatePercent%",
                subtitle = "Victory Share",
                accentColor = EucalyptusSage,
                modifier = Modifier
                  .weight(1f)
                  .testTag("profile_win_rate_tile")
              )
            }

            // Visual Win / Loss Ratio Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Wins: $totalWins ($winRatePercent%)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = EucalyptusSage
                )
                Text(
                  text = "W/L Ratio: $winLossRatioFormatted ($totalMatches Games)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Losses: $totalLosses",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = TerracottaRed
                )
              }
              val winFraction = if (totalMatches > 0) {
                (totalWins.toFloat() / totalMatches.toFloat()).coerceIn(0.08f, 0.92f)
              } else {
                0.5f
              }
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(999.dp))
                  .background(TerracottaRed.copy(alpha = 0.75f))
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(winFraction)
                    .fillMaxHeight()
                    .background(EucalyptusSage)
                )
              }
            }

            // 2x2 Key Stat Telemetry Cards
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              TelemetryStatTile(
                title = "WIN / LOSS RECORD",
                primaryValue = "${totalWins}W - ${totalLosses}L",
                secondaryLabel = "$winRatePercent% Career Win Rate • $winLossRatioFormatted W/L",
                accentColor = EucalyptusSage,
                modifier = Modifier
                  .weight(1f)
                  .testTag("profile_win_loss_tile")
              )
              TelemetryStatTile(
                title = "TOTAL CAPTURES",
                primaryValue = "$totalCaptures",
                secondaryLabel = "$avgCaptures Captures / Match",
                accentColor = TerracottaRed,
                modifier = Modifier
                  .weight(1f)
                  .testTag("profile_total_captures_tile")
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              TelemetryStatTile(
                title = "PAWNS REACHED HOME",
                primaryValue = "$totalHomes",
                secondaryLabel = "+15 Rank PTS per Home Pawn",
                accentColor = BrushedBrass,
                modifier = Modifier
                  .weight(1f)
                  .testTag("profile_total_homes_tile")
              )
              TelemetryStatTile(
                title = "TOURNAMENT WINS",
                primaryValue = "${uiState.tournamentWins} / 3",
                secondaryLabel = uiState.tournamentCurrentRound.title,
                accentColor = BrushedBrass,
                modifier = Modifier
                  .weight(1f)
                  .testTag("profile_tournament_progress_tile")
              )
            }
          }
        }

        // 3B. Daily Missions & Currency Progress Section
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .testTag("profile_daily_missions_section"),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                tint = BrushedBrass
              )
              Column {
                Text(
                  text = "Daily Missions & Currency Objectives",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Complete objectives like 'Win 3 games' or 'Roll a 6 five times' • Progress stored in Firestore",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            DailyMissionCatalog.MISSIONS.forEach { mission ->
              val progress = viewModel.missionProgressCount(mission, profile).coerceAtMost(mission.targetCount)
              val isDone = progress >= mission.targetCount
              val isClaimed = viewModel.isMissionClaimed(mission.id, profile)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag("profile_mission_row_${mission.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${mission.title} ($progress/${mission.targetCount})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "${mission.description} • +${mission.rewardCoins} Coins",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                when {
                  isClaimed -> {
                    Text(
                      text = "CLAIMED",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = EucalyptusSage
                    )
                  }
                  isDone -> {
                    Button(
                      onClick = { viewModel.claimDailyMission(mission, profile) },
                      shape = RoundedCornerShape(10.dp),
                      colors = ButtonDefaults.buttonColors(
                        containerColor = BrushedBrass,
                        contentColor = Color.Black
                      ),
                      modifier = Modifier.testTag("profile_claim_mission_${mission.id}")
                    ) {
                      Text("Claim +${mission.rewardCoins}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                  }
                  else -> {
                    Text(
                      text = "+${mission.rewardCoins} Coins",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = BrushedBrass
                    )
                  }
                }
              }
            }
          }
        }

        // 4. Equipped 3D Emotes & Stickers Loadout Section
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .testTag("profile_equipped_emotes_section"),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.Palette,
                  contentDescription = null,
                  tint = BrushedBrass
                )
                Column {
                  Text(
                    text = "Equipped 3D Emotes Loadout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Active 3D animations & stickers above your pawns during matches",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Button(
                onClick = { viewModel.openEmoteGalleryScreen() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = BrushedBrass,
                  contentColor = Color.Black
                ),
                modifier = Modifier.testTag("profile_open_emote_gallery_button")
              ) {
                Text("Customize", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
              }
            }

            EmoteMomentTrigger.entries.forEach { moment ->
              val equippedEmote = uiState.equippedEmoteForMoment(moment)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                  .clickable {
                    viewModel.selectEmoteMomentSlot(moment)
                    viewModel.openEmoteGalleryScreen()
                  }
                  .padding(horizontal = 12.dp, vertical = 10.dp)
                  .testTag("profile_equipped_emote_${moment.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(BrushedBrass.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = equippedEmote.stickerSymbol,
                      style = MaterialTheme.typography.titleMedium
                    )
                  }
                  Column {
                    Text(
                      text = "${moment.shortTag} • ${equippedEmote.title}",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = "${equippedEmote.category.label} • \"${equippedEmote.calloutText}\"",
                      style = MaterialTheme.typography.labelSmall,
                      color = BrushedBrass
                    )
                  }
                }

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                      if (equippedEmote.category == EmoteCategory.ANIMATION_3D) {
                        EucalyptusSage.copy(alpha = 0.2f)
                      } else {
                        BrushedBrass.copy(alpha = 0.2f)
                      }
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = equippedEmote.rarityLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (equippedEmote.category == EmoteCategory.ANIMATION_3D) {
                      EucalyptusSage
                    } else {
                      BrushedBrass
                    }
                  )
                }
              }
            }
          }
        }

        // 5. Win / Loss Match History Log Section
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp))
            .testTag("profile_match_history_section"),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.History,
                  contentDescription = null,
                  tint = BrushedBrass
                )
                Column {
                  Text(
                    text = "Recent Win / Loss Match History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Chronological match outcomes, captures & rating deltas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              OutlinedButton(
                onClick = { viewModel.openReplayStudioScreen() },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("profile_open_replays_button")
              ) {
                Icon(
                  imageVector = Icons.Default.VideoLibrary,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = BrushedBrass
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("3D Replays", style = MaterialTheme.typography.labelSmall)
              }
            }

            matchHistory.forEachIndexed { idx, entry ->
              MatchHistoryRow(
                entry = entry,
                index = idx
              )
            }
          }
        }
      }
    }
  }

  if (showEditNameDialog) {
    var draftName by remember(displayName) { mutableStateOf(displayName) }
    AlertDialog(
      onDismissRequest = { showEditNameDialog = false },
      title = { Text("Update Royal Display Name") },
      text = {
        OutlinedTextField(
          value = draftName,
          onValueChange = { draftName = it },
          label = { Text("Display Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_name_input")
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateDisplayName(draftName)
            showEditNameDialog = false
          },
          modifier = Modifier.testTag("profile_save_name_button")
        ) {
          Text("Save to Firestore")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditNameDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showCountryDialog) {
    AlertDialog(
      onDismissRequest = { showCountryDialog = false },
      title = { Text("Select Country & Region") },
      text = {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 340.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(CountryCatalog.COUNTRIES, key = { it.code }) { c ->
            val isSelected = c.code == activeCountryCode
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                  if (isSelected) BrushedBrass.copy(alpha = 0.18f)
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
                .clickable {
                  viewModel.updateUserCountry(c.code)
                  showCountryDialog = false
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${c.flagEmoji} ${c.name} (${c.currencyCode})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = BrushedBrass,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCountryDialog = false }) {
          Text("Close")
        }
      }
    )
  }
}

@Composable
private fun ProfileRankBadgeCard(
  label: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
      .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
      .padding(horizontal = 10.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.ExtraBold,
      color = accentColor
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
private fun TelemetryStatTile(
  title: String,
  primaryValue: String,
  secondaryLabel: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      .padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = primaryValue,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.ExtraBold,
      color = accentColor
    )
    Text(
      text = secondaryLabel,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun MatchHistoryRow(
  entry: ProfileMatchHistoryEntry,
  index: Int
) {
  val badgeColor = when (entry.outcome) {
    MatchOutcomeType.WIN -> EucalyptusSage
    MatchOutcomeType.LOSS -> TerracottaRed
    MatchOutcomeType.FORFEIT -> TerracottaRed
  }
  val deltaText = if (entry.scoreDelta >= 0) "+${entry.scoreDelta} PTS" else "${entry.scoreDelta} PTS"

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
      .padding(horizontal = 12.dp, vertical = 10.dp)
      .testTag("profile_match_history_item_$index"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.weight(1f)
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(badgeColor.copy(alpha = 0.2f))
          .padding(horizontal = 8.dp, vertical = 5.dp)
      ) {
        Text(
          text = entry.outcome.label,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
          color = badgeColor
        )
      }
      Column {
        Text(
          text = entry.modeLabel,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "${entry.captures} Captures • ${entry.homesReached} Home Pawns • ${entry.timestampLabel}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Text(
      text = deltaText,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.ExtraBold,
      color = if (entry.scoreDelta >= 0) EucalyptusSage else TerracottaRed
    )
  }
}
