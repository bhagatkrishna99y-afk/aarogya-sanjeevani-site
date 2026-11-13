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
            <span className="inline-flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-brand-50 to-brand-100 text-brand-700 ring-1 ring-brand-100">
              <Icon icon={category.icon} />
            </span>
            <h3 className="mt-5 font-semibold text-slate-900">{category.name}</h3>
            <p className="mt-1.5 text-sm leading-relaxed text-slate-600">{category.description}</p>
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
