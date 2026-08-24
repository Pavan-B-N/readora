import { defineConfig } from '@playwright/test';

// API-only — no browser projects, so `npx playwright install` isn't needed for this suite.
export default defineConfig({
  timeout: 30_000,
  fullyParallel: false,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8080',
    extraHTTPHeaders: { 'Content-Type': 'application/json' },
  },
  projects: [
    { name: 'smoke', testDir: './smoke' },
  ],
});
