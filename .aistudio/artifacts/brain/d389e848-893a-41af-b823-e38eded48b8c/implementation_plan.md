# Implementation Plan — Royal Ludo Game Expansion (Dual-Dice, 6/8-Player 3D Boards, Leaderboards & Live Audio)

## 1. Overview & Aesthetic Direction
Building on the **Warm Architectural Minimal** aesthetic of **Royal Ludo Game** (`#FAF6F0` Alabaster Canvas, `#EFE8DD` Travertine Stone, `#C5A059` Brushed Brass, and sculpted 3D matte tokens), this update introduces seven major gameplay and social expansions:
1. **Dual-Dice Roll & Sequential Selection**: Roll two 3D dice simultaneously, choose which die to play first, move a pawn, and then play the second die sequentially.
2. **6-Player & 8-Player Simultaneous Support**: Dynamic 3D board rendering supporting classic 4-player cross boards, 6-player hexagonal boards, and 8-player octagonal boards.
3. **Custom Color Selection**: Choose from an 8-color architectural palette before match start.
4. **3D Particle System & Fireworks Animations**: Real-time physics particle bursts for fireworks, capture shockwaves, and golden blockade/double-6 auras.
5. **Global & Country-Wise Leaderboards**: Persistent rank scoring with device-locale country auto-detection, manual country switcher, and Global / Country filter tabs.
6. **1-to-1 & Group Live Audio Calls**: WebRTC-backed voice channels synchronized via Firestore signaling with group room chat and private 1-to-1 peer calling/mute controls.
7. **Configurable Pawn Movement Speed**: Instant in-game speed selector (`0.5x Slow`, `1x Normal`, `2x Fast`, `Instant`) controlling pawn hop animations.

---

## 2. Core Feature Architecture

### A. Dual-Dice Roll & Sequential Selection
- **Dual-Dice State (`LudoModels.kt` & `LudoRulesEngine.kt`)**:
  - Track `diceValues: List<Int>` (2 dice: Die A and Die B), `diceUsed: List<Boolean>`, and `selectedDieIndex: Int` (`0` or `1`).
  - **Rolling**: Tapping the 3D dice tray tumbles both 3D cubes simultaneously.
  - **Sequential Play**:
    - Players tap either Die 1 or Die 2 in the HUD to highlight valid tokens for that specific die value, move a token, and then automatically switch to the remaining unused die for their second move.
    - If only one die has valid moves, it is auto-selected; if neither die has valid moves, the turn passes automatically after a brief pause.
  - **6s & Bonus Rolls**:
    - Rolling a `6` on **either** die allows exiting a token from the home yard (`-1 -> 0`) and awards a bonus roll after both dice are resolved.
    - Rolling **double 6s (`6 + 6`)** triggers a golden aura particle burst and increments the consecutive 6s counter; three consecutive 6-rolls forfeit the turn.

### B. 6-Player & 8-Player 3D Board Topologies & Custom Color Picker
- **Expanded 8-Color Architectural Palette**:
  - `0`: Terracotta Red (`#D9534F`)
  - `1`: Sage Green (`#3B8EA5`)
  - `2`: Ochre Gold (`#E09F3E`)
  - `3`: Slate Blue (`#4A6FA5`)
  - `4`: Royal Amethyst (`#8E5572`)
  - `5`: Coral Rose (`#E06D53`)
  - `6`: Teal Lagoon (`#2A9D8F`)
  - `7`: Amber Bronze (`#B56576`)
- **Custom Color Selection**:
  - In both the Online Room Lobby and Offline 3D Studio Setup, players can pick any available color swatch from the 8-color palette for their seat before launching the match.
