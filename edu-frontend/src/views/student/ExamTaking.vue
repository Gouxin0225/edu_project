<template>
  <div class="exam-taking-container">
    <div class="exam-header" :class="{ 'warning': remainingSeconds <= 300 }">
      <div class="header-left">
        <span class="exam-title">{{ examTitle }}</span>
      </div>
      <div class="header-center">
        <span class="timer" :class="{ 'blink': remainingSeconds <= 300 }">
          {{ formatTime(remainingSeconds) }}
        </span>
      </div>
      <div class="header-right">
        <span class="progress">已答 {{ answeredCount }}/{{ totalQuestions }}</span>
      </div>
    </div>

    <div class="question-nav" :class="{ 'collapsed': navCollapsed }">
      <div class="nav-toggle" @click="navCollapsed = !navCollapsed">
        <el-icon :size="20">
          <ArrowLeft v-if="!navCollapsed" />
          <ArrowRight v-else />
        </el-icon>
      </div>
      <div class="nav-content" v-show="!navCollapsed">
        <div class="nav-legend">
          <span class="legend-item"><span class="dot green"></span>已答</span>
          <span class="legend-item"><span class="dot dark"></span>未答</span>
          <span class="legend-item"><span class="dot orange"></span>已标记</span>
        </div>
        <div class="question-grid">
          <div
            v-for="(q, idx) in questions"
            :key="getQuestionRenderKey(q, idx)"
            class="question-dot"
            :class="[getDotClass(q.questionId), { active: idx === currentQuestionIndex }]"
            @click="goToQuestion(idx)"
          >
            {{ idx + 1 }}
          </div>
        </div>
      </div>
    </div>

    <div class="answer-area">
      <div
        v-if="currentQuestion"
        ref="questionCardRef"
        :key="currentQuestionRenderKey"
        :id="'question-' + currentQuestionIndex"
        class="question-card"
      >
        <div class="question-header">
          <span class="question-index">第 {{ currentQuestionIndex + 1 }} 题</span>
          <el-tag size="small" :type="getTypeTagType(currentQuestion.type)">
            {{ getTypeLabel(currentQuestion.type) }}
          </el-tag>
          <el-button
            size="small"
            :type="currentQuestionMarked ? 'warning' : 'info'"
            :disabled="currentQuestionId === null"
            @click="toggleMark(currentQuestionId)"
          >
            {{ currentQuestionMarked ? '取消标记' : '标记本题' }}
          </el-button>
        </div>
        <div class="question-content">{{ currentQuestion.content }}</div>
        <div class="question-options">
          <el-radio-group
            v-if="currentQuestion.type === 'SINGLE'"
            v-model="currentAnswer"
            @change="handleSingleChange(currentQuestionId, $event)"
          >
            <el-radio
              v-for="(opt, optIdx) in parseOptions(currentQuestion.optionsJson)"
              :key="optIdx"
              :value="String.fromCharCode(65 + optIdx)"
              class="cyberpunk-radio"
            >
              {{ String.fromCharCode(65 + optIdx) }}. {{ opt }}
            </el-radio>
          </el-radio-group>

          <el-checkbox-group
            v-else-if="currentQuestion.type === 'MULTIPLE'"
            v-model="currentMultipleAnswer"
            @change="handleMultipleChange(currentQuestionId)"
          >
            <el-checkbox
              v-for="(opt, optIdx) in parseOptions(currentQuestion.optionsJson)"
              :key="optIdx"
              :value="String.fromCharCode(65 + optIdx)"
              class="cyberpunk-checkbox"
            >
              {{ String.fromCharCode(65 + optIdx) }}. {{ opt }}
            </el-checkbox>
          </el-checkbox-group>

          <el-radio-group
            v-else-if="currentQuestion.type === 'JUDGE'"
            v-model="currentAnswer"
            @change="handleSingleChange(currentQuestionId, $event)"
          >
            <el-radio value="T" class="cyberpunk-radio">正确</el-radio>
            <el-radio value="F" class="cyberpunk-radio">错误</el-radio>
          </el-radio-group>

          <el-input
            v-else-if="currentQuestion.type === 'SHORT'"
            type="textarea"
            :rows="6"
            v-model="currentAnswer"
            placeholder="请输入答案..."
            @blur="handleAnswerChange(currentQuestionId)"
            class="cyberpunk-textarea"
          />

          <CodeEditor
            v-else-if="currentQuestion.type === 'CODE'"
            v-model="currentAnswer"
            language="python"
            :height="420"
            :font-size="16"
            class="code-input"
            @blur="handleAnswerChange(currentQuestionId)"
          />
        </div>
      </div>
    </div>

    <div class="bottom-bar">
      <el-button @click="prevQuestion" :disabled="currentQuestionIndex === 0" class="cyberpunk-btn">
        上一题
      </el-button>
      <el-button @click="nextQuestion" :disabled="currentQuestionIndex === questions.length - 1" class="cyberpunk-btn">
        下一题
      </el-button>
      <el-button type="danger" :loading="submittingExam" @click="handleSubmit" class="cyberpunk-btn submit-btn">
        提交试卷
      </el-button>
    </div>

    <el-dialog
      v-model="showRequiredSurveyDialog"
      :title="requiredSurveyRequirement?.surveyTitle || '提交前问卷'"
      width="760px"
      top="5vh"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="required-survey-dialog"
    >
      <div v-loading="requiredSurveyLoading" class="required-survey-body">
        <div class="required-survey-alert">
          提交试卷前必须完成该调查问卷，问卷提交成功后系统会自动交卷。
        </div>

        <div v-for="(question, index) in requiredSurveyDetail?.questions" :key="question.questionId" class="survey-question-block">
          <div class="survey-question-title">
            <span v-if="question.isRequired === 1" class="required-mark">*</span>
            <span>{{ index + 1 }}. {{ question.title }}</span>
          </div>

          <el-rate
            v-if="question.type === 'STAR'"
            v-model="requiredSurveyAnswers[question.questionId]"
            :max="5"
            show-text
            :texts="['非常不满意', '不满意', '一般', '满意', '非常满意']"
          />

          <div v-else-if="question.type === 'NPS'" class="survey-nps-grid">
            <el-button
              v-for="score in 11"
              :key="score - 1"
              :type="requiredSurveyAnswers[question.questionId] === score - 1 ? 'primary' : 'default'"
              @click="requiredSurveyAnswers[question.questionId] = score - 1"
            >
              {{ score - 1 }}
            </el-button>
          </div>

          <el-slider
            v-else-if="question.type === 'SCALE'"
            v-model="requiredSurveyAnswers[question.questionId]"
            :min="1"
            :max="5"
            :step="1"
            :marks="getScaleMarks(question.optionsJson)"
          />

          <el-input
            v-else-if="question.type === 'TEXT'"
            v-model="requiredSurveyAnswers[question.questionId]"
            type="textarea"
            :rows="4"
            placeholder="请输入您的回答"
          />
        </div>
      </div>

      <template #footer>
        <el-button type="primary" :loading="requiredSurveySubmitting" @click="handleRequiredSurveySubmit">
          提交问卷并交卷
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import {
  startExam, resumeExam, saveExamAnswers, submitExam, reportScreenSwitch as reportScreenSwitchApi,
  getExamSubmitRequirement, type ExamSubmitRequirement
} from '@/api/exam'
import { getSurveyDetail, submitSurvey, type SurveyAnswer, type SurveyDetail } from '@/api/survey'
import CodeEditor from '@/components/CodeEditor.vue'

