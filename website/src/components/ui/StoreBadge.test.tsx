import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { StoreBadge } from "./StoreBadge";

describe("StoreBadge", () => {
  it("links to the given href with an accessible name combining the app and store", () => {
    render(
      <StoreBadge
        store="google-play"
        href="https://play.google.com/store/apps/details?id=x"
        appLabel="Patient app"
      />,
    );
    const link = screen.getByRole("link", { name: "GET IT ON Google Play — Patient app" });
    expect(link).toHaveAttribute("href", "https://play.google.com/store/apps/details?id=x");
  });

  it("opens in a new tab safely", () => {
    render(<StoreBadge store="app-store" href="https://apple.com" appLabel="Doctor app" />);
    const link = screen.getByRole("link");
    expect(link).toHaveAttribute("target", "_blank");
    expect(link).toHaveAttribute("rel", "noopener noreferrer");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <StoreBadge store="google-play" href="https://play.google.com" appLabel="Patient app" />,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });

  it("renders a non-interactive Coming soon state when href is omitted", () => {
    render(<StoreBadge store="google-play" appLabel="Doctor app" />);
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
    expect(screen.getByText("Coming soon")).toBeInTheDocument();
  });

  it("also renders Coming soon when href is an empty string (an unset env var)", () => {
    render(<StoreBadge store="google-play" href="" appLabel="Doctor app" />);
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
    expect(screen.getByText("Coming soon")).toBeInTheDocument();
  });

  it("has no detectable accessibility violations in the Coming soon state", async () => {
    const { container } = render(<StoreBadge store="app-store" appLabel="Doctor app" />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
