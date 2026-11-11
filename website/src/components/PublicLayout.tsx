import { NavLink, Outlet } from "react-router-dom";

const navItems = [
  { to: "/", label: "Home" },
  { to: "/services", label: "Services" },
  { to: "/about", label: "About" },
  { to: "/contact", label: "Contact" },
];

export function PublicLayout() {
  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <nav
          aria-label="Primary"
          className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4 sm:px-6"
        >
          <NavLink to="/" className="flex items-center gap-2 text-lg font-bold text-brand-800">
            <span aria-hidden="true">🌿</span>
            Aarogya Sanjeevani
          </NavLink>
          <ul className="flex items-center gap-1 sm:gap-2">
            {navItems.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.to === "/"}
                  className={({ isActive }) =>
                    `inline-flex min-h-[48px] items-center rounded-md px-3 text-sm font-medium transition-colors ${
                      isActive
                        ? "bg-brand-50 text-brand-700"
                        : "text-slate-600 hover:bg-slate-100 hover:text-slate-900"
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-slate-200 bg-white">
        <div className="mx-auto max-w-6xl px-4 py-8 text-sm text-slate-500 sm:px-6">
          <p>© {new Date().getFullYear()} Aarogya Sanjeevani. Connecting patients with approved doctors.</p>
          <p className="mt-1">
            For emergencies, please contact your nearest hospital directly. This platform does not
            provide emergency, ambulance or prescription services.
          </p>
        </div>
      </footer>
    </div>
  );
}
