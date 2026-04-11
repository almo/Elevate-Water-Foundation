import './style.css';

// Core Libraries
import Alpine from 'alpinejs';
import ajax from '@imacrayon/alpine-ajax';

// Architectural Layer: Stores & Components
import authStore from './auth.js';
import i18nStore from './i18n.js';
import mainData from './components/main/index.js';
import settings from './components/settings/index.js';
import sidebarNav from './components/sidebar/index.js';

// Register Alpine Plugins
Alpine.plugin(ajax);

// Register Stores & Data Components
Alpine.store('auth', authStore);
Alpine.store('i18n', i18nStore);
Alpine.data('mainData', mainData);
Alpine.data('settings', settings);
Alpine.data('sidebarNav', sidebarNav);

// Initialize framework
Alpine.start();

// ==========================================
// AJAX INTERCEPTOR: Attach Firebase Token
// ==========================================
document.addEventListener('alpine-ajax:before-send', async (event) => {
    const token = await authStore.getToken();
    if (token) {
        event.detail.config.headers['Authorization'] = `Bearer ${token}`;
    }
});

// ==========================================
// BOOTLOADER: Wait for Auth then Load Fragment
// ==========================================
const loadInitialFragment = async () => {
    const contentTarget = document.getElementById('content');
    if (!contentTarget) return;

    // 1. Wait for Firebase to determine if we have a user
    // We check the initialized state from our authStore
    if (!authStore.initialized) {
        setTimeout(loadInitialFragment, 50); // Retry until initialized
        return;
    }

    // 2. If no user, don't fetch (the index.html template handles showing login)
    if (!authStore.user) return;

    // 3. User is authenticated, proceed with fetch
    const currentPath = window.location.pathname;
    contentTarget.setAttribute('aria-busy', 'true');

    try {
        const token = await authStore.getToken();
        const response = await fetch(currentPath, {
            headers: {
                'X-Alpine-Request': 'true',
                'Authorization': `Bearer ${token}` // Manually add token for the raw fetch
            }
        });

        if (!response.ok) throw new Error(`Server returned ${response.status}`);

        const html = await response.text();
        contentTarget.innerHTML = html;
    } catch (error) {
        console.error("Failed to load initial view:", error);
        contentTarget.innerHTML = `<div class="error-message" style="padding: 2rem;">Failed to load module.</div>`;
    } finally {
        contentTarget.removeAttribute('aria-busy');
    }
};

// Start the boot sequence
document.addEventListener('DOMContentLoaded', loadInitialFragment);