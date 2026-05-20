<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="header-text">// 考试记录</span>
        </div>
      </template>

      <el-table :data="records" v-loading="loading">
        <el-table-column prop="title" label="考试标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="截止时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.deadline) }}
          </template>
        </el-table-column>
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
            <span v-if="row.scoreGained !== null" class="score-value">{{ row.scoreGained }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'GRADED'"
              type="primary"
              size="small"
              text
              @click="viewDetail(row)"
            >
              查看详情
            </el-button>
            <span v-else-if="row.status === 'SUBMITTED'" class="pending-text">待批改</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStudentExamRecords, type ExamRecordItem } from '@/api/exam'

const router = useRouter()
const loading = ref(false)
const records = ref<ExamRecordItem[]>([])

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
    RETURNED: 'info'
  }
  return map[status] || 'info'
}

function getStatusText(status: string): string {
  const map: Record<string, string> = {
    SUBMITTED: '待批改',
    GRADED: '已批改',
    RETURNED: '已发放'
  }
  return map[status] || status
}

async function loadRecords() {
  loading.value = true
  try {
    const res = await getStudentExamRecords()
    records.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载考试记录失败')
  } finally {
    loading.value = false
  }
}

function viewDetail(record: ExamRecordItem) {
  router.push(`/student/exam-result/${record.examId}`)
}

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.card-header {
  font-weight: 600;
}

.header-text {
  font-weight: 700;
  font-size: 14px;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: #00FFFF;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.score-value {
  font-weight: 700;
  color: #39FF14;
  text-shadow: 0 0 8px rgba(57, 255, 20, 0.3);
}

.pending-text {
  color: #FFFF00;
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
}
</style>
