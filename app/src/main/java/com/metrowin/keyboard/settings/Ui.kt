package com.metrowin.keyboard.settings

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.R as M
import com.metrowin.keyboard.R
import com.metrowin.keyboard.ui.dp

/** The small kit the app's screens are built from: Material 3 type styles and colours (from the theme, so they follow Material You), buttons and the top app bar. */
object Ui {
    /** A colour of the current theme, by its attribute. */
    fun color(c: Context, attr: Int, fallback: Int = 0xFF808080.toInt()): Int = MaterialColors.getColor(c, attr, fallback)

    fun primary(c: Context) = color(c, androidx.appcompat.R.attr.colorPrimary)
    fun onSurface(c: Context) = color(c, M.attr.colorOnSurface)
    fun onSurfaceVariant(c: Context) = color(c, M.attr.colorOnSurfaceVariant)
    fun primaryContainer(c: Context) = color(c, M.attr.colorPrimaryContainer)
    fun onPrimaryContainer(c: Context) = color(c, M.attr.colorOnPrimaryContainer)

    /** Android 15 draws apps edge to edge: keep the content out from under the system bars and the keyboard. */
    fun applyInsets(root: View) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    /** The Material 3 type styles the screens use. */
    enum class Type(val attr: Int) {
        HEADLINE(M.attr.textAppearanceHeadlineSmall), TITLE(M.attr.textAppearanceTitleLarge), SUBTITLE(M.attr.textAppearanceTitleMedium),
        BODY(M.attr.textAppearanceBodyLarge), BODY_SMALL(M.attr.textAppearanceBodyMedium), LABEL(M.attr.textAppearanceLabelLarge), CAPTION(M.attr.textAppearanceBodySmall),
    }

    /** A text in one of the type styles; [color] defaults to the main text colour of the theme. */
    fun text(c: Context, text: CharSequence, type: Type, color: Int = onSurface(c)): TextView = TextView(c).apply {
        val tv = TypedValue()
        if (c.theme.resolveAttribute(type.attr, tv, true)) TextViewCompat.setTextAppearance(this, tv.resourceId)
        this.text = text
        setTextColor(color)
    }

    /** A Material button: filled, or outlined. */
    fun button(c: Context, text: CharSequence, filled: Boolean = true, onClick: () -> Unit): MaterialButton =
        MaterialButton(c, null, if (filled) M.attr.materialButtonStyle else M.attr.materialButtonOutlinedStyle).apply {
            this.text = text
            setOnClickListener { onClick() }
        }

    /** The top app bar of a screen of its own: a title and, if [back] is given, an arrow that calls it. */
    fun toolbar(c: Context, title: CharSequence, back: (() -> Unit)? = null): MaterialToolbar = MaterialToolbar(c).apply {
        this.title = title
        if (back != null) {
            setNavigationIcon(R.drawable.ic_arrow_back)
            setNavigationContentDescription(R.string.back)
            setNavigationOnClickListener { back() }
        }
    }

    fun toolbar(a: AppCompatActivity, title: CharSequence) = toolbar(a, title) { a.onBackPressedDispatcher.onBackPressed() }

    /** True when this app's keyboard is the one Android types with. */
    fun isActiveKeyboard(c: Context): Boolean =
        android.provider.Settings.Secure.getString(c.contentResolver, android.provider.Settings.Secure.DEFAULT_INPUT_METHOD)?.startsWith("${c.packageName}/") == true

    /** A note, with a button for the keyboard picker, for the screens that only make sense when this keyboard is the active one. */
    fun notActiveNotice(c: Context): LinearLayout = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, c.dp(12f).toInt())
        addView(text(c, c.getString(R.string.keyboard_not_active), Type.BODY_SMALL, onSurfaceVariant(c)))
        addView(button(c, c.getString(R.string.onb_open_picker), filled = false) { c.getSystemService(android.view.inputmethod.InputMethodManager::class.java).showInputMethodPicker() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = c.dp(8f).toInt() })
        visibility = if (isActiveKeyboard(c)) View.GONE else View.VISIBLE
    }

    fun space(c: Context, dp: Int) = View(c).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, c.dp(dp.toFloat()).toInt()) }
}
