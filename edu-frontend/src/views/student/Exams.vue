<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="header-text">// 我的考试</span>
        </div>
      </template>

      <el-table :data="examList" v-loading="loading">
        <el-table-column prop="examTitle" label="考试名称" min-width="180" />
        <el-table-column label="考试时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="totalScore" label="总分" width="80" align="center" />
        <el-table-column label="成绩" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.status === 'GRADED' && row.scoreGained !== undefined && row.scoreGained !== null" class="score-value">
              {{ row.scoreGained }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button 
              v-if="row.status === 'ONGOING' || row.status === 'UN'" 
              type="primary" 
              size="small"
              @click="startExam(row)"
            >
              {{ row.status === 'UN' ? '继续考试' : '开始考试' }}
            </el-button>
            <el-button 
              v-else-if="row.status === 'GRADED'" 
              type="info" 
              size="small"
              @click="viewResult(row)"
            >
              查看成绩
            </el-button>
            <el-button
              v-else-if="row.status === 'SUBMITTED'"
              type="warning"
              size="small"
              disabled
            >
              待批改
            </el-button>
            <el-button 
              v-else-if="row.status === 'NOT_STARTED'" 
              type="warning" 
              size="small"
              disabled
            >
              未开始
            </el-button>
            <el-button 
              v-else-if="row.status === 'EXPIRED'" 
              type="danger" 
              size="small"
              disabled
            >
              已过期
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="resultDialogVisible" title="考试成绩" width="500px">
      <div class="result-content" v-if="currentExam">
        <div class="result-title">{{ currentExam.examTitle }}</div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="考试时间">
            {{ formatTime(currentExam.startTime) }} ~ {{ formatTime(currentExam.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="总分">
            {{ currentExam.totalScore }}
          </el-descriptions-item>
          <el-descriptions-item label="得分">
            <span class="score">{{ currentExam.scoreGained ?? '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentExam.status)" size="small">
              {{ getStatusText(currentExam.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { getStudentExamList, type StudentExamListItem } from '@/api/exam'

const router = useRouter()
const loading = ref(false)
const examList = ref<StudentExamListItem[]>([])
const resultDialogVisible = ref(false)
const currentExam = ref<StudentExamListItem | null>(null)

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
    ONGOING: 'success',
    UN: 'warning',
    SUBMITTED: 'warning',
    GRADED: 'primary',
    NOT_STARTED: 'info',
    EXPIRED: 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    ONGOING: '进行中',
    UN: '答题中',
    SUBMITTED: '已提交',
    GRADED: '已完成',
    NOT_STARTED: '未开始',
    EXPIRED: '已过期'
  }
  return map[status] || status
}

async function loadExamList() {
  loading.value = true
  try {
    const res = await getStudentExamList()
    examList.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载考试列表失败')
  } finally {
    loading.value = false
  }
}

function startExam(exam: StudentExamListItem) {
  router.push({
    path: `/student/exam/${exam.examId}`,
    query: { title: exam.examTitle }
  })
}

function viewResult(exam: StudentExamListItem) {
  currentExam.value = exam
  resultDialogVisible.value = true
}

onMounted(() => {
  loadExamList()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
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

.result-content {
  padding: 10px 0;
}

.result-title {
  font-size: 18px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 20px;
  color: var(--text-primary);
  letter-spacing: 1px;
}

.score {
  font-size: 28px;
  font-weight: 700;
  color: #39FF14;
  text-shadow: 0 0 15px rgba(57, 255, 20, 0.5);
}

.score-value {
  font-weight: 700;
  color: #39FF14;
  text-shadow: 0 0 8px rgba(57, 255, 20, 0.3);
}
</style>
