import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  root: '.',
  
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    // We remove the explicit assetFileNames to allow proper caching hashes
    manifest: true, 
    rollupOptions: {
      input: {
        // 1. The Global Shell (Outputs to dist/index.html)
        main: resolve(__dirname, 'index.html'),
        
        // 2. The Fragments (Outputs to dist/components/.../template.html)
        // The key determines the folder structure inside `dist`
        'components/main/template': resolve(__dirname, 'components/main/template.html'),
        'components/settings/template': resolve(__dirname, 'components/settings/template.html'),
        'components/sidebar/template': resolve(__dirname, 'components/sidebar/template.html')
      }
    }
  },
  
  resolve: {
    alias: {
      'components': resolve(__dirname, 'components'),
      'locales': resolve(__dirname, 'locales'),
      'i18n.js': resolve(__dirname, 'i18n.js')
    }
  }
});