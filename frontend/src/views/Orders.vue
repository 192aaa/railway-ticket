<template>
  <div class="orders">
    <AppNav />

    <section class="orders-body">
      <div class="page-header">
        <h1>我的订单</h1>
        <p class="page-sub">查看您的购票记录 · 共 {{ orders.length }} 张票</p>
        <div v-if="orders.length" class="order-stats">
          <div class="stat-chip paid-chip">
            <span class="sc-dot"></span>
            <span>{{ counts.paid }} 张有效</span>
          </div>
          <div class="stat-chip refunded-chip">
            <span class="sc-dot"></span>
            <span>{{ counts.refunded }} 张已退票</span>
          </div>
          <div class="stat-chip pending-chip" v-if="counts.pending">
            <span class="sc-dot"></span>
            <span>{{ counts.pending }} 张待支付</span>
          </div>
        </div>
      </div>

      <div v-if="orders.length" class="filter-tabs">
        <button
          v-for="t in filterTabs"
          :key="t.key"
          class="filter-tab"
          :class="{ active: activeFilter === t.key }"
          @click="setFilter(t.key)"
        >
          {{ t.label }}
          <span class="ft-count">{{ t.count }}</span>
        </button>
      </div>

      <div v-if="loading" class="skeleton-list">
        <div v-for="i in 3" :key="i" class="skeleton-card glass-card">
          <div class="sk-line sk-lg"></div>
          <div class="sk-line sk-md"></div>
          <div class="sk-line sk-sm"></div>
          <div class="sk-chips">
            <div class="sk-chip"></div>
            <div class="sk-chip wide"></div>
          </div>
        </div>
      </div>

      <TransitionGroup v-else-if="filteredOrders.length" name="list" tag="div" class="order-list stagger-children">
        <div
          v-for="(order, idx) in filteredOrders"
          :key="order.id"
          class="ticket-card glass-card"
          :data-oid="order.id"
          :style="{ animationDelay: (idx * 0.05) + 's' }"
          :class="{
            refunded: order.status === 'REFUNDED' || order.status === '已退票',
            pending: order.status === 'PENDING' || order.status === '待支付',
            cancelled: order.status === 'CANCELLED' || order.status === '已取消',
            paid: isPaid(order.status),
            'highlight-on': order.id === highlightId
          }"
          @click="openDetail(order)"
        >
          <div class="shimmer-overlay"></div>
          <div class="ticket-left">
            <div class="ticket-edge top"></div>
            <div class="ticket-edge bottom"></div>
            <div class="ticket-hole top-hole"></div>
            <div class="ticket-hole bottom-hole"></div>

            <div class="ticket-grid">
              <div class="tg-row">
                <span class="tg-label">车次</span>
                <span class="tg-train">{{ order.trainNumber }}</span>
              </div>
              <div class="tg-row">
                <span class="tg-label">座位号</span>
                <span class="tg-seat">{{ order.seatNo }}</span>
              </div>
              <div class="tg-row">
                <span class="tg-label">票种</span>
                <span class="tg-type">{{ order.seatType }}</span>
              </div>
              <div class="tg-row">
                <span class="tg-label">票价</span>
                <span class="tg-price">¥{{ order.price }}</span>
              </div>
              <div class="tg-row">
                <span class="tg-label">乘客</span>
                <span class="tg-name">{{ order.passengerName || '-' }}</span>
              </div>
              <div class="tg-row">
                <span class="tg-label">日期</span>
                <span class="tg-date">{{ order.trainDate }}</span>
              </div>
            </div>
          </div>

          <div class="ticket-right">
            <div class="route-v">
              <div class="rv-time">
                <span class="rv-time-big">{{ formatTime(order.startTime) }}</span>
                <span class="rv-station">{{ order.fromStation }}</span>
              </div>
              <div class="rv-track">
                <div class="rv-line"></div>
                <div class="rv-train-dot"></div>
              </div>
              <div class="rv-time end">
                <span class="rv-time-big">{{ formatTime(order.endTime) }}</span>
                <span class="rv-station">{{ order.toStation }}</span>
              </div>
            </div>

            <div class="status-wrap">
              <span class="status-dot" :class="statusClass(order.status)"></span>
              <span class="status-text">{{ statusLabel(order.status) }}</span>
              <button
                v-if="isPaid(order.status)"
                class="refund-btn"
                :disabled="refundingId === order.id"
                @click="refund(order.id)"
              >
                {{ refundingId === order.id ? '退票中...' : '退票' }}
              </button>
            </div>
          </div>
        </div>
      </TransitionGroup>

      <div v-else-if="!loading" class="empty-state glass-card">
        <template v-if="orders.length">
          <svg class="empty-filter-svg" viewBox="0 0 120 90" fill="none">
            <defs>
              <linearGradient id="ef-g" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#6366f1"/>
                <stop offset="100%" stop-color="#a855f7"/>
              </linearGradient>
            </defs>
            <g class="ef-lens">
              <circle cx="48" cy="38" r="22" fill="none" stroke="url(#ef-g)" stroke-width="3"/>
              <circle cx="48" cy="38" r="16" fill="rgba(99,102,241,0.08)" class="ef-inner"/>
            </g>
            <line class="ef-handle" x1="64" y1="52" x2="90" y2="78" stroke="url(#ef-g)" stroke-width="4" stroke-linecap="round"/>
            <g class="ef-ticket">
              <rect x="20" y="68" width="36" height="16" rx="3" fill="none" stroke="#475569" stroke-width="1.5" stroke-dasharray="3 2"/>
              <line x1="26" y1="76" x2="50" y2="76" stroke="#475569" stroke-width="1"/>
              <circle cx="26" cy="76" r="2" fill="#475569" class="ef-hole"/>
              <circle cx="50" cy="76" r="2" fill="#475569" class="ef-hole"/>
            </g>
          </svg>
          <h3>当前筛选下无订单</h3>
          <p>换个筛选条件试试吧</p>
          <button class="go-home-btn" @click="setFilter('ALL')">查看全部订单</button>
        </template>
        <template v-else>
          <svg class="empty-train-svg" viewBox="0 0 120 80" fill="none">
            <defs>
              <linearGradient id="eo-g" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.8"/>
                <stop offset="100%" stop-color="#8b5cf6" stop-opacity="0.6"/>
              </linearGradient>
            </defs>
            <line class="et-track" x1="0" y1="66" x2="120" y2="66" stroke="#334155" stroke-width="2" stroke-dasharray="6 4"/>
            <g class="et-cloud et-c1">
              <ellipse cx="25" cy="14" rx="18" ry="8" fill="#475569" opacity="0.4"/>
            </g>
            <g class="et-cloud et-c2">
              <ellipse cx="95" cy="10" rx="14" ry="6" fill="#475569" opacity="0.35"/>
            </g>
            <g class="et-body">
              <rect x="18" y="24" width="72" height="34" rx="6" fill="url(#eo-g)" class="et-fill"/>
              <rect x="26" y="30" width="14" height="12" rx="2" fill="#0f172a" class="et-window"/>
              <rect x="48" y="30" width="14" height="12" rx="2" fill="#0f172a" class="et-window"/>
              <rect x="70" y="30" width="14" height="12" rx="2" fill="#0f172a" class="et-window"/>
              <rect x="30" y="48" width="48" height="3" rx="1.5" fill="#0f172a" opacity="0.4"/>
            </g>
            <circle cx="34" cy="62" r="5" fill="#0f172a" stroke="#60a5fa" stroke-width="1.8" class="et-wheel"/>
            <circle cx="74" cy="62" r="5" fill="#0f172a" stroke="#60a5fa" stroke-width="1.8" class="et-wheel"/>
            <line class="et-smoke et-s1" x1="92" y1="30" x2="100" y2="18" stroke="#94a3b8" stroke-width="2" stroke-linecap="round" opacity="0.5"/>
            <line class="et-smoke et-s2" x1="94" y1="32" x2="106" y2="22" stroke="#94a3b8" stroke-width="1.5" stroke-linecap="round" opacity="0.35"/>
          </svg>
          <h3>暂无订单</h3>
          <p>快去首页买一张票吧</p>
          <button class="go-home-btn" @click="$router.push('/home')">去购票</button>
        </template>
      </div>

      <el-dialog
        v-model="detailVisible"
        width="520px"
        :show-close="false"
        align-center
        class="ticket-detail-dialog"
        :close-on-click-modal="true"
      >
        <div v-if="detailOrder" class="detail-wrap">
          <button class="detail-close" @click="detailVisible = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>

          <div class="detail-header">
            <div class="dh-title">
              <span class="status-dot" :class="statusClass(detailOrder.status)"></span>
              <span class="dh-status">{{ statusLabel(detailOrder.status) }}</span>
            </div>
            <div class="dh-train">
              <span class="dh-train-num">{{ detailOrder.trainNumber }}</span>
              <span class="dh-sep">·</span>
              <span class="dh-type">{{ detailOrder.seatType }}</span>
            </div>
          </div>

          <div class="detail-route">
            <div class="dr-station">
              <span class="dr-time">{{ formatTime(detailOrder.startTime) || '--:--' }}</span>
              <span class="dr-name">{{ detailOrder.fromStation }}</span>
            </div>
            <div class="dr-line">
              <div class="dr-inner"></div>
              <span class="dr-duration">{{ calcDuration(detailOrder.startTime, detailOrder.endTime) }}</span>
            </div>
            <div class="dr-station end">
              <span class="dr-time">{{ formatTime(detailOrder.endTime) || '--:--' }}</span>
              <span class="dr-name">{{ detailOrder.toStation }}</span>
            </div>
          </div>

          <div class="detail-grid">
            <div class="dg-item">
              <span class="dg-label">订单号</span>
              <span class="dg-val mono">{{ detailOrder.orderNo || '-' }}</span>
            </div>
            <div class="dg-item">
              <span class="dg-label">乘车日期</span>
              <span class="dg-val">{{ detailOrder.trainDate || '-' }}</span>
            </div>
            <div class="dg-item">
              <span class="dg-label">乘车人</span>
              <span class="dg-val bold">{{ detailOrder.passengerName || '-' }}</span>
            </div>
            <div class="dg-item">
              <span class="dg-label">证件号</span>
              <span class="dg-val mono">{{ maskIdCard(detailOrder.passengerIdCard) }}</span>
            </div>
            <div class="dg-item">
              <span class="dg-label">座位号</span>
              <span class="dg-val">{{ detailOrder.seatNo || '-' }}</span>
            </div>
            <div class="dg-item">
              <span class="dg-label">票价</span>
              <span class="dg-val price">¥{{ detailOrder.price ?? '-' }}</span>
            </div>
            <div class="dg-item">
              <span class="dg-label">下单时间</span>
              <span class="dg-val">{{ formatFullTime(detailOrder.createTime) }}</span>
            </div>
            <div class="dg-item">
              <span class="dg-label">支付时间</span>
              <span class="dg-val">{{ formatFullTime(detailOrder.payTime) }}</span>
            </div>
          </div>

          <div class="eticket-bar">
            <div class="eticket-qr">
              <div class="qr-grid">
                <template v-for="(row, ri) in qrMatrix(detailOrder)" :key="ri">
                  <span
                    v-for="(cell, ci) in row"
                    :key="ci"
                    class="qr-cell"
                    :class="{ on: cell }"
                  ></span>
                </template>
              </div>
              <div class="qr-label">电子检票码</div>
            </div>
            <div class="eticket-barcode">
              <div class="barcode-lines">
                <span v-for="n in 48" :key="n" class="bar" :style="{ width: (n % 3 === 0 ? 2 : n % 5 === 0 ? 3 : 1) + 'px' }"></span>
              </div>
              <div class="barcode-text">{{ (detailOrder.orderNo || 'NOORDER') }}</div>
            </div>
          </div>

          <div class="detail-actions" @click.stop>
            <button
              v-if="isPaid(detailOrder.status)"
              class="action-print"
              @click="printTicket"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/></svg>
              打印/导出PDF
            </button>
            <button
              v-if="isPaid(detailOrder.status)"
              class="action-refund"
              :disabled="refundingId === detailOrder.id"
              @click="refund(detailOrder.id)"
            >{{ refundingId === detailOrder.id ? '退票中...' : '申请退票' }}</button>
            <button class="action-close" @click="detailVisible = false">关闭</button>
          </div>
        </div>
      </el-dialog>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import AppNav from '../components/AppNav.vue'

