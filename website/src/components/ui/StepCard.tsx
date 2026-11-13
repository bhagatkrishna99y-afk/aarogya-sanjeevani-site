import { Card } from "./Card";

export interface StepCardProps {
  /** 1-based step number, rendered as "Step N". */
  step: number;
  title: string;
  description: string;
  className?: string;
}

/** A numbered step in a "how it works" sequence. */
export function StepCard({ step, title, description, className }: StepCardProps) {
  return (
    <Card className={className}>
      <span
        aria-hidden="true"
        className="inline-flex h-9 w-9 items-center justify-center rounded-full bg-gradient-to-br from-brand-600 to-brand-800 text-sm font-bold text-white shadow-elevation-1"
      >
        {step}
      </span>
      <p className="sr-only">Step {step}</p>
      <h3 className="mt-4 font-semibold text-slate-900">{title}</h3>
      <p className="mt-1.5 text-sm leading-relaxed text-slate-600">{description}</p>
    </Card>
  );
}
