import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { checkA11y } from "@/test/axe";
import { servicesContent } from "@/content/services";
import { ServicesPage } from "./ServicesPage";

function renderServicesPage() {
  return render(
    <MemoryRouter>
      <ServicesPage />
    </MemoryRouter>,
  );
}

describe("ServicesPage", () => {
  it("renders exactly one h1, the intro heading", () => {
    renderServicesPage();
    const headings = screen.getAllByRole("heading", { level: 1 });
    expect(headings).toHaveLength(1);
    expect(headings[0]).toHaveTextContent(servicesContent.intro.heading);
  });

  it("renders a section for every service category with its example services and modes", () => {
    renderServicesPage();
    for (const category of servicesContent.categories) {
      expect(screen.getByRole("heading", { name: category.name })).toBeInTheDocument();
      for (const example of category.examples) {
        expect(screen.getByText(example.name)).toBeInTheDocument();
      }
    }
    // Spot-check at least one mode label renders as a visible badge.
    expect(screen.getAllByText("Home visit").length).toBeGreaterThan(0);
  });

  it("renders the how-requesting steps using numbered StepCards", () => {
    renderServicesPage();
    servicesContent.howRequesting.steps.forEach((step, index) => {
      expect(screen.getByText(`Step ${index + 1}`)).toBeInTheDocument();
      expect(screen.getByRole("heading", { name: step.title })).toBeInTheDocument();
    });
  });

  it("renders the emergency notice", () => {
    renderServicesPage();
    expect(screen.getByRole("alert")).toHaveTextContent(servicesContent.emergencyNotice.title);
    expect(screen.getByRole("alert")).toHaveTextContent(servicesContent.emergencyNotice.body);
  });

  it("renders calls to action for both apps", () => {
    renderServicesPage();
    expect(
      screen.getByRole("link", { name: servicesContent.appCta.patient.label }),
    ).toHaveAttribute("href", servicesContent.appCta.patient.href);
    expect(screen.getByRole("link", { name: servicesContent.appCta.doctor.label })).toHaveAttribute(
      "href",
      servicesContent.appCta.doctor.href,
    );
  });

  it("has no detectable accessibility violations", async () => {
    const { container } = renderServicesPage();
    const violations = await checkA11y(container);
    expect(violations).toHaveLength(0);
  }, 15000);
});
