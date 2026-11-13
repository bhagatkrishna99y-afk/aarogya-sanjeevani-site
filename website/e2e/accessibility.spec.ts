import AxeBuilder from "@axe-core/playwright";
import { test, expect } from "@playwright/test";

/**
 * Runs a real axe-core scan against every route a signed-out visitor can
 * reach, in a real browser (not jsdom) -- catches things the
 * component-level vitest-axe tests can't: real computed styles/fonts/
 * layout, real cascade, real focus order across a fully-assembled page.
 * `/admin/*` beyond `/admin/login` needs a real session this suite has no
 * way to establish, so it isn't covered here; its component-level pages
 * still get the same vitest-axe treatment as everything else (see each
 * page's own `*.test.tsx` under src/pages/admin -- tracked separately,
 * not part of this file).
 *
 * WCAG 2.2 AA is the explicit target everywhere in this app -- see
 * `withTags` below. That tag set includes axe-core's automatable checks
 * for 2.5.8 (target size, i.e. the 44px touch-target minimum) and 1.4.3/
 * 1.4.11 (contrast) -- both covered by this one scan per page, not a
 * separate test each.
 */
// Pages rendered inside PublicLayout (header/primary-nav/main/footer).
// /admin/login deliberately isn't one of these -- it's a standalone,
// minimal auth screen with none of that chrome by design, so it's checked
// separately below instead of against the same landmark expectations.
const LAYOUT_PAGES = [
  "/",
  "/services",
  "/about",
  "/contact",
  "/privacy-policy",
  "/terms",
  "/this-page-does-not-exist", // the 404 page -- still nested inside PublicLayout
];
const PUBLIC_PAGES = [...LAYOUT_PAGES, "/admin/login"];

for (const path of PUBLIC_PAGES) {
  test(`${path} has no axe violations (WCAG 2.2 AA)`, async ({ page }) => {
    await page.goto(path);
    const results = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"])
      .analyze();

    expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
  });
}

for (const path of LAYOUT_PAGES) {
  // axe's "page-has-heading-one"/"landmark-*" rules are opt-in best-practice
  // checks, not part of the WCAG-tagged set scanned above -- checked
  // directly instead so a regression here is unambiguous, not buried in a
  // 50-rule tag scan.
  test(`${path} has exactly one h1 and the four standard landmarks`, async ({ page }) => {
    await page.goto(path);
    await expect(page.locator("h1")).toHaveCount(1);
    await expect(page.getByRole("banner")).toHaveCount(1); // <header>
    await expect(page.getByRole("navigation", { name: "Primary" })).toHaveCount(1);
    await expect(page.getByRole("main")).toHaveCount(1);
    await expect(page.getByRole("contentinfo")).toHaveCount(1); // <footer>
  });

  // WCAG 1.4.4 (Resize Text): content must reflow and stay usable at 200%
  // zoom, not just shrink/clip. Zooming a 1280px-wide viewport to 200% is
  // equivalent to laying the same page out in 640 CSS px -- if that
  // produces horizontal scrolling, real content is being cut off/requires
  // scrolling to reach, which is exactly what 1.4.4 forbids.
  test(`${path} has no horizontal overflow at a 200%-zoom-equivalent width`, async ({ page }) => {
    await page.setViewportSize({ width: 640, height: 800 });
    await page.goto(path);
    const { scrollWidth, clientWidth } = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }));
    expect(
      scrollWidth,
      "page content is wider than the viewport -- something doesn't reflow",
    ).toBeLessThanOrEqual(clientWidth);
  });
}

test("/admin/login has no horizontal overflow at a 200%-zoom-equivalent width", async ({
  page,
}) => {
  await page.setViewportSize({ width: 640, height: 800 });
  await page.goto("/admin/login");
  const { scrollWidth, clientWidth } = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
  }));
  expect(scrollWidth).toBeLessThanOrEqual(clientWidth);
});
