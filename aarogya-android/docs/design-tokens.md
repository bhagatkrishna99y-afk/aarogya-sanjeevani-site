# Design tokens -- Android (`:core:designsystem`)

Source of truth for values: `core/designsystem/src/main/kotlin/.../theme/Color.kt`.
This doc mirrors `website/docs/design-tokens.md` in structure and exists for
the same reason that one does: state, in one place, exactly which pairings
were verified and what their ratio is, computed the same way every time, so
the doc and the code can't silently drift from each other without one of
them visibly being wrong.

**Formula**: WCAG 2 relative-luminance contrast, the same implementation the
website's own `contrast()` helper uses (`website/src/styleguide/contrast.ts`)
-- not estimated, not read off a design tool's preview. Verified with a
one-off Node script against every pairing below before any of it was
written into `Color.kt`/`Theme.kt`/`ExtendedColors.kt`.

**Rule used throughout**: WCAG AA requires 4.5:1 for normal text and 3:1 for
large text / non-text UI components (icon glyphs, borders). Every pairing
below is a real text-or-icon-on-background pairing used somewhere in the
theme, so all of them are held to the 4.5:1 bar unless stated otherwise.

## Where each hue comes from

Every scale is the **same brand palette as the website**
(`website/docs/design-tokens.md`, itself sourced from Tailwind shades) --
not a separately invented Android palette. `Color.kt`'s own comments map
each `BrandNN`/`AccentNN`/etc. tone number to the Tailwind shade it mirrors;
this doc only repeats that mapping where it matters for a specific ratio.

| Family                      | Hue               | Tailwind scale | Used for                                            |
| ---------------------------- | ----------------- | --------------- | ---------------------------------------------------- |
| `Brand`                      | teal               | `teal`           | primary + secondary (two tone-pairs, one hue)         |
| `Accent`                      | warm orange        | `orange`         | tertiary                                              |
| `Success`                     | emerald             | `emerald`        | success + request "completed" / doctor "approved"     |
| `Warning`                     | amber               | `amber`          | warning + request "accepted" / doctor "pending"       |
| `Info`                        | sky                | `sky`            | info + request "requested"                            |
| `Error`                       | red                 | `red`            | Material `error` + request "rejected" / doctor "rejected" |
| `Violet`                      | violet              | `violet`         | doctor "suspended" only -- kept apart from `Error` red so a suspended doctor is never confused with a rejected one at a glance |
| `Neutral`                     | slate               | `slate`          | background/surface/outline + request "cancelled"      |

Tone numbers follow Material 3 convention (0 = black, 100 = white). A tone's
Tailwind-shade mapping is in `Color.kt`'s own inline comments.

## Light scheme

