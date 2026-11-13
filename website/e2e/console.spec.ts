import { test, expect } from "@playwright/test";

/**
 * Fails if any page logs a console error or warning in a production
 * build. React's own dev-mode warnings (key props, deprecated lifecycle
 * methods, etc.) only exist in development builds -- this runs against
 * `dist/` via the same preview server every other e2e test uses, so a
 * failure here is a real issue, not noise from React's dev tooling.
 */
const PAGES = [
  "/",
  "/services",
  "/about",
  "/contact",
  "/privacy-policy",
  "/terms",
  "/this-page-does-not-exist",
  "/admin/login",
];

for (const path of PAGES) {
  test(`${path} logs no console errors or warnings`, async ({ page }) => {
    const messages: string[] = [];
    page.on("console", (msg) => {
      if (msg.type() === "error" || msg.type() === "warning") {
        messages.push(`[${msg.type()}] ${msg.text()}`);
      }
    });
    page.on("pageerror", (err) => messages.push(`[pageerror] ${err.message}`));

    await page.goto(path, { waitUntil: "networkidle" });

    expect(messages, messages.join("\n")).toEqual([]);
  });
}
