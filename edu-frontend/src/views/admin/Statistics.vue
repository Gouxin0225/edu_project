<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <h2>数据统计</h2>
        <p>先定位需要关注的班级，再进入详情查看作业、考试、问卷和人员。</p>
      </div>
      <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
    </header>

    <section class="overview-grid">
      <div v-for="item in overviewCards" :key="item.label" class="metric">
        <component :is="item.icon" class="metric-icon" />
        <div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-label">{{ item.label }}</div>
        </div>
      </div>
    </section>

    <section class="toolbar">
      <el-input
        v-model="keyword"
        class="search-input"
        :prefix-icon="Search"
        clearable
        placeholder="搜索班级、讲师或班主任"
      />
      <el-select v-model="riskFilter" class="filter-select" placeholder="关注状态">
        <el-option label="全部班级" value="ALL" />
        <el-option label="需关注" value="RISK" />
        <el-option label="正常" value="NORMAL" />
      </el-select>
      <el-select v-model="sortKey" class="filter-select" placeholder="排序方式">
        <el-option label="按关注优先" value="risk" />
        <el-option label="按作业完成率" value="homework" />
        <el-option label="按考试完成率" value="exam" />
        <el-option label="按问卷完成率" value="survey" />
        <el-option label="按学生人数" value="student" />
      </el-select>
    </section>

    <el-skeleton v-if="loading" :rows="8" animated class="skeleton" />
    <el-empty v-else-if="!classes.length" description="暂无班级数据" />

    <section v-else class="table-panel">
      <el-table :data="filteredRows" row-key="classId" stripe>
        <el-table-column label="班级" min-width="180" fixed>
          <template #default="{ row }">
            <div class="class-cell">
              <strong>{{ row.className }}</strong>
              <span>{{ row.studentCount }} 名学生</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.riskLevel === 'RISK' ? 'danger' : 'success'" effect="plain">
              {{ row.riskLevel === 'RISK' ? '需关注' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="讲师 / 班主任" min-width="220">
          <template #default="{ row }">
            <div class="people-cell">
              <span>讲师：{{ names(row.teachers) }}</span>
              <span>班主任：{{ names(row.assistants) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="作业完成率" min-width="160">
          <template #default="{ row }">
            <RateCell :rate="row.homeworkRate" :count="row.homeworkSummary.taskCount" />
          </template>
        </el-table-column>

        <el-table-column label="作业均分" width="110" align="center">
          <template #default="{ row }">
            <ScoreCell :score="row.homeworkSummary.averageScore" :sample-count="row.homeworkSummary.scoreSampleCount" />
          </template>
        </el-table-column>

        <el-table-column label="考试完成率" min-width="160">
          <template #default="{ row }">
            <RateCell :rate="row.examRate" :count="row.examSummary.taskCount" />
          </template>
        </el-table-column>

        <el-table-column label="考试均分" width="110" align="center">
          <template #default="{ row }">
            <ScoreCell :score="row.examSummary.averageScore" :sample-count="row.examSummary.scoreSampleCount" />
          </template>
        </el-table-column>

        <el-table-column label="问卷完成率" min-width="160">
          <template #default="{ row }">
            <RateCell :rate="row.surveyRate" :count="row.surveySummary.surveyCount" />
          </template>
        </el-table-column>

        <el-table-column label="问卷均分" width="110" align="center">
          <template #default="{ row }">
            <ScoreCell :score="row.surveySummary.averageScore" :sample-count="row.surveySummary.scoreSampleCount" :digits="2" />
          </template>
        </el-table-column>

        <el-table-column label="待处理" width="130" align="center">
          <template #default="{ row }">
            <div class="pending-cell">
              <strong>{{ row.pendingTotal }}</strong>
              <span>未交/未填</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="待批改" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.pendingGradeTotal ? 'warning' : 'info'" effect="plain">
              {{ row.pendingGradeTotal }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110" align="right" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text :icon="View" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer
      v-model="detailVisible"
      size="760px"
      :with-header="false"
      class="class-drawer"
      modal-class="class-drawer-modal"
    >
      <div v-if="selectedClass" class="drawer-content">
        <header class="drawer-header">
          <div>
            <h3>{{ selectedClass.className }}</h3>
            <p>{{ selectedClass.studentCount }} 名学生 · {{ selectedClass.statusName }}</p>
          </div>
          <el-tag :type="selectedClass.riskLevel === 'RISK' ? 'danger' : 'success'" effect="plain">
            {{ selectedClass.riskLevel === 'RISK' ? '需关注' : '正常' }}
          </el-tag>
        </header>

        <section class="drawer-metrics">
          <div>
            <strong>{{ formatPercent(selectedClass.homeworkRate) }}</strong>
            <span>作业完成率</span>
          </div>
          <div>
            <strong>{{ formatPercent(selectedClass.examRate) }}</strong>
            <span>考试完成率</span>
          </div>
          <div>
            <strong>{{ formatPercent(selectedClass.surveyRate) }}</strong>
            <span>问卷完成率</span>
          </div>
          <div>
            <strong>{{ selectedClass.pendingGradeTotal }}</strong>
            <span>待批改</span>
          </div>
          <div>
            <strong>{{ formatScore(selectedClass.homeworkSummary.averageScore, 1, selectedClass.homeworkSummary.scoreSampleCount) }}</strong>
            <span>作业均分 · {{ formatSample(selectedClass.homeworkSummary.scoreSampleCount) }}</span>
          </div>
          <div>
            <strong>{{ formatScore(selectedClass.examSummary.averageScore, 1, selectedClass.examSummary.scoreSampleCount) }}</strong>
            <span>考试均分 · {{ formatSample(selectedClass.examSummary.scoreSampleCount) }}</span>
          </div>
          <div>
            <strong>{{ formatScore(selectedClass.surveySummary.averageScore, 2, selectedClass.surveySummary.scoreSampleCount) }}</strong>
            <span>问卷均分 · {{ formatSample(selectedClass.surveySummary.scoreSampleCount) }}</span>
          </div>
        </section>

        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="作业" name="homework">
            <WorkList
              empty-text="暂无作业"
              type-label="作业"
              :summary="selectedClass.homeworkSummary"
              :items="selectedClass.homeworks"
            />
          </el-tab-pane>
          <el-tab-pane label="考试" name="exam">
            <WorkList
              empty-text="暂无考试"
              type-label="考试"
              :summary="selectedClass.examSummary"
              :items="selectedClass.exams"
            />
          </el-tab-pane>
          <el-tab-pane label="问卷" name="survey">
            <SurveyList :summary="selectedClass.surveySummary" :items="selectedClass.surveys" />
          </el-tab-pane>
          <el-tab-pane label="人员" name="people">
            <section class="people-detail">
              <div>
                <h4>讲师</h4>
                <div v-if="selectedClass.teachers.length" class="tag-list">
                  <el-tag v-for="teacher in selectedClass.teachers" :key="teacher.id" effect="plain">
                    {{ teacher.name }}
                  </el-tag>
                </div>
                <el-empty v-else description="暂无讲师" :image-size="64" />
              </div>
              <div>
                <h4>班主任</h4>
                <div v-if="selectedClass.assistants.length" class="tag-list">
                  <el-tag v-for="assistant in selectedClass.assistants" :key="assistant.id" type="warning" effect="plain">
                    {{ assistant.name }}
                  </el-tag>
                </div>
                <el-empty v-else description="暂无班主任" :image-size="64" />
              </div>
            </section>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { ElEmpty } from 'element-plus/es/components/empty/index'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElProgress } from 'element-plus/es/components/progress/index'
import { ElTag } from 'element-plus/es/components/tag/index'
import { Collection, DocumentChecked, Files, Refresh, School, Search, UserFilled, View, Warning } from '@element-plus/icons-vue'
import {
  getAdminClassStatistics,
  type AdminClassStatsModule,
  type AdminStatsPerson,
  type AdminSurveyItem,
  type AdminSurveySummary,
  type AdminWorkItem,
  type AdminWorkSummary
} from '@/api/admin/statistics'

type RiskFilter = 'ALL' | 'RISK' | 'NORMAL'
type SortKey = 'risk' | 'homework' | 'exam' | 'survey' | 'student'
type RiskLevel = 'RISK' | 'NORMAL'

interface ClassRow extends AdminClassStatsModule {
  homeworkRate: number
  examRate: number
  surveyRate: number
  pendingTotal: number
  pendingGradeTotal: number
  riskScore: number
  riskLevel: RiskLevel
}

const loading = ref(false)
const keyword = ref('')
const riskFilter = ref<RiskFilter>('ALL')
const sortKey = ref<SortKey>('risk')
const classes = ref<AdminClassStatsModule[]>([])
const selectedClass = ref<ClassRow | null>(null)
const detailVisible = ref(false)
const activeTab = ref('homework')
const overview = ref({
  classCount: 0,
  activeClassCount: 0,
  finishedClassCount: 0,
  studentCount: 0,
  homeworkCount: 0,
  examCount: 0,
  surveyCount: 0,
  homeworkAverageScore: 0,
  examAverageScore: 0,
  surveyAverageScore: 0,
  homeworkScoreSampleCount: 0,
  examScoreSampleCount: 0,
  surveyScoreSampleCount: 0
})

const overviewCards = computed(() => [
  { label: '班级', value: overview.value.classCount, icon: School },
  { label: '进行中', value: overview.value.activeClassCount, icon: Collection },
  { label: '学生', value: overview.value.studentCount, icon: UserFilled },
  { label: '作业', value: overview.value.homeworkCount, icon: Files },
  { label: '考试', value: overview.value.examCount, icon: DocumentChecked },
  { label: '问卷', value: overview.value.surveyCount, icon: Collection },
  { label: `作业均分 ${formatSample(overview.value.homeworkScoreSampleCount)}`, value: formatScore(overview.value.homeworkAverageScore, 1, overview.value.homeworkScoreSampleCount), icon: Files },
  { label: `考试均分 ${formatSample(overview.value.examScoreSampleCount)}`, value: formatScore(overview.value.examAverageScore, 1, overview.value.examScoreSampleCount), icon: DocumentChecked },
  { label: `问卷均分 ${formatSample(overview.value.surveyScoreSampleCount)}`, value: formatScore(overview.value.surveyAverageScore, 2, overview.value.surveyScoreSampleCount), icon: Collection }
])

const rows = computed<ClassRow[]>(() => classes.value.map(toClassRow))

const filteredRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return rows.value
    .filter(row => {
      if (riskFilter.value !== 'ALL' && row.riskLevel !== riskFilter.value) return false
      if (!query) return true
      return [
        row.className,
        ...row.teachers.map(item => item.name),
        ...row.assistants.map(item => item.name)
      ].some(value => value.toLowerCase().includes(query))
    })
    .sort((a, b) => {
      if (sortKey.value === 'homework') return a.homeworkRate - b.homeworkRate
      if (sortKey.value === 'exam') return a.examRate - b.examRate
      if (sortKey.value === 'survey') return a.surveyRate - b.surveyRate
      if (sortKey.value === 'student') return b.studentCount - a.studentCount
      return b.riskScore - a.riskScore
    })
})

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminClassStatistics()
    overview.value = res.data.overview
    classes.value = res.data.classes || []
    if (selectedClass.value) {
      const latest = rows.value.find(item => item.classId === selectedClass.value?.classId)
      selectedClass.value = latest || null
    }
  } catch (error) {
    ElMessage.error('数据统计加载失败')
  } finally {
    loading.value = false
  }
}

