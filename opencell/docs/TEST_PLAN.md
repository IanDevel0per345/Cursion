# Plano e resultados de teste

## Executado neste ambiente (Linux, JDK 21, sem dispositivo)
| Comando | Resultado |
|---|---|
| `./gradlew :agent-core:test` | **54 testes, 0 falhas** (StateMachine 5, Perception 9, Policy 10, Provider 15, Orchestrator 15) |
| `./gradlew :app:assembleDebug :playground:assembleDebug` | Sucesso (APKs debug, não assinados para distribuição) |
| `./gradlew :app:lintDebug :playground:lintDebug` | 0 erros (app: 5 avisos; playground: 3) |
| `./gradlew :app:compileDebugAndroidTestKotlin` | Sucesso |

O `FakeDevice` dos testes do orquestrador é um **dublê de teste**: prova a lógica do ciclo (aprovação, recuperação, cancelamento, pausa, loop, correção do usuário), não a interação com o Android.

## Instrumentados (escritos, NÃO executados)
`E2ePlaygroundTest`: abrir Playground + tocar aba + rolar com verificação; preencher campo sem enviar; envio exige aprovação e recusa não altera contador; botão renomeado → falha sem cliques cegos; parar cancela. Usam `Assume` (ignoram se o serviço não estiver conectado). `MainActivityTest`: abre sem crash. Execução: `scripts/e2e.sh` com dispositivo.

## Critérios de aceitação (seção 29)
| # | Critério | Estado |
|---|---|---|
| 1 | App abre sem crash em Android de teste | **Não verificado** (só compila) |
| 2 | Onboarding salva preferências | Implementado; não verificado |
| 3 | Provedor configurado e testado | Implementado; testado só contra mock |
| 4–5 | Habilitar e detectar serviço de acessibilidade | Implementado; não verificado |
| 6–7 | Painel manual + rota adicional | Implementado; não verificado |
| 8 | Comando por texto/voz | Implementado; não verificado |
| 9 | Interpreta objetivo e cria plano estruturado | **Validado em JVM** (RuleBased + ModelPlanner/mock) |
| 10–13 | Encontrar elementos, tocar, rolar, digitar no app de teste | Implementado; **não verificado em aparelho** |
| 14 | Verificador confirma mudança | **Lógica validada em JVM**; contra tela real não verificado |
| 15 | Tarefa multietapas | **Validada em JVM** com dublê; real não verificada |
| 16 | Pausa/cancela/recupera | **Validado em JVM**; real não verificado |
| 17 | Ações sensíveis bloqueadas até confirmação | **Validado em JVM** |
| 18 | Histórico com resultados reais | Implementado (Room); não verificado |
| 19 | Revogar permissão desativa capacidades | Implementado; não verificado |
| 20 | Documentação das limitações | Este conjunto de docs |

**Os itens 10–14 só serão "entregues" após rodar `scripts/e2e.sh` num aparelho/emulador.** Até lá, esta versão não cumpre a seção 29 da especificação.

## Checklist manual (preencher ao testar)
Três perfis: Android recente, intermediário, emulador. Para cada: fabricante/modelo/API; ativar serviço; abrir painel (home, tile, notificação); teste guiado; preencher mensagem sem enviar; aprovar/recusar envio; rolar lista; trocar rótulo do botão e confirmar falha limpa; parar durante execução; revogar acessibilidade durante tarefa; girar tela; teclado aberto; ditado; wake word. Registrar esperado × obtido × falhas.
