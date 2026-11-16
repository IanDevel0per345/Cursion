package com.opencell.core.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class TaskController {
    private val pausedFlow = MutableStateFlow(false)
    @Volatile var cancelled = false
        private set
    @Volatile private var correction: String? = null

    val paused get() = pausedFlow.value

    fun pause() { pausedFlow.value = true }
    fun resume() { pausedFlow.value = false }
    fun cancel() { cancelled = true; pausedFlow.value = false }
    fun correct(instruction: String) { correction = instruction }
    fun takeCorrection(): String? = correction.also { correction = null }
    suspend fun awaitResumed() { pausedFlow.first { !it } }
}
