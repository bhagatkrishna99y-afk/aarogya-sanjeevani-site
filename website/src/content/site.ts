/**
 * Site-wide, non-page-specific copy and config, read once from the typed
 * env vars in `vite-env.d.ts`. Per-page copy belongs in its own file next
 * to this one (e.g. `home.ts`, `about.ts`) as the site grows -- this file
 * is only for things shared across every page (nav, footer, app-store
 * links, contact details).
 */

export interface NavItem {
  label: string;
  to: string;
}

export const siteContent = {
  name: "Aarogya Sanjeevani",
  tagline: "Healthcare, connected simply.",
  description:
    "Discover approved doctors and services, and track your care requests from one place. Booking itself happens in the Patient app -- this site is for discovery, information, and the operations console.",
  url: import.meta.env.VITE_SITE_URL,
  playStore: {
    patient: import.meta.env.VITE_PLAY_STORE_PATIENT_URL,
    doctor: import.meta.env.VITE_PLAY_STORE_DOCTOR_URL,
  },
  contact: {
    email: import.meta.env.VITE_CONTACT_EMAIL,
    phone: import.meta.env.VITE_CONTACT_PHONE,
  },
  /** India's single, nationwide emergency number -- used in the
   * footer's non-emergency disclaimer. Not configurable: it isn't a
   * per-deployment setting, it's a fact about where this platform
   * currently operates. */
  emergencyPhoneIndia: "112",
  nav: [
    { label: "Home", to: "/" },
    { label: "About", to: "/about" },
    { label: "Services", to: "/services" },
    { label: "Contact", to: "/contact" },
  ] satisfies NavItem[],
  legalNav: [
    { label: "Privacy Policy", to: "/privacy-policy" },
    { label: "Terms", to: "/terms" },
  ] satisfies NavItem[],
} as const;

export type SiteContent = typeof siteContent;
