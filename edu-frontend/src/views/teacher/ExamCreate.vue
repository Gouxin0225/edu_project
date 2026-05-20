<template>
  <div class="page">
    <el-card shadow="never" class="form-card cyber-card">
      <el-form :model="examForm" :rules="formRules" ref="examFormRef" label-width="100px">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="考试标题" prop="title">
              <el-input v-model="examForm.title" placeholder="请输入考试标题" clearable class="cyber-input" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目标班级" prop="targetClassIds">
              <el-select
                v-model="examForm.targetClassIds"
                multiple
                collapse-tags
                placeholder="请选择班级"
                style="width:100%"
                class="cyber-select"
              >
                <el-option
                  v-for="c in classList"
                  :key="c.id"
                  :label="c.className"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="考试时长" prop="duration">
              <el-input-number v-model="examForm.duration" :min="1" :max="300" style="width:100%" class="cyber-input-number" />
              <span class="unit-label">分钟</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="examForm.startTime"
                type="datetime"
                placeholder="选择开始时间"
                style="width:100%"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="cyber-date-picker"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="截止时间" prop="endTime">
              <el-date-picker
                v-model="examForm.endTime"
                type="datetime"
                placeholder="选择截止时间"
                style="width:100%"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="cyber-date-picker"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="及格分数" prop="passScore">
              <el-input-number v-model="examForm.passScore" :min="1" :max="examForm.totalScore || 100" style="width:100%" class="cyber-input-number" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="14">
        <el-card shadow="never" class="cyber-card">
          <template #header>
            <span class="cyber-section-title">题库浏览</span>
          </template>
          <div class="filter-bar">
            <el-select
              v-model="filter.courseCategory"
              placeholder="题目类型"
              clearable
              style="width:140px"
              @change="handleSearch"
              class="cyber-select"
            >
              <el-option v-for="c in COURSE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
            <el-select v-model="filter.type" placeholder="全部题型" clearable style="width:120px" @change="handleSearch" class="cyber-select">
              <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <el-select v-model="filter.difficulty" placeholder="全部难度" clearable style="width:120px" @change="handleSearch" class="cyber-select">
              <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
            <el-select v-model="filter.creatorId" placeholder="全部创建人" clearable style="width:140px" @change="handleSearch" class="cyber-select">
              <el-option v-for="u in creatorList" :key="u.id" :label="u.realName" :value="u.id" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="handleSearch" class="cyber-btn">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset" class="cyber-btn-secondary">重置</el-button>
          </div>

          <div class="batch-bar">
            <span class="batch-hint">已勾选 {{ tableSelection.length }} 题</span>
            <el-button
              type="primary"
              size="small"
              :disabled="tableSelection.length === 0"
              @click="addSelectedQuestions"
              class="cyber-btn-small"
            >批量加入试卷</el-button>
          </div>

          <el-table
            ref="questionTableRef"
            :data="tableData"
            v-loading="tableLoading"
            stripe
            border
            style="width:100%"
            @selection-change="handleSelectionChange"
            class="cyber-table"
          >
            <el-table-column type="selection" width="40" :selectable="isSelectable" />
            <el-table-column prop="knowledgePoint" label="知识点" min-width="120" show-overflow-tooltip />
            <el-table-column prop="courseCategory" label="课程方向" width="100" show-overflow-tooltip />
            <el-table-column label="题型" width="80">
              <template #default="{ row }">
                <el-tag :type="typeTagType(row.type)" size="small" class="cyber-tag">{{ typeLabel(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="难度" width="70">
              <template #default="{ row }">
                <el-tag :type="diffTagType(row.difficulty)" size="small" class="cyber-tag">{{ diffLabel(row.difficulty) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="!selectedIds.has(row.id)"
                  size="small"
                  type="primary"
                  @click="addQuestion(row)"
                  class="cyber-btn-text"
                >加入试卷</el-button>
                <el-tag v-else type="success" size="small" class="cyber-tag">已添加</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.size"
              :total="pagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @current-change="fetchQuestions"
              @size-change="(s: number) => { pagination.size = s; pagination.page = 1; fetchQuestions() }"
              class="cyber-pagination"
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <div class="cart-container">
          <el-card shadow="never" class="cart-card cyber-card-accent">
            <template #header>
              <div class="cart-header">
                <span class="cyber-section-title">已选题目</span>
                <el-tag type="info" class="cyber-tag">{{ selectedQuestions.length }} 题</el-tag>
              </div>
            </template>

            <div class="cart-stats">
              <span class="stats-label">总分：</span>
              <span :class="totalScore === 100 ? 'score-ok' : 'score-warning'">{{ totalScore }}</span>
              <span class="stats-unit">/ 100 分</span>
              <el-tag v-if="totalScore !== 100" type="danger" size="small" style="margin-left:8px" class="cyber-tag-danger">总分需为100</el-tag>
            </div>

            <el-table
              v-if="selectedQuestions.length > 0"
              :data="selectedQuestions"
              stripe
              size="small"
              style="width:100%"
              max-height="300"
              class="cyber-table"
            >
              <el-table-column label="序号" width="50" align="center">
                <template #default="{ $index }">{{ $index + 1 }}</template>
              </el-table-column>
              <el-table-column label="题型" width="70">
                <template #default="{ row }">
                  <el-tag :type="typeTagType(row.type)" size="small" class="cyber-tag">{{ typeLabel(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="难度" width="60">
                <template #default="{ row }">
                  <el-tag :type="diffTagType(row.difficulty)" size="small" class="cyber-tag">{{ diffLabel(row.difficulty) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="分值" width="120">
                <template #default="{ row }">
                  <el-input-number
                    v-model="row.score"
                    :min="0"
                    :max="100"
                    size="small"
                    controls-position="right"
                    class="cyber-input-number-small"
                    @change="(val: number) => handleScoreChange(row.id, val)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60" align="center">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    type="danger"
                    text
                    :icon="Delete"
                    @click="removeQuestion(row.id)"
                    class="cyber-btn-delete"
                  />
                </template>
              </el-table-column>
            </el-table>

            <el-empty v-else description="请从左侧题库添加题目" :image-size="60" class="cyber-empty" />

            <div class="cart-actions">
              <el-button
                type="warning"
                :icon="MagicStick"
                :disabled="selectedQuestions.length === 0 || !examId"
                :loading="autoScoreLoading"
                @click="handleAutoScore"
                class="cyber-btn-warning"
              >
                一键分配100分
              </el-button>
              <el-button
                type="primary"
                :icon="Promotion"
                :disabled="!canPublish"
                :loading="publishLoading"
                @click="handlePublish"
                class="cyber-btn"
              >
                发布考试
              </el-button>
              <el-button
                :disabled="selectedQuestions.length === 0"
                :loading="savingTemplate"
                @click="showSaveTemplateDialog = true"
                class="cyber-btn-save-template"
              >
                保存为模板
              </el-button>
            </div>
          </el-card>
        </div>
      </el-col>
    </el-row>

    <div class="bottom-bar cyber-bottom-bar">
      <el-button @click="handleBack" class="cyber-btn-secondary">返回列表</el-button>
      <el-button type="primary" :disabled="!canCreateExam" :loading="createLoading" @click="handleCreateExam" class="cyber-btn">
        创建考试
      </el-button>
    </div>

    <el-dialog v-model="showSaveTemplateDialog" title="保存为模板" width="450px" class="cyber-dialog">
      <el-form :model="templateForm" label-width="90px" class="cyber-form">
        <el-form-item label="模板名称" required>
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" class="cyber-input" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="templateForm.description" type="textarea" :rows="3" placeholder="请输入模板描述（可选）" class="cyber-textarea" />
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="templateForm.courseName" placeholder="请输入课程名称（可选）" class="cyber-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSaveTemplateDialog = false" class="cyber-btn-secondary">取消</el-button>
        <el-button type="primary" @click="handleSaveTemplate" :loading="savingTemplate" class="cyber-btn">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Delete, MagicStick, Promotion } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { QUESTION_TYPES, DIFFICULTIES, COURSE_CATEGORIES, getQuestionCreators } from '@/api/question'
import type { QuestionRecord, QuestionType, Difficulty } from '@/api/question'
import {
  getMyClasses, getQuestionList, createExam,
  addExamQuestions, removeExamQuestion, autoScore as autoScoreApi,
  updateQuestionScore, publishExam, getExamQuestions
} from '@/api/exam'
import type { ClassInfo, ExamQuestionItem } from '@/api/exam'
import { createTemplate } from '@/api/template'

const router = useRouter()

const QUESTION_TYPES_REF = QUESTION_TYPES
const DIFFICULTIES_REF = DIFFICULTIES

const examFormRef = ref()
const examForm = reactive({
  title: '',
  startTime: '',
  endTime: '',
  targetClassIds: [] as number[],
  duration: 90,
  totalScore: 100,
  passScore: 60
})

const formRules = {
  title: [{ required: true, message: '请输入考试标题', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
  targetClassIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一个班级', trigger: 'change' }]
}

const classList = ref<ClassInfo[]>([])

const filter = reactive({ courseCategory: '', type: '', difficulty: '', creatorId: '' })
const tableData = ref<QuestionRecord[]>([])
const tableLoading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })
const questionTableRef = ref()
const tableSelection = ref<QuestionRecord[]>([])
const creatorList = ref<{ id: number; realName: string; role: string }[]>([])

async function fetchQuestions() {
  tableLoading.value = true
  try {
    const res = await getQuestionList({
      page: pagination.page,
      size: pagination.size,
      courseCategory: filter.courseCategory || undefined,
      type: filter.type || undefined,
      difficulty: filter.difficulty || undefined,
      creatorId: filter.creatorId || undefined
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } finally {
    tableLoading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchQuestions()
}

function handleReset() {
  filter.courseCategory = ''
  filter.type = ''
  filter.difficulty = ''
  filter.creatorId = ''
  handleSearch()
}

function handleSelectionChange(rows: QuestionRecord[]) {
  tableSelection.value = rows
}

function isSelectable(row: QuestionRecord) {
  return !selectedIds.value.has(row.id)
}

async function addSelectedQuestions() {
  const toAdd = tableSelection.value.filter(row => !selectedIds.value.has(row.id))
  if (toAdd.length === 0) return

  try {
    if (examId.value) {
      await addExamQuestions(examId.value, toAdd.map(row => row.id))
      await fetchExamQuestions()
    } else {
      toAdd.forEach(row => {
        selectedQuestions.value.push({ ...row, score: 0 })
      })
    }
    questionTableRef.value?.clearSelection()
    ElMessage.success(`已添加 ${toAdd.length} 道题目`)
  } catch {
    ElMessage.error('添加题目失败')
  }
}

const selectedQuestions = ref<ExamQuestionItem[]>([])
const selectedIds = computed(() => new Set(selectedQuestions.value.map(q => q.id)))

async function addQuestion(row: QuestionRecord) {
  if (selectedIds.value.has(row.id)) return
  try {
    if (examId.value) {
      await addExamQuestions(examId.value, [row.id])
      await fetchExamQuestions()
    } else {
      selectedQuestions.value.push({ ...row, score: 0 })
    }
  } catch {
    ElMessage.error('添加题目失败')
  }
}

async function removeQuestion(questionId: number) {
  try {
    if (examId.value) {
      await removeExamQuestion(examId.value, questionId)
    }
    selectedQuestions.value = selectedQuestions.value.filter(q => q.id !== questionId)
  } catch {
    ElMessage.error('移除题目失败')
  }
}

async function handleScoreChange(questionId: number, score: number) {
  if (!examId.value) return
  try {
    await updateQuestionScore(examId.value, questionId, score)
  } catch {
    ElMessage.error('分值更新失败')
  }
}

const totalScore = computed(() =>
  selectedQuestions.value.reduce((sum, q) => sum + (q.score || 0), 0)
)

const examId = ref<number | null>(null)
const createLoading = ref(false)
const autoScoreLoading = ref(false)
const publishLoading = ref(false)
const savingTemplate = ref(false)
const showSaveTemplateDialog = ref(false)
const templateForm = reactive({
  name: '',
  description: '',
  courseName: ''
})

const canCreateExam = computed(() =>
  examForm.title &&
  examForm.startTime &&
  examForm.endTime &&
  examForm.targetClassIds.length > 0 &&
  selectedQuestions.value.length > 0 &&
  !examId.value
)

const canPublish = computed(() =>
  examId.value &&
  selectedQuestions.value.length > 0 &&
  totalScore.value === 100
)

async function handleCreateExam() {
  const valid = await examFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请先添加题目')
    return
  }

  createLoading.value = true
  try {
    const res = await createExam({
      title: examForm.title,
      startTime: examForm.startTime,
      endTime: examForm.endTime,
      targetClassIds: examForm.targetClassIds,
      duration: examForm.duration,
      totalScore: 100,
      passScore: examForm.passScore
    })
    examId.value = res.data.id
    ElMessage.success('考试创建成功')

    const qIds = selectedQuestions.value.map(q => q.id)
    await addExamQuestions(examId.value, qIds)
    const scoredQuestions = selectedQuestions.value.filter(q => (q.score || 0) > 0)
    await Promise.all(scoredQuestions.map(q => updateQuestionScore(examId.value!, q.id, q.score || 0)))
    await fetchExamQuestions()
    ElMessage.success('题目已添加到试卷')
  } catch {
    ElMessage.error('创建考试失败')
  } finally {
    createLoading.value = false
  }
}

async function handleAutoScore() {
  if (!examId.value) {
    ElMessage.warning('请先创建考试')
    return
  }
  autoScoreLoading.value = true
  try {
    await autoScoreApi(examId.value)
    ElMessage.success('100分智能分配完成')
    await fetchExamQuestions()
  } catch {
    ElMessage.error('智能分配失败')
  } finally {
    autoScoreLoading.value = false
  }
}

async function fetchExamQuestions() {
  if (!examId.value) return
  const res = await getExamQuestions(examId.value)
  const existingMap = new Map(selectedQuestions.value.map(q => [q.id, q]))
  selectedQuestions.value = (res.data || []).map((q) => {
    const existing = existingMap.get(q.questionId || q.id)
    return {
      ...(existing || {}),
      ...q,
      id: q.questionId || q.id,
      questionId: q.questionId || q.id,
      score: q.score || 0
    }
  })
}

async function handlePublish() {
  if (totalScore.value !== 100) {
    ElMessage.warning('总分必须为100分才能发布')
    return
  }
  if (!examId.value) return

  publishLoading.value = true
  try {
    await publishExam(examId.value)
    ElMessage.success('考试已发布')
    router.push('/teacher/exams')
  } catch {
    ElMessage.error('发布失败')
  } finally {
    publishLoading.value = false
  }
}

function handleBack() {
  router.push('/teacher/exams')
}

async function handleSaveTemplate() {
  if (!templateForm.name.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (selectedQuestions.value.length === 0) {
    ElMessage.warning('请先添加题目')
    return
  }
  
  savingTemplate.value = true
  try {
    await createTemplate({
      name: templateForm.name,
      description: templateForm.description,
      courseName: templateForm.courseName,
      questionIds: selectedQuestions.value.map(q => q.id),
      scores: selectedQuestions.value.map(q => q.score || 0),
      totalScore: totalScore.value
    })
    ElMessage.success('模板保存成功')
    showSaveTemplateDialog.value = false
    templateForm.name = ''
    templateForm.description = ''
    templateForm.courseName = ''
  } catch {
    ElMessage.error('保存模板失败')
  } finally {
    savingTemplate.value = false
  }
}

const typeLabel = (v: string) => QUESTION_TYPES.find(t => t.value === v)?.label ?? v
const diffLabel = (v: string) => DIFFICULTIES.find(d => d.value === v)?.label ?? v
const typeTagType = (v: string): any =>
  ({ SINGLE: '', MULTIPLE: 'success', JUDGE: 'warning', SHORT: 'info', CODE: 'danger' }[v] ?? '')
const diffTagType = (v: string): any =>
  ({ EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[v] ?? '')

onMounted(async () => {
  fetchQuestions()
  try {
    const res = await getMyClasses()
    classList.value = res.data || []
  } catch {
    ElMessage.error('获取班级列表失败')
  }
  const creatorRes = await getQuestionCreators()
  creatorList.value = creatorRes.data || []
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 80px;
  background: #030303 !important;
  min-height: 100vh;
  padding: 16px;
  font-family: 'JetBrains Mono', monospace !important;
}

.form-card :deep(.el-card__body) {
  padding-bottom: 10px;
}

.cyber-card {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px)) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1) 0%, transparent 100%) !important;
  border-bottom: 1px solid #1a1a2e !important;
  padding: 16px 20px !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-card__body) {
  background: #0a0a0a !important;
  padding: 20px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card-accent {
  background: #0a0a0a !important;
  border: 1px solid #ff10f0 !important;
  box-shadow: 0 0 20px rgba(255, 16, 240, 0.15), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px)) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card-accent :deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(255, 16, 240, 0.15) 0%, transparent 100%) !important;
  border-bottom: 1px solid #ff10f0 !important;
  padding: 16px 20px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card-accent :deep(.el-card__body) {
  background: #0a0a0a !important;
  padding: 20px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-section-title {
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 16px !important;
  font-weight: 700 !important;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
  letter-spacing: 2px !important;
}

.cyber-card-accent .cyber-section-title {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

.cyber-table {
  background: #0a0a0a !important;
  font-family: 'JetBrains Mono', monospace !important;
  color: #e0e0e0 !important;
}

.cyber-table :deep(.el-table__header-wrapper th) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.15) 0%, rgba(255, 16, 240, 0.05) 100%) !important;
  color: #00ffff !important;
  border-bottom: 1px solid #1a1a2e !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
  text-transform: uppercase !important;
  font-size: 12px !important;
}

.cyber-table :deep(.el-table__body-wrapper tr) {
  background: #0a0a0a !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table :deep(.el-table__body-wrapper td) {
  background: #0a0a0a !important;
  border-bottom: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table :deep(.el-table__body-wrapper tr:hover > td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.cyber-table :deep(.el-table__body-wrapper .el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.3) !important;
}

.cyber-table :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.3) !important;
}

.cyber-table :deep(.el-table__empty-text) {
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table :deep(.el-checkbox__inner) {
  background: #0a0a0a !important;
  border-color: #1a1a2e !important;
}

.cyber-table :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-table :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #00ffff !important;
}

.cyber-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: rgba(0, 0, 0, 0.5) !important;
  border: 1px solid currentColor !important;
  box-shadow: 0 0 8px currentColor !important;
}

.cyber-tag-danger {
  font-family: 'JetBrains Mono', monospace !important;
  background: rgba(255, 16, 240, 0.1) !important;
  border: 1px solid #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 8px rgba(255, 16, 240, 0.3) !important;
}

.cyber-btn {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  border: none !important;
  color: #0a0a0a !important;
  font-weight: 700 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.4) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn:hover {
  box-shadow: 0 0 25px rgba(0, 255, 255, 0.6) !important;
  transform: translateY(-2px) !important;
}

.cyber-btn-secondary {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
  font-weight: 600 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-secondary:hover {
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-btn-small {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  border: none !important;
  color: #0a0a0a !important;
  font-weight: 600 !important;
  font-size: 12px !important;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.4) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-small:hover:not(:disabled) {
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.6) !important;
  transform: translateY(-1px) !important;
}

.cyber-btn-small:disabled {
  background: #1a1a2e !important;
  color: #606266 !important;
  box-shadow: none !important;
}

.cyber-btn-warning {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, #ff9800 0%, #cc7a00 100%) !important;
  border: none !important;
  color: #0a0a0a !important;
  font-weight: 700 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  box-shadow: 0 0 15px rgba(255, 152, 0, 0.4) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-warning:hover:not(:disabled) {
  box-shadow: 0 0 25px rgba(255, 152, 0, 0.6) !important;
  transform: translateY(-2px) !important;
}

.cyber-btn-warning:disabled {
  background: #1a1a2e !important;
  color: #606266 !important;
  box-shadow: none !important;
}

.cyber-btn-text {
  font-family: 'JetBrains Mono', monospace !important;
  color: #00ffff !important;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.5) !important;
  transition: all 0.3s ease !important;
  font-size: 12px !important;
}

.cyber-btn-text:hover {
  color: #39ff14 !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.8) !important;
}

.cyber-btn-delete {
  font-family: 'JetBrains Mono', monospace !important;
  color: #ff10f0 !important;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-delete:hover {
  color: #ff4444 !important;
  text-shadow: 0 0 10px rgba(255, 68, 68, 0.8) !important;
}

.cyber-btn-save-template {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, #ff10f0 0%, #cc0dc0 100%) !important;
  border: none !important;
  color: #ffffff !important;
  font-weight: 700 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.4) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-save-template:hover:not(:disabled) {
  box-shadow: 0 0 25px rgba(255, 16, 240, 0.6) !important;
  transform: translateY(-2px) !important;
}

.cyber-btn-save-template:disabled {
  background: #1a1a2e !important;
  color: #606266 !important;
  box-shadow: none !important;
}

.cyber-pagination {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pagination__total) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pager li) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pager li:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

.cyber-pagination :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  color: #0a0a0a !important;
  font-weight: 700 !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.5) !important;
}

.cyber-pagination :deep(.btn-prev), .cyber-pagination :deep(.btn-next) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
}

.cyber-pagination :deep(.btn-prev:hover), .cyber-pagination :deep(.btn-next:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.batch-hint {
  font-size: 13px;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace !important;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5) !important;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.cart-container {
  position: sticky;
  top: 16px;
}

.cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cart-stats {
  padding: 12px 0;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #ff10f0 !important;
  margin-bottom: 12px;
  font-family: 'JetBrains Mono', monospace !important;
}

.stats-label {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.stats-unit {
  color: #909399 !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 14px !important;
}

.score-ok {
  color: #39ff14 !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.5) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.score-warning {
  color: #ff9800 !important;
  text-shadow: 0 0 10px rgba(255, 152, 0, 0.5) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.unit-label {
  margin-left: 8px;
  color: #ff10f0 !important;
  font-size: 13px;
  font-family: 'JetBrains Mono', monospace !important;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5) !important;
}

.cart-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  justify-content: center;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 24px;
  background: linear-gradient(135deg, rgba(10, 10, 10, 0.95) 0%, rgba(3, 3, 3, 0.98) 100%) !important;
  border-top: 1px solid #1a1a2e !important;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  z-index: 100;
  box-shadow: 0 -5px 20px rgba(0, 255, 255, 0.1), 0 -5px 40px rgba(255, 16, 240, 0.05) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-empty :deep(.el-empty__description) {
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.5) !important;
}

.cyber-empty :deep(.el-empty__image svg) {
  fill: #39ff14 !important;
  filter: drop-shadow(0 0 10px rgba(57, 255, 20, 0.5)) !important;
}

.cyber-input :deep(.el-input__wrapper) {
  background-color: #0a0a0a !important;
  box-shadow: 0 0 0 1px #1a1a2e inset !important;
  transition: all 0.3s ease !important;
}

.cyber-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff inset !important;
}

.cyber-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #00ffff, 0 0 15px rgba(0, 255, 255, 0.3) inset !important;
}

.cyber-input :deep(.el-input__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-input :deep(.el-input__inner::placeholder) {
  color: rgba(224, 224, 224, 0.4) !important;
}

.cyber-select :deep(.el-input__wrapper) {
  background-color: #0a0a0a !important;
  box-shadow: 0 0 0 1px #1a1a2e inset !important;
  transition: all 0.3s ease !important;
}

.cyber-select :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff inset !important;
}

.cyber-select :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #00ffff, 0 0 15px rgba(0, 255, 255, 0.3) inset !important;
}

.cyber-select :deep(.el-input__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select-dropdown) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
}

.cyber-select :deep(.el-select-dropdown__item) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select-dropdown__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
}

