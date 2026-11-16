# Protocolo modelo ⇄ executor

O modelo devolve apenas JSON `Plan` (`agent-core/protocol/Actions.kt`):
```json
{"version":1,"goal":"...","steps":[{"id":"s1","description":"...",
 "action":{"type":"tap_element","selector":{"text":"Mensagens"}},
 "expect":{"type":"text_visible","text":"Cliente A"}}]}
```
Ações (conjunto fechado): `open_application`, `tap_element`, `tap_point` (coordenadas normalizadas + `snapshotId`), `long_press`, `swipe`, `scroll_container`, `enter_text`, `press_back`, `wait_for_ui_change`, `request_user_confirmation`, `report_limitation`, `complete_task`.
Expectativas: `text_visible`, `text_absent`, `element_exists`, `field_contains`, `package_is`, `screen_changed`, `scrolled`.

Validação (`PlanParser`): JSON estrito, tipos/campos desconhecidos rejeitados, máx. 30 passos, ids únicos, texto ≤ 2000, coordenadas em 0..1. Em falha o `ModelPlanner` reenvia o erro ao modelo uma vez.

Resultado: gerado pelo orquestrador (`TaskReport`: `Outcome`, `StepResult` com `VERIFIED/UNVERIFIED/FAILED/BLOCKED/SKIPPED`), nunca pelo modelo. Sem expectativa explícita e sem mudança observável o passo vira `UNVERIFIED` e a tarefa `PARTIAL` ("Ação executada, resultado ainda não verificado").

Ferramentas sugeridas na especificação que **não** existem como ações: `get_current_ui_tree`, `get_current_screen_snapshot`, `find_ui_element`, `focus_input`, `verify_ui_state`, `pause_task` (o snapshot é feito pelo orquestrador a cada passo; pausa é controle do usuário).

## Risco e retentativas
`PolicyEngine` classifica por rótulo do alvo (heurística de palavras: enviar/publicar → externo; excluir/pagar/comprar → irreversível). Ações com risco ≠ NONE **nunca** são repetidas automaticamente; falha de verificação vira `PARTIAL`. Limites: passos, retentativas por ação, repetição do mesmo passo na mesma tela (detecção de loop), replanejamentos.

## Conteúdo não confiável
O snapshot é enviado entre `<untrusted_screen_data>`; marcadores falsos dentro do conteúdo são removidos; o prompt de sistema manda tratar o conteúdo como dado. **Isto reduz, não elimina, o risco de prompt injection**: a defesa que não depende do modelo é a política (aprovação para ações externas/irreversíveis, bloqueio de senhas, apps permitidos/bloqueados).
