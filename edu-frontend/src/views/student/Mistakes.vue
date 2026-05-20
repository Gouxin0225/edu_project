<template>
  <div class="mistakes-page">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="filter-group">
          <el-input
            v-model="filter.knowledgePoint"
            placeholder="搜索知识点"
            clearable
            style="width:180px"
            @keyup.enter="fetchMistakes"
            @clear="fetchMistakes"
            class="cyberpunk-input"
          />
          <el-select
            v-model="filter.questionType"
            placeholder="全部题型"
            clearable
            style="width:130px"
            @change="fetchMistakes"
            class="cyberpunk-select"
          >
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTIPLE" />
            <el-option label="判断题" value="JUDGE" />
            <el-option label="简答题" value="SHORT" />
            <el-option label="编程题" value="CODE" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="fetchMistakes" class="cyberpunk-btn">搜索</el-button>
        </div>
        <div class="action-group">
          <el-tag type="info" size="large" class="cyberpunk-tag count-tag">
            共 {{ mistakeList.length }} 道错题
          </el-tag>
          <el-button type="warning" :icon="Trophy" @click="openChallengeDialog" :disabled="mistakeList.length === 0" class="cyberpunk-btn warning-btn">
            错题挑战
          </el-button>
        </div>
      </div>
    </el-card>

    <div v-loading="loading" class="mistake-list">
      <el-empty v-if="!loading && mistakeList.length === 0" description="暂无错题记录" :image-size="100" class="cyberpunk-empty" />

      <el-card
        v-for="item in mistakeList"
        :key="item.mistakeId"
        shadow="never"
        class="mistake-card"
      >
        <div class="mistake-header" @click="toggleExpand(item.mistakeId)">
          <div class="mistake-tags">
            <el-tag size="small" class="cyberpunk-tag">{{ item.knowledgePoint }}</el-tag>
            <el-tag size="small" :type="typeTagType(item.questionType)" class="cyberpunk-tag">{{ typeLabel(item.questionType) }}</el-tag>
            <el-tag size="small" :type="diffTagType(item.difficulty)" class="cyberpunk-tag">{{ diffLabel(item.difficulty) }}</el-tag>
          </div>
          <div class="mistake-meta">
            <span class="wrong-count">
              <el-icon><WarningFilled /></el-icon>
              答错 {{ item.wrongCount }} 次
            </span>
            <span class="last-time">{{ formatTime(item.lastWrongTime) }}</span>
            <el-button
              type="success"
              size="small"
              :icon="Check"
              @click.stop="handleMaster(item)"
              class="master-btn"
            >已攻克</el-button>
            <el-icon class="expand-icon" :class="{ rotated: expandedIds.has(item.mistakeId) }">
              <ArrowDown />
            </el-icon>
          </div>
        </div>

        <div v-if="expandedIds.has(item.mistakeId)" class="mistake-detail">
          <div class="question-content">
            <div class="section-label">题目</div>
            <div class="content-text">{{ item.content }}</div>
          </div>

          <div v-if="item.optionsJson" class="question-options">
            <div class="section-label">选项</div>
            <div
              v-for="(opt, idx) in parseOptions(item.optionsJson)"
              :key="idx"
              class="option-item"
              :class="{ 'correct-option': isCorrectOption(opt.key, item.standardAnswer) }"
            >
              <span class="option-key">{{ opt.key }}.</span>
              <span class="option-val">{{ opt.value }}</span>
              <el-icon v-if="isCorrectOption(opt.key, item.standardAnswer)" class="correct-icon"><Check /></el-icon>
            </div>
          </div>

          <div class="question-answer">
            <div class="section-label">正确答案</div>
            <el-tag type="success" size="large" class="cyberpunk-tag">{{ item.standardAnswer }}</el-tag>
          </div>

          <div v-if="item.analysis" class="question-analysis">
            <div class="section-label">解析</div>
            <div class="analysis-text">{{ item.analysis }}</div>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="challengeDialog" title="错题挑战" width="400px" :close-on-click-modal="false" class="cyberpunk-dialog">
      <div class="challenge-setup">
        <p class="challenge-tip">从错题本中随机抽取题目进行练习，练习完后查看解析，不计入成绩。</p>
        <el-form label-width="90px">
          <el-form-item label="挑战题数" class="cyberpunk-form-item">
            <el-input-number
              v-model="challengeCount"
              :min="1"
              :max="Math.min(20, mistakeList.length)"
              :step="1"
              style="width:140px"
              class="cyberpunk-input-number"
            />
            <span class="max-hint">最多 {{ Math.min(20, mistakeList.length) }} 题</span>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="challengeDialog = false" class="cancel-btn">取消</el-button>
        <el-button type="primary" :loading="challengeLoading" @click="startChallenge" class="cyberpunk-btn">开始挑战</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="challengeActiveDialog"
      title="错题挑战"
      width="700px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="cyberpunk-dialog challenge-dialog"
    >
      <div v-if="challengeData" class="challenge-body">
        <div class="challenge-progress">
          <span class="progress-text">第 {{ challengeCurrentIdx + 1 }} / {{ challengeData.questions.length }} 题</span>
          <el-progress
            :percentage="Math.round(((challengeCurrentIdx + 1) / challengeData.questions.length) * 100)"
            :stroke-width="8"
            class="cyberpunk-progress"
            style="flex:1;margin:0 12px"
          />
        </div>

        <div v-if="currentChallengeQuestion" class="challenge-question">
          <div class="challenge-meta">
            <el-tag size="small" :type="typeTagType(currentChallengeQuestion.type)" class="cyberpunk-tag">{{ typeLabel(currentChallengeQuestion.type) }}</el-tag>
            <el-tag size="small" :type="diffTagType(currentChallengeQuestion.difficulty)" class="cyberpunk-tag">{{ diffLabel(currentChallengeQuestion.difficulty) }}</el-tag>
            <el-tag size="small" class="cyberpunk-tag">{{ currentChallengeQuestion.knowledgePoint }}</el-tag>
          </div>

          <div class="challenge-content">{{ currentChallengeQuestion.content }}</div>

          <div v-if="currentChallengeQuestion.optionsJson" class="challenge-options">
            <div
              v-for="opt in parseOptions(currentChallengeQuestion.optionsJson)"
              :key="opt.key"
              class="challenge-option"
              :class="getChallengeOptionClass(opt.key)"
              @click="selectChallengeAnswer(opt.key)"
            >
              <span class="option-key">{{ opt.key }}</span>
              <span class="option-val">{{ opt.value }}</span>
            </div>
          </div>

          <div v-else class="challenge-text-answer">
            <el-input
              v-model="challengeAnswer"
              type="textarea"
              :rows="4"
              placeholder="请输入答案..."
              :disabled="challengeShowAnalysis"
              class="cyberpunk-textarea"
            />
          </div>

          <div class="challenge-actions">
            <el-button
              v-if="!challengeShowAnalysis"
              type="primary"
              :disabled="!challengeAnswer"
              @click="submitChallengeAnswer"
              class="cyberpunk-btn"
            >提交答案</el-button>
            <el-button
              v-if="challengeShowAnalysis && challengeCurrentIdx < challengeData.questions.length - 1"
              type="primary"
              @click="nextChallengeQuestion"
              class="cyberpunk-btn"
            >下一题</el-button>
            <el-button
              v-if="challengeShowAnalysis && challengeCurrentIdx === challengeData.questions.length - 1"
              type="success"
              @click="finishChallenge"
              class="success-btn"
            >完成挑战</el-button>
          </div>

          <div v-if="challengeShowAnalysis && currentChallengeQuestion?.type !== 'SHORT' && currentChallengeQuestion?.type !== 'CODE'" class="answer-result">
            <el-alert
              :type="isLastAnswerCorrect ? 'success' : 'error'"
              :title="isLastAnswerCorrect ? '回答正确！' : `回答错误，正确答案：${currentChallengeQuestion?.standardAnswer}`"
              show-icon
              :closable="false"
              style="margin-bottom:12px"
              class="cyberpunk-alert"
            />
          </div>

          <div v-if="challengeShowAnalysis" class="challenge-analysis">
            <div class="analysis-header">
              <el-icon><InfoFilled /></el-icon>
              题目解析
            </div>
            <div class="analysis-text">{{ currentChallengeQuestion.analysis || '暂无解析' }}</div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="finishChallenge" class="cancel-btn">退出挑战</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Trophy, Check, ArrowDown, WarningFilled, InfoFilled } from '@element-plus/icons-vue'
