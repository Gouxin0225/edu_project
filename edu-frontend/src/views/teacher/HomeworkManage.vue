<template>
  <div class="page">
    <!-- 作业列表视图 -->
    <template v-if="!isGrading">
      <el-card shadow="never">
        <template #header>
          <div class="header-bar">
            <span>作业管理</span>
            <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">创建作业</el-button>
          </div>
        </template>

        <el-table :data="homeworkList" v-loading="loading" stripe border>
          <el-table-column prop="title" label="作业标题" min-width="180" show-overflow-tooltip />
          <el-table-column label="截止时间" width="160">
            <template #default="{ row }">
              {{ formatTime(row.deadline) }}
            </template>
          </el-table-column>
          <el-table-column label="提交进度" width="120" align="center">
            <template #default="{ row }">
              <span class="progress-text">
                {{ row.totalSubmissions || 0 }} / {{ row.totalSubmissions || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="gradedSubmissions" label="已批改" width="80" align="center">
            <template #default="{ row }">
              <el-tag type="success" size="small">{{ row.gradedSubmissions || 0 }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="pendingSubmissions" label="待批改" width="80" align="center">
            <template #default="{ row }">
              <el-tag type="warning" size="small">{{ row.pendingSubmissions || 0 }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" text @click="enterGrading(row)">批改</el-button>
              <el-button size="small" type="danger" text @click="handleDeleteHomework(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 批改视图 -->
    <template v-else>
      <el-card shadow="never">
        <template #header>
          <div class="header-bar">
            <el-button text @click="isGrading = false" class="back-btn">
              <el-icon><ArrowLeft /></el-icon>
              返回列表
            </el-button>
            <span class="hw-title">{{ currentHomework?.title }}</span>
          </div>
        </template>

        <!-- 学生提交列表 -->
        <el-table :data="submissionList" v-loading="subLoading" stripe border>
          <el-table-column prop="studentName" label="学生姓名" width="100" />
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">
              {{ row.submitTime ? formatTime(row.submitTime) : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="得分" width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.scoreGained !== null" class="score">{{ row.scoreGained }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" text @click="viewSubmission(row)">查看</el-button>
              <el-button
                v-if="row.status === 'SUBMITTED' || row.status === 'RETURNED'"
                size="small"
                type="success"
                text
                @click="openGradeDialog(row)"
              >评分</el-button>
              <el-button
                v-if="row.status === 'SUBMITTED' || row.status === 'RETURNED'"
                size="small"
                type="danger"
                text
                @click="openReturnDialog(row)"
              >打回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 创建作业弹窗 -->
    <el-dialog v-model="showCreateDialog" title="创建作业" width="600px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="作业标题" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入作业标题" />
        </el-form-item>
        <el-form-item label="作业内容" prop="content">
          <el-input v-model="createForm.content" type="textarea" :rows="4" placeholder="请输入作业内容" />
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker
            v-model="createForm.deadline"
            type="datetime"
            placeholder="选择截止时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="目标班级">
          <el-checkbox-group v-model="createForm.targetClassIds">
            <el-checkbox v-for="cls in myClasses" :key="cls.id" :value="cls.id">
              {{ cls.className }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="附件链接">
          <el-input v-model="createForm.attachments" placeholder="可选的附件链接" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 提交详情弹窗 -->
    <el-dialog v-model="showSubmissionDetail" title="作业详情" width="700px">
      <div v-if="currentSubmission" class="submission-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生姓名">{{ currentSubmission.studentName }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ currentSubmission.submitTime ? formatTime(currentSubmission.submitTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentSubmission.status)" size="small">
              {{ getStatusText(currentSubmission.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="得分">
            {{ currentSubmission.totalScoreGained ?? '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="answer-section">
          <h4>作业内容</h4>
          <div class="answer-item">
            <div class="q-content" v-if="currentSubmission.content">{{ currentSubmission.content }}</div>
            <div class="q-content empty" v-else>（无内容）</div>
            <div class="q-link" v-if="currentSubmission.gitLink">
              <span class="label">CSDN链接：</span>
              <a :href="currentSubmission.gitLink" target="_blank">{{ currentSubmission.gitLink }}</a>
            </div>
            <div class="q-link" v-if="currentSubmission.fileUrl">
              <span class="label">附件：</span>
              <a @click.prevent="downloadFile(currentSubmission.fileUrl)" style="color: #409eff; cursor: pointer;">{{ currentSubmission.fileUrl }}</a>
            </div>
          </div>
        </div>

        <div class="comment-section" v-if="currentSubmission.teacherComment">
          <h4>教师评语</h4>
          <div class="comment-text">{{ currentSubmission.teacherComment }}</div>
        </div>
      </div>
    <template #footer>
        <el-button @click="showSubmissionDetail = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 评分弹窗 -->
    <el-dialog v-model="showGradeDialog" title="评分" width="500px">
      <el-form :model="gradeForm" ref="gradeFormRef" label-width="80px">
        <el-form-item label="得分">
          <el-input-number v-model="gradeForm.scoreGained" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="gradeForm.teacherComment" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGradeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleGrade" :loading="grading">提交</el-button>
      </template>
    </el-dialog>

    <!-- 打回弹窗 -->
    <el-dialog v-model="showReturnDialog" title="打回重做" width="500px">
      <el-form :model="returnForm" :rules="returnRules" ref="returnFormRef" label-width="80px">
        <el-form-item label="打回意见" prop="teacherComment">
          <el-input v-model="returnForm.teacherComment" type="textarea" :rows="4" placeholder="请输入打回意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReturnDialog = false">取消</el-button>
        <el-button type="danger" @click="handleReturn" :loading="returning">确认打回</el-button>
      </template>
    </el-dialog>

    <!-- 历史版本弹窗 -->
    <el-dialog v-model="showHistoryDialog" title="提交历史" width="700px">
      <el-timeline v-if="historyList.length">
        <el-timeline-item
          v-for="item in historyList"
          :key="item.submissionId"
          :type="item.status === 'GRADED' ? 'success' : item.status === 'RETURNED' ? 'danger' : 'primary'"
          :timestamp="formatTime(item.submitTime)"
        >
          <div class="history-item">
            <div class="history-header">
              <span class="version">第{{ item.version }}版</span>
              <el-tag :type="getStatusType(item.status)" size="small">
                {{ getStatusText(item.status) }}
              </el-tag>
              <span v-if="item.scoreGained !== null" class="score">得分: {{ item.scoreGained }}</span>
            </div>
            <div class="history-content">{{ item.content }}</div>
            <div class="history-link" v-if="item.gitLink">
              <a :href="item.gitLink" target="_blank">{{ item.gitLink }}</a>
            </div>
            <div class="history-file" v-if="item.fileUrl">
              <span class="label">附件：</span>
              <a @click.prevent="downloadFile(item.fileUrl)" style="color: #409eff; cursor: pointer;">{{ item.fileUrl }}</a>
            </div>
            <div class="history-comment" v-if="item.teacherComment">
              <span class="label">教师评语：</span>{{ item.teacherComment }}
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无历史版本" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, ArrowLeft } from '@element-plus/icons-vue'
import axios from 'axios'
import {
  getTeacherHomeworkList,
  createHomework,
  deleteHomework,
  getHomeworkSubmissions,
  getHomeworkSubmissionDetail,
  gradeHomework,
  returnHomework,
  getSubmissionHistory,
  type HomeworkListItem,
  type SubmissionHistoryItem,
  type HomeworkSubmissionDetail
} from '@/api/homework'
import { getMyClasses, type TeacherClass } from '@/api/teacher'

const loading = ref(false)
const subLoading = ref(false)
const creating = ref(false)
const grading = ref(false)
const returning = ref(false)

const homeworkList = ref<HomeworkListItem[]>([])
const myClasses = ref<TeacherClass[]>([])
const submissionList = ref<any[]>([])
const historyList = ref<SubmissionHistoryItem[]>([])

const isGrading = ref(false)
const showCreateDialog = ref(false)
const showSubmissionDetail = ref(false)
const showGradeDialog = ref(false)
const showReturnDialog = ref(false)
const showHistoryDialog = ref(false)

const currentHomework = ref<HomeworkListItem | null>(null)
const currentSubmission = ref<HomeworkSubmissionDetail | null>(null)
const currentSubmissionForHistory = ref<any | null>(null)

const createFormRef = ref()
const gradeFormRef = ref()
const returnFormRef = ref()

const createForm = reactive({
  title: '',
  content: '',
  deadline: '',
  targetClassIds: [] as number[],
  attachments: ''
})

const gradeForm = reactive({
  scoreGained: 0,
  teacherComment: ''
})

const returnForm = reactive({
  teacherComment: ''
})

const createRules = {
  title: [{ required: true, message: '请输入作业标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入作业内容', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }]
}

const returnRules = {
  teacherComment: [{ required: true, message: '请输入打回意见', trigger: 'blur' }]
}

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

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    SUBMITTED: 'warning',
    GRADED: 'success',
    RETURNED: 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    SUBMITTED: '待批改',
    GRADED: '已批改',
    RETURNED: '已打回'
  }
  return map[status] || status
}

async function fetchHomeworkList() {
  loading.value = true
  try {
    const res = await getTeacherHomeworkList()
    homeworkList.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载作业列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchMyClasses() {
  try {
    const res = await getMyClasses()
    myClasses.value = res.data || []
  } catch (error: any) {
    console.error('加载班级列表失败', error)
  }
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    await createHomework({
      title: createForm.title,
      content: createForm.content,
      deadline: createForm.deadline,
      targetClassIds: createForm.targetClassIds,
      attachments: createForm.attachments
    })
    ElMessage.success('作业创建成功')
    showCreateDialog.value = false
    fetchHomeworkList()
    createForm.title = ''
    createForm.content = ''
    createForm.deadline = ''
    createForm.targetClassIds = []
    createForm.attachments = ''
  } catch (error: any) {
    ElMessage.error(error.message || '创建作业失败')
  } finally {
    creating.value = false
  }
}

async function handleDeleteHomework(homework: HomeworkListItem) {
  try {
    await ElMessageBox.confirm(`确认删除作业「${homework.title}」？删除后学生端将不再显示该作业。`, '删除作业', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteHomework(homework.homeworkId)
    ElMessage.success('作业已删除')
    fetchHomeworkList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除作业失败')
    }
  }
}

async function enterGrading(homework: HomeworkListItem) {
  currentHomework.value = homework
  isGrading.value = true
  await fetchSubmissions(homework.homeworkId)
}

async function fetchSubmissions(homeworkId: number) {
  subLoading.value = true
  try {
    const res = await getHomeworkSubmissions(homeworkId)
    submissionList.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载提交列表失败')
  } finally {
    subLoading.value = false
  }
}

async function viewSubmission(row: any) {
  try {
    const res = await getHomeworkSubmissionDetail(currentHomework.value!.homeworkId, row.submissionId)
    currentSubmission.value = res.data
    showSubmissionDetail.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '加载详情失败')
  }
}

function openGradeDialog(row: any) {
  currentSubmissionForHistory.value = row
  gradeForm.scoreGained = row.scoreGained ?? 0
  gradeForm.teacherComment = ''
  showGradeDialog.value = true
}

async function handleGrade() {
  try {
    await gradeHomework(currentHomework.value!.homeworkId, currentSubmissionForHistory.value.submissionId, {
      scoreGained: gradeForm.scoreGained,
      teacherComment: gradeForm.teacherComment
    })
    ElMessage.success('评分成功')
    showGradeDialog.value = false
    await fetchSubmissions(currentHomework.value!.homeworkId)
  } catch (error: any) {
    ElMessage.error(error.message || '评分失败')
  }
}

function openReturnDialog(row: any) {
  currentSubmissionForHistory.value = row
  returnForm.teacherComment = ''
  showReturnDialog.value = true
}

async function handleReturn() {
  const valid = await returnFormRef.value?.validate().catch(() => false)
  if (!valid) return

  try {
    await returnHomework(currentHomework.value!.homeworkId, currentSubmissionForHistory.value.submissionId, {
      teacherComment: returnForm.teacherComment
    })
    ElMessage.success('作业已打回')
    showReturnDialog.value = false
    await fetchSubmissions(currentHomework.value!.homeworkId)
  } catch (error: any) {
    ElMessage.error(error.message || '打回失败')
  }
}

async function viewHistory(row: any) {
  currentSubmissionForHistory.value = row
  try {
    const res = await getSubmissionHistory(currentHomework.value!.homeworkId, row.submissionId)
    historyList.value = res.data || []
    showHistoryDialog.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '加载历史失败')
  }
}

async function downloadFile(filename: string) {
  if (!filename) return
  try {
    const token = localStorage.getItem('token')
    const response = await axios.get(`/api/file/download?filename=${encodeURIComponent(filename)}`, {
      headers: {
        Authorization: `Bearer ${token}`
      },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error: any) {
    ElMessage.error('文件下载失败')
  }
}

onMounted(() => {
  fetchHomeworkList()
  fetchMyClasses()
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

.page :deep(.el-card) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
}

.page :deep(.el-card__header) {
  padding: 12px 20px;
  background: transparent !important;
  border-bottom: 1px solid #1a1a2e !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
}

.page :deep(.el-card__body) {
  background: transparent !important;
  padding: 16px 20px !important;
}

.header-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-bar span {
  font-weight: 600;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.header-bar :deep(.el-button) {
  font-family: 'JetBrains Mono', monospace;
  border: 1px solid !important;
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
}

.header-bar :deep(.el-button--primary) {
  background: transparent !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
}

.header-bar :deep(.el-button--primary:hover) {
  background: rgba(255, 16, 240, 0.1) !important;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.3) !important;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #ff10f0 !important;
  background: transparent !important;
  border-color: #ff10f0 !important;
}

.back-btn:hover {
  background: rgba(255, 16, 240, 0.1) !important;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.3) !important;
}

.back-btn :deep(.el-icon) {
  color: #ff10f0 !important;
}

.hw-title {
  font-weight: 600;
  margin-left: 12px;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.page :deep(.el-table) {
  --el-table-bg-color: #0a0a0a !important;
  --el-table-tr-bg-color: #0a0a0a !important;
  --el-table-header-bg-color: rgba(26, 26, 46, 0.8) !important;
  --el-table-header-text-color: #00ffff !important;
  --el-table-text-color: #e0e0e0 !important;
  --el-table-border-color: #1a1a2e !important;
  --el-table-row-hover-bg-color: rgba(0, 255, 255, 0.05) !important;
  font-family: 'JetBrains Mono', monospace;
  border: 1px solid #1a1a2e !important;
}

.page :deep(.el-table th.el-table__cell) {
  background: rgba(26, 26, 46, 0.8) !important;
  border-bottom: 1px solid #1a1a2e !important;
  color: #00ffff !important;
  font-weight: 600;
}

.page :deep(.el-table td.el-table__cell) {
  border-bottom: 1px solid #1a1a2e !important;
}

.page :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.progress-text {
  color: #00ffff !important;
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.5);
}

.score {
  color: #39ff14 !important;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(57, 255, 20, 0.5);
}

.page :deep(.el-tag) {
  font-family: 'JetBrains Mono', monospace;
  border: 1px solid !important;
}

.page :deep(.el-tag--success) {
  background: transparent !important;
  border-color: #39ff14 !important;
  color: #39ff14 !important;
  box-shadow: 0 0 10px rgba(57, 255, 20, 0.2);
}

.page :deep(.el-tag--warning) {
  background: transparent !important;
  border-color: #ff9800 !important;
  color: #ff9800 !important;
  box-shadow: 0 0 10px rgba(255, 152, 0, 0.2);
}

.page :deep(.el-tag--danger) {
  background: transparent !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.2);
}

.page :deep(.el-button--text) {
  color: #00ffff !important;
  background: transparent !important;
  border: none !important;
  font-family: 'JetBrains Mono', monospace;
}

.page :deep(.el-button--text:hover) {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5);
}

.page :deep(.el-button--success.is-text) {
  color: #39ff14 !important;
}

.page :deep(.el-button--success.is-text:hover) {
  color: #39ff14 !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.5);
}

.page :deep(.el-button--danger.is-text) {
  color: #ff10f0 !important;
}

.page :deep(.el-button--danger.is-text:hover) {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5);
}

.page :deep(.el-dialog) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  clip-path: polygon(0 0, calc(100% - 25px) 0, 100% 25px, 100% 100%, 25px 100%, 0 calc(100% - 25px));
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.15), 0 0 60px rgba(255, 16, 240, 0.1) !important;
}

.page :deep(.el-dialog__header) {
  border-bottom: 1px solid #1a1a2e !important;
  padding: 16px 20px !important;
}

.page :deep(.el-dialog__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 600;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.page :deep(.el-dialog__body) {
  background: transparent !important;
  padding: 20px !important;
  color: #e0e0e0 !important;
}

.page :deep(.el-dialog__footer) {
  border-top: 1px solid #1a1a2e !important;
  padding: 12px 20px !important;
}

.page :deep(.el-form-item__label) {
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 500;
}

.page :deep(.el-input__wrapper) {
  background: #030303 !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.05) !important;
}

.page :deep(.el-input__inner) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.page :deep(.el-input__inner::placeholder) {
  color: rgba(224, 224, 224, 0.4) !important;
}

.page :deep(.el-input:hover .el-input__wrapper) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.2) !important;
}

.page :deep(.el-input__wrapper.is-focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.3) !important;
}

.page :deep(.el-textarea__inner) {
  background: #030303 !important;
  border: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.page :deep(.el-textarea__inner:hover) {
  border-color: #00ffff !important;
}

.page :deep(.el-textarea__inner:focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.3) !important;
}

.page :deep(.el-input-number) {
  --el-input-bg-color: #030303;
  --el-input-border-color: #1a1a2e;
  --el-input-text-color: #00ffff;
}

.page :deep(.el-input-number .el-input__wrapper) {
  background: #030303 !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.1) !important;
}

.page :deep(.el-input-number .el-input__inner) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.5);
}

.page :deep(.el-input-number__decrease),
.page :deep(.el-input-number__increase) {
  background: #0a0a0a !important;
  border-color: #1a1a2e !important;
  color: #00ffff !important;
}

.page :deep(.el-input-number__decrease:hover),
.page :deep(.el-input-number__increase:hover) {
  color: #ff10f0 !important;
}

.page :deep(.el-checkbox) {
  --el-checkbox-bg-color: transparent;
  --el-checkbox-border-color: #1a1a2e;
  --el-checkbox-text-color: #e0e0e0;
  --el-checkbox-checked-bg-color: rgba(0, 255, 255, 0.1);
  --el-checkbox-checked-border-color: #00ffff;
  --el-checkbox-checked-text-color: #00ffff;
}

.page :deep(.el-checkbox__inner) {
  background: transparent !important;
  border-color: #1a1a2e !important;
}

.page :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: rgba(0, 255, 255, 0.1) !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

.page :deep(.el-checkbox__label) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.page :deep(.el-date-editor) {
  --el-input-bg-color: #030303;
  --el-date-editor-width: 100%;
}

.page :deep(.el-picker-panel) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
}

.page :deep(.el-date-picker__header-label) {
  color: #00ffff !important;
}

.page :deep(.el-date-table th) {
  color: #e0e0e0 !important;
}

.page :deep(.el-button) {
  font-family: 'JetBrains Mono', monospace;
  border: 1px solid !important;
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
}

.page :deep(.el-button--default) {
  background: transparent !important;
  border-color: #1a1a2e !important;
  color: #e0e0e0 !important;
}

.page :deep(.el-button--default:hover) {
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.2) !important;
}

.page :deep(.el-button--primary) {
  background: linear-gradient(135deg, #00ffff, #ff10f0) !important;
  border: none !important;
  color: #0a0a0a !important;
  font-weight: 600;
}

.page :deep(.el-button--primary:hover) {
  box-shadow: 0 0 25px rgba(0, 255, 255, 0.4), 0 0 50px rgba(255, 16, 240, 0.2) !important;
  transform: translateY(-1px);
}

.page :deep(.el-button--danger) {
  background: transparent !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
}

.page :deep(.el-button--danger:hover) {
  background: rgba(255, 16, 240, 0.1) !important;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.3) !important;
}

.submission-detail {
  padding: 12px 0;
}

.answer-section,
.comment-section {
  margin-top: 20px;
}

.answer-section h4,
.comment-section h4 {
  margin-bottom: 12px;
  font-weight: 600;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.3);
}

