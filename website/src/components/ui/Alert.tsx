import type { ReactNode } from "react";
import { CheckCircle2, Info, TriangleAlert, CircleX, X } from "lucide-react";
import { Icon } from "./Icon";

export type AlertVariant = "success" | "warning" | "error" | "info";

const config: Record<
  AlertVariant,
  { icon: typeof Info; classes: string; role: "alert" | "status" }
> = {
  success: {
    icon: CheckCircle2,
    classes: "border-success-200 bg-success-50 text-success-800",
    role: "status",
  },
  info: { icon: Info, classes: "border-info-200 bg-info-50 text-info-800", role: "status" },
  warning: {
    icon: TriangleAlert,
    classes: "border-warning-200 bg-warning-50 text-warning-900",
    role: "alert",
  },
  error: { icon: CircleX, classes: "border-error-200 bg-error-50 text-error-800", role: "alert" },
};

export interface AlertProps {
  variant: AlertVariant;
  title?: ReactNode;
  children: ReactNode;
  onDismiss?: () => void;
  className?: string;
}

/**
 * `warning`/`error` use `role="alert"` (interrupts screen readers
 * immediately -- something needs the user's attention now); `success`/
 * `info` use the gentler `role="status"`. The icon is always decorative
 * (the colored border/background plus the variant word in `title`/
 * `children` already carry the meaning -- never rely on the icon or
 * color alone).
 */
export function Alert({ variant, title, children, onDismiss, className }: AlertProps) {
  const { icon, classes, role } = config[variant];
  return (
    <div
      role={role}
      className={["flex gap-3 rounded-lg border p-4", classes, className].filter(Boolean).join(" ")}
    >
      <Icon icon={icon} className="mt-0.5 shrink-0" />
      <div className="flex-1 text-sm">
        {title && <p className="font-semibold">{title}</p>}
        <div className={title ? "mt-1" : undefined}>{children}</div>
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          aria-label="Dismiss"
          className="-m-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-md hover:bg-black/5"
        >
          <Icon icon={X} size="sm" />
        </button>
      )}
    </div>
  );
}
