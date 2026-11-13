import { Seo } from "@/components/seo/Seo";
import { siteContent } from "@/content/site";

export function TermsPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <Seo title="Terms" description="Terms of use for the Aarogya Sanjeevani website." />
      <h1 className="text-3xl font-bold text-slate-900">Terms of Use</h1>
      <p className="mt-2 text-sm text-slate-500">Last updated: {new Date().getFullYear()}</p>

      <div className="mt-6 space-y-6 text-slate-700">
        <section>
          <h2 className="text-xl font-semibold text-slate-900">Scope</h2>
          <p className="mt-2">
            These terms cover use of <strong>this website</strong> -- browsing service and doctor
            information, and contacting us. Booking an appointment, submitting a doctor application,
            and any use of health-related features happen in the Patient app or Doctor app, which
            have their own terms shown in-app.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">Not for medical emergencies</h2>
          <p className="mt-2">
            {siteContent.name} is not an emergency service. If you have a medical emergency, call
            your local emergency number ({siteContent.emergencyPhoneIndia} in India) or go to the
            nearest hospital directly -- do not use this website or wait for a response from us.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">No medical advice</h2>
          <p className="mt-2">
            Nothing on this website is medical advice. Service and doctor listings are for discovery
            purposes; they don't diagnose, treat, or substitute for an actual consultation with a
            qualified doctor.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">Acceptable use</h2>
          <p className="mt-2">
            Use this website lawfully and don't attempt to disrupt it, scrape it at a volume that
            degrades it for others, or misrepresent your identity when contacting us.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">Information accuracy</h2>
          <p className="mt-2">
            We aim to keep service and doctor information current, but availability and details can
            change. Always confirm specifics (availability, pricing, what a service includes) in the
            Patient app at the time of booking rather than relying solely on this website.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">Liability</h2>
          <p className="mt-2">
            This website is provided as-is, for informational purposes. To the extent permitted by
            law, we aren't liable for decisions made solely on the basis of information found here,
            or for interruptions to the website's availability.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">Changes to these terms</h2>
          <p className="mt-2">
            We may update these terms as the website changes. Continued use after an update means
            you accept the revised terms.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">Contact us</h2>
          <p className="mt-2">
            Questions about these terms? Email{" "}
            <a
              href={`mailto:${siteContent.contact.email}`}
              className="text-brand-700 underline underline-offset-2"
            >
              {siteContent.contact.email}
            </a>
            .
          </p>
        </section>
      </div>
    </div>
  );
}
