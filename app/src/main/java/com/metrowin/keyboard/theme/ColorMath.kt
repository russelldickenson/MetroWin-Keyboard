package com.metrowin.keyboard.theme

import kotlin.math.max
import kotlin.math.min

/** Colour helpers on plain ARGB ints, with no Android classes so they run in unit tests. */
object ColorMath {
    fun alpha(c: Int) = c ushr 24 and 0xFF
    fun red(c: Int) = c shr 16 and 0xFF
    fun green(c: Int) = c shr 8 and 0xFF
    fun blue(c: Int) = c and 0xFF
    fun argb(a: Int, r: Int, g: Int, b: Int) = (a and 0xFF shl 24) or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)
    fun withAlpha(c: Int, a: Int) = (c and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    /** [t] of the way from [a] to [b] (0 = a, 1 = b), keeping the alpha of [a] blended too. */
    fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun ch(x: Int, y: Int) = (x + (y - x) * k + 0.5f).toInt()
        return argb(ch(alpha(a), alpha(b)), ch(red(a), red(b)), ch(green(a), green(b)), ch(blue(a), blue(b)))
    }

    /** Relative luminance (WCAG), 0 black to 1 white. */
    fun luminance(c: Int): Double {
        fun lin(v: Int): Double { val s = v / 255.0; return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4) }
        return 0.2126 * lin(red(c)) + 0.7152 * lin(green(c)) + 0.0722 * lin(blue(c))
    }

    fun isDark(c: Int) = luminance(c) < 0.4

    /** Black or white, whichever reads better on [background]. */
    fun onColor(background: Int) = if (luminance(background) > 0.45) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

    /** WCAG contrast ratio of two opaque colours, 1 to 21. */
    fun contrast(a: Int, b: Int): Double { val x = luminance(a); val y = luminance(b); return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05) }

    /** [fg] laid over [bg] (its alpha blended), as an opaque colour. */
    fun over(fg: Int, bg: Int): Int {
        val a = alpha(fg) / 255f
        fun ch(f: Int, b: Int) = (f * a + b * (1 - a) + 0.5f).toInt()
        return argb(255, ch(red(fg), red(bg)), ch(green(fg), green(bg)), ch(blue(fg), blue(bg)))
    }

    /** Black or white, whichever has the higher contrast on [background], to the letter: [onColor] leans to white on mid tones. */
    fun bestOn(background: Int) = if (contrast(0xFF000000.toInt(), background) >= contrast(0xFFFFFFFF.toInt(), background)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

    /** [fg] moved towards black or white, whichever the [bg] allows, until it has at least [min] contrast on it; unchanged if it already has. */
    fun ensureContrast(fg: Int, bg: Int, min: Double): Int {
        if (contrast(over(fg, bg), bg) >= min) return fg
        val toward = if (luminance(bg) > 0.4) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        var t = 0.05f
        while (t < 1f) { val c = mix(fg, toward, t); if (contrast(over(c, bg), bg) >= min) return c; t += 0.05f }
        return toward
    }

    fun lighten(c: Int, t: Float) = mix(c, 0xFFFFFFFF.toInt(), t)
    fun darken(c: Int, t: Float) = mix(c, 0xFF000000.toInt(), t)

    /** Hue (0-360), saturation and value (0-1). */
    fun hsv(c: Int): FloatArray {
        val r = red(c) / 255f; val g = green(c) / 255f; val b = blue(c) / 255f
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val d = mx - mn
        val h = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }
        return floatArrayOf((h + 360f) % 360f, if (mx == 0f) 0f else d / mx, mx)
    }

    fun hsvToArgb(h: Float, s: Float, v: Float): Int {
        val c = v * s; val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1)); val m = v - c
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0f); h < 120 -> Triple(x, c, 0f); h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c); h < 300 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
        }
        return argb(255, ((r + m) * 255 + 0.5f).toInt(), ((g + m) * 255 + 0.5f).toInt(), ((b + m) * 255 + 0.5f).toInt())
    }
}