function toClassRow(item: AdminClassStatsModule): ClassRow {
  const homeworkRate = Number(item.homeworkSummary.completionRate || 0)
  const examRate = Number(item.examSummary.completionRate || 0)
  const surveyRate = Number(item.surveySummary.completionRate || 0)
  const pendingTotal = Number(item.homeworkSummary.pendingCount || 0)
    + Number(item.examSummary.pendingCount || 0)
    + Number(item.surveySummary.totalRequired - item.surveySummary.responseCount || 0)
  const pendingGradeTotal = Number(item.homeworkSummary.pendingGradeCount || 0)
    + Number(item.examSummary.pendingGradeCount || 0)
  const lowRatePenalty = [homeworkRate, examRate, surveyRate]
    .filter(rate => rate > 0 && rate < 60)
    .reduce((sum, rate) => sum + (60 - rate), 0)
  const missingPeoplePenalty = item.teachers.length === 0 || item.assistants.length === 0 ? 8 : 0
  const riskScore = pendingTotal + pendingGradeTotal * 2 + lowRatePenalty + missingPeoplePenalty

  return {
    ...item,
    homeworkRate,
    examRate,
    surveyRate,
    pendingTotal,
    pendingGradeTotal,
    riskScore,
    riskLevel: riskScore > 0 ? 'RISK' : 'NORMAL'
  }
}