.cyber-select :deep(.el-select-dropdown__item.is-selected) {
  background: rgba(255, 16, 240, 0.2) !important;
  color: #ff10f0 !important;
}

.cyber-input-number :deep(.el-input__wrapper) {
  background-color: #0a0a0a !important;
  box-shadow: 0 0 0 1px #1a1a2e inset !important;
  transition: all 0.3s ease !important;
}

.cyber-input-number :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff inset !important;
}

.cyber-input-number :deep(.el-input-number__decrease),
.cyber-input-number :deep(.el-input-number__increase) {
  background: #1a1a2e !important;
  color: #00ffff !important;
  border-color: #1a1a2e !important;
}

.cyber-input-number :deep(.el-input-number__decrease:hover),
.cyber-input-number :deep(.el-input-number__increase:hover) {
  color: #ff10f0 !important;
}

.cyber-input-number :deep(.el-input__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-input-number-small :deep(.el-input__wrapper) {
  background-color: #0a0a0a !important;
  box-shadow: 0 0 0 1px #1a1a2e inset !important;
  transition: all 0.3s ease !important;
  padding: 0 5px !important;
}

.cyber-input-number-small :deep(.el-input__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 12px !important;
}

.cyber-input-number-small :deep(.el-input-number__decrease),
.cyber-input-number-small :deep(.el-input-number__increase) {
  background: #1a1a2e !important;
  color: #00ffff !important;
  border-color: #1a1a2e !important;
  width: 18px !important;
  font-size: 10px !important;
}

.cyber-input-number-small :deep(.el-input-number__decrease:hover),
.cyber-input-number-small :deep(.el-input-number__increase:hover) {
  color: #ff10f0 !important;
}

.cyber-date-picker :deep(.el-input__wrapper) {
  background-color: #0a0a0a !important;
  box-shadow: 0 0 0 1px #1a1a2e inset !important;
  transition: all 0.3s ease !important;
}

.cyber-date-picker :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff inset !important;
}

