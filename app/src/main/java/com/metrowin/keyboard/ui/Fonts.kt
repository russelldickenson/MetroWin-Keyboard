package com.metrowin.keyboard.ui

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.metrowin.keyboard.R

/** Open Sans (SIL OFL), the stand-in for Segoe WP, which is proprietary: light for the big things, regular and semibold for the small. */
object Fonts {
    @Volatile private var loaded: Triple<Typeface, Typeface, Typeface>? = null

    private fun load(c: Context): Triple<Typeface, Typeface, Typeface> = loaded ?: synchronized(this) {
        loaded ?: Triple(
            ResourcesCompat.getFont(c, R.font.opensans_light) ?: Typeface.DEFAULT,
            ResourcesCompat.getFont(c, R.font.opensans_regular) ?: Typeface.DEFAULT,
            ResourcesCompat.getFont(c, R.font.opensans_semibold) ?: Typeface.DEFAULT_BOLD,
        ).also { loaded = it }
    }

    fun light(c: Context) = load(c).first
    fun regular(c: Context) = load(c).second
    fun semibold(c: Context) = load(c).third
}

/** The font of the key labels, chosen in Appearance: a family and a weight from 200 to 500. */
object KeyFonts {
    val families = listOf("system", "opensans", "roboto", "atkinson")
    val weights = listOf(200, 300, 400, 500)

    /** The weights that look different in this family: the variable fonts stop at their lightest,  */
    fun weightsFor(family: String): List<Int> = when (family) {
        "opensans" -> listOf(300, 400, 500)
        else -> weights
    }

    /** The weight of [family] nearest to [weight]. */
    fun snap(family: String, weight: Int): Int = weightsFor(family).minByOrNull { Math.abs(it - weight) } ?: 300
    private val files = mapOf("opensans" to "OpenSans-VF.ttf", "roboto" to "Roboto-VF.ttf", "atkinson" to "AtkinsonHyperlegibleNext-VF.ttf")
    private val cache = HashMap<String, Typeface>()

    /**
     * The variable fonts clamp to their own range: Open Sans starts at 300, so its list leaves out 200. The system font follows the phone's own weights.
     */
    @Synchronized fun get(c: Context, family: String, weight: Int): Typeface {
        val w = weight.coerceIn(200, 500)
        return cache.getOrPut("$family/$w") {
            val file = files[family]
            if (file == null) system(w)
            else try {
                Typeface.Builder(c.applicationContext.assets, "fonts/$file").setFontVariationSettings("'wght' $w").build()
            } catch (_: Exception) { null } ?: Fonts.light(c)
        }
    }

    private fun system(w: Int): Typeface =
        if (android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.SANS_SERIF, w, false)
        else Typeface.create(when { w <= 200 -> "sans-serif-thin"; w <= 300 -> "sans-serif-light"; w <= 400 -> "sans-serif"; else -> "sans-serif-medium" }, Typeface.NORMAL)
}
