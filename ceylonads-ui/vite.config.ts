import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiBaseUrl = env.VITE_API_BASE_URL || "http://localhost:8080";

  return {
    plugins: [react()],
    server: {
      // The backend does not send CORS headers for local development, so the
      // dev server proxies API calls same-origin instead of the browser
      // hitting VITE_API_BASE_URL directly. See apiClient.ts.
      proxy: {
        "/api": { target: apiBaseUrl, changeOrigin: true },
        "/media": { target: apiBaseUrl, changeOrigin: true },
        // sitemap.xml is generated from live ad/category/location data, so it's served by the
        // backend (see SitemapController) but needs to live at the frontend's own origin/root -
        // the sitemap protocol only allows it to list URLs at or below its own path.
        "/sitemap.xml": { target: apiBaseUrl, changeOrigin: true },
      },
    },
  };
});