.cyber-date-picker :deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #00ffff, 0 0 15px rgba(0, 255, 255, 0.3) inset !important;
}

.cyber-date-picker :deep(.el-input__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-picker-panel) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-date-picker__header-label) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-date-table th) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-date-table td.available:hover .el-date-table-cell__text) {
  background: rgba(0, 255, 255, 0.1) !important;
}

.cyber-date-picker :deep(.el-date-table td.current:not(.disabled) .el-date-table-cell__text) {
  background: #00ffff !important;
  color: #0a0a0a !important;
}

.cyber-date-picker :deep(.el-picker-panel__icon-btn) {
  color: #00ffff !important;
}

.cyber-date-picker :deep(.el-time-panel) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-time-spinner__item) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-date-picker :deep(.el-time-spinner__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
}

.cyber-date-picker :deep(.el-time-spinner__item.is-active) {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

.cyber-textarea :deep(.el-textarea__inner) {
  background-color: #0a0a0a !important;
  box-shadow: 0 0 0 1px #1a1a2e inset !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
  transition: all 0.3s ease !important;
}

.cyber-textarea :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #00ffff inset !important;
}

.cyber-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px #00ffff, 0 0 15px rgba(0, 255, 255, 0.3) inset !important;
}

.cyber-card :deep(.el-form-item__label) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
}

