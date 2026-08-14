import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => ({
  plugins: [react(), tailwindcss()],
  // Vitest's module runner transforms via esbuild rather than Vite 8's oxc build
  // pipeline, so it needs the automatic JSX runtime spelled out explicitly here
  // (dev/build already get it from @vitejs/plugin-react and don't need this).
  esbuild: mode === 'test' ? { jsx: 'automatic' } : undefined,
  test: {
    include: ['src/**/*.test.{js,jsx,ts,tsx}'],
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test-setup.js'],
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
}))
