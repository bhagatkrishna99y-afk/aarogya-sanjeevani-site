import { Link } from "react-router-dom";

const steps = [
  { title: "Discover a service", body: "Browse consultations, tests and specialist care near you." },
  { title: "Pick a doctor & slot", body: "See real-time availability and choose a time that works for you." },
  { title: "Submit your request", body: "Add any notes the doctor should know before your visit." },
  { title: "Track it end to end", body: "Get notified as your request is accepted, confirmed and completed." },
];

export function HomePage() {
  return (
    <div>
      <section className="mx-auto max-w-6xl px-4 py-16 sm:px-6 sm:py-24">
        <div className="max-w-2xl">
          <h1 className="text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl">
            Healthcare, connected simply.
          </h1>
          <p className="mt-4 text-lg text-slate-600">
            Aarogya Sanjeevani connects you with approved doctors for consultations and services —
            discover, book and track your care requests from one place.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              to="/services"
              className="inline-flex min-h-[48px] items-center rounded-md bg-brand-600 px-6 text-sm font-semibold text-white hover:bg-brand-700"
            >
              Browse services
            </Link>
            <Link
              to="/about"
              className="inline-flex min-h-[48px] items-center rounded-md border border-slate-300 px-6 text-sm font-semibold text-slate-700 hover:bg-white"
            >
              How it works
            </Link>
          </div>
          <p className="mt-6 text-sm text-slate-500">
            Booking happens in the Patient app. This website is for discovery, information and the
            operations console.
          </p>
        </div>
      </section>

      <section className="border-t border-slate-200 bg-white">
        <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6">
          <h2 className="text-2xl font-bold text-slate-900">How a request works</h2>
          <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {steps.map((step, index) => (
              <div key={step.title} className="rounded-lg border border-slate-200 p-5">
                <span className="text-sm font-semibold text-brand-600">Step {index + 1}</span>
                <h3 className="mt-2 font-semibold text-slate-900">{step.title}</h3>
                <p className="mt-1 text-sm text-slate-600">{step.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="border-t border-slate-200">
        <div className="mx-auto max-w-6xl px-4 py-16 text-center sm:px-6">
          <h2 className="text-2xl font-bold text-slate-900">Are you a doctor?</h2>
          <p className="mx-auto mt-2 max-w-xl text-slate-600">
            Register through the Doctor app to offer your services. Every application is reviewed
            by our team before you appear to patients.
          </p>
        </div>
      </section>
    </div>
  );
}
