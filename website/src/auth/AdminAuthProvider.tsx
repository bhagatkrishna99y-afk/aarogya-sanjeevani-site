import { Outlet } from "react-router-dom";
import { AuthProvider } from "./AuthContext";

/**
 * Thin wrapper so `AuthProvider` (and the `@supabase/supabase-js` import
 * chain it pulls in) can be lazy-loaded as a route element, scoped to only
 * the `/admin*` subtree in router.tsx. No public page touches auth at all
 * (confirmed: nothing under src/pages/public, src/components/layout, or
 * src/components/sections imports `useAuth`), so a visitor who never
 * visits /admin never downloads Supabase's client at all.
 */
export function AdminAuthProvider() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  );
}
