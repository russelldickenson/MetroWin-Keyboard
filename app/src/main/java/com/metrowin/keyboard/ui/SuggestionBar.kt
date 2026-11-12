package com.metrowin.keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import com.metrowin.keyboard.R
import com.metrowin.keyboard.theme.ColorMath
import com.metrowin.keyboard.theme.KeyboardColors

/** What the toolbox offers. The same list feeds the quick button setting. */
enum class Tool(val code: String, val icon: Icon) {
    CLIPBOARD("clipboard", Icon.CLIPBOARD), EMOJI("emoji", Icon.EMOJI),
    MIC("mic", Icon.MIC), HELP("help", Icon.HELP), SETTINGS("settings", Icon.SETTINGS);

    companion object { fun from(code: String?) = entries.firstOrNull { it.code == code } ?: CLIPBOARD }
}

/** One of the three places of the bar. [best] is the word that space will insert; [literal] is exactly what was typed. */
data class Slot(val text: String, val best: Boolean = false, val literal: Boolean = false, val phrase: Boolean = false, val emoji: Boolean = false, val snippet: Boolean = false)

/**
 * The strip above the keys, laid out like an app bar: the three dots on the left open the toolbox, the middle holds three
 * suggestions (or the tools, when the toolbox is open), and on the right a quick button goes straight to one tool (the clipboard
 * by default), with a dot when something new was copied. Also shows a short status text (an emoji search, "listening"). With more than
 * three slots (emoji found by ":dog") the middle is a row that slides sideways.
 */
class SuggestionBar(context: Context) : View(context) {
    interface Listener {
        fun onSlot(slot: Slot)
        fun onTool(tool: Tool)
    }

