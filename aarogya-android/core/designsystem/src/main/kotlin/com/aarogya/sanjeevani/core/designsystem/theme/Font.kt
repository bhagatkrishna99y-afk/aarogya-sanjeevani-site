package com.aarogya.sanjeevani.core.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.aarogya.sanjeevani.core.designsystem.R

// Approved redesign spec: Plus Jakarta Sans for both headings and body,
// weights 400/500/600/700/800. This repo has no mechanism to author or
// vendor binary .ttf files, so rather than a half-finished "bundle the
// font files" attempt, this keeps the same downloadable-fonts delivery
// the previous pairing already used (cached on-device by Google Play
// services after first fetch, not a per-screen network request) and
// just points it at the new family -- same visual result, no fake
// binaries. res/values/font_certs.xml carries the (non-secret,
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
        Font(
            googleFont = GoogleFont(fontName),
            fontProvider = GoogleFontProvider,
            weight = FontWeight.ExtraBold,
        ),
    )

/** Headings -- Plus Jakarta Sans, per the approved redesign brief. */
val HeadingFontFamily = googleFontFamily("Plus Jakarta Sans")

/** Body copy -- same family as headings per the brief (a single-typeface
 *  system, unlike the previous heading/body pairing). */
val BodyFontFamily = googleFontFamily("Plus Jakarta Sans")
