# Release checklist

Run through this for every release of the website (a merge to the
production branch that Vercel deploys). `docs/deployment.md` covers the
deploy mechanics (env vars, domain, Git connection, post-deploy checks) in
detail -- this doc is the overall gate: what has to be true before a
release goes out, including one gate CI can't check for you.

## 1. Legal review gate -- Privacy Policy & Terms

**Required before any release that:**

- changes the text of `/privacy-policy` or `/terms`
  (`src/pages/public/PrivacyPolicyPage.tsx` / `TermsPage.tsx`), or
- changes what the website collects, stores, or sends anywhere (a new form
  field, a new third-party integration, analytics, a new cookie, a new
  "we share data with X" relationship) -- even if the legal pages'
  _text_ doesn't change in the same PR. The policy has to stay true, not
  just unedited.

**A concrete example of exactly what this gate exists to catch**, found
while writing this checklist: the Privacy Policy's "Third parties" section
currently states _"This website doesn't share, sell, or send your
information to any third party, because it doesn't collect any in the
first place beyond a message you choose to send us directly."_ But the
contact form (`src/lib/contactForm.ts`) submits the visitor's name, email,
and message to Formspree -- a third-party form-processing service -- over
`fetch()`. Formspree _is_ a third party receiving that data, so the policy
as currently worded is inaccurate the moment a real (non-placeholder)
`VITE_CONTACT_FORM_ENDPOINT` goes live. This needs a decision (update the
policy to name Formspree as a processor, or change the architecture) before
shipping a real contact-form endpoint to production -- resolving this is a
legal/content call, not an engineering one, which is exactly why this gate
exists separately from CI.

**Checklist:**

- [ ] A legal/compliance reviewer (not the engineer who wrote the change)
      has read the full current text of both pages since the last approved
      version, not just the diff.
- [ ] Every third party that receives user-submitted or user-identifying
      data is named in the Privacy Policy. As of this release: **Formspree**
      (contact form submissions) and **Supabase** (admin console
      authentication -- already scoped as out-of-policy for this site per
      the "What this policy covers" section, since the admin console isn't
      a public-facing data collection surface, but confirm that framing
      still holds if admin-facing data handling changes).
- [ ] If this release adds a new third party, new form field, analytics,
      or cookie, it's named in the appropriate section before merge.
- [ ] "Last updated" date is current if any content changed.
- [ ] Jurisdiction-specific obligations reviewed if relevant (e.g. India's
      DPDP Act, GDPR for EU visitors, CCPA for California visitors) --
      confirm nothing in this release changes what's required.
- [ ] Sign-off recorded in the PR description or linked ticket (reviewer
      name + date), not just verbal or chat approval.

This gate blocks the release the same way a failing CI check does. Do not
merge or deploy a release with known-stale or unreviewed legal copy even
if every other check below is green.

## 2. CI gate

- [ ] The PR's CI run (`.github/workflows/website-ci.yml`) is green: lint,
      format check, typecheck + build, unit/component tests, broken-link
      check, the full 5-project Playwright matrix (Chromium/Firefox/WebKit
      × desktop/mobile), and Lighthouse CI thresholds.
- [ ] If the release touches `HeroSection.tsx`, its illustration, or
      anything else in the hero's rendered output, the visual-regression
      baselines (`e2e/visual.spec.ts-snapshots/`) were reviewed and, if the
      change is intentional, regenerated
      (`npx playwright test e2e/visual.spec.ts --update-snapshots`, committed).
- [ ] If accessibility, performance, or SEO work landed in this release,
      `docs/hardening-report.md` reflects it (or a follow-up entry is
      planned).

## 3. Configuration gate

- [ ] Any new `VITE_*` environment variable is added in all three places:
      `.env.example`, `src/vite-env.d.ts`, and the table in
      `docs/deployment.md` -- and is actually set in the Vercel dashboard
      for both Production and Preview.
- [ ] If this release makes the browser talk to a new external origin
      (a `fetch`/`XHR` target, an embed, a new font/script CDN),
      `vercel.json`'s CSP (`connect-src`/`img-src`/`script-src`/etc.) is
      updated to allow it, and `docs/security-headers.md` explains why --
      an un-updated CSP fails the feature silently in production (blocked
      by the browser) even though it worked fine in local dev, where no
      CSP is enforced.

## 4. Deploy

- [ ] Sections 1-3 above are all checked off.
- [ ] Merge to the production branch (`main`). Vercel deploys
      automatically -- see `docs/deployment.md` for first-time Git
      connection setup; routine releases need no manual deploy step.

## 5. Post-deploy verification

- [ ] Run `docs/deployment.md`'s full post-deploy checklist against the
      live URL (sitemap/robots, direct-URL route checks including a
      refresh, OG preview via a link debugger, live Lighthouse run, store
      link checks, HTTPS/header checks, a real contact-form submission).

## 6. Rollback, if needed

- [ ] **Vercel → Deployments → select the last known-good deployment →
      "Promote to Production"** is the fastest rollback: no new build, no
      DNS change, live again in seconds. Prefer this over reverting the
      commit and waiting for a fresh build when something is actively
      broken in production; revert the commit afterward at normal pace
      once the rollback has stopped the bleeding.
