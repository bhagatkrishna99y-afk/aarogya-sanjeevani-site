import { useRef } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { checkA11y } from "@/test/axe";
import { MobileMenu } from "./MobileMenu";

const items = [
  { label: "Home", to: "/" },
  { label: "About", to: "/about" },
];

/** A real trigger button + the menu, like Header actually composes them -- needed so `returnFocusRef` has something real to give focus back to. */
function Harness({ open, onClose }: { open: boolean; onClose: () => void }) {
  const triggerRef = useRef<HTMLButtonElement>(null);
  return (
    <MemoryRouter>
      <button ref={triggerRef}>Open menu</button>
      <MobileMenu
        open={open}
        onClose={onClose}
        items={items}
        appHref="https://play.google.com"
        returnFocusRef={triggerRef}
      />
    </MemoryRouter>
  );
}

describe("MobileMenu", () => {
  it("is a labelled dialog that is not interactive while closed", () => {
    render(<Harness open={false} onClose={() => {}} />);
    const dialog = screen.getByRole("dialog", { name: "Menu", hidden: true });
    expect(dialog).toHaveAttribute("inert");
  });

  it("moves focus to its close button when opened", () => {
    render(<Harness open onClose={() => {}} />);
    expect(screen.getByRole("button", { name: "Close menu" })).toHaveFocus();
  });

  it("traps Tab within the panel, wrapping from the last focusable element back to the first", async () => {
    const user = userEvent.setup();
    render(<Harness open onClose={() => {}} />);

    const closeButton = screen.getByRole("button", { name: "Close menu" });
    const getTheApp = screen.getByRole("link", { name: "Get the App" });

    // Close button is first; keep tabbing until we reach the last item (Get the App), then one more Tab should wrap to the close button again.
    getTheApp.focus();
    await user.tab();
    expect(closeButton).toHaveFocus();
  });

  it("wraps Shift+Tab from the first focusable element to the last", async () => {
    const user = userEvent.setup();
    render(<Harness open onClose={() => {}} />);

    const closeButton = screen.getByRole("button", { name: "Close menu" });
    const getTheApp = screen.getByRole("link", { name: "Get the App" });

    expect(closeButton).toHaveFocus();
    await user.tab({ shift: true });
    expect(getTheApp).toHaveFocus();
  });

  it("calls onClose when Escape is pressed", async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(<Harness open onClose={onClose} />);
    await user.keyboard("{Escape}");
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("calls onClose when a nav link is clicked", async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(<Harness open onClose={onClose} />);
    await user.click(screen.getByRole("link", { name: "About" }));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("returns focus to the trigger when it closes", () => {
    const { rerender } = render(<Harness open onClose={() => {}} />);
    expect(screen.getByRole("button", { name: "Close menu" })).toHaveFocus();

    rerender(<Harness open={false} onClose={() => {}} />);
    expect(screen.getByRole("button", { name: "Open menu" })).toHaveFocus();
  });

  it("has no detectable accessibility violations while open", async () => {
    const { container } = render(<Harness open onClose={() => {}} />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
