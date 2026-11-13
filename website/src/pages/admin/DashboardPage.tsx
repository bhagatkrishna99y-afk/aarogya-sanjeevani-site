import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { supabase } from "../../lib/supabase";
import { ErrorState, LoadingState, FRIENDLY_ERROR } from "../../components/StatusViews";
import { Seo } from "@/components/seo/Seo";

interface Stats {
  pendingDoctors: number;
  approvedDoctors: number;
  openRequests: number;
  servicesCount: number;
}

export function DashboardPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setStats(null);
    setError(null);

    (async () => {
      const [pending, approved, open, services] = await Promise.all([
        supabase
          .from("doctor_profiles")
          .select("*", { count: "exact", head: true })
          .eq("status", "pending"),
        supabase
          .from("doctor_profiles")
          .select("*", { count: "exact", head: true })
          .eq("status", "approved"),
        supabase
          .from("requests")
          .select("*", { count: "exact", head: true })
          .in("status", ["requested", "accepted", "confirmed"]),
        supabase.from("services").select("*", { count: "exact", head: true }).eq("is_active", true),
      ]);

      if (!active) return;

      if (pending.error || approved.error || open.error || services.error) {
        setError(FRIENDLY_ERROR);
        return;
      }

      setStats({
        pendingDoctors: pending.count ?? 0,
        approvedDoctors: approved.count ?? 0,
        openRequests: open.count ?? 0,
        servicesCount: services.count ?? 0,
      });
    })();

    return () => {
      active = false;
    };
  }, [reloadKey]);

  return (
    <div>
      <Seo title="Dashboard" />
      <h1 className="text-2xl font-bold text-slate-900">Dashboard</h1>

      {error && (
        <div className="mt-6">
          <ErrorState message={error} onRetry={() => setReloadKey((k) => k + 1)} />
        </div>
      )}
      {!error && !stats && <LoadingState label="Loading dashboard…" />}
      {!error && stats && (
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            label="Pending doctor applications"
            value={stats.pendingDoctors}
            href="/admin/doctors?status=pending"
            highlight={stats.pendingDoctors > 0}
          />
          <StatCard
            label="Approved doctors"
            value={stats.approvedDoctors}
            href="/admin/doctors?status=approved"
          />
          <StatCard label="Open requests" value={stats.openRequests} href="/admin/requests" />
          <StatCard label="Active services" value={stats.servicesCount} href="/admin/catalog" />
        </div>
      )}
    </div>
  );
}

function StatCard({
  label,
  value,
  href,
  highlight,
}: {
  label: string;
  value: number;
  href: string;
  highlight?: boolean;
}) {
  return (
    <Link
      to={href}
      className={`block rounded-lg border p-5 transition-colors hover:border-brand-400 ${
        highlight ? "border-brand-300 bg-brand-50" : "border-slate-200 bg-white"
      }`}
    >
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-1 text-3xl font-bold text-slate-900">{value}</p>
    </Link>
  );
}
