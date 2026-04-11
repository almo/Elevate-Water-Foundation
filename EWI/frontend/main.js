import './style.css';

// Core Libraries
import Alpine from 'alpinejs';
import ajax from '@imacrayon/alpine-ajax';

// Architectural Layer: Stores & Components
import i18nStore from './i18n.js';
import mainData from './components/main/index.js';
import settings from './components/settings/index.js';
import sidebarNav from './components/sidebar/index.js';

// Register Alpine Plugins
Alpine.plugin(ajax);

// Register Stores & Data Components
Alpine.store('i18n', i18nStore);
Alpine.data('mainData', mainData);
Alpine.data('settings', settings);
Alpine.data('sidebarNav', sidebarNav);

// Initialize framework
Alpine.start();

// ==========================================
// INITIAL FRAGMENT BOOTLOADER
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    // 1. Get the URL the user landed on (e.g., '/' or '/settings')
    const currentPath = window.location.pathname;
    const contentTarget = document.getElementById('content');
    
    if (contentTarget) {
        // 2. Trigger your CSS spinner so the user knows it's loading
        contentTarget.setAttribute('aria-busy', 'true');

        // 3. Make the initial AJAX call, mimicking Alpine AJAX's header
        fetch(currentPath, {
            headers: {
                'X-Alpine-Request': 'true'
            }
        })
        .then(response => {
            if (!response.ok) throw new Error('Fragment not found');
            return response.text();
        })
        .then(html => {
            // 4. Inject the HTML. 
            // Because Alpine uses a MutationObserver, it will automatically 
            // detect this new HTML, parse it, and initialize any x-data components inside!
            contentTarget.innerHTML = html;
        })
        .catch(error => {
            console.error("Failed to load initial view:", error);
            contentTarget.innerHTML = `<div class="error-message" style="padding: 2rem;">Failed to load module.</div>`;
        })
        .finally(() => {
            // 5. Remove the spinner
            contentTarget.removeAttribute('aria-busy');
        });
    }
});