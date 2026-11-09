package com.metrowin.keyboard.settings

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.metrowin.keyboard.KeyboardApp
import com.metrowin.keyboard.R
import com.metrowin.keyboard.tutorial.Tutorial
import com.metrowin.keyboard.ui.Icon
import com.metrowin.keyboard.ui.Icons
import com.metrowin.keyboard.ui.dp

/** Draws one of the keyboard's icons big, in the accent colour. */
class IconView(context: Context) : View(context) {
    var icon: Icon? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val i = icon ?: return
        paint.color = Ui.primary(context)
        Icons.draw(canvas, i, width / 2f, height / 2f, minOf(width, height) * 0.8f, paint)
    }
}

/** The tutorial of the keyboard, one page at a time, with the pages added since the last time marked as new. */
class TutorialActivity : AppCompatActivity() {
    private var index = 0
    private var seen = 0
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var body: TextView
    private lateinit var tag: TextView
    private lateinit var iconView: IconView
    private lateinit var counter: TextView
    private lateinit var back: TextView
    private lateinit var next: TextView
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = KeyboardApp.of(this).prefs
        seen = prefs.tutorialSeen
        index = savedInstanceState?.getInt("index") ?: intent.getIntExtra("start", Tutorial.startIndex(seen))

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        toolbar = Ui.toolbar(this, getString(R.string.tutorial_title))
        root.addView(toolbar)

        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16f).toInt(), dp(12f).toInt(), dp(16f).toInt(), dp(12f).toInt()) }
        tag = Ui.text(this, getString(R.string.tutorial_new), Ui.Type.LABEL, Ui.primary(this))
        iconView = IconView(this)
        body = Ui.text(this, "", Ui.Type.BODY).apply { setLineSpacing(0f, 1.2f) }
        content.addView(tag)
        content.addView(iconView, LinearLayout.LayoutParams(dp(72f).toInt(), dp(72f).toInt()).apply { topMargin = dp(8f).toInt(); bottomMargin = dp(8f).toInt() })
        content.addView(body)
        root.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val footer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16f).toInt(), dp(8f).toInt(), dp(16f).toInt(), dp(16f).toInt()) }
        back = Ui.button(this, getString(R.string.tutorial_back), filled = false) { go(index - 1) }
        counter = Ui.text(this, "", Ui.Type.BODY_SMALL, Ui.onSurfaceVariant(this)).apply { gravity = Gravity.CENTER }
        next = Ui.button(this, getString(R.string.tutorial_next)) { if (index == Tutorial.pages.lastIndex) finish() else go(index + 1) }
        footer.addView(back)
        footer.addView(counter, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        footer.addView(next)
        root.addView(footer)
        setContentView(root)
        Ui.applyInsets(root)
        show()
    }

    private fun go(i: Int) {
        if (i !in Tutorial.pages.indices) return
        index = i
        content.animate().alpha(0f).translationX(-dp(24f)).setDuration(90).withEndAction {
            show()
            content.translationX = dp(24f)
            content.animate().alpha(1f).translationX(0f).setDuration(140).start()
        }.start()
    }

    private fun show() {
        val page = Tutorial.pages[index]
        toolbar.title = getString(page.title)
        body.text = getString(page.body)
        iconView.icon = page.icon; iconView.visibility = if (page.icon == null) View.GONE else View.VISIBLE; iconView.invalidate()
        tag.visibility = if (seen > 0 && page.since > seen) View.VISIBLE else View.GONE
        counter.text = "${index + 1} / ${Tutorial.pages.size}"
        back.visibility = if (index == 0) View.INVISIBLE else View.VISIBLE
        next.text = getString(if (index == Tutorial.pages.lastIndex) R.string.tutorial_done else R.string.tutorial_next)
        // Reaching the last page counts as having seen it all.
        if (index == Tutorial.pages.lastIndex) KeyboardApp.of(this).prefs.tutorialSeen = Tutorial.VERSION
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putInt("index", index) }
}
