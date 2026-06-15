<template>
  <div class="page">
    <el-card shadow="never" class="survey-card">
      <template #header>
        <div class="card-header">
          <span class="header-bracket">[</span>
          <span>我的问卷</span>
          <span class="header-bracket">]</span>
        </div>
      </template>

      <el-table :data="surveyList" v-loading="loading" class="cyberpunk-table">
        <el-table-column prop="title" label="问卷标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="targetTeacherName" label="授课教师" width="120" />
        <el-table-column label="截止时间" width="160">
          <template #default="{ row }">
            <span :class="{ overdue: isOverdue(row.endTime) && !row.alreadySubmitted }">
              {{ formatTime(row.endTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="匿名" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isAnonymousRequired === 1 ? 'warning' : 'info'" size="small" class="cyberpunk-tag">
              {{ row.isAnonymousRequired === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.alreadySubmitted" type="success" size="small" class="cyberpunk-tag">已填写</el-tag>
            <el-tag v-else-if="isOverdue(row.endTime)" type="danger" size="small" class="cyberpunk-tag">已截止</el-tag>
            <el-tag v-else type="primary" size="small" class="cyberpunk-tag">待填写</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalQuestions" label="题目数" width="80" align="center" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.alreadySubmitted && !isOverdue(row.endTime)"
              size="small"
              type="primary"
              @click="fillSurvey(row)"
              class="action-btn"
            >填写问卷</el-button>
            <el-button
              v-else-if="row.alreadySubmitted"
              size="small"
              type="info"
              text
              class="disabled-btn"
            >已提交</el-button>
            <el-button
              v-else
              size="small"
              type="info"
              text
              disabled
              class="disabled-btn"
            >已截止</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && surveyList.length === 0" description="暂无问卷" class="cyberpunk-empty" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { getStudentSurveyList, type StudentSurveyItem } from '@/api/survey'

const router = useRouter()
const loading = ref(false)
const surveyList = ref<StudentSurveyItem[]>([])

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

function isOverdue(endTime: string): boolean {
  if (!endTime) return false
  return new Date(endTime) < new Date()
}

async function fetchSurveyList() {
  loading.value = true
  try {
    const res = await getStudentSurveyList()
    surveyList.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载问卷列表失败')
  } finally {
    loading.value = false
  }
}

function fillSurvey(survey: StudentSurveyItem) {
  router.push(`/student/survey/${survey.surveyId}`)
}

onMounted(() => {
  fetchSurveyList()
})
</script>

<style scoped>
.page {
  padding: 0;
  background: var(--bg-base);
  min-height: 100vh;
}

.survey-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.5);
}

:deep(.el-card__header) {
  background: var(--bg-surface) !important;
  border-bottom: 1px solid var(--border) !important;
}

:deep(.el-card__body) {
  background: var(--bg-surface) !important;
}

.card-header {
  font-weight: 600;
  font-size: 16px;
  font-family: 'JetBrains Mono', monospace;
  color: #00ffff;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.header-bracket {
  color: #ff10f0;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5);
}

.cyberpunk-table {
  background: transparent !important;
  font-family: 'JetBrains Mono', monospace;
}

:deep(.el-table) {
  background: transparent !important;
  color: var(--text-primary) !important;
}

:deep(.el-table__header-wrapper th) {
  background: var(--bg-surface) !important;
  color: #00ffff !important;
  border-bottom: 1px solid var(--border) !important;
  font-family: 'JetBrains Mono', monospace;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.3);
}

:deep(.el-table__body-wrapper tr) {
  background: var(--bg-surface) !important;
}

:deep(.el-table__body-wrapper td) {
  background: var(--bg-surface) !important;
  border-bottom: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
}

:deep(.el-table__body-wrapper tr:hover > td) {
  background: var(--bg-surface) !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.5) !important;
}

.overdue {
  color: #ff10f0 !important;
  text-shadow: 0 0 8px rgba(255, 16, 240, 0.4);
}

.cyberpunk-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid !important;
}

:deep(.el-tag--success) {
  border-color: #39ff14 !important;
  color: #39ff14 !important;
  box-shadow: 0 0 8px rgba(57, 255, 20, 0.3);
}

:deep(.el-tag--warning) {
  border-color: #ff9800 !important;
  color: #ff9800 !important;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.3);
}

:deep(.el-tag--danger) {
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 8px rgba(255, 16, 240, 0.3);
}

:deep(.el-tag--info) {
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3);
}

:deep(.el-tag--primary) {
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3);
}

.action-btn {
  background: transparent !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px);
}

.action-btn:hover {
  background: #00ffff !important;
  color: #000 !important;
}

.disabled-btn {
  color: var(--text-secondary) !important;
  font-family: 'JetBrains Mono', monospace;
}

.cyberpunk-empty {
  font-family: 'JetBrains Mono', monospace;
}

:deep(.el-empty__description) {
  color: var(--text-secondary) !important;
}

:deep(.el-empty__image svg) {
  fill: #00ffff !important;
  opacity: 0.3;
}

:deep(.el-loading-mask) {
  background-color: rgba(3, 3, 3, 0.9) !important;
}

:deep(.el-loading-spinner .circular) {
  stroke: #00ffff !important;
}

:deep(.el-table__empty-text) {
  color: var(--text-secondary) !important;
}
</style>
