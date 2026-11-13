import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StructuredData } from "./StructuredData";

function readJsonLdScripts(container: HTMLElement) {
  return Array.from(container.querySelectorAll('script[type="application/ld+json"]')).map((el) =>
    JSON.parse(el.innerHTML),
  );
}

describe("StructuredData", () => {
  it("renders exactly one Organization and one WebSite JSON-LD script", () => {
    const { container } = render(<StructuredData />);
    const scripts = readJsonLdScripts(container);
    const types = scripts.map((s) => s["@type"]);
    expect(types).toContain("Organization");
    expect(types).toContain("WebSite");
    expect(scripts).toHaveLength(2);
  });

  it("includes the site name and url in both schemas", () => {
    const { container } = render(<StructuredData />);
    const [organization, website] = readJsonLdScripts(container);
    expect(organization.name).toBe("Aarogya Sanjeevani");
    expect(organization.url).toBeTruthy();
    expect(website.name).toBe("Aarogya Sanjeevani");
    expect(website.url).toBe(organization.url);
  });

  it("gives the Organization a contact point with an email and phone", () => {
    const { container } = render(<StructuredData />);
    const [organization] = readJsonLdScripts(container);
    expect(organization.contactPoint["@type"]).toBe("ContactPoint");
    expect(organization.contactPoint.email).toBeTruthy();
    expect(organization.contactPoint.telephone).toBeTruthy();
  });
});
