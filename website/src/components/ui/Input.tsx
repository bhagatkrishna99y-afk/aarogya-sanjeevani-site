import { forwardRef, type InputHTMLAttributes } from "react";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { invalid, className, ...props },
  ref,
) {
  return (
    <input
      ref={ref}
      aria-invalid={invalid || undefined}
      className={[
        "block min-h-[48px] w-full rounded-md border px-3 text-sm text-slate-900 placeholder:text-slate-400",
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
