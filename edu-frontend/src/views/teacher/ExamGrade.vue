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
            <el-button type="success" :loading="publishing" @click="handlePublish">
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
            <div class="stat-value">{{ statistics ? `${(statistics.passRate * 100).toFixed(0)}%` : '-' }}</div>
            <div class="stat-label">及格率</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-value">{{ statistics?.gradedCount || 0 }}/{{ statistics?.submittedCount || 0 }}</div>
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
            <div v-if="submissions.length === 0" class="empty-tip">
              暂无提交
            </div>
            <div
              v-for="item in submissions"
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
            <span>答卷详情 - {{ currentStudentName }}</span>
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

                    <div class="answer-section">
                      <div class="section-title">学生答案：</div>
                      <div class="answer-text student-answer" v-if="answer.type !== 'SHORT' && answer.type !== 'CODE'">
                        {{ formatAnswer(answer.studentAnswer, answer.type, answer.optionsJson) }}
                      </div>
                      <div class="answer-text student-answer short-answer" v-else>
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
                <el-button type="primary" size="large" :loading="submitting" @click="handleSubmitGrade">
                  提交批改
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
const selectedSubmissionId = ref<number | null>(null)
const currentStudentName = ref('')
const submissionDetail = ref<SubmissionDetail | null>(null)

const loadingSubmissions = ref(false)
const loadingDetail = ref(false)
const submitting = ref(false)
const publishing = ref(false)

const gradeMap = ref<Record<number, number>>({})
const aiLoadingMap = reactive<Record<number, boolean>>({})
const aiSuggestMap = reactive<Record<number, AiGradeSuggest>>({})

const statistics = ref<{
  averageScore: number
  highestScore: number
  passRate: number
  gradedCount: number
  submittedCount: number
} | null>(null)

const hasSubjectiveAnswers = computed(() => {
  return submissionDetail.value?.answers.some(a => a.type === 'SHORT' || a.type === 'CODE') ?? false
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
    const res = await getAiGradeSuggest({
      questionId: answer.questionId,
      questionType: answer.type,
      questionContent: answer.content,
      standardAnswer: answer.standardAnswer,
      studentAnswer: answer.studentAnswer || '',
      maxScore: answer.questionScore,
      scoringRubric: `请按本题满分 ${answer.questionScore} 分给出建议分。`
    })
    aiSuggestMap[answer.questionId] = res.data
    gradeMap.value[answer.questionId] = clampScore(Number(res.data.suggestedScore), answer.questionScore)
    ElMessage.success('AI建议已生成，已填入建议分')
  } catch {
    ElMessage.error('AI辅助批改失败')
  } finally {
    aiLoadingMap[answer.questionId] = false
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


async function handleSubmitGrade() {
  if (!selectedSubmissionId.value) return
  
  const grades = Object.entries(gradeMap.value).map(([questionId, scoreGained]) => ({
    questionId: Number(questionId),
    scoreGained
  }))

  submitting.value = true
  try {
    await submitGrade(examId, selectedSubmissionId.value, grades)
    ElMessage.success('批改完成')
    await fetchSubmissions()
    await fetchSubmissionDetail(selectedSubmissionId.value)
    await fetchStatistics()
  } catch {
    ElMessage.error('提交批改失败')
  } finally {
    submitting.value = false
  }
}

async function handlePublish() {
  publishing.value = true
  try {
    await publishScore(examId)
    ElMessage.success('成绩已发布')
    await fetchSubmissions()
  } catch {
    ElMessage.error('发布成绩失败')
  } finally {
    publishing.value = false
  }
}

function getStatusText(status: string) {
  const map: Record<string, string> = {
    SUBMITTED: '待批改',
    GRADED: '已批改',
    RETURNED: '已发放'
  }
  return map[status] || status
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
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
    return JSON.parse(optionsJson)
  } catch {
    return []
  }
}

function formatAnswer(answer: string | null, type: string, optionsJson: string | null): string {
  if (!answer) return '(未作答)'
  if (type === 'SINGLE' || type === 'MULTIPLE' || type === 'JUDGE') {
    const options = parseOptions(optionsJson)
    if (type === 'JUDGE') {
      return answer === 'T' ? '正确' : '错误'
    }
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
  background: #030303;
  min-height: 100vh;
  padding: 16px;
  font-family: 'JetBrains Mono', monospace;
}

.header-card {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
}

.header-card :deep(.el-card__header) {
  padding: 12px 20px;
  background: transparent !important;
  border-bottom: 1px solid #1a1a2e !important;
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
  background: #0a0a0a;
  border: 1px solid #1a1a2e;
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
  color: #e0e0e0 !important;
  margin-top: 4px;
  font-family: 'JetBrains Mono', monospace;
  opacity: 0.8;
}

.student-list-card {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px));
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.08) !important;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.student-list-card :deep(.el-card__header) {
  padding: 12px 16px;
  background: transparent !important;
  border-bottom: 1px solid #1a1a2e !important;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
}

.student-list-card :deep(.el-card__body) {
  background: transparent !important;
  padding: 0 !important;
}

.student-list-card :deep(.el-loading-mask) {
  background: rgba(3, 3, 3, 0.9) !important;
}

.student-item {
  padding: 12px 16px;
  border-bottom: 1px solid #1a1a2e;
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
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.student-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #e0e0e0 !important;
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
  border-color: #e0e0e0 !important;
  color: #e0e0e0 !important;
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
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px));
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.08) !important;
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

.answer-card :deep(.el-card__header) {
  padding: 12px 16px;
  background: transparent !important;
  border-bottom: 1px solid #1a1a2e !important;
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
  color: #e0e0e0 !important;
  padding: 40px 0;
  font-family: 'JetBrains Mono', monospace;
  opacity: 0.6;
}

.question-card {
  margin-bottom: 16px;
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 12px) 0, 100% 12px, 100% 100%, 12px 100%, 0 calc(100% - 12px));
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.05) !important;
}

.question-card :deep(.el-card__header) {
  padding: 10px 14px;
  background: rgba(26, 26, 46, 0.5) !important;
  border-bottom: 1px solid #1a1a2e !important;
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
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
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
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
  border-left: 3px solid;
}

.student-answer {
  border-left-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.1);
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
  border-bottom: 1px solid #1a1a2e !important;
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
  color: #e0e0e0 !important;
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
  --el-input-bg-color: #0a0a0a;
  --el-input-border-color: #1a1a2e;
  --el-input-text-color: #00ffff;
  --el-input-hover-border-color: #00ffff;
  --el-input-focus-border-color: #00ffff;
}

.grading-controls :deep(.el-input-number .el-input__wrapper) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.1) !important;
}

.grading-controls :deep(.el-input-number .el-input__inner) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.5);
}

.grading-controls :deep(.el-input-number__decrease),
.grading-controls :deep(.el-input-number__increase) {
  background: #0a0a0a !important;
  border-color: #1a1a2e !important;
  color: #00ffff !important;
}

.grading-controls :deep(.el-input-number__decrease:hover),
.grading-controls :deep(.el-input-number__increase:hover) {
  color: #ff10f0 !important;
}

.score-max {
  color: #e0e0e0 !important;
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
  color: #e0e0e0;
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
  background: #030303;
}

::-webkit-scrollbar-thumb {
  background: #1a1a2e;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #00ffff;
}
</style>
