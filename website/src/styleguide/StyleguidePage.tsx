import { useState } from "react";
import { Heart, Mail, ShieldCheck } from "lucide-react";
import { Seo } from "@/components/seo/Seo";
import { Section } from "@/components/ui/Section";
import { Button } from "@/components/ui/Button";
import { LinkButton } from "@/components/ui/LinkButton";
import { Card } from "@/components/ui/Card";
import { FeatureCard } from "@/components/ui/FeatureCard";
import { StepCard } from "@/components/ui/StepCard";
import { Badge, type BadgeVariant } from "@/components/ui/Badge";
import { Accordion, AccordionItem } from "@/components/ui/Accordion";
import { Label } from "@/components/ui/Label";
import { Input } from "@/components/ui/Input";
import { Textarea } from "@/components/ui/Textarea";
import { Select } from "@/components/ui/Select";
import { FieldError } from "@/components/ui/FieldError";
import { FormField } from "@/components/ui/FormField";
import { Alert, type AlertVariant } from "@/components/ui/Alert";
import { Skeleton } from "@/components/ui/Skeleton";
import { Icon } from "@/components/ui/Icon";
import { StoreBadge } from "@/components/ui/StoreBadge";
import { Breadcrumbs } from "@/components/ui/Breadcrumbs";
import { contrastRatio, formatRatio, passesAA, type ContrastBar } from "./contrast";

const COLOR_SCALES = {
  brand: [
    "#f0fdfa",
    "#ccfbf1",
    "#99f6e4",
    "#5eead4",
    "#2dd4bf",
    "#14b8a6",
    "#0d9488",
    "#0f766e",
    "#115e59",
    "#134e4a",
  ],
  accent: [
    "#fff7ed",
    "#ffedd5",
    "#fed7aa",
    "#fdba74",
    "#fb923c",
    "#f97316",
    "#ea580c",
    "#c2410c",
    "#9a3412",
    "#7c2d12",
  ],
  success: [
    "#ecfdf5",
    "#d1fae5",
    "#a7f3d0",
    "#6ee7b7",
    "#34d399",
    "#10b981",
    "#059669",
    "#047857",
    "#065f46",
    "#064e3b",
  ],
  warning: [
    "#fffbeb",
    "#fef3c7",
    "#fde68a",
    "#fcd34d",
    "#fbbf24",
    "#f59e0b",
    "#d97706",
    "#b45309",
    "#92400e",
    "#78350f",
  ],
  error: [
    "#fef2f2",
    "#fee2e2",
    "#fecaca",
    "#fca5a5",
    "#f87171",
    "#ef4444",
    "#dc2626",
    "#b91c1c",
    "#991b1b",
    "#7f1d1d",
  ],
  info: [
    "#f0f9ff",
    "#e0f2fe",
    "#bae6fd",
    "#7dd3fc",
    "#38bdf8",
    "#0ea5e9",
    "#0284c7",
    "#0369a1",
    "#075985",
    "#0c4a6e",
  ],
} as const;
const SHADE_LABELS = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900];

const TEXT_PAIRINGS: { label: string; fg: string; bg: string; bar: ContrastBar }[] = [
  { label: "brand-700 text on white", fg: "#0f766e", bg: "#ffffff", bar: 4.5 },
  { label: "white text on brand-700 (solid button)", fg: "#ffffff", bg: "#0f766e", bar: 4.5 },
  {
    label: "white text on brand-600 (fails -- decorative use only)",
    fg: "#ffffff",
    bg: "#0d9488",
    bar: 4.5,
  },
  { label: "white text on accent-700 (solid button)", fg: "#ffffff", bg: "#c2410c", bar: 4.5 },
  { label: "accent-900 text on accent-50", fg: "#7c2d12", bg: "#fff7ed", bar: 4.5 },
  { label: "success-700 text on success-50", fg: "#047857", bg: "#ecfdf5", bar: 4.5 },
  {
    label: "warning-700 text on warning-50 (tightest margin)",
    fg: "#b45309",
    bg: "#fffbeb",
    bar: 4.5,
  },
  { label: "error-700 text on error-50", fg: "#b91c1c", bg: "#fef2f2", bar: 4.5 },
  { label: "info-700 text on info-50", fg: "#0369a1", bg: "#f0f9ff", bar: 4.5 },
  { label: "slate-900 text on white", fg: "#0f172a", bg: "#ffffff", bar: 4.5 },
  { label: "slate-600 text on white", fg: "#475569", bg: "#ffffff", bar: 4.5 },
  { label: "slate-500 text on white (slim margin)", fg: "#64748b", bg: "#ffffff", bar: 4.5 },
  {
    label: "brand-600 as a border/icon on white (non-text, 3:1)",
    fg: "#0d9488",
    bg: "#ffffff",
    bar: 3,
  },
];

