import type { HTMLAttributes } from "react";

export interface SkeletonProps extends HTMLAttributes<HTMLDivElement> {}

/**
 * A loading placeholder. The pulse animation is pure opacity/`animate-pulse`
 * (a Tailwind utility backed by a CSS animation), which the global
 * `prefers-reduced-motion` rule in src/styles/index.css already slows to
 * effectively nothing -- no extra work needed here. Purely decorative, so
 * it's `aria-hidden`; pair it with a `LoadingState`/`aria-live` region
 * (see StatusViews.tsx) when a screen reader also needs to know
 * something is loading.
 */
export function Skeleton({ className, ...props }: SkeletonProps) {
  return (
    <div
      aria-hidden="true"
      className={["animate-pulse rounded-md bg-slate-200", className].filter(Boolean).join(" ")}
      {...props}
    />
  );
}
