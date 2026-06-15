<template>
  <div class="page">
    <el-card shadow="never" class="cyber-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <span class="card-title">班级列表</span>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog" class="cyber-btn cyber-btn-primary">创建班级</el-button>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索班级名称"
          :prefix-icon="Search"
          clearable
          style="width:220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
          class="cyber-input"
        />
        <el-select
          v-model="searchStatus"
          placeholder="全部状态"
          clearable
          style="width:130px"
          @change="handleSearch"
          class="cyber-select"
        >
          <el-option label="进行中" :value="1" />
          <el-option label="已结课" :value="0" />
        </el-select>
        <el-select
          v-model="searchAssignedUserId"
          filterable
          clearable
          placeholder="选择人员"
          style="width:220px"
          :loading="userOptionsLoading"
          @visible-change="handleUserFilterVisible"
          @change="handleSearch"
          class="cyber-select"
        >
          <el-option
            v-for="user in allAssignableUsers"
            :key="user.id"
            :label="`${user.realName}（${roleLabel(user.role)} / ${user.username}）`"
            :value="user.id"
          />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch" class="cyber-btn cyber-btn-primary">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset" class="cyber-btn">重置</el-button>
      </div>

      <!-- 班级表格 -->
      <el-table :data="tableData" v-loading="tableLoading" stripe border style="width:100%" class="cyber-table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="className" label="班级名称" min-width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" class="cyber-tag" :class="row.status === 1 ? 'cyber-tag-success' : 'cyber-tag-info'">
              {{ row.status === 1 ? '进行中' : '已结课' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已分配讲师" min-width="200">
          <template #default="{ row }">
            <template v-if="row.teacherNames && row.teacherNames.length">
              <el-tag
                v-for="name in row.teacherNames"
                :key="name"
                size="small"
                type="warning"
                style="margin:2px"
                class="cyber-tag cyber-tag-warning"
              >{{ name }}</el-tag>
            </template>
            <el-text v-else type="info" size="small" class="cyber-text-info">暂未分配</el-text>
          </template>
        </el-table-column>
        <el-table-column label="已分配班主任" min-width="200">
          <template #default="{ row }">
            <template v-if="row.assistantNames && row.assistantNames.length">
              <el-tag
                v-for="name in row.assistantNames"
                :key="name"
                size="small"
                type="success"
                style="margin:2px"
                class="cyber-tag cyber-tag-success"
              >{{ name }}</el-tag>
            </template>
            <el-text v-else type="info" size="small" class="cyber-text-info">暂未分配</el-text>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="165">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" text :icon="UserFilled" @click="openAssignDialog(row)" class="cyber-btn-text">
              分配人员
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="fetchList"
          @size-change="handleSizeChange"
          class="cyber-pagination"
        />
      </div>
    </el-card>

    <!-- ===== 创建班级弹窗 ===== -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建班级"
      width="420px"
      :close-on-click-modal="false"
      @closed="createFormRef?.resetFields()"
      class="cyber-dialog"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="班级名称" prop="className" class="cyber-form-item">
          <el-input
            v-model="createForm.className"
            placeholder="如：Java全栈2401期"
            clearable
            maxlength="50"
            show-word-limit
            class="cyber-input"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false" class="cyber-btn">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate" class="cyber-btn cyber-btn-primary">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- ===== 分配讲师弹窗 ===== -->
    <el-dialog
      v-model="showAssignDialog"
      :title="`分配人员 — ${assignTarget?.className}`"
      width="460px"
      :close-on-click-modal="false"
      @opened="handleAssignDialogOpen"
      @closed="resetAssignForm"
      class="cyber-dialog"
    >
      <!-- 已分配讲师 -->
      <div class="section-label">当前已分配讲师</div>
      <div class="current-teachers">
        <template v-if="assignTarget?.teacherNames?.length">
          <el-tag
            v-for="(name, index) in assignTarget.teacherNames"
            :key="`${assignTarget.teacherIds?.[index]}-${name}`"
            type="warning"
            closable
            style="margin:4px"
            class="cyber-tag cyber-tag-warning"
            @close="handleRemoveAssigned(assignTarget.teacherIds?.[index], name, 'TEACHER')"
          >{{ name }}</el-tag>
        </template>
        <el-text v-else type="info" size="small" class="cyber-text-info">暂未分配讲师</el-text>
      </div>

      <div class="section-label">当前已分配班主任</div>
      <div class="current-teachers">
        <template v-if="assignTarget?.assistantNames?.length">
          <el-tag
            v-for="(name, index) in assignTarget.assistantNames"
            :key="`${assignTarget.assistantIds?.[index]}-${name}`"
            type="success"
            closable
            style="margin:4px"
            class="cyber-tag cyber-tag-success"
            @close="handleRemoveAssigned(assignTarget.assistantIds?.[index], name, 'ASSISTANT')"
          >{{ name }}</el-tag>
        </template>
        <el-text v-else type="info" size="small" class="cyber-text-info">暂未分配班主任</el-text>
      </div>

      <el-divider class="cyber-divider" />

      <!-- 添加新人员 -->
      <div class="section-label">添加人员</div>
      <el-form ref="assignFormRef" :model="assignForm" :rules="assignRules" label-width="0">
        <el-form-item prop="userId" class="cyber-form-item">
          <el-select
            v-model="assignForm.userId"
            filterable
            remote
            reserve-keyword
            placeholder="搜索姓名或用户名"
            :remote-method="searchAssignableUser"
            :loading="userSearchLoading"
            style="width:100%"
            clearable
            class="cyber-select"
          >
            <el-option
              v-for="user in filteredAssignableUsers"
              :key="user.id"
              :label="`${user.realName}（${roleLabel(user.role)} / ${user.username}）`"
              :value="user.id"
              :disabled="isAssignedUser(user.id)"
            >
              <div class="teacher-option">
                <span>{{ user.realName }}</span>
                <el-text type="info" size="small">{{ user.username }}</el-text>
                <el-tag size="small" :type="user.role === 'ASSISTANT' ? 'success' : 'warning'">{{ roleLabel(user.role) }}</el-tag>
                <el-tag v-if="isAssignedUser(user.id)" size="small" type="info">已分配</el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showAssignDialog = false" class="cyber-btn">关闭</el-button>
        <el-button
          type="primary"
          :loading="assignLoading"
          :disabled="!assignForm.userId"
          @click="submitAssign"
          class="cyber-btn cyber-btn-primary"
        >
          确认分配
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, UserFilled, Search, Refresh } from '@element-plus/icons-vue'
import {
  getClassList, createClass, assignTeacher, removeTeacher, getAssignableUserOptions
} from '@/api/admin/class'
import type { ClassRecord } from '@/api/admin/class'
import type { UserRecord } from '@/api/admin/user'

// ─────────── 列表 + 搜索 ───────────
const tableData = ref<ClassRecord[]>([])
const tableLoading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })
const searchKeyword = ref('')
const searchStatus = ref<0 | 1 | ''>('')
const searchAssignedUserId = ref<number | null>(null)

