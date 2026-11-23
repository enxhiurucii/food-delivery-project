import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In development the Vite dev server plays nginx's role: same-origin proxy to both APIs (no CORS).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api/backoffice": {
        target: "http://localhost:8081",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/backoffice/, "/api"),
      },
      "/api/webshop": {
        target: "http://localhost:8082",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/webshop/, "/api"),
      },
    },
  },
});
