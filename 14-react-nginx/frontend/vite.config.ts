import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // 개발 서버(npm run dev)에서도 /api 를 백엔드로 넘긴다. 운영에서는 Nginx 가 같은 역할을 한다
    proxy: { "/api": "http://localhost:8090" },
  },
});
