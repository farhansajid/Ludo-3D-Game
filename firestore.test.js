const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function createValidRoomPayload(hostUid, status = "WAITING", isPublic = true, maxPlayers = 4) {
  const now = new Date();
  const tokenCount = maxPlayers <= 4 ? 16 : maxPlayers * 4;
  return {
    hostUid,
    roomCode: "LUDO01",
    isPublic,
    status,
    maxPlayers,
    memberUids: [hostUid],
    playerNames: ["Alice"],
    playerColors: ["RED"],
    playerUids: [hostUid],
    tokens: Array(tokenCount).fill(-1),
    currentTurnColor: "RED",
    turnPhase: "ROLL",
    diceValue: 0,
    diceValue2: 0,
    dice1Used: false,
    dice2Used: false,
    selectedDieIndex: 0,
    consecutiveSixes: 0,
    turnNumber: 1,
    lastEvent: "Waiting for players",
    reactionEmoji: "",
    reactionSender: "",
    winnerColor: "NONE",
    winnerOrder: [],
    activeVoiceUids: [],
    callMode: "NONE",
    callInitiatorUid: "",
    callTargetUid: "",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read users, rooms, or appConfig", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("rooms").doc("LUDO01").get());
  await assertFails(unauthDb.collection("appConfig").doc("global").get());
});

test("Authenticated user: can create own profile with coins/xp/missions, list leaderboards, and save admin config", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      countryCode: "US",
      countryName: "United States",
      score: 250,
      gamesPlayed: 3,
      gamesWon: 2,
      capturesTotal: 4,
      homesReachedTotal: 8,
      coins: 400,
      xp: 120,
      claimedMissionIds: ["mission_win_1"],
      unlockedItems: ["cosmetic_brass_dice"],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Alice can save global admin config
  await assertSucceeds(
    aliceDb.collection("appConfig").doc("global").set({
      updatedByUid: ALICE_UID,
      ownerEmail: "farhanSajid@gmail.com",
      customFirebaseDatabaseId: "ai-studio-android-royallud-d389e848-893a-41af-b823-e38eded48b8c",
      turnDurationSeconds: 22,
      winPoints: 100,
      capturePoints: 25,
      homePoints: 15,
      missionRewardMultiplier: 2,
      announcementBanner: "2x Daily Challenge Rewards Active!",
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  // Bob can read leaderboard profiles and global admin config
  await assertSucceeds(bobDb.collection("users").orderBy("score", "desc").limit(20).get());
  await assertSucceeds(bobDb.collection("appConfig").doc("global").get());
  // Bob cannot modify Alice's profile
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      score: 999999,
      updatedAt: new Date(),
    })
  );
});

