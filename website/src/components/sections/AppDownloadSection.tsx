import { Container } from "@/components/ui/Container";
import { StoreBadge } from "@/components/ui/StoreBadge";
import { homeContent } from "@/content/home";

export function AppDownloadSection() {
  const { heading, description, patient, doctor } = homeContent.app;

  return (
    <section aria-labelledby="app-download-heading" className="bg-brand-900 py-16 sm:py-24">
      <Container>
        <div className="text-center">
          <h2 id="app-download-heading" className="text-2xl font-bold text-white sm:text-3xl">
            {heading}
          </h2>
          <p className="mx-auto mt-3 max-w-xl text-base text-brand-100">{description}</p>

          <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
            <StoreBadge store="google-play" href={patient.href} appLabel={patient.label} />
            <StoreBadge store="app-store" href={doctor.href} appLabel={doctor.label} />
          </div>
        </div>
      </Container>
    </section>
  );
}
