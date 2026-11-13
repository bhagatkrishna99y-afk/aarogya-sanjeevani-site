import type { HTMLAttributes, ReactNode } from "react";

export type BadgeVariant =
  "neutral" | "brand" | "accent" | "success" | "warning" | "error" | "info";

// Every variant is a verified-AA soft-surface + ink pair from docs/design-tokens.md.
const variantClasses: Record<BadgeVariant, string> = {
  neutral: "bg-slate-100 text-slate-700",
  brand: "bg-brand-50 text-brand-700",
  accent: "bg-accent-50 text-accent-800",
  success: "bg-success-50 text-success-700",
  warning: "bg-warning-50 text-warning-700",
  error: "bg-error-50 text-error-700",
  info: "bg-info-50 text-info-700",
};

export interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  children: ReactNode;
}

/** A small status/category pill. Color alone never carries the meaning -- always pair with a visible word (the badge's own text), never an icon-only/color-only badge. */
export function Badge({ variant = "neutral", className, children, ...props }: BadgeProps) {
  return (
    <span
      className={[
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium",
        variantClasses[variant],
        className,
      ]
        .filter(Boolean)
        .join(" ")}
      {...props}
    >
      {children}
    </span>
  );
}
