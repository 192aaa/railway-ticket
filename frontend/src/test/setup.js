import { vi } from 'vitest'

// 生产构建里 ElMessage 由 unplugin-auto-import 注入；测试模式跳过了自动导入，
// 这里提供等价的全局 mock，便于断言与避免 ESM 注入干扰。
globalThis.ElMessage = {
  success: vi.fn(),
  error: vi.fn(),
  warning: vi.fn(),
  info: vi.fn()
}
