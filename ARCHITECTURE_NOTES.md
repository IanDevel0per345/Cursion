# Architecture Notes — Cursion (fork de e2b-dev/dashboard)

> Documento de estudo da base — Fase 1 do projeto Cursion (IPaaS).

## 1. Stack e Configuração

- **Next.js 16**, **React 19**, **TypeScript**
- Gerenciador: **Bun** (`bun.lock`, scripts em `package.json`)
- Lint/Format: **Biome** (`biome.json`)
- Componentes UI: **shadcn/ui** (`components.json`), ícones **lucide**
- Estilização: **Tailwind CSS** (`postcss.config.js`, `tailwind.config.ts`)
- Testes: **Vitest** (`vitest.config.ts`)
- OpenAPI: tipos gerados via `openapi-typescript` em `spec/*.yaml` → `src/core/shared/contracts/*.types.ts`
- Telemetria: **OpenTelemetry** (OTLP HTTP para logs/métricas/traces)

## 2. Estrutura de Rotas e Layout

### App Router (Next.js 16)

- `src/app/(dashboard)/` — layout protegido por API key
  - `layout.tsx`: valida `apiKey` via `getApiKey()`; redireciona `/` se ausente
  - `Sidebar` + `DashboardLayoutView` como wrapper
  - Rotas protegidas: `/sandboxes/**`, `/templates/**`

- Páginas principais:
  - `/sandboxes` — lista paginada de sandboxes (`NewSandboxesTable`)
  - `/sandboxes/[sandboxId]` — detalhe com abas:
    - `/monitoring` — métricas CPU/memória/disco
    - `/logs` — logs filtráveis
    - `/terminal` — terminal web (xterm/pty)
    - `/filesystem` — inspetor de arquivos
  - `/templates/list`, `/templates/builds` — listagem de templates e builds
  - `/templates/[templateId]` — detalhe com overview, builds, tags

- API routes internas:
  - `/api/sign-out` — limpa cookie da API key
  - `/api/trpc/[trpc]` — endpoint tRPC
  - `/api/health` — health check

### Layout visual

- **Sidebar** (`src/features/dashboard/sidebar/`):
  - Header (logo/brand), Content (links de navegação), Footer (user/settings), Rail (colapsável)
  - Estado salvo em cookie (`COOKIE_KEYS.SIDEBAR_STATE`)

- **Header/Navbar** — dentro de `DashboardLayoutView`
- **Tema claro/escuro** — variáveis CSS em `src/styles/theme.css`

## 3. Design System

### Cores (light theme base)

- Fundo: `--bg: #fafafa`, `--bg-1: #f5f5f5`, `--bg-hover: #f5f5f5`
- Texto: `--fg: #0a0a0a`, `--fg-secondary: #333333`, `--fg-tertiary: #707070`
- Bordas: `--stroke: #d6d6d6`, `--stroke-active: #c2c2c2`
- **Destaque principal (laranja)**: `--accent-main-highlight: #e56f00`, `--accent-main-bg: #e56f0029`
- Status:
  - Positivo: `--accent-positive-highlight: #00a670`
  - Info: `--accent-info-highlight: #3c98c7`
  - Warning: `--accent-warning-highlight: #d1a102`
  - Erro: `--accent-error-highlight: #ff4400`

### Tipografia e espaçamentos

- Headings: `1.25rem` (h1-h3), `1rem` (h4-h6), uppercase, font-weight 700
- Body: via `prose-body` do Tailwind
- Scrollbars customizadas (4px, cor `--bg-highlight`)

### Componentes reutilizáveis (`src/ui/`)

- `data-table.tsx` — tabela genérica com sorting, filtros, column sizing
- `alert-dialog.tsx`, `alert-popover.tsx` — modais de confirmação/aviso
- `code-block.tsx` — blocos de código com syntax highlighting (shiki)
- `copy-button.tsx` — botão de copiar para clipboard
- `dashboard-tabs.tsx` — abas de navegação internas
- `empty.tsx`, `not-found.tsx`, `error.tsx` — estados vazios/erro
- `live.tsx` — indicador "ao vivo" (polling)
- `time-range-picker.tsx` — seletor de intervalo de tempo (para métricas)
- `theme-switcher.tsx` — toggle claro/escuro
- `primitives/` — componentes shadcn (button, input, select, popover, etc.)

