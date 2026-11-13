import { useEffect, useState } from "react";
import { supabase } from "../../lib/supabase";
import type { Enums, Tables } from "../../lib/database.types";
import { EmptyState, ErrorState, LoadingState, FRIENDLY_ERROR } from "../../components/StatusViews";
import { Seo } from "@/components/seo/Seo";

type RequestStatus = Enums<"request_status">;
type HistoryRow = Tables<"request_status_history">;
type RequestRow = Tables<"requests"> & {
  patient: Pick<Tables<"profiles">, "full_name"> | null;
  doctor: Pick<Tables<"profiles">, "full_name"> | null;
  service: Pick<Tables<"services">, "name"> | null;
};

const STATUS_FILTERS: { value: RequestStatus | "all"; label: string }[] = [
  { value: "all", label: "All" },
  { value: "requested", label: "Requested" },
  { value: "accepted", label: "Accepted" },
  { value: "confirmed", label: "Confirmed" },
  { value: "completed", label: "Completed" },
  { value: "rejected", label: "Rejected" },
  { value: "cancelled", label: "Cancelled" },
];

const STATUS_STYLES: Record<RequestStatus, string> = {
  requested: "bg-slate-100 text-slate-700",
  accepted: "bg-blue-100 text-blue-700",
  confirmed: "bg-brand-100 text-brand-800",
  completed: "bg-emerald-100 text-emerald-700",
  rejected: "bg-red-100 text-red-700",
  cancelled: "bg-amber-100 text-amber-800",
};

export function RequestsPage() {
  const [filter, setFilter] = useState<RequestStatus | "all">("all");
  const [requests, setRequests] = useState<RequestRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [history, setHistory] = useState<Record<string, HistoryRow[]>>({});

  useEffect(() => {
    let active = true;
    setRequests(null);
    setError(null);

    let query = supabase
      .from("requests")
      .select(
        "*, patient:profiles!requests_patient_id_fkey(full_name), doctor:profiles!requests_doctor_id_fkey(full_name), service:services(name)",
      )
      .order("created_at", { ascending: false })
      .limit(100);

    if (filter !== "all") query = query.eq("status", filter);

    query.then(({ data, error: fetchError }) => {
      if (!active) return;
      if (fetchError) {
        setError(FRIENDLY_ERROR);
        return;
      }
      setRequests((data ?? []) as unknown as RequestRow[]);
    });

    return () => {
      active = false;
    };
  }, [filter, reloadKey]);

  async function toggleHistory(requestId: string) {
    if (expandedId === requestId) {
      setExpandedId(null);
      return;
    }
    setExpandedId(requestId);
    if (!history[requestId]) {
      const { data } = await supabase
        .from("request_status_history")
        .select("*")
        .eq("request_id", requestId)
        .order("changed_at", { ascending: true });
      setHistory((prev) => ({ ...prev, [requestId]: data ?? [] }));
    }
  }

  return (
    <div>
      <Seo title="Requests" />
      <h1 className="text-2xl font-bold text-slate-900">Requests</h1>

      <div className="mt-4 flex flex-wrap gap-1">
        {STATUS_FILTERS.map((f) => (
          <button
            key={f.value}
            onClick={() => setFilter(f.value)}
            className={`min-h-[48px] rounded-md px-3 text-sm font-medium ${
              filter === f.value ? "bg-brand-700 text-white" : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {error && <ErrorState message={error} onRetry={() => setReloadKey((k) => k + 1)} />}
        {!error && requests === null && <LoadingState label="Loading requests…" />}
        {!error && requests !== null && requests.length === 0 && (
          <EmptyState title="No requests found" />
        )}
        {!error && requests !== null && requests.length > 0 && (
          <ul className="space-y-3">
            {requests.map((request) => (
              <li key={request.id} className="rounded-lg border border-slate-200 bg-white">
                <button
                  onClick={() => toggleHistory(request.id)}
                  className="flex w-full flex-wrap items-center justify-between gap-3 px-4 py-3 text-left"
                  aria-expanded={expandedId === request.id}
                >
                  <div>
                    <p className="font-medium text-slate-900">
                      {request.service?.name ?? "Unknown service"}
                    </p>
                    <p className="text-sm text-slate-500">
                      Patient: {request.patient?.full_name ?? "—"}
                      {request.doctor?.full_name && ` · Doctor: ${request.doctor.full_name}`}
                    </p>
                    <p className="text-xs text-slate-500">
                      {new Date(request.scheduled_start).toLocaleString()}
                    </p>
                  </div>
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-medium ${STATUS_STYLES[request.status]}`}
                  >
                    {request.status}
                  </span>
                </button>

                {expandedId === request.id && (
                  <div className="border-t border-slate-100 px-4 py-3">
                    <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                      Status history
                    </p>
                    {!history[request.id] ? (
                      <LoadingState label="Loading history…" />
                    ) : history[request.id].length === 0 ? (
                      <p className="mt-2 text-sm text-slate-500">No history recorded.</p>
                    ) : (
                      <ol className="mt-2 space-y-1 text-sm text-slate-600">
                        {history[request.id].map((entry) => (
                          <li key={entry.id}>
                            {entry.old_status ? `${entry.old_status} → ` : "Created as "}
                            <span className="font-medium">{entry.new_status}</span>{" "}
                            <span className="text-slate-500">
                              ({new Date(entry.changed_at).toLocaleString()})
                            </span>
                            {entry.note && (
                              <span className="italic text-slate-500"> — {entry.note}</span>
                            )}
                          </li>
                        ))}
                      </ol>
                    )}
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