import { getMistakeList, markMistakeMastered, startChallenge as apiStartChallenge } from '@/api/student'
import type { MistakeVO, ChallengeVO, ChallengeQuestionVO } from '@/api/student'

const filter = reactive({ knowledgePoint: '', questionType: '' })
const mistakeList = ref<MistakeVO[]>([])
const loading = ref(false)
const expandedIds = ref<Set<number>>(new Set())

async function fetchMistakes() {
  loading.value = true
  try {
    const res = await getMistakeList({
      knowledgePoint: filter.knowledgePoint || undefined,
      questionType: filter.questionType || undefined
    })
    mistakeList.value = res.data || []
    expandedIds.value = new Set()
  } catch {
    ElMessage.error('加载错题失败')
  } finally {
    loading.value = false
  }
}

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

async function handleMaster(item: MistakeVO) {
  try {
    await ElMessageBox.confirm(`确认将「${item.knowledgePoint}」标记为已攻克？`, '提示', { type: 'warning' })
    await markMistakeMastered(item.questionId)
    ElMessage.success('已标记为攻克，从错题本移除')
    mistakeList.value = mistakeList.value.filter(m => m.mistakeId !== item.mistakeId)
  } catch {
  }
}

function parseOptions(optionsJson: string | null): { key: string; value: string }[] {
  if (!optionsJson) return []
  try {
    const parsed = JSON.parse(optionsJson)
    if (Array.isArray(parsed)) {
      return parsed.map((item: any) => {
        if (typeof item === 'object' && item.key) return item
        return { key: String(item), value: String(item) }
      })
    }
    return Object.entries(parsed).map(([k, v]) => ({ key: k, value: String(v) }))
  } catch {
    return []
  }
}

