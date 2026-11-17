package com.aarogya.sanjeevani.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.aarogya.sanjeevani.core.designsystem.R

// Same two-font pairing as the marketing website (website/docs/design-
// tokens.md's Typography section) -- Figtree for headings, Noto Sans for
// body, both humanist sans faces. The website self-hosts them via
// Fontsource; Android has no equivalent "bundle the .ttf" story without
// shipping binary font files in this repo, so these are fetched through
// Jetpack Compose's downloadable-fonts API instead (cached on-device by
// Google Play services after first fetch, so this isn't a per-screen
// network request). res/values/font_certs.xml carries the (non-secret,
// identical-for-every-app) provider certificate this API requires.
private val GoogleFontProvider =
    GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs,
    )

private fun googleFontFamily(fontName: String) =
    FontFamily(
        Font(googleFont = GoogleFont(fontName), fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
        Font(
            googleFont = GoogleFont(fontName),
            fontProvider = GoogleFontProvider,
            weight = FontWeight.Medium,
        ),
        Font(
            googleFont = GoogleFont(fontName),
            fontProvider = GoogleFontProvider,
            weight = FontWeight.SemiBold,
        ),
        Font(googleFont = GoogleFont(fontName), fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
    )

/** Headings -- Figtree, a humanist geometric sans with a touch of warmth. */
val HeadingFontFamily = googleFontFamily("Figtree")

/** Body copy -- Noto Sans, chosen (same reasoning as the website) for
 *  legibility over a more stylized face, appropriate for healthcare copy
 *  that needs to be read correctly, not just skimmed. */
val BodyFontFamily = googleFontFamily("Noto Sans")
