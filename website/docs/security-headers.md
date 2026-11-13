# Security headers

This is a static SPA (no server-side code) -- these headers have to come
from whatever static host serves `dist/`, since there's no app server here
to set them in code. The exact mechanism differs per host; two of the most
common ones are given below, plus the plain header list for any other
platform.

## The headers, and why each one is set this way

| Header                      | Value                                                  | Why                                                                                                                                                                                                                                                                                                                                                                                                     |
| --------------------------- | ------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Content-Security-Policy`   | see below                                              | Restricts which origins scripts/styles/images/fonts/connections can come from -- the main defense against injected/third-party script execution.                                                                                                                                                                                                                                                        |
| `X-Content-Type-Options`    | `nosniff`                                              | Stops the browser from guessing a different MIME type than the server declares, which is how some XSS/content-confusion attacks work.                                                                                                                                                                                                                                                                   |
| `Referrer-Policy`           | `strict-origin-when-cross-origin`                      | Sends the full URL as referrer to same-origin requests (useful for analytics/debugging later), but only the origin (no path/query) cross-origin -- this site's URLs don't carry sensitive query params today, but this is the standard safe default regardless.                                                                                                                                         |
| `X-Frame-Options`           | `DENY`                                                 | Legacy clickjacking defense for browsers that don't support `frame-ancestors` yet (effectively none today, but it's one line and free).                                                                                                                                                                                                                                                                 |
| `Permissions-Policy`        | `camera=(), microphone=(), geolocation=(), payment=()` | This site never needs any of these APIs -- explicitly disabling them means a future XSS/supply-chain bug in a dependency can't invoke them either.                                                                                                                                                                                                                                                      |
| `Strict-Transport-Security` | `max-age=63072000; includeSubDomains`                  | Tells the browser to never downgrade this origin to plain HTTP again, even if a future link/redirect somehow points at `http://` -- closes the window a man-in-the-middle could otherwise use on the very first request. Two years, matching the common convention; not `preload` by default (see "Before enforcing this in production" below -- that's a separate, much harder to reverse commitment). |

### The actual CSP

```
Content-Security-Policy:
  default-src 'self';
  script-src 'self';
  style-src 'self';
  img-src 'self' data:;
  font-src 'self';
  connect-src 'self' https://oleazbruvnlmuobqomge.supabase.co https://formspree.io;
  frame-ancestors 'none';
  base-uri 'self';
  form-action 'self';
  object-src 'none';
```

