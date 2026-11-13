import { Smartphone } from "lucide-react";
import { Icon } from "./Icon";

export type StoreName = "google-play" | "app-store";

const STORE_LABEL: Record<StoreName, { caption: string; name: string }> = {
  "google-play": { caption: "GET IT ON", name: "Google Play" },
  "app-store": { caption: "Download on the", name: "App Store" },
};

export interface StoreBadgeProps {
  store: StoreName;
  /** Omit (or pass an empty string) when the app isn't live yet on this store -- renders a "Coming soon" placeholder instead of a dead/guessed link. */
  href?: string;
  /** e.g. "Patient app" / "Doctor app" -- read for screen readers and shown nowhere else, since the badge itself doesn't have room for it. */
  appLabel: string;
  className?: string;
}

/**
 * A custom-styled store link -- deliberately **not** a reproduction of
 * Google's/Apple's official badge artwork. This project has no verified,
 * licensed copy of those assets to draw from, and guessing at one risks
 * a distorted, incorrectly-colored, or outright incorrect logo, which is
 * both a brand-usage problem and a worse user experience than a plain,
 * honest custom badge. If the real Play Store badge PNG/SVG ever gets
 * added to `src/assets/` from Google's own asset kit, swap this
 * component's markup for an `<img>` of that file; until then, this is the
 * actual button, not a placeholder.
 */
export function StoreBadge({ store, href, appLabel, className }: StoreBadgeProps) {
  const { caption, name } = STORE_LABEL[store];
  const classes = [
    "inline-flex min-h-[48px] items-center gap-3 rounded-lg px-4 text-white",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  if (!href) {
    return (
      <span
        aria-disabled="true"
        className={`${classes} cursor-not-allowed bg-slate-400`}
        title={`${appLabel} isn't available on ${name} yet`}
      >
        <Icon icon={Smartphone} size="lg" />
        <span className="text-left leading-tight">
          <span className="block text-[10px] uppercase tracking-wide text-slate-100">{name}</span>
          <span className="block text-sm font-semibold">Coming soon</span>
        </span>
      </span>
    );
  }

  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      // WCAG 2.5.3 (Label in Name): the accessible name must contain the
      // element's own visible text verbatim. The two <span>s below render
      // with no whitespace text node between them (JSX collapses the
      // newline+indentation to nothing), so the real DOM text is
      // "GET IT ONGoogle Play" -- the `{" "}` between them exists
      // specifically so that visible text has a real space in it, matching
      // the space used here, rather than making this label concatenate
      // the two ugly to match a no-space DOM quirk.
      aria-label={`${caption} ${name} — ${appLabel}`}
      className={`${classes} bg-slate-900 hover:bg-slate-800`}
    >
      <Icon icon={Smartphone} size="lg" />
      <span className="text-left leading-tight">
        <span className="block text-[10px] uppercase tracking-wide text-slate-300">{caption}</span>{" "}
        <span className="block text-sm font-semibold">{name}</span>
      </span>
    </a>
  );
}