const BADGE_VARIANTS: BadgeVariant[] = [
  "neutral",
  "brand",
  "accent",
  "success",
  "warning",
  "error",
  "info",
];
const ALERT_VARIANTS: AlertVariant[] = ["success", "info", "warning", "error"];

function ColorScale({ name, shades }: { name: string; shades: readonly string[] }) {
  return (
    <div>
      <p className="font-mono text-sm font-semibold text-slate-900">{name}</p>
      <div className="mt-2 grid grid-cols-5 gap-2 sm:grid-cols-10">
        {shades.map((hex, i) => (
          <div key={hex} className="text-center">
            <div
              className="h-12 w-full rounded-md border border-slate-200"
              style={{ backgroundColor: hex }}
            />
            <p className="mt-1 text-[11px] text-slate-500">{SHADE_LABELS[i]}</p>
            <p className="font-mono text-[10px] text-slate-500">{hex}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function ContrastRow({ label, fg, bg, bar }: (typeof TEXT_PAIRINGS)[number]) {
  const ratio = contrastRatio(fg, bg);
  const pass = passesAA(ratio, bar);
  return (
    <div className="flex items-center justify-between gap-4 border-b border-slate-100 py-2 text-sm last:border-0">
      <div className="flex items-center gap-3">
        <span
          className="flex h-8 w-16 shrink-0 items-center justify-center rounded border border-slate-200 text-xs font-medium"
          style={{ backgroundColor: bg, color: fg }}
        >
          Aa
        </span>
        <span className="text-slate-700">{label}</span>
      </div>
      <span
        className={`font-mono text-xs font-semibold ${pass ? "text-success-700" : "text-error-700"}`}
      >
        {formatRatio(ratio)} ({bar}:1 min) {pass ? "PASS" : "FAIL"}
      </span>
    </div>
  );
}

/**
 * Dev-only component gallery: every color/type style and a live instance
 * of every reusable component, in every variant. Not registered in the
 * production route tree at all (see src/app/router.tsx) -- this whole
 * module is only ever imported when import.meta.env.DEV is true, so it
 * doesn't ship in a production bundle either.
 */
export function StyleguidePage() {
  const [accentLoading, setAccentLoading] = useState(false);

  return (
    <div className="bg-white">
      <Seo title="Styleguide (dev only)" noIndex />

      <Section
        eyebrow="Internal · development only"
        heading="Aarogya Sanjeevani styleguide"
        description="Every color, type style and reusable component, with live computed WCAG contrast ratios. See docs/design-tokens.md for the full written rationale."
      />

      <Section eyebrow="Tokens" heading="Color scales">
        <div className="space-y-8">
          {Object.entries(COLOR_SCALES).map(([name, shades]) => (
            <ColorScale key={name} name={name} shades={shades} />
          ))}
        </div>
      </Section>

      <Section
        eyebrow="Tokens"
        heading="Contrast verification"
        description="Computed live, not hand-typed -- if this page ever says PASS for a pairing that's actually broken, the bug is in the math, not the claim."
      >
        <div className="rounded-xl border border-slate-200 p-4">
          {TEXT_PAIRINGS.map((p) => (
            <ContrastRow key={p.label} {...p} />
          ))}
        </div>
      </Section>

      <Section eyebrow="Tokens" heading="Typography">
        <div className="space-y-4">
          <h1 className="text-5xl font-bold text-slate-900">Heading 1 / text-5xl</h1>
          <h2 className="text-3xl font-bold text-slate-900">Heading 2 / text-3xl</h2>
          <h3 className="text-2xl font-semibold text-slate-900">Heading 3 / text-2xl</h3>
          <h4 className="text-xl font-semibold text-slate-900">Heading 4 / text-xl</h4>
          <p className="max-w-2xl text-base leading-relaxed text-slate-700">
            Body text / text-base, leading-relaxed (1.625 line height). Set in Noto Sans, chosen for
            legibility over something more stylized -- healthcare copy needs to be read correctly,
            not just skimmed. The quick brown fox jumps over the lazy dog.
          </p>
          <p className="text-sm text-slate-600">
            Small / text-sm, for secondary text and captions.
          </p>
        </div>
      </Section>

      <Section eyebrow="Components" heading="Button">
        <div className="flex flex-wrap items-center gap-3">
          <Button variant="primary">Primary</Button>
          <Button variant="secondary">Secondary</Button>
          <Button variant="ghost">Ghost</Button>
          <Button variant="accent">Accent</Button>
        </div>
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <Button size="sm">Small</Button>
          <Button size="md">Medium</Button>
          <Button size="lg">Large</Button>
        </div>
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <Button
            variant="accent"
            loading={accentLoading}
            onClick={() => setAccentLoading((v) => !v)}
          >
            {accentLoading ? "Loading…" : "Click to toggle loading"}
          </Button>
          <Button variant="primary" disabled>
            Disabled
          </Button>
        </div>
      </Section>

      <Section eyebrow="Components" heading="LinkButton">
        <div className="flex flex-wrap items-center gap-3">
          <LinkButton to="/" variant="primary">
            Internal link
          </LinkButton>
          <LinkButton href="https://example.com" variant="secondary">
            External link
          </LinkButton>
        </div>
      </Section>

      <Section eyebrow="Components" heading="Card, FeatureCard, StepCard">
        <div className="grid gap-4 sm:grid-cols-3">
          <Card>
            <p className="font-semibold text-slate-900">Plain Card</p>
            <p className="mt-1 text-sm text-slate-600">
              The base every card-shaped component builds on.
            </p>
          </Card>
          <FeatureCard
            icon={ShieldCheck}
            title="FeatureCard"
            description="Icon + title + description."
          />
          <StepCard step={1} title="StepCard" description="Numbered step + title + description." />
        </div>
      </Section>

      <Section eyebrow="Components" heading="Badge">
        <div className="flex flex-wrap gap-2">
          {BADGE_VARIANTS.map((v) => (
            <Badge key={v} variant={v}>
              {v}
            </Badge>
          ))}
        </div>
      </Section>

      <Section
        eyebrow="Components"
        heading="Accordion"
        description="Click a question, or focus one and use Up/Down/Home/End."
      >
        <Accordion
          aria-label="Example FAQ"
          className="max-w-2xl rounded-xl border border-slate-200 px-4"
        >
          <AccordionItem id="a" question="Is this accordion keyboard operable?">
            Yes -- each question is a real button with aria-expanded/aria-controls, and arrow keys
            move focus between questions.
          </AccordionItem>
          <AccordionItem id="b" question="Can more than one item be open at once?">
            Only if the Accordion is given allowMultiple -- this example defaults to single-open.
          </AccordionItem>
          <AccordionItem id="c" question="Does it work without JavaScript-only hover tricks?">
            Yes -- state is plain React state, toggled by a real click/Enter/Space on a real button.
          </AccordionItem>
        </Accordion>
      </Section>

      <Section eyebrow="Components" heading="Form fields">
        <div className="grid max-w-2xl gap-6">
          <FormField label="Email" required helperText="We'll never share this.">
            <Input type="email" placeholder="you@example.com" />
          </FormField>
          <FormField label="Message" error="Please enter a message.">
            <Textarea placeholder="How can we help?" />
          </FormField>
          <div>
            <Label htmlFor="sg-select">Topic (composed by hand, without FormField)</Label>
            <div className="mt-1">
              <Select id="sg-select" defaultValue="">
                <option value="" disabled>
                  Choose one
                </option>
                <option value="billing">Billing</option>
                <option value="support">Support</option>
              </Select>
            </div>
            <FieldError>{null}</FieldError>
          </div>
        </div>
      </Section>

      <Section eyebrow="Components" heading="Alert">
        <div className="grid max-w-2xl gap-3">
          {ALERT_VARIANTS.map((v) => (
            <Alert key={v} variant={v} title={v[0]!.toUpperCase() + v.slice(1)}>
              This is an example {v} alert message.
            </Alert>
          ))}
        </div>
      </Section>

      <Section eyebrow="Components" heading="Skeleton">
        <div className="max-w-sm space-y-2">
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-4 w-1/2" />
          <Skeleton className="h-24 w-full" />
        </div>
      </Section>

      <Section eyebrow="Components" heading="Icon">
        <div className="flex items-center gap-6">
          <Icon icon={Heart} size="sm" label="Example small, meaningful icon" />
          <Icon icon={Mail} size="md" />
          <Icon icon={ShieldCheck} size="lg" />
        </div>
      </Section>

      <Section eyebrow="Components" heading="StoreBadge">
        <div className="flex flex-wrap gap-3">
          <StoreBadge store="google-play" href="https://play.google.com" appLabel="Patient app" />
          <StoreBadge store="app-store" href="https://apple.com" appLabel="Doctor app" />
        </div>
      </Section>

      <Section eyebrow="Components" heading="Breadcrumbs">
        <Breadcrumbs
          items={[
            { label: "Home", to: "/" },
            { label: "Services", to: "/services" },
            { label: "Styleguide" },
          ]}
        />
      </Section>
    </div>
  );
}
