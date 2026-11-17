<template>
  <div class="dashboard">
    <AppNav />

    <section class="db-body">
      <div class="db-header glass-card">
        <div class="db-title">
          <h1>📊 数据大屏</h1>
          <p>railflow · 实时运营数据概览</p>
        </div>
        <div class="db-time">
          <span class="db-clock">{{ clock }}</span>
          <button class="db-refresh" @click="load" :disabled="loading">
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              :class="{ spinning: loading }"
            >
              <polyline points="23 4 23 10 17 10" />
              <polyline points="1 20 1 14 7 14" />
              <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
            </svg>
            刷新
          </button>
        </div>
      </div>

      <div v-if="loading" class="db-skeleton">
        <div v-for="i in 6" :key="i" class="sk-card glass-card"></div>
      </div>

      <template v-else>
        <div class="stats-grid">
          <div
            class="stat-card glass-card"
            v-for="s in statsCards"
            :key="s.label"
            :style="{ '--accent': s.color }"
          >
            <div class="sc-icon" :style="{ background: s.bg }">{{ s.icon }}</div>
            <div class="sc-body">
              <div class="sc-value">{{ formatNum(s.value) }}</div>
              <div class="sc-label">{{ s.label }}</div>
            </div>
            <div class="sc-spark">
              <div class="spark-line" :style="{ background: s.bg }"></div>
            </div>
          </div>
        </div>

        <div class="charts-row">
          <div class="chart-card glass-card">
            <div class="chart-head">
              <h3>🔥 热门路线 Top10</h3>
            </div>
            <div class="top-routes">
              <div v-if="topRoutes.length === 0" class="empty-hint">暂无购票数据</div>
              <div v-for="(r, i) in topRoutes" :key="i" class="route-row">
                <div class="rank-badge" :class="{ top1: i === 0, top2: i === 1, top3: i === 2 }">
                  {{ i + 1 }}
                </div>
                <div class="route-label">
                  <span class="from">{{ r.fromStation }}</span>
                  <span class="arrow">→</span>
                  <span class="to">{{ r.toStation }}</span>
                </div>
                <div class="route-bar-wrap">
                  <div class="route-bar" :style="{ width: (r.count / maxRouteCount) * 100 + '%' }"></div>
                </div>
                <div class="route-count">{{ r.count }} 张</div>
              </div>
            </div>
          </div>

          <div class="chart-card glass-card">
            <div class="chart-head">
              <h3>📈 近 7 日购票趋势</h3>
            </div>
            <div class="trend-chart">
              <div v-for="(t, i) in trend" :key="i" class="trend-col">
                <div class="trend-bar" :style="{ height: (t.orders / maxTrend) * 100 + '%' }"></div>
                <div class="trend-value">{{ t.orders }}</div>
                <div class="trend-date">{{ t.date }}</div>
              </div>
            </div>
            <div class="trend-total">
              <span>7 日总计：</span>
              <span class="tt-num">{{ trend.reduce((s, t) => s + t.orders, 0) }} 张</span>
              <span class="tt-sep">·</span>
              <span class="tt-num">¥{{ trend.reduce((s, t) => s + t.revenue, 0).toFixed(0) }}</span>
            </div>
          </div>
        </div>

        <div class="status-card glass-card">
          <div class="chart-head">
            <h3>📋 订单状态分布</h3>
          </div>
          <div class="status-row">
            <div v-for="s in statusList" :key="s.key" class="status-item">
              <div class="si-circle" :style="{ background: s.color }"></div>
              <div class="si-name">{{ s.label }}</div>
              <div class="si-count">{{ s.value }}</div>
              <div class="si-bar-wrap">
                <div
                  class="si-bar"
                  :style="{ width: (s.value / totalOrders) * 100 + '%', background: s.color }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import AppNav from '../components/AppNav.vue'
import request from '../utils/request'

const loading = ref(false)
const overview = ref({})
const topRoutes = ref([])
const trend = ref([])
const clock = ref('')

let clockTimer = null

function startClock() {
  const update = () => {
    const now = new Date()
    const pad = (n) => String(n).padStart(2, '0')
    clock.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
  }
  update()
  clockTimer = setInterval(update, 1000)
}

