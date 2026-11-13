import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { SkipLink } from "./SkipLink";

function renderWithTarget() {
  return render(
    <>
      <SkipLink targetId="main-content" />
      <button>Decoy button before main content</button>
      <main id="main-content" tabIndex={-1}>
        Main content
      </main>
    </>,
  );
}

describe("SkipLink", () => {
  it("is the first focusable element when tabbing from the top of the page", async () => {
    const user = userEvent.setup();
    renderWithTarget();
    await user.tab();
    expect(screen.getByRole("link", { name: "Skip to main content" })).toHaveFocus();
  });

  it("links to the given target id", () => {
    renderWithTarget();
    expect(screen.getByRole("link", { name: "Skip to main content" })).toHaveAttribute(
      "href",
      "#main-content",
    );
  });

  it("is visually hidden until focused", () => {
    renderWithTarget();
    expect(screen.getByRole("link", { name: "Skip to main content" })).toHaveClass("sr-only");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = renderWithTarget();
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
