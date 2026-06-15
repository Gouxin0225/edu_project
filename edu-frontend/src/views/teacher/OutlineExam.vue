<template>
  <div class="page-container outline-page">
    <div class="page-header">
      <div>
        <h2>AI 大纲出卷</h2>
        <p>上传课程大纲后，AI 自动分析重难点并按常规考试比例生成题目，讲师审核后决定保留。</p>
      </div>
    </div>

    <el-steps :active="activeStep" finish-status="success" class="steps">
      <el-step title="上传大纲" />
      <el-step title="题目预览" />
      <el-step title="确认发布" />
    </el-steps>

    <section v-if="activeStep === 0" class="panel">
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="100px">
        <el-form-item label="课程方向" prop="courseCategory">
          <el-select v-model="uploadForm.courseCategory" placeholder="请选择课程方向" style="width: 280px">
            <el-option v-for="item in COURSE_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="试卷名称">
          <el-input v-model="uploadForm.title" placeholder="默认使用文件名" style="width: 360px" clearable />
        </el-form-item>
        <el-form-item label="大纲文件" prop="file">
          <el-upload
            drag
            action="#"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".txt,.md,.docx"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="upload-text">拖拽或点击上传 .txt / .md / .docx</div>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="uploading || generating" @click="handleUpload">上传并生成题目</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section v-if="activeStep === 1" class="panel">
      <div class="toolbar">
        <div>
          <div class="doc-title">已根据《{{ document?.title }}》生成 {{ questions.length }} 道题</div>
          <div class="doc-subtitle">题目默认全部保留，可取消勾选或编辑后再确认入库。</div>
        </div>
        <div>
          <el-button :loading="generating" @click="handleGenerateQuestions">重新生成</el-button>
          <el-button type="danger" @click="handleDeleteDocument">删除大纲</el-button>
          <el-button type="primary" @click="activeStep = 2">进入确认</el-button>
        </div>
      </div>
      <el-table :data="questions" border>
        <el-table-column label="入库" width="70" align="center">
          <template #default="{ row }"><el-checkbox v-model="row.selected" @change="saveQuestion(row)" /></template>
        </el-table-column>
        <el-table-column label="题型" width="100">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="难度" width="90">
          <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
        </el-table-column>
        <el-table-column prop="knowledgePoint" label="知识点" min-width="130" show-overflow-tooltip />
        <el-table-column prop="content" label="题干" min-width="260" show-overflow-tooltip />
        <el-table-column prop="standardAnswer" label="答案" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="90">
          <template #default="{ row }"><el-button text @click="openEdit(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="activeStep === 2" class="panel">
      <el-form :model="confirmForm" label-width="100px">
        <el-form-item label="确认题目">
          <el-tag type="success">{{ selectedCount }} 道</el-tag>
        </el-form-item>
        <el-form-item label="保存模板">
          <el-switch v-model="confirmForm.createTemplate" />
        </el-form-item>
        <el-form-item v-if="confirmForm.createTemplate" label="模板名称">
          <el-input v-model="confirmForm.templateName" style="width: 360px" />
        </el-form-item>
        <el-form-item label="创建考试">
          <el-switch v-model="confirmForm.createExam" />
        </el-form-item>
        <template v-if="confirmForm.createExam">
          <el-form-item label="考试标题">
            <el-input v-model="confirmForm.examTitle" style="width: 360px" />
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="confirmForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker v-model="confirmForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
          </el-form-item>
          <el-form-item label="目标班级">
            <el-select v-model="confirmForm.targetClassIds" multiple style="width: 360px">
              <el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item>
          <el-button @click="activeStep = 1">返回预览</el-button>
          <el-button type="danger" @click="handleDeleteDocument">删除大纲</el-button>
          <el-button type="primary" :loading="confirming" @click="handleConfirm">确认</el-button>
        </el-form-item>
      </el-form>
    </section>

    <el-dialog v-model="editDialog" title="编辑题目" width="760px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="知识点"><el-input v-model="editForm.knowledgePoint" /></el-form-item>
        <el-form-item label="题型">
          <el-select v-model="editForm.type">
            <el-option v-for="item in QUESTION_TYPES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="editForm.difficulty">
            <el-option v-for="item in DIFFICULTIES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干"><el-input v-model="editForm.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="选项"><el-input v-model="editForm.optionsJson" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="答案"><el-input v-model="editForm.standardAnswer" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="解析"><el-input v-model="editForm.analysis" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { COURSE_CATEGORIES, DIFFICULTIES, QUESTION_TYPES, type CourseCategory, type Difficulty, type QuestionType } from '@/api/question'
import { getMyClasses, type ClassInfo } from '@/api/exam'
import {
  confirmOutline,
  deleteOutline,
  generateOutlineQuestions,
  updateOutlineQuestion,
  uploadOutline,
  type OutlineDocument,
  type OutlineQuestion
} from '@/api/outline'

const activeStep = ref(0)
const uploadFormRef = ref<FormInstance>()
const uploading = ref(false)
const generating = ref(false)
const confirming = ref(false)
const document = ref<OutlineDocument | null>(null)
const questions = ref<OutlineQuestion[]>([])
const classes = ref<ClassInfo[]>([])
const editDialog = ref(false)
const editingId = ref<number | null>(null)

