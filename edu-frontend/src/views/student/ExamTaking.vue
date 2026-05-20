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
            :key="q.questionId"
            class="question-dot"
            :class="getDotClass(q.questionId)"
            @click="goToQuestion(idx)"
          >
            {{ idx + 1 }}
          </div>
        </div>
      </div>
    </div>

    <div class="answer-area" ref="answerAreaRef">
      <div
        v-for="(question, idx) in questions"
        :key="question.questionId"
        :id="'question-' + idx"
        class="question-card"
      >
        <div class="question-header">
          <span class="question-index">第 {{ idx + 1 }} 题</span>
          <el-tag size="small" :type="getTypeTagType(question.type)">
            {{ getTypeLabel(question.type) }}
          </el-tag>
          <el-button
            size="small"
            :type="markedQuestions.has(question.questionId) ? 'warning' : 'info'"
            @click="toggleMark(question.questionId)"
          >
            {{ markedQuestions.has(question.questionId) ? '取消标记' : '标记本题' }}
          </el-button>
        </div>
        <div class="question-content">{{ question.content }}</div>
        <div class="question-options">
          <el-radio-group
            v-if="question.type === 'SINGLE'"
            v-model="answers[question.questionId]"
            @change="handleSingleChange(question.questionId, $event)"
          >
            <el-radio
              v-for="(opt, optIdx) in parseOptions(question.optionsJson)"
              :key="optIdx"
              :value="String.fromCharCode(65 + optIdx)"
              class="cyberpunk-radio"
            >
              {{ String.fromCharCode(65 + optIdx) }}. {{ opt }}
            </el-radio>
          </el-radio-group>

          <el-checkbox-group
            v-else-if="question.type === 'MULTIPLE'"
            v-model="multipleAnswers[question.questionId]"
            @change="handleMultipleChange(question.questionId)"
          >
            <el-checkbox
              v-for="(opt, optIdx) in parseOptions(question.optionsJson)"
              :key="optIdx"
              :value="String.fromCharCode(65 + optIdx)"
              class="cyberpunk-checkbox"
            >
              {{ String.fromCharCode(65 + optIdx) }}. {{ opt }}
            </el-checkbox>
          </el-checkbox-group>

          <el-radio-group
            v-else-if="question.type === 'JUDGE'"
            v-model="answers[question.questionId]"
            @change="handleSingleChange(question.questionId, $event)"
          >
            <el-radio value="T" class="cyberpunk-radio">正确</el-radio>
            <el-radio value="F" class="cyberpunk-radio">错误</el-radio>
          </el-radio-group>

          <el-input
            v-else-if="question.type === 'SHORT'"
            type="textarea"
            :rows="6"
            v-model="answers[question.questionId]"
            placeholder="请输入答案..."
            @blur="handleAnswerChange(question.questionId)"
            class="cyberpunk-textarea"
          />

          <el-input
            v-else-if="question.type === 'CODE'"
            type="textarea"
            :rows="10"
            v-model="answers[question.questionId]"
            placeholder="请输入代码..."
            class="code-input cyberpunk-textarea"
            @blur="handleAnswerChange(question.questionId)"
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
      <el-button type="danger" @click="handleSubmit" class="cyberpunk-btn submit-btn">
        提交试卷
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import { startExam, resumeExam, saveExamAnswers, submitExam, reportScreenSwitch } from '@/api/exam'

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
const currentQuestionIndex = ref(0)

let autoSaveTimer: number | null = null
let countdownTimer: number | null = null
let remainingSeconds = ref(0)
let screenSwitchCount = 0
let switchScreenLimit = ref(5)

const totalQuestions = computed(() => questions.value.length)

const answeredCount = computed(() => {
  let count = 0
  for (const q of questions.value) {
    const qid = q.questionId
    if (q.type === 'MULTIPLE') {
      if (multipleAnswers[qid]?.length > 0) count++
    } else {
      if (answers[qid]?.trim()) count++
    }
  }
  return count
})

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

