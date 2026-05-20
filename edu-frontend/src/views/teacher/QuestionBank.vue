<template>
  <div class="page">
    <el-card shadow="never" class="cyber-card">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select
          v-model="filter.courseCategory"
          placeholder="题目类型"
          clearable
          style="width:140px"
          @change="handleSearch"
        >
          <el-option v-for="c in COURSE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
        </el-select>
        <el-select v-model="filter.type" placeholder="全部题型" clearable style="width:120px" @change="handleSearch">
          <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="filter.difficulty" placeholder="全部难度" clearable style="width:120px" @change="handleSearch">
          <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
        <el-select v-model="filter.creatorId" placeholder="全部创建人" clearable style="width:140px" @change="handleSearch">
          <el-option v-for="u in creatorList" :key="u.id" :label="u.realName" :value="u.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch" class="cyber-btn cyber-btn-primary">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset" class="cyber-btn">重置</el-button>
        <div class="flex-spacer" />
        <el-button :icon="Plus" @click="openFormDialog()" class="cyber-btn cyber-btn-accent">手动新建</el-button>
        <el-button :icon="Upload" @click="showImportDialog = true" class="cyber-btn">Excel 导入</el-button>
        <el-button type="success" :icon="MagicStick" @click="showAiDialog = true" class="cyber-btn cyber-btn-success">AI 出题</el-button>
      </div>

      <!-- 题目列表 -->
      <el-table :data="tableData" v-loading="tableLoading" stripe border style="width:100%" class="cyber-table">
        <el-table-column prop="knowledgePoint" label="知识点" min-width="140" show-overflow-tooltip />
        <el-table-column prop="courseCategory" label="课程方向" width="120" show-overflow-tooltip />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" class="cyber-tag">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="80">
          <template #default="{ row }">
            <el-tag :type="diffTagType(row.difficulty)" size="small" class="cyber-tag">{{ diffLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题干" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="content-preview">{{ row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column label="AI生成" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isAiGenerated === 1" type="warning" size="small" class="cyber-tag cyber-tag-ai">AI</el-tag>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" text @click="openFormDialog(row)" class="cyber-btn-text">编辑</el-button>
            <el-button
              v-if="userRole === 'ADMIN' && !row.isPublic"
              size="small" type="success" text @click="handlePublish(row)"
              class="cyber-btn-text"
            >公开</el-button>
            <el-button size="small" type="danger" text @click="handleDelete(row)" class="cyber-btn-text cyber-btn-danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchList"
          @size-change="(s) => { pagination.size = s; pagination.page = 1; fetchList() }"
          class="cyber-pagination"
        />
      </div>
    </el-card>

    <!-- ═══════════════════════════════════════════
         新建 / 编辑题目弹窗
    ═══════════════════════════════════════════ -->
    <el-dialog
      v-model="showFormDialog"
      :title="formMode === 'create' ? '新建题目' : '编辑题目'"
      width="700px"
      :close-on-click-modal="false"
      @closed="resetForm"
      class="cyber-dialog"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="90px"
        label-position="right"
      >
        <!-- 通用字段 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="题目类型" prop="courseCategory">
              <el-select v-model="form.courseCategory" style="width:100%" placeholder="请选择题目类型">
                <el-option v-for="c in COURSE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="知识点" prop="knowledgePoint">
              <el-input v-model="form.knowledgePoint" placeholder="如：异常处理" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="题型" prop="type">
              <el-select v-model="form.type" style="width:100%" @change="handleTypeChange">
                <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度" prop="difficulty">
              <el-select v-model="form.difficulty" style="width:100%">
                <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 题干 -->
        <el-form-item label="题干" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="3"
            placeholder="请输入题目内容"
          />
        </el-form-item>

        <!-- ── 单选 / 多选：选项 + 答案 ── -->
        <template v-if="form.type === 'SINGLE' || form.type === 'MULTIPLE'">
          <el-form-item label="选项" prop="options">
            <div style="width:100%">
              <div
                v-for="(opt, idx) in form.options"
                :key="idx"
                class="option-row"
              >
                <span class="option-label">{{ optionLetter(idx) }}</span>
                <el-input v-model="form.options[idx]" :placeholder="`选项 ${optionLetter(idx)}`" clearable />
                <el-button
                  :disabled="form.options.length <= 2"
                  type="danger" text :icon="Delete"
                  @click="removeOption(idx)"
                />
              </div>
              <el-button
                :disabled="form.options.length >= 8"
                size="small" :icon="Plus"
                @click="form.options.push('')"
                class="cyber-btn-small"
              >添加选项</el-button>
            </div>
          </el-form-item>

          <!-- 单选答案 -->
          <el-form-item v-if="form.type === 'SINGLE'" label="正确答案" prop="singleAnswer">
            <el-radio-group v-model="form.singleAnswer" class="cyber-radio-group">
              <el-radio
                v-for="(_, idx) in form.options"
                :key="idx"
                :value="optionLetter(idx)"
                class="cyber-radio"
              >{{ optionLetter(idx) }}</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 多选答案 -->
          <el-form-item v-if="form.type === 'MULTIPLE'" label="正确答案" prop="multipleAnswer">
            <el-checkbox-group v-model="form.multipleAnswer" class="cyber-checkbox-group">
              <el-checkbox
                v-for="(_, idx) in form.options"
                :key="idx"
                :value="optionLetter(idx)"
                class="cyber-checkbox"
              >{{ optionLetter(idx) }}</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </template>

        <!-- ── 判断题：答案 ── -->
        <template v-if="form.type === 'JUDGE'">
          <el-form-item label="正确答案" prop="judgeAnswer">
            <el-radio-group v-model="form.judgeAnswer" class="cyber-radio-group">
              <el-radio value="true" class="cyber-radio">正确</el-radio>
              <el-radio value="false" class="cyber-radio">错误</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- ── 简答 / 代码：参考答案 ── -->
        <template v-if="form.type === 'SHORT' || form.type === 'CODE'">
          <el-form-item label="参考答案" prop="standardAnswer">
            <el-input
              v-model="form.standardAnswer"
              type="textarea"
              :rows="form.type === 'CODE' ? 6 : 4"
              :placeholder="form.type === 'CODE' ? '请输入参考代码' : '请输入参考答案'"
              :style="form.type === 'CODE' ? 'font-family:monospace' : ''"
            />
          </el-form-item>
        </template>

        <!-- 解析（通用） -->
        <el-form-item label="解析">
          <el-input
            v-model="form.analysis"
            type="textarea"
            :rows="2"
            placeholder="选填：题目解析或解题思路"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showFormDialog = false" class="cyber-btn">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="submitForm" class="cyber-btn cyber-btn-primary">
          {{ formMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ═══════════════════════════════════════════
         AI 出题弹窗
    ═══════════════════════════════════════════ -->
    <el-dialog
      v-model="showAiDialog"
      title="AI 智能出题"
      width="480px"
      :close-on-click-modal="false"
      @closed="aiFormRef?.resetFields()"
      class="cyber-dialog"
    >
      <el-form ref="aiFormRef" :model="aiForm" :rules="aiRules" label-width="90px">
        <el-form-item label="题目类型" prop="courseCategory">
          <el-select v-model="aiForm.courseCategory" style="width:100%" placeholder="请选择题目类型">
            <el-option v-for="c in COURSE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点" prop="knowledgePoint">
          <el-input v-model="aiForm.knowledgePoint" placeholder="可选，如：Java异常处理" clearable />
        </el-form-item>
        <el-form-item label="题型" prop="types">
          <el-select
            v-model="aiForm.types"
            multiple
            collapse-tags
            placeholder="请选择（可多选）"
            style="width:100%"
          >
            <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="aiForm.difficulty" style="width:100%">
            <el-option v-for="d in DIFFICULTIES" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="生成数量" prop="count">
          <el-input-number
            v-model="aiForm.count"
            :min="1" :max="20" :step="1"
            controls-position="right"
            style="width:140px"
          />
          <el-text type="info" size="small" style="margin-left:8px">最多 20 题</el-text>
        </el-form-item>
      </el-form>

      <!-- 生成中提示 -->
      <div v-if="aiLoading" class="ai-loading-tip">
        <el-icon class="ai-spin"><Loading /></el-icon>
        <span>AI 正在生成题目，通常需要 15～40 秒，请耐心等待{{ aiDots }}</span>
      </div>

      <template #footer>
        <el-button :disabled="aiLoading" @click="showAiDialog = false" class="cyber-btn">取消</el-button>
        <el-button type="success" :loading="aiLoading" :icon="MagicStick" @click="submitAiGenerate" class="cyber-btn cyber-btn-success">
          {{ aiLoading ? '生成中...' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ═══════════════════════════════════════════
         Excel 导入弹窗
    ═══════════════════════════════════════════ -->
    <el-dialog
      v-model="showImportDialog"
      title="批量导入题目"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetImport"
      class="cyber-dialog"
    >
      <div class="template-tip">
        <el-icon color="#00ffff"><InfoFilled /></el-icon>
        <span>请按模板格式填写，</span>
        <el-button type="primary" link :icon="Download" @click="downloadTemplate" class="cyber-link">下载导入模板</el-button>
      </div>
      <el-upload
        ref="importUploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="(f: any) => { importFile = f.raw; importResult = null }"
        :on-exceed="() => ElMessage.warning('只能上传一个文件')"
        drag
        class="cyber-upload"
      >
        <el-icon :size="40" color="#00ffff"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 拖到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls</div>
        </template>
      </el-upload>

      <template v-if="importResult">
        <el-divider class="cyber-divider" />
        <el-descriptions :column="2" border class="cyber-descriptions">
          <el-descriptions-item label="成功">
            <el-text type="success" class="cyber-text-success">{{ importResult.success }} 条</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="失败">
            <el-text type="danger" class="cyber-text-danger">{{ importResult.failed }} 条</el-text>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="importResult.errors.length" class="error-list">
          <div class="error-title">错误详情：</div>
          <el-scrollbar max-height="140px">
            <div v-for="(e, i) in importResult.errors" :key="i" class="error-item">
              <el-icon color="#ff10f0"><Warning /></el-icon> {{ e }}
            </div>
          </el-scrollbar>
        </div>
      </template>

      <template #footer>
        <el-button @click="showImportDialog = false" class="cyber-btn">关闭</el-button>
        <el-button type="primary" :loading="importLoading" :disabled="!importFile" @click="submitImport" class="cyber-btn cyber-btn-primary">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadInstance } from 'element-plus'
import {
  Search, Refresh, Plus, Upload, Delete,
  MagicStick, InfoFilled, Download, UploadFilled, Warning, Loading
} from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
import {
  QUESTION_TYPES, COURSE_CATEGORIES, DIFFICULTIES,
  getQuestionList, createQuestion, updateQuestion,
  deleteQuestion, importQuestions, aiGenerateQuestions, publishQuestion,
  getQuestionCreators
} from '@/api/question'
import type { QuestionRecord, QuestionType, Difficulty, CourseCategory } from '@/api/question'
import { useUserStore } from '@/stores/user'

const userRole = computed(() => useUserStore().userInfo?.role ?? '')

// ─────────── 工具函数 ───────────
const optionLetter = (i: number) => String.fromCharCode(65 + i)  // 0→A, 1→B …

const typeLabel = (v: string) => QUESTION_TYPES.find(t => t.value === v)?.label ?? v
const diffLabel = (v: string) => DIFFICULTIES.find(d => d.value === v)?.label ?? v

const typeTagType = (v: string): any =>
  ({ SINGLE: '', MULTIPLE: 'success', JUDGE: 'warning', SHORT: 'info', CODE: 'danger' }[v] ?? '')

const diffTagType = (v: string): any =>
  ({ EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[v] ?? '')

const formatTime = (t: string) => t ? t.replace('T', ' ').slice(0, 16) : '-'

// ─────────── 筛选 + 列表 ───────────
const filter = reactive({ courseCategory: '', type: '', difficulty: '', creatorId: '' })
const tableData = ref<QuestionRecord[]>([])
const tableLoading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })
const creatorList = ref<{ id: number; realName: string; role: string }[]>([])

async function fetchList() {
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

function handleSearch() { pagination.page = 1; fetchList() }
function handleReset() {
  filter.courseCategory = ''
  filter.type = ''
  filter.difficulty = ''
  filter.creatorId = ''
  handleSearch()
}

// ─────────── 新建/编辑表单 ───────────
type FormMode = 'create' | 'edit'

interface QuestionForm {
  courseCategory: CourseCategory | ''
  knowledgePoint: string
  type: QuestionType | ''
  difficulty: Difficulty | ''
  content: string
  options: string[]         // SINGLE / MULTIPLE
  singleAnswer: string      // SINGLE
  multipleAnswer: string[]  // MULTIPLE
  judgeAnswer: string       // JUDGE
  standardAnswer: string    // SHORT / CODE
  analysis: string
}

const showFormDialog = ref(false)
const formMode = ref<FormMode>('create')
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const defaultForm = (): QuestionForm => ({
  courseCategory: '', knowledgePoint: '',
  type: 'SINGLE', difficulty: 'MEDIUM',
  content: '',
  options: ['', '', '', ''],
  singleAnswer: '', multipleAnswer: [],
  judgeAnswer: '', standardAnswer: '',
  analysis: ''
})

const courseCategoryOptions = COURSE_CATEGORIES

const form = reactive<QuestionForm>(defaultForm())

// 动态校验规则
const formRules = computed<FormRules>(() => ({
  courseCategory: [{ required: true, message: '请选择题目类型', trigger: 'change' }],
  knowledgePoint: [{ required: true, message: '请输入知识点', trigger: 'blur' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  options: form.type === 'SINGLE' || form.type === 'MULTIPLE'
    ? [{ validator: validateOptions, trigger: 'blur' }] : [],
  singleAnswer: form.type === 'SINGLE'
    ? [{ required: true, message: '请选择正确答案', trigger: 'change' }] : [],
  multipleAnswer: form.type === 'MULTIPLE'
    ? [{ validator: validateMultiple, trigger: 'change' }] : [],
  judgeAnswer: form.type === 'JUDGE'
    ? [{ required: true, message: '请选择答案', trigger: 'change' }] : []
}))

function validateOptions(_rule: any, _val: any, cb: Function) {
  const filled = form.options.filter(o => o.trim())
  if (filled.length < 2) return cb(new Error('至少填写 2 个选项'))
  cb()
}
function validateMultiple(_rule: any, _val: any, cb: Function) {
  if (!form.multipleAnswer.length) return cb(new Error('请至少选择 1 个正确答案'))
  cb()
}

function openFormDialog(row?: QuestionRecord) {
  Object.assign(form, defaultForm())
  if (row) {
    formMode.value = 'edit'
    editingId.value = row.id
    form.courseCategory = row.courseCategory
    form.knowledgePoint = row.knowledgePoint
    form.type = row.type
    form.difficulty = row.difficulty
    form.content = row.content
    form.analysis = row.analysis ?? ''
    // 解析选项
    if (row.type === 'SINGLE' || row.type === 'MULTIPLE') {
      try {
        form.options = JSON.parse(row.optionsJson ?? '["","","",""]')
      } catch { form.options = ['', '', '', ''] }
      if (row.type === 'SINGLE') {
        form.singleAnswer = row.standardAnswer
      } else {
        form.multipleAnswer = row.standardAnswer ? row.standardAnswer.split('') : []
      }
    } else if (row.type === 'JUDGE') {
      form.judgeAnswer = row.standardAnswer
    } else {
      form.standardAnswer = row.standardAnswer
    }
  } else {
    formMode.value = 'create'
    editingId.value = null
  }
  showFormDialog.value = true
}

function handleTypeChange() {
  form.options = ['', '', '', '']
  form.singleAnswer = ''
  form.multipleAnswer = []
  form.judgeAnswer = ''
  form.standardAnswer = ''
}

function removeOption(idx: number) {
  form.options.splice(idx, 1)
  // 移除答案中对应字母
  const letter = optionLetter(idx)
  if (form.singleAnswer === letter) form.singleAnswer = ''
  form.multipleAnswer = form.multipleAnswer
    .filter(a => a !== letter)
    .map(a => a > letter ? String.fromCharCode(a.charCodeAt(0) - 1) : a)
}

function buildPayload() {
  const type = form.type as QuestionType
  let optionsJson: string | undefined
  let standardAnswer = ''

  if (type === 'SINGLE' || type === 'MULTIPLE') {
    optionsJson = JSON.stringify(form.options.filter(o => o.trim()))
    standardAnswer = type === 'SINGLE'
      ? form.singleAnswer
      : [...form.multipleAnswer].sort().join('')
  } else if (type === 'JUDGE') {
    standardAnswer = form.judgeAnswer
  } else {
    standardAnswer = form.standardAnswer
  }

  return {
    courseCategory: form.courseCategory,
    knowledgePoint: form.knowledgePoint,
    type,
    difficulty: form.difficulty as Difficulty,
    content: form.content,
    optionsJson,
    standardAnswer,
    analysis: form.analysis || undefined
  }
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  formLoading.value = true
  try {
    const payload = buildPayload()
    if (formMode.value === 'create') {
      await createQuestion(payload)
      ElMessage.success('题目创建成功')
    } else {
      await updateQuestion(editingId.value!, payload)
      ElMessage.success('题目保存成功')
    }
    showFormDialog.value = false
    fetchList()
  } finally {
    formLoading.value = false
  }
}

function resetForm() {
  formRef.value?.clearValidate()
  Object.assign(form, defaultForm())
}

// ─────────── 删除 ───────────
async function handleDelete(row: QuestionRecord) {
  await ElMessageBox.confirm(
    `确定删除题目「${row.knowledgePoint}」吗？`,
    '删除确认',
    { type: 'error', confirmButtonText: '确定删除', cancelButtonText: '取消' }
  )
  await deleteQuestion(row.id)
  ElMessage.success('删除成功')
  fetchList()
}

// ─────────── 公开题目（ADMIN） ───────────
async function handlePublish(row: QuestionRecord) {
  await ElMessageBox.confirm('确定将此题目公开给所有人使用？', '公开题目', { type: 'warning' })
  await publishQuestion(row.id)
  ElMessage.success('已公开')
  row.isPublic = 1
}

// ─────────── AI 出题 ───────────
const showAiDialog = ref(false)
const aiLoading = ref(false)
const aiFormRef = ref<FormInstance>()
const aiDots = ref('')
let aiDotsTimer: ReturnType<typeof setInterval> | null = null

function startAiDots() {
  aiDots.value = ''
  aiDotsTimer = setInterval(() => {
    aiDots.value = aiDots.value.length >= 3 ? '' : aiDots.value + '.'
  }, 600)
}
function stopAiDots() {
  if (aiDotsTimer) { clearInterval(aiDotsTimer); aiDotsTimer = null }
  aiDots.value = ''
}
const aiForm = reactive({ courseCategory: '' as CourseCategory | '', knowledgePoint: '', types: [] as QuestionType[], difficulty: 'MEDIUM' as Difficulty, count: 5 })
const aiRules: FormRules = {
  courseCategory: [{ required: true, message: '请选择题目类型', trigger: 'change' }],
  types: [{ required: true, type: 'array', min: 1, message: '请至少选择一种题型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

async function submitAiGenerate() {
  const valid = await aiFormRef.value?.validate().catch(() => false)
  if (!valid) return
  aiLoading.value = true
  startAiDots()
  try {
    const res = await aiGenerateQuestions({
      courseCategory: aiForm.courseCategory,
      knowledgePoint: aiForm.knowledgePoint,
      types: aiForm.types,
      difficulty: aiForm.difficulty,
      count: aiForm.count
    })
    ElMessage.success(`已成功生成 ${res.data.count} 道题目`)
    showAiDialog.value = false
    fetchList()
  } finally {
    aiLoading.value = false
    stopAiDots()
  }
}

// ─────────── Excel 导入 ───────────
const showImportDialog = ref(false)
const importLoading = ref(false)
const importFile = ref<File | null>(null)
const importResult = ref<{ success: number; failed: number; errors: string[] } | null>(null)
const importUploadRef = ref<UploadInstance>()

function resetImport() {
  importUploadRef.value?.clearFiles()
  importFile.value = null
  importResult.value = null
}

async function submitImport() {
  if (!importFile.value) return
  importLoading.value = true
  try {
    const res = await importQuestions(importFile.value)
    importResult.value = res.data
    if (res.data.success > 0) fetchList()
  } finally {
    importLoading.value = false
  }
}

function downloadTemplate() {
  const headers = [['课程方向', '知识点', '题型', '难度', '题干', '选项A', '选项B', '选项C', '选项D', '正确答案', '解析']]
  const samples = [
    ['Java', 'try-catch', 'SINGLE', 'MEDIUM', '以下哪个关键字用于捕获异常？', 'try', 'catch', 'finally', 'throw', 'B', ''],
    ['Java', '异常类型', 'JUDGE', 'EASY', 'RuntimeException是受检异常', '', '', '', '', 'false', 'RuntimeException是非受检异常'],
    ['Java', '多态', 'MULTIPLE', 'HARD', '以下关于多态的说法正确的是', '子类重写父类方法', '父类引用指向子类', '编译时多态', '运行时多态', 'ABD', ''],
    ['Java', '集合', 'SHORT', 'MEDIUM', '简述ArrayList和LinkedList的区别', '', '', '', '', 'ArrayList底层数组，LinkedList底层链表', ''],
    ['Java', '排序', 'CODE', 'HARD', '用Java实现冒泡排序', '', '', '', '', '', '']
  ]
  const ws = XLSX.utils.aoa_to_sheet([...headers, ...samples])
  ws['!cols'] = [10, 12, 10, 8, 30, 12, 12, 12, 12, 12, 20].map(w => ({ wch: w }))
  const remark = [['说明：题型填 SINGLE/MULTIPLE/JUDGE/SHORT/CODE；难度填 EASY/MEDIUM/HARD；单选答案填单字母如"B"；多选答案填合并字母如"ABD"；判断题答案填 true/false']]
  XLSX.utils.sheet_add_aoa(ws, remark, { origin: { r: headers.length + samples.length + 1, c: 0 } })
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '题目导入模板')
  XLSX.writeFile(wb, '题目批量导入模板.xlsx')
}

onMounted(async () => {
  fetchList()
  const res = await getQuestionCreators()
  creatorList.value = res.data
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: #030303 !important;
  min-height: 100vh;
  padding: 16px;
  font-family: 'JetBrains Mono', monospace !important;
}

/* Cyber Card */
.cyber-card {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
}

.cyber-card :deep(.el-card__body) {
  background: transparent !important;
}

/* Filter Bar */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.flex-spacer { flex: 1; }

/* Cyber Buttons */
.cyber-btn {
  font-family: 'JetBrains Mono', monospace !important;
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
  transition: all 0.3s ease !important;
  padding: 8px 16px !important;
}

.cyber-btn:hover {
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
  color: #00ffff !important;
}

.cyber-btn-primary {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.2), rgba(0, 255, 255, 0.1)) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
}

.cyber-btn-primary:hover {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.4), rgba(0, 255, 255, 0.2)) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.5) !important;
}

.cyber-btn-success {
  background: linear-gradient(135deg, rgba(57, 255, 20, 0.2), rgba(57, 255, 20, 0.1)) !important;
  border-color: #39ff14 !important;
  color: #39ff14 !important;
}

.cyber-btn-success:hover {
  background: linear-gradient(135deg, rgba(57, 255, 20, 0.4), rgba(57, 255, 20, 0.2)) !important;
  box-shadow: 0 0 20px rgba(57, 255, 20, 0.5) !important;
}

.cyber-btn-accent {
  background: linear-gradient(135deg, rgba(255, 16, 240, 0.2), rgba(255, 16, 240, 0.1)) !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
}

.cyber-btn-accent:hover {
  background: linear-gradient(135deg, rgba(255, 16, 240, 0.4), rgba(255, 16, 240, 0.2)) !important;
  box-shadow: 0 0 20px rgba(255, 16, 240, 0.5) !important;
}

.cyber-btn-small {
  font-family: 'JetBrains Mono', monospace !important;
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
  clip-path: polygon(0 0, calc(100% - 6px) 0, 100% 6px, 100% 100%, 6px 100%, 0 calc(100% - 6px));
}

.cyber-btn-small:hover {
  border-color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3) !important;
  color: #00ffff !important;
}

.cyber-btn-text {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: none !important;
  color: #00ffff !important;
  padding: 2px 4px !important;
}

.cyber-btn-text:hover {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

.cyber-btn-danger {
  color: #ff10f0 !important;
}

.cyber-btn-danger:hover {
  color: #ff9800 !important;
  text-shadow: 0 0 10px rgba(255, 152, 0, 0.5) !important;
}

/* Cyber Table */
.cyber-table {
  background: transparent !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table :deep(.el-table__header-wrapper th) {
  background: #0a0a0a !important;
  border-bottom: 1px solid #1a1a2e !important;
  color: #00ffff !important;
  font-weight: 600 !important;
}

.cyber-table :deep(.el-table__body-wrapper tr) {
  background: #0a0a0a !important;
}

.cyber-table :deep(.el-table__body-wrapper td) {
  background: #0a0a0a !important;
  border-bottom: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
}

.cyber-table :deep(.el-table__body-wrapper tr:hover > td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.cyber-table :deep(.el-table--border .el-table__cell) {
  border-right: 1px solid #1a1a2e !important;
}

/* Cyber Tags */
.cyber-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid !important;
}

.cyber-tag-ai {
  color: #ff9800 !important;
  border-color: #ff9800 !important;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.3) !important;
}

/* Pagination */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.cyber-pagination {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pagination__total) {
  color: #e0e0e0 !important;
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
}

.cyber-pagination :deep(.el-pager li.is-active) {
  background: rgba(0, 255, 255, 0.2) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
}

.cyber-pagination :deep(.el-pagination__jump) {
  color: #e0e0e0 !important;
}

.cyber-pagination :deep(.el-pagination .el-pagination__editor) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
}

/* Cyber Dialog */
.cyber-dialog {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-dialog :deep(.el-dialog) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 30px) 0, 100% 30px, 100% 100%, 30px 100%, 0 calc(100% - 30px));
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.2), 0 0 60px rgba(255, 16, 240, 0.1) !important;
}

.cyber-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #1a1a2e !important;
}

.cyber-dialog :deep(.el-dialog__title) {
  color: #00ffff !important;
  font-weight: 600 !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-dialog :deep(.el-dialog__body) {
  background: #0a0a0a !important;
  color: #e0e0e0 !important;
}

.cyber-dialog :deep(.el-dialog__footer) {
  border-top: 1px solid #1a1a2e !important;
}

/* Form Items */
.cyber-dialog :deep(.el-form-item__label) {
  color: #00ffff !important;
  font-weight: 500 !important;
}

.cyber-dialog :deep(.el-form-item) {
  margin-bottom: 18px !important;
}

/* Inputs */
.cyber-dialog :deep(.el-input__wrapper) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: none !important;
}

.cyber-dialog :deep(.el-input__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-dialog :deep(.el-input__inner::placeholder) {
  color: rgba(224, 224, 224, 0.4) !important;
}

.cyber-dialog :deep(.el-input__wrapper:hover) {
  border-color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.2) !important;
}

.cyber-dialog :deep(.el-input__wrapper.is-focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 12px rgba(0, 255, 255, 0.3) !important;
}

/* Textarea */
.cyber-dialog :deep(.el-textarea__inner) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-dialog :deep(.el-textarea__inner:hover) {
  border-color: #00ffff !important;
}

.cyber-dialog :deep(.el-textarea__inner:focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 12px rgba(0, 255, 255, 0.3) !important;
}

/* Select */
.cyber-dialog :deep(.el-select__wrapper) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: none !important;
  color: #e0e0e0 !important;
}

