<template>
  <div class="page-container">
    <el-button text @click="router.push('/student/exam-records')" class="back-btn">
      <span class="back-arrow">←</span> 返回列表
    </el-button>

    <el-card v-if="result" class="result-card">
      <template #header>
        <div class="card-header">
          <span class="header-bracket">[</span>
          <span>{{ result.examTitle }}</span>
          <span class="header-bracket">]</span>
        </div>
      </template>

      <div class="result-summary">
        <el-row :gutter="20">
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-label">总分</div>
              <div class="stat-value total">{{ result.totalScore }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-label">得分</div>
              <div class="stat-value score">{{ result.scoreGained ?? '-' }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-item">
              <div class="stat-label">结果</div>
              <div class="stat-value">
                <el-tag :type="result.passed ? 'success' : 'danger'" size="large" class="cyberpunk-tag">
                  {{ result.passed ? '及格' : '不及格' }}
                </el-tag>
              </div>
            </div>
          </el-col>
        </el-row>
        <el-row :gutter="20" class="second-row">
          <el-col :span="12">
            <div class="stat-item">
              <div class="stat-label">排名</div>
              <div class="stat-value rank">
                {{ result.rank !== null ? `${result.rank} / ${result.totalStudents}` : '-' }}
              </div>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="stat-item">
              <div class="stat-label">提交时间</div>
              <div class="stat-value time">{{ result.submitTime ? formatTime(result.submitTime) : '-' }}</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <el-card class="answers-card" v-if="result">
      <template #header>
        <div class="card-header">
          <span class="header-bracket">[</span>
          <span>答题详情</span>
          <span class="header-bracket">]</span>
        </div>
      </template>

      <div class="answer-list">
        <el-card
          v-for="(answer, index) in result.answers"
          :key="answer.questionId"
          shadow="never"
          class="question-card"
        >
          <template #header>
            <div class="question-header">
              <span class="question-index">第{{ index + 1 }}题</span>
              <el-tag size="small" type="info" class="cyberpunk-tag">{{ getQuestionTypeText(answer.type) }}</el-tag>
              <el-tag size="small" :type="getDifficultyType(answer.difficulty)" class="cyberpunk-tag">
                {{ getDifficultyText(answer.difficulty) }}
              </el-tag>
              <span class="question-score">{{ answer.score }}分</span>
            </div>
          </template>

          <div class="question-content">
            <div class="content-text">{{ answer.content }}</div>

            <div class="answer-section" v-if="answer.type !== 'SHORT' && answer.type !== 'CODE'">
              <div class="section-title">选项：</div>
              <div class="options-text" v-if="answer.optionsJson">
                <div v-for="(opt, idx) in parseOptions(answer.optionsJson)" :key="idx" class="option-item">
                  <span class="option-letter">{{ String.fromCharCode(65 + idx) }}.</span> {{ opt }}
                </div>
              </div>
            </div>

            <div class="answer-section">
              <div class="section-title">学生答案：</div>
              <div
                class="answer-text student-answer"
                :class="{
                  correct: answer.isCorrect === true,
                  wrong: answer.isCorrect === false,
                  'code-answer': answer.type === 'CODE'
                }"
              >
                {{ formatAnswer(answer.studentAnswer, answer.type, answer.optionsJson) || '(未作答)' }}
              </div>
            </div>

            <div class="answer-section" v-if="result.showAnalysis && answer.standardAnswer">
              <div class="section-title">标准答案：</div>
              <div class="answer-text standard-answer" :class="{ 'code-answer': answer.type === 'CODE' }">
                {{ formatAnswer(answer.standardAnswer, answer.type, answer.optionsJson) }}
              </div>
            </div>

            <div class="answer-section">
              <div class="section-title">批改结果：</div>
              <div class="result-row">
                <el-tag :type="answer.isCorrect ? 'success' : 'danger'" size="small" class="cyberpunk-tag">
                  {{ answer.isCorrect === null ? '未批改' : (answer.isCorrect ? '正确' : '错误') }}
                </el-tag>
                <span class="score-gained">得分：{{ answer.scoreGained ?? 0 }} / {{ answer.score }}</span>
              </div>
            </div>

            <div class="answer-section" v-if="formatAiSuggestion(answer)">
              <div class="section-title">AI评分建议：</div>
              <div class="ai-suggest-box">
                <div class="ai-score-row">
                  <el-tag type="success" size="small" class="cyberpunk-tag">
                    建议 {{ formatAiSuggestion(answer)?.suggestedScore }} / {{ answer.score }}
                  </el-tag>
                  <el-tag size="small" class="cyberpunk-tag">
                    {{ confidenceText(formatAiSuggestion(answer)?.confidence) }}
                  </el-tag>
                </div>
                <div v-if="formatAiSuggestion(answer)?.reasoning" class="analysis-text">
                  {{ formatAiSuggestion(answer)?.reasoning }}
                </div>
                <div v-if="formatAiSuggestion(answer)?.matchedPoints.length" class="ai-point-row">
                  <span>命中点：</span>
                  <el-tag
                    v-for="point in formatAiSuggestion(answer)?.matchedPoints"
                    :key="point"
                    size="small"
                    type="success"
                    effect="plain"
                  >
                    {{ point }}
                  </el-tag>
                </div>
                <div v-if="formatAiSuggestion(answer)?.missingPoints.length" class="ai-point-row">
                  <span>缺失/扣分点：</span>
                  <el-tag
                    v-for="point in formatAiSuggestion(answer)?.missingPoints"
                    :key="point"
                    size="small"
                    type="warning"
                    effect="plain"
                  >
                    {{ point }}
                  </el-tag>
                </div>
              </div>
            </div>

            <div class="answer-section" v-if="result.showAnalysis && answer.analysis">
              <div class="section-title">答案解析：</div>
              <div class="analysis-text">{{ answer.analysis }}</div>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="!result || result.answers.length === 0" class="empty-tip">
        <span class="glitch-text">暂无答题详情</span>
      </div>
    </el-card>

    <el-card v-else-if="!loading" class="empty-card">
      <el-empty description="暂无成绩详情" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { getMyExamResult, type ExamResultData } from '@/api/exam'
import type { QuestionResult } from '@/api/exam'

const route = useRoute()
const router = useRouter()
const examId = Number(route.params.id)
const loading = ref(false)
const result = ref<ExamResultData | null>(null)

function formatTime(time: string): string {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getQuestionTypeText(type: string): string {
  const map: Record<string, string> = {
    SINGLE: '单选题',
    MULTIPLE: '多选题',
    JUDGE: '判断题',
    SHORT: '简答题',
    CODE: '编程题'
  }
  return map[type] || type
}

function getDifficultyType(difficulty: string): string {
  const map: Record<string, string> = {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger'
  }
  return map[difficulty] || 'info'
}

function getDifficultyText(difficulty: string): string {
  const map: Record<string, string> = {
    EASY: '简单',
    MEDIUM: '中等',
    HARD: '困难'
  }
  return map[difficulty] || difficulty
}

function parseOptions(optionsJson: string | null): string[] {
  if (!optionsJson) return []
  try {
    return JSON.parse(optionsJson)
  } catch {
    return []
  }
}

function formatAnswer(answer: string | null, type: string, optionsJson: string | null): string {
  if (!answer) return ''
  if (type === 'SINGLE' || type === 'MULTIPLE' || type === 'JUDGE') {
    if (type === 'JUDGE') {
      return answer === 'T' ? '正确' : '错误'
    }
    const options = parseOptions(optionsJson)
    if (options.length > 0) {
      const letters = answer.split(',').map(a => a.trim().toUpperCase())
      return letters.map(l => {
        const idx = l.charCodeAt(0) - 65
        return idx >= 0 && idx < options.length ? `${l}. ${options[idx]}` : l
      }).join(', ')
    }
  }
  return answer
}

function formatAiSuggestion(answer: QuestionResult) {
  if (!answer.aiSuggestDetail && answer.aiSuggestScore == null) return null
  if (answer.aiSuggestDetail) {
    try {
      const parsed = JSON.parse(answer.aiSuggestDetail)
      return {
        suggestedScore: Number(parsed.suggestedScore ?? answer.aiSuggestScore ?? 0),
        reasoning: String(parsed.reasoning || parsed.suggestion || ''),
        confidence: String(parsed.confidence || 'MEDIUM'),
        matchedPoints: Array.isArray(parsed.matchedPoints) ? parsed.matchedPoints.map(String) : [],
        missingPoints: Array.isArray(parsed.missingPoints) ? parsed.missingPoints.map(String) : []
      }
    } catch {
    }
  }
  return {
    suggestedScore: Number(answer.aiSuggestScore ?? 0),
    reasoning: '',
    confidence: 'MEDIUM',
    matchedPoints: [],
    missingPoints: []
  }
}

function confidenceText(confidence?: string) {
  const map: Record<string, string> = {
    HIGH: '高置信',
    MEDIUM: '中置信',
    LOW: '低置信'
  }
  return confidence ? (map[confidence] || confidence) : '中置信'
}

async function loadResult() {
  loading.value = true
  try {
    const res = await getMyExamResult(examId)
    result.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '加载成绩详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadResult()
})
</script>

<style scoped>
.page-container {
  padding: 0;
  background: var(--bg-base);
  min-height: 100vh;
}

.back-btn {
  margin-bottom: 16px;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  border: 1px solid #00ffff !important;
  background: transparent !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px);
}

.back-btn:hover {
  background: #00ffff !important;
  color: #000 !important;
}

.back-arrow {
  margin-right: 8px;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
  font-family: 'JetBrains Mono', monospace;
  color: #00ffff;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.header-bracket {
  color: #ff10f0;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5);
}

.result-card {
  margin-bottom: 16px;
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

.result-summary {
  padding: 20px 0;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
  letter-spacing: 2px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--text-primary);
  font-family: 'JetBrains Mono', monospace;
}

.stat-value.total {
  color: #00ffff;
  text-shadow: 0 0 15px rgba(0, 255, 255, 0.6);
}

.stat-value.score {
  color: #39ff14;
  text-shadow: 0 0 15px rgba(57, 255, 20, 0.6);
}

.stat-value.rank {
  color: #ff9800;
  text-shadow: 0 0 15px rgba(255, 152, 0, 0.6);
}

.stat-value.time {
  color: var(--text-primary);
  font-size: 22px;
}

.second-row {
  margin-top: 20px;
}

.answers-card {
  margin-top: 16px;
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
}

.answer-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.3);
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px));
}

