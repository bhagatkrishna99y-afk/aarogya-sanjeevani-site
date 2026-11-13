import { Seo } from "@/components/seo/Seo";
import { ContactForm } from "@/components/sections/ContactForm";
import { doctorContent } from "@/content/doctor";

export function ContactPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <Seo title="Contact" description="How to reach Aarogya Sanjeevani support." />
      <h1 className="text-3xl font-bold text-slate-900">Contact us</h1>
      <p className="mt-4 text-slate-700">
        For questions about an existing request, please use the Patient or Doctor app so our team
        can see your account context. For anything else, send us a message below or reach us
        directly at the details below.
      </p>
      <dl className="mt-8 space-y-4 text-sm">
        <div>
          <dt className="font-medium text-slate-900">
            {doctorContent.name} &middot; {doctorContent.role}
          </dt>
          <dd className="mt-1 text-slate-600">
            <a href={`tel:${doctorContent.phone}`} className="text-brand-700 hover:underline">
              {doctorContent.phone}
            </a>
            {" · "}
            <a href={`mailto:${doctorContent.email}`} className="text-brand-700 hover:underline">
              {doctorContent.email}
            </a>
          </dd>
        </div>
        <div>
          <dt className="font-medium text-slate-900">Doctor onboarding</dt>
          <dd className="text-slate-600">
            Register in the Doctor app; our admin team reviews every application.
          </dd>
        </div>
      </dl>

      <div className="mt-10 border-t border-slate-200 pt-10">
        <ContactForm />
      </div>

      <p className="mt-8 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
        If this is a medical emergency, contact your nearest hospital or local emergency number
        directly. This platform does not dispatch ambulances.
      </p>
    </div>
  );
}
