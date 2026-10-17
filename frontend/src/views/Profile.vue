<template>
  <div class="profile">
    <AppNav />

    <section class="hero-fade"></section>

    <section class="profile-main">
      <div class="profile-header glass-card">
        <div class="avatar-wrap">
          <div class="avatar" :style="{ background: avatarGradient }">
            {{ (userInfo?.realName || userStore.username)?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div class="avatar-ring"></div>
        </div>
        <div class="user-info">
          <div class="user-name">
            {{ userInfo?.realName || userStore.username }}
            <span class="user-tag">Member</span>
          </div>
          <div class="user-meta">
            <span class="meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              {{ userStore.username }}
            </span>
            <span class="meta-dot">·</span>
            <span v-if="userInfo?.phone" class="meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
              {{ maskPhone(userInfo.phone) }}
            </span>
            <span v-if="userInfo?.idCard" class="meta-dot">·</span>
            <span v-if="userInfo?.idCard" class="meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="5" width="20" height="14" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>
              {{ maskIdCard(userInfo.idCard) }}
            </span>
            <span class="meta-dot">·</span>
            <span class="meta-item">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
              加入 {{ formatDate(userInfo?.createTime) }}
            </span>
          </div>
        </div>
        <div v-if="userStore.isLoggedIn" class="header-actions">
          <button class="action-btn" @click="openEditProfile">编辑资料</button>
          <button class="action-btn ghost" @click="openChangePwd">修改密码</button>
        </div>
        <button v-else class="ph-login" @click="$router.push('/login')">登录 / 注册</button>
      </div>

      <div class="stats-row stagger-children">
        <div
          class="stat-card glass-card clickable"
          v-for="(s, i) in statsList"
          :key="s.label"
          :style="{ animationDelay: (i * 0.06) + 's' }"
          @click="s.route && $router.push(s.route)"
          :class="{ disabled: !s.route }"
        >
          <div class="stat-icon" :style="{ background: s.bg }">{{ s.icon }}</div>
          <div class="stat-info">
            <div class="stat-num">{{ animatedNums[s.label] !== undefined ? animatedNums[s.label] : s.value }}</div>
            <div class="stat-label">{{ s.label }}</div>
          </div>
          <div v-if="s.route" class="stat-arrow">→</div>
        </div>
      </div>

      <div class="sections">
        <div class="section-card glass-card">
          <div class="section-header">
            <h3>⭐ 收藏车次</h3>
            <button v-if="favorites.length" class="section-clear" @click="clearAllFavs">清空</button>
          </div>
          <div v-if="favorites.length" class="fav-list">
            <div v-for="(t, i) in favorites" :key="i" class="fav-item" @click="jumpToRoute(t)">
              <div class="fav-num">{{ t.trainNumber }}</div>
              <div class="fav-route">
                <span>{{ t.fromStation }}</span>
                <span class="fav-arrow">→</span>
                <span>{{ t.toStation }}</span>
              </div>
              <div class="fav-time">{{ formatShortTime(t.startTime) }}</div>
              <button class="fav-del" @click.stop="removeFav(t)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6"/></svg>
              </button>
            </div>
          </div>
          <div v-else class="section-empty">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
            <p>还没有收藏车次，去首页星标常用车次吧</p>
            <button class="empty-action" @click="$router.push('/home')">去首页购票 →</button>
          </div>
        </div>

        <div class="section-card glass-card">
          <div class="section-header">
            <h3>👥 常用购票人</h3>
            <button class="section-add" @click="openAddPax">+ 添加</button>
          </div>
          <div v-if="savedPassengers.length" class="pax-list">
            <div v-for="(p, i) in savedPassengers" :key="i" class="pax-item">
              <div class="pax-avatar">{{ p.name?.charAt(0) || '?' }}</div>
              <div class="pax-info">
                <div class="pax-name">{{ p.name }}</div>
                <div class="pax-id">{{ p.idCard }}</div>
              </div>
              <button class="pax-del" @click="removePax(i)">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6"/></svg>
              </button>
            </div>
          </div>
          <div v-else class="section-empty">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/></svg>
            <p>暂无常用购票人，添加后购票时可快速选择</p>
            <button class="empty-action" @click="openAddPax">+ 添加购票人</button>
          </div>
        </div>

        <div class="section-card glass-card">
          <div class="section-header">
            <h3>📋 最近订单</h3>
            <router-link to="/orders" class="section-more">查看全部 →</router-link>
          </div>
          <div v-if="recentOrders.length" class="recent-list">
            <div v-for="o in recentOrders" :key="o.id" class="recent-item" @click="$router.push({ path: '/orders', query: { highlight: o.id } })">
              <div class="r-top">
                <span class="r-train">{{ o.trainNumber }}</span>
                <span class="r-status" :class="statusClass(o.status)">{{ statusLabel(o.status) }}</span>
              </div>
              <div class="r-route">
                <span>{{ o.fromStation }}</span>
                <span class="r-arrow">→</span>
                <span>{{ o.toStation }}</span>
              </div>
              <div class="r-bottom">
                <span class="r-date">{{ o.trainDate }}</span>
                <span class="r-price">¥{{ o.price }}</span>
              </div>
            </div>
          </div>
          <div v-else class="section-empty">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            <p>还没有订单记录</p>
            <button class="empty-action" @click="$router.push('/home')">去购票 →</button>
          </div>
        </div>
      </div>
    </section>

    <el-dialog v-model="paxDialogVisible" title="添加购票人" width="420px" :close-on-click-modal="false">
      <el-form :model="paxForm" label-width="80px" label-position="left">
        <el-form-item label="姓名">
          <el-input v-model="paxForm.name" placeholder="请输入真实姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="paxForm.idCard" placeholder="请输入 18 位身份证号" maxlength="18" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="pax-btn cancel" @click="paxDialogVisible = false">取消</button>
        <button class="pax-btn confirm" @click="confirmAddPax">保存</button>
      </template>
    </el-dialog>

    <el-dialog v-model="editProfileVisible" title="编辑个人资料" width="460px" :close-on-click-modal="false">
      <el-form :model="profileForm" label-width="90px" label-position="left">
        <el-form-item label="用户名">
          <el-input :model-value="userStore.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" placeholder="请输入 11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="profileForm.idCard" placeholder="请输入 18 位证件号" maxlength="18" />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="pax-btn cancel" @click="editProfileVisible = false">取消</button>
        <button class="pax-btn confirm" :disabled="profileSaving" @click="saveProfile">
          {{ profileSaving ? '保存中...' : '保存' }}
        </button>
      </template>
    </el-dialog>

    <el-dialog v-model="changePwdVisible" title="修改密码" width="420px" :close-on-click-modal="false">
      <el-form :model="pwdForm" label-width="90px" label-position="left">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="至少 6 位" show-password />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <button class="pax-btn cancel" @click="changePwdVisible = false">取消</button>
        <button class="pax-btn confirm" :disabled="pwdSaving" @click="savePassword">
          {{ pwdSaving ? '提交中...' : '确认修改' }}
        </button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user.js'
import request from '../utils/request'
import AppNav from '../components/AppNav.vue'

const userStore = useUserStore()
const router = useRouter()
const uk = (k) => `rf_${userStore.username || 'guest'}_${k}`

const avatarGradient = computed(() => {
  const colors = [
    'linear-gradient(135deg, #3b82f6, #8b5cf6)',
    'linear-gradient(135deg, #f472b6, #8b5cf6)',
    'linear-gradient(135deg, #06b6d4, #3b82f6)',
    'linear-gradient(135deg, #fbbf24, #f97316)',
    'linear-gradient(135deg, #10b981, #06b6d4)'
  ]
  const hash = (userStore.username || '').split('').reduce((a, c) => a + c.charCodeAt(0), 0)
  return colors[hash % colors.length]
})

const orders = ref([])
const recentOrders = computed(() => orders.value.slice(0, 5))

const totalSpent = computed(() => {
  let total = 0
  for (const o of orders.value) {
    if (o.status === 'PAID' || o.status === '已支付') {
      total += (o.price || 0)
    }
  }
  return total.toFixed(0)
})

const statsList = computed(() => {
  let paid = 0, refunded = 0, pending = 0
  for (const o of orders.value) {
    if (o.status === 'PAID' || o.status === '已支付') paid++
    else if (o.status === 'REFUNDED' || o.status === '已退票') refunded++
    else pending++
  }
  return [
    { label: '全部订单', value: orders.value.length, icon: '📋', bg: 'rgba(59,130,246,0.15)', route: '/orders' },
    { label: '有效订单', value: paid, icon: '✅', bg: 'rgba(16,185,129,0.15)', route: '/orders?status=PAID' },
    { label: '已退票', value: refunded, icon: '↩️', bg: 'rgba(249,115,22,0.15)', route: '/orders?status=REFUNDED' },
    { label: '总消费', value: '¥' + totalSpent.value, icon: '💰', bg: 'rgba(251,191,36,0.15)', route: null }
  ]
})

const animatedNums = reactive({})

function startCounter(label, target, duration = 900) {
  if (typeof target !== 'number') { animatedNums[label] = target; return }
  animatedNums[label] = 0
  const start = performance.now()
  function tick(now) {
    const p = Math.min((now - start) / duration, 1)
    const eased = 1 - Math.pow(1 - p, 3)
    animatedNums[label] = Math.round(target * eased)
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

async function loadOrders() {
  try {
    const list = await request.get('/order/my')
    orders.value = Array.isArray(list) ? list : []
  } catch {}
}

const favorites = ref([])

function loadFavs() {
  try {
    const raw = localStorage.getItem(uk('favorites'))
    if (raw) favorites.value = JSON.parse(raw)
  } catch {}
}

function removeFav(t) {
  favorites.value = favorites.value.filter(f => !(f.trainNumber === t.trainNumber && f.startTime === t.startTime))
  localStorage.setItem(uk('favorites'), JSON.stringify(favorites.value))
  ElMessage.success('已取消收藏')
}

function clearAllFavs() {
  ElMessageBox.confirm('确定清空全部收藏车次？', '确认', { type: 'warning' })
    .then(() => {
      favorites.value = []
      localStorage.removeItem(uk('favorites'))
      ElMessage.success('已清空')
    }).catch(() => {})
}

function jumpToRoute(t) {
  localStorage.setItem(uk('quick_search'), JSON.stringify({
    from: t.fromStation,
    to: t.toStation
  }))
  router.push('/home')
}

const savedPassengers = ref([])

function loadPassengers() {
  try {
    const raw = localStorage.getItem(uk('passengers'))
    if (raw) savedPassengers.value = JSON.parse(raw)
  } catch {}
}

function removePax(i) {
  savedPassengers.value.splice(i, 1)
  localStorage.setItem(uk('passengers'), JSON.stringify(savedPassengers.value))
  ElMessage.success('已删除')
}

const paxDialogVisible = ref(false)
const paxForm = reactive({ name: '', idCard: '' })

function openAddPax() {
  paxForm.name = ''
  paxForm.idCard = ''
  paxDialogVisible.value = true
}

function confirmAddPax() {
  const name = paxForm.name.trim()
  const idCard = paxForm.idCard.trim()
  if (!name) { ElMessage.warning('请输入姓名'); return }
  if (!/^\d{17}[\dXx]$/.test(idCard)) { ElMessage.warning('身份证号格式不正确'); return }
  if (savedPassengers.value.some(p => p.idCard === idCard)) {
    ElMessage.warning('该乘车人已存在'); return
  }
  savedPassengers.value.push({ name, idCard })
  localStorage.setItem(uk('passengers'), JSON.stringify(savedPassengers.value))
  paxDialogVisible.value = false
  ElMessage.success('添加成功')
}

function formatDate(t) {
  if (!t) return '今天'
  try {
    const d = new Date(typeof t === 'string' ? t.replace(' ', 'T') : t)
    if (isNaN(d.getTime())) return t
    return `${d.getFullYear()}`
  } catch { return t }
}

function formatShortTime(t) {
  if (!t) return ''
  return t.split('T')[1]?.substring(0, 5) || t
}

function statusClass(s) {
  if (s === 'PAID' || s === '已支付') return 'paid'
  if (s === 'REFUNDED' || s === '已退票') return 'refunded'
  return 'pending'
}

function statusLabel(s) {
  if (s === 'PAID') return '已支付'
  if (s === 'REFUNDED') return '已退票'
  if (s === 'PENDING') return '待支付'
  return s || ''
}

const userInfo = computed(() => userStore.userInfo || {})

function maskPhone(phone) {
  if (!phone || phone.length < 7) return phone || ''
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

function maskIdCard(id) {
  if (!id || id.length < 10) return id || ''
  return id.slice(0, 4) + '**********' + id.slice(-4)
}

const editProfileVisible = ref(false)
const profileSaving = ref(false)
const profileForm = reactive({ realName: '', phone: '', idCard: '' })

function openEditProfile() {
  profileForm.realName = userInfo.value.realName || ''
  profileForm.phone = userInfo.value.phone || ''
  profileForm.idCard = userInfo.value.idCard || ''
  editProfileVisible.value = true
}

async function saveProfile() {
  if (!profileForm.realName.trim()) { ElMessage.warning('真实姓名不能为空'); return }
  if (profileForm.phone && !/^1[3-9]\d{9}$/.test(profileForm.phone.trim())) { ElMessage.warning('手机号格式不正确'); return }
  if (profileForm.idCard && !/^\d{15}$|^\d{17}[\dXx]$/.test(profileForm.idCard.trim())) { ElMessage.warning('身份证号格式不正确'); return }

  profileSaving.value = true
  try {
    await request.put('/user/profile', {
      realName: profileForm.realName.trim(),
      phone: profileForm.phone.trim(),
      idCard: profileForm.idCard.trim()
    })
    await userStore.fetchUserInfo()
    ElMessage.success('资料已更新')
    editProfileVisible.value = false
  } catch (e) {} finally {
    profileSaving.value = false
  }
}

const changePwdVisible = ref(false)
const pwdSaving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

function openChangePwd() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  changePwdVisible.value = true
}

async function savePassword() {
  if (!pwdForm.oldPassword) { ElMessage.warning('请输入原密码'); return }
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) { ElMessage.warning('新密码至少 6 位'); return }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) { ElMessage.warning('两次输入的新密码不一致'); return }
  if (pwdForm.oldPassword === pwdForm.newPassword) { ElMessage.warning('新密码不能与原密码相同'); return }

  pwdSaving.value = true
  try {
    await request.post('/user/change-password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    changePwdVisible.value = false
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 1200)
  } catch (e) {} finally {
    pwdSaving.value = false
  }
}

onMounted(async () => {
  if (userStore.isLoggedIn) {
    await userStore.fetchUserInfo()
    await loadOrders()
  }
  loadFavs()
  loadPassengers()
  requestAnimationFrame(() => {
    statsList.value.forEach((s, i) => {
      setTimeout(() => startCounter(s.label, s.value, 900 + i * 80), i * 100)
    })
  })
})
</script>

<style scoped>
.profile {
  min-height: 100vh;
}

.hero-fade {
  height: 180px;
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.12) 0%, transparent 100%);
  pointer-events: none;
}

