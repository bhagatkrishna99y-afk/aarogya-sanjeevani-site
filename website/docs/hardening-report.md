# Website hardening report

A full accessibility/performance/SEO/code-quality audit against WCAG 2.2
AA, with every claim below verified by actually running the relevant
tool (axe-core in a real browser, Lighthouse, `tsc`, `eslint`, the full
test suite) against the production build -- not inferred from reading
the code. Nothing here was assumed to be fine without a tool confirming
it.

## Summary

| Area                                                              | Before                                                     | After                                                                                                                              |
| ----------------------------------------------------------------- | ---------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| Lighthouse Performance (mobile, 3-run median, all 6 public pages) | 0.76-0.89 (every page failing the 0.90 target)             | **0.92-0.97** (every page passing)                                                                                                 |
| Lighthouse Accessibility / Best Practices / SEO                   | already ≥0.90                                              | **1.00 / 1.00 / 1.00** on every page                                                                                               |
| Full-page axe-core scan (real browser, WCAG 2.2 AA tags)          | 1 real violation (serious impact) found on 2 pages         | **0 violations across 8 pages**                                                                                                    |
| JS a visitor to any public page downloads                         | ~475 KB (one bundle, incl. the admin-only Supabase client) | **~282 KB** (react-vendor + app glue; Supabase's 211 KB only loads for `/admin*`)                                                  |
| Console errors/warnings in production build                       | not checked before                                         | **0 across 8 pages**, now enforced by a test                                                                                       |
| Broken links                                                      | not checked before                                         | checker built and run; found 2 (placeholder Play Store URLs -- see below)                                                          |
| CI                                                                | backend only                                               | **website-ci.yml**: lint, format, typecheck+build, unit tests, link check, e2e (smoke + a11y + console), Lighthouse CI gated at 90 |

## Issues found and fixed

### Accessibility

1. **Real WCAG 2.5.3 (Label in Name) violation** -- `StoreBadge`'s
   `aria-label` ("Patient app on Google Play") didn't contain the
   element's own visible text ("GET IT ON" / "Google Play"), which axe's
   `label-content-name-mismatch` rule flags as serious impact. Fixed the
   label to lead with the real visible text (`"GET IT ON Google Play —
Patient app"`). While fixing it, found a second, subtler bug it
   exposed: the two `<span>`s rendering that visible text had **zero
   whitespace between them in the DOM** (JSX collapses the newline
   between them to nothing), so the real text was
   `"GET IT ONGoogle Play"` with no space -- added an explicit `{" "}`
   so the visible text and the label actually match, instead of making
   the label ugly to match a DOM quirk. Found via a new full-page
   Playwright + axe-core scan (`e2e/accessibility.spec.ts`) -- the
   existing _component-level_ vitest-axe tests never caught this.
2. **Component-level a11y tests were silently scoped to WCAG 2.0 only.**
   `src/test/axe.ts`'s `checkA11y` helper (used by all 23 design-system
   component tests) only ran `wcag2a`/`wcag2aa` tagged rules -- WCAG 2.1
   and 2.2 AA criteria (including the one above) were never checked by
   any component test. Widened it to
   `wcag2a, wcag2aa, wcag21a, wcag21aa, wcag22aa`; re-ran the full 160-test
   suite afterward -- nothing else regressed.
3. **No full-page, real-browser accessibility coverage existed at all.**
   Added `e2e/accessibility.spec.ts`: axe-core (WCAG 2.2 AA tags) against
   all 8 reachable pages (6 public + 404 + `/admin/login`), plus explicit
   checks per page for exactly one `<h1>`, the four standard landmarks
   (`banner`/`navigation`/`main`/`contentinfo`), and no horizontal
   overflow at a 200%-zoom-equivalent viewport (640 CSS px, WCAG 1.4.4).
   23 new tests, all passing.