.cyber-dialog :deep(.el-select__wrapper:hover) {
  border-color: #00ffff !important;
}

.cyber-dialog :deep(.el-select__wrapper.is-focused) {
  border-color: #00ffff !important;
  box-shadow: 0 0 12px rgba(0, 255, 255, 0.3) !important;
}

.cyber-dialog :deep(.el-select__dropdown) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
}

.cyber-dialog :deep(.el-select-dropdown__item) {
  color: #e0e0e0 !important;
}

.cyber-dialog :deep(.el-select-dropdown__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
}

/* Input Number */
.cyber-dialog :deep(.el-input-number__wrapper) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
}

.cyber-dialog :deep(.el-input-number__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

/* Radio */
.cyber-radio-group {
  display: flex;
  gap: 16px !important;
}

.cyber-radio {
  font-family: 'JetBrains Mono', monospace !important;
  color: #e0e0e0 !important;
}

.cyber-dialog :deep(.el-radio__input.is-checked .el-radio__inner) {
  background: rgba(0, 255, 255, 0.2) !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3) !important;
}

.cyber-dialog :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #00ffff !important;
}

.cyber-dialog :deep(.el-radio__inner) {
  background: #0a0a0a !important;
  border-color: #1a1a2e !important;
}

/* Checkbox */
.cyber-checkbox-group {
  display: flex;
  gap: 16px !important;
  flex-wrap: wrap !important;
}

