import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Link, Routes, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { RouteFocusManager } from "./RouteFocusManager";
import { MAIN_CONTENT_ID } from "@/components/layout/PublicLayout";

function PageA() {
  return (
    <main id={MAIN_CONTENT_ID} tabIndex={-1}>
      <h1>Page A</h1>
      <Link to="/b">Go to B</Link>
      <a id="jump-target" href="#jump-target">
        anchor target
      </a>
    </main>
  );
}

function PageB() {
  return (
    <main id={MAIN_CONTENT_ID} tabIndex={-1}>
      <h1>Page B</h1>
    </main>
  );
}

function Harness({ initialEntries }: { initialEntries: string[] }) {
  return (
    <MemoryRouter initialEntries={initialEntries}>
      <RouteFocusManager />
      <Routes>
        <Route path="/" element={<PageA />} />
        <Route path="/b" element={<PageB />} />
      </Routes>
    </MemoryRouter>
  );
}

describe("RouteFocusManager", () => {
  beforeEach(() => {
    vi.spyOn(window, "scrollTo").mockImplementation(() => {});
  });

  it("does not steal focus or scroll on the initial render", () => {
    render(<Harness initialEntries={["/"]} />);
    expect(window.scrollTo).not.toHaveBeenCalled();
    expect(screen.getByRole("heading", { level: 1 })).not.toHaveFocus();
  });

  it("scrolls to top and focuses the new page's h1 after navigating", async () => {
    const user = userEvent.setup();
    render(<Harness initialEntries={["/"]} />);

    await user.click(screen.getByRole("link", { name: "Go to B" }));

    expect(await screen.findByRole("heading", { level: 1, name: "Page B" })).toHaveFocus();
    expect(window.scrollTo).toHaveBeenCalledWith(0, 0);
  });

  it("makes the heading focusable (tabindex=-1) so it can actually receive focus", async () => {
    const user = userEvent.setup();
    render(<Harness initialEntries={["/"]} />);
    await user.click(screen.getByRole("link", { name: "Go to B" }));
    const heading = await screen.findByRole("heading", { level: 1, name: "Page B" });
    expect(heading).toHaveAttribute("tabindex", "-1");
  });
});
