import { axe } from "vitest-axe";

/**
 * Runs axe-core restricted to WCAG 2.2 AA's full rule set (2.0 + the 2.1
 * and 2.2 criteria added since -- WCAG 2.2 AA is this project's explicit
 * target, so 2.0-only would silently skip real 2.1/2.2 criteria like
 * "Label in Name" (2.5.3), which is exactly how a real mismatch in
 * StoreBadge's aria-label went undetected here before a full-page
 * Playwright+axe scan caught it). Component tests render an isolated
 * fragment, not a full page -- axe's default ruleset also includes
 * page-level best-practice checks (exactly one <h1>, landmark regions,
 * ...) that are meaningless (and would be false positives) on a fragment
 * that is never meant to be the whole document, which is why this is
 * still scoped by tag rather than calling `axe()` unrestricted.
 */
export async function checkA11y(container: Element) {
  const results = await axe(container, {
    runOnly: { type: "tag", values: ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "wcag22aa"] },
  });
  return results.violations;
}
