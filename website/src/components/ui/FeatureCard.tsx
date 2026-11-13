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
      <span className="inline-flex h-10 w-10 items-center justify-center rounded-lg bg-brand-50 text-brand-700">
        <Icon icon={icon} />
      </span>
      <h3 className="mt-4 font-semibold text-slate-900">{title}</h3>
      <p className="mt-1 text-sm text-slate-600">{description}</p>
    </Card>
  );
}
