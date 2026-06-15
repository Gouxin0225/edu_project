<template>
  <div class="page-container">
    <el-card shadow="never" class="cyber-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">我的作业</span>
        </div>
      </template>
      <el-empty description="功能开发中...">
        <template #image>
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="#00ffff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 17L12 22L22 17" stroke="#00ffff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M2 12L12 17L22 12" stroke="#00ffff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
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
  background: var(--bg-surface);
  min-height: 100%;
  font-family: 'JetBrains Mono', monospace;
}

.cyber-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
  height: calc(100vh - 120px);
}

.cyber-card :deep(.el-card__header) {
  background: rgba(26, 26, 46, 0.4) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 15px 20px !important;
}

.cyber-card :deep(.el-card__body) {
  background: var(--bg-surface) !important;
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
  color: #00ffff !important;
  font-weight: 600 !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.4) !important;
  letter-spacing: 1px;
}

.cyber-card :deep(.el-empty__description) {
  color: rgba(255, 255, 255, 0.4) !important;
}
</style>