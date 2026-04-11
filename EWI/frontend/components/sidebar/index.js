export default () => ({
    currentPath: window.location.pathname,
    
    init() {
        window.addEventListener('ajax:success', () => {
            this.currentPath = window.location.pathname;
        });
        window.addEventListener('popstate', () => {
            this.currentPath = window.location.pathname;
        });
    },
    
    isActive(path) {
        if (path === '/') {
            return this.currentPath === '/';
        }
        return this.currentPath.startsWith(path);
    }
});