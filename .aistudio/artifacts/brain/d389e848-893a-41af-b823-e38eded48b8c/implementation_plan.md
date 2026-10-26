# Ludo Atelier 3D — Minimalist 3D Multiplayer Board Game

Ludo Atelier 3D brings the classic tabletop race to Android as a tactile, architectural 3D experience with real-time online multiplayer, private room codes, quick matchmaking, and smart offline play. Built around the rule set of the live web Ludo game, it pairs smooth 3D token hops, tumbling 3D dice physics, and intuitive orbit/tilt/zoom touch controls with a calm, matte-pastel minimalist interface.

### User Review & Critical Decisions

> [!IMPORTANT]
> All core gameplay rules, multiplayer modes, 3D camera controls, and aesthetic preferences have been confirmed from your answers. Review the architectural plan below and click **Proceed** to begin provisioning Firebase and building the application.

- **Confirmed Ludo Rules**:
  - **Entry & Movement**: Roll a `6` to enter a token from the home yard onto the starting square; tokens move clockwise along the 52-tile outer track and up the 6-tile colored home column into the center home triangle via exact roll.
  - **Bonus Rolls**: Rolling a `6`, capturing an opponent's token, or landing a token inside the center home triangle grants an immediate bonus roll.
  - **Three-Sixes Penalty**: Rolling three consecutive `6`s immediately forfeits the current turn.
  - **Safe Star Squares & Starting Tiles**: Tokens resting on the 8 safe squares (4 colored start squares + 4 star squares) cannot be captured.
  - **Same-Color Blockades**: Two or more tokens of the same color occupying the same square form an impassable blockade that opponents cannot land on or pass through.
  - **Smart Auto-Move**: When only one legal token move exists after a dice roll, the game automatically executes that move after a brief visual highlight.
- **Confirmed Game Modes & Social Features**:
  - **Private Room Codes**: Create a 6-character room code to invite friends directly, or join a friend's room code.
  - **Quick Online Matchmaking**: Join an open public lobby waiting for 2–4 players, synced in real time via Cloud Firestore.
  - **Offline Modes**: Play locally against smart tactical AI bots (configurable 2–4 players) or Pass-and-Play on a single device.
  - **In-Game Emoji Reactions & Turn Timers**: Live floating 3D/HUD emoji reactions and a 20-second visual turn countdown ring to keep online matches brisk.
- **Confirmed 3D Camera & Touch Controls**:
  - **Interactive 3D Camera**: Full perspective 3D projection supporting one-finger/two-finger orbit rotation (`yaw`), vertical tilt adjustment (`pitch` between 25° and 80°), pinch-to-zoom, a quick "Reset Camera / Auto-Focus Turn" toggle, and direct 3D ray-cast tap detection on tokens and dice.
- **Confirmed Visual Aesthetic**:
  - **Warm Architectural Minimal**: Matte alabaster and travertine board geometry, soft directional drop shadows, muted terracotta/sage/ochre/slate-blue player palettes, and clean geometric typography.

---

### 1. Overview & Core Concept

