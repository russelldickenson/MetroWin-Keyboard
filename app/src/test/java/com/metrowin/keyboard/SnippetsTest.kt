package com.metrowin.keyboard

import com.metrowin.keyboard.engine.Snippet
import com.metrowin.keyboard.engine.Snippets
import org.junit.Assert.*
import org.junit.Test

class SnippetsTest {
    @Test fun aShortcutIsOneWordOfLetters() {
        assertTrue(Snippets.validKey("wyd")); assertTrue(Snippets.validKey("Perché")); assertTrue(Snippets.validKey("привет"))
        assertFalse(Snippets.validKey("")); assertFalse(Snippets.validKey("two words")); assertFalse(Snippets.validKey("a1")); assertFalse(Snippets.validKey("x".repeat(Snippets.MAX_KEY + 1)))
        assertFalse(Snippets.validKey("it's")); assertFalse(Snippets.validKey("a.b"))
    }

    @Test fun theTextMustBeThereAndNotTooLong() {
        assertTrue(Snippets.validText("What do you need from me?")); assertTrue(Snippets.validText("line one\nline two"))
        assertFalse(Snippets.validText("")); assertFalse(Snippets.validText("   ")); assertFalse(Snippets.validText("x".repeat(Snippets.MAX_TEXT + 1)))
    }

    @Test fun whatIsSavedComesBack() {
        val list = listOf(Snippet("wyd", "What do you need from me?"), Snippet("brb", "Be right back — \"soon\"\nthanks"))
        assertEquals(list, Snippets.parse(Snippets.encode(list)))
    }

    @Test fun damagedOrInvalidDataGivesTheValidPartOrNothing() {
        assertEquals(emptyList<Snippet>(), Snippets.parse(""))
        assertEquals(emptyList<Snippet>(), Snippets.parse("not json"))
        val json = """[{"k":"ok","t":"fine"},{"k":"two words","t":"bad key"},{"k":"empty","t":""},{"k":"OK","t":"same shortcut"},5,{"k":"fine","t":"also fine"}]"""
        assertEquals(listOf(Snippet("ok", "fine"), Snippet("fine", "also fine")), Snippets.parse(json))
    }

    @Test fun lookupIgnoresCase() {
        assertEquals("Hi there", Snippets.lookup(listOf(Snippet("HeLLo", "Hi there")))["hello"])
    }
}
