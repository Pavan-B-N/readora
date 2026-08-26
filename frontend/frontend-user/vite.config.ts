import { fileURLToPath, URL } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
  },
  // sockjs-client (notification WebSocket) references the Node global `global`, which Vite
  // doesn't polyfill for the browser like Webpack does — without this the app fails to boot.
  define: {
    global: 'globalThis',
  },
})