.cyber-card :deep(.el-form-item__error) {
  color: #ff9800 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-tag--primary) {
  background: rgba(0, 255, 255, 0.1) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3) !important;
}

.cyber-card :deep(.el-tag--success) {
  background: rgba(57, 255, 20, 0.1) !important;
  border-color: #39ff14 !important;
  color: #39ff14 !important;
  box-shadow: 0 0 8px rgba(57, 255, 20, 0.3) !important;
}

.cyber-card :deep(.el-tag--warning) {
  background: rgba(255, 152, 0, 0.1) !important;
  border-color: #ff9800 !important;
  color: #ff9800 !important;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.3) !important;
}

.cyber-card :deep(.el-tag--danger) {
  background: rgba(255, 16, 240, 0.1) !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 8px rgba(255, 16, 240, 0.3) !important;
}

.cyber-card :deep(.el-tag--info) {
  background: rgba(224, 224, 224, 0.1) !important;
  border-color: #e0e0e0 !important;
  color: #e0e0e0 !important;
  box-shadow: 0 0 8px rgba(224, 224, 224, 0.2) !important;
}

.cyber-card :deep(.el-loading-mask) {
  background: rgba(3, 3, 3, 0.9) !important;
}

.cyber-card :deep(.el-loading-spinner .el-loading-text) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-loading-spinner .path) {
  stroke: #00ffff !important;
}

