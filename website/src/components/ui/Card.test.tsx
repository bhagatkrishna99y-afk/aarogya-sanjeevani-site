import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Card } from "./Card";

describe("Card", () => {
  it("renders its children", () => {
    render(<Card>Card content</Card>);
    expect(screen.getByText("Card content")).toBeInTheDocument();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<Card>Card content</Card>);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