async function fetchList() {
  tableLoading.value = true
  try {
    const res = await getClassList({
      page: pagination.page,
      size: pagination.size,
      keyword: searchKeyword.value || undefined,
      status: searchStatus.value === '' ? undefined : searchStatus.value,
      teacherId: searchAssignedUserId.value || undefined
    })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } finally {
    tableLoading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchList()
}

function handleReset() {
  searchKeyword.value = ''
  searchStatus.value = ''
  searchAssignedUserId.value = null
  handleSearch()
}

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.page = 1
  fetchList()
}

const formatTime = (t?: string) => t ? t.replace('T', ' ').slice(0, 16) : '-'

// ─────────── 创建班级 ───────────
const showCreateDialog = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ className: '' })
const createRules: FormRules = {
  className: [
    { required: true, message: '请输入班级名称', trigger: 'blur' },
    { min: 2, max: 50, message: '班级名称长度 2~50 字符', trigger: 'blur' }
  ]
}

function openCreateDialog() {
  showCreateDialog.value = true
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    const res = await createClass(createForm.className)
    ElMessage.success(`班级「${res.data.className}」创建成功`)
    showCreateDialog.value = false
    // 将新班级插入列表首位（无需重新请求）
    tableData.value.unshift({
      ...res.data,
      teacherNames: [],
      teacherIds: [],
      assistantNames: [],
      assistantIds: [],
      createTime: new Date().toISOString()
    })
    pagination.total++
  } finally {
    createLoading.value = false
  }
}

