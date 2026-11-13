# Aarogya Sanjeevani — Website

Public marketing site (service discovery, information) plus a role-gated
operations console for admins, built with React, TypeScript, Vite, React
Router and Tailwind CSS, talking directly to Supabase.

## Folder structure

```
src/
  app/            Router (router.tsx), providers (providers.tsx), and
                   RouteFocusManager.tsx (scroll/focus on navigation)
  auth/            Supabase auth context + the /admin route guard
  components/
    ui/            The design system -- Button, Card, Alert, form fields,
                   Accordion, Icon, SkipLink, PageSkeleton, etc. (see
                   "Design system" below). Each has a co-located *.test.tsx.
    layout/        PublicLayout, Header, MobileMenu, Footer, AdminLayout --
                   see "Layout & routing" below.
    seo/           Seo.tsx (per-page) + StructuredData.tsx (site-wide
                   JSON-LD) -- see "SEO" below.
    sections/      Larger, page-specific composed blocks (e.g. the Home
                   page's HeroSection, BenefitsSection, FaqSection, etc.) --
                   built from src/content/*.ts + src/components/ui/*.
    *.tsx          Pre-existing components not yet sorted into the above
  content/         Page/site copy as typed objects -- site.ts (nav items,
                   legal links, contact, app store URLs, the India
                   emergency number), home.ts, about.ts, services.ts (copy
                   for their matching public pages)
  pages/
    public/        Home, Services, About, Contact, PrivacyPolicy, Terms, 404
    admin/         Login, Dashboard, Doctors, Catalog, Requests, Users
  lib/             Supabase client, generated database types, useFocusTrap
  styles/          Global CSS -- Tailwind entry point AND this project's
                   design tokens (@theme block; see "Design system" below)
  styleguide/      Dev-only component gallery (StyleguidePage.tsx) + the
                   contrast-ratio helper it (and docs/design-tokens.md) use
  test/            Vitest setup + the shared `checkA11y` helper -- tests
                   themselves live next to the component/page they cover
                   (e.g. StatusViews.test.tsx)
  assets/          Images/icons imported by code -- logo.png (source of
                   truth for the favicon set/manifest/OG image) and the
                   Home page's original hero/trust SVG illustrations
e2e/               Playwright smoke tests
public/            the generated favicon PNG set, site.webmanifest,
                   og-image.png, robots.txt, sitemap.xml
scripts/           generate-icons.mjs, generate-og-image.mjs (both use `sharp`)
docs/
  design-tokens.md Full color/typography/spacing rationale + every verified contrast ratio
  seo.md           What index.html vs Seo.tsx vs StructuredData each do, and why
```

## Routes

Every route-level page is lazy-loaded (`React.lazy` + one `<Suspense
fallback={<PageSkeleton />}>` wrapping the whole route tree in
`src/app/router.tsx`) -- each page's JS only downloads when it's first
visited, confirmed in `npm run build`'s output: every page is its own
chunk, not part of the main bundle.

- `/`, `/services`, `/about`, `/contact`, `/privacy-policy`, `/terms` —
  public, no auth required. `/services` is static marketing copy
  (`src/content/services.ts`) showing example categories/services for
  orientation -- the live, bookable catalog (Supabase `service_categories`
  - `services`, managed in `/admin/catalog`) is only ever read from the
    Patient/Doctor apps, not this site.
- Any unmatched path (including a stray `/admin/*` typo) renders the 404
  page -- nested _inside_ `PublicLayout`'s route group, not as a bare
  top-level route, so it still has the normal header/footer and a way
  back home, rather than being an orphan error page.
- `/admin/login` — email/password sign-in.
- `/admin/*` — gated by `RequireAdmin`: requires a signed-in session **and**
  `profiles.role === 'admin'`. Covers the dashboard, doctor application
  review (approve/reject/suspend/reactivate), catalog management, request
  monitoring with status history, and user role management.

## Layout & routing

