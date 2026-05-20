<template>
  <div class="page">
    <el-card shadow="never" class="cyber-card">
      <template #header>
        <div class="header-bar">
          <span class="cyber-title">问卷管理</span>
          <el-button type="primary" class="cyber-btn" @click="openCreateDialog">
            <span class="btn-text">创建问卷</span>
          </el-button>
        </div>
      </template>

      <el-table :data="surveyList" v-loading="loading" stripe border class="cyber-table">
        <el-table-column prop="title" label="问卷标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="targetTeacherName" label="被评讲师" width="100" />
        <el-table-column label="匿名" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isAnonymousRequired ? 'warning' : 'success'" size="small" class="cyber-tag">
              {{ row.isAnonymousRequired ? '匿名' : '实名' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalSubmissions" label="提交数" width="80" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" class="cyber-tag">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 1"
              size="small"
              type="success"
              text
              class="cyber-btn-success"
              @click="handlePublish(row)"
            >发布</el-button>
            <el-button size="small" type="primary" text class="cyber-btn-primary" @click="viewStats(row)">查看统计</el-button>
            <el-button size="small" type="danger" text class="cyber-btn-danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建问卷弹窗 -->
    <el-dialog v-model="showCreateDialog" title="创建问卷" width="700px" @close="resetForm" class="cyber-dialog">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="cyber-form">
        <el-form-item label="问卷标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入问卷标题" />
        </el-form-item>
        <el-form-item label="截止时间" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择截止时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="目标班级" prop="targetClassIds">
          <el-select v-model="form.targetClassIds" multiple placeholder="选择班级" style="width: 100%">
            <el-option
              v-for="cls in classList"
              :key="cls.id"
              :label="cls.className"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="被评讲师" prop="targetTeacherId">
          <el-select v-model="form.targetTeacherId" placeholder="选择讲师" style="width: 100%">
            <el-option
              v-for="teacher in teacherList"
              :key="teacher.id"
              :label="teacher.realName"
              :value="teacher.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="被评讲师">
          <el-input :model-value="userInfo?.realName || ''" disabled />
        </el-form-item>
        <el-form-item label="匿名调查">
          <el-switch v-model="form.isAnonymousRequired" :active-value="1" :inactive-value="0" />
          <span class="cyber-hint">开启后学生提交时无法记录身份信息</span>
        </el-form-item>
      </el-form>

      <div class="questions-section">
        <div class="section-header">
          <span class="cyber-title">题目配置</span>
          <el-button type="primary" size="small" class="cyber-btn" @click="addQuestion">
            <span class="btn-text">添加题目</span>
          </el-button>
        </div>

        <div v-for="(q, index) in form.questions" :key="index" class="question-item">
          <div class="question-header">
            <span class="question-num">第 {{ index + 1 }} 题</span>
            <el-button type="danger" size="small" text class="cyber-btn-danger" @click="removeQuestion(index)">删除</el-button>
          </div>
          <el-form :model="q" label-width="80px" style="margin-top: 10px">
            <el-form-item label="题型">
              <el-select v-model="q.type" style="width: 100%">
                <el-option label="星级评分 (STAR)" value="STAR" />
                <el-option label="NPS评分 (NPS)" value="NPS" />
                <el-option label="量表评分 (SCALE)" value="SCALE" />
                <el-option label="文本回答 (TEXT)" value="TEXT" />
              </el-select>
            </el-form-item>
            <el-form-item label="题干">
              <el-input v-model="q.title" placeholder="请输入题目" />
            </el-form-item>
            <el-form-item label="必填">
              <el-switch v-model="q.isRequired" :active-value="1" :inactive-value="0" />
            </el-form-item>
            <el-form-item v-if="q.type === 'STAR'" label="选项配置">
              <div class="option-config">
                <span>固定为5星评分</span>
              </div>
            </el-form-item>
            <el-form-item v-if="q.type === 'NPS'" label="选项配置">
              <div class="option-config">
                <span>固定为0-10分推荐意愿评分</span>
              </div>
            </el-form-item>
            <el-form-item v-if="q.type === 'SCALE'" label="量表配置">
              <div class="scale-config">
                <span>请在下方JSON中配置labels数组，如：["非常不同意","不同意","一般","同意","非常同意"]</span>
              </div>
            </el-form-item>
            <el-form-item v-if="q.type === 'SCALE'" label="选项JSON">
              <el-input
                v-model="q.optionsJson"
                type="textarea"
                :rows="2"
                placeholder='["非常不同意","不同意","一般","同意","非常同意"]'
              />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <el-button @click="showCreateDialog = false" class="cyber-btn-cancel">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating" class="cyber-btn">创建</el-button>
      </template>
    </el-dialog>

    <!-- 统计大屏 -->
    <el-dialog v-model="showStatsDialog" title="问卷统计" width="90%" top="20px" class="cyber-dialog cyber-dialog-wide">
      <SurveyStats v-if="showStatsDialog" :survey-id="currentSurveyId" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent, ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSurveyList,
  createSurvey,
  publishSurvey,
  deleteSurvey,
  getTeacherOptions,
  type SurveyListItem,
  type CreateSurveyDTO,
  type TeacherOption
} from '@/api/survey'
import { getMyClasses, type TeacherClass } from '@/api/teacher'

const SurveyStats = defineAsyncComponent(() => import('./SurveyStats.vue'))

const loading = ref(false)
const creating = ref(false)
const showCreateDialog = ref(false)
const showStatsDialog = ref(false)
const currentSurveyId = ref<number | null>(null)

const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
const isAdmin = computed(() => userInfo?.role === 'ADMIN')

const surveyList = ref<SurveyListItem[]>([])
const classList = ref<TeacherClass[]>([])
const teacherList = ref<TeacherOption[]>([])

const formRef = ref()
const form = reactive<CreateSurveyDTO>({
  title: '',
  endTime: '',
  targetClassIds: [],
  isAnonymousRequired: 0,
  targetTeacherId: 0,
  questions: []
})

const rules = {
  title: [{ required: true, message: '请输入问卷标题', trigger: 'blur' }],
  endTime: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
  targetClassIds: [{ required: true, message: '请选择班级', trigger: 'change', type: 'array' }]
}

function formatTime(time: string): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

async function fetchSurveyList() {
  loading.value = true
  try {
    const res = await getSurveyList()
    surveyList.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载问卷列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchClassList() {
  try {
    const res = await getMyClasses()
    classList.value = res.data || []
  } catch (error: any) {
    console.error('加载班级列表失败', error)
  }
}

async function fetchTeacherList() {
  try {
    const res = await getTeacherOptions()
    teacherList.value = res.data || []
  } catch (error: any) {
    console.error('加载讲师列表失败', error)
  }
}

function openCreateDialog() {
  showCreateDialog.value = true
  fetchClassList()
  if (isAdmin.value) {
    fetchTeacherList()
  } else {
    form.targetTeacherId = userInfo?.id || 0
  }
}

function addQuestion() {
  form.questions.push({
    type: 'STAR',
    title: '',
    optionsJson: '',
    isRequired: 1,
    sortOrder: form.questions.length + 1
  })
}

function removeQuestion(index: number) {
  form.questions.splice(index, 1)
  form.questions.forEach((q, i) => {
    q.sortOrder = i + 1
  })
}

function resetForm() {
  form.title = ''
  form.endTime = ''
  form.targetClassIds = []
  form.isAnonymousRequired = 0
  form.questions = []
  if (!isAdmin.value) {
    form.targetTeacherId = userInfo?.id || 0
  } else {
    form.targetTeacherId = 0
  }
}

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (form.questions.length === 0) {
    ElMessage.warning('请至少添加一道题目')
    return
  }

  creating.value = true
  try {
    await createSurvey(form)
    ElMessage.success('问卷创建成功')
    showCreateDialog.value = false
    fetchSurveyList()
    resetForm()
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function handlePublish(row: SurveyListItem) {
  try {
    await publishSurvey(row.surveyId)
    ElMessage.success('发布成功')
    fetchSurveyList()
  } catch (error: any) {
    ElMessage.error(error.message || '发布失败')
  }
}

async function handleDelete(row: SurveyListItem) {
  try {
    await ElMessageBox.confirm(`确认删除问卷「${row.title}」？删除后学生端将不再显示该问卷。`, '删除问卷', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteSurvey(row.surveyId)
    ElMessage.success('问卷已删除')
    fetchSurveyList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除问卷失败')
    }
  }
}

function viewStats(row: SurveyListItem) {
  currentSurveyId.value = row.surveyId
  showStatsDialog.value = true
}

onMounted(() => {
  fetchSurveyList()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.page {
  padding: 0;
  background: #030303;
  min-height: 100vh;
  font-family: 'JetBrains Mono', monospace;
}

.cyber-card {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
}

.cyber-card :deep(.el-card__header) {
  background: #0a0a0a !important;
  border-bottom: 1px solid #1a1a2e !important;
  padding: 15px 20px !important;
}

.cyber-card :deep(.el-card__body) {
  background: #0a0a0a !important;
  padding: 20px !important;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.cyber-title {
  color: #00ffff !important;
  font-weight: 700 !important;
  font-size: 18px !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
  letter-spacing: 2px !important;
}

.cyber-btn {
  background: transparent !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
  clip-path: polygon(10px 0, 100% 0, 100% calc(100% - 10px), calc(100% - 10px) 100%, 0 100%, 0 10px) !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
  transition: all 0.3s ease !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-btn:hover {
  background: rgba(0, 255, 255, 0.15) !important;
  box-shadow: 0 0 25px rgba(0, 255, 255, 0.5) !important;
}

.cyber-btn :deep(.el-button__content),
.cyber-btn :deep(span) {
  color: #00ffff !important;
}

.btn-text {
  color: #00ffff !important;
}

.cyber-btn-success {
  color: #39ff14 !important;
  border: none !important;
  background: transparent !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-btn-success:hover {
  color: #39ff14 !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.5) !important;
}

.cyber-btn-primary {
  color: #ff10f0 !important;
  border: none !important;
  background: transparent !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-btn-primary:hover {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

.cyber-btn-danger {
  color: #ff4444 !important;
  border: none !important;
  background: transparent !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-btn-danger:hover {
  color: #ff4444 !important;
  text-shadow: 0 0 10px rgba(255, 68, 68, 0.5) !important;
}

.cyber-btn-cancel {
  background: transparent !important;
  border: 1px solid #ff9800 !important;
  color: #ff9800 !important;
  clip-path: polygon(10px 0, 100% 0, 100% calc(100% - 10px), calc(100% - 10px) 100%, 0 100%, 0 10px) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-btn-cancel:hover {
  background: rgba(255, 152, 0, 0.15) !important;
}

.cyber-table {
  background: #0a0a0a !important;
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

.cyber-table :deep(.el-table__body-wrapper tr:hover td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.cyber-table :deep(.el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.3) !important;
}

.cyber-table :deep(.el-table--striped .el-table__body tr.el-table__row--striped:hover td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.cyber-tag {
  font-family: 'JetBrains Mono', monospace !important;
  border: 1px solid currentColor !important;
  background: transparent !important;
  clip-path: polygon(5px 0, 100% 0, 100% calc(100% - 5px), calc(100% - 5px) 100%, 0 100%, 0 5px) !important;
}

.cyber-tag.el-tag--success {
  color: #39ff14 !important;
  box-shadow: 0 0 10px rgba(57, 255, 20, 0.3) !important;
}

.cyber-tag.el-tag--warning {
  color: #ff9800 !important;
  box-shadow: 0 0 10px rgba(255, 152, 0, 0.3) !important;
}

.cyber-tag.el-tag--info {
  color: #909399 !important;
}

.cyber-dialog {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 30px) 0, 100% 30px, 100% 100%, 30px 100%, 0 calc(100% - 30px)) !important;
  box-shadow: 0 0 50px rgba(255, 16, 240, 0.2), 0 0 100px rgba(0, 255, 255, 0.1) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-dialog-wide {
  max-width: 1400px;
}

.cyber-dialog :deep(.el-dialog__header) {
  background: #0a0a0a !important;
  border-bottom: 1px solid #1a1a2e !important;
  padding: 20px !important;
}

.cyber-dialog :deep(.el-dialog__title) {
  color: #ff10f0 !important;
  font-weight: 700 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

.cyber-dialog :deep(.el-dialog__body) {
  background: #0a0a0a !important;
  padding: 20px !important;
  color: #e0e0e0 !important;
}

.cyber-dialog :deep(.el-dialog__footer) {
  background: #0a0a0a !important;
  border-top: 1px solid #1a1a2e !important;
  padding: 15px 20px !important;
}

.cyber-form :deep(.el-form-item__label) {
  color: #00ffff !important;
  font-weight: 500 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-form :deep(.el-input__wrapper) {
  background: #030303 !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: none !important;
}

.cyber-form :deep(.el-input__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-form :deep(.el-input__inner::placeholder) {
  color: #666 !important;
}

.cyber-form :deep(.el-input.is-disabled .el-input__wrapper) {
  background: #0a0a0a !important;
  border-color: #1a1a2e !important;
}

.cyber-form :deep(.el-input.is-disabled .el-input__inner) {
  color: #666 !important;
}

.cyber-form :deep(.el-select__wrapper) {
  background: #030303 !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: none !important;
}

.cyber-form :deep(.el-select__selected-item) {
  color: #e0e0e0 !important;
}

.cyber-form :deep(.el-select__placeholder) {
  color: #666 !important;
}

.cyber-form :deep(.el-select-dropdown) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
}

.cyber-form :deep(.el-select-dropdown__item) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-form :deep(.el-select-dropdown__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
}

.cyber-form :deep(.el-select-dropdown__item.is-selected) {
  color: #00ffff !important;
  background: rgba(0, 255, 255, 0.15) !important;
}

.cyber-form :deep(.el-textarea__inner) {
  background: #030303 !important;
  border: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
  box-shadow: none !important;
}

.cyber-form :deep(.el-date-editor) {
  --el-date-editor-width: 100% !important;
}

.cyber-form :deep(.el-picker__popper) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
}

.cyber-form :deep(.el-date-picker__header-label) {
  color: #00ffff !important;
}

.cyber-form :deep(.el-date-table th) {
  color: #00ffff !important;
}

.cyber-form :deep(.el-date-table td.available:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
}

.cyber-form :deep(.el-date-table td.current:not(.disabled)) {
  color: #00ffff !important;
}

.cyber-form :deep(.el-switch) {
  --el-switch-off-color: #1a1a2e !important;
  --el-switch-on-color: #00ffff !important;
}

.cyber-hint {
  margin-left: 10px;
  color: #666 !important;
  font-size: 12px !important;
}

.questions-section {
  margin-top: 20px;
  border-top: 1px solid #1a1a2e;
  padding-top: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.question-item {
  background: rgba(26, 26, 46, 0.5) !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px)) !important;
  padding: 15px;
  margin-bottom: 15px;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-num {
  font-weight: 600;
  color: #ff10f0 !important;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.4) !important;
}

.option-config,
.scale-config {
  color: #666 !important;
  font-size: 13px !important;
}

.cyber-table :deep(.el-loading-mask) {
  background: rgba(10, 10, 10, 0.9) !important;
}

.cyber-table :deep(.el-loading-spinner .el-loading-text) {
  color: #00ffff !important;
}

.cyber-table :deep(.el-loading-spinner .circular) {
  stroke: #00ffff !important;
}
</style>
