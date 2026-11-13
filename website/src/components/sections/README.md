# sections

Larger, page-specific composed blocks (a homepage hero, a pricing table,
a testimonials strip) that are too big to call "ui" but aren't reused
across unrelated pages.

The Home page (`src/pages/public/HomePage.tsx`) is the first user: Hero,
Benefits, HowItWorks, Services, TrustSafety, AppDownload, Faq and
FinalCta, each composed from `src/content/home.ts` + `src/components/ui/*`.