- **`PublicLayout`** (`src/components/layout/`): skip link (first child,
  before the header) → `Header` → `<main id="main-content" tabIndex={-1}>`
  → `Footer`. `StructuredData` (JSON-LD) also renders here, once.
- **`Header`**: logo + wordmark, desktop primary nav, a "Get the App"
  button (links to the Patient app's Play Store listing), and a hamburger
  (mobile only) that opens `MobileMenu`. Sticky (`position: sticky; top:
0`) -- `--header-height` is kept in sync with the header's real
  rendered height (it differs between mobile/desktop) via a
  `ResizeObserver`, and `html { scroll-padding-top: var(--header-height)
}` in `src/styles/index.css` uses it so a sticky header never ends up
  covering an in-page anchor target (or the skip link's own target)
  after a jump.
- **`MobileMenu`**: a real `role="dialog" aria-modal="true"` slide-in
  panel. Tab/Shift+Tab are trapped inside it while open
  (`src/lib/useFocusTrap.ts`), Escape closes it, opening moves focus to
  its close button, and closing returns focus to the hamburger that
  opened it. `inert` when closed (removed from both the tab order and
  the accessibility tree, not just visually hidden).
- **`RouteFocusManager`** (`src/app/`): this app uses the declarative
  `<BrowserRouter>`/`<Routes>` API, which has no built-in
  `<ScrollRestoration>` (that only exists for `createBrowserRouter`). On
  every route change (not the first page load): a `#hash` URL scrolls to
  that element; otherwise the viewport resets to the top and focus moves
  to the new page's `<h1>` (added `tabindex="-1"` if it doesn't have one
  already) so a screen reader announces the new page -- an SPA route
  change triggers no real page load, so without this, assistive tech has
  no cue anything navigated at all.
- **Cookie/consent**: deliberately no banner -- see the comment in
  `PublicLayout.tsx` for why (no non-essential cookies or analytics exist
  today) and exactly where to add one if that changes.

## Setup

```bash
npm install
cp .env.example .env   # already filled in with this project's URL + publishable key
npm run dev
```

## Commands

| Command                                   | What it does                                                                      |
| ----------------------------------------- | --------------------------------------------------------------------------------- |
| `npm run dev`                             | Start the Vite dev server.                                                        |
| `npm run build`                           | Typecheck (`tsc -b`) then production-build to `dist/`.                            |
| `npm run preview`                         | Serve the last `dist/` build locally.                                             |
| `npm run lint`                            | ESLint (jsx-a11y, react-hooks, react-refresh) over `src/`.                        |
| `npm run lint:fix`                        | Same, applying auto-fixes.                                                        |
| `npm run lint:oxlint`                     | The pre-existing fast `oxlint` pass -- kept alongside ESLint, not replaced by it. |
| `npm run format` / `npm run format:check` | Prettier, write or check-only.                                                    |
| `npm run test`                            | Vitest (jsdom + React Testing Library), single run.                               |
| `npm run test:watch`                      | Vitest in watch mode.                                                             |
| `npm run test:e2e`                        | Playwright smoke tests (builds and serves `dist/` first -- see below).            |
| `npm run generate:icons`                  | Regenerate the favicon PNG set from `src/assets/logo.png`.                        |
| `npm run generate:og-image`               | Regenerate `public/og-image.png` (the social preview card).                       |

A pre-commit hook (husky + lint-staged) runs ESLint and Prettier on staged
`.ts`/`.tsx` files, and Prettier on staged `.js`/`.json`/`.css`/`.md` files,
automatically on every `git commit`.

## Path alias

`@/*` resolves to `src/*` (configured in both `tsconfig.app.json` and
`vite.config.ts`, which must be kept in sync by hand -- Vite doesn't read
`tsconfig.json` paths automatically). Prefer it for anything outside the
current file's own folder; relative imports (`../../lib/supabase`) are
still fine for same-area imports and haven't been mass-rewritten in
existing files.

## Design system

Full rationale (every color's hex value, which pairings were verified
against which WCAG bar, and why) lives in `docs/design-tokens.md` -- this
is just the map of where things are:

- **Tokens**: `src/styles/index.css`'s `@theme` block. Tailwind v4 is
  CSS-first, so that block _is_ this project's `tailwind.config` -- there's
  no separate config file. Colors: `brand` (teal, primary, pre-existing),
  `accent` (warm orange, for calls to action), `success`/`warning`/`error`/
  `info` (semantic), and Tailwind's own `slate` as the neutral scale.
  Typography: Figtree (headings) + Noto Sans (body), self-hosted via
  Fontsource, Latin subset only (see the comment above the `@import` lines
  before widening that). Spacing/radius/shadow are Tailwind's own default
  scales, used as-is.
- **Components**: `src/components/ui/` -- Button, LinkButton, Container,
  Section, Card, FeatureCard, StepCard, Badge, Accordion, Label, Input,
  Textarea, Select, FieldError, FormField, Alert, Skeleton, Icon,
  StoreBadge, Breadcrumbs, SkipLink, PageSkeleton. Every one is typed,
  keyboard-operable, and has a co-located test asserting both behavior
  and (`checkA11y`-verified) zero accessibility violations. (Per-page
  `<title>`/meta tags moved out to `Seo.tsx` -- see "SEO" below.)
- **Live gallery**: visit `/styleguide` in dev (`npm run dev`) -- every
  color swatch, type style, and component variant, with contrast ratios
  computed live by the same function `docs/design-tokens.md`'s numbers
  come from. The route only exists in development: it's lazy-imported and
  gated behind `import.meta.env.DEV` in `src/app/router.tsx`, so
  `npm run build` doesn't ship it (confirmed: it lands in its own separate
  chunk that nothing in the production route tree ever references).

