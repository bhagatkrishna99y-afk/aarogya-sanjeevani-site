# Design tokens

Source of truth for values: `src/styles/index.css`'s `@theme` block (Tailwind
v4 is CSS-first -- that block _is_ this project's token configuration; see
its header comment for why there's no `tailwind.config.js`). This doc exists
to state, in one place, exactly which pairings were verified and what their
ratio is -- the live `/styleguide` route (dev-only) renders every swatch
with the same numbers, computed the same way, so the two can never silently
drift apart from each other without one of them visibly being wrong.

All ratios below were computed with the WCAG 2 relative-luminance contrast
formula (not estimated, not taken from a design tool's preview) --
see the `contrast()` helper in `src/styleguide/contrast.ts`, which both the
styleguide page and this doc's numbers come from.

**Rule used throughout:** WCAG AA requires 4.5:1 for normal text, 3:1 for
large text (≥24px, or ≥18.66px bold) and for non-text UI components
(borders, icon glyphs, focus indicators). Every pairing below states which
bar it was checked against.

## Color: primary -- `brand` (deep teal)

Pre-existing scale, values unchanged by this work.

| Shade   | Hex       | Role                                                  |
| ------- | --------- | ----------------------------------------------------- |
| 50      | `#f0fdfa` | Soft surface (paired with 700 text below)             |
| 100-500 | —         | Decorative tints/steps, not used for text             |
| 600     | `#0d9488` | **Decorative only** -- borders, icons, the focus ring |
| 700     | `#0f766e` | **Text-safe** -- body/heading text, solid-button fill |
| 800     | `#115e59` | Text-safe, higher contrast                            |
| 900     | `#134e4a` | Text-safe, highest contrast                           |

| Pairing                                           | Ratio  | Bar   | Result   |
| ------------------------------------------------- | ------ | ----- | -------- |
| `brand-700` text on white                         | 5.47:1 | 4.5:1 | PASS     |
| white text on `brand-700` (solid button)          | 5.47:1 | 4.5:1 | PASS     |
| `brand-700` text on `brand-50` (nav active state) | 5.25:1 | 4.5:1 | PASS     |
| `brand-800` text on white                         | 7.58:1 | 4.5:1 | PASS     |
| `brand-900` text on white                         | 9.48:1 | 4.5:1 | PASS     |
| `brand-600` as a border/ring/icon on white        | 3.74:1 | 3:1   | PASS     |
| white text on `brand-600` (solid button)          | 3.74:1 | 4.5:1 | **FAIL** |

**`brand-600` must never carry white text that needs to be read as a
sentence or label** (it's fine for a large standalone glyph/icon, where
only 3:1 applies). This was an actual pre-existing bug: the homepage's
"Browse services" button used `bg-brand-600` with white text at 3.74:1 --
fixed to `bg-brand-700` as part of this work (now 5.47:1).

## Color: accent -- warm CTA (orange)

New scale, chosen as the near-complement of the teal primary -- warm and
energetic without reading as an alarm color (that's what `error` is for).

| Shade | Hex       | Role                                                                                                                                      |
| ----- | --------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| 50    | `#fff7ed` | Soft surface                                                                                                                              |
| 500   | `#f97316` | Swatch/illustrative only -- **fails even the 3:1 bar on white** (2.80:1), never use for a border/icon/text directly on a white background |
| 600   | `#ea580c` | **Decorative only** (3:1) -- borders, icons                                                                                               |
| 700   | `#c2410c` | **Text-safe** -- solid-button fill, link text                                                                                             |
| 800   | `#9a3412` | Text-safe on `accent-50`                                                                                                                  |
| 900   | `#7c2d12` | Text-safe on `accent-50`, highest contrast                                                                                                |

| Pairing                                   | Ratio  | Bar   | Result   |
| ----------------------------------------- | ------ | ----- | -------- |
| white text on `accent-700` (solid button) | 5.18:1 | 4.5:1 | PASS     |
| `accent-800` text on `accent-50`          | 6.88:1 | 4.5:1 | PASS     |
| `accent-900` text on `accent-50`          | 8.83:1 | 4.5:1 | PASS     |
| `accent-600` as a border/icon on white    | 3.56:1 | 3:1   | PASS     |
| `accent-500` as a border/icon on white    | 2.80:1 | 3:1   | **FAIL** |

## Color: semantic (success / warning / error / info)

Same two-role pattern as above for every one of the four: a `-50` soft
surface, a `-700` ink that is text-safe both on that surface and as a
solid white-text button/badge fill, and a mid shade (`-500`/`-600`) that is
decorative-only.

| Semantic            | Soft surface           | Text/solid ink          | Soft-surface ratio | Solid white-text ratio |
| ------------------- | ---------------------- | ----------------------- | ------------------ | ---------------------- |
| `success` (emerald) | `success-50` `#ecfdf5` | `success-700` `#047857` | 5.21:1             | 5.48:1                 |
| `warning` (amber)   | `warning-50` `#fffbeb` | `warning-700` `#b45309` | 4.84:1             | 5.02:1                 |
| `error` (red)       | `error-50` `#fef2f2`   | `error-700` `#b91c1c`   | 5.91:1             | 6.47:1                 |
| `info` (sky)        | `info-50` `#f0f9ff`    | `info-700` `#0369a1`    | 5.57:1             | 5.93:1                 |

`warning-700` has the tightest margin in the whole palette (4.84:1 against
its own soft surface) -- never substitute a lighter warning shade for text.

A decorative-only exception worth knowing: `warning-500` (`#f59e0b`) _can_
carry **dark** text (`slate-900`, 8.31:1) as a solid badge fill -- just
never white text on it (that pairing was not checked and should be assumed
to fail until verified, consistent with every other `-500`/`-600` shade in
this palette).

## Color: neutral / background -- `slate`

Tailwind's built-in `slate` scale, already used throughout the site before
this design system existed -- kept as the neutral scale rather than adding
a redundant `neutral-*` alias for the same colors.

| Pairing                        | Ratio   | Bar   | Result                  |
| ------------------------------ | ------- | ----- | ----------------------- |
| `slate-900` text on white      | 17.85:1 | 4.5:1 | PASS                    |
| `slate-700` text on white      | 10.35:1 | 4.5:1 | PASS                    |
| `slate-600` text on white      | 7.58:1  | 4.5:1 | PASS                    |
| `slate-500` text on white      | 4.76:1  | 4.5:1 | PASS (slim margin)      |
| `slate-900` text on `slate-50` | 17.06:1 | 4.5:1 | PASS                    |
| `slate-600` text on `slate-50` | 7.24:1  | 4.5:1 | PASS                    |
| `slate-500` text on `slate-50` | 4.55:1  | 4.5:1 | PASS (very slim margin) |
| `slate-400` text on white      | 2.56:1  | 4.5:1 | **FAIL**                |

**`slate-500` passes AA by a slim margin on both backgrounds** (4.76:1 /
4.55:1) -- fine for secondary/caption text, but prefer `slate-600` when
there's room, since it clears the bar much more comfortably (7.58:1 /
7.24:1). **`slate-400` must never be used for real text** -- decorative or
disabled-state use only, where no contrast minimum applies.

## Typography

- **Headings**: Figtree (500/600/700 weights imported) -- a humanist
  geometric sans with a touch of warmth, used for every `h1`-`h6`.
- **Body**: Noto Sans (400/500/600 weights imported) -- chosen specifically
  for legibility (built for maximum script/size coverage) over a more
  stylized body font, appropriate for healthcare copy that needs to be read
  correctly, not just quickly skimmed.
- Both are self-hosted via **Fontsource** (`@fontsource/figtree`,
  `@fontsource/noto-sans`), imported as plain CSS in `src/styles/index.css`
  -- bundled by Vite like any other asset, never a render-blocking request
  to an external font host.
- **Type scale**: Tailwind's own default `text-*` scale (`xs` through
  `9xl`) -- not reinvented, since it's already a coherent modular
  progression with sensible paired line-heights. Body copy uses `text-base`
  (16px) with Tailwind's default 1.5 line-height (`leading-normal`) at
  minimum; anywhere body copy needs more breathing room, `leading-relaxed`
  (1.625) is used instead -- never below 1.5 for a paragraph of body text.

