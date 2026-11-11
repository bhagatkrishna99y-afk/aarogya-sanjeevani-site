export function AboutPage() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-bold text-slate-900">About Aarogya Sanjeevani</h1>
      <div className="mt-6 space-y-4 text-slate-700">
        <p>
          Aarogya Sanjeevani connects patients with doctors who have been reviewed and approved by
          our team. Every doctor you see in the app has gone through a verification step before
          they can accept requests.
        </p>
        <p>
          Patients discover services, pick an approved doctor when one is required, choose a time
          from the doctor's real availability, and submit a request. Doctors accept or decline
          requests, and confirm and complete them as care is delivered. Every status change is
          recorded, and both sides get notified in the app.
        </p>
        <h2 className="pt-4 text-xl font-semibold text-slate-900">What this platform is not</h2>
        <p>
          In this release, Aarogya Sanjeevani does not process payments, store medical records or
          prescriptions, arrange insurance or ambulance dispatch, or provide in-app chat or video
          calls. It focuses on getting a request from the right patient to the right doctor,
          reliably.
        </p>
      </div>
    </div>
  );
}
