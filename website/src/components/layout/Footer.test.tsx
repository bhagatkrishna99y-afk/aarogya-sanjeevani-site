import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Footer } from "./Footer";

function renderFooter() {
  return render(
    <MemoryRouter>
      <Footer />
    </MemoryRouter>,
  );
}

describe("Footer", () => {
  it("renders the quick links and legal links", () => {
    renderFooter();
    expect(screen.getByRole("navigation", { name: "Quick links" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Privacy Policy" })).toHaveAttribute(
      "href",
      "/privacy-policy",
    );
    expect(screen.getByRole("link", { name: "Terms" })).toHaveAttribute("href", "/terms");
  });

  it("renders contact details", () => {
    renderFooter();
    expect(screen.getByRole("link", { name: /@/ })).toHaveAttribute(
      "href",
      expect.stringContaining("mailto:"),
    );
  });

  it("renders app store badges for both apps", () => {
    renderFooter();
    expect(screen.getByRole("link", { name: /Patient app/ })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Doctor app/ })).toBeInTheDocument();
  });

  it("states the non-emergency disclaimer, naming India's emergency number", () => {
    renderFooter();
    expect(screen.getByRole("note")).toHaveTextContent(/not for medical emergencies/i);
    expect(screen.getByRole("note")).toHaveTextContent("112");
  });

  it("computes the copyright year rather than hardcoding it", () => {
    renderFooter();
    const year = new Date().getFullYear().toString();
    expect(screen.getByText(new RegExp(`© ${year}`))).toBeInTheDocument();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = renderFooter();
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
