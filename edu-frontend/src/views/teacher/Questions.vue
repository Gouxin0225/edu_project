<template>
  <div class="page-container">
    <el-card shadow="never" class="cyber-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">题库管理</span>
        </div>
      </template>
      <el-empty description="功能开发中...">
        <template #image>
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="12" cy="12" r="10" stroke="#ff10f0" stroke-width="1.5"/>
            <path d="M9.09 9C9.3251 8.33167 9.78915 7.76811 10.3994 7.40913C11.0096 7.05016 11.7286 6.91894 12.4272 7.03871C13.1258 7.15849 13.7588 7.52152 14.2151 8.06353C14.6713 8.60553 14.9201 9.29152 14.92 10C14.92 12 9.92 12 9.92 10" stroke="#ff10f0" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M12 17H12.01" stroke="#ff10f0" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </template>
      </el-empty>
    </el-card>
  </div>
</template>
<script setup lang="ts"></script>
<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.page-container {
  padding: 0;
  background: #030303;
  min-height: 100%;
  font-family: 'JetBrains Mono', monospace;
}

.cyber-card {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 30px rgba(255, 16, 240, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
  height: calc(100vh - 120px);
}

.cyber-card :deep(.el-card__header) {
  background: rgba(26, 26, 46, 0.4) !important;
  border-bottom: 1px solid #1a1a2e !important;
  padding: 15px 20px !important;
}

.cyber-card :deep(.el-card__body) {
  background: #0a0a0a !important;
  padding: 40px 20px !important;
  height: calc(100% - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-header {
  display: flex;
  align-items: center;
}

.card-title {
  color: #ff10f0 !important;
  font-weight: 600 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.4) !important;
  letter-spacing: 1px;
}

.cyber-card :deep(.el-empty__description) {
  color: rgba(255, 255, 255, 0.4) !important;
}
</style>