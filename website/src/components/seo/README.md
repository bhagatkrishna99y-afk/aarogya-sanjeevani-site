# seo

Document-head and structured-data concerns -- kept separate from
`components/ui/` because these aren't visual building blocks, and from
`PageMeta`/per-page usage because `StructuredData` is rendered exactly
once, site-wide.

- `Seo.tsx` -- per-page `<title>`/meta description/canonical/OG/Twitter
  tags. Render once near the top of every page's JSX.
- `StructuredData.tsx` -- Organization + WebSite JSON-LD. Rendered once,
  in `PublicLayout`, not per-page.

See `docs/seo.md` for the full rationale (why `index.html` also carries
its own static OG/Twitter copy, what JSON-LD a client-only SPA can and
can't do for crawlers that don't execute JS, etc).
