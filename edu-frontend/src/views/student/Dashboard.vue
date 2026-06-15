<template>
  <div class="dashboard">
    <section class="dashboard-hero">
      <div class="hero-copy">
        <div class="hero-kicker">SMART LEARNING</div>
        <h1>任务看板</h1>
        <p>集中查看待处理任务、临近截止事项和已完成记录。</p>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <span>{{ totalTaskCount }}</span>
          <label>全部任务</label>
        </div>
        <div class="hero-stat">
          <span>{{ completionRate }}%</span>
          <label>完成率</label>
        </div>
        <div class="hero-stat">
          <span>{{ nextDeadlineText }}</span>
          <label>最近截止</label>
        </div>
      </div>
      <div class="hero-visual">
        <el-icon><Reading /></el-icon>
      </div>
    </section>

    <section class="summary-grid" v-loading="loading">
      <div class="summary-card upcoming-card">
        <div class="summary-info">
          <div class="summary-label">
            <el-icon><Timer /></el-icon>
            <span>即将截止</span>
          </div>
          <div class="summary-num">{{ dashboard.upcomingTasks.length }}</div>
          <p>{{ dashboard.upcomingTasks.length ? `最近 ${nextDeadlineText}` : '暂无紧急事项' }}</p>
        </div>
        <div class="summary-art">
          <el-icon><AlarmClock /></el-icon>
        </div>
      </div>

      <div class="summary-card pending-card">
        <div class="summary-info">
          <div class="summary-label">
            <el-icon><List /></el-icon>
            <span>待提交</span>
          </div>
          <div class="summary-num">{{ todoTasks.length }}</div>
          <p>待完成任务需及时处理</p>
        </div>
        <div class="summary-art">
          <el-icon><FolderOpened /></el-icon>
        </div>
      </div>

      <div class="summary-card submitted-card">
        <div class="summary-info">
          <div class="summary-label">
            <el-icon><Checked /></el-icon>
            <span>待批改</span>
          </div>
          <div class="summary-num">{{ submittedTasks.length }}</div>
          <p>已提交，等待教师批改</p>
        </div>
        <div class="summary-art">
          <el-icon><Reading /></el-icon>
        </div>
      </div>

      <div class="summary-card completed-card">
        <div class="summary-info">
          <div class="summary-label">
            <el-icon><CircleCheck /></el-icon>
            <span>已完成</span>
          </div>
          <div class="summary-num">{{ dashboard.completedTasks.length }}</div>
          <p>继续保持当前节奏</p>
        </div>
        <div class="summary-art">
          <el-icon><Checked /></el-icon>
        </div>
      </div>
    </section>

    <section class="dashboard-grid" v-loading="loading">
      <div class="main-stack">
        <section class="panel task-panel">
          <div class="panel-header">
            <div>
              <h2>最近作业</h2>
              <p>优先处理临近截止和未提交任务</p>
            </div>
          </div>
          <div class="task-table">
            <div
              v-for="task in primaryTasks"
              :key="`${task.taskType}-${task.taskId}`"
              class="task-row"
              :class="getTaskTone(task)"
              @click="goToTask(task)"
            >
              <div class="task-subject">
                <span class="task-type" :class="task.taskType.toLowerCase()">
                  {{ task.taskType === 'EXAM' ? '考试' : '作业' }}
                </span>
                <span class="task-title">{{ task.title }}</span>
              </div>
              <div class="task-meta">
                <span>{{ getTaskStateText(task) }}</span>
                <span>{{ formatTime(task.deadline) }}</span>
              </div>
              <div class="task-action">
                <el-tag :type="task.taskType === 'EXAM' ? 'danger' : 'warning'" size="small">
                  {{ getTaskActionText(task) }}
                </el-tag>
              </div>
            </div>
            <el-empty v-if="primaryTasks.length === 0" description="暂无待处理任务" :image-size="76" />
          </div>
        </section>

        <section class="panel split-panel">
          <div class="panel-header compact">
            <div>
              <h2>即将截止</h2>
              <p>请优先完成这些任务</p>
            </div>
          </div>
          <div class="compact-list">
            <div
              v-for="task in dashboard.upcomingTasks"
              :key="`upcoming-${task.taskId}`"
              class="deadline-item"
              @click="goToTask(task)"
            >
              <div class="deadline-icon">
                <el-icon><Timer /></el-icon>
              </div>
              <div class="deadline-main">
                <strong>{{ task.title }}</strong>
                <span>{{ formatTime(task.deadline) }}</span>
              </div>
              <em>{{ getCountdown(task.deadline) }}</em>
            </div>
            <el-empty v-if="dashboard.upcomingTasks.length === 0" description="暂无即将截止任务" :image-size="70" />
          </div>
        </section>
      </div>

      <aside class="side-stack">
        <section class="panel progress-panel">
          <div class="panel-header compact">
            <div>
              <h2>学习统计</h2>
              <p>基于当前任务状态</p>
            </div>
          </div>
          <div class="progress-card">
            <div class="progress-ring" :style="{ '--progress': `${completionRate * 3.6}deg` }">
              <span>{{ completionRate }}%</span>
            </div>
            <div class="progress-copy">
              <strong>完成率</strong>
              <span>已完成 {{ dashboard.completedTasks.length }} / {{ totalTaskCount || 0 }} 项</span>
            </div>
          </div>
          <div class="stat-grid">
            <div>
              <span>{{ todoTasks.length }}</span>
              <label>待提交</label>
            </div>
            <div>
              <span>{{ dashboard.upcomingTasks.length }}</span>
              <label>临近截止</label>
            </div>
            <div>
              <span>{{ submittedTasks.length }}</span>
              <label>待批改</label>
            </div>
          </div>
        </section>

        <section class="panel submitted-panel">
          <div class="panel-header compact">
            <div>
              <h2>待批改</h2>
              <p>已提交，等待教师评分</p>
            </div>
          </div>
          <div class="completed-list">
            <div
              v-for="task in submittedTasks"
              :key="`submitted-${task.taskType}-${task.taskId}`"
              class="completed-item"
              @click="goToTask(task)"
            >
              <div class="submitted-icon">
                <el-icon><Checked /></el-icon>
              </div>
              <div class="completed-main">
                <strong>{{ task.title }}</strong>
                <span>已提交待批改</span>
              </div>
            </div>
            <el-empty v-if="submittedTasks.length === 0" description="暂无待批改任务" :image-size="70" />
          </div>
        </section>

        <section class="panel completed-panel">
          <div class="panel-header compact">
            <div>
              <h2>完成记录</h2>
              <p>最近完成任务</p>
            </div>
          </div>
          <div class="completed-list">
            <div
              v-for="task in dashboard.completedTasks"
              :key="`completed-${task.taskId}`"
              class="completed-item"
              @click="goToTask(task)"
            >
              <div class="completed-icon">
                <el-icon><CircleCheck /></el-icon>
              </div>
              <div class="completed-main">
                <strong>{{ task.title }}</strong>
                <span>{{ task.scoreGained !== null ? `得分 ${task.scoreGained} 分` : '已完成' }}</span>
              </div>
            </div>
            <el-empty v-if="dashboard.completedTasks.length === 0" description="暂无已完成任务" :image-size="70" />
          </div>
        </section>

        <section class="study-tip">
          <div class="tip-bot">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div>
            <strong>Hi，同学</strong>
            <span>有学习问题可以随时去 AI 问答。</span>
          </div>
        </section>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Timer, List, CircleCheck, AlarmClock, Reading, FolderOpened, Checked, ChatDotRound } from '@element-plus/icons-vue'