const route = useRoute()
const router = useRouter()

const examId = computed(() => Number(route.params.id))
const examTitle = ref(route.query.title as string || '在线考试')

const submissionId = ref<number | null>(null)
const questions = ref<any[]>([])
const answers = reactive<Record<number, string>>({})
const multipleAnswers = reactive<Record<number, string[]>>({})
const markedQuestions = ref(new Set<number>())

const navCollapsed = ref(false)
const submittingExam = ref(false)
const showRequiredSurveyDialog = ref(false)
const requiredSurveyLoading = ref(false)
const requiredSurveySubmitting = ref(false)
const requiredSurveyRequirement = ref<ExamSubmitRequirement | null>(null)
const requiredSurveyDetail = ref<SurveyDetail | null>(null)
const requiredSurveyAnswers = reactive<Record<number, any>>({})

let autoSaveTimer: number | null = null
let countdownTimer: number | null = null
let remainingSeconds = ref(0)
let screenSwitchCount = 0
let switchScreenLimit = ref(5)
const currentQuestionIndex = ref(0)
const questionCardRef = ref<HTMLElement | null>(null)

const totalQuestions = computed(() => questions.value.length)
const currentQuestion = computed(() => questions.value[currentQuestionIndex.value] || null)
const currentQuestionId = computed(() => getQuestionId(currentQuestion.value))
const currentQuestionRenderKey = computed(() => getQuestionRenderKey(currentQuestion.value, currentQuestionIndex.value))
const currentQuestionMarked = computed(() => {
  const qid = currentQuestionId.value
  return qid !== null && markedQuestions.value.has(qid)
})
const currentAnswer = computed({
  get: () => {
    const qid = currentQuestionId.value
    return qid === null ? '' : answers[qid] || ''
  },
  set: (value: string) => {
    const qid = currentQuestionId.value
    if (qid !== null) answers[qid] = value
  }
})
const currentMultipleAnswer = computed({
  get: () => {
    const qid = currentQuestionId.value
    return qid === null ? [] : multipleAnswers[qid] || []
  },
  set: (value: string[]) => {
    const qid = currentQuestionId.value
    if (qid !== null) multipleAnswers[qid] = value
  }
})