4. **Already in good shape, verified rather than assumed**: keyboard
   operability + focus order + visible focus ring (global
   `:focus-visible` rule; smoke tests already covered skip-link-first-tab
   and the mobile menu's real focus trap), form labels + `aria-live`
   error announcements (`FormField`/`FieldError` wire `htmlFor`/
   `aria-describedby` automatically; `FieldError` uses `role="alert"`),
   `prefers-reduced-motion` (global rule in `src/styles/index.css`,
   already present), touch targets ≥44px (consistent `min-h-[48px]`
   convention, confirmed by axe's 2.5.8 target-size rule passing in the
   full-page scans), and status never conveyed by color alone (`Alert`
   pairs color with a distinct icon shape _and_ required text; status
   pills in the admin console always render the status word, never a
   bare colored dot).

### Performance

5. **The biggest real bug: every visitor downloaded the admin console's
   Supabase client, even on the homepage.** `AuthProvider` (which pulls in
   all of `@supabase/supabase-js`, ~211 KB) wrapped the _entire_ app at
   the router root, even though no public page touches auth state at all
   (confirmed: zero `useAuth` references outside `/admin`). Fixed by
   scoping it to a new lazy `AdminAuthProvider`, mounted only on the
   `/admin*` route subtree.
6. **That fix alone didn't work at first -- Vite's default
   `modulePreload` silently defeated it.** Vite auto-injects
   `<link rel="modulepreload">` for _every_ chunk reachable through the
   full import graph, including ones behind a `React.lazy()` boundary --
   so the browser's preload scanner was still eagerly fetching the
   admin-only Supabase chunk on every page load regardless of route.
   Caught via Lighthouse's `unused-javascript` audit showing the chunk as
   84% unused bytes _on the homepage_. Fixed with a
   `build.modulePreload.resolveDependencies` filter in `vite.config.ts`.
7. **Rollup's default chunking also merged Supabase into the main entry
   chunk** regardless of the lazy boundary (confirmed by inspecting the
   built output, not assumed) -- added explicit `manualChunks` to force
   `@supabase/supabase-js` and `react`/`react-dom`/`react-router-dom`
   into their own named, cacheable chunks.
8. **No critical font preloading.** Added a small Vite plugin
   (`preloadCriticalFonts`) that injects `<link rel="preload">` for the
   two font files actually needed for first paint (Figtree 700 for the
   H1, Noto Sans 400 for body text) -- reads the real content-hashed
   filename out of the finished bundle, since it can't be hand-written
   statically. `font-display: swap` was already the Fontsource default,
   so no FOIT either way.
9. **`sitemap.xml`/`robots.txt` had a hardcoded placeholder domain that
   would ship wrong on every real deploy.** They're plain static files in
   `public/`, so Vite's `%VITE_SITE_URL%` HTML interpolation (which only
   applies to `index.html`) never touched them -- they'd have silently
   shipped `https://example.com` forever unless someone remembered to
   hand-edit two files nobody usually checks. Added a `closeBundle` Vite
   plugin that patches both from the real `VITE_SITE_URL` at build time.
