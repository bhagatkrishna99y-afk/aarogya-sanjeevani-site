import type { LabelHTMLAttributes, ReactNode } from "react";

export interface LabelProps extends LabelHTMLAttributes<HTMLLabelElement> {
  children: ReactNode;
  required?: boolean;
}

export function Label({ required, children, className, ...props }: LabelProps) {
  return (
    <label
      className={["text-sm font-medium text-slate-700", className].filter(Boolean).join(" ")}
      {...props}
    >
      {children}
      {required && (
        <>
          <span aria-hidden="true" className="text-error-700">
            {" "}
            *
          </span>
          <span className="sr-only"> (required)</span>
        </>
      )}
    </label>
  );
}
