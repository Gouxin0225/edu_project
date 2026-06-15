<template>
  <div class="page">
    <el-card shadow="never" v-loading="loading" class="survey-card">
      <template #header>
        <div class="header-bar">
          <span class="title-text">{{ surveyDetail?.title }}</span>
          <el-button @click="$router.back()" size="small" class="back-btn">返回</el-button>
        </div>
      </template>

      <div class="anonymous-warning" v-if="surveyDetail?.isAnonymousRequired === 1">
        <el-icon><Warning /></el-icon>
        <span>此问卷为匿名问卷，您的回答将不会显示您的个人信息</span>
      </div>

      <div class="survey-info">
        <p>授课教师：{{ surveyDetail?.targetTeacherName }}</p>
        <p>共 {{ surveyDetail?.questions?.length || 0 }} 道题</p>
      </div>

      <el-divider class="cyberpunk-divider" />

      <el-form :model="answers" :rules="formRules" ref="formRef" label-width="20px">
        <div v-for="(question, index) in surveyDetail?.questions" :key="question.questionId" class="question-block">
          <div class="question-title">
            <span class="required-mark" v-if="question.isRequired === 1">*</span>
            <span class="question-num">{{ index + 1 }}. </span>
            <span>{{ question.title }}</span>
          </div>

          <div class="question-content">
            <el-rate
              v-if="question.type === 'STAR'"
              v-model="answers[question.questionId]"
              :max="5"
              show-text
              :texts="['非常不满意', '不满意', '一般', '满意', '非常满意']"
              class="cyberpunk-rate"
            />

            <div v-else-if="question.type === 'NPS'" class="nps-container">
              <div class="nps-labels">
                <span>非常不推荐</span>
                <span>非常推荐</span>
              </div>
              <div class="nps-buttons">
                <el-button
                  v-for="score in 11"
                  :key="score - 1"
                  :type="answers[question.questionId] === score - 1 ? 'primary' : 'default'"
                  :class="{ selected: answers[question.questionId] === score - 1 }"
                  @click="answers[question.questionId] = score - 1"
                  size="default"
                  class="nps-btn"
                >
                  {{ score - 1 }}
                </el-button>
              </div>
              <div class="nps-result" v-if="answers[question.questionId] !== undefined">
                <el-tag :type="getNpsTagType(answers[question.questionId])" size="small" class="cyberpunk-tag">
                  {{ getNpsTagText(answers[question.questionId]) }}
                </el-tag>
              </div>
            </div>

            <div v-else-if="question.type === 'SCALE'" class="scale-container">
              <div class="scale-labels">
                <span v-if="question.optionsJson">{{ getScaleLabels(question.optionsJson).low }}</span>
                <span v-if="question.optionsJson">{{ getScaleLabels(question.optionsJson).high }}</span>
              </div>
              <el-slider
                v-model="answers[question.questionId]"
                :min="1"
                :max="5"
                :marks="getScaleMarks(question.optionsJson)"
                :step="1"
                class="cyberpunk-slider"
              />
            </div>

            <el-input
              v-else-if="question.type === 'TEXT'"
              v-model="answers[question.questionId]"
              type="textarea"
              :rows="4"
              placeholder="请输入您的回答"
              class="cyberpunk-textarea"
            />
          </div>
        </div>
      </el-form>

      <div class="submit-section">
        <el-button @click="$router.back()" class="cancel-btn">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting" class="submit-btn">提交问卷</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Warning } from '@element-plus/icons-vue'
import { getSurveyDetail, submitSurvey, type SurveyDetail, type SurveyAnswer } from '@/api/survey'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

const surveyDetail = ref<SurveyDetail | null>(null)
const answers = reactive<Record<number, any>>({})

const formRules = {}

function getScaleLabels(optionsJson: string): { low: string; high: string } {
  try {
    const labels = JSON.parse(optionsJson)
    return { low: labels[0] || '非常不同意', high: labels[4] || '非常同意' }
  } catch {
    return { low: '非常不同意', high: '非常同意' }
  }
}

function getScaleMarks(optionsJson: string): Record<number, string> {
  try {
    const labels = JSON.parse(optionsJson)
    return {
      1: labels[0] || '1',
      2: labels[1] || '2',
      3: labels[2] || '3',
      4: labels[3] || '4',
      5: labels[4] || '5'
    }
  } catch {
    return { 1: '1', 2: '2', 3: '3', 4: '4', 5: '5' }
  }
}

function getNpsTagType(score: number): string {
  if (score <= 6) return 'danger'
  if (score <= 8) return 'warning'
  return 'success'
}

function getNpsTagText(score: number): string {
  if (score <= 6) return '不推荐'
  if (score <= 8) return '中立'
  return '推荐'
}

