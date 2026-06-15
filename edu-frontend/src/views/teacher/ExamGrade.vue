<template>
  <div class="page">
    <el-card shadow="never" class="header-card">
      <template #header>
        <div class="header-bar">
          <div class="exam-info">
            <span class="exam-title">{{ examTitle }}</span>
            <el-button text @click="$router.back()">返回</el-button>
          </div>
          <div class="header-actions">
            <el-button
              type="success"
              :loading="publishing"
              :disabled="pendingSubmissions.length > 0"
              :title="pendingSubmissions.length > 0 ? '仍有待批改答卷，不能发布成绩' : '发布成绩'"
              @click="handlePublish"
            >
              发布成绩
            </el-button>
          </div>
        </div>
      </template>
      <el-row :gutter="20" class="statistics-row">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ statistics?.averageScore?.toFixed(1) || '-' }}</div>
            <div class="stat-label">平均分</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ statistics?.highestScore || '-' }}</div>
            <div class="stat-label">最高分</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ statistics ? formatPassRate(statistics.passRate) : '-' }}</div>
            <div class="stat-label">及格率</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ submittedGradeCountText }}</div>
            <div class="stat-label">已批改/总提交</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-card shadow="never" class="student-list-card">
          <template #header>
            <span>学生列表</span>
          </template>
          <div v-loading="loadingSubmissions">
            <el-tabs v-model="activeSubmissionTab" class="grade-tabs">
              <el-tab-pane :label="`待批改 ${pendingSubmissions.length}`" name="pending" />
              <el-tab-pane :label="`已批改 ${gradedSubmissions.length}`" name="graded" />
              <el-tab-pane :label="`答题中 ${inProgressSubmissions.length}`" name="inProgress" />
              <el-tab-pane :label="`全部 ${submissions.length}`" name="all" />
            </el-tabs>
            <div v-if="submissions.length === 0" class="empty-tip">
              暂无提交
            </div>
            <div v-else-if="displaySubmissions.length === 0" class="empty-tip compact">
              当前分组暂无学生
            </div>
            <div
              v-for="item in displaySubmissions"
              :key="item.submissionId"
              class="student-item"
              :class="{ active: selectedSubmissionId === item.submissionId }"
              @click="selectSubmission(item)"
            >
              <div class="student-name">{{ item.studentName }}</div>
              <div class="student-info">
                <el-tag :type="getStatusType(item.status)" size="small">
                  {{ getStatusText(item.status) }}
                </el-tag>
                <span class="score" v-if="item.scoreGained !== null">
                  {{ item.scoreGained }}分
                </span>
                <span class="screen-count" v-if="item.switchScreenCount > 0">
                  切屏{{ item.switchScreenCount }}次
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="16">
        <el-card shadow="never" class="answer-card">
          <template #header>
            <div class="answer-card-title">
              <span>答卷详情 - {{ currentStudentName || '未选择学生' }}</span>
              <el-tag v-if="submissionDetail" :type="getStatusType(submissionDetail.status)" size="small">
                {{ getStatusText(submissionDetail.status) }}
              </el-tag>
            </div>
          </template>
          <div v-loading="loadingDetail">
            <div v-if="!selectedSubmissionId" class="empty-tip">
              请选择左侧学生查看答卷
            </div>
            <div v-else-if="submissionDetail" class="answer-list">
              <div
                v-for="(answer, index) in submissionDetail.answers"
                :key="answer.questionId"
                class="question-item"
              >
                <el-card shadow="never" class="question-card">
                  <template #header>
                    <div class="question-header">
                      <span class="question-index">第{{ index + 1 }}题</span>
                      <el-tag size="small" type="info">{{ getQuestionTypeText(answer.type) }}</el-tag>
                      <el-tag size="small" type="warning">{{ answer.questionScore }}分</el-tag>
                    </div>
                  </template>
                  <div class="question-content">
                    <div class="content-text">{{ answer.content }}</div>

                    <div class="answer-section" v-if="formatOptions(answer.optionsJson).length">
                      <div class="section-title">题目选项：</div>
                      <div class="option-list">
                        <div v-for="option in formatOptions(answer.optionsJson)" :key="option" class="option-item">
                          {{ option }}
                        </div>
                      </div>
                    </div>

                    <div class="answer-section">
                      <div class="section-title">学生答案：</div>
                      <div
                        class="answer-text student-answer"
                        :class="{ empty: !answer.studentAnswer }"
                        v-if="answer.type !== 'SHORT' && answer.type !== 'CODE'"
                      >
                        {{ formatAnswer(answer.studentAnswer, answer.type, answer.optionsJson) }}
                      </div>
                      <div class="answer-text student-answer short-answer" :class="{ empty: !answer.studentAnswer }" v-else>
                        {{ answer.studentAnswer || '(未作答)' }}
                      </div>
                    </div>

                    <div class="answer-section">
                      <div class="section-title">标准答案：</div>
                      <el-collapse>
                        <el-collapse-item title="点击查看" name="1">
                          <div class="answer-text standard-answer">{{ formatAnswer(answer.standardAnswer, answer.type, answer.optionsJson) }}</div>
                        </el-collapse-item>
                      </el-collapse>
                    </div>

                    <div class="answer-section" v-if="answer.type !== 'SHORT' && answer.type !== 'CODE'">
                      <div class="section-title">批改结果：</div>
                      <div class="result-tags">
                        <el-tag :type="answer.isCorrect ? 'success' : 'danger'" size="small">
                          {{ answer.isCorrect ? '正确' : '错误' }}
                        </el-tag>
                        <span class="score-gained">得分：{{ answer.scoreGained ?? 0 }} / {{ answer.questionScore }}</span>
                      </div>
                    </div>

                    <div class="grading-section" v-else>
                      <div class="section-title">批改打分：</div>
                      <div class="grading-controls">
                        <el-input-number
                          v-model="gradeMap[answer.questionId]"
                          :min="0"
                          :max="answer.questionScore"
                          :step="0.5"
                          :precision="1"
                          size="default"
                        />
                        <span class="score-max">/ {{ answer.questionScore }}分</span>
                        <el-button
                          type="primary"
                          :icon="MagicStick"
                          :loading="aiLoadingMap[answer.questionId]"
                          @click="handleAiSuggest(answer)"
                        >
                          AI建议
                        </el-button>
                      </div>

                      <div v-if="aiSuggestMap[answer.questionId]" class="ai-suggest-panel">
                        <div class="ai-suggest-header">
                          <span class="ai-title">AI建议得分</span>
                          <el-tag type="success" size="small">
                            {{ aiSuggestMap[answer.questionId].suggestedScore }} / {{ answer.questionScore }}
                          </el-tag>
                          <el-tag size="small" :type="confidenceType(aiSuggestMap[answer.questionId].confidence)">
                            {{ confidenceText(aiSuggestMap[answer.questionId].confidence) }}
                          </el-tag>
                          <el-button size="small" type="success" text @click="applyAiScore(answer.questionId)">
                            采用此分
                          </el-button>
                        </div>
                        <div class="ai-reason">
                          {{ aiSuggestMap[answer.questionId].reasoning || aiSuggestMap[answer.questionId].suggestion || 'AI未返回详细原因' }}
                        </div>
                        <div class="ai-points" v-if="aiSuggestMap[answer.questionId].matchedPoints?.length">
                          <div class="ai-points-title">命中点</div>
                          <el-tag
                            v-for="point in aiSuggestMap[answer.questionId].matchedPoints"
                            :key="point"
                            size="small"
                            type="success"
                            effect="plain"
                          >
                            {{ point }}
                          </el-tag>
                        </div>
                        <div class="ai-points" v-if="aiSuggestMap[answer.questionId].missingPoints?.length">
                          <div class="ai-points-title">缺失/扣分点</div>
                          <el-tag
                            v-for="point in aiSuggestMap[answer.questionId].missingPoints"
                            :key="point"
                            size="small"
                            type="warning"
                            effect="plain"
                          >
                            {{ point }}
                          </el-tag>
                        </div>
                        <div class="ai-points" v-if="aiSuggestMap[answer.questionId].keyPoints?.length">
                          <div class="ai-points-title">主要采分点</div>
                          <el-tag
                            v-for="point in aiSuggestMap[answer.questionId].keyPoints"
                            :key="point"
                            size="small"
                            type="info"
                            effect="plain"
                          >
                            {{ point }}
                          </el-tag>
                        </div>
                      </div>
                    </div>
                  </div>
                </el-card>
              </div>

              <div class="submit-grade-btn" v-if="hasSubjectiveAnswers">
                <el-button type="success" size="large" :icon="MagicStick" :loading="aiBatchLoading" @click="handleAiSuggestAll()">
                  AI建议全部主观题
                </el-button>
                <el-button type="primary" size="large" :loading="submitting" @click="handleSubmitGrade">
                  {{ submissionDetail?.status === 'GRADED' || submissionDetail?.status === 'RETURNED' ? '保存修订' : '提交批改' }}
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { MagicStick } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import {
  getExamSubmissions,
  getSubmissionDetail,
  submitGrade,
  publishScore,
  getExamStatistics,
  getExamDetail,
  getAiGradeSuggest,
  type SubmissionItem,
  type SubmissionDetail,
  type AnswerItem,
  type AiGradeSuggest
} from '@/api/exam'

