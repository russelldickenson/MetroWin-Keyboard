package com.metrowin.keyboard

import com.metrowin.keyboard.engine.Text
import com.metrowin.keyboard.engine.WordDictionary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.text.Normalizer
import java.util.Locale

/** The faster versions of fold and top give exactly what the plain ones did. */
class FastPathsTest {
    private val marks = Regex("\\p{M}+")
    private fun reference(s: String) = Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(marks, "")

    @Test fun foldMatchesTheOriginalOnManyKindsOfText() {
        val samples = listOf(
            "", "a", "Hello", "WORLD", "perché", "Perché", "ÀÈÌÒÙ", "straße", "Ørsted", "İstanbul", "ΑΒΓ άέή", "Привет", "Українська",
            "العَرَبِيَّة", "עִבְרִית", "naïve café", "áb̀", "ǅ", "ﬁ", "Dž", "x⃝", "cos'è", "l’amico", "123", "tab\tsp ace",
        )
        for (s in samples) assertEquals("fold of \"$s\"", reference(s), Text.fold(s))
        // Every character of the Basic Multilingual Plane, alone and after a letter.
        for (c in 0..0xFFFF) {
            if (c in 0xD800..0xDFFF) continue
            val ch = c.toChar().toString()
            assertEquals("U+%04X".format(c), reference(ch), Text.fold(ch))
            assertEquals("a + U+%04X".format(c), reference("a$ch"), Text.fold("a$ch"))
        }
    }

    @Test fun topMatchesAPassOverTheWholeList() {
        val dict = File("src/main/assets/dictionaries/en.txt").bufferedReader().use { WordDictionary.parse(it, Locale.ENGLISH) }
        fun brute(limit: Int, skip: (String) -> Boolean): List<String> =
            (0 until dict.size).filter { !skip(dict.wordAt(it)) }.map { dict.wordAt(it) to Math.log1p(dict.freqAt(it).toDouble()) }
                .let { l -> l.withIndex().sortedWith(compareBy({ -it.value.second }, { it.index })).map { it.value.first } }.take(limit)
        val none: (String) -> Boolean = { false }
        val some: (String) -> Boolean = { it.length % 2 == 0 }
        val many: (String) -> Boolean = { it.length != 3 }      // skips so many that the cached best words run out
        for (skip in listOf(none, some, many)) for (limit in listOf(1, 6, 12, 64, 100)) {
            assertEquals("limit $limit", brute(limit, skip), dict.top(limit, skip).map { it.word })
        }
    }
}