const uploadForm = reactive<{ courseCategory: CourseCategory | ''; title: string; file: File | null }>({
  courseCategory: '',
  title: '',
  file: null
})

const uploadRules: FormRules = {
  courseCategory: [{ required: true, message: '请选择课程方向', trigger: 'change' }],
  file: [{ required: true, message: '请上传大纲文件', trigger: 'change' }]
}

const confirmForm = reactive({
  createExam: false,
  createTemplate: true,
  examTitle: '',
  templateName: '',
  startTime: '',
  endTime: '',
  targetClassIds: [] as number[]
})

const editForm = reactive<Partial<OutlineQuestion>>({})

const selectedCount = computed(() => questions.value.filter(item => item.selected).length)

function handleFileChange(file: UploadFile) {
  uploadForm.file = file.raw || null
}

function handleFileRemove() {
  uploadForm.file = null
}

async function handleUpload() {
  const valid = await uploadFormRef.value?.validate().catch(() => false)
  if (!valid || !uploadForm.file || !uploadForm.courseCategory) return
  uploading.value = true
  try {
    const res = await uploadOutline({
      file: uploadForm.file,
      title: uploadForm.title,
      courseCategory: uploadForm.courseCategory
    })
    document.value = res.data
    confirmForm.examTitle = `${res.data.title} - AI大纲试卷`
    confirmForm.templateName = `${res.data.title} - AI大纲模板`
    await handleGenerateQuestions()
    activeStep.value = 1
  } finally {
    uploading.value = false
  }
}

async function handleGenerateQuestions() {
  if (!document.value) return
  generating.value = true
  try {
    const res = await generateOutlineQuestions(document.value.id)
    questions.value = res.data
    activeStep.value = 1
    ElMessage.success(`已生成 ${res.data.length} 道题`)
  } finally {
    generating.value = false
  }
}

function openEdit(row: OutlineQuestion) {
  editingId.value = row.id
  Object.assign(editForm, { ...row })
  editDialog.value = true
}

async function saveQuestion(row: OutlineQuestion) {
  await updateOutlineQuestion(row.id, { selected: row.selected })
}

async function saveEdit() {
  if (!editingId.value) return
  const res = await updateOutlineQuestion(editingId.value, editForm)
  const index = questions.value.findIndex(item => item.id === editingId.value)
  if (index >= 0) questions.value[index] = res.data
  editDialog.value = false
  ElMessage.success('题目已更新')
}

async function handleConfirm() {
  if (!document.value) return
  if (!selectedCount.value) {
    ElMessage.warning('请至少选择一道题')
    return
  }
  if (confirmForm.createExam && !confirmForm.endTime) {
    ElMessage.warning('请选择考试截止时间')
    return
  }
  if (confirmForm.createTemplate && !confirmForm.templateName.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  confirming.value = true
  try {
    const res = await confirmOutline(document.value.id, confirmForm)
    const actions = ['已入库']
    if (res.data.templateId) actions.push('已保存为考试模板')
    if (res.data.examId) actions.push('已创建考试')
    ElMessage.success(`${actions.join('，')}，共 ${res.data.savedCount} 道题`)
  } finally {
    confirming.value = false
  }
}

async function handleDeleteDocument() {
  if (!document.value) return
  try {
    await ElMessageBox.confirm(`确认删除大纲「${document.value.title}」及其待审核题目？`, '删除大纲', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteOutline(document.value.id)
    document.value = null
    questions.value = []
    uploadForm.title = ''
    uploadForm.file = null
    confirmForm.examTitle = ''
    confirmForm.templateName = ''
    confirmForm.startTime = ''
    confirmForm.endTime = ''
    confirmForm.targetClassIds = []
    activeStep.value = 0
    ElMessage.success('大纲已删除')
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除大纲失败')
    }
  }
}

function typeLabel(value: QuestionType) {
  return QUESTION_TYPES.find(item => item.value === value)?.label || value
}

function difficultyLabel(value: Difficulty) {
  return DIFFICULTIES.find(item => item.value === value)?.label || value
}

getMyClasses().then(res => {
  classes.value = res.data || []
})
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: var(--text-primary);
}

.page-header p {
  margin: 6px 0 0;
  color: rgba(226, 232, 240, 0.68);
}

.steps {
  margin-bottom: 18px;
}

.panel {
  background:
    linear-gradient(180deg, rgba(12, 20, 40, 0.9) 0%, rgba(12, 20, 40, 0.76) 100%);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 20px;
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(14px);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.doc-title {
  font-weight: 600;
  color: var(--text-primary);
}

.upload-icon {
  font-size: 36px;
  color: #7fb0ff;
}

.upload-text {
  color: rgba(226, 232, 240, 0.78);
}

.add-btn {
  margin-top: 12px;
}

.doc-subtitle {
  margin-top: 6px;
  color: rgba(226, 232, 240, 0.58);
}

.outline-page :deep(.el-form-item__content) {
  color: var(--text-primary);
}

.outline-page :deep(.el-upload) {
  width: 100%;
  max-width: 560px;
}

.outline-page :deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

.outline-page :deep(.el-dialog__body) {
  padding-top: 18px !important;
}
</style>
