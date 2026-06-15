<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2>入班审核</h2>
        <p>审核学生自主提交的入班申请，通过后学生即可正常跟班学习。</p>
      </div>
      <div class="actions">
        <el-select v-model="filters.classId" placeholder="全部班级" clearable style="width: 180px" @change="loadData">
          <el-option v-for="item in classes" :key="item.id" :label="item.className" :value="item.id" />
        </el-select>
        <el-select v-model="filters.status" placeholder="状态" style="width: 130px" @change="loadData">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="全部" value="ALL" />
        </el-select>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </header>

    <el-card shadow="never" class="table-card">
      <el-table :data="applications" v-loading="loading" stripe>
        <el-table-column label="学生" min-width="180">
          <template #default="{ row }">
            <div class="student-cell">
              <strong>{{ row.studentName }}</strong>
              <span>{{ row.phone || row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="schoolName" label="学校" min-width="160" show-overflow-tooltip />
        <el-table-column prop="className" label="申请班级" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.applyTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" text @click="approve(row)">通过</el-button>
              <el-button type="danger" text @click="openReject(row)">拒绝</el-button>
            </template>
            <span v-else class="muted">{{ row.auditorName || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="rejectVisible" title="拒绝申请" width="420px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请输入拒绝原因" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Refresh } from '@element-plus/icons-vue'
import {
  approveJoinApplication,
  getJoinApplications,
  getMyClasses,
  rejectJoinApplication,
  type ClassJoinApplication,
  type TeacherClass
} from '@/api/teacher'

const loading = ref(false)
const rejecting = ref(false)
const classes = ref<TeacherClass[]>([])
const applications = ref<ClassJoinApplication[]>([])
const rejectVisible = ref(false)
const rejectTarget = ref<ClassJoinApplication | null>(null)
const rejectReason = ref('')
const filters = reactive({
  classId: null as number | null,
  status: 'PENDING'
})

async function loadData() {
  loading.value = true
  try {
    if (!classes.value.length) {
      const classRes = await getMyClasses()
      classes.value = classRes.data || []
    }
    const res = await getJoinApplications({
      classId: filters.classId || undefined,
      status: filters.status
    })
    applications.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function approve(row: ClassJoinApplication) {
  await ElMessageBox.confirm(`确认通过 ${row.studentName} 加入 ${row.className}？`, '审核确认')
  await approveJoinApplication(row.id)
  ElMessage.success('已通过申请')
  await loadData()
}

function openReject(row: ClassJoinApplication) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function submitReject() {
  if (!rejectTarget.value) return
  rejecting.value = true
  try {
    await rejectJoinApplication(rejectTarget.value.id, rejectReason.value || '不符合入班条件')
    ElMessage.success('已拒绝申请')
    rejectVisible.value = false
    await loadData()
  } finally {
    rejecting.value = false
  }
}

function statusText(status: string) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELED: '已取消' }[status] || status
}

function statusType(status: string) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELED: 'info' }[status] || 'info'
}

function formatTime(value?: string | null) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

onMounted(loadData)
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header h2 {
  margin: 0 0 6px;
}

.page-header p {
  margin: 0;
  color: rgba(226, 232, 240, 0.62);
}

.actions {
  display: flex;
  gap: 10px;
}

.table-card {
  background: rgba(12, 20, 40, 0.82);
  border: 1px solid rgba(64, 128, 255, 0.2);
}

.student-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.student-cell span,
.muted {
  color: rgba(226, 232, 240, 0.58);
}
</style>
