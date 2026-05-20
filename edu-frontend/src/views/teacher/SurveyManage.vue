<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-bar">
          <span>问卷管理</span>
          <el-button type="primary" @click="openCreateDialog">创建问卷</el-button>
        </div>
      </template>

      <el-table :data="surveyList" v-loading="loading" stripe border>
        <el-table-column prop="title" label="问卷标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="targetTeacherName" label="被评讲师" width="100" />
        <el-table-column label="匿名" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isAnonymousRequired ? 'warning' : 'success'" size="small">
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
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status !== 1"
              size="small"
              type="success"
              text
              @click="handlePublish(row)"
            >发布</el-button>
            <el-button size="small" type="primary" text @click="viewStats(row)">查看统计</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建问卷弹窗 -->
    <el-dialog v-model="showCreateDialog" title="创建问卷" width="700px" @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
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
        <el-form-item label="被评讲师" prop="targetTeacherId">
          <el-select v-model="form.targetTeacherId" placeholder="选择讲师" style="width: 100%">
            <el-option
              v-for="teacher in teacherList"
              :key="teacher.id"
              :label="teacher.realName"
              :value="teacher.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="匿名调查">
          <el-switch v-model="form.isAnonymousRequired" :value="1" :inactive-value="0" />
          <span style="margin-left: 10px; color: #909399">开启后学生提交时无法记录身份信息</span>
        </el-form-item>
      </el-form>

      <div class="questions-section">
        <div class="section-header">
          <span>题目配置</span>
          <el-button type="primary" size="small" @click="addQuestion">添加题目</el-button>
        </div>

        <div v-for="(q, index) in form.questions" :key="index" class="question-item">
          <div class="question-header">
            <span class="question-num">第 {{ index + 1 }} 题</span>
            <el-button type="danger" size="small" text @click="removeQuestion(index)">删除</el-button>
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
                placeholder='{"min":1,"max":5,"labels":["非常不同意","不同意","一般","同意","非常同意"]}'
              />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 统计大屏 -->
    <el-dialog v-model="showStatsDialog" title="问卷统计" width="90%" top="20px">
      <SurveyStats v-if="showStatsDialog" :survey-id="currentSurveyId" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent, ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getSurveyList,
  createSurvey,
  publishSurvey,
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
  targetClassIds: [{ required: true, message: '请选择班级', trigger: 'change', type: 'array' }],
  targetTeacherId: [{ required: true, message: '请选择讲师', trigger: 'change' }]
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
  fetchTeacherList()
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
  form.targetTeacherId = 0
  form.questions = []
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
  min-height: 100%;
  font-family: 'JetBrains Mono', monospace;
}

.page :deep(.el-card) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
}

.page :deep(.el-card__header) {
  background: rgba(26, 26, 46, 0.4) !important;
  border-bottom: 1px solid #1a1a2e !important;
  padding: 15px 20px !important;
}

.page :deep(.el-card__body) {
  background: #0a0a0a !important;
  padding: 20px !important;
}

.page :deep(.el-table) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  --el-table-bg-color: #0a0a0a !important;
  --el-table-tr-bg-color: #0a0a0a !important;
  --el-table-header-bg-color: #111111 !important;
  --el-table-header-text-color: #00ffff !important;
  --el-table-text-color: #e0e0e0 !important;
  --el-table-border-color: #1a1a2e !important;
  --el-table-row-hover-bg-color: rgba(0, 255, 255, 0.05) !important;
}

.page :deep(.el-table__header-wrapper th) {
  background: #111111 !important;
  color: #00ffff !important;
  font-weight: 600 !important;
  border-bottom: 1px solid #1a1a2e !important;
}

.page :deep(.el-table__body-wrapper tr) {
  background: #0a0a0a !important;
}

.page :deep(.el-table__body-wrapper td) {
  background: #0a0a0a !important;
  border-bottom: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
}

.page :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.3) !important;
}

.page :deep(.el-dialog) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 30px) 0, 100% 30px, 100% 100%, 30px 100%, 0 calc(100% - 30px));
  box-shadow: 0 0 50px rgba(255, 16, 240, 0.2), 0 0 100px rgba(0, 0, 0, 0.8) !important;
}

