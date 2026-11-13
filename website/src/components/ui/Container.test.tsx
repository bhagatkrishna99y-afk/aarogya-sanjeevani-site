import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Container } from "./Container";

describe("Container", () => {
  it("renders its children inside a centered max-width wrapper", () => {
    render(<Container>Content</Container>);
    expect(screen.getByText("Content")).toBeInTheDocument();
  });

  it("can render as a different element via `as`", () => {
    render(<Container as="main">Main content</Container>);
    expect(screen.getByRole("main")).toHaveTextContent("Main content");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<Container>Content</Container>);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
