# assets

Images, icons, and other static files that are **imported by code** (so
Vite can hash/optimize them), as opposed to `public/`, which serves files
verbatim at a fixed URL.

- `logo.png` -- the real, client-supplied logo. Used directly in the
  header and footer, and as the source for the whole generated favicon
  set, the web manifest icons, and the OG image (see
  `scripts/generate-icons.mjs` / `scripts/generate-og-image.mjs`). Not to
  be redrawn, recolored, or cropped -- it's used as-is everywhere.
- `hero-illustration.svg` -- original artwork for the Home page hero
  (above the fold, rendered eagerly).
- `trust-illustration.svg` -- original artwork for the Home page's Trust
  & Safety section (below the fold, rendered with `loading="lazy"`).
