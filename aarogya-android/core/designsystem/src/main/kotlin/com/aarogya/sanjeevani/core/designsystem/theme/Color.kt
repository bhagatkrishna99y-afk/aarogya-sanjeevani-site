@file:Suppress("MagicNumber") // every hex literal below *is* the named constant detekt wants it defined as

package com.aarogya.sanjeevani.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Every scale here is the same brand palette as the marketing website
// (website/docs/design-tokens.md, itself sourced from
// website/src/styles/index.css's `@theme` block) -- one brand across
// platforms. Tone numbers follow Material 3 convention (0 = black,
// 100 = white; lower number = darker), each one mapped onto the
// Tailwind shade the website already uses and names in that doc, so a
// value here can always be traced back to a specific website shade.
//
// Every pairing this file's ColorScheme/ExtendedColors actually use
// (container + on-container, main + on-main) was verified against the
// WCAG 2 relative-luminance contrast formula -- see
// ../../../../../../../../docs/design-tokens.md in this module's repo
// root for the full ratio table. None of that verification is repeated
// per-value here; only the Tailwind shade each tone maps to is noted,
// so the doc and this file can be cross-checked against each other.

// ---- Brand -- teal (primary + secondary both pull from this one hue;
// secondary just uses a different tone pair, see Theme.kt) ---------------
val Brand10 = Color(0xFF042F2E) // teal-950
val Brand20 = Color(0xFF0C4A45) // between teal-950/900, pre-existing value
val Brand30 = Color(0xFF115E59) // teal-800
val Brand40 = Color(0xFF0F766E) // teal-700 -- text-safe main, per website doc
val Brand50 = Color(0xFF0D9488) // teal-600 -- decorative only (border/icon), per website doc
val Brand60 = Color(0xFF14B8A6) // teal-500
val Brand70 = Color(0xFF2DD4BF) // teal-400
val Brand80 = Color(0xFF5EEAD4) // teal-300
val Brand90 = Color(0xFF99F6E4) // teal-200
val Brand95 = Color(0xFFCCFBF1) // teal-100
val Brand99 = Color(0xFFF0FDFA) // teal-50

// ---- Accent -- warm orange (tertiary role; a near-complement of the
// teal primary, never used for anything alarm-coded -- that's `Error`) --
val Accent10 = Color(0xFF431407) // orange-950
val Accent20 = Color(0xFF7C2D12) // orange-900
val Accent30 = Color(0xFF9A3412) // orange-800
val Accent40 = Color(0xFFC2410C) // orange-700 -- text-safe main, per website doc
val Accent50 = Color(0xFFEA580C) // orange-600 -- decorative only, per website doc

// orange-500 -- swatch/illustrative only; fails even the 3:1 bar on white per website doc
val Accent60 = Color(0xFFF97316)
val Accent70 = Color(0xFFFB923C) // orange-400
val Accent80 = Color(0xFFFDBA74) // orange-300
val Accent90 = Color(0xFFFED7AA) // orange-200
val Accent95 = Color(0xFFFFEDD5) // orange-100
val Accent99 = Color(0xFFFFF7ED) // orange-50

// ---- Semantic: success (emerald) ----------------------------------------
val Success10 = Color(0xFF022C22) // emerald-950
val Success20 = Color(0xFF064E3B) // emerald-900
val Success30 = Color(0xFF065F46) // emerald-800
val Success40 = Color(0xFF047857) // emerald-700 -- text-safe main, per website doc
val Success50 = Color(0xFF059669) // emerald-600
val Success60 = Color(0xFF10B981) // emerald-500
val Success70 = Color(0xFF34D399) // emerald-400
val Success80 = Color(0xFF6EE7B7) // emerald-300
val Success90 = Color(0xFFA7F3D0) // emerald-200
val Success95 = Color(0xFFD1FAE5) // emerald-100
val Success99 = Color(0xFFECFDF5) // emerald-50

