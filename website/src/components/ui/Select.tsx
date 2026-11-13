import { forwardRef, type SelectHTMLAttributes } from "react";
import { ChevronDown } from "lucide-react";
import { Icon } from "./Icon";

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  invalid?: boolean;
}

/** A native `<select>` -- intentionally not a custom listbox: native selects already give correct keyboard behavior, screen reader support and mobile OS pickers for free. */
export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { invalid, className, children, ...props },
  ref,
) {
  return (
    <div className="relative">
      <select
        ref={ref}
        aria-invalid={invalid || undefined}
        className={[
          "block min-h-[48px] w-full appearance-none rounded-md border bg-white px-3 pr-10 text-sm text-slate-900",
          invalid
            ? "border-error-600 focus:border-error-600"
            : "border-slate-300 focus:border-brand-500",
          className,
        ]
          .filter(Boolean)
          .join(" ")}
        {...props}
      >
        {children}
      </select>
      <Icon
        icon={ChevronDown}
        className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-slate-500"
      />
    </div>
  );
});
