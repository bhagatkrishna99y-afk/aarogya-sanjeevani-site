import { render, screen } from "@testing-library/react";
import { Heart } from "lucide-react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Icon } from "./Icon";

describe("Icon", () => {
  it("is hidden from the accessibility tree when decorative (no label)", () => {
    render(<Icon icon={Heart} />);
    const svg = document.querySelector("svg");
    expect(svg).toHaveAttribute("aria-hidden", "true");
  });

  it("gets an accessible name when given a label", () => {
    render(<Icon icon={Heart} label="Favorite" />);
    expect(screen.getByRole("img", { name: "Favorite" })).toBeInTheDocument();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<Icon icon={Heart} label="Favorite" />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