const route = useRoute()

const examId = Number(route.params.id)

const examTitle = ref('')
const submissions = ref<SubmissionItem[]>([])
const activeSubmissionTab = ref<'pending' | 'graded' | 'inProgress' | 'all'>('pending')
const selectedSubmissionId = ref<number | null>(null)
const currentStudentName = ref('')
const submissionDetail = ref<SubmissionDetail | null>(null)

const loadingSubmissions = ref(false)
const loadingDetail = ref(false)
const submitting = ref(false)
const publishing = ref(false)
const aiBatchLoading = ref(false)

const gradeMap = ref<Record<number, number>>({})
const aiLoadingMap = reactive<Record<number, boolean>>({})
const aiSuggestMap = reactive<Record<number, AiGradeSuggest>>({})

const statistics = ref<{
  averageScore: number
  highestScore: number
  passRate: number
} | null>(null)

const hasSubjectiveAnswers = computed(() => {
  return submissionDetail.value?.answers.some(isSubjectiveAnswer) ?? false
})

const subjectiveAnswers = computed(() =>
  submissionDetail.value?.answers.filter(isSubjectiveAnswer) ?? []
)

const pendingSubmissions = computed(() => submissions.value.filter(item => item.status === 'SUBMITTED'))
const gradedSubmissions = computed(() => submissions.value.filter(item => item.status === 'GRADED' || item.status === 'RETURNED'))
const inProgressSubmissions = computed(() => submissions.value.filter(item => item.status === 'UN'))
const submittedSubmissions = computed(() => submissions.value.filter(item => item.status !== 'UN'))
const submittedGradeCountText = computed(() => `${gradedSubmissions.value.length}/${submittedSubmissions.value.length}`)
const displaySubmissions = computed(() => {
  if (activeSubmissionTab.value === 'pending') return pendingSubmissions.value
  if (activeSubmissionTab.value === 'graded') return gradedSubmissions.value
  if (activeSubmissionTab.value === 'inProgress') return inProgressSubmissions.value
  return submissions.value
})

