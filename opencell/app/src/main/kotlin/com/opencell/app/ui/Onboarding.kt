package com.opencell.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.opencell.app.AppContainer
import com.opencell.core.policy.AutonomyMode
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(c: AppContainer, openProviders: () -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by c.settings.collectAsState()
    val ui by c.runtime.ui.collectAsState()
    rememberResumeTick()
    var page by remember { mutableIntStateOf(0) }
    var autonomy by remember { mutableStateOf(AutonomyMode.ASSISTED) }
    val total = 9

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            LinearProgressIndicator(progress = { (page + 1f) / total }, modifier = Modifier.fillMaxWidth())
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (page) {
                    0 -> {
                        Text("Bem-vindo ao OpenCell", style = MaterialTheme.typography.headlineMedium)
                        Text("Um agente que não só responde: ele opera a interface do seu celular, com sua autorização, e mostra o que realmente conseguiu fazer.")
                        Text("Limites reais: só atua em apps que expõem seus elementos ao Android, não resolve CAPTCHAs, não digita senhas e depende de permissões que você concede.", style = MaterialTheme.typography.bodyMedium)
                    }
                    1 -> {
                        Text("Como funciona", style = MaterialTheme.typography.headlineMedium)
                        Text("Falar ou digitar → entender → agir → verificar.")
                        Text("Cada passo é observado na tela depois de executado. Ações como enviar, pagar ou excluir pedem sua confirmação. Você pode pausar ou tocar em “Parar OpenCell” a qualquer momento.")
                    }
                    2 -> {
                        Text("Escolha da IA", style = MaterialTheme.typography.headlineMedium)
                        Text("Comandos simples (abrir app, tocar em texto, digitar, rolar, voltar) funcionam sem IA. Para pedidos livres, configure OpenAI, Gemini, Claude ou um endpoint compatível e execute o teste de conexão.")
                        Text("Provedor atual: " + if (s.model.isBlank()) "não configurado" else "${s.providerKind.name} · ${s.model}")
                        Button(onClick = openProviders) { Text("Configurar provedor e testar") }
                    }
                    3 -> {
                        Text("Acessibilidade", style = MaterialTheme.typography.headlineMedium)
                        Text("O OpenCell usa o serviço de acessibilidade do Android para ler a tela e tocar, rolar e digitar. Você ativa em Configurações › Acessibilidade › OpenCell Agente.")
                        Text("Estado: " + if (SystemChecks.accessibilityConnected()) "ativo e conectado" else if (SystemChecks.accessibilityEnabled(ctx)) "ativado, aguardando conexão" else "desativado")
                        Button(onClick = { SystemChecks.openAccessibilitySettings(ctx) }) { Text("Abrir configurações de acessibilidade") }
                    }
                    4 -> {
                        Text("Compartilhamento de tela", style = MaterialTheme.typography.headlineMedium)
                        Text("Nesta versão o OpenCell NÃO captura imagens da tela. Ele lê apenas a árvore de acessibilidade. Aplicativos que não expõem elementos (jogos, alguns apps protegidos) não são suportados.")
                    }
                    5 -> {
                        Text("Voz e ativação", style = MaterialTheme.typography.headlineMedium)
                        Text("Disponíveis neste aparelho:")
                        Text("• Painel pelo app e notificação: sim\n• Tile de Configurações Rápidas: sim (adicione manualmente)\n• Assistente padrão: " +
                            (if (SystemChecks.assistantRoleAvailable(ctx)) "disponível (defina em Apps padrão)" else "indisponível") +
                            "\n• Ditado por microfone: " + (if (android.speech.SpeechRecognizer.isRecognitionAvailable(ctx)) "sim" else "não") +
                            "\n• “Ok OpenCell” em segundo plano: experimental, opcional em Configurações")
                    }
                    6 -> {
                        Text("Nível de autonomia", style = MaterialTheme.typography.headlineMedium)
                        ChoiceRow("Comece de forma conservadora", AutonomyMode.values().filter { it != AutonomyMode.BOUNDED_ROUTINE }.map { it to autonomyLabel(it) }, autonomy) { autonomy = it }
                        Text("Você pode mudar isso depois em Configurações.", style = MaterialTheme.typography.labelSmall)
                    }
                    7 -> {
                        Text("Teste guiado", style = MaterialTheme.typography.headlineMedium)
                        Text("Vai abrir o aplicativo OpenCell Test Playground (instalado à parte) e rolar a tela. É uma ação real; o resultado vem da verificação, não de uma animação.")
                        Button(enabled = !ui.taskActive, onClick = { c.runtime.submit(GUIDED_TEST_GOAL); c.panelLauncher.show() }) { Text("Executar teste") }
                        ui.message?.let { Text("Resultado: $it") }
                    }
                    else -> {
                        Text("Tudo pronto", style = MaterialTheme.typography.headlineMedium)
                        Text("Acessibilidade: " + if (SystemChecks.accessibilityConnected()) "ativa" else "pendente (você pode ativar depois)")
                        Text("Microfone: " + if (SystemChecks.micGranted(ctx)) "concedido" else "não concedido (opcional)")
                        Text("Provedor de IA: " + if (s.model.isBlank()) "não configurado (apenas comandos simples)" else "configurado")
                        Text("Autonomia: ${autonomyLabel(autonomy)}")
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (page > 0) OutlinedButton(onClick = { page-- }) { Text("Voltar") } else TextButton(onClick = {}, enabled = false) { Text("") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (page in 2..7) TextButton(onClick = { page++ }) { Text("Pular") }
                    Button(onClick = {
                        if (page < total - 1) page++ else scope.launch {
                            c.settingsRepo.update { it.copy(onboardingDone = true, autonomy = autonomy) }
                            onDone()
                        }
                    }) { Text(if (page < total - 1) "Continuar" else "Concluir") }
                }
            }
        }
    }
}
