import { siteContent } from "@/content/site";

/**
 * Organization + WebSite JSON-LD -- site-wide identity data, not
 * page-specific, so this renders once (in PublicLayout), not per-page
 * like `Seo`. A plain inline `<script>` works fine here: JSON-LD doesn't
 * need to live in `<head>` specifically (crawlers parse it anywhere in
 * the document), so this doesn't need React 19's head-hoisting at all --
 * it just renders exactly where it's placed in the tree.
 *
 * Known limitation worth being honest about: this is a client-only SPA
 * with no server-side rendering, so this script only exists once
 * JavaScript has run. Googlebot executes JS before indexing, so it sees
 * this fine; a crawler that doesn't run JS never will. Fixing that for
 * real would mean SSR/prerendering, which is out of scope here.
 */
export function StructuredData() {
  const organization = {
    "@context": "https://schema.org",
    "@type": "Organization",
    name: siteContent.name,
    url: siteContent.url,
    logo: `${siteContent.url}/android-chrome-512x512.png`,
    contactPoint: {
      "@type": "ContactPoint",
      email: siteContent.contact.email,
      telephone: siteContent.contact.phone,
      contactType: "customer service",
    },
  };

  const website = {
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: siteContent.name,
    url: siteContent.url,
  };

  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(organization) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(website) }}
      />
    </>
  );
}
