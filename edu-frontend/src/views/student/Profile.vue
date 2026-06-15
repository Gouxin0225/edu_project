<template>
  <div class="profile-page">
    <el-card shadow="never" class="profile-hero">
      <div class="hero-content">
        <div class="avatar-section">
          <div class="avatar-ring">
            <div class="avatar-inner">
              <span class="avatar-letter">{{ avatarLetter }}</span>
            </div>
          </div>
          <div class="avatar-badge">{{ roleText }}</div>
        </div>
        <div class="user-info-section">
          <h2 class="user-realname">{{ userStore.userInfo?.realName }}</h2>
          <div class="user-meta">
            <span class="meta-item">
              <el-icon><User /></el-icon>
              {{ roleText }}
            </span>
            <span class="meta-divider">·</span>
            <span class="meta-item">
              <el-icon><School /></el-icon>
              智慧教务管理平台
            </span>
          </div>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16" class="stats-row" v-loading="loadingStats">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-upcoming">
          <div class="stat-icon-wrap"><el-icon><Clock /></el-icon></div>
          <div class="stat-num">{{ stats.upcomingCount }}</div>
          <div class="stat-label">即将到期</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-pending">
          <div class="stat-icon-wrap"><el-icon><EditPen /></el-icon></div>
          <div class="stat-num">{{ stats.pendingCount }}</div>
          <div class="stat-label">待完成任务</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-done">
          <div class="stat-icon-wrap"><el-icon><CircleCheck /></el-icon></div>
          <div class="stat-num">{{ stats.completedCount }}</div>
          <div class="stat-label">已完成任务</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card stat-mistake">
          <div class="stat-icon-wrap"><el-icon><Warning /></el-icon></div>
          <div class="stat-num">{{ stats.mistakeCount }}</div>
          <div class="stat-label">待攻克错题</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never" class="completion-card">
          <template #header><span class="card-header">学习完成情况</span></template>
          <div class="ring-chart-wrap">
            <div class="ring-chart">
              <svg viewBox="0 0 120 120" width="160" height="160">
                <circle cx="60" cy="60" r="52" fill="none" stroke="rgba(0, 255, 255, 0.1)" stroke-width="14"/>
                <circle
                  cx="60" cy="60" r="52"
                  fill="none"
                  stroke="url(#ringGrad)"
                  stroke-width="14"
                  stroke-linecap="round"
                  stroke-dasharray="326.7"
                  :stroke-dashoffset="326.7 * (1 - completionRate)"
                  transform="rotate(-90 60 60)"
                  style="transition: stroke-dashoffset 1s ease"
                />
                <defs>
                  <linearGradient id="ringGrad" x1="0%" y1="0%" x2="100%" y2="0%">
                    <stop offset="0%" stop-color="#ff10f0"/>
                    <stop offset="100%" stop-color="#00ffff"/>
                  </linearGradient>
                </defs>
                <text x="60" y="55" text-anchor="middle" fill="#00ffff" font-size="20" font-weight="700" font-family="JetBrains Mono">
                  {{ Math.round(completionRate * 100) }}%
                </text>
                <text x="60" y="72" text-anchor="middle" fill="rgba(0, 255, 255, 0.5)" font-size="9" font-family="JetBrains Mono">
                  完成率
                </text>
              </svg>
            </div>
            <div class="ring-legend">
              <div class="legend-item">
                <span class="legend-dot" style="background: linear-gradient(135deg, #ff10f0, #00ffff)"></span>
                <span>已完成 {{ stats.completedCount }}</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background: rgba(0, 255, 255, 0.1); border: 1px solid rgba(0, 255, 255, 0.3)"></span>
                <span>待完成 {{ stats.pendingCount + stats.upcomingCount }}</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background: rgba(115, 87, 255, 0.55); border: 1px solid rgba(115, 87, 255, 0.85)"></span>
                <span>待批改 {{ stats.submittedCount }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="never" class="activity-card">
          <template #header>
            <div class="card-header-bar">
              <span class="card-header">近期任务</span>
              <el-tag size="small" class="cyberpunk-tag">{{ recentTasks.length }} 条</el-tag>
            </div>
          </template>
          <div v-if="recentTasks.length === 0" class="empty-tip">暂无任务记录</div>
          <div v-else class="task-list">
            <div v-for="task in recentTasks" :key="task.taskId + '-' + task.taskType" class="task-item">
              <div class="task-type-icon" :class="task.taskType === 'EXAM' ? 'icon-exam' : 'icon-hw'">
                <el-icon><component :is="task.taskType === 'EXAM' ? 'EditPen' : 'Notebook'" /></el-icon>
              </div>
              <div class="task-info">
                <div class="task-title">{{ task.title }}</div>
                <div class="task-sub">{{ task.taskType === 'EXAM' ? '考试' : '作业' }} · {{ task.deadline ? formatDate(task.deadline) : '-' }}</div>
              </div>
              <el-tag :type="statusTagType(task.status)" size="small" class="cyberpunk-tag">
                {{ statusText(task.status) }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getDashboard, getMistakeList } from '@/api/student'
import type { TaskVO } from '@/api/student'

const userStore = useUserStore()
const loadingStats = ref(false)

const stats = ref({
  upcomingCount: 0,
  pendingCount: 0,
  submittedCount: 0,
  completedCount: 0,
  mistakeCount: 0
})

const recentTasks = ref<TaskVO[]>([])

const avatarLetter = computed(() => {
  const name = userStore.userInfo?.realName || ''
  return name.charAt(0) || '?'
})

const roleText = computed(() => {
  const map: Record<string, string> = {
    STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员', ASSISTANT: '助教'
  }
  return map[userStore.userInfo?.role ?? ''] ?? ''
})

const completionRate = computed(() => {
  const total = stats.value.completedCount + stats.value.pendingCount + stats.value.upcomingCount + stats.value.submittedCount
  if (total === 0) return 0
  return stats.value.completedCount / total
})

function statusText(status: string) {
  const map: Record<string, string> = {
    UPCOMING: '即将开始', PENDING: '待完成', RETURNED: '退回待重交', SUBMITTED: '待批改', COMPLETED: '已完成'
  }
  return map[status] || status
}

function statusTagType(status: string): any {
  const map: Record<string, string> = {
    UPCOMING: 'warning', PENDING: 'danger', RETURNED: 'danger', SUBMITTED: 'info', COMPLETED: 'success'
  }
  return map[status] || 'info'
}

function formatDate(t: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 10)
}

