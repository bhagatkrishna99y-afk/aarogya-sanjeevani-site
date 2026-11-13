import { Seo } from "@/components/seo/Seo";
import { siteContent } from "@/content/site";

export function PrivacyPolicyPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <Seo
        title="Privacy Policy"
        description="What this website collects (and doesn't), and how to reach us with privacy questions."
      />
      <h1 className="text-3xl font-bold text-slate-900">Privacy Policy</h1>
      <p className="mt-2 text-sm text-slate-500">Last updated: {new Date().getFullYear()}</p>

      <div className="mt-6 space-y-6 text-slate-700">
        <section>
          <h2 className="text-xl font-semibold text-slate-900">What this policy covers</h2>
          <p className="mt-2">
            This policy covers <strong>this website only</strong> -- the public marketing and
            information pages you're reading now. It does not cover the Patient app or Doctor app,
            which collect and process personal and health-related information (accounts, appointment
            requests, doctor applications) under their own, more detailed privacy notices shown
            in-app, since they handle meaningfully more sensitive data than a marketing site does.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">What this website collects</h2>
          <p className="mt-2">
            Browsing this website does not create an account, and this site does not run any
            analytics, advertising, or third-party tracking script -- there is nothing here to opt
            out of, because nothing is being collected as you browse.
          </p>
          <p className="mt-2">
            If you email us or use the contact form, we receive whatever you choose to include in
            that message (e.g. your name and email address) so we can reply. We don't use that
            information for anything beyond responding to you. The contact form is processed by a
            third-party form provider, Formspree -- see "Third parties" below.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">Cookies</h2>
          <p className="mt-2">
            This website does not set any cookies beyond what's strictly required for it to function
            (currently, none). If that changes in the future -- for example, if we add analytics to
            understand how the site is used -- we'll add a cookie/consent banner before any
            non-essential cookie is set, and update this section to describe it.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">Third parties</h2>
          <p className="mt-2">
            This website doesn't sell your information, and doesn't share it with any third party
            beyond what's needed to deliver a message you choose to send us directly.
          </p>
          <p className="mt-2">
            When you submit the contact form, its contents (your name, email address, and message)
            are sent to and processed by{" "}
            <a
              href="https://formspree.io"
              className="text-brand-700 underline underline-offset-2"
              target="_blank"
              rel="noopener noreferrer"
            >
              Formspree
            </a>
            , a third-party form-to-email service, so that message can reach us. Formspree only
            receives what you type into that form -- it has no other access to this website or to
            any other information about you. See{" "}
            <a
              href="https://formspree.io/legal/privacy-policy"
              className="text-brand-700 underline underline-offset-2"
              target="_blank"
              rel="noopener noreferrer"
            >
              Formspree's own privacy policy
            </a>{" "}
            for how it handles that data.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">Changes to this policy</h2>
          <p className="mt-2">
            If this policy changes -- for instance, when analytics or a new feature is added to the
            website -- we'll update this page and the "last updated" date above.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold text-slate-900">Contact us</h2>
          <p className="mt-2">
            Questions about this policy? Email{" "}
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