A real, pre-existing bug this work found and fixed: several buttons across
the site (admin console included) used `bg-brand-600` with white text,
which is 3.74:1 -- below the 4.5:1 AA minimum. Fixed to `bg-brand-700`
(5.47:1) everywhere it occurred; see `docs/design-tokens.md` for the full
audit.

## SEO

Full rationale in `docs/seo.md` -- short version:

- **Per page**: render `<Seo title="..." description="..." />` (from
  `@/components/seo/Seo`) near the top of a page's JSX -- sets
  `<title>`, meta description, canonical link, and Open Graph/Twitter
  tags for that route. Pass `noIndex` on pages that shouldn't be indexed
  (404, the dev-only styleguide). Built on React 19's native
  `<title>`/`<meta>`/`<link>` head-hoisting -- no provider,
  `react-helmet-async`, or similar needed.
- **Site-wide**: `<StructuredData />` (Organization + WebSite JSON-LD)
  renders once, in `PublicLayout`.
- **Static fallback**: `index.html` carries its own complete, static copy
  of the OG/Twitter tags. This isn't redundant -- most social
  link-preview bots (Facebook's, Twitter/X's, Slack's, ...) don't execute
  JavaScript, so for this client-only SPA, `index.html`'s static values
  are the _only_ thing those crawlers ever see, for every route.
  `docs/seo.md` explains this in full.
- **Favicons/manifest**: `src/assets/logo.png` is the one source of truth.
  `npm run generate:icons` derives the PNG sizes iOS/Android specifically
  require from it (`apple-touch-icon.png`, `android-chrome-*.png`,
  `favicon-*.png`); `npm run generate:og-image` builds the 1200x630
  social preview card. Re-run both if the logo changes -- never hand-edit
  the generated PNGs.
- **`robots.txt` / `sitemap.xml`** (`public/`): ship with the
  `https://example.com` placeholder domain in source (matching
  `.env.example`'s `VITE_SITE_URL`), but `vite.config.ts`'s
  `patchSeoFilesWithSiteUrl` plugin rewrites both to the real
  `VITE_SITE_URL` as part of `npm run build` -- no hand-editing needed,
  same effect as `index.html`'s native `%VITE_SITE_URL%` interpolation.

## Environment variables

- `VITE_SUPABASE_URL` — the Supabase project URL.
- `VITE_SUPABASE_PUBLISHABLE_KEY` — the project's publishable (`sb_publishable_...`) key. Safe for the client; all access control is enforced server-side via RLS and RPCs, never by hiding this key.
- `VITE_SITE_URL` — this site's own canonical URL. Used to build absolute canonical/OG/Twitter URLs (`Seo.tsx`, `StructuredData.tsx`) and interpolated directly into `index.html` via Vite's built-in `%VITE_SITE_URL%` HTML env replacement.
- `VITE_PLAY_STORE_PATIENT_URL` / `VITE_PLAY_STORE_DOCTOR_URL` — Play Store listing links for the two Android apps; the header's "Get the App" button and the footer's/mobile menu's store badges all read these.
- `VITE_CONTACT_EMAIL` / `VITE_CONTACT_PHONE` — support contact details, shown on `/contact` and in the footer.

All five are typed in `src/vite-env.d.ts` (augmenting `ImportMetaEnv`) and
read once into `src/content/site.ts`'s `siteContent` object -- import that
rather than reading `import.meta.env` directly in a page. `.env.example`
has placeholder values; never commit a real `.env`.

## Testing

- **Unit/component** (`npm run test`): Vitest + React Testing Library,
  jsdom environment. Every component in `src/components/ui/` has a
  co-located `*.test.tsx` that renders it, asserts its real behavior, and
  checks it for accessibility violations via `checkA11y` (`src/test/axe.ts`):
  ```ts
  import { checkA11y } from "@/test/axe";
  const violations = await checkA11y(container);
  expect(violations).toHaveLength(0);
  ```
  `checkA11y` wraps `vitest-axe`'s `axe()` runner, restricted to WCAG 2 A/AA
  rules (not axe's full best-practice ruleset, which includes page-level
  checks like "exactly one `<h1>`" that are meaningless -- and would be
  false positives -- on an isolated component fragment). Two things worth
  knowing if you touch this: (1) `vitest-axe@0.1.0`'s own
  `toHaveNoViolations` custom-matcher registration (`vitest-axe/extend-expect`)
  ships broken (its compiled JS is an empty file, and its type declarations
  mark a real runtime export as type-only) -- asserting on `.violations`
  directly, as `checkA11y`'s callers do, sidesteps both bugs. (2) jsdom
  doesn't implement `HTMLCanvasElement.getContext`, so axe's color-contrast
  check logs a benign `Not implemented` console error on every run -- it
  doesn't fail the test or mean the check didn't happen, just noise.
- **End-to-end smoke** (`npm run test:e2e`): Playwright, against a real
  production build. Its config builds (`npm run build`) and serves
  (`npm run preview`) `dist/` automatically before running, so just run
  `npm run test:e2e` directly. First time only: `npx playwright install
chromium` to download the browser binary. Covers routing (every public
  page, the 404 fallback, the legal pages from the footer), the skip
  link (a real Tab press, then Enter jumps focus to `#main-content`),
  and the mobile menu at a real narrow viewport (hamburger → focus-trapped
  dialog → Escape closes it and returns focus, or choosing a nav item
  closes it and navigates). One thing worth knowing if you touch the
  mobile menu tests: the closed panel is checked via `toHaveAttribute("inert",
"")` + `not.toBeInViewport()`, not `toBeHidden()` -- it's translated
  off-screen via a CSS transform for a smooth slide animation (not
  `display:none`), so Playwright's layout-based visibility check sees it
  as "visible" regardless of `inert`; those two checks are what actually
  reflects "closed" for this specific implementation.
- **Routing/mobile-menu/skip-link unit coverage**: `src/app/router.test.tsx`
  (every route renders its lazy-loaded page; the 404 fallback keeps the
  header/footer), `src/components/layout/MobileMenu.test.tsx` (focus
  trap wrapping both directions, Escape, initial/return focus),
  `src/components/ui/SkipLink.test.tsx` (first-Tab-reaches-it, target
  id). `src/app/RouteFocusManager.test.tsx` covers the scroll-to-top +
  focus-the-new-heading behavior on navigation.

## Tooling notes

- **TypeScript is the new native-port TS 7** (`"typescript": "~7.0.2"`).
  `typescript-eslint` doesn't run against it yet (it hard-fails --
  [tracking issue](https://github.com/typescript-eslint/typescript-eslint/issues/10940)),
  so ESLint parses `.ts`/`.tsx` with `@babel/eslint-parser` +
  `@babel/preset-typescript` instead (strips TS syntax for linting
  purposes only -- doesn't need the `typescript` package at all). This
  means two scope-based core ESLint rules are turned off in
  `eslint.config.js` (`no-unused-vars`, `no-undef`): Babel's parser
  doesn't patch `eslint-scope` with TypeScript-aware binding analysis the
  way `typescript-eslint` does, so those rules can't tell a `type`/
  `interface` declaration from an undefined reference. `tsc -b` already
  enforces both correctly, with full type information. Generated/ambient
  type-only files (`**/*.d.ts`, `src/lib/database.types.ts`) are excluded
  from ESLint entirely for the same reason, and reviewed via `tsc -b`.
- **TS 7 also removed the `baseUrl` compiler option.** The `@/*` path
  alias uses only `paths` (which now resolves relative to the tsconfig
  file's own directory).
- **Vitest is pinned to `^5.1.0`**, not `^2.x`: this project's `vite` is
  `^8.3.6`, and Vitest only started declaring support for Vite 8 in 5.1.0;
  anything older bundles its own incompatible nested `vite` copy and
  `tsc -b` fails with a confusing duplicate-type error inside
  `vite.config.ts`.

## Verifying the admin console

1. Bootstrap an admin (see `backend/README.md` — "Bootstrapping the first admin").
2. `npm run dev`, open `/admin`, sign in. You should land on `/admin` (dashboard with live counts).
3. A non-admin or signed-out visit to `/admin` redirects to `/admin/login`; a signed-in non-admin sees "Access restricted" instead of the console.
4. Doctors → Pending tab shows real applications from `submit_doctor_application`; Approve/Reject call `admin_set_doctor_status` and the row moves tabs immediately.
5. Catalog → adding/deactivating a category or service writes directly to `service_categories`/`services` (RLS permits this only for `is_admin()`).
6. Requests → status history expands per request, sourced from `request_status_history`.
7. Users → "Make admin" requires typing the user's exact name to confirm before calling `admin_assign_admin_role`.

## Build & deploy

```bash
npm run build   # tsc -b && vite build, output in dist/
npm run preview # sanity-check the build locally before deploying
```

`dist/` is a static site (no server-side code) -- deploy it to any static
host (Vercel, Netlify, Cloudflare Pages, S3+CloudFront, etc.) with a
catch-all rewrite to `index.html` (React Router needs this for client-side
routes like `/admin/doctors` to work on a hard refresh/direct link). Set
the same environment variables as `.env` in the host's build environment
before running `npm run build` there -- Vite inlines `VITE_*` vars at
build time, so they must be present during the build, not just at runtime.

**Before a real (non-placeholder) deploy**, one thing needs a human to
update it -- it isn't wired into the build:

1. If `src/assets/logo.png` has changed since the last deploy, re-run
   `npm run generate:icons` and `npm run generate:og-image` first and
   commit the regenerated files -- they're checked-in assets, not
   generated as part of `npm run build`.

`public/sitemap.xml` and `public/robots.txt` do **not** need hand-editing:
`vite.config.ts`'s `patchSeoFilesWithSiteUrl` plugin rewrites every
`https://example.com` in both files to the real `VITE_SITE_URL` as part of
`npm run build` (verified: building with `VITE_SITE_URL` set to a real
domain produces a `dist/sitemap.xml`/`dist/robots.txt` with that domain,
not the placeholder). Same mechanism as `index.html`'s `%VITE_SITE_URL%`
interpolation, just via a plugin instead of Vite's native HTML templating
-- see `docs/deployment.md` for the full list of env vars a real deploy
needs set.
