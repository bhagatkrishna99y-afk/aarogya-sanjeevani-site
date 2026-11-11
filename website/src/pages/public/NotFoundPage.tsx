import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div className="flex flex-col items-center gap-3 px-4 py-24 text-center">
      <h1 className="text-2xl font-bold text-slate-900">Page not found</h1>
      <p className="text-slate-600">The page you're looking for doesn't exist.</p>
      <Link to="/" className="min-h-[48px] text-brand-700 underline underline-offset-2">
        Back to home
      </Link>
    </div>
  );
}