function openDetail(row: ClassRow) {
  selectedClass.value = row
  activeTab.value = 'homework'
  detailVisible.value = true
}

function names(people: AdminStatsPerson[]) {
  return people.length ? people.map(item => item.name).join('、') : '暂无'
}

function formatDate(value?: string | null) {
  if (!value) return '未设置'
  return value.replace('T', ' ').slice(0, 16)
}

function formatPercent(value?: number) {
  return `${Number(value || 0).toFixed(1)}%`
}

function formatScore(value?: number, digits = 1, sampleCount?: number) {
  if (sampleCount !== undefined && sampleCount <= 0) return '-'
  return Number(value || 0).toFixed(digits)
}

function formatSample(value?: number) {
  return `n=${Number(value || 0)}`
}

function statusColor(rate: number) {
  if (rate < 60) return '#f87171'
  if (rate < 85) return '#fbbf24'
  return '#34d399'
}

const RateCell = defineComponent({
  name: 'RateCell',
  props: {
    rate: { type: Number, required: true },
    count: { type: Number, required: true }
  },
  setup(props) {
    return () => h('div', { class: 'rate-cell' }, [
      h('div', { class: 'rate-line' }, [
        h('strong', { style: { color: statusColor(props.rate) } }, formatPercent(props.rate)),
        h('span', `${props.count} 项`)
      ]),
      h(ElProgress, {
        percentage: Math.max(0, Math.min(100, Number(props.rate || 0))),
        strokeWidth: 8,
        showText: false,
        color: statusColor(props.rate)
      })
    ])
  }
})

