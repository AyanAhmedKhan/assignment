import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests/auth',
  fullyParallel: false,
  workers: 1,
  timeout: 30000,
  use: { baseURL: process.env.APP_URL || 'http://127.0.0.1:8081', viewport: { width: 1440, height: 1000 } },
});
