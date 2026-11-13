/**
 * Lighthouse CI config. `npm run lhci` (= `lhci autorun`) builds the site,
 * serves the `dist/` output, runs Lighthouse against every public page on
 * a mobile emulation profile, and fails the process (non-zero exit) if any
 * page scores below the thresholds in `assertions` -- wired into
 * .github/workflows/website-ci.yml so a regression fails the build, not
 * just a local run.
 *
 * Only public pages are audited: /admin/* requires a real signed-in
 * session to render anything meaningful, which isn't available in CI, and
 * the dev-only /styleguide route is excluded from production builds
 * entirely (see router.tsx).
 */
module.exports = {
  ci: {
    collect: {
      // Not `staticDistDir`: that mode serves dist/ with a plain static
      // file server with no SPA fallback, so a client-side route like
      // /services (which has no real dist/services/index.html -- Vite
      // only emits one index.html for the single entry point) 404s.
      // `vite preview` serves exactly what production hosting should
      // (see README.md's "Build & deploy" -- the same catch-all rewrite
      // requirement), so every route below resolves correctly.
      startServerCommand: "npm run preview -- --port 4173 --strictPort",
      // Not "Local:" -- Vite prints that word wrapped in ANSI bold-on/off
      // escape codes with the colon *outside* them, so the literal
      // substring "Local:" never appears contiguously in the raw output.
      startServerReadyPattern: "Local",
      url: [
        "http://localhost:4173/",
        "http://localhost:4173/services",
        "http://localhost:4173/about",
        "http://localhost:4173/contact",
        "http://localhost:4173/privacy-policy",
        "http://localhost:4173/terms",
      ],
      // Lighthouse's timing-based metrics (TBT especially) are noisy on a
      // single run -- 3 runs + the median (LHCI's default aggregation) is
      // the documented way to get a score that isn't one unlucky sample.
      numberOfRuns: 3,
      // No `preset` -- Lighthouse's default (no preset/formFactor override)
      // already emulates a mid-tier mobile device with mobile network
      // throttling, which is what "mobile" means here. "mobile" isn't
      // actually a valid --preset value (only "perf"/"experimental"/
      // "desktop" are); the default IS the mobile profile we want.
    },
    assert: {
      assertions: {
        "categories:performance": ["error", { minScore: 0.9 }],
        "categories:accessibility": ["error", { minScore: 0.9 }],
        "categories:best-practices": ["error", { minScore: 0.9 }],
        "categories:seo": ["error", { minScore: 0.9 }],
      },
    },
    upload: {
      target: "temporary-public-storage",
    },
  },
};
