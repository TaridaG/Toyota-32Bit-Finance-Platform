# frontend-web

React 19 + TypeScript portal UI (Vite).

<p align="center">
  <a href="../README.tr.md">Türkçe</a> · <a href="../README.md">English</a> · <a href="../README.de.md">Deutsch</a>
</p>

## Commands

```bash
npm install
npm run dev      # http://localhost:5173
npm run build    # tsc -b && vite build
npm run preview  # production bundle preview
```

## Environment files

| File | When |
|------|------|
| [`Docker/.env`](../Docker/.env.example) | Full Docker stack: **required** `TCMB_API_KEY`, `FINNHUB_API_KEY`; optional SMTP — [getting-started](../docs/english/getting-started.md) |
| [`.env.development`](.env.example) | Local `npm run dev` or hybrid proxy only (`cp .env.example .env.development`) |

When using Docker at http://localhost:5173 (nginx prod), `.env.development` is **not** required.

Setup and proxy details: [docs/english/services/frontend-web.md](../docs/english/services/frontend-web.md) · [docs/turkce/services/frontend-web.md](../docs/turkce/services/frontend-web.md) · [docs/deutsch/services/frontend-web.md](../docs/deutsch/services/frontend-web.md).
