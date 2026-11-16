package com.opencell.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.opencell.app.agent.PendingApproval
import com.opencell.app.agent.StepUi
import com.opencell.app.agent.StepUiStatus

@Composable
fun VoiceWaveform(active: Boolean, modifier: Modifier = Modifier, bars: Int = 24) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "wave")
    Row(
        modifier.height(36.dp).semantics { contentDescription = if (active) "Ouvindo" else "Microfone parado" },
        horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(bars) { i ->
            val level by transition.animateFloat(
                initialValue = 0.2f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(420 + (i * 37) % 300), RepeatMode.Reverse), label = "bar$i",
            )
            val h = if (active && !reduce) level else 0.2f
            Box(Modifier.width(4.dp).height((34 * h).dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = if (active) 1f else 0.35f)))
        }
    }
}

@Composable
fun stepIcon(status: StepUiStatus): Pair<ImageVector, Color> = when (status) {
    StepUiStatus.PENDING -> Icons.Filled.RadioButtonUnchecked to MaterialTheme.colorScheme.outline
    StepUiStatus.RUNNING -> Icons.Filled.RadioButtonUnchecked to MaterialTheme.colorScheme.primary
    StepUiStatus.VERIFIED -> Icons.Filled.CheckCircle to OcColors.Success
    StepUiStatus.UNVERIFIED -> Icons.Filled.HelpOutline to OcColors.Warning
    StepUiStatus.FAILED -> Icons.Filled.Error to MaterialTheme.colorScheme.error
    StepUiStatus.BLOCKED -> Icons.Filled.Block to MaterialTheme.colorScheme.error
    StepUiStatus.SKIPPED -> Icons.Filled.SkipNext to MaterialTheme.colorScheme.outline
}

@Composable
fun AgentActivityTimeline(steps: List<StepUi>, modifier: Modifier = Modifier, showDetails: Boolean = true) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEach { step ->
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step.status == StepUiStatus.RUNNING) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    val (icon, tint) = stepIcon(step.status)
                    Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(step.description, style = MaterialTheme.typography.bodyMedium)
                    if (showDetails && step.detail.isNotBlank() && step.status != StepUiStatus.PENDING) {
                        Text(step.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskProgressCard(goal: String, steps: List<StepUi>, modifier: Modifier = Modifier) {
    val done = steps.count { it.status != StepUiStatus.PENDING && it.status != StepUiStatus.RUNNING }
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(goal, style = MaterialTheme.typography.titleMedium)
            if (steps.isNotEmpty()) {
                LinearProgressIndicator(progress = { done.toFloat() / steps.size }, modifier = Modifier.fillMaxWidth())
                Text("$done de ${steps.size} passos processados", style = MaterialTheme.typography.labelSmall)
                AgentActivityTimeline(steps)
            }
        }
    }
}

@Composable
fun ApprovalSheet(approval: PendingApproval, onApprove: () -> Unit, onDeny: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(MaterialTheme.colorScheme.surfaceVariant, label = "approval")
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Preciso da sua aprovação", style = MaterialTheme.typography.titleMedium)
        Text(approval.reason, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Confirmar") }
            OutlinedButton(onClick = onDeny, modifier = Modifier.weight(1f)) { Text("Recusar") }
        }
    }
}

data class PermissionInfo(
    val title: String,
    val granted: Boolean?,
    val statusLabel: String,
    val why: String,
    val allows: String,
    val denies: String,
    val whenUsed: String,
    val revoke: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

@Composable
fun PermissionExplainer(info: PermissionInfo, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(info.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                val color = when (info.granted) { true -> OcColors.Success; false -> OcColors.Warning; null -> MaterialTheme.colorScheme.outline }
                Text(info.statusLabel, color = color, style = MaterialTheme.typography.labelLarge)
            }
            Labeled("Por que precisa", info.why)
            Labeled("O que permite", info.allows)
            Labeled("O que não permite", info.denies)
            Labeled("Quando é usado", info.whenUsed)
            Labeled("Como revogar", info.revoke)
            if (info.actionLabel != null && info.onAction != null) {
                Button(onClick = info.onAction) { Text(info.actionLabel) }
            }
        }
    }
}

@Composable
private fun Labeled(label: String, text: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ProviderCard(name: String, model: String, status: String, ok: Boolean?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(if (model.isBlank()) "Nenhum modelo definido" else model, style = MaterialTheme.typography.bodyMedium)
            Text(status, style = MaterialTheme.typography.labelLarge,
                color = when (ok) { true -> OcColors.Success; false -> OcColors.Warning; null -> MaterialTheme.colorScheme.onSurfaceVariant })
        }
    }
}
