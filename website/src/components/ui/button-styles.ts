/**
 * Shared visual variants between Button and LinkButton so a link that
 * looks like a button and an actual button are pixel-identical. Every
 * variant's text/background pairing is one of the verified-AA pairs from
 * docs/design-tokens.md -- never swap in a lighter shade here without
 * re-checking that doc.
 */
export type ButtonVariant = "primary" | "secondary" | "ghost" | "accent";
export type ButtonSize = "sm" | "md" | "lg";

const base =
  "inline-flex items-center justify-center gap-2 rounded-md font-medium transition-colors " +
  "disabled:cursor-not-allowed disabled:opacity-50";

const variantClasses: Record<ButtonVariant, string> = {
  primary: "bg-brand-700 text-white hover:bg-brand-800",
  secondary: "border border-slate-300 bg-white text-slate-700 hover:bg-slate-50",
  ghost: "text-brand-700 hover:bg-brand-50",
  accent: "bg-accent-700 text-white hover:bg-accent-800",
};

// min-h-[48px]/[44px] guarantee the touch-target minimum regardless of
// how little text/padding a given size otherwise implies.
const sizeClasses: Record<ButtonSize, string> = {
  sm: "min-h-[44px] px-3 text-sm",
  md: "min-h-[48px] px-4 text-sm",
  lg: "min-h-[48px] px-6 text-base",
};

export function buttonClasses(variant: ButtonVariant, size: ButtonSize, extra?: string): string {
  return [base, variantClasses[variant], sizeClasses[size], extra].filter(Boolean).join(" ");
}
