import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Label } from "./Label";
import { Input } from "./Input";

describe("Input", () => {
  it("accepts typed input", async () => {
    const user = userEvent.setup();
    render(
      <>
        <Label htmlFor="email">Email</Label>
        <Input id="email" />
      </>,
    );
    const input = screen.getByLabelText("Email");
    await user.type(input, "a@example.com");
    expect(input).toHaveValue("a@example.com");
  });

  it("marks itself invalid via aria-invalid when the invalid prop is set", () => {
    render(<Input invalid aria-label="Email" />);
    expect(screen.getByLabelText("Email")).toHaveAttribute("aria-invalid", "true");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <>
        <Label htmlFor="email">Email</Label>
        <Input id="email" />
      </>,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
