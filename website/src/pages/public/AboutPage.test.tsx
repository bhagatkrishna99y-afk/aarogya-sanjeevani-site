import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { aboutContent } from "@/content/about";
import { AboutPage } from "./AboutPage";

function renderAboutPage() {
  return render(
    <MemoryRouter>
      <AboutPage />
    </MemoryRouter>,
  );
}

describe("AboutPage", () => {
  it("renders exactly one h1, the intro heading", () => {
    renderAboutPage();
    const headings = screen.getAllByRole("heading", { level: 1 });
    expect(headings).toHaveLength(1);
    expect(headings[0]).toHaveTextContent(aboutContent.intro.heading);
  });

  it("renders mission and vision as two separate blocks", () => {
    renderAboutPage();
    expect(
      screen.getByRole("heading", { name: aboutContent.missionVision.mission.heading }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: aboutContent.missionVision.vision.heading }),
    ).toBeInTheDocument();
  });

  it("renders every value card", () => {
    renderAboutPage();
    for (const item of aboutContent.values.items) {
      expect(screen.getByText(item.title)).toBeInTheDocument();
    }
  });

  it("renders every platform safety point", () => {
    renderAboutPage();
    for (const point of aboutContent.safety.points) {
      expect(screen.getByText(point.title)).toBeInTheDocument();
    }
  });

  it("renders the closing call to action", () => {
    renderAboutPage();
    expect(
      screen.getByRole("link", { name: aboutContent.closingCta.cta.label }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: aboutContent.closingCta.secondaryCta.label }),
    ).toHaveAttribute("href", aboutContent.closingCta.secondaryCta.to);
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = renderAboutPage();
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  }, 15000);
});
