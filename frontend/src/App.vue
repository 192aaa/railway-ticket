<template>
  <div class="app-shell">
    <canvas ref="particleCanvas" class="canvas-particles"></canvas>

    <div class="bg-beams">
      <div class="beam beam-1"></div>
      <div class="beam beam-2"></div>
      <div class="beam beam-3"></div>
    </div>
    <div class="bg-orb orb-a"></div>
    <div class="bg-orb orb-b"></div>
    <div class="bg-orb orb-c"></div>

    <div class="aurora">
      <div class="aurora-band band-1"></div>
      <div class="aurora-band band-2"></div>
      <div class="aurora-band band-3"></div>
    </div>

    <div class="cursor-glow" ref="cursorGlow"></div>
    <div class="shooting-stars" ref="shootingStars"></div>

    <div id="top-progress"></div>

    <div class="page-layer">
      <router-view v-slot="{ Component, route }">
        <component :is="Component" :key="route.fullPath" />
      </router-view>
      <BackToTop />
    </div>

    <Transition name="palette">
      <div v-if="paletteOpen" class="palette-overlay" @click.self="closePalette" @keydown.esc="closePalette">
        <div class="palette-box" role="dialog">
          <div class="palette-input-wrap">
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
              class="palette-search-icon"
            >
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              ref="paletteInputRef"
              v-model="paletteQuery"
              class="palette-input"
              placeholder="搜索热门路线、车次号、页面跳转..."
              @keydown.up.prevent="paletteUp"
              @keydown.down.prevent="paletteDown"
              @keydown.enter.prevent="paletteEnter"
            />
            <span class="palette-close-btn" @click="closePalette">ESC</span>
          </div>

          <div class="palette-body">
            <template v-if="paletteQuery.trim()">
              <div class="palette-section" v-if="paletteFilteredRoutes.length">
                <div class="palette-section-title">🚄 热门路线</div>
                <button
                  v-for="(r, i) in paletteFilteredRoutes"
                  :key="'r' + i"
                  class="palette-item"
                  :class="{ active: paletteActiveIdx === i }"
                  @click="paletteSelectRoute(r)"
                >
                  <span class="pi-icon">🎯</span>
                  <div class="pi-body">
                    <div class="pi-title">{{ r.from }} → {{ r.to }}</div>
                    <div class="pi-sub">{{ r.train }}</div>
                  </div>
                  <span class="pi-hint">{{ r.dur }}</span>
                </button>
              </div>

              <div class="palette-section" v-if="paletteFilteredPages.length">
                <div class="palette-section-title">📄 页面跳转</div>
                <button
                  v-for="(p, i) in paletteFilteredPages"
                  :key="'p' + i"
                  class="palette-item"
                  :class="{ active: paletteActiveIdx === paletteFilteredRoutes.length + i }"
                  @click="paletteGoPage(p)"
                >
                  <span class="pi-icon">{{ p.icon }}</span>
                  <div class="pi-body">
                    <div class="pi-title">{{ p.label }}</div>
                    <div class="pi-sub">{{ p.path }}</div>
                  </div>
                  <span class="pi-hint">↵</span>
                </button>
              </div>

              <div v-if="!paletteFilteredRoutes.length && !paletteFilteredPages.length" class="palette-empty">
                <div class="pe-emoji">🔍</div>
                <div class="pe-text">没有匹配的结果</div>
                <div class="pe-sub">试试输入「北京上海」「G7」或「订单」</div>
              </div>
            </template>
            <template v-else>
              <div class="palette-section">
                <div class="palette-section-title">⚡ 快捷页面</div>
                <button
                  v-for="(p, i) in palettePages"
                  :key="'pq' + i"
                  class="palette-item"
                  :class="{ active: paletteActiveIdx === i }"
                  @click="paletteGoPage(p)"
                >
                  <span class="pi-icon">{{ p.icon }}</span>
                  <div class="pi-body">
                    <div class="pi-title">{{ p.label }}</div>
                    <div class="pi-sub">{{ p.path }}</div>
                  </div>
                  <span class="pi-hint">↵</span>
                </button>
              </div>

              <div class="palette-section">
                <div class="palette-section-title">🔥 热门路线</div>
                <button
                  v-for="(r, i) in paletteHotRoutes"
                  :key="'ph' + i"
                  class="palette-item"
                  :class="{ active: paletteActiveIdx === palettePages.length + i }"
                  @click="paletteSelectRoute(r)"
                >
                  <span class="pi-icon">🎯</span>
                  <div class="pi-body">
                    <div class="pi-title">{{ r.from }} → {{ r.to }}</div>
                    <div class="pi-sub">{{ r.train }}</div>
                  </div>
                  <span class="pi-hint">{{ r.dur }}</span>
                </button>
              </div>
            </template>
          </div>

          <div class="palette-footer">
            <span class="pf-hint"><kbd>↑</kbd><kbd>↓</kbd> 选择</span>
            <span class="pf-hint"><kbd>↵</kbd> 确认</span>
            <span class="pf-hint"><kbd>Esc</kbd> 关闭</span>
            <span class="pf-hint"><kbd>Ctrl</kbd>+<kbd>K</kbd> 呼出</span>
          </div>
        </div>
      </div>
    </Transition>

    <div class="kbd-hint" v-if="!paletteOpen && showKbdHint" @click="openPalette">
      <kbd>Ctrl</kbd>+<kbd>K</kbd>
      <span class="kbd-label">搜索</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import BackToTop from './components/BackToTop.vue'

