<template>
  <div class="not-found">
    <div class="nf-inner">
      <div class="nf-num">
        <span>4</span>
        <svg class="nf-train" viewBox="0 0 100 60" fill="none">
          <rect x="5" y="15" width="90" height="35" rx="6" fill="url(#nf-g)"/>
          <rect x="15" y="22" width="18" height="14" rx="2" fill="#fff" opacity="0.9"/>
          <rect x="40" y="22" width="18" height="14" rx="2" fill="#fff" opacity="0.9"/>
          <rect x="65" y="22" width="18" height="14" rx="2" fill="#fff" opacity="0.9"/>
          <circle cx="22" cy="52" r="6" fill="#0f172a" stroke="#60a5fa" stroke-width="2.5"/>
          <circle cx="78" cy="52" r="6" fill="#0f172a" stroke="#60a5fa" stroke-width="2.5"/>
          <defs>
            <linearGradient id="nf-g" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stop-color="#60a5fa"/>
              <stop offset="100%" stop-color="#a78bfa"/>
            </linearGradient>
          </defs>
        </svg>
        <span>4</span>
      </div>
      <h1 class="nf-title">列车开过头啦</h1>
      <p class="nf-sub">抱歉，您要找的页面不在当前线路上</p>
      <button class="nf-btn" @click="$router.push('/home')">
        <el-icon><HomeFilled /></el-icon>
        返回首页
      </button>
    </div>
  </div>
</template>

<script setup>
import { HomeFilled } from '@element-plus/icons-vue'
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 32px;
}

.nf-inner {
  animation: slideUp 0.7s ease-out;
}

.nf-num {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
}

.nf-num span {
  font-size: 120px;
  font-weight: 900;
  background: linear-gradient(135deg, #60a5fa 0%, #a78bfa 50%, #f472b6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.nf-train {
  width: 100px;
  height: 60px;
  animation: trainMove 3s ease-in-out infinite;
}

.nf-title {
  font-size: 32px;
  font-weight: 800;
  color: #f1f5f9;
  margin-bottom: 8px;
}

.nf-sub {
  color: #64748b;
  font-size: 16px;
  margin-bottom: 32px;
}

.nf-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 14px 36px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  color: white;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  cursor: pointer;
  box-shadow: 0 6px 25px rgba(59, 130, 246, 0.4);
  transition: all 0.3s ease;
}

.nf-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 35px rgba(59, 130, 246, 0.55);
}
</style>