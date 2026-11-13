import { useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";
import { MAIN_CONTENT_ID } from "@/components/layout/PublicLayout";

/**
 * This app uses the declarative `<BrowserRouter>`/`<Routes>` API, not
 * `createBrowserRouter`, so there's no built-in `<ScrollRestoration>` --
 * that component only exists for the data-router API. This is the manual
 * equivalent, plus the focus management a `<ScrollRestoration>` wouldn't
 * give you anyway: on every route change (not the first render, which is
 * the browser's own initial load/refresh and shouldn't be hijacked),
 *
 * - a `#hash` URL scrolls to and focuses that element (so an in-page
 *   anchor link still works like one normally would), otherwise
 * - the viewport resets to the top, and
 * - focus moves to the new page's `<h1>` (falling back to the `<main>`
 *   landmark if a page is somehow missing one) so a screen reader
 *   announces the new page's heading -- an SPA route change doesn't
 *   trigger a full page (re)load, so without this, assistive tech has no
 *   cue that anything navigated at all.
 *
 * Renders nothing; mount it once, inside the Router, alongside the
 * routes it's reacting to (see src/app/router.tsx).
 */
export function RouteFocusManager() {
  const location = useLocation();
  const isFirstRender = useRef(true);

  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }

    if (location.hash) {
      const target = document.getElementById(location.hash.slice(1));
      target?.scrollIntoView();
      return;
    }

    window.scrollTo(0, 0);

    const main = document.getElementById(MAIN_CONTENT_ID);
    const heading = main?.querySelector<HTMLElement>("h1");
    const focusTarget = heading ?? main;
    if (!focusTarget) return;

    if (!focusTarget.hasAttribute("tabindex")) {
      focusTarget.setAttribute("tabindex", "-1");
    }
    focusTarget.focus();
  }, [location.pathname, location.hash]);

  return null;
}
