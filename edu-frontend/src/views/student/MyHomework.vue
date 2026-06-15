<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="header-bar">
          <span class="header-text">// 我的作业</span>
        </div>
      </template>

      <el-table :data="homeworkList" v-loading="loading" stripe border>
        <el-table-column prop="title" label="作业标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="content" label="作业内容" min-width="250" show-overflow-tooltip />
        <el-table-column label="截止时间" width="160">
          <template #default="{ row }">
            <span :class="{ overdue: isOverdue(row.deadline) && !row.status }">
              {{ formatTime(row.deadline) }}
            </span>
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
            <el-button
              v-if="!row.status"
              size="small"
              type="primary"
              @click="openSubmitDialog(row)"
            >提交</el-button>
            <el-button
              v-else-if="row.status === 'SUBMITTED'"
              size="small"
              type="info"
              text
            >待批改</el-button>
            <el-button
              v-else-if="row.status === 'RETURNED'"
              size="small"
              type="warning"
              @click="openSubmitDialog(row, true)"
            >重新提交</el-button>
            <el-button
              v-else-if="row.status === 'GRADED'"
              size="small"
              type="success"
              text
              @click="viewHistory(row)"
            >查看历史</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showCommentDialog" title="作业已打回" width="500px">
      <div class="return-comment" v-if="currentHomework">
        <p class="comment-label">教师评语：</p>
        <div class="comment-content">{{ returnComment }}</div>
      </div>
      <template #footer>
        <el-button @click="showCommentDialog = false">关闭</el-button>
        <el-button type="primary" @click="reSubmit">重新提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showSubmitDialog" :title="isResubmit ? '重新提交作业' : '提交作业'" width="600px">
      <el-form :model="submitForm" :rules="submitRules" ref="submitFormRef" label-width="100px">
        <el-form-item label="作业内容" prop="content">
          <el-input
            v-model="submitForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入作业内容"
          />
        </el-form-item>
        <el-form-item label="CSDN链接">
          <el-input v-model="submitForm.gitLink" placeholder="可选，如 https://blog.csdn.net/xxx/article/details/xxx" />
        </el-form-item>
        <el-form-item label="文件上传">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">可选，支持常见文档、图片和压缩包，最大 20MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSubmitDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>

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
import { ElMessage } from 'element-plus/es/components/message/index'
import axios from 'axios'
import {
  getStudentHomeworkList,
  submitHomework,
  getSubmissionHistory,
  type StudentHomeworkItem,
  type SubmissionHistoryItem
} from '@/api/homework'

const loading = ref(false)
const submitting = ref(false)

const homeworkList = ref<StudentHomeworkItem[]>([])
const historyList = ref<SubmissionHistoryItem[]>([])

const showSubmitDialog = ref(false)
const showHistoryDialog = ref(false)
const showCommentDialog = ref(false)
const isResubmit = ref(false)
const returnComment = ref('')

const currentHomework = ref<StudentHomeworkItem | null>(null)

const submitFormRef = ref()
const uploadRef = ref()

const submitForm = reactive({
  content: '',
  gitLink: '',
  fileUrl: ''
})

const submitRules = {
  content: [{ required: true, message: '请输入作业内容', trigger: 'blur' }]
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

function isOverdue(deadline: string): boolean {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

function getStatusType(status: string | undefined): string {
  if (!status) return 'info'
  const map: Record<string, string> = {
    SUBMITTED: 'warning',
    GRADED: 'success',
    RETURNED: 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status: string | undefined): string {
  if (!status) return '待提交'
  const map: Record<string, string> = {
    SUBMITTED: '已提交',
    GRADED: '已批改',
    RETURNED: '已打回'
  }
  return map[status] || status
}

async function fetchHomeworkList() {
  loading.value = true
  try {
    const res = await getStudentHomeworkList()
    homeworkList.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载作业列表失败')
  } finally {
    loading.value = false
  }
}

function openSubmitDialog(homework: StudentHomeworkItem, resubmit = false) {
  currentHomework.value = homework
  isResubmit.value = resubmit
  submitForm.content = ''
  submitForm.gitLink = ''
  submitForm.fileUrl = ''
  showSubmitDialog.value = true
}

async function handleFileChange(file: any) {
  const token = localStorage.getItem('token')
  const formData = new FormData()
  formData.append('file', file.raw)
  try {
    const res = await axios.post('/api/file/upload', formData, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data'
      }
    })
    if (res.data.code === 200) {
      submitForm.fileUrl = res.data.data
      ElMessage.success('文件上传成功')
    } else {
      ElMessage.error(res.data.message || '文件上传失败')
    }
  } catch (error: any) {
    ElMessage.error('文件上传失败')
  }
}

function handleFileRemove() {
  submitForm.fileUrl = ''
}

async function handleSubmit() {
  const valid = await submitFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (isOverdue(currentHomework.value!.deadline) && !isResubmit.value) {
    ElMessage.warning('作业已截止，无法提交')
    return
  }

  submitting.value = true
  try {
    await submitHomework(currentHomework.value!.homeworkId, {
      content: submitForm.content,
      gitLink: submitForm.gitLink,
      fileUrl: submitForm.fileUrl
    })
    ElMessage.success('提交成功')
    showSubmitDialog.value = false
    fetchHomeworkList()
  } catch (error: any) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function viewHistory(homework: StudentHomeworkItem) {
  currentHomework.value = homework
  if (!homework.submissionId) {
    ElMessage.error('找不到提交记录')
    return
  }
  try {
    const res = await getSubmissionHistory(homework.homeworkId, homework.submissionId)
    historyList.value = res.data || []
    showHistoryDialog.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '加载历史失败')
  }
}

function reSubmit() {
  showCommentDialog.value = false
  openSubmitDialog(currentHomework.value!, true)
}

onMounted(() => {
  fetchHomeworkList()
})
</script>

<style scoped>
.page {
  padding: 20px;
}

.header-bar {
  display: flex;
  align-items: center;
}

.header-text {
  font-weight: 700;
  font-size: 14px;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: #00FFFF;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.overdue {
  color: #FF10F0;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.5);
}

.score {
  color: #39FF14;
  font-weight: 600;
  text-shadow: 0 0 8px rgba(57, 255, 20, 0.3);
}

.return-comment {
  padding: 12px;
  background: rgba(255, 16, 240, 0.05);
  border: 1px solid rgba(255, 16, 240, 0.2);
}

.comment-label {
  color: #FF10F0;
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 1px;
}

.comment-content {
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  white-space: pre-wrap;
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
  font-weight: 700;
  color: #00FFFF;
}

.history-content {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 8px;
  white-space: pre-wrap;
}

.history-link {
  font-size: 12px;
  margin-bottom: 4px;
}

.history-link a {
  color: #00FFFF;
}

.history-comment {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.history-comment .label {
  color: #FF10F0;
}
</style>
