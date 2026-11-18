package com.aarogya.sanjeevani.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Approved redesign spec's scale: headline 26/800, title 20/700, body
// 15/400, label 13/700, caption 12. M3's own slot names are kept (no
// call site needs to change) with sizes/weights remapped onto them;
// line-heights/letter-spacing follow M3's own ratio for each role
// rather than being reinvented.
val AarogyaTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                letterSpacing = (-0.25).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                lineHeight = 42.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                lineHeight = 38.sp,
                letterSpacing = 0.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        // "Headline" per the brief -- 26/800.
        headlineSmall =
            TextStyle(
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        // "Title" per the brief -- 20/700.
        titleMedium =
            TextStyle(
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = HeadingFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.2.sp,
            ),
        // "Body" per the brief -- 15/400.
        bodyMedium =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.1.sp,
            ),
        // "Caption" per the brief -- 12.
        bodySmall =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.1.sp,
            ),
        // "Label" per the brief -- 13/700.
        labelLarge =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = BodyFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.3.sp,
            ),
    )
