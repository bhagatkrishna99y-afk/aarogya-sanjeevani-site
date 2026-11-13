import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { PageSkeleton } from "./PageSkeleton";

describe("PageSkeleton", () => {
  it("is hidden from the accessibility tree (purely decorative)", () => {
    const { container } = render(<PageSkeleton />);
    expect(container.firstChild).toHaveAttribute("aria-hidden", "true");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<PageSkeleton />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
