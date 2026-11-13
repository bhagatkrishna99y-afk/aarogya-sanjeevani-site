import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { homeContent } from "@/content/home";
import { HowItWorksSection } from "./HowItWorksSection";

describe("HowItWorksSection", () => {
  it("renders every patient step in order", () => {
    render(<HowItWorksSection />);
    homeContent.howItWorks.patientSteps.forEach((step, index) => {
      expect(screen.getByText(`Step ${index + 1}`)).toBeInTheDocument();
      expect(screen.getByText(step.title)).toBeInTheDocument();
    });
  });

  it("renders the doctor strip with its own steps", () => {
    render(<HowItWorksSection />);
    expect(screen.getByText(homeContent.howItWorks.doctorStrip.heading)).toBeInTheDocument();
    homeContent.howItWorks.doctorStrip.steps.forEach((step) => {
      expect(screen.getByText(step.title)).toBeInTheDocument();
    });
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = render(<HowItWorksSection />);
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  });
});
