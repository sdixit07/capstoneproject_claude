import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 60000,
  expect: { timeout: 10000},
  use: {
    baseURL: process.env.FE_BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
  },
  webServer: process.env.CI_CI ? undefined : {
    command: 'npm run dev -- --host 0.0.0.0',
    url: process.env.FE_BASE_URL || 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 120000,
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices ['Desktop Chrome'] },
    },
  ],
});
