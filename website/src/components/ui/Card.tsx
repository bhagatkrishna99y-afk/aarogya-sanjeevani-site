import type { HTMLAttributes, ReactNode } from "react";

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

/** Generic bordered, padded surface -- the base every other card-shaped component builds on. */
export function Card({ className, children, ...props }: CardProps) {
  return (
    <div
      className={[
        "rounded-2xl border border-slate-200/70 bg-white p-6 shadow-elevation-2",
        "transition-shadow duration-300 hover:shadow-elevation-3",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
      {...props}
    >
      {children}
    </div>
  );
}
