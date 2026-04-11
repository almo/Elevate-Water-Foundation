import {setGlobalOptions} from "firebase-functions/v2";
import {
  beforeUserCreated,
  beforeUserSignedIn,
} from "firebase-functions/v2/identity";
import {HttpsError} from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const db = admin.firestore();

// 1. Set Global Region for all v2 functions
setGlobalOptions({
  maxInstances: 10,
  region: "europe-west6",
});

/**
 * Validates if an email exists in the 'invites' collection and is active.
 * @param {string | undefined} email The email of the user to check.
 * @return {Promise<boolean>} True if the invitation is valid and active.
 */
async function checkInvitation(email: string | undefined): Promise<boolean> {
  if (!email) return false;

  try {
    const inviteRef = db.collection("invites").doc(email);
    const inviteDoc = await inviteRef.get();

    if (!inviteDoc.exists) return false;

    const data = inviteDoc.data();
    return data?.active === true;
  } catch (error) {
    console.error("Invitation check failed:", error);
    return false;
  }
}

/**
 * TRIGGER: Runs during the first-ever sign up.
 */
export const validateAndCreateUser = beforeUserCreated(async (event) => {
  const user = event.data;

  // GUARD: If no user data is present, block the request
  if (!user) {
    throw new HttpsError("internal", "No user data found in event.");
  }

  const email = user.email;
  const authorized = await checkInvitation(email);

  if (!authorized) {
    throw new HttpsError("permission-denied", "User not invited or inactive.");
  }

  const userRef = db.collection("users").doc(user.uid);
  await userRef.set({
    uid: user.uid,
    email: email,
    displayName: user.displayName || "",
    photoURL: user.photoURL || "",
    role: "viewer",
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    lastLogin: admin.firestore.FieldValue.serverTimestamp(),
  });
});

/**
 * TRIGGER: Runs on every subsequent login.
 */
export const validateSignIn = beforeUserSignedIn(async (event) => {
  const user = event.data;
  if (!user) throw new HttpsError("internal", "No user data found.");

  const authorized = await checkInvitation(user.email);
  if (!authorized) {
    throw new HttpsError("permission-denied", "Access denied.");
  }

  const userRef = db.collection("users").doc(user.uid);

  // RUN A TRANSACTION
  await db.runTransaction(async (transaction) => {
    const userDoc = await transaction.get(userRef);

    if (!userDoc.exists) {
      // INITIALIZE
      transaction.set(userRef, {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName || "Anonymous",
        photoURL: user.photoURL || "",
        role: "viewer",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        lastLogin: admin.firestore.FieldValue.serverTimestamp(),
      });
    } else {
      // UPDATE
      transaction.update(userRef, {
        lastLogin: admin.firestore.FieldValue.serverTimestamp(),
        displayName: user.displayName || userDoc.data()?.displayName,
        photoURL: user.photoURL || userDoc.data()?.photoURL,
      });
    }
  });
});
