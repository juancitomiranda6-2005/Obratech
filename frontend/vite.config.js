import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: false,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/oauth2': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/js': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/css': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/img': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/styles': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
      '/webjars': {
        target: 'http://localhost:9999',
        changeOrigin: true,
      },
    },
  },
})
