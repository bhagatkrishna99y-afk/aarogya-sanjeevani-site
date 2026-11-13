import { FeatureCard } from "@/components/ui/FeatureCard";
import { Section } from "@/components/ui/Section";
import { homeContent } from "@/content/home";

export function BenefitsSection() {
  return (
    <Section
      className="bg-white"
      eyebrow="Key benefits"
      heading="Everything you need to find the right care"
    >
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {homeContent.benefits.map((benefit) => (
          <FeatureCard
            key={benefit.title}
            icon={benefit.icon}
            title={benefit.title}
            description={benefit.description}
          />
        ))}
      </div>
    </Section>
  );
}
