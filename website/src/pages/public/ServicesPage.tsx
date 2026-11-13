import { Alert } from "@/components/ui/Alert";
import { Badge } from "@/components/ui/Badge";
import { Card } from "@/components/ui/Card";
import { Container } from "@/components/ui/Container";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { Section } from "@/components/ui/Section";
import { StepCard } from "@/components/ui/StepCard";
import { Seo } from "@/components/seo/Seo";
import { SERVICE_MODE_LABEL, servicesContent } from "@/content/services";

export function ServicesPage() {
  const { intro, categories, howRequesting, emergencyNotice, appCta } = servicesContent;

  return (
    <div>
      <Seo
        title="Services"
        description="An overview of the kinds of healthcare services available through Aarogya Sanjeevani, requested from the Patient app."
      />

      <section aria-labelledby="services-intro-heading" className="py-16 sm:py-24">
        <Container>
          <div className="max-w-2xl">
            <h1
              id="services-intro-heading"
              className="text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl"
            >
              {intro.heading}
            </h1>
            <p className="mt-4 text-lg text-slate-600">{intro.paragraph}</p>
          </div>
        </Container>
      </section>

      {categories.map((category, index) => (
        <Section
          key={category.name}
          className={index % 2 === 0 ? "bg-white" : undefined}
          heading={category.name}
          description={category.description}
        >
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {category.examples.map((example) => (
              <li key={example.name}>
                <Card>
                  <span className="inline-flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
                    <Icon icon={category.icon} />
                  </span>
                  <p className="mt-4 font-semibold text-slate-900">{example.name}</p>
                  <p className="mt-1 text-sm text-slate-600">{example.description}</p>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {example.modes.map((mode) => (
                      <Badge key={mode} variant="brand">
                        {SERVICE_MODE_LABEL[mode]}
                      </Badge>
                    ))}
                  </div>
                </Card>
              </li>
            ))}
          </ul>
        </Section>
      ))}

      <Section
        className={categories.length % 2 === 0 ? "bg-white" : undefined}
        heading={howRequesting.heading}
        description={howRequesting.description}
      >
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {howRequesting.steps.map((step, index) => (
            <StepCard
              key={step.title}
              step={index + 1}
              title={step.title}
              description={step.description}
            />
          ))}
        </div>
      </Section>

      <section aria-label="Emergency notice" className="py-8">
        <Container>
          <Alert variant="warning" title={emergencyNotice.title}>
            <p>{emergencyNotice.body}</p>
          </Alert>
        </Container>
      </section>

      <section aria-labelledby="services-app-cta-heading" className="py-16 sm:py-24">
        <Container>
          <div className="rounded-2xl bg-brand-50 px-6 py-12 text-center sm:px-12">
            <h2
              id="services-app-cta-heading"
              className="text-2xl font-bold text-slate-900 sm:text-3xl"
            >
              {appCta.heading}
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-base text-slate-600">{appCta.description}</p>

            <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
              <LinkButton href={appCta.patient.href} variant="primary" size="lg">
                {appCta.patient.label}
              </LinkButton>
              <LinkButton href={appCta.doctor.href} variant="secondary" size="lg">
                {appCta.doctor.label}
              </LinkButton>
            </div>
          </div>
        </Container>
      </section>
    </div>
  );
}
