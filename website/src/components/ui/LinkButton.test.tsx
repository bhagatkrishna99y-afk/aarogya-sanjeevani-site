import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { LinkButton } from "./LinkButton";

describe("LinkButton", () => {
  it("renders an internal link to the given route", () => {
    render(
      <MemoryRouter>
        <LinkButton to="/services">Browse services</LinkButton>
      </MemoryRouter>,
    );
    const link = screen.getByRole("link", { name: "Browse services" });
    expect(link).toHaveAttribute("href", "/services");
  });

  it("renders an external link with target and rel set for safety", () => {
    render(<LinkButton href="https://example.com">Visit</LinkButton>);
    const link = screen.getByRole("link", { name: "Visit" });
    expect(link).toHaveAttribute("href", "https://example.com");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <MemoryRouter>
        <LinkButton to="/services" variant="primary">
          Internal
        </LinkButton>
        <LinkButton href="https://example.com" variant="secondary">
          External
        </LinkButton>
      </MemoryRouter>,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