.answer-item {
  padding: 12px;
  background: rgba(26, 26, 46, 0.5);
  border: 1px solid #1a1a2e;
  border-left: 3px solid #00ffff;
  border-radius: 0;
  margin-bottom: 12px;
}

.q-content {
  font-size: 14px;
  margin-bottom: 8px;
  white-space: pre-wrap;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.q-content.empty {
  color: rgba(224, 224, 224, 0.5) !important;
}

.q-link {
  font-size: 13px;
  margin-bottom: 4px;
}

.q-link a {
  color: #00ffff !important;
  text-decoration: none;
}

.q-link a:hover {
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.q-link .label {
  color: rgba(224, 224, 224, 0.6) !important;
  font-family: 'JetBrains Mono', monospace;
}

.comment-text {
  padding: 12px;
  background: rgba(255, 16, 240, 0.1);
  border: 1px solid rgba(255, 16, 240, 0.3);
  border-left: 3px solid #ff10f0;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.page :deep(.el-descriptions) {
  --el-descriptions-table-border: 1px solid #1a1a2e;
}

.page :deep(.el-descriptions__label) {
  background: rgba(26, 26, 46, 0.5) !important;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace;
  border-color: #1a1a2e !important;
}

.page :deep(.el-descriptions__content) {
  background: transparent !important;
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
  border-color: #1a1a2e !important;
}

.page :deep(.el-descriptions__cell) {
  border-color: #1a1a2e !important;
}

.page :deep(.el-timeline) {
  --el-timeline-node-color: #00ffff;
}

.page :deep(.el-timeline-item__node) {
  background: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.page :deep(.el-timeline-item__content) {
  color: #e0e0e0 !important;
}

.page :deep(.el-timeline-item__timestamp) {
  color: rgba(224, 224, 224, 0.6) !important;
  font-family: 'JetBrains Mono', monospace;
}

.page :deep(.el-empty__description) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.history-item {
  padding-bottom: 8px;
}

.history-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.version {
  font-weight: 600;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace;
}

.history-content {
  font-size: 14px;
  color: #e0e0e0 !important;
  margin-bottom: 8px;
  white-space: pre-wrap;
  font-family: 'JetBrains Mono', monospace;
}

.history-link {
  font-size: 13px;
  margin-bottom: 4px;
}

.history-link a {
  color: #00ffff !important;
}

.history-file {
  font-size: 13px;
  margin-bottom: 4px;
}

.history-file a {
  color: #00ffff !important;
}

.history-comment {
  font-size: 13px;
  color: rgba(224, 224, 224, 0.6) !important;
}

.history-comment .label {
  color: #ff10f0 !important;
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

.page :deep(.el-loading-mask) {
  background: rgba(3, 3, 3, 0.9) !important;
}

.page :deep(.el-loading-spinner .circular) {
  stroke: #00ffff !important;
}

.page :deep(.el-loading-text) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
}
</style>
