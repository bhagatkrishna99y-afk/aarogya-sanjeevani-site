import { test, expect } from "@playwright/test";

/**
 * The form's submission is a plain `fetch` to VITE_CONTACT_FORM_ENDPOINT
 * (see src/lib/contactForm.ts) -- `page.route` intercepts that exact
 * network request so these tests never depend on, or hit, the real
 * third-party form service.
 */
const FORM_ENDPOINT = process.env.VITE_CONTACT_FORM_ENDPOINT!;

test.describe("contact form", () => {
  test("shows a validation error per field when submitted empty", async ({ page }) => {
    await page.goto("/contact");
    await page.getByRole("button", { name: "Send message" }).click();

    await expect(page.getByText("Please enter your name.")).toBeVisible();
    await expect(page.getByText("Please enter your email address.")).toBeVisible();
    await expect(page.getByText("Please enter a message.")).toBeVisible();
  });

  test("shows a success state once a mocked submission succeeds", async ({ page }) => {
    await page.route(FORM_ENDPOINT, (route) => route.fulfill({ status: 200, body: "{}" }));

    await page.goto("/contact");
    await page.getByLabel("Your name", { exact: false }).fill("Jane Doe");
    await page.getByLabel("Email address", { exact: false }).fill("jane@example.com");
    await page
      .getByLabel("Message", { exact: false })
      .fill("I have a question about booking a service.");
    await page.getByRole("button", { name: "Send message" }).click();

    await expect(page.getByText("Message sent")).toBeVisible();
    await expect(page.getByText("Thanks for reaching out")).toBeVisible();
  });

  test("shows an error state when the mocked submission fails", async ({ page }) => {
    await page.route(FORM_ENDPOINT, (route) => route.fulfill({ status: 500, body: "{}" }));

    await page.goto("/contact");
    await page.getByLabel("Your name", { exact: false }).fill("Jane Doe");
    await page.getByLabel("Email address", { exact: false }).fill("jane@example.com");
    await page
      .getByLabel("Message", { exact: false })
      .fill("I have a question about booking a service.");
    await page.getByRole("button", { name: "Send message" }).click();

    await expect(page.getByText("Something went wrong")).toBeVisible();
    // The form itself (and what was typed into it) is still there to retry --
    // a failed submission must not be treated like a successful one.
    await expect(page.getByLabel("Your name", { exact: false })).toHaveValue("Jane Doe");
  });
});
