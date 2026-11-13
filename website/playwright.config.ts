import { defineConfig, devices } from "@playwright/test";

// So e2e/*.spec.ts files can read VITE_PLAY_STORE_PATIENT_URL etc. from the
// same .env the app itself was built with (process.env, not import.meta.env
// -- this file and the specs run in plain Node, never bundled by Vite).
process.loadEnvFile(".env");

// accessibility.spec.ts and console.spec.ts are deep per-page checks (axe-
// core's full WCAG rule set, console-error capture) whose outcome doesn't
// depend on which rendering engine ran them, so they only run once, on the
// "chromium" project below (which has no testMatch restriction). Every
// other spec is a real cross-browser/cross-viewport interaction test and
// runs on all 5 projects.
// Visual-regression baselines are brittle across *engines* (different
// font/anti-aliasing per browser), so they only ever run under one engine
// (chromium) -- see e2e/visual.spec.ts, which sets its own 3 viewports
// itself via `test.use()` rather than needing separate projects here.
const CROSS_BROWSER_ONLY = /^(?!.*(accessibility|console|visual)\.spec\.ts$).*\.spec\.ts$/;

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  // Browser process launch failures (seen running headless Firefox under
  // heavy parallel load) are a known category of CI flakiness, not a sign
  // of a broken test -- retrying once in CI is standard Playwright
  // practice for exactly this, and costs nothing locally where it's off.
  retries: process.env.CI ? 1 : 0,
  // Running 3 browser engines' worth of headless processes fully in
  // parallel is real resource pressure; capped (rather than one worker
  // per CPU core, Playwright's default) keeps that pressure down without
  // making the suite meaningfully slower.
  workers: process.env.CI ? 4 : 3,
  reporter: "html",
  use: {
    baseURL: "http://localhost:4173",
    trace: "on-first-retry",
  },
  webServer: {
    command: "npm run build && npm run preview -- --port 4173",
    url: "http://localhost:4173",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
  projects: [
    // The "primary" project: every spec, including the chromium-only ones
    // and the visual-regression baselines.
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },

    { name: "firefox", use: { ...devices["Desktop Firefox"] }, testMatch: CROSS_BROWSER_ONLY },
    { name: "webkit", use: { ...devices["Desktop Safari"] }, testMatch: CROSS_BROWSER_ONLY },
    // No "Mobile Firefox": Playwright has no device preset for it (Firefox
    // for Android isn't driven the same way as desktop Firefox), so mobile
    // coverage is Chromium + WebKit only -- the same two engines every real
    // phone actually ships (Chrome-based browsers and Mobile Safari/WKWebView).
    {
      name: "mobile-chrome",
      use: { ...devices["Pixel 5"] },
      testMatch: CROSS_BROWSER_ONLY,
    },
    {
      name: "mobile-safari",
      use: { ...devices["iPhone 13"] },
      testMatch: CROSS_BROWSER_ONLY,
    },
  ],
});
