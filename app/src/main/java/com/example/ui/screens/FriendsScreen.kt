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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CountryCatalog
import com.example.data.FriendEntryUiModel
import com.example.data.FriendPresenceState
import com.example.data.FriendRequest
import com.example.data.FriendRequestStatus
import com.example.data.PlayerProfile
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.theme.AlabasterLinen
import com.example.ui.theme.BrushedBrass
import com.example.ui.theme.CharcoalMuted
import com.example.ui.theme.EucalyptusSage
import com.example.ui.theme.NordicSlateBlue
import com.example.ui.theme.Obsidianink
import com.example.ui.theme.TerracottaRed
import com.example.ui.theme.TravertineStone
import com.example.ui.viewmodel.DataStreamState
import com.example.ui.viewmodel.LudoGameViewModel
import com.example.ui.viewmodel.LudoUiState

private val WarmAlabaster = AlabasterLinen
private val WarmLinen = TravertineStone
private val ObsidianCharcoal = Obsidianink
private val MutedSlate = CharcoalMuted
private val DeepTerracotta = TerracottaRed
private val SageGreen = EucalyptusSage
private val SlateBlue = NordicSlateBlue

@Composable
fun FriendsScreen(
  uiState: LudoUiState,
  viewModel: LudoGameViewModel
) {
  BackHandler {
    viewModel.leaveToLobby()
  }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var authErrorText by remember { mutableStateOf<String?>(null) }

  val friendRequestsStream by viewModel.friendRequestsState.collectAsStateWithLifecycle()
  val waitingRooms by viewModel.waitingRoomsForFriendsState.collectAsStateWithLifecycle()
  val spectatablePlayingRooms by viewModel.spectatablePublicPlayingRoomsState.collectAsStateWithLifecycle()
  val openPublicRoomsStream by viewModel.openPublicRoomsState.collectAsStateWithLifecycle()
  val globalLeadersStream by viewModel.globalLeaderboardState.collectAsStateWithLifecycle()

  val cloudRequests = (friendRequestsStream as? DataStreamState.Success)?.data.orEmpty()
  val openPublicRooms = (openPublicRoomsStream as? DataStreamState.Success)?.data.orEmpty()
  val leaderboardProfiles = (globalLeadersStream as? DataStreamState.Success)?.data.orEmpty()

  val myUid = viewModel.currentUserId ?: "LOCAL_PLAYER"

  val resolvedFriends = viewModel.buildResolvedFriendsList(
    cloudRequests = cloudRequests,
    leaderboardProfiles = leaderboardProfiles,
    waitingRooms = waitingRooms,
    openPublicRooms = openPublicRooms,
    spectatablePlayingRooms = spectatablePlayingRooms
  )

  val incomingRequests = (
    cloudRequests.filter {
      it.status == FriendRequestStatus.PENDING.name && it.toUid == myUid
    } + uiState.localIncomingRequests
    ).distinctBy { it.id }

  val sentRequests = (
    cloudRequests.filter {
      it.status == FriendRequestStatus.PENDING.name && it.fromUid == myUid
    } + uiState.localSentRequests
    ).distinctBy { it.id }

  val onlineCount = resolvedFriends.count { it.presenceState != FriendPresenceState.OFFLINE }
  val activeRoomFriendsCount = resolvedFriends.count {
    it.presenceState == FriendPresenceState.IN_WAITING_ROOM ||
      it.presenceState == FriendPresenceState.IN_ACTIVE_MATCH
  }
  val totalPendingCount = incomingRequests.size + sentRequests.size

  val friendUidSet = resolvedFriends.map { it.friendUid }.toSet()
  val pendingTargetUidSet = (incomingRequests.map { it.fromUid } + sentRequests.map { it.toUid }).toSet()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(WarmAlabaster, WarmLinen)
        )
      )
      .windowInsetsPadding(WindowInsets.safeDrawing)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 820.dp)
        .align(Alignment.TopCenter)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Top Navigation Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          IconButton(
            onClick = { viewModel.leaveToLobby() },
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(ObsidianCharcoal)
              .testTag("friends_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Lobby",
              tint = WarmAlabaster
            )
          }
          Column {
            Text(
              text = "ROYAL FRIENDS & CLUB",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = ObsidianCharcoal,
              letterSpacing = 0.8.sp
            )
            Text(
              text = "Search by Username • Friend Requests • Live Status & Active Rooms",
              style = MaterialTheme.typography.bodySmall,
              color = MutedSlate
            )
          }
        }

        IconButton(
          onClick = { viewModel.openFriendsScreen(uiState.selectedFriendsTab) },
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCharcoal.copy(alpha = 0.08f))
            .testTag("friends_refresh_button")
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refresh Friends & Active Rooms",
            tint = ObsidianCharcoal
          )
        }
      }

      // Toast / Error Feedback Banner
      val bannerMessage = uiState.errorMessage ?: authErrorText ?: uiState.toastMessage
      if (!bannerMessage.isNullOrBlank()) {
        val isError = uiState.errorMessage != null || authErrorText != null
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (isError) DeepTerracotta.copy(alpha = 0.14f) else SageGreen.copy(alpha = 0.16f),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              1.dp,
              if (isError) DeepTerracotta else SageGreen,
              RoundedCornerShape(14.dp)
            )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = bannerMessage,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = if (isError) DeepTerracotta else ObsidianCharcoal,
              modifier = Modifier.weight(1f)
            )
            TextButton(
              onClick = {
                viewModel.clearErrorMessage()
                authErrorText = null
              }
            ) {
              Text("Dismiss", fontWeight = FontWeight.Bold, color = ObsidianCharcoal)
            }
          }
        }
      }

      // Hero Summary Card: Online Friends, Active Game Rooms, Requests & Cloud Sync
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCharcoal),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, BrushedBrass.copy(alpha = 0.65f), RoundedCornerShape(22.dp))
          .testTag("friends_hero_summary_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(BrushedBrass.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Group,
                  contentDescription = null,
                  tint = BrushedBrass,
                  modifier = Modifier.size(24.dp)
                )
              }
              Column {
                Text(
                  text = "SOVEREIGN SOCIAL NETWORK",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = BrushedBrass,
                  letterSpacing = 1.1.sp
                )
                Text(
                  text = "${resolvedFriends.size} Friends • $onlineCount Online Now",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Black,
                  color = WarmAlabaster
                )
              }
            }

            if (viewModel.isAuthenticated) {
              Surface(
                shape = RoundedCornerShape(50),
                color = SageGreen.copy(alpha = 0.22f),
                modifier = Modifier.border(1.dp, SageGreen, RoundedCornerShape(50))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = SageGreen,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = "LIVE SYNC",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = WarmAlabaster
                  )
                }
              }
            } else {
              Button(
                onClick = {
                  onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    scope = scope,
                    onAuthSuccess = { authErrorText = null },
                    onAuthError = { msg -> authErrorText = msg }
                  )
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = BrushedBrass,
                  contentColor = ObsidianCharcoal
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("friends_google_signin_button")
              ) {
                Text(
                  text = "Sign In to Sync",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }
          }

          // 3-Column Swiss Grid Metrics
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            FriendsHeroMetricTile(
              label = "ONLINE NOW",
              value = "$onlineCount",
              subtitle = "Active in Studio",
              accentColor = SageGreen,
              modifier = Modifier.weight(1f)
            )
            FriendsHeroMetricTile(
              label = "ACTIVE ROOMS",
              value = "$activeRoomFriendsCount",
              subtitle = "Joinable / Playing",
              accentColor = BrushedBrass,
              modifier = Modifier.weight(1f)
            )
            FriendsHeroMetricTile(
              label = "REQUESTS",
              value = "$totalPendingCount",
              subtitle = "${incomingRequests.size} In • ${sentRequests.size} Sent",
              accentColor = if (incomingRequests.isNotEmpty()) DeepTerracotta else SlateBlue,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      // Username Search Bar (Always Accessible)
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, ObsidianCharcoal.copy(alpha = 0.14f), RoundedCornerShape(18.dp))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "SEARCH PLAYERS BY USERNAME",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.ExtraBold,
              color = ObsidianCharcoal,
              letterSpacing = 0.7.sp
            )
            Text(
              text = "${uiState.friendsSearchResults.size} Players Found",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MutedSlate
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = uiState.friendsSearchQuery,
              onValueChange = { query ->
                viewModel.updateFriendsSearchQuery(query)
                if (query.isNotBlank() && uiState.selectedFriendsTab != 2) {
                  viewModel.selectFriendsTab(2)
                }
              },
              placeholder = {
                Text(
                  text = "Search username (e.g. Kenji, Chloe, Zainab)...",
                  style = MaterialTheme.typography.bodySmall
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = null,
                  tint = ObsidianCharcoal
                )
              },
              trailingIcon = {
                if (uiState.friendsSearchQuery.isNotEmpty()) {
                  IconButton(onClick = { viewModel.updateFriendsSearchQuery("") }) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Clear search",
                      tint = MutedSlate
                    )
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("friends_username_search_input")
            )

            Button(
              onClick = {
                viewModel.selectFriendsTab(2)
                viewModel.executePlayerSearch()
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = ObsidianCharcoal,
                contentColor = WarmAlabaster
              ),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .height(54.dp)
                .testTag("friends_search_submit_button")
            ) {
              Text(
                text = "Find",
                fontWeight = FontWeight.ExtraBold
              )
            }
          }
        }
      }

      // 3-Tab Segmented Navigation Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(ObsidianCharcoal.copy(alpha = 0.08f))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FriendsTabPill(
          title = "Friends (${resolvedFriends.size})",
          selected = uiState.selectedFriendsTab == 0,
          badgeCount = activeRoomFriendsCount,
          onClick = { viewModel.selectFriendsTab(0) },
          testTag = "friends_tab_my_friends",
          modifier = Modifier.weight(1f)
        )
        FriendsTabPill(
          title = "Requests",
          selected = uiState.selectedFriendsTab == 1,
          badgeCount = incomingRequests.size,
          onClick = { viewModel.selectFriendsTab(1) },
          testTag = "friends_tab_requests",
          modifier = Modifier.weight(1f)
        )
        FriendsTabPill(
          title = "Directory",
          selected = uiState.selectedFriendsTab == 2,
          badgeCount = 0,
          onClick = { viewModel.selectFriendsTab(2) },
          testTag = "friends_tab_find_players",
          modifier = Modifier.weight(1f)
        )
      }

      // Active Tab Content
      when (uiState.selectedFriendsTab) {
        0 -> {
          FriendsListAndRoomsSection(
            friends = resolvedFriends,
            onJoinFriendRoom = { friend -> viewModel.joinFriendActiveRoom(friend) },
            onSpectateFriendRoom = { friend -> viewModel.spectateFriendActiveRoom(friend) },
            onRemoveFriend = { friend ->
              viewModel.removeFriendOrCancelRequest(friend.requestId, friend.displayName)
            },
            onSwitchToSearchTab = { viewModel.selectFriendsTab(2) }
          )
        }
        1 -> {
          FriendRequestsSection(
            incomingRequests = incomingRequests,
            sentRequests = sentRequests,
            onAcceptRequest = { req -> viewModel.respondToFriendRequest(req, accept = true) },
            onDeclineRequest = { req -> viewModel.respondToFriendRequest(req, accept = false) },
            onCancelSentRequest = { req ->
              viewModel.removeFriendOrCancelRequest(req.id, req.toName)
            },
            onSwitchToSearchTab = { viewModel.selectFriendsTab(2) }
          )
        }
        else -> {
          PlayerDirectorySearchSection(
            searchQuery = uiState.friendsSearchQuery,
            searchResults = uiState.friendsSearchResults,
            isSearching = uiState.isSearchingPlayers,
            friendUidSet = friendUidSet,
            pendingTargetUidSet = pendingTargetUidSet,
            onSendFriendRequest = { profile -> viewModel.sendFriendRequestToPlayer(profile) }
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun FriendsHeroMetricTile(
  label: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color.White.copy(alpha = 0.07f),
    modifier = modifier.border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = accentColor,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 10.sp
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black,
        color = WarmAlabaster
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = WarmAlabaster.copy(alpha = 0.72f),
        fontSize = 11.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun FriendsTabPill(
  title: String,
  selected: Boolean,
  badgeCount: Int,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (selected) ObsidianCharcoal else Color.Transparent,
    modifier = modifier
      .height(48.dp)
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.ExtraBold,
        color = if (selected) WarmAlabaster else ObsidianCharcoal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (badgeCount > 0) {
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) BrushedBrass else DeepTerracotta)
            .padding(horizontal = 6.dp, vertical = 2.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "$badgeCount",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = if (selected) ObsidianCharcoal else WarmAlabaster,
            fontSize = 10.sp
          )
        }
      }
    }
  }
}