async function fetchExamInfo() {
  try {
    const res = await getExamDetail(examId)
    examTitle.value = res.data.title || '考试批改'
  } catch {
    examTitle.value = '考试批改'
  }
}

async function fetchSubmissions() {
  loadingSubmissions.value = true
  try {
    const res = await getExamSubmissions(examId)
    submissions.value = res.data || []
    if (!selectedSubmissionId.value && submissions.value.length > 0) {
      const first = pendingSubmissions.value[0] || gradedSubmissions.value[0] || inProgressSubmissions.value[0] || submissions.value[0]
      if (first) {
        await selectSubmission(first)
      }
    }
  } catch {
    ElMessage.error('获取提交列表失败')
  } finally {
    loadingSubmissions.value = false
  }
}

async function fetchStatistics() {
  try {
    const res = await getExamStatistics(examId)
    statistics.value = res.data
  } catch {
  }
}

async function selectSubmission(item: SubmissionItem) {
  selectedSubmissionId.value = item.submissionId
  currentStudentName.value = item.studentName
  await fetchSubmissionDetail(item.submissionId)
}

async function fetchSubmissionDetail(submissionId: number) {
  loadingDetail.value = true
  aiBatchLoading.value = false
  gradeMap.value = {}
  Object.keys(aiSuggestMap).forEach(key => delete aiSuggestMap[Number(key)])
  Object.keys(aiLoadingMap).forEach(key => delete aiLoadingMap[Number(key)])
  try {
    const res = await getSubmissionDetail(examId, submissionId)
    submissionDetail.value = res.data
    for (const answer of res.data.answers) {
      if (answer.scoreGained !== null) {
        gradeMap.value[answer.questionId] = answer.scoreGained
      }
      const savedSuggestion = buildSavedAiSuggestion(answer)
      if (savedSuggestion) {
        aiSuggestMap[answer.questionId] = savedSuggestion
      }
    }
    if (shouldAutoSuggestForDetail(res.data)) {
      void handleAiSuggestAll({ auto: true, submissionId: res.data.submissionId })
    }
  } catch {
    ElMessage.error('获取答卷详情失败')
  } finally {
    loadingDetail.value = false
  }
}

