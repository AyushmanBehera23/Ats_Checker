import { defineConfig } from 'vite';

export default defineConfig({
  root: 'src/main/resources/static',
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
