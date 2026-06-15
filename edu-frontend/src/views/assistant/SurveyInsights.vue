<template>
  <div class="assistant-surveys" v-loading="loading">
    <header class="page-header">
      <div>
        <h2>问卷反馈</h2>
        <p>查看负责班级的问卷填写名单、未填写学生和原始反馈明细。</p>
      </div>
      <el-button :icon="Refresh" @click="loadSurveys">刷新</el-button>
    </header>

    <section class="workbench">
      <aside class="survey-list">
        <button
          v-for="item in surveys"
          :key="item.surveyId"
          class="survey-item"
          :class="{ active: currentSurvey?.surveyId === item.surveyId }"
          @click="selectSurvey(item)"
        >
          <span class="item-title">{{ item.title }}</span>
          <span class="item-meta">截止 {{ formatTime(item.endTime) }}</span>
          <span class="item-stats">
            反馈 {{ item.totalSubmissions || 0 }}
            <em>{{ item.isAnonymousRequired ? '匿名' : '实名' }}</em>
          </span>
        </button>
        <el-empty v-if="!surveys.length" description="暂无问卷数据" :image-size="80" />
      </aside>

      <main class="detail-panel" v-if="currentSurvey">
        <div class="detail-head">
          <div>
            <h3>{{ responses?.title || currentSurvey.title }}</h3>
            <p>{{ currentSurvey.targetTeacherName || '未指定讲师' }} · {{ currentSurvey.isAnonymousRequired ? '匿名问卷' : '实名问卷' }}</p>
          </div>
          <div class="head-actions">
            <el-button
              size="small"
              :icon="Download"
              :disabled="!canUseMissingList"
              @click="exportMissingStudents"
            >
              导出未填写
            </el-button>
            <el-button
              type="primary"
              size="small"
              :disabled="!canUseMissingList"
              @click="remindMissingStudents"
            >
              一键提醒
            </el-button>
            <el-tag :type="currentSurvey.status === 1 ? 'success' : 'info'" effect="plain">
              {{ currentSurvey.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </div>
        </div>

        <section class="summary-grid">
          <div>
            <strong>{{ responses?.totalStudents || 0 }}</strong>
            <span>目标学生</span>
          </div>
          <div>
            <strong>{{ responses?.submittedCount || 0 }}</strong>
            <span>已填写</span>
          </div>
          <div>
            <strong>{{ responses?.unsubmittedCount || 0 }}</strong>
            <span>未填写</span>
          </div>
          <div>
            <strong>{{ completionRate }}%</strong>
            <span>完成率</span>
          </div>
        </section>

        <el-tabs v-model="activeTab">
          <el-tab-pane :label="`反馈明细 ${responses?.responses.length || 0}`" name="responses">
            <el-table :data="responses?.responses || []" height="430" size="small">
              <el-table-column prop="studentName" label="学生" width="130" />
              <el-table-column prop="username" label="账号" width="120" show-overflow-tooltip />
              <el-table-column prop="className" label="班级" min-width="140" show-overflow-tooltip />
              <el-table-column label="提交时间" width="150">
                <template #default="{ row }">{{ formatTime(row.submitTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="90" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" text size="small" @click="openResponse(row)">查看</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`未填写 ${responses?.unsubmittedStudents.length || 0}`" name="missing">
            <el-alert
              v-if="responses?.isAnonymousRequired"
              type="info"
              show-icon
              :closable="false"
              title="匿名问卷不展示未填写学生名单，仅展示未填写人数。"
            />
            <el-table v-else :data="responses?.unsubmittedStudents || []" height="430" size="small">
              <el-table-column prop="studentName" label="学生" min-width="120" />
              <el-table-column prop="username" label="账号" min-width="140" />
              <el-table-column prop="className" label="班级" min-width="160" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </main>

      <main class="detail-panel empty-panel" v-else>
        <el-empty description="请选择左侧问卷" />
      </main>
    </section>

    <el-drawer v-model="responseVisible" size="640px" :title="responseTarget?.studentName || '反馈详情'">
      <div v-if="responseTarget" class="response-detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="学生">{{ responseTarget.studentName }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ responseTarget.className }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatTime(responseTarget.submitTime) }}</el-descriptions-item>
        </el-descriptions>
        <div v-for="answer in responseTarget.answers" :key="answer.questionId" class="answer-card">
          <div class="answer-head">
            <strong>{{ answer.title }}</strong>
            <el-tag size="small" effect="plain">{{ questionTypeText(answer.type) }}</el-tag>
          </div>
          <p>{{ answer.answerValue || '-' }}</p>
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
  getAssistantSurveyList,
  getAssistantSurveyResponses,
  remindAssistantSurveyStudents,
  type AssistantSurveyResponseItem,
  type AssistantSurveyResponses
} from '@/api/assistantInsight'
import type { SurveyListItem } from '@/api/survey'
import { exportRowsToXlsx } from '@/utils/exportRows'

const loading = ref(false)
const surveys = ref<SurveyListItem[]>([])
const currentSurvey = ref<SurveyListItem | null>(null)
const responses = ref<AssistantSurveyResponses | null>(null)
const responseTarget = ref<AssistantSurveyResponseItem | null>(null)
const responseVisible = ref(false)
const activeTab = ref('responses')

const completionRate = computed(() => {
  if (!responses.value?.totalStudents) return '0.0'
  return ((responses.value.submittedCount / responses.value.totalStudents) * 100).toFixed(1)
})

const canUseMissingList = computed(() =>
  !responses.value?.isAnonymousRequired && Boolean(responses.value?.unsubmittedStudents.length)
)

async function loadSurveys() {
  loading.value = true
  try {
    const res = await getAssistantSurveyList()
    surveys.value = res.data || []
    if (!currentSurvey.value && surveys.value.length) {
      await selectSurvey(surveys.value[0])
    } else if (currentSurvey.value) {
      const latest = surveys.value.find(item => item.surveyId === currentSurvey.value?.surveyId)
      currentSurvey.value = latest || null
      if (latest) await loadResponses(latest.surveyId)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载问卷反馈失败')
  } finally {
    loading.value = false
  }
}

async function selectSurvey(survey: SurveyListItem) {
  currentSurvey.value = survey
  activeTab.value = 'responses'
  await loadResponses(survey.surveyId)
}

async function loadResponses(surveyId: number) {
  const res = await getAssistantSurveyResponses(surveyId)
  responses.value = res.data
}

function openResponse(row: AssistantSurveyResponseItem) {
  responseTarget.value = row
  responseVisible.value = true
}

async function remindMissingStudents() {
  if (!currentSurvey.value || !responses.value?.unsubmittedStudents.length || responses.value.isAnonymousRequired) return
  const students = responses.value.unsubmittedStudents
  await ElMessageBox.confirm(`确认提醒 ${students.length} 名未填写问卷学生？`, '一键提醒', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await remindAssistantSurveyStudents(currentSurvey.value.surveyId, students.map(item => item.studentId))
  ElMessage.success('已记录提醒')
}

async function exportMissingStudents() {
  if (!currentSurvey.value || !responses.value?.unsubmittedStudents.length || responses.value.isAnonymousRequired) return
  await exportRowsToXlsx(
    `${currentSurvey.value.title}-未填写名单.xlsx`,
    [
      { label: '问卷标题', prop: () => currentSurvey.value?.title || '' },
      { label: '学生姓名', prop: 'studentName' },
      { label: '账号', prop: 'username' },
      { label: '班级', prop: 'className' },
      { label: '状态', prop: () => '未填写' },
      { label: '截止时间', prop: () => formatTime(currentSurvey.value?.endTime) }
    ],
    responses.value.unsubmittedStudents
  )
}

function formatTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function questionTypeText(type: string) {
  return { STAR: '星级评分', NPS: 'NPS', SCALE: '量表', TEXT: '文本' }[type] || type
}

onMounted(loadSurveys)
</script>

<style scoped>
.assistant-surveys {
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

.workbench {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  min-height: 620px;
}

.survey-list,
.detail-panel,
.summary-grid > div,
.answer-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
}

.survey-list,
.detail-panel {
  padding: 14px;
}

.survey-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
}

.survey-item {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--text-primary);
  text-align: left;
  cursor: pointer;
}

.survey-item.active {
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
  color: var(--primary);
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
}

.summary-grid > div {
  padding: 14px;
}

.summary-grid strong {
  display: block;
  color: var(--text-primary);
  font-size: 24px;
}

.summary-grid span {
  color: var(--text-secondary);
  font-size: 12px;
}

.response-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.answer-card {
  padding: 14px;
}

.answer-head strong {
  color: var(--text-primary);
}

.answer-card p {
  margin: 10px 0 0;
  padding: 10px;
  border-radius: 6px;
  background: var(--bg-base);
  color: var(--text-primary);
  white-space: pre-wrap;
}

@media (max-width: 980px) {
  .workbench,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
