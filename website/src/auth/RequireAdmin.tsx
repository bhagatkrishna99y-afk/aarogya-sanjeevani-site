import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "./AuthContext";
import { FullPageSpinner } from "../components/FullPageSpinner";

/** Gates the /admin section: must be signed in AND have role === 'admin'. */
export function RequireAdmin() {
  const { session, profile, loading } = useAuth();

  if (loading) return <FullPageSpinner />;
  if (!session) return <Navigate to="/admin/login" replace />;
  if (profile?.role !== "admin") {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-2 px-4 text-center">
        <h1 className="text-xl font-semibold text-slate-900">Access restricted</h1>
        <p className="max-w-sm text-sm text-slate-600">
          This account does not have admin access. If you believe this is a mistake, contact
          another administrator.
        </p>
      </div>
    );
  }

  return <Outlet />;
}
