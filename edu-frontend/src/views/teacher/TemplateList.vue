<template>
  <div class="page">
    <el-card shadow="never" class="cyber-card">
      <template #header>
        <div class="header-bar">
          <span class="cyber-title">试卷模板</span>
          <div class="header-actions">
            <el-button type="primary" :icon="Plus" @click="showCreateDialog = true" class="cyber-btn">创建模板</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="tableLoading" stripe border class="cyber-table">
        <el-table-column prop="name" label="模板名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="courseName" label="课程" width="120" show-overflow-tooltip />
        <el-table-column prop="questionCount" label="题目数量" width="100" align="center" />
        <el-table-column prop="totalScore" label="总分" width="80" align="center" />
        <el-table-column prop="creatorName" label="创建人" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" text @click="viewDetail(row)" class="cyber-btn-text">查看</el-button>
            <el-button size="small" type="success" text @click="useTemplate(row)" class="cyber-btn-text">使用</el-button>
            <el-button size="small" type="warning" text :icon="Download" @click="handleDownload(row)" class="cyber-btn-text">下载试卷Excel</el-button>
            <el-button size="small" type="success" text :icon="Download" @click="handleDownloadWord(row)" class="cyber-btn-text">下载试卷Word</el-button>
            <el-button size="small" type="danger" text @click="handleDelete(row)" class="cyber-btn-text">删除</el-button>
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
          @current-change="fetchList"
          @size-change="(s) => { pagination.size = s; pagination.page = 1; fetchList() }"
          class="cyber-pagination"
        />
      </div>
    </el-card>

    <el-dialog v-model="showDetail" title="模板详情" width="700px" class="cyber-dialog">
      <el-descriptions :column="2" border v-if="currentTemplate" class="cyber-descriptions">
        <el-descriptions-item label="模板名称">{{ currentTemplate.name }}</el-descriptions-item>
        <el-descriptions-item label="课程">{{ currentTemplate.courseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="题目数量">{{ currentTemplate.questionCount }}</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentTemplate.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ currentTemplate.creatorName }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(currentTemplate.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentTemplate.description || '-' }}</el-descriptions-item>
      </el-descriptions>
      
      <el-divider content-position="left" class="cyber-divider">题目列表</el-divider>
      
      <el-table :data="templateQuestions" v-loading="questionsLoading" size="small" class="cyber-table" max-height="400">
        <el-table-column type="index" width="60" label="序号" align="center" />
        <el-table-column prop="type" label="题型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" class="cyber-tag">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="difficulty" label="难度" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="difficultyType(row.difficulty)" class="cyber-tag">{{ difficultyLabel(row.difficulty) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="score" label="分值" width="80" align="center" />
      </el-table>
      
      <template #footer>
        <el-button @click="showDetail = false" class="cyber-btn">关闭</el-button>
        <el-button type="primary" @click="useTemplate(currentTemplate!)" class="cyber-btn">使用此模板</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCreateDialog" title="使用模板创建考试" width="500px" class="cyber-dialog">
      <el-form :model="createForm" label-width="100px" class="cyber-form">
        <el-form-item label="模板">
          <el-select v-model="createForm.templateId" placeholder="请选择模板" @change="onTemplateChange" class="cyber-select">
            <el-option v-for="t in tableData" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考试标题" required>
          <el-input v-model="createForm.title" placeholder="请输入考试标题" class="cyber-input" />
        </el-form-item>
        <el-form-item label="目标班级" required>
          <el-select
            v-model="createForm.targetClassIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择考试发布班级"
            class="cyber-select"
          >
            <el-option
              v-for="classItem in classList"
              :key="classItem.id"
              :label="classItem.className"
              :value="classItem.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间" required>
          <el-date-picker
            v-model="createForm.endTime"
            type="datetime"
            placeholder="选择截止时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="cyber-date-picker"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="createForm.startTime"
            type="datetime"
            placeholder="选择开始时间（可选）"
            value-format="YYYY-MM-DDTHH:mm:ss"
            class="cyber-date-picker"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false" class="cyber-btn">取消</el-button>
        <el-button type="primary" @click="handleCreateExam" :loading="creating" class="cyber-btn">创建考试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Download, Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getTemplateList, getTemplateQuestions, deleteTemplate, createExamFromTemplate, type Template, type TemplateQuestion } from '@/api/template'
import { getMyClasses, type TeacherClass } from '@/api/teacher'
import { downloadAuthorizedFile } from '@/utils/download'

const router = useRouter()

const tableData = ref<Template[]>([])
const tableLoading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })

const showDetail = ref(false)
const currentTemplate = ref<Template | null>(null)
const templateQuestions = ref<TemplateQuestion[]>([])
const questionsLoading = ref(false)

const showCreateDialog = ref(false)
const classList = ref<TeacherClass[]>([])
const createForm = reactive({
  templateId: undefined as number | undefined,
  title: '',
  startTime: '',
  endTime: '',
  targetClassIds: [] as number[]
})
const creating = ref(false)