function isCorrectOption(key: string, answer: string) {
  return answer?.includes(key) ?? false
}

const typeLabel = (v: string) => ({ SINGLE:'单选题', MULTIPLE:'多选题', JUDGE:'判断题', SHORT:'简答题', CODE:'编程题' }[v] ?? v)
const diffLabel = (v: string) => ({ EASY:'简单', MEDIUM:'中等', HARD:'困难' }[v] ?? v)
const typeTagType = (v: string): any => ({ SINGLE:'', MULTIPLE:'success', JUDGE:'warning', SHORT:'info', CODE:'danger' }[v] ?? '')
const diffTagType = (v: string): any => ({ EASY:'success', MEDIUM:'warning', HARD:'danger' }[v] ?? '')

function formatTime(t: string) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

const challengeDialog = ref(false)
const challengeCount = ref(5)
const challengeLoading = ref(false)
const challengeData = ref<ChallengeVO | null>(null)
const challengeActiveDialog = ref(false)
const challengeCurrentIdx = ref(0)
const challengeAnswer = ref('')
const challengeShowAnalysis = ref(false)
const challengeMultiAnswer = ref<string[]>([])

const currentChallengeQuestion = computed<ChallengeQuestionVO | null>(() => {
  if (!challengeData.value) return null
  return challengeData.value.questions[challengeCurrentIdx.value] ?? null
})

const isLastAnswerCorrect = computed<boolean>(() => {
  const q = currentChallengeQuestion.value
  if (!q || !challengeShowAnalysis.value) return false
  const standard = (q.standardAnswer || '').trim().toUpperCase()
  const userAns = challengeAnswer.value.trim().toUpperCase()
  if (q.type === 'JUDGE' || q.type === 'SINGLE') return userAns === standard
  if (q.type === 'MULTIPLE') {
    return userAns.split('').sort().join('') === standard.split('').sort().join('')
  }
  return false
})

