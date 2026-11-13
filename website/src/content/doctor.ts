import { siteContent } from "./site";

/**
 * The one named physician featured on the public site today (Contact
 * page, Footer, and the About page's profile card). Phone/email are not
 * duplicated here -- they come from the same `VITE_CONTACT_PHONE`/
 * `VITE_CONTACT_EMAIL` env vars as the rest of the site's contact info
 * (see `site.ts`), so there's exactly one place to change them.
 */
export const doctorContent = {
  name: "Dr. Dilip Kumar Singh",
  role: "Resident Doctor, IMS BHU",
  bio: "Trained across government and private healthcare systems, committed to serving the unserved with integrity, compassion and ethical care.",
  tagline: "Service | Duty | Sacrifice",
  phone: siteContent.contact.phone,
  email: siteContent.contact.email,
} as const;
