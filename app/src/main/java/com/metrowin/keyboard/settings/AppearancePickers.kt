package com.metrowin.keyboard.settings

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.metrowin.keyboard.K
import com.metrowin.keyboard.KeyboardApp
import com.metrowin.keyboard.R
import com.metrowin.keyboard.theme.Accents
import com.metrowin.keyboard.theme.KeyboardTheme
import com.metrowin.keyboard.ui.Fonts
import com.metrowin.keyboard.ui.KeyFonts
import com.metrowin.keyboard.ui.dp

/**
 * A setting of the Appearance screen that shows its choices instead of hiding them in a list: a small heading in the Metro cyan and, under
 * it, whatever [build] puts there. [refresh] redraws it from the settings, which the screen calls whenever one changes.
 */
/** Something on the Appearance screen that redraws itself from the settings. */
interface PickerRefresh { fun refresh() }

abstract class PickerPreference(context: Context, attrs: AttributeSet?, private val titleRes: Int) : Preference(context, attrs), PickerRefresh {
    protected val prefs get() = KeyboardApp.of(context).prefs
    private var content: LinearLayout? = null

    init {
        layoutResource = R.layout.pref_keyboard_preview
        isSelectable = false
        isPersistent = false
    }

    protected abstract fun build(content: LinearLayout)
    abstract override fun refresh()

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val frame = holder.itemView as FrameLayout
        if (frame.childCount == 0) {
            val box = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(0, context.dp(8f).toInt(), 0, 0) }
            box.addView(Ui.text(context, context.getString(titleRes), Ui.Type.LABEL, Ui.metroText(context)).apply { typeface = Fonts.semibold(context) },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = context.dp(8f).toInt() })
            val c = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            box.addView(c)
            frame.addView(box, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            content = c
            build(c)
        }
        refresh()
    }

    protected fun save(key: String, value: String) = prefs.sp.edit().putString(key, value).apply()
}

/** Dark or light: three buttons side by side, the one in use filled with the Metro cyan. */
class ModePicker(context: Context, attrs: AttributeSet?) : PickerPreference(context, attrs, R.string.pref_theme_mode_title) {
    private val modes = listOf("system" to R.string.mode_system, "light" to R.string.mode_light, "dark" to R.string.mode_dark)
    private var group: MaterialButtonToggleGroup? = null
    private val ids = modes.indices.map { View.generateViewId() }

    override fun build(content: LinearLayout) {
        val tile = Ui.metroTile(context); val onTile = Ui.onMetroTile(context); val text = Ui.onSurface(context); val outline = Ui.color(context, com.google.android.material.R.attr.colorOutline)
        val checked = intArrayOf(android.R.attr.state_checked); val other = intArrayOf()
        val g = MaterialButtonToggleGroup(context).apply { isSingleSelection = true; isSelectionRequired = true }
        modes.forEachIndexed { i, (code, label) ->
            val b = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                id = ids[i]; this.text = context.getString(label); isCheckable = true
                backgroundTintList = ColorStateList(arrayOf(checked, other), intArrayOf(tile, Color.TRANSPARENT))
                setTextColor(ColorStateList(arrayOf(checked, other), intArrayOf(onTile, text)))
                strokeColor = ColorStateList.valueOf(outline)
                typeface = Fonts.regular(context)
                setOnClickListener { if (prefs.themeMode != code) save(K.THEME_MODE, code) }
            }
            g.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        content.addView(g, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        group = g
    }

    override fun refresh() {
        val i = modes.indexOfFirst { it.first == prefs.themeMode }.coerceAtLeast(0)
        group?.let { if (it.checkedButtonId != ids[i]) it.check(ids[i]) }
    }
}

/** The key font: each family as a tile that shows "Aa" in that font (at the weight in use) and its name. */
class FontPicker(context: Context, attrs: AttributeSet?) : PickerPreference(context, attrs, R.string.pref_key_font_title) {
    private var row: LinearLayout? = null

