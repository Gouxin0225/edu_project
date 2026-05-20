<template>
  <div class="global-bg" aria-hidden="true">
    <div class="aurora-layer"></div>
    <div class="dot-grid"></div>
  </div>
  <el-config-provider :locale="zhCn" override>
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<style scoped>
.global-bg {
  position: fixed;
  inset: 0;
  background: #060B18;
  pointer-events: none;
  z-index: 0;
}

/* Aurora — radial-gradient 直接合成，不用 filter:blur，不受 overflow 裁切 */
.aurora-layer {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 900px 700px at -5% 0%,   rgba(48, 96, 220, 0.28) 0%, transparent 65%),
    radial-gradient(ellipse 700px 700px at 105% 95%, rgba(88, 48, 200, 0.22) 0%, transparent 65%),
    radial-gradient(ellipse 500px 400px at 55% 55%,  rgba(24, 72, 200, 0.14) 0%, transparent 65%);
  animation: auroraShift 22s ease-in-out infinite;
}

@keyframes auroraShift {
  0%   { opacity: 0.85; transform: scale(1)    translate(0px,   0px);  }
  25%  { opacity: 1;    transform: scale(1.04) translate(20px, -15px); }
  50%  { opacity: 0.9;  transform: scale(0.97) translate(-15px, 20px); }
  75%  { opacity: 1;    transform: scale(1.02) translate(10px,  10px); }
  100% { opacity: 0.85; transform: scale(1)    translate(0px,   0px);  }
}

/* 点阵网格 */
.dot-grid {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle, rgba(80, 140, 255, 0.22) 1px, transparent 1px);
  background-size: 28px 28px;
  background-position: 0 0;
}
</style>