const route = useRoute()
const router = useRouter()
const orders = ref([])
const loading = ref(true)
const refundingId = ref(null)
const highlightId = ref(null)

const detailVisible = ref(false)
const detailOrder = ref(null)

function openDetail(order) {
  detailOrder.value = order
  detailVisible.value = true
}

function printTicket() {
  const detailEl = document.querySelector('.ticket-detail-dialog .detail-wrap')
  if (!detailEl) return
  const printStyles = Array.from(document.querySelectorAll('style, link[rel="stylesheet"]'))
    .map(el => el.outerHTML)
    .join('\n')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>电子客票 - ${detailOrder.value?.orderNo || ''}</title>${printStyles}<style>
    body { margin: 0; padding: 20px; background: white !important; }
    .detail-wrap { box-shadow: none !important; border: 2px dashed #94a3b8; border-radius: 12px; padding: 28px; max-width: 480px; margin: 0 auto; background: white !important; color: #1e293b !important; }
    .detail-close, .detail-actions { display: none !important; }
    .glass-card, .glass { background: white !important; backdrop-filter: none !important; border: none !important; }
    .status-dot { background: #22c55e !important; box-shadow: none !important; }
    .dh-status, .dh-train-num, .dh-type, .dr-time, .dr-name, .dr-duration { color: #1e293b !important; }
    .dg-label { color: #64748b !important; }
    .dg-val { color: #1e293b !important; }
    .dg-val.price { color: #dc2626 !important; font-weight: 700; }
    .qr-cell.on { background: #1e293b !important; }
    .qr-cell { background: white !important; }
    .qr-label, .barcode-text { color: #64748b !important; }
    .barcode-lines .bar { background: #1e293b !important; }
    .eticket-bar { border-top: 2px dashed #94a3b8; padding-top: 20px; margin-top: 20px; }
    .detail-grid { background: #f8fafc !important; border-radius: 8px; padding: 12px; }
  </style></head><body>${detailEl.outerHTML}</body></html>`
  const w = window.open('', '_blank')
  w.document.write(html)
  w.document.close()
  w.onload = () => {
    setTimeout(() => {
      w.print()
    }, 300)
  }
}

function formatFullTime(t) {
  if (!t) return '-'
  try {
    const d = new Date(typeof t === 'string' ? t.replace(' ', 'T') : t)
    if (isNaN(d.getTime())) return t
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch {
    return t
  }
}

function maskIdCard(id) {
  if (!id) return '-'
  if (id.length <= 8) return id
  return id.substring(0, 4) + '**********' + id.substring(id.length - 4)
}

function calcDuration(s, e) {
  if (!s || !e) return ''
  const [sh, sm] = s.split(':').map(Number)
  const [eh, em] = e.split(':').map(Number)
  let mins = (eh * 60 + em) - (sh * 60 + sm)
  if (mins < 0) mins += 24 * 60
  if (mins <= 0) return ''
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return h ? (m ? `${h}h${m}m` : `${h}h`) : `${m}m`
}

function qrMatrix(order) {
  const size = 11
  const seedStr = `${order.id || 0}${order.orderNo || ''}${order.trainNumber || ''}`
  let hash = 0
  for (let i = 0; i < seedStr.length; i++) hash = (hash * 31 + seedStr.charCodeAt(i)) >>> 0
  const matrix = []
  for (let r = 0; r < size; r++) {
    const row = []
    for (let c = 0; c < size; c++) {
      hash = (hash * 1103515245 + 12345) & 0x7fffffff
      row.push(((hash >> 8) & 1) === 1)
    }
    matrix.push(row)
  }
  for (let i = 0; i < 3; i++) {
    for (let j = 0; j < 3; j++) {
      const v = (i === 0 || i === 2 || j === 0 || j === 2)
      matrix[i][j] = v
      matrix[i][size - 1 - j] = v
      matrix[size - 1 - i][j] = v
    }
  }
  return matrix
}

const counts = computed(() => {
  let paid = 0, refunded = 0, pending = 0, cancelled = 0
  for (const o of orders.value) {
    if (o.status === 'PAID' || o.status === '已支付') paid++
    else if (o.status === 'REFUNDED' || o.status === '已退票') refunded++
    else if (o.status === 'PENDING' || o.status === '待支付') pending++
    else cancelled++
  }
  return { paid, refunded, pending, cancelled }
})

const activeFilter = computed(() => {
  const s = route.query.status
  if (s === 'PAID') return 'PAID'
  if (s === 'REFUNDED') return 'REFUNDED'
  if (s === 'PENDING') return 'PENDING'
  return 'ALL'
})

const filteredOrders = computed(() => {
  const f = activeFilter.value
  if (f === 'ALL') return orders.value
  return orders.value.filter(o => o.status === f || (f === 'PAID' && o.status === '已支付') || (f === 'REFUNDED' && o.status === '已退票') || (f === 'PENDING' && o.status === '待支付'))
})

const filterTabs = computed(() => [
  { key: 'ALL', label: '全部', count: orders.value.length },
  { key: 'PAID', label: '有效', count: counts.value.paid },
  { key: 'REFUNDED', label: '已退票', count: counts.value.refunded },
  { key: 'PENDING', label: '待支付', count: counts.value.pending }
])

function setFilter(key) {
  if (key === 'ALL') {
    router.replace({ path: '/orders' })
  } else {
    router.replace({ path: '/orders', query: { status: key } })
  }
}

let loadAbort = null

onMounted(load)

onBeforeUnmount(() => {
  if (loadAbort) loadAbort.abort()
})

watch(() => route.query, async (q) => {
  if (q.highlight === 'new') {
    await load()
    if (orders.value.length) {
      highlightId.value = orders.value[0].id
      requestAnimationFrame(() => {
        const el = document.querySelector(`[data-oid="${highlightId.value}"]`)
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      })
    }
  } else if (q.highlight && q.highlight !== 'new') {
    await load()
    const targetId = Number(q.highlight)
    if (orders.value.some(o => o.id === targetId)) {
      highlightId.value = targetId
      requestAnimationFrame(() => {
        const el = document.querySelector(`[data-oid="${targetId}"]`)
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      })
    }
  }
})

async function load() {
  if (loadAbort) loadAbort.abort()
  const controller = new AbortController()
  loadAbort = controller

  loading.value = true
  try {
    const list = await request.get('/order/my', { signal: controller.signal })
    orders.value = Array.isArray(list)
      ? [...list].sort((a, b) => {
          const ta = a.createTime || a.id || 0
          const tb = b.createTime || b.id || 0
          return String(tb).localeCompare(String(ta))
        })
      : []
  } catch (e) {
    if (e.code !== 'ERR_CANCELED') {
      orders.value = []
    }
  } finally {
    if (loadAbort === controller) {
      loading.value = false
      loadAbort = null
    }
  }
}

function formatTime(t) {
  if (!t) return '--:--'
  if (typeof t === 'string' && t.includes('T')) {
    return t.split('T')[1]?.substring(0, 5) || t
  }
  return t
}

const STATUS_MAP = {
  'PAID': { label: '已支付', cls: 'paid' },
  '已支付': { label: '已支付', cls: 'paid' },
  'REFUNDED': { label: '已退票', cls: 'refunded' },
  '已退票': { label: '已退票', cls: 'refunded' },
  'PENDING': { label: '待支付', cls: 'pending' },
  '待支付': { label: '待支付', cls: 'pending' },
  'CANCELLED': { label: '已取消', cls: 'cancelled' },
  '已取消': { label: '已取消', cls: 'cancelled' }
}

function statusLabel(s) {
  return STATUS_MAP[s]?.label || s
}

function statusClass(s) {
  return STATUS_MAP[s]?.cls || ''
}

function isPaid(s) {
  return s === 'PAID' || s === '已支付'
}

async function refund(id) {
  const order = detailOrder.value?.id === id
    ? detailOrder.value
    : orders.value.find(o => o.id === id)

  if (!order) {
    ElMessage.error('未找到该订单')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定申请退票？\n\n车次：${order.trainNumber}\n乘客：${order.passengerName || '-'}\n座位：${order.seatNo || '-'}\n票价：¥${order.price ?? '-'}\n\n退票后余票将返还，退款将按原路径返回。`,
      '确认退票',
      {
        confirmButtonText: '确认退票',
        cancelButtonText: '再想想',
        type: 'warning',
        customClass: 'refund-confirm-box'
      }
    )
  } catch { return }

  refundingId.value = id
  try {
    await request.post(`/order/refund/${id}`)
    detailVisible.value = false
    ElMessage.success('退票成功，余票已返还')
    load()
  } catch (e) {
  } finally {
    refundingId.value = null
  }
}
</script>

<style scoped>
.orders {
  min-height: 100vh;
}

.orders-body {
  padding: 40px 32px 60px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 32px;
  font-weight: 800;
  color: #f1f5f9;
  margin-bottom: 4px;
}

.page-sub {
  color: #64748b;
  font-size: 14px;
}

.filter-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.filter-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid rgba(148, 163, 184, 0.15);
  background: rgba(30, 41, 59, 0.4);
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tab:hover {
  background: rgba(30, 41, 59, 0.7);
  color: #e2e8f0;
  border-color: rgba(96, 165, 250, 0.3);
}

.filter-tab.active {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(99, 102, 241, 0.2));
  color: #93c5fd;
  border-color: rgba(96, 165, 250, 0.5);
}

.ft-count {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(148, 163, 184, 0.15);
  font-weight: 700;
}

.filter-tab.active .ft-count {
  background: rgba(96, 165, 250, 0.25);
  color: #bfdbfe;
}

.order-stats {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.stat-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 600;
}

.sc-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.paid-chip {
  background: rgba(16, 185, 129, 0.12);
  border: 1px solid rgba(16, 185, 129, 0.25);
  color: #34d399;
}

.paid-chip .sc-dot { background: #10b981; }

.refunded-chip {
  background: rgba(100, 116, 139, 0.12);
  border: 1px solid rgba(100, 116, 139, 0.25);
  color: #94a3b8;
}

.refunded-chip .sc-dot { background: #64748b; }

.pending-chip {
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.25);
  color: #fbbf24;
}

.pending-chip .sc-dot { background: #f59e0b; }

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-card {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sk-line {
  height: 18px;
  border-radius: 8px;
  background: rgba(71, 85, 105, 0.5);
  animation: skPulse 1.2s ease-in-out infinite;
}

.sk-lg { width: 70%; height: 26px; }
.sk-md { width: 50%; height: 20px; }
.sk-sm { width: 90%; height: 14px; }

.sk-chips {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.sk-chip {
  width: 80px;
  height: 44px;
  border-radius: 12px;
  background: rgba(71, 85, 105, 0.5);
  animation: skPulse 1.2s ease-in-out infinite;
}

.sk-chip.wide { width: 160px; }

@keyframes skPulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.ticket-card {
  position: relative;
  padding: 0;
  overflow: hidden;
  display: grid;
  grid-template-columns: 1fr 260px;
  border-radius: 20px;
  cursor: pointer;
  transition: transform 0.4s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.4s ease, border-color 0.3s ease;
}

.ticket-card:hover {
  transform: translateY(-3px);
  border-color: rgba(96, 165, 250, 0.4) !important;
  box-shadow:
    0 10px 40px rgba(0, 0, 0, 0.45),
    0 0 0 1px rgba(59, 130, 246, 0.15);
}

.ticket-card:hover .shimmer-overlay {
  opacity: 1;
  animation: cardShimmer 1.2s ease-out forwards;
}

.ticket-card.refunded {
  opacity: 0.55;
  filter: grayscale(0.4);
}

.ticket-card.refunded:hover {
  filter: grayscale(0);
  opacity: 0.85;
}

.ticket-card.highlight-on {
  animation: ticketHighlight 2.4s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

@keyframes ticketHighlight {
  0% {
    box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.9), 0 0 40px rgba(59, 130, 246, 0.6), 0 0 80px rgba(139, 92, 246, 0.3);
    transform: translateY(-4px) scale(1.01);
  }
  40% {
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.8), 0 0 60px rgba(59, 130, 246, 0.5), 0 0 120px rgba(139, 92, 246, 0.2);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0);
    transform: translateY(0) scale(1);
  }
}

.ticket-left {
  position: relative;
  padding: 28px 32px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.12) 0%, rgba(139, 92, 246, 0.08) 100%);
  border-right: 2px dashed rgba(148, 163, 184, 0.2);
}

.ticket-edge.top, .ticket-edge.bottom {
  position: absolute;
  right: -2px;
  width: 16px;
  height: 16px;
  background: #0f172a;
  border-radius: 50%;
}

.ticket-edge.top { top: -8px; }
.ticket-edge.bottom { bottom: -8px; }

.ticket-hole {
  position: absolute;
  left: 50%;
  width: 14px;
  height: 14px;
  background: radial-gradient(circle, #0f172a 50%, rgba(15, 23, 42, 0) 70%);
  border-radius: 50%;
}

.top-hole { top: -7px; }
.bottom-hole { bottom: -7px; }

.ticket-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px 24px;
}

.tg-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tg-label {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 600;
}

.tg-train {
  font-size: 20px;
  font-weight: 800;
  color: #f1f5f9;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.tg-seat {
  font-size: 16px;
  font-weight: 700;
  color: #10b981;
}

.tg-type {
  font-size: 14px;
  font-weight: 600;
  color: #cbd5e1;
}

.tg-price {
  font-size: 18px;
  font-weight: 800;
  color: #fbbf24;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.tg-name {
  font-size: 14px;
  color: #cbd5e1;
  font-weight: 500;
}

.tg-date {
  font-size: 14px;
  color: #94a3b8;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.ticket-right {
  padding: 28px 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.route-v {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rv-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.rv-time-big {
  font-size: 22px;
  font-weight: 800;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  color: #f1f5f9;
}

.rv-station {
  font-size: 12px;
  color: #94a3b8;
}

.rv-track {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: 8px 0;
}

.rv-line {
  width: 80%;
  height: 2px;
  background: linear-gradient(90deg, #3b82f6 0%, #a78bfa 100%);
  border-radius: 2px;
}

.rv-train-dot {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #f472b6;
  box-shadow: 0 0 8px rgba(244, 114, 182, 0.6);
}

.status-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  position: relative;
}

.status-dot.paid {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.7), 0 0 0 0 rgba(16, 185, 129, 0.6);
  animation: dotPulsePaid 2.4s ease-in-out infinite;
}

@keyframes dotPulsePaid {
  0%, 100% { box-shadow: 0 0 8px rgba(16, 185, 129, 0.7), 0 0 0 0 rgba(16, 185, 129, 0.6); }
  50% { box-shadow: 0 0 14px rgba(16, 185, 129, 0.9), 0 0 0 6px rgba(16, 185, 129, 0); }
}

.status-dot.pending {
  background: #f59e0b;
  box-shadow: 0 0 8px rgba(245, 158, 11, 0.7), 0 0 0 0 rgba(245, 158, 11, 0.5);
  animation: dotPulsePending 1.8s ease-in-out infinite;
}

@keyframes dotPulsePending {
  0%, 100% { box-shadow: 0 0 8px rgba(245, 158, 11, 0.7), 0 0 0 0 rgba(245, 158, 11, 0.5); }
  50% { box-shadow: 0 0 14px rgba(245, 158, 11, 0.9), 0 0 0 5px rgba(245, 158, 11, 0); }
}

.status-dot.refunded {
  background: #64748b;
  box-shadow: 0 0 4px rgba(100, 116, 139, 0.3);
}

.status-dot.cancelled {
  background: #ef4444;
  box-shadow: 0 0 4px rgba(239, 68, 68, 0.4);
}

.status-text {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: #cbd5e1;
}

.refund-btn {
  padding: 6px 14px;
  border: 1px solid rgba(239, 68, 68, 0.4);
  background: rgba(239, 68, 68, 0.1);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  color: #f87171;
  cursor: pointer;
}

.refund-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
}

.refund-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.empty-state {
  padding: 80px 32px;
  text-align: center;
}

.empty-svg {
  width: 96px;
  height: 72px;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 20px;
  color: #e2e8f0;
  margin-bottom: 8px;
}

.empty-state p {
  color: #64748b;
  margin-bottom: 24px;
}

.go-home-btn {
  padding: 12px 32px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%);
  color: white;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.list-enter-active {
  animation: listIn 0.35s ease-out;
}

.list-leave-active {
  animation: listIn 0.2s ease-in reverse;
}

.list-move {
  transition: transform 0.25s ease;
}

@keyframes listIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 900px) {
  .ticket-card { grid-template-columns: 1fr; }
  .ticket-left { border-right: none; border-bottom: 2px dashed rgba(148, 163, 184, 0.2); }
  .ticket-grid { grid-template-columns: repeat(2, 1fr); }
  .rv-time-big { font-size: 18px; }
}

.ticket-card {
  cursor: pointer;
}

.ticket-card .refund-btn {
  cursor: pointer;
}

.ticket-card:hover {
  transform: translateY(-2px);
  border-color: rgba(96, 165, 250, 0.4);
}

:deep(.ticket-detail-dialog .el-dialog) {
  background: rgba(15, 23, 42, 0.98);
  border: 1px solid rgba(96, 165, 250, 0.2);
  border-radius: 20px;
  backdrop-filter: blur(20px);
  box-shadow: 0 25px 80px rgba(0, 0, 0, 0.6), 0 0 60px rgba(59, 130, 246, 0.15);
  margin: 0;
}

:deep(.ticket-detail-dialog .el-dialog__body) {
  padding: 0;
}

.detail-wrap {
  position: relative;
  padding: 28px 28px 24px;
  color: #e2e8f0;
}

.detail-close {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(15, 23, 42, 0.6);
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 2;
}

.detail-close:hover {
  color: #f87171;
  border-color: rgba(239, 68, 68, 0.3);
  background: rgba(239, 68, 68, 0.1);
}

.detail-header {
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
}

.dh-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.dh-status {
  font-size: 13px;
  font-weight: 600;
  color: #cbd5e1;
}

.dh-train {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.dh-train-num {
  font-size: 26px;
  font-weight: 800;
  color: #93c5fd;
  letter-spacing: 1px;
}

.dh-sep {
  color: #475569;
}

.dh-type {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.detail-route {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.1);
}

.dr-station {
  flex: 0 0 auto;
  text-align: center;
}

.dr-station.end {
  order: 3;
}

.dr-time {
  display: block;
  font-size: 24px;
  font-weight: 800;
  color: #f1f5f9;
  margin-bottom: 6px;
  font-variant-numeric: tabular-nums;
}

.dr-name {
  font-size: 15px;
  color: #94a3b8;
  font-weight: 600;
}

.dr-line {
  flex: 1;
  position: relative;
  height: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dr-inner {
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, #3b82f6, #8b5cf6);
  border-radius: 2px;
}

.dr-duration {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  padding: 2px 10px;
  background: rgba(15, 23, 42, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  padding: 20px 0 16px;
}

.dg-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dg-label {
  font-size: 11px;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-weight: 700;
}

.dg-val {
  font-size: 14px;
  color: #cbd5e1;
  font-weight: 600;
  word-break: break-all;
}

.dg-val.mono {
  font-family: 'Consolas', 'Monaco', monospace;
  letter-spacing: 0.5px;
  font-size: 13px;
}

.dg-val.price {
  color: #fbbf24;
  font-size: 18px;
  font-weight: 800;
}

.dg-val.bold {
  font-weight: 800;
  font-size: 15px;
  color: #f1f5f9;
}

.detail-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.1);
}

.action-print {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border: 1px solid rgba(96, 165, 250, 0.45);
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.2), rgba(99, 102, 241, 0.15));
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #93c5fd;
  cursor: pointer;
  transition: all 0.2s;
}

.action-print:hover {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.35), rgba(99, 102, 241, 0.25));
  color: #dbeafe;
  transform: translateY(-1px);
}

.action-close {
  padding: 9px 22px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(30, 41, 59, 0.6);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}

.action-close:hover {
  color: #cbd5e1;
  border-color: rgba(148, 163, 184, 0.35);
}

.action-refund {
  padding: 9px 22px;
  border: 1px solid rgba(239, 68, 68, 0.4);
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.2), rgba(249, 115, 22, 0.15));
  border-radius: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #fda4af;
  cursor: pointer;
  transition: all 0.2s;
}

.action-refund:hover:not(:disabled) {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.3), rgba(249, 115, 22, 0.25));
  color: #fecaca;
}

