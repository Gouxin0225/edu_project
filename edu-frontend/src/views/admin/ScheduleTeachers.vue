<template>
  <div class="page">
    <el-card shadow="never" class="cyber-card">
      <div class="toolbar">
        <span class="card-title">讲师能力管理</span>
      </div>

      <div class="filter-bar">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索讲师姓名 / 用户名 / 手机号"
          :prefix-icon="Search"
          clearable
          style="width:260px"
          class="cyber-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filters.dispatchStatus" placeholder="全部派课状态" clearable style="width:150px" class="cyber-select" @change="handleSearch">
          <el-option label="可派" value="AVAILABLE" />
          <el-option label="暂停派课" value="PAUSED" />
          <el-option label="不可派" value="UNAVAILABLE" />
        </el-select>
        <el-select v-model="filters.courseId" filterable clearable placeholder="可授课程" style="width:180px" class="cyber-select" @change="handleSearch">
          <el-option v-for="course in courseOptions" :key="course.id" :label="course.courseName" :value="course.id" />
        </el-select>
        <el-select v-model="filters.canTravel" placeholder="出差状态" clearable style="width:130px" class="cyber-select" @change="handleSearch">
          <el-option label="可出差" :value="1" />
          <el-option label="不可出差" :value="0" />
        </el-select>
        <el-select v-model="filters.isCenterReserve" placeholder="中心保底" clearable style="width:130px" class="cyber-select" @change="handleSearch">
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" class="cyber-btn cyber-btn-primary" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" class="cyber-btn" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="tableData" v-loading="tableLoading" stripe border style="width:100%" class="cyber-table">
        <el-table-column prop="teacherName" label="讲师" min-width="120">
          <template #default="{ row }">
            <div class="teacher-cell">
              <span>{{ row.teacherName }}</span>
              <el-text type="info" size="small">{{ row.username }}</el-text>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="homeSchoolName" label="常驻校区" min-width="130">
          <template #default="{ row }">{{ row.homeSchoolName || row.schoolName || '-' }}</template>
        </el-table-column>
        <el-table-column label="派课状态" width="120">
          <template #default="{ row }">
            <el-tag :type="dispatchTagType(row.dispatchStatus)" size="small" class="cyber-tag">
              {{ dispatchStatusLabel(row.dispatchStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可出差" width="90">
          <template #default="{ row }">
            <el-tag :type="row.canTravel === 1 ? 'success' : 'info'" size="small">{{ row.canTravel === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="中心保底" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isCenterReserve === 1 ? 'warning' : 'info'" size="small">{{ row.isCenterReserve === 1 ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可授课程" min-width="260">
          <template #default="{ row }">
            <div v-if="row.skills?.length" class="skill-list">
              <el-tag
                v-for="skill in activeSkills(row.skills)"
                :key="`${skill.courseId}-${skill.skillLevel}`"
                size="small"
                class="cyber-tag"
                :type="skillTagType(skill.skillLevel)"
              >
                {{ skill.courseName || courseName(skill.courseId) }} / {{ skillLevelLabel(skill.skillLevel) }}
              </el-tag>
            </div>
            <el-text v-else type="info" size="small">暂未配置</el-text>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" text :icon="Setting" class="cyber-btn-text" @click="openConfigDialog(row)">
              配置
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
      :title="`配置讲师能力 - ${currentTeacher?.teacherName || ''}`"
      width="760px"
      :close-on-click-modal="false"
      class="cyber-dialog"
      @closed="resetDialog"
    >
      <el-form ref="formRef" :model="form" label-width="110px" class="cyber-form">
        <el-form-item label="常驻校区">
          <el-input v-model="form.homeSchoolName" placeholder="如：北京校区" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="派课状态">
          <el-radio-group v-model="form.dispatchStatus">
            <el-radio-button label="AVAILABLE">可派</el-radio-button>
            <el-radio-button label="PAUSED">暂停派课</el-radio-button>
            <el-radio-button label="UNAVAILABLE">不可派</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="派课属性">
          <div class="switch-row">
            <el-switch v-model="form.canTravelBool" active-text="可出差" inactive-text="不可出差" />
            <el-switch v-model="form.isCenterReserveBool" active-text="中心保底讲师" inactive-text="非保底" />
          </div>
        </el-form-item>
        <el-form-item label="可授课程">
          <el-select
            v-model="selectedCourseIds"
            multiple
            filterable
            placeholder="选择课程"
            style="width:100%"
            class="cyber-select"
            @change="syncSkillRows"
          >
            <el-option v-for="course in courseOptions" :key="course.id" :label="course.courseName" :value="course.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="skillRows.length" label="能力等级">
          <el-table :data="skillRows" border size="small" class="cyber-table skill-table">
            <el-table-column label="课程" min-width="180">
              <template #default="{ row }">{{ courseName(row.courseId) }}</template>
            </el-table-column>
            <el-table-column label="等级" width="220">
              <template #default="{ row }">
                <el-select v-model="row.skillLevel" style="width:160px" class="cyber-select">
                  <el-option label="主讲" value="MAIN" />
                  <el-option label="可讲" value="TEACHABLE" />
                  <el-option label="辅助" value="ASSIST" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="90">
              <template #default="{ row }">
                <el-switch v-model="row.active" />
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.availableRemark" type="textarea" :rows="3" maxlength="500" show-word-limit class="cyber-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cyber-btn" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" class="cyber-btn cyber-btn-primary" @click="submitConfig">
          保存配置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Refresh, Search, Setting } from '@element-plus/icons-vue'
import {
  getTeacherCapabilityList,
  saveTeacherProfile,
  saveTeacherSkills
} from '@/api/scheduleTeacher'
import type { DispatchStatus, SkillLevel, TeacherDispatchRecord, TeacherSkill } from '@/api/scheduleTeacher'
import { getScheduleCourseList } from '@/api/scheduleCourse'
import type { ScheduleCourseRecord } from '@/api/scheduleCourse'

interface SkillRow {
  courseId: number
  skillLevel: SkillLevel
  active: boolean
}

const tableData = ref<TeacherDispatchRecord[]>([])
const tableLoading = ref(false)
const courseOptions = ref<ScheduleCourseRecord[]>([])
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const filters = reactive<{
  keyword: string
  dispatchStatus: DispatchStatus | ''
  courseId?: number
  canTravel: 0 | 1 | ''
  isCenterReserve: 0 | 1 | ''
}>({
  keyword: '',
  dispatchStatus: '',
  courseId: undefined,
  canTravel: '',
  isCenterReserve: ''
})

const dialogVisible = ref(false)
const submitLoading = ref(false)
const currentTeacher = ref<TeacherDispatchRecord | null>(null)
const selectedCourseIds = ref<number[]>([])
const skillRows = ref<SkillRow[]>([])
const form = reactive({
  homeSchoolName: '',
  dispatchStatus: 'AVAILABLE' as DispatchStatus,
  canTravelBool: true,
  isCenterReserveBool: false,
  availableRemark: ''
})

async function fetchCourses() {
  const res = await getScheduleCourseList({ pageNum: 1, pageSize: 500, status: 'ACTIVE' })
  courseOptions.value = res.data.records
}

async function fetchList() {
  tableLoading.value = true
  try {
    const res = await getTeacherCapabilityList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: filters.keyword,
      dispatchStatus: filters.dispatchStatus,
      courseId: filters.courseId,
      canTravel: filters.canTravel,
      isCenterReserve: filters.isCenterReserve
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
  filters.dispatchStatus = ''
  filters.courseId = undefined
  filters.canTravel = ''
  filters.isCenterReserve = ''
  handleSearch()
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.pageNum = 1
  fetchList()
}

function openConfigDialog(row: TeacherDispatchRecord) {
  currentTeacher.value = row
  form.homeSchoolName = row.homeSchoolName || row.schoolName || ''
  form.dispatchStatus = row.dispatchStatus || 'AVAILABLE'
  form.canTravelBool = row.canTravel !== 0
  form.isCenterReserveBool = row.isCenterReserve === 1
  form.availableRemark = row.availableRemark || ''
  selectedCourseIds.value = activeSkills(row.skills).map(skill => skill.courseId)
  skillRows.value = activeSkills(row.skills).map(skill => ({
    courseId: skill.courseId,
    skillLevel: skill.skillLevel || 'TEACHABLE',
    active: skill.status !== 'DISABLED'
  }))
  dialogVisible.value = true
}

function resetDialog() {
  currentTeacher.value = null
  selectedCourseIds.value = []
  skillRows.value = []
}

function syncSkillRows() {
  const existing = new Map(skillRows.value.map(row => [row.courseId, row]))
  skillRows.value = selectedCourseIds.value.map(courseId => existing.get(courseId) || {
    courseId,
    skillLevel: 'TEACHABLE',
    active: true
  })
}

async function submitConfig() {
  if (!currentTeacher.value) return
  if (!skillRows.value.length) {
    ElMessage.warning('请至少选择一门可授课程')
    return
  }
  submitLoading.value = true
  try {
    const teacherId = currentTeacher.value.teacherId
    await saveTeacherProfile(teacherId, {
      homeSchoolName: form.homeSchoolName.trim() || undefined,
      dispatchStatus: form.dispatchStatus,
      canTravel: form.canTravelBool ? 1 : 0,
      isCenterReserve: form.isCenterReserveBool ? 1 : 0,
      availableRemark: form.availableRemark.trim() || undefined
    })
    await saveTeacherSkills(teacherId, {
      courses: skillRows.value.map(row => ({
        courseId: row.courseId,
        skillLevel: row.skillLevel,
        status: row.active ? 'ACTIVE' : 'DISABLED'
      }))
    })
    ElMessage.success('讲师能力配置已保存')
    dialogVisible.value = false
    await fetchList()
  } finally {
    submitLoading.value = false
  }
}

function activeSkills(skills: TeacherSkill[] = []) {
  return skills.filter(skill => skill.status !== 'DISABLED')
}

function courseName(courseId: number) {
  return courseOptions.value.find(course => course.id === courseId)?.courseName || `课程 ${courseId}`
}

function dispatchStatusLabel(status: DispatchStatus) {
  const map: Record<DispatchStatus, string> = {
    AVAILABLE: '可派',
    PAUSED: '暂停派课',
    UNAVAILABLE: '不可派'
  }
  return map[status] ?? status
}

function skillLevelLabel(level: SkillLevel) {
  const map: Record<SkillLevel, string> = {
    MAIN: '主讲',
    TEACHABLE: '可讲',
    ASSIST: '辅助'
  }
  return map[level] ?? level
}

function dispatchTagType(status: DispatchStatus) {
  if (status === 'AVAILABLE') return 'success'
  if (status === 'PAUSED') return 'warning'
  return 'info'
}

function skillTagType(level: SkillLevel) {
  if (level === 'MAIN') return 'danger'
  if (level === 'TEACHABLE') return 'success'
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

.teacher-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.skill-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.switch-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.skill-table {
  width: 100%;
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
