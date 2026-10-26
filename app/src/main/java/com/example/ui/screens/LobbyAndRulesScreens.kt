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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LudoRoom
import com.example.data.PlayerProfile
import com.example.ui.board3d.paletteForColor
import com.example.ui.theme.BrushedBrass
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
  viewModel: LudoGameViewModel,
  onSignOut: () -> Unit
) {
  var joinCodeInput by remember { mutableStateOf("") }
  var selectedOnlineMaxPlayers by remember { mutableIntStateOf(4) }
  var showEditNameDialog by remember { mutableStateOf(false) }

  val profile = (profileState as? DataStreamState.Success)?.data
  val openRooms = (openRoomsState as? DataStreamState.Success)?.data.orEmpty()

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
              text = stringResource(R.string.tagline_3d_atelier),
              style = MaterialTheme.typography.labelMedium,
              color = BrushedBrass
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = { viewModel.toggleRulesModal(true) },
              modifier = Modifier.testTag("open_rules_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = stringResource(R.string.rules_button_label),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Rules")
            }

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

        // Error Banner if any
        if (uiState.errorMessage != null) {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                text = uiState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
              )
              TextButton(onClick = { viewModel.clearErrorMessage() }) {
                Text("Dismiss")
              }
            }
          }
        }

        // Hero Banner + Player Career Stats Card
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
                .height(140.dp)
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
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(CircleShape)
                      .background(BrushedBrass.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.EmojiEvents,
                      contentDescription = null,
                      tint = BrushedBrass
                    )
                  }
                  Column {
                    Text(
                      text = profile?.displayName ?: "Royal Player",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "Online Multiplayer Ready",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

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

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                StatBadge(
                  label = "Matches",
                  value = (profile?.gamesPlayed ?: 0L).toString(),
                  modifier = Modifier.weight(1f)
                )
                StatBadge(
                  label = "Victories",
                  value = (profile?.gamesWon ?: 0L).toString(),
                  modifier = Modifier.weight(1f)
                )
                StatBadge(
                  label = "Captures",
                  value = (profile?.capturesTotal ?: 0L).toString(),
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }

        // Card 1: Online Multiplayer & Private Room Codes
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
                  text = "Online 3D Multiplayer",
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = stringResource(R.string.private_room_desc),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Seat size selector (2 or 4 players)
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Text(
                text = "Room Size:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              listOf(2, 4).forEach { count ->
                FilterChip(
                  selected = selectedOnlineMaxPlayers == count,
                  onClick = { selectedOnlineMaxPlayers = count },
                  label = { Text("$count Players") },
                  leadingIcon = if (selectedOnlineMaxPlayers == count) {
                    {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  } else null
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = { viewModel.startQuickOnlineMatch(selectedOnlineMaxPlayers) },
                enabled = !uiState.isLoadingAction,
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
                  viewModel.createOnlinePrivateRoom(
                    maxPlayers = selectedOnlineMaxPlayers,
                    isPublic = false
                  )
                },
                enabled = !uiState.isLoadingAction,
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

            // Join by 6-Character Room Code
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedTextField(
                value = joinCodeInput,
                onValueChange = { joinCodeInput = it.uppercase().take(8) },
                label = { Text("Enter Room Code") },
                placeholder = { Text("e.g. LUDO26") },
                singleLine = true,
                modifier = Modifier
                  .weight(1f)
                  .testTag("join_room_code_input"),
                shape = RoundedCornerShape(14.dp)
              )

              Button(
                onClick = { viewModel.joinOnlineRoomByCode(joinCodeInput) },
                enabled = !uiState.isLoadingAction && joinCodeInput.trim().length >= 4,
                modifier = Modifier
                  .height(54.dp)
                  .testTag("join_room_code_button"),
                shape = RoundedCornerShape(14.dp)
              ) {
                Text("Join")
              }
            }

            // Live Open Public Lobbies List
            if (openRooms.isNotEmpty()) {
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
                        text = "Room #${room.roomCode}",
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

        // Card 2: Offline 3D Studio (AI Bots & Pass-and-Play)
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
                  text = stringResource(R.string.offline_studio_desc),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              uiState.offlineDraftSeats.forEach { seat ->
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
                    Box(
                      modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(colorPrimary)
                    )
                    Column {
                      Text(
                        text = seat.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = if (!seat.isEnabled) "Seat Off" else if (seat.isBot) "Tactical AI Bot" else "Human (Pass & Play)",
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
                          viewModel.updateOfflineSeatDraft(seat.color, isBot = !seat.isBot)
                        },
                        label = { Text(if (seat.isBot) "AI Bot" else "Human") },
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
                        viewModel.updateOfflineSeatDraft(seat.color, isEnabled = enabled)
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
              Text(
                text = "Launch 3D Tabletop Match",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
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

  if (uiState.showRulesModal) {
    LiveLudoRulesDialog(onDismiss = { viewModel.toggleRulesModal(false) })
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
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
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
            text = if (uiState.isPublicRoom) "Public Matchmaking Room" else "Private Invite Room",
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
              modifier = Modifier.padding(vertical = 8.dp),
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )

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
                    "Fill Empty Seats with AI & Start"
                  } else {
                    "Start 3D Online Match"
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
fun LiveLudoRulesDialog(onDismiss: () -> Unit) {
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
          title = "1. Entering the Board (The 6 Rule)",
          body = "All 4 pawns begin inside your corner yard. Roll a 6 to deploy a pawn onto your colored starting square."
        )
        RuleSection(
          title = "2. Bonus Rolls (+1 Turn)",
          body = "Rolling a 6, capturing an opponent's pawn, or landing a pawn inside the center Home Triangle immediately grants a bonus roll."
        )
        RuleSection(
          title = "3. Three Consecutive 6s Penalty",
          body = "Rolling three 6s in a single turn immediately forfeits your turn and passes the die clockwise."
        )
        RuleSection(
          title = "4. Safe Star & Start Squares",
          body = "The 4 colored starting squares and 4 brushed-brass Star squares protect all pawns resting on them from capture."
        )
        RuleSection(
          title = "5. Same-Color Blockades",
          body = "Two or more pawns of the same color on an outer track square form a golden Blockade shield that opponents cannot land on or pass through."
        )
        RuleSection(
          title = "6. Smart Auto-Move & Exact Home Entry",
          body = "When only one legal move exists after rolling, your pawn hops automatically. You must roll the exact step count required to enter the center Home Triangle."
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        modifier = Modifier.testTag("close_rules_dialog_button")
      ) {
        Text("Got It")
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
