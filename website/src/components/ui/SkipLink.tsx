export interface SkipLinkProps {
  /** Must match the `id` on the page's main landmark. */
  targetId: string;
}

/**
 * Must be rendered as the very first focusable element in the document
 * (first thing inside <body>, before the header) -- that's what makes it
 * useful: a keyboard/screen-reader user's first Tab press lands here,
 * letting them jump straight past the header/nav into the page's real
 * content instead of tabbing through every nav link first. Invisible
 * until focused (`sr-only` / `focus:not-sr-only`).
 */
export function SkipLink({ targetId }: SkipLinkProps) {
  return (
    <a
      href={`#${targetId}`}
      className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-brand-700 focus:px-4 focus:py-3 focus:text-sm focus:font-semibold focus:text-white"
    >
      Skip to main content
    </a>
  );
}