import { getDashboard } from '@/api/student'
import type { DashboardVO, TaskVO } from '@/api/student'

const router = useRouter()
const loading = ref(false)
const dashboard = reactive<DashboardVO>({
  upcomingTasks: [],
  todoTasks: [],
  pendingTasks: [],
  submittedTasks: [],
  completedTasks: []
})

const tick = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const todoTasks = computed(() => dashboard.todoTasks ?? dashboard.pendingTasks)
const submittedTasks = computed(() => dashboard.submittedTasks ?? [])
const totalTaskCount = computed(() =>
  dashboard.upcomingTasks.length + todoTasks.value.length + submittedTasks.value.length + dashboard.completedTasks.length
)
const completionRate = computed(() => {
  if (!totalTaskCount.value) return 0
  return Math.round((dashboard.completedTasks.length / totalTaskCount.value) * 100)
})
const primaryTasks = computed(() => [...dashboard.upcomingTasks, ...todoTasks.value].slice(0, 6))
const nextDeadlineText = computed(() => {
  const candidates = [...dashboard.upcomingTasks, ...todoTasks.value]
    .filter(task => task.deadline)
    .sort((a, b) => new Date(a.deadline).getTime() - new Date(b.deadline).getTime())
  return candidates.length ? getCountdown(candidates[0].deadline) : '无'
})