const router = useRouter()

const cursorGlow = ref(null)
const shootingStars = ref(null)
const particleCanvas = ref(null)

const paletteOpen = ref(false)
const paletteQuery = ref('')
const paletteInputRef = ref(null)
const paletteActiveIdx = ref(0)
const showKbdHint = ref(true)

const palettePages = [
  { label: '首页 · 查票', path: '/', icon: '🏠' },
  { label: '我的订单', path: '/orders', icon: '🎫' },
  { label: '消息中心', path: '/messages', icon: '💬' },
  { label: '个人中心', path: '/profile', icon: '👤' },
  { label: '数据大屏', path: '/dashboard', icon: '📊' },
  { label: '登录', path: '/login', icon: '🔐' },
  { label: '注册', path: '/register', icon: '✨' }
]

const paletteHotRoutes = [
  { from: '北京', to: '上海', dur: '4h30m', train: 'G7 · G101' },
  { from: '上海', to: '广州', dur: '7h', train: 'G85 · D931' },
  { from: '北京', to: '西安', dur: '4h15m', train: 'G19 · G87' },
  { from: '上海', to: '成都', dur: '10h', train: 'D352 · G2185' },
  { from: '广州', to: '深圳', dur: '1h', train: 'G6321 · C7001' },
  { from: '北京', to: '杭州', dur: '5h', train: 'G39 · G41' },
  { from: '上海', to: '南京', dur: '1h10m', train: 'G7001 · G101' },
  { from: '成都', to: '重庆', dur: '2h', train: 'G2204 · D2202' }
]

const paletteFilteredRoutes = computed(() => {
  const q = paletteQuery.value.trim().toLowerCase()
  if (!q) return paletteHotRoutes.value
  return paletteHotRoutes.value.filter((r) => {
    return (
      r.from.toLowerCase().includes(q) ||
      r.to.toLowerCase().includes(q) ||
      (r.from + r.to).toLowerCase().includes(q) ||
      (r.to + r.from).toLowerCase().includes(q) ||
      r.train.toLowerCase().includes(q)
    )
  })
})

const paletteFilteredPages = computed(() => {
  const q = paletteQuery.value.trim().toLowerCase()
  if (!q) return []
  return palettePages.value.filter((p) => {
    return p.label.toLowerCase().includes(q) || p.path.includes(q)
  })
})

function paletteTotalCount() {
  return paletteFilteredRoutes.value.length + paletteFilteredPages.value.length
}