.profile-main {
  max-width: 1100px;
  margin: -100px auto 60px;
  padding: 0 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 28px 32px;
  position: relative;
  overflow: hidden;
}

.profile-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(96, 165, 250, 0.5), transparent);
}

.avatar-wrap {
  position: relative;
  transition: transform 0.4s ease;
}

.avatar-wrap:hover {
  transform: scale(1.05);
}

.avatar {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 800;
  color: white;
  letter-spacing: 1px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3), 0 0 20px rgba(59, 130, 246, 0.3);
  position: relative;
  z-index: 1;
}

.avatar-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid transparent;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6) border-box;
  -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  animation: avatarSpin 8s linear infinite;
}

.avatar-ring::after {
  content: '';
  position: absolute;
  inset: -10px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(96, 165, 250, 0.15) 0%, transparent 60%);
  animation: avatarGlow 3s ease-in-out infinite;
}

@keyframes avatarSpin {
  to { transform: rotate(360deg); }
}

@keyframes avatarGlow {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.08); }
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 24px;
  font-weight: 800;
  color: #f1f5f9;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-tag {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.3), rgba(139, 92, 246, 0.3));
  border: 1px solid rgba(96, 165, 250, 0.4);
  color: #93c5fd;
  letter-spacing: 1px;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.meta-item svg {
  color: #475569;
}

