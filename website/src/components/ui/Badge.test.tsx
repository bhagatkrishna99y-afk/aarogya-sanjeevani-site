import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Badge, type BadgeVariant } from "./Badge";

const VARIANTS: BadgeVariant[] = [
  "neutral",
  "brand",
  "accent",
  "success",
  "warning",
  "error",
  "info",
];

describe("Badge", () => {
  it("renders its text content", () => {
    render(<Badge variant="success">Approved</Badge>);
    expect(screen.getByText("Approved")).toBeInTheDocument();
  });

  it("has no detectable accessibility violations in any variant", async () => {
    const { container } = render(
      <>
        {VARIANTS.map((v) => (
          <Badge key={v} variant={v}>
            {v}
          </Badge>
        ))}
      </>,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
