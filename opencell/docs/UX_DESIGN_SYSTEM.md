# Design system

Tokens em `ui/Theme.kt` (cores semânticas claro/escuro, tipografia, formas), ícone temporário substituível (anel teal sobre azul-noite). Identidade própria: sem marcas ou assets de terceiros.

Componentes (`ui/Components.kt`, `ui/Panel.kt`): `OpenCellBottomPanel`, `VoiceWaveform` (anima só enquanto ouve; estática com "reduzir animações"), `TaskProgressCard`, `ApprovalSheet`, `PermissionExplainer`, `ProviderCard`, `AgentActivityTimeline`.

Estados do painel (`PanelState`): Idle, Listening, Transcribing, Thinking, Planning, Executing, WaitingForApproval, Paused, Success, PartialSuccess, Error; abrir/fechar são animações (deslizar+fade, 320 ms, 0 ms com redução de movimento). `Closed/Opening/Closing` não são estados do modelo.

Acessibilidade: `contentDescription` nos ícones, alvos de toque ≥ 48 dp via Material, foco de teclado/IME tratado (`imePadding`). Não auditado com TalkBack nem com verificador de contraste. Háptico em aprovação/resultado (configurável).
