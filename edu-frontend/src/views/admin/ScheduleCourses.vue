<template>
  <div class="page">
    <el-card shadow="never" class="cyber-card">
      <div class="toolbar">
        <span class="card-title">课程基础管理</span>
        <el-button type="primary" :icon="Plus" class="cyber-btn cyber-btn-primary" @click="openCreateDialog">
          新增课程
        </el-button>
      </div>

      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索课程名称 / 编码"
          :prefix-icon="Search"
          clearable
          style="width:220px"
          class="cyber-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filters.courseLevel" placeholder="全部层级" clearable style="width:130px" class="cyber-select" @change="handleSearch">
          <el-option label="基础" value="BASIC" />
          <el-option label="中级" value="INTERMEDIATE" />
          <el-option label="高级" value="ADVANCED" />
        </el-select>
        <el-select v-model="filters.courseType" placeholder="全部类型" clearable style="width:150px" class="cyber-select" @change="handleSearch">
          <el-option label="普通校区课" value="FIXED" />
          <el-option label="高级集中课" value="CUSTOM" />
        </el-select>
        <el-select v-model="filters.status" placeholder="全部状态" clearable style="width:120px" class="cyber-select" @change="handleSearch">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" class="cyber-btn cyber-btn-primary" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" class="cyber-btn" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="tableLoading" stripe border style="width:100%" class="cyber-table">
        <el-table-column prop="courseCode" label="课程编码" width="150" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" />
        <el-table-column label="课程层级" width="110">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.courseLevel)" size="small" class="cyber-tag">
              {{ levelLabel(row.courseLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="方向" width="120">
          <template #default="{ row }">{{ directionLabel(row.courseDirection) }}</template>
        </el-table-column>
        <el-table-column label="课程类型" width="130">
          <template #default="{ row }">
            <el-tag :type="row.courseType === 'CUSTOM' ? 'warning' : 'success'" size="small" class="cyber-tag">
              {{ typeLabel(row.courseType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teacherCapacity" label="单讲师承载人数" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small" class="cyber-tag">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" text :icon="Edit" class="cyber-btn-text" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button
              size="small"
              :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
              text
              :icon="Switch"
              class="cyber-btn-text"
              @click="toggleStatus(row)"
            >
              {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
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
      v-model="dialogVisible"
      :title="editingCourse ? '编辑课程' : '新增课程'"
      width="560px"
      :close-on-click-modal="false"
      class="cyber-dialog"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="118px" class="cyber-form">
        <el-form-item label="课程编码" prop="courseCode">
          <el-input v-model="form.courseCode" placeholder="如：HCIA" clearable maxlength="50" class="cyber-input" />
        </el-form-item>
        <el-form-item label="课程名称" prop="courseName">
          <el-input v-model="form.courseName" placeholder="如：HCIA" clearable maxlength="100" class="cyber-input" />
        </el-form-item>
        <el-form-item label="课程方向" prop="courseDirection">
          <el-select v-model="form.courseDirection" filterable allow-create default-first-option placeholder="请选择或输入方向" style="width:100%" class="cyber-select">
            <el-option label="网络" value="NETWORK" />
            <el-option label="开发" value="DEV" />
            <el-option label="Linux" value="LINUX" />
            <el-option label="Web" value="WEB" />
            <el-option label="数据库" value="DB" />
            <el-option label="安全" value="SECURITY" />
            <el-option label="云计算" value="CLOUD" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程层级" prop="courseLevel">
          <el-radio-group v-model="form.courseLevel">
            <el-radio-button label="BASIC">基础</el-radio-button>
            <el-radio-button label="INTERMEDIATE">中级</el-radio-button>
            <el-radio-button label="ADVANCED">高级</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="课程类型" prop="courseType">
          <el-radio-group v-model="form.courseType">
            <el-radio-button label="FIXED">普通校区课</el-radio-button>
            <el-radio-button label="CUSTOM">高级集中课</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="承载人数" prop="teacherCapacity">
          <el-input-number v-model="form.teacherCapacity" :min="1" :max="999" controls-position="right" style="width:180px" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width:180px" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" active-value="ACTIVE" inactive-value="DISABLED" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit class="cyber-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cyber-btn" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" class="cyber-btn cyber-btn-primary" @click="submitForm">
          确认保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import type { FormInstance, FormRules } from 'element-plus'
import { Edit, Plus, Refresh, Search, Switch } from '@element-plus/icons-vue'
import {
  createScheduleCourse,
  getScheduleCourseList,
  updateScheduleCourse,
  updateScheduleCourseStatus
} from '@/api/scheduleCourse'
import type { CourseLevel, CourseStatus, CourseType, ScheduleCoursePayload, ScheduleCourseRecord } from '@/api/scheduleCourse'

const tableData = ref<ScheduleCourseRecord[]>([])
const tableLoading = ref(false)
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const filters = reactive<{
  keyword: string
  courseLevel: CourseLevel | ''
  courseType: CourseType | ''
  status: CourseStatus | ''
}>({
  keyword: '',
  courseLevel: '',
  courseType: '',
  status: ''
})

const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const editingCourse = ref<ScheduleCourseRecord | null>(null)
const form = reactive<ScheduleCoursePayload>({
  courseCode: '',
  courseName: '',
  courseDirection: '',
  courseLevel: 'BASIC',
  courseType: 'FIXED',
  teacherCapacity: 1,
  sortOrder: 0,
  status: 'ACTIVE',
  remark: ''
})

const rules: FormRules = {
  courseCode: [{ required: true, message: '请输入课程编码', trigger: 'blur' }],
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseDirection: [{ required: true, message: '请选择或输入课程方向', trigger: 'change' }],
  courseLevel: [{ required: true, message: '请选择课程层级', trigger: 'change' }],
  courseType: [{ required: true, message: '请选择课程类型', trigger: 'change' }],
  teacherCapacity: [{ required: true, message: '请输入单讲师承载人数', trigger: 'blur' }]
}

async function fetchList() {
  tableLoading.value = true
  try {
    const res = await getScheduleCourseList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: filters.keyword,
      courseLevel: filters.courseLevel,
      courseType: filters.courseType,
      status: filters.status
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
  filters.keyword = ''
  filters.courseLevel = ''
  filters.courseType = ''
  filters.status = ''
  handleSearch()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchList()
}

function openCreateDialog() {
  editingCourse.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: ScheduleCourseRecord) {
  editingCourse.value = row
  Object.assign(form, {
    courseCode: row.courseCode,
    courseName: row.courseName,
    courseDirection: row.courseDirection,
    courseLevel: row.courseLevel,
    courseType: row.courseType,
    teacherCapacity: row.teacherCapacity,
    sortOrder: row.sortOrder ?? 0,
    status: row.status,
    remark: row.remark ?? ''
  })
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.clearValidate()
  if (editingCourse.value) return
  Object.assign(form, {
    courseCode: '',
    courseName: '',
    courseDirection: '',
    courseLevel: 'BASIC',
    courseType: 'FIXED',
    teacherCapacity: 1,
    sortOrder: 0,
    status: 'ACTIVE',
    remark: ''
  })
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    const payload: ScheduleCoursePayload = {
      ...form,
      courseCode: form.courseCode.trim(),
      courseName: form.courseName.trim(),
      courseDirection: form.courseDirection.trim().toUpperCase(),
      remark: form.remark?.trim() || undefined
    }
    if (editingCourse.value) {
      await updateScheduleCourse(editingCourse.value.id, payload)
      ElMessage.success('课程已更新')
    } else {
      await createScheduleCourse(payload)
      ElMessage.success('课程已新增')
    }
    dialogVisible.value = false
    await fetchList()
  } finally {
    submitLoading.value = false
  }
}

async function toggleStatus(row: ScheduleCourseRecord) {
  const nextStatus: CourseStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await ElMessageBox.confirm(
    `确定要${nextStatus === 'ACTIVE' ? '启用' : '停用'}课程「${row.courseName}」吗？`,
    '状态变更',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  )
  await updateScheduleCourseStatus(row.id, nextStatus)
  ElMessage.success(`课程已${nextStatus === 'ACTIVE' ? '启用' : '停用'}`)
  row.status = nextStatus
}

function levelLabel(level: CourseLevel) {
  const map: Record<CourseLevel, string> = {
    BASIC: '基础',
    INTERMEDIATE: '中级',
    ADVANCED: '高级'
  }
  return map[level] ?? level
}

function typeLabel(type: CourseType) {
  const map: Record<CourseType, string> = {
    FIXED: '普通校区课',
    CUSTOM: '高级集中课'
  }
  return map[type] ?? type
}

function statusLabel(status: CourseStatus) {
  return status === 'ACTIVE' ? '启用' : '停用'
}

function directionLabel(direction: string) {
  const map: Record<string, string> = {
    NETWORK: '网络',
    DEV: '开发',
    LINUX: 'Linux',
    WEB: 'Web',
    DB: '数据库',
    SECURITY: '安全',
    CLOUD: '云计算'
  }
  return map[direction] ?? direction
}

function levelTagType(level: CourseLevel) {
  if (level === 'ADVANCED') return 'danger'
  if (level === 'INTERMEDIATE') return 'warning'
  return 'success'
}

onMounted(fetchList)
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
