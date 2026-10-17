<template>
  <div class="auth-page">
    <div class="auth-bg-blob">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
    </div>
    <div class="auth-wrapper register">
      <div class="auth-card glass-card">
        <h2 class="auth-title">创建账户</h2>
        <p class="auth-sub">加入 RailFlow，开启便捷出行</p>

        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="submit" class="auth-form">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" size="large" :disabled="loading">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password :disabled="loading">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="realName">
            <el-input v-model="form.realName" placeholder="真实姓名" size="large" :disabled="loading">
              <template #prefix><el-icon><UserFilled /></el-icon></template>
            </el-input>
          </el-form-item>
          <div class="form-row">
            <el-form-item>
              <el-input v-model="form.idCard" placeholder="身份证号 (选填)" size="large" :disabled="loading" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="form.phone" placeholder="手机号 (选填)" size="large" :disabled="loading" />
            </el-form-item>
          </div>
          <el-form-item>
            <button type="button" class="submit-btn" :disabled="loading" @click="submit">
              <svg v-if="loading" class="spinner" viewBox="0 0 50 50">
                <circle class="path" cx="25" cy="25" r="20" fill="none" stroke="currentColor" stroke-width="5"/>
              </svg>
              <span>{{ loading ? '注册中...' : '注 册' }}</span>
            </button>
          </el-form-item>
        </el-form>

        <div class="auth-links">
          <router-link to="/login" class="link">已有账号？立即登录 →</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, UserFilled } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  realName: '',
  idCard: '',
  phone: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await request.post('/user/register', form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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

.auth-wrapper.register {
  max-width: 480px;
  width: 100%;
  animation: slideUp 0.7s ease-out;
}

.auth-card {
  padding: 40px;
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
  margin-bottom: 24px;
}

.auth-form {
  animation: formStagger 0.6s ease-out 0.15s both;
}

.auth-form .el-form-item:nth-child(1) { animation: itemIn 0.5s ease-out 0.1s both; }
.auth-form .el-form-item:nth-child(2) { animation: itemIn 0.5s ease-out 0.25s both; }
.auth-form .el-form-item:nth-child(3) { animation: itemIn 0.5s ease-out 0.4s both; }
.auth-form .el-form-item:nth-child(4) { animation: itemIn 0.5s ease-out 0.55s both; }
.auth-form .el-form-item:nth-child(5) { animation: itemIn 0.5s ease-out 0.7s both; }

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
  margin-bottom: 18px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
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
  letter-spacing: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
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

.spinner {
  animation: spin 1s linear infinite;
  width: 18px;
  height: 18px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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
}

.link:hover {
  color: #93c5fd;
}

@media (max-width: 500px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>