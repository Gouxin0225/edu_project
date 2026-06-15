<template>
  <div class="page-container">
    <el-card shadow="never" class="cyber-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">班级管理</span>
        </div>
      </template>
      <el-empty description="功能开发中...">
        <template #image>
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M17 21V19C17 17.9391 16.5786 16.9217 15.8284 16.1716C15.0783 15.4214 14.0609 15 13 15H5C3.93913 15 2.92172 15.4214 2.17157 16.1716C1.42143 16.9217 1 17.9391 1 19V21" stroke="#00ffff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="9" cy="7" r="4" stroke="#00ffff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M23 21V19C22.9993 18.1137 22.7044 17.2528 22.1614 16.5523C21.6184 15.8519 20.8581 15.3516 20 15.13" stroke="#00ffff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M16 3.13C16.8604 3.35031 17.623 3.85071 18.1676 4.55232C18.7122 5.25392 19.0078 6.11683 19.0078 7.005C19.0078 7.89318 18.7122 8.75608 18.1676 9.45769C17.623 10.1593 16.8604 10.6597 16 10.88" stroke="#00ffff" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
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