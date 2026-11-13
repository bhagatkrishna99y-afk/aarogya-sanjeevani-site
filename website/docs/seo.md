# SEO foundation

## Pieces and where they live

| Piece                                           | Where                                     | Scope                                                             |
| ----------------------------------------------- | ----------------------------------------- | ----------------------------------------------------------------- |
| Per-page title/description/canonical/OG/Twitter | `src/components/seo/Seo.tsx`              | Rendered once per page                                            |
| Baseline OG/Twitter (static fallback)           | `index.html`                              | Whole site, one set of values                                     |
| Organization + WebSite JSON-LD                  | `src/components/seo/StructuredData.tsx`   | Rendered once, in `PublicLayout`                                  |
| `sitemap.xml` / `robots.txt`                    | `public/`                                 | Static files                                                      |
| Favicon set + web manifest                      | `public/*.png`, `public/site.webmanifest` | Generated from `src/assets/logo.png` via `npm run generate:icons` |
| Social preview card                             | `public/og-image.png`                     | Generated via `npm run generate:og-image`                         |

## Why `index.html` duplicates some of what `Seo.tsx` does

This is a client-only SPA (Vite + React, no SSR/prerendering). Googlebot
executes JavaScript before indexing a page, so it sees whatever
`Seo.tsx` set for that specific route -- per-page titles and descriptions
genuinely work for Google. Most **social link-preview bots** (Facebook's,
Twitter/X's, Slack's, WhatsApp's, iMessage's) do **not** execute
JavaScript at all. When someone pastes a link to `/services` into Slack,
the bot that generates the preview card fetches the raw HTML response --
which, for a client-rendered SPA, is `index.html`, completely
unchanged, no matter which route was requested -- and never runs the JS
that would have swapped in `Seo.tsx`'s per-page `og:title`/`og:description`.

That's why `index.html` carries its own complete, static set of
OG/Twitter tags: they're not a redundant leftover, they're the **only**
thing most social crawlers for this site will ever actually see, for
every route. `Seo.tsx`'s per-page values are a real improvement for
Google and for the browser tab title, but they can't replace the static
baseline for social previews -- only server-side rendering or
prerendering (generating real per-route HTML at build/request time)
could make social previews page-specific, and that's a meaningfully
bigger change than this task, not something to half-do.

## JSON-LD and the same limitation

`StructuredData` has the identical SPA caveat: the `<script
type="application/ld+json">` it renders only exists in the DOM after
React has mounted and run. A crawler that executes JS (Google) sees it
fine; one that doesn't, won't. There's no static fallback for this one
(unlike OG tags, JSON-LD isn't typically duplicated in a `<head>` meta
tag), so for this project, the honest statement is: structured data
works for Google, not for a hypothetical crawler that skips JS entirely.

## Regenerating the generated assets

```bash
npm run generate:icons     # favicon-*.png, apple-touch-icon.png, android-chrome-*.png
npm run generate:og-image  # og-image.png (1200x630 social card)
```

Both read `src/assets/logo.png` as their source -- re-run them any time
the logo file changes, don't hand-edit the PNGs. The OG image composites
the real logo onto a plain card background rather than re-typesetting the
wordmark/tagline separately, since the logo already carries both.

## `sitemap.xml`'s domain is a placeholder in source, not in a real build

The checked-in file ships with `https://example.com` (matching
`.env.example`'s `VITE_SITE_URL`), same as the rest of this project's
placeholder-value convention. It does **not** need hand-editing before a
real deploy, though: `vite.config.ts`'s `patchSeoFilesWithSiteUrl` plugin
rewrites every `https://example.com` in both `sitemap.xml` and
`robots.txt` to the real `VITE_SITE_URL` as part of `npm run build`
(`closeBundle`, after Vite's own copy of `public/` into `dist/`) --
verified by building with a real `VITE_SITE_URL` set and checking the
output. Same effect as `index.html`'s native `%VITE_SITE_URL%`
interpolation, just via a plugin instead, since these two files are
served as plain static files with no templating of their own. Get
`VITE_SITE_URL` right in the deploy environment (see
`docs/deployment.md`) and both files come out correct automatically.

## A known, accepted gap

`Seo.tsx` and `StructuredData` both read `siteContent.url`
(`VITE_SITE_URL`) to build absolute URLs. In local dev that's
`http://localhost:5173` -- fine for development, meaningless in any
canonical/OG URL a real crawler would see. This is expected: the
production deploy's own `VITE_SITE_URL` (set in the hosting
environment, per `README.md`'s "Build & deploy" section) is what actually
ships in a real build.
