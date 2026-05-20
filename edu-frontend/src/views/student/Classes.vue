<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h2>加入班级</h2>
        <p>申请加入正在开课的班级，班主任审核通过后即可查看对应课程、考试和作业。</p>
      </div>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </header>

    <section class="class-grid" v-loading="loading">
      <el-card v-for="item in classes" :key="item.classId" shadow="never" class="class-card">
        <div class="class-head">
          <div>
            <h3>{{ item.className }}</h3>
            <p>{{ item.schoolName || '通用班级' }}</p>
          </div>
          <el-tag :type="statusTagType(item)">{{ statusText(item) }}</el-tag>
        </div>

        <div class="meta-row">
          <span>讲师</span>
          <strong>{{ item.teacherNames?.join('、') || '暂未分配' }}</strong>
        </div>
        <div class="meta-row">
          <span>班主任</span>
          <strong>{{ item.assistantNames?.join('、') || '暂未分配' }}</strong>
        </div>
        <div v-if="item.rejectReason" class="reject-reason">
          {{ item.rejectReason }}
        </div>

        <template #footer>
          <el-button
            type="primary"
            :disabled="!canApply(item)"
            :loading="applyingId === item.classId"
            @click="apply(item)"
          >
            {{ actionText(item) }}
          </el-button>
        </template>
      </el-card>
      <el-empty v-if="!loading && classes.length === 0" description="暂无可申请班级" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { applyJoinClass, getAvailableClasses, type StudentClassItem } from '@/api/student'

const loading = ref(false)
const applyingId = ref<number | null>(null)
const classes = ref<StudentClassItem[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await getAvailableClasses()
    classes.value = res.data || []
  } finally {
    loading.value = false
  }
}

function canApply(item: StudentClassItem) {
  return !item.joined && item.applicationStatus !== 'PENDING' && item.allowStudentApply
}

function actionText(item: StudentClassItem) {
  if (item.joined) return '已加入'
  if (item.applicationStatus === 'PENDING') return '审核中'
  if (item.applicationStatus === 'REJECTED') return '重新申请'
  return '申请加入'
}

function statusText(item: StudentClassItem) {
  if (item.joined) return '已加入'
  if (item.applicationStatus === 'PENDING') return '审核中'
  if (item.applicationStatus === 'REJECTED') return '已拒绝'
  return '可申请'
}

function statusTagType(item: StudentClassItem) {
  if (item.joined) return 'success'
  if (item.applicationStatus === 'PENDING') return 'warning'
  if (item.applicationStatus === 'REJECTED') return 'danger'
  return 'info'
}

async function apply(item: StudentClassItem) {
  applyingId.value = item.classId
  try {
    await applyJoinClass(item.classId)
    ElMessage.success('申请已提交')
    await loadData()
  } finally {
    applyingId.value = null
  }
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
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header h2 {
  margin: 0 0 6px;
}

.page-header p,
.class-head p {
  margin: 0;
  color: rgba(226, 232, 240, 0.62);
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.class-card {
  background: rgba(12, 20, 40, 0.82);
  border: 1px solid rgba(64, 128, 255, 0.2);
}

.class-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.class-head h3 {
  margin: 0 0 6px;
}

.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  color: rgba(226, 232, 240, 0.7);
}

.meta-row strong {
  color: #e2e8f0;
  text-align: right;
}

.reject-reason {
  margin-top: 12px;
  color: #f87171;
  line-height: 1.5;
}
</style>