.cyber-checkbox {
  font-family: 'JetBrains Mono', monospace !important;
  color: #e0e0e0 !important;
}

.cyber-dialog :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: rgba(0, 255, 255, 0.2) !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3) !important;
}

.cyber-dialog :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #00ffff !important;
}

.cyber-dialog :deep(.el-checkbox__inner) {
  background: #0a0a0a !important;
  border-color: #1a1a2e !important;
}

/* Option Row */
.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.option-label {
  width: 20px;
  font-weight: 600;
  color: #ff10f0 !important;
  flex-shrink: 0;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.5) !important;
}

/* Content Preview */
.content-preview {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  font-size: 13px;
  color: #e0e0e0 !important;
}

.text-muted { color: rgba(224, 224, 224, 0.4) !important; }

/* AI Loading Tip */
.ai-loading-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(255, 16, 240, 0.1) !important;
  border: 1px solid rgba(255, 16, 240, 0.3) !important;
  border-radius: 0 !important;
  font-size: 13px;
  color: #ff10f0 !important;
  margin-top: 12px;
  clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px));
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.2) !important;
  animation: neon-pulse 2s ease-in-out infinite !important;
}

@keyframes neon-pulse {
  0%, 100% { box-shadow: 0 0 15px rgba(255, 16, 240, 0.2) !important; }
  50% { box-shadow: 0 0 25px rgba(255, 16, 240, 0.4) !important; }
}