- **`script-src 'self'`, no `'unsafe-inline'`**: every script this site
  loads is a same-origin, Vite-built, content-hashed file -- there is no
  inline `<script>` that executes JS. (`StructuredData.tsx`'s
  `<script type="application/ld+json">` doesn't count: browsers don't
  execute JSON-LD as script, so `script-src` doesn't gate it.)
- **`style-src 'self'`, no `'unsafe-inline'` either**: checked directly
  (`grep -rn "style={{" src`) rather than assumed -- the only `style=`
  attribute anywhere in the codebase is in the dev-only `/styleguide`
  page (`src/styleguide/StyleguidePage.tsx`, rendering live swatch
  colors), and that whole route is excluded from production builds
  entirely (see its `import.meta.env.DEV`-gated dynamic import in
  `src/app/router.tsx`). Tailwind's utility classes all live in the
  external, hashed stylesheet. If a future component legitimately needs
  an inline `style` in production, this directive needs `'unsafe-inline'`
  added back (or a per-request nonce, which a static host serving a
  pre-built `index.html` with no templating can't generate) -- don't
  just add it preemptively.
- **`connect-src` allows the real Supabase project origin**: the admin
  console talks directly to Supabase's REST/Auth API from the browser
  (see `src/lib/supabase.ts`). Pinned to the exact project subdomain
  (`https://oleazbruvnlmuobqomge.supabase.co`, matching `.env.example`'s
  `VITE_SUPABASE_URL`) rather than a `https://*.supabase.co` wildcard --
  tighter, and this project's Supabase project is already fixed. If you
  ever rotate to a different Supabase project, update this value (and
  `vercel.json`'s copy of it) to match the new `VITE_SUPABASE_URL`.
- **`connect-src` allows `https://formspree.io`**: the contact form
  (`src/lib/contactForm.ts`) does a plain `fetch()` POST to
  `VITE_CONTACT_FORM_ENDPOINT`, which defaults to a Formspree URL --
  without this origin in `connect-src`, the browser blocks that request
  outright and the contact form silently fails in production. If you
  point `VITE_CONTACT_FORM_ENDPOINT` at a different provider, add that
  provider's origin here instead (or in addition).
- **`frame-ancestors 'none'`**: nothing about this site is meant to be
  embedded in anyone else's page.
- **`img-src` allows `data:`**: a few components inline tiny SVGs as data
  URIs; no other image source is used (see `src/assets/README.md` --
  everything else is either a bundled asset or `public/`, both same-origin).

### Before enforcing this in production

Ship it as `Content-Security-Policy-Report-Only` first (same value, report-only
header name) with a `report-to`/`report-uri` endpoint, or just manually
click through every page and the admin console with DevTools' console open,
watching for CSP violation messages -- a static analysis of the source
(this document) is a reasonable starting policy, not a guarantee against
every edge case a real browser session might hit.

## Vercel

This project ships a real `website/vercel.json` with exactly this policy
already wired up -- see that file rather than copying the policy by hand.
A few things worth calling out about its shape:

- The long-cache rule is scoped to `/assets/(.*)` specifically, not by
  file extension: every `dist/assets/*` file is content-hashed by Vite
  (a change to the file means a new filename, never the same URL serving
  different bytes), so it's safe to cache for a year and `immutable`.
  `public/*.png`/`site.webmanifest`, by contrast, are
  **not** content-hashed -- they keep the same filename across deploys --
  so they get a much shorter `max-age=86400, must-revalidate` instead; a
  year-long `immutable` cache on a non-hashed file would mean a changed
  favicon or OG image silently not showing up for a year on repeat
  visitors.
- `index.html`, `robots.txt`, and `sitemap.xml` are each served fresh
  (`max-age=0` or a short `max-age` with `must-revalidate`): all three
  can legitimately change on a redeploy without a new filename.
- `rewrites` sends everything else to `/index.html` for client-side
  routing. This is safe to combine with the cache rules above because
  Vercel's routing checks real static files first and only falls through
  to `rewrites` for paths that don't match one -- a request for
  `/assets/index-abc123.js` is served as that file (with its own cache
  header), never rewritten to `index.html`.

## Netlify

`public/_headers` (Netlify reads this file verbatim and serves it alongside
every matching response -- note this lives in `public/`, not the repo
root, so Vite copies it into `dist/` as part of the normal build):

```
/*
  Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self' https://oleazbruvnlmuobqomge.supabase.co https://formspree.io; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'
  X-Content-Type-Options: nosniff
  Referrer-Policy: strict-origin-when-cross-origin
  X-Frame-Options: DENY
  Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()
  Strict-Transport-Security: max-age=63072000; includeSubDomains

/assets/*
  Cache-Control: public, max-age=31536000, immutable

/index.html
  Cache-Control: public, max-age=0, must-revalidate

/robots.txt
  Cache-Control: public, max-age=3600, must-revalidate

/sitemap.xml
  Cache-Control: public, max-age=3600, must-revalidate
```

Netlify also needs `public/_redirects` for the SPA catch-all rewrite
(`/* /index.html 200`) -- see this project's README for why that rewrite
is required at all.

## Any other static host

Set the same five headers from the table above or configure the host to
copy `public/_headers`/`vercel.json`'s shape, whatever the platform's own
syntax is (Cloudflare Pages reads `_headers` the same way Netlify does;
S3+CloudFront needs a response-headers policy in the CloudFront
distribution instead, configured in its console/Terraform, not a file in
this repo).
