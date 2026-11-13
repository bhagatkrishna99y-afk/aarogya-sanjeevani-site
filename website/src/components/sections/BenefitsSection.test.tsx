import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { homeContent } from "@/content/home";
import { BenefitsSection } from "./BenefitsSection";

describe("BenefitsSection", () => {
  it("renders a card for every benefit", () => {
    render(<BenefitsSection />);
    for (const benefit of homeContent.benefits) {
      expect(screen.getByText(benefit.title)).toBeInTheDocument();
      expect(screen.getByText(benefit.description)).toBeInTheDocument();
    }
  });

  it("labels the section via its heading", () => {
    render(<BenefitsSection />);
    const heading = screen.getByRole("heading", { level: 2 });
    const section = heading.closest("section");
    expect(section).toHaveAttribute("aria-labelledby", heading.id);
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<BenefitsSection />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
