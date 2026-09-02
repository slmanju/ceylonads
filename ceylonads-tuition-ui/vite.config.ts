import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiBaseUrl = env.VITE_API_BASE_URL || "http://localhost:8080";

  return {
    plugins: [react()],
    server: {
      port: 5174,
      // The backend does not send CORS headers for local development, so the dev server proxies
      // API calls same-origin instead of the browser hitting VITE_API_BASE_URL directly.
      proxy: {
        "/api": { target: apiBaseUrl, changeOrigin: true },
        "/media": { target: apiBaseUrl, changeOrigin: true },
      },
    },
  };
});
