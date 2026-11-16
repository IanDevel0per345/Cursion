# Compatibilidade

`minSdk 26` (Android 8.0): `dispatchGesture` (24), ícones adaptativos, canais de notificação; abrange a grande maioria dos aparelhos ativos. `targetSdk 35`. Recursos acima têm guardas: serviço de microfone tipado (29), reconhecimento on-device (31), papel de assistente (29), dispensar a aba de notificações (31), tile com `PendingIntent` (34).

**Nenhuma marca ou versão foi testada.** Riscos conhecidos: Samsung/Xiaomi/Motorola/Huawei podem encerrar serviços em segundo plano, bloquear overlays ou o início de serviço de microfone; apps com `FLAG_SECURE`, WebViews, jogos e UIs customizadas expõem árvore incompleta; Android 13+ exige liberar "Configurações restritas" para APKs instalados fora da loja; tablets/dobráveis usam as dimensões da janela atual (sem tratamento específico de recortes). A tela *Diagnóstico* lista o que o aparelho declara e nunca mostra "100% compatível".
