package com.metrowin.keyboard.engine

import org.json.JSONArray
import org.json.JSONObject

/** A shortcut and the text it turns into: "wyd" and "What do you need from me?". */
data class Snippet(val key: String, val text: String)

/**
 * The user's snippets: kept as one small piece of JSON in the settings (so the backup carries them), checked when they are made and
 * when they are read. A shortcut is a word (letters only, no spaces or digits), because it is matched against the word being typed.
 */
object Snippets {
    const val MAX_SNIPPETS = 200
    const val MAX_KEY = 24
    const val MAX_TEXT = 1000

    fun validKey(key: String) = key.length in 1..MAX_KEY && key.all { Text.isWordChar(it) }

    fun validText(text: String) = text.isNotBlank() && text.length <= MAX_TEXT

    fun encode(list: List<Snippet>): String =
        JSONArray().also { a -> for (s in list) a.put(JSONObject().put("k", s.key).put("t", s.text)) }.toString()

    /** Reads what [encode] wrote; anything that is not a valid snippet is left out, and a damaged text gives an empty list. */
    fun parse(json: String): List<Snippet> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val a = JSONArray(json)
            val out = ArrayList<Snippet>()
            val seen = HashSet<String>()
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val s = Snippet(o.optString("k"), o.optString("t"))
                if (validKey(s.key) && validText(s.text) && seen.add(s.key.lowercase()) && out.size < MAX_SNIPPETS) out += s
            }
            out
        }.getOrDefault(emptyList())
    }

    /** Shortcut (lower case) to text, for the lookup while typing. */
    fun lookup(list: List<Snippet>): Map<String, String> = list.associate { it.key.lowercase() to it.text }
}