.page :deep(.el-dialog__header) {
  background: rgba(26, 26, 46, 0.5) !important;
  border-bottom: 1px solid #1a1a2e !important;
  padding: 15px 20px !important;
}

.page :deep(.el-dialog__title) {
  color: #ff10f0 !important;
  font-weight: 600 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

.page :deep(.el-dialog__body) {
  background: #0a0a0a !important;
  padding: 25px !important;
  color: #e0e0e0 !important;
}

.page :deep(.el-dialog__footer) {
  background: rgba(26, 26, 46, 0.3) !important;
  border-top: 1px solid #1a1a2e !important;
  padding: 15px 20px !important;
}

.page :deep(.el-form-item__label) {
  color: #00ffff !important;
  font-weight: 500 !important;
}

.page :deep(.el-input__wrapper) {
  background: #111111 !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: none !important;
}

.page :deep(.el-input__inner) {
  color: #e0e0e0 !important;
}

.page :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3) !important;
}

.page :deep(.el-select__wrapper) {
  background: #111111 !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: none !important;
  color: #e0e0e0 !important;
}

.page :deep(.el-select__dropdown) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
}

.page :deep(.el-select-dropdown__item) {
  color: #e0e0e0 !important;
}

.page :deep(.el-select-dropdown__item.hover),
.page :deep(.el-select-dropdown__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
}

.page :deep(.el-date-editor) {
  --el-date-editor-width: 100% !important;
}

.page :deep(.el-picker__popper) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-bar span {
  font-weight: 600;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.4) !important;
  letter-spacing: 1px;
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
  font-weight: 600;
  color: #ff10f0 !important;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.4) !important;
}

.question-item {
  background: #111111;
  border: 1px solid #1a1a2e;
  clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px));
  padding: 15px;
  margin-bottom: 15px;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5) !important;
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-num {
  font-weight: 600;
  color: #ff10f0 !important;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.5) !important;
}

.option-config,
.scale-config {
  color: #909399;
  font-size: 13px;
  font-family: 'JetBrains Mono', monospace;
}

.page :deep(.el-switch.is-checked .el-switch__core) {
  background: linear-gradient(90deg, #00ffff, #ff10f0) !important;
  border-color: #ff10f0 !important;
}

.page :deep(.el-button--primary) {
  --el-button-bg-color: #ff10f0 !important;
  --el-button-border-color: #ff10f0 !important;
  --el-button-hover-bg-color: #ff10f0 !important;
  --el-button-hover-border-color: #ff10f0 !important;
  background: #ff10f0 !important;
  border-color: #ff10f0 !important;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.4) !important;
}

.page :deep(.el-button--success) {
  --el-button-bg-color: #39ff14 !important;
  --el-button-border-color: #39ff14 !important;
  --el-button-hover-bg-color: #39ff14 !important;
  --el-button-hover-border-color: #39ff14 !important;
  background: #39ff14 !important;
  border-color: #39ff14 !important;
  box-shadow: 0 0 15px rgba(57, 255, 20, 0.4) !important;
}

.page :deep(.el-button--danger) {
  --el-button-bg-color: #ff4444 !important;
  --el-button-border-color: #ff4444 !important;
  --el-button-hover-bg-color: #ff4444 !important;
  --el-button-hover-border-color: #ff4444 !important;
  background: #ff4444 !important;
  border-color: #ff4444 !important;
  box-shadow: 0 0 15px rgba(255, 68, 68, 0.4) !important;
}

.page :deep(.el-tag--warning) {
  --el-tag-bg-color: rgba(255, 152, 0, 0.2) !important;
  --el-tag-border-color: #ff9800 !important;
  --el-tag-text-color: #ff9800 !important;
}

.page :deep(.el-tag--success) {
  --el-tag-bg-color: rgba(57, 255, 20, 0.2) !important;
  --el-tag-border-color: #39ff14 !important;
  --el-tag-text-color: #39ff14 !important;
}

.page :deep(.el-tag--info) {
  --el-tag-bg-color: rgba(0, 255, 255, 0.2) !important;
  --el-tag-border-color: #00ffff !important;
  --el-tag-text-color: #00ffff !important;
}
</style>
