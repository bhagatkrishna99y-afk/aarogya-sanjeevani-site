@file:Suppress("MagicNumber") // every hex literal below *is* the named constant detekt wants it defined as

package com.aarogya.sanjeevani.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Approved patient-app visual redesign palette. Named roles below are
// exactly the hex values from the design brief (ocean teal primary,
// violet secondary, coral tertiary/alert); tonal ramps around each are
// this module's own derivation for dark-theme/container variants where
// the brief didn't specify one, following the same Tailwind-shade-chain
// convention the rest of this file already used (lower number = darker).

// ---- Brand -- ocean teal (primary) --------------------------------------
val Teal10 = Color(0xFF042F2E)
val Teal20 = Color(0xFF0C3B42)
val Teal30 = Color(0xFF0A5365)
val Teal40 = Color(0xFF0E7490) // text-safe main -- exact brief value
val Teal50 = Color(0xFF0E88A8)
val Teal60 = Color(0xFF0891B2)
val Teal70 = Color(0xFF22D3EE)
val Teal80 = Color(0xFF67E8F9)
val Teal90 = Color(0xFFA5F3FC)
val Teal95 = Color(0xFFDDF3F6) // sky tint -- exact brief value (primaryContainer)
val Teal99 = Color(0xFFF2F8FA) // canvas background -- exact brief value

// ---- Brand -- violet (secondary) ----------------------------------------
val Violet10 = Color(0xFF1E1240)
val Violet20 = Color(0xFF2E1065)
val Violet30 = Color(0xFF3F2A8C)
val Violet40 = Color(0xFF5B3FD1) // text-safe main -- exact brief value
val Violet50 = Color(0xFF6D28D9)
val Violet60 = Color(0xFF7C3AED)
val Violet70 = Color(0xFF9B7CF2)
val Violet80 = Color(0xFFC4B5FD)
val Violet90 = Color(0xFFDDD6FE)
val Violet95 = Color(0xFFECE6FF) // category-tile container -- exact brief value
val Violet99 = Color(0xFFFAF5FF)

// ---- Brand -- coral (tertiary/alert) -------------------------------------
val Coral10 = Color(0xFF3D0A14)
val Coral20 = Color(0xFF5C0F1E)
val Coral30 = Color(0xFF8A1E32)
val Coral40 = Color(0xFFC8324A) // text-safe main -- exact brief value
val Coral50 = Color(0xFFD94C63)
val Coral60 = Color(0xFFE56B80)
val Coral70 = Color(0xFFF08FA0)
val Coral80 = Color(0xFFF7B4C0)
val Coral90 = Color(0xFFFCDCE2)
val Coral95 = Color(0xFFFFE3E8) // category-tile container -- exact brief value
val Coral99 = Color(0xFFFFF4F6)

// ---- Semantic: success (green, request "Confirmed"/"Completed"-adjacent) --
val Success10 = Color(0xFF052E16)
val Success20 = Color(0xFF14532D) // Confirmed chip text -- exact brief value
val Success30 = Color(0xFF166534)
val Success40 = Color(0xFF15803D) // category-tile fg -- exact brief value
val Success50 = Color(0xFF16A34A)
val Success60 = Color(0xFF22C55E)
val Success70 = Color(0xFF4ADE80)
val Success80 = Color(0xFF86EFAC)
val Success90 = Color(0xFFBBF7D0)
val Success95 = Color(0xFFDCFCE7) // Confirmed chip bg -- exact brief value
val Success99 = Color(0xFFF0FDF4)

// ---- Semantic: warning/amber (request "Requested", category tile) --------
val Amber10 = Color(0xFF2D1600)
val Amber20 = Color(0xFF4A2600)
val Amber30 = Color(0xFF6B3700)
val Amber40 = Color(0xFF8A4B08) // Requested chip text -- exact brief value
val Amber50 = Color(0xFFB45309) // category-tile fg -- exact brief value
val Amber60 = Color(0xFFD97706)
val Amber70 = Color(0xFFF59E0B)
val Amber80 = Color(0xFFFCD34D)
val Amber90 = Color(0xFFFDE68A)
val Amber95 = Color(0xFFFEF3C7) // Requested chip bg -- exact brief value
val Amber99 = Color(0xFFFFFBEB)

// ---- Semantic: info/blue (request "Accepted") ----------------------------
val Blue10 = Color(0xFF0B1F4A)
val Blue20 = Color(0xFF142B6B)
val Blue30 = Color(0xFF1E3A8A)
val Blue40 = Color(0xFF1E40AF) // Accepted chip text -- exact brief value
val Blue50 = Color(0xFF2563EB)
val Blue60 = Color(0xFF3B82F6)
val Blue70 = Color(0xFF60A5FA)
val Blue80 = Color(0xFF93C5FD)
val Blue90 = Color(0xFFBFDBFE)
val Blue95 = Color(0xFFDBEAFE) // Accepted chip bg -- exact brief value
val Blue99 = Color(0xFFEFF6FF)

// ---- Semantic: error/red (errors + request "Rejected") -------------------
val Red10 = Color(0xFF450A0A)
val Red20 = Color(0xFF5C1010)
val Red30 = Color(0xFF7F1D1D)
val Red40 = Color(0xFF991B1B) // Rejected chip text -- exact brief value
val Red50 = Color(0xFFB42318) // error main -- exact brief value
val Red60 = Color(0xFFDC2626)
val Red70 = Color(0xFFEF4444)
val Red80 = Color(0xFFFCA5A5)
val Red90 = Color(0xFFFECACA)
val Red95 = Color(0xFFFEE2E2) // Rejected chip bg -- exact brief value
val Red99 = Color(0xFFFEF2F2)

// ---- Neutral -- ink/slate (background/surface/outline/"Cancelled"/"Completed") --
val Neutral10 = Color(0xFF0B1720) // dark background -- exact brief value
val Neutral15 = Color(0xFF12222D) // dark surface -- exact brief value
val Neutral20 = Color(0xFF1B2E3A)
val Neutral30 = Color(0xFF263B48)
val Neutral40 = Color(0xFF374151) // Completed chip text -- exact brief value
val Neutral50 = Color(0xFF475B6B) // onSurfaceVariant/muted -- exact brief value
val Neutral60 = Color(0xFF4B5563) // Cancelled chip text -- exact brief value
val Neutral70 = Color(0xFFC4D4DF) // outline -- exact brief value
val Neutral80 = Color(0xFFDCE7EE) // outlineVariant -- exact brief value
val Neutral85 = Color(0xFFE5E7EB) // Completed chip bg -- exact brief value
val Neutral90 = Color(0xFFEEF0F2) // Cancelled chip bg -- exact brief value
val Neutral95 = Color(0xFFF2F8FA) // background -- exact brief value (same as Teal99)
val Neutral99 = Color(0xFFFFFFFF) // surface -- exact brief value
val InkDark = Color(0xFF0F2230) // onSurface/ink -- exact brief value
