import { defineConfig } from 'vite';

// In development the API runs on :8080; proxying keeps the browser on a single origin (no CORS).
export default defineConfig({
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
