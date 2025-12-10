import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  timeout: 120000,
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:9080',
    headless: true,
  },
  retries: 0,
});
