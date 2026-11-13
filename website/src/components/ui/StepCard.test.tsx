import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { StepCard } from "./StepCard";

describe("StepCard", () => {
  it("renders the step number, title and description", () => {
    render(<StepCard step={2} title="Pick a slot" description="Choose a time that works." />);
    expect(screen.getByText("Step 2")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Pick a slot" })).toBeInTheDocument();
    expect(screen.getByText("Choose a time that works.")).toBeInTheDocument();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <StepCard step={1} title="Discover" description="Browse services." />,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
