import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'node:path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // 测试环境跳过自动导入（ElMessage 由 src/test/setup.js 全局 mock），避免 ESM 注入干扰
  const isTest = process.env.VITEST === 'true'

  return {
    plugins: [
      vue(),
      ...(isTest
        ? []
        : [
            AutoImport({ resolvers: [ElementPlusResolver()] }),
            Components({ resolvers: [ElementPlusResolver()] })
          ])
    ],
    test: {
      environment: 'happy-dom',
      setupFiles: ['./src/test/setup.js'],
      include: ['src/**/*.test.js'],
      clearMocks: true
    },
    resolve: {
      alias: {
        '@': resolve(process.cwd(), 'src')
      }
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_API_TARGET || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    build: {
      target: 'es2020',
      sourcemap: mode === 'development',
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (id.includes('node_modules')) {
              if (id.includes('element-plus') || id.includes('@element-plus')) {
                return 'element-plus'
              }
              if (id.includes('vue') || id.includes('pinia')) {
                return 'vue-vendor'
              }
              if (id.includes('axios')) {
                return 'axios'
              }
              return 'vendor'
            }
          }
        }
      }
    }
  }
})