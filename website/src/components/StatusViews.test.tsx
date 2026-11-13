import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { checkA11y } from "@/test/axe";
import { EmptyState, ErrorState, LoadingState } from "./StatusViews";

describe("LoadingState", () => {
  it("announces itself to assistive tech via role=status", () => {
    render(<LoadingState label="Loading things…" />);
    expect(screen.getByRole("status")).toHaveTextContent("Loading things…");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<LoadingState />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});

describe("ErrorState", () => {
  it("announces itself as an alert and calls onRetry when clicked", () => {
    const onRetry = vi.fn();
    render(<ErrorState message="Something broke" onRetry={onRetry} />);

    expect(screen.getByRole("alert")).toHaveTextContent("Something broke");
    fireEvent.click(screen.getByRole("button", { name: "Try again" }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it("omits the retry button when no onRetry is given", () => {
    render(<ErrorState message="Something broke" />);
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<ErrorState message="Something broke" onRetry={() => {}} />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});

describe("EmptyState", () => {
  it("renders the title and optional description", () => {
    render(<EmptyState title="Nothing here" description="Check back later." />);
    expect(screen.getByText("Nothing here")).toBeInTheDocument();
    expect(screen.getByText("Check back later.")).toBeInTheDocument();
  });
});
