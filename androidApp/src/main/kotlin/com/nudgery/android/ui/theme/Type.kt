package com.nudgery.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nudgery.android.R

val AtkinsonHyperlegibleNext = FontFamily(
    Font(R.font.atkinson_hyperlegible_next_extra_light,      FontWeight.ExtraLight),
    Font(R.font.atkinson_hyperlegible_next_extra_light_italic, FontWeight.ExtraLight, FontStyle.Italic),
    Font(R.font.atkinson_hyperlegible_next_light,            FontWeight.Light),
    Font(R.font.atkinson_hyperlegible_next_light_italic,     FontWeight.Light,      FontStyle.Italic),
    Font(R.font.atkinson_hyperlegible_next_regular,          FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_next_regular_italic,   FontWeight.Normal,     FontStyle.Italic),
    Font(R.font.atkinson_hyperlegible_next_medium,           FontWeight.Medium),
    Font(R.font.atkinson_hyperlegible_next_medium_italic,    FontWeight.Medium,     FontStyle.Italic),
    Font(R.font.atkinson_hyperlegible_next_semi_bold,        FontWeight.SemiBold),
    Font(R.font.atkinson_hyperlegible_next_semi_bold_italic, FontWeight.SemiBold,   FontStyle.Italic),
    Font(R.font.atkinson_hyperlegible_next_bold,             FontWeight.Bold),
    Font(R.font.atkinson_hyperlegible_next_bold_italic,      FontWeight.Bold,       FontStyle.Italic),
    Font(R.font.atkinson_hyperlegible_next_extra_bold,       FontWeight.ExtraBold),
    Font(R.font.atkinson_hyperlegible_next_extra_bold_italic, FontWeight.ExtraBold, FontStyle.Italic),
)

fun nudgeryTypography(bold: Boolean): Typography {
    val bodyWeight = if (bold) FontWeight.Medium else FontWeight.Normal
    val labelWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold

    return Typography(
        displayLarge = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = FontWeight.Bold, fontSize = 57.sp),
        displayMedium = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = FontWeight.Bold, fontSize = 45.sp),
        displaySmall = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = FontWeight.Bold, fontSize = 36.sp),

        headlineLarge = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
        headlineMedium = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineSmall = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),

        titleLarge = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
        titleMedium = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
        titleSmall = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),

        bodyLarge = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = bodyWeight, fontSize = 18.sp, lineHeight = 28.sp),
        bodyMedium = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = bodyWeight, fontSize = 16.sp, lineHeight = 24.sp),
        bodySmall = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = bodyWeight, fontSize = 14.sp, lineHeight = 20.sp),

        labelLarge = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = labelWeight, fontSize = 16.sp, lineHeight = 24.sp),
        labelMedium = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = labelWeight, fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall = TextStyle(fontFamily = AtkinsonHyperlegibleNext, fontWeight = labelWeight, fontSize = 12.sp, lineHeight = 16.sp),
    )
}
