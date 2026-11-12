package com.metrowin.keyboard.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.metrowin.keyboard.KeyboardApp
import com.metrowin.keyboard.R
import com.metrowin.keyboard.K
import com.metrowin.keyboard.theme.KeyboardColors
import com.metrowin.keyboard.theme.KeyboardTheme
import com.metrowin.keyboard.theme.ThemeStyle
import com.metrowin.keyboard.ui.dp

/**
 * The looks as a row of small keyboards to tap, each drawn in its own colours and key shape (in the dark or light the settings ask for),
 * so a look is chosen by seeing it. The one in use has an outline in the colour of the app.
 */
class LookPicker(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    private var row: LinearLayout? = null

    init {
        layoutResource = R.layout.pref_keyboard_preview
        isSelectable = false
        isPersistent = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val frame = holder.itemView as FrameLayout
        if (frame.childCount == 0) {
            val r = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, context.dp(4f).toInt()) }
            val scroll = HorizontalScrollView(context).apply { isHorizontalScrollBarEnabled = false; clipToPadding = false; addView(r) }
            val box = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(Ui.text(context, context.getString(R.string.pref_theme_style_title), Ui.Type.LABEL, Ui.primary(context)),
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = context.dp(8f).toInt() })
                addView(scroll)
            }
            frame.addView(box, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            row = r
        }
        refresh()
    }

    fun refresh() {
        val r = row ?: return
        val p = KeyboardApp.of(context).prefs
        val names = context.resources.getStringArray(R.array.theme_style_names)
        val dark = KeyboardTheme.isSystemDark(context).let { sys -> when (p.themeMode) { "dark" -> true; "light" -> false; else -> sys } }
        val current = ThemeStyle.from(p.themeStyle)
        r.removeAllViews()
        ThemeStyle.entries.forEachIndexed { i, style ->
            val colors = KeyboardTheme.resolve(context, p, KeyboardTheme.isSystemDark(context), style)
            val tile = Tile(context, colors, style == current, dark)
            val label = Ui.text(context, names.getOrElse(i) { style.code }.substringBefore(" ("), Ui.Type.CAPTION,
                if (style == current) Ui.primary(context) else Ui.onSurfaceVariant(context)).apply { gravity = Gravity.CENTER_HORIZONTAL; maxLines = 2 }
            val cell = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                isClickable = true; isFocusable = true
                contentDescription = names.getOrElse(i) { style.code }
                isSelected = style == current
                setOnClickListener { p.sp.edit().putString(K.THEME_STYLE, style.code).apply() }
                addView(tile, LinearLayout.LayoutParams(context.dp(112f).toInt(), context.dp(76f).toInt()))
                addView(label, LinearLayout.LayoutParams(context.dp(112f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = context.dp(4f).toInt() })
            }
            r.addView(cell, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = context.dp(10f).toInt() })
        }
    }

    /** A miniature keyboard: the surround, three rows of keys, a special key on each side of the bottom one and the accent on the action key. */
    private class Tile(c: Context, private val k: KeyboardColors, private val selected: Boolean, dark: Boolean) : View(c) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()
        private val ring = Ui.primary(c)

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            val inset = context.dp(2f)
            val pad = context.dp(6f)
            // The surround, rounded as a card.
            paint.style = Paint.Style.FILL; paint.color = k.background or 0xFF000000.toInt()
            rect.set(inset, inset, w - inset, h - inset)
            canvas.drawRoundRect(rect, context.dp(10f), context.dp(10f), paint)
            val gap = if (k.spacedKeys) context.dp(3.5f) else context.dp(2f)
            val rows = intArrayOf(7, 6, 5)
            val kh = (h - 2 * inset - 2 * pad - gap * 3) / 4
            val corner = (k.cornerDp.coerceAtMost(14f) / 14f) * kh / 2f
            var y = inset + pad
            for ((ri, n) in rows.withIndex()) {
                val kw = (w - 2 * inset - 2 * pad - gap * (n - 1)) / n
                for (j in 0 until n) {
                    val special = ri == 2 && (j == 0 || j == n - 1)
                    paint.style = Paint.Style.FILL; paint.color = (if (special) k.special else k.key).let { it or 0xFF000000.toInt() }
                    val x = inset + pad + j * (kw + gap)
                    rect.set(x, y, x + kw, y + kh)
                    canvas.drawRoundRect(rect, corner, corner, paint)
                    if (k.strokeDp > 0f) { paint.style = Paint.Style.STROKE; paint.strokeWidth = context.dp(k.strokeDp); paint.color = k.stroke or 0xFF000000.toInt(); canvas.drawRoundRect(rect, corner, corner, paint) }
                }
                y += kh + gap
            }
            // The bottom row: a short special key, the space bar, the action key in the accent.
            val bw = w - 2 * inset - 2 * pad
            val parts = listOf(0.18f to k.special, 0.52f to k.key, 0.22f to k.accent)
            var x = inset + pad
            for ((frac, col) in parts) {
                val width = (bw - gap * 2) * frac / 0.92f
                paint.style = Paint.Style.FILL; paint.color = col or 0xFF000000.toInt()
                rect.set(x, y, x + width, y + kh)
                canvas.drawRoundRect(rect, corner, corner, paint)
                x += width + gap
            }
            if (selected) {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = context.dp(2.5f); paint.color = ring
                rect.set(inset, inset, w - inset, h - inset)
                canvas.drawRoundRect(rect, context.dp(10f), context.dp(10f), paint)
            }
        }
    }
}
