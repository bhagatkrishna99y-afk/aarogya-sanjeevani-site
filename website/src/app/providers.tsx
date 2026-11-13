import type { ReactNode } from "react";
import { BrowserRouter } from "react-router-dom";

/**
 * Every context provider the whole app needs, composed in one place.
 * `AuthProvider` deliberately does NOT live here -- no public page uses
 * auth state, so it's scoped to the `/admin*` route subtree instead (see
 * `AdminAuthProvider` in router.tsx) and lazy-loaded with it, keeping
 * `@supabase/supabase-js` out of the bundle for visitors who never go
 * near /admin.
 */
export function AppProviders({ children }: { children: ReactNode }) {
  return <BrowserRouter>{children}</BrowserRouter>;
}
