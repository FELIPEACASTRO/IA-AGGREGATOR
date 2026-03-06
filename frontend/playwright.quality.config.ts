import { defineConfig, devices } from '@playwright/test';

const QUALITY_BASE_URL = process.env.E2E_BASE_URL || 'http://127.0.0.1:3001';

export default defineConfig({
  testDir: './e2e',
  testMatch: ['quality-gates.spec.ts'],
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  timeout: 120_000,
  expect: {
    timeout: 12_000,
    toHaveScreenshot: {
      maxDiffPixelRatio: 0.02,
      animations: 'disabled',
      caret: 'hide',
    },
  },
  snapshotPathTemplate: '{testDir}/{testFilePath}-snapshots/{arg}{ext}',
  reporter: [['list']],
  use: {
    baseURL: QUALITY_BASE_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  webServer: {
    command: 'npm run start -- --port 3001',
    cwd: '.',
    url: `${QUALITY_BASE_URL}/login`,
    timeout: 120_000,
    reuseExistingServer: !process.env.CI,
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
      },
    },
  ],
});

