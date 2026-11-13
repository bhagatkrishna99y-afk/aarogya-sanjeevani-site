import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Label } from "./Label";

describe("Label", () => {
  it("renders its text and associates with a control via htmlFor", () => {
    render(
      <>
        <Label htmlFor="name">Full name</Label>
        <input id="name" />
      </>,
    );
    expect(screen.getByLabelText("Full name")).toBeInTheDocument();
  });

  it("marks the field required visually and for assistive tech", () => {
    render(<Label required>Email</Label>);
    expect(screen.getByText("(required)")).toBeInTheDocument();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <>
        <Label htmlFor="name" required>
          Full name
        </Label>
        <input id="name" />
      </>,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
