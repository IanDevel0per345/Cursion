# Relatório da simulação em massa

**Importante:** não há aparelho nem emulador neste ambiente. Os testes abaixo usam um **celular simulado** (`agent-core/src/test/.../sim`) com Chrome (busca, YouTube, canais), Mensagens (com mensagem de injeção), Configurações, Notas e Arquivos, listas com rolagem, diálogos que interrompem e atrasos de carregamento. Os resultados são conferidos no **estado do simulador**, não pelo que a IA diz. Isso valida o motor (planejamento iterativo, política, verificação, recuperação); **não** valida o comportamento do Chrome/YouTube reais do aparelho.

## Parte A — motor, 1300 tarefas por rodada (planejador de referência, não é IA)
13 tipos de tarefa, metade em modo permissivo e metade em supervisionado com aprovação automática.
| Rodada | Resultado |
|---|---|
| Sementes 7, 11, 21, 31 (sem ruído) | 1300/1300 em todas |
| Antes das correções (semente 42) | 500/1300 (bugs descritos abaixo) |
| Planejador que erra 25% das chamadas (antes → depois da correção) | 79% → 99,7% |
| Planejador que erra 40% das chamadas (antes → depois) | 51% → 76% (o resto cai por construção: 3 erros seguidos) |

## Parte B — Gemini real
Cota gratuita: ~10 req/min e limite diário por modelo; a cota acabou em vários modelos durante o teste. Rodada 1: 17/26. Rodada 2 (interrompida por cota): 11/18, das quais 4 falhas por HTTP 429. Amostra pequena; **não** é uma taxa de sucesso confiável.

## Defeitos encontrados e corrigidos
1. Plano feito uma única vez, olhando só a primeira tela → planejamento iterativo (`continue_planning`); fim de plano sem `complete_task` também continua.
2. Faltava a ação Enter (digitar no campo não executa a busca) → `press_enter` (`ACTION_IME_ENTER`, Android 11+).
3. Orçamento de replanejamento de 2 por tarefa inteira → agora conta falhas consecutivas (zera ao verificar um passo), padrão 4.
4. Tarefas certas terminavam "PARCIAL" por um passo inofensivo sem confirmação (ex.: tocar na barra de endereço) → conclusão baseada na tela final observada é sucesso com aviso.
5. Planejador local lia "abra o app de notas e crie…" como nome de app → só aceita comandos curtos e inequívocos.
6. Expectativa errada da IA em toque de alternância repetia o toque e desfazia o efeito → se a tela mudou, não repete: replaneja.
7. Erro de planejamento sem motivo → agora mostra o motivo (ex.: cota 429); backoff maior para 429.
8. IA indo além do pedido e citando rótulos com pequenas diferenças → regras no prompt e resolvedor tolerante a pontuação/acento.
9. Gemini 2.5-flash: raciocínio interno estourava o limite de tokens → `thinkingBudget=0` nesse modelo.
