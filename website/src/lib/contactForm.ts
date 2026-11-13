export interface ContactFormData {
  name: string;
  email: string;
  message: string;
}

export class ContactFormSubmitError extends Error {}

/**
 * Submits the contact form to a third-party form-to-email service
 * (Formspree by default -- see VITE_CONTACT_FORM_ENDPOINT in .env.example).
 * This site has no backend of its own to receive form submissions, so a
 * plain `fetch` to a configurable endpoint is the whole implementation --
 * deliberately not routed through `@/lib/supabase`, since contact messages
 * have nothing to do with the product's own data model.
 *
 * Isolated in its own module (rather than inlined in ContactPage) so it's
 * a single, mockable seam: component tests can replace this function
 * directly, and e2e tests can intercept the exact `fetch` call via
 * `page.route(endpoint, ...)` without needing to know anything about the
 * form UI.
 */
export async function submitContactMessage(data: ContactFormData): Promise<void> {
  const endpoint = import.meta.env.VITE_CONTACT_FORM_ENDPOINT;

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    throw new ContactFormSubmitError(
      `Contact form submission failed with status ${response.status}`,
    );
  }
}
