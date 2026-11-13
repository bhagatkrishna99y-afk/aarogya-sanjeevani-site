import { cloneElement, isValidElement, useId, type ReactElement, type ReactNode } from "react";
import { Label } from "./Label";
import { FieldError } from "./FieldError";

export interface FormFieldProps {
  label: ReactNode;
  required?: boolean;
  error?: ReactNode;
  helperText?: ReactNode;
  /** A single Input/Textarea/Select element. Its `id`/`aria-describedby`/`invalid` are wired up automatically. */
  children: ReactElement<{ id?: string; "aria-describedby"?: string; invalid?: boolean }>;
  className?: string;
}

/**
 * Wires a Label, a single form control (Input/Textarea/Select), and its
 * FieldError/helper text together with matching ids -- so nobody has to
 * remember `htmlFor`/`aria-describedby` by hand each time. Using Label +
 * the control directly (without FormField) is still fully supported for
 * custom layouts; this is the convenience path for the common case.
 */
export function FormField({
  label,
  required,
  error,
  helperText,
  children,
  className,
}: FormFieldProps) {
  const inputId = useId();
  const errorId = useId();
  const helperId = useId();
  const describedBy =
    [error ? errorId : null, helperText ? helperId : null].filter(Boolean).join(" ") || undefined;

  return (
    <div className={className}>
      <Label htmlFor={inputId} required={required}>
        {label}
      </Label>
      <div className="mt-1">
        {isValidElement(children)
          ? cloneElement(children, {
              id: inputId,
              "aria-describedby": describedBy,
              invalid: Boolean(error),
            })
          : children}
      </div>
      {helperText && !error && (
        <p id={helperId} className="mt-1 text-sm text-slate-500">
          {helperText}
        </p>
      )}
      <FieldError id={errorId}>{error}</FieldError>
    </div>
  );
}
