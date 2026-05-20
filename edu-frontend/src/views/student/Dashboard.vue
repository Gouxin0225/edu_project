<template>
  <div class="dashboard">
    <el-row :gutter="16" class="summary-row">
      <el-col :span="8">
        <el-card class="summary-card upcoming-card" shadow="never">
          <div class="summary-content">
            <div class="summary-num">{{ dashboard.upcomingTasks.length }}</div>
            <div class="summary-label">即将截止</div>
          </div>
          <el-icon class="summary-icon"><Timer /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="summary-card pending-card" shadow="never">
          <div class="summary-content">
            <div class="summary-num">{{ dashboard.pendingTasks.length }}</div>
            <div class="summary-label">待处理</div>
          </div>
          <el-icon class="summary-icon"><List /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="summary-card completed-card" shadow="never">
          <div class="summary-content">
            <div class="summary-num">{{ dashboard.completedTasks.length }}</div>
            <div class="summary-label">已完成</div>
          </div>
          <el-icon class="summary-icon"><CircleCheck /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" v-loading="loading">
      <el-col :span="8">
        <el-card shadow="never" class="board-card">
          <template #header>
            <div class="board-header upcoming-header">
              <el-icon><Timer /></el-icon>
              <span>即将截止</span>
              <el-tag type="danger" size="small">{{ dashboard.upcomingTasks.length }}</el-tag>
            </div>
          </template>
          <div class="task-list">
            <div
              v-for="task in dashboard.upcomingTasks"
              :key="task.taskId"
              class="task-card upcoming-task"
              @click="goToTask(task)"
            >
              <div class="task-top">
                <span class="task-title">{{ task.title }}</span>
                <el-tag :type="task.taskType === 'EXAM' ? 'danger' : 'warning'" size="small">
                  {{ task.taskType === 'EXAM' ? '考试' : '作业' }}
                </el-tag>
              </div>
              <div class="task-countdown">
                <el-icon><AlarmClock /></el-icon>
                <span class="countdown-text">{{ getCountdown(task.deadline) }}</span>
              </div>
              <div class="task-deadline">截止：{{ formatTime(task.deadline) }}</div>
            </div>
            <el-empty v-if="dashboard.upcomingTasks.length === 0" description="暂无即将截止任务" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" class="board-card">
          <template #header>
            <div class="board-header pending-header">
              <el-icon><List /></el-icon>
              <span>待处理</span>
              <el-tag type="primary" size="small">{{ dashboard.pendingTasks.length }}</el-tag>
            </div>
          </template>
          <div class="task-list">
            <div
              v-for="task in dashboard.pendingTasks"
              :key="task.taskId"
              class="task-card pending-task"
              @click="goToTask(task)"
            >
              <div class="task-top">
                <span class="task-title">{{ task.title }}</span>
                <el-tag :type="task.taskType === 'EXAM' ? 'danger' : 'warning'" size="small">
                  {{ task.taskType === 'EXAM' ? '考试' : '作业' }}
                </el-tag>
              </div>
              <div class="task-status">
                <el-tag :type="task.status === 'SUBMITTED' ? 'info' : ''" size="small">
                  {{ task.status === 'SUBMITTED' ? '已提交待批改' : '待提交' }}
                </el-tag>
              </div>
              <div class="task-deadline">截止：{{ formatTime(task.deadline) }}</div>
            </div>
            <el-empty v-if="dashboard.pendingTasks.length === 0" description="暂无待处理任务" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" class="board-card">
          <template #header>
            <div class="board-header completed-header">
              <el-icon><CircleCheck /></el-icon>
              <span>已完成</span>
              <el-tag type="success" size="small">{{ dashboard.completedTasks.length }}</el-tag>
            </div>
          </template>
          <div class="task-list">
            <div
              v-for="task in dashboard.completedTasks"
              :key="task.taskId"
              class="task-card completed-task"
              @click="goToTask(task)"
            >
              <div class="task-top">
                <span class="task-title">{{ task.title }}</span>
                <el-tag :type="task.taskType === 'EXAM' ? 'danger' : 'warning'" size="small">
                  {{ task.taskType === 'EXAM' ? '考试' : '作业' }}
                </el-tag>
              </div>
              <div class="task-score" v-if="task.scoreGained !== null">
                <span class="score-label">得分</span>
                <span class="score-value">{{ task.scoreGained }}</span>
                <span class="score-unit">分</span>
              </div>
              <div class="task-deadline">截止：{{ formatTime(task.deadline) }}</div>
            </div>
            <el-empty v-if="dashboard.completedTasks.length === 0" description="暂无已完成任务" :image-size="60" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Timer, List, CircleCheck, AlarmClock } from '@element-plus/icons-vue'
import { getDashboard } from '@/api/student'
import type { DashboardVO, TaskVO } from '@/api/student'

const router = useRouter()
const loading = ref(false)
const dashboard = reactive<DashboardVO>({
  upcomingTasks: [],
  pendingTasks: [],
  completedTasks: []
})

const tick = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

