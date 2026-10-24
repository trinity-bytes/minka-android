package com.trinitybytes.minka.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.trinitybytes.minka.R

// Figma Foundations > Tipografía. Both fonts are bundled variable fonts (Google Fonts, OFL).

/**
 * Fraunces as Figma renders it: SOFT 0, WONK 1 and an optical size that matches the font size
 * (Figma's automatic optical sizing). FontFamily resolves by weight only, so each size gets its own.
 */
private fun fraunces(weight: FontWeight, opticalSize: Int) = FontFamily(
    Font(
        resId = R.font.fraunces,
        weight = weight,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.weight),
            FontVariation.opticalSizing(opticalSize.sp),
            FontVariation.Setting("SOFT", 0f),
            FontVariation.Setting("WONK", 1f),
        ),
    ),
)

private fun inter(weight: FontWeight) = Font(
    resId = R.font.inter,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.ExtraBold),
)

private fun frauncesStyle(weight: FontWeight, size: Int, lineHeight: Int, tracking: Float) = TextStyle(
    fontFamily = fraunces(weight, size),
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

private fun interStyle(weight: FontWeight, size: Int, lineHeight: Int, tracking: Float) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
)

internal val MinkaTypography = Typography(
    displayLarge = frauncesStyle(FontWeight.Black, 57, 64, -1.4f),
    displayMedium = frauncesStyle(FontWeight.Black, 45, 52, -1.1f),
    displaySmall = frauncesStyle(FontWeight.Black, 36, 44, -0.9f),
    headlineLarge = frauncesStyle(FontWeight.Bold, 32, 40, -0.3f),
    headlineMedium = frauncesStyle(FontWeight.Bold, 28, 36, -0.2f),
    headlineSmall = frauncesStyle(FontWeight.Bold, 24, 32, 0f),
    titleLarge = frauncesStyle(FontWeight.Bold, 22, 28, 0f),
    titleMedium = interStyle(FontWeight.SemiBold, 16, 24, 0.15f),
    titleSmall = interStyle(FontWeight.SemiBold, 14, 20, 0.1f),
    bodyLarge = interStyle(FontWeight.Normal, 16, 24, 0.5f),
    bodyMedium = interStyle(FontWeight.Normal, 14, 20, 0.25f),
    bodySmall = interStyle(FontWeight.Normal, 12, 16, 0.4f),
    labelLarge = interStyle(FontWeight.Medium, 14, 20, 0.1f),
    labelMedium = interStyle(FontWeight.Medium, 12, 16, 0.5f),
    labelSmall = interStyle(FontWeight.Medium, 11, 16, 0.5f),
)

/** Minka text styles with no Material 3 slot. */
@Immutable
data class MinkaTextStyles(
    /** `Label/Sticker`: uppercase tags on stickers. */
    val sticker: TextStyle,
)

internal val MinkaTextStyleTokens = MinkaTextStyles(
    sticker = interStyle(FontWeight.ExtraBold, 13, 18, 6f),
)
