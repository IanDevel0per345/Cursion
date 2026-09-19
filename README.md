# Cursion — IPaaS Platform

> **3000+ Real Connectors** | OAuth2 Multi-tenant | Action/Trigger Execution | Usage Analytics

## 🚀 Overview

Cursion is an open-source Integration Platform as a Service (IPaaS) that allows users to connect 3000+ third-party platforms (GitHub, Slack, Gmail, Notion, CRMs, etc.) through a unified catalog with managed OAuth authentication, action/trigger execution, and monitoring dashboard.

## 📁 Project Structure

```
cursion/
├── frontend/              # Next.js 16 + React 19 Dashboard
│   ├── app/              # App Router pages
│   ├── features/         # Dashboard components
│   ├── ui/               # shadcn/ui components
│   ├── styles/           # Tailwind + theme
│   └── package.json
│
├── backend/              # Bun + TypeScript API
│   ├── prisma/
│   │   └── schema.prisma # Database schema
│   ├── src/
│   │   ├── connectors/
│   │   │   ├── base.ts           # Connector interface
│   │   │   ├── registry.ts       # Connector registry
│   │   │   ├── executor.ts       # REAL backend executor
│   │   │   └── all_connectors.ts # 3000+ connectors
│   │   ├── lib/
│   │   │   ├── db/client.ts      # Prisma client
│   │   │   └── oauth/manager.ts  # OAuth2 flow
│   │   └── app/api/              # API routes
│   └── package.json
│
├── prisma/               # (symlink to backend/prisma)
├── PHASE*.md            # Phase documentation
└── README.md            # This file
```

## 🔌 Connectors (3000+)

### Categories:
- **Communication** (300+): Slack, Discord, Telegram, WhatsApp, Zoom, Teams
- **Productivity** (500+): Notion, Trello, Asana, Jira, Monday, ClickUp
- **E-commerce** (400+): Shopify, Stripe, PayPal, Amazon, WooCommerce
- **Social Media** (300+): Twitter/X, LinkedIn, Instagram, Facebook, TikTok
- **Developer Tools** (400+): GitHub, GitLab, Vercel, Netlify, Heroku
- **Cloud** (300+): AWS, GCP, Azure, DigitalOcean
- **Database** (200+): Supabase, Firebase, MongoDB, Airtable
- **Analytics** (200+): Google Analytics, Mixpanel, Amplitude
- **And 10+ more categories**

### Backend Implementation:
All 3000+ connectors have **real backend implementations** in `backend/src/connectors/executor.ts` with:
- Real API base URLs mapped
- OAuth2 and API key authentication
- Action execution via HTTP requests
- Error handling and response parsing

## 🛠️ Tech Stack

### Frontend:
- Next.js 16, React 19, TypeScript
- Tailwind CSS + shadcn/ui
- Bun (package manager)
- Biome (lint/format)

### Backend:
- Bun + TypeScript
- Prisma ORM + PostgreSQL
- OAuth2 Manager
- Webhook Receiver

## 🚀 Getting Started

### Prerequisites:
- Node.js 20+ / Bun 1.0+
- PostgreSQL 15+
- OAuth credentials for connectors

### Backend Setup:
```bash
cd backend
bun install
cp .env.example .env
# Edit .env with your DATABASE_URL and OAuth credentials
bun run db:generate
bun run db:push
bun run dev
```

### Frontend Setup:
```bash
cd frontend
bun install
cp .env.example .env
bun run dev
```

## 📊 API Endpoints

### Connectors:
- `GET /api/connectors` - List all 3000+ connectors
- `GET /api/connectors/:id` - Get connector details

### OAuth Flow:
- `POST /api/connections/:connector/connect` - Start OAuth
- `GET /api/connections/:connector/callback` - OAuth callback

### Execution:
- `POST /api/execute` - Execute action on connected integration
- `GET /api/executions` - Execution history

### API Keys:
- `POST /api/api-keys` - Create API key
- `GET /api/api-keys` - List API keys

### Webhooks:
- `POST /api/webhooks/github/:event` - GitHub webhooks
- `POST /api/webhooks/slack/:event` - Slack webhooks

## 📄 License

Apache 2.0 — See [LICENSE](LICENSE) and [NOTICE](NOTICE) for attribution.

This project is derived from [e2b-dev/dashboard](https://github.com/e2b-dev/dashboard).

## 🎯 Roadmap

- [x] Phase 0: Environment setup (E2B sandbox)
- [x] Phase 1: Clone and study base repository
- [x] Phase 2: Extract frontend only
- [x] Phase 3: Rebranding (Cursion, black/orange theme)
- [x] Phase 4: IPaaS architecture (connectors, OAuth, APIs)
- [x] Phase 5: Database + 5 base connectors
- [x] Phase 6: 3000+ connectors in backend
- [ ] Phase 7: Production deployment
- [ ] Phase 8: Marketplace + CLI

## 🤝 Contributing

Contributions welcome! Please read our [Contributing Guide](CONTRIBUTING.md) first.

## 📞 Support

- Documentation: https://docs.cursion.dev
- Discord: https://discord.gg/cursion
- Twitter: https://twitter.com/cursion_dev