async function load() {
  loading.value = true
  try {
    const [ov, routes, tr] = await Promise.all([
      request.get('/dashboard/overview'),
      request.get('/dashboard/top-routes').catch(() => []),
      request.get('/dashboard/trend').catch(() => [])
    ])
    overview.value = ov || {}
    topRoutes.value = routes || []
    trend.value = tr || []
  } catch (e) {
  } finally {
    loading.value = false
  }
}

const statsCards = computed(() => [
  {
    label: '总订单数',
    value: overview.value.totalOrders || 0,
    icon: '📋',
    color: '#3b82f6',
    bg: 'linear-gradient(135deg, rgba(59,130,246,0.3), rgba(59,130,246,0.1))'
  },
  {
    label: '今日购票',
    value: overview.value.todayOrders || 0,
    icon: '🎫',
    color: '#10b981',
    bg: 'linear-gradient(135deg, rgba(16,185,129,0.3), rgba(16,185,129,0.1))'
  },
  {
    label: '今日收入',
    value: overview.value.todayRevenue || 0,
    icon: '💰',
    color: '#f59e0b',
    bg: 'linear-gradient(135deg, rgba(245,158,11,0.3), rgba(245,158,11,0.1))'
  },
  {
    label: '列车总数',
    value: overview.value.totalTrains || 0,
    icon: '🚄',
    color: '#ec4899',
    bg: 'linear-gradient(135deg, rgba(236,72,153,0.3), rgba(236,72,153,0.1))'
  },
  {
    label: '注册用户',
    value: overview.value.totalUsers || 0,
    icon: '👥',
    color: '#8b5cf6',
    bg: 'linear-gradient(135deg, rgba(139,92,246,0.3), rgba(139,92,246,0.1))'
  }
])

const maxRouteCount = computed(() => {
  const max = Math.max(...topRoutes.value.map((r) => r.count), 1)
  return max
})

const maxTrend = computed(() => {
  const max = Math.max(...trend.value.map((t) => t.orders), 1)
  return max
})

const totalOrders = computed(() => {
  const s = overview.value.statusCounts || {}
  return Object.values(s).reduce((a, b) => a + b, 0) || 1
})

const statusList = computed(() => {
  const s = overview.value.statusCounts || {}
  return [
    { key: 'PAID', label: '已支付', value: s.PAID || 0, color: '#3b82f6' },
    { key: 'COMPLETED', label: '已完成', value: s.COMPLETED || 0, color: '#10b981' },
    { key: 'REFUNDED', label: '已退票', value: s.REFUNDED || 0, color: '#64748b' },
    { key: 'PENDING', label: '待支付', value: s.PENDING || 0, color: '#f59e0b' },
    { key: 'CANCELLED', label: '已取消', value: s.CANCELLED || 0, color: '#ef4444' }
  ]
})

function formatNum(n) {
  if (typeof n === 'number' && n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (typeof n === 'number' && n % 1 !== 0) return '¥' + n.toFixed(0)
  return n
}

onMounted(() => {
  startClock()
  load()
})

onBeforeUnmount(() => {
  if (clockTimer) clearInterval(clockTimer)
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
}

.db-body {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px 48px;
}

.db-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 32px;
  margin-bottom: 24px;
}

.db-title h1 {
  font-size: 26px;
  margin: 0 0 4px;
  background: linear-gradient(90deg, #60a5fa, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.db-title p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.db-time {
  display: flex;
  align-items: center;
  gap: 12px;
}

.db-clock {
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 14px;
  padding: 8px 16px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
  color: #93c5fd;
}

.db-refresh {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(148, 163, 184, 0.1);
  color: #cbd5e1;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.db-refresh:hover:not(:disabled) {
  background: rgba(148, 163, 184, 0.2);
  color: #fff;
}

.db-refresh:disabled {
  opacity: 0.6;
  cursor: wait;
}

.db-refresh svg.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  padding: 20px;
  position: relative;
  overflow: hidden;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.sc-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 14px;
}

.sc-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--accent);
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  line-height: 1.2;
}

.sc-label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 4px;
}

.sc-spark {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 80px;
  height: 40px;
  opacity: 0.4;
}

.spark-line {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  clip-path: polygon(0 100%, 15% 60%, 30% 80%, 50% 30%, 70% 50%, 85% 20%, 100% 40%, 100% 100%);
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  padding: 24px;
}

.chart-head h3 {
  font-size: 16px;
  margin: 0 0 18px;
  color: #f1f5f9;
}