async function handleAiSuggest(answer: AnswerItem) {
  aiLoadingMap[answer.questionId] = true
  try {
    await requestAiSuggest(answer, selectedSubmissionId.value)
    ElMessage.success('AI建议已生成，已填入建议分')
  } catch {
    ElMessage.error('AI辅助批改失败')
  } finally {
    aiLoadingMap[answer.questionId] = false
  }
}

async function requestAiSuggest(answer: AnswerItem, submissionId: number | null) {
  const res = await getAiGradeSuggest({
    questionId: answer.questionId,
    questionType: answer.type,
    questionContent: answer.content,
    standardAnswer: answer.standardAnswer,
    studentAnswer: answer.studentAnswer || '',
    maxScore: answer.questionScore,
    scoringRubric: `请按本题满分 ${answer.questionScore} 分给出建议分。`
  })
  if (submissionId !== selectedSubmissionId.value) {
    return false
  }
  aiSuggestMap[answer.questionId] = res.data
  gradeMap.value[answer.questionId] = clampScore(Number(res.data.suggestedScore), answer.questionScore)
  return true
}

async function handleAiSuggestAll(options: { auto?: boolean; submissionId?: number | null } = {}) {
  const targetSubmissionId = options.submissionId ?? selectedSubmissionId.value
  const answers = submissionDetail.value?.submissionId === targetSubmissionId
    ? subjectiveAnswers.value
    : []
  if (answers.length === 0) {
    ElMessage.warning('当前答卷没有主观题')
    return
  }
  aiBatchLoading.value = true
  let failed = 0
  let applied = 0
  try {
    for (const answer of answers) {
      if (targetSubmissionId !== selectedSubmissionId.value) {
        return
      }
      aiLoadingMap[answer.questionId] = true
      try {
        const didApply = await requestAiSuggest(answer, targetSubmissionId)
        if (didApply) {
          applied += 1
        }
      } catch {
        failed += 1
      } finally {
        aiLoadingMap[answer.questionId] = false
      }
    }
    if (targetSubmissionId !== selectedSubmissionId.value) {
      return
    }
    if (failed > 0) {
      ElMessage.warning(`AI建议已生成，${failed} 道题失败`)
    } else if (options.auto) {
      ElMessage.success('AI建议已自动填入，可修改后提交批改')
    } else {
      ElMessage.success(`AI建议已全部生成，已填入 ${applied} 道题`)
    }
  } finally {
    if (targetSubmissionId === selectedSubmissionId.value) {
      aiBatchLoading.value = false
    }
  }
}

function applyAiScore(questionId: number) {
  const answer = submissionDetail.value?.answers.find(item => item.questionId === questionId)
  const suggestion = aiSuggestMap[questionId]
  if (!answer || !suggestion) return
  gradeMap.value[questionId] = clampScore(Number(suggestion.suggestedScore), answer.questionScore)
  ElMessage.success('已采用AI建议分')
}

function clampScore(score: number, maxScore: number) {
  if (Number.isNaN(score)) return 0
  return Math.min(Math.max(Number(score.toFixed(1)), 0), maxScore)
}

function isSubjectiveAnswer(answer: AnswerItem) {
  return answer.type === 'SHORT' || answer.type === 'CODE'
}

function shouldAutoSuggestForDetail(detail: SubmissionDetail) {
  return detail.status === 'SUBMITTED' && detail.answers.some(isSubjectiveAnswer)
}

