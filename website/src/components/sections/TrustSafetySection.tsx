import { Container } from "@/components/ui/Container";
import { homeContent } from "@/content/home";
import trustIllustration from "@/assets/trust-illustration.svg";

export function TrustSafetySection() {
  const { heading, description, points } = homeContent.trust;

  return (
    <section aria-labelledby="trust-safety-heading" className="bg-white py-16 sm:py-24">
      <Container>
        <div className="grid items-center gap-10 lg:grid-cols-2">
          <div>
            <h2 id="trust-safety-heading" className="text-2xl font-bold text-slate-900 sm:text-3xl">
              {heading}
            </h2>
            <p className="mt-3 max-w-xl text-base text-slate-600">{description}</p>

            <dl className="mt-8 space-y-6">
              {points.map((point) => (
                <div key={point.title}>
                  <dt className="font-semibold text-slate-900">{point.title}</dt>
                  <dd className="mt-1 text-sm text-slate-600">{point.description}</dd>
                </div>
              ))}
            </dl>
          </div>

          <div className="mx-auto w-full max-w-sm lg:max-w-none">
            {/* Below the fold -- safe to defer. */}
            <img
              src={trustIllustration}
              alt="Illustration of a padlock with a checkmark badge, representing secure accounts and privacy-first data handling"
              width={320}
              height={320}
              loading="lazy"
              className="h-auto w-full"
            />
          </div>
        </div>
      </Container>
    </section>
  );
}
