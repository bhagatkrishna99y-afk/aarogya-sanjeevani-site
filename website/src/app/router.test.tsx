import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AppRouter } from "./router";

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AppRouter />
    </MemoryRouter>,
  );
}

describe("AppRouter", () => {
  it.each([
    ["/", "Find trusted healthcare, without the guesswork."],
    ["/about", "About Aarogya Sanjeevani"],
    ["/services", "Services"],
    ["/contact", "Contact us"],
    ["/privacy-policy", "Privacy Policy"],
    ["/terms", "Terms of Use"],
  ])(
    "renders the right page (lazy-loaded) for %s",
    async (path, expectedHeading) => {
      renderAt(path);
      // `findByRole`'s own wait-for defaults to 1s, independent of the test
      // timeout -- too tight for a lazy import to resolve once the full
      // suite is running many jsdom environments in parallel.
      expect(
        await screen.findByRole("heading", { level: 1, name: expectedHeading }, { timeout: 15000 }),
      ).toBeInTheDocument();
    },
    20000,
  );

  it("renders the header and footer around every public page", async () => {
    renderAt("/about");
    await screen.findByRole("heading", { level: 1 });
    expect(screen.getByRole("navigation", { name: "Primary" })).toBeInTheDocument();
    expect(screen.getByRole("contentinfo")).toBeInTheDocument();
  });

  it("renders the friendly 404 page, with header/footer, for an unknown route", async () => {
    renderAt("/this-page-does-not-exist");
    expect(
      await screen.findByRole("heading", { level: 1, name: "Page not found" }),
    ).toBeInTheDocument();
    // Still inside the normal site chrome, not a bare orphan page.
    expect(screen.getByRole("navigation", { name: "Primary" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Back to home" })).toBeInTheDocument();
  });

  it("also falls back to the 404 page for an unmatched path under /admin", async () => {
    renderAt("/admin/this-does-not-exist");
    expect(
      await screen.findByRole("heading", { level: 1, name: "Page not found" }),
    ).toBeInTheDocument();
  });
});