## 4. Autenticação e Chamadas às APIs da E2B

### Autenticação

- **Team API Key** (`e2b_...`):
  - Inserida em `/` (form de API key) ou via env `E2B_API_KEY`
  - Armazenada em cookie `httpOnly` (`e2b_api_key`)
  - Header upstream: `X-API-Key` (via `apiKeyHeaders()` em `src/configs/api.ts`)
  - Middleware tRPC (`src/core/server/api/middlewares/auth.ts`) injeta `apiKey` no contexto

- Server-only: chave nunca exposta ao client JS; chamadas API feitas no servidor

### APIs upstream

- **infra-api** (`https://api.<domain>`):
  - Sandboxes: `GET /v2/sandboxes`, `GET /v2/sandboxes/{id}/logs`, `GET /v2/sandboxes/{id}/metrics`
  - Templates: `GET /templates`, `POST /templates`, `GET /templates/{id}/builds`
  - Especificação: `spec/openapi.infra.yaml` → tipos em `src/contracts/infra-api`

- **dashboard-api** (`https://dashboard-api.<domain>`):
  - Metadados de templates/builds
  - Especificação: `spec/openapi.dashboard-api.yaml`

- Clientes HTTP: `src/core/shared/clients/api.ts` (`api`, `infra`)
  - Usam `openapi-fetch` com tipos gerados
  - Headers de API key injetados via `apiKeyHeaders(apiKey)`

### Repositórios (camada de dados)

- `src/core/modules/sandboxes/repository.server.ts`:
  - `listSandboxesPaginated()`, `getSandboxDetails()`, `getSandboxLogs()`, `getSandboxMetrics()`
  - Tratam erro HTTP → `repoErrorFromHttp()`
  - Retornam `RepoResult<T>` (padrão `ok()` / `err()`)

- `src/core/modules/templates/repository.server.ts` — similar para templates
- `src/core/modules/builds/repository.server.ts` — builds de templates

## 5. Componentes Específicos da E2B (a remover na Fase 2)

### Acoplados a sandboxes/templates

- `src/features/dashboard/sandboxes/` — lista, filtros, tabela, header
- `src/features/dashboard/sandbox/` — detalhe de um sandbox:
  - `monitoring/` — gráficos de CPU/memória/disco
  - `logs/` — tabela de logs com filtro de nível/busca
  - `terminal/` — terminal web (xterm) com sessão PTY
  - `filesystem/` — árvore de arquivos com download/upload

- `src/features/dashboard/templates/` — lista, builds, tags, visibilidade
- `src/features/dashboard/build/` — logs de build de template (streaming)

### Tipos e contratos

- `src/core/modules/sandboxes/models.ts` — `Sandbox`, `SandboxState`, `SandboxesMetricsRecord`
- `src/core/modules/templates/models.ts` — `Template`, `TemplateVisibility`, `Build`
- `src/contracts/` — tipos gerados das APIs (infra + dashboard)

### Configurações específicas

- `src/configs/api.ts` — `API_KEY_PREFIX = 'e2b_'`, `API_KEY_HEADER = 'X-API-Key'`
- `src/configs/urls.ts` — `PROTECTED_URLS` com rotas de sandboxes/templates
- `src/configs/metadata.ts` — títulos/descrições com "E2B"
- `src/configs/favicon.ts` — favicon da E2B
- Variáveis de ambiente:
  - `PUBLIC_E2B_DOMAIN` (obrigatória)
  - `E2B_INFRA_API_URL`, `E2B_DASHBOARD_API_URL` (overrides)
  - `PUBLIC_SANDBOX_URL` (base para tráfego de sandbox no browser)