function openChallengeDialog() {
  challengeCount.value = Math.min(5, mistakeList.value.length)
  challengeDialog.value = true
}

async function startChallenge() {
  challengeLoading.value = true
  try {
    const res = await apiStartChallenge(challengeCount.value)
    challengeData.value = res.data
    challengeDialog.value = false
    challengeCurrentIdx.value = 0
    resetChallengeState()
    challengeActiveDialog.value = true
  } catch {
    ElMessage.error('启动挑战失败，请稍后重试')
  } finally {
    challengeLoading.value = false
  }
}

function resetChallengeState() {
  challengeAnswer.value = ''
  challengeMultiAnswer.value = []
  challengeShowAnalysis.value = false
}

function selectChallengeAnswer(key: string) {
  if (challengeShowAnalysis.value) return
  const q = currentChallengeQuestion.value
  if (!q) return

  if (q.type === 'MULTIPLE') {
    const idx = challengeMultiAnswer.value.indexOf(key)
    if (idx >= 0) {
      challengeMultiAnswer.value.splice(idx, 1)
    } else {
      challengeMultiAnswer.value.push(key)
    }
    challengeAnswer.value = [...challengeMultiAnswer.value].sort().join('')
  } else {
    challengeAnswer.value = key
  }
}

function getChallengeOptionClass(key: string) {
  if (!challengeShowAnalysis.value) {
    const isSelected = currentChallengeQuestion.value?.type === 'MULTIPLE'
      ? challengeMultiAnswer.value.includes(key)
      : challengeAnswer.value === key
    return isSelected ? 'option-selected' : ''
  }
  return ''
}

async function submitChallengeAnswer() {
  if (!challengeAnswer.value && challengeMultiAnswer.value.length === 0) {
    ElMessage.warning('请先作答')
    return
  }
  challengeShowAnalysis.value = true

  const q = currentChallengeQuestion.value
  if (!q) return
  const standard = (q.standardAnswer || '').trim().toUpperCase()
  const userAns = challengeAnswer.value.trim().toUpperCase()
  let isCorrect = false

  if (q.type === 'JUDGE') {
    isCorrect = userAns === standard
  } else if (q.type === 'SINGLE') {
    isCorrect = userAns === standard
  } else if (q.type === 'MULTIPLE') {
    const userSorted = userAns.split('').sort().join('')
    const stdSorted  = standard.split('').sort().join('')
    isCorrect = userSorted === stdSorted
  }

  if (isCorrect) {
    ElMessage.success('回答正确！已自动标记为已攻克')
    try {
      await markMistakeMastered(q.questionId)
      mistakeList.value = mistakeList.value.filter(m => m.questionId !== q.questionId)
    } catch {
    }
  }
}

function nextChallengeQuestion() {
  challengeCurrentIdx.value++
  resetChallengeState()
}

function finishChallenge() {
  challengeActiveDialog.value = false
  challengeData.value = null
  challengeCurrentIdx.value = 0
  resetChallengeState()
  ElMessage.success('挑战完成！继续加油！')
}

onMounted(fetchMistakes)
</script>

<style scoped>
.mistakes-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--bg-base, #030303);
  min-height: 100vh;
  padding: 0;
}

.toolbar-card :deep(.el-card__body) {
  padding: 12px 16px;
  background: #0a0a0a !important;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-group, .action-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mistake-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mistake-card {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 15px rgba(0, 0, 0, 0.3);
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px));
}

:deep(.el-card__body) {
  background: #0a0a0a !important;
}

.mistake-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  cursor: pointer;
  transition: background 0.15s;
  flex-wrap: wrap;
  gap: 8px;
}

.mistake-header:hover {
  background: rgba(0, 255, 255, 0.03);
}

.mistake-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.mistake-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.wrong-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #ff10f0;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.4);
}

.last-time {
  font-size: 12px;
  color: #909090;
  font-family: 'JetBrains Mono', monospace;
}

.expand-icon {
  transition: transform 0.2s;
  color: #00ffff;
}

.expand-icon.rotated {
  transform: rotate(180deg);
}

