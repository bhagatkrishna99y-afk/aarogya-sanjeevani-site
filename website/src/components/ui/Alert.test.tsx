import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { checkA11y } from "@/test/axe";
import { Alert, type AlertVariant } from "./Alert";

const VARIANTS: AlertVariant[] = ["success", "info", "warning", "error"];

describe("Alert", () => {
  it("uses role=alert for warning/error and role=status for success/info", () => {
    render(<Alert variant="error">Something went wrong.</Alert>);
    expect(screen.getByRole("alert")).toHaveTextContent("Something went wrong.");

    render(<Alert variant="success">All good.</Alert>);
    expect(screen.getByText("All good.").closest('[role="status"]')).toBeInTheDocument();
  });

  it("calls onDismiss when the dismiss button is clicked", async () => {
    const user = userEvent.setup();
    const onDismiss = vi.fn();
    render(
      <Alert variant="info" onDismiss={onDismiss}>
        Dismissible
      </Alert>,
    );
    await user.click(screen.getByRole("button", { name: "Dismiss" }));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it("has no detectable accessibility violations in any variant", async () => {
    const { container } = render(
      <>
        {VARIANTS.map((v) => (
          <Alert key={v} variant={v} title={v}>
            Example {v} message.
          </Alert>
        ))}
      </>,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
