# Problemas comuns

- *Não consigo ativar a acessibilidade (item acinzentado)*: Android 13+ › Info do app › ⋮ › Permitir configurações restritas.
- *Painel não aparece sobre outros apps*: o overlay só existe com o serviço conectado; sem ele o painel abre como `PanelActivity`.
- *"Não consigo observar a tela"*: serviço desativado, ou o app em primeiro plano é o próprio OpenCell (não é alvo) ou não expõe elementos.
- *"Elemento não encontrado" ou "N elementos correspondem ao seletor"*: refine o texto ou use `index` num plano de IA.
- *Digitação recusada*: o campo não suporta `ACTION_SET_TEXT`.
- *Toque por gesto não funciona*: algumas telas bloqueiam gestos injetados; prefira apps com botões acessíveis.
- *Teste de conexão falha com 401/403*: chave/endpoint/modelo incorretos; 429: limite do provedor.
- *Wake word não responde*: experimental; use o tile ou a notificação.
