import { fileURLToPath } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  build: {
    // The editor chunk is Monaco, loaded on demand; it is as big as it is.
    chunkSizeWarningLimit: 3200,
  },
  resolve: {
    alias: {
      '@content': fileURLToPath(new URL('../../content', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: true,
  },
});