function findNextPendingSubmission(currentSubmissionId: number) {
  if (submissions.value.length === 0) return null
  const currentIndex = submissions.value.findIndex(item => item.submissionId === currentSubmissionId)
  const startIndex = currentIndex >= 0 ? currentIndex + 1 : 0
  return submissions.value.slice(startIndex).find(item => item.status === 'SUBMITTED')
    || submissions.value.slice(0, Math.max(startIndex, 0)).find(item => item.status === 'SUBMITTED')
    || null
}

async function scrollToAnswerTop() {
  await nextTick()
  document.querySelector('.answer-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function formatPassRate(passRate: number) {
  const normalizedRate = passRate > 0 && passRate <= 1 ? passRate * 100 : passRate
  return `${normalizedRate.toFixed(0)}%`
}

async function handleSubmitGrade() {
  if (!selectedSubmissionId.value || !submissionDetail.value) return
  const currentSubmissionId = selectedSubmissionId.value
  const wasPendingSubmission = submissionDetail.value.status === 'SUBMITTED'
  
  const grades = submissionDetail.value.answers.map(answer => ({
    questionId: answer.questionId,
    scoreGained: clampScore(
      Number(gradeMap.value[answer.questionId] ?? answer.scoreGained ?? 0),
      answer.questionScore
    ),
    aiSuggestScore: aiSuggestMap[answer.questionId]?.suggestedScore ?? answer.aiSuggestScore ?? null,
    aiSuggestDetail: aiSuggestMap[answer.questionId]
      ? JSON.stringify(aiSuggestMap[answer.questionId])
      : answer.aiSuggestDetail ?? null
  }))

  submitting.value = true
  try {
    await submitGrade(examId, currentSubmissionId, grades)
    await fetchSubmissions()
    const nextPendingSubmission = wasPendingSubmission ? findNextPendingSubmission(currentSubmissionId) : null
    if (nextPendingSubmission) {
      activeSubmissionTab.value = 'pending'
      await selectSubmission(nextPendingSubmission)
      await scrollToAnswerTop()
      ElMessage.success(`批改完成，已切换到 ${nextPendingSubmission.studentName}`)
    } else {
      await fetchSubmissionDetail(currentSubmissionId)
      ElMessage.success(wasPendingSubmission ? '批改完成，已无待批改答卷' : '成绩修订已保存')
    }
    await fetchStatistics()
  } catch {
    ElMessage.error('提交批改失败')
  } finally {
    submitting.value = false
  }
}

function buildSavedAiSuggestion(answer: AnswerItem): AiGradeSuggest | null {
  if (!answer.aiSuggestDetail && answer.aiSuggestScore == null) return null
  if (answer.aiSuggestDetail) {
    try {
      const parsed = JSON.parse(answer.aiSuggestDetail) as AiGradeSuggest
      return {
        suggestedScore: Number(parsed.suggestedScore ?? answer.aiSuggestScore ?? 0),
        maxScore: Number(parsed.maxScore ?? answer.questionScore),
        reasoning: parsed.reasoning || parsed.suggestion || '',
        suggestion: parsed.suggestion,
        keyPoints: parsed.keyPoints || [],
        matchedPoints: parsed.matchedPoints || [],
        missingPoints: parsed.missingPoints || [],
        confidence: parsed.confidence || 'MEDIUM'
      }
    } catch {
    }
  }
  return {
    suggestedScore: Number(answer.aiSuggestScore ?? 0),
    maxScore: answer.questionScore,
    reasoning: '',
    keyPoints: [],
    matchedPoints: [],
    missingPoints: [],
    confidence: 'MEDIUM'
  }
}

async function handlePublish() {
  publishing.value = true
  try {
    await publishScore(examId)
    ElMessage.success('成绩已发布')
    await fetchSubmissions()
  } catch (error: any) {
    ElMessage.error(error.message || '发布成绩失败')
  } finally {
    publishing.value = false
  }
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    UN: '答题中',
    SUBMITTED: '待批改',
    GRADED: '已批改',
    RETURNED: '已发放'
  }
  return map[status] || status
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
    UN: 'info',
    SUBMITTED: 'warning',
    GRADED: 'success',
    RETURNED: 'info'
  }
  return map[status] || 'info'
}