.ph-login {
  padding: 10px 24px;
  border: 1px solid rgba(96, 165, 250, 0.3);
  border-radius: 10px;
  background: rgba(59, 130, 246, 0.15);
  color: #93c5fd;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.ph-login:hover {
  background: rgba(59, 130, 246, 0.25);
}

.header-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  padding: 10px 20px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.4);
}

.action-btn.ghost {
  background: transparent;
  border: 1px solid rgba(148, 163, 184, 0.3);
  color: #cbd5e1;
}

.action-btn.ghost:hover {
  background: rgba(148, 163, 184, 0.1);
  border-color: rgba(148, 163, 184, 0.5);
  box-shadow: none;
  transform: translateY(-1px);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.stat-card.clickable {
  cursor: pointer;
  transition: transform 0.35s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.35s ease, border-color 0.3s ease;
}

.stat-card.clickable:hover {
  transform: translateY(-4px);
  border-color: rgba(96, 165, 250, 0.4);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(59, 130, 246, 0.15);
}

.stat-card.clickable:hover .stat-arrow {
  opacity: 1;
  transform: translateX(4px);
  color: #60a5fa;
}

.stat-card.disabled {
  cursor: default;
}

.stat-arrow {
  margin-left: auto;
  font-size: 18px;
  color: #475569;
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

.stat-card.clickable:hover .stat-arrow {
  opacity: 1;
  color: #60a5fa;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.stat-num {
  font-size: 22px;
  font-weight: 800;
  color: #f1f5f9;
  line-height: 1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.sections {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.section-card {
  padding: 22px 24px;
}

.section-card:nth-child(3) {
  grid-column: 1 / -1;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.section-header h3 {
  font-size: 15px;
  font-weight: 700;
  color: #e2e8f0;
}

.section-clear {
  font-size: 12px;
  color: #64748b;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
}

.section-clear:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #f87171;
}

.section-add {
  font-size: 12px;
  color: #60a5fa;
  background: none;
  border: 1px solid rgba(96, 165, 250, 0.3);
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 6px;
  font-weight: 600;
  transition: all 0.2s;
}

.section-add:hover {
  background: rgba(59, 130, 246, 0.15);
  border-color: rgba(96, 165, 250, 0.6);
}

.section-more {
  font-size: 12px;
  color: #60a5fa;
  text-decoration: none;
  font-weight: 600;
}

.section-more:hover {
  text-decoration: underline;
}

.section-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 28px;
  color: #475569;
}

.section-empty svg {
  color: #334155;
}

.section-empty p {
  font-size: 13px;
}

.empty-action {
  margin-top: 4px;
  padding: 8px 22px;
  border: 1px solid rgba(96, 165, 250, 0.4);
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.15);
  color: #93c5fd;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.empty-action:hover {
  background: rgba(59, 130, 246, 0.3);
  border-color: rgba(96, 165, 250, 0.7);
  transform: translateY(-1px);
}

.fav-list, .pax-list, .recent-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.fav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(30, 41, 59, 0.4);
  border: 1px solid rgba(148, 163, 184, 0.08);
  cursor: pointer;
  transition: all 0.2s;
}

.fav-item:hover {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(96, 165, 250, 0.25);
  transform: translateX(2px);
}

.fav-num {
  font-size: 14px;
  font-weight: 800;
  color: #93c5fd;
  min-width: 56px;
}

.fav-route {
  flex: 1;
  font-size: 13px;
  color: #cbd5e1;
}

.fav-arrow {
  color: #475569;
  margin: 0 6px;
}

.fav-time {
  font-size: 13px;
  color: #64748b;
  font-variant-numeric: tabular-nums;
}

.fav-del {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #475569;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.2s;
}

.fav-item:hover .fav-del {
  opacity: 1;
}

.fav-del:hover {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}

.pax-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(30, 41, 59, 0.4);
  border: 1px solid rgba(148, 163, 184, 0.08);
}

