<template>
  <div class="app-shell">
    <div class="bg-beams">
      <div class="beam beam-1"></div>
      <div class="beam beam-2"></div>
      <div class="beam beam-3"></div>
    </div>
    <div class="bg-orb orb-a"></div>
    <div class="bg-orb orb-b"></div>
    <div class="bg-orb orb-c"></div>

    <div id="top-progress"></div>

    <router-view v-slot="{ Component, route }">
      <Transition name="page" mode="out-in">
        <component :is="Component" :key="route.fullPath" />
      </Transition>
    </router-view>
    <BackToTop />
  </div>
</template>

<script setup>
import BackToTop from './components/BackToTop.vue'
</script>

<style>
.app-shell {
  position: relative;
  min-height: 100vh;
}

.bg-beams {
  position: fixed;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: -1;
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
  background: linear-gradient(180deg, transparent 0%, rgba(96, 165, 250, 0.12) 20%, rgba(147, 51, 234, 0.08) 50%, transparent 100%);
  transform: rotate(-15deg);
  animation: beamSweep 18s linear infinite;
}

.beam-2 {
  left: 55%;
  background: linear-gradient(180deg, transparent 0%, rgba(168, 85, 247, 0.1) 30%, rgba(59, 130, 246, 0.08) 60%, transparent 100%);
  transform: rotate(8deg);
  animation: beamSweep 22s linear infinite reverse;
}

.beam-3 {
  left: 80%;
  background: linear-gradient(180deg, transparent 0%, rgba(236, 72, 153, 0.08) 25%, rgba(99, 102, 241, 0.1) 55%, transparent 100%);
  transform: rotate(-5deg);
  animation: beamSweep 25s linear infinite;
}

@keyframes beamSweep {
  0% { opacity: 0; transform: translateX(-30%) rotate(-20deg); }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { opacity: 0; transform: translateX(30%) rotate(15deg); }
}

.bg-orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.35;
  pointer-events: none;
  z-index: -2;
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
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(60px, 40px) scale(1.1); }
  50% { transform: translate(30px, 80px) scale(0.95); }
  75% { transform: translate(-30px, 30px) scale(1.05); }
}

@keyframes orbFloatB {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-50px, -40px) scale(1.08); }
  66% { transform: translate(40px, -20px) scale(0.92); }
}

@keyframes orbFloatC {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-60px, 50px) scale(1.15); }
}

#top-progress {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  width: 0%;
  background: linear-gradient(90deg, #3b82f6 0%, #8b5cf6 50%, #ec4899 100%);
  box-shadow: 0 0 14px rgba(59, 130, 246, 0.7), 0 0 30px rgba(139, 92, 246, 0.4);
  z-index: 9999;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.25s ease;
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

.page-enter-active {
  animation: pageIn 0.4s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform, opacity;
}

.page-leave-active {
  animation: pageOut 0.2s cubic-bezier(0.4, 0, 1, 1);
  will-change: transform, opacity;
}

@keyframes pageIn {
  from {
    opacity: 0;
    transform: translateY(16px) scale(0.98);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

@keyframes pageOut {
  from {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  to {
    opacity: 0;
    transform: translateY(-8px) scale(0.98);
  }
}
</style>