async function fetchDashboard() {
  loading.value = true
  try {
    const res = await getDashboard()
    const nextTodoTasks = res.data.todoTasks || res.data.pendingTasks || []
    dashboard.upcomingTasks = res.data.upcomingTasks || []
    dashboard.todoTasks = nextTodoTasks
    dashboard.pendingTasks = nextTodoTasks
    dashboard.submittedTasks = res.data.submittedTasks || []
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

function getTaskTone(task: TaskVO) {
  if (dashboard.upcomingTasks.some(item => item.taskId === task.taskId && item.taskType === task.taskType)) return 'is-urgent'
  if (task.status === 'SUBMITTED') return 'is-submitted'
  return 'is-pending'
}

function getTaskStateText(task: TaskVO) {
  if (dashboard.upcomingTasks.some(item => item.taskId === task.taskId && item.taskType === task.taskType)) {
    return getCountdown(task.deadline)
  }
  if (task.status === 'SUBMITTED') return '已提交待批改'
  if (task.status === 'RETURNED') return '退回待重交'
  return '待提交'
}

function getTaskActionText(task: TaskVO) {
  if (task.status === 'SUBMITTED') return '查看'
  if (task.status === 'RETURNED') return '重新提交'
  if (task.taskType === 'EXAM') return '去考试'
  return '去完成'
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
  display: grid;
  gap: 20px;
  padding: 18px 22px 28px;
  animation: fadeInUp 0.4s ease;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

.dashboard-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto 132px;
  align-items: center;
  gap: 22px;
  min-height: 142px;
  padding: 26px 30px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background:
    radial-gradient(circle at 78% 20%, rgba(104, 150, 255, 0.28), transparent 28%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(238, 244, 255, 0.82));
  box-shadow: 0 18px 45px rgba(41, 83, 166, 0.12);
}

.dashboard-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(120deg, transparent 0 54%, rgba(99, 132, 255, 0.1) 54% 72%, transparent 72%),
    radial-gradient(circle at 24px 24px, rgba(64, 128, 255, 0.18) 0 2px, transparent 3px);
  background-size: auto, 18px 18px;
  pointer-events: none;
}

.hero-copy,
.hero-stats,
.hero-visual {
  position: relative;
  z-index: 1;
}

.hero-kicker {
  margin-bottom: 8px;
  color: #4080ff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
}

.hero-copy h1 {
  margin: 0;
  color: #101a3d;
  font-size: 30px;
  line-height: 1.2;
  letter-spacing: 0;
}

.hero-copy p {
  margin: 10px 0 0;
  color: #52607a;
  font-size: 14px;
  line-height: 1.7;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(88px, 1fr));
  gap: 10px;
}

.hero-stat {
  min-width: 96px;
  padding: 12px 14px;
  border: 1px solid rgba(64, 128, 255, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.62);
}

.hero-stat span {
  display: block;
  color: #17234d;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.2;
}