.mistake-detail {
  padding: 0 16px 16px;
  border-top: 1px solid #1a1a2e;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  color: #00ffff;
  margin: 12px 0 6px;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.3);
}

.content-text {
  font-size: 14px;
  color: #e0e0e0;
  line-height: 1.7;
  white-space: pre-wrap;
  font-family: 'JetBrains Mono', monospace;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  font-size: 14px;
  margin-bottom: 4px;
  background: #0a0a0a;
  border: 1px solid #1a1a2e;
  font-family: 'JetBrains Mono', monospace;
  color: #e0e0e0;
}

.correct-option {
  background: rgba(57, 255, 20, 0.1);
  border-color: rgba(57, 255, 20, 0.3);
  color: #39ff14;
  text-shadow: 0 0 5px rgba(57, 255, 20, 0.3);
}

.option-key {
  font-weight: 700;
  min-width: 20px;
  color: #00ffff;
}

.correct-icon {
  color: #39ff14;
  margin-left: auto;
}

.analysis-text {
  font-size: 14px;
  color: #e0e0e0;
  line-height: 1.7;
  background: rgba(255, 152, 0, 0.05);
  border: 1px solid rgba(255, 152, 0, 0.2);
  padding: 10px 14px;
  white-space: pre-wrap;
  font-family: 'JetBrains Mono', monospace;
}

.challenge-setup {
  padding: 8px 0;
}

.challenge-tip {
  color: #909090;
  font-size: 14px;
  margin-bottom: 16px;
  line-height: 1.6;
  font-family: 'JetBrains Mono', monospace;
}

.challenge-body {
  min-height: 300px;
}

.challenge-progress {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  font-size: 14px;
  color: #00ffff;
  font-family: 'JetBrains Mono', monospace;
}

.progress-text {
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.4);
}

.cyberpunk-progress :deep(.el-progress-bar__outer) {
  background-color: #1a1a2e !important;
}

.cyberpunk-progress :deep(.el-progress-bar__inner) {
  background-color: #ff10f0 !important;
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.5);
}

.challenge-meta {
  display: flex;
  gap: 6px;
  margin-bottom: 14px;
}

.challenge-content {
  font-size: 15px;
  color: #e0e0e0;
  line-height: 1.7;
  margin-bottom: 16px;
  white-space: pre-wrap;
  font-family: 'JetBrains Mono', monospace;
}

.challenge-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.challenge-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid #1a1a2e;
  cursor: pointer;
  transition: all 0.15s;
  font-size: 14px;
  background: #0a0a0a;
  font-family: 'JetBrains Mono', monospace;
  color: #e0e0e0;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
}

.challenge-option:hover {
  border-color: #00ffff;
  background: rgba(0, 255, 255, 0.05);
  color: #00ffff;
}

.challenge-option.option-selected {
  border-color: #ff10f0;
  background: rgba(255, 16, 240, 0.1);
  color: #ff10f0;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.3);
}

.challenge-text-answer {
  margin-bottom: 16px;
}

.challenge-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.challenge-analysis {
  background: rgba(255, 152, 0, 0.05);
  border: 1px solid rgba(255, 152, 0, 0.3);
  padding: 14px;
  clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px));
}

.analysis-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #ff9800;
  margin-bottom: 8px;
  font-size: 14px;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(255, 152, 0, 0.4);
}

.cyberpunk-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid !important;
}

:deep(.el-tag--success) {
  border-color: #39ff14 !important;
  color: #39ff14 !important;
}

:deep(.el-tag--warning) {
  border-color: #ff9800 !important;
  color: #ff9800 !important;
}

:deep(.el-tag--danger) {
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
}

:deep(.el-tag--info) {
  border-color: #00ffff !important;
  color: #00ffff !important;
}

.cyberpunk-btn {
  background: transparent !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
}

.cyberpunk-btn:hover {
  background: #00ffff !important;
  color: #000 !important;
}

.warning-btn {
  border-color: #ff9800 !important;
  color: #ff9800 !important;
}

