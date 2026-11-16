# Modelo de ameaças (simples)

| Ameaça | Mitigação implementada | Teste |
|---|---|---|
| Roubo de chave de API | Keystore AES-GCM; nunca exibida; `Redactor` em erros/histórico; `allowBackup=false` | `ProviderTest.authErrorDoesNotLeakKey`, `PerceptionTest.redactorMasksSecrets` |
| Prompt injection (app/site/mensagem) | Dados entre `<untrusted_screen_data>`, marcadores removidos, prompt de sistema; plano em JSON estrito com ações fechadas; política independente do modelo | `planPromptIsolatesUntrustedScreenData`, `parserAcceptsFencedJsonAndRejectsUnknownActions`. **Não há teste adversarial contra um modelo real** |
| Ação em app errado | `package_is` após abrir app; apps permitidos/bloqueados; OpenCell nunca é alvo | `PolicyTest` |
| Duplicar envio após erro | Risco ≠ NONE nunca é repetido; falha de verificação → `PARTIAL` | `externalActionIsNeverBlindlyRetriedWhenUnverified` |
| Execução após revogar permissão | Serviço chama `runtime.stop`; ponte nula → falha explícita | Não testado em aparelho |
| Captura de campo sensível | Campos de senha: texto não enviado ao modelo, digitação negada | `passwordFieldTypingDenied` |
| Dados em notificações/logs | Notificação mostra objetivo redigido; sem screenshots; histórico redigido | Revisão de código |
| Interromper durante ação crítica | Cancelamento cooperativo + `cancelPending` do gesto; "Parar OpenCell" no painel e na notificação | `controllerCancelStopsBeforeNextAction`, `coroutineCancellationDuringRun…` |
| Provedor externo com política própria | O usuário escolhe o provedor; o texto da tela vai ao provedor — **o app não pede consentimento específico por tarefa** (lacuna) | — |
| Heurística de risco burlada (rótulo "Mandar") | Risco por palavras: **limitação conhecida**; mitigar com modo Assistido | — |