const ScoreCell = defineComponent({
  name: 'ScoreCell',
  props: {
    score: { type: Number, default: 0 },
    sampleCount: { type: Number, default: 0 },
    digits: { type: Number, default: 1 }
  },
  setup(props) {
    return () => h('div', { class: 'score-cell' }, [
      h('strong', { class: 'score-text' }, formatScore(props.score, props.digits, props.sampleCount)),
      h('span', { class: 'score-sample' }, formatSample(props.sampleCount))
    ])
  }
})

const WorkList = defineComponent({
  name: 'WorkList',
  props: {
    typeLabel: { type: String, required: true },
    emptyText: { type: String, required: true },
    summary: { type: Object as () => AdminWorkSummary, required: true },
    items: { type: Array as () => AdminWorkItem[], required: true }
  },
  setup(props) {
    return () => h('section', { class: 'detail-list' }, [
      h('div', { class: 'summary-strip' }, [
        h('span', `${props.typeLabel} ${props.summary.taskCount} 项`),
        h('span', `提交 ${props.summary.submittedCount}/${props.summary.totalRequired}`),
        h('span', `未交 ${props.summary.pendingCount}`),
        h('span', `待批 ${props.summary.pendingGradeCount}`),
        h('span', `完成率 ${formatPercent(Number(props.summary.completionRate || 0))}`),
        h('span', `均分 ${formatScore(props.summary.averageScore, 1, props.summary.scoreSampleCount)} ${formatSample(props.summary.scoreSampleCount)}`)
      ]),
      props.items.length
        ? props.items.map(item => h('article', { class: 'work-card', key: item.taskId }, [
            h('div', { class: 'work-head' }, [
              h('div', [
                h('strong', item.title),
                h('p', `截止 ${formatDate(item.deadline)}`)
              ]),
              h(ElTag, { type: item.pendingCount || item.pendingGradeCount ? 'warning' : 'success', effect: 'plain' }, () =>
                item.pendingCount ? `未交 ${item.pendingCount}` : '进度正常'
              )
            ]),
            h(ElProgress, {
              percentage: Number(item.completionRate || 0),
              strokeWidth: 10,
              color: statusColor(Number(item.completionRate || 0))
            }),
            h('div', { class: 'work-stats' }, [
              h('span', `已交 ${item.submittedCount}/${item.totalStudents}`),
              h('span', `已批 ${item.gradedCount}`),
              h('span', `待批 ${item.pendingGradeCount}`),
              h('span', `退回 ${item.returnedCount}`),
              h('span', `均分 ${formatScore(item.averageScore, 1, item.scoreSampleCount)} ${formatSample(item.scoreSampleCount)}`)
            ])
          ]))
        : h(ElEmpty, { description: props.emptyText, imageSize: 80 })
    ])
  }
})

