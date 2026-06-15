<template>
  <div class="visit-page">
    <header class="page-header">
      <div>
        <h2>学员回访记录</h2>
        <p>讲师与班主任共用记录，按负责班级沉淀学员回访结果和后续跟进事项。</p>
      </div>
      <el-button type="primary" :icon="Plus" :disabled="!studentOptions.length" @click="openCreateDialog">
        新增回访
      </el-button>
    </header>

    <section class="filter-panel">
      <el-select v-model="filters.classId" placeholder="全部负责班级" clearable @change="handleClassFilterChange">
        <el-option v-for="item in classList" :key="item.id" :label="item.className" :value="item.id" />
      </el-select>
      <el-select v-model="filters.studentId" placeholder="全部学员" clearable filterable @change="loadRecords">
        <el-option
          v-for="student in studentOptions"
          :key="student.id"
          :label="`${student.realName}（${student.username}）`"
          :value="student.id"
        />
      </el-select>
      <el-button :icon="Refresh" :loading="loading" @click="loadRecords">刷新</el-button>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <el-icon><component :is="item.icon" /></el-icon>
        <div>
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <section class="table-panel">
      <el-table :data="records" v-loading="loading" stripe>
        <el-table-column label="学员" min-width="150">
          <template #default="{ row }">
            <div class="student-cell">
              <strong>{{ row.studentName }}</strong>
              <span>{{ row.studentUsername }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" min-width="150" show-overflow-tooltip />
        <el-table-column label="回访人" min-width="120">
          <template #default="{ row }">
            {{ row.visitorName || '-' }}
            <el-tag size="small" effect="plain" class="role-tag">{{ roleLabel(row.visitorRole) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="方式" width="100">
          <template #default="{ row }">{{ methodLabel(row.visitMethod) }}</template>
        </el-table-column>
        <el-table-column label="结果" width="120">
          <template #default="{ row }">
            <el-tag :type="resultTagType(row.visitResult)" effect="plain">
              {{ resultLabel(row.visitResult) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="problemCategory" label="问题分类" width="120" show-overflow-tooltip />
        <el-table-column prop="content" label="回访内容" min-width="260" show-overflow-tooltip />
        <el-table-column prop="conclusion" label="沟通结论" min-width="180" show-overflow-tooltip />
        <el-table-column label="附件" width="90">
          <template #default="{ row }">
            <el-button v-if="row.attachmentUrl" text type="primary" @click="openAttachment(row.attachmentUrl)">查看</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="解决" width="90">
          <template #default="{ row }">
            <el-tag :type="row.resolved ? 'success' : 'warning'" size="small" effect="plain">
              {{ row.resolved ? '已解决' : '未解决' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回访时间" width="160">
          <template #default="{ row }">{{ formatTime(row.visitTime) }}</template>
        </el-table-column>
        <el-table-column label="下次跟进" width="160">
          <template #default="{ row }">{{ formatTime(row.nextFollowTime) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="新增回访记录" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="班级">
          <el-select v-model="formClassId" placeholder="选择班级" style="width: 100%" @change="handleFormClassChange">
            <el-option v-for="item in classList" :key="item.id" :label="item.className" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学员" prop="studentId">
          <el-select v-model="form.studentId" placeholder="选择学员" filterable style="width: 100%">
            <el-option
              v-for="student in formStudentOptions"
              :key="student.id"
              :label="`${student.realName}（${student.username}）`"
              :value="student.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="回访方式" prop="visitMethod">
          <el-select v-model="form.visitMethod" style="width: 100%">
            <el-option v-for="item in methodOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="回访结果" prop="visitResult">
          <el-select v-model="form.visitResult" style="width: 100%">
            <el-option v-for="item in resultOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题分类">
          <el-select v-model="form.problemCategory" clearable placeholder="选择问题分类" style="width: 100%">
            <el-option v-for="item in problemCategoryOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="回访时间">
          <el-date-picker v-model="form.visitTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="默认当前时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker v-model="form.nextFollowTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="可选" style="width: 100%" />
        </el-form-item>
        <el-form-item label="回访内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="记录沟通情况、学生反馈、问题原因和后续动作" />
        </el-form-item>
        <el-form-item label="沟通结论">
          <el-input v-model="form.conclusion" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="记录处理结果、家校共识或后续安排" />
        </el-form-item>
        <el-form-item label="附件链接">
          <el-input v-model="form.attachmentUrl" maxlength="500" clearable placeholder="粘贴截图、文件或沟通凭证链接" />
        </el-form-item>
        <el-form-item label="是否解决">
          <el-switch v-model="form.resolved" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRecord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import type { FormInstance, FormRules } from 'element-plus'
import { ChatDotRound, Finished, Plus, Refresh, User } from '@element-plus/icons-vue'
import { getMyClasses, getStudentList, type StudentRecord, type TeacherClass } from '@/api/teacher'
import { createStudentVisitRecord, getStudentVisitRecords, type StudentVisitRecord, type VisitMethod, type VisitResult } from '@/api/studentVisit'

const methodOptions: Array<{ label: string; value: VisitMethod }> = [
  { label: '电话', value: 'PHONE' },
  { label: '微信', value: 'WECHAT' },
  { label: '线下', value: 'OFFLINE' },
  { label: '线上会议', value: 'ONLINE' },
  { label: '其他', value: 'OTHER' }
]

const resultOptions: Array<{ label: string; value: VisitResult }> = [
  { label: '已联系', value: 'REACHED' },
  { label: '未联系上', value: 'UNREACHED' },
  { label: '需继续跟进', value: 'NEED_FOLLOW' },
  { label: '已解决', value: 'RESOLVED' }
]

const problemCategoryOptions = ['学习进度', '作业缺交', '考试低分', '切屏异常', '问卷反馈', '账号问题', '家校沟通', '其他']

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const classList = ref<TeacherClass[]>([])
const studentMap = ref(new Map<number, StudentRecord[]>())
const records = ref<StudentVisitRecord[]>([])
const formRef = ref<FormInstance>()
const formClassId = ref<number | null>(null)

const filters = reactive({ classId: null as number | null, studentId: null as number | null })
const form = reactive({
  studentId: null as number | null,
  visitMethod: 'PHONE',
  visitResult: 'REACHED',
  problemCategory: '',
  content: '',
  conclusion: '',
  attachmentUrl: '',
  resolved: false,
  visitTime: null as string | null,
  nextFollowTime: null as string | null
})

const rules: FormRules = {
  studentId: [{ required: true, message: '请选择学员', trigger: 'change' }],
  visitMethod: [{ required: true, message: '请选择回访方式', trigger: 'change' }],
  visitResult: [{ required: true, message: '请选择回访结果', trigger: 'change' }],
  content: [{ required: true, message: '请输入回访内容', trigger: 'blur' }]
}

const studentOptions = computed(() => filters.classId ? studentMap.value.get(filters.classId) || [] : Array.from(studentMap.value.values()).flat())
const formStudentOptions = computed(() => formClassId.value ? studentMap.value.get(formClassId.value) || [] : [])

const summaryCards = computed(() => {
  const needFollow = records.value.filter(item => item.visitResult === 'NEED_FOLLOW').length
  const unresolved = records.value.filter(item => item.visitResult === 'UNREACHED' || item.visitResult === 'NEED_FOLLOW').length
  return [
    { label: '回访记录', value: records.value.length, icon: ChatDotRound },
    { label: '覆盖学员', value: new Set(records.value.map(item => item.studentId)).size, icon: User },
    { label: '需跟进', value: needFollow, icon: Refresh },
    { label: '已解决', value: records.value.filter(item => item.visitResult === 'RESOLVED').length, icon: Finished },
    { label: '未闭环', value: unresolved, icon: ChatDotRound }
  ]
})

async function loadInitialData() {
  const res = await getMyClasses()
  classList.value = res.data || []
  await Promise.all(classList.value.map(item => ensureStudents(item.id)))
  await loadRecords()
}

async function ensureStudents(classId: number) {
  if (studentMap.value.has(classId)) return
  const res = await getStudentList(classId)
  studentMap.value.set(classId, res.data || [])
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await getStudentVisitRecords({
      classId: filters.classId || undefined,
      studentId: filters.studentId || undefined
    })
    records.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载回访记录失败')
  } finally {
    loading.value = false
  }
}

async function handleClassFilterChange(value?: number) {
  filters.studentId = null
  if (value) await ensureStudents(value)
  await loadRecords()
}

async function handleFormClassChange(value?: number) {
  form.studentId = null
  if (value) await ensureStudents(value)
}

function openCreateDialog() {
  formClassId.value = filters.classId || classList.value[0]?.id || null
  form.studentId = filters.studentId || null
  form.visitMethod = 'PHONE'
  form.visitResult = 'REACHED'
  form.problemCategory = ''
  form.content = ''
  form.conclusion = ''
  form.attachmentUrl = ''
  form.resolved = false
  form.visitTime = null
  form.nextFollowTime = null
  dialogVisible.value = true
}

async function submitRecord() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !form.studentId) return
  saving.value = true
  try {
    await createStudentVisitRecord({
      studentId: form.studentId,
      visitMethod: form.visitMethod,
      visitResult: form.visitResult,
      content: form.content,
      problemCategory: form.problemCategory || null,
      conclusion: form.conclusion || null,
      attachmentUrl: form.attachmentUrl || null,
      resolved: form.resolved || form.visitResult === 'RESOLVED',
      visitTime: form.visitTime || null,
      nextFollowTime: form.nextFollowTime || null
    })
    ElMessage.success('回访记录已保存')
    dialogVisible.value = false
    await loadRecords()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存回访记录失败')
  } finally {
    saving.value = false
  }
}

function methodLabel(value: VisitMethod) {
  return methodOptions.find(item => item.value === value)?.label || value
}

function resultLabel(value: VisitResult) {
  return resultOptions.find(item => item.value === value)?.label || value
}

function resultTagType(value: VisitResult) {
  const map: Record<VisitResult, string> = { REACHED: 'success', UNREACHED: 'info', NEED_FOLLOW: 'warning', RESOLVED: 'primary' }
  return map[value]
}

function roleLabel(value: string) {
  return value === 'ASSISTANT' ? '班主任' : '讲师'
}

function openAttachment(url: string) {
  window.open(url, '_blank')
}

function formatTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(loadInitialData)
</script>

<style scoped>
.visit-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
}

.page-header,
.filter-panel,
.summary-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header {
  justify-content: space-between;
  align-items: flex-start;
}

.page-header h2 {
  margin: 0;
  color: var(--text-primary);
  font-size: 24px;
  letter-spacing: 0;
}

.page-header p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.filter-panel,
.table-panel,
.summary-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-surface);
  box-shadow: var(--shadow-md);
}

.filter-panel,
.table-panel {
  padding: 12px;
}

.filter-panel .el-select {
  width: 240px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(130px, 1fr));
  gap: 12px;
}

.summary-card {
  min-height: 82px;
  padding: 14px;
}

.summary-card .el-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: rgba(64, 128, 255, 0.12);
  color: #4080ff;
}

.summary-card strong {
  display: block;
  color: var(--text-primary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 24px;
}

.summary-card span,
.student-cell span {
  color: var(--text-muted);
  font-size: 12px;
}

.student-cell {
  display: grid;
  gap: 4px;
}

.student-cell strong {
  color: var(--text-primary);
}

.role-tag {
  margin-left: 6px;
}

.visit-page :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: var(--bg-base);
  --el-table-header-text-color: var(--text-secondary);
  --el-table-text-color: var(--text-primary);
  --el-table-border-color: var(--border-subtle);
  --el-table-row-hover-bg-color: var(--surface-hover);
}

.visit-page :deep(.el-table th.el-table__cell),
.visit-page :deep(.el-table td.el-table__cell) {
  background: transparent;
}

@media (max-width: 1180px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-header,
  .filter-panel {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-panel .el-select {
    width: 100%;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
