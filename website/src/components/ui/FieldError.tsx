import type { ReactNode } from "react";

export interface FieldErrorProps {
  id?: string;
  children?: ReactNode;
}

/** Renders nothing when there's no error -- pass the same `id` as the field's `aria-describedby`. */
export function FieldError({ id, children }: FieldErrorProps) {
  if (!children) return null;
  return (
    <p id={id} role="alert" className="mt-1 text-sm text-error-700">
      {children}
    </p>
  );
}
