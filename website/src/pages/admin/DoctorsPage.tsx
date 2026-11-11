import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { supabase } from "../../lib/supabase";
import type { Enums, Tables } from "../../lib/database.types";
import { EmptyState, ErrorState, LoadingState, FRIENDLY_ERROR } from "../../components/StatusViews";

type DoctorStatus = Enums<"doctor_status">;
type DoctorRow = Tables<"doctor_profiles"> & { profiles: Pick<Tables<"profiles">, "full_name" | "phone"> };

const TABS: { value: DoctorStatus; label: string }[] = [
  { value: "pending", label: "Pending" },
  { value: "approved", label: "Approved" },
  { value: "suspended", label: "Suspended" },
  { value: "rejected", label: "Rejected" },
];

export function DoctorsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const status = (searchParams.get("status") as DoctorStatus) || "pending";

  const [doctors, setDoctors] = useState<DoctorRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [actioningId, setActioningId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setDoctors(null);
    setError(null);

    supabase
      .from("doctor_profiles")
      .select("*, profiles!doctor_profiles_user_id_fkey(full_name, phone)")
      .eq("status", status)
      .order("created_at", { ascending: false })
      .then(({ data, error: fetchError }) => {
        if (!active) return;
        if (fetchError) {
          setError(FRIENDLY_ERROR);
          return;
        }
        setDoctors((data ?? []) as unknown as DoctorRow[]);
      });

    return () => {
      active = false;
    };
  }, [status, reloadKey]);

  async function setStatus(doctorId: string, newStatus: DoctorStatus, reason?: string) {
    setActioningId(doctorId);
    setActionError(null);
    const { error: rpcError } = await supabase.rpc("admin_set_doctor_status", {
      p_doctor_id: doctorId,
      p_new_status: newStatus,
      p_reason: reason,
    });
    setActioningId(null);
    if (rpcError) {
      setActionError(rpcError.message || FRIENDLY_ERROR);
      return;
    }
    setReloadKey((k) => k + 1);
  }

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900">Doctors</h1>

      <div role="tablist" aria-label="Doctor status" className="mt-4 flex gap-1 border-b border-slate-200">
        {TABS.map((tab) => (
          <button
            key={tab.value}
            role="tab"
            aria-selected={status === tab.value}
            onClick={() => setSearchParams({ status: tab.value })}
            className={`min-h-[48px] border-b-2 px-4 text-sm font-medium ${
              status === tab.value
                ? "border-brand-600 text-brand-700"
                : "border-transparent text-slate-500 hover:text-slate-700"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {actionError && (
        <p role="alert" className="mt-4 text-sm text-red-700">
          {actionError}
        </p>
      )}

      <div className="mt-6">
        {error && <ErrorState message={error} onRetry={() => setReloadKey((k) => k + 1)} />}
        {!error && doctors === null && <LoadingState label="Loading doctors…" />}
        {!error && doctors !== null && doctors.length === 0 && (
          <EmptyState title={`No ${status} doctors`} />
        )}
        {!error && doctors !== null && doctors.length > 0 && (
          <ul className="space-y-4">
            {doctors.map((doctor) => (
              <li key={doctor.user_id} className="rounded-lg border border-slate-200 bg-white p-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <p className="font-semibold text-slate-900">{doctor.profiles.full_name}</p>
                    <p className="text-sm text-slate-600">{doctor.specialization}</p>
                    <p className="text-xs text-slate-500">
                      License: {doctor.license_number} · {doctor.years_experience} yrs experience
                    </p>
                    {doctor.profiles.phone && (
                      <p className="text-xs text-slate-500">Phone: {doctor.profiles.phone}</p>
                    )}
                    {doctor.bio && <p className="mt-2 max-w-xl text-sm text-slate-600">{doctor.bio}</p>}
                    {doctor.rejection_reason && (
                      <p className="mt-2 text-sm text-red-700">Reason: {doctor.rejection_reason}</p>
                    )}
                  </div>

                  <DoctorActions
                    doctor={doctor}
                    busy={actioningId === doctor.user_id}
                    onApprove={() => setStatus(doctor.user_id, "approved")}
                    onReject={(reason) => setStatus(doctor.user_id, "rejected", reason)}
                    onSuspend={() => setStatus(doctor.user_id, "suspended")}
                    onReactivate={() => setStatus(doctor.user_id, "approved")}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function DoctorActions({
  doctor,
  busy,
  onApprove,
  onReject,
  onSuspend,
  onReactivate,
}: {
  doctor: DoctorRow;
  busy: boolean;
  onApprove: () => void;
  onReject: (reason: string) => void;
  onSuspend: () => void;
  onReactivate: () => void;
}) {
  const [showRejectInput, setShowRejectInput] = useState(false);
  const [reason, setReason] = useState("");

  const buttonClass =
    "min-h-[48px] rounded-md px-4 text-sm font-medium disabled:opacity-60 disabled:cursor-not-allowed";

  if (doctor.status === "pending") {
    return (
      <div className="flex flex-col items-end gap-2">
        {!showRejectInput ? (
          <div className="flex gap-2">
            <button
              disabled={busy}
              onClick={onApprove}
              className={`${buttonClass} bg-brand-600 text-white hover:bg-brand-700`}
            >
              Approve
            </button>
            <button
              disabled={busy}
              onClick={() => setShowRejectInput(true)}
              className={`${buttonClass} border border-red-300 text-red-700 hover:bg-red-50`}
            >
              Reject
            </button>
          </div>
        ) : (
          <div className="flex w-64 flex-col gap-2">
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Reason for rejection (shown to the doctor)"
              className="min-h-[72px] rounded-md border border-slate-300 p-2 text-sm"
            />
            <div className="flex justify-end gap-2">
              <button
                disabled={busy}
                onClick={() => setShowRejectInput(false)}
                className={`${buttonClass} text-slate-600`}
              >
                Cancel
              </button>
              <button
                disabled={busy || reason.trim() === ""}
                onClick={() => onReject(reason)}
                className={`${buttonClass} bg-red-600 text-white hover:bg-red-700`}
              >
                Confirm reject
              </button>
            </div>
          </div>
        )}
      </div>
    );
  }

  if (doctor.status === "approved") {
    return (
      <button
        disabled={busy}
        onClick={onSuspend}
        className={`${buttonClass} border border-amber-300 text-amber-800 hover:bg-amber-50`}
      >
        Suspend
      </button>
    );
  }

  if (doctor.status === "suspended") {
    return (
      <button
        disabled={busy}
        onClick={onReactivate}
        className={`${buttonClass} bg-brand-600 text-white hover:bg-brand-700`}
      >
        Reactivate
      </button>
    );
  }

  return null;
}