function openPalette() {
  paletteOpen.value = true
  paletteQuery.value = ''
  paletteActiveIdx.value = 0
  showKbdHint.value = false
  setTimeout(() => paletteInputRef.value?.focus(), 60)
}

function closePalette() {
  paletteOpen.value = false
  paletteQuery.value = ''
  paletteActiveIdx.value = 0
}

function paletteUp() {
  const total = paletteTotalCount()
  if (total === 0) return
  paletteActiveIdx.value = paletteActiveIdx.value <= 0 ? total - 1 : paletteActiveIdx.value - 1
}

function paletteDown() {
  const total = paletteTotalCount()
  if (total === 0) return
  paletteActiveIdx.value = paletteActiveIdx.value >= total - 1 ? 0 : paletteActiveIdx.value + 1
}

function paletteEnter() {
  const routesLen = paletteFilteredRoutes.value.length
  if (paletteActiveIdx.value < routesLen) {
    paletteSelectRoute(paletteFilteredRoutes.value[paletteActiveIdx.value])
  } else {
    const pIdx = paletteActiveIdx.value - routesLen
    paletteGoPage(paletteFilteredPages.value[pIdx] || paletteFilteredPages.value[0])
  }
}

function paletteSelectRoute(r) {
  router.push({ path: '/', query: { from: r.from, to: r.to } })
  closePalette()
}

function paletteGoPage(p) {
  if (!p) return
  router.push(p.path)
  closePalette()
}

function onPaletteKeydown(e) {
  if (paletteOpen.value) return
  const tag = e.target?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || e.target?.isContentEditable) return
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    openPalette()
  }
  if (e.key === 'Escape' && paletteOpen.value) {
    closePalette()
  }
}

function onMouseMove(e) {
  if (cursorGlow.value) {
    cursorGlow.value.style.left = e.clientX + 'px'
    cursorGlow.value.style.top = e.clientY + 'px'
  }
  mouse.x = e.clientX
  mouse.y = e.clientY
}

let meteorTimer = null

function spawnMeteor() {
  if (!shootingStars.value) return
  const star = document.createElement('div')
  star.className = 'star'
  star.style.left = Math.random() * 50 + 10 + '%'
  star.style.top = Math.random() * 30 + '%'
  shootingStars.value.appendChild(star)
  requestAnimationFrame(() => star.classList.add('falling'))
  setTimeout(() => star.remove(), 3100)
}

let rafId = null
const mouse = { x: -9999, y: -9999 }
let particles = []

function initParticles() {
  const canvas = particleCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const dpr = Math.min(window.devicePixelRatio || 1, 2)

  function resize() {
    canvas.width = window.innerWidth * dpr
    canvas.height = window.innerHeight * dpr
    canvas.style.width = window.innerWidth + 'px'
    canvas.style.height = window.innerHeight + 'px'
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
  resize()

  const count = Math.min(140, Math.floor((window.innerWidth * window.innerHeight) / 12000))
  particles = Array.from({ length: count }, () => ({
    x: Math.random() * window.innerWidth,
    y: Math.random() * window.innerHeight,
    vx: (Math.random() - 0.5) * 0.4,
    vy: (Math.random() - 0.5) * 0.4,
    r: Math.random() * 1.4 + 0.5,
    hue: 210 + Math.random() * 80
  }))

  function tick() {
    const w = window.innerWidth
    const h = window.innerHeight
    ctx.clearRect(0, 0, w, h)

    for (const p of particles) {
      const dx = mouse.x - p.x
      const dy = mouse.y - p.y
      const dist = Math.hypot(dx, dy)
      if (dist < 160) {
        const f = ((160 - dist) / 160) * 0.02
        p.vx += dx * f
        p.vy += dy * f
      }

      p.x += p.vx
      p.y += p.vy
      p.vx *= 0.98
      p.vy *= 0.98

      if (p.x < 0 || p.x > w) p.vx *= -1
      if (p.y < 0 || p.y > h) p.vy *= -1
      p.x = Math.max(0, Math.min(w, p.x))
      p.y = Math.max(0, Math.min(h, p.y))

      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = `hsla(${p.hue}, 80%, 70%, 0.7)`
      ctx.fill()
    }

    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const a = particles[i]
        const b = particles[j]
        const dx = a.x - b.x
        const dy = a.y - b.y
        const d = Math.hypot(dx, dy)
        if (d < 120) {
          const alpha = (1 - d / 120) * 0.25
          ctx.strokeStyle = `rgba(120, 170, 255, ${alpha})`
          ctx.lineWidth = 0.6
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.stroke()
        }
      }
    }

    rafId = requestAnimationFrame(tick)
  }

  tick()
  window.addEventListener('resize', resize)
  particleCleanup = () => {
    window.removeEventListener('resize', resize)
  }
}

