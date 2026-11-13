import { test, expect } from "@playwright/test";

/**
 * Every real route the app serves: loads with a genuine HTTP 200 (not
 * just "the page didn't crash") and renders the correct h1. `/admin`
 * itself isn't listed -- it redirects to `/admin/login` (see
 * smoke.spec.ts's "admin console redirects a signed-out visitor" test),
 * so its own h1 is never actually what renders at that URL.
 */
const ROUTES: { path: string; h1: string }[] = [
  { path: "/", h1: "Find trusted healthcare, without the guesswork." },
  { path: "/services", h1: "Services" },
  { path: "/about", h1: "About Aarogya Sanjeevani" },
  { path: "/contact", h1: "Contact us" },
  { path: "/privacy-policy", h1: "Privacy Policy" },
  { path: "/terms", h1: "Terms of Use" },
  { path: "/admin/login", h1: "Ops console sign in" },
];

for (const { path, h1 } of ROUTES) {
  test(`${path} responds 200 and renders its h1`, async ({ page }) => {
    const response = await page.goto(path);
    expect(response?.status()).toBe(200);
    await expect(page.getByRole("heading", { level: 1 })).toHaveText(h1);
  });
}
