package com.opencell.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Speaker(private val context: Context) {
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false

    fun speak(text: String, languageTag: String) {
        val engine = tts ?: TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
        }.also { tts = it }
        if (!ready) return
        engine.language = Locale.forLanguageTag(languageTag)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "opencell")
    }

    fun stop() { tts?.stop() }

    fun shutdown() { tts?.shutdown(); tts = null; ready = false }
}
