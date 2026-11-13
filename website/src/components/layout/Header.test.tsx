import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Header } from "./Header";

function renderHeader() {
  return render(
    <MemoryRouter>
      <Header />
    </MemoryRouter>,
  );
}

describe("Header", () => {
  it("renders the wordmark linking home, and the primary nav in the right order", () => {
    renderHeader();
    expect(screen.getByRole("link", { name: /Aarogya Sanjeevani/ })).toHaveAttribute("href", "/");

    const nav = screen.getByRole("navigation", { name: "Primary" });
    const labels = Array.from(nav.querySelectorAll("a")).map((a) => a.textContent);
    expect(labels).toEqual(["Home", "About", "Services", "Contact"]);
  });

  it("renders a prominent Get the App link", () => {
    renderHeader();
    expect(screen.getAllByRole("link", { name: "Get the App" })[0]).toHaveAttribute(
      "target",
      "_blank",
    );
  });

  it("opens the mobile menu from the hamburger button, with aria-expanded reflecting state", async () => {
    const user = userEvent.setup();
    renderHeader();

    const hamburger = screen.getByRole("button", { name: "Open menu" });
    expect(hamburger).toHaveAttribute("aria-expanded", "false");

    await user.click(hamburger);
    expect(hamburger).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByRole("dialog", { name: "Menu" })).toBeInTheDocument();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = renderHeader();
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
