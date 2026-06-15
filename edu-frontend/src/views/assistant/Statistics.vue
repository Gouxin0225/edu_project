<template>
  <div class="assistant-stats" v-loading="loading">
    <header class="stats-header">
      <div>
        <h2>数据统计</h2>
        <p>{{ statistics.selectedClassName || '全部班级' }} · 班级作业、考试与风险学生跟进视图。</p>
      </div>
      <div class="filter-bar">
        <el-select v-model="selectedClassId" class="class-select" placeholder="选择班级" @change="fetchStatistics">
          <el-option label="全部班级" :value="0" />
          <el-option
            v-for="item in statistics.classes"
            :key="item.classId"
            :label="`${item.className}（${item.studentCount}人）`"
            :value="item.classId"
          />
        </el-select>
        <el-button :icon="Refresh" @click="fetchStatistics">刷新</el-button>
      </div>
    </header>

    <section class="kpi-grid">
      <div v-for="item in kpiCards" :key="item.label" class="metric-card">
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
          <h3>近期任务进度</h3>
          <div class="panel-actions">
            <el-button text type="primary" @click="router.push('/assistant/exams')">考试跟踪</el-button>
            <el-button text type="primary" @click="router.push('/assistant/homework')">作业跟踪</el-button>
          </div>
        </div>
        <div class="task-list">
          <div v-for="task in statistics.recentTasks" :key="task.taskId" class="task-row">
            <div class="task-main">
              <div class="task-title">
                <el-tag size="small" :type="task.type === 'EXAM' ? 'danger' : 'warning'">
                  {{ task.type === 'EXAM' ? '考试' : '作业' }}
                </el-tag>
                <span>{{ task.title }}</span>
              </div>
              <div class="task-meta">
                <span>截止 {{ formatDateTime(task.deadline) }}</span>
                <span>{{ task.targetClassNames.join(' / ') || '全部班级' }}</span>
              </div>
            </div>
            <div class="task-progress">
              <div class="progress-top">
                <span>提交 {{ task.submittedCount }}/{{ task.totalStudents }}</span>
                <strong>{{ formatPercent(task.completionRate) }}</strong>
              </div>
              <el-progress :percentage="normalizePercent(task.completionRate)" :show-text="false" />
              <div class="progress-foot">
                <span>未交 {{ task.pendingCount }}</span>
                <span>待批 {{ task.pendingGradeCount }}</span>
                <span>均分 {{ formatNumber(task.averageScore) }}</span>
              </div>
              <el-button size="small" text type="primary" @click="goTaskDetail(task)">
                查看明细
              </el-button>
            </div>
          </div>
          <el-empty v-if="statistics.recentTasks.length === 0" description="暂无任务数据" :image-size="80" />
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h3>风险学生</h3>
          <el-button text type="primary" @click="router.push('/assistant/students')">查看全部</el-button>
        </div>
        <el-table :data="statistics.riskStudents" size="small" max-height="440">
          <el-table-column prop="studentName" label="学生" min-width="90" />
          <el-table-column prop="className" label="班级" min-width="120" show-overflow-tooltip />
          <el-table-column prop="missingCount" label="缺交" width="70" />
          <el-table-column prop="lowScoreCount" label="低分" width="70" />
          <el-table-column prop="switchScreenCount" label="切屏" width="70" />
          <el-table-column label="均分" width="80">
            <template #default="{ row }">{{ formatNumber(row.averageScore) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default>
              <el-button text type="primary" size="small" @click="router.push('/assistant/students')">跟进</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <section class="secondary-grid">
      <div class="panel">
        <div class="panel-header">
          <h3>班级对比</h3>
          <span>{{ selectedClassId === 0 ? '全部负责班级' : '当前班级' }}</span>
        </div>
        <el-table :data="statistics.classComparisons" size="small">
          <el-table-column prop="className" label="班级" min-width="150" show-overflow-tooltip />
          <el-table-column prop="studentCount" label="人数" width="70" />
          <el-table-column prop="taskCount" label="任务" width="70" />
          <el-table-column label="提交率" width="100">
            <template #default="{ row }">{{ formatPercent(row.submissionRate) }}</template>
          </el-table-column>
          <el-table-column label="均分" width="90">
            <template #default="{ row }">{{ formatNumber(row.averageScore) }}</template>
          </el-table-column>
          <el-table-column prop="riskCount" label="风险" width="70" />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="selectClass(row.classId)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h3>成绩分布</h3>
          <el-button text type="primary" @click="router.push('/assistant/exams')">成绩明细</el-button>
        </div>
        <div class="distribution-list">
          <div v-for="item in statistics.scoreDistribution" :key="item.range" class="distribution-row">
            <span>{{ item.range }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: normalizePercent(item.rate) + '%' }"></div>
            </div>
            <em>{{ item.count }}</em>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h3>AI 与问卷</h3>
          <el-button text type="primary" @click="router.push('/assistant/surveys')">问卷明细</el-button>
        </div>
        <div class="mini-grid">
          <div>
            <strong>{{ statistics.aiUsageSummary.studentQuestionCount }}</strong>
            <span>学生提问</span>
          </div>
          <div>
            <strong>{{ statistics.aiUsageSummary.activeStudentCount }}</strong>
            <span>活跃学生</span>
          </div>
          <div>
            <strong>{{ statistics.surveySummary.surveyCount }}</strong>
            <span>问卷</span>
          </div>
          <div>
            <strong>{{ statistics.surveySummary.totalResponses }}</strong>
            <span>反馈</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Collection, DataLine, Document, Finished, Refresh, School, TrendCharts, User } from '@element-plus/icons-vue'
