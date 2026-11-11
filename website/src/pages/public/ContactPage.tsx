export function ContactPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold text-slate-900">Contact us</h1>
      <p className="mt-4 text-slate-700">
        For questions about an existing request, please use the Patient or Doctor app so our team
        can see your account context. For anything else, reach us at the details below.
      </p>
      <dl className="mt-8 space-y-4 text-sm">
        <div>
          <dt className="font-medium text-slate-900">Support email</dt>
          <dd className="text-slate-600">support@aarogyasanjeevani.example</dd>
        </div>
        <div>
          <dt className="font-medium text-slate-900">Doctor onboarding</dt>
          <dd className="text-slate-600">
            Register in the Doctor app; our admin team reviews every application.
          </dd>
        </div>
      </dl>
      <p className="mt-8 rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
        If this is a medical emergency, contact your nearest hospital or local emergency number
        directly. This platform does not dispatch ambulances.
      </p>
    </div>
  );
}
