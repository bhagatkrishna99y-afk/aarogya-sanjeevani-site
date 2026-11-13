import { useId, type ReactNode } from "react";
import { Container } from "./Container";

export interface SectionProps {
  /** Small label above the heading, e.g. "HOW IT WORKS". */
  eyebrow?: string;
  heading?: ReactNode;
  /** Which heading level to render -- most sections should stay at the default (a page typically has exactly one h1 elsewhere, in its own hero). */
  headingLevel?: "h1" | "h2" | "h3";
  description?: ReactNode;
  children?: ReactNode;
  className?: string;
  /** Extra classes for the inner Container, if a section needs a narrower/wider measure than the page default. */
  containerClassName?: string;
  /** Overrides the auto-generated id used for `aria-labelledby` -- only needed if something else (an in-page anchor link, say) must target this section's heading by a stable, known id. */
  headingId?: string;
}

/**
 * A page section with a consistent vertical rhythm and an optional
 * eyebrow/heading/description header block above its content. When a
 * `heading` is given, the outer `<section>` gets `aria-labelledby`
 * pointing at it -- the standard pattern for a landmark whose accessible
 * name comes from its own visible heading, rather than a redundant
 * `aria-label` duplicating text already on the page.
 */
export function Section({
  eyebrow,
  heading,
  headingLevel = "h2",
  description,
  children,
  className,
  containerClassName,
  headingId,
}: SectionProps) {
  const Heading = headingLevel;
  const hasHeader = eyebrow || heading || description;
  const generatedId = useId();
  const resolvedHeadingId = headingId ?? generatedId;

  return (
    <section
      aria-labelledby={heading ? resolvedHeadingId : undefined}
      className={["py-16 sm:py-24", className].filter(Boolean).join(" ")}
    >
      <Container className={containerClassName}>
        {hasHeader && (
          <div className="max-w-2xl">
            {eyebrow && (
              <p className="text-sm font-semibold uppercase tracking-wide text-brand-700">
                {eyebrow}
              </p>
            )}
            {heading && (
              <Heading
                id={resolvedHeadingId}
                className="mt-2 text-2xl font-bold text-slate-900 sm:text-3xl"
              >
                {heading}
              </Heading>
            )}
            {description && <p className="mt-3 text-base text-slate-600">{description}</p>}
          </div>
        )}
        {children && <div className={hasHeader ? "mt-10" : undefined}>{children}</div>}
      </Container>
    </section>
  );
}
