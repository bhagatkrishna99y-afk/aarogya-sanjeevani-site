import { Link } from "react-router-dom";
import { StoreBadge } from "@/components/ui/StoreBadge";
import { siteContent } from "@/content/site";
import { doctorContent } from "@/content/doctor";
import logo from "@/assets/logo.png";

export function Footer() {
  return (
    <footer className="border-t border-slate-200 bg-white">
      <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-4">
          <div className="lg:col-span-2">
            <img src={logo} alt={siteContent.name} className="h-16 w-auto" />
            <p className="mt-3 max-w-sm text-sm text-slate-600">{siteContent.description}</p>
          </div>

          <nav aria-label="Quick links">
            <h2 className="text-sm font-semibold text-slate-900">Quick links</h2>
            <ul className="mt-3 space-y-2 text-sm">
              {siteContent.nav.map((item) => (
                <li key={item.to}>
                  <Link
                    to={item.to}
                    className="text-slate-600 hover:text-brand-700 hover:underline"
                  >
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>

          <div>
            <h2 className="text-sm font-semibold text-slate-900">Legal</h2>
            <ul className="mt-3 space-y-2 text-sm">
              {siteContent.legalNav.map((item) => (
                <li key={item.to}>
                  <Link
                    to={item.to}
                    className="text-slate-600 hover:text-brand-700 hover:underline"
                  >
                    {item.label}
                  </Link>
                </li>
              ))}
            </ul>
            <h2 className="mt-6 text-sm font-semibold text-slate-900">Contact</h2>
            <p className="mt-3 text-sm font-medium text-slate-900">{doctorContent.name}</p>
            <p className="text-sm text-slate-600">{doctorContent.role}</p>
            <ul className="mt-2 space-y-2 text-sm text-slate-600">
              <li>
                <a
                  href={`mailto:${doctorContent.email}`}
                  className="hover:text-brand-700 hover:underline"
                >
                  {doctorContent.email}
                </a>
              </li>
              <li>
                <a
                  href={`tel:${doctorContent.phone}`}
                  className="hover:text-brand-700 hover:underline"
                >
                  {doctorContent.phone}
                </a>
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-10 flex flex-wrap gap-3 border-t border-slate-200 pt-8">
          <StoreBadge
            store="google-play"
            href={siteContent.playStore.patient}
            appLabel="Patient app"
          />
          <StoreBadge
            store="google-play"
            href={siteContent.playStore.doctor}
            appLabel="Doctor app"
          />
        </div>

        <div className="mt-8 border-t border-slate-200 pt-6 text-sm text-slate-500">
          <p role="note">
            {siteContent.name} is not for medical emergencies. If this is an emergency, call your
            local emergency services ({siteContent.emergencyPhoneIndia} in India) or go to your
            nearest hospital directly.
          </p>
          <p className="mt-2">
            © {new Date().getFullYear()} {siteContent.name}. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}