.action-refund:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.eticket-bar {
  display: flex;
  gap: 24px;
  padding: 18px 20px;
  margin: 16px 0;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.08), rgba(139, 92, 246, 0.06));
  border: 1px solid rgba(59, 130, 246, 0.15);
  border-radius: 12px;
}

.eticket-qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.qr-grid {
  display: grid;
  grid-template-columns: repeat(11, 1fr);
  gap: 1px;
  padding: 6px;
  background: #f1f5f9;
  border-radius: 6px;
}

.qr-cell {
  width: 6px;
  height: 6px;
  background: transparent;
  border-radius: 1px;
}

.qr-cell.on {
  background: #0f172a;
}

.qr-label {
  font-size: 10px;
  color: #64748b;
  letter-spacing: 1px;
}

.eticket-barcode {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 0 8px;
}

.barcode-lines {
  display: flex;
  align-items: center;
  height: 40px;
  background: #ffffff;
  padding: 4px 12px;
  border-radius: 4px;
  gap: 1px;
}

.barcode-lines .bar {
  background: #0f172a;
  height: 100%;
}

.barcode-text {
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #94a3b8;
  text-align: center;
}

.empty-filter-svg {
  width: 200px;
  height: 150px;
}

.empty-filter-svg .ef-lens {
  animation: efPulse 2.8s ease-in-out infinite;
}

.empty-filter-svg .ef-inner {
  animation: efShine 2s ease-in-out infinite;
}

@keyframes efPulse {
  0%, 100% { transform: scale(1); opacity: 0.9; }
  50% { transform: scale(1.05); opacity: 1; }
}

@keyframes efShine {
  0%, 100% { opacity: 0.08; }
  50% { opacity: 0.2; }
}

.empty-filter-svg .ef-handle {
  animation: efWiggle 3s ease-in-out infinite;
  transform-origin: 64px 52px;
}

@keyframes efWiggle {
  0%, 100% { transform: rotate(0deg); }
  50% { transform: rotate(6deg); }
}

.empty-filter-svg .ef-ticket {
  animation: efTicket 2.2s ease-in-out infinite;
}

@keyframes efTicket {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}

.empty-filter-svg .ef-hole {
  animation: efHole 2s ease-in-out infinite;
}

@keyframes efHole {
  0%, 100% { fill: #475569; }
  50% { fill: #64748b; }
}
</style>