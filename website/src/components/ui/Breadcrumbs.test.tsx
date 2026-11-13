import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Breadcrumbs } from "./Breadcrumbs";

const items = [
  { label: "Home", to: "/" },
  { label: "Services", to: "/services" },
  { label: "Cardiology" },
];

describe("Breadcrumbs", () => {
  it("renders every item as a link except the last, current one", () => {
    render(
      <MemoryRouter>
        <Breadcrumbs items={items} />
      </MemoryRouter>,
    );
    expect(screen.getByRole("link", { name: "Home" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "Services" })).toHaveAttribute("href", "/services");
    expect(screen.queryByRole("link", { name: "Cardiology" })).not.toBeInTheDocument();
  });

  it("marks the last item as the current page", () => {
    render(
      <MemoryRouter>
        <Breadcrumbs items={items} />
      </MemoryRouter>,
    );
    expect(screen.getByText("Cardiology")).toHaveAttribute("aria-current", "page");
  });

  it("has an accessible nav label", () => {
    render(
      <MemoryRouter>
        <Breadcrumbs items={items} />
      </MemoryRouter>,
    );
    expect(screen.getByRole("navigation", { name: "Breadcrumb" })).toBeInTheDocument();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <MemoryRouter>
        <Breadcrumbs items={items} />
      </MemoryRouter>,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