import { getTeacherStatistics, type TeacherStatistics } from '@/api/teacher'

const router = useRouter()
const loading = ref(false)
const selectedClassId = ref(0)

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

const kpiCards = computed(() => [
  { label: '负责班级', value: statistics.overview.classCount, icon: School },
  { label: '学生人数', value: statistics.overview.studentCount, icon: User },
  { label: '考试数量', value: statistics.overview.examCount, icon: Collection },
  { label: '作业数量', value: statistics.overview.homeworkCount, icon: Document },
  { label: '待批改', value: statistics.overview.pendingGradeCount, icon: Finished },
  { label: '平均分', value: formatNumber(statistics.overview.averageScore), icon: TrendCharts },
  { label: '及格率', value: formatPercent(statistics.overview.passRate), icon: DataLine }
])

async function fetchStatistics() {
  loading.value = true
  try {
    const res = await getTeacherStatistics(selectedClassId.value || undefined)
    Object.assign(statistics, res.data)
  } catch (error: any) {
    ElMessage.error(error?.message || '加载统计数据失败')
  } finally {
    loading.value = false
  }
}

function goTaskDetail(task: { type: string }) {
  router.push(task.type === 'EXAM' ? '/assistant/exams' : '/assistant/homework')
}

function selectClass(classId: number) {
  selectedClassId.value = classId
  fetchStatistics()
}

function normalizePercent(value?: number) {
  return Math.max(0, Math.min(100, Number(value || 0)))
}

function formatPercent(value?: number) {
  return `${formatNumber(value)}%`
}

function formatNumber(value?: number) {
  return Number(value || 0).toFixed(1)
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(fetchStatistics)
</script>

<style scoped>
.assistant-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
}

.stats-header,
.filter-bar,
.panel-header,
.task-title,
.task-meta,
.progress-top,
.progress-foot,
.distribution-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stats-header {
  justify-content: space-between;
}

.stats-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 24px;
  letter-spacing: 0;
}

.stats-header p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.class-select {
  width: 260px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(120px, 1fr));
  gap: 12px;
}

.metric-card,
.panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-card);
  box-shadow: var(--card-shadow);
  backdrop-filter: blur(14px);
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 92px;
  padding: 14px;
}

.metric-card .el-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--primary-dim);
  color: var(--primary-light);
}

.metric-card strong,
.mini-grid strong {
  display: block;
  color: var(--text-primary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 24px;
}

.metric-card span,
.panel-header span,
.mini-grid span {
  color: var(--text-secondary);
  font-size: 12px;
}

.main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(420px, 0.9fr);
  gap: 16px;
}

.secondary-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.7fr) minmax(300px, 0.6fr);
  gap: 16px;
}

.panel {
  min-width: 0;
  padding: 16px;
}

.panel-header {
  justify-content: space-between;
  margin-bottom: 14px;
}

.panel-header h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
}

.task-list {
  display: grid;
  gap: 10px;
}

.task-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 16px;
  align-items: center;
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  background: var(--surface-muted);
  padding: 12px;
}

.task-main {
  min-width: 0;
}

.task-title span:last-child {
  overflow: hidden;
  color: var(--text-primary);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta,
.progress-foot,
.progress-top {
  justify-content: space-between;
  color: var(--text-secondary);
  font-size: 12px;
}

.task-meta {
  justify-content: flex-start;
  margin-top: 8px;
}

.progress-top {
  margin-bottom: 7px;
}

.progress-top strong {
  color: var(--primary-light);
}

.progress-foot {
  margin-top: 7px;
}

.distribution-list {
  display: grid;
  gap: 13px;
}

.distribution-row {
  grid-template-columns: 54px minmax(0, 1fr) 34px;
  color: var(--text-secondary);
  font-size: 12px;
}

.bar-track {
  height: 9px;
  border-radius: 99px;
  background: var(--surface-muted);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #4080ff, #22c55e);
}

.distribution-row em {
  color: var(--primary-light);
  font-family: 'JetBrains Mono', monospace;
  font-style: normal;
  text-align: right;
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.mini-grid div {
  border-radius: 8px;
  background: var(--surface-muted);
  padding: 12px;
}

@media (max-width: 1280px) {
  .kpi-grid {
    grid-template-columns: repeat(4, minmax(140px, 1fr));
  }

  .main-grid,
  .secondary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .stats-header,
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .class-select {
    width: 100%;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-row {
    grid-template-columns: 1fr;
  }
}
</style>
