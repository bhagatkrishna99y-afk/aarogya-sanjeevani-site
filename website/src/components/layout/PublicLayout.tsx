import { Outlet } from "react-router-dom";
import { SkipLink } from "@/components/ui/SkipLink";
import { StructuredData } from "@/components/seo/StructuredData";
import { Header } from "./Header";
import { Footer } from "./Footer";

export const MAIN_CONTENT_ID = "main-content";

export function PublicLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      {/* Site-wide Organization/WebSite JSON-LD -- once, not per-page. */}
      <StructuredData />

      {/* Literally the first focusable element in the document --
          SkipLink must be the first child rendered here, before Header. */}
      <SkipLink targetId={MAIN_CONTENT_ID} />

      <Header />

      {/* tabIndex={-1} makes this programmatically focusable (not part of
          the normal Tab order) -- the route-change focus manager
          (src/app/RouteFocusManager.tsx) focuses it after each
          navigation so screen reader users hear the new page announced,
          and the skip link above targets the same id. */}
      <main id={MAIN_CONTENT_ID} tabIndex={-1} className="flex-1 outline-none">
        <Outlet />
      </main>

      <Footer />

      {/*
       * Cookie/consent banner: intentionally not here.
       *
       * This site sets no non-essential cookies and runs no analytics,
       * advertising, or tracking script today (see docs/design-tokens.md's
       * neighbor docs/seo.md and the Privacy Policy page for the current,
       * accurate statement of that) -- a consent banner with nothing to
       * actually ask consent for is just noise for every visitor, and
       * legally unnecessary where there's nothing non-essential being
       * set.
       *
       * If that changes -- analytics gets added, for instance -- add the
       * banner here, as a sibling after <Footer />, gating the actual
       * analytics script's initialization on the user's choice (never
       * load the tracking script first and ask after). Update the
       * Privacy Policy page's "Cookies" section in the same change.
       */}
    </div>
  );
}