// ─────────── 分配人员 ───────────
const showAssignDialog = ref(false)
const assignLoading = ref(false)
const assignFormRef = ref<FormInstance>()
const assignTarget = ref<ClassRecord | null>(null)
const assignForm = reactive({ userId: null as number | null })
const assignRules: FormRules = {
  userId: [{ required: true, message: '请选择人员', trigger: 'change' }]
}

const roleLabels: Record<string, string> = {
  TEACHER: '讲师',
  ASSISTANT: '班主任',
  ADMIN: '管理员',
  STUDENT: '学生'
}
const roleLabel = (role: string) => roleLabels[role] ?? role

// 可分配人员搜索
const allAssignableUsers = ref<UserRecord[]>([])
const filteredAssignableUsers = ref<UserRecord[]>([])
const userSearchLoading = ref(false)
const userOptionsLoading = ref(false)

async function ensureAssignableUserOptions() {
  if (allAssignableUsers.value.length) return
  userOptionsLoading.value = true
  userSearchLoading.value = true
  try {
    allAssignableUsers.value = await getAssignableUserOptions()
    filteredAssignableUsers.value = allAssignableUsers.value
  } finally {
    userOptionsLoading.value = false
    userSearchLoading.value = false
  }
}

function handleUserFilterVisible(visible: boolean) {
  if (visible) {
    ensureAssignableUserOptions()
  }
}

function openAssignDialog(row: ClassRecord) {
  assignTarget.value = { ...row }
  showAssignDialog.value = true
}

async function handleAssignDialogOpen() {
  await ensureAssignableUserOptions()
  filteredAssignableUsers.value = allAssignableUsers.value
}

function searchAssignableUser(query: string) {
  if (!query) {
    filteredAssignableUsers.value = allAssignableUsers.value
    return
  }
  const q = query.toLowerCase()
  filteredAssignableUsers.value = allAssignableUsers.value.filter(
    t => t.realName.toLowerCase().includes(q) || t.username.toLowerCase().includes(q)
  )
}

function assignedUserIds(record: ClassRecord | null) {
  return [
    ...(record?.teacherIds ?? []),
    ...(record?.assistantIds ?? [])
  ]
}

function isAssignedUser(userId: number) {
  return assignedUserIds(assignTarget.value).includes(userId)
}

function appendAssignedUser(record: ClassRecord, user: UserRecord) {
  if (user.role === 'ASSISTANT') {
    record.assistantNames = [...(record.assistantNames ?? []), user.realName]
    record.assistantIds = [...(record.assistantIds ?? []), user.id]
    return
  }
  record.teacherNames = [...(record.teacherNames ?? []), user.realName]
  record.teacherIds = [...(record.teacherIds ?? []), user.id]
}

function removeAssignedUserFromRecord(record: ClassRecord, userId: number, role: 'TEACHER' | 'ASSISTANT') {
  const idsKey: 'assistantIds' | 'teacherIds' = role === 'ASSISTANT' ? 'assistantIds' : 'teacherIds'
  const namesKey: 'assistantNames' | 'teacherNames' = role === 'ASSISTANT' ? 'assistantNames' : 'teacherNames'
  const ids = record[idsKey] ?? []
  const names = record[namesKey] ?? []
  const idx = ids.indexOf(userId)
  if (idx > -1) {
    record[idsKey] = ids.filter(id => id !== userId)
    record[namesKey] = names.filter((_, index) => index !== idx)
  }
}

