<template>
  <div class="auth-page">
    <div class="auth-bg-blob">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
    </div>
    <div class="auth-wrapper">
      <div class="auth-left">
        <svg class="hero-train" viewBox="0 0 200 120" fill="none" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="g1" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#60a5fa"/>
              <stop offset="100%" stop-color="#a78bfa"/>
            </linearGradient>
          </defs>
          <rect x="20" y="30" width="160" height="55" rx="10" fill="url(#g1)"/>
          <rect x="32" y="42" width="28" height="22" rx="4" fill="#fff" opacity="0.9"/>
          <rect x="68" y="42" width="28" height="22" rx="4" fill="#fff" opacity="0.9"/>
          <rect x="104" y="42" width="28" height="22" rx="4" fill="#fff" opacity="0.9"/>
          <rect x="140" y="42" width="24" height="22" rx="4" fill="#fff" opacity="0.9"/>
          <circle cx="40" cy="100" r="10" fill="#0f172a" stroke="#60a5fa" stroke-width="3"/>
          <circle cx="160" cy="100" r="10" fill="#0f172a" stroke="#60a5fa" stroke-width="3"/>
          <line x1="0" y1="115" x2="200" y2="115" stroke="#475569" stroke-width="4" stroke-linecap="round"/>
        </svg>
        <h1 class="auth-slogan">
          <span class="gradient-text">旅程的起点，</span>
          <br/>
          <span>从这里开启</span>
        </h1>
        <p class="auth-desc">实时余票 · 极速购票 · 安心出行</p>
      </div>

      <div class="auth-card glass-card">
        <h2 class="auth-title">欢迎回来</h2>
        <p class="auth-sub">登录你的账户继续购票</p>

        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="submit" class="auth-form">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" size="large">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <button
              type="button"
              class="submit-btn"
              :disabled="loading"
              @click="submit"
            >
              <svg v-if="loading" class="spinner" viewBox="0 0 50 50">
                <circle class="path" cx="25" cy="25" r="20" fill="none" stroke="currentColor" stroke-width="5"/>
              </svg>
              <span>{{ loading ? '登录中...' : '登 录' }}</span>
            </button>
          </el-form-item>
        </el-form>

        <div class="auth-links">
          <router-link to="/register" class="link">没有账号？立即注册 →</router-link>
        </div>

        <div class="test-hint">
          <el-icon><InfoFilled /></el-icon>
          测试账号: admin / 123456
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, InfoFilled } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect || '/home'
    router.replace(redirect)
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  position: relative;
  overflow: hidden;
}

.auth-wrapper {
  display: grid;
  grid-template-columns: 1fr 440px;
  gap: 60px;
  max-width: 1000px;
  width: 100%;
  position: relative;
  z-index: 1;
  animation: slideUp 0.7s ease-out;
}

.auth-left {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 24px;
  text-align: left;
}

.hero-train {
  width: 180px;
  height: auto;
  filter: drop-shadow(0 10px 30px rgba(59, 130, 246, 0.3));
  animation: trainMove 4s ease-in-out infinite;
}

.auth-slogan {
  font-size: 40px;
  font-weight: 800;
  line-height: 1.2;
  color: #f1f5f9;
  letter-spacing: -1px;
}

.gradient-text {
  background: linear-gradient(135deg, #60a5fa 0%, #a78bfa 50%, #f472b6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-desc {
  font-size: 16px;
  color: #94a3b8;
}

.auth-card {
  padding: 48px 40px;
}

.auth-title {
  font-size: 28px;
  font-weight: 800;
  color: #f1f5f9;
  margin-bottom: 8px;
}

.auth-sub {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 28px;
}

.auth-form {
  animation: formStagger 0.6s ease-out 0.15s both;
}

.auth-form .el-form-item:nth-child(1) { animation: itemIn 0.5s ease-out 0.15s both; }
.auth-form .el-form-item:nth-child(2) { animation: itemIn 0.5s ease-out 0.3s both; }
.auth-form .el-form-item:nth-child(3) { animation: itemIn 0.5s ease-out 0.45s both; }

@keyframes itemIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

.auth-form :deep(.el-input__wrapper) {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 12px;
  box-shadow: none !important;
  padding: 4px 14px;
  transition: all 0.3s ease;
}

.auth-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(96, 165, 250, 0.4);
  background: rgba(15, 23, 42, 0.75);
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: transparent !important;
  background: rgba(15, 23, 42, 0.85);
  box-shadow:
    0 0 0 2px rgba(59, 130, 246, 0.25),
    0 0 25px rgba(59, 130, 246, 0.2),
    inset 0 0 20px rgba(59, 130, 246, 0.08) !important;
}

.auth-form :deep(.el-input__inner) {
  color: #e2e8f0;
  font-size: 15px;
}

.auth-form :deep(.el-input__inner::placeholder) {
  color: #64748b;
}

.auth-form :deep(.el-input__prefix .el-icon) {
  color: #64748b;
  transition: color 0.2s;
}

.auth-form :deep(.el-input__wrapper.is-focus .el-input__prefix .el-icon) {
  color: #60a5fa;
}

.auth-form .el-form-item {
  margin-bottom: 20px;
}

.submit-btn {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  color: white;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  cursor: pointer;
  box-shadow: 0 6px 25px rgba(59, 130, 246, 0.4);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  letter-spacing: 2px;
  position: relative;
  overflow: hidden;
}

.submit-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.25), transparent);
  transition: left 0.6s ease;
}

.submit-btn:hover:not(:disabled)::before {
  left: 100%;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 35px rgba(59, 130, 246, 0.55), 0 0 40px rgba(139, 92, 246, 0.25);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-links {
  margin-top: 12px;
  text-align: center;
}

.link {
  color: #60a5fa;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s;
}

.link:hover {
  color: #93c5fd;
}

.test-hint {
  margin-top: 28px;
  padding: 12px 16px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px dashed rgba(59, 130, 246, 0.3);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  color: #60a5fa;
}

.spinner {
  animation: spin 1s linear infinite;
  width: 20px;
  height: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 800px) {
  .auth-wrapper {
    grid-template-columns: 1fr;
  }
  .auth-left {
    display: none;
  }
}
</style>