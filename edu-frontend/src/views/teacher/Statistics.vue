<template>
  <div class="stats-page" v-loading="loading">
    <header class="stats-header">
      <div>
        <h2>数据统计</h2>
        <p>{{ statistics.selectedClassName || '全部班级' }} · 教学进度、成绩质量与风险学生概览</p>
      </div>
      <div class="filter-bar">
        <el-select
          v-model="selectedClassId"
          class="class-select"
          placeholder="选择班级"
          @change="fetchStatistics"
        >
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
        <div class="metric-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-label">{{ item.label }}</div>
        </div>
      </div>
    </section>

    <section class="main-grid">
      <div class="panel task-panel">
        <div class="panel-header">
          <h3>近期任务进度</h3>
          <span>{{ statistics.recentTasks.length }} 项</span>
        </div>
        <div class="task-list">
          <button
            v-for="task in statistics.recentTasks"
            :key="task.taskId"
            class="task-row"
            @click="goToTask(task)"
          >
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
                <span>待批 {{ task.pendingGradeCount }}</span>
                <span>均分 {{ formatNumber(task.averageScore) }}</span>
              </div>
            </div>
          </button>
          <el-empty v-if="statistics.recentTasks.length === 0" description="暂无任务数据" :image-size="80" />
        </div>
      </div>

      <div class="panel distribution-panel">
        <div class="panel-header">
          <h3>成绩分布</h3>
          <span>已评分提交</span>
        </div>
        <div class="distribution-list">
          <div v-for="item in statistics.scoreDistribution" :key="item.range" class="distribution-row">
            <span class="range">{{ item.range }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: normalizePercent(item.rate) + '%' }"></div>
            </div>
            <span class="count">{{ item.count }}</span>
          </div>
        </div>
      </div>
    </section>

    <section class="secondary-grid">
      <div class="panel">
        <div class="panel-header">
          <h3>班级对比</h3>
          <span>{{ selectedClassId === 0 ? '横向比较' : '当前班级' }}</span>
        </div>
        <el-table :data="statistics.classComparisons" size="small">
          <el-table-column prop="className" label="班级" min-width="150" show-overflow-tooltip />
          <el-table-column prop="studentCount" label="人数" width="70" />
          <el-table-column prop="taskCount" label="任务" width="70" />
          <el-table-column label="提交率" width="110">
            <template #default="{ row }">{{ formatPercent(row.submissionRate) }}</template>
          </el-table-column>
          <el-table-column label="均分" width="90">
            <template #default="{ row }">{{ formatNumber(row.averageScore) }}</template>
          </el-table-column>
          <el-table-column prop="riskCount" label="风险" width="70" />
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h3>风险学生</h3>
          <span>按风险分排序</span>
        </div>
        <el-table :data="statistics.riskStudents" size="small">
          <el-table-column prop="studentName" label="学生" min-width="90" />
          <el-table-column prop="className" label="班级" min-width="120" show-overflow-tooltip />
          <el-table-column prop="missingCount" label="缺交" width="70" />
          <el-table-column prop="lowScoreCount" label="低分" width="70" />
          <el-table-column prop="switchScreenCount" label="切屏" width="70" />
          <el-table-column label="均分" width="80">
            <template #default="{ row }">{{ formatNumber(row.averageScore) }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel survey-panel">
        <div class="panel-header">
          <h3>问卷反馈</h3>
          <span>{{ statistics.surveySummary.totalResponses }} 份反馈</span>
        </div>
        <div class="survey-summary">
          <div>
            <strong>{{ statistics.surveySummary.surveyCount }}</strong>
            <span>问卷总数</span>
          </div>
          <div>
            <strong>{{ statistics.surveySummary.publishedCount }}</strong>
            <span>已发布</span>
          </div>
          <div>
            <strong>{{ statistics.surveySummary.totalResponses }}</strong>
            <span>回收数</span>
          </div>
        </div>
        <div class="survey-list">
          <button
            v-for="survey in statistics.surveySummary.recentSurveys"
            :key="survey.surveyId"
            class="survey-row"
            @click="router.push('/teacher/surveys')"
          >
            <span>{{ survey.title }}</span>
            <em>{{ survey.totalResponses }} 份</em>
          </button>
          <el-empty v-if="statistics.surveySummary.recentSurveys.length === 0" description="暂无问卷数据" :image-size="70" />
        </div>
      </div>
    </section>

    <section class="insight-grid">
      <div class="panel trend-panel">
        <div class="panel-header">
          <h3>成绩趋势</h3>
          <span>最近考试</span>
        </div>
        <div class="trend-list">
          <div v-for="item in statistics.scoreTrends" :key="item.taskId" class="trend-row">
            <div class="trend-meta">
              <strong>{{ item.title }}</strong>
              <span>{{ formatDateTime(item.deadline) }} · 提交 {{ item.submittedCount }}/{{ item.totalStudents }}</span>
            </div>
            <div class="trend-score">
              <span>{{ formatNumber(item.averageScore) }}</span>
              <em>{{ formatPercent(item.completionRate) }}</em>
            </div>
          </div>
          <el-empty v-if="statistics.scoreTrends.length === 0" description="暂无成绩趋势" :image-size="70" />
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h3>薄弱知识点</h3>
          <span>按错误率排序</span>
        </div>
        <div class="weak-list">
          <div v-for="item in statistics.weakKnowledgePoints" :key="`${item.courseCategory}-${item.knowledgePoint}`" class="weak-row">
            <div class="weak-main">
              <strong>{{ item.knowledgePoint }}</strong>
              <span>{{ item.courseCategory || '未分类' }} · {{ item.wrongCount }}/{{ item.attempts }} 次错误</span>
            </div>
            <div class="weak-rate">
              <span>{{ formatPercent(item.wrongRate) }}</span>
              <em>得分率 {{ formatPercent(item.averageScoreRate) }}</em>
            </div>
          </div>
          <el-empty v-if="statistics.weakKnowledgePoints.length === 0" description="暂无答题明细" :image-size="70" />
        </div>
      </div>

      <div class="panel ai-panel">
        <div class="panel-header">
          <h3>AI 使用概览</h3>
          <span>学生与知识库</span>
        </div>
        <div class="ai-grid">
          <div>
            <strong>{{ statistics.aiUsageSummary.studentQuestionCount }}</strong>
            <span>学生提问</span>
          </div>
          <div>
            <strong>{{ statistics.aiUsageSummary.activeStudentCount }}</strong>
            <span>活跃学生</span>
          </div>
          <div>
            <strong>{{ statistics.aiUsageSummary.teacherQuestionCount }}</strong>
            <span>教师提问</span>
          </div>
          <div>
            <strong>{{ statistics.aiUsageSummary.knowledgeDocumentCount }}</strong>
            <span>知识文档</span>
          </div>
          <div>
            <strong>{{ statistics.aiUsageSummary.knowledgeChunkCount }}</strong>
            <span>知识切片</span>
          </div>
          <div>
            <strong>{{ statistics.aiUsageSummary.reviewNoteCount }}</strong>
            <span>复习笔记</span>
          </div>
        </div>
        <div class="topic-list">
          <div v-for="topic in statistics.aiUsageSummary.topTopics" :key="topic.label" class="topic-row">
            <span>{{ topic.label }}</span>
            <em>{{ topic.count }}</em>
          </div>
          <el-empty v-if="statistics.aiUsageSummary.topTopics.length === 0" description="暂无 AI 主题" :image-size="60" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Collection, DataLine, Document, Finished, Refresh, School, TrendCharts, User } from '@element-plus/icons-vue'