let particleCleanup = null

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove, { passive: true })
  window.addEventListener('keydown', onPaletteKeydown)
  meteorTimer = setInterval(spawnMeteor, 4200)
  setTimeout(spawnMeteor, 800)
  setTimeout(spawnMeteor, 2000)
  initParticles()
})

onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('keydown', onPaletteKeydown)
  if (meteorTimer) clearInterval(meteorTimer)
  if (rafId) cancelAnimationFrame(rafId)
  if (particleCleanup) particleCleanup()
})
</script>

<style>
.app-shell {
  position: relative;
  min-height: 100vh;
}

.page-layer {
  position: relative;
  z-index: 1;
}

.bg-beams {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: -4;
}

.beam {
  position: absolute;
  top: -20%;
  width: 1px;
  height: 140%;
  opacity: 0;
  transform-origin: center top;
}

.beam-1 {
  left: 25%;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(96, 165, 250, 0.12) 20%,
    rgba(147, 51, 234, 0.08) 50%,
    transparent 100%
  );
  transform: rotate(-15deg);
  animation: beamSweep 18s linear infinite;
}

.beam-2 {
  left: 55%;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(168, 85, 247, 0.1) 30%,
    rgba(59, 130, 246, 0.08) 60%,
    transparent 100%
  );
  transform: rotate(8deg);
  animation: beamSweep 22s linear infinite reverse;
}

.beam-3 {
  left: 80%;
  background: linear-gradient(
    180deg,
    transparent 0%,
    rgba(236, 72, 153, 0.08) 25%,
    rgba(99, 102, 241, 0.1) 55%,
    transparent 100%
  );
  transform: rotate(-5deg);
  animation: beamSweep 25s linear infinite;
}

@keyframes beamSweep {
  0% {
    opacity: 0;
    transform: translateX(-30%) rotate(-20deg);
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateX(30%) rotate(15deg);
  }
}

.bg-orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
  pointer-events: none;
  z-index: -4;
  mix-blend-mode: screen;
}

.orb-a {
  width: 520px;
  height: 520px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.6), transparent 70%);
  top: -10%;
  left: -8%;
  animation: orbFloatA 22s ease-in-out infinite;
}

.orb-b {
  width: 460px;
  height: 460px;
  background: radial-gradient(circle, rgba(139, 92, 246, 0.55), transparent 70%);
  bottom: -12%;
  right: -6%;
  animation: orbFloatB 26s ease-in-out infinite;
}

.orb-c {
  width: 360px;
  height: 360px;
  background: radial-gradient(circle, rgba(236, 72, 153, 0.45), transparent 70%);
  top: 40%;
  left: 45%;
  animation: orbFloatC 30s ease-in-out infinite;
}

@keyframes orbFloatA {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(60px, 40px) scale(1.1);
  }
  50% {
    transform: translate(30px, 80px) scale(0.95);
  }
  75% {
    transform: translate(-30px, 30px) scale(1.05);
  }
}

@keyframes orbFloatB {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(-50px, -40px) scale(1.08);
  }
  66% {
    transform: translate(40px, -20px) scale(0.92);
  }
}

