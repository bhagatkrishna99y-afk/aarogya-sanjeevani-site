import { forwardRef, type ButtonHTMLAttributes } from "react";
import { Spinner } from "../FullPageSpinner";
import { buttonClasses, type ButtonSize, type ButtonVariant } from "./button-styles";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant;
  size?: ButtonSize;
  /** Shows a spinner and disables the button, while keeping its label in the DOM for screen readers (never replace the label with just a spinner). */
  loading?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  {
    variant = "primary",
    size = "md",
    loading = false,
    disabled,
    className,
    type = "button",
    children,
    ...props
  },
  ref,
) {
  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={buttonClasses(variant, size, className)}
      {...props}
    >
      {loading && (
        <Spinner
          className={
            variant === "secondary" || variant === "ghost" ? "h-4 w-4" : "h-4 w-4 text-white"
          }
        />
      )}
      {children}
    </button>
  );
});
