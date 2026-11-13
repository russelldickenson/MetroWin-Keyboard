package com.metrowin.keyboard.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.metrowin.keyboard.Prefs

enum class ThemeStyle(val code: String, private val of: ThemeStyle? = null) {
    /** Flat Windows Phone: square tiles, the accent on what is pressed. */
    CLASSIC("classic"),

    /** Frosted, rounded glass with light rims. */
    GLASS("glass"),

    /** Material You: tonal surfaces (the wallpaper's colours on Android 12+), rounded keys, a filled action key. */
    DROID("droid"),

    /** Modern Droid with the keys spaced as on the iPhone look. */
    DROID_SPACED("droid_spaced", DROID),

    /** The iOS keyboard: pale grey, white rounded keys with a thin shadow under them, darker grey for the special keys. */
    IPHONE("iphone"),

    /** Dark grey keys with light grey symbols, on a light grey surround (a dark one in dark mode). */
    SLATE("slate"),

    /** Modern Slate with the keys spaced as on the iPhone look. */
    SLATE_SPACED("slate_spaced", SLATE);

    /** The look this one draws like: the spaced versions draw as the look they are made from. */
    val base: ThemeStyle get() = of ?: this
    val spaced: Boolean get() = of != null

    companion object { fun from(code: String?) = entries.firstOrNull { it.code == code } ?: CLASSIC }
}

/** Everything the views need to paint the keyboard, resolved from the settings. Plain data, so it is cheap to compare. */
data class KeyboardColors(
    val style: ThemeStyle,
    val dark: Boolean,
    val background: Int,
    val bar: Int,
    val key: Int,
    val keyPressed: Int,
    val special: Int,
    val text: Int,
    val subtle: Int,
    val accent: Int,
    val onAccent: Int,
    val stroke: Int,
    val popup: Int,
    val cornerDp: Float,
    val strokeDp: Float,
    val translucent: Boolean,
    /** A line under each key, as on iOS; 0 for none. */
    val shadowDp: Float = 0f,
    /** The colour of what is written on the keys, when it is not [text] (the keys are dark on a light surround); 0 for [text]. */
    val keyTextColor: Int = 0,
    /** The accent as seen on the bar above the keys, when the surround is so light that [accent] would not read there; 0 for [accent]. */
    val barAccentColor: Int = 0,
    /** The keys are further apart than the setting says (about 6 dp across and 10 dp between rows), as on iOS. */
    val spacedKeys: Boolean = false,
) {
    val barAccent get() = if (barAccentColor != 0) barAccentColor else accent
    val keyText get() = if (keyTextColor != 0) keyTextColor else text
}

/** How opaque the small writing on the keys is: the digit in the corner and the language on the space bar (both need 4.5:1 on their key). */
const val HINT_ALPHA = 0xE0
const val SPACE_LABEL_ALPHA = 0xD0

