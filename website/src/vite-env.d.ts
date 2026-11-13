/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SUPABASE_URL: string;
  readonly VITE_SUPABASE_PUBLISHABLE_KEY: string;
  readonly VITE_SITE_URL: string;
  readonly VITE_PLAY_STORE_PATIENT_URL: string;
  readonly VITE_PLAY_STORE_DOCTOR_URL: string;
  readonly VITE_CONTACT_EMAIL: string;
  readonly VITE_CONTACT_PHONE: string;
  /** A Formspree-style form endpoint (POST, Accept: application/json) that
   * accepts {name, email, message}. See src/lib/contactForm.ts. */
  readonly VITE_CONTACT_FORM_ENDPOINT: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
