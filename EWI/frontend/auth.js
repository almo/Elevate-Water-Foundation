// root/frontend/auth.js
import { auth } from './firebase.js';
// ADD 'GoogleAuthProvider' and 'signInWithPopup' to this line:
import {
    onAuthStateChanged,
    signOut,
    GoogleAuthProvider,
    signInWithPopup
} from "firebase/auth";

export default {
    user: null,
    initialized: false,
    error: null,

    init() {
        onAuthStateChanged(auth, (user) => {
            this.user = user;
            this.initialized = true;
        });
    },

    async login() {
        this.error = null; // Clear previous errors
        const provider = new GoogleAuthProvider();

        try {
            await signInWithPopup(auth, provider);
        } catch (error) {
            console.error("Login failed:", error.code, error.message);

            // Catch the specific rejection from europe-west6
            if (error.message.includes("Access denied") || error.message.includes("permission-denied")) {
                this.error = "Your email is not authorized for this platform.";
            } else if (error.code === 'auth/popup-closed-by-user') {
                this.error = "Login window was closed.";
            } else {
                this.error = "Authentication failed.";
            }
        }
    },

    async logout() {
        await signOut(auth);
        window.location.reload();
    },

    async getToken() {
        if (!auth.currentUser) return null;
        return await auth.currentUser.getIdToken();
    }
};