import { Card } from "@/components/ui/Card";
import { doctorContent } from "@/content/doctor";

export function DoctorProfileCard() {
  return (
    <Card className="mx-auto max-w-xl text-center">
      <h3 className="text-xl font-semibold text-slate-900">{doctorContent.name}</h3>
      <p className="text-sm font-medium text-brand-700">{doctorContent.role}</p>
      <p className="mt-3 text-sm text-slate-600">{doctorContent.bio}</p>
      <p className="mt-4 text-sm font-medium tracking-wide text-slate-500">
        {doctorContent.tagline}
      </p>
      <dl className="mt-5 flex flex-wrap justify-center gap-x-6 gap-y-2 text-sm">
        <div className="flex items-center gap-1.5">
          <dt className="sr-only">Phone</dt>
          <dd>
            <a href={`tel:${doctorContent.phone}`} className="text-brand-700 hover:underline">
              {doctorContent.phone}
            </a>
          </dd>
        </div>
        <div className="flex items-center gap-1.5">
          <dt className="sr-only">Email</dt>
          <dd>
            <a href={`mailto:${doctorContent.email}`} className="text-brand-700 hover:underline">
              {doctorContent.email}
            </a>
          </dd>
        </div>
      </dl>
    </Card>
  );
}
