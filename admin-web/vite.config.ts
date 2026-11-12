import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
  build: {
    rollupOptions: {
      output: {
        // 拆分体积较大的第三方依赖，独立缓存、减小首屏主包体积。
        manualChunks: {
          react: ["react", "react-dom"],
          antd: ["antd"],
        },
      },
    },
  },
});
