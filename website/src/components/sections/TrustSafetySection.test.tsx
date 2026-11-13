import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { homeContent } from "@/content/home";
import { TrustSafetySection } from "./TrustSafetySection";

describe("TrustSafetySection", () => {
  it("renders every trust point", () => {
    render(<TrustSafetySection />);
    for (const point of homeContent.trust.points) {
      expect(screen.getByText(point.title)).toBeInTheDocument();
    }
  });

  it("lazy-loads the trust illustration with meaningful alt text", () => {
    render(<TrustSafetySection />);
    const image = screen.getByRole("img");
    expect(image.getAttribute("alt")).toBeTruthy();
    expect(image).toHaveAttribute("loading", "lazy");
  });

  it("labels the section via its heading", () => {
    render(<TrustSafetySection />);
    const heading = screen.getByRole("heading", { level: 2 });
    const section = heading.closest("section");
    expect(section).toHaveAttribute("aria-labelledby", heading.id);
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<TrustSafetySection />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
