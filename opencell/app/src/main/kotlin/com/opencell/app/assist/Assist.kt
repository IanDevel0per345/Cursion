package com.opencell.app.assist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import com.opencell.app.OpenCellApp
import com.opencell.app.ui.PanelActivity

class OpenCellVoiceInteractionService : VoiceInteractionService()

class OpenCellSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = OpenCellSession(this)
}

class OpenCellSession(context: Context) : VoiceInteractionSession(context) {
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        val container = (context.applicationContext as OpenCellApp).container
        if (container.panelLauncher.overlay != null) {
            container.panelLauncher.show()
            hide()
        } else {
            startAssistantActivity(Intent(context, PanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

/** Exigido pelo framework para serviços de interação por voz; OpenCell não implementa um motor de reconhecimento próprio. */
class OpenCellRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback) {
        listener.error(SpeechRecognizer.ERROR_CLIENT)
    }
    override fun onCancel(listener: Callback) = Unit
    override fun onStopListening(listener: Callback) = Unit
}
