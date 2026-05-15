import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Empty prefix: load every key from `.env*` so local overrides (not only `VITE_*`) apply to proxy targets.
  const env = loadEnv(mode, process.cwd(), '')

  const financeTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'
  const marketTarget = env.VITE_MARKET_PROXY_TARGET || 'http://localhost:8082'
  /**
   * When true, all `/api` uses `VITE_PROXY_TARGET` only (API gateway or BFF).
   * Required for Docker Compose frontend (api-gateway hostname); do not use split routing there.
   */
  const useSingleApiProxy =
    env.VITE_DEV_PROXY_GATEWAY === '1' || env.VITE_DEV_PROXY_GATEWAY === 'true'

  const singleApiProxy = {
    '/api': {
      target: financeTarget,
      changeOrigin: true,
      secure: false,
    },
  }

  /**
   * Default: all `/api` → finance-api; finance-api proxies /api/market/* to market-data-service
   * (see MarketDataProxyController). Optional direct MDS routing for debugging only.
   */
  const useDirectMarketProxy =
    env.VITE_DEV_MARKET_DIRECT_TO_MDS === '1' || env.VITE_DEV_MARKET_DIRECT_TO_MDS === 'true'

  const splitApiProxy = useDirectMarketProxy
    ? {
        '/api/market/overview': {
          target: financeTarget,
          changeOrigin: true,
          secure: false,
        },
        '/api/market/insights': {
          target: financeTarget,
          changeOrigin: true,
          secure: false,
        },
        // Fundamentals are proxied by finance-api; direct MDS would 404 on catalog gaps.
        '/api/market/instruments': {
          target: financeTarget,
          changeOrigin: true,
          secure: false,
        },
        '/api/market': {
          target: marketTarget,
          changeOrigin: true,
          secure: false,
        },
        '/api/rates': {
          target: marketTarget,
          changeOrigin: true,
          secure: false,
        },
        '/api': {
          target: financeTarget,
          changeOrigin: true,
          secure: false,
        },
      }
    : singleApiProxy

  // eslint-disable-next-line no-console -- dev-only proxy diagnostics
  console.info(
    `[vite] /api proxy → ${financeTarget}${
      useSingleApiProxy
        ? ' (gateway mode)'
        : useDirectMarketProxy
          ? `, /api/market (except overview/insights) → ${marketTarget} (direct MDS)`
          : ' (market via finance-api BFF)'
    }`,
  )

  return {
    plugins: [react()],
    server: {
      proxy: useSingleApiProxy ? singleApiProxy : splitApiProxy,
    },
  }
})
