import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { Section } from "@/components/ui/Section";
import { homeContent } from "@/content/home";

export function ServicesSection() {
  const { heading, description, categories, cta } = homeContent.services;

  return (
    <Section
      className="bg-white"
      eyebrow="Main services"
      heading={heading}
      description={description}
    >
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {categories.map((category) => (
          <Card key={category.name}>
            <span className="inline-flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
              <Icon icon={category.icon} />
            </span>
            <h3 className="mt-4 font-semibold text-slate-900">{category.name}</h3>
            <p className="mt-1 text-sm text-slate-600">{category.description}</p>
          </Card>
        ))}
      </div>

      <div className="mt-10">
        <LinkButton to={cta.to} variant="secondary" size="lg">
          {cta.label}
        </LinkButton>
      </div>
    </Section>
  );
}