.ai-spin {
  animation: spin 1s linear infinite !important;
  color: #ff10f0 !important;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

/* Template Tip */
.template-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #e0e0e0 !important;
  margin-bottom: 12px;
}

.cyber-link {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-link:hover {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

/* Upload */
.cyber-upload :deep(.el-upload-dragger) {
  background: #0a0a0a !important;
  border: 2px dashed #1a1a2e !important;
  border-radius: 0 !important;
}

.cyber-upload :deep(.el-upload-dragger:hover) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.2) !important;
}

.cyber-upload :deep(.el-upload__text) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-upload :deep(.el-upload__tip) {
  color: rgba(224, 224, 224, 0.4) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

/* Divider */
.cyber-divider {
  border-color: #1a1a2e !important;
}

/* Descriptions */
.cyber-descriptions :deep(.el-descriptions__label) {
  background: #0a0a0a !important;
  color: #00ffff !important;
  border-color: #1a1a2e !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-descriptions :deep(.el-descriptions__content) {
  background: #0a0a0a !important;
  color: #e0e0e0 !important;
  border-color: #1a1a2e !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-text-success {
  color: #39ff14 !important;
  text-shadow: 0 0 8px rgba(57, 255, 20, 0.3) !important;
}

.cyber-text-danger {
  color: #ff10f0 !important;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.3) !important;
}

/* Error List */
.error-list { margin-top: 12px; }
.error-title { 
  font-size: 13px; 
  color: #ff10f0 !important; 
  font-weight: 600; 
  margin-bottom: 6px;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.3) !important;
}