@keyframes orbFloatC {
  0%,
  100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(-60px, 50px) scale(1.15);
  }
}

#top-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  width: 0%;
  background: linear-gradient(90deg, #3b82f6 0%, #8b5cf6 50%, #ec4899 100%);
  box-shadow:
    0 0 14px rgba(59, 130, 246, 0.7),
    0 0 30px rgba(139, 92, 246, 0.4);
  z-index: 2100;
  transition:
    width 0.35s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.25s ease;
  opacity: 0;
  border-radius: 0 2px 2px 0;
}

#top-progress::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: inherit;
  filter: blur(6px);
  opacity: 0.8;
}

:root {
  --surface: rgba(15, 23, 42, 0.7);
  --surface-solid: #0f172a;
  --surface-2: rgba(30, 41, 59, 0.6);
  --text-primary: #e2e8f0;
  --text-secondary: #94a3b8;
  --text-muted: #64748b;
  --border: rgba(148, 163, 184, 0.15);
  --border-strong: rgba(148, 163, 184, 0.25);
  --glass-bg: rgba(15, 23, 42, 0.55);
  --home-bg:
    radial-gradient(ellipse at 20% 10%, rgba(59, 130, 246, 0.15), transparent 50%),
    radial-gradient(ellipse at 80% 90%, rgba(139, 92, 246, 0.12), transparent 50%),
    linear-gradient(180deg, #020617 0%, #0f172a 100%);
}

html[data-theme='light'] {
  --surface: rgba(255, 255, 255, 0.85);
  --surface-solid: #ffffff;
  --surface-2: rgba(240, 249, 255, 0.9);
  --text-primary: #0f172a;
  --text-secondary: #475569;
  --text-muted: #94a3b8;
  --border: rgba(148, 163, 184, 0.2);
  --border-strong: rgba(148, 163, 184, 0.35);
  --glass-bg: rgba(255, 255, 255, 0.65);
  --home-bg:
    radial-gradient(ellipse at 15% 0%, rgba(167, 139, 250, 0.35), transparent 50%),
    radial-gradient(ellipse at 85% 5%, rgba(59, 130, 246, 0.28), transparent 48%),
    radial-gradient(ellipse at 75% 95%, rgba(236, 72, 153, 0.22), transparent 52%),
    radial-gradient(ellipse at 10% 85%, rgba(6, 182, 212, 0.25), transparent 50%),
    linear-gradient(180deg, #eef2ff 0%, #fdf4ff 40%, #fdf2f8 70%, #ecfeff 100%);
}

html[data-theme='light'] .glass-card {
  background: rgba(255, 255, 255, 0.68) !important;
  border-color: rgba(255, 255, 255, 0.8) !important;
  backdrop-filter: blur(20px) saturate(1.4);
  box-shadow:
    0 4px 24px rgba(99, 102, 241, 0.08),
    0 1px 3px rgba(15, 23, 42, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}
html[data-theme='light'] .glass-card * {
  color: #1e293b;
}

html[data-theme='light'] .glass {
  background: rgba(255, 255, 255, 0.58) !important;
  border-color: rgba(255, 255, 255, 0.75) !important;
  backdrop-filter: blur(20px) saturate(1.4);
  box-shadow:
    0 4px 20px rgba(99, 102, 241, 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

html[data-theme='light'] .home,
html[data-theme='light'] .profile-page,
html[data-theme='light'] .orders-page,
html[data-theme='light'] .login-page,
html[data-theme='light'] .register-page,
html[data-theme='light'] .messages-page {
  background: var(--home-bg) !important;
}

html[data-theme='light'] .results-header h2 {
  color: #1e293b !important;
}

html[data-theme='light'] .hero-sub {
  color: #64748b !important;
}

html[data-theme='light'] .filter-btn {
  background: rgba(255, 255, 255, 0.72) !important;
  color: #475569 !important;
  border-color: rgba(199, 210, 254, 0.6) !important;
  box-shadow: 0 1px 3px rgba(99, 102, 241, 0.06);
}
html[data-theme='light'] .filter-btn:hover {
  background: rgba(199, 210, 254, 0.7) !important;
  color: #4338ca !important;
  border-color: rgba(139, 92, 246, 0.4) !important;
}
html[data-theme='light'] .filter-btn.active {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.18), rgba(139, 92, 246, 0.18)) !important;
  color: #4f46e5 !important;
  border-color: rgba(99, 102, 241, 0.5) !important;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.15);
}

html[data-theme='light'] .filter-bar {
  background: rgba(255, 255, 255, 0.68) !important;
  border-color: rgba(255, 255, 255, 0.8) !important;
  box-shadow:
    0 4px 16px rgba(99, 102, 241, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(16px);
}
html[data-theme='light'] .filter-label {
  color: #6366f1 !important;
  font-weight: 600 !important;
}

html[data-theme='light'] .train-card {
  background: rgba(255, 255, 255, 0.72) !important;
  border-color: rgba(199, 210, 254, 0.5) !important;
  box-shadow:
    0 2px 12px rgba(99, 102, 241, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(16px);
}
html[data-theme='light'] .train-card * {
  color: #1e293b;
}

html[data-theme='light'] .bg-orb {
  opacity: 0.45;
}

html[data-theme='light'] .beam-1,
html[data-theme='light'] .beam-2,
html[data-theme='light'] .beam-3 {
  opacity: 0.5;
}

.palette-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  background: rgba(2, 6, 23, 0.72);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 10vh;
}

.palette-box {
  width: min(640px, 92vw);
  max-height: 75vh;
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.95), rgba(15, 23, 42, 0.97));
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 16px;
  box-shadow:
    0 30px 60px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(96, 165, 250, 0.1),
    0 0 80px rgba(99, 102, 241, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.palette-input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.15);
}

.palette-search-icon {
  color: #94a3b8;
  flex-shrink: 0;
}

.palette-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: #f1f5f9;
  font-size: 15px;
  font-family: inherit;
  caret-color: #60a5fa;
}

