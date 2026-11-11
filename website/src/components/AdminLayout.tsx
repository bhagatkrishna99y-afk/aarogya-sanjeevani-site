import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

const navItems = [
  { to: "/admin", label: "Dashboard" },
  { to: "/admin/doctors", label: "Doctors" },
  { to: "/admin/catalog", label: "Catalog" },
  { to: "/admin/requests", label: "Requests" },
  { to: "/admin/users", label: "Users" },
];

export function AdminLayout() {
  const { profile, signOut } = useAuth();

  return (
    <div className="flex min-h-screen bg-slate-100">
      <aside className="hidden w-60 flex-col border-r border-slate-200 bg-white p-4 sm:flex">
        <p className="mb-6 px-2 text-lg font-bold text-brand-800">Ops Console</p>
        <nav aria-label="Admin" className="flex flex-1 flex-col gap-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/admin"}
              className={({ isActive }) =>
                `min-h-[48px] rounded-md px-3 py-2 text-sm font-medium ${
                  isActive ? "bg-brand-50 text-brand-700" : "text-slate-600 hover:bg-slate-100"
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-200 pt-4">
          <p className="truncate px-2 text-xs text-slate-500">{profile?.full_name}</p>
          <button
            type="button"
            onClick={() => void signOut()}
            className="mt-2 min-h-[48px] w-full rounded-md px-3 py-2 text-left text-sm font-medium text-slate-600 hover:bg-slate-100"
          >
            Sign out
          </button>
        </div>
      </aside>

      <div className="flex-1 overflow-x-hidden">
        <header className="border-b border-slate-200 bg-white sm:hidden">
          <div className="flex items-center justify-between px-4 py-3">
            <p className="font-bold text-brand-800">Ops Console</p>
            <button
              type="button"
              onClick={() => void signOut()}
              className="min-h-[48px] rounded-md px-3 text-sm font-medium text-slate-600"
            >
              Sign out
            </button>
          </div>
          <nav aria-label="Admin" className="flex gap-1 overflow-x-auto px-2 pb-2">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === "/admin"}
                className={({ isActive }) =>
                  `min-h-[48px] shrink-0 rounded-md px-3 py-2 text-sm font-medium ${
                    isActive ? "bg-brand-50 text-brand-700" : "text-slate-600"
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </header>
        <main className="p-4 sm:p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