- **What It Does**: Provides a complete 2-to-4-player 3D Ludo experience supporting both low-latency online multiplayer rooms (synced via Firebase Authentication and Cloud Firestore) and instant offline matches (AI bots and local Pass-and-Play).
- **Target Audience / Persona**: Friends, families, and board game enthusiasts looking for a refined, distraction-free multiplayer Ludo game without cluttered casino tropes or noisy ads.
- **Key Value**: Combines authentic tactical Ludo rules (blockades, capture bonuses, triple-six forfeit, safe stars) with a tactile 3D board you can freely inspect, rotate, and tap directly.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Sign-In & Welcome Gate**: Users authenticate with Google Sign-In via Android Credential Manager to unlock online lobbies and persistent player stats, with immediate access to the minimalist mode selector.
  2. **Mode Selection & Matchmaking Hub**:
     - *Quick Match*: Automatically joins an open waiting room in Firestore or provisions a new public room until 2–4 players are ready.
     - *Private Room*: Generates a shareable 6-character alphanumeric room code with live player slot cards, color selection, and a host "Start Match" trigger.
     - *Offline Studio*: Configure 2, 3, or 4 seats with any mix of Human (Pass-and-Play) and Tactical AI Bot players.
  3. **Interactive 3D Gameplay**:
     - The central viewport renders the 3D Ludo board with extruded base yards, recessed tile bevels, raised star safe markers, stacked blockade badges, and 3D pawn tokens that hop tile-by-tile along parabolic arcs.
     - On a player's turn, tapping the 3D die (or the ergonomic bottom roll pill) launches a 3D tumbling die animation with haptic feedback.
     - Legal tokens pulse with an elevated halo ring on the 3D surface; if only one legal move exists, Smart Auto-Move smoothly advances the token automatically.
     - Players can drag to orbit/tilt the 3D board, pinch to zoom, send quick floating emoji reactions (`👏`, `🔥`, `😮`, `😅`, `🎲`, `👑`), or tap the camera compass button to snap back to their home corner.
  4. **Victory Podium & Rematch**: When a player brings all 4 tokens into the center home prism, an architectural 3D celebration overlay displays rankings, capture statistics, and a one-tap Rematch or Lobby return action.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Warm Architectural Minimal — inspired by boutique wooden/ceramic tabletop objects and monument-style isometric geometry.
  - *Color Palette & Mood*:
    - `--background`: Warm Alabaster Linen (`#F6F3EC` light / `#171614` dark mode)
    - `--surface`: Soft Travertine Stone (`#EFECE4` / `#22201D`)
    - `--Red Player (Terracotta Coral)`: `#D95D4E` with soft clay base `#F4DCD8`
    - `--Green Player (Eucalyptus Sage)`: `#4A8B6E` with soft mist base `#D9EBE2`
    - `--Yellow Player (Warm Ochre Gold)`: `#DF9A3B` with soft sand base `#F8E9CE`
    - `--Blue Player (Nordic Slate Blue)`: `#4A78A8` with soft sky base `#DAE6F2`
    - `--accent / Safe Star`: Brushed Warm Brass (`#C59B4E`)
  - *Typography & Hierarchy*:
    - **Display / Headings**: *Outfit* (geometric, architectural poise for room codes, turn banners, and podium headers).
    - **Body / HUD & Numerals**: *Plus Jakarta Sans* (crisp legibility for player cards, timer rings, and rule callouts).
  - *Component Styling & Layout*:
    - Floating translucent Material 3 HUD cards (`tonalElevation`, subtle 1dp stone borders, `24.dp` corner radii) anchored cleanly inside safe drawing insets so the 3D board occupies full-bleed center stage.
    - Adaptive layout support: switches from vertical HUD framing on compact phones to a side Supporting Pane + 3D viewport split on tablets and foldables (`Expanded` window size class).