import { getTeacherStatistics, type TeacherStatistics, type TeacherTaskProgress } from '@/api/teacher'

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
  { label: '管理班级', value: statistics.overview.classCount, icon: School },
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

function goToTask(task: TeacherTaskProgress) {
  if (task.type === 'EXAM') {
    router.push('/teacher/exams')
  } else {
    router.push('/teacher/homework')
  }
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
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  min-height: 100%;
}

.stats-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.stats-header h2 {
  margin: 0;
  color: #e2e8f0;
  font-size: 24px;
  letter-spacing: 0;
}

.stats-header p {
  margin: 6px 0 0;
  color: rgba(226, 232, 240, 0.6);
  font-size: 13px;
}

.filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
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
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(12, 20, 40, 0.74);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(14px);
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 92px;
  padding: 14px;
  border-radius: 8px;
}

.metric-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: rgba(64, 128, 255, 0.12);
  color: #8bb5ff;
}

.metric-value {
  color: #f8fafc;
  font-family: 'JetBrains Mono', monospace;
  font-size: 25px;
  font-weight: 700;
  line-height: 1;
}

.metric-label {
  margin-top: 7px;
  color: rgba(226, 232, 240, 0.58);
  font-size: 12px;
}

.main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.secondary-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 16px;
}

.insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(320px, 0.9fr);
  gap: 16px;
}

