import { test, expect } from "@playwright/test";

/**
 * Visual-regression baseline for the Home page hero at three viewports.
 * Chromium only (playwright.config.ts routes this file there exclusively)
 * -- comparing screenshots *across different rendering engines* is
 * inherently noisy (font hinting/anti-aliasing differ even for pixel-
 * identical markup), so cross-engine coverage belongs to the real
 * interaction tests, not pixel snapshots.
 *
 * IMPORTANT -- platform-dependent baselines: Playwright's screenshot
 * comparison is itself sensitive to the OS it ran on (font rendering
 * differs between Windows/macOS/Linux even within the same browser
 * engine and Playwright version). These baselines were generated on
 * Windows; a Linux CI runner (e.g. GitHub Actions' ubuntu-latest, used
 * by .github/workflows/website-ci.yml) will very likely need its own
 * baseline. The first time this runs in that CI environment, expect it
 * to fail on a pixel diff -- that's not a real regression, it's the
 * platform mismatch. Fix it with:
 *
 *   npx playwright test e2e/visual.spec.ts --update-snapshots
 *
 * run *inside* that CI environment (a one-off workflow_dispatch job, or
 * downloaded from a failed run's artifacts), then commit the regenerated
 * files under e2e/visual.spec.ts-snapshots/. After that one-time fix,
 * further runs on that same CI platform compare correctly.
 */
const VIEWPORTS = {
  mobile: { width: 390, height: 844 },
  tablet: { width: 768, height: 1024 },
  desktop: { width: 1280, height: 800 },
};

for (const [name, viewport] of Object.entries(VIEWPORTS)) {
  test.describe(`home hero (${name})`, () => {
    test.use({ viewport });

    test(`matches the ${name} baseline`, async ({ page }) => {
      await page.goto("/");
      const hero = page.locator("section", { has: page.locator("#hero-heading") });
      await expect(hero).toHaveScreenshot(`home-hero-${name}.png`, {
        // A little slack for sub-pixel anti-aliasing noise between runs
        // on the *same* platform -- not meant to absorb a real visual
        // change, which is almost always a much bigger ratio than this.
        maxDiffPixelRatio: 0.02,
      });
    });
  });
}