async function loadData() {
  loadingStats.value = true
  try {
    const [dashRes, mistakeRes] = await Promise.all([
      getDashboard(),
      getMistakeList()
    ])
    const d = dashRes.data
    const todoTasks = d.todoTasks || d.pendingTasks || []
    stats.value.upcomingCount  = d.upcomingTasks?.length  ?? 0
    stats.value.pendingCount   = todoTasks.length
    stats.value.submittedCount = d.submittedTasks?.length ?? 0
    stats.value.completedCount = d.completedTasks?.length ?? 0
    stats.value.mistakeCount   = (mistakeRes.data || []).length

    const all: TaskVO[] = [
      ...todoTasks,
      ...(d.upcomingTasks || []),
      ...(d.submittedTasks || []),
      ...(d.completedTasks || [])
    ]
    recentTasks.value = all.slice(0, 10)
  } catch {
  } finally {
    loadingStats.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--bg-base);
  min-height: 100vh;
  padding: 0;
}

.profile-hero :deep(.el-card__body) {
  padding: 28px 32px;
  background: var(--bg-surface) !important;
}

.profile-hero {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
}

.hero-content {
  display: flex;
  align-items: center;
  gap: 28px;
}

.avatar-section {
  position: relative;
  flex-shrink: 0;
}

.avatar-ring {
  width: 88px;
  height: 88px;
  padding: 3px;
  background: linear-gradient(135deg, #ff10f0, #00ffff, #39ff14);
  clip-path: polygon(50% 0%, 100% 25%, 100% 75%, 50% 100%, 0% 75%, 0% 25%);
}

.avatar-inner {
  width: 100%;
  height: 100%;
  background: var(--bg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-letter {
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff10f0, #00ffff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-family: 'JetBrains Mono', monospace;
}

.avatar-badge {
  position: absolute;
  bottom: -2px;
  right: -2px;
  background: linear-gradient(135deg, #ff10f0, #00ffff);
  color: #000;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  clip-path: polygon(4px 0, 100% 0, calc(100% - 4px) 100%, 0 100%);
  font-family: 'JetBrains Mono', monospace;
}

.user-info-section {
  flex: 1;
}

.user-realname {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px;
  letter-spacing: 1px;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  font-family: 'JetBrains Mono', monospace;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-divider {
  color: var(--text-secondary);
}

.hero-actions {
  flex-shrink: 0;
}

.stats-row {
  margin-bottom: 0;
}

.stat-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.3);
}

:deep(.el-card__body) {
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 8px;
  background: var(--bg-surface) !important;
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-bottom: 4px;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px);
}

.stat-upcoming .stat-icon-wrap {
  background: rgba(255, 152, 0, 0.1);
  color: #ff9800;
  border: 1px solid rgba(255, 152, 0, 0.3);
}

.stat-pending .stat-icon-wrap {
  background: rgba(255, 16, 240, 0.1);
  color: #ff10f0;
  border: 1px solid rgba(255, 16, 240, 0.3);
}

.stat-done .stat-icon-wrap {
  background: rgba(57, 255, 20, 0.1);
  color: #39ff14;
  border: 1px solid rgba(57, 255, 20, 0.3);
}

.stat-mistake .stat-icon-wrap {
  background: rgba(0, 255, 255, 0.1);
  color: #00ffff;
  border: 1px solid rgba(0, 255, 255, 0.3);
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #ff10f0, #00ffff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  font-family: 'JetBrains Mono', monospace;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  letter-spacing: 0.5px;
  font-family: 'JetBrains Mono', monospace;
}

.completion-card :deep(.el-card__body) {
  padding: 20px;
  background: var(--bg-surface) !important;
}

.completion-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.3);
}

