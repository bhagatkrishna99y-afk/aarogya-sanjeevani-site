import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { FieldError } from "./FieldError";

describe("FieldError", () => {
  it("renders nothing when there is no error", () => {
    const { container } = render(<FieldError>{null}</FieldError>);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders the error as an alert when given one", () => {
    render(<FieldError>Please enter a value.</FieldError>);
    expect(screen.getByRole("alert")).toHaveTextContent("Please enter a value.");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<FieldError id="err">Please enter a value.</FieldError>);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
