package com.opencell.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.opencell.app.AppContainer
import com.opencell.app.data.API_KEY_SECRET
import com.opencell.app.data.AppSettings
import com.opencell.app.data.TaskEntity
import com.opencell.app.data.ThemeMode
import com.opencell.app.voice.WakeWordService
import com.opencell.core.policy.AutonomyMode
import com.opencell.core.provider.ConnectionTestResult
import com.opencell.core.provider.ConnectionTester
import com.opencell.core.provider.OkHttpTransport
import com.opencell.core.provider.ProviderConfig
import com.opencell.core.provider.ProviderException
import com.opencell.core.provider.ProviderFactory
import com.opencell.core.provider.ProviderKind
import com.opencell.core.security.Redactor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

const val GUIDED_TEST_GOAL = "abra o aplicativo de teste, role para baixo"

@Composable
fun HomeScreen(c: AppContainer, nav: (String) -> Unit) {
    val ctx = LocalContext.current
    val s by c.settings.collectAsState()
    val ui by c.runtime.ui.collectAsState()
    val recent by c.db.tasks().recent(5).collectAsState(emptyList())
    rememberResumeTick()
    val connected = SystemChecks.accessibilityConnected()
    val status = when {
        ui.taskActive -> "Executando uma tarefa"
        !connected -> "Aguardando permissão de acessibilidade"
        else -> "Pronto"
    }
    ScreenScaffold("OpenCell", null) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Your phone. Your AI. Your control.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(status, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (s.model.isBlank()) "IA: comandos livres exigem um provedor configurado. Comandos simples funcionam localmente."
                        else "IA: ${s.providerKind.name} · ${s.model}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(onClick = { c.panelLauncher.show() }, Modifier.fillMaxWidth()) { Text("Conversar com OpenCell") }
                    if (ui.taskActive) OutlinedButton(onClick = { c.runtime.stop("Tarefa interrompida pelo botão Parar OpenCell.") }, Modifier.fillMaxWidth()) { Text("Parar OpenCell") }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Autorizações", style = MaterialTheme.typography.titleMedium)
                    Text("Acessibilidade: " + if (connected) "ativa" else if (SystemChecks.accessibilityEnabled(ctx)) "ativada, aguardando conexão" else "desativada")
                    Text("Microfone: " + if (SystemChecks.micGranted(ctx)) "concedido" else "não concedido")
                    Text("Notificações: " + if (SystemChecks.notificationsGranted(ctx)) "concedidas" else "não concedidas")
                    Text("Assistente padrão: " + if (SystemChecks.assistantRoleHeld(ctx)) "OpenCell" else "não definido")
                    Text("Autonomia: ${autonomyLabel(s.autonomy)}")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { c.runtime.submit(GUIDED_TEST_GOAL); c.panelLauncher.show() }, label = { Text("Teste guiado") })
                AssistChip(onClick = { nav("permissions") }, label = { Text("Permissões") })
                AssistChip(onClick = { nav("diagnostics") }, label = { Text("Diagnóstico") })
            }
        }
        item { Text("Tarefas recentes", style = MaterialTheme.typography.titleMedium) }
        if (recent.isEmpty()) item { Text("Nenhuma tarefa executada ainda.", style = MaterialTheme.typography.bodyMedium) }
        items(recent, key = { it.id }) { TaskRow(it) { nav("history/${it.id}") } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { nav("history") }) { Text("Histórico") }
                TextButton(onClick = { nav("providers") }) { Text("Provedor de IA") }
                TextButton(onClick = { nav("settings") }) { Text("Configurações") }
            }
        }
    }
}

fun autonomyLabel(m: AutonomyMode) = when (m) {
    AutonomyMode.OBSERVER -> "Observador (não executa toques)"
    AutonomyMode.ASSISTED -> "Assistido (pede aprovação a cada ação)"
    AutonomyMode.SUPERVISED -> "Supervisionada (confirma ações externas/irreversíveis)"
    AutonomyMode.BOUNDED_ROUTINE -> "Rotinas delimitadas"
    AutonomyMode.PERMISSIVE -> "100% permissivo (faz tudo sem pedir autorização)"
}

