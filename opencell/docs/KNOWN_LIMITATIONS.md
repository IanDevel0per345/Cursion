# Limitações conhecidas

## Não validado em dispositivo
Tudo o que depende do Android em execução: serviço de acessibilidade, gestos, overlay, voz, assistente, tile, Keystore, Room/DataStore, renderização Compose. Só há prova de compilação, lint e testes JVM.

## Não implementado
Percepção visual (MediaProjection), rotinas/automações salvas, streaming, estimativa de custo, teste de visão, modo economia/qualidade adaptativa, modelos locais, exportação de diagnóstico, tela de histórico de chamadas de API, suporte a `tap_point` vindo do modelo sem screenshot (existe a ação, mas sem visão não há de onde extrair coordenadas), ferramentas `get_current_*`/`find_ui_element` expostas ao modelo.

## Técnicas
- O alvo exclui o próprio OpenCell; não automatiza suas telas.
- Risco por rótulo é heurística; rótulos ambíguos/ícones sem texto podem passar sem aprovação (use modo Assistido).
- Dois níveis de verificação: expectativa explícita (falha → recuperação) ou `ScreenChanged` heurístico (falha → `UNVERIFIED`).
- `protectedContent` nunca é preenchido (o Android não informa `FLAG_SECURE` ao serviço): telas protegidas aparecem como árvore vazia/incompleta.
- Correções do usuário durante a execução exigem provedor de IA (replanejamento); sem IA a tarefa é falha limpa.
- Wake word experimental (ver VOICE_AND_WAKEWORD).
- Sem consentimento por tarefa antes de enviar o conteúdo da tela ao provedor (só a configuração global).

## Distribuição (Google Play)
Distinguir: **viabilidade técnica** (a API existe) ≠ **permissão do usuário** (ele ativa o serviço) ≠ **elegibilidade de distribuição**. A política do Google Play restringe o uso de `AccessibilityService` a funcionalidades de acessibilidade e exige declaração; um agente autônomo generalista **provavelmente não é elegível** à Play Store como está, e a política vigente deve ser lida diretamente antes de qualquer submissão (não foi consultada nesta sessão). Alternativas legais: distribuição direta (APK/F-Droid/loja alternativa) com a mesma transparência, ou reduzir o escopo para a categoria permitida. Não descrever o app como ferramenta de acessibilidade para pessoas com deficiência sem que isso seja verdade.
