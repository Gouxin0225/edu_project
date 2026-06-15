<template>
  <div class="page">
    <el-card shadow="never" class="cyber-card">
      <div class="toolbar">
        <span class="card-title">未上课程池</span>
      </div>

      <div class="filter-bar">
        <el-input
          v-model="filters.schoolName"
          placeholder="校区"
          clearable
          style="width:160px"
          class="cyber-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-input
          v-model="filters.keyword"
          placeholder="年级关键词"
          :prefix-icon="Search"
          clearable
          style="width:180px"
          class="cyber-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filters.courseId" filterable clearable placeholder="课程" style="width:170px" class="cyber-select" @change="handleSearch">
          <el-option v-for="course in courseOptions" :key="course.id" :label="course.courseName" :value="course.id" />
        </el-select>
        <el-select v-model="filters.courseDirection" clearable placeholder="方向" style="width:140px" class="cyber-select" @change="handleSearch">
          <el-option label="网络" value="NETWORK" />
          <el-option label="开发" value="DEV" />
          <el-option label="Linux" value="LINUX" />
          <el-option label="Web" value="WEB" />
          <el-option label="数据库" value="DB" />
          <el-option label="安全" value="SECURITY" />
          <el-option label="云计算" value="CLOUD" />
        </el-select>
        <el-button type="primary" :icon="Search" class="cyber-btn cyber-btn-primary" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" class="cyber-btn" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="displayRows" v-loading="tableLoading" stripe border style="width:100%" class="cyber-table">
        <el-table-column prop="schoolName" label="校区" min-width="120" />
        <el-table-column prop="gradeName" label="年级" min-width="120" />
        <el-table-column label="课程" min-width="140">
          <template #default="{ row }">{{ row.courseName || row.courseCode }}</template>
        </el-table-column>
        <el-table-column label="方向" width="110">
          <template #default="{ row }">{{ directionLabel(row.courseDirection) }}</template>
        </el-table-column>
        <el-table-column label="层级" width="100">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.courseLevel)" size="small">{{ levelLabel(row.courseLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectedStudentCount" label="预计人数" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.progressStatus === 'NEED_MAKEUP' ? 'danger' : 'info'" size="small">
              {{ progressStatusLabel(row.progressStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可安排" width="100">
          <template #default="{ row }">
            <el-tag :type="row.canSchedule ? 'success' : 'warning'" size="small">
              {{ row.canSchedule ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="不可安排原因" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.canSchedule ? '-' : row.blockedReasons?.join('；') || '未满足排课条件' }}
          </template>
        </el-table-column>
        <el-table-column prop="suggestedTeacherCount" label="建议讲师数" width="120" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              text
              :icon="Plus"
              :disabled="!row.canSchedule"
              class="cyber-btn-text"
              @click="openCreatePlanDialog(row)"
            >
              创建派遣计划
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.pageNum"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          class="cyber-pagination"
          @current-change="fetchList"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="planDialogVisible"
      title="创建派遣计划"
      width="620px"
      :close-on-click-modal="false"
      class="cyber-dialog"
      @closed="resetPlanDialog"
    >
      <el-form ref="planFormRef" :model="planForm" :rules="planRules" label-width="110px" class="cyber-form">
        <el-form-item label="课程">
          <el-text>{{ selectedRow?.schoolName }} / {{ selectedRow?.gradeName }} / {{ selectedRow?.courseName }}</el-text>
        </el-form-item>
        <el-form-item label="建议讲师">
          <el-tag type="warning" size="small">{{ selectedRow?.suggestedTeacherCount || 1 }} 人</el-tag>
        </el-form-item>
        <el-form-item label="上课周期" prop="dateRange">
          <el-date-picker
            v-model="planForm.dateRange"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="讲师">
          <el-select
            v-model="planForm.teacherIds"
            multiple
            filterable
            clearable
            placeholder="可不选，系统允许低于建议讲师数"
            style="width:100%"
            class="cyber-select"
          >
            <el-option
              v-for="teacher in teacherOptions"
              :key="teacher.teacherId"
              :label="`${teacher.teacherName}（${dispatchStatusLabel(teacher.dispatchStatus)}）`"
              :value="teacher.teacherId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="planForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit class="cyber-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cyber-btn" @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" class="cyber-btn cyber-btn-primary" @click="submitPlan">
          创建计划
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { getScheduleCourseList } from '@/api/scheduleCourse'
import type { CourseLevel, ScheduleCourseRecord } from '@/api/scheduleCourse'
import { createDispatchPlan, getSchedulingCoursePool } from '@/api/scheduling'
import type { SchedulingCoursePoolRecord } from '@/api/scheduling'
import { getTeacherCapabilityList } from '@/api/scheduleTeacher'
import type { DispatchStatus, TeacherDispatchRecord } from '@/api/scheduleTeacher'
import type { ProgressStatus } from '@/api/scheduleCampusProgress'

const tableData = ref<SchedulingCoursePoolRecord[]>([])
const tableLoading = ref(false)
const courseOptions = ref<ScheduleCourseRecord[]>([])
const teacherOptions = ref<TeacherDispatchRecord[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const filters = reactive({
  schoolName: '',
  keyword: '',
  courseId: undefined as number | undefined,
  courseDirection: ''
})

const planDialogVisible = ref(false)
const submitLoading = ref(false)
const selectedRow = ref<SchedulingCoursePoolRecord | null>(null)
const planFormRef = ref<FormInstance>()
const planForm = reactive({
  dateRange: [] as string[],
  teacherIds: [] as number[],
  remark: ''
})
const planRules: FormRules = {
  dateRange: [{ required: true, message: '请选择上课周期', trigger: 'change' }]
}

const displayRows = computed(() => {
  if (!filters.courseDirection) return tableData.value
  return tableData.value.filter(row => row.courseDirection === filters.courseDirection)
})

async function fetchCourses() {
  const res = await getScheduleCourseList({ pageNum: 1, pageSize: 500, status: 'ACTIVE' })
  courseOptions.value = res.data.records
}

async function fetchList() {
  tableLoading.value = true
  try {
    const res = await getSchedulingCoursePool({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      schoolName: filters.schoolName,
      keyword: filters.keyword,
      courseId: filters.courseId
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } finally {
    tableLoading.value = false
  }
}

function handleSearch() {
  pagination.pageNum = 1
  fetchList()
}

function handleReset() {
  filters.schoolName = ''
  filters.keyword = ''
  filters.courseId = undefined
  filters.courseDirection = ''
  handleSearch()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchList()
}

async function openCreatePlanDialog(row: SchedulingCoursePoolRecord) {
  selectedRow.value = row
  planForm.dateRange = []
  planForm.teacherIds = []
  planForm.remark = ''
  planDialogVisible.value = true
  const res = await getTeacherCapabilityList({
    pageNum: 1,
    pageSize: 200,
    courseId: row.courseId,
    dispatchStatus: 'AVAILABLE'
  })
  teacherOptions.value = res.data.records
}

function resetPlanDialog() {
  selectedRow.value = null
  teacherOptions.value = []
  planFormRef.value?.clearValidate()
}

async function submitPlan() {
  const valid = await planFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedRow.value) return
  submitLoading.value = true
  try {
    const res = await createDispatchPlan({
      campusCourseProgressId: selectedRow.value.progressId,
      plannedStartDate: planForm.dateRange[0],
      plannedEndDate: planForm.dateRange[1],
      teacherIds: planForm.teacherIds,
      remark: planForm.remark.trim() || undefined
    })
    const warnings = [res.data.warning, ...(res.data.warnings || [])].filter(Boolean)
    if (warnings.length) {
      ElMessage.warning(warnings.join('；'))
    } else {
      ElMessage.success('派遣计划已创建')
    }
    planDialogVisible.value = false
    await fetchList()
  } finally {
    submitLoading.value = false
  }
}

function progressStatusLabel(status: ProgressStatus) {
  const map: Record<ProgressStatus, string> = {
    NOT_STARTED: '未上',
    PLANNED: '已计划',
    IN_PROGRESS: '执行中',
    COMPLETED: '已完成',
    NEED_MAKEUP: '需补课',
    CANCELED: '已取消'
  }
  return map[status] ?? status
}

function levelLabel(level?: CourseLevel) {
  const map: Record<string, string> = {
    BASIC: '基础',
    INTERMEDIATE: '中级',
    ADVANCED: '高级'
  }
  return level ? map[level] || level : '-'
}

function levelTagType(level?: CourseLevel) {
  if (level === 'ADVANCED') return 'danger'
  if (level === 'INTERMEDIATE') return 'warning'
  return 'success'
}

function directionLabel(direction?: string) {
  const map: Record<string, string> = {
    NETWORK: '网络',
    DEV: '开发',
    LINUX: 'Linux',
    WEB: 'Web',
    DB: '数据库',
    SECURITY: '安全',
    CLOUD: '云计算'
  }
  return direction ? map[direction] || direction : '-'
}

function dispatchStatusLabel(status: DispatchStatus) {
  const map: Record<DispatchStatus, string> = {
    AVAILABLE: '可派',
    PAUSED: '暂停',
    UNAVAILABLE: '不可派'
  }
  return map[status] ?? status
}

onMounted(async () => {
  await fetchCourses()
  await fetchList()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 16px;
  background: var(--bg-surface);
}

.cyber-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
}

.cyber-card :deep(.el-card__body) {
  background: transparent !important;
  padding: 20px !important;
}

.toolbar,
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar {
  justify-content: space-between;
  margin-bottom: 16px;
}

.filter-bar {
  margin-bottom: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #00ffff;
  letter-spacing: 1px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.cyber-btn-text {
  padding: 4px 8px !important;
}
</style>
