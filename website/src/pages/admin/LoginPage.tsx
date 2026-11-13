import { useState, type FormEvent } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import { Spinner } from "../../components/FullPageSpinner";
import { Seo } from "@/components/seo/Seo";

export function LoginPage() {
  const { session, profile, loading, signInWithPassword } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (session && !loading && profile?.role === "admin") {
    return <Navigate to="/admin" replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError(null);

    const { error: signInError } = await signInWithPassword(email, password);
    if (signInError) {
      setError(signInError);
      setSubmitting(false);
    }
    // on success, profile loads via AuthContext and the redirect above fires
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50 px-4">
      <Seo title="Ops console sign in" />
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm">
        <h1 className="text-xl font-bold text-slate-900">Ops console sign in</h1>
        <p className="mt-1 text-sm text-slate-500">Admin access only.</p>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label htmlFor="email" className="text-sm font-medium text-slate-700">
              Email
            </label>
            <input
              id="email"
              type="email"
              required
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="mt-1 block min-h-[48px] w-full rounded-md border border-slate-300 px-3 text-sm focus:border-brand-500"
            />
          </div>
          <div>
            <label htmlFor="password" className="text-sm font-medium text-slate-700">
              Password
            </label>
            <input
              id="password"
              type="password"
              required
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 block min-h-[48px] w-full rounded-md border border-slate-300 px-3 text-sm focus:border-brand-500"
            />
          </div>

          {error && (
            <p role="alert" className="text-sm text-red-700">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-md bg-brand-700 text-sm font-semibold text-white hover:bg-brand-800 disabled:opacity-60"
          >
            {submitting && <Spinner className="h-4 w-4 text-white" />}
            Sign in
          </button>
        </form>
      </div>
    </div>
  );
}