- **Dynamic 3D Board Topologies (`LudoBoard3DRenderer.kt` & `LudoRulesEngine.kt`)**:
  - **2–4 Players**: Classic 15x15 4-arm cross board (`52` perimeter tiles, `13` tiles per arm, `4` tokens per player).
  - **6 Players**: Sculpted 3D Hexagonal 6-arm board (`78` perimeter tiles, `13` tiles per arm, `6` home yards, `6` home stretch lanes of 6 steps, `24` total tokens).
  - **8 Players**: Sculpted 3D Octagonal 8-arm board (`104` perimeter tiles, `13` tiles per arm, `8` home yards, `8` home stretch lanes of 6 steps, `32` total tokens).
  - Star safe squares (`+8` offset from each arm's start square) and colored starting squares scale seamlessly across 4, 6, and 8 arms.

### C. Event Animations & 3D Particle / Fireworks System
- **Physics Particle Engine (`ParticleSystemOverlay.kt`)**:
  - **Fireworks (Match Victory & Home Goal Entry)**: Multi-stage aerial rocket bursts with gravity, drag, and sparkling color trails in the scoring player's color and brushed gold.
  - **Capture Shockwave & Sparks**: Expanding 3D ring wave at the capture tile plus high-velocity radial ember sparks in the captured and capturing token colors.
  - **Golden Aura Burst (Blockade Formed & Double 6s)**: Shimmering ascending gold particles (`#C5A059` / `#FFD166`) around the blockade tile or dice tray.

### D. Global & Country-Wise Leaderboards
- **Scoring Formula**:
  - **Match Win**: `+100 pts`
  - **Opponent Capture**: `+25 pts`
  - **Pawn Reaching Center Home**: `+15 pts`
  - **Match Loss / Forfeit**: `-20 pts` (minimum score floor of `0`)
- **Country Detection & Selector**:
  - Auto-detects the user's ISO 3166-1 alpha-2 country code and flag emoji from `Locale.getDefault().country` on first sign-in, with a country picker dialog in the Leaderboard & Profile sheet so users can switch their represented country at any time.
- **Leaderboard Screen (`LeaderboardScreen.kt`)**:
  - Accessible from the main lobby header; features **Global Top Ranks** and **My Country Ranks** tabs, podium cards for Top 3 players, and the signed-in player's live rank summary card.
- **Firestore Schema & Security Rules**:
  - Update `/users/{userId}` with `score`, `countryCode`, `countryName`, `homesReachedTotal`, and `lossesTotal`.
  - Allow authenticated users to query `/users` (`allow get, list: if isSignedIn();`) for global and country-filtered leaderboards while keeping zero PII in the document.

### E. 1-to-1 & Group Live Audio Calls
- **Permissions**:
  - Declare `android.permission.RECORD_AUDIO` and `android.permission.MODIFY_AUDIO_SETTINGS` in `AndroidManifest.xml`, with runtime permission requests via `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`.
- **WebRTC + Firestore Signaling (`AudioCallManager.kt`)**:
  - Uses WebRTC audio peer connections (`org.webrtc` / low-latency PCM audio stream fallback over Firestore signaling channels `/rooms/{roomId}/calls/{callId}`) so players can talk in real time.
  - **Group Voice Mode**: Toggle microphone and speaker for the entire room table, with animated speaking rings around active player seats.
  - **1-to-1 Direct Call & Per-Player Mute**: Tap any opponent's seat badge to initiate a private 1-to-1 audio channel or individually mute/unmute specific players in the group call.

### F. Configurable Pawn Movement Speed
- **Speed Control Selector**:
  - Compact speed pill in the top HUD (`0.5x`, `1x`, `2x`, `Instant`) persisted in ViewModel state.
  - Adjusts the per-tile hop interpolation duration (`150ms` at `0.5x`, `75ms` at `1x`, `35ms` at `2x`, `0ms` direct teleport at `Instant`).

---

## 3. Verification Plan
1. **Firestore Rules Validation**:
   - Update `firebase-blueprint.json`, `firestore.rules`, `firestore.test.js`, and `LudoRepositoryRuleTest.kt` to cover leaderboard queries (`list` on `/users`), 6/8-player room states (`maxPlayers` up to 8, `tokenPositions` up to 32), dual-dice fields, and `/rooms/{roomId}/calls/{callId}` signaling documents.
   - Run Tier 1 (`node --test firestore.test.js`) and Tier 2 (`gradle :app:testDebugUnitTest --tests "*Rule*"`) against local emulators, then deploy rules via `rpc_action(service_name = "fax", method_name = "DeployRules")`.
2. **Unit & Compilation Checks**:
   - Verify 2-dice sequential moves, 6-player (78-tile) and 8-player (104-tile) pathing, and score calculation in `ExampleUnitTest.kt`, followed by `compile_applet`.