.hero-stat label {
  display: block;
  margin-top: 5px;
  color: #6f7d99;
  font-size: 12px;
}

.hero-visual {
  width: 118px;
  height: 98px;
  border-radius: 8px;
  background:
    radial-gradient(circle at 72% 20%, rgba(255, 202, 87, 0.65), transparent 18%),
    linear-gradient(145deg, #dbe8ff, #ffffff);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 0 1px rgba(64, 128, 255, 0.14), 0 16px 30px rgba(64, 128, 255, 0.16);
}

.hero-visual .el-icon {
  color: #4080ff;
  font-size: 58px;
  filter: drop-shadow(0 12px 18px rgba(64, 128, 255, 0.28));
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  position: relative;
  min-height: 142px;
  padding: 22px 24px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 34px rgba(30, 41, 59, 0.08);
}

.summary-num {
  margin-top: 14px;
  font-size: 42px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0;
}

.summary-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 800;
}

.summary-label .el-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.summary-card p {
  margin: 10px 0 0;
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.summary-info {
  position: relative;
  z-index: 1;
}

.summary-art {
  position: absolute;
  right: 20px;
  bottom: 18px;
  width: 82px;
  height: 68px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.9;
}

.summary-art .el-icon {
  font-size: 46px;
}

.upcoming-card {
  background: linear-gradient(135deg, #fff8f2, #fff);
}

.upcoming-card .summary-num,
.upcoming-card .summary-label {
  color: #f05a28;
}

.upcoming-card .summary-label .el-icon,
.upcoming-card .summary-art {
  background: linear-gradient(135deg, #ff7a3d, #ffd25e);
  color: #fff;
}

.pending-card {
  background: linear-gradient(135deg, #f2f6ff, #fff);
}

.pending-card .summary-num,
.pending-card .summary-label {
  color: #2f66ff;
}

.pending-card .summary-label .el-icon,
.pending-card .summary-art {
  background: linear-gradient(135deg, #4080ff, #8fb2ff);
  color: #fff;
}

.submitted-card {
  background: linear-gradient(135deg, #f7f4ff, #fff);
}

.submitted-card .summary-num,
.submitted-card .summary-label {
  color: #7357ff;
}

.submitted-card .summary-label .el-icon,
.submitted-card .summary-art {
  background: linear-gradient(135deg, #7357ff, #a993ff);
  color: #fff;
}

.completed-card {
  background: linear-gradient(135deg, #f0fff8, #fff);
}

.completed-card .summary-num,
.completed-card .summary-label {
  color: #18b66a;
}

.completed-card .summary-label .el-icon,
.completed-card .summary-art {
  background: linear-gradient(135deg, #18c77a, #8df0bd);
  color: #fff;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 16px;
  align-items: start;
}

.main-stack,
.side-stack {
  display: grid;
  gap: 16px;
}

.panel {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(30, 41, 59, 0.08);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 22px 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.panel-header.compact {
  padding-bottom: 14px;
}

.panel-header h2 {
  margin: 0;
  color: #101a3d;
  font-size: 18px;
  line-height: 1.3;
  letter-spacing: 0;
}

.panel-header p {
  margin: 6px 0 0;
  color: #7a869f;
  font-size: 12px;
}

.task-table {
  display: grid;
  padding: 0 22px 18px;
}

.task-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 210px 84px;
  align-items: center;
  gap: 14px;
  min-height: 58px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  cursor: pointer;
}

.task-row:last-child {
  border-bottom: 0;
}

.task-row:hover .task-title,
.deadline-item:hover strong,
.completed-item:hover strong {
  color: #4080ff;
}

.task-subject {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 12px;
}

.task-type {
  flex: 0 0 auto;
  min-width: 56px;
  padding: 6px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
}

.task-type.exam {
  color: #f05a28;
  background: #fff1e8;
}

.task-type.homework {
  color: #2f66ff;
  background: #eef4ff;
}

.task-title {
  min-width: 0;
  overflow: hidden;
  color: #17234d;
  font-size: 14px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  display: grid;
  gap: 4px;
  color: #7a869f;
  font-size: 12px;
  line-height: 1.35;
}

.task-meta span:first-child {
  font-weight: 700;
}

.task-row.is-urgent .task-meta span:first-child {
  color: #f05a28;
}

.task-row.is-submitted .task-meta span:first-child {
  color: #7a869f;
}

.task-action {
  display: flex;
  justify-content: flex-end;
}

.compact-list,
.completed-list {
  display: grid;
  padding: 12px 20px 18px;
}

.deadline-item,
.completed-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 66px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
  cursor: pointer;
}

.deadline-item:last-child,
.completed-item:last-child {
  border-bottom: 0;
}

.deadline-icon,
.completed-icon,
.submitted-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.deadline-icon {
  color: #f05a28;
  background: #fff1e8;
}

.completed-icon {
  color: #18b66a;
  background: #eafff4;
}

.submitted-icon {
  color: #7357ff;
  background: #f0ecff;
}

.deadline-main,
.completed-main {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.deadline-main strong,
.completed-main strong {
  overflow: hidden;
  color: #17234d;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.deadline-main span,
.completed-main span {
  color: #7a869f;
  font-size: 12px;
}

.deadline-item em {
  color: #f05a28;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.progress-panel {
  padding-bottom: 18px;
}

.progress-card {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 20px;
}

.progress-ring {
  --progress: 0deg;
  width: 108px;
  height: 108px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: conic-gradient(#7357ff var(--progress), #e8ecfb 0);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 24px rgba(115, 87, 255, 0.16);
}

.progress-ring::before {
  content: '';
  position: absolute;
}

.progress-ring span {
  width: 78px;
  height: 78px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #17234d;
  font-size: 22px;
  font-weight: 800;
}

.progress-copy {
  display: grid;
  gap: 6px;
}

.progress-copy strong {
  color: #17234d;
  font-size: 16px;
}

.progress-copy span {
  color: #7a869f;
  font-size: 13px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 0 20px;
}

.stat-grid div {
  padding: 14px 16px;
  border: 1px solid rgba(64, 128, 255, 0.12);
  border-radius: 8px;
  background: #f8fbff;
}

.stat-grid span {
  display: block;
  color: #17234d;
  font-size: 24px;
  font-weight: 800;
}

.stat-grid label {
  display: block;
  margin-top: 4px;
  color: #7a869f;
  font-size: 12px;
}

.submitted-panel,
.completed-panel {
  min-height: 238px;
}

.study-tip {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  min-height: 108px;
  padding: 18px;
  border: 1px solid rgba(64, 128, 255, 0.18);
  border-radius: 8px;
  background: linear-gradient(135deg, #eef5ff, #ffffff);
  box-shadow: 0 14px 34px rgba(30, 41, 59, 0.08);
}

.tip-bot {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: linear-gradient(135deg, #7357ff, #4080ff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12px 24px rgba(64, 128, 255, 0.22);
}

.tip-bot .el-icon {
  font-size: 28px;
}

.study-tip strong,
.study-tip span {
  display: block;
}

.study-tip strong {
  color: #17234d;
  font-size: 16px;
}

.study-tip span {
  margin-top: 6px;
  color: #6f7d99;
  font-size: 13px;
  line-height: 1.6;
}

:deep(.el-empty) {
  padding: 18px 0;
}

@media (max-width: 1180px) {
  .dashboard-hero {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .hero-visual {
    display: none;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 780px) {
  .dashboard {
    padding: 14px;
  }

  .summary-grid,
  .hero-stats {
    grid-template-columns: 1fr;
  }

  .dashboard-hero {
    padding: 22px;
  }

  .task-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .task-action {
    justify-content: flex-start;
  }

  .deadline-item,
  .completed-item {
    grid-template-columns: 38px minmax(0, 1fr);
  }

  .deadline-item em {
    grid-column: 2;
  }
}
</style>
