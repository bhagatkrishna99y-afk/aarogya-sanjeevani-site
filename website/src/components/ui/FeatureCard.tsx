import type { LucideIcon } from "lucide-react";
import { Card } from "./Card";
import { Icon } from "./Icon";

export interface FeatureCardProps {
  icon: LucideIcon;
  title: string;
  description: string;
  className?: string;
}

/** Icon + title + description -- for "what this product does" style feature grids. */
export function FeatureCard({ icon, title, description, className }: FeatureCardProps) {
  return (
    <Card className={className}>
      <span className="inline-flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-brand-50 to-brand-100 text-brand-700 ring-1 ring-brand-100">
        <Icon icon={icon} />
      </span>
      <h3 className="mt-5 font-semibold text-slate-900">{title}</h3>
      <p className="mt-1.5 text-sm leading-relaxed text-slate-600">{description}</p>
    </Card>
  );
}
