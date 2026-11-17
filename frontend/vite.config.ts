import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import mkcert from 'vite-plugin-mkcert'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [mkcert(), react(), tailwindcss()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
