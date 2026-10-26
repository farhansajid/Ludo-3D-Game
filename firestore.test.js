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

function createValidRoomPayload(hostUid, status = "WAITING", isPublic = true) {
  const now = new Date();
  return {
    hostUid,
    roomCode: "LUDO01",
    isPublic,
    status,
    maxPlayers: 4,
    memberUids: [hostUid],
    playerNames: ["Alice"],
    playerColors: ["RED"],
    playerUids: [hostUid],
    tokens: [-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],
    currentTurnColor: "RED",
    turnPhase: "ROLL",
    diceValue: 0,
    consecutiveSixes: 0,
    turnNumber: 1,
    lastEvent: "Waiting for players",
    reactionEmoji: "",
    reactionSender: "",
    winnerColor: "NONE",
    winnerOrder: [],
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read users or rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("rooms").doc("LUDO01").get());
});

test("Authenticated user: can create and read own profile, but cannot read another user's profile", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      gamesPlayed: 0,
      gamesWon: 0,
      capturesTotal: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow update test: rejects ghost fields on profile creation", async () => {
  const now = new Date();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      gamesPlayed: 0,
      gamesWon: 0,
      capturesTotal: 0,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Room lifecycle: create waiting room, Bob joins, non-member Charlie blocked once PLAYING, terminal FINISHED locked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const roomRefAlice = aliceDb.collection("rooms").doc("LUDO01");
  await assertSucceeds(roomRefAlice.set(createValidRoomPayload(ALICE_UID, "WAITING", true)));

  // Bob can join while WAITING
  await assertSucceeds(
    bobDb.collection("rooms").doc("LUDO01").update({
      memberUids: [ALICE_UID, BOB_UID],
      playerNames: ["Alice", "Bob"],
      playerColors: ["RED", "YELLOW"],
      playerUids: [ALICE_UID, BOB_UID],
      status: "PLAYING",
      lastEvent: "Bob joined and game started!",
      updatedAt: new Date(),
    })
  );

  // Charlie (non-member) cannot read or update a PLAYING room
  await assertFails(charlieDb.collection("rooms").doc("LUDO01").get());
  await assertFails(
    charlieDb.collection("rooms").doc("LUDO01").update({
      diceValue: 6,
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

test("Client query alignment: member can list own rooms with array-contains filter, fails without filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("rooms").doc("LUDO02").set(createValidRoomPayload(ALICE_UID, "PLAYING", false))
  );

  await assertSucceeds(
    aliceDb.collection("rooms").where("memberUids", "array-contains", ALICE_UID).get()
  );

  await assertFails(aliceDb.collection("rooms").get());
});
