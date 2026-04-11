// Import the functions you need from the SDKs you need
import { initializeApp } from "firebase/app";
import { getAnalytics } from "firebase/analytics";
import { getAuth } from "firebase/auth";
// TODO: Add SDKs for Firebase products that you want to use
// https://firebase.google.com/docs/web/setup#available-libraries

// Your web app's Firebase configuration
// For Firebase JS SDK v7.20.0 and later, measurementId is optional
const firebaseConfig = {
  apiKey: "AIzaSyD2THB4vW42uDHsUSVpiTpX9x3jR2eTVZg",
  authDomain: "elevate-water-foundation.firebaseapp.com",
  projectId: "elevate-water-foundation",
  storageBucket: "elevate-water-foundation.firebasestorage.app",
  messagingSenderId: "367827184501",
  appId: "1:367827184501:web:a1ccfe275de6bf766b013c",
  measurementId: "G-GCWVJTZ499"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);
const analytics = getAnalytics(app);

export const auth = getAuth(app);