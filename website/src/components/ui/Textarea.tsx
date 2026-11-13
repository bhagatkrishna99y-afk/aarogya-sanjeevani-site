import { forwardRef, type TextareaHTMLAttributes } from "react";

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { invalid, className, rows = 4, ...props },
  ref,
) {
  return (
    <textarea
      ref={ref}
      rows={rows}
      aria-invalid={invalid || undefined}
      className={[
        "block w-full rounded-md border px-3 py-2 text-sm text-slate-900 placeholder:text-slate-400",
        invalid
          ? "border-error-600 focus:border-error-600"
          : "border-slate-300 focus:border-brand-500",
        className,
      ]
        .filter(Boolean)
        .join(" ")}
      {...props}
    />
  );
});
