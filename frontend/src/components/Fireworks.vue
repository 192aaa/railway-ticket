<template>
  <div class="fireworks-root" ref="root" @click="burstAt($event)">
    <canvas ref="canvas"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({ show: Boolean })
const emit = defineEmits(['done'])

const root = ref(null)
const canvas = ref(null)

let ctx = null
let W = 0, H = 0, DPR = 1
let animId = null
let rockets = []
let particles = []
let startTime = 0
let launched = 0

const colors = [
  '#ef4444', '#f97316', '#f59e0b', '#eab308',
  '#84cc16', '#22c55e', '#10b981', '#14b8a6',
  '#06b6d4', '#0ea5e9', '#3b82f6', '#6366f1',
  '#8b5cf6', '#a855f7', '#d946ef', '#ec4899',
  '#f43f5e', '#fbbf24'
]

function resize() {
  const r = root.value
  if (!r || !canvas.value) return
  DPR = window.devicePixelRatio || 1
  W = r.clientWidth
  H = r.clientHeight
  canvas.value.width = W * DPR
  canvas.value.height = H * DPR
  canvas.value.style.width = W + 'px'
  canvas.value.style.height = H + 'px'
  ctx.setTransform(DPR, 0, 0, DPR, 0, 0)
}

function rand(a, b) { return a + Math.random() * (b - a) }

function launchRocket(x, y, targetX, targetY, color) {
  rockets.push({
    x: x, y: y,
    vx: (targetX - x) / 40,
    vy: (targetY - y) / 40 - 0.15,
    targetY: targetY,
    color: color || colors[Math.floor(Math.random() * colors.length)],
    trail: []
  })
}

function explode(x, y, color, big = false) {
  const count = big ? 120 : 80
  const palette = color ? [color, '#ffffff', '#fef3c7'] : [
    colors[Math.floor(Math.random() * colors.length)],
    colors[Math.floor(Math.random() * colors.length)],
    '#ffffff'
  ]
  for (let i = 0; i < count; i++) {
    const angle = (Math.PI * 2 * i) / count + rand(-0.1, 0.1)
    const speed = rand(big ? 3 : 2, big ? 7 : 5)
    particles.push({
      x, y,
      vx: Math.cos(angle) * speed,
      vy: Math.sin(angle) * speed,
      life: 1,
      decay: rand(0.008, 0.02),
      color: palette[Math.floor(Math.random() * palette.length)],
      size: rand(1.5, 3.5),
      gravity: 0.05,
      trail: []
    })
  }
}

function burstAt(e) {
  const rect = root.value.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  launchRocket(W / 2, H, x, y)
  explode(x, y)
}

function update() {
  ctx.fillStyle = 'rgba(10, 12, 28, 0.22)'
  ctx.fillRect(0, 0, W, H)

  for (let i = rockets.length - 1; i >= 0; i--) {
    const r = rockets[i]
    r.trail.push({ x: r.x, y: r.y })
    if (r.trail.length > 8) r.trail.shift()
    r.x += r.vx
    r.y += r.vy
    r.vy += 0.015

    ctx.beginPath()
    ctx.moveTo(r.trail[0].x, r.trail[0].y)
    for (let j = 1; j < r.trail.length; j++) {
      ctx.lineTo(r.trail[j].x, r.trail[j].y)
    }
    ctx.strokeStyle = r.color
    ctx.lineWidth = 2
    ctx.globalAlpha = 0.6
    ctx.stroke()
    ctx.globalAlpha = 1

    ctx.beginPath()
    ctx.arc(r.x, r.y, 3, 0, Math.PI * 2)
    ctx.fillStyle = '#fff'
    ctx.fill()

    if (r.y <= r.targetY || r.vy >= 0) {
      explode(r.x, r.y, r.color, true)
      rockets.splice(i, 1)
    }
  }

  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i]
    p.trail.push({ x: p.x, y: p.y })
    if (p.trail.length > 5) p.trail.shift()

    p.vy += p.gravity
    p.vx *= 0.985
    p.vy *= 0.985
    p.x += p.vx
    p.y += p.vy
    p.life -= p.decay

    if (p.life <= 0) { particles.splice(i, 1); continue }

    ctx.beginPath()
    ctx.arc(p.x, p.y, p.size * p.life, 0, Math.PI * 2)
    ctx.fillStyle = p.color
    ctx.globalAlpha = p.life
    ctx.shadowBlur = 8
    ctx.shadowColor = p.color
    ctx.fill()
    ctx.shadowBlur = 0
    ctx.globalAlpha = 1
  }
}

function animate() {
  const elapsed = Date.now() - startTime

  if (elapsed < 2500 && Math.random() < 0.04 + launched * 0.003) {
    const tx = rand(W * 0.2, W * 0.8)
    const ty = rand(H * 0.15, H * 0.5)
    launchRocket(rand(W * 0.1, W * 0.9), H + 10, tx, ty)
    launched++
  }

  update()

  if (elapsed > 4500 && particles.length === 0 && rockets.length === 0) {
    emit('done')
    return
  }

  animId = requestAnimationFrame(animate)
}

function startShow() {
  if (!ctx || !root.value) return
  resize()
  rockets = []
  particles = []
  launched = 0
  startTime = Date.now()

  for (let i = 0; i < 6; i++) {
    setTimeout(() => {
      const tx = rand(W * 0.2, W * 0.8)
      const ty = rand(H * 0.15, H * 0.45)
      launchRocket(rand(W * 0.1, W * 0.9), H + 10, tx, ty)
    }, i * 180)
  }

  animate()
}

function stopShow() {
  if (animId) cancelAnimationFrame(animId)
  animId = null
  if (ctx && canvas.value) {
    ctx.clearRect(0, 0, canvas.value.width, canvas.value.height)
  }
  rockets = []
  particles = []
}

watch(() => props.show, (v) => {
  if (v) startShow()
  else stopShow()
})

onMounted(() => {
  ctx = canvas.value.getContext('2d')
  window.addEventListener('resize', resize)
  resize()
  if (props.show) startShow()
})

onUnmounted(() => {
  stopShow()
  window.removeEventListener('resize', resize)
})
</script>

<style scoped>
.fireworks-root {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9998;
  overflow: hidden;
  background: rgba(10, 12, 28, 0);
}
.fireworks-root canvas {
  display: block;
}
</style>