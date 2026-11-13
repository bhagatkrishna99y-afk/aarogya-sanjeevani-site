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
      <span className="text-sm font-semibold text-brand-700">Step {step}</span>
      <h3 className="mt-2 font-semibold text-slate-900">{title}</h3>
      <p className="mt-1 text-sm text-slate-600">{description}</p>
    </Card>
  );
}
