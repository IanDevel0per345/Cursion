# Rascunho de política de privacidade (não é texto jurídico)

- O OpenCell lê, pelo serviço de acessibilidade, textos e estrutura da tela do aplicativo em primeiro plano **durante uma tarefa**.
- Quando há provedor de IA configurado e o comando exige planejamento por IA, o objetivo e uma descrição textual da tela (sem conteúdo de campos de senha, com padrões sensíveis mascarados) são enviados **ao provedor escolhido por você**, sob a política dele. Comandos simples resolvidos localmente não enviam nada.
- Nenhuma imagem de tela é capturada nesta versão.
- O histórico (objetivo, passos, resultado, aplicativo, tokens) fica apenas no aparelho, com retenção configurável (padrão 30 dias), exportação e exclusão. Chave de API fica cifrada no Keystore.
- Sem analytics, sem servidor próprio.
- Áudio: processado pelo reconhecedor de voz do sistema; o OpenCell não grava nem guarda áudio. Dependendo do motor do aparelho, o áudio pode ir à nuvem do fabricante.
