const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "cs69FLrgVqMFNyKDqDth9BwZBKt1";
const ALICE_UID = "alice_supervisor_1";
const BOB_UID = "bob_supervisor_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("app_config").doc("global").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("ledgers").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("expenses").get()
  );
});

test("User isolation: Alice can manage her own ledgers, drafts, expenses, and trash, but not Bob's", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "রহিম মিয়া",
      aratName: "সোনার বাংলা আড়ত",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ledgers")
      .doc("ledger_1")
      .set({
        userId: ALICE_UID,
        dateText: "2026-11-17",
        displayDate: "১৭ নভেম্বর, ২০২৬",
        aratName: "সোনার বাংলা আড়ত",
        supplierName: "করিম চাষী",
        grades: [{ gradeId: "g1", name: "SS VIP", ratePerKg: 450, cartonWeights: [2.5, 3.0] }],
        commissionPercent: 7.0,
        kuliExpense: 150.0,
        otherExpense: 50.0,
        chalanNote: "ঢাকা মেট্রো-ট ১১-২২৩৩",
        paymentMethod: "নগদ (Cash)",
        signatureText: "রহিম",
        totalCartons: 2,
        totalWeightKg: 5.5,
        grossSales: 2475.0,
        commissionAmount: 173.25,
        netPayable: 2101.75,
        deleted: false,
        createdAt: now,
        updatedAt: now,
      })
  );

  // Soft delete to Recycle Bin
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ledgers")
      .doc("ledger_1")
      .update({
        deleted: true,
        updatedAt: new Date(Date.now() - 500),
      })
  );

  // Cross-user isolation checks
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("ledgers").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ledgers")
      .doc("ledger_1")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).get()
  );
  await assertFails(bobDb.collection("users").get());
});

test("Super Admin UID can manage app_config/global and list registered users; non-admins cannot", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Non-admin cannot write global config
  await assertFails(
    aliceDb.collection("app_config").doc("global").set({
      appName: "Hacked",
      aratName: "Fake",
      defaultCommissionPercent: 1.0,
      defaultKuliRate: 0.0,
      welcomeBanner: "Hi",
      qualityGrades: ["SS VIP"],
      updatedAt: now,
    })
  );

  // Super Admin can create and update global config
  await assertSucceeds(
    adminDb.collection("app_config").doc("global").set({
      appName: "স্ট্রবেরি আড়ত প্রো",
      aratName: "কেন্দ্রীয় স্ট্রবেরি আড়ত",
      defaultCommissionPercent: 7.0,
      defaultKuliRate: 100.0,
      welcomeBanner: "আজকের হিসাব শুরু করুন",
      qualityGrades: ["SS VIP", "VIP", "B-Grade", "Cat (ক্যাট)"],
      updatedAt: now,
    })
  );

  // Signed-in user can read global config
  await assertSucceeds(aliceDb.collection("app_config").doc("global").get());

  // Super Admin can list users directory
  await assertSucceeds(adminDb.collection("users").get());
});

test("Expenses and trash_items enforce ownership and schema validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("expenses")
      .doc("exp_1")
      .set({
        userId: ALICE_UID,
        dateText: "2026-11-17",
        category: "অটো ভাড়া",
        description: "বাজার থেকে আড়ত",
        amount: 120.0,
        deleted: false,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("trash_items")
      .doc("tr_1")
      .set({
        userId: ALICE_UID,
        itemType: "CARTON",
        gradeName: "SS VIP",
        weightKg: 2.5,
        ratePerKg: 450.0,
        cartonWeights: [],
        createdAt: now,
      })
  );

  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("expenses")
      .doc("exp_1")
      .delete()
  );
});
