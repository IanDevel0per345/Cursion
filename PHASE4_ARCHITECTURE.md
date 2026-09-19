# Phase 4 — IPaaS Architecture

## Visã©©o Geral

Cursion é uma plataforma IPaaS (Integration Platform as a Service) que permite:
- Conectar contas de plataformas terceiras (GitHub, Slack, Gmail, etc.)
- Executar ações e triggers nessas plataformas
- Monitorar uso e métricas de integraç©©o

## Arquitetura de Conectores

### Design Pattern: Connector Interface

```typescript
interface Connector {
  id: string;           // ex: "github", "slack", "gmail"
  name: string;         // ex: "GitHub"
  auth: AuthConfig;     // OAuth2, API Key, etc.
  actions: Action[];    // Aç©£oes executá¡©veis
  triggers: Trigger[];  // Webhooks/Triggers
}

interface AuthConfig {
  type: "oauth2" | "api_key" | "basic";
  authorizationUrl?: string;
  tokenUrl?: string;
  scopes?: string[];
  refreshToken?: boolean;
}

interface Action {
  id: string;           // ex: "create_issue", "send_message"
  name: string;
  description: string;
  inputSchema: JSONSchema;
  execute: (params: any, tokens: TokenSet) => Promise<any>;
}

interface Trigger {
  id: string;           // ex: "new_issue", "message_received"
  name: string;
  webhookPath?: string;
  subscribe: (config: any, tokens: TokenSet) => Promise<string>;
  unsubscribe: (subscriptionId: string) => Promise<void>;
}
```

### Roadmap de Conectores (1500+ meta)

**Fase 4A - MVP (20 conectores):**
1. GitHub
2. Slack
3. Gmail
4. Google Sheets
5. Google Drive
6. Notion
7. Trello
8. Asana
9. Linear
10. Jira
11. Discord
12. Telegram
13. Twitter/X
14. LinkedIn
15. HubSpot
16. Salesforce
17. Stripe
18. PayPal
19. Shopify
20. Airtable

**Fase 4B - Expansã©©o (100 conectores):**
- Microsoft 365 (Outlook, Teams, OneDrive)
- Meta (Instagram, Facebook Pages)
- AWS services
- Zapier
- Make
- E mais 80...

**Fase 4C - Long Tail (1500+ conectores):**
- APIs públicas menos populares
- Webhooks gené©©ricos
- HTTP Request customizado

## Arquitetura do Sistema

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (Next.js)                    │
│  - Dashboard de integraç©£oes                             │
│  - Configuraç©©o de OAuth                                 │
│  - Execuç©©o de aç­­es                                   │
│  - Monitoramento de uso                                   │
└─────────────────────────────────────────────────────────┘
                              │
                              │ API REST / GraphQL
                              ▼
┌─────────────────────────────────────────────────────────┐
│                    API Gateway                           │
│  - Autenticaç©©o (JWT + API Keys)                        │
│  - Rate limiting                                        │
│  - Logging e auditoria                                  │
└─────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
    ┌─────────────────┐ ┌───────────┐ ┌──────────────┐
    │  Connector      │ │  OAuth    │ │   Webhook    │
    │  Executor       │ │  Manager  │ │   Receiver   │
    │                 │ │           │ │              │
    │ - GitHub        │ │ - Token   │ │ - Recebe     │
    │ - Slack         │ │   refresh │ │   webhooks   │
    │ - Gmail         │ │ - Storage │ │   externos   │
    │ - ...           │ │   seguro  │ │              │
    └─────────────────┘ └───────────┘ └──────────────┘
              │               │               │
              └───────────────┼───────────────┘
                              │
                              ▼
    ┌─────────────────────────────────────────────────────┐
    │                   Data Layer                         │
    │  - PostgreSQL (users, connections, executions)       │
    │  - Redis (cache, sessions, rate limit)               │
    │  - Vault/Encrypted storage (tokens OAuth)            │
    └─────────────────────────────────────────────────────┘
```

## Schema do Database

```sql
-- Users & Organizations
CREATE TABLE users (
  id UUID PRIMARY KEY,
  email TEXT UNIQUE,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE organizations (
  id UUID PRIMARY KEY,
  name TEXT,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE org_members (
  org_id UUID REFERENCES organizations(id),
  user_id UUID REFERENCES users(id),
  role TEXT, -- "owner", "admin", "member"
  PRIMARY KEY (org_id, user_id)
);

-- Connectors & Connections
CREATE TABLE connectors (
  id TEXT PRIMARY KEY, -- "github", "slack", etc.
  name TEXT,
  auth_type TEXT, -- "oauth2", "api_key"
  config JSONB -- OAuth URLs, scopes, etc.
);

CREATE TABLE connections (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id),
  connector_id TEXT REFERENCES connectors(id),
  account_name TEXT, -- ex: email do Gmail
  access_token_encrypted TEXT,
  refresh_token_encrypted TEXT,
  token_expires_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

-- Executions
CREATE TABLE executions (
  id UUID PRIMARY KEY,
  connection_id UUID REFERENCES connections(id),
  action_id TEXT,
  input JSONB,
  output JSONB,
  status TEXT, -- "pending", "success", "failed"
  error TEXT,
  started_at TIMESTAMP,
  completed_at TIMESTAMP
);

-- API Keys
CREATE TABLE api_keys (
  id UUID PRIMARY KEY,
  user_id UUID REFERENCES users(id),
  name TEXT,
  key_hash TEXT, -- bcrypt hash
  created_at TIMESTAMP DEFAULT NOW(),
  last_used_at TIMESTAMP,
  expires_at TIMESTAMP
);
```

## API Endpoints

### Autenticaç©©o
```
POST /api/auth/signup
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
```

### Conectores
```
GET  /api/connectors           -- Listar todos conectores
GET  /api/connectors/:id        -- Detalhes de um conector
GET  /api/connectors/:id/actions -- Aç©£oes disponí©©veis
```

### Conex­­es (OAuth)
```
GET  /api/connections          -- Listar conex­­es do usuá¡©rio
POST /api/connections/:connector/connect  -- Iniciar OAuth flow
GET  /api/connections/:connector/callback -- OAuth callback
DELETE /api/connections/:id    -- Remover conexã©©o
```

### Execuç©©o de Aç­­es
```
POST /api/execute              -- Executar açã©©o
GET  /api/executions           -- Histó¡©©rico de execu­­es
GET  /api/executions/:id        -- Detalhes da execuã©©o
```

### API Keys
```
POST /api/api-keys             -- Criar nova API key
GET  /api/api-keys             -- Listar API keys
DELETE /api/api-keys/:id       -- Revogar API key
```

### Webhooks
```
POST /api/webhooks/:connector/:event -- Receber webhook externo
```

## Pró¡©©ximos Passos

1. **Setup do Database** - PostgreSQL schema
2. **OAuth Manager** - Fluxo OAuth2 gené©©rico
3. **Connector SDK** - Base para implementar conectores
4. **API Routes** - Next.js API endpoints
5. **Dashboard UI** - Pá¡©ginas de conexã©©o e execuã©©o
