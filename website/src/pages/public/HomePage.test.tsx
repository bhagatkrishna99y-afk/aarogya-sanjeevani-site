import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { homeContent } from "@/content/home";
import { HomePage } from "./HomePage";

function renderHomePage() {
  return render(
    <MemoryRouter>
      <HomePage />
    </MemoryRouter>,
  );
}

describe("HomePage", () => {
  it("renders exactly one h1, the hero headline", () => {
    renderHomePage();
    const headings = screen.getAllByRole("heading", { level: 1 });
    expect(headings).toHaveLength(1);
    expect(headings[0]).toHaveTextContent(homeContent.hero.headline);
  });

  it("renders every section's heading, in order", () => {
    renderHomePage();
    const headings = screen
      .getAllByRole("heading", { level: 1 })
      .concat(screen.getAllByRole("heading", { level: 2 }));
    const text = headings.map((h) => h.textContent).join(" | ");
    expect(text).toContain(homeContent.hero.headline);
    expect(text).toContain(homeContent.howItWorks.heading);
    expect(text).toContain(homeContent.services.heading);
    expect(text).toContain(homeContent.trust.heading);
    expect(text).toContain(homeContent.app.heading);
    expect(text).toContain(homeContent.finalCta.heading);
  });

  it("renders the FAQ questions", () => {
    renderHomePage();
    for (const item of homeContent.faq) {
      expect(screen.getByRole("button", { name: item.question })).toBeInTheDocument();
    }
  });

  it(
    "has no detectable accessibility violations",
    async () => {
      const { container } = renderHomePage();
      const violations = await checkA11y(container);
      expect(violations).toHaveLength(0);
    },
    // A full composed page (8 sections) gives axe a lot more DOM to scan
    // than any single-component test -- the default 5s test timeout can
    // be too tight under load.
    15000,
  );
});
