// components/settings/index.js
export default () => ({
    profile: {
        name: 'Jane Doe',
        email: 'jane.doe@elevatewater.org'
    },
    isSaving: false,      // Tracks network state
    showSuccess: false,   // Tracks toast state
    showErrors: false,    // Prevents premature validation yelling
    
    saveSettings() {
        // 1. Basic validation check
        if (!this.profile.name || !this.profile.email) {
            this.showErrors = true;
            return;
        }

        // 2. Lock the UI
        this.isSaving = true;
        this.showErrors = false;
        this.showSuccess = false;
        
        // 3. Simulate Ktor API request
        setTimeout(() => {
            this.isSaving = false;
            this.showSuccess = true;
            
            // Auto-hide success message
            setTimeout(() => {
                this.showSuccess = false;
            }, 3000);
        }, 800); // 800ms feels like a realistic network hop
    }
});