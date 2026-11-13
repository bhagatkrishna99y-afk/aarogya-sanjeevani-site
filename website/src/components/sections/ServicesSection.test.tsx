import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { homeContent } from "@/content/home";
import { ServicesSection } from "./ServicesSection";

function renderServices() {
  return render(
    <MemoryRouter>
      <ServicesSection />
    </MemoryRouter>,
  );
}

describe("ServicesSection", () => {
  it("renders every service category", () => {
    renderServices();
    for (const category of homeContent.services.categories) {
      expect(screen.getByText(category.name)).toBeInTheDocument();
    }
  });

  it("links to the full services page", () => {
    renderServices();
    const link = screen.getByRole("link", { name: homeContent.services.cta.label });
    expect(link).toHaveAttribute("href", homeContent.services.cta.to);
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = renderServices();
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