.cyber-card :deep(.el-message-box) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.2) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-message-box__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-message-box__content) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-overlay) {
  background: rgba(3, 3, 3, 0.85) !important;
}

.cyber-card :deep(.el-dialog) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.2) !important;
}

.cyber-card :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1) 0%, rgba(255, 16, 240, 0.05) 100%) !important;
  border-bottom: 1px solid #1a1a2e !important;
  padding: 20px !important;
}

.cyber-card :deep(.el-dialog__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-dialog__body) {
  background: #0a0a0a !important;
  color: #e0e0e0 !important;
  padding: 24px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-dialog__footer) {
  background: #0a0a0a !important;
  border-top: 1px solid #1a1a2e !important;
  padding: 16px 20px !important;
}

.cyber-card :deep(.el-popover.el-popper) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-popover.el-popper.is-light) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
}

.cyber-card :deep(.el-popover__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-tooltip__popper.is-dark) {
  background: linear-gradient(135deg, #0a0a0a 0%, #1a1a2e 100%) !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-card :deep(.el-slider__runway) {
  background: #1a1a2e !important;
}

.cyber-card :deep(.el-slider__bar) {
  background: linear-gradient(90deg, #00ffff 0%, #ff10f0 100%) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-card :deep(.el-slider__button) {
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-card :deep(.el-progress__text) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-progress-bar__outer) {
  background: #1a1a2e !important;
}

.cyber-card :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #00ffff 0%, #ff10f0 100%) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}
</style>
