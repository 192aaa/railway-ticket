import js from '@eslint/js'
import vue from 'eslint-plugin-vue'
import globals from 'globals'
import prettier from 'eslint-config-prettier'

export default [
  {
    ignores: [
      'dist/**',
      'node_modules/**',
      'auto-imports.d.ts',
      'components.d.ts',
      '*.log',
      'vite-*.txt'
    ]
  },
  js.configs.recommended,
  ...vue.configs['flat/recommended'],
  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node
      }
    },
    rules: {
      // unplugin-auto-import 会在编译期注入 Vue API 与 Element Plus 组件，
      // 源码中未显式 import 属正常，关闭 no-undef 避免大量误报
      'no-undef': 'off',
      // Vue 模板中的自动导入指令/组件同样由构建插件解析
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'off', // AI 助手对话内容已在前端做基本转义，场景受控
      // 项目中大量空 catch 用于缓存/AI 降级，允许但要求有注释说明意图（此处统一允许）
      'no-empty': ['error', { allowEmptyCatch: true }],
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }]
    }
  },
  prettier
]