@Composable
private fun FriendsListAndRoomsSection(
  friends: List<FriendEntryUiModel>,
  onJoinFriendRoom: (FriendEntryUiModel) -> Unit,
  onSpectateFriendRoom: (FriendEntryUiModel) -> Unit,
  onRemoveFriend: (FriendEntryUiModel) -> Unit,
  onSwitchToSearchTab: () -> Unit
) {
  val activeRoomFriends = friends.filter { !it.activeRoomCode.isNullOrBlank() }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Active Game Rooms Quick-Join Strip
    if (activeRoomFriends.isNotEmpty()) {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianCharcoal),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.5.dp, BrushedBrass, RoundedCornerShape(18.dp))
          .testTag("friends_active_rooms_banner")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.MeetingRoom,
              contentDescription = null,
              tint = BrushedBrass,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "FRIENDS' ACTIVE GAME ROOMS (${activeRoomFriends.size})",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Black,
              color = BrushedBrass,
              letterSpacing = 0.8.sp
            )
          }

          activeRoomFriends.forEach { friend ->
            val flag = CountryCatalog.flagEmojiForCountryCode(friend.countryCode)
            val isWaiting = friend.presenceState == FriendPresenceState.IN_WAITING_ROOM
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = Color.White.copy(alpha = 0.08f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = "$flag ${friend.displayName}",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.ExtraBold,
                      color = WarmAlabaster
                    )
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = if (isWaiting) SageGreen.copy(alpha = 0.28f) else BrushedBrass.copy(alpha = 0.25f)
                    ) {
                      val roomKindTag = if (friend.activeRoomIsPublic) "ROOM" else "PRIVATE ROOM"
                      Text(
                        text = "$roomKindTag #${friend.activeRoomCode}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = if (isWaiting) SageGreen else BrushedBrass,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  Text(
                    text = if (isWaiting) {
                      "${if (friend.activeRoomIsPublic) "Open" else "Private"} Waiting Room • ${friend.activeRoomPlayersCount}/${friend.activeRoomMaxPlayers} Seats"
                    } else {
                      "Ongoing ${if (friend.activeRoomIsPublic) "Public" else "Private"} ${friend.activeRoomMaxPlayers}P Match • Turn ${friend.activeRoomTurnNumber} • Observe Live"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmAlabaster.copy(alpha = 0.78f)
                  )
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  OutlinedButton(
                    onClick = { onSpectateFriendRoom(friend) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("quick_spectate_room_${friend.friendUid}")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Visibility,
                      contentDescription = "Spectate ${friend.displayName} in View-Only Mode",
                      tint = BrushedBrass,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Watch",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Black,
                      color = WarmAlabaster
                    )
                  }

                  Button(
                    onClick = { onJoinFriendRoom(friend) },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = BrushedBrass,
                      contentColor = ObsidianCharcoal
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("quick_join_room_${friend.friendUid}")
                  ) {
                    Icon(
                      imageVector = Icons.Default.PlayArrow,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Join #${friend.activeRoomCode}",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Black
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Friends Roster Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "FRIENDS ROSTER & LIVE STATUS",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = ObsidianCharcoal,
        letterSpacing = 0.7.sp
      )
      TextButton(onClick = onSwitchToSearchTab) {
        Icon(
          imageVector = Icons.Default.PersonAdd,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = ObsidianCharcoal
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Add Friends",
          fontWeight = FontWeight.ExtraBold,
          color = ObsidianCharcoal
        )
      }
    }

    if (friends.isEmpty()) {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "No Friends Added Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ObsidianCharcoal
          )
          Text(
            text = "Search for players by username above to send a friend request and see their active game rooms.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate
          )
          Button(
            onClick = onSwitchToSearchTab,
            colors = ButtonDefaults.buttonColors(
              containerColor = ObsidianCharcoal,
              contentColor = WarmAlabaster
            )
          ) {
            Text("Browse Player Directory", fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      friends.forEach { friend ->
        FriendStatusCard(
          friend = friend,
          onJoinRoom = { onJoinFriendRoom(friend) },
          onSpectateRoom = { onSpectateFriendRoom(friend) },
          onRemoveFriend = { onRemoveFriend(friend) }
        )
      }
    }
  }
}

@Composable
private fun FriendStatusCard(
  friend: FriendEntryUiModel,
  onJoinRoom: () -> Unit,
  onSpectateRoom: () -> Unit,
  onRemoveFriend: () -> Unit
) {
  val flag = CountryCatalog.flagEmojiForCountryCode(friend.countryCode)
  val statusColor = when (friend.presenceState) {
    FriendPresenceState.IN_WAITING_ROOM -> SageGreen
    FriendPresenceState.IN_ACTIVE_MATCH -> BrushedBrass
    FriendPresenceState.ONLINE_LOBBY -> SageGreen
    FriendPresenceState.OFFLINE -> MutedSlate
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    modifier = Modifier
      .fillMaxWidth()
      .border(
        width = if (friend.activeRoomCode != null) 1.5.dp else 1.dp,
        color = if (friend.activeRoomCode != null) statusColor else ObsidianCharcoal.copy(alpha = 0.14f),
        shape = RoundedCornerShape(18.dp)
      )
      .testTag("friend_card_${friend.friendUid}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          // Avatar Circle with Live Presence Dot
          Box(modifier = Modifier.size(48.dp)) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianCharcoal),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = flag,
                fontSize = 22.sp
              )
            }
            Box(
              modifier = Modifier
                .size(14.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(statusColor)
                .border(2.dp, Color.White, CircleShape)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = friend.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = ObsidianCharcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Surface(
                shape = RoundedCornerShape(50),
                color = statusColor.copy(alpha = 0.16f)
              ) {
                Text(
                  text = friend.presenceState.badgeTag,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (friend.presenceState == FriendPresenceState.IN_ACTIVE_MATCH) {
                    ObsidianCharcoal
                  } else {
                    statusColor
                  },
                  fontSize = 10.sp,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }

            Text(
              text = "${friend.rankTierTitle} • ${friend.score} PTS • ${friend.wins}W / ${friend.capturesTotal} Captures",
              style = MaterialTheme.typography.bodySmall,
              color = MutedSlate,
              fontSize = 11.sp
            )

            Text(
              text = friend.statusSubtitle,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = if (friend.presenceState == FriendPresenceState.OFFLINE) MutedSlate else ObsidianCharcoal
            )
          }
        }

        IconButton(
          onClick = onRemoveFriend,
          modifier = Modifier
            .size(44.dp)
            .testTag("remove_friend_${friend.friendUid}")
        ) {
          Icon(
            imageVector = Icons.Default.PersonRemove,
            contentDescription = "Remove ${friend.displayName}",
            tint = MutedSlate
          )
        }
      }

      // Active Room Action Row if friend is in a room
      if (!friend.activeRoomCode.isNullOrBlank()) {
        HorizontalDivider(color = ObsidianCharcoal.copy(alpha = 0.08f))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.SportsEsports,
              contentDescription = null,
              tint = ObsidianCharcoal,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "Active Room #${friend.activeRoomCode} (${friend.activeRoomPlayersCount}/${friend.activeRoomMaxPlayers} Players)",
              style = MaterialTheme.typography.labelMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = ObsidianCharcoal
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onSpectateRoom,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("spectate_friend_room_${friend.friendUid}")
            ) {
              Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = "Spectate ${friend.displayName} (View-Only Mode)",
                tint = ObsidianCharcoal,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Spectate",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = ObsidianCharcoal
              )
            }

            Button(
              onClick = onJoinRoom,
              colors = ButtonDefaults.buttonColors(
                containerColor = ObsidianCharcoal,
                contentColor = WarmAlabaster
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("join_friend_room_${friend.friendUid}")
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = BrushedBrass,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Join Room",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FriendRequestsSection(
  incomingRequests: List<FriendRequest>,
  sentRequests: List<FriendRequest>,
  onAcceptRequest: (FriendRequest) -> Unit,
  onDeclineRequest: (FriendRequest) -> Unit,
  onCancelSentRequest: (FriendRequest) -> Unit,
  onSwitchToSearchTab: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // Incoming Requests
    Text(
      text = "INCOMING FRIEND REQUESTS (${incomingRequests.size})",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Black,
      color = ObsidianCharcoal,
      letterSpacing = 0.7.sp
    )

    if (incomingRequests.isEmpty()) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, ObsidianCharcoal.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
      ) {
        Text(
          text = "No pending incoming friend requests right now.",
          style = MaterialTheme.typography.bodySmall,
          color = MutedSlate,
          modifier = Modifier.padding(16.dp)
        )
      }
    } else {
      incomingRequests.forEach { req ->
        val flag = CountryCatalog.flagEmojiForCountryCode(req.fromCountry)
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, BrushedBrass, RoundedCornerShape(16.dp))
            .testTag("incoming_request_card_${req.id}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
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
                  .size(44.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(ObsidianCharcoal),
                contentAlignment = Alignment.Center
              ) {
                Text(text = flag, fontSize = 20.sp)
              }
              Column {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = DeepTerracotta,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = req.fromName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = ObsidianCharcoal
                  )
                }
                Text(
                  text = "Wants to add you as a Royal Ludo friend (${req.fromCountry})",
                  style = MaterialTheme.typography.bodySmall,
                  color = MutedSlate
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { onDeclineRequest(req) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("decline_request_${req.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Decline",
                  modifier = Modifier.size(16.dp),
                  tint = DeepTerracotta
                )
              }
              Button(
                onClick = { onAcceptRequest(req) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = SageGreen,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("accept_request_${req.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Accept", fontWeight = FontWeight.ExtraBold)
              }
            }
          }
        }
      }
    }

    // Sent Requests
    Text(
      text = "SENT FRIEND REQUESTS (${sentRequests.size})",
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Black,
      color = ObsidianCharcoal,
      letterSpacing = 0.7.sp
    )

    if (sentRequests.isEmpty()) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, ObsidianCharcoal.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "No outgoing requests pending.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate
          )
          TextButton(onClick = onSwitchToSearchTab) {
            Text("Find Players", fontWeight = FontWeight.ExtraBold, color = ObsidianCharcoal)
          }
        }
      }
    } else {
      sentRequests.forEach { req ->
        val flag = CountryCatalog.flagEmojiForCountryCode(req.toCountry)
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianCharcoal.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .testTag("sent_request_card_${req.id}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
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
                  .size(40.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(ObsidianCharcoal),
                contentAlignment = Alignment.Center
              ) {
                Text(text = flag, fontSize = 18.sp)
              }
              Column {
                Text(
                  text = req.toName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = ObsidianCharcoal
                )
                Text(
                  text = "Awaiting response • ${req.toCountry}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MutedSlate
                )
              }
            }

            OutlinedButton(
              onClick = { onCancelSentRequest(req) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("cancel_request_${req.id}")
            ) {
              Text("Cancel", fontWeight = FontWeight.Bold, color = DeepTerracotta)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PlayerDirectorySearchSection(
  searchQuery: String,
  searchResults: List<PlayerProfile>,
  isSearching: Boolean,
  friendUidSet: Set<String>,
  pendingTargetUidSet: Set<String>,
  onSendFriendRequest: (PlayerProfile) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = if (searchQuery.isBlank()) {
          "RECOMMENDED & ONLINE PLAYERS"
        } else {
          "SEARCH RESULTS FOR \"${searchQuery.uppercase()}\""
        },
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = ObsidianCharcoal,
        letterSpacing = 0.7.sp
      )
      if (isSearching) {
        Text(
          text = "Searching Cloud...",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = SlateBlue
        )
      }
    }

    if (searchResults.isEmpty()) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, ObsidianCharcoal.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "No players matched \"$searchQuery\"",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ObsidianCharcoal
          )
          Text(
            text = "Try searching another username or country code (e.g. JP, FR, PK, US).",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSlate
          )
        }
      }
    } else {
      searchResults.forEach { player ->
        val uid = player.userId.ifBlank { player.id }
        val flag = CountryCatalog.flagEmojiForCountryCode(player.countryCode)
        val isAlreadyFriend = uid in friendUidSet
        val isPending = uid in pendingTargetUidSet

        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ObsidianCharcoal.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .testTag("search_player_card_$uid")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
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
                  .size(44.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(ObsidianCharcoal),
                contentAlignment = Alignment.Center
              ) {
                Text(text = flag, fontSize = 20.sp)
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = player.displayName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Black,
                  color = ObsidianCharcoal,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${player.rankTierTitle} • ${player.score} PTS • ${player.countryName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MutedSlate,
                  fontSize = 11.sp
                )
                Text(
                  text = "${player.gamesWon} Wins / ${player.gamesPlayed} Matches • ${player.capturesTotal} Captures",
                  style = MaterialTheme.typography.bodySmall,
                  color = ObsidianCharcoal.copy(alpha = 0.78f),
                  fontSize = 11.sp
                )
              }
            }

            when {
              isAlreadyFriend -> {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = SageGreen.copy(alpha = 0.16f)
                ) {
                  Text(
                    text = "Friends ✓",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = SageGreen,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                  )
                }
              }
              isPending -> {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = BrushedBrass.copy(alpha = 0.22f)
                ) {
                  Text(
                    text = "Requested",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ObsidianCharcoal,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                  )
                }
              }
              else -> {
                Button(
                  onClick = { onSendFriendRequest(player) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = ObsidianCharcoal,
                    contentColor = WarmAlabaster
                  ),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("send_friend_request_$uid")
                ) {
                  Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = BrushedBrass,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Add Friend",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold
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
