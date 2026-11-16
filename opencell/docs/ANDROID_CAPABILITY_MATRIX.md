# Matriz de viabilidade

Fontes: conhecimento da API pública do Android (níveis de API indicados). **A documentação oficial online não foi reconsultada nesta sessão**; confirme antes de publicar. "Status real" reflete o que foi *verificado aqui*: compila ≠ funciona em aparelho.

| Recurso | API | Permissão | API mín. | Restrições | Alternativa | Status real |
|---|---|---|---|---|---|---|
| Painel manual | Compose + `TYPE_ACCESSIBILITY_OVERLAY` / `PanelActivity` | Serviço de acessibilidade (overlay) | 26 | Overlay só com serviço conectado | `PanelActivity` | Compila; não testado em aparelho |
| Painel via assistente | `VoiceInteractionService` + `RoleManager.ROLE_ASSISTANT` | Usuário define o assistente | 29 (role) | Fabricante pode não liberar a terceiros | Tile/notificação | Compila; não testado |
| "Ok OpenCell" tela ligada | `SpeechRecognizer` em loop (FGS microfone) | `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE` | 26 (tipo FGS: 29) | Bipes, bateria, cortes por OEM | Tile/assistente | Experimental; não testado |
| "Ok OpenCell" em segundo plano | idem | idem | 30+ restringe início de FGS de microfone em background | Início só com app visível | Gatilhos manuais | Experimental; não testado |
| "Ok OpenCell" tela desligada | — | — | — | Sem garantia para app de terceiros | Assistente do sistema | **Não prometido** |
| Conteúdo visual | `MediaProjection` / `takeScreenshot` | Consentimento por sessão | 21 / 30 | Telas protegidas | Árvore de acessibilidade | **Não implementado** |
| Árvore de acessibilidade | `AccessibilityNodeInfo`, `windows` | Serviço | 21 | Apps sem rótulos, `FLAG_SECURE`, WebViews | Intervenção do usuário | Compila; não testado |
| Tocar botão | `ACTION_CLICK`, `dispatchGesture` | Serviço (`canPerformGestures`) | 24 | App pode ignorar | Coordenadas | Compila; não testado |
| Rolar lista | `ACTION_SCROLL_FORWARD/BACKWARD`, swipe | idem | 21 / 24 | Container sem ação | Swipe | Compila; não testado |
| Inserir texto | `ACTION_SET_TEXT` | idem | 21 | Campos customizados podem recusar | — (falha explícita) | Compila; não testado |
| Enviar após confirmar | Política + `ApprovalGate` | — | — | — | — | Lógica **validada em JVM** (`OrchestratorTest`); UI não testada |
| Continuar após trocar de app | Alvo = janela de app mais ao topo | Serviço | 21 | — | — | Compila; não testado |
| Recuperar de erro | Replanejamento/limites | — | — | Exige provedor para replanejar | Falha explícita | **Validado em JVM** |
| Offline | `RuleBasedPlanner` | — | — | Só comandos simples | — | **Validado em JVM** |
| Marcas/versões | — | — | — | — | Diagnóstico | Não testado em nenhuma marca |
