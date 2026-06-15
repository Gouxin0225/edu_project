<template>
  <div class="assistant-dashboard" v-loading="loading">
    <header class="page-header">
      <div>
        <h2>班主任工作台</h2>
        <p>集中查看今日待办、风险学员和需要继续跟进的回访事项。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        <el-button type="primary" :icon="User" @click="router.push('/assistant/students')">学员跟进</el-button>
      </div>
    </header>

    <section class="kpi-grid">
      <div v-for="item in kpiCards" :key="item.label" class="kpi-card">
        <el-icon><component :is="item.icon" /></el-icon>
        <div>
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <section class="main-grid">
      <div class="panel">
        <div class="panel-header">
          <h3>今日待办</h3>
          <span>{{ todoItems.length }} 项</span>
        </div>
        <div class="todo-list">
          <button v-for="item in todoItems" :key="item.key" class="todo-row" @click="router.push(item.path)">
            <el-icon><component :is="item.icon" /></el-icon>
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </div>
            <em>{{ item.count }}</em>
          </button>
          <el-empty v-if="todoItems.length === 0" description="暂无待办" :image-size="80" />
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h3>高风险学员</h3>
          <el-button text type="primary" @click="router.push('/assistant/students')">查看全部</el-button>
        </div>
        <el-table :data="topRiskStudents" size="small" max-height="360">
          <el-table-column prop="studentName" label="学员" min-width="90" />
          <el-table-column prop="className" label="班级" min-width="120" show-overflow-tooltip />
          <el-table-column prop="missingCount" label="缺交" width="64" />
          <el-table-column prop="lowScoreCount" label="低分" width="64" />
          <el-table-column prop="riskScore" label="风险" width="70" />
        </el-table>
      </div>
    </section>

    <section class="secondary-grid">
      <div class="panel">
        <div class="panel-header">
          <h3>近期任务预警</h3>
          <span>按未交和待批排序</span>
        </div>
        <div class="task-list">
          <button v-for="task in warningTasks" :key="task.taskId" class="task-row task-button" @click="goTaskDetail(task)">
            <div>
              <strong>{{ task.title }}</strong>
              <span>{{ task.type === 'EXAM' ? '考试' : '作业' }} · 截止 {{ formatTime(task.deadline) }}</span>
            </div>
            <div class="task-metrics">
              <el-tag type="warning" effect="plain">未交 {{ task.pendingCount }}</el-tag>
              <el-tag v-if="task.pendingGradeCount" type="danger" effect="plain">待批 {{ task.pendingGradeCount }}</el-tag>
            </div>
          </button>
          <el-empty v-if="warningTasks.length === 0" description="暂无任务预警" :image-size="80" />
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h3>待继续跟进</h3>
          <el-button text type="primary" @click="router.push('/assistant/student-visits')">回访记录</el-button>
        </div>
        <div class="visit-list">
          <div v-for="item in needFollowVisits" :key="item.id" class="visit-row">
            <div>
              <strong>{{ item.studentName }}</strong>
              <span>{{ item.className }} · {{ formatTime(item.visitTime) }}</span>
            </div>
            <el-tag v-if="item.nextFollowTime" :type="followTagType(item)" effect="plain" size="small">
              {{ followLabel(item) }}
            </el-tag>
            <p>{{ item.content }}</p>
          </div>
          <el-empty v-if="needFollowVisits.length === 0" description="暂无待跟进回访" :image-size="80" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ChatDotRound, Collection, Finished, Notebook, Refresh, User, Warning } from '@element-plus/icons-vue'
import { getTeacherStatistics, type TeacherStatistics } from '@/api/teacher'
import { getStudentVisitRecords, type StudentVisitRecord } from '@/api/studentVisit'