:deep(.el-card__header) {
  background: var(--bg-surface) !important;
  border-bottom: 1px solid var(--border) !important;
}

:deep(.el-card__body) {
  background: var(--bg-surface) !important;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.question-index {
  font-weight: 600;
  color: #00ffff;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 8px rgba(0, 255, 255, 0.4);
}

.question-score {
  margin-left: auto;
  color: #ff9800;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 8px rgba(255, 152, 0, 0.4);
}

.question-content {
  padding: 12px 0;
}

.content-text {
  font-size: 15px;
  line-height: 1.7;
  margin-bottom: 16px;
  white-space: pre-wrap;
  color: var(--text-primary);
  font-family: 'JetBrains Mono', monospace;
}

.answer-section {
  margin-bottom: 12px;
}

.section-title {
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 8px;
  font-family: 'JetBrains Mono', monospace;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-size: 12px;
}

.options-text {
  padding: 8px 12px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  line-height: 1.8;
  font-family: 'JetBrains Mono', monospace;
  color: var(--text-primary);
}

.option-item {
  margin-bottom: 4px;
}

.option-letter {
  color: #00ffff;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.4);
}

.answer-text {
  padding: 10px 12px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  line-height: 1.6;
  font-family: 'JetBrains Mono', monospace;
  color: var(--text-primary);
}

.code-answer {
  white-space: pre-wrap;
  overflow-x: auto;
  tab-size: 4;
  word-break: normal;
}

.student-answer.correct {
  border-left: 3px solid #39ff14;
  box-shadow: 0 0 10px rgba(57, 255, 20, 0.2);
}

.student-answer.wrong {
  border-left: 3px solid #ff10f0;
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.2);
}

