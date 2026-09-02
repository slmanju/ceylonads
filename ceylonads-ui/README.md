# CeylonAds — Frontend (Phase 1)

React + Vite + TypeScript public marketplace frontend for CeylonAds, built against the
Spring Boot backend's OpenAPI contract (`/v3/api-docs`).

## Stack

- React 19, Vite, TypeScript
- React Router
- Axios
- react-icons

## Setup

```bash
cp .env.example .env.local
npm install
npm run dev
```

The backend must be running at the URL configured by `VITE_API_BASE_URL`
(default `http://localhost:8080`):

```bash
cd ../CeylonAds-API
gradle bootRun
```

## Local dev and CORS

The backend does not send CORS headers, so the browser cannot call it directly
cross-origin. The Vite dev server proxies `/api` and `/media` to
`VITE_API_BASE_URL` (see `vite.config.ts`), and `apiClient.ts` uses a relative
base URL in dev so requests go through that proxy. In a production build,
`apiClient.ts` calls `VITE_API_BASE_URL` directly — the backend will need CORS
configured (or requests proxied by a reverse proxy) before that works from a
browser.

## Scripts

```bash
npm run dev      # start dev server (http://localhost:5173)
npm run build    # type-check and build for production
npm run preview  # preview the production build
```

## Demo accounts (local backend seed data)

| Role | Username | Password |
|---|---|---|
| ADMIN | `admin` | `admin123` |
| CUSTOMER | `kamal` | `customer123` |
| CUSTOMER | `nimal` | `customer123` |
