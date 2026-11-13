import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { homeContent } from "@/content/home";
import { AppDownloadSection } from "./AppDownloadSection";

describe("AppDownloadSection", () => {
  it("renders the heading and both store badges", () => {
    render(<AppDownloadSection />);
    expect(screen.getByText(homeContent.app.heading)).toBeInTheDocument();
    expect(screen.getByText("Google Play")).toBeInTheDocument();
    expect(screen.getByText("App Store")).toBeInTheDocument();
  });

  it("labels the section via its heading", () => {
    render(<AppDownloadSection />);
    const heading = screen.getByRole("heading", { level: 2 });
    const section = heading.closest("section");
    expect(section).toHaveAttribute("aria-labelledby", heading.id);
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<AppDownloadSection />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