.palette-input::placeholder {
  color: #64748b;
}

.palette-close-btn {
  font-size: 10px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(148, 163, 184, 0.15);
  color: #94a3b8;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  letter-spacing: 0.5px;
}

.palette-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.palette-body::-webkit-scrollbar {
  width: 6px;
}
.palette-body::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 3px;
}

.palette-section {
  margin-bottom: 10px;
}

.palette-section-title {
  padding: 10px 12px 6px;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: #64748b;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition:
    background 0.12s,
    border-color 0.12s;
  color: #f1f5f9;
}

.palette-item:hover,
.palette-item.active {
  background: rgba(59, 130, 246, 0.15);
  border-color: rgba(96, 165, 250, 0.3);
}

.palette-item.active {
  box-shadow: inset 0 0 0 1px rgba(96, 165, 250, 0.25);
}

.pi-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.pi-body {
  flex: 1;
  min-width: 0;
}

.pi-title {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
  margin-bottom: 2px;
}

.pi-sub {
  font-size: 11px;
  color: #64748b;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
}

.pi-hint {
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  padding: 2px 8px;
  background: rgba(148, 163, 184, 0.1);
  border-radius: 6px;
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  flex-shrink: 0;
}

.palette-empty {
  text-align: center;
  padding: 40px 20px;
}

.pe-emoji {
  font-size: 40px;
  margin-bottom: 12px;
}
.pe-text {
  font-size: 15px;
  font-weight: 600;
  color: #cbd5e1;
  margin-bottom: 6px;
}
.pe-sub {
  font-size: 12px;
  color: #64748b;
}

.palette-footer {
  display: flex;
  gap: 14px;
  padding: 10px 16px;
  border-top: 1px solid rgba(148, 163, 184, 0.15);
  font-size: 11px;
  color: #64748b;
}

