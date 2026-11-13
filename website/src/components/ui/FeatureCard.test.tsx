import { render, screen } from "@testing-library/react";
import { ShieldCheck } from "lucide-react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { FeatureCard } from "./FeatureCard";

describe("FeatureCard", () => {
  it("renders the title and description, with the icon hidden from assistive tech", () => {
    render(
      <FeatureCard
        icon={ShieldCheck}
        title="Verified doctors"
        description="Every doctor is reviewed."
      />,
    );
    expect(screen.getByRole("heading", { name: "Verified doctors" })).toBeInTheDocument();
    expect(screen.getByText("Every doctor is reviewed.")).toBeInTheDocument();
    expect(document.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <FeatureCard
        icon={ShieldCheck}
        title="Verified doctors"
        description="Every doctor is reviewed."
      />,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