function getDotClass(questionId: number): string {
  if (markedQuestions.value.has(questionId)) return 'orange'
  const q = questions.value.find(q => q.questionId === questionId)
  if (!q) return 'dark'
  if (q.type === 'MULTIPLE') {
    return multipleAnswers[questionId]?.length > 0 ? 'green' : 'dark'
  }
  return answers[questionId]?.trim() ? 'green' : 'dark'
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

function toggleMark(questionId: number) {
  if (markedQuestions.value.has(questionId)) {
    markedQuestions.value.delete(questionId)
  } else {
    markedQuestions.value.add(questionId)
  }
  markedQuestions.value = new Set(markedQuestions.value)
}

function handleSingleChange(questionId: number, value: string) {
  answers[questionId] = value
}

function handleMultipleChange(questionId: number) {}

function handleAnswerChange(questionId: number) {
  void saveAnswers()
}

function goToQuestion(index: number) {
  currentQuestionIndex.value = index
  const element = document.getElementById('question-' + index)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
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
    const qid = q.questionId
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
    const qid = q.questionId
    let val = ''
    if (q.type === 'MULTIPLE') {
      const arr = multipleAnswers[qid]
      val = arr ? arr.join(',') : ''
    } else {
      val = answers[qid] || ''
    }
    if (val.trim()) {
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
    reportScreenSwitch()
  }
}

async function reportScreenSwitch() {
  if (!submissionId.value) return
  try {
    const res = await reportScreenSwitch(examId.value)
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
  clearInterval(autoSaveTimer!)
  clearInterval(countdownTimer!)
  document.removeEventListener('visibilitychange', handleVisibilityChange)

  try {
    await saveAnswers()
    await submitExam(examId.value)
    ElMessage.success('交卷成功')
    router.push('/student/exams')
  } catch (error: any) {
    ElMessage.error(error.message || '交卷失败')
    startCountdown()
    startAutoSave()
    document.addEventListener('visibilitychange', handleVisibilityChange)
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
  background: var(--bg-base, #030303);
  padding-top: 60px;
  padding-bottom: 70px;
}

.exam-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: #0a0a0a;
  border-bottom: 1px solid #1a1a2e;
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
  color: #e0e0e0;
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
  color: #909090;
  font-family: 'JetBrains Mono', monospace;
}

.question-nav {
  position: fixed;
  top: 70px;
  left: 0;
  width: 200px;
  background: #0a0a0a;
  border-right: 1px solid #1a1a2e;
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
  background: #0a0a0a;
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
  color: #909090;
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
  background: #1a1a2e;
  border: 1px solid #333;
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
  border: 1px solid #333;
  color: #909090;
  background: #0a0a0a;
}

.question-dot.green {
  background: #39ff14;
  color: #000;
  border-color: #39ff14;
  box-shadow: 0 0 8px rgba(57, 255, 20, 0.5);
}

.question-dot.dark {
  background: #1a1a2e;
  color: #909090;
  border-color: #333;
}

.question-dot.orange {
  background: #ff9800;
  color: #000;
  border-color: #ff9800;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.5);
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
  background: #0a0a0a;
  border: 1px solid #1a1a2e;
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
  color: #e0e0e0;
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
  color: #e0e0e0;
}

:deep(.cyberpunk-radio .el-radio__input .el-radio__inner),
:deep(.cyberpunk-checkbox .el-checkbox__input .el-checkbox__inner) {
  background-color: #1a1a2e !important;
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
  background-color: #000;
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
  background-color: #1a1a2e !important;
  border-color: #00ffff !important;
}

:deep(.el-radio__input.is-checked .el-radio__inner),
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #ff10f0 !important;
  border-color: #ff10f0 !important;
}

:deep(.el-radio__label),
:deep(.el-checkbox__label) {
  color: #e0e0e0 !important;
}

:deep(.el-radio__input.is-checked + .el-radio__label),
:deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #ff10f0 !important;
}

:deep(.el-textarea__inner) {
  background-color: #0a0a0a !important;
  border-color: #1a1a2e !important;
  color: #e0e0e0 !important;
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
  background: #0a0a0a;
  border-top: 1px solid #1a1a2e;
  display: flex;
  justify-content: center;
  gap: 16px;
  z-index: 999;
  box-shadow: 0 0 20px rgba(255, 16, 240, 0.1);
}

.cyberpunk-btn {
  min-width: 100px;
  background: #0a0a0a !important;
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
  border-color: #333 !important;
  color: #666 !important;
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

:deep(.el-button--warning) {
  background: #ff9800 !important;
  border-color: #ff9800 !important;
  color: #000 !important;
}

:deep(.el-button--info) {
  background: #1a1a2e !important;
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
