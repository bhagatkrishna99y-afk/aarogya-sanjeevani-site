import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { homeContent } from "@/content/home";
import { HeroSection } from "./HeroSection";

function renderHero() {
  return render(
    <MemoryRouter>
      <HeroSection />
    </MemoryRouter>,
  );
}

describe("HeroSection", () => {
  it("renders the headline as the page's h1", () => {
    renderHero();
    expect(
      screen.getByRole("heading", { level: 1, name: homeContent.hero.headline }),
    ).toBeInTheDocument();
  });

  it("renders both calls to action and the trust line", () => {
    renderHero();
    expect(
      screen.getByRole("link", { name: homeContent.hero.primaryCta.label }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: homeContent.hero.secondaryCta.label }),
    ).toBeInTheDocument();
    expect(screen.getByText(homeContent.hero.trustLine)).toBeInTheDocument();
  });

  it("renders the hero illustration with meaningful alt text", () => {
    renderHero();
    const image = screen.getByRole("img");
    expect(image.getAttribute("alt")).toBeTruthy();
  });

  it("labels the section via the h1", () => {
    renderHero();
    const section = screen.getByRole("heading", { level: 1 }).closest("section");
    expect(section).toHaveAttribute("aria-labelledby", "hero-heading");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = renderHero();
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