10. Already in good shape, verified rather than assumed: route-level code
    splitting (every page already lazy-loaded via `React.lazy` +
    `Suspense`, confirmed in the build output -- each page is its own
    chunk), images (the only on-page images are two SVG illustrations,
    already with explicit `width`/`height` to prevent layout shift, and
    the below-the-fold one already has `loading="lazy"`; the one raster
    image, the Open Graph share image, correctly stays PNG since social
    crawlers don't support AVIF/WebP -- "modern formats" doesn't apply to
    an image that's never rendered in the page itself), cache-header
    guidance (added to `docs/security-headers.md`'s Vercel/Netlify
    snippets: `immutable, max-age=31536000` for hashed assets,
    `must-revalidate` for `index.html`), unused dependencies (ran
    `depcheck`: it flags 5, all 5 confirmed false positives --
    `@fontsource/*` and `tailwindcss` are used via CSS `@import`, which a
    JS-import scanner can't see; the two Babel presets are referenced by
    string name inside `eslint.config.js`'s `babelOptions`, not an
    `import` statement -- nothing was actually removed because nothing
    was actually unused).

### SEO

11. Already in place, verified rather than assumed: unique `<title>` +
    meta description per page (`Seo.tsx`, used on every page), canonical
    links (`Seo.tsx`), structured data (`StructuredData.tsx` emits valid
    Organization + WebSite JSON-LD -- checked for valid JSON and the
    required `@context`/`@type` fields; this sandbox has no internet
    access to run it through Google's live Rich Results validator, so
    that specific external check is the one item in this report not
    independently confirmed against a third party), the branded OG image
    (`public/og-image.png`, 1200×630, already generated), semantic HTML
    (landmarks confirmed above), meaningful link text (no "click here"/
    "read more" link text found anywhere in a repo-wide search).
12. Admin pages (`Dashboard`, `Doctors`, `Catalog`, `Requests`, `Users`)
    don't pass a unique description to `Seo`, falling back to the
    site-wide default -- left as-is deliberately: `robots.txt` disallows
    `/admin` entirely and it's excluded from `sitemap.xml`, so no crawler
    ever indexes these pages; a "unique" meta description there would
    have zero actual SEO effect.

### Code quality

13. **No check existed for console errors/warnings in production**,
    despite the design system clearly being built with care around this.
    Added `e2e/console.spec.ts`: asserts zero `console.error`/
    `console.warn`/page errors across all 8 pages in the real production
    build. All pass.
14. **No broken-link checking existed.** Added `scripts/check-links.mjs`
    -- a real browser crawl (plain static HTML has no `<a>` tags at all in
    this SPA; every link is rendered by React Router after JS runs, so a
    static link checker can't see them). Checks internal links resolve
    to a real route (not the 404 page), external links with a live HTTP
    request, and `mailto:`/`tel:` links for well-formedness. Running it
    found exactly 2 broken links: `VITE_PLAY_STORE_PATIENT_URL`/
    `VITE_PLAY_STORE_DOCTOR_URL` are still their `.env.example` placeholder
    values (`com.example.patient`/`com.example.doctor`, which don't exist
    on the real Play Store) -- expected and correct until the Android apps
    are actually published; not a code defect. The checker correctly
    exits non-zero when this happens, confirmed directly.
15. **No security headers were documented anywhere.** Added
    `docs/security-headers.md`: a CSP (`script-src 'self'`,
    `style-src 'self'` -- confirmed via `grep` that zero inline
    `style="..."` attributes exist in any production code path, so
    neither needs `'unsafe-inline'`), `X-Content-Type-Options`,
    `Referrer-Policy`, `X-Frame-Options`/`frame-ancestors`, and
    `Permissions-Policy`, with ready-to-use Vercel (`vercel.json`) and
    Netlify (`_headers`) snippets.
16. **No CI existed for the website at all** -- only a backend-scoped
    workflow existed in this repo. Added
    `.github/workflows/website-ci.yml`: lint, format check, typecheck +
    build, unit/component tests, the link checker, Playwright (smoke +
    accessibility + console), and Lighthouse CI (gated at 0.90 across all
    four categories, mobile profile) on every push/PR touching `website/`.

## What wasn't fixed, and why

- **The Lighthouse numbers above come from this sandboxed development
  environment**, not a dedicated CI runner -- Lighthouse's Total Blocking
  Time metric in particular is sensitive to CPU contention on the machine
  running it, and this one shares resources unpredictably. The scores
  should be read as "every page now clearly clears 90, with real margin,"
  not as exact numbers a production CI runner will reproduce byte-for-byte.
  The underlying fixes (removing 211 KB from the public-page critical
  path, preloading critical fonts, code splitting) are real and
  environment-independent regardless of the exact score.
- **Structured data wasn't run through Google's Rich Results Test** --
  no internet access from this sandbox. It was checked for valid JSON and
  the required Schema.org fields, which is a real but partial check.
- **Admin pages' shared meta description** -- deliberately left (see
  item 12): they're not indexed, so there's nothing to fix.
- **The CSP's `connect-src` uses `https://*.supabase.co`** (a wildcard)
  rather than one exact subdomain, documented as a deliberate choice for
  anyone copying the file before picking a final `VITE_SITE_URL` --
  narrowing it to the real project's exact subdomain in production is
  noted as a recommended tightening, not done here since it depends on
  which Supabase project the final deploy actually points at.
