import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { RequireAdmin } from "@/auth/RequireAdmin";
import { PublicLayout } from "@/components/layout/PublicLayout";
import { AdminLayout } from "@/components/layout/AdminLayout";
import { PageSkeleton } from "@/components/ui/PageSkeleton";
import { FullPageSpinner } from "@/components/FullPageSpinner";
import { RouteFocusManager } from "./RouteFocusManager";

// Every route-level page is lazy -- each only loads the JS it needs,
// when it's first visited, instead of all of them landing in one upfront
// bundle. `<Suspense fallback={<PageSkeleton />}>` below covers the brief
// gap while a given page's chunk downloads.
const HomePage = lazy(() =>
  import("@/pages/public/HomePage").then((m) => ({ default: m.HomePage })),
);
const ServicesPage = lazy(() =>
  import("@/pages/public/ServicesPage").then((m) => ({ default: m.ServicesPage })),
);
const AboutPage = lazy(() =>
  import("@/pages/public/AboutPage").then((m) => ({ default: m.AboutPage })),
);
const ContactPage = lazy(() =>
  import("@/pages/public/ContactPage").then((m) => ({ default: m.ContactPage })),
);
const PrivacyPolicyPage = lazy(() =>
  import("@/pages/public/PrivacyPolicyPage").then((m) => ({ default: m.PrivacyPolicyPage })),
);
const TermsPage = lazy(() =>
  import("@/pages/public/TermsPage").then((m) => ({ default: m.TermsPage })),
);
const NotFoundPage = lazy(() =>
  import("@/pages/public/NotFoundPage").then((m) => ({ default: m.NotFoundPage })),
);

const AdminAuthProvider = lazy(() =>
  import("@/auth/AdminAuthProvider").then((m) => ({ default: m.AdminAuthProvider })),
);
const LoginPage = lazy(() =>
  import("@/pages/admin/LoginPage").then((m) => ({ default: m.LoginPage })),
);
const DashboardPage = lazy(() =>
  import("@/pages/admin/DashboardPage").then((m) => ({ default: m.DashboardPage })),
);
const DoctorsPage = lazy(() =>
  import("@/pages/admin/DoctorsPage").then((m) => ({ default: m.DoctorsPage })),
);
const CatalogPage = lazy(() =>
  import("@/pages/admin/CatalogPage").then((m) => ({ default: m.CatalogPage })),
);
const RequestsPage = lazy(() =>
  import("@/pages/admin/RequestsPage").then((m) => ({ default: m.RequestsPage })),
);
const UsersPage = lazy(() =>
  import("@/pages/admin/UsersPage").then((m) => ({ default: m.UsersPage })),
);

// Dynamically imported (not a plain top-level import) and only ever
// referenced inside `import.meta.env.DEV`, which Vite inlines as a
// literal `false` in a production build -- the whole branch below is
// dead code there, so `vite build` doesn't ship StyleguidePage (or any
// of the component demos it imports) to real users.
const StyleguidePage = lazy(() =>
  import("@/styleguide/StyleguidePage").then((m) => ({ default: m.StyleguidePage })),
);

export function AppRouter() {
  return (
    <>
      <RouteFocusManager />
      <Suspense fallback={<PageSkeleton />}>
        <Routes>
          <Route element={<PublicLayout />}>
            <Route index element={<HomePage />} />
            <Route path="services" element={<ServicesPage />} />
            <Route path="about" element={<AboutPage />} />
            <Route path="contact" element={<ContactPage />} />
            <Route path="privacy-policy" element={<PrivacyPolicyPage />} />
            <Route path="terms" element={<TermsPage />} />
            {/* Nested here (not a top-level sibling) so an unknown route
                still renders with the normal header/footer, not as a
                bare orphan page -- a "friendly" 404 means one that still
                lets you navigate away, not just an error with no chrome.
                As the catch-all for the whole app, this still matches
                stray /admin/* typos too (nothing more specific claims
                them), same as before this route moved here. */}
            <Route path="*" element={<NotFoundPage />} />
          </Route>

          {/* AuthContext (and the @supabase/supabase-js it imports) is only
              reachable through this lazy AdminAuthProvider -- a visitor who
              never navigates under /admin never downloads it at all. */}
          <Route element={<AdminAuthProvider />}>
            <Route path="admin/login" element={<LoginPage />} />

            <Route path="admin" element={<RequireAdmin />}>
              <Route element={<AdminLayout />}>
                <Route index element={<DashboardPage />} />
                <Route path="doctors" element={<DoctorsPage />} />
                <Route path="catalog" element={<CatalogPage />} />
                <Route path="requests" element={<RequestsPage />} />
                <Route path="users" element={<UsersPage />} />
              </Route>
            </Route>
          </Route>

          {import.meta.env.DEV && (
            <Route
              path="styleguide"
              element={
                <Suspense fallback={<FullPageSpinner />}>
                  <StyleguidePage />
                </Suspense>
              }
            />
          )}
        </Routes>
      </Suspense>
    </>
  );
}
