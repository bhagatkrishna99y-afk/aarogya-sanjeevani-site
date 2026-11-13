import { test, expect } from "@playwright/test";

test.describe("public site smoke test", () => {
  test("home page loads with the right title and primary nav", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveTitle(/Home · Aarogya Sanjeevani/);
    await expect(page.getByRole("heading", { level: 1 })).toContainText(
      "Find trusted healthcare, without the guesswork.",
    );
    await expect(page.getByRole("navigation", { name: "Primary" })).toBeVisible();
  });

  test("primary nav links to Services, About and Contact work", async ({ page, isMobile }) => {
    // Desktop-only: the "mobile-chrome"/"mobile-safari" projects render
    // the responsive layout's hamburger menu instead, where this exact
    // desktop nav is legitimately hidden/`inert`, not broken -- that
    // interaction is covered separately by the "mobile menu" describe
    // block below, which every project (including the 3 desktop ones,
    // via its own narrow-viewport override) also runs.
    test.skip(isMobile, "the desktop primary nav is hidden at mobile viewports by design");

    await page.goto("/");
    // Scoped to the primary nav specifically: the homepage's services
    // section also has a "Browse all services" CTA link, whose accessible
    // name contains "services" and would otherwise match getByRole's
    // default substring matching too.
    const nav = page.getByRole("navigation", { name: "Primary" });

    await nav.getByRole("link", { name: "Services", exact: true }).click();
    await expect(page).toHaveURL(/\/services$/);
    await expect(page).toHaveTitle(/Services · Aarogya Sanjeevani/);

    await nav.getByRole("link", { name: "About", exact: true }).click();
    await expect(page).toHaveURL(/\/about$/);
    await expect(page).toHaveTitle(/About · Aarogya Sanjeevani/);

    await nav.getByRole("link", { name: "Contact", exact: true }).click();
    await expect(page).toHaveURL(/\/contact$/);
    await expect(page).toHaveTitle(/Contact · Aarogya Sanjeevani/);
  });

  test("an unknown route renders the not-found page with a way back home", async ({ page }) => {
    await page.goto("/this-page-does-not-exist");
    await expect(page.getByRole("heading", { name: "Page not found" })).toBeVisible();
    await page.getByRole("link", { name: "Back to home" }).click();
    await expect(page).toHaveURL(/\/$/);
  });

  test("the admin console redirects a signed-out visitor to sign in", async ({ page }) => {
    await page.goto("/admin");
    await expect(page).toHaveURL(/\/admin\/login$/);
    await expect(page.getByRole("heading", { name: "Ops console sign in" })).toBeVisible();
  });

  test("the legal pages are reachable from the footer", async ({ page }) => {
    await page.goto("/");
    await page.getByRole("contentinfo").getByRole("link", { name: "Privacy Policy" }).click();
    await expect(page).toHaveURL(/\/privacy-policy$/);
    await expect(page.getByRole("heading", { level: 1 })).toContainText("Privacy Policy");

    // Not `page.goBack()`: its default `waitUntil: "load"` waits for a
    // real navigation "load" event, which a client-side-routed SPA never
    // fires on a history-only transition -- Firefox's Playwright driver
    // actually waits for it and times out; re-visiting "/" directly gets
    // the same end state without depending on that event firing at all.
    await page.goto("/");
    await page.getByRole("contentinfo").getByRole("link", { name: "Terms" }).click();
    await expect(page).toHaveURL(/\/terms$/);
    await expect(page.getByRole("heading", { level: 1 })).toContainText("Terms of Use");
  });

  test("the footer states the non-emergency disclaimer with India's emergency number", async ({
    page,
  }) => {
    await page.goto("/");
    await expect(page.getByRole("contentinfo")).toContainText("not for medical emergencies");
    await expect(page.getByRole("contentinfo")).toContainText("112");
  });

  test("the skip link is the first thing Tab reaches, and jumps focus to main content", async ({
    page,
    browserName,
  }) => {
    // WebKit's Playwright driver doesn't move focus on a real Tab
    // keypress by default (matching real Safari, which needs OS-level
    // "Full Keyboard Access" enabled for Tab to reach non-form elements
    // like links -- a known, documented WebKit/Safari behavior, not a
    // bug in this site). Programmatic `.focus()` still works on webkit,
    // just not a simulated Tab keypress, so this specific interaction
    // can't be verified on this engine.
    test.skip(browserName === "webkit", "WebKit doesn't move focus on a simulated Tab keypress");

    await page.goto("/");
    const skipLink = page.getByRole("link", { name: "Skip to main content" });
    // Wait for the app to have actually mounted before pressing Tab --
    // `goto()` resolving doesn't mean React has rendered into #root yet,
    // and Tab (unlike an `expect(...).toBeVisible()` assertion) doesn't
    // retry/wait for anything to exist.
    await skipLink.waitFor({ state: "attached" });

    await page.keyboard.press("Tab");
    await expect(skipLink).toBeFocused();

    await page.keyboard.press("Enter");
    await expect(page.locator("#main-content")).toBeFocused();
  });
});

test.describe("mobile menu (narrow viewport)", () => {
  // A plain narrow viewport override, not a full `devices[...]` preset --
  // this describe block runs under every project in playwright.config.ts
  // (including the real "mobile-chrome"/"mobile-safari" device projects,
  // which already default to a narrow viewport; this override is a no-op
  // there and the thing that actually matters on the 3 desktop-engine
  // projects, forcing the same narrow width that flips the `md:hidden`/
  // `md:block` Tailwind breakpoints switching the header into its
  // hamburger layout).
  test.use({ viewport: { width: 390, height: 844 } });

  test("opens from the hamburger, traps focus, and Escape closes it back to the hamburger", async ({
    page,
  }) => {
    await page.goto("/");

    const hamburger = page.getByRole("button", { name: "Open menu" });
    await expect(hamburger).toBeVisible();
    await hamburger.click();

    const dialog = page.getByRole("dialog", { name: "Menu" });
    await expect(dialog).toBeVisible();
    await expect(page.getByRole("button", { name: "Close menu" })).toBeFocused();

    await page.keyboard.press("Escape");
    // Not `toBeHidden()`: the closed panel is still laid out (translated
    // off-screen via CSS transform for a smooth slide animation, not
    // display:none), so Playwright's layout-based visibility check sees
    // it as "visible" regardless -- `inert` plus actually being outside
    // the viewport is what makes it correctly non-interactive and
    // unseen, so check those two things directly instead.
    await expect(dialog).toHaveAttribute("inert", "");
    await expect(dialog).not.toBeInViewport();
    await expect(hamburger).toBeFocused();
  });

  test("closes after choosing a destination, and navigates there", async ({ page }) => {
    await page.goto("/");
    await page.getByRole("button", { name: "Open menu" }).click();
    const dialog = page.getByRole("dialog", { name: "Menu" });
    await dialog.getByRole("link", { name: "About" }).click();
    await expect(page).toHaveURL(/\/about$/);
    await expect(dialog).toHaveAttribute("inert", "");
    await expect(dialog).not.toBeInViewport();
  });
});
