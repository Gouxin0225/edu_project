<template>
  <div class="assistant-exams" v-loading="loading">
    <header class="page-header">
      <div>
        <h2>考试跟踪</h2>
        <p>查看负责班级的参考情况、成绩明细、切屏异常和学生答卷。</p>
      </div>
      <el-button :icon="Refresh" @click="loadExams">刷新</el-button>
    </header>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <strong>{{ item.value }}</strong>
        <span>{{ item.label }}</span>
      </div>
    </section>

    <section class="workbench">
      <aside class="exam-list">
        <button
          v-for="item in exams"
          :key="item.id"
          class="exam-item"
          :class="{ active: currentExam?.id === item.id }"
          @click="selectExam(item)"
        >
          <span class="item-title">{{ item.title }}</span>
          <span class="item-meta">{{ item.targetClassNames.join(' / ') || '全部班级' }}</span>
          <span class="item-stats">
            参考 {{ item.submittedCount }}/{{ item.targetStudentCount }}
            <em>未参加 {{ item.notSubmittedCount }}</em>
          </span>
        </button>
        <el-empty v-if="!exams.length" description="暂无考试数据" :image-size="80" />
      </aside>

      <main class="detail-panel" v-if="currentExam">
        <div class="detail-head">
          <div>
            <h3>{{ currentExam.title }}</h3>
            <p>考试时间 {{ formatTime(currentExam.startTime) }} ~ {{ formatTime(currentExam.endTime) }}</p>
          </div>
          <div class="head-actions">
            <el-button
              size="small"
              :icon="Download"
              :disabled="!participation?.notSubmittedStudents.length"
              @click="exportMissingStudents"
            >
              导出未参加
            </el-button>
            <el-button
              type="primary"
              size="small"
              :disabled="!participation?.notSubmittedStudents.length"
              @click="remindMissingStudents"
            >
              一键提醒
            </el-button>
            <el-tag :type="examStatusType(currentExam.status)" effect="plain">{{ examStatusText(currentExam.status) }}</el-tag>
          </div>
        </div>

        <div class="metric-strip">
          <div>
            <strong>{{ currentExam.averageScore?.toFixed?.(1) || '0.0' }}</strong>
            <span>平均分</span>
          </div>
          <div>
            <strong>{{ currentExam.passRate?.toFixed?.(1) || '0.0' }}%</strong>
            <span>及格率</span>
          </div>
          <div>
            <strong>{{ currentExam.inProgressCount }}</strong>
            <span>答题中</span>
          </div>
          <div>
            <strong>{{ currentExam.notSubmittedCount }}</strong>
            <span>未参加</span>
          </div>
        </div>

        <el-tabs v-model="activeTab">
          <el-tab-pane label="成绩明细" name="scores">
            <el-table :data="submissions" size="small" height="420">
              <el-table-column prop="studentName" label="学生" min-width="110" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="submissionStatusType(row.status)" size="small">{{ submissionStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="得分" width="90">
                <template #default="{ row }">{{ row.scoreGained ?? '-' }}</template>
              </el-table-column>
              <el-table-column label="切屏" width="80">
                <template #default="{ row }">
                  <el-tag v-if="row.switchScreenCount" type="warning" size="small">{{ row.switchScreenCount }}</el-tag>
                  <span v-else>0</span>
                </template>
              </el-table-column>
              <el-table-column label="交卷时间" min-width="150">
                <template #default="{ row }">{{ formatTime(row.submitTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" text size="small" @click="viewSubmission(row)">答卷</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`未参加 ${participation?.notSubmittedStudents.length || 0}`" name="missing">
            <el-table :data="participation?.notSubmittedStudents || []" size="small" height="420">
              <el-table-column prop="studentName" label="学生" min-width="140" />
              <el-table-column label="状态" width="120">
                <template #default>
                  <el-tag type="danger" size="small">未参加</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`答题中 ${participation?.inProgressStudents.length || 0}`" name="progress">
            <el-table :data="participation?.inProgressStudents || []" size="small" height="420">
              <el-table-column prop="studentName" label="学生" min-width="140" />
              <el-table-column label="开始时间" min-width="160">
                <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="120">
                <template #default="{ row }">
                  <el-tag type="warning" size="small">{{ submissionStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </main>

      <main class="detail-panel empty-panel" v-else>
        <el-empty description="请选择左侧考试" />
      </main>
    </section>

    <el-drawer v-model="detailVisible" size="720px" :title="submissionDetail?.studentName || '答卷详情'">
      <div v-if="submissionDetail" class="answer-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生">{{ submissionDetail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ submissionStatusText(submissionDetail.status) }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatTime(submissionDetail.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ submissionDetail.totalScoreGained ?? '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-for="(answer, index) in submissionDetail.answers" :key="answer.questionId" class="answer-card">
          <div class="answer-head">
            <strong>第{{ index + 1 }}题</strong>
            <div>
              <el-tag size="small" effect="plain">{{ questionTypeText(answer.type) }}</el-tag>
              <el-tag size="small" type="warning" effect="plain">{{ answer.scoreGained ?? 0 }}/{{ answer.questionScore }}</el-tag>
            </div>
          </div>
          <p class="question-content">{{ answer.content }}</p>
          <div class="answer-block">
            <span>学生答案</span>
            <p>{{ formatAnswer(answer.studentAnswer, answer.type, answer.optionsJson) || '未作答' }}</p>
          </div>
          <div class="answer-block">
            <span>标准答案</span>
            <p>{{ formatAnswer(answer.standardAnswer, answer.type, answer.optionsJson) || '-' }}</p>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Download, Refresh } from '@element-plus/icons-vue'
import {
  getAssistantExamList,
  getAssistantExamParticipation,
  getAssistantExamSubmissionDetail,
  getAssistantExamSubmissions,
  remindAssistantExamStudents,
  type AssistantExamItem
} from '@/api/assistantInsight'
import type { ExamParticipation, SubmissionDetail, SubmissionItem } from '@/api/exam'
import { exportRowsToXlsx } from '@/utils/exportRows'

const loading = ref(false)
const detailLoading = ref(false)
const exams = ref<AssistantExamItem[]>([])
const currentExam = ref<AssistantExamItem | null>(null)
const submissions = ref<SubmissionItem[]>([])
const participation = ref<ExamParticipation | null>(null)
const submissionDetail = ref<SubmissionDetail | null>(null)
const detailVisible = ref(false)
const activeTab = ref('scores')

const summaryCards = computed(() => [
  { label: '考试数量', value: exams.value.length },
  { label: '目标学生', value: exams.value.reduce((sum, item) => sum + Number(item.targetStudentCount || 0), 0) },
  { label: '已交卷', value: exams.value.reduce((sum, item) => sum + Number(item.submittedCount || 0), 0) },
  { label: '未参加', value: exams.value.reduce((sum, item) => sum + Number(item.notSubmittedCount || 0), 0) },
  { label: '答题中', value: exams.value.reduce((sum, item) => sum + Number(item.inProgressCount || 0), 0) }
])

async function loadExams() {
  loading.value = true
  try {
    const res = await getAssistantExamList()
    exams.value = res.data || []
    if (!currentExam.value && exams.value.length) {
      await selectExam(exams.value[0])
    } else if (currentExam.value) {
      const latest = exams.value.find(item => item.id === currentExam.value?.id)
      currentExam.value = latest || null
      if (latest) await loadExamDetail(latest.id)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载考试跟踪失败')
  } finally {
    loading.value = false
  }
}

async function selectExam(exam: AssistantExamItem) {
  currentExam.value = exam
  activeTab.value = 'scores'
  await loadExamDetail(exam.id)
}

async function loadExamDetail(examId: number) {
  const [submissionRes, participationRes] = await Promise.all([
    getAssistantExamSubmissions(examId),
    getAssistantExamParticipation(examId)
  ])
  submissions.value = submissionRes.data || []
  participation.value = participationRes.data
}

async function viewSubmission(row: SubmissionItem) {
  if (!currentExam.value) return
  detailLoading.value = true
  try {
    const res = await getAssistantExamSubmissionDetail(currentExam.value.id, row.submissionId)
    submissionDetail.value = res.data
    detailVisible.value = true
  } catch (error: any) {
    ElMessage.error(error?.message || '加载答卷详情失败')
  } finally {
    detailLoading.value = false
  }
}

async function remindMissingStudents() {
  if (!currentExam.value || !participation.value?.notSubmittedStudents.length) return
  const students = participation.value.notSubmittedStudents
  await ElMessageBox.confirm(`确认提醒 ${students.length} 名未参加考试学生？`, '一键提醒', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await remindAssistantExamStudents(currentExam.value.id, students.map(item => item.studentId))
  ElMessage.success('已记录提醒')
}

async function exportMissingStudents() {
  if (!currentExam.value || !participation.value?.notSubmittedStudents.length) return
  await exportRowsToXlsx(
    `${currentExam.value.title}-未参加名单.xlsx`,
    [
      { label: '考试标题', prop: () => currentExam.value?.title || '' },
      { label: '学生姓名', prop: 'studentName' },
      { label: '状态', prop: () => '未参加' },
      { label: '开始时间', prop: () => formatTime(currentExam.value?.startTime) },
      { label: '结束时间', prop: () => formatTime(currentExam.value?.endTime) }
    ],
    participation.value.notSubmittedStudents
  )
}

function formatTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function examStatusText(status: string) {
  return { NOT_STARTED: '未开始', PUBLISHED: '进行中', ENDED: '已结束' }[status] || status
}

function examStatusType(status: string) {
  return { NOT_STARTED: 'info', PUBLISHED: 'success', ENDED: 'warning' }[status] || 'info'
}

function submissionStatusText(status: string) {
  return { UN: '答题中', SUBMITTED: '待批改', GRADED: '已评分', RETURNED: '已打回', NOT_SUBMITTED: '未参加' }[status] || status
}

function submissionStatusType(status: string) {
  return { UN: 'warning', SUBMITTED: 'info', GRADED: 'success', RETURNED: 'danger' }[status] || 'info'
}

function questionTypeText(type: string) {
  return { SINGLE: '单选', MULTI: '多选', MULTIPLE: '多选', JUDGE: '判断', SHORT: '简答', CODE: '编程' }[type] || type
}

function formatAnswer(value?: string | null, type?: string, optionsJson?: string | null) {
  if (!value) return ''
  if (type === 'JUDGE') {
    const normalized = value.trim().toLowerCase()
    if (['t', 'true', '1', '正确', '对', '是'].includes(normalized)) return '正确'
    if (['f', 'false', '0', '错误', '错', '否'].includes(normalized)) return '错误'
    return value
  }
  if (type !== 'SINGLE' && type !== 'MULTI' && type !== 'MULTIPLE') return value
  const options = parseOptions(optionsJson)
  if (!options.length) return value
  const letters = value.split(/[,，、\s]+/).map(item => item.trim().toUpperCase()).filter(Boolean)
  return letters.map(letter => {
    const index = letter.charCodeAt(0) - 65
    return index >= 0 && index < options.length ? `${letter}. ${options[index]}` : letter
  }).join(', ')
}

function parseOptions(optionsJson?: string | null): string[] {
  if (!optionsJson) return []
  try {
    const parsed = JSON.parse(optionsJson)
    return Array.isArray(parsed) ? parsed.map(String) : []
  } catch {
    return []
  }
}

onMounted(loadExams)
</script>

<style scoped>
.assistant-exams {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
}

.page-header,
.detail-head,
.head-actions,
.item-stats,
.answer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.head-actions {
  justify-content: flex-end;
  flex-wrap: wrap;
}

.page-header h2,
.detail-head h3 {
  margin: 0;
  color: var(--text-primary);
}

.page-header p,
.detail-head p,
.item-meta {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.summary-grid,
.metric-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 12px;
}

.summary-card,
.metric-strip > div,
.detail-panel,
.exam-list,
.answer-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
}

.summary-card,
.metric-strip > div {
  padding: 14px;
}

.summary-card strong,
.metric-strip strong {
  display: block;
  color: var(--text-primary);
  font-size: 24px;
}

.summary-card span,
.metric-strip span {
  color: var(--text-secondary);
  font-size: 12px;
}

.workbench {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  min-height: 560px;
}

.exam-list,
.detail-panel {
  padding: 14px;
}

.exam-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
}

.exam-item {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
}

.exam-item.active {
  border-color: var(--primary);
  background: var(--primary-dim);
}

.item-title {
  display: block;
  font-weight: 700;
}

.item-stats {
  margin-top: 10px;
  color: var(--text-primary);
  font-size: 12px;
}

.item-stats em {
  color: var(--danger);
  font-style: normal;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.empty-panel {
  align-items: center;
  justify-content: center;
}

.answer-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.answer-card {
  padding: 14px;
}

.answer-head strong,
.question-content {
  color: var(--text-primary);
}

.answer-block {
  margin-top: 12px;
}

.answer-block span {
  color: var(--text-secondary);
  font-size: 12px;
}

.answer-block p {
  margin: 6px 0 0;
  padding: 10px;
  border-radius: 6px;
  background: var(--bg-base);
  color: var(--text-primary);
  white-space: pre-wrap;
}

@media (max-width: 980px) {
  .workbench,
  .summary-grid,
  .metric-strip {
    grid-template-columns: 1fr;
  }
}
</style>
