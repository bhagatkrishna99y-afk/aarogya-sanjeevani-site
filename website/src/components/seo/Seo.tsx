import { useLocation } from "react-router-dom";
import { siteContent } from "@/content/site";

export interface SeoProps {
  title: string;
  description?: string;
  /** Override the canonical/OG URL's path instead of using the current route (rarely needed). */
  path?: string;
  /** Absolute image URL for OG/Twitter cards. Defaults to the site's generic social card. */
  image?: string;
  /** Pages that shouldn't be indexed (the 404 page, the dev-only styleguide). */
  noIndex?: boolean;
}

/**
 * Per-page `<title>`, meta description, canonical link, and Open
 * Graph/Twitter card tags. React 19 hoists `<title>`, `<meta>` and
 * `<link>` rendered anywhere in the tree straight into `<head>`
 * automatically -- no provider, no react-helmet-async needed. Render
 * this once near the top of each page's JSX.
 *
 * `index.html` carries its own baseline copy of the OG/Twitter tags this
 * sets per-page -- see the comment there for why: most social
 * link-preview bots don't execute JavaScript, so they only ever see
 * index.html's static values, never this component's. This component's
 * job is the page's actual `<title>` (what the browser tab and bookmarks
 * show), the canonical link, and the richer per-page values that
 * JS-executing crawlers like Googlebot do pick up.
 */
export function Seo({ title, description, path, image, noIndex }: SeoProps) {
  const location = useLocation();
  const canonicalPath = path ?? location.pathname;
  const canonicalUrl = `${siteContent.url}${canonicalPath}`;
  const resolvedDescription = description ?? siteContent.description;
  const resolvedImage = image ?? `${siteContent.url}/og-image.png`;
  const fullTitle = `${title} · ${siteContent.name}`;

  return (
    <>
      <title>{fullTitle}</title>
      <meta name="description" content={resolvedDescription} />
      <link rel="canonical" href={canonicalUrl} />
      {noIndex && <meta name="robots" content="noindex, nofollow" />}

      <meta property="og:type" content="website" />
      <meta property="og:site_name" content={siteContent.name} />
      <meta property="og:title" content={fullTitle} />
      <meta property="og:description" content={resolvedDescription} />
      <meta property="og:url" content={canonicalUrl} />
      <meta property="og:image" content={resolvedImage} />

      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={fullTitle} />
      <meta name="twitter:description" content={resolvedDescription} />
      <meta name="twitter:image" content={resolvedImage} />
    </>
  );
}
