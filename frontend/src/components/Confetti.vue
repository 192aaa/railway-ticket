<template>
  <div class="confetti-root" ref="root">
    <div
      v-for="p in pieces"
      :key="p.id"
      class="confetti-piece"
      :class="p.type"
      :style="p.style"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const emit = defineEmits(['done'])

const colors = [
  '#3b82f6', '#6366f1', '#8b5cf6', '#a855f7',
  '#ec4899', '#f472b6', '#f59e0b', '#10b981',
  '#06b6d4', '#60a5fa', '#f97316', '#14b8a6'
]

const pieces = ref([])

onMounted(() => {
  const arr = []
  let id = 0

  for (let i = 0; i < 140; i++) {
    const angle = -Math.PI / 2 + (Math.random() - 0.5) * Math.PI * 0.8
    const speed = 250 + Math.random() * 400
    arr.push({
      id: id++,
      type: Math.random() > 0.7 ? 'star' : 'square',
      style: {
        '--x': `${Math.cos(angle) * speed}px`,
        '--y': `${Math.sin(angle) * speed - 200}px`,
        '--rot': `${Math.random() * 1080 - 540}deg`,
        '--delay': `${Math.random() * 0.4}s`,
        '--dur': `${2.2 + Math.random() * 1.8}s`,
        '--bg': colors[Math.floor(Math.random() * colors.length)],
        '--size': `${6 + Math.random() * 10}px`,
        left: `${35 + Math.random() * 30}%`,
        top: '40%',
        borderRadius: Math.random() > 0.5 ? '2px' : '50%'
      }
    })
  }

  for (let i = 0; i < 50; i++) {
    const angle = Math.random() * Math.PI * 2
    const speed = 150 + Math.random() * 280
    arr.push({
      id: id++,
      type: 'ribbon',
      style: {
        '--x': `${Math.cos(angle) * speed}px`,
        '--y': `${Math.sin(angle) * speed - 100}px`,
        '--rot': `${Math.random() * 720 - 360}deg`,
        '--delay': `${Math.random() * 0.5}s`,
        '--dur': `${2.8 + Math.random() * 1.4}s`,
        '--bg': colors[Math.floor(Math.random() * colors.length)],
        left: `${30 + Math.random() * 40}%`,
        top: `${20 + Math.random() * 30}%`
      }
    })
  }

  pieces.value = arr

  setTimeout(() => emit('done'), 4500)
})
</script>

<style scoped>
.confetti-root {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9999;
  overflow: hidden;
}

.confetti-piece {
  position: absolute;
  animation: confetti-pop var(--dur) cubic-bezier(0.25, 0.46, 0.45, 0.94) var(--delay) forwards;
  filter: drop-shadow(0 0 2px rgba(0, 0, 0, 0.3));
}

.confetti-piece.square {
  width: var(--size);
  height: var(--size);
  background: var(--bg);
  transform: translate(-50%, -50%) rotate(0deg) scale(1);
}

.confetti-piece.star {
  width: var(--size);
  height: var(--size);
  background: var(--bg);
  clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);
  transform: translate(-50%, -50%) rotate(0deg) scale(0);
  animation-name: confetti-star;
}

.confetti-piece.ribbon {
  width: 6px;
  height: 28px;
  background: var(--bg);
  border-radius: 3px;
  transform: translate(-50%, -50%) rotate(0deg);
  animation-name: confetti-ribbon;
}

@keyframes confetti-pop {
  0% {
    transform: translate(-50%, -50%) rotate(0deg) scale(1);
    opacity: 1;
  }
  70% { opacity: 1; }
  100% {
    transform: translate(calc(-50% + var(--x)), calc(-50% + var(--y) + 500px)) rotate(var(--rot)) scale(0.5);
    opacity: 0;
  }
}

@keyframes confetti-star {
  0% {
    transform: translate(-50%, -50%) rotate(0deg) scale(0);
    opacity: 0;
  }
  15% {
    transform: translate(-50%, -50%) rotate(0deg) scale(1.3);
    opacity: 1;
  }
  70% { opacity: 1; }
  100% {
    transform: translate(calc(-50% + var(--x)), calc(-50% + var(--y) + 500px)) rotate(var(--rot)) scale(0.4);
    opacity: 0;
  }
}

@keyframes confetti-ribbon {
  0% {
    transform: translate(-50%, -50%) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translate(calc(-50% + var(--x)), calc(-50% + var(--y) + 600px)) rotate(calc(var(--rot) * 2));
    opacity: 0;
  }
}
</style>