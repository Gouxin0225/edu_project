<template>
  <div class="global-bg" aria-hidden="true">
    <div class="aurora-layer"></div>
    <div class="dot-grid"></div>
  </div>
  <el-config-provider :locale="zhCn" override>
    <ThemeToggle v-if="showFloatingThemeToggle" variant="floating" />
    <router-view />
    <FloatingMascot />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElConfigProvider } from 'element-plus/es/components/config-provider/index'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import FloatingMascot from '@/components/FloatingMascot.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const route = useRoute()
const showFloatingThemeToggle = computed(() => route.meta.requiresAuth === false)
</script>

<style scoped>
.global-bg {
  position: fixed;
  inset: 0;
  background: var(--bg-base);
  pointer-events: none;
  z-index: 0;
  transition: background 0.2s ease;
  overflow: hidden;
}

.global-bg::before,
.global-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.global-bg::before {
  background:
    linear-gradient(90deg, var(--circuit-line) 1px, transparent 1px),
    linear-gradient(0deg, var(--circuit-line) 1px, transparent 1px),
    linear-gradient(135deg, transparent 0 47%, var(--circuit-line-strong) 47% 49%, transparent 49% 100%);
  background-size: 96px 96px, 96px 96px, 192px 192px;
  opacity: 0.72;
  animation: circuitDrift 34s linear infinite;
}

.global-bg::after {
  inset: auto;
  top: 0;
  bottom: 0;
  width: 42%;
  left: -48%;
  background: linear-gradient(90deg, transparent, var(--scanline), transparent);
  transform: skewX(-18deg);
  animation: bgSweep 9s ease-in-out infinite;
}

.aurora-layer {
  position: absolute;
  inset: 0;
  background: var(--aurora-bg);
  animation: auroraShift 22s ease-in-out infinite;
  transition: background 0.2s ease;
}

@keyframes auroraShift {
  0%   { opacity: 0.85; transform: scale(1)    translate(0px,   0px);  }
  25%  { opacity: 1;    transform: scale(1.04) translate(20px, -15px); }
  50%  { opacity: 0.9;  transform: scale(0.97) translate(-15px, 20px); }
  75%  { opacity: 1;    transform: scale(1.02) translate(10px,  10px); }
  100% { opacity: 0.85; transform: scale(1)    translate(0px,   0px);  }
}

.dot-grid {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, var(--dot-grid-color) 1px, transparent 1px);
  background-size: 28px 28px;
  background-position: 0 0;
  opacity: var(--dot-grid-opacity);
}

@keyframes bgSweep {
  0%, 18%   { transform: translateX(0) skewX(-18deg); opacity: 0; }
  34%      { opacity: 0.7; }
  74%, 100% { transform: translateX(360%) skewX(-18deg); opacity: 0; }
}
</style>
