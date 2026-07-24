import { defineConfig } from 'vite';
import path from 'path';

export default defineConfig({
  root: 'src/main/resources/static',
  publicDir: path.resolve(__dirname, 'src/main/resources/static'),
  build: {
    outDir: path.resolve(__dirname, 'dist'),
    emptyOutDir: true
  },
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
