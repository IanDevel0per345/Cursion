# Voz e ativação

| Modo | Implementação | Estado |
|---|---|---|
| D — manual | Botão na home / `PanelActivity` | Compila; não testado em aparelho |
| C — atalhos | Tile de Configurações Rápidas, notificação "Abrir painel", atalho do launcher, `ACTION_ASSIST` | Compila; não testado |
| A — assistente padrão | `VoiceInteractionService`/`SessionService` (+ `RecognitionService` mínimo exigido pelo framework, que apenas devolve erro) | Compila; não testado; depende do fabricante |
| B — "Ok OpenCell" | `WakeWordService`: `SpeechRecognizer` em loop com serviço em primeiro plano (microfone) e notificação "Desativar" | **Experimental**, não testado |

O modo B **não** é um detector de wake word dedicado: usa o reconhecedor do sistema em ciclos, o que pode tocar bipes, consumir bateria, enviar áudio ao serviço de reconhecimento do fabricante (conforme o motor), e ser encerrado pelo sistema. Não funciona de forma confiável com a tela desligada e não está disponível para apps de terceiros como escuta permanente em muitos aparelhos. Um detector local de verdade exigiria um motor dedicado (ex.: biblioteca de wake word) e não foi integrado.

STT: `SpeechRecognizer` do sistema, transcrição parcial exibida, idioma configurável. TTS: `TextToSpeech` do sistema (opcional). Sem supressão de ruído própria, sem ajuste de sensibilidade (os motores do sistema não expõem).
