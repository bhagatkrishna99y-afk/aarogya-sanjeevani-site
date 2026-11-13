import { Card } from "@/components/ui/Card";
import { Container } from "@/components/ui/Container";
import { FeatureCard } from "@/components/ui/FeatureCard";
import { LinkButton } from "@/components/ui/LinkButton";
import { Section } from "@/components/ui/Section";
import { Seo } from "@/components/seo/Seo";
import { aboutContent } from "@/content/about";
import { DoctorProfileCard } from "@/components/sections/DoctorProfileCard";

export function AboutPage() {
  const { intro, missionVision, approach, values, safety, closingCta } = aboutContent;

  return (
    <div>
      <Seo
        title="About"
        description="Who Aarogya Sanjeevani is, why it exists, and how we approach healthcare discovery, requests and safety."
      />

      <section aria-labelledby="about-intro-heading" className="py-16 sm:py-24">
        <Container>
          <div className="max-w-2xl">
            <h1
              id="about-intro-heading"
              className="text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl"
            >
              {intro.heading}
            </h1>
            {intro.paragraphs.map((paragraph) => (
              <p key={paragraph} className="mt-4 text-lg text-slate-600">
                {paragraph}
              </p>
            ))}
          </div>
        </Container>
      </section>

      <Section className="bg-white" heading="Mission and vision">
        <div className="grid gap-6 sm:grid-cols-2">
          <Card>
            <h3 className="font-semibold text-slate-900">{missionVision.mission.heading}</h3>
            <p className="mt-2 text-sm text-slate-600">{missionVision.mission.body}</p>
          </Card>
          <Card>
            <h3 className="font-semibold text-slate-900">{missionVision.vision.heading}</h3>
            <p className="mt-2 text-sm text-slate-600">{missionVision.vision.body}</p>
          </Card>
        </div>
      </Section>

      <Section eyebrow="Our approach" heading={approach.heading} description={approach.description}>
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {approach.items.map((item) => (
            <FeatureCard
              key={item.title}
              icon={item.icon}
              title={item.title}
              description={item.description}
            />
          ))}
        </div>
      </Section>

      <Section
        className="bg-white"
        eyebrow="Our values"
        heading={values.heading}
        description={values.description}
      >
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {values.items.map((item) => (
            <FeatureCard
              key={item.title}
              icon={item.icon}
              title={item.title}
              description={item.description}
            />
          ))}
        </div>
      </Section>

      <Section eyebrow="Platform safety" heading={safety.heading} description={safety.description}>
        <dl className="grid gap-6 sm:grid-cols-3">
          {safety.points.map((point) => (
            <div key={point.title} className="rounded-xl border border-slate-200 bg-white p-5">
              <dt className="font-semibold text-slate-900">{point.title}</dt>
              <dd className="mt-1 text-sm text-slate-600">{point.description}</dd>
            </div>
          ))}
        </dl>
      </Section>

      <Section eyebrow="Our team" heading="Meet the doctor">
        <DoctorProfileCard />
      </Section>

      <section aria-labelledby="about-cta-heading" className="py-16 sm:py-24">
        <Container>
          <div className="rounded-2xl bg-gradient-to-br from-brand-50 to-brand-100/50 px-6 py-12 text-center shadow-elevation-2 ring-1 ring-brand-100 sm:px-12">
            <h2 id="about-cta-heading" className="text-2xl font-bold text-slate-900 sm:text-3xl">
              {closingCta.heading}
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-base text-slate-600">
              {closingCta.description}
            </p>

            <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
              <LinkButton href={closingCta.cta.href} variant="primary" size="lg">
                {closingCta.cta.label}
              </LinkButton>
              <LinkButton to={closingCta.secondaryCta.to} variant="secondary" size="lg">
                {closingCta.secondaryCta.label}
              </LinkButton>
            </div>
          </div>
        </Container>
      </section>
    </div>
  );
}