.warning-btn:hover {
  background: #ff9800 !important;
  color: #000 !important;
}

.master-btn {
  background: transparent !important;
  border: 1px solid #39ff14 !important;
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
}

.master-btn:hover {
  background: #39ff14 !important;
  color: #000 !important;
}

.success-btn {
  background: transparent !important;
  border: 1px solid #39ff14 !important;
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
}

.success-btn:hover {
  background: #39ff14 !important;
  color: #000 !important;
}

.cancel-btn {
  background: transparent !important;
  border: 1px solid #666 !important;
  color: #909090 !important;
  font-family: 'JetBrains Mono', monospace;
}

.cancel-btn:hover {
  border-color: #00ffff !important;
  color: #00ffff !important;
}

.count-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
}

.cyberpunk-input :deep(.el-input__wrapper) {
  background-color: #0a0a0a !important;
  border-color: #1a1a2e !important;
  box-shadow: none !important;
}

.cyberpunk-input :deep(.el-input__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-input :deep(.el-input__inner::placeholder) {
  color: #666 !important;
}

.cyberpunk-input :deep(.el-input__wrapper:focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

.cyberpunk-select :deep(.el-input__wrapper) {
  background-color: #0a0a0a !important;
  border-color: #1a1a2e !important;
  box-shadow: none !important;
}

.cyberpunk-select :deep(.el-select__wrapper) {
  background-color: #0a0a0a !important;
  border-color: #1a1a2e !important;
  box-shadow: none !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-select :deep(.el-select__wrapper:focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

.cyberpunk-select :deep(.el-select-dropdown) {
  background-color: #0a0a0a !important;
  border-color: #1a1a2e !important;
}

.cyberpunk-select :deep(.el-select-dropdown__item) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-select :deep(.el-select-dropdown__item.hover),
.cyberpunk-select :deep(.el-select-dropdown__item:hover) {
  background-color: #1a1a2e !important;
}

.cyberpunk-input-number :deep(.el-input__wrapper) {
  background-color: #0a0a0a !important;
  border-color: #1a1a2e !important;
  box-shadow: none !important;
}

.cyberpunk-input-number :deep(.el-input__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-textarea :deep(.el-textarea__inner) {
  background-color: #0a0a0a !important;
  border-color: #1a1a2e !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-textarea :deep(.el-textarea__inner:focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyberpunk-form-item :deep(.el-form-item__label) {
  color: #909090 !important;
  font-family: 'JetBrains Mono', monospace;
}

.max-hint {
  margin-left: 8px;
  color: #666;
  font-size: 13px;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-dialog {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
}

:deep(.el-dialog) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.1);
}

:deep(.el-dialog__header) {
  border-bottom: 1px solid #1a1a2e !important;
}

:deep(.el-dialog__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

:deep(.el-dialog__body) {
  background: #0a0a0a !important;
  color: #e0e0e0 !important;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #1a1a2e !important;
}

:deep(.el-message-box) {
  background: #0a0a0a !important;
  border-color: #1a1a2e !important;
}

:deep(.el-message-box__title) {
  color: #00ffff !important;
}

:deep(.el-message-box__message) {
  color: #e0e0e0 !important;
}

.cyberpunk-alert :deep(.el-alert__title) {
  font-family: 'JetBrains Mono', monospace;
}

:deep(.el-alert--success) {
  background-color: rgba(57, 255, 20, 0.1) !important;
  border-color: rgba(57, 255, 20, 0.3) !important;
}

:deep(.el-alert--error) {
  background-color: rgba(255, 16, 240, 0.1) !important;
  border-color: rgba(255, 16, 240, 0.3) !important;
}

.cyberpunk-empty {
  font-family: 'JetBrains Mono', monospace;
}

:deep(.el-empty__description) {
  color: #909090 !important;
}

:deep(.el-empty__image svg) {
  fill: #00ffff !important;
  opacity: 0.3;
}

:deep(.el-loading-mask) {
  background-color: rgba(3, 3, 3, 0.9) !important;
}

:deep(.el-loading-spinner .circular) {
  stroke: #00ffff !important;
}
</style>