async function fetchSurveyDetail() {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('无效的问卷ID')
    router.back()
    return
  }

  loading.value = true
  try {
    const res = await getSurveyDetail(id)
    surveyDetail.value = res.data
    surveyDetail.value?.questions?.forEach(q => {
      if (!(q.questionId in answers)) {
        if (q.type === 'STAR') answers[q.questionId] = 0
        else if (q.type === 'NPS') answers[q.questionId] = undefined
        else if (q.type === 'SCALE') answers[q.questionId] = 3
        else answers[q.questionId] = ''
      }
    })
  } catch (error: any) {
    ElMessage.error(error.message || '加载问卷详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

function validateAnswers(): boolean {
  if (!surveyDetail.value?.questions) return false

  for (const q of surveyDetail.value.questions) {
    if (q.isRequired === 1) {
      const answer = answers[q.questionId]
      if (answer === undefined || answer === null || answer === '' || answer === 0) {
        ElMessage.warning(`请回答第${surveyDetail.value.questions.indexOf(q) + 1}题（必填）`)
        return false
      }
    }
  }
  return true
}

async function handleSubmit() {
  if (!validateAnswers()) return

  const id = Number(route.params.id)
  submitting.value = true
  try {
    const answerList: SurveyAnswer[] = []
    surveyDetail.value?.questions?.forEach(q => {
      answerList.push({
        questionId: q.questionId,
        answerValue: String(answers[q.questionId] ?? '')
      })
    })

    await submitSurvey(id, {
      surveyId: id,
      answers: answerList
    })
    ElMessage.success('提交成功')
    router.push('/student/surveys')
  } catch (error: any) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchSurveyDetail()
})
</script>

<style scoped>
.page {
  padding: 0;
  background: var(--bg-base);
  min-height: 100vh;
}

.survey-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
}

:deep(.el-card__header) {
  background: var(--bg-surface) !important;
  border-bottom: 1px solid var(--border) !important;
}

:deep(.el-card__body) {
  background: var(--bg-surface) !important;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-text {
  font-weight: 600;
  font-size: 16px;
  font-family: 'JetBrains Mono', monospace;
  color: #00ffff;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.back-btn {
  background: transparent !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
}

.back-btn:hover {
  background: #00ffff !important;
  color: #000 !important;
}

.anonymous-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(255, 152, 0, 0.05);
  border: 1px solid rgba(255, 152, 0, 0.3);
  color: #ff9800;
  font-size: 14px;
  margin-bottom: 20px;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(255, 152, 0, 0.3);
}

.survey-info {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 0;
  font-family: 'JetBrains Mono', monospace;
}

.survey-info p {
  margin: 4px 0;
}

.cyberpunk-divider {
  border-color: var(--border-subtle) !important;
  margin: 16px 0 !important;
}

.question-block {
  padding: 20px 0;
  border-bottom: 1px solid var(--border);
}

.question-block:last-of-type {
  border-bottom: none;
}

.question-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 16px;
  font-family: 'JetBrains Mono', monospace;
}

.required-mark {
  color: #ff10f0;
  margin-right: 4px;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.5);
}

.question-num {
  color: #00ffff;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.4);
}

.question-content {
  padding-left: 20px;
}

.cyberpunk-rate :deep(.el-rate__icon) {
  color: #1a1a2e !important;
}

.cyberpunk-rate :deep(.el-rate__icon.active) {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.6);
}

:deep(.el-rate__text) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace;
}

.nps-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.nps-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
  width: 400px;
  font-family: 'JetBrains Mono', monospace;
}

.nps-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.nps-btn {
  width: 44px;
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace;
}

.nps-btn:hover {
  border-color: #00ffff !important;
  color: #00ffff !important;
}

.nps-btn.selected {
  background: #ff10f0 !important;
  border-color: #ff10f0 !important;
  color: #000 !important;
  font-weight: bold;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.5);
}

.nps-result {
  margin-top: 8px;
}

.cyberpunk-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid !important;
}

:deep(.el-tag--success) {
  border-color: #39ff14 !important;
  color: #39ff14 !important;
  box-shadow: 0 0 8px rgba(57, 255, 20, 0.3);
}

:deep(.el-tag--warning) {
  border-color: #ff9800 !important;
  color: #ff9800 !important;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.3);
}

:deep(.el-tag--danger) {
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 8px rgba(255, 16, 240, 0.3);
}

.scale-container {
  padding-right: 80px;
}

.scale-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-slider :deep(.el-slider__runway) {
  background-color: var(--bg-surface) !important;
}

.cyberpunk-slider :deep(.el-slider__bar) {
  background-color: #ff10f0 !important;
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.5);
}

.cyberpunk-slider :deep(.el-slider__button) {
  border-color: #ff10f0 !important;
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.5);
}

:deep(.el-slider__marks .el-slider__marks-text) {
  color: var(--text-secondary) !important;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-textarea :deep(.el-textarea__inner) {
  background-color: var(--bg-surface) !important;
  border-color: var(--border-subtle) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-textarea :deep(.el-textarea__inner:focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.submit-section {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
}

.cancel-btn {
  background: transparent !important;
  border: 1px solid var(--text-secondary) !important;
  color: var(--text-secondary) !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
}

.cancel-btn:hover {
  border-color: #00ffff !important;
  color: #00ffff !important;
}

.submit-btn {
  background: transparent !important;
  border: 1px solid #ff10f0 !important;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.2);
}

.submit-btn:hover {
  background: #ff10f0 !important;
  color: #000 !important;
  box-shadow: 0 0 25px rgba(255, 16, 240, 0.6);
}

:deep(.el-loading-mask) {
  background-color: rgba(3, 3, 3, 0.9) !important;
}

:deep(.el-loading-spinner .circular) {
  stroke: #00ffff !important;
}
</style>
