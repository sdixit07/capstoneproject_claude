import { defineConfig, configDefaults } from 'vitest/config'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  test: {
    // tests/ holds Playwright e2e specs; vitest must not collect them
    exclude: [...configDefaults.exclude, 'tests/**'],
  },
})
