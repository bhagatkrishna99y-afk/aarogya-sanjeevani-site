import { test, expect } from "@playwright/test";

test.describe("FAQ accordion", () => {
  test("opens and closes a question by keyboard alone", async ({ page }) => {
    await page.goto("/");
    const question = page.getByRole("button", { name: "What is Aarogya Sanjeevani?" });
    await question.focus();
    await expect(question).toHaveAttribute("aria-expanded", "false");

    // The answer panel is `hidden` (not just visually collapsed) while
    // closed, so it genuinely isn't in the accessibility tree yet.
    await question.press("Enter");
    await expect(question).toHaveAttribute("aria-expanded", "true");
    const panelId = await question.getAttribute("aria-controls");
    const panel = page.locator(`#${panelId}`);
    await expect(panel).toBeVisible();

    await question.press("Enter");
    await expect(question).toHaveAttribute("aria-expanded", "false");
    await expect(panel).toBeHidden();

    // Space toggles it too, not just Enter.
    await question.press("Space");
    await expect(question).toHaveAttribute("aria-expanded", "true");
    await expect(panel).toBeVisible();
  });

  test("ArrowDown/ArrowUp move focus between questions", async ({ page }) => {
    await page.goto("/");
    const first = page.getByRole("button", { name: "What is Aarogya Sanjeevani?" });
    await first.focus();

    await page.keyboard.press("ArrowDown");
    await expect(first).not.toBeFocused();

    await page.keyboard.press("ArrowUp");
    await expect(first).toBeFocused();
  });
});

test.describe("app store badges", () => {
  test("link to the configured Play Store URLs", async ({ page }) => {
    const patientUrl = process.env.VITE_PLAY_STORE_PATIENT_URL!;
    const doctorUrl = process.env.VITE_PLAY_STORE_DOCTOR_URL!;

    // Scoped to the "Get the app" section specifically -- the footer also
    // has its own pair of store badges, so an unscoped locator matches both.
    await page.goto("/");
    const section = page.getByRole("region", { name: "Get the app" });
    const patientBadge = section.getByRole("link", { name: /Patient app$/ });
    const doctorBadge = section.getByRole("link", { name: /Doctor app$/ });

    await expect(patientBadge).toHaveAttribute("href", patientUrl);
    await expect(doctorBadge).toHaveAttribute("href", doctorUrl);

    // Both open in a new tab safely -- never `target="_blank"` alone.
    await expect(patientBadge).toHaveAttribute("rel", "noopener noreferrer");
    await expect(doctorBadge).toHaveAttribute("rel", "noopener noreferrer");
  });
});
