import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Label } from "./Label";
import { Textarea } from "./Textarea";

describe("Textarea", () => {
  it("accepts typed input", async () => {
    const user = userEvent.setup();
    render(
      <>
        <Label htmlFor="message">Message</Label>
        <Textarea id="message" />
      </>,
    );
    const textarea = screen.getByLabelText("Message");
    await user.type(textarea, "Hello there");
    expect(textarea).toHaveValue("Hello there");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <>
        <Label htmlFor="message">Message</Label>
        <Textarea id="message" />
      </>,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