async function fetchDashboard() {
  loading.value = true
  try {
    const res = await getDashboard()
    dashboard.upcomingTasks = res.data.upcomingTasks || []
    dashboard.pendingTasks = res.data.pendingTasks || []
    dashboard.completedTasks = res.data.completedTasks || []
  } catch {
    ElMessage.error('加载任务看板失败')
  } finally {
    loading.value = false
  }
}

function formatTime(deadline: string) {
  if (!deadline) return '-'
  const d = new Date(deadline)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function getCountdown(deadline: string) {
  void tick.value
  const diff = new Date(deadline).getTime() - Date.now()
  if (diff <= 0) return '已截止'
  const totalSec = Math.floor(diff / 1000)
  const h = Math.floor(totalSec / 3600)
  const m = Math.floor((totalSec % 3600) / 60)
  const s = totalSec % 60
  if (h >= 24) {
    const days = Math.floor(h / 24)
    return `${days}天${h % 24}小时后截止`
  }
  return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')} 后截止`
}

function goToTask(task: TaskVO) {
  if (task.taskType === 'EXAM') {
    router.push('/student/exams')
  } else {
    router.push('/student/homework')
  }
}

onMounted(() => {
  fetchDashboard()
  countdownTimer = setInterval(() => { tick.value++ }, 1000)
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 24px;
  animation: fadeInUp 0.4s ease;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

.summary-row { margin-bottom: 0; }

/* ── Summary Cards ── */
.summary-card {
  overflow: hidden;
  position: relative;
  border: 1px solid rgba(255,255,255,0.06) !important;
  transition: all 0.2s ease !important;
}

.summary-card :deep(.el-card__body) {
  padding: 22px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.summary-num {
  font-size: 44px;
  font-weight: 700;
  line-height: 1;
  font-family: 'JetBrains Mono', monospace;
}

.summary-label {
  font-size: 12px;
  margin-top: 6px;
  opacity: 0.65;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.summary-icon {
  font-size: 40px;
  opacity: 0.12;
}

/* Color variants */
.upcoming-card {
  background: rgba(239, 68, 68, 0.06) !important;
  border-color: rgba(239, 68, 68, 0.15) !important;
}
.upcoming-card .summary-num { color: #EF4444; }
.upcoming-card .summary-icon { color: #EF4444; opacity: 0.2; }

.pending-card {
  background: rgba(64, 128, 255, 0.06) !important;
  border-color: rgba(64, 128, 255, 0.15) !important;
}
.pending-card .summary-num { color: #6499FF; }
.pending-card .summary-icon { color: #4080FF; opacity: 0.2; }

.completed-card {
  background: rgba(34, 197, 94, 0.06) !important;
  border-color: rgba(34, 197, 94, 0.15) !important;
}
.completed-card .summary-num { color: #22C55E; }
.completed-card .summary-icon { color: #22C55E; opacity: 0.2; }

/* ── Board Cards ── */
.board-card {
  height: 100%;
}
.board-card :deep(.el-card__header) { padding: 14px 18px; }

.board-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.3px;
}

.upcoming-header  { color: #F87171; }
.pending-header   { color: #6499FF; }
.completed-header { color: #4ADE80; }

/* ── Task List ── */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 120px;
}

.task-card {
  padding: 12px 14px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.18s ease;
  border-left: 3px solid transparent;
}

.task-card:hover {
  transform: translateX(3px);
}

.upcoming-task {
  background: rgba(239, 68, 68, 0.06);
  border-left-color: #EF4444;
}
.upcoming-task:hover {
  background: rgba(239, 68, 68, 0.1);
  box-shadow: 0 2px 12px rgba(239, 68, 68, 0.1);
}

.pending-task {
  background: rgba(64, 128, 255, 0.06);
  border-left-color: #4080FF;
}
.pending-task:hover {
  background: rgba(64, 128, 255, 0.1);
  box-shadow: 0 2px 12px rgba(64, 128, 255, 0.1);
}

.completed-task {
  background: rgba(34, 197, 94, 0.05);
  border-left-color: #22C55E;
}
.completed-task:hover {
  background: rgba(34, 197, 94, 0.1);
  box-shadow: 0 2px 12px rgba(34, 197, 94, 0.08);
}

.task-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.task-title {
  font-weight: 600;
  font-size: 13px;
  color: #C8D5E8;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-countdown {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 5px;
  color: #F87171;
}

.countdown-text {
  font-size: 12px;
  font-weight: 600;
  color: #F87171;
  font-family: 'JetBrains Mono', monospace;
}

.task-status { margin-bottom: 4px; }

.task-score {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 4px;
}

.score-label { font-size: 11px; color: rgba(148, 163, 184, 0.5); }
.score-value {
  font-size: 22px;
  font-weight: 700;
  color: #4ADE80;
  font-family: 'JetBrains Mono', monospace;
}
.score-unit { font-size: 11px; color: rgba(148, 163, 184, 0.5); }

.task-deadline { font-size: 11px; color: rgba(148, 163, 184, 0.4); margin-top: 2px; }
</style>