:deep(.el-card__header) {
  background: var(--bg-surface) !important;
  border-bottom: 1px solid var(--border) !important;
}

.card-header {
  font-weight: 600;
  font-size: 14px;
  font-family: 'JetBrains Mono', monospace;
  color: #00ffff;
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.4);
}

.ring-chart-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.ring-chart {
  position: relative;
}

.ring-legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  font-family: 'JetBrains Mono', monospace;
}

.legend-dot {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
}

.activity-card :deep(.el-card__body) {
  padding: 16px 20px;
  background: var(--bg-surface) !important;
}

.activity-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.3);
}

.card-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-tip {
  text-align: center;
  color: var(--text-secondary);
  padding: 30px 0;
  font-size: 13px;
  font-family: 'JetBrains Mono', monospace;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px solid var(--border);
}

.task-item:last-child {
  border-bottom: none;
}

.task-type-icon {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
}

.icon-exam {
  background: rgba(255, 16, 240, 0.1);
  color: #ff10f0;
  border: 1px solid rgba(255, 16, 240, 0.3);
}

.icon-hw {
  background: rgba(0, 255, 255, 0.1);
  color: #00ffff;
  border: 1px solid rgba(0, 255, 255, 0.3);
}

.task-info {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: 'JetBrains Mono', monospace;
}

.task-sub {
  font-size: 11px;
  color: var(--text-secondary);
  margin-top: 2px;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-btn {
  background: transparent !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px);
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.2);
}

.cyberpunk-btn:hover {
  background: #00ffff !important;
  color: #000 !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.5);
}

.cyberpunk-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid !important;
}

:deep(.el-tag--success) {
  border-color: #39ff14 !important;
  color: #39ff14 !important;
}

:deep(.el-tag--warning) {
  border-color: #ff9800 !important;
  color: #ff9800 !important;
}

:deep(.el-tag--danger) {
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
}

:deep(.el-tag--info) {
  border-color: #00ffff !important;
  color: #00ffff !important;
}

:deep(.el-loading-mask) {
  background-color: rgba(3, 3, 3, 0.9) !important;
}

:deep(.el-loading-spinner .circular) {
  stroke: #00ffff !important;
}
</style>
