import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: ["./src/test/setup.ts"],
    // React Testing Library's auto-cleanup-between-tests only registers when it finds a global
    // `afterEach`, so this needs to stay true even though test files import their own APIs.
    globals: true,
  },
});
