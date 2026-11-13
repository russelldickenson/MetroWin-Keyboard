package com.metrowin.keyboard

import android.speech.SpeechRecognizer
import com.metrowin.keyboard.voice.VoiceInput
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A voice failure is told in words with what to do next, never as a number. */
@RunWith(RobolectricTestRunner::class)
class VoiceErrorTest {
    @Test fun everyRecogniserErrorHasAMessageThatIsNotJustANumber() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<KeyboardApp>()
        for (code in 0..20) {
            val text = app.getString(VoiceInput.messageFor(code))
            assertTrue("error $code: $text", text.length > 20 && !text.contains(Regex("\\d{1,2}\\b")))
        }
    }

    @Test fun theCauseDecidesTheAdvice() {
        assertEquals(R.string.voice_error_network, VoiceInput.messageFor(SpeechRecognizer.ERROR_NETWORK))
        assertEquals(R.string.voice_error_audio, VoiceInput.messageFor(SpeechRecognizer.ERROR_AUDIO))
        assertEquals(R.string.voice_error_busy, VoiceInput.messageFor(SpeechRecognizer.ERROR_RECOGNIZER_BUSY))
        assertEquals(R.string.voice_error_language, VoiceInput.messageFor(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED))
        assertEquals(R.string.voice_permission, VoiceInput.messageFor(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
        assertEquals(R.string.voice_error_other, VoiceInput.messageFor(SpeechRecognizer.ERROR_CLIENT))
    }
}