async function submitAssign() {
  if (!assignForm.userId || !assignTarget.value) return
  assignLoading.value = true
  try {
    await assignTeacher(assignTarget.value.id, assignForm.userId)
    const user = allAssignableUsers.value.find(t => t.id === assignForm.userId)
    if (user) {
      // 同步更新本地数据
      const row = tableData.value.find(r => r.id === assignTarget.value!.id)
      if (row) {
        appendAssignedUser(row, user)
      }
      appendAssignedUser(assignTarget.value, user)
    }
    ElMessage.success(`${user ? roleLabel(user.role) : '人员'}分配成功`)
    assignForm.userId = null
  } finally {
    assignLoading.value = false
  }
}

function handleRemoveAssigned(userId: number | undefined, name: string, role: 'TEACHER' | 'ASSISTANT') {
  if (!assignTarget.value) return
  if (!userId) {
    ElMessage.error('未找到人员ID，无法移除')
    return
  }
  ElMessageBox.confirm(
    `确定要移除${roleLabel(role)} <b>${name}</b> 吗？`,
    `移除${roleLabel(role)}`,
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
  ).then(async () => {
    if (!assignTarget.value) return
    await removeTeacher(assignTarget.value.id, userId)
    removeAssignedUserFromRecord(assignTarget.value, userId, role)
    const row = tableData.value.find(r => r.id === assignTarget.value!.id)
    if (row) {
      removeAssignedUserFromRecord(row, userId, role)
    }
    ElMessage.success(`${roleLabel(role)}已移除`)
  }).catch(() => {})
}

function resetAssignForm() {
  assignForm.userId = null
  filteredAssignableUsers.value = allAssignableUsers.value
}

// ─────────── 初始化 ───────────
onMounted(fetchList)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  font-family: 'JetBrains Mono', monospace;
  background: var(--bg-surface);
  min-height: 100vh;
  padding: 16px;
}

/* Cyber Card */
.cyber-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
}

.cyber-card :deep(.el-card__body) {
  background: transparent !important;
  padding: 20px !important;
}

/* Toolbar */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5), 0 0 20px rgba(0, 255, 255, 0.3);
  letter-spacing: 2px;
  text-transform: uppercase;
}

/* Cyber Buttons */
.cyber-btn {
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 13px !important;
  font-weight: 500 !important;
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px);
  padding: 8px 16px !important;
  transition: all 0.3s ease !important;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.cyber-btn:hover {
  background: var(--bg-surface) !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
  color: #00ffff !important;
}

.cyber-btn-primary {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1), rgba(255, 16, 240, 0.1)) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
}

.cyber-btn-primary:hover {
  background: rgba(0, 255, 255, 0.2) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.5), 0 0 40px rgba(0, 255, 255, 0.2) !important;
}

.cyber-btn-text {
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 13px !important;
  color: #00ffff !important;
  background: transparent !important;
  border: none !important;
  padding: 4px 8px !important;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.cyber-btn-text:hover {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

/* Filter Bar */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

/* Cyber Inputs */
.cyber-input {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-input :deep(.el-input__wrapper) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px) !important;
  padding: 2px 10px !important;
}

.cyber-input :deep(.el-input__wrapper:hover) {
  border-color: #00ffff !important;
}

.cyber-input :deep(.el-input__wrapper.is-focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-input :deep(.el-input__inner) {
  font-family: 'JetBrains Mono', monospace !important;
  color: var(--text-primary) !important;
  font-size: 13px !important;
}

.cyber-input :deep(.el-input__inner::placeholder) {
  color: var(--text-secondary) !important;
}

.cyber-input :deep(.el-input__prefix) {
  color: #00ffff !important;
}

/* Cyber Select */
.cyber-select :deep(.el-select__wrapper) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  box-shadow: none !important;
  clip-path: polygon(6px 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%, 0 6px) !important;
  min-height: 32px !important;
  padding: 2px 10px !important;
}

.cyber-select :deep(.el-select__wrapper:hover) {
  border-color: #00ffff !important;
}

.cyber-select :deep(.el-select__wrapper.is-focused) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-select :deep(.el-select__selected-item) {
  color: var(--text-primary) !important;
}

.cyber-select :deep(.el-select__placeholder) {
  color: var(--text-secondary) !important;
}

.cyber-select :deep(.el-select__dropdown) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
}