const router = useRouter()
const loading = ref(false)
const visits = ref<StudentVisitRecord[]>([])
const statistics = reactive<TeacherStatistics>({
  selectedClassId: null,
  selectedClassName: '全部班级',
  overview: {
    classCount: 0,
    studentCount: 0,
    examCount: 0,
    homeworkCount: 0,
    activeTaskCount: 0,
    pendingGradeCount: 0,
    averageScore: 0,
    passRate: 0
  },
  classes: [],
  recentTasks: [],
  classComparisons: [],
  riskStudents: [],
  scoreDistribution: [],
  scoreTrends: [],
  weakKnowledgePoints: [],
  aiUsageSummary: {
    studentQuestionCount: 0,
    teacherQuestionCount: 0,
    activeStudentCount: 0,
    knowledgeDocumentCount: 0,
    knowledgeChunkCount: 0,
    reviewNoteCount: 0,
    topTopics: []
  },
  surveySummary: {
    surveyCount: 0,
    publishedCount: 0,
    totalResponses: 0,
    recentSurveys: []
  }
})

const needFollowVisits = computed(() =>
  visits.value
    .filter(item => item.visitResult === 'NEED_FOLLOW' || item.visitResult === 'UNREACHED')
    .sort((a, b) => followPriority(b) - followPriority(a))
    .slice(0, 6)
)

const dueFollowVisits = computed(() =>
  visits.value.filter(item =>
    (item.visitResult === 'NEED_FOLLOW' || item.visitResult === 'UNREACHED') &&
    (isOverdue(item) || isToday(item.nextFollowTime))
  )
)

const warningTasks = computed(() =>
  [...statistics.recentTasks]
    .filter(item => item.pendingCount > 0 || item.pendingGradeCount > 0)
    .sort((a, b) => (b.pendingCount + b.pendingGradeCount * 2) - (a.pendingCount + a.pendingGradeCount * 2))
    .slice(0, 6)
)

const topRiskStudents = computed(() => [...statistics.riskStudents].sort((a, b) => b.riskScore - a.riskScore).slice(0, 8))

const kpiCards = computed(() => [
  { label: '负责班级', value: statistics.overview.classCount, icon: Collection },
  { label: '学员人数', value: statistics.overview.studentCount, icon: User },
  { label: '风险学员', value: statistics.riskStudents.length, icon: Warning },
  { label: '待批改', value: statistics.overview.pendingGradeCount, icon: Finished },
  { label: '任务预警', value: warningTasks.value.length, icon: Notebook },
  { label: '跟进到期', value: dueFollowVisits.value.length, icon: ChatDotRound }
])

const todoItems = computed(() => {
  const items: Array<{ key: string; title: string; desc: string; count: number; path: string; icon: any }> = []
  if (statistics.riskStudents.length) {
    items.push({
      key: 'risk',
      title: '处理风险学员',
      desc: '存在缺交、低分或异常行为的学员',
      count: statistics.riskStudents.length,
      path: '/assistant/students',
      icon: Warning
    })
  }
  if (warningTasks.value.length) {
    const hasExam = warningTasks.value.some(item => item.type === 'EXAM')
    const hasHomework = warningTasks.value.some(item => item.type === 'HOMEWORK')
    items.push({
      key: 'tasks',
      title: '跟进任务预警',
      desc: '近期作业/考试存在未交或待批',
      count: warningTasks.value.length,
      path: hasExam && !hasHomework ? '/assistant/exams' : hasHomework && !hasExam ? '/assistant/homework' : '/assistant/statistics',
      icon: Notebook
    })
  }
  if (needFollowVisits.value.length) {
    items.push({
      key: 'visits',
      title: '继续回访跟进',
      desc: '上次回访未联系上、仍需跟进或已到期',
      count: Math.max(needFollowVisits.value.length, dueFollowVisits.value.length),
      path: '/assistant/students',
      icon: ChatDotRound
    })
  }
  return items
})

async function loadData() {
  loading.value = true
  try {
    const [statRes, visitRes] = await Promise.all([
      getTeacherStatistics(),
      getStudentVisitRecords()
    ])
    Object.assign(statistics, statRes.data)
    visits.value = visitRes.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载工作台失败')
  } finally {
    loading.value = false
  }
}

function goTaskDetail(task: { type: string }) {
  router.push(task.type === 'EXAM' ? '/assistant/exams' : '/assistant/homework')
}

function formatTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function followPriority(item: StudentVisitRecord) {
  if (isOverdue(item)) return 3
  if (isToday(item.nextFollowTime)) return 2
  if (item.visitResult === 'NEED_FOLLOW') return 1
  return 0
}

function followLabel(item: StudentVisitRecord) {
  if (isOverdue(item)) return `已逾期 ${formatTime(item.nextFollowTime)}`
  if (isToday(item.nextFollowTime)) return `今日 ${formatTime(item.nextFollowTime).slice(11)}`
  return `下次跟进 ${formatTime(item.nextFollowTime)}`
}

function followTagType(item: StudentVisitRecord) {
  if (isOverdue(item)) return 'danger'
  if (isToday(item.nextFollowTime)) return 'warning'
  return 'info'
}

function isOverdue(item: StudentVisitRecord) {
  if (!item.nextFollowTime || item.visitResult === 'RESOLVED') return false
  return new Date(item.nextFollowTime).getTime() < Date.now() && !isToday(item.nextFollowTime)
}

function isToday(value?: string | null) {
  if (!value) return false
  const date = new Date(value)
  const now = new Date()
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth() && date.getDate() === now.getDate()
}

onMounted(loadData)
</script>

<style scoped>
.assistant-dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
}

.page-header,
.header-actions,
.kpi-card,
.panel-header,
.todo-row,
.task-row,
.task-metrics,
.visit-row > div {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header,
.panel-header,
.task-row,
.visit-row > div {
  justify-content: space-between;
}

.page-header {
  align-items: flex-start;
}

.page-header h2,
.panel-header h3 {
  margin: 0;
  color: var(--text-primary);
  letter-spacing: 0;
}

.page-header p {
  margin: 6px 0 0;
  color: rgba(226, 232, 240, 0.6);
  font-size: 13px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 12px;
}

.main-grid,
.secondary-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(360px, 0.8fr);
  gap: 16px;
}

.kpi-card,
.panel {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(12, 20, 40, 0.74);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.28);
}

.kpi-card {
  min-height: 84px;
  padding: 14px;
}

.kpi-card .el-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: rgba(64, 128, 255, 0.12);
  color: #8bb5ff;
}

.kpi-card strong {
  display: block;
  color: var(--text-primary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 24px;
}

.kpi-card span,
.panel-header span,
.todo-row span,
.task-row span,
.visit-row span {
  color: rgba(226, 232, 240, 0.54);
  font-size: 12px;
}

.panel {
  min-width: 0;
  padding: 16px;
}

.todo-list,
.task-list,
.visit-list {
  display: grid;
  gap: 10px;
}

.todo-row,
.task-row,
.visit-row {
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  background: var(--surface-muted);
  color: inherit;
  padding: 12px;
  text-align: left;
}

.visit-row {
  display: grid;
  gap: 8px;
}

.todo-row {
  cursor: pointer;
}

.task-button {
  cursor: pointer;
  font: inherit;
}

.todo-row:hover,
.task-button:hover {
  border-color: rgba(64, 128, 255, 0.35);
  background: rgba(64, 128, 255, 0.08);
}

.todo-row .el-icon {
  color: #93c5fd;
}

.todo-row strong,
.task-row strong,
.visit-row strong {
  display: block;
  color: var(--text-primary);
}

.todo-row em {
  margin-left: auto;
  color: #fbbf24;
  font-family: 'JetBrains Mono', monospace;
  font-style: normal;
  font-weight: 700;
}

.visit-row p {
  margin: 0;
  color: rgba(226, 232, 240, 0.74);
  font-size: 13px;
  line-height: 1.6;
}

.assistant-dashboard :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(15, 23, 42, 0.92);
  --el-table-header-text-color: #cbd5e1;
  --el-table-text-color: #e5e7eb;
  --el-table-border-color: rgba(148, 163, 184, 0.14);
  --el-table-row-hover-bg-color: rgba(64, 128, 255, 0.08);
}

.assistant-dashboard :deep(.el-table th.el-table__cell),
.assistant-dashboard :deep(.el-table td.el-table__cell) {
  background: transparent;
}

@media (max-width: 1280px) {
  .kpi-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .main-grid,
  .secondary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
