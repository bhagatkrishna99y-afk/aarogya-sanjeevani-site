import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { Section } from "./Section";

describe("Section", () => {
  it("renders the eyebrow, heading and description", () => {
    render(<Section eyebrow="Eyebrow" heading="Heading" description="Description" />);
    expect(screen.getByText("Eyebrow")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Heading" })).toBeInTheDocument();
    expect(screen.getByText("Description")).toBeInTheDocument();
  });

  it("renders children without a header block when none is given", () => {
    render(
      <Section>
        <p>Just content</p>
      </Section>,
    );
    expect(screen.getByText("Just content")).toBeInTheDocument();
  });

  it("supports overriding the heading level", () => {
    render(<Section heading="Title" headingLevel="h3" />);
    expect(screen.getByRole("heading", { level: 3, name: "Title" })).toBeInTheDocument();
  });

  it("labels the section landmark via aria-labelledby pointing at its own heading", () => {
    render(<Section heading="Key benefits" />);
    const region = screen.getByRole("region", { name: "Key benefits" });
    const heading = screen.getByRole("heading", { name: "Key benefits" });
    expect(region).toHaveAttribute("aria-labelledby", heading.id);
  });

  it("supports a caller-provided heading id, e.g. for a stable in-page anchor", () => {
    render(<Section heading="FAQ" headingId="faq-heading" />);
    expect(screen.getByRole("heading", { name: "FAQ" })).toHaveAttribute("id", "faq-heading");
  });

  it("has no aria-labelledby when there is no heading to label it with", () => {
    render(
      <Section>
        <p>Just content</p>
      </Section>,
    );
    expect(document.querySelector("section")).not.toHaveAttribute("aria-labelledby");
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(
      <Section eyebrow="Eyebrow" heading="Heading" headingLevel="h2" description="Description">
        <p>Content</p>
      </Section>,
    );
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