function getQuestionTypeText(type: string) {
  const map: Record<string, string> = {
    SINGLE: '单选题',
    MULTIPLE: '多选题',
    JUDGE: '判断题',
    SHORT: '简答题',
    CODE: '编程题'
  }
  return map[type] || type
}

function confidenceText(confidence?: string) {
  const map: Record<string, string> = {
    HIGH: '高置信',
    MEDIUM: '中置信',
    LOW: '低置信'
  }
  return confidence ? (map[confidence] || confidence) : '中置信'
}

function confidenceType(confidence?: string) {
  const map: Record<string, string> = {
    HIGH: 'success',
    MEDIUM: 'warning',
    LOW: 'info'
  }
  return confidence ? (map[confidence] || 'info') : 'warning'
}

function parseOptions(optionsJson: string | null): string[] {
  if (!optionsJson) return []
  try {
    const parsed = JSON.parse(optionsJson)
    if (Array.isArray(parsed)) {
      return parsed.map((option) => {
        if (typeof option === 'string') return option
        if (option && typeof option === 'object') {
          const record = option as Record<string, unknown>
          return String(record.content ?? record.text ?? record.label ?? record.value ?? '')
        }
        return String(option ?? '')
      }).filter(Boolean)
    }
    if (parsed && typeof parsed === 'object') {
      return Object.entries(parsed as Record<string, unknown>)
        .sort(([left], [right]) => left.localeCompare(right))
        .map(([, value]) => {
          if (typeof value === 'string') return value
          if (value && typeof value === 'object') {
            const record = value as Record<string, unknown>
            return String(record.content ?? record.text ?? record.label ?? record.value ?? '')
          }
          return String(value ?? '')
        })
        .filter(Boolean)
    }
    return []
  } catch {
    return []
  }
}

function formatOptions(optionsJson: string | null) {
  return parseOptions(optionsJson).map((option, index) => `${String.fromCharCode(65 + index)}. ${option}`)
}

function formatAnswer(answer: string | null, type: string, optionsJson: string | null): string {
  if (!answer) return '(未作答)'
  if (type === 'SINGLE' || type === 'MULTIPLE' || type === 'JUDGE') {
    const options = parseOptions(optionsJson)
    if (type === 'JUDGE') {
      const normalized = answer.trim().toLowerCase()
      if (['t', 'true', '1', '正确', '对', '是'].includes(normalized)) return '正确'
      if (['f', 'false', '0', '错误', '错', '否'].includes(normalized)) return '错误'
      return answer
    }
    if (options.length > 0) {
      const letters = answer.split(/[,，、\s]+/).map(a => a.trim().toUpperCase()).filter(Boolean)
      return letters.map(l => {
        const idx = l.charCodeAt(0) - 65
        return idx >= 0 && idx < options.length ? `${l}. ${options[idx]}` : l
      }).join(', ')
    }
  }
  return answer
}

onMounted(() => {
  fetchExamInfo()
  fetchSubmissions()
  fetchStatistics()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--bg-surface);
  min-height: 100vh;
  padding: 16px;
  font-family: 'JetBrains Mono', monospace;
}

.header-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
}

.header-card :deep(.el-card__header) {
  padding: 12px 20px;
  background: transparent !important;
  border-bottom: 1px solid var(--border) !important;
}

.header-card :deep(.el-card__body) {
  background: transparent !important;
}

.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.exam-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.exam-title {
  font-size: 18px;
  font-weight: 600;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.header-bar :deep(.el-button) {
  font-family: 'JetBrains Mono', monospace;
  color: #ff10f0 !important;
  border: 1px solid #ff10f0 !important;
  background: transparent !important;
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
}

.header-bar :deep(.el-button:hover) {
  background: rgba(255, 16, 240, 0.1) !important;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.3) !important;
}

.header-actions :deep(.el-button--success) {
  font-family: 'JetBrains Mono', monospace;
  color: #39ff14 !important;
  border: 1px solid #39ff14 !important;
  background: transparent !important;
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
}

.header-actions :deep(.el-button--success:hover) {
  background: rgba(57, 255, 20, 0.1) !important;
  box-shadow: 0 0 15px rgba(57, 255, 20, 0.3) !important;
}

.statistics-row {
  padding: 8px 0;
}

