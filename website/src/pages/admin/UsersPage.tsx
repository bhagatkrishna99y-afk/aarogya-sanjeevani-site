import { useEffect, useState } from "react";
import { supabase } from "../../lib/supabase";
import type { Enums, Tables } from "../../lib/database.types";
import { EmptyState, ErrorState, LoadingState, FRIENDLY_ERROR } from "../../components/StatusViews";

type Role = Enums<"user_role">;
type Profile = Tables<"profiles">;

const ROLE_FILTERS: { value: Role | "all"; label: string }[] = [
  { value: "all", label: "All" },
  { value: "user", label: "Patients" },
  { value: "doctor", label: "Doctors" },
  { value: "admin", label: "Admins" },
];

export function UsersPage() {
  const [filter, setFilter] = useState<Role | "all">("all");
  const [users, setUsers] = useState<Profile[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [promotingId, setPromotingId] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    setUsers(null);
    setError(null);

    let query = supabase.from("profiles").select("*").order("created_at", { ascending: false }).limit(200);
    if (filter !== "all") query = query.eq("role", filter);

    query.then(({ data, error: fetchError }) => {
      if (!active) return;
      if (fetchError) {
        setError(FRIENDLY_ERROR);
        return;
      }
      setUsers(data ?? []);
    });

    return () => {
      active = false;
    };
  }, [filter, reloadKey]);

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900">Users</h1>
      <p className="mt-1 max-w-xl text-sm text-slate-600">
        Roles can only change here or via doctor approval — never by users themselves. Granting
        admin access cannot be undone from this screen; remove access by editing the database
        directly if needed.
      </p>

      <div className="mt-4 flex flex-wrap gap-1">
        {ROLE_FILTERS.map((f) => (
          <button
            key={f.value}
            onClick={() => setFilter(f.value)}
            className={`min-h-[48px] rounded-md px-3 text-sm font-medium ${
              filter === f.value ? "bg-brand-600 text-white" : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      <div className="mt-6">
        {error && <ErrorState message={error} onRetry={() => setReloadKey((k) => k + 1)} />}
        {!error && users === null && <LoadingState label="Loading users…" />}
        {!error && users !== null && users.length === 0 && <EmptyState title="No users found" />}
        {!error && users !== null && users.length > 0 && (
          <ul className="divide-y divide-slate-200 rounded-lg border border-slate-200 bg-white">
            {users.map((user) => (
              <li key={user.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
                <div>
                  <p className="font-medium text-slate-900">{user.full_name}</p>
                  <p className="text-xs text-slate-500">
                    {user.phone ?? "No phone on file"} · joined {new Date(user.created_at).toLocaleDateString()}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium capitalize text-slate-700">
                    {user.role}
                  </span>
                  {user.role !== "admin" && (
                    <PromoteButton
                      user={user}
                      promoting={promotingId === user.id}
                      onPromoted={() => {
                        setPromotingId(null);
                        setReloadKey((k) => k + 1);
                      }}
                      onStart={() => setPromotingId(user.id)}
                    />
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function PromoteButton({
  user,
  promoting,
  onStart,
  onPromoted,
}: {
  user: Profile;
  promoting: boolean;
  onStart: () => void;
  onPromoted: () => void;
}) {
  const [confirming, setConfirming] = useState(false);
  const [confirmText, setConfirmText] = useState("");
  const [error, setError] = useState<string | null>(null);

  if (!confirming) {
    return (
      <button
        onClick={() => setConfirming(true)}
        className="min-h-[48px] rounded-md border border-slate-300 px-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
      >
        Make admin
      </button>
    );
  }

  async function confirm() {
    if (confirmText.trim() !== user.full_name) return;
    onStart();
    setError(null);
    const { error: rpcError } = await supabase.rpc("admin_assign_admin_role", { p_user_id: user.id });
    if (rpcError) {
      setError(rpcError.message || "Couldn't grant admin access.");
      onPromoted();
      return;
    }
    onPromoted();
  }

  return (
    <div className="flex flex-col items-end gap-1">
      <div className="flex items-center gap-2">
        <input
          value={confirmText}
          onChange={(e) => setConfirmText(e.target.value)}
          placeholder={`Type "${user.full_name}" to confirm`}
          className="min-h-[48px] w-48 rounded-md border border-slate-300 px-2 text-sm"
        />
        <button
          disabled={promoting || confirmText.trim() !== user.full_name}
          onClick={confirm}
          className="min-h-[48px] rounded-md bg-red-600 px-3 text-sm font-semibold text-white disabled:opacity-50"
        >
          Confirm
        </button>
        <button
          onClick={() => {
            setConfirming(false);
            setConfirmText("");
          }}
          className="min-h-[48px] px-2 text-sm text-slate-500"
        >
          Cancel
        </button>
      </div>
      {error && <p className="text-xs text-red-700">{error}</p>}
    </div>
  );
}
