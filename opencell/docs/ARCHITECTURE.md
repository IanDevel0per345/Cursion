# Arquitetura

## Módulos (e por que não são ~40)
A especificação sugere dezenas de módulos. Foi adotado o mínimo que preserva a separação que importa: **`agent-core`** (Kotlin/JVM, sem Android, testável sem dispositivo), **`app`** (adaptadores Android + UI) e **`playground`** (app alvo). Dentro de `agent-core` os pacotes `model`, `protocol`, `policy`, `engine`, `perception`, `provider`, `security` mantêm as responsabilidades separadas. Dividir mais traria custo de build sem ganho agora.

## Ciclo
`Orchestrator` (agent-core/engine) executa: planejar → para cada passo: observar → resolver alvo → política → (aprovação) → executar → aguardar → verificar → recuperar/replanejar.

Portas (interfaces) que o Android implementa: `ScreenObserver` (`AndroidScreenObserver` ← `AccessibilityBridge.snapshot`), `ActionExecutor` (`AndroidActionExecutor`), `ApprovalGate` (`AgentRuntime.ApprovalBridge`), `TaskJournal`. Em testes são substituídas por `FakeDevice`.

## Estados
`IDLE → UNDERSTANDING → PLANNING → [WAITING_FOR_PERMISSION | WAITING_FOR_APPROVAL] → OBSERVING → EXECUTING → VERIFYING → (OBSERVING | RECOVERING | PAUSED | COMPLETED | FAILED | CANCELLED)`. Transições inválidas lançam `IllegalTransitionException`; a tabela está em `AgentState.kt` e testada.

## Percepção
Somente canal semântico: `AccessibilityNodeInfo` → `UiSnapshot` (ids por caminho na árvore, máximo 600 nós, campos de senha marcados). O alvo é a janela de aplicativo mais ao topo cujo pacote **não** é o do OpenCell. Canal visual: não implementado.

## Interação (ordem de preferência)
1. Ação semântica (`ACTION_CLICK` no ancestral clicável, `ACTION_SET_TEXT`, `ACTION_SCROLL_*`).
2. Gesto por coordenadas (`dispatchGesture`) usando os limites do nó; o painel fica transparente a toques durante o gesto (`FLAG_NOT_TOUCHABLE`).
3. Falha explícita para o usuário.

## Segurança do executor
O modelo só produz um `Plan` JSON. `PlanParser` usa `kotlinx.serialization` com `ignoreUnknownKeys=false` e discriminador fechado: ação ou campo desconhecido → plano rejeitado. O executor valida alvo, política e verificação; o texto da tela vai ao modelo dentro de `<untrusted_screen_data>`.

## Persistência
DataStore (preferências), Room (tarefas e eventos, com `Redactor`), Keystore AES-256-GCM (chave de API).
