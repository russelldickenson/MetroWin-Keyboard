package com.metrowin.keyboard.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.R as M
import com.metrowin.keyboard.KeyboardApp
import com.metrowin.keyboard.R
import com.metrowin.keyboard.tutorial.Tutorial
import com.metrowin.keyboard.ui.Fonts
import com.metrowin.keyboard.ui.dp

/** The screens of the settings: the home page opens them by class name, and a test loads each one so a typo cannot hide until it is tapped. */
object Home {
    class Screen(val fragment: String, val title: Int, val summary: Int)

    private fun screen(name: String, title: Int, summary: Int) = Screen("com.metrowin.keyboard.settings.${name}Fragment", title, summary)

    /** The settings in three groups of no more than four, so no list asks for more than a glance. */
    val typing = listOf(
        screen("Languages", R.string.screen_languages, R.string.screen_languages_summary),
        screen("Typing", R.string.screen_typing, R.string.screen_typing_summary),
        screen("Suggestions", R.string.screen_suggestions, R.string.screen_suggestions_summary),
        screen("Snippets", R.string.screen_snippets, R.string.screen_snippets_summary),
    )
    val looks = listOf(
        screen("Appearance", R.string.screen_appearance, R.string.screen_appearance_summary),
        screen("Size", R.string.screen_size, R.string.screen_size_summary),
        screen("Feedback", R.string.screen_feedback, R.string.screen_feedback_summary),
    )
    val tools = listOf(
        screen("Clipboard", R.string.screen_clipboard, R.string.screen_clipboard_summary),
        screen("Voice", R.string.screen_voice, R.string.screen_voice_summary),
        screen("Backup", R.string.screen_backup, R.string.screen_backup_summary),
    )
    val settings = typing + looks + tools
    val about = listOf(
        screen("About", R.string.screen_about, R.string.screen_about_summary),
    )
    val all get() = (settings + about).map { it.fragment }
}

/**
 * The home page: one scrolling list. At the top the things worth doing first (the calibration, turning the keyboard on),
 * then the settings screens and the about screens, each group under a small heading.
 */
