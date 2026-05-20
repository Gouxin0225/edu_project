<template>
  <div class="page-container">
    <el-card shadow="never" class="cyber-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">用户管理</span>
        </div>
      </template>
      <el-empty description="功能开发中...">
        <template #image>
          <svg width="80" height="80" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M20 21V19C20 17.9391 19.5786 16.9217 18.8284 16.1716C18.0783 15.4214 17.0609 15 16 15H8C6.93913 15 5.92172 15.4214 5.17157 16.1716C4.42143 16.9217 4 17.9391 4 19V21" stroke="#ff9800" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="12" cy="7" r="4" stroke="#ff9800" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
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
  box-shadow: 0 0 30px rgba(255, 152, 0, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
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
  color: #ff9800 !important;
  font-weight: 600 !important;
  text-shadow: 0 0 10px rgba(255, 152, 0, 0.4) !important;
  letter-spacing: 1px;
}

.cyber-card :deep(.el-empty__description) {
  color: rgba(255, 255, 255, 0.4) !important;
}
</style>