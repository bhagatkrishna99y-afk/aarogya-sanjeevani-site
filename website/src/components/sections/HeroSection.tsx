import { Container } from "@/components/ui/Container";
import { LinkButton } from "@/components/ui/LinkButton";
import { homeContent } from "@/content/home";
import heroIllustration from "@/assets/hero-illustration.svg";

/**
 * The page's own `<h1>` lives here (not in `Seo`/layout) -- `id="hero-heading"`
 * both labels this `<section>` via `aria-labelledby` and is what
 * `RouteFocusManager` focuses on navigation.
 */
export function HeroSection() {
  const { headline, subheading, primaryCta, secondaryCta, trustLine } = homeContent.hero;

  return (
    <section aria-labelledby="hero-heading" className="py-16 sm:py-24">
      <Container>
        <div className="grid items-center gap-10 lg:grid-cols-2">
          <div>
            <h1
              id="hero-heading"
              className="text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl"
            >
              {headline}
            </h1>
            <p className="mt-4 max-w-xl text-lg text-slate-600">{subheading}</p>

            <div className="mt-8 flex flex-wrap gap-3">
              <LinkButton href={primaryCta.href} variant="primary" size="lg">
                {primaryCta.label}
              </LinkButton>
              <LinkButton href={secondaryCta.href} variant="secondary" size="lg">
                {secondaryCta.label}
              </LinkButton>
            </div>

            <p className="mt-6 text-sm font-medium text-slate-500">{trustLine}</p>
          </div>

          <div className="mx-auto w-full max-w-md lg:max-w-none">
            {/* Above the fold -- the first meaningful visual on the page,
                so not lazy-loaded (lazy-loading something this high up
                would delay it, not help). */}
            <img
              src={heroIllustration}
              alt="Illustration of a shield with a heartbeat line and a small care cross, representing secure, connected healthcare"
              width={480}
              height={420}
              className="h-auto w-full"
            />
          </div>
        </div>
      </Container>
    </section>
  );
}