    override fun build(content: LinearLayout) {
        val r = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, context.dp(4f).toInt()) }
        content.addView(HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; addView(r) })
        row = r
    }

    override fun refresh() {
        val r = row ?: return
        val names = context.resources.getStringArray(R.array.key_font_names)
        val now = prefs.keyFont
        r.removeAllViews()
        KeyFonts.families.forEachIndexed { i, family ->
            val picked = family == now
            val sample = Ui.text(context, "Aa", Ui.Type.HEADLINE, Ui.onSurface(context)).apply {
                typeface = KeyFonts.get(context, family, prefs.keyWeight); setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f); gravity = Gravity.CENTER
            }
            val name = Ui.text(context, names.getOrElse(i) { family }, Ui.Type.CAPTION, if (picked) Ui.metroText(context) else Ui.onSurfaceVariant(context)).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; minLines = 2; maxLines = 2 }
            val tile = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(context.dp(6f).toInt(), context.dp(8f).toInt(), context.dp(6f).toInt(), context.dp(8f).toInt())
                background = GradientDrawable().apply {
                    cornerRadius = context.dp(10f)
                    setStroke(context.dp(if (picked) 2.5f else 1f).toInt(), if (picked) Ui.metroText(context) else Ui.color(context, com.google.android.material.R.attr.colorOutlineVariant))
                }
                isClickable = true; isFocusable = true; isSelected = picked
                contentDescription = names.getOrElse(i) { family }
                setOnClickListener { if (family != prefs.keyFont) prefs.sp.edit().putString(K.KEY_FONT, family).putString(K.KEY_WEIGHT, KeyFonts.snap(family, prefs.keyWeight).toString()).apply() }
                addView(sample); addView(name)
            }
            // Every name takes two lines, so the tiles are alike in height at any text size.
            r.addView(tile, LinearLayout.LayoutParams(context.dp(112f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = context.dp(10f).toInt() })
        }
    }
}

/** The key weight: a sample sentence in the font and weight in use, over a slider with a stop for each weight the font really has. */
class WeightPicker(context: Context, attrs: AttributeSet?) : PickerPreference(context, attrs, R.string.pref_key_weight_title) {
    private var sample: TextView? = null
    private var name: TextView? = null
    private var slider: Slider? = null
    private var family = ""

    private fun weightName(w: Int) = context.getString(when (w) { 200 -> R.string.weight_200; 300 -> R.string.weight_300; 400 -> R.string.weight_400; else -> R.string.weight_500 })

    override fun build(content: LinearLayout) {
        val s = Ui.text(context, context.getString(R.string.weight_sample), Ui.Type.TITLE, Ui.onSurface(context)).apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f) }
        val n = Ui.text(context, "", Ui.Type.CAPTION, Ui.onSurfaceVariant(context))
        val sl = Slider(context).apply {
            valueFrom = 0f; stepSize = 1f; valueTo = 1f
            val cyan = ColorStateList.valueOf(Ui.metroText(context))
            thumbTintList = cyan; trackActiveTintList = cyan; haloTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            trackInactiveTintList = ColorStateList.valueOf(Ui.color(context, com.google.android.material.R.attr.colorOutlineVariant))
            tickActiveTintList = ColorStateList.valueOf(Ui.onMetroTile(context)); tickInactiveTintList = ColorStateList.valueOf(Ui.color(context, com.google.android.material.R.attr.colorOutline))
            setLabelFormatter { weightName(KeyFonts.weightsFor(family).getOrElse(it.toInt()) { 400 }) }
            addOnChangeListener { _, v, fromUser ->
                if (!fromUser) return@addOnChangeListener
                val w = KeyFonts.weightsFor(family).getOrNull(v.toInt()) ?: return@addOnChangeListener
                if (w != prefs.keyWeight) save(K.KEY_WEIGHT, w.toString())
            }
        }
        content.addView(s); content.addView(n)
        content.addView(sl, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        sample = s; name = n; slider = sl
    }

    override fun refresh() {
        val sl = slider ?: return
        family = prefs.keyFont
        val weights = KeyFonts.weightsFor(family)
        val w = prefs.keyWeight
        sample?.typeface = KeyFonts.get(context, family, w)
        name?.text = weightName(w) + " · " + w
        // A font with a single weight has nothing to slide.
        sl.visibility = if (weights.size > 1) View.VISIBLE else View.GONE
        sl.valueTo = (weights.size - 1).toFloat().coerceAtLeast(1f)
        sl.contentDescription = context.getString(R.string.pref_key_weight_title)
        val idx = weights.indexOf(w).coerceAtLeast(0).toFloat()
        if (sl.value != idx) sl.value = idx
    }
}

/**
 * The accent colour: "follow the look", six colours and a custom one, as round swatches. The colour of the look is shown on its own swatch
 * so that it is a choice made by seeing; the custom swatch opens the box for a #RRGGBB.
 */
