import { Mail, PhoneCall } from "lucide-react";
import { Container } from "@/components/ui/Container";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { homeContent } from "@/content/home";
import { siteContent } from "@/content/site";

export function FinalCtaSection() {
  const { heading, description, cta } = homeContent.finalCta;

  return (
    <section aria-labelledby="final-cta-heading" className="py-16 sm:py-24">
      <Container>
        <div className="rounded-2xl bg-gradient-to-br from-brand-50 to-brand-100/50 px-6 py-12 text-center shadow-elevation-2 ring-1 ring-brand-100 sm:px-12">
          <h2 id="final-cta-heading" className="text-2xl font-bold text-slate-900 sm:text-3xl">
            {heading}
          </h2>
          <p className="mx-auto mt-3 max-w-xl text-base text-slate-600">{description}</p>

          <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
            <LinkButton href={cta.href} variant="primary" size="lg">
              {cta.label}
            </LinkButton>
          </div>

          <div className="mt-8 flex flex-wrap items-center justify-center gap-6 text-sm text-slate-600">
            <a
              href={`mailto:${siteContent.contact.email}`}
              className="inline-flex items-center gap-2 hover:text-brand-700"
            >
              <Icon icon={Mail} size="sm" />
              {siteContent.contact.email}
            </a>
            <a
              href={`tel:${siteContent.contact.phone}`}
              className="inline-flex items-center gap-2 hover:text-brand-700"
            >
              <Icon icon={PhoneCall} size="sm" />
              {siteContent.contact.phone}
            </a>
          </div>
        </div>
      </Container>
    </section>
  );
}
