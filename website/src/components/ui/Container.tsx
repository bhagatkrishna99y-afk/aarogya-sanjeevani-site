import type { ElementType, HTMLAttributes, ReactNode } from "react";

export interface ContainerProps extends HTMLAttributes<HTMLElement> {
  as?: ElementType;
  children: ReactNode;
}

/** Centered max-width wrapper with consistent horizontal padding -- the one place page width/gutters are decided. */
export function Container({ as: Tag = "div", className, children, ...props }: ContainerProps) {
  return (
    <Tag
      className={["mx-auto max-w-6xl px-4 sm:px-6", className].filter(Boolean).join(" ")}
      {...props}
    >
      {children}
    </Tag>
  );
}
