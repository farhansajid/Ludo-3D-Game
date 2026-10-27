package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.LudoMultiplayerRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.GameArenaScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LobbyHubScreen
import com.example.ui.screens.OwnerAdminControlScreen
import com.example.ui.screens.ReplayStudioScreen
import com.example.ui.screens.RoyalBoutiqueStoreScreen
import com.example.ui.screens.WaitingRoomScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.LudoGameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  var enteredLobbyWithoutSignIn by remember { mutableStateOf(true) }

  LaunchedEffect(user) {
    if (user == null) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {},
        onUnauthenticated = {},
        scope = scope
      )
    }
  }

  if (user == null && !enteredLobbyWithoutSignIn) {
    AuthScreen(
      onAuthSuccess = {},
      onPlayOfflineImmediately = { enteredLobbyWithoutSignIn = true }
    )
  } else {
    LudoAppShell(
      currentUserId = user?.uid,
      currentUserEmail = user?.email,
      initialDisplayName = user?.displayName ?: user?.email?.substringBefore("@") ?: "Royal Player",
      onReturnedToSignIn = { enteredLobbyWithoutSignIn = false }
    )
  }
}

@Composable
private fun LudoAppShell(
  currentUserId: String?,
  currentUserEmail: String?,
  initialDisplayName: String,
  onReturnedToSignIn: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val viewModel: LudoGameViewModel = viewModel(
    key = currentUserId ?: "offline_studio_guest",
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        LudoGameViewModel(
          repository = LudoMultiplayerRepository(db),
          currentUserId = currentUserId,
          initialDisplayName = initialDisplayName,
          currentUserEmail = currentUserEmail
        )
      }
    }
  )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val openRoomsState by viewModel.openPublicRoomsState.collectAsStateWithLifecycle()
  val globalLeaderboardState by viewModel.globalLeaderboardState.collectAsStateWithLifecycle()
  val countryLeaderboardState by viewModel.countryLeaderboardState.collectAsStateWithLifecycle()
  val adminConfig by viewModel.adminConfigState.collectAsStateWithLifecycle()

  when (uiState.currentScreen) {
    AppScreen.LOBBY -> {
      LobbyHubScreen(
        uiState = uiState,
        profileState = profileState,
        openRoomsState = openRoomsState,
        adminConfig = adminConfig,
        viewModel = viewModel,
        onSignOut = {
          viewModel.leaveToLobby()
          signOutUser(
            context = context,
            onSignOutComplete = onReturnedToSignIn,
            scope = scope
          )
        }
      )
    }

    AppScreen.WAITING_ROOM -> {
      WaitingRoomScreen(
        uiState = uiState,
        viewModel = viewModel
      )
    }

    AppScreen.GAME_ARENA -> {
      GameArenaScreen(
        uiState = uiState,
        viewModel = viewModel
      )
    }

    AppScreen.LEADERBOARD -> {
      LeaderboardScreen(
        uiState = uiState,
        profileState = profileState,
        globalLeaderboardState = globalLeaderboardState,
        countryLeaderboardState = countryLeaderboardState,
        viewModel = viewModel
      )
    }

    AppScreen.STORE -> {
      RoyalBoutiqueStoreScreen(
        uiState = uiState,
        profileState = profileState,
        viewModel = viewModel
      )
    }

    AppScreen.ADMIN_PANEL -> {
      OwnerAdminControlScreen(
        uiState = uiState,
        adminConfig = adminConfig,
        viewModel = viewModel
      )
    }

    AppScreen.REPLAY_STUDIO -> {
      ReplayStudioScreen(
        uiState = uiState,
        viewModel = viewModel
      )
    }

    AppScreen.EMOTE_GALLERY -> {
      com.example.ui.screens.EmoteGalleryScreen(
        uiState = uiState,
        profileState = profileState,
        viewModel = viewModel
      )
    }

    AppScreen.WEEKLY_TOURNAMENT -> {
      com.example.ui.screens.WeeklyTournamentScreen(
        uiState = uiState,
        profileState = profileState,
        viewModel = viewModel
      )
    }

    AppScreen.PROFILE -> {
      com.example.ui.screens.ProfileScreen(
        uiState = uiState,
        profileState = profileState,
        globalLeaderboardState = globalLeaderboardState,
        countryLeaderboardState = countryLeaderboardState,
        viewModel = viewModel
      )
    }

    AppScreen.FRIENDS -> {
      com.example.ui.screens.FriendsScreen(
        uiState = uiState,
        viewModel = viewModel
      )
    }

    AppScreen.HOW_TO_PLAY_TUTORIAL -> {
      com.example.ui.screens.HowToPlayTutorialScreen(
        uiState = uiState,
        viewModel = viewModel
      )
    }
  }
}
