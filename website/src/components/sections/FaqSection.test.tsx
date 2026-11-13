import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { homeContent } from "@/content/home";
import { FaqSection } from "./FaqSection";

describe("FaqSection", () => {
  it("renders every question as a collapsed accordion header", () => {
    render(<FaqSection />);
    for (const item of homeContent.faq) {
      const header = screen.getByRole("button", { name: item.question });
      expect(header).toHaveAttribute("aria-expanded", "false");
    }
  });

  it("expands an answer when its question is clicked", async () => {
    render(<FaqSection />);
    const [firstItem] = homeContent.faq;
    const header = screen.getByRole("button", { name: firstItem.question });
    await userEvent.click(header);
    expect(header).toHaveAttribute("aria-expanded", "true");
    expect(screen.getByText(firstItem.answer)).toBeVisible();
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<FaqSection />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
