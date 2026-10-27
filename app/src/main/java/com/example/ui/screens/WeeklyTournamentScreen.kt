package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CountryCatalog
import com.example.data.PlayerProfile
import com.example.data.TournamentBracketMatch
import com.example.data.TournamentCosmeticReward
import com.example.data.TournamentRoundTier
import com.example.data.WeeklyTournamentCatalog
import com.example.ui.board3d.paletteForColor
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.TerracottaRed
import com.example.ui.viewmodel.DataStreamState
import com.example.ui.viewmodel.LudoGameViewModel
import com.example.ui.viewmodel.LudoUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeeklyTournamentScreen(
  uiState: LudoUiState,
  profileState: DataStreamState<PlayerProfile?>,
  viewModel: LudoGameViewModel
) {
  BackHandler {
    viewModel.leaveToLobby()
  }

  val profile = (profileState as? DataStreamState.Success)?.data
  val currentCoins = if (viewModel.isAuthenticated && profile != null) {
    profile.coins
  } else {
    uiState.localGuestCoins
  }

  var selectedRoundFilter by remember { mutableStateOf<TournamentRoundTier?>(null) }
  val countdownParts = remember(uiState.tournamentRemainingSeconds) {
    WeeklyTournamentCatalog.splitRemainingTime(uiState.tournamentRemainingSeconds)
  }
  val formattedCountdown = remember(uiState.tournamentRemainingSeconds) {
    WeeklyTournamentCatalog.formatRemainingTime(uiState.tournamentRemainingSeconds)
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
          .widthIn(max = 640.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Top Navigation & Season Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
          ) {
            IconButton(
              onClick = { viewModel.leaveToLobby() },
              modifier = Modifier.testTag("tournament_back_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to Lobby"
              )
            }
            Column {
              Text(
                text = "Weekly Tournament",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
              )
              Text(
                text = uiState.tournamentSeasonTitle,
                style = MaterialTheme.typography.labelMedium,
                color = BrushedBrass,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(BrushedBrass.copy(alpha = 0.16f))
                .border(1.dp, BrushedBrass.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = "🏆 ${uiState.tournamentLadderPoints} LP • $currentCoins Coins",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = BrushedBrass
              )
            }
          }
        }

        // Toast or Error Notification Banner
        val activeNotification = uiState.toastMessage ?: uiState.errorMessage
        if (activeNotification != null) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = if (uiState.toastMessage != null) {
                EucalyptusSage.copy(alpha = 0.2f)
              } else {
                MaterialTheme.colorScheme.errorContainer
              }
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = activeNotification,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = { viewModel.clearErrorMessage() }) {
                Text("Dismiss")
              }
            }
          }
        }

        // Live Tournament Countdown & Progress Ticker Card
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, BrushedBrass.copy(alpha = 0.65f), RoundedCornerShape(22.dp)),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(EucalyptusSage)
                )
                Text(
                  text = "LIVE WEEKLY BRACKET",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = EucalyptusSage
                )
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Schedule,
                  contentDescription = null,
                  tint = BrushedBrass,
                  modifier = Modifier.size(15.dp)
                )
                Text(
                  text = "Ends in $formattedCountdown",
                  style = MaterialTheme.typography.labelMedium,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = BrushedBrass,
                  modifier = Modifier.testTag("tournament_remaining_time_text")
                )
              }
            }

            // 4-Unit Swiss Grid Countdown Display
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CountdownUnitBox(
                value = "%02d".format(countdownParts.days),
                label = "DAYS",
                modifier = Modifier.weight(1f)
              )
              CountdownUnitBox(
                value = "%02d".format(countdownParts.hours),
                label = "HOURS",
                modifier = Modifier.weight(1f)
              )
              CountdownUnitBox(
                value = "%02d".format(countdownParts.minutes),
                label = "MINS",
                modifier = Modifier.weight(1f)
              )
              CountdownUnitBox(
                value = "%02d".format(countdownParts.seconds),
                label = "SECS",
                modifier = Modifier.weight(1f)
              )
            }

            // Live Bracket Progress Stepper
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Current Stage: ${uiState.tournamentCurrentRound.title}",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${uiState.tournamentWins} / 3 Bracket Wins",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = BrushedBrass
                )
              }

              LinearProgressIndicator(
                progress = { (uiState.tournamentWins.toFloat() / 3f).coerceIn(0.08f, 1f) },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(RoundedCornerShape(999.dp)),
                color = BrushedBrass,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                TournamentRoundTier.entries.forEach { tier ->
                  val isReached = uiState.tournamentCurrentRound.roundIndex >= tier.roundIndex
                  val isCurrent = uiState.tournamentCurrentRound == tier
                  Text(
                    text = tier.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                    color = when {
                      isCurrent -> BrushedBrass
                      isReached -> EucalyptusSage
                      else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                  )
                }
              }
            }

            // Live Contender Feed Ticker
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = uiState.tournamentLiveFeedMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("tournament_live_feed_banner")
              )
            }
          }
        }

        // Next Bracket Match Action Card OR Champion Podium Card
        ActiveTournamentRoundCard(
          uiState = uiState,
          onPlay3DMatch = { viewModel.startTournamentBracketMatch() },
          onInstantResolveRound = { viewModel.advanceTournamentBracketRoundOnVictory(profile) },
          onResetBracket = { viewModel.resetTournamentBracketRun() }
        )

        // Ladder Bracket Tree Visualizer Section
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Ladder Bracket Tree",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "8 Seeded Contenders • Single-Elimination Knockout Ladder",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Round Filter Chips
            FlowRow(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              FilterChip(
                selected = selectedRoundFilter == null,
                onClick = { selectedRoundFilter = null },
                label = { Text("All Rounds") },
                modifier = Modifier.testTag("bracket_filter_all")
              )
              listOf(
                TournamentRoundTier.QUARTERFINALS,
                TournamentRoundTier.SEMIFINALS,
                TournamentRoundTier.GRAND_FINALS
              ).forEach { roundTier ->
                FilterChip(
                  selected = selectedRoundFilter == roundTier,
                  onClick = { selectedRoundFilter = roundTier },
                  label = { Text("${roundTier.title} (${roundTier.shortLabel})") },
                  modifier = Modifier.testTag("bracket_filter_${roundTier.shortLabel}")
                )
              }
            }

            val roundsToDisplay = if (selectedRoundFilter != null) {
              listOf(selectedRoundFilter!!)
            } else {
              listOf(
                TournamentRoundTier.QUARTERFINALS,
                TournamentRoundTier.SEMIFINALS,
                TournamentRoundTier.GRAND_FINALS
              )
            }

            roundsToDisplay.forEach { roundTier ->
              val roundMatches = uiState.tournamentBracketMatches.filter { it.roundTier == roundTier }
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "${roundTier.title.uppercase()} • ${roundTier.matchFormatLabel}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (uiState.tournamentCurrentRound == roundTier) {
                      BrushedBrass
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    }
                  )
                }

                roundMatches.forEach { match ->
                  BracketMatchNodeCard(match = match)
                }
              }
            }
          }
        }

        // Exclusive Cosmetic Rewards Ladder Section
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = BrushedBrass
              )
              Column {
                Text(
                  text = "Exclusive Cosmetic Rewards",
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Advance up the weekly bracket ladder to unlock & equip limited-edition cosmetics",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            WeeklyTournamentCatalog.EXCLUSIVE_REWARDS.forEach { reward ->
              val isUnlocked = viewModel.isTournamentRewardUnlocked(reward, profile)
              val isClaimed = viewModel.isTournamentRewardClaimed(reward, profile)
              val isEquipped = uiState.equippedTournamentCosmeticId == reward.id

              TournamentCosmeticRewardCard(
                reward = reward,
                isUnlocked = isUnlocked,
                isClaimed = isClaimed,
                isEquipped = isEquipped,
                currentWins = uiState.tournamentWins,
                onActionClick = {
                  viewModel.claimOrEquipTournamentReward(reward, profile)
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CountdownUnitBox(
  value: String,
  label: String,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .border(1.dp, BrushedBrass.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
      .padding(vertical = 10.dp, horizontal = 6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp)
  ) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.ExtraBold,
      color = BrushedBrass
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun ActiveTournamentRoundCard(
  uiState: LudoUiState,
  onPlay3DMatch: () -> Unit,
  onInstantResolveRound: () -> Unit,
  onResetBracket: () -> Unit
) {
  val currentRound = uiState.tournamentCurrentRound
  val activeMatch = uiState.activeTournamentMatch

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .border(1.5.dp, BrushedBrass, RoundedCornerShape(22.dp)),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    )
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      if (currentRound == TournamentRoundTier.CHAMPION) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(BrushedBrass.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
          ) {
            Text(text = "👑", style = MaterialTheme.typography.headlineSmall)
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "SOVEREIGN CUP CHAMPION!",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = BrushedBrass
            )
            Text(
              text = "You went 3-0 across Quarterfinals, Semifinals, and Grand Finals! All 4 Exclusive Tournament Cosmetics are unlocked.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        OutlinedButton(
          onClick = onResetBracket,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("reset_tournament_run_button"),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Enter New Championship Defense Bracket")
        }
      } else if (activeMatch != null) {
        val p1Flag = CountryCatalog.fromCode(activeMatch.player1Country).flagEmoji
        val p2Flag = CountryCatalog.fromCode(activeMatch.player2Country).flagEmoji

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "NEXT BRACKET MATCH • ${currentRound.title.uppercase()}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = BrushedBrass
          )
          Text(
            text = "Match #${activeMatch.matchId}",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "#${activeMatch.player1Seed} ${activeMatch.player1Name} $p1Flag",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${activeMatch.player1Color.label} • Contender",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(999.dp))
              .background(BrushedBrass.copy(alpha = 0.2f))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = "VS",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.ExtraBold,
              color = BrushedBrass
            )
          }

          Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
          ) {
            Text(
              text = "#${activeMatch.player2Seed} ${activeMatch.player2Name} $p2Flag",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${activeMatch.player2Color.label} • Seeded Rival",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onPlay3DMatch,
            modifier = Modifier
              .weight(1.2f)
              .height(48.dp)
              .testTag("play_tournament_match_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = BrushedBrass,
              contentColor = Color.Black
            )
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Play 3D Match",
              fontWeight = FontWeight.ExtraBold
            )
          }

          OutlinedButton(
            onClick = onInstantResolveRound,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("instant_resolve_tournament_round_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = null,
              tint = BrushedBrass,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Quick Win ${currentRound.shortLabel}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BracketMatchNodeCard(
  match: TournamentBracketMatch
) {
  val p1Country = CountryCatalog.fromCode(match.player1Country)
  val p2Country = CountryCatalog.fromCode(match.player2Country)
  val (p1ColorDot, _, _) = paletteForColor(match.player1Color)
  val (p2ColorDot, _, _) = paletteForColor(match.player2Color)

  val borderColor = when {
    match.isPlayerMatch && match.winnerName == null -> BrushedBrass
    match.isPlayerMatch && match.winnerName != null -> EucalyptusSage
    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
      .border(1.2.dp, borderColor, RoundedCornerShape(16.dp))
      .padding(12.dp)
      .testTag("bracket_match_card_${match.matchId}"),
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
        Text(
          text = match.matchId,
          style = MaterialTheme.typography.labelSmall,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = BrushedBrass
        )
        Text(
          text = "• ${match.liveStatusText}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      val statusBadgeText = when {
        match.winnerName != null -> "FINAL"
        match.isLive -> "LIVE"
        else -> "UPCOMING"
      }
      val statusBadgeColor = when {
        match.winnerName != null -> EucalyptusSage
        match.isLive -> TerracottaRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .background(statusBadgeColor.copy(alpha = 0.18f))
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text(
          text = statusBadgeText,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.ExtraBold,
          color = statusBadgeColor
        )
      }
    }

    // Contender 1 Row
    BracketContenderRow(
      seed = match.player1Seed,
      name = match.player1Name,
      flagEmoji = p1Country.flagEmoji,
      colorDot = p1ColorDot,
      score = match.player1Score,
      isWinner = match.winnerName == match.player1Name
    )

    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

    // Contender 2 Row
    BracketContenderRow(
      seed = match.player2Seed,
      name = match.player2Name,
      flagEmoji = p2Country.flagEmoji,
      colorDot = p2ColorDot,
      score = match.player2Score,
      isWinner = match.winnerName == match.player2Name
    )
  }
}

@Composable
private fun BracketContenderRow(
  seed: Int,
  name: String,
  flagEmoji: String,
  colorDot: Color,
  score: Int,
  isWinner: Boolean
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
      Box(
        modifier = Modifier
          .size(12.dp)
          .clip(CircleShape)
          .background(colorDot)
      )
      Text(
        text = "#$seed",
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "$name $flagEmoji",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.SemiBold,
        color = if (isWinner) EucalyptusSage else MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (isWinner) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Round Winner",
          tint = EucalyptusSage,
          modifier = Modifier.size(15.dp)
        )
      }
    }

    Text(
      text = "$score pts",
      style = MaterialTheme.typography.labelMedium,
      fontFamily = FontFamily.Monospace,
      fontWeight = if (isWinner) FontWeight.ExtraBold else FontWeight.Medium,
      color = if (isWinner) EucalyptusSage else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun TournamentCosmeticRewardCard(
  reward: TournamentCosmeticReward,
  isUnlocked: Boolean,
  isClaimed: Boolean,
  isEquipped: Boolean,
  currentWins: Int,
  onActionClick: () -> Unit
) {
  val borderColor = when {
    isEquipped -> BrushedBrass
    isUnlocked -> EucalyptusSage.copy(alpha = 0.7f)
    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
      .border(1.2.dp, borderColor, RoundedCornerShape(18.dp))
      .padding(14.dp),
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
          .size(46.dp)
          .clip(RoundedCornerShape(14.dp))
          .background(BrushedBrass.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = reward.iconSymbol,
          style = MaterialTheme.typography.headlineSmall
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = reward.rarityLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = BrushedBrass
          )
          Text(
            text = "• ${reward.categoryLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Text(
          text = reward.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = reward.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Tier: ${reward.requiredRound.title} (${currentWins.coerceAtMost(reward.winsRequired)}/${reward.winsRequired} Wins) • +${reward.bonusCoins} Coins & +${reward.bonusXp} XP",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = if (isUnlocked) EucalyptusSage else BrushedBrass
        )
      }
    }

    Spacer(modifier = Modifier.width(10.dp))

    when {
      isEquipped -> {
        OutlinedButton(
          onClick = onActionClick,
          modifier = Modifier
            .height(48.dp)
            .testTag("tournament_reward_action_${reward.id}"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = BrushedBrass,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Equipped", style = MaterialTheme.typography.labelMedium)
        }
      }
      isClaimed -> {
        OutlinedButton(
          onClick = onActionClick,
          modifier = Modifier
            .height(48.dp)
            .testTag("tournament_reward_action_${reward.id}"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Equip", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
      }
      isUnlocked -> {
        Button(
          onClick = onActionClick,
          modifier = Modifier
            .height(48.dp)
            .testTag("tournament_reward_action_${reward.id}"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = EucalyptusSage,
            contentColor = Color.White
          )
        ) {
          Text("Claim", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
        }
      }
      else -> {
        OutlinedButton(
          onClick = onActionClick,
          modifier = Modifier
            .height(48.dp)
            .testTag("tournament_reward_action_${reward.id}"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Locked Reward",
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("${reward.winsRequired}W", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}
