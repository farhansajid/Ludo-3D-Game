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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.CountryCatalog
import com.example.data.PlayerProfile
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.TerracottaRed
import com.example.ui.viewmodel.DataStreamState
import com.example.ui.viewmodel.LudoGameViewModel
import com.example.ui.viewmodel.LudoUiState

@Composable
fun LeaderboardScreen(
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

  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Global, 1 = Country
  var showCountryPickerDialog by remember { mutableStateOf(false) }

  val myProfile = (profileState as? DataStreamState.Success)?.data
  val selectedCountryInfo = CountryCatalog.fromCode(uiState.selectedCountryFilter)
  val activeListState = if (selectedTab == 0) globalLeaderboardState else countryLeaderboardState
  val players = (activeListState as? DataStreamState.Success)?.data.orEmpty()

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
          .fillMaxSize()
          .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Top Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            IconButton(
              onClick = { viewModel.leaveToLobby() },
              modifier = Modifier.testTag("leaderboard_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Lobby"
              )
            }
            Column {
              Text(
                text = "Royal Hall of Ranks",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = "+100 Win • +25 Capture • +15 Home • -15 Loss",
                style = MaterialTheme.typography.labelMedium,
                color = BrushedBrass
              )
            }
          }

          OutlinedButton(
            onClick = { showCountryPickerDialog = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("select_country_button")
          ) {
            Text("${selectedCountryInfo.flagEmoji} ${selectedCountryInfo.code}")
          }
        }

        // Signed-in User Summary Card
        if (viewModel.isAuthenticated && myProfile != null) {
          val myCountry = CountryCatalog.fromCode(myProfile.countryCode)
          val myRankIndex = players.indexOfFirst { it.userId == myProfile.userId }
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .border(1.5.dp, BrushedBrass, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BrushedBrass.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = if (myRankIndex >= 0) "#${myRankIndex + 1}" else myCountry.flagEmoji,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrushedBrass
                  )
                }
                Column {
                  Text(
                    text = "${myProfile.displayName} ${myCountry.flagEmoji}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "${myProfile.gamesWon} Wins • ${myProfile.capturesTotal} Captures • ${myProfile.homesReachedTotal} Home",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "${myProfile.score} PTS",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = BrushedBrass
                )
                Text(
                  text = myCountry.name,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else if (!viewModel.isAuthenticated) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Sign in to Rank Globally & By Country",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Save your match score, captures, and country flag on the live leaderboard.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Button(
                onClick = {
                  onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {},
                    onAuthError = {},
                    scope = scope
                  )
                }
              ) {
                Text("Sign In")
              }
            }
          }
        }

        // Global vs Country Filter Tabs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          FilterChip(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            label = { Text("🌍 Global Leaderboard") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            },
            modifier = Modifier
              .weight(1f)
              .testTag("tab_global_leaderboard")
          )

          FilterChip(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            label = { Text("${selectedCountryInfo.flagEmoji} ${selectedCountryInfo.name}") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            },
            modifier = Modifier
              .weight(1f)
              .testTag("tab_country_leaderboard")
          )
        }

        // Leaderboard List Content
        when (activeListState) {
          is DataStreamState.Loading -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(color = BrushedBrass)
            }
          }
          is DataStreamState.Error -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = activeListState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          is DataStreamState.Success -> {
            if (players.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                contentAlignment = Alignment.Center
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = BrushedBrass,
                    modifier = Modifier.size(42.dp)
                  )
                  Text(
                    text = if (selectedTab == 0) {
                      "No ranked players yet — complete a match to claim #1!"
                    } else {
                      "No ranked players in ${selectedCountryInfo.name} yet!"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                itemsIndexed(players, key = { _, item -> item.userId.ifBlank { item.id } }) { index, player ->
                  val rank = index + 1
                  val cInfo = CountryCatalog.fromCode(player.countryCode)
                  val isMe = player.userId == viewModel.currentUserId
                  val badgeColor = when (rank) {
                    1 -> BrushedBrass
                    2 -> EucalyptusSage
                    3 -> TerracottaRed
                    else -> MaterialTheme.colorScheme.surfaceVariant
                  }

                  Card(
                    modifier = Modifier
                      .fillMaxWidth()
                      .border(
                        width = if (isMe || rank <= 3) 1.5.dp else 1.dp,
                        color = if (isMe) BrushedBrass else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp)
                      ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                      containerColor = if (isMe) {
                        BrushedBrass.copy(alpha = 0.10f)
                      } else {
                        MaterialTheme.colorScheme.surface
                      }
                    )
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = if (rank <= 3) 0.24f else 1f)),
                          contentAlignment = Alignment.Center
                        ) {
                          Text(
                            text = "#$rank",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (rank <= 3) badgeColor else MaterialTheme.colorScheme.onSurface
                          )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = "${cInfo.flagEmoji} ${player.displayName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Text(
                            text = "${cInfo.name} • ${player.gamesWon}W / ${player.gamesPlayed}G • ${player.capturesTotal} Captures",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                      }

                      Column(horizontalAlignment = Alignment.End) {
                        Text(
                          text = "${player.score} pts",
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = if (rank == 1) BrushedBrass else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                          text = "${player.homesReachedTotal} Home",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }
  }

  if (showCountryPickerDialog) {
    AlertDialog(
      onDismissRequest = { showCountryPickerDialog = false },
      title = { Text("Select Country for Leaderboard & Profile") },
      text = {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          itemsIndexed(CountryCatalog.COUNTRIES) { _, country ->
            val isSelected = country.code == uiState.selectedCountryFilter
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (isSelected) BrushedBrass.copy(alpha = 0.22f)
                  else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable {
                  viewModel.updateUserCountry(country.code)
                  selectedTab = 1
                  showCountryPickerDialog = false
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${country.flagEmoji}  ${country.name}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
              Text(
                text = country.code,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCountryPickerDialog = false }) {
          Text("Close")
        }
      }
    )
  }
}