    var listener: Listener? = null
    private var colors: KeyboardColors? = null
    private var slots: List<Slot?> = listOf(null, null, null)
    private var toolsOpen = false
    private var status: String? = null
    private var voiceActive = false
    private var quick = Tool.CLIPBOARD
    private var badge = false
    private var pressedIndex = NONE
    private var scroll = 0f
    private var scrollAtDown = 0f
    private var downX = 0f
    private var dragging = false
    private val slop = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val metrics = Paint.FontMetrics()
    // What is worked out once and kept until a slot, the font or the look changes: the three typefaces and the widths of the cells.
    private val faces = arrayOfNulls<android.graphics.Typeface>(3)
    private var cachedEdges: FloatArray? = null
    private var cachedEdgesW = -1f
    private var cachedEdgesH = -1
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG)
    // The tool the quick button already offers is not repeated in the toolbox.
    private val tools get() = Tool.entries.filter { it != quick }

    fun setColors(c: KeyboardColors) { if (colors?.style != c.style) forget(); colors = c; invalidate() }

    private fun forget() { faces.fill(null); cachedEdges = null }
    fun setSlots(s: List<Slot?>) { if (s != slots) { scroll = 0f; cachedEdges = null }; slots = s; if (!toolsOpen) invalidate(); a11y.invalidateRoot() }
    internal fun slotsForTest() = slots
    fun setStatus(text: String?) { status = text; invalidate(); a11y.invalidateRoot() }
    fun setVoiceActive(on: Boolean) { voiceActive = on; invalidate() }
    fun setQuickTool(t: Tool) { if (quick != t) { quick = t; invalidate(); a11y.invalidateRoot() } }
    fun setBadge(on: Boolean) { if (badge != on) { badge = on; invalidate(); a11y.invalidateRoot() } }
    fun toggleTools() { toolsOpen = !toolsOpen; invalidate(); a11y.invalidateRoot() }
    fun closeTools() { if (toolsOpen) { toolsOpen = false; invalidate(); a11y.invalidateRoot() } }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(46f).toInt())

    private fun buttonWidth() = height * 1.1f

    override fun onDraw(canvas: Canvas) {
        val c = colors ?: return
        canvas.drawColor(c.bar)
        val h = height.toFloat()
        val bw = buttonWidth()
        if (pressedIndex == LEFT) highlight(canvas, c, 0f, bw)
        icon.color = if (toolsOpen) c.barAccent else c.subtle
        Icons.draw(canvas, Icon.ELLIPSIS, bw / 2f, h / 2f, h * 0.42f, icon)

        // The quick button, drawn where the Windows Phone app bar keeps its main action.
        val qx = width - bw
        if (pressedIndex == QUICK) highlight(canvas, c, qx, width.toFloat())
        icon.color = c.text
        Icons.draw(canvas, quick.icon, qx + bw / 2f, h / 2f, h * 0.42f, icon)
        if (badge) { fill.color = c.barAccent; canvas.drawCircle(qx + bw / 2f + h * 0.2f, h / 2f - h * 0.2f, h * 0.075f, fill) }

        val x0 = bw
        val w = width - 2 * bw
        val text = status
        if (text != null) {
            drawText(canvas, text, x0 + w / 2f, h / 2f, w - PAD, c.text, false, false)
            if (voiceActive) { icon.color = c.barAccent; Icons.draw(canvas, Icon.MIC, qx + bw / 2f, h / 2f, h * 0.42f, icon) }
            return
        }
        if (toolsOpen) {
            val cell = w / tools.size
            tools.forEachIndexed { i, t ->
                if (pressedIndex == i) highlight(canvas, c, x0 + cell * i, x0 + cell * (i + 1))
                icon.color = if (t == Tool.MIC && voiceActive) c.barAccent else c.text
                Icons.draw(canvas, t.icon, x0 + cell * i + cell / 2f, h / 2f, h * 0.4f, icon)
            }
            return
        }
        if (scrolling()) { drawRow(canvas, c, x0, w); return }
        val e = edges(w)
        if (c.style == com.metrowin.keyboard.theme.ThemeStyle.IPHONE) {
            fill.color = ColorMath.withAlpha(c.text, 0x30)
            for (i in 1..2) canvas.drawRect(x0 + e[i] - dp(0.5f), h * 0.2f, x0 + e[i] + dp(0.5f), h * 0.8f, fill)
        }
        for (i in 0 until 3) {
            val s = slots.getOrNull(i) ?: continue
            if (pressedIndex == i) highlight(canvas, c, x0 + e[i], x0 + e[i + 1])
            drawText(canvas, shown(s), x0 + (e[i] + e[i + 1]) / 2f, h / 2f, e[i + 1] - e[i] - PAD, c.text, s.best, s.phrase)
        }
    }

    private fun scrolling() = slots.size > 3 && !toolsOpen && status == null

    private fun rowCell(w: Float) = w / 5f
    private fun maxScroll(w: Float) = maxOf(0f, slots.size * rowCell(w) - w)

    /** The slots as a row of equal cells, five in view, that slides sideways. */
    private fun drawRow(canvas: Canvas, c: KeyboardColors, x0: Float, w: Float) {
        val cell = rowCell(w)
        canvas.save()
        canvas.clipRect(x0, 0f, x0 + w, height.toFloat())
        paint.color = c.text
        paint.typeface = face(bold = false)
        paint.textSkewX = 0f
        paint.textSize = height * 0.52f
        paint.getFontMetrics(metrics)
        slots.forEachIndexed { i, s ->
            val left = x0 + i * cell - scroll
            if (s == null || left + cell < x0 || left > x0 + w) return@forEachIndexed
            if (pressedIndex == i) highlight(canvas, c, left, left + cell)
            canvas.drawText(s.text, left + cell / 2f, height / 2f - (metrics.ascent + metrics.descent) / 2f, paint)
        }
        canvas.restore()
    }

    private fun shown(s: Slot) = when { s.literal -> "“${s.text}”"; s.phrase -> s.text + " …"; else -> s.text }

    private var keyFamily = "opensans"
    private var keyWeight = 300

    /** The font of the key letters, which every look but Classic also uses for the suggestions. */
    fun setKeyFont(family: String, weight: Int) { if (keyFamily != family || keyWeight != weight) { keyFamily = family; keyWeight = weight; forget(); invalidate() } }

    /**
     * The typeface of a suggestion. Classic keeps Open Sans (regular, semibold for the best one, light for a phrase); the other looks use
     * the font and weight chosen for the keys, a heavier weight for the best suggestion and a lighter one for a phrase.
     */
    private fun face(bold: Boolean, light: Boolean = false): android.graphics.Typeface {
        val i = if (bold) 1 else if (light) 2 else 0
        faces[i]?.let { return it }
        val made = if (colors?.style == com.metrowin.keyboard.theme.ThemeStyle.CLASSIC) { if (bold) Fonts.semibold(context) else if (light) Fonts.light(context) else Fonts.regular(context) }
            else KeyFonts.get(context, keyFamily, when { bold -> keyWeight + 300; light -> keyWeight - 100; else -> keyWeight })
        faces[i] = made
        return made
    }

    private fun style(bold: Boolean, italic: Boolean) {
        paint.textSize = height * 0.38f
        paint.typeface = if (!bold && italic) face(bold = false, light = true) else face(bold)
        paint.textSkewX = if (italic) -0.18f else 0f
    }

    /**
     * Left edges of the three cells, plus the right end, over a width of [w]. Equal thirds while every text fits in one; otherwise the
     * room is shared in proportion to the texts, so that a long word is not cut.
     */
    private fun edges(w: Float): FloatArray {
        cachedEdges?.let { if (cachedEdgesW == w && cachedEdgesH == height) return it }
        val e = computeEdges(w)
        cachedEdges = e; cachedEdgesW = w; cachedEdgesH = height
        return e
    }

    private fun computeEdges(w: Float): FloatArray {
        val want = FloatArray(3) { i ->
            slots.getOrNull(i)?.let { style(it.best, it.phrase); paint.measureText(shown(it)) + PAD } ?: 0f
        }
        val e = FloatArray(4)
        if (want.all { it <= w / 3f }) { for (i in 1..3) e[i] = w / 3f * i; return e }
        val share = FloatArray(3) { if (slots.getOrNull(it) == null) 0f else maxOf(want[it], w / 6f) }
        val k = w / share.sum()
        for (i in 0 until 3) e[i + 1] = e[i] + share[i] * k
        return e
    }

    private fun highlight(canvas: Canvas, c: KeyboardColors, left: Float, right: Float) {
        fill.color = ColorMath.withAlpha(c.text, 0x22)
        canvas.drawRect(left, 0f, right, height.toFloat(), fill)
    }

    private fun drawText(canvas: Canvas, text: String, cx: Float, cy: Float, maxW: Float, color: Int, bold: Boolean, italic: Boolean) {
        paint.color = color
        style(bold, italic)
        // A text that is too wide is made smaller; only below a size that still reads is it cut.
        val full = paint.measureText(text)
        if (full > maxW) paint.textSize = maxOf(paint.textSize * maxW / full, height * 0.38f * MIN_SCALE)
        val shown = TextUtils.ellipsize(text, paint, maxW, TextUtils.TruncateAt.END).toString()
        paint.getFontMetrics(metrics)
        canvas.drawText(shown, cx, cy - (metrics.ascent + metrics.descent) / 2f, paint)
        paint.textSkewX = 0f
    }

    private fun indexAt(x: Float): Int {
        val bw = buttonWidth()
        if (x < bw) return LEFT
        if (x > width - bw) return QUICK
        val w = width - 2 * bw
        if (scrolling()) return ((x - bw + scroll) / rowCell(w)).toInt().coerceIn(0, slots.size - 1)
        if (toolsOpen && status == null) return ((x - bw) / (w / tools.size)).toInt().coerceIn(0, tools.size - 1)
        val e = edges(w)
        return (0..2).firstOrNull { x - bw < e[it + 1] } ?: 2
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { pressedIndex = indexAt(e.x); downX = e.x; scrollAtDown = scroll; dragging = false; invalidate() }
            MotionEvent.ACTION_MOVE -> {
                if (scrolling() && (dragging || (pressedIndex >= 0 && kotlin.math.abs(e.x - downX) > slop))) {
                    dragging = true; pressedIndex = NONE
                    scroll = (scrollAtDown - (e.x - downX)).coerceIn(0f, maxScroll(width - 2 * buttonWidth()))
                    invalidate()
                } else if (indexAt(e.x) != pressedIndex) { pressedIndex = NONE; invalidate() }
            }
            MotionEvent.ACTION_UP -> {
                val i = indexAt(e.x)
                if (!dragging && i == pressedIndex) when {
                    i == LEFT -> toggleTools()
                    i == QUICK -> listener?.onTool(quick)
                    status != null -> {}
                    toolsOpen -> listener?.onTool(tools[i])
                    else -> slots.getOrNull(i)?.let { listener?.onSlot(it) }
                }
                pressedIndex = NONE; invalidate()
            }
            MotionEvent.ACTION_CANCEL -> { pressedIndex = NONE; invalidate() }
        }
        return true
    }

    // --- accessibility ------------------------------------------------------------------------------------------------
    // Everything on the bar is drawn, so TalkBack gets the left button, the quick button and each cell as virtual views.

    private fun toolName(t: Tool) = context.getString(when (t) {
        Tool.CLIPBOARD -> R.string.a11y_tool_clipboard; Tool.EMOJI -> R.string.a11y_tool_emoji; Tool.MIC -> R.string.a11y_tool_mic
        Tool.HELP -> R.string.a11y_tool_help; Tool.SETTINGS -> R.string.a11y_tool_settings
    })

    /** The virtual views, left to right: id, description and the horizontal span. A cell is only offered where something is drawn. */
    private class Part(val id: Int, val description: String, val left: Float, val right: Float)

    private fun parts(): List<Part> {
        val bw = buttonWidth(); val w = width - 2 * bw
        val out = ArrayList<Part>()
        out += Part(LEFT, context.getString(if (toolsOpen) R.string.a11y_tools_close else R.string.a11y_tools), 0f, bw)
        val text = status
        if (text != null) out += Part(STATUS, text, bw, bw + w)
        else if (toolsOpen) { val cell = w / tools.size; tools.forEachIndexed { i, t -> out += Part(i, toolName(t), bw + cell * i, bw + cell * (i + 1)) } }
        else if (scrolling()) { val cell = rowCell(w); slots.forEachIndexed { i, s -> if (s != null) out += Part(i, slotName(s), bw + i * cell - scroll, bw + (i + 1) * cell - scroll) } }
        else { val e = edges(w); for (i in 0 until 3) slots.getOrNull(i)?.let { out += Part(i, slotName(it), bw + e[i], bw + e[i + 1]) } }
        out += Part(QUICK, toolName(quick), width - bw, width.toFloat())
        return out
    }

    private fun slotName(s: Slot) = context.getString(if (s.best) R.string.a11y_suggestion_best else R.string.a11y_suggestion, shown(s))

    private val a11y = object : androidx.customview.widget.ExploreByTouchHelper(this) {
        override fun getVirtualViewAt(x: Float, y: Float): Int = parts().firstOrNull { x >= it.left && x < it.right }?.id ?: INVALID_ID
        override fun getVisibleVirtualViews(ids: MutableList<Int>) { for (p in parts()) ids += p.id }
        override fun onPopulateNodeForVirtualView(id: Int, node: androidx.core.view.accessibility.AccessibilityNodeInfoCompat) {
            val p = parts().firstOrNull { it.id == id } ?: run { node.contentDescription = ""; node.setBoundsInParent(android.graphics.Rect(0, 0, 1, 1)); return }
            node.contentDescription = if (id == QUICK && badge) p.description + ", new" else p.description
            node.setBoundsInParent(android.graphics.Rect(p.left.toInt().coerceAtLeast(0), 0, p.right.toInt().coerceAtMost(width), height))
            if (id != STATUS) { node.addAction(androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK); node.isClickable = true }
        }
        override fun onPerformActionForVirtualView(id: Int, action: Int, arguments: android.os.Bundle?): Boolean {
            if (action != androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_CLICK) return false
            when {
                id == LEFT -> toggleTools()
                id == QUICK -> listener?.onTool(quick)
                id == STATUS -> return false
                toolsOpen -> tools.getOrNull(id)?.let { listener?.onTool(it) }
                else -> slots.getOrNull(id)?.let { listener?.onSlot(it) }
            }
            return true
        }
    }

    init { androidx.core.view.ViewCompat.setAccessibilityDelegate(this, a11y) }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean = a11y.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)

    private val PAD get() = dp(12f)

    private companion object { const val NONE = -9; const val LEFT = -4; const val QUICK = -2; const val STATUS = -3; const val MIN_SCALE = 0.55f }
}