.error-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #e0e0e0 !important;
  padding: 3px 0;
}

/* Scrollbar */
::-webkit-scrollbar {
  width: 6px !important;
  height: 6px !important;
}

::-webkit-scrollbar-track {
  background: #0a0a0a !important;
}

::-webkit-scrollbar-thumb {
  background: #1a1a2e !important;
  border-radius: 0 !important;
}

::-webkit-scrollbar-thumb:hover {
  background: #00ffff !important;
}

/* Element Plus Override - Global */
:deep(.el-message-box) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
}

:deep(.el-message-box__title) {
  color: #00ffff !important;
}

:deep(.el-message-box__message) {
  color: #e0e0e0 !important;
}

:deep(.el-overlay) {
  background: rgba(3, 3, 3, 0.85) !important;
}

:deep(.el-select-dropdown__item) {
  background: #0a0a0a !important;
  color: #e0e0e0 !important;
}

:deep(.el-select-dropdown__item.hover),
:deep(.el-select-dropdown__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
}

:deep(.el-select-dropdown__item.selected) {
  color: #00ffff !important;
}

:deep(.el-popper.is-light) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
}

:deep(.el-popper.is-light .el-popper__arrow::before) {
  background: #0a0a0a !important;
  border-color: #1a1a2e !important;
}
</style>
