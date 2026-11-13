import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { FormField } from "./FormField";
import { Input } from "./Input";

describe("FormField", () => {
  it("connects the label to the control via a shared id", () => {
    render(
      <FormField label="Email" required>
        <Input type="email" />
      </FormField>,
    );
    expect(screen.getByLabelText(/Email/)).toBeInTheDocument();
  });

  it("wires the error to the control via aria-describedby and marks it invalid", () => {
    render(
      <FormField label="Email" error="Please enter a valid email.">
        <Input type="email" />
      </FormField>,
    );
    const input = screen.getByLabelText("Email");
    const errorEl = screen.getByRole("alert");
    expect(errorEl).toHaveTextContent("Please enter a valid email.");
    expect(input).toHaveAttribute("aria-describedby", errorEl.id);
    expect(input).toHaveAttribute("aria-invalid", "true");
  });

  it("shows helper text when there is no error", () => {
    render(
      <FormField label="Email" helperText="We'll never share this.">
        <Input type="email" />
      </FormField>,
    );
    expect(screen.getByText("We'll never share this.")).toBeInTheDocument();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <FormField label="Email" required error="Please enter a valid email.">
        <Input type="email" />
      </FormField>,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