.pf-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.palette-footer kbd {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 5px;
  border-radius: 4px;
  background: rgba(148, 163, 184, 0.15);
  border: 1px solid rgba(148, 163, 184, 0.25);
  color: #94a3b8;
}

.palette-enter-active {
  transition: opacity 0.18s ease;
}

.palette-enter-active .palette-box {
  animation: paletteBoxIn 0.22s cubic-bezier(0.16, 1, 0.3, 1);
}

.palette-leave-active {
  transition: opacity 0.14s ease;
}

.palette-enter-from,
.palette-leave-to {
  opacity: 0;
}

@keyframes paletteBoxIn {
  from {
    transform: translateY(-12px) scale(0.97);
    opacity: 0;
  }
  to {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
}

.kbd-hint {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px 6px 6px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.2);
  backdrop-filter: blur(12px);
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.kbd-hint:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(99, 102, 241, 0.25);
}

.kbd-hint kbd {
  font-family: 'SF Mono', 'JetBrains Mono', monospace;
  font-size: 10px;
  font-weight: 800;
  padding: 3px 6px;
  border-radius: 5px;
  background: rgba(59, 130, 246, 0.2);
  border: 1px solid rgba(96, 165, 250, 0.35);
  color: #93c5fd;
}

.kbd-hint .kbd-label {
  font-size: 11px;
  font-weight: 600;
  color: #cbd5e1;
  padding-left: 4px;
  border-left: 1px solid rgba(148, 163, 184, 0.25);
  margin-left: 2px;
}

html[data-theme='light'] .palette-overlay {
  background: rgba(15, 23, 42, 0.3);
}

html[data-theme='light'] .palette-box {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.99));
  border-color: rgba(148, 163, 184, 0.35);
  box-shadow:
    0 30px 60px rgba(15, 23, 42, 0.2),
    0 0 0 1px rgba(96, 165, 250, 0.08);
}

html[data-theme='light'] .palette-input {
  color: #0f172a;
}
html[data-theme='light'] .palette-input::placeholder {
  color: #94a3b8;
}
html[data-theme='light'] .palette-search-icon {
  color: #64748b;
}
html[data-theme='light'] .palette-close-btn {
  background: rgba(148, 163, 184, 0.2);
  color: #64748b;
}
html[data-theme='light'] .palette-item {
  color: #1e293b;
}
html[data-theme='light'] .palette-item:hover,
html[data-theme='light'] .palette-item.active {
  background: rgba(59, 130, 246, 0.1);
  border-color: rgba(37, 99, 235, 0.35);
}
html[data-theme='light'] .pi-title {
  color: #0f172a;
}
html[data-theme='light'] .pi-sub {
  color: #64748b;
}
html[data-theme='light'] .pi-hint {
  background: rgba(148, 163, 184, 0.18);
  color: #64748b;
}
html[data-theme='light'] .palette-section-title {
  color: #64748b;
}
html[data-theme='light'] .palette-footer {
  color: #64748b;
  border-color: rgba(148, 163, 184, 0.2);
}
html[data-theme='light'] .palette-footer kbd {
  background: rgba(148, 163, 184, 0.18);
  border-color: rgba(148, 163, 184, 0.3);
  color: #64748b;
}
html[data-theme='light'] .palette-body {
  border-color: rgba(148, 163, 184, 0.2);
}
html[data-theme='light'] .palette-input-wrap {
  border-color: rgba(148, 163, 184, 0.2);
}

html[data-theme='light'] .kbd-hint {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(148, 163, 184, 0.3);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
}
html[data-theme='light'] .kbd-hint kbd {
  background: rgba(59, 130, 246, 0.12);
  border-color: rgba(37, 99, 235, 0.3);
  color: #2563eb;
}
html[data-theme='light'] .kbd-hint .kbd-label {
  color: #334155;
  border-color: rgba(148, 163, 184, 0.25);
}

.page-enter-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}
.page-leave-active {
  transition:
    opacity 0.15s ease,
    transform 0.15s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