const SurveyList = defineComponent({
  name: 'SurveyList',
  props: {
    summary: { type: Object as () => AdminSurveySummary, required: true },
    items: { type: Array as () => AdminSurveyItem[], required: true }
  },
  setup(props) {
    return () => h('section', { class: 'detail-list' }, [
      h('div', { class: 'summary-strip' }, [
        h('span', `问卷 ${props.summary.surveyCount} 项`),
        h('span', `反馈 ${props.summary.responseCount}/${props.summary.totalRequired}`),
        h('span', `完成率 ${formatPercent(Number(props.summary.completionRate || 0))}`),
        h('span', `均分 ${formatScore(props.summary.averageScore, 2, props.summary.scoreSampleCount)} ${formatSample(props.summary.scoreSampleCount)}`)
      ]),
      props.items.length
        ? props.items.map(item => h('article', { class: 'work-card', key: item.surveyId }, [
            h('div', { class: 'work-head' }, [
              h('div', [
                h('strong', item.title),
                h('p', `截止 ${formatDate(item.deadline)}${item.targetTeacherName ? ` · 评价 ${item.targetTeacherName}` : ''}`)
              ]),
              h(ElTag, { type: item.pendingCount ? 'warning' : 'success', effect: 'plain' }, () =>
                item.pendingCount ? `未填 ${item.pendingCount}` : item.statusName
              )
            ]),
            h(ElProgress, {
              percentage: Number(item.completionRate || 0),
              strokeWidth: 10,
              color: statusColor(Number(item.completionRate || 0))
            }),
            h('div', { class: 'work-stats' }, [
              h('span', `已填 ${item.responseCount}/${item.totalStudents}`),
              h('span', `未填 ${item.pendingCount}`),
              h('span', `均分 ${formatScore(item.averageScore, 2, item.scoreSampleCount)} ${formatSample(item.scoreSampleCount)}`),
              h('span', item.statusName)
            ])
          ]))
        : h(ElEmpty, { description: '暂无问卷', imageSize: 80 })
    ])
  }
})

onMounted(loadData)
</script>

<style scoped>
.page-container {
  min-height: 100%;
  padding: 24px;
  color: var(--text-primary);
}