## 6. Partes Puramente Visuais (reaproveitáveis para IPaaS)

### Layout e navegação

- `src/app/(dashboard)/layout.tsx` — estrutura base (Sidebar + conteúdo)
- `src/features/dashboard/sidebar/` — sidebar genérica
- `src/features/dashboard/layouts/layout.tsx` — wrapper de páginas
- `src/ui/primitives/sidebar.tsx` — componente Sidebar do shadcn

### Tabelas e listas

- `src/ui/data-table.tsx` — tabela genérica (TanStack Table)
- `src/features/dashboard/sandboxes/list/table.tsx` — exemplo de uso (adaptável para "conexões"/"integrações")
- Filtros, sorting, paginação, column sizing — tudo genérico

### Cards e status

- Componentes de badge, indicator, empty state, error state
- `src/ui/alert-dialog.tsx`, `src/ui/alert-popover.tsx` — úteis para confirmações de conexão OAuth

### Gráficos e métricas

- `src/features/dashboard/sandbox/monitoring/` — gráficos de série temporal (CPU/memória/disco)
- `src/ui/time-range-picker.tsx` — seletor de intervalo reutilizável
- Padrões de polling (`live.tsx`, `polling-button.tsx`)

### Terminal e logs

- `src/features/dashboard/terminal/` — terminal web (xterm) + gestão de sessão PTY
- `src/features/dashboard/sandbox/logs/` — tabela de logs com filtros
- Reaproveitável para "logs de execução de ações/triggers" no IPaaS

### Formulários e inputs

- `src/ui/number-input.tsx`, `src/ui/time-input.tsx`
- `src/ui/primitives/` — input, select, checkbox, radio, etc.
- `@hookform/resolvers` + `react-hook-form` para validação

## 7. Pontos de Atenção para a Fase 2 (Extração do Frontend)

### O que manter

- Todo o design system (`src/styles/`, `src/ui/`, `components.json`)
- Layout do dashboard (`(dashboard)/layout.tsx`, sidebar, header)
- Componentes genéricos: tabelas, cards, badges, modais, terminal, time-range-picker
- Estrutura de rotas (App Router) como esqueleto
- tRPC como padrão de chamadas server→client (mas com novo backend)

### O que remover

- Chamadas a `infra` e `api` (clientes OpenAPI) em `src/core/shared/clients/api.ts`
- Repositórios de sandboxes/templates/builds (`src/core/modules/**/repository.server.ts`)
- Tipos gerados em `spec/` e `src/contracts/` (ou substituir por contratos do IPaaS)
- Autenticação por team API key da E2B (`src/core/server/auth.ts`, middleware tRPC)
- Configs com `E2B_` prefix (`src/configs/api.ts`, `urls.ts`, `metadata.ts`, `favicon.ts`)
- Páginas específicas de sandboxes/templates (ou refatorar para "conexões"/"conectores")

## 8. Mapeamento Conceitual (E2B → IPaaS Cursion)

| E2B Dashboard | Cursion IPaaS |
|---|---|
| Sandbox | Conexão/Integração ativa de um usuário |
| Template | Conector/blueprint (ex.: "GitHub", "Slack") |
| Build logs | Logs de execução de ação/trigger |
| Team API key | Chave de API da plataforma (por usuário/org) |
| Monitoring (CPU/mem) | Métricas de uso (chamadas, erros, latência) |
| Terminal | Console de logs de execução |
| Filesystem inspector | Inspetor de payloads/requests (futuro) |

## 9. Próximos Passos (Fase 2)

1. Criar branch `cursion/ipaas-base` a partir deste código
2. Remover chamadas a `infra`/`api` e substituir por mocks
3. Manter páginas de lista/detalhe como templates genéricos
4. Rodar `bun install`, `bun dev` e validar build sem erros
5. Documentar quebras e dívidas técnicas para a Fase 3 (rebranding)

---

> Licença original: **Apache 2.0** (`LICENSE`). Manter arquivo e aviso de copyright; adicionar nota de derivação em `NOTICE` ou README.
