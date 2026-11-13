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
            val name = Ui.text(context, names.getOrElse(i) { family }, Ui.Type.CAPTION, if (picked) Ui.metroText(context) else Ui.onSurfaceVariant(context)).apply { gravity = Gravity.CENTER; maxLines = 2 }
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
            r.addView(tile, LinearLayout.LayoutParams(context.dp(104f).toInt(), context.dp(88f).toInt()).apply { marginEnd = context.dp(10f).toInt() })
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
