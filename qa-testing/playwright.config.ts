import { defineConfig } from '@playwright/test';

/**
 * No browser is ever launched here — every test uses Playwright's `request` fixture (a plain
 * HTTP client) against the live gateway, never `page`. That's why there's no `use.browserName`
 * and no `npx playwright install` step in this project's setup.
 */
export default defineConfig({
  timeout: 30_000,
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: 'list',
  use: {
    baseURL: process.env.GATEWAY_URL ?? 'http://localhost:8080',
    extraHTTPHeaders: {
      'Content-Type': 'application/json',
    },
  },
  projects: [
    { name: 'smoke', testDir: './smoke' },
    { name: 'api', testDir: './api' },
  ],
});