.pax-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #06b6d4, #3b82f6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 700;
}

.pax-info {
  flex: 1;
}

.pax-name {
  font-size: 14px;
  font-weight: 700;
  color: #e2e8f0;
  margin-bottom: 2px;
}

.pax-id {
  font-size: 12px;
  color: #64748b;
  font-family: Consolas, Monaco, monospace;
}

.pax-del {
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #475569;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.2s;
}

.pax-item:hover .pax-del {
  opacity: 1;
}

.pax-del:hover {
  background: rgba(239, 68, 68, 0.15);
  color: #f87171;
}

.recent-item {
  padding: 12px 14px;
  border-radius: 10px;
  background: rgba(30, 41, 59, 0.4);
  border: 1px solid rgba(148, 163, 184, 0.08);
  cursor: pointer;
  transition: all 0.2s;
}

.recent-item:hover {
  background: rgba(30, 41, 59, 0.7);
  border-color: rgba(96, 165, 250, 0.25);
  transform: translateX(2px);
}

.r-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.r-train {
  font-size: 14px;
  font-weight: 800;
  color: #93c5fd;
}

.r-status {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
}

.r-status.paid {
  background: rgba(16, 185, 129, 0.15);
  color: #34d399;
}

.r-status.refunded {
  background: rgba(148, 163, 184, 0.15);
  color: #94a3b8;
}

.r-status.pending {
  background: rgba(251, 191, 36, 0.15);
  color: #fbbf24;
}

.r-route {
  font-size: 13px;
  color: #cbd5e1;
  margin-bottom: 6px;
}

.r-arrow {
  color: #475569;
  margin: 0 6px;
}

.r-bottom {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.r-date {
  color: #64748b;
}

.r-price {
  color: #fbbf24;
  font-weight: 700;
}

.pax-btn {
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.pax-btn.cancel {
  background: rgba(100, 116, 139, 0.2);
  color: #94a3b8;
  border: 1px solid rgba(100, 116, 139, 0.3);
}

.pax-btn.cancel:hover {
  background: rgba(100, 116, 139, 0.3);
}

.pax-btn.confirm {
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: white;
}

.pax-btn.confirm:hover {
  background: linear-gradient(135deg, #2563eb, #4f46e5);
}

@media (max-width: 900px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
  }
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .sections {
    grid-template-columns: 1fr;
  }
}
</style>