.top-routes {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty-hint {
  text-align: center;
  color: #64748b;
  padding: 40px;
  font-size: 14px;
}

.route-row {
  display: grid;
  grid-template-columns: 28px 130px 1fr 60px;
  align-items: center;
  gap: 10px;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: rgba(148, 163, 184, 0.15);
  color: #94a3b8;
}

.rank-badge.top1 {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  color: #000;
}
.rank-badge.top2 {
  background: linear-gradient(135deg, #94a3b8, #cbd5e1);
  color: #000;
}
.rank-badge.top3 {
  background: linear-gradient(135deg, #d97706, #92400e);
  color: #fff;
}

.route-label {
  font-size: 13px;
  font-weight: 600;
  color: #cbd5e1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.route-label .arrow {
  color: #64748b;
  margin: 0 4px;
}
.route-label .to {
  color: #fbbf24;
}

.route-bar-wrap {
  height: 8px;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.route-bar {
  height: 100%;
  background: linear-gradient(90deg, #60a5fa, #a78bfa);
  border-radius: 4px;
  transition: width 0.6s ease;
}

.route-count {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  text-align: right;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 180px;
  padding: 0 10px 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.trend-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
  max-width: 40px;
}

.trend-bar {
  width: 16px;
  min-height: 4px;
  background: linear-gradient(180deg, #60a5fa, #3b82f6);
  border-radius: 4px 4px 0 0;
  transition: height 0.6s ease;
  box-shadow: 0 0 8px rgba(59, 130, 246, 0.3);
}

.trend-value {
  font-size: 10px;
  color: #93c5fd;
  font-weight: 600;
}

.trend-date {
  font-size: 10px;
  color: #64748b;
  margin-top: 6px;
  font-family: monospace;
}

.trend-total {
  text-align: center;
  margin-top: 14px;
  font-size: 13px;
  color: #94a3b8;
}

.tt-num {
  font-weight: 700;
  color: #60a5fa;
}

.tt-sep {
  margin: 0 8px;
  color: #475569;
}

.status-card {
  padding: 24px;
}

.status-row {
  display: grid;
  gap: 14px;
}

.status-item {
  display: grid;
  grid-template-columns: 12px 100px 60px 1fr;
  align-items: center;
  gap: 12px;
}

.si-circle {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.si-name {
  font-size: 13px;
  font-weight: 600;
  color: #cbd5e1;
}

.si-count {
  font-size: 13px;
  font-weight: 700;
  font-family: monospace;
  color: #f1f5f9;
}

.si-bar-wrap {
  height: 6px;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.si-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
}

.db-skeleton {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.sk-card {
  height: 120px;
  background: linear-gradient(90deg, rgba(30, 41, 59, 0.8), rgba(51, 65, 85, 0.8), rgba(30, 41, 59, 0.8));
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s infinite;
}

@keyframes skeleton-shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

html[data-theme='light'] .db-title h1 {
  background: linear-gradient(90deg, #2563eb, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
html[data-theme='light'] .db-title p {
  color: #64748b;
}
html[data-theme='light'] .db-clock {
  color: #1d4ed8;
  border-color: rgba(59, 130, 246, 0.3);
  background: rgba(59, 130, 246, 0.08);
}
html[data-theme='light'] .db-refresh {
  color: #475569;
  border-color: rgba(100, 116, 139, 0.2);
}
html[data-theme='light'] .db-refresh:hover:not(:disabled) {
  color: #1e293b;
}
html[data-theme='light'] .sc-label {
  color: #64748b;
}
html[data-theme='light'] .chart-head h3 {
  color: #1e293b;
}
html[data-theme='light'] .rank-badge {
  background: rgba(100, 116, 139, 0.15);
  color: #64748b;
}
html[data-theme='light'] .route-label {
  color: #334155;
}
html[data-theme='light'] .route-label .to {
  color: #d97706;
}
html[data-theme='light'] .route-count {
  color: #64748b;
}
html[data-theme='light'] .empty-hint {
  color: #64748b;
}
html[data-theme='light'] .trend-value {
  color: #2563eb;
}
html[data-theme='light'] .trend-date {
  color: #64748b;
}
html[data-theme='light'] .trend-total {
  color: #475569;
}
html[data-theme='light'] .tt-num {
  color: #2563eb;
}
html[data-theme='light'] .si-name {
  color: #334155;
}
html[data-theme='light'] .si-count {
  color: #1e293b;
}
</style>
