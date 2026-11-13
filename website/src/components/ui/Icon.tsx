import type { LucideIcon } from "lucide-react";

const SIZES = { sm: 16, md: 20, lg: 24 } as const;

export interface IconProps {
  icon: LucideIcon;
  size?: keyof typeof SIZES;
  className?: string;
  /**
   * Only set this when the icon is the *sole* conveyor of meaning (no
   * adjacent visible text says the same thing) -- it gives the icon an
   * accessible name. Omit it for a decorative icon sitting next to a
   * visible label (the common case): it's then hidden from the
   * accessibility tree instead, so screen readers don't announce it twice.
   */
  label?: string;
}

/** Thin, consistent wrapper around lucide-react icons -- fixed size steps, and the right accessibility treatment depending on whether the icon carries meaning on its own. */
export function Icon({ icon: LucideIconComponent, size = "md", className, label }: IconProps) {
  const px = SIZES[size];
  if (label) {
    return (
      <LucideIconComponent
        width={px}
        height={px}
        className={className}
        role="img"
        aria-label={label}
      />
    );
  }
  return (
    <LucideIconComponent
      width={px}
      height={px}
      className={className}
      aria-hidden="true"
      focusable="false"
    />
  );
}