- **Interactive Feedback & Motion**:
  - **Parabolic 3D Token Arcs**: Tokens hop step-by-step along each tile with `spring()` vertical elevation and squash-and-stretch landing shadows.
  - **3D Tumbling Die**: Multi-axis 3D rotation (`rotX`, `rotY`, `rotZ`) with face normal shading and recessed pips that settle onto the rolled value.
  - **Capture & Bonus Callouts**: Subtle architectural toast pills announce *"Capture Bonus! Roll Again"*, *"Home Bonus! +1 Roll"*, *"Blockade Formed"*, or *"Three 6s — Turn Forfeited"*.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Custom Perspective 3D Projection Engine on Jetpack Compose Canvas**
  - *Chosen Approach*: Build a dedicated 3D mathematical projection pipeline (view/perspective matrix transformation, painter's depth sorting, directional Lambertian lighting, extruded 3D board tiles, multi-segment 3D cylindrical/conical pawns, and a 6-face 3D cube die) with ray-plane intersection for accurate touch picking.
  - *Why*: Delivers silky 60fps 3D camera orbit, tilt, zoom, parabolic token hops, and crisp vector/shader precision at any zoom level without native C++ engine bloat or WebView latency.
  - *Alternatives Considered*: Flat 2D grid with drop shadows (lacks true 3D camera tilt/orbit) or heavy external OpenGL/Filament scene binaries (harder to style with cohesive Material 3 vector typography and increases APK failure surface).
- **Decision 2: Authoritative Turn State & Real-Time Firestore Synchronization**
  - *Chosen Approach*: Store match state in a single atomic Firestore document per room (`rooms/{roomId}`), updated via validated state transitions and observed via `DocumentReference.snapshots()` Flow.
  - *Why*: Eliminates split-brain desynchronization across multiple subcollections while keeping turn transitions, blockade checks, and emoji reactions instantaneous for all 2–4 connected players.
  - *Alternatives Considered*: Per-move subcollection event sourcing (requires replaying hundreds of documents on reconnect and complicates security rules).

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:
```
┌────────────────────────────────────────────────────────────────────────────┐
│                        MainActivity (Edge-to-Edge)                         │
│                    AuthGate (Google Sign-In / CredentialManager)           │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ Authenticated UID
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   Navigation Host & WindowSizeClass Shell                  │
├──────────────────────┬──────────────────────────────┬──────────────────────┤
│   Lobby & MatchHub   │     3D Game Arena Screen     │   Rules & Profile    │
│  - Quick Matchmaking │  - Interactive3DLudoCanvas   │  - Live Ludo Rules   │
│  - Private Room Code │  - CameraOrbitController     │  - Player Stats Card │
│  - Offline Setup     │  - TurnTimer & EmojiHUD      │  - Sign Out Control  │
└──────────┬───────────┴──────────────┬───────────────┴──────────┬───────────┘
           │                          │                          │
           ▼                          ▼                          ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                         LudoGameViewModel (MVVM)                           │
│  - Pure LudoRulesEngine (6-entry, Blockades, Safe Stars, 3x6 Forfeit,      │
│    Capture/Home Bonus, Smart Auto-Move, Tactical AI Bot Evaluator)         │
│  - 3D Animation Sequencer (Step-by-step parabolic hop & 3D dice tumble)    │
│  - Turn Countdown Coroutine Timer (20s per action)                         │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                    LudoMultiplayerRepository (Firestore)                   │
│  - Custom DB Instance (R.string.firestore_database_id)                     │
│  - Room Creation / 6-Char Code Lookup / Quick Match Transaction            │
│  - Real-time Room Snapshot Flow & Structured Error Handling                │
└────────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - **`users/{userId}` (`PlayerProfile`)**: Stores `displayName`, `photoUrl`, `gamesPlayed`, `gamesWon`, `capturesTotal`, `createdAt`, `updatedAt`.
  - **`rooms/{roomId}` (`LudoRoomDocument`)**:
    - `roomCode`: 6-character uppercase code for private invites.
    - `isPublic`: Boolean flag for quick matchmaking discovery.
    - `status`: `"WAITING" | "PLAYING" | "FINISHED"`.
    - `hostUid`: Creator's UID; `memberUids`: List of participant UIDs (2–4 players).
    - `players`: Map/List of seat configurations (`color`, `uid`, `name`, `isBot`).
    - `tokens`: Compact 16-integer list representing each token's position (`-1` = in yard, `0..50` = main track offset from start, `51..56` = home stretch, `57` = home triangle finished).
    - `currentTurnColor`: Active player color (`RED`, `GREEN`, `YELLOW`, `BLUE`).
    - `diceValue`: Last rolled value (`1..6` or `0` when awaiting roll).
    - `turnPhase`: `"ROLL" | "MOVE" | "GAME_OVER"`.
    - `consecutiveSixes`: Counter (`0..2`) to enforce the three-sixes turn forfeit rule.
    - `lastEvent`: Human-readable status banner (e.g., *"Red captured Blue! Bonus roll"*).
    - `lastReaction`: Ephemeral emoji payload (`senderColor`, `emoji`, `timestampEpochMs`) for floating in-game reactions.
    - `winnerOrder`: Ordered list of player colors as they finish.

- **Interactive Component & State Mapping**:
  - **3D Camera Gesture Controller**: Multi-touch `pointerInput` gesture detector separates single-tap ray-casting from drag-to-orbit (`yaw += dx`, `pitch = (pitch + dy).coerceIn(25f, 80f)`) and pinch-to-zoom (`zoom = (zoom * scale).coerceIn(0.75f, 1.85f)`).
  - **3D Dice & Auto-Move Pipeline**:
    1. Player taps the 3D die or Roll button (validated against `currentTurnColor` and local/online ownership).
    2. Die animates through a 450ms 3D tumble before settling on `diceValue`.
    3. If `diceValue == 6` and `consecutiveSixes == 2`, the engine triggers the *Three-Sixes Forfeit* banner and advances the turn to the next clockwise player.
    4. Otherwise, `LudoRulesEngine.getLegalMoves()` computes movable tokens (checking yard exit on `6`, exact home entry, and verifying no opponent same-color blockade of $\ge 2$ tokens blocks the path or destination).
    5. If `legalMoves.isEmpty()`, the turn automatically passes after a brief pause; if `legalMoves.size == 1`, **Smart Auto-Move** highlights and hops that token automatically; if multiple tokens are legal, pulsing 3D rings invite the player to tap their chosen 3D token.
  - **Firestore Security Rules & Testing**:
    - Enforces authenticated access (`request.auth != null`), strict schema validation (`isValidPlayerProfile`, `isValidLudoRoom`), and participant authorization (`request.auth.uid in resource.data.memberUids` or joining a `WAITING` room).
    - Validated locally against the Firestore Emulator using both Tier 1 (`firestore.test.js`) and Tier 2 (`LudoRepositoryRuleTest.kt`) prior to rule deployment.