@Composable
private fun TaskRow(t: TaskEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(t.goal, style = MaterialTheme.typography.titleMedium)
            Text("${t.outcome ?: "em andamento"} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(t.startedAtMs))}", style = MaterialTheme.typography.labelSmall)
            t.summary?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
fun HistoryScreen(c: AppContainer, taskId: String?, nav: (String) -> Unit, back: () -> Unit) {
    val scope = rememberCoroutineScope()
    if (taskId == null) {
        val tasks by c.db.tasks().recent(200).collectAsState(emptyList())
        ScreenScaffold("Histórico", back) {
            if (tasks.isEmpty()) item { Text("O histórico está vazio.") }
            items(tasks, key = { it.id }) { TaskRow(it) { nav("history/${it.id}") } }
        }
    } else {
        val events by c.db.tasks().events(taskId).collectAsState(emptyList())
        val tasks by c.db.tasks().recent(200).collectAsState(emptyList())
        val task = tasks.firstOrNull { it.id == taskId }
        ScreenScaffold("Tarefa", back) {
            if (task != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(task.goal, style = MaterialTheme.typography.titleLarge)
                        Text("Resultado: ${task.outcome ?: "em andamento"}")
                        task.appPackage?.let { Text("Aplicativo: $it") }
                        task.provider?.let { Text("Provedor: $it · tokens ${task.inputTokens}/${task.outputTokens} (entrada/saída)") }
                        task.summary?.let { Text(it) }
                    }
                }
            }
            items(events, key = { it.rowId }) { e ->
                Column {
                    Text("${e.title}", style = MaterialTheme.typography.bodyLarge)
                    if (e.detail.isNotBlank()) Text(e.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateFormat.getTimeInstance().format(Date(e.atMs)) + " · " + e.kind, style = MaterialTheme.typography.labelSmall)
                }
            }
            item {
                OutlinedButton(onClick = { scope.launch(Dispatchers.IO) { c.db.tasks().deleteEvents(taskId); c.db.tasks().deleteTask(taskId) }; back() }) { Text("Excluir este registro") }
            }
        }
    }
}

