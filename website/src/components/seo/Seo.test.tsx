import { render, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { Seo } from "./Seo";

function renderAt(path: string, props: React.ComponentProps<typeof Seo>) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Seo {...props} />
    </MemoryRouter>,
  );
}

describe("Seo", () => {
  it("sets document.title suffixed with the site name", async () => {
    renderAt("/services", { title: "Services" });
    await waitFor(() => expect(document.title).toBe("Services · Aarogya Sanjeevani"));
  });

  it("sets the meta description, falling back to the site default when none is given", async () => {
    renderAt("/", { title: "Home", description: "Custom description." });
    await waitFor(() => {
      expect(document.querySelector('meta[name="description"]')).toHaveAttribute(
        "content",
        "Custom description.",
      );
    });
  });

  it("builds the canonical link from the current route", async () => {
    renderAt("/about", { title: "About" });
    await waitFor(() => {
      expect(document.querySelector('link[rel="canonical"]')).toHaveAttribute(
        "href",
        expect.stringContaining("/about"),
      );
    });
  });

  it("sets matching Open Graph and Twitter card tags", async () => {
    renderAt("/contact", { title: "Contact", description: "Reach us." });
    await waitFor(() => {
      expect(document.querySelector('meta[property="og:title"]')).toHaveAttribute(
        "content",
        "Contact · Aarogya Sanjeevani",
      );
      expect(document.querySelector('meta[property="og:description"]')).toHaveAttribute(
        "content",
        "Reach us.",
      );
      expect(document.querySelector('meta[name="twitter:card"]')).toHaveAttribute(
        "content",
        "summary_large_image",
      );
    });
  });

  it("adds a noindex robots meta tag only when asked", async () => {
    renderAt("/404", { title: "Not found", noIndex: true });
    await waitFor(() => {
      expect(document.querySelector('meta[name="robots"]')).toHaveAttribute(
        "content",
        "noindex, nofollow",
      );
    });
  });

  it("omits the robots meta tag by default", () => {
    renderAt("/", { title: "Home" });
    expect(document.querySelector('meta[name="robots"]')).not.toBeInTheDocument();
  });
});
