# Deploying to Vercel

This is a static SPA (Vite + React Router, no server-side code) living in
`website/` inside a larger monorepo (`backend/`, `apps/` are siblings).
`website/vercel.json` carries the SPA rewrite, security headers, and cache
rules -- see `docs/security-headers.md` for why each header is set the way
it is. This doc covers everything else: what to configure in the Vercel
dashboard, the production domain, and connecting the repo for PR previews.

## 1. Environment variables

Set these in **Vercel → Project → Settings → Environment Variables**. Vite
inlines every `VITE_*` var into the client bundle at build time, so they
must be set _before_ a build runs, not just at runtime -- there is no way
to change one without triggering a new deploy.

| Variable                        | Example / where it comes from                                   | Notes                                                                                                                                                                                                                                                             |
| ------------------------------- | --------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `VITE_SUPABASE_URL`             | `https://oleazbruvnlmuobqomge.supabase.co`                      | This project's real Supabase project URL. Also appears in `vercel.json`'s CSP `connect-src` -- if this ever changes, update both.                                                                                                                                 |
| `VITE_SUPABASE_PUBLISHABLE_KEY` | Supabase dashboard → Project Settings → API → "anon public" key | This is the **publishable/anon** key, not a secret -- it's safe (and necessary) to expose client-side. Never put a `service_role` key in a `VITE_*` var; it would end up readable in the shipped JS bundle.                                                       |
| `VITE_SITE_URL`                 | `https://aarogyasanjeevani.com` (your real production domain)   | Drives canonical/OG URLs in `Seo.tsx`, the JSON-LD in `StructuredData.tsx`, and `sitemap.xml`/`robots.txt` (auto-rewritten at build time by `vite.config.ts`'s `patchSeoFilesWithSiteUrl` plugin -- see `docs/seo.md`). See the Preview-deployments caveat below. |
| `VITE_PLAY_STORE_PATIENT_URL`   | Real Play Store listing URL for the patient app                 | Used by the "Get the Patient App" store badge.                                                                                                                                                                                                                    |
| `VITE_PLAY_STORE_DOCTOR_URL`    | Real Play Store listing URL for the doctor app                  | Used by the "For Doctors" store badge.                                                                                                                                                                                                                            |
| `VITE_CONTACT_EMAIL`            | A real monitored inbox                                          | Shown on `/contact` and in `mailto:` links on the legal pages.                                                                                                                                                                                                    |
| `VITE_CONTACT_PHONE`            | A real monitored number                                         | Shown on `/contact`.                                                                                                                                                                                                                                              |
| `VITE_CONTACT_FORM_ENDPOINT`    | A real Formspree form URL (`https://formspree.io/f/<id>`)       | See `src/lib/contactForm.ts`. If you use a different form-to-email provider, its origin also needs adding to `vercel.json`'s CSP `connect-src` -- the browser will silently block the submission otherwise.                                                       |

Apply all of these to both the **Production** and **Preview** environments
in the dashboard (Vercel lets you scope each variable per environment), so
pull-request preview deployments render real content instead of the
Supabase/contact-form client failing to initialize.

**`VITE_SITE_URL` on preview deployments** is a known, accepted gap: each
preview gets its own unique `*.vercel.app` URL that isn't known until
Vercel creates it, so a single dashboard value can't match every preview's
real address. Setting it to the production domain for all environments
means preview deployments will show the production domain in their
canonical/OG tags and sitemap -- harmless in practice (previews aren't
meant to be indexed or widely shared), but worth knowing rather than being
surprised by it.

## 2. Production domain: apex + `www`, HTTPS

1. In **Vercel → Project → Settings → Domains**, add both the apex domain
   (`aarogyasanjeevani.com`) and the `www` subdomain
   (`www.aarogyasanjeevani.com`).
2. Pick which one is canonical. When you add the second domain, Vercel
   offers to redirect it to the first -- either direction works; pick one
   and stay consistent. (This project has no cookie or analytics reason to
   prefer one over the other, so default to redirecting the apex →
   `www`, or the reverse, whichever matches how you've advertised the URL
   elsewhere.)
3. Update DNS at your registrar: Vercel's dashboard shows the exact
   records to add once a domain is pending verification --
   **don't hand-type an IP from memory**, use what the dashboard displays
   at the time, since Vercel's anycast addresses can change. Typically an
   `A` record (or `ALIAS`/`ANAME` if your registrar supports apex aliasing)
   for the apex, and a `CNAME` to `cname.vercel-dns.com` for `www`.
4. Vercel automatically provisions and renews a free TLS certificate
   (Let's Encrypt) for every verified domain, and redirects plain `http://`
   requests to `https://` -- no extra configuration needed beyond DNS
   verification. `vercel.json`'s `Strict-Transport-Security` header (see
   `docs/security-headers.md`) then tells browsers to never try `http://`
   again for this origin.
5. Once the domain is live, set `VITE_SITE_URL` (Production environment)
   to the chosen canonical `https://` URL and redeploy, so the canonical
   tags, OG tags, JSON-LD, and `sitemap.xml`/`robots.txt` all pick it up.

## 3. Connect the Git repository for automatic PR previews

1. **Vercel → Add New → Project → Import Git Repository**, and authorize
   access to this repo (GitHub/GitLab/Bitbucket).
2. **Root Directory**: set to `website` -- this is a monorepo
   (`backend/`, `apps/`, `website/` as siblings with no root
   `package.json`), so Vercel needs to be told where the actual site
   lives. `website/vercel.json`'s `framework`/`buildCommand`/
   `outputDirectory` fields pin the build explicitly as a backstop, but
   Root Directory still has to be set correctly for Vercel to find that
   file at all.
3. **Framework Preset**: should auto-detect as Vite once Root Directory is
   set; leave it as-is.
4. Add every environment variable from the table above, for both
   Production and Preview.
5. _(Recommended, optional)_ Set a custom **Ignore Build Step** so changes
   to `backend/` or `apps/` alone don't trigger a website rebuild --
   mirrors the `paths: website/**` filter already used in
   `.github/workflows/website-ci.yml`:
   ```
   git diff --quiet HEAD^ HEAD -- .
   ```
   (run with the working directory set to `website/`, which Root Directory
   already does) -- exit code `0` tells Vercel to skip the build.
6. Save. From here on, Vercel's Git integration is automatic: every push
   to the production branch deploys to production, and **every pull
   request gets its own preview deployment** at a unique URL, posted as a
   check/comment on the PR -- no further configuration needed.

## 4. Post-deploy checklist

Run through this against the **live URL**, not local `vite preview` --
real DNS, real TLS, and the real `vercel.json` headers can all behave
differently from a local build.

- [ ] Visit `/sitemap.xml` and `/robots.txt` on the live domain; confirm
      every `<loc>` and the `Sitemap:` line show the real production
      domain, not `example.com` or `localhost`.
- [ ] Direct-hit every public route **by URL** (type it in, don't
      navigate in-app) and **hit refresh** on each: `/`, `/services`,
      `/about`, `/contact`, `/privacy-policy`, `/terms`, `/admin`,
      `/admin/login` -- confirms `vercel.json`'s SPA rewrite is wired
      correctly (a refresh 404ing on a deep link is the single most common
      SPA-hosting misconfiguration).
- [ ] Direct-hit an unknown path (e.g. `/this-does-not-exist`) and confirm
      the site's own 404 page renders, not the hosting platform's default 404.
- [ ] Paste the live homepage URL (and at least one inner page) into a
      link debugger -- Facebook's Sharing Debugger, Twitter/X's Card
      Validator, or LinkedIn's Post Inspector -- and confirm the OG
      title/description/image resolve to the real content, not the
      `example.com` placeholder or a blank image.
- [ ] Run Lighthouse against the **live** URL (Chrome DevTools or
      PageSpeed Insights), at least for the homepage, and confirm
      Performance/Accessibility/Best Practices/SEO are still ≥ 90 on
      mobile -- a live CDN-fronted domain with real headers can score
      differently than the `npm run lhci` check CI runs against a local
      `vite preview`.
- [ ] Click both store badges ("Get the Patient App" / "For Doctors") on
      the live site and confirm they open the real App Store/Play Store
      listings configured for this environment, not the `.env.example`
      placeholders.
- [ ] Load the apex and `www` over plain `http://` and confirm both
      redirect to `https://` and to the chosen canonical host.
- [ ] Open DevTools' Network tab on the live site and confirm
      `Content-Security-Policy`, `Strict-Transport-Security`, and the
      other headers from `vercel.json` are actually present on the
      response -- a typo in a header rule's `source` pattern fails silently
      (the rule just never matches) rather than erroring.
- [ ] Submit the contact form on the live site for real (not mocked) and
      confirm the message actually arrives (e.g. in the Formspree
      dashboard/inbox) -- this is the one feature the e2e suite only ever
      exercises against a mock, so a live end-to-end check is the only way
      to catch a misconfigured `VITE_CONTACT_FORM_ENDPOINT` or a CSP
      `connect-src` that's missing the real provider's origin.
