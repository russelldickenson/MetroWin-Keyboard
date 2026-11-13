package com.metrowin.keyboard.theme

/** The accent the Classic look has when nothing else is chosen: the cyan of Windows Phone. Anything else comes from the theme, the wallpaper or the one custom colour. */
object Accents {
    val DEFAULT: Int = 0xFF1BA1E2.toInt()

    /** The colours the settings offer as swatches (the Windows Phone ones that read well on every look), in the order of the names in R.array.accent_names. */
    val PRESETS: List<Int> = listOf(0xFF1BA1E2, 0xFF0050EF, 0xFF60A917, 0xFFFA6800, 0xFFE51400, 0xFFD80073).map { it.toInt() }
}
