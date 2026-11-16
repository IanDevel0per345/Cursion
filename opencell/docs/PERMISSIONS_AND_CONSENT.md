# Permissões e consentimento

A Central de permissões (`PermissionsScreen`) mostra, para cada item, *por que precisa*, *o que permite*, *o que não permite*, *quando é usado* e *como revogar*, com o estado verificado em tempo real (`SystemChecks`).

| Item | Verificação | Revogação |
|---|---|---|
| Acessibilidade | `ENABLED_ACCESSIBILITY_SERVICES` + conexão real do serviço | Configurações › Acessibilidade. `onUnbind/onDestroy` chamam `runtime.stop(...)`, derrubam o painel e anulam a ponte; ações seguintes falham com mensagem clara |
| Microfone | `checkSelfPermission` | Config. do app |
| Notificações | API 33+ | Config. do app |
| Assistente | `RoleManager.isRoleHeld` | Apps padrão |
| Captura de tela | Não implementada | — |

Negar uma permissão não bloqueia o app: sem microfone só a voz some; sem notificações some o aviso; sem acessibilidade as tarefas falham com a explicação. Nada é concedido ou ativado automaticamente. Em Android 13+ fora da Play Store o usuário pode precisar liberar "Configurações restritas".
