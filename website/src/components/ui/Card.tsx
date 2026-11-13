import type { HTMLAttributes, ReactNode } from "react";

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

/** Generic bordered, padded surface -- the base every other card-shaped component builds on. */
export function Card({ className, children, ...props }: CardProps) {
  return (
    <div
      className={["rounded-xl border border-slate-200 bg-white p-5 shadow-sm", className]
        .filter(Boolean)
        .join(" ")}
      {...props}
    >
      {children}
    </div>
  );
}