| Pairing                                                | Ratio   | Result |
| ------------------------------------------------------- | ------- | ------ |
| `onPrimary` (white) on `primary` (Brand40/teal-700)      | 5.47:1  | PASS   |
| `onPrimaryContainer` (Brand10) on `primaryContainer` (Brand95) | 12.84:1 | PASS   |
| `onSecondary` (white) on `secondary` (Brand30/teal-800)  | 7.58:1  | PASS   |
| `onSecondaryContainer` (Brand10) on `secondaryContainer` (Brand90) | 11.48:1 | PASS |
| `onTertiary` (white) on `tertiary` (Accent40/orange-700) | 5.18:1  | PASS   |
| `onTertiaryContainer` (Accent10) on `tertiaryContainer` (Accent95) | 13.66:1 | PASS |
| `onError` (white) on `error` (Error40/red-700)           | 6.47:1  | PASS   |
| `onErrorContainer` (Error10) on `errorContainer` (Error90) | 11.16:1 | PASS   |
| `onBackground` (Neutral10) on `background` (Neutral95)   | 17.06:1 | PASS   |
| `onSurface` (Neutral10) on `surface` (Neutral99/white)   | 17.85:1 | PASS   |
| `onSurfaceVariant` (Neutral30) on `surfaceVariant` (Neutral90) | 9.45:1  | PASS   |
| `outline` (Neutral50/slate-500) as a UI element on `surface` (white) | 4.76:1 | PASS (slim margin -- same caution as the website doc's slate-500 entry) |
| `inverseOnSurface` (Neutral95) on `inverseSurface` (Neutral20) | 13.98:1 | PASS |
| text (Neutral10) on `surfaceContainerHighest` (Neutral70) | 12.02:1 | PASS   |

## Dark scheme

| Pairing                                                | Ratio   | Result |
| ------------------------------------------------------- | ------- | ------ |
| `onPrimary` (Brand10) on `primary` (Brand80)             | 9.78:1  | PASS   |
| `onPrimaryContainer` (Brand95) on `primaryContainer` (Brand30) | 6.73:1  | PASS   |
| `onSecondary` (Brand20) on `secondary` (Brand90)         | 8.00:1  | PASS   |
| `onSecondaryContainer` (Brand95) on `secondaryContainer` (Brand30) | 6.73:1 | PASS |
| `onTertiary` (Accent10) on `tertiary` (Accent80)         | 9.28:1  | PASS   |
| `onTertiaryContainer` (Accent95) on `tertiaryContainer` (Accent30) | 6.38:1 | PASS |
| `onError` (Error20) on `error` (Error80)                 | 5.28:1  | PASS   |
| `onErrorContainer` (Error90) on `errorContainer` (Error30) | 5.74:1  | PASS   |
| `onBackground` (Neutral95) on `background` (Neutral0)    | 19.28:1 | PASS   |
| `onSurfaceVariant` (Neutral80) on `surfaceVariant` (Neutral20) | 11.87:1 | PASS |
| `outline` (Neutral60) as a UI element on `surface` (Neutral0) | 7.87:1 | PASS   |
| `inverseOnSurface` (Neutral20) on `inverseSurface` (Neutral90) | 13.35:1 | PASS |
| text (Neutral95) on `surfaceContainerHighest` (Neutral40) | 7.24:1  | PASS   |

## Extended colors -- success / warning / info

Same two-role pattern as the website doc: a very-light container (tone 95 in
light, tone 30 in dark) paired with a near-black/near-white ink (tone 10 /
tone 90) that's text-safe against it.

| Semantic  | Light: container / onContainer       | Ratio   | Dark: container / onContainer      | Ratio  |
| --------- | ------------------------------------- | ------- | ------------------------------------ | ------ |
| `success` | Success95 / Success10                 | 14.38:1 | Success30 / Success90                | 5.99:1 |
| `warning` | Warning95 / Warning10                 | 14.44:1 | Warning30 / Warning90                | 5.69:1 |
| `info`    | Info95 / Info10                       | 13.02:1 | Info30 / Info90                      | 5.70:1 |

(`success`/`warning`/`info`'s own "main" tone -- `Success40`/`Warning40`/`Info40`
in light, `Success80`/`Warning80`/`Info80` in dark -- follow the exact same
tone pattern as `error` above and clear the same bar; not re-tabulated.)

## Status colors -- request status

One container/on-container pair per `AsRequestStatus` (see `StatusChip.kt`),
always rendered together (icon + label both drawn in `onContainer` on top of
`container`) -- never color alone, so the distinction survives
color-blindness and TalkBack equally.

| Status      | Hue      | Light ratio | Dark ratio |
| ----------- | -------- | ------------ | ----------- |
| Requested   | Info     | 13.02:1      | 5.70:1      |
| Accepted    | Warning  | 14.44:1      | 5.69:1      |
| Confirmed   | Brand    | 12.84:1      | 6.02:1      |
| Completed   | Success  | 14.38:1      | 5.99:1      |
| Rejected    | Error    | 13.22:1      | 5.74:1      |
| Cancelled   | Neutral  | 16.30:1      | 9.45:1      |

## Status colors -- doctor status

| Status    | Hue      | Light ratio | Dark ratio |
| --------- | -------- | ------------ | ----------- |
| Pending   | Warning  | 14.44:1      | 5.69:1      |
| Approved  | Success  | 14.38:1      | 5.99:1      |
| Rejected  | Error    | 13.22:1      | 5.74:1      |
| Suspended | Violet   | 12.83:1      | 6.47:1      |

## Decorative-only tones -- never solid fill + text

Same caution the website doc makes for `accent-500`/`accent-600`: a handful
of mid-scale tones exist for borders/icons/illustrative swatches only and
must never carry body text or sit behind white text as a solid button fill.

| Pairing                                      | Ratio  | Bar checked | Result                         |
| --------------------------------------------- | ------ | ----------- | ------------------------------- |
| White text on `Brand50` (teal-600, decorative) | 3.74:1 | 4.5:1        | **FAIL** -- border/icon use only |
| White text on `Accent50` (orange-600, decorative) | 3.56:1 | 4.5:1     | **FAIL** -- border/icon use only |

Both pass the 3:1 non-text/large-text bar (which is why they're fine as a
decorative border or a standalone icon glyph), which is also exactly why
they must never be reached for as a solid button fill with a text label on
top -- the same mistake the website's own `brand-600` button bug was, before
it got fixed to `brand-700`.

## Typography, shape, spacing, elevation

- **Typography**: Figtree (headings) + Noto Sans (body) -- the same pairing
  as the website (`website/docs/design-tokens.md`'s Typography section),
  fetched via Compose's downloadable-fonts API (`Font.kt`) rather than
  bundled binaries, so both platforms read as one brand. Material 3's own
  baseline type scale (sizes/line-heights/letter-spacing) is unchanged, same
  call the website made keeping Tailwind's scale rather than reinventing it.
- **Shape**: `AarogyaShapes` (`Shape.kt`) -- 8/12/16/20/24dp, rounder than
  Material's own defaults for the "premium, rounded surfaces" brief.
- **Spacing**: `AsSpacing` (`Spacing.kt`) -- 0/4/8/12/16/24/32dp, a 4dp
  rhythm matching the website's own Tailwind-derived scale.
- **Elevation**: `AsElevation` (`Elevation.kt`) -- Material 3's own levels
  (0/1/3/6/8/12dp), named rather than left as bare `.dp` literals at call
  sites. Most surfaces stay at level0/level1: a flat, low-shadow look reads
  calmer and more clinical than heavy elevation, the same call the website
  design system made (see its own "Shadow" section).

## Dark mode

Implemented (unlike the website, which left it as explicitly optional/future
work) -- `AarogyaTheme` takes a `themeMode: ThemeMode` (`System`/`Light`/`Dark`),
defaulting to following the system setting, with Profile able to override
it later via that same parameter. Every pairing in this doc's dark-scheme
and status-color tables was independently verified against the dark
palette, not assumed safe by inverting the light numbers -- exactly the
caution the website doc calls for if dark mode is ever added there.

## Dynamic color

Opt-in (`useDynamicColor` on `AarogyaTheme`, off by default) -- when it's on,
only Material's own `ColorScheme` (primary/secondary/tertiary/etc.) is
replaced by the wallpaper-derived dynamic scheme. `AsTheme.extendedColors`
(success/warning/info and every status color) is **never** derived from
dynamic color and always comes from this doc's fixed, verified palette --
status meaning has to stay legible and consistent regardless of which
`ColorScheme` is active.