object KeyboardTheme {
    /**
     * @param systemDark whether Android is in dark mode
     */
    fun resolve(context: Context, p: Prefs, systemDark: Boolean, look: ThemeStyle? = null): KeyboardColors {
        val chosen = look ?: ThemeStyle.from(p.themeStyle)
        val style = chosen.base
        val dark = when (p.themeMode) { "dark" -> true; "light" -> false; else -> systemDark }
        val system = if (style == ThemeStyle.DROID && p.dynamicColor && p.customAccent == 0) SystemTones.read(context) else null

        val accent = when {
            p.customAccent != 0 -> p.customAccent or 0xFF000000.toInt()
            system != null -> if (dark) system.accent200 else system.accent600
            style == ThemeStyle.IPHONE -> 0xFF007AFF.toInt()
            style == ThemeStyle.SLATE -> 0xFF8AB4F8.toInt()
            else -> Accents.DEFAULT
        }

        var bg = when {
            style == ThemeStyle.DROID && system != null -> if (dark) system.neutral900 else system.neutral50
            style == ThemeStyle.DROID -> if (dark) ColorMath.mix(0xFF101114.toInt(), accent, 0.10f) else ColorMath.mix(0xFFFBFBFD.toInt(), accent, 0.06f)
            style == ThemeStyle.SLATE -> if (dark) 0xFF263238.toInt() else 0xFFD3D6DB.toInt()
            style == ThemeStyle.IPHONE -> if (dark) 0xFF1C1C1E.toInt() else 0xFFD2D4DA.toInt()
            style == ThemeStyle.GLASS -> if (dark) ColorMath.mix(0xFF0A1020.toInt(), accent, 0.22f) else ColorMath.mix(0xFFEAF1FA.toInt(), accent, 0.12f)
            else -> if (dark) (if (p.oledBlack) 0xFF000000.toInt() else 0xFF1B1B1B.toInt()) else 0xFFD6D6D6.toInt()
        }
        if (p.oledBlack && dark && style != ThemeStyle.CLASSIC) bg = ColorMath.darken(bg, 0.85f)

        val text = ColorMath.onColor(bg)
        val subtle = ColorMath.withAlpha(text, 0xB0)
        // Classic keeps the white arrow of Windows Phone on its cyan (2.9:1, a hair under 3); any other accent gets whichever of black and white reads better.
        val onAccent = if (style == ThemeStyle.CLASSIC && accent == Accents.DEFAULT) ColorMath.onColor(accent) else ColorMath.bestOn(accent)
        val colors = when (style) {
            ThemeStyle.CLASSIC -> KeyboardColors(
                style, dark, bg, ColorMath.mix(bg, text, 0.04f),
                key = ColorMath.mix(bg, text, if (dark) 0.14f else 0.0f).let { if (dark) it else 0xFFF2F2F2.toInt() },
                keyPressed = accent, special = ColorMath.mix(bg, text, if (dark) 0.07f else 0.10f),
                text = text, subtle = subtle, accent = accent, onAccent = onAccent, stroke = 0,
                popup = ColorMath.mix(bg, text, 0.22f), cornerDp = 1f, strokeDp = 0f, translucent = false,
            )
            ThemeStyle.GLASS -> KeyboardColors(
                style, dark, ColorMath.withAlpha(bg, 0xE6), ColorMath.withAlpha(bg, 0x30),
                key = ColorMath.withAlpha(text, if (dark) 0x26 else 0x30),
                keyPressed = ColorMath.withAlpha(accent, 0xC0), special = ColorMath.withAlpha(text, 0x14),
                text = text, subtle = subtle, accent = accent, onAccent = onAccent, stroke = ColorMath.withAlpha(text, 0x48),
                popup = ColorMath.withAlpha(ColorMath.mix(bg, text, 0.12f), 0xF2), cornerDp = 14f, strokeDp = 1f, translucent = true,
            )
            ThemeStyle.IPHONE -> KeyboardColors(
                style, dark, bg, bg,
                key = if (dark) 0xFF6B6B6E.toInt() else 0xFFFFFFFF.toInt(),
                keyPressed = if (dark) 0xFF4A4A4D.toInt() else 0xFFAEB3BE.toInt(),
                special = if (dark) 0xFF444447.toInt() else 0xFFAEB3BE.toInt(),
                text = text, subtle = subtle, accent = accent, onAccent = onAccent, stroke = 0,
                popup = if (dark) 0xFF6B6B6E.toInt() else 0xFFFFFFFF.toInt(), cornerDp = 5f, strokeDp = 0f, translucent = false,
                shadowDp = 1f,
            )
            ThemeStyle.SLATE, ThemeStyle.SLATE_SPACED -> KeyboardColors(
                style, dark, bg, bg,
                key = if (dark) 0xFF444B50.toInt() else 0xFF41474C.toInt(),
                keyPressed = 0xFF68717A.toInt(),
                special = if (dark) 0xFF323A40.toInt() else 0xFF2E3338.toInt(),
                text = text, subtle = subtle, accent = accent, onAccent = onAccent, stroke = 0,
                popup = if (dark) 0xFF444B50.toInt() else 0xFF41474C.toInt(), cornerDp = 9f, strokeDp = 0f, translucent = false,
                shadowDp = 1.5f, keyTextColor = 0xFFE3E5E8.toInt(),
                barAccentColor = if (!dark && p.customAccent == 0) 0xFF1A66D6.toInt() else 0,
            )
            ThemeStyle.DROID, ThemeStyle.DROID_SPACED -> {
                val keyColor = system?.let { if (dark) it.neutral800 else it.neutral0 } ?: ColorMath.mix(bg, text, if (dark) 0.10f else 0.0f).let { if (dark) it else ColorMath.lighten(bg, 0.6f) }
                val specialColor = system?.let { if (dark) it.accent2_700 else it.accent2_100 } ?: ColorMath.mix(bg, accent, if (dark) 0.28f else 0.22f)
                KeyboardColors(
                    style, dark, bg, bg, keyColor, ColorMath.mix(keyColor, accent, 0.35f), specialColor,
                    text, subtle, accent, onAccent, 0, ColorMath.mix(bg, text, 0.16f), cornerDp = 16f, strokeDp = 0f, translucent = false,
                )
            }
        }
        val spaced = if (chosen.spaced || style == ThemeStyle.IPHONE) colors.copy(spacedKeys = true) else colors
        // The accent on the bar (the dot for something new, the open toolbox, the microphone) is a graphic: 3:1 on the bar, so it is darkened or lightened until it is.
        val barUnder = ColorMath.over(spaced.bar, ColorMath.over(spaced.background, if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()))
        val barAccent = if (spaced.barAccentColor != 0) spaced.barAccentColor else ColorMath.ensureContrast(accent, barUnder, 3.0)
        return if (barAccent != accent) spaced.copy(barAccentColor = barAccent) else spaced
    }

    /** The accent the chosen look has on its own, with no colour of the user's: what "Follow the look" gives. */
    fun lookAccent(context: Context, p: Prefs, systemDark: Boolean): Int {
        val style = ThemeStyle.from(p.themeStyle).base
        val dark = when (p.themeMode) { "dark" -> true; "light" -> false; else -> systemDark }
        val system = if (style == ThemeStyle.DROID && p.dynamicColor) SystemTones.read(context) else null
        return when {
            system != null -> if (dark) system.accent200 else system.accent600
            style == ThemeStyle.IPHONE -> 0xFF007AFF.toInt()
            style == ThemeStyle.SLATE -> 0xFF8AB4F8.toInt()
            else -> Accents.DEFAULT
        }
    }

    fun isSystemDark(context: Context) =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

/** The tonal colours Android 12+ makes from the wallpaper, or null on an older phone. */
class SystemTones(
    val accent200: Int, val accent600: Int, val accent2_100: Int, val accent2_700: Int,
    val neutral0: Int, val neutral50: Int, val neutral800: Int, val neutral900: Int,
) {
    companion object {
        fun read(c: Context): SystemTones? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
            return runCatching {
                SystemTones(
                    c.getColor(android.R.color.system_accent1_200), c.getColor(android.R.color.system_accent1_600),
                    c.getColor(android.R.color.system_accent2_100), c.getColor(android.R.color.system_accent2_700),
                    c.getColor(android.R.color.system_neutral1_0), c.getColor(android.R.color.system_neutral1_50),
                    c.getColor(android.R.color.system_neutral1_800), c.getColor(android.R.color.system_neutral1_900),
                )
            }.getOrNull()
        }
    }
}
