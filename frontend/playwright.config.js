import { defineConfig, devices } from '@playwright/test'

// E2E 全部用 page.route mock 后端响应，不依赖真实后端服务，CI 可直接运行。
export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: false,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://127.0.0.1:4173',
    trace: 'retain-on-failure',
    ...devices['Desktop Chrome'],
    // 本地用系统 Edge（免下载浏览器）；CI 不设 channel，用 npx playwright install 的 chromium
    ...(process.env.CI ? {} : { channel: 'msedge' })
  },
  webServer: {
    command: 'npm run preview -- --host 127.0.0.1 --port 4173',
    url: 'http://127.0.0.1:4173',
    reuseExistingServer: !process.env.CI,
    timeout: 60000
  }
})
