import { useId } from "react";
import { Container } from "@/components/ui/Container";
import { StepCard } from "@/components/ui/StepCard";
import { homeContent } from "@/content/home";

export function HowItWorksSection() {
  const { heading, description, patientSteps, doctorStrip } = homeContent.howItWorks;
  const headingId = useId();
  const doctorHeadingId = useId();

  return (
    <section aria-labelledby={headingId} className="py-16 sm:py-24">
      <Container>
        <div className="max-w-2xl">
          <h2 id={headingId} className="text-2xl font-bold text-slate-900 sm:text-3xl">
            {heading}
          </h2>
          <p className="mt-3 text-base text-slate-600">{description}</p>
        </div>

        <div className="mt-10 grid gap-6 sm:grid-cols-2 lg:grid-cols-5">
          {patientSteps.map((step, index) => (
            <StepCard
              key={step.title}
              step={index + 1}
              title={step.title}
              description={step.description}
            />
          ))}
        </div>

        <div
          aria-labelledby={doctorHeadingId}
          className="mt-12 rounded-2xl bg-gradient-to-br from-brand-50 to-brand-100/50 p-6 ring-1 ring-brand-100 sm:p-8"
        >
          <h3 id={doctorHeadingId} className="text-lg font-semibold text-slate-900">
            {doctorStrip.heading}
          </h3>
          <p className="mt-1 text-sm text-slate-600">{doctorStrip.description}</p>

          <ol className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {doctorStrip.steps.map((step, index) => (
              <li
                key={step.title}
                className="rounded-xl bg-white p-4 shadow-elevation-1 transition-shadow duration-300 hover:shadow-elevation-2"
              >
                <span
                  aria-hidden="true"
                  className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-brand-700 text-xs font-bold text-white"
                >
                  {index + 1}
                </span>
                <p className="mt-2 font-medium text-slate-900">{step.title}</p>
                <p className="mt-1 text-sm text-slate-600">{step.description}</p>
              </li>
            ))}
          </ol>
        </div>
      </Container>
    </section>
  );
}
