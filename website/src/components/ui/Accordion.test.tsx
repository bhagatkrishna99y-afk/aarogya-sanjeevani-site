import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Accordion, AccordionItem } from "./Accordion";

function renderAccordion(props: Partial<React.ComponentProps<typeof Accordion>> = {}) {
  return render(
    <Accordion aria-label="FAQ" {...props}>
      <AccordionItem id="a" question="Question A">
        Answer A
      </AccordionItem>
      <AccordionItem id="b" question="Question B">
        Answer B
      </AccordionItem>
      <AccordionItem id="c" question="Question C">
        Answer C
      </AccordionItem>
    </Accordion>,
  );
}

describe("Accordion", () => {
  it("starts with every item collapsed and aria-expanded=false", () => {
    renderAccordion();
    for (const name of ["Question A", "Question B", "Question C"]) {
      expect(screen.getByRole("button", { name })).toHaveAttribute("aria-expanded", "false");
    }
    expect(screen.queryByText("Answer A")).not.toBeVisible();
  });

  it("opens an item on click and shows its answer", async () => {
    const user = userEvent.setup();
    renderAccordion();
    await user.click(screen.getByRole("button", { name: "Question A" }));
    expect(screen.getByRole("button", { name: "Question A" })).toHaveAttribute(
      "aria-expanded",
      "true",
    );
    expect(screen.getByText("Answer A")).toBeVisible();
  });

  it("closes the previously open item when a different one opens (single-open by default)", async () => {
    const user = userEvent.setup();
    renderAccordion();
    await user.click(screen.getByRole("button", { name: "Question A" }));
    await user.click(screen.getByRole("button", { name: "Question B" }));
    expect(screen.getByRole("button", { name: "Question A" })).toHaveAttribute(
      "aria-expanded",
      "false",
    );
    expect(screen.getByRole("button", { name: "Question B" })).toHaveAttribute(
      "aria-expanded",
      "true",
    );
  });

  it("allows multiple open items when allowMultiple is set", async () => {
    const user = userEvent.setup();
    renderAccordion({ allowMultiple: true });
    await user.click(screen.getByRole("button", { name: "Question A" }));
    await user.click(screen.getByRole("button", { name: "Question B" }));
    expect(screen.getByRole("button", { name: "Question A" })).toHaveAttribute(
      "aria-expanded",
      "true",
    );
    expect(screen.getByRole("button", { name: "Question B" })).toHaveAttribute(
      "aria-expanded",
      "true",
    );
  });

  it("moves focus between headers with ArrowDown/ArrowUp/Home/End", async () => {
    const user = userEvent.setup();
    renderAccordion();
    const [a, b, c] = ["Question A", "Question B", "Question C"].map((name) =>
      screen.getByRole("button", { name }),
    );
    a!.focus();
    await user.keyboard("{ArrowDown}");
    expect(b).toHaveFocus();
    await user.keyboard("{ArrowDown}");
    expect(c).toHaveFocus();
    await user.keyboard("{Home}");
    expect(a).toHaveFocus();
    await user.keyboard("{End}");
    expect(c).toHaveFocus();
    await user.keyboard("{ArrowUp}");
    expect(b).toHaveFocus();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = renderAccordion();
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
