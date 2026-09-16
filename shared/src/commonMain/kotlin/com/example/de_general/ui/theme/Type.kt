package com.example.de_general.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import degeneral.shared.generated.resources.Res
import degeneral.shared.generated.resources.newsreader_italic_variable
import degeneral.shared.generated.resources.newsreader_variable
import degeneral.shared.generated.resources.plus_jakarta_sans_italic_variable
import degeneral.shared.generated.resources.plus_jakarta_sans_variable
import org.jetbrains.compose.resources.Font

/**
 * Typography for the "Mindful Scribe" design system.
 *
 * Two families do all the work: Newsreader (an optical serif) carries every heading, date anchor
 * and prompt; Plus Jakarta Sans carries body prose and every label. Both are shipped as variable
 * fonts, so a single file per family covers all the weights the design asks for.
 *
 * Note on Android: font variation settings are only honoured from API 26, and this module's
 * `minSdk` is 24. On API 24-25 the variable fonts render at their default instance (wght 400)
 * with synthetic bolding, so headings lose the 500 weight and Newsreader loses its optical sizing.
 * Everything still lays out correctly; it just looks a little flatter on those two API levels.
 */

/** Optical sizes for Newsreader's `opsz` axis, matched to each slot's rendered size. */
private fun newsreaderSettings(weight: FontWeight, style: FontStyle, opticalSize: TextUnit) =
    FontVariation.Settings(weight, style, FontVariation.opticalSizing(opticalSize))

@Composable
private fun newsreaderFamily(opticalSize: TextUnit): FontFamily = FontFamily(
    Font(
        resource = Res.font.newsreader_variable,
        weight = FontWeight.Normal,
        style = FontStyle.Normal,
        variationSettings = newsreaderSettings(FontWeight.Normal, FontStyle.Normal, opticalSize),
    ),
    Font(
        resource = Res.font.newsreader_variable,
        weight = FontWeight.Medium,
        style = FontStyle.Normal,
        variationSettings = newsreaderSettings(FontWeight.Medium, FontStyle.Normal, opticalSize),
    ),
    // Italic Newsreader is not a slot of its own, but the design system calls for it on prompts,
    // prompt questions and mood pull-quotes — reach it with `fontStyle = FontStyle.Italic`.
    Font(
        resource = Res.font.newsreader_italic_variable,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = newsreaderSettings(FontWeight.Normal, FontStyle.Italic, opticalSize),
    ),
    Font(
        resource = Res.font.newsreader_italic_variable,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
        variationSettings = newsreaderSettings(FontWeight.Medium, FontStyle.Italic, opticalSize),
    ),
)

@Composable
private fun plusJakartaSansFamily(): FontFamily = FontFamily(
    Font(Res.font.plus_jakarta_sans_variable, FontWeight.Normal, FontStyle.Normal),
    Font(Res.font.plus_jakarta_sans_variable, FontWeight.Medium, FontStyle.Normal),
    Font(Res.font.plus_jakarta_sans_variable, FontWeight.SemiBold, FontStyle.Normal),
    Font(Res.font.plus_jakarta_sans_italic_variable, FontWeight.Normal, FontStyle.Italic),
    Font(Res.font.plus_jakarta_sans_italic_variable, FontWeight.Medium, FontStyle.Italic),
    Font(Res.font.plus_jakarta_sans_italic_variable, FontWeight.SemiBold, FontStyle.Italic),
)

/**
 * The eleven text styles the design system actually names, kept under their own names so they stay
 * greppable against the Stitch design system. Material's [Typography] is derived from these in
 * [mindfulScribeTypography].
 */
@Immutable
data class MindfulTypeTokens(
    val displayLg: TextStyle,
    /** The phone swap for [displayLg]. Material has no slot for it; use it directly. */
    val displayLgMobile: TextStyle,
    val headlineLg: TextStyle,
    val headlineMd: TextStyle,
    val headlineSm: TextStyle,
    val bodyLg: TextStyle,
    val bodyMd: TextStyle,
    val bodySm: TextStyle,
    val labelLg: TextStyle,
    val labelMd: TextStyle,
    val labelSm: TextStyle,
)

@Composable
fun mindfulScribeTypeTokens(): MindfulTypeTokens {
    val sans = plusJakartaSansFamily()
    return MindfulTypeTokens(
        displayLg = TextStyle(
            fontFamily = newsreaderFamily(40.sp),
            fontSize = 40.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.02).em,
        ),
        displayLgMobile = TextStyle(
            fontFamily = newsreaderFamily(32.sp),
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.01).em,
        ),
        headlineLg = TextStyle(
            fontFamily = newsreaderFamily(28.sp),
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.01).em,
        ),
        headlineMd = TextStyle(
            fontFamily = newsreaderFamily(24.sp),
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Medium,
        ),
        headlineSm = TextStyle(
            fontFamily = newsreaderFamily(20.sp),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Medium,
        ),
        bodyLg = TextStyle(
            fontFamily = sans,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-0.01).em,
        ),
        bodyMd = TextStyle(
            fontFamily = sans,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodySm = TextStyle(
            fontFamily = sans,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
        ),
        labelLg = TextStyle(
            fontFamily = sans,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.01.em,
        ),
        labelMd = TextStyle(
            fontFamily = sans,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.02.em,
        ),
        labelSm = TextStyle(
            fontFamily = sans,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.03.em,
        ),
    )
}

/**
 * Material's [Typography] built from [MindfulTypeTokens].
 *
 * Material has fifteen slots against the design system's eleven, so four are deliberately doubled
 * up rather than invented:
 *  - `displaySmall` and `headlineLarge` both take `headline-lg`
 *  - `headlineSmall` and `titleLarge` both take `headline-sm`
 *  - `titleMedium` takes `label-lg`, `titleSmall` takes `label-md`
 *
 * The design system defines no title scale of its own; its labels are what its screens use in
 * those positions.
 */
@Composable
fun mindfulScribeTypography(tokens: MindfulTypeTokens = mindfulScribeTypeTokens()): Typography =
    Typography(
        displayLarge = tokens.displayLg,
        displayMedium = tokens.displayLgMobile,
        displaySmall = tokens.headlineLg,
        headlineLarge = tokens.headlineLg,
        headlineMedium = tokens.headlineMd,
        headlineSmall = tokens.headlineSm,
        titleLarge = tokens.headlineSm,
        titleMedium = tokens.labelLg,
        titleSmall = tokens.labelMd,
        bodyLarge = tokens.bodyLg,
        bodyMedium = tokens.bodyMd,
        bodySmall = tokens.bodySm,
        labelLarge = tokens.labelLg,
        labelMedium = tokens.labelMd,
        labelSmall = tokens.labelSm,
    )