class AccentPicker(context: Context, attrs: AttributeSet?) : PickerPreference(context, attrs, R.string.pref_accent_title) {
    /** What the custom swatch does; the screen sets it. */
    var onCustom: (() -> Unit)? = null
    private var row: LinearLayout? = null

    override fun build(content: LinearLayout) {
        val r = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, context.dp(4f).toInt()) }
        content.addView(HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; addView(r) })
        row = r
    }

    private fun dot(fill: Int, picked: Boolean, glyph: String? = null): View {
        val size = context.dp(48f).toInt()
        val outline = Ui.color(context, com.google.android.material.R.attr.colorOutline)
        val view = TextView(context).apply {
            gravity = Gravity.CENTER; text = glyph ?: ""; setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f); setTextColor(Ui.onSurface(context))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (fill != 0) setColor(fill) else setColor(Color.TRANSPARENT)
                setStroke(context.dp(1f).toInt(), outline)
            }
        }
        // The ring of the one in use stands a little off the colour, in the cyan of the other pickers.
        return FrameLayout(context).apply {
            val gap = context.dp(4f).toInt()
            setPadding(gap, gap, gap, gap)
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT); if (picked) setStroke(context.dp(2.5f).toInt(), Ui.metroText(context)) }
            addView(view, FrameLayout.LayoutParams(size, size))
        }
    }

    override fun refresh() {
        val r = row ?: return
        val names = context.resources.getStringArray(R.array.accent_names)
        val now = prefs.customAccent
        val system = KeyboardTheme.isSystemDark(context)
        val presets = Accents.PRESETS
        val custom = now != 0 && presets.none { (it or 0xFF000000.toInt()) == (now or 0xFF000000.toInt()) }
        r.removeAllViews()
        fun tile(label: String, picked: Boolean, dotView: View, onClick: () -> Unit) {
            val name = Ui.text(context, label, Ui.Type.CAPTION, if (picked) Ui.metroText(context) else Ui.onSurfaceVariant(context)).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; minLines = 2; maxLines = 2 }
            val t = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                isClickable = true; isFocusable = true; isSelected = picked; contentDescription = label
                setOnClickListener { onClick() }
                addView(dotView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(name, LinearLayout.LayoutParams(context.dp(96f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            r.addView(t, LinearLayout.LayoutParams(context.dp(96f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = context.dp(4f).toInt() })
        }
        // Modern Droid on Android 12+ can take its colours from the wallpaper: that is one more swatch, in the wallpaper's own colour, not a separate switch.
        val wallpaper = com.metrowin.keyboard.theme.ThemeStyle.from(prefs.themeStyle).base == com.metrowin.keyboard.theme.ThemeStyle.DROID && com.metrowin.keyboard.theme.SystemTones.read(context) != null
        val onWallpaper = wallpaper && now == 0 && prefs.dynamicColor
        tile(context.getString(R.string.accent_look), now == 0 && !onWallpaper, dot(KeyboardTheme.lookAccent(context, prefs, system, wallpaper = false), now == 0 && !onWallpaper)) {
            // Only where the wallpaper is an option does "follow the look" also turn it off; on the other looks that setting is left alone.
            prefs.sp.edit().putInt(K.CUSTOM_ACCENT, 0).apply { if (wallpaper) putBoolean(K.DYNAMIC_COLOR, false) }.apply()
        }
        if (wallpaper) tile(context.getString(R.string.accent_wallpaper), onWallpaper, dot(KeyboardTheme.lookAccent(context, prefs, system, wallpaper = true), onWallpaper)) {
            prefs.sp.edit().putInt(K.CUSTOM_ACCENT, 0).putBoolean(K.DYNAMIC_COLOR, true).apply()
        }
        presets.forEachIndexed { i, c ->
            val picked = now != 0 && (c or 0xFF000000.toInt()) == (now or 0xFF000000.toInt())
            tile(names.getOrElse(i) { "" }, picked, dot(c, picked)) { prefs.sp.edit().putInt(K.CUSTOM_ACCENT, c or 0xFF000000.toInt()).apply() }
        }
        tile(context.getString(R.string.accent_custom), custom, if (custom) dot(now or 0xFF000000.toInt(), true) else dot(0, false, "+")) { onCustom?.invoke() }
    }
}