async function fetchList() {
  tableLoading.value = true
  try {
    const res = await getTemplateList({ page: pagination.page, size: pagination.size })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch {
    ElMessage.error('获取模板列表失败')
  } finally {
    tableLoading.value = false
  }
}

async function viewDetail(row: Template) {
  currentTemplate.value = row
  showDetail.value = true
  questionsLoading.value = true
  try {
    const res = await getTemplateQuestions(row.id)
    templateQuestions.value = res.data
  } catch {
    ElMessage.error('获取题目列表失败')
  } finally {
    questionsLoading.value = false
  }
}

function useTemplate(row: Template) {
  showDetail.value = false
  createForm.templateId = row.id
  createForm.title = row.name + ' - 考试'
  createForm.startTime = ''
  createForm.endTime = ''
  createForm.targetClassIds = defaultTargetClassIds()
  showCreateDialog.value = true
}

async function onTemplateChange(templateId: number) {
  const t = tableData.value.find(item => item.id === templateId)
  if (t) {
    createForm.title = t.name + ' - 考试'
  }
}

async function handleCreateExam() {
  if (!createForm.templateId) {
    ElMessage.warning('请选择模板')
    return
  }
  if (!createForm.title.trim()) {
    ElMessage.warning('请输入考试标题')
    return
  }
  if (!createForm.endTime) {
    ElMessage.warning('请选择截止时间')
    return
  }
  if (createForm.targetClassIds.length === 0) {
    ElMessage.warning('请至少选择一个目标班级')
    return
  }
  
  creating.value = true
  try {
    const res = await createExamFromTemplate({
      templateId: createForm.templateId,
      title: createForm.title,
      startTime: createForm.startTime || undefined,
      endTime: createForm.endTime,
      targetClassIds: createForm.targetClassIds
    })
    ElMessage.success('考试创建成功')
    showCreateDialog.value = false
    router.push(`/teacher/exams/grade/${res.data.id}`)
  } catch {
    ElMessage.error('创建考试失败')
  } finally {
    creating.value = false
  }
}

async function handleDelete(row: Template) {
  try {
    await ElMessageBox.confirm(`确定删除模板"${row.name}"吗？`, '提示', { type: 'warning' })
    await deleteTemplate(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function handleDownload(row: Template) {
  try {
    await downloadAuthorizedFile(`/api/template/${row.id}/export`, `${row.name || '试卷模板'}.xlsx`)
  } catch {
    ElMessage.error('模板下载失败')
  }
}

async function handleDownloadWord(row: Template) {
  try {
    await downloadAuthorizedFile(`/api/template/${row.id}/export-word`, `${row.name || '试卷模板'}.docx`)
  } catch {
    ElMessage.error('Word模板下载失败')
  }
}

function formatTime(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}

function typeLabel(type: string) {
  const map: Record<string, string> = {
    SINGLE: '单选', MULTIPLE: '多选', JUDGE: '判断', SHORT: '简答', CODE: '编程'
  }
  return map[type] || type
}

function difficultyLabel(d: string) {
  const map: Record<string, string> = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
  return map[d] || d
}

function difficultyType(d: string) {
  const map: Record<string, string> = { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }
  return map[d] || ''
}

function defaultTargetClassIds() {
  return classList.value.length === 1 ? [classList.value[0].id] : []
}

async function fetchClasses() {
  try {
    const res = await getMyClasses()
    classList.value = res.data || []
  } catch {
    ElMessage.error('获取班级列表失败')
  }
}

onMounted(() => {
  fetchList()
  fetchClasses()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--bg-surface) !important;
  min-height: 100vh;
  padding: 16px;
  font-family: 'JetBrains Mono', monospace !important;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.cyber-title {
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 20px !important;
  font-weight: 700 !important;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
  letter-spacing: 2px !important;
}

.cyber-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px)) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1) 0%, transparent 100%) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 16px 20px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-card__body) {
  background: var(--bg-surface) !important;
  padding: 20px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table {
  background: var(--bg-surface) !important;
  font-family: 'JetBrains Mono', monospace !important;
  color: var(--text-primary) !important;
}

.cyber-table :deep(.el-table__header-wrapper th) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.15) 0%, rgba(255, 16, 240, 0.05) 100%) !important;
  color: #00ffff !important;
  border-bottom: 1px solid var(--border) !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
}

.cyber-table :deep(.el-table__body-wrapper td) {
  background: var(--bg-surface) !important;
  border-bottom: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table :deep(.el-table__body-wrapper tr:hover > td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.cyber-table :deep(.el-table__empty-text) {
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: rgba(0, 0, 0, 0.5) !important;
  border: 1px solid currentColor !important;
  box-shadow: 0 0 8px currentColor !important;
}

.cyber-btn {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  border: none !important;
  color: #0a0a0a !important;
  font-weight: 700 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.4) !important;
}

.cyber-btn:hover {
  box-shadow: 0 0 25px rgba(0, 255, 255, 0.6) !important;
  transform: translateY(-2px) !important;
}

.cyber-btn-text {
  font-family: 'JetBrains Mono', monospace !important;
  color: #ff10f0 !important;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5) !important;
}

.cyber-btn-text:hover {
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.8) !important;
}

.cyber-pagination {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pager li) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pager li:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
}

.cyber-pagination :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  color: #0a0a0a !important;
  font-weight: 700 !important;
}

.cyber-dialog {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-dialog :deep(.el-dialog) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.2) !important;
}

.cyber-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1) 0%, rgba(255, 16, 240, 0.05) 100%) !important;
  border-bottom: 1px solid var(--border) !important;
}

.cyber-dialog :deep(.el-dialog__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 700 !important;
}

.cyber-dialog :deep(.el-dialog__body) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  padding: 24px !important;
}

.cyber-form :deep(.el-form-item__label) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-form :deep(.el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) !important;
}

.cyber-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff !important;
}

.cyber-form :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-form :deep(.el-select .el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) !important;
}

.cyber-divider {
  border-color: var(--border-subtle) !important;
}

.cyber-divider :deep(.el-divider__text) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  background: var(--bg-surface) !important;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.cyber-date-picker {
  width: 100% !important;
}

.cyber-date-picker :deep(.el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) !important;
}

.cyber-select :deep(.el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) !important;
}

.cyber-descriptions :deep(.el-descriptions__label) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
  border: 1px solid var(--border) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-descriptions :deep(.el-descriptions__content) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  border: 1px solid var(--border) !important;
  font-family: 'JetBrains Mono', monospace !important;
}
</style>