.stat-item {
  text-align: center;
  padding: 12px 8px;
  background: var(--bg-surface);
  border: 1px solid var(--border);
  clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px));
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
  font-family: 'JetBrains Mono', monospace;
}

.stat-label {
  font-size: 12px;
  color: var(--text-primary) !important;
  margin-top: 4px;
  font-family: 'JetBrains Mono', monospace;
  opacity: 0.8;
}

.student-list-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px));
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.08) !important;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.student-list-card :deep(.el-card__header) {
  padding: 12px 16px;
  background: transparent !important;
  border-bottom: 1px solid var(--border) !important;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
}

.student-list-card :deep(.el-card__body) {
  background: transparent !important;
  padding: 0 0 8px !important;
}

.grade-tabs {
  padding: 0 12px;
}

.grade-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.grade-tabs :deep(.el-tabs__nav-wrap::after) {
  background: var(--bg-surface);
}

.grade-tabs :deep(.el-tabs__item) {
  color: var(--text-secondary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
}

.grade-tabs :deep(.el-tabs__item.is-active) {
  color: #00ffff;
}

.grade-tabs :deep(.el-tabs__active-bar) {
  background: #00ffff;
}

.student-list-card :deep(.el-loading-mask) {
  background: rgba(3, 3, 3, 0.9) !important;
}

.student-item {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  transition: all 0.3s;
  background: transparent;
}

.student-item:hover {
  background: rgba(0, 255, 255, 0.05) !important;
}

.student-item.active {
  background: rgba(255, 16, 240, 0.1) !important;
  border-left: 3px solid #ff10f0;
  box-shadow: inset 0 0 20px rgba(255, 16, 240, 0.1);
}

.student-name {
  font-weight: 500;
  margin-bottom: 6px;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-primary) !important;
}

.student-info :deep(.el-tag) {
  font-family: 'JetBrains Mono', monospace;
  border: 1px solid !important;
}

.student-info :deep(.el-tag--warning) {
  background: transparent !important;
  border-color: #ff9800 !important;
  color: #ff9800 !important;
}

.student-info :deep(.el-tag--success) {
  background: transparent !important;
  border-color: #39ff14 !important;
  color: #39ff14 !important;
}

.student-info :deep(.el-tag--info) {
  background: transparent !important;
  border-color: var(--text-primary) !important;
  color: var(--text-primary) !important;
}

.score {
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace;
}

.screen-count {
  color: #ff9800 !important;
  font-family: 'JetBrains Mono', monospace;
}

.answer-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px));
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.08) !important;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.answer-card :deep(.el-card__header) {
  padding: 12px 16px;
  background: transparent !important;
  border-bottom: 1px solid var(--border) !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
}

.answer-card :deep(.el-card__body) {
  background: transparent !important;
  padding: 16px !important;
}

.answer-card :deep(.el-loading-mask) {
  background: rgba(3, 3, 3, 0.9) !important;
}

.empty-tip {
  text-align: center;
  color: var(--text-primary) !important;
  padding: 40px 0;
  font-family: 'JetBrains Mono', monospace;
  opacity: 0.6;
}

.empty-tip.compact {
  padding: 24px 0;
}

.answer-card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.question-card {
  margin-bottom: 16px;
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px));
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.05) !important;
}

.question-card :deep(.el-card__header) {
  padding: 10px 14px;
  background: rgba(26, 26, 46, 0.5) !important;
  border-bottom: 1px solid var(--border) !important;
}

.question-card :deep(.el-card__body) {
  background: transparent !important;
  padding: 12px 14px !important;
}

.question-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.question-index {
  font-weight: 600;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.question-header :deep(.el-tag) {
  font-family: 'JetBrains Mono', monospace;
  border: 1px solid !important;
}

.question-header :deep(.el-tag--info) {
  background: transparent !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
}

.question-header :deep(.el-tag--warning) {
  background: transparent !important;
  border-color: #ff9800 !important;
  color: #ff9800 !important;
}

.question-content {
  padding: 12px 0;
}

.content-text {
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 16px;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace;
}

.option-list {
  display: grid;
  gap: 6px;
}

.option-item {
  color: var(--text-primary);
  padding: 8px 10px;
  border: 1px solid var(--border);
  background: rgba(0, 0, 0, 0.22);
  font-size: 12px;
  line-height: 1.5;
}

.answer-section {
  margin-bottom: 12px;
}

.section-title {
  font-weight: 500;
  color: #ff10f0 !important;
  margin-bottom: 8px;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.3);
}

