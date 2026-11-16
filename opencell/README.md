# OpenCell

Agente Android nativo (Kotlin + Compose) que recebe uma tarefa por texto ou voz, monta um plano estruturado e o executa na interface do celular por meio do `AccessibilityService` (toque, rolagem, digitação, voltar, abrir app), **verificando na tela o efeito de cada passo**.

> Your phone. Your AI. Your control.

## Estado honesto

Escrito e validado **neste ambiente (Linux, sem emulador, sem KVM, sem aparelho)**:

| Item | Estado |
|---|---|
| Núcleo do agente (`agent-core`: protocolo, política, máquina de estados, orquestrador, resolvedor, verificador, provedores, parser de plano) | Implementado e **validado por 54 testes JVM** (0 falhas) |
| Clientes OpenAI-compatível / Anthropic / Gemini | Implementados e validados **somente contra servidor simulado** (MockWebServer). Nenhuma chamada real foi feita com chave verdadeira |
| App Android (`app`) e Test Playground | **Compilam** (`assembleDebug`), `lintDebug` com 0 erros; testes instrumentados **compilam** |
| Serviço de acessibilidade, gestos reais, painel sobreposto, voz, assistente, tile, Keystore, Room/DataStore em execução, UI renderizada | **Implementado, NÃO testado em aparelho** |
| Palavra de ativação "Ok OpenCell" | Implementação **experimental**, não testada |
| Captura de tela / visão multimodal (MediaProjection) | **Não implementada** |
| Rotinas salvas, streaming, estimativa de custo, economia de bateria | **Não implementados** |

Detalhes e a checklist de aceitação: [docs/TEST_PLAN.md](docs/TEST_PLAN.md) e [docs/KNOWN_LIMITATIONS.md](docs/KNOWN_LIMITATIONS.md).

## Estrutura

```
agent-core/   JVM puro: protocolo de ações, política, orquestrador, percepção, provedores de IA, testes
app/          Android: serviço de acessibilidade, executor, painel (Compose), telas, Room, DataStore, Keystore
playground/   OpenCell Test Playground: app alvo com telas controladas para os testes de interação
docs/         Documentação técnica
scripts/e2e.sh  Instala os APKs, habilita o serviço via adb e roda os testes instrumentados
```

## Requisitos

JDK 17+ (testado com 21), Android SDK com `platforms;android-35` e `build-tools;35.0.0`. Aponte `sdk.dir` em `local.properties`.

## Comandos

```
./gradlew :agent-core:test                 # testes unitários do núcleo
./gradlew :app:assembleDebug :playground:assembleDebug   # APKs de debug (não assinados para distribuição)
./gradlew :app:lintDebug
scripts/e2e.sh                             # exige dispositivo/emulador adb
```

APKs: `app/build/outputs/apk/debug/app-debug.apk`, `playground/build/outputs/apk/debug/playground-debug.apk`.

## Uso rápido

1. Instale os dois APKs. Abra o OpenCell e conclua o onboarding.
2. Ative **Configurações › Acessibilidade › OpenCell Agente** (em Android 13+ fora da Play Store pode ser preciso liberar "Configurações restritas" na tela de info do app).
3. Opcional: configure um provedor em *Provedor de IA* e use *Testar conexão*. Sem provedor, comandos simples funcionam localmente: `abra o aplicativo de teste, toque em Mensagens, toque em Cliente A, digite "Olá!" no campo`.
4. Abra o painel: botão na home, tile de Configurações Rápidas, notificação ou assistente padrão. Digite ou fale a tarefa.

Documentos: ver [docs/](docs/).
