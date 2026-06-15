<template>
  <div class="page">
    <el-card shadow="never" class="cyber-card">
      <div class="toolbar">
        <span class="card-title">校区课程进度维护</span>
        <el-button v-if="isCenterSide" type="primary" :icon="Plus" class="cyber-btn cyber-btn-primary" @click="openInitDialog">
          初始化应上课程
        </el-button>
      </div>

      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索校区 / 年级"
          :prefix-icon="Search"
          clearable
          style="width:220px"
          class="cyber-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-input
          v-if="isCenterSide"
          v-model="filters.schoolName"
          placeholder="校区名称"
          clearable
          style="width:180px"
          class="cyber-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filters.courseId" filterable clearable placeholder="课程" style="width:170px" class="cyber-select" @change="handleSearch">
          <el-option v-for="course in courseOptions" :key="course.id" :label="course.courseName" :value="course.id" />
        </el-select>
        <el-select v-model="filters.progressStatus" clearable placeholder="课程状态" style="width:140px" class="cyber-select" @change="handleSearch">
          <el-option label="未上" value="NOT_STARTED" />
          <el-option label="已计划" value="PLANNED" />
          <el-option label="执行中" value="IN_PROGRESS" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="需补课" value="NEED_MAKEUP" />
          <el-option label="已取消" value="CANCELED" />
        </el-select>
        <el-button type="primary" :icon="Search" class="cyber-btn cyber-btn-primary" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" class="cyber-btn" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="tableLoading" stripe border style="width:100%" class="cyber-table">
        <el-table-column prop="schoolName" label="校区" min-width="130" />
        <el-table-column prop="gradeName" label="年级" min-width="130" />
        <el-table-column label="课程" min-width="150">
          <template #default="{ row }">
            {{ row.courseName || row.courseCode || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.progressStatus)" size="small" class="cyber-tag">
              {{ progressStatusLabel(row.progressStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expectedStudentCount" label="预计学生人数" width="130" />
        <el-table-column label="可安排" width="100">
          <template #default="{ row }">
            <el-tag :type="row.canSchedule ? 'success' : 'warning'" size="small">
              {{ row.canSchedule ? '可安排' : '受限' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="限制原因" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.blockedReasons?.length ? row.blockedReasons.join('；') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" text :icon="Edit" class="cyber-btn-text" @click="openStudentCountDialog(row)">
              人数
            </el-button>
            <el-button size="small" type="primary" text :icon="Memo" class="cyber-btn-text" @click="openRemarkDialog(row)">
              备注
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

    <el-dialog v-model="studentDialogVisible" title="修改预计学生人数" width="420px" :close-on-click-modal="false" class="cyber-dialog">
      <el-form label-width="110px">
        <el-form-item label="课程">
          <el-text>{{ activeRecord?.courseName }}</el-text>
        </el-form-item>
        <el-form-item label="预计人数">
          <el-input-number v-model="studentCountForm.expectedStudentCount" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cyber-btn" @click="studentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" class="cyber-btn cyber-btn-primary" @click="submitStudentCount">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="remarkDialogVisible" title="修改备注" width="480px" :close-on-click-modal="false" class="cyber-dialog">
      <el-input v-model="remarkForm.remark" type="textarea" :rows="4" maxlength="500" show-word-limit class="cyber-input" />
      <template #footer>
        <el-button class="cyber-btn" @click="remarkDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" class="cyber-btn cyber-btn-primary" @click="submitRemark">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="initDialogVisible" title="初始化校区年级应上课程" width="620px" :close-on-click-modal="false" class="cyber-dialog">
      <el-form ref="initFormRef" :model="initForm" :rules="initRules" label-width="110px" class="cyber-form">
        <el-form-item label="校区名称" prop="schoolName">
          <el-input v-model="initForm.schoolName" placeholder="如：北京校区" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="年级名称" prop="gradeName">
          <el-input v-model="initForm.gradeName" placeholder="如：2026春" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="预计人数">
          <el-input-number v-model="initForm.studentCount" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="应上课程" prop="courseIds">
          <el-select v-model="initForm.courseIds" multiple filterable placeholder="选择课程" style="width:100%" class="cyber-select">
            <el-option v-for="course in courseOptions" :key="course.id" :label="course.courseName" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="initForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit class="cyber-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cyber-btn" @click="initDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" class="cyber-btn cyber-btn-primary" @click="submitInit">
          初始化
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import type { FormInstance, FormRules } from 'element-plus'
import { Edit, Memo, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getScheduleCourseList } from '@/api/scheduleCourse'
import type { ScheduleCourseRecord } from '@/api/scheduleCourse'
import {
  getAllCampusProgress,
  getMyCampusProgress,
  initCampusProgress,
  updateExpectedStudentCount,
  updateProgressRemark
} from '@/api/scheduleCampusProgress'
import type { CampusCourseProgressRecord, ProgressStatus } from '@/api/scheduleCampusProgress'

const userStore = useUserStore()
const isCenterSide = computed(() => userStore.userInfo?.role === 'ADMIN')

const tableData = ref<CampusCourseProgressRecord[]>([])
const tableLoading = ref(false)
const courseOptions = ref<ScheduleCourseRecord[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const filters = reactive<{
  keyword: string
  schoolName: string
  courseId?: number
  progressStatus: ProgressStatus | ''
}>({
  keyword: '',
  schoolName: '',
  courseId: undefined,
  progressStatus: ''
})

const activeRecord = ref<CampusCourseProgressRecord | null>(null)
const submitLoading = ref(false)
const studentDialogVisible = ref(false)
const remarkDialogVisible = ref(false)
const initDialogVisible = ref(false)
const initFormRef = ref<FormInstance>()
const studentCountForm = reactive({ expectedStudentCount: 0 })
const remarkForm = reactive({ remark: '' })
const initForm = reactive({
  schoolName: '',
  gradeName: '',
  studentCount: 0,
  courseIds: [] as number[],
  remark: ''
})
const initRules: FormRules = {
  schoolName: [{ required: true, message: '请输入校区名称', trigger: 'blur' }],
  gradeName: [{ required: true, message: '请输入年级名称', trigger: 'blur' }],
  courseIds: [{ required: true, message: '请选择应上课程', trigger: 'change' }]
}

async function fetchCourses() {
  const res = await getScheduleCourseList({ pageNum: 1, pageSize: 500, status: 'ACTIVE' })
  courseOptions.value = res.data.records
}

async function fetchList() {
  tableLoading.value = true
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      courseId: filters.courseId,
      progressStatus: filters.progressStatus,
      keyword: filters.keyword
    }
    const res = isCenterSide.value
      ? await getAllCampusProgress({ ...params, schoolName: filters.schoolName })
      : await getMyCampusProgress(params)
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
  filters.keyword = ''
  filters.schoolName = ''
  filters.courseId = undefined
  filters.progressStatus = ''
  handleSearch()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchList()
}

function openStudentCountDialog(row: CampusCourseProgressRecord) {
  activeRecord.value = row
  studentCountForm.expectedStudentCount = row.expectedStudentCount ?? 0
  studentDialogVisible.value = true
}

function openRemarkDialog(row: CampusCourseProgressRecord) {
  activeRecord.value = row
  remarkForm.remark = row.remark || ''
  remarkDialogVisible.value = true
}

function openInitDialog() {
  initForm.schoolName = ''
  initForm.gradeName = ''
  initForm.studentCount = 0
  initForm.courseIds = courseOptions.value.map(course => course.id)
  initForm.remark = ''
  initDialogVisible.value = true
}

async function submitStudentCount() {
  if (!activeRecord.value) return
  submitLoading.value = true
  try {
    await updateExpectedStudentCount(activeRecord.value.id, studentCountForm.expectedStudentCount)
    ElMessage.success('预计学生人数已更新')
    studentDialogVisible.value = false
    await fetchList()
  } finally {
    submitLoading.value = false
  }
}

async function submitRemark() {
  if (!activeRecord.value) return
  submitLoading.value = true
  try {
    await updateProgressRemark(activeRecord.value.id, remarkForm.remark.trim())
    ElMessage.success('备注已更新')
    remarkDialogVisible.value = false
    await fetchList()
  } finally {
    submitLoading.value = false
  }
}

async function submitInit() {
  const valid = await initFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    await initCampusProgress({
      schoolName: initForm.schoolName.trim(),
      gradeName: initForm.gradeName.trim(),
      studentCount: initForm.studentCount,
      courses: initForm.courseIds.map(courseId => ({
        courseId,
        expectedStudentCount: initForm.studentCount,
        progressStatus: 'NOT_STARTED',
        remark: initForm.remark.trim() || undefined
      }))
    })
    ElMessage.success('校区年级应上课程已初始化')
    initDialogVisible.value = false
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

function statusTagType(status: ProgressStatus) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS' || status === 'PLANNED') return 'warning'
  if (status === 'NEED_MAKEUP') return 'danger'
  return 'info'
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
