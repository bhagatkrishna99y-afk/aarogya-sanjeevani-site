# Aarogya Sanjeevani — Website

Public marketing site (service discovery, information) plus a role-gated
operations console for admins, built with React, TypeScript, Vite, React
Router and Tailwind CSS, talking directly to Supabase.

## Routes

- `/`, `/services`, `/about`, `/contact` — public, no auth required. Services
  are read live from the Supabase catalog (RLS allows anonymous reads of
  active rows).
- `/admin/login` — email/password sign-in.
- `/admin/*` — gated by `RequireAdmin`: requires a signed-in session **and**
  `profiles.role === 'admin'`. Covers the dashboard, doctor application
  review (approve/reject/suspend/reactivate), catalog management, request
  monitoring with status history, and user role management.

## Setup

```bash
npm install
cp .env.example .env   # already filled in with this project's URL + publishable key
npm run dev
```

## Environment variables

- `VITE_SUPABASE_URL` — the Supabase project URL.
- `VITE_SUPABASE_PUBLISHABLE_KEY` — the project's publishable (`sb_publishable_...`) key. Safe for the client; all access control is enforced server-side via RLS and RPCs, never by hiding this key.

## Verifying the admin console

1. Bootstrap an admin (see `backend/README.md` — "Bootstrapping the first admin").
2. `npm run dev`, open `/admin`, sign in. You should land on `/admin` (dashboard with live counts).
3. A non-admin or signed-out visit to `/admin` redirects to `/admin/login`; a signed-in non-admin sees "Access restricted" instead of the console.
4. Doctors → Pending tab shows real applications from `submit_doctor_application`; Approve/Reject call `admin_set_doctor_status` and the row moves tabs immediately.
5. Catalog → adding/deactivating a category or service writes directly to `service_categories`/`services` (RLS permits this only for `is_admin()`).
6. Requests → status history expands per request, sourced from `request_status_history`.
7. Users → "Make admin" requires typing the user's exact name to confirm before calling `admin_assign_admin_role`.

## Build

```bash
npm run build   # tsc -b && vite build, output in dist/
```
