package com.opencell.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.opencell.app.AppContainer
import com.opencell.app.agent.AgentUiState
import com.opencell.app.agent.PanelState
import com.opencell.app.voice.VoiceInput

fun PanelState.label(): String = when (this) {
    PanelState.Idle -> "Pronto. Diga ou digite uma tarefa."
    PanelState.Listening -> "Ouvindo…"
    PanelState.Transcribing -> "Transcrevendo…"
    PanelState.Thinking -> "Entendendo o pedido…"
    PanelState.Planning -> "Planejando…"
    PanelState.Executing -> "Executando"
    PanelState.WaitingForApproval -> "Aguardando sua aprovação"
    PanelState.Paused -> "Pausado"
    PanelState.Success -> "Concluído e verificado"
    PanelState.PartialSuccess -> "Concluído parcialmente"
    PanelState.Error -> "Não foi possível concluir"
}

@Composable
fun PanelHost(container: AppContainer, onDismiss: () -> Unit, onRequestMic: () -> Unit) {
    val ui by container.runtime.ui.collectAsState()
    val settings by container.settings.collectAsState()
    val context = LocalContext.current
    val voice = remember { VoiceInput(context) }
    var micError by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) { onDispose { voice.stop() } }

    OpenCellBottomPanel(
        state = ui,
        micError = micError,
        onSubmit = { micError = null; container.runtime.submit(it) },
        onMic = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                micError = "Conceda o microfone no aplicativo OpenCell."
                onRequestMic()
            } else {
                micError = null
                voice.start(
                    settings.language,
                    onPartial = { container.runtime.setListening(it) },
                    onFinal = { container.runtime.setListening(null); container.runtime.submit(it) },
                    onError = { container.runtime.setListening(null); micError = it },
                )
            }
        },
        onApprove = { container.runtime.approve(true) },
        onDeny = { container.runtime.approve(false) },
        onPause = { container.runtime.pause() },
        onResume = { container.runtime.resume() },
        onStop = { container.runtime.stop("Tarefa interrompida pelo botão Parar OpenCell.") },
        onClose = { voice.stop(); container.runtime.setListening(null); container.runtime.dismissResult(); onDismiss() },
    )
}

@Composable
fun OpenCellBottomPanel(
    state: AgentUiState,
    micError: String?,
    onSubmit: (String) -> Unit,
    onMic: () -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val haptic = LocalHapticFeedback.current
    val hapticsOn = LocalHaptics.current
    var shown by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { shown = true }
    LaunchedEffect(state.panel) {
        if (hapticsOn && (state.panel == PanelState.WaitingForApproval || state.panel == PanelState.Success || state.panel == PanelState.Error)) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val dur = if (reduce) 0 else 320

    AnimatedVisibility(
        visible = shown,
        enter = slideInVertically(tween(dur)) { it } + fadeIn(tween(dur)),
        exit = slideOutVertically(tween(dur)) { it } + fadeOut(tween(dur)),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp, shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("OpenCell", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(state.panel.label(), style = MaterialTheme.typography.titleMedium)
                    }
                    if (state.steps.isNotEmpty()) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess, if (expanded) "Recolher" else "Expandir")
                        }
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Fechar painel") }
                }

                if (state.panel == PanelState.Listening || state.panel == PanelState.Transcribing) {
                    VoiceWaveform(active = true, modifier = Modifier.align(Alignment.CenterHorizontally))
                    if (state.partialTranscript.isNotBlank()) Text(state.partialTranscript, style = MaterialTheme.typography.bodyLarge)
                }

                if (state.goal.isNotBlank()) Text("“${state.goal}”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (state.steps.isNotEmpty()) {
                    val current = state.steps.firstOrNull { it.status == com.opencell.app.agent.StepUiStatus.RUNNING }
                    if (expanded) {
                        Box(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) { AgentActivityTimeline(state.steps) }
                    } else if (current != null) {
                        Text("Passo atual: ${current.description}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                state.approval?.let { ApprovalSheet(it, onApprove, onDeny) }

                state.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { contentDescription = "Resultado: $it" }) }
                micError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }

                if (state.taskActive) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (state.panel == PanelState.Paused) {
                            Button(onClick = onResume, Modifier.weight(1f)) { Icon(Icons.Filled.PlayArrow, null); Text(" Continuar") }
                        } else {
                            OutlinedButton(onClick = onPause, Modifier.weight(1f)) { Icon(Icons.Filled.Pause, null); Text(" Pausar") }
                        }
                        Button(
                            onClick = onStop, Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                        ) { Icon(Icons.Filled.Stop, null); Text(" Parar OpenCell") }
                    }
                    OutlinedTextField(
                        value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        placeholder = { Text("Corrigir ou ajustar a tarefa…") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSubmit(input); input = "" }),
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), singleLine = true,
                            placeholder = { Text("O que você quer que eu faça?") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { if (input.isNotBlank()) { onSubmit(input); input = "" } }),
                        )
                        IconButton(onClick = onMic) { Icon(Icons.Filled.Mic, "Falar") }
                        IconButton(onClick = { if (input.isNotBlank()) { onSubmit(input); input = "" } }, enabled = input.isNotBlank()) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Enviar tarefa")
                        }
                    }
                }
            }
        }
    }
}