class HomeFragment : Fragment() {
    private var setup: LinearLayout? = null
    private var enable: Row? = null
    private var select: Row? = null
    private var tutorial: Row? = null

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val c = requireContext()
        val list = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; setPadding(0, c.dp(8f).toInt(), 0, c.dp(24f).toInt()) }
        list.addView(homeTitle(c))
        // The two steps that make the keyboard work come first, and go away once both are done.
        setup = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        setup!!.addView(heading(c, R.string.pivot_start))
        enable = row(c, R.string.setup_enable_title, R.string.setup_enable_summary) { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }.also { setup!!.addView(it.view) }
        select = row(c, R.string.setup_select_title, R.string.setup_select_summary) { c.getSystemService(InputMethodManager::class.java).showInputMethodPicker() }.also { setup!!.addView(it.view) }
        list.addView(setup)
        list.addView(calibrationCard(c))
        list.addView(heading(c, R.string.pivot_typing)); for (s in Home.typing) list.addView(row(c, s.title, s.summary) { open(s.fragment) }.view)
        list.addView(heading(c, R.string.pivot_looks)); for (s in Home.looks) list.addView(row(c, s.title, s.summary) { open(s.fragment) }.view)
        list.addView(heading(c, R.string.pivot_tools)); for (s in Home.tools) list.addView(row(c, s.title, s.summary) { open(s.fragment) }.view)
        list.addView(heading(c, R.string.pivot_help))
        list.addView(row(c, R.string.setup_try_title, R.string.setup_try_summary) { startActivity(Intent(c, TestActivity::class.java)) }.view)
        tutorial = row(c, R.string.setup_tutorial_title, R.string.setup_tutorial_summary) { startActivity(Intent(c, TutorialActivity::class.java)) }.also { list.addView(it.view) }
        list.addView(row(c, R.string.setup_guide_title, R.string.setup_guide_summary) { startActivity(Intent(c, OnboardingActivity::class.java)) }.view)
        for (screen in Home.about) list.addView(row(c, screen.title, screen.summary) { open(screen.fragment) }.view)
        return NestedScrollView(c).apply { clipToPadding = false; addView(list, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)) }
    }

    private fun open(name: String) = parentFragmentManager.beginTransaction()
        .replace(R.id.settings_container, parentFragmentManager.fragmentFactory.instantiate(requireContext().classLoader, name)).addToBackStack(null).commit()

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.app_name)
        val pkg = requireContext().packageName
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        val enabled = imm.enabledInputMethodList.any { it.packageName == pkg }
        val active = Settings.Secure.getString(requireContext().contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.startsWith("$pkg/") == true
        setup?.visibility = if (enabled && active) View.GONE else View.VISIBLE
        enable?.summary?.setText(if (enabled) R.string.setup_enabled else R.string.setup_disabled)
        select?.summary?.setText(if (active) R.string.setup_active else R.string.setup_inactive)
        val seen = KeyboardApp.of(requireContext()).prefs.tutorialSeen
        tutorial?.summary?.setText(if (Tutorial.hasNew(seen)) R.string.setup_tutorial_new else R.string.setup_tutorial_summary)
    }

    /** A tonal card in the colour of the app: a title, a line under it, and a tap that opens [onClick]. */
    private fun card(c: Context, title: String, body: String, onClick: () -> Unit): Pair<MaterialCardView, TextView> {
        // A flat tile, square-cornered, in the cyan of Windows Phone, with light type on it.
        val on = Ui.onMetroTile(c)
        val bodyView = Ui.text(c, body, Ui.Type.BODY_SMALL, on).apply { typeface = Fonts.regular(c) }
        val card = MaterialCardView(c, null, M.attr.materialCardViewFilledStyle).apply {
            setCardBackgroundColor(Ui.metroTile(c)); radius = 0f; cardElevation = 0f; strokeWidth = 0
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
            addView(LinearLayout(c).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(c.dp(20f).toInt(), c.dp(16f).toInt(), c.dp(20f).toInt(), c.dp(16f).toInt())
                addView(Ui.text(c, title, Ui.Type.TITLE, on).apply { typeface = Fonts.light(c); setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f) })
                addView(bodyView)
            })
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(c.dp(16f).toInt(), c.dp(4f).toInt(), c.dp(16f).toInt(), c.dp(8f).toInt()) }
        }
        return card to bodyView
    }

    /** The calibration: the thing most worth doing right after turning the keyboard on. */
    private fun calibrationCard(c: Context): View =
        card(c, getString(R.string.tile_calibrate_title), getString(R.string.tile_calibrate_body)) { startActivity(Intent(c, CalibrationActivity::class.java)) }.first

    /** The large light title of the home page, as the panorama title of Windows Phone; it stands where the app bar would be. */
    private fun homeTitle(c: Context) = Ui.text(c, getString(R.string.app_name), Ui.Type.HEADLINE).apply {
        // On a short (landscape) screen the title is smaller and sits closer to the top, so the list is not pushed below the fold.
        val short = c.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        typeface = Fonts.light(c); setTextSize(TypedValue.COMPLEX_UNIT_SP, if (short) 28f else 38f)
        setPadding(c.dp(16f).toInt(), c.dp(if (short) 8f else 32f).toInt(), c.dp(16f).toInt(), c.dp(if (short) 4f else 12f).toInt())
        ViewCompat.setAccessibilityHeading(this, true)
    }

    private fun heading(c: Context, title: Int) =
        Ui.text(c, getString(title), Ui.Type.LABEL, Ui.metroText(c)).apply {
            typeface = Fonts.semibold(c); ViewCompat.setAccessibilityHeading(this, true)
            setPadding(c.dp(16f).toInt(), c.dp(20f).toInt(), c.dp(16f).toInt(), c.dp(4f).toInt())
        }

    private fun row(c: Context, title: Int, summary: Int, onClick: () -> Unit) = Row(c, c.getString(title), if (summary != 0) c.getString(summary) else "", onClick)
}

/** One entry of a list: a title over a line of smaller text, with the ripple of a Material list item. */
class Row(c: Context, title: String, summary: String, onClick: () -> Unit) {
    val summary: TextView = Ui.text(c, summary, Ui.Type.BODY_SMALL, Ui.onSurfaceVariant(c)).apply { typeface = Fonts.regular(c) }
    val view = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(c.dp(16f).toInt(), c.dp(12f).toInt(), c.dp(16f).toInt(), c.dp(12f).toInt())
        addView(Ui.text(c, title, Ui.Type.BODY).apply { typeface = Fonts.regular(c) })
        addView(this@Row.summary)
        val ripple = TypedValue()
        if (c.theme.resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)) setBackgroundResource(ripple.resourceId)
        isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }
}
