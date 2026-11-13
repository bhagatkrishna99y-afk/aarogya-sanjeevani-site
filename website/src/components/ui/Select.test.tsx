import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Label } from "./Label";
import { Select } from "./Select";

describe("Select", () => {
  it("lets the user choose an option", async () => {
    const user = userEvent.setup();
    render(
      <>
        <Label htmlFor="topic">Topic</Label>
        <Select id="topic" defaultValue="">
          <option value="" disabled>
            Choose one
          </option>
          <option value="billing">Billing</option>
          <option value="support">Support</option>
        </Select>
      </>,
    );
    const select = screen.getByLabelText("Topic");
    await user.selectOptions(select, "support");
    expect(select).toHaveValue("support");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <>
        <Label htmlFor="topic">Topic</Label>
        <Select id="topic" defaultValue="billing">
          <option value="billing">Billing</option>
          <option value="support">Support</option>
        </Select>
      </>,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