.standard-answer {
  border-left: 3px solid #00ffff;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.2);
}

.result-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.score-gained {
  color: #39ff14;
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 8px rgba(57, 255, 20, 0.4);
}

.analysis-text {
  padding: 10px 12px;
  background: rgba(57, 255, 20, 0.05);
  border: 1px solid rgba(57, 255, 20, 0.2);
  border-left: 3px solid #39ff14;
  color: var(--text-primary);
  line-height: 1.6;
  font-family: 'JetBrains Mono', monospace;
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

:deep(.el-tag--info) {
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3);
}

.empty-card {
  margin-top: 16px;
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
}

.empty-tip {
  text-align: center;
  color: var(--text-secondary);
  padding: 40px 0;
  font-family: 'JetBrains Mono', monospace;
}

.glitch-text {
  color: #ff10f0;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5);
  animation: glitch 2s infinite;
}

@keyframes glitch {
  0%, 100% { text-shadow: 0 0 10px rgba(255, 16, 240, 0.5); }
  50% { text-shadow: -2px 0 #00ffff, 2px 0 #ff10f0; }
}

:deep(.el-empty__description) {
  color: var(--text-secondary) !important;
}

:deep(.el-empty__image svg) {
  fill: #00ffff !important;
  opacity: 0.3;
}
</style>
