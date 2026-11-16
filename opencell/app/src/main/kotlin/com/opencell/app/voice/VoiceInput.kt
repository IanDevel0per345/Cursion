package com.opencell.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceInput(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    val available get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(languageTag: String, onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (String) -> Unit) {
        if (!available) { onError("Reconhecimento de voz indisponível neste aparelho."); return }
        stop()
        val r = SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onPartialResults(partialResults: Bundle) = onPartial(first(partialResults).orEmpty())
            override fun onResults(results: Bundle) { first(results)?.let(onFinal) ?: onError("Nada foi entendido."); stop() }
            override fun onError(error: Int) { onError(describe(error)); stop() }
            override fun onReadyForSpeech(params: Bundle?) = onPartial("")
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true),
        )
    }

    fun stop() {
        recognizer?.run { runCatching { cancel() }; destroy() }
        recognizer = null
    }

    private fun first(b: Bundle) = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun describe(code: Int) = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Não ouvi nada. Tente novamente."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone não concedida."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Reconhecimento de voz sem conexão."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "O reconhecedor está ocupado."
        else -> "Falha no reconhecimento de voz (código $code)."
    }
}