.cyber-select :deep(.el-select-dropdown__item) {
  font-family: 'JetBrains Mono', monospace !important;
  color: var(--text-primary) !important;
}

.cyber-select :deep(.el-select-dropdown__item:hover) {
  background: var(--bg-surface) !important;
  color: #00ffff !important;
}

.cyber-select :deep(.el-select-dropdown__item.is-selected) {
  color: #00ffff !important;
  background: rgba(0, 255, 255, 0.1) !important;
}

/* Cyber Table */
.cyber-table {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
}

.cyber-table :deep(.el-table__header-wrapper) {
  background: var(--bg-surface) !important;
}

.cyber-table :deep(.el-table__header) {
  border-bottom: 1px solid var(--border) !important;
}

.cyber-table :deep(.el-table__header th) {
  background: var(--bg-surface) !important;
  color: #00ffff !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  text-transform: uppercase;
  letter-spacing: 1px;
  border-bottom: 1px solid var(--border) !important;
  padding: 12px 8px !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

.cyber-table :deep(.el-table__body-wrapper) {
  background: transparent !important;
}

.cyber-table :deep(.el-table__body tr) {
  background: transparent !important;
}

.cyber-table :deep(.el-table__body td) {
  background: transparent !important;
  color: var(--text-primary) !important;
  font-size: 13px !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 10px 8px !important;
}

.cyber-table :deep(.el-table__body tr:hover > td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.cyber-table :deep(.el-table__body .el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.3) !important;
}

.cyber-table :deep(.el-table__empty-block) {
  background: transparent !important;
}

.cyber-table :deep(.el-table__empty-text) {
  color: var(--text-secondary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

/* Cyber Tags */
.cyber-tag {
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 11px !important;
  border-radius: 0 !important;
  border: none !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.cyber-tag-success {
  background: rgba(57, 255, 20, 0.1) !important;
  color: #39ff14 !important;
  border: 1px solid rgba(57, 255, 20, 0.3) !important;
  box-shadow: 0 0 10px rgba(57, 255, 20, 0.2);
}

.cyber-tag-warning {
  background: rgba(255, 152, 0, 0.1) !important;
  color: #ff9800 !important;
  border: 1px solid rgba(255, 152, 0, 0.3) !important;
  box-shadow: 0 0 10px rgba(255, 152, 0, 0.2);
}

.cyber-tag-info {
  background: rgba(102, 102, 102, 0.1) !important;
  color: var(--text-secondary) !important;
  border: 1px solid rgba(102, 102, 102, 0.3) !important;
}

.cyber-text-info {
  font-family: 'JetBrains Mono', monospace !important;
  color: var(--text-secondary) !important;
  font-size: 13px !important;
}

/* Cyber Pagination */
.cyber-pagination {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pagination__total) {
  color: var(--text-primary) !important;
  font-size: 13px !important;
}

.cyber-pagination :deep(.el-pagination__sizes) {
  color: var(--text-primary) !important;
}

.cyber-pagination :deep(.el-pagination__sizes .el-input__inner) {
  font-family: 'JetBrains Mono', monospace !important;
  background: var(--bg-surface) !important;
  border-color: var(--border-subtle) !important;
  color: var(--text-primary) !important;
}

.cyber-pagination :deep(.el-pager li) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 13px !important;
  border-radius: 0 !important;
  min-width: 32px !important;
  height: 32px !important;
  line-height: 32px !important;
}

.cyber-pagination :deep(.el-pager li:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
}

.cyber-pagination :deep(.el-pager li.is-active) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3);
}

.cyber-pagination :deep(.el-pagination__jump) {
  color: var(--text-primary) !important;
}

.cyber-pagination :deep(.el-pagination__editor) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.btn-prev),
.cyber-pagination :deep(.btn-next) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  border-radius: 0 !important;
}

