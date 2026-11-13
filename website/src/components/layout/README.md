# layout

Page-shell components that render chrome around a route via `<Outlet />`
(header/nav/footer, sidebars) -- not the pages themselves.

- `PublicLayout.tsx` -- the public site shell: skip link, `Header`,
  `<main>` (the route-change focus target), `Footer`.
- `Header.tsx` / `MobileMenu.tsx` -- the sticky header and its mobile
  slide-in nav panel (focus-trapped, Escape-to-close).
- `Footer.tsx` -- brand blurb, quick/legal links, contact, app store
  badges, the non-emergency disclaimer, and the copyright line.
- `AdminLayout.tsx` -- the `/admin/*` ops-console shell (sidebar nav +
  sign-out).
