package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AdminAppConfig
import com.example.data.CountryCatalog
import com.example.data.DailyMissionCatalog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FriendsStudioCatalog
import com.example.data.LudoRoom
import com.example.data.PlayerColor
import com.example.data.PlayerProfile
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.board3d.paletteForColor
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.TerracottaRed
import com.example.ui.viewmodel.DataStreamState
import com.example.ui.viewmodel.LudoGameViewModel
import com.example.ui.viewmodel.LudoUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LobbyHubScreen(
  uiState: LudoUiState,
  profileState: DataStreamState<PlayerProfile?>,
  openRoomsState: DataStreamState<List<LudoRoom>>,
  adminConfig: AdminAppConfig = AdminAppConfig(),
  viewModel: LudoGameViewModel,
  onSignOut: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var joinCodeInput by remember { mutableStateOf("") }
  var selectedOnlineMaxPlayers by remember { mutableIntStateOf(4) }
  var showEditNameDialog by remember { mutableStateOf(false) }
  var showLocationCurrencyDialog by remember { mutableStateOf(false) }
  var editingColorForSeatIndex by remember { mutableStateOf<Int?>(null) }
  var isSigningInForOnline by remember { mutableStateOf(false) }
  var authErrorBanner by remember { mutableStateOf<String?>(null) }

  val profile = (profileState as? DataStreamState.Success)?.data
  val openRooms = (openRoomsState as? DataStreamState.Success)?.data.orEmpty()
  val spectatablePlayingRooms by viewModel.spectatablePublicPlayingRoomsState.collectAsStateWithLifecycle()
  val ongoingCloudPrivateRooms = spectatablePlayingRooms.filter { !it.isPublic }
  val activeCountryCode = profile?.countryCode ?: uiState.selectedCountryFilter
  val countryInfo = CountryCatalog.fromCode(activeCountryCode)
  val currentCoins = if (viewModel.isAuthenticated && profile != null) {
    profile.coins
  } else {
    uiState.localGuestCoins
  }
  val currentXp = if (viewModel.isAuthenticated && profile != null) {
    profile.xp
  } else {
    uiState.localGuestXp
  }
  val playerLevel = ((currentXp / 200L) + 1L).toInt().coerceAtLeast(1)

  fun triggerOnlineGoogleSignIn() {
    authErrorBanner = null
    isSigningInForOnline = true
    onGoogleSignInClicked(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isSigningInForOnline = false
      },
      onAuthError = { msg ->
        isSigningInForOnline = false
        authErrorBanner = msg
      },
      scope = scope,
      onAuthCancelled = {
        isSigningInForOnline = false
      }
    )
  }

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
          .widthIn(max = 600.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        // Top Header Bar
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = adminConfig.announcementBanner.ifBlank { "Dual-Dice • 2 to 8 Players • 3D Voice Arena" },
                style = MaterialTheme.typography.labelMedium,
                color = BrushedBrass,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              IconButton(
                onClick = { viewModel.toggleSoundEnabled() },
                modifier = Modifier.testTag("lobby_sound_toggle_button")
              ) {
                Icon(
                  imageVector = if (uiState.isSoundEnabled) {
                    Icons.AutoMirrored.Filled.VolumeUp
                  } else {
                    Icons.AutoMirrored.Filled.VolumeOff
                  },
                  contentDescription = stringResource(R.string.sound_toggle_label),
                  tint = if (uiState.isSoundEnabled) BrushedBrass else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              IconButton(
                onClick = { viewModel.openAdminPanelScreen() },
                modifier = Modifier.testTag("open_admin_button")
              ) {
                Icon(
                  imageVector = Icons.Default.AdminPanelSettings,
                  contentDescription = stringResource(R.string.admin_button_label),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              if (viewModel.isAuthenticated) {
                IconButton(
                  onClick = onSignOut,
                  modifier = Modifier.testTag("sign_out_button")
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = stringResource(R.string.sign_out_label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          // Quick Action Strip: Profile, Friends, Tourney, Store (IAP), Emotes, Leaderboards, Replays, Rules
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            OutlinedButton(
              onClick = { viewModel.openProfileScreen() },
              modifier = Modifier
                .weight(1f)
                .testTag("open_profile_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Player Profile",
                modifier = Modifier.size(14.dp),
                tint = BrushedBrass
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text("Profile", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = { viewModel.openFriendsScreen() },
              modifier = Modifier
                .weight(1f)
                .testTag("open_friends_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = "Friends & Active Rooms",
                modifier = Modifier.size(14.dp),
                tint = BrushedBrass
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text("Friends", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = { viewModel.openWeeklyTournamentScreen() },
              modifier = Modifier
                .weight(1f)
                .testTag("open_tournament_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Weekly Tournament",
                modifier = Modifier.size(14.dp),
                tint = BrushedBrass
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text("Tourney", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = { viewModel.openStoreScreen() },
              modifier = Modifier
                .weight(1f)
                .testTag("open_store_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = stringResource(R.string.store_button_label),
                modifier = Modifier.size(14.dp),
                tint = BrushedBrass
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text("Store", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = { viewModel.openEmoteGalleryScreen() },
              modifier = Modifier
                .weight(1f)
                .testTag("open_emote_gallery_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "3D Emote Gallery",
                modifier = Modifier.size(14.dp),
                tint = BrushedBrass
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text("Emotes", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = { viewModel.openLeaderboardScreen() },
              modifier = Modifier
                .weight(1f)
                .testTag("open_leaderboard_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Leaderboard,
                contentDescription = stringResource(R.string.leaderboard_title),
                modifier = Modifier.size(14.dp),
                tint = BrushedBrass
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text("Ranks", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = { viewModel.openReplayStudioScreen() },
              modifier = Modifier
                .weight(1f)
                .testTag("open_replays_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = "3D Match Replays",
                modifier = Modifier.size(14.dp),
                tint = BrushedBrass
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text("Replays", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = { viewModel.openTutorialScreen() },
              modifier = Modifier
                .weight(1f)
                .testTag("open_tutorial_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Interactive 3D Tutorial",
                modifier = Modifier.size(14.dp),
                tint = BrushedBrass
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text("How to Play", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
              onClick = { viewModel.toggleRulesModal(true) },
              modifier = Modifier
                .weight(1f)
                .testTag("open_rules_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = stringResource(R.string.rules_button_label),
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text("Rules", maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }
          }
        }

        // Error or Reward Toast Banner if any
        val activeBanner = uiState.toastMessage ?: uiState.errorMessage ?: authErrorBanner
        if (activeBanner != null) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = if (uiState.toastMessage != null) {
                EucalyptusSage.copy(alpha = 0.18f)
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              }
            ),
            shape = RoundedCornerShape(16.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = activeBanner,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
              )
              TextButton(
                onClick = {
                  viewModel.clearErrorMessage()
                  authErrorBanner = null
                }
              ) {
                Text("Dismiss")
              }
            }
          }
        }

        // Hero Banner + Player Career Stats, Coins & XP Level Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
            ) {
              Image(
                painter = painterResource(id = R.drawable.img_ludo_atelier_banner),
                contentDescription = "Royal Ludo 3D Board Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
            }

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
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(42.dp)
                      .clip(CircleShape)
                      .background(Color(uiState.selectedAvatar.primaryColorHex).copy(alpha = 0.24f))
                      .border(1.5.dp, Color(uiState.selectedAvatar.ringColorHex), CircleShape)
                      .clickable { viewModel.openProfileScreen() },
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = uiState.selectedAvatar.badgeSymbol,
                      style = MaterialTheme.typography.titleMedium
                    )
                  }
                  Column {
                    Text(
                      text = if (viewModel.isAuthenticated) {
                        "${profile?.displayName ?: uiState.localDisplayName} ${countryInfo.flagEmoji} • Lv.$playerLevel"
                      } else {
                        "${uiState.localDisplayName} ${countryInfo.flagEmoji} • Lv.$playerLevel"
                      },
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "$currentCoins Royal Coins (≈ ${CountryCatalog.formatCoinsEquivalent(currentCoins, activeCountryCode)}) • $currentXp XP",
                      style = MaterialTheme.typography.bodySmall,
                      color = BrushedBrass,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  OutlinedButton(
                    onClick = { showLocationCurrencyDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("lobby_location_currency_button")
                  ) {
                    Text(
                      text = "${countryInfo.flagEmoji} ${countryInfo.currencyCode}",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold
                    )
                  }

                  if (viewModel.isAuthenticated) {
                    IconButton(
                      onClick = { showEditNameDialog = true },
                      modifier = Modifier.testTag("edit_display_name_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Player Name",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                StatBadge(
                  label = "Coins (${countryInfo.currencySymbol.trim()})",
                  value = currentCoins.toString(),
                  modifier = Modifier.weight(1f)
                )
                StatBadge(
                  label = "XP",
                  value = currentXp.toString(),
                  modifier = Modifier.weight(1f)
                )
                StatBadge(
                  label = "Score",
                  value = (profile?.score ?: 100L).toString(),
                  modifier = Modifier.weight(1f)
                )
                StatBadge(
                  label = "Wins",
                  value = maxOf((profile?.gamesWon ?: 0L).toInt(), uiState.sessionWinsCount).toString(),
                  modifier = Modifier.weight(1f)
                )
              }

              OutlinedButton(
                onClick = { viewModel.openProfileScreen() },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("lobby_profile_card_button"),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = BrushedBrass
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "View Player Profile • Global Rank • Win/Loss & Equipped Emotes",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold
                )
              }

              Button(
                onClick = { viewModel.openTutorialScreen() },
                colors = ButtonDefaults.buttonColors(
                  containerColor = BrushedBrass.copy(alpha = 0.22f),
                  contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .border(1.dp, BrushedBrass, RoundedCornerShape(12.dp))
                  .testTag("lobby_how_to_play_tutorial_button"),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.School,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = BrushedBrass
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Interactive 'How to Play' 3D Tutorial • 6 Hands-On Lessons (+150 Coins)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }
          }
        }

        // Weekly Tournament Live Bracket Spotlight Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, BrushedBrass.copy(alpha = 0.7f), RoundedCornerShape(24.dp)),
          shape = RoundedCornerShape(24.dp),
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
                  imageVector = Icons.Default.EmojiEvents,
                  contentDescription = null,
                  tint = BrushedBrass
                )
                Column {
                  Text(
                    text = "Weekly Tournament • ${uiState.tournamentCurrentRound.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Ends in ${com.example.data.WeeklyTournamentCatalog.formatRemainingTime(uiState.tournamentRemainingSeconds)} • ${uiState.tournamentWins}/3 Wins",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrushedBrass
                  )
                }
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(999.dp))
                  .background(EucalyptusSage.copy(alpha = 0.2f))
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "LIVE LADDER",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = EucalyptusSage
                )
              }
            }

            Text(
              text = uiState.tournamentLiveFeedMessage,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = { viewModel.openWeeklyTournamentScreen() },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("lobby_open_tournament_card_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = BrushedBrass,
                  contentColor = Color.Black
                )
              ) {
                Text("View Ladder Bracket", fontWeight = FontWeight.ExtraBold)
              }

              OutlinedButton(
                onClick = { viewModel.startTournamentBracketMatch() },
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("lobby_play_tournament_match_button"),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = BrushedBrass
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play ${uiState.tournamentCurrentRound.shortLabel}", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // Daily Challenges Section (Main Menu Daily Missions for Extra Currency & XP)
        DailyChallengesCard(
          profile = profile,
          rewardMultiplier = adminConfig.missionRewardMultiplier.coerceIn(1L, 10L),
          viewModel = viewModel
        )

        // Card 1: Offline 3D Studio (Supports 2, 4, 6, and 8 Simultaneous Players + Custom Color Picker)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = BrushedBrass
              )
              Column {
                Text(
                  text = stringResource(R.string.offline_studio_title),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Play 2, 4, 6, or 8 players simultaneously • Tap any color dot to customize",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Player Count Selector (2, 4, 6, 8 Simultaneous Players)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Simultaneous Players & 3D Board Topology:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf(2, 4, 6, 8).forEach { count ->
                  val isSelected = uiState.offlinePlayerCount == count
                  FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setOfflinePlayerCount(count) },
                    label = { Text("${count}P") },
                    leadingIcon = if (isSelected) {
                      {
                        Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = null,
                          modifier = Modifier.size(15.dp)
                        )
                      }
                    } else null,
                    modifier = Modifier
                      .weight(1f)
                      .testTag("offline_player_count_$count")
                  )
                }
              }
            }

            // Seat List with Custom Color Swatch Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              uiState.offlineDraftSeats.take(uiState.offlinePlayerCount.coerceAtLeast(4)).forEach { seat ->
                val (colorPrimary, _, _) = paletteForColor(seat.color)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                      if (seat.isEnabled) MaterialTheme.colorScheme.surfaceVariant
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    // Tappable Color Swatch Button to pick any of the 8 colors
                    Box(
                      modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colorPrimary)
                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable { editingColorForSeatIndex = seat.seatIndex }
                        .testTag("seat_color_picker_${seat.seatIndex}"),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Change Seat Color",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                      )
                    }
                    Column {
                      Text(
                        text = seat.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = "${seat.color.label} • ${if (!seat.isEnabled) "Off" else if (seat.isBot) "AI Bot" else "Human"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    if (seat.isEnabled) {
                      FilterChip(
                        selected = seat.isBot,
                        onClick = {
                          viewModel.updateOfflineSeatDraft(seat.seatIndex, isBot = !seat.isBot)
                        },
                        label = { Text(if (seat.isBot) "AI" else "Human") },
                        leadingIcon = {
                          Icon(
                            imageVector = if (seat.isBot) Icons.Default.SmartToy else Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                          )
                        }
                      )
                    }
                    Switch(
                      checked = seat.isEnabled,
                      onCheckedChange = { enabled ->
                        viewModel.updateOfflineSeatDraft(seat.seatIndex, isEnabled = enabled)
                      }
                    )
                  }
                }
              }
            }

            Button(
              onClick = { viewModel.startOfflineGame() },
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("start_offline_game_button"),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = BrushedBrass,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              val enabledCount = uiState.offlineDraftSeats.count { it.isEnabled }
              Text(
                text = "Launch ${enabledCount}-Player 3D Match",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // Card 2: Online 3D Multiplayer (2, 4, 6, 8 Players + Custom Color Picker + Voice Rooms)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = TerracottaRed
              )
              Column {
                Text(
                  text = "Online 3D Multiplayer & Voice",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "2, 4, 6, or 8 Players • 1-on-1 & Group Audio Calls",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            if (!viewModel.isAuthenticated) {
              Button(
                onClick = { triggerOnlineGoogleSignIn() },
                enabled = !isSigningInForOnline,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                )
              ) {
                if (isSigningInForOnline) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = stringResource(R.string.sign_in_with_google),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }

            // Room Size Selector: 2, 4, 6, or 8 Players
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Online Room Capacity:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf(2, 4, 6, 8).forEach { count ->
                  FilterChip(
                    selected = selectedOnlineMaxPlayers == count,
                    onClick = { selectedOnlineMaxPlayers = count },
                    label = { Text("${count}P") },
                    leadingIcon = if (selectedOnlineMaxPlayers == count) {
                      {
                        Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = null,
                          modifier = Modifier.size(15.dp)
                        )
                      }
                    } else null,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }

            // Preferred Player Color Swatch Bar (8 Colors)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Your Preferred Color: ${uiState.preferredOnlineColor.label}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              ColorPaletteSelectorRow(
                selectedColor = uiState.preferredOnlineColor,
                takenColors = emptySet(),
                onColorSelected = { viewModel.setPreferredOnlineColor(it) }
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = {
                  if (viewModel.isAuthenticated) {
                    viewModel.startQuickOnlineMatch(selectedOnlineMaxPlayers)
                  } else {
                    triggerOnlineGoogleSignIn()
                  }
                },
                enabled = !uiState.isLoadingAction && !isSigningInForOnline,
                modifier = Modifier
                  .weight(1f)
                  .height(50.dp)
                  .testTag("quick_match_button"),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Bolt,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Quick Match", maxLines = 1, overflow = TextOverflow.Ellipsis)
              }

              OutlinedButton(
                onClick = {
                  if (viewModel.isAuthenticated) {
                    viewModel.createOnlinePrivateRoom(
                      maxPlayers = selectedOnlineMaxPlayers,
                      isPublic = false
                    )
                  } else {
                    triggerOnlineGoogleSignIn()
                  }
                },
                enabled = !uiState.isLoadingAction && !isSigningInForOnline,
                modifier = Modifier
                  .weight(1f)
                  .height(50.dp)
                  .testTag("create_private_room_button"),
                shape = RoundedCornerShape(14.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.VpnKey,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Room", maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Private Room Code & Live Observer",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Firestore Real-Time Sync",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = BrushedBrass
                )
              }
              Text(
                text = "Enter a 6-character code to join a waiting room or observe an ongoing Private Room in View-Only Mode without interacting with the board.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedTextField(
                  value = joinCodeInput,
                  onValueChange = { joinCodeInput = it.uppercase().take(8) },
                  label = { Text("Enter Private Room Code") },
                  placeholder = { Text("e.g. PRV409") },
                  singleLine = true,
                  modifier = Modifier
                    .weight(1f)
                    .testTag("join_room_code_input"),
                  shape = RoundedCornerShape(14.dp)
                )

                Button(
                  onClick = {
                    val isOngoingShowcase = FriendsStudioCatalog.ONGOING_PRIVATE_ROOMS_SHOWCASE.any {
                      it.roomCode.equals(joinCodeInput.trim(), ignoreCase = true)
                    }
                    if (viewModel.isAuthenticated || isOngoingShowcase) {
                      viewModel.joinOnlineRoomByCode(joinCodeInput)
                    } else {
                      triggerOnlineGoogleSignIn()
                    }
                  },
                  enabled = !uiState.isLoadingAction && !isSigningInForOnline && joinCodeInput.trim().length >= 4,
                  modifier = Modifier
                    .height(54.dp)
                    .testTag("join_room_code_button"),
                  shape = RoundedCornerShape(14.dp)
                ) {
                  Text("Join")
                }

                OutlinedButton(
                  onClick = {
                    viewModel.spectateOnlineRoomByCode(joinCodeInput)
                  },
                  enabled = !uiState.isLoadingAction && joinCodeInput.trim().length >= 4,
                  modifier = Modifier
                    .height(54.dp)
                    .testTag("spectate_room_code_button"),
                  shape = RoundedCornerShape(14.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Spectate Private Room in View-Only Mode",
                    modifier = Modifier.size(16.dp),
                    tint = BrushedBrass
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Watch")
                }
              }

              // Ongoing Private Rooms Quick-Spectate Strip
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                  .border(1.dp, BrushedBrass.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                  .padding(12.dp)
                  .testTag("ongoing_private_rooms_strip"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Visibility,
                      contentDescription = null,
                      tint = BrushedBrass,
                      modifier = Modifier.size(15.dp)
                    )
                    Text(
                      text = "ONGOING PRIVATE ROOMS • TAP TO OBSERVE (VIEW-ONLY)",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.ExtraBold,
                      color = BrushedBrass
                    )
                  }
                }

                if (ongoingCloudPrivateRooms.isNotEmpty()) {
                  ongoingCloudPrivateRooms.take(3).forEach { privRoom ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable {
                          joinCodeInput = privRoom.roomCode
                          viewModel.spectateOnlineRoomByCode(privRoom.roomCode)
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("spectate_private_room_${privRoom.roomCode}"),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = "Private Room #${privRoom.roomCode} (${privRoom.maxPlayers}P)",
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                          text = "Host: ${privRoom.playerNames.firstOrNull() ?: "Player"} • Turn ${privRoom.turnNumber} • Live Firestore Sync",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                      Text(
                        text = "Observe →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrushedBrass
                      )
                    }
                  }
                }

                FlowRow(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  FriendsStudioCatalog.ONGOING_PRIVATE_ROOMS_SHOWCASE.forEach { preset ->
                    OutlinedButton(
                      onClick = {
                        joinCodeInput = preset.roomCode
                        viewModel.spectateOnlineRoomByCode(
                          codeInput = preset.roomCode,
                          friendNameHint = preset.hostName
                        )
                      },
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier
                        .weight(1f)
                        .testTag("spectate_private_room_${preset.roomCode}")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Observe Private Room ${preset.roomCode}",
                        tint = BrushedBrass,
                        modifier = Modifier.size(14.dp)
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        text = "#${preset.roomCode} (${preset.maxPlayers}P)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                      )
                    }
                  }
                }
              }
            }

            if (viewModel.isAuthenticated && openRooms.isNotEmpty()) {
              Text(
                text = "Open Public Lobbies (${openRooms.size})",
                style = MaterialTheme.typography.labelLarge,
                color = BrushedBrass
              )
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                openRooms.take(3).forEach { room ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                      .clickable { viewModel.joinOnlineRoomByCode(room.roomCode) }
                      .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(
                        text = "Room #${room.roomCode} (${room.maxPlayers}P)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                      )
                      Text(
                        text = "Host: ${room.playerNames.firstOrNull() ?: "Player"} • ${room.memberUids.size}/${room.maxPlayers} seated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                    Text(
                      text = "JOIN →",
                      style = MaterialTheme.typography.labelLarge,
                      color = TerracottaRed
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Custom Color Picker Dialog for Offline Seat
  val activeSeatIdxForColor = editingColorForSeatIndex
  if (activeSeatIdxForColor != null) {
    val seatDraft = uiState.offlineDraftSeats.getOrNull(activeSeatIdxForColor)
    AlertDialog(
      onDismissRequest = { editingColorForSeatIndex = null },
      title = { Text("Choose Color for ${seatDraft?.name ?: "Seat"}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          PlayerColor.entries.forEach { colorOption ->
            val (primary, _, _) = paletteForColor(colorOption)
            val isCurrent = seatDraft?.color == colorOption
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (isCurrent) primary.copy(alpha = 0.18f)
                  else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable {
                  viewModel.updateOfflineSeatDraft(activeSeatIdxForColor, newColor = colorOption)
                  editingColorForSeatIndex = null
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(primary)
                )
                Text(
                  text = colorOption.label,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
              }
              if (isCurrent) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = primary
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { editingColorForSeatIndex = null }) {
          Text("Done")
        }
      }
    )
  }

  if (showEditNameDialog) {
    var draftName by remember { mutableStateOf(profile?.displayName ?: "Royal Player") }
    AlertDialog(
      onDismissRequest = { showEditNameDialog = false },
      title = { Text("Update Display Name") },
      text = {
        OutlinedTextField(
          value = draftName,
          onValueChange = { draftName = it.take(40) },
          label = { Text("Display Name") },
          singleLine = true
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.updateDisplayName(draftName)
            showEditNameDialog = false
          }
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditNameDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showLocationCurrencyDialog) {
    LocationCurrencyPickerDialog(
      selectedCountryCode = activeCountryCode,
      onSelectCountry = { code ->
        viewModel.updateUserCountry(code)
        showLocationCurrencyDialog = false
      },
      onDismiss = { showLocationCurrencyDialog = false }
    )
  }

  if (uiState.showRulesModal) {
    LiveLudoRulesDialog(
      onDismiss = { viewModel.toggleRulesModal(false) },
      onStartInteractiveTutorial = { viewModel.openTutorialScreen() }
    )
  }
}

@Composable
private fun DailyChallengesCard(
  profile: PlayerProfile?,
  rewardMultiplier: Long,
  viewModel: LudoGameViewModel
) {
  val completedCount = DailyMissionCatalog.MISSIONS.count { m ->
    viewModel.missionProgressCount(m, profile) >= m.targetCount
  }
  val readyToClaimCount = DailyMissionCatalog.MISSIONS.count { m ->
    viewModel.missionProgressCount(m, profile) >= m.targetCount &&
      !viewModel.isMissionClaimed(m.id, profile)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
      .testTag("daily_challenges_card"),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = null,
            tint = BrushedBrass
          )
          Column {
            Text(
              text = stringResource(R.string.daily_challenges_title),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Earn Royal Coins by completing objectives ('Win 3 games', 'Roll a 6 five times') • Progress synced in Firestore",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(EucalyptusSage.copy(alpha = 0.18f))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "$completedCount/${DailyMissionCatalog.MISSIONS.size} DONE",
              style = MaterialTheme.typography.labelSmall,
              color = EucalyptusSage,
              fontWeight = FontWeight.ExtraBold
            )
          }
          if (rewardMultiplier > 1L) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(BrushedBrass.copy(alpha = 0.22f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "${rewardMultiplier}x BONUS",
                style = MaterialTheme.typography.labelSmall,
                color = BrushedBrass,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      if (readyToClaimCount > 1) {
        Button(
          onClick = { viewModel.claimAllReadyDailyMissions(profile) },
          modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .testTag("claim_all_daily_missions_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = EucalyptusSage,
            contentColor = Color.White
          )
        ) {
          Text(
            text = "Claim All $readyToClaimCount Completed Missions",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DailyMissionCatalog.MISSIONS.forEach { mission ->
          val progress = viewModel.missionProgressCount(mission, profile).coerceAtMost(mission.targetCount)
          val isCompleted = progress >= mission.targetCount
          val isClaimed = viewModel.isMissionClaimed(mission.id, profile)
          val effectiveCoins = mission.rewardCoins * rewardMultiplier
          val effectiveXp = mission.rewardXp * rewardMultiplier

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(14.dp)
              .testTag("daily_mission_card_${mission.id}"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(
                  text = mission.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = mission.description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              when {
                isClaimed -> {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = "Claimed",
                      tint = EucalyptusSage,
                      modifier = Modifier.size(18.dp)
                    )
                    Text(
                      text = "Claimed",
                      style = MaterialTheme.typography.labelMedium,
                      color = EucalyptusSage,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
                isCompleted -> {
                  Button(
                    onClick = { viewModel.claimDailyMission(mission, profile) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = BrushedBrass,
                      contentColor = Color.White
                    ),
                    modifier = Modifier
                      .height(36.dp)
                      .testTag("claim_mission_${mission.id}")
                  ) {
                    Text(
                      text = "Claim +$effectiveCoins",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
                else -> {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(MaterialTheme.colorScheme.surface)
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.MonetizationOn,
                      contentDescription = null,
                      tint = BrushedBrass,
                      modifier = Modifier.size(14.dp)
                    )
                    Text(
                      text = "+$effectiveCoins • +${effectiveXp}XP",
                      style = MaterialTheme.typography.labelSmall,
                      color = BrushedBrass,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              LinearProgressIndicator(
                progress = {
                  (progress.toFloat() / mission.targetCount.toFloat()).coerceIn(0f, 1f)
                },
                modifier = Modifier
                  .weight(1f)
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = if (isCompleted) EucalyptusSage else BrushedBrass,
                trackColor = MaterialTheme.colorScheme.surface
              )
              Text(
                text = "$progress/${mission.targetCount}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) EucalyptusSage else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPaletteSelectorRow(
  selectedColor: PlayerColor,
  takenColors: Set<PlayerColor>,
  onColorSelected: (PlayerColor) -> Unit
) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    PlayerColor.entries.forEach { color ->
      val (primary, _, _) = paletteForColor(color)
      val isSelected = color == selectedColor
      val isTaken = color in takenColors && !isSelected
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(if (isTaken) primary.copy(alpha = 0.28f) else primary)
          .border(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) BrushedBrass else Color.White.copy(alpha = 0.6f),
            shape = CircleShape
          )
          .clickable(enabled = !isTaken) { onColorSelected(color) }
          .testTag("color_swatch_${color.name}"),
        contentAlignment = Alignment.Center
      ) {
        if (isSelected) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = color.label,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun StatBadge(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .padding(vertical = 10.dp, horizontal = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
fun WaitingRoomScreen(
  uiState: LudoUiState,
  viewModel: LudoGameViewModel
) {
  BackHandler {
    viewModel.leaveToLobby()
  }

  val clipboardManager = LocalClipboardManager.current
  var copiedToast by remember { mutableStateOf(false) }

  val mySeat = uiState.seats.firstOrNull { it.uid == viewModel.currentUserId }
  val takenColors = uiState.seats.map { it.color }.toSet()

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.Center
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 520.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          IconButton(
            onClick = { viewModel.leaveToLobby() },
            modifier = Modifier.testTag("leave_waiting_room_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Lobby"
            )
          }
          Text(
            text = if (uiState.isPublicRoom) "Public ${uiState.maxPlayers}P Room" else "Private ${uiState.maxPlayers}P Room",
            style = MaterialTheme.typography.titleMedium
          )
          Spacer(modifier = Modifier.width(48.dp))
        }

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text(
              text = "ROOM INVITE CODE",
              style = MaterialTheme.typography.labelMedium,
              color = BrushedBrass
            )
            Text(
              text = uiState.roomCode,
              style = MaterialTheme.typography.displayLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(uiState.roomCode))
                copiedToast = true
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("copy_room_code_button")
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Room Code",
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(if (copiedToast) "Code Copied!" else "Copy Invite Code")
            }

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 6.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

            // Custom Color Picker for the seated player
            if (mySeat != null) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "Choose Your Pawn Color (${mySeat.color.label}):",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ColorPaletteSelectorRow(
                  selectedColor = mySeat.color,
                  takenColors = takenColors,
                  onColorSelected = { viewModel.selectOnlineSeatColor(it) }
                )
              }

              HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
              )
            }

            Text(
              text = "Seated Players (${uiState.seats.size}/${uiState.maxPlayers})",
              style = MaterialTheme.typography.titleSmall
            )

            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              uiState.seats.forEach { seat ->
                val (colorPrimary, _, _) = paletteForColor(seat.color)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(colorPrimary)
                    )
                    Text(
                      text = seat.name,
                      style = MaterialTheme.typography.titleSmall
                    )
                  }
                  Text(
                    text = seat.color.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }

        if (uiState.isHost) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { viewModel.hostStartOnlineMatch(fillWithBots = true) },
              enabled = !uiState.isLoadingAction,
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("host_start_with_bots_button"),
              shape = RoundedCornerShape(16.dp)
            ) {
              if (uiState.isLoadingAction) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = MaterialTheme.colorScheme.onPrimary,
                  strokeWidth = 2.dp
                )
              } else {
                Text(
                  text = if (uiState.seats.size < uiState.maxPlayers) {
                    "Fill Empty Seats with AI & Start (${uiState.maxPlayers}P)"
                  } else {
                    "Start ${uiState.maxPlayers}P 3D Online Match"
                  },
                  style = MaterialTheme.typography.titleMedium
                )
              }
            }

            if (uiState.seats.size >= 2 && uiState.seats.size < uiState.maxPlayers) {
              OutlinedButton(
                onClick = { viewModel.hostStartOnlineMatch(fillWithBots = false) },
                enabled = !uiState.isLoadingAction,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp),
                shape = RoundedCornerShape(14.dp)
              ) {
                Text("Start with ${uiState.seats.size} Human Players Only")
              }
            }
          }
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(
              text = "Waiting for host to launch the 3D board...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
fun LiveLudoRulesDialog(
  onDismiss: () -> Unit,
  onStartInteractiveTutorial: (() -> Unit)? = null
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Royal Ludo 3D — Official Rules",
        style = MaterialTheme.typography.headlineMedium
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        RuleSection(
          title = "1. Dual-Dice Roll & Sequential Selection",
          body = "Roll 2 3D dice simultaneously on your turn. Tap either Die 1 or Die 2 to move a pawn first, and then play the second die sequentially."
        )
        RuleSection(
          title = "2. Entering the Board & Bonus Rolls",
          body = "Rolling a 6 on either die deploys a pawn from your yard onto your starting square and awards a bonus roll after both dice are resolved. Capturing an opponent or reaching Home also grants a bonus roll."
        )
        RuleSection(
          title = "3. Three Consecutive 6s Penalty",
          body = "Rolling 6s three times in a row within the same turn forfeits your turn."
        )
        RuleSection(
          title = "4. 2, 4, 6 & 8 Simultaneous Players",
          body = "Play on classic 4-arm cross boards (2–4 players), 6-arm hexagonal 3D boards (6 players), or 8-arm octagonal 3D boards (8 players) with 8 custom architectural colors."
        )
        RuleSection(
          title = "5. Safe Stars & Golden Blockades",
          body = "Colored starting squares and brushed-brass Star squares protect pawns from capture. Two friendly pawns on the same outer square form an impassable golden Blockade shield."
        )
        RuleSection(
          title = "6. Global & Country Leaderboard Scoring",
          body = "Earn +100 pts per Win, +25 pts per Capture, and +15 pts per Home Pawn (-15 pts on loss, -25 pts on forfeit)."
        )
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onStartInteractiveTutorial != null) {
          OutlinedButton(
            onClick = {
              onDismiss()
              onStartInteractiveTutorial()
            },
            modifier = Modifier.testTag("launch_interactive_tutorial_from_rules_button")
          ) {
            Icon(
              imageVector = Icons.Default.School,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = BrushedBrass
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Interactive 3D Tutorial")
          }
        }
        Button(
          onClick = onDismiss,
          modifier = Modifier.testTag("close_rules_dialog_button")
        ) {
          Text("Got It")
        }
      }
    }
  )
}

@Composable
private fun RuleSection(title: String, body: String) {
  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = BrushedBrass,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = body,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