.cyber-pagination :deep(.btn-prev:hover),
.cyber-pagination :deep(.btn-next:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
}

/* Pagination Wrap */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* Cyber Dialog */
.cyber-dialog {
  font-family: 'JetBrains Mono', monospace !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  clip-path: polygon(0 0, calc(100% - 30px) 0, 100% 30px, 100% 100%, 30px 100%, 0 calc(100% - 30px));
  box-shadow: 0 0 40px rgba(0, 255, 255, 0.2), 0 0 80px rgba(255, 16, 240, 0.1) !important;
}

.cyber-dialog :deep(.el-dialog__header) {
  background: var(--bg-surface) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 16px 20px !important;
}

.cyber-dialog :deep(.el-dialog__title) {
  color: #00ffff !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.cyber-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: var(--text-primary) !important;
  font-size: 18px !important;
}

.cyber-dialog :deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5);
}

.cyber-dialog :deep(.el-dialog__body) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  padding: 20px !important;
}

.cyber-dialog :deep(.el-dialog__footer) {
  background: var(--bg-surface) !important;
  border-top: 1px solid var(--border) !important;
  padding: 16px 20px !important;
}

/* Cyber Form Item */
.cyber-form-item :deep(.el-form-item__label) {
  font-family: 'JetBrains Mono', monospace !important;
  color: #00ffff !important;
  font-size: 13px !important;
  text-transform: uppercase;
  letter-spacing: 1px;
}

/* Cyber Divider */
.cyber-divider {
  border-color: var(--border-subtle) !important;
  margin: 16px 0 !important;
}

/* Section Label */
.section-label {
  font-size: 13px;
  font-weight: 600;
  color: #00ffff !important;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
}

/* Current Teachers */
.current-teachers {
  min-height: 36px;
  padding: 6px 0;
}

/* Teacher Option */
.teacher-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Loading */
:deep(.el-loading-mask) {
  background: rgba(10, 10, 10, 0.8) !important;
}

:deep(.el-loading-spinner .el-loading-text) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

:deep(.el-loading-spinner .circular) {
  stroke: #00ffff !important;
}

/* Override Element Plus Select Dropdown Internal White Background */
.el-select-dropdown,
.el-select-dropdown__list,
.el-select-dropdown__item,
.el-popper.is-light,
.el-popper__popper {
  background: var(--bg-surface) !important;
  border-color: var(--border-subtle) !important;
}

.el-select-dropdown__item.selected {
  color: #00ffff !important;
}

/* Override Element Plus Input Internal White Background */
.el-input__wrapper,
.el-input__inner {
  background: var(--bg-surface) !important;
}

/* Override Element Plus Table Internal White Background */
.el-table,
.el-table__header-wrapper,
.el-table__body-wrapper,
.el-table__header,
.el-table__body {
  background: transparent !important;
}

/* Override Element Plus Dialog Internal White Background */
.el-dialog,
.el-dialog__header,
.el-dialog__body,
.el-dialog__footer {
  background: var(--bg-surface) !important;
}

/* Override Element Plus Form Internal White Background */
.el-form-item__label,
.el-form-item__error {
  color: #00ffff !important;
}

/* Override Element Plus Pagination Internal White Background */
.el-pagination {
  background: transparent !important;
}

/* Override Element Plus Tag Internal White Background */
.el-tag {
  background: var(--bg-surface) !important;
}

/* Message Box Override */
:deep(.el-message-box) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
}

:deep(.el-message-box__title) {
  color: #00ffff !important;
}

:deep(.el-message-box__content) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}
</style>