test("Shadow update test: rejects ghost fields on profile creation", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      countryCode: "US",
      countryName: "United States",
      score: 100,
      gamesPlayed: 0,
      gamesWon: 0,
      capturesTotal: 0,
      homesReachedTotal: 0,
      coins: 250,
      xp: 0,
      claimedMissionIds: [],
      unlockedItems: [],
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Room lifecycle (including 8-player and voiceSignals): create room, Bob joins, voice signaling works for members, non-member blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const roomRefAlice = aliceDb.collection("rooms").doc("LUDO08");
  await assertSucceeds(roomRefAlice.set(createValidRoomPayload(ALICE_UID, "WAITING", true, 8)));

  // Bob can join while WAITING and choose PURPLE color
  await assertSucceeds(
    bobDb.collection("rooms").doc("LUDO08").update({
      memberUids: [ALICE_UID, BOB_UID],
      playerNames: ["Alice", "Bob"],
      playerColors: ["RED", "PURPLE"],
      playerUids: [ALICE_UID, BOB_UID],
      status: "PLAYING",
      lastEvent: "Bob joined as Royal Amethyst!",
      updatedAt: new Date(),
    })
  );

  // Alice and Bob can exchange voiceSignals inside the room
  await assertSucceeds(
    aliceDb.collection("rooms").doc("LUDO08").collection("voiceSignals").doc("sig_1").set({
      senderUid: ALICE_UID,
      senderName: "Alice",
      targetUid: BOB_UID,
      signalType: "AUDIO_FRAME",
      payload: "AAECAwQF",
      createdAt: new Date(),
    })
  );

  // Charlie (non-member spectator) CAN read public PLAYING room and send emoji reactions,
  // but CANNOT mutate gameplay fields (tokens, diceValue) or read voiceSignals
  await assertSucceeds(charlieDb.collection("rooms").doc("LUDO08").get());
  await assertSucceeds(
    charlieDb.collection("rooms").doc("LUDO08").update({
      reactionEmoji: "🔥",
      reactionSender: "Charlie (Spectator)",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    charlieDb.collection("rooms").doc("LUDO08").update({
      diceValue: 6,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    charlieDb.collection("rooms").doc("LUDO08").collection("voiceSignals").doc("sig_1").get()
  );

  // Private PLAYING room can be observed (read-only board state) by authenticated spectators,
  // but spectators CANNOT mutate board tokens, dice, turn state, or join as players once PLAYING
  await assertSucceeds(
    aliceDb.collection("rooms").doc("PRIV08").set({
      ...createValidRoomPayload(ALICE_UID, "PLAYING", false, 4),
      roomCode: "PRIV08",
    })
  );
  await assertSucceeds(charlieDb.collection("rooms").doc("PRIV08").get());
  await assertSucceeds(
    charlieDb.collection("rooms").doc("PRIV08").update({
      reactionEmoji: "👏",
      reactionSender: "Charlie (Observer)",
      updatedAt: new Date(),
    })
  );
  await assertFails(
    charlieDb.collection("rooms").doc("PRIV08").update({
      tokens: Array(16).fill(0),
      updatedAt: new Date(),
    })
  );
  await assertFails(
    charlieDb.collection("rooms").doc("PRIV08").update({
      memberUids: [ALICE_UID, CHARLIE_UID],
      updatedAt: new Date(),
    })
  );

  // Alice finishes the game
  await assertSucceeds(
    roomRefAlice.update({
      status: "FINISHED",
      turnPhase: "GAME_OVER",
      winnerColor: "RED",
      winnerOrder: ["RED"],
      updatedAt: new Date(),
    })
  );

  // Terminal state lock: no further updates allowed after FINISHED
  await assertFails(
    roomRefAlice.update({
      lastEvent: "Trying to mutate finished game",
      updatedAt: new Date(),
    })
  );
});

test("Friend requests lifecycle: Alice sends to Bob, Bob accepts, memberUids query works, Charlie blocked", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const reqId = `fr_${ALICE_UID}_${BOB_UID}`;

  // Unauthenticated cannot read or write friendRequests
  await assertFails(unauthDb.collection("friendRequests").doc(reqId).get());

  // Alice creates a pending friend request to Bob
  await assertSucceeds(
    aliceDb.collection("friendRequests").doc(reqId).set({
      fromUid: ALICE_UID,
      fromName: "Alice Royal",
      fromCountry: "US",
      toUid: BOB_UID,
      toName: "Bob Grandmaster",
      toCountry: "GB",
      status: "PENDING",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice cannot self-accept her own outgoing request
  await assertFails(
    aliceDb.collection("friendRequests").doc(reqId).update({
      status: "ACCEPTED",
      updatedAt: new Date(),
    })
  );

  // Charlie (non-member) cannot read or update the friend request
  await assertFails(charlieDb.collection("friendRequests").doc(reqId).get());
  await assertFails(
    charlieDb.collection("friendRequests").doc(reqId).update({
      status: "ACCEPTED",
      updatedAt: new Date(),
    })
  );

  // Bob can query his friend requests via memberUids array-contains
  await assertSucceeds(
    bobDb.collection("friendRequests").where("memberUids", "array-contains", BOB_UID).get()
  );

  // Bob accepts the friend request
  await assertSucceeds(
    bobDb.collection("friendRequests").doc(reqId).update({
      status: "ACCEPTED",
      updatedAt: new Date(),
    })
  );

  // Either member can remove/unfriend
  await assertSucceeds(aliceDb.collection("friendRequests").doc(reqId).delete());
});

test("Private room chat & dual-dice sequential selection sync: members and observers can chat with profanity filter, and active player syncs selectedDieIndex", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const privRefAlice = aliceDb.collection("rooms").doc("PRV409");
  await assertSucceeds(
    privRefAlice.set({
      ...createValidRoomPayload(ALICE_UID, "PLAYING", false, 4),
      roomCode: "PRV409",
      turnPhase: "MOVE",
      diceValue: 6,
      diceValue2: 4,
      selectedDieIndex: 0,
    })
  );

  // Alice switches her sequential die selection to Die 2 (selectedDieIndex = 1) and syncs via Firestore
  await assertSucceeds(
    privRefAlice.update({
      selectedDieIndex: 1,
      lastEvent: "Alice selected Die #2 (+4) for sequential move",
      updatedAt: new Date(),
    })
  );

  // Alice sends a private room chat message with a quick-reply emoji
  await assertSucceeds(
    privRefAlice.collection("chatMessages").doc("msg_1").set({
      senderUid: ALICE_UID,
      senderName: "Alice",
      senderColor: "RED",
      messageText: "Good roll! Let's use Die #2 first.",
      quickEmoji: "🎲",
      wasFiltered: false,
      createdAt: now,
    })
  );

  // Raw profanity is rejected by Firestore security rules if client filter is bypassed
  await assertFails(
    privRefAlice.collection("chatMessages").doc("msg_bad").set({
      senderUid: ALICE_UID,
      senderName: "Alice",
      senderColor: "RED",
      messageText: "what the fuck",
      quickEmoji: "",
      wasFiltered: false,
      createdAt: now,
    })
  );

  // Masked profanity (client-sanitized with ***) succeeds
  await assertSucceeds(
    bobDb.collection("rooms").doc("PRV409").collection("chatMessages").doc("msg_2").set({
      senderUid: BOB_UID,
      senderName: "Bob",
      senderColor: "SPECTATOR",
      messageText: "what the ****! Close call!",
      quickEmoji: "😅",
      wasFiltered: true,
      createdAt: now,
    })
  );
});