## Spacing, radius, shadow

All three are Tailwind's own default scales, unmodified:

- **Spacing**: Tailwind's scale is already a 4px rhythm (`space-1` = 4px,
  `space-2` = 8px, `space-4` = 16px, `space-6` = 24px, `space-8` = 32px, ...).
  Components in this project stick to multiples of `1`/`2` (4px/8px steps)
  for padding/gaps -- never an arbitrary pixel value.
- **Radius**: `rounded-md` (buttons, inputs, badges-as-pills use
  `rounded-full` instead), `rounded-lg`/`rounded-xl` (cards).
- **Shadow**: `shadow-sm` (cards at rest), `shadow-md` (cards/menus on
  hover or elevated state). Nothing heavier -- a flat, low-shadow look
  reads calmer and more clinical than heavy elevation, which is also why
  this project did **not** adopt the "Neumorphism" style a general design
  search suggested for healthcare UI: that style's own accessibility risk
  is rated high (low-contrast embossed surfaces), which directly conflicts
  with the AA-everywhere requirement this document exists to prove.

## Dark mode

Not implemented. The brief marked it optional, and doing it properly would
mean re-deriving and re-verifying every pairing above against dark
surfaces independently (a dark background doesn't just invert these
values safely) -- real additional scope, not a flag to flip. If it's added
later: respect `prefers-color-scheme` (never a manual-only toggle as the
sole mechanism), and re-run the same contrast verification against the
dark palette before shipping it, exactly as this document does for light
mode.
