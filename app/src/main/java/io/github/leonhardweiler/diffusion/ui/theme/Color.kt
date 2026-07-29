package io.github.leonhardweiler.diffusion.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * The app is black on white and white on black, with grey for everything that
 * has to be told apart from the page it sits on and nothing else.
 *
 * There is no accent colour and nothing is taken from the wallpaper: a note is
 * text, the list is text, and a blue that appeared on a button here and a
 * heading there said nothing about either. Dark is real black rather than dark
 * grey, which is what an oled screen can switch off — that used to be a setting
 * and is now simply what dark means.
 *
 * Red is the one colour left, and only for errors: it is the one thing on the
 * screen that has to be read as different before it is read at all.
 */

private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)

// Light: greys measured off white, dark enough to read on it where they carry
// text, light enough to stay a background where they do not.
private val GreyLight0 = Color(0xFFF7F7F7)
private val GreyLight1 = Color(0xFFF2F2F2)
private val GreyLight2 = Color(0xFFEBEBEB)
private val GreyLight3 = Color(0xFFE6E6E6)
private val GreyLight4 = Color(0xFFE0E0E0)
private val GreyLight5 = Color(0xFFC7C7C7)
private val GreyMid = Color(0xFF737373)
private val GreyDarkText = Color(0xFF3D3D3D)

// Dark: the same steps counted up from black.
private val GreyDark0 = Color(0xFF0D0D0D)
private val GreyDark1 = Color(0xFF141414)
private val GreyDark2 = Color(0xFF1F1F1F)
private val GreyDark3 = Color(0xFF2B2B2B)
private val GreyDark4 = Color(0xFF333333)
private val GreyDark5 = Color(0xFF3D3D3D)
private val GreyMidDark = Color(0xFF8A8A8A)
private val GreyLightText = Color(0xFFC7C7C7)

val md_theme_light_primary = Black
val md_theme_light_onPrimary = White
val md_theme_light_primaryContainer = GreyLight4
val md_theme_light_onPrimaryContainer = Black
val md_theme_light_secondary = GreyDarkText
val md_theme_light_onSecondary = White
val md_theme_light_secondaryContainer = GreyLight3
val md_theme_light_onSecondaryContainer = Black
val md_theme_light_tertiary = Black
val md_theme_light_onTertiary = White
val md_theme_light_tertiaryContainer = GreyLight3
val md_theme_light_onTertiaryContainer = Black
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = White
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = White
val md_theme_light_onBackground = Black
val md_theme_light_surface = White
val md_theme_light_onSurface = Black
val md_theme_light_surfaceVariant = GreyLight3
val md_theme_light_onSurfaceVariant = GreyDarkText
val md_theme_light_outline = GreyMid
val md_theme_light_inverseOnSurface = White
val md_theme_light_inverseSurface = Color(0xFF1A1A1A)
val md_theme_light_inversePrimary = White
val md_theme_light_surfaceTint = Black
val md_theme_light_outlineVariant = GreyLight5
val md_theme_light_scrim = Black
val md_theme_light_surfaceDim = GreyLight4
val md_theme_light_surfaceBright = White
val md_theme_light_surfaceContainerLowest = White
val md_theme_light_surfaceContainerLow = GreyLight0
val md_theme_light_surfaceContainer = GreyLight1
val md_theme_light_surfaceContainerHigh = GreyLight2
val md_theme_light_surfaceContainerHighest = GreyLight3

val md_theme_dark_primary = White
val md_theme_dark_onPrimary = Black
val md_theme_dark_primaryContainer = GreyDark4
val md_theme_dark_onPrimaryContainer = White
val md_theme_dark_secondary = GreyLightText
val md_theme_dark_onSecondary = Black
val md_theme_dark_secondaryContainer = GreyDark3
val md_theme_dark_onSecondaryContainer = White
val md_theme_dark_tertiary = White
val md_theme_dark_onTertiary = Black
val md_theme_dark_tertiaryContainer = GreyDark3
val md_theme_dark_onTertiaryContainer = White
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Black
val md_theme_dark_onBackground = White
val md_theme_dark_surface = Black
val md_theme_dark_onSurface = White
val md_theme_dark_surfaceVariant = GreyDark3
val md_theme_dark_onSurfaceVariant = GreyLightText
val md_theme_dark_outline = GreyMidDark
val md_theme_dark_inverseOnSurface = Black
val md_theme_dark_inverseSurface = Color(0xFFF5F5F5)
val md_theme_dark_inversePrimary = Black
val md_theme_dark_surfaceTint = White
val md_theme_dark_outlineVariant = GreyDark5
val md_theme_dark_scrim = Black
val md_theme_dark_surfaceDim = Black
val md_theme_dark_surfaceBright = GreyDark4
val md_theme_dark_surfaceContainerLowest = Black
val md_theme_dark_surfaceContainerLow = GreyDark0
val md_theme_dark_surfaceContainer = GreyDark1
val md_theme_dark_surfaceContainerHigh = GreyDark2
val md_theme_dark_surfaceContainerHighest = GreyDark3
