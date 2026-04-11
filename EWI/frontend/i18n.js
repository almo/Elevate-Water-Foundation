import en from './locales/en.json';
import es from './locales/es.json';

// ARCHITECTURE: Alpine.js Global Store
// By placing i18n in a global Alpine store, we enable instant reactivity across all HTML fragments.
// A UI language swap doesn't require a page reload or a server round-trip,
// adhering to standard SPA UX expectations even in a hypermedia-driven architecture.

export default {
    locale: 'en',
    messages: { en, es },

    init() {
        // Check browser preference on initialization
        const browserLang = navigator.language.split('-')[0];
        if (this.messages[browserLang]) {
            this.setLocale(browserLang);
        }
    },
    setLocale(lang) {
        this.locale = lang;
        // UX/ACCESSIBILITY: Always update the root lang attribute so screen readers know the current dialect.
        document.documentElement.lang = lang;
    },
    t(key) {
        return this.messages[this.locale]?.[key] || key;
    }
};