import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Skeleton } from "./Skeleton";

describe("Skeleton", () => {
  it("is hidden from the accessibility tree (purely decorative)", () => {
    const { container } = render(<Skeleton className="h-4 w-full" />);
    expect(container.firstChild).toHaveAttribute("aria-hidden", "true");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<Skeleton className="h-4 w-full" />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