.panel {
  min-width: 0;
  border-radius: 8px;
  padding: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-header h3 {
  margin: 0;
  color: #e2e8f0;
  font-size: 15px;
  letter-spacing: 0;
}

.panel-header span {
  color: rgba(226, 232, 240, 0.48);
  font-size: 12px;
}

.task-list {
  display: grid;
  gap: 10px;
}

.task-row,
.survey-row {
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.06);
  background: rgba(255, 255, 255, 0.03);
  color: inherit;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.task-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 16px;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  text-align: left;
}

.task-row:hover,
.survey-row:hover {
  border-color: rgba(64, 128, 255, 0.32);
  background: rgba(64, 128, 255, 0.08);
}

.task-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #e2e8f0;
  font-weight: 600;
}

.task-title span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta,
.progress-foot,
.progress-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: rgba(226, 232, 240, 0.52);
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
  color: #bfdbfe;
  font-weight: 700;
}

.progress-foot {
  margin-top: 7px;
}

.distribution-list {
  display: grid;
  gap: 13px;
}

.distribution-row {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 34px;
  gap: 10px;
  align-items: center;
  color: rgba(226, 232, 240, 0.72);
  font-size: 12px;
}

.bar-track {
  height: 9px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #4080ff, #22c55e);
}

.count {
  text-align: right;
  font-family: 'JetBrains Mono', monospace;
}

.survey-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.survey-summary div {
  padding: 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.survey-summary strong {
  display: block;
  color: #f8fafc;
  font-family: 'JetBrains Mono', monospace;
  font-size: 22px;
}

.survey-summary span {
  color: rgba(226, 232, 240, 0.54);
  font-size: 12px;
}

.survey-list {
  display: grid;
  gap: 8px;
}

.survey-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  text-align: left;
}

.survey-row span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #e2e8f0;
}

.survey-row em {
  flex-shrink: 0;
  color: rgba(226, 232, 240, 0.55);
  font-style: normal;
  font-size: 12px;
}

.trend-list,
.weak-list,
.topic-list {
  display: grid;
  gap: 9px;
}

.trend-row,
.weak-row,
.topic-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.trend-meta,
.weak-main {
  min-width: 0;
}

.trend-meta strong,
.weak-main strong {
  display: block;
  overflow: hidden;
  color: #e2e8f0;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trend-meta span,
.weak-main span,
.trend-score em,
.weak-rate em {
  display: block;
  margin-top: 5px;
  color: rgba(226, 232, 240, 0.5);
  font-size: 12px;
  font-style: normal;
}

.trend-score,
.weak-rate {
  flex-shrink: 0;
  text-align: right;
}

.trend-score span,
.weak-rate span {
  color: #bfdbfe;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
}

.ai-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.ai-grid div {
  padding: 10px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.ai-grid strong {
  display: block;
  color: #f8fafc;
  font-family: 'JetBrains Mono', monospace;
  font-size: 21px;
}

.ai-grid span {
  color: rgba(226, 232, 240, 0.54);
  font-size: 12px;
}

.topic-row {
  align-items: center;
  padding: 8px 10px;
}

.topic-row span {
  min-width: 0;
  overflow: hidden;
  color: rgba(226, 232, 240, 0.78);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-row em {
  color: #bfdbfe;
  font-family: 'JetBrains Mono', monospace;
  font-style: normal;
}

@media (max-width: 1280px) {
  .kpi-grid {
    grid-template-columns: repeat(4, minmax(140px, 1fr));
  }

  .main-grid,
  .secondary-grid,
  .insight-grid {
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