.page-header,
.toolbar,
.metric,
.rate-line,
.drawer-header,
.work-head,
.work-stats,
.summary-strip {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header,
.drawer-header,
.work-head {
  justify-content: space-between;
}

.page-header {
  align-items: flex-start;
  margin-bottom: 18px;
}

.page-header h2,
.drawer-header h3 {
  margin: 0;
  color: var(--text-primary);
  letter-spacing: 0;
}

.page-header h2 {
  font-size: 24px;
}

.page-header p,
.drawer-header p,
.work-head p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.metric,
.table-panel,
.toolbar,
.drawer-metrics > div,
.work-card,
.people-detail > div {
  border: 1px solid var(--border);
  background: var(--surface-card);
  border-radius: 8px;
}

.metric {
  padding: 14px;
}

.metric-icon {
  width: 22px;
  height: 22px;
  color: var(--primary-light);
}

.metric-value {
  color: var(--text-primary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 22px;
  font-weight: 700;
}

.metric-label,
.class-cell span,
.people-cell span,
.pending-cell span {
  color: var(--text-secondary);
}

.toolbar {
  margin-bottom: 16px;
  padding: 12px;
}

.search-input {
  max-width: 320px;
}

.filter-select {
  width: 150px;
}

.table-panel {
  padding: 12px;
}

.class-cell,
.people-cell,
.pending-cell {
  display: grid;
  gap: 5px;
}

.class-cell strong {
  color: var(--text-primary);
}

.rate-cell {
  display: grid;
  gap: 7px;
}

.rate-line {
  justify-content: space-between;
  font-size: 12px;
}

.pending-cell strong {
  color: var(--color-warning);
  font-family: 'JetBrains Mono', monospace;
  font-size: 18px;
}

.score-text {
  color: var(--primary-light);
  font-family: 'JetBrains Mono', monospace;
  font-size: 16px;
}

.score-cell {
  display: grid;
  gap: 3px;
  justify-items: center;
}

.score-sample {
  color: var(--text-muted);
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
}

.drawer-content {
  padding: 22px;
}

.drawer-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 18px 0;
}

.drawer-metrics > div {
  padding: 14px;
}

.drawer-metrics strong {
  display: block;
  color: var(--text-primary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 22px;
}

.drawer-metrics span {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
}

.detail-list {
  display: grid;
  gap: 12px;
}

.summary-strip {
  flex-wrap: wrap;
  border: 1px solid rgba(245, 158, 11, 0.28);
  border-radius: 8px;
  background: rgba(245, 158, 11, 0.10);
  color: var(--text-primary);
  padding: 10px 12px;
  font-size: 12px;
}

.work-card {
  padding: 14px;
}

.work-head {
  align-items: flex-start;
  margin-bottom: 12px;
}

.work-head strong {
  color: var(--text-primary);
}

.work-stats {
  flex-wrap: wrap;
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 12px;
}

.people-detail {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.people-detail > div {
  min-width: 0;
  padding: 14px;
}

.people-detail h4 {
  margin: 0 0 12px;
  color: var(--text-primary);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skeleton {
  padding: 20px;
}

.page-container :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: var(--surface-muted);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-text-color: var(--text-primary);
  --el-table-border-color: var(--border);
  --el-table-row-hover-bg-color: var(--surface-hover);
}

.page-container :deep(.el-table th.el-table__cell),
.page-container :deep(.el-table td.el-table__cell) {
  background: transparent;
}

.page-container :deep(.el-drawer) {
  background: var(--bg-surface);
  color: var(--text-primary);
}

:global(.class-drawer) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
}

:global(.class-drawer .el-drawer__body) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  padding: 0 !important;
}

:global(.class-drawer .el-tabs__item) {
  color: var(--text-secondary) !important;
}

:global(.class-drawer .el-tabs__item.is-active),
:global(.class-drawer .el-tabs__item:hover) {
  color: var(--primary-light) !important;
}

:global(.class-drawer .el-tabs__active-bar) {
  background-color: var(--primary-light) !important;
}

:global(.class-drawer .el-tabs__nav-wrap::after) {
  background-color: var(--border) !important;
}

:global(.class-drawer .el-progress__text) {
  color: var(--text-primary) !important;
}

:global(.class-drawer .el-empty__description p) {
  color: var(--text-secondary) !important;
}

:global(.class-drawer .el-tag) {
  background: var(--surface-muted) !important;
}

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-container {
    padding: 14px;
  }

  .page-header,
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .overview-grid,
  .drawer-metrics,
  .people-detail {
    grid-template-columns: 1fr;
  }

  .search-input,
  .filter-select {
    width: 100%;
    max-width: none;
  }
}
</style>
