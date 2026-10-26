package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.LudoMultiplayerRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.screens.GameArenaScreen
import com.example.ui.screens.LobbyHubScreen
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
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    AuthScreen(
      onAuthSuccess = {}
    )
  } else {
    AuthenticatedLudoApp(
      currentUserId = user.uid,
      initialDisplayName = user.displayName ?: user.email?.substringBefore("@") ?: "Royal Player"
    )
  }
}

@Composable
private fun AuthenticatedLudoApp(
  currentUserId: String,
  initialDisplayName: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val viewModel: LudoGameViewModel = viewModel(
    key = currentUserId,
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
          initialDisplayName = initialDisplayName
        )
      }
    }
  )

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val openRoomsState by viewModel.openPublicRoomsState.collectAsStateWithLifecycle()

  when (uiState.currentScreen) {
    AppScreen.LOBBY -> {
      LobbyHubScreen(
        uiState = uiState,
        profileState = profileState,
        openRoomsState = openRoomsState,
        viewModel = viewModel,
        onSignOut = {
          viewModel.leaveToLobby()
          signOutUser(
            context = context,
            onSignOutComplete = {},
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
  }
}