// ---- Semantic: warning (amber) ------------------------------------------
val Warning10 = Color(0xFF451A03) // amber-950
val Warning20 = Color(0xFF78350F) // amber-900
val Warning30 = Color(0xFF92400E) // amber-800
val Warning40 = Color(0xFFB45309) // amber-700 -- text-safe main, tightest margin in the palette per website doc
val Warning50 = Color(0xFFD97706) // amber-600
val Warning60 = Color(0xFFF59E0B) // amber-500 -- can carry dark text only, per website doc
val Warning70 = Color(0xFFFBBF24) // amber-400
val Warning80 = Color(0xFFFCD34D) // amber-300
val Warning90 = Color(0xFFFDE68A) // amber-200
val Warning95 = Color(0xFFFEF3C7) // amber-100
val Warning99 = Color(0xFFFFFBEB) // amber-50

// ---- Semantic: info (sky) -----------------------------------------------
val Info10 = Color(0xFF082F49) // sky-950
val Info20 = Color(0xFF0C4A6E) // sky-900
val Info30 = Color(0xFF075985) // sky-800
val Info40 = Color(0xFF0369A1) // sky-700 -- text-safe main, per website doc
val Info50 = Color(0xFF0284C7) // sky-600
val Info60 = Color(0xFF0EA5E9) // sky-500
val Info70 = Color(0xFF38BDF8) // sky-400
val Info80 = Color(0xFF7DD3FC) // sky-300
val Info90 = Color(0xFFBAE6FD) // sky-200
val Info95 = Color(0xFFE0F2FE) // sky-100
val Info99 = Color(0xFFF0F9FF) // sky-50

// ---- Semantic: error (red) ----------------------------------------------
val Error10 = Color(0xFF450A0A) // red-950
val Error20 = Color(0xFF7F1D1D) // red-900
val Error30 = Color(0xFF991B1B) // red-800
val Error40 = Color(0xFFB91C1C) // red-700 -- text-safe main, per website doc
val Error50 = Color(0xFFDC2626) // red-600
val Error60 = Color(0xFFEF4444) // red-500
val Error70 = Color(0xFFF87171) // red-400
val Error80 = Color(0xFFFCA5A5) // red-300
val Error90 = Color(0xFFFECACA) // red-200
val Error95 = Color(0xFFFEE2E2) // red-100
val Error99 = Color(0xFFFEF2F2) // red-50

// ---- Status-only hue: violet, for doctor-status "suspended" -- deliberately
// distinct from `Error` red so a suspended doctor is never visually
// confused with a rejected one at a glance. ------------------------------
val Violet10 = Color(0xFF2E1065)
val Violet20 = Color(0xFF4C1D95)
val Violet30 = Color(0xFF5B21B6)
val Violet40 = Color(0xFF6D28D9)
val Violet50 = Color(0xFF7C3AED)
val Violet60 = Color(0xFF8B5CF6)
val Violet70 = Color(0xFFA78BFA)
val Violet80 = Color(0xFFC4B5FD)
val Violet90 = Color(0xFFDDD6FE)
val Violet95 = Color(0xFFEDE9FE)
val Violet99 = Color(0xFFFAF5FF)

// ---- Neutral -- slate (background/surface/outline roles) ---------------
// Tailwind's `slate` scale, same one the website already uses for every
// neutral/background role -- not a separate "designed" neutral invented
// just for Android.
val Neutral0 = Color(0xFF020617) // slate-950
val Neutral10 = Color(0xFF0F172A) // slate-900
val Neutral20 = Color(0xFF1E293B) // slate-800
val Neutral30 = Color(0xFF334155) // slate-700
val Neutral40 = Color(0xFF475569) // slate-600

// slate-500 -- passes AA by a slim margin per website doc; prefer 30/40 when there's room
val Neutral50 = Color(0xFF64748B)

// slate-400 -- decorative/disabled only, never real text, per website doc
val Neutral60 = Color(0xFF94A3B8)
val Neutral70 = Color(0xFFCBD5E1) // slate-300
val Neutral80 = Color(0xFFE2E8F0) // slate-200
val Neutral90 = Color(0xFFF1F5F9) // slate-100
val Neutral95 = Color(0xFFF8FAFC) // slate-50
val Neutral99 = Color(0xFFFFFFFF)
