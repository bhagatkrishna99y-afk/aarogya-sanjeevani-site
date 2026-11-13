import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { homeContent } from "@/content/home";
import { siteContent } from "@/content/site";
import { FinalCtaSection } from "./FinalCtaSection";

function renderFinalCta() {
  return render(
    <MemoryRouter>
      <FinalCtaSection />
    </MemoryRouter>,
  );
}

describe("FinalCtaSection", () => {
  it("renders the heading, call to action and contact details", () => {
    renderFinalCta();
    expect(screen.getByRole("heading", { level: 2 })).toHaveTextContent(
      homeContent.finalCta.heading,
    );
    expect(screen.getByRole("link", { name: homeContent.finalCta.cta.label })).toBeInTheDocument();
    expect(screen.getByText(siteContent.contact.email)).toBeInTheDocument();
    expect(screen.getByText(siteContent.contact.phone)).toBeInTheDocument();
  });

  it("labels the section via its heading", () => {
    renderFinalCta();
    const heading = screen.getByRole("heading", { level: 2 });
    const section = heading.closest("section");
    expect(section).toHaveAttribute("aria-labelledby", heading.id);
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = renderFinalCta();
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