@Composable
fun PermissionsScreen(c: AppContainer, back: () -> Unit) {
    val ctx = LocalContext.current
    rememberResumeTick()
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val connected = SystemChecks.accessibilityConnected()
    val enabled = SystemChecks.accessibilityEnabled(ctx)
    val items = listOf(
        PermissionInfo(
            "Serviço de acessibilidade", connected, if (connected) "Ativo" else if (enabled) "Ativado, sem conexão" else "Desativado",
            "É a única forma suportada de o OpenCell ler elementos da tela e tocar, rolar e digitar em outros aplicativos.",
            "Ler textos e botões visíveis do aplicativo em primeiro plano; tocar, rolar e inserir texto durante uma tarefa sua.",
            "Não ignora telas protegidas, não digita senhas, não resolve CAPTCHAs e não age sem uma tarefa iniciada por você.",
            "Somente enquanto uma tarefa está em execução ou quando você pede para observar a tela.",
            "Configurações › Acessibilidade › OpenCell Agente › Desativar. As tarefas em andamento são encerradas na hora.",
            "Abrir configurações de acessibilidade", { SystemChecks.openAccessibilitySettings(ctx) },
        ),
        PermissionInfo(
            "Microfone", SystemChecks.micGranted(ctx), if (SystemChecks.micGranted(ctx)) "Concedido" else "Não concedido",
            "Para ditar comandos e, se você ativar, ouvir a palavra de ativação.",
            "Capturar sua voz para transcrição pelo reconhecedor do sistema.",
            "Não grava continuamente; sem a opção de palavra de ativação o microfone só liga ao tocar no botão.",
            "Ao tocar no microfone do painel; contínuo apenas com a palavra de ativação (experimental) ligada.",
            "Configurações do Android › Apps › OpenCell › Permissões › Microfone.",
            "Conceder microfone", { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        ),
        PermissionInfo(
            "Notificações", SystemChecks.notificationsGranted(ctx), if (SystemChecks.notificationsGranted(ctx)) "Concedidas" else "Não concedidas",
            "Para mostrar o controle \"Parar OpenCell\" e o aviso de conclusão quando o painel não está visível.",
            "Exibir notificações de tarefa em andamento e concluída.",
            "Não envia conteúdo da tela nem textos privados nas notificações.",
            "Durante e após uma tarefa.",
            "Configurações do Android › Apps › OpenCell › Notificações.",
            "Conceder notificações", { if (Build.VERSION.SDK_INT >= 33) micLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else SystemChecks.openAppSettings(ctx) },
        ),
        PermissionInfo(
            "Assistente padrão", if (SystemChecks.assistantRoleAvailable(ctx)) SystemChecks.assistantRoleHeld(ctx) else null,
            if (!SystemChecks.assistantRoleAvailable(ctx)) "Indisponível neste aparelho" else if (SystemChecks.assistantRoleHeld(ctx)) "Definido" else "Não definido",
            "Permite abrir o painel pelo gesto/botão de assistente do sistema, quando o fabricante liberar para apps de terceiros.",
            "Receber o gatilho de assistente do Android para abrir o painel.",
            "Não concede escuta permanente nem privilégios de sistema; isso é exclusivo de assistentes do fabricante.",
            "Quando você aciona o gesto de assistente.",
            "Configurações › Apps › Apps padrão › App assistente digital.",
            "Abrir apps padrão", { SystemChecks.openAssistantSettings(ctx) },
        ),
        PermissionInfo(
            "Captura de tela (visão)", null, "Não implementado nesta versão",
            "Seria usada para enviar capturas a um modelo multimodal quando a árvore de acessibilidade não basta.",
            "—", "A percepção atual usa apenas a árvore de acessibilidade; nenhuma imagem é capturada ou enviada.",
            "Nunca nesta versão.", "Não há o que revogar.",
        ),
    )
    ScreenScaffold("Central de permissões", back) {
        items(items) { PermissionExplainer(it) }
        item {
            Text("Em instalações fora da Play Store, o Android 13+ pode exigir liberar \"Configurações restritas\" em Info do app › ⋮ antes de ativar a acessibilidade.", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun DiagnosticsScreen(c: AppContainer, back: () -> Unit) {
    val ctx = LocalContext.current
    rememberResumeTick()
    val ui by c.runtime.ui.collectAsState()
    val s by c.settings.collectAsState()
    val rows = listOf(
        "Aparelho" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "Serviço de acessibilidade conectado" to yesNo(SystemChecks.accessibilityConnected()),
        "Gestos por toque (dispatchGesture)" to if (SystemChecks.accessibilityConnected()) "disponível (declarado no serviço)" else "depende do serviço de acessibilidade",
        "Reconhecimento de voz do sistema" to yesNo(android.speech.SpeechRecognizer.isRecognitionAvailable(ctx)),
        "Reconhecimento no aparelho (offline)" to if (Build.VERSION.SDK_INT >= 31) yesNo(android.speech.SpeechRecognizer.isOnDeviceRecognitionAvailable(ctx)) else "não verificável (API < 31)",
        "Papel de assistente disponível" to yesNo(SystemChecks.assistantRoleAvailable(ctx)),
        "Palavra de ativação" to "experimental; exige microfone e serviço em primeiro plano; sem garantia com tela desligada",
        "Rede" to yesNo(SystemChecks.online(ctx)),
        "Provedor configurado" to yesNo(s.model.isNotBlank() && !c.secrets.get(API_KEY_SECRET).isNullOrBlank()),
        "Captura de tela / visão" to "não implementada nesta versão",
        "Estado do agente" to ui.agentState.name,
    )
    ScreenScaffold("Diagnóstico de compatibilidade", back) {
        items(rows) { (k, v) ->
            Column { Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Text(v) }
        }
        item {
            Text("Este diagnóstico lista o que o aparelho declara. Compatibilidade real só é confirmada executando o teste guiado.", style = MaterialTheme.typography.labelSmall)
            Button(onClick = { c.runtime.submit(GUIDED_TEST_GOAL); c.panelLauncher.show() }) { Text("Executar teste guiado") }
        }
    }
}

private fun yesNo(b: Boolean) = if (b) "sim" else "não"

@Composable
fun ProvidersScreen(c: AppContainer, back: () -> Unit) {
    val s by c.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var kind by remember(s.providerKind) { mutableStateOf(s.providerKind) }
    var baseUrl by remember(s.baseUrl) { mutableStateOf(s.baseUrl) }
    var model by remember(s.model) { mutableStateOf(s.model) }
    var fallback by remember(s.fallbackModel) { mutableStateOf(s.fallbackModel) }
    var key by remember { mutableStateOf("") }
    var models by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<ConnectionTestResult>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val hasKey = !c.secrets.get(API_KEY_SECRET).isNullOrBlank()

    fun config(): ProviderConfig? {
        val k = key.ifBlank { c.secrets.get(API_KEY_SECRET).orEmpty() }
        if (k.isBlank()) { error = "Informe a chave de API."; return null }
        if (model.isBlank()) { error = "Informe o identificador do modelo."; return null }
        val googleKey = k.startsWith("AIza") || k.startsWith("AQ.")
        if (googleKey && kind != ProviderKind.GEMINI) {
            error = "Esta chave parece ser do Google (Gemini), mas o provedor selecionado é outro. Selecione \"Google Gemini\"."
            return null
        }
        return ProviderConfig(kind, baseUrl.trim(), k, model.trim(), s.timeoutSec * 1000L, s.temperature.toDoubleOrNull())
    }

    ScreenScaffold("Provedor de IA", back) {
        item { ProviderCard(kind.name, model, if (hasKey) "Chave salva com criptografia (Keystore)" else "Sem chave", hasKey, {}) }
        item {
            ChoiceRow("Provedor", ProviderKind.values().map { it to providerLabel(it) }, kind) {
                kind = it; baseUrl = ProviderConfig.defaultBaseUrl(it); models = emptyList()
            }
        }
        item { OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Endpoint") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item {
            OutlinedTextField(key, { key = it }, label = { Text(if (hasKey) "Chave de API (já salva; digite para substituir)" else "Chave de API") },
                singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        }
        item { OutlinedTextField(model, { model = it }, label = { Text("Modelo principal (identificador)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(fallback, { fallback = it }, label = { Text("Modelo de fallback (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        item { NumberField("Timeout (s)", s.timeoutSec) { v -> scope.launch { c.settingsRepo.update { it.copy(timeoutSec = v) } } } }
        item { NumberField("Tentativas em erro 429/5xx", s.providerRetries) { v -> scope.launch { c.settingsRepo.update { it.copy(providerRetries = v) } } } }
        item {
            OutlinedTextField(s.temperature, { v -> scope.launch { c.settingsRepo.update { it.copy(temperature = v.filter { ch -> ch.isDigit() || ch == '.' }.take(4)) } } },
                label = { Text("Temperatura (vazio = padrão do provedor)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !busy, onClick = {
                    if (key.isNotBlank()) c.secrets.put(API_KEY_SECRET, key)
                    key = ""
                    scope.launch { c.settingsRepo.update { it.copy(providerKind = kind, baseUrl = baseUrl.trim(), model = model.trim(), fallbackModel = fallback.trim()) } }
                    error = "Configuração salva."
                }) { Text("Salvar") }
                OutlinedButton(enabled = !busy, onClick = {
                    val cfg = config() ?: return@OutlinedButton
                    busy = true; error = null
                    scope.launch {
                        models = try { withContext(Dispatchers.IO) { ProviderFactory.create(cfg, OkHttpTransport()).listModels() } }
                        catch (e: ProviderException) { error = e.message; emptyList() }
                        busy = false
                    }
                }) { Text("Listar modelos") }
            }
        }
        item {
            Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                val cfg = config() ?: return@Button
                busy = true; error = null; results = emptyList()
                scope.launch {
                    results = ConnectionTester.run(ProviderFactory.create(cfg, OkHttpTransport()))
                    busy = false
                }
            }) { Text(if (busy) "Testando…" else "Testar conexão, texto e saída estruturada") }
        }
        error?.let { e -> item { Text(Redactor.redact(e), color = MaterialTheme.colorScheme.primary) } }
        items(results) { r ->
            Column {
                Text((if (r.ok) "✔ " else "✘ ") + r.step + " · ${r.latencyMs} ms", style = MaterialTheme.typography.bodyLarge)
                Text(r.detail, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (models.isNotEmpty()) {
            item { Text("Modelos disponíveis (toque para usar)", style = MaterialTheme.typography.titleMedium) }
            items(models.take(60)) { m -> TextButton(onClick = { model = m }) { Text(m) } }
        }
        item {
            Text("Teste de visão: não disponível nesta versão (a captura de tela não está implementada).", style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun providerLabel(k: ProviderKind) = when (k) {
    ProviderKind.OPENAI_COMPATIBLE -> "OpenAI e compatíveis (/chat/completions)"
    ProviderKind.ANTHROPIC -> "Anthropic Claude (/v1/messages)"
    ProviderKind.GEMINI -> "Google Gemini (generateContent)"
}

@Composable
fun SettingsScreen(c: AppContainer, back: () -> Unit) {
    val s by c.settings.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var askPermissive by remember { mutableStateOf(false) }
    if (askPermissive) androidx.compose.material3.AlertDialog(
        onDismissRequest = { askPermissive = false },
        title = { Text("Ativar modo 100% permissivo?") },
        text = {
            Text("O OpenCell passará a enviar mensagens, publicar, excluir, comprar e pagar sem pedir sua confirmação, se a tarefa levar a isso. " +
                "Uma instrução ambígua ou um erro da IA pode causar ações que não dá para desfazer. " +
                "Continuam bloqueados: digitar em campos de senha, apps bloqueados e telas protegidas. O botão Parar OpenCell sempre funciona.")
        },
        confirmButton = { TextButton(onClick = { askPermissive = false; scope.launch { c.settingsRepo.update { it.copy(autonomy = AutonomyMode.PERMISSIVE) } } }) { Text("Ativar mesmo assim") } },
        dismissButton = { TextButton(onClick = { askPermissive = false }) { Text("Cancelar") } },
    )
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch(Dispatchers.IO) {
            val dao = c.db.tasks()
            val json = kotlinx.serialization.json.buildJsonObject {
                put("tasks", kotlinx.serialization.json.JsonArray(dao.all().map { t ->
                    kotlinx.serialization.json.buildJsonObject {
                        put("id", kotlinx.serialization.json.JsonPrimitive(t.id)); put("goal", kotlinx.serialization.json.JsonPrimitive(t.goal))
                        put("outcome", kotlinx.serialization.json.JsonPrimitive(t.outcome.orEmpty())); put("summary", kotlinx.serialization.json.JsonPrimitive(t.summary.orEmpty()))
                        put("startedAtMs", kotlinx.serialization.json.JsonPrimitive(t.startedAtMs))
                    }
                }))
                put("events", kotlinx.serialization.json.JsonArray(dao.allEvents().map { e ->
                    kotlinx.serialization.json.buildJsonObject {
                        put("taskId", kotlinx.serialization.json.JsonPrimitive(e.taskId)); put("kind", kotlinx.serialization.json.JsonPrimitive(e.kind))
                        put("title", kotlinx.serialization.json.JsonPrimitive(e.title)); put("detail", kotlinx.serialization.json.JsonPrimitive(e.detail))
                        put("atMs", kotlinx.serialization.json.JsonPrimitive(e.atMs))
                    }
                }))
            }
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toString().toByteArray()) }
            status = "Registros exportados."
        }
    }
    fun update(t: (AppSettings) -> AppSettings) { scope.launch { c.settingsRepo.update(t) } }
    fun show(vararg words: String) = query.isBlank() || words.any { it.contains(query, ignoreCase = true) }

    ScreenScaffold("Configurações", back) {
        item { OutlinedTextField(query, { query = it }, label = { Text("Buscar configuração") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (show("autonomia", "modo", "agente")) item {
            ChoiceRow("Nível de autonomia", AutonomyMode.values().map { it to autonomyLabel(it) }, s.autonomy) { v ->
                if (v == AutonomyMode.PERMISSIVE) askPermissive = true else update { it.copy(autonomy = v) }
            }
        }
        if (show("passos", "limite", "agente")) item { NumberField("Limite de passos por tarefa", s.maxSteps) { v -> update { it.copy(maxSteps = v) } } }
        if (show("timeout", "tempo", "agente")) item { NumberField("Timeout global da tarefa (s)", s.taskTimeoutSec) { v -> update { it.copy(taskTimeoutSec = v) } } }
        if (show("tentativas", "repetição", "agente")) item { NumberField("Repetições máximas por ação", s.maxRetriesPerAction) { v -> update { it.copy(maxRetriesPerAction = v) } } }
        if (show("confirmar", "externas", "agente")) item { SwitchRow("Confirmar ações externas", "Enviar, publicar, compartilhar", s.confirmExternal) { v -> update { it.copy(confirmExternal = v) } } }
        if (show("confirmar", "irreversíveis", "agente")) item { SwitchRow("Confirmar ações irreversíveis", "Excluir, pagar, comprar", s.confirmIrreversible) { v -> update { it.copy(confirmIrreversible = v) } } }
        if (show("aplicativos", "permitidos", "bloqueados")) {
            item {
                OutlinedTextField(s.allowedPackages.joinToString(","), { v -> update { it.copy(allowedPackages = v.split(',').map(String::trim).filter(String::isNotEmpty).toSet()) } },
                    label = { Text("Apps permitidos (pacotes, vírgula; vazio = todos)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(s.blockedPackages.joinToString(","), { v -> update { it.copy(blockedPackages = v.split(',').map(String::trim).filter(String::isNotEmpty).toSet()) } },
                    label = { Text("Apps bloqueados (pacotes, vírgula)") }, modifier = Modifier.fillMaxWidth())
            }
        }
        if (show("idioma", "voz", "language")) item {
            ChoiceRow("Idioma da voz", listOf("pt-BR" to "Português (Brasil)", "en-US" to "English (US)", "es-ES" to "Español"), s.language) { v -> update { it.copy(language = v) } }
        }
        if (show("falar", "resposta", "voz", "tts")) item { SwitchRow("Falar o resultado das tarefas", "Usa a síntese de voz do sistema", s.speakResponses) { v -> update { it.copy(speakResponses = v) } } }
        if (show("palavra", "ativação", "ok opencell", "voz")) item {
            SwitchRow("Palavra de ativação “Ok OpenCell” (experimental)", "Mantém o microfone ligado com notificação permanente; pode consumir bateria e não funciona com tela desligada em vários aparelhos.", s.wakeWordEnabled) { v ->
                if (v && !SystemChecks.micGranted(ctx)) { status = "Conceda o microfone na Central de permissões primeiro." }
                else {
                    update { it.copy(wakeWordEnabled = v) }
                    val intent = android.content.Intent(ctx, WakeWordService::class.java)
                    if (v) ctx.startForegroundService(intent) else ctx.stopService(intent)
                }
            }
        }
        if (show("tema", "interface", "escuro", "claro")) item {
            ChoiceRow("Tema", listOf(ThemeMode.SYSTEM to "Do sistema", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Escuro"), s.theme) { v -> update { it.copy(theme = v) } }
        }
        if (show("háptico", "vibração", "interface")) item { SwitchRow("Feedback háptico", null, s.haptics) { v -> update { it.copy(haptics = v) } } }
        if (show("animação", "movimento", "acessibilidade", "interface")) item { SwitchRow("Reduzir animações", "Desliga ondas de áudio e transições do painel", s.reduceMotion) { v -> update { it.copy(reduceMotion = v) } } }
        if (show("retenção", "histórico", "privacidade")) item { NumberField("Reter histórico por (dias; 0 = indefinido)", s.retentionDays) { v -> update { it.copy(retentionDays = v) } } }
        if (show("exportar", "privacidade")) item { OutlinedButton(onClick = { export.launch("opencell-historico.json") }) { Text("Exportar registros (JSON)") } }
        if (show("limpar", "excluir", "privacidade")) item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { scope.launch(Dispatchers.IO) { c.db.tasks().clearEvents(); c.db.tasks().clearTasks(); status = "Histórico apagado." } }) { Text("Limpar histórico") }
                OutlinedButton(onClick = {
                    c.secrets.remove(API_KEY_SECRET)
                    scope.launch(Dispatchers.IO) { c.db.tasks().clearEvents(); c.db.tasks().clearTasks() }
                    update { AppSettings() }
                    status = "Chave de API, histórico e preferências apagados."
                }) { Text("Apagar todos os dados do OpenCell") }
            }
        }
        status?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        item {
            Text("Não implementado nesta versão: captura de tela/visão, rotinas salvas, streaming de respostas, estimativa de custo, modo economia de bateria.", style = MaterialTheme.typography.labelSmall)
        }
    }
}
