/**
 * SQPlus Server-Side Admin Role Provisioning Script
 * 
 * Usage with Firebase Admin SDK:
 * 1. Download your serviceAccountKey.json from Firebase Console -> Project Settings -> Service Accounts
 * 2. Run: node scripts/set-admin-claim.js <USER_EMAIL_OR_UID>
 * 
 * This sets the custom claim { admin: true } on the Firebase Auth account.
 * This claim is cryptographically signed by Firebase and cannot be forged by client apps.
 */

const admin = require('firebase-admin');
const serviceAccount = require('../serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

async function setAdminClaim(identifier) {
  try {
    let user;
    if (identifier.includes('@')) {
      user = await admin.auth().getUserByEmail(identifier);
    } else {
      user = await admin.auth().getUser(identifier);
    }

    console.log(`Found user: ${user.email} (${user.uid})`);

    // Set custom user claims for Admin
    await admin.auth().setCustomUserClaims(user.uid, {
      admin: true,
      role: 'admin'
    });

    // Update Firestore user document
    await admin.firestore().collection('users').doc(user.uid).set({
      uid: user.uid,
      email: user.email,
      role: 'admin',
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    console.log(`Successfully assigned { admin: true } claim to ${user.email}.`);
    process.exit(0);
  } catch (error) {
    console.error('Error setting admin claim:', error);
    process.exit(1);
  }
}

const targetUser = process.argv[2] || 'hollowfaith1001@gmail.com';
setAdminClaim(targetUser);