.answer-text {
  padding: 10px 12px;
  border-radius: 0;
  background: rgba(26, 26, 46, 0.5);
  line-height: 1.6;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace;
  border-left: 3px solid;
}

.student-answer {
  border-left-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.1);
}

.student-answer.empty {
  color: var(--text-secondary) !important;
  border-left-color: var(--text-secondary) !important;
  background: rgba(144, 144, 144, 0.06);
}

.standard-answer {
  border-left-color: #39ff14 !important;
  box-shadow: 0 0 10px rgba(57, 255, 20, 0.1);
}

.short-answer {
  white-space: pre-wrap;
  min-height: 60px;
}

.answer-section :deep(.el-collapse) {
  border: none !important;
  background: transparent !important;
}

.answer-section :deep(.el-collapse-item__header) {
  background: transparent !important;
  border-bottom: 1px solid var(--border) !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  height: 36px;
  line-height: 36px;
}

.answer-section :deep(.el-collapse-item__wrap) {
  background: transparent !important;
  border-bottom: none !important;
}

.answer-section :deep(.el-collapse-item__content) {
  background: transparent !important;
  padding: 10px 0 !important;
  color: var(--text-primary) !important;
}

.result-tags {
  display: flex;
  align-items: center;
  gap: 12px;
}

.result-tags :deep(.el-tag) {
  font-family: 'JetBrains Mono', monospace;
  border: 1px solid !important;
}

.result-tags :deep(.el-tag--success) {
  background: transparent !important;
  border-color: #39ff14 !important;
  color: #39ff14 !important;
  box-shadow: 0 0 10px rgba(57, 255, 20, 0.2);
}

.result-tags :deep(.el-tag--danger) {
  background: transparent !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.2);
}

.score-gained {
  color: #39ff14 !important;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(57, 255, 20, 0.5);
}

.grading-section {
  margin-top: 12px;
}

.grading-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.grading-controls :deep(.el-input-number) {
  --el-input-bg-color: var(--bg-surface);
  --el-input-border-color: var(--border-subtle);
  --el-input-text-color: #00ffff;
  --el-input-hover-border-color: #00ffff;
  --el-input-focus-border-color: #00ffff;
}

.grading-controls :deep(.el-input-number .el-input__wrapper) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.1) !important;
}

.grading-controls :deep(.el-input-number .el-input__inner) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.5);
}

.grading-controls :deep(.el-input-number__decrease),
.grading-controls :deep(.el-input-number__increase) {
  background: var(--bg-surface) !important;
  border-color: var(--border-subtle) !important;
  color: #00ffff !important;
}

.grading-controls :deep(.el-input-number__decrease:hover),
.grading-controls :deep(.el-input-number__increase:hover) {
  color: #ff10f0 !important;
}

.score-max {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace;
  opacity: 0.7;
}

.ai-suggest-panel {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid rgba(0, 255, 255, 0.25);
  background: rgba(0, 255, 255, 0.04);
}

.ai-suggest-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.ai-title {
  color: #00ffff;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
}

.ai-reason {
  color: var(--text-primary);
  line-height: 1.7;
  white-space: pre-wrap;
  margin-bottom: 10px;
}

.ai-points {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.ai-points-title {
  color: #ff10f0;
  font-size: 12px;
  font-weight: 600;
  margin-right: 2px;
}

.submit-grade-btn {
  text-align: center;
  padding: 20px 0;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.submit-grade-btn :deep(.el-button--primary) {
  font-family: 'JetBrains Mono', monospace;
  color: #0a0a0a !important;
  background: linear-gradient(135deg, #00ffff, #ff10f0) !important;
  border: none !important;
  clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px));
  font-weight: 600;
  padding: 12px 32px;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.4), 0 0 40px rgba(255, 16, 240, 0.2);
}

.submit-grade-btn :deep(.el-button--primary:hover) {
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.6), 0 0 60px rgba(255, 16, 240, 0.4);
  transform: translateY(-2px);
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: var(--bg-surface);
}

::-webkit-scrollbar-thumb {
  background: var(--bg-surface);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #00ffff;
}
</style>
