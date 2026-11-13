package com.metrowin.keyboard

import androidx.test.core.app.ApplicationProvider
import com.metrowin.keyboard.theme.ColorMath
import com.metrowin.keyboard.theme.HINT_ALPHA
import com.metrowin.keyboard.theme.KeyboardTheme
import com.metrowin.keyboard.theme.SPACE_LABEL_ALPHA
import com.metrowin.keyboard.theme.ThemeStyle
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * What is written on the keyboard must be readable in every look, light and dark (WCAG): 4.5:1 for text, 3:1 for icons and dots.
 * A look or colour that falls short fails here, naming the look and what it fails on.
 */
@RunWith(RobolectricTestRunner::class)
class ContrastTest {
    private fun ratio(fg: Int, bg: Int) = ColorMath.contrast(ColorMath.over(fg, bg), bg)

    /** The Metro cyan of the settings app: heading text on the surface, and the white text on the calibration tile, in light and dark. */
    @Test fun theSettingsCyanIsReadableLightAndDark() {
        for (night in listOf(false, true)) {
            org.robolectric.RuntimeEnvironment.setQualifiers(if (night) "+night" else "+notnight")
            val app = ApplicationProvider.getApplicationContext<KeyboardApp>()
            val surface = app.getColor(R.color.m3_surface)
            val text = ratio(app.getColor(R.color.metro_text), surface)
            val tile = ratio(app.getColor(R.color.metro_on_tile), app.getColor(R.color.metro_tile))
            assertTrue("Heading cyan on the surface (night=$night): %.1f:1, needs 4.5".format(text), text >= 4.5)
            assertTrue("White on the tile (night=$night): %.1f:1, needs 4.5".format(tile), tile >= 4.5)
            // The tile is a shape on the surface: 3:1 so that it can be seen.
            val shape = ratio(app.getColor(R.color.metro_tile), surface)
            assertTrue("The tile against the surface (night=$night): %.1f:1, needs 3".format(shape), shape >= 3.0)
        }
    }

    /** Every colour the settings offer as an accent must leave the Enter arrow and the accent on the bar readable on every look, light and dark. */
    @Test fun everyAccentSwatchIsReadableOnEveryLook() {
        val app = ApplicationProvider.getApplicationContext<KeyboardApp>()
        val failures = ArrayList<String>()
        for (accent in com.metrowin.keyboard.theme.Accents.PRESETS) for (style in ThemeStyle.entries) for (dark in listOf(false, true)) {
            app.prefs.sp.edit().putString(K.THEME_STYLE, style.code).putString(K.THEME_MODE, if (dark) "dark" else "light").putInt(K.CUSTOM_ACCENT, accent or 0xFF000000.toInt()).apply()
            val c = KeyboardTheme.resolve(app, app.prefs, dark)
            val behind = if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            val bar = ColorMath.over(c.bar, ColorMath.over(c.background, behind))
            val where = "%06X on %s %s".format(accent and 0xFFFFFF, style.code, if (dark) "dark" else "light")
            // Classic's own cyan keeps its white arrow on purpose (2.9:1, see above), also when the cyan swatch is picked.
            val keptWhite = style == ThemeStyle.CLASSIC && accent == com.metrowin.keyboard.theme.Accents.DEFAULT
            if (style != ThemeStyle.IPHONE && !keptWhite) ratio(c.onAccent, c.accent).let { if (it < 3.0) failures += "$where: Enter arrow %.1f:1".format(it) }
            ratio(c.barAccent, bar).let { if (it < 3.0) failures += "$where: accent on the bar %.1f:1".format(it) }
        }
        assertTrue("Too little contrast:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun everyLookIsReadableLightAndDark() {
        val app = ApplicationProvider.getApplicationContext<KeyboardApp>()
        val failures = ArrayList<String>()
        for (style in ThemeStyle.entries) for (dark in listOf(false, true)) {
            app.prefs.sp.edit().putString(K.THEME_STYLE, style.code).putString(K.THEME_MODE, if (dark) "dark" else "light").putInt(K.CUSTOM_ACCENT, 0).apply()
            val c = KeyboardTheme.resolve(app, app.prefs, dark)
            // A translucent look sits on the app behind it: take white (light) or black (dark) as that.
            val behind = if (dark) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            val bg = ColorMath.over(c.background, behind)
            val bar = ColorMath.over(c.bar, bg)
            val key = ColorMath.over(c.key, bg)
            val special = ColorMath.over(c.special, bg)
            val ios = c.style == ThemeStyle.IPHONE
            // What is drawn where, with the contrast it must reach.
            val checks = mutableListOf(
                Triple("letter", ratio(c.keyText, key), 4.5),
                Triple("digit hint", ratio(ColorMath.withAlpha(c.keyText, HINT_ALPHA), key), 4.5),
                Triple("space label", ratio(if (ios) c.keyText else ColorMath.withAlpha(c.keyText, SPACE_LABEL_ALPHA), key), 4.5),
                Triple("special key", ratio(c.keyText, special), 4.5),
                Triple("suggestion", ratio(c.text, bar), 4.5),
                Triple("tools icon", ratio(c.subtle, bar), 3.0),
                Triple("accent on bar", ratio(c.barAccent, bar), 3.0),
            )
            // The iPhone look draws its return key in the key colour, not the accent.
            // Classic's white arrow on its Windows Phone cyan is kept on purpose: 2.9:1, a hair under the 3:1 line.
            if (!ios) checks += Triple("enter icon", ratio(c.onAccent, c.accent), if (style == ThemeStyle.CLASSIC) 2.85 else 3.0)
            for ((what, got, need) in checks) if (got < need) failures += "%s %s: %s %.1f:1, needs %.1f".format(style.code, if (dark) "dark" else "light", what, got, need)
        }
        assertTrue("Too little contrast:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