const answeredCount = computed(() => {
  let count = 0
  for (const q of questions.value) {
    const qid = getQuestionId(q)
    if (qid === null) continue
    if (q.type === 'MULTIPLE') {
      if (multipleAnswers[qid]?.length > 0) count++
    } else {
      if (String(answers[qid] || '').trim()) count++
    }
  }
  return count
})

function normalizeQuestionId(value: any): number | null {
  if (value === null || value === undefined || value === '') return null
  const id = Number(value)
  return Number.isFinite(id) ? id : null
}

function getQuestionId(question: any): number | null {
  return normalizeQuestionId(question?.questionId)
}

function getQuestionRenderKey(question: any, index: number): number | string {
  return getQuestionId(question) ?? `index-${index}`
}

function parseOptions(optionsJson: string | null): string[] {
  if (!optionsJson) return []
  try {
    return JSON.parse(optionsJson)
  } catch {
    return []
  }
}

function formatTime(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function getDotClass(questionId: number | string | null | undefined): string {
  const qid = normalizeQuestionId(questionId)
  if (qid === null) return 'dark'
  if (markedQuestions.value.has(qid)) return 'orange'
  const q = questions.value.find(q => getQuestionId(q) === qid)
  if (!q) return 'dark'
  if (q.type === 'MULTIPLE') {
    return multipleAnswers[qid]?.length > 0 ? 'green' : 'dark'
  }
  return String(answers[qid] || '').trim() ? 'green' : 'dark'
}

function getTypeTagType(type: string): string {
  const map: Record<string, string> = {
    SINGLE: '', MULTIPLE: 'success', JUDGE: 'warning', SHORT: 'info', CODE: 'danger'
  }
  return map[type] || ''
}

function getTypeLabel(type: string): string {
  const map: Record<string, string> = {
    SINGLE: '单选题', MULTIPLE: '多选题', JUDGE: '判断题', SHORT: '简答题', CODE: '代码题'
  }
  return map[type] || type
}

function getScaleMarks(optionsJson: string): Record<number, string> {
  try {
    const labels = JSON.parse(optionsJson || '[]')
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

function toggleMark(questionId: number | null) {
  if (questionId === null) return
  if (markedQuestions.value.has(questionId)) {
    markedQuestions.value.delete(questionId)
  } else {
    markedQuestions.value.add(questionId)
  }
  markedQuestions.value = new Set(markedQuestions.value)
}

function handleSingleChange(questionId: number | null, value: string) {
  if (questionId === null) return
  answers[questionId] = value
}

function handleMultipleChange(questionId: number | null) {
  if (questionId === null) return
}

function handleAnswerChange(questionId: number | null) {
  if (questionId === null) return
  void saveAnswers()
}

function goToQuestion(index: number) {
  if (index < 0 || index >= questions.value.length) return
  currentQuestionIndex.value = index
  scrollQuestionIntoView()
}

function scrollQuestionIntoView() {
  nextTick(() => {
    const scrollContainer = questionCardRef.value?.closest('.layout-main') as HTMLElement | null
    if (scrollContainer) {
      scrollContainer.scrollTo({ top: 0, behavior: 'smooth' })
      return
    }
    questionCardRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function prevQuestion() {
  if (currentQuestionIndex.value > 0) {
    goToQuestion(currentQuestionIndex.value - 1)
  }
}

function nextQuestion() {
  if (currentQuestionIndex.value < questions.value.length - 1) {
    goToQuestion(currentQuestionIndex.value + 1)
  }
}

async function fetchStartExam() {
  const res = await startExam(examId.value)
  submissionId.value = res.data.submissionId
  questions.value = res.data.questions || []
  remainingSeconds.value = res.data.remainingSeconds ?? (res.data.duration || 90) * 60
  switchScreenLimit.value = res.data.switchScreenLimit || 5
  restoreAnswersFromQuestions()
}

function parseMultipleAnswer(answerValue?: string | null): string[] {
  if (!answerValue) return []
  const normalized = answerValue.replace(/\s+/g, '').toUpperCase()
  if (!normalized) return []
  if (/^[A-Z]+$/.test(normalized) && normalized.length > 1) {
    return normalized.split('')
  }
  return normalized.split(/[,，/|;]+/).filter(Boolean)
}

function restoreAnswersFromQuestions() {
  for (const q of questions.value) {
    const qid = getQuestionId(q)
    if (qid === null) continue
    if (q.type === 'MULTIPLE') {
      multipleAnswers[qid] = parseMultipleAnswer(q.studentAnswer)
    } else {
      answers[qid] = q.studentAnswer || ''
    }
  }
}

async function initExam() {
  document.addEventListener('visibilitychange', handleVisibilityChange)

  try {
    const resumeData = await resumeExam(examId.value)
    if (resumeData?.data?.submissionId) {
      submissionId.value = resumeData.data.submissionId
      questions.value = resumeData.data.questions || []
      remainingSeconds.value = resumeData.data.remainingSeconds ?? (resumeData.data.duration || 90) * 60
      switchScreenLimit.value = resumeData.data.switchScreenLimit || 5
      if (resumeData.data.status === 'SUBMITTED' || resumeData.data.status === 'GRADED') {
        ElMessage.info('本次考试已提交')
        router.push('/student/exams')
        return
      }
      restoreAnswersFromQuestions()
      ElMessage.success('已恢复答题进度')
    } else {
      await fetchStartExam()
      ElMessage.success('考试已开始')
    }
  } catch {
    await fetchStartExam()
    ElMessage.success('考试已开始')
  }

  startCountdown()
  startAutoSave()
}

async function saveAnswers() {
  if (!submissionId.value) return

  const answersList: { questionId: number; answerValue: string }[] = []

  for (const q of questions.value) {
    const qid = getQuestionId(q)
    if (qid === null) continue
    let val = ''
    if (q.type === 'MULTIPLE') {
      const arr = multipleAnswers[qid]
      val = arr ? arr.join(',') : ''
    } else {
      val = answers[qid] || ''
    }
    if (String(val || '').trim()) {
      answersList.push({ questionId: qid, answerValue: val })
    }
  }

  if (answersList.length === 0) return

  try {
    await saveExamAnswers(examId.value, answersList)
  } catch (error) {
    console.error('自动保存失败', error)
  }
}

function resetRequiredSurveyAnswers() {
  Object.keys(requiredSurveyAnswers).forEach(key => {
    delete requiredSurveyAnswers[Number(key)]
  })
}

async function loadRequiredSurvey(requirement: ExamSubmitRequirement) {
  if (!requirement.surveyId) {
    throw new Error('考试未配置必填问卷')
  }
  requiredSurveyRequirement.value = requirement
  requiredSurveyLoading.value = true
  showRequiredSurveyDialog.value = true
  try {
    const res = await getSurveyDetail(requirement.surveyId)
    requiredSurveyDetail.value = res.data
    resetRequiredSurveyAnswers()
    requiredSurveyDetail.value?.questions?.forEach(question => {
      if (question.type === 'STAR') requiredSurveyAnswers[question.questionId] = 0
      else if (question.type === 'NPS') requiredSurveyAnswers[question.questionId] = undefined
      else if (question.type === 'SCALE') requiredSurveyAnswers[question.questionId] = 3
      else requiredSurveyAnswers[question.questionId] = ''
    })
  } finally {
    requiredSurveyLoading.value = false
  }
}

function validateRequiredSurveyAnswers() {
  if (!requiredSurveyDetail.value?.questions) return false
  for (const question of requiredSurveyDetail.value.questions) {
    if (question.isRequired !== 1) continue
    const answer = requiredSurveyAnswers[question.questionId]
    if (answer === undefined || answer === null || answer === '' || answer === 0) {
      const index = requiredSurveyDetail.value.questions.indexOf(question) + 1
      ElMessage.warning(`请回答问卷第${index}题`)
      return false
    }
  }
  return true
}

async function finishExamSubmit() {
  await submitExam(examId.value)
  ElMessage.success('交卷成功')
  router.push('/student/exams')
}

function startCountdown() {
  if (countdownTimer) clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    if (remainingSeconds.value > 0) {
      remainingSeconds.value--
    } else {
      ElMessage.warning('考试时间已到，正在自动交卷...')
      doSubmit()
    }
  }, 1000)
}

function startAutoSave() {
  if (autoSaveTimer) clearInterval(autoSaveTimer)
  autoSaveTimer = window.setInterval(() => {
    saveAnswers()
  }, 30000)
}

function handleVisibilityChange() {
  if (document.hidden) {
    handleScreenSwitchReport()
  }
}

async function handleScreenSwitchReport() {
  if (!submissionId.value) return
  try {
    const res = await reportScreenSwitchApi(examId.value)
    screenSwitchCount = res.data || 0
    if (screenSwitchCount >= switchScreenLimit.value) {
      ElMessage.error('切屏次数过多，已自动交卷')
      doSubmit()
    }
  } catch (error) {
    console.error('切屏上报失败', error)
  }
}

async function doSubmit() {
  if (submittingExam.value) return
  submittingExam.value = true
  clearInterval(autoSaveTimer!)
  clearInterval(countdownTimer!)
  document.removeEventListener('visibilitychange', handleVisibilityChange)

  try {
    await saveAnswers()
    const requirementRes = await getExamSubmitRequirement(examId.value)
    const requirement = requirementRes.data
    if (requirement.requireSurvey && !requirement.surveySubmitted) {
      if (requirement.surveyExpired) {
        throw new Error(requirement.message || '绑定的调查问卷已截止且未填写，无法提交试卷')
      }
      await loadRequiredSurvey(requirement)
      return
    }
    if (!requirement.requireSurvey && requirement.message) {
      ElMessage.warning(requirement.message)
    }
    await finishExamSubmit()
  } catch (error: any) {
    ElMessage.error(error.message || '交卷失败')
    startCountdown()
    startAutoSave()
    document.addEventListener('visibilitychange', handleVisibilityChange)
    submittingExam.value = false
  }
}

async function handleRequiredSurveySubmit() {
  if (!requiredSurveyRequirement.value?.surveyId || !validateRequiredSurveyAnswers()) return

  requiredSurveySubmitting.value = true
  try {
    const answerList: SurveyAnswer[] = []
    requiredSurveyDetail.value?.questions?.forEach(question => {
      answerList.push({
        questionId: question.questionId,
        answerValue: String(requiredSurveyAnswers[question.questionId] ?? '')
      })
    })
    await submitSurvey(requiredSurveyRequirement.value.surveyId, {
      surveyId: requiredSurveyRequirement.value.surveyId,
      answers: answerList
    })
    showRequiredSurveyDialog.value = false
    ElMessage.success('问卷提交成功，正在交卷')
    await finishExamSubmit()
  } catch (error: any) {
    ElMessage.error(error.message || '问卷提交失败')
    if (!showRequiredSurveyDialog.value) {
      startCountdown()
      startAutoSave()
      document.addEventListener('visibilitychange', handleVisibilityChange)
    }
  } finally {
    requiredSurveySubmitting.value = false
    if (!showRequiredSurveyDialog.value) {
      submittingExam.value = false
    }
  }
}

async function handleSubmit() {
  try {
    await ElMessageBox.confirm('确定要提交试卷吗？提交后将无法修改。', '交卷确认', {
      confirmButtonText: '确定交卷',
      cancelButtonText: '继续答题',
      type: 'warning'
    })
    await doSubmit()
  } catch {
  }
}

onMounted(() => {
  initExam()
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  if (autoSaveTimer) clearInterval(autoSaveTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})
</script>

<style scoped>
.exam-taking-container {
  min-height: 100vh;
  background: var(--bg-base);
  padding-top: 60px;
  padding-bottom: 70px;
}

.exam-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: var(--bg-surface);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 1000;
  box-shadow: 0 0 20px rgba(255, 16, 240, 0.15);
}

.exam-header.warning {
  background: #1a0a0a;
  border-bottom-color: #ff10f0;
  box-shadow: 0 0 20px rgba(255, 16, 240, 0.3);
}

.header-left,
.header-right {
  flex: 1;
}

.header-center {
  flex: 1;
  text-align: center;
}

.header-right {
  text-align: right;
}

.exam-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.timer {
  font-size: 28px;
  font-weight: 700;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  color: #00ffff;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.8), 0 0 20px rgba(0, 255, 255, 0.5);
}

.timer.blink {
  color: #ff10f0;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.8), 0 0 20px rgba(255, 16, 240, 0.5);
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.progress {
  font-size: 16px;
  color: var(--text-secondary);
  font-family: 'JetBrains Mono', monospace;
}

.question-nav {
  position: fixed;
  top: 70px;
  left: 0;
  width: 200px;
  background: var(--bg-surface);
  border-right: 1px solid var(--border);
  box-shadow: 2px 0 20px rgba(0, 255, 255, 0.1);
  z-index: 999;
  transition: width 0.3s;
}

.question-nav.collapsed {
  width: 40px;
}

.nav-toggle {
  position: absolute;
  top: 50%;
  right: -16px;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  background: var(--bg-surface);
  border: 1px solid #00ffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #00ffff;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

.nav-toggle:hover {
  color: #ff10f0;
  border-color: #ff10f0;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.5);
}

.nav-content {
  padding: 16px;
}

.nav-legend {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
  font-size: 12px;
  color: var(--text-secondary);
  font-family: 'JetBrains Mono', monospace;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dot {
  width: 12px;
  height: 12px;
}

.dot.green {
  background: #39ff14;
  box-shadow: 0 0 8px rgba(57, 255, 20, 0.6);
}

.dot.dark {
  background: var(--bg-surface);
  border: 1px solid var(--border);
}

.dot.orange {
  background: #ff9800;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.6);
}

.question-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.question-dot {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: 'JetBrains Mono', monospace;
  border: 1px solid var(--border);
  color: var(--text-secondary);
  background: var(--bg-surface);
}

.question-dot.green {
  background: #39ff14;
  color: #000;
  border-color: #39ff14;
  box-shadow: 0 0 8px rgba(57, 255, 20, 0.5);
}

.question-dot.dark {
  background: var(--bg-surface);
  color: var(--text-secondary);
  border-color: var(--border);
}

.question-dot.orange {
  background: #ff9800;
  color: #000;
  border-color: #ff9800;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.5);
}

.question-dot.active {
  border-color: #ff10f0;
  color: #000;
  box-shadow: 0 0 14px rgba(255, 16, 240, 0.7);
  transform: scale(1.08);
}

.question-dot:hover {
  transform: scale(1.1);
  border-color: #00ffff;
  color: #00ffff;
}

.answer-area {
  margin-left: 220px;
  margin-right: 20px;
  padding: 20px;
}

.question-card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5), inset 0 0 30px rgba(0, 255, 255, 0.02);
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
}

.question-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.question-index {
  font-size: 16px;
  font-weight: 600;
  color: #00ffff;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.question-content {
  font-size: 16px;
  line-height: 1.8;
  color: var(--text-primary);
  margin-bottom: 20px;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(255, 255, 255, 0.1);
}

.question-options {
  padding-left: 8px;
}

.question-options :deep(.el-radio),
.question-options :deep(.el-checkbox) {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  font-size: 15px;
  line-height: 1.6;
  font-family: 'JetBrains Mono', monospace;
}

.question-options :deep(.el-radio__label),
.question-options :deep(.el-checkbox__label) {
  word-break: break-all;
  line-height: 1.6;
  color: var(--text-primary);
}

:deep(.cyberpunk-radio .el-radio__input .el-radio__inner),
:deep(.cyberpunk-checkbox .el-checkbox__input .el-checkbox__inner) {
  background-color: var(--bg-surface) !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3);
}

:deep(.cyberpunk-radio .el-radio__input.is-checked .el-radio__inner),
:deep(.cyberpunk-checkbox .el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #ff10f0 !important;
  border-color: #ff10f0 !important;
  box-shadow: 0 0 12px rgba(255, 16, 240, 0.6);
}

:deep(.cyberpunk-radio .el-radio__input.is-checked + .el-radio__label),
:deep(.cyberpunk-checkbox .el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #ff10f0;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.5);
}

:deep(.cyberpunk-radio .el-radio__input.is-checked .el-radio__inner::after),
:deep(.cyberpunk-checkbox .el-checkbox__input.is-checked .el-checkbox__inner::after) {
  background-color: var(--bg-base);
}

:deep(.cyberpunk-radio:hover .el-radio__label),
:deep(.cyberpunk-checkbox:hover .el-checkbox__label) {
  color: #00ffff;
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.5);
}

:deep(.cyberpunk-radio:hover .el-radio__inner),
:deep(.cyberpunk-checkbox:hover .el-checkbox__inner) {
  border-color: #00ffff !important;
  box-shadow: 0 0 12px rgba(0, 255, 255, 0.5);
}

:deep(.el-radio__inner),
:deep(.el-checkbox__inner) {
  background-color: var(--bg-surface) !important;
  border-color: #00ffff !important;
}

:deep(.el-radio__input.is-checked .el-radio__inner),
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #ff10f0 !important;
  border-color: #ff10f0 !important;
}

:deep(.el-radio__label),
:deep(.el-checkbox__label) {
  color: var(--text-primary) !important;
}

:deep(.el-radio__input.is-checked + .el-radio__label),
:deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #ff10f0 !important;
}

:deep(.el-textarea__inner) {
  background-color: var(--bg-surface) !important;
  border-color: var(--border-subtle) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace;
}

:deep(.el-textarea__inner:focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.code-input :deep(.el-textarea__inner) {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 220px;
  right: 0;
  padding: 12px 24px;
  background: var(--bg-surface);
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: center;
  gap: 16px;
  z-index: 999;
  box-shadow: 0 0 20px rgba(255, 16, 240, 0.1);
}

.cyberpunk-btn {
  min-width: 100px;
  background: var(--bg-surface) !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px);
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.2);
  transition: all 0.3s ease;
}

.cyberpunk-btn:hover {
  background: #00ffff !important;
  color: #000 !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.5);
}

.cyberpunk-btn:disabled {
  opacity: 0.4;
  border-color: var(--border) !important;
  color: var(--text-secondary) !important;
  box-shadow: none;
}

.submit-btn {
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.2);
}

.submit-btn:hover {
  background: #ff10f0 !important;
  color: #000 !important;
  box-shadow: 0 0 25px rgba(255, 16, 240, 0.6);
}

.required-survey-dialog :deep(.el-dialog) {
  background: var(--bg-surface) !important;
  border: 1px solid #00ffff !important;
}

.required-survey-dialog :deep(.el-dialog__header),
.required-survey-dialog :deep(.el-dialog__footer) {
  border-color: var(--border-subtle) !important;
}

.required-survey-dialog :deep(.el-dialog__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
}

.required-survey-body {
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 8px;
}

.required-survey-alert {
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 152, 0, 0.35);
  background: rgba(255, 152, 0, 0.08);
  color: #ff9800;
  font-size: 14px;
}

.survey-question-block {
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
}

.survey-question-title {
  margin-bottom: 12px;
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.6;
}

.required-mark {
  color: #ff10f0;
  margin-right: 4px;
}

.survey-nps-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.el-button--warning) {
  background: #ff9800 !important;
  border-color: #ff9800 !important;
  color: #000 !important;
}

:deep(.el-button--info) {
  background: var(--bg-surface) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
}

:deep(.el-tag) {
  font-family: 'JetBrains Mono', monospace;
}

:deep(.el-tag--success) {
  background: rgba(57, 255, 20, 0.1) !important;
  border-color: #39ff14 !important;
  color: #39ff14 !important;
}

:deep(.el-tag--warning) {
  background: rgba(255, 152, 0, 0.1) !important;
  border-color: #ff9800 !important;
  color: #ff9800 !important;
}

:deep(.el-tag--danger) {
  background: rgba(255, 16, 240, 0.1) !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
}

:deep(.el-tag--info) {
  background: rgba(0, 255, 255, 0.1) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
}

:deep(.el-tag) {
  background: rgba(0, 255, 255, 0.05) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
}
</style>
