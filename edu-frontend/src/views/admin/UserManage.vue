<template>
  <div class="page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-inner">
            <el-icon :size="36" :color="card.color"><component :is="card.icon" /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主内容卡片 -->
    <el-card shadow="never" class="main-card">
      <!-- 筛选栏 + 操作按钮 -->
      <div class="toolbar">
        <div class="filter-group">
          <el-input
            v-model="searchKeyword"
            placeholder="精确搜索用户名 / 姓名 / 手机号"
            clearable
            style="width:260px"
            class="cyber-input"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select v-model="filterRole" placeholder="全部角色" clearable style="width:140px" @change="handleSearch" class="cyber-select">
            <el-option label="讲师" value="TEACHER" />
            <el-option label="班主任" value="ASSISTANT" />
            <el-option label="学生" value="STUDENT" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="handleSearch" class="cyber-btn cyber-btn-primary">搜索</el-button>
        </div>
        <div class="action-group">
          <el-button type="primary" :icon="Plus" @click="openCreateDialog" class="cyber-btn cyber-btn-primary">创建用户</el-button>
          <el-button :icon="Upload" @click="showImportDialog = true" class="cyber-btn cyber-btn-secondary">Excel 批量导入</el-button>
        </div>
      </div>

      <!-- 用户列表 -->
      <el-table :data="tableData" v-loading="tableLoading" stripe border style="width:100%" class="cyber-table">
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="班级/课程" min-width="150">
          <template #default="{ row }">
            <template v-if="row.role === 'STUDENT'">
              <span v-if="row.className">{{ row.className }}</span>
              <span v-else class="muted">未分配</span>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small" class="cyber-tag">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.role === 'STUDENT'" size="small" type="primary" text @click="viewHistory(row)" class="cyber-btn-text">课程历史</el-button>
            <el-button size="small" type="warning" text @click="handleResetPwd(row)" class="cyber-btn-text cyber-btn-warning">重置密码</el-button>
            <el-button size="small" type="danger" text @click="handleDelete(row)" class="cyber-btn-text cyber-btn-danger">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
...
    <!-- 课程历史弹窗 -->
    <el-dialog v-model="showHistoryDialog" title="学员课程历史" width="760px" class="cyber-dialog">
      <template v-if="currentUser">
        <div class="history-profile">
          <div>
            <div class="history-name">{{ currentUser.realName }}</div>
            <div class="history-meta">{{ currentUser.username }}<span v-if="currentUser.phone"> / {{ currentUser.phone }}</span></div>
          </div>
          <el-tag v-if="currentUser.className" type="success" size="large" class="cyber-tag">当前：{{ currentUser.className }}</el-tag>
          <el-tag v-else type="info" size="large" class="cyber-tag">当前未分配班级</el-tag>
        </div>

        <div class="history-summary">
          <div class="history-summary-item">
            <strong>{{ historyList.length }}</strong>
            <span>累计班级</span>
          </div>
          <div class="history-summary-item active">
            <strong>{{ activeHistoryCount }}</strong>
            <span>学习中</span>
          </div>
          <div class="history-summary-item left">
            <strong>{{ leftHistoryCount }}</strong>
            <span>已退出</span>
          </div>
        </div>

        <div v-if="historyList.length" class="history-timeline">
          <div
            v-for="item in historyList"
            :key="`${item.classId}-${item.joinTime}`"
            class="history-timeline-item"
            :class="{ current: item.joinStatus === 'ACTIVE' }"
          >
            <div class="history-dot"></div>
            <div class="history-card">
              <div class="history-card-head">
                <div>
                  <strong>{{ item.className }}</strong>
                  <span v-if="currentUser.classId === item.classId" class="history-current-mark">当前主班级</span>
                </div>
                <div class="history-tags">
                  <el-tag :type="item.status === '1' ? 'success' : 'info'" size="small" class="cyber-tag">
                    {{ item.status === '1' ? '班级进行中' : '班级已结束' }}
                  </el-tag>
                  <el-tag :type="item.joinStatus === 'ACTIVE' ? 'primary' : 'warning'" size="small" class="cyber-tag">
                    {{ item.joinStatus === 'ACTIVE' ? '学习中' : '已退出' }}
                  </el-tag>
                </div>
              </div>
              <div class="history-time-grid">
                <div>
                  <span>加入时间</span>
                  <strong>{{ formatTime(item.joinTime) }}</strong>
                </div>
                <div>
                  <span>退出时间</span>
                  <strong>{{ item.leaveTime ? formatTime(item.leaveTime) : '-' }}</strong>
                </div>
                <div>
                  <span>学习周期</span>
                  <strong>{{ historyDuration(item.joinTime, item.leaveTime) }}</strong>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无历史记录" />
      </template>
      <template #footer>
        <el-button @click="showHistoryDialog = false" class="cyber-btn">关闭</el-button>
      </template>
    </el-dialog>

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

    <!-- ===== 创建用户弹窗 ===== -->
    <el-dialog v-model="showCreateDialog" title="创建用户" width="440px" :close-on-click-modal="false" @closed="resetCreateForm" class="cyber-dialog">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px" class="cyber-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="如：teacher001" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="createForm.realName" placeholder="如：张老师" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" placeholder="请选择角色" style="width:100%" class="cyber-select">
            <el-option label="讲师" value="TEACHER" />
            <el-option label="班主任" value="ASSISTANT" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false" class="cyber-btn cyber-btn-secondary">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate" class="cyber-btn cyber-btn-primary">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 创建成功：显示初始密码 -->
    <el-dialog v-model="showInitPwdDialog" title="创建成功" width="420px" :show-close="false" :close-on-click-modal="false" class="cyber-dialog">
      <el-result icon="success" title="用户创建成功" class="cyber-result">
        <template #sub-title>
          <p>系统已生成一次性初始密码，请告知用户登录后自行修改</p>
        </template>
        <template #extra>
          <div class="init-pwd-box">
            <span class="init-pwd-label">用户名：</span>
            <el-text type="primary" tag="b">{{ createdUser.username }}</el-text>
          </div>
          <div class="init-pwd-box">
            <span class="init-pwd-label">初始密码：</span>
            <el-text type="danger" tag="b" style="font-size:18px;letter-spacing:2px">
              {{ createdUser.initialPassword }}
            </el-text>
            <el-button size="small" text type="primary" :icon="CopyDocument" @click="copyPwd(createdUser.initialPassword)" class="cyber-btn-text">复制</el-button>
          </div>
        </template>
      </el-result>
      <template #footer>
        <el-button type="primary" @click="showInitPwdDialog = false" class="cyber-btn cyber-btn-primary">我已记录，关闭</el-button>
      </template>
    </el-dialog>

    <!-- ===== 重置密码结果弹窗 ===== -->
    <el-dialog v-model="showResetPwdDialog" title="重置密码成功" width="400px" class="cyber-dialog">
      <div class="init-pwd-box" style="margin-bottom:12px">
        <span class="init-pwd-label">用户名：</span>
        <el-text type="primary" tag="b">{{ resetResult.username }}</el-text>
      </div>
      <div class="init-pwd-box">
        <span class="init-pwd-label">新密码：</span>
        <el-text type="danger" tag="b" style="font-size:18px;letter-spacing:2px">
          {{ resetResult.newPassword }}
        </el-text>
        <el-button size="small" text type="primary" :icon="CopyDocument" @click="copyPwd(resetResult.newPassword)" class="cyber-btn-text">复制</el-button>
      </div>
      <template #footer>
        <el-button type="primary" @click="showResetPwdDialog = false" class="cyber-btn cyber-btn-primary">我已记录，关闭</el-button>
      </template>
    </el-dialog>

    <!-- ===== Excel 批量导入弹窗 ===== -->
    <el-dialog v-model="showImportDialog" title="批量导入用户" width="520px" :close-on-click-modal="false" @closed="resetImport" class="cyber-dialog">
      <!-- 下载模板 -->
      <div class="template-tip">
        <el-icon color="#00ffff"><InfoFilled /></el-icon>
        <span>请按模板格式填写后上传，</span>
        <el-button type="primary" link :icon="Download" @click="downloadTemplate" class="cyber-link">下载导入模板</el-button>
      </div>

      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
        :on-exceed="() => ElMessage.warning('只能上传一个文件')"
        drag
        class="cyber-upload"
      >
        <el-icon :size="40" color="#00ffff"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 文件拖到此处，或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls 格式</div>
        </template>
      </el-upload>

      <!-- 导入结果 -->
      <template v-if="importResult">
        <el-divider class="cyber-divider" />
        <el-descriptions title="导入结果" :column="2" border class="cyber-descriptions">
          <el-descriptions-item label="成功">
            <el-text type="success">{{ importResult.success }} 条</el-text>
          </el-descriptions-item>
          <el-descriptions-item label="失败">
            <el-text type="danger">{{ importResult.failed }} 条</el-text>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="importResult.credentials?.length" class="error-list">
          <div class="error-title">本次导入初始密码：</div>
          <el-scrollbar max-height="180px">
            <div v-for="item in importResult.credentials" :key="item.username" class="error-item">
              {{ item.username }} / {{ item.realName }}：{{ item.initialPassword }}
              <el-button size="small" text type="primary" :icon="CopyDocument" @click="copyPwd(item.initialPassword)">复制</el-button>
            </div>
          </el-scrollbar>
        </div>
        <div v-if="importResult.errors.length" class="error-list">
          <div class="error-title">错误详情：</div>
          <el-scrollbar max-height="160px">
            <div v-for="(err, i) in importResult.errors" :key="i" class="error-item">
              <el-icon color="#ff10f0"><Warning /></el-icon> {{ err }}
            </div>
          </el-scrollbar>
        </div>
      </template>

      <template #footer>
        <el-button @click="showImportDialog = false" class="cyber-btn cyber-btn-secondary">关闭</el-button>
        <el-button type="primary" :loading="importLoading" :disabled="!importFile" @click="submitImport" class="cyber-btn cyber-btn-primary">
          开始导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import type { FormInstance, FormRules, UploadInstance, UploadFile } from 'element-plus'
import {
  Search, Plus, Upload, CopyDocument, UploadFilled, Warning,
  User, UserFilled, Avatar, Reading, InfoFilled, Download
} from '@element-plus/icons-vue'
import {
  getUserList, getUserStats, createUser, batchImportUsers,
  resetPassword, deleteUser
} from '@/api/admin/user'
import type { UserRecord, UserStats, CreateUserResult, BatchImportResult } from '@/api/admin/user'

// ---------- 统计卡片 ----------
const stats = ref<UserStats>({ total: 0, teachers: 0, assistants: 0, students: 0 })

const statCards = computed(() => [
  { label: '总用户数', value: stats.value.total,      icon: 'UserFilled', color: '#00ffff' },
  { label: '讲师',     value: stats.value.teachers,   icon: 'Avatar',     color: '#39ff14' },
  { label: '班主任',   value: stats.value.assistants, icon: 'User',       color: '#ff9800' },
  { label: '学生',     value: stats.value.students,   icon: 'Reading',    color: '#ff10f0' }
])

async function fetchStats() {
  const res = await getUserStats()
  stats.value = res.data
}

// ---------- 列表 ----------
const tableData = ref<UserRecord[]>([])
const tableLoading = ref(false)
const searchKeyword = ref('')
const filterRole = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })
const showHistoryDialog = ref(false)
const currentUser = ref<UserRecord | null>(null)
const historyList = computed(() => (currentUser.value?.classHistory || [])
  .slice()
  .sort((left, right) => Date.parse(right.joinTime || '') - Date.parse(left.joinTime || '')))
const activeHistoryCount = computed(() => historyList.value.filter(item => item.joinStatus === 'ACTIVE').length)
const leftHistoryCount = computed(() => historyList.value.filter(item => item.joinStatus !== 'ACTIVE').length)

async function fetchList() {
  tableLoading.value = true
  try {
    const res = await getUserList({
      page: pagination.page,
      size: pagination.size,
      role: filterRole.value || undefined,
      keyword: searchKeyword.value.trim() || undefined
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

function handleSizeChange(size: number) {
  pagination.size = size
  pagination.page = 1
  fetchList()
}

function viewHistory(row: UserRecord) {
  currentUser.value = row
  showHistoryDialog.value = true
}

// ---------- 角色显示工具 ----------
const roleLabel = (role: string) =>
  ({ ADMIN: '管理员', TEACHER: '讲师', ASSISTANT: '班主任', STUDENT: '学生' }[role] ?? role)

const roleTagType = (role: string): '' | 'success' | 'warning' | 'danger' | 'info' =>
  ({ ADMIN: 'danger', TEACHER: 'success', ASSISTANT: 'warning', STUDENT: '' }[role] as any ?? 'info')

const formatTime = (t: string) => t ? t.replace('T', ' ').slice(0, 19) : '-'

function historyDuration(joinTime?: string, leaveTime?: string) {
  if (!joinTime) return '-'
  const start = Date.parse(joinTime.replace(' ', 'T'))
  const end = leaveTime ? Date.parse(leaveTime.replace(' ', 'T')) : Date.now()
  if (!Number.isFinite(start) || !Number.isFinite(end) || end < start) return '-'
  const days = Math.max(1, Math.ceil((end - start) / 86400000))
  return leaveTime ? `${days} 天` : `${days} 天至今`
}

// ---------- 创建用户 ----------
const showCreateDialog = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ username: '', realName: '', role: '' as 'TEACHER' | 'ASSISTANT' | '' })

const createRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 30, message: '用户名长度 3~30 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: ['change', 'blur'] }]
}

const showInitPwdDialog = ref(false)
const createdUser = ref<Partial<CreateUserResult>>({})

function openCreateDialog() {
  showCreateDialog.value = true
}

function resetCreateForm() {
  createFormRef.value?.resetFields()
}

async function submitCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  createLoading.value = true
  try {
    const res = await createUser({
      username: createForm.username,
      realName: createForm.realName,
      role: createForm.role as 'TEACHER' | 'ASSISTANT'
    })
    showCreateDialog.value = false
    createdUser.value = { ...res.data, initialPassword: res.data.initialPassword }
    showInitPwdDialog.value = true
    fetchList()
    fetchStats()
  } finally {
    createLoading.value = false
  }
}

// ---------- 重置密码 ----------
const showResetPwdDialog = ref(false)
const resetResult = ref({ username: '', newPassword: '' })

async function handleResetPwd(row: UserRecord) {
  await ElMessageBox.confirm(
    `确定要重置用户 <b>${row.realName}（${row.username}）</b> 的密码吗？`,
    '重置密码',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
  )
  const res = await resetPassword(row.id)
  resetResult.value = { username: res.data.username, newPassword: res.data.newPassword }
  showResetPwdDialog.value = true
}

// ---------- 删除用户 ----------
async function handleDelete(row: UserRecord) {
  await ElMessageBox.confirm(
    `确定要删除用户 <b>${row.realName}（${row.username}）</b> 吗？此操作不可恢复！`,
    '删除用户',
    { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'error', dangerouslyUseHTMLString: true }
  )
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  fetchList()
  fetchStats()
}

// ---------- 复制密码 ----------
function copyPwd(pwd: string) {
  navigator.clipboard.writeText(pwd).then(() => ElMessage.success('已复制到剪贴板'))
}

// ---------- 下载导入模板 ----------
async function downloadTemplate() {
  const XLSX = await import('xlsx')
  const headers = [['用户名', '真实姓名', '手机号', '所属学校', '角色']]
  const samples = [
    ['teacher001', '张讲师', '13900000001', '博瑞教育', 'TEACHER'],
    ['assistant001', '李班主任', '13900000002', '博瑞教育', 'ASSISTANT']
  ]
  const ws = XLSX.utils.aoa_to_sheet([...headers, ...samples])

  // 列宽
  ws['!cols'] = [{ wch: 18 }, { wch: 14 }, { wch: 18 }, { wch: 20 }, { wch: 14 }]

  // 备注行
  const remark = [['说明：1.角色只能填写 TEACHER 或 ASSISTANT；2.用户名不能重复；3.手机号若已存在将无法创建']]
  XLSX.utils.sheet_add_aoa(ws, remark, { origin: { r: headers.length + samples.length + 1, c: 0 } })

  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '用户导入模板')
  XLSX.writeFile(wb, '用户批量导入模板.xlsx')
}

// ---------- Excel 批量导入 ----------
const showImportDialog = ref(false)
const importLoading = ref(false)
const uploadRef = ref<UploadInstance>()
const importFile = ref<File | null>(null)
const importResult = ref<BatchImportResult | null>(null)

function handleFileChange(file: UploadFile) {
  importFile.value = file.raw ?? null
  importResult.value = null
}

function resetImport() {
  uploadRef.value?.clearFiles()
  importFile.value = null
  importResult.value = null
}

async function submitImport() {
  if (!importFile.value) return
  importLoading.value = true
  try {
    const res = await batchImportUsers(importFile.value)
    importResult.value = res.data
    if (res.data.success > 0) {
      fetchList()
      fetchStats()
    }
  } finally {
    importLoading.value = false
  }
}

// ---------- 初始化 ----------
onMounted(() => {
  fetchStats()
  fetchList()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  font-family: 'JetBrains Mono', monospace !important;
  background: var(--bg-surface);
  min-height: 100vh;
  padding: 16px;
}

/* 统计卡片 */
.stat-row {
  margin-bottom: 0;
}

.stat-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  clip-path: polygon(0 0, calc(100% - 16px) 0, 100% 16px, 100% 100%, 16px 100%, 0 calc(100% - 16px));
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1), inset 0 0 20px rgba(0, 255, 255, 0.02) !important;
  cursor: default;
  transition: all 0.3s ease !important;
}

.stat-card:hover {
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.3), inset 0 0 30px rgba(0, 255, 255, 0.05) !important;
  border-color: #00ffff !important;
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5), 0 0 20px rgba(0, 255, 255, 0.3);
}

.stat-label {
  font-size: 13px;
  color: var(--text-primary) !important;
  margin-top: 4px;
  opacity: 0.8;
}

/* 主卡片 */
.main-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.1), inset 0 0 30px rgba(0, 255, 255, 0.02) !important;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.filter-group,
.action-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

/* Cyberpunk 按钮 */
.cyber-btn {
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 13px !important;
  font-weight: 600 !important;
  border-radius: 0 !important;
  clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px));
  padding: 8px 16px !important;
  height: auto !important;
  transition: all 0.3s ease !important;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.cyber-btn-primary {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.2), rgba(0, 255, 255, 0.1)) !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3), inset 0 0 10px rgba(0, 255, 255, 0.1) !important;
}

.cyber-btn-primary:hover {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.4), rgba(0, 255, 255, 0.2)) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.5), inset 0 0 15px rgba(0, 255, 255, 0.2) !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.8) !important;
}

.cyber-btn-secondary {
  background: linear-gradient(135deg, rgba(255, 16, 240, 0.15), rgba(255, 16, 240, 0.05)) !important;
  border: 1px solid #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.2), inset 0 0 10px rgba(255, 16, 240, 0.05) !important;
}

.cyber-btn-secondary:hover {
  background: linear-gradient(135deg, rgba(255, 16, 240, 0.3), rgba(255, 16, 240, 0.1)) !important;
  box-shadow: 0 0 20px rgba(255, 16, 240, 0.4), inset 0 0 15px rgba(255, 16, 240, 0.1) !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.8) !important;
}

.cyber-btn-text {
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 12px !important;
  font-weight: 500 !important;
  padding: 2px 6px !important;
  height: auto !important;
  background: transparent !important;
  border: none !important;
  border-radius: 0 !important;
  transition: all 0.3s ease !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.cyber-btn-warning {
  color: #ff9800 !important;
  text-shadow: 0 0 5px rgba(255, 152, 0, 0.5) !important;
}

.cyber-btn-warning:hover {
  background: rgba(255, 152, 0, 0.1) !important;
  text-shadow: 0 0 10px rgba(255, 152, 0, 0.8) !important;
}

.cyber-btn-danger {
  color: #ff10f0 !important;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5) !important;
}

.cyber-btn-danger:hover {
  background: rgba(255, 16, 240, 0.1) !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.8) !important;
}

.cyber-link {
  font-family: 'JetBrains Mono', monospace !important;
  color: #00ffff !important;
  text-shadow: 0 0 5px rgba(0, 255, 255, 0.5) !important;
}

.cyber-link:hover {
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.8) !important;
}

/* Cyberpunk 表格 */
.cyber-table {
  background: transparent !important;
  font-family: 'JetBrains Mono', monospace !important;
  --el-table-bg-color: transparent !important;
  --el-table-tr-bg-color: transparent !important;
  --el-table-header-bg-color: rgba(0, 255, 255, 0.05) !important;
  --el-table-row-hover-bg-color: rgba(0, 255, 255, 0.03) !important;
  --el-table-border-color: var(--border-subtle) !important;
  --el-table-header-text-color: #00ffff !important;
  --el-table-text-color: var(--text-primary) !important;
  border: 1px solid var(--border) !important;
}

.cyber-table :deep(.el-table__header-wrapper th) {
  background: rgba(0, 255, 255, 0.08) !important;
  border-bottom: 1px solid var(--border) !important;
  font-weight: 600 !important;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.cyber-table :deep(.el-table__body-wrapper tr) {
  background: transparent !important;
}

.cyber-table :deep(.el-table__body-wrapper td) {
  border-bottom: 1px solid var(--border) !important;
  background: transparent !important;
}

.cyber-table :deep(.el-table__body-wrapper tr:hover td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.cyber-table :deep(.el-table__empty-block) {
  background: transparent !important;
}

.cyber-table :deep(.el-table__empty-text) {
  color: var(--text-primary) !important;
}

/* Cyberpunk Tag */
.cyber-tag {
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 11px !important;
  font-weight: 600 !important;
  border-radius: 0 !important;
  border: 1px solid !important;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  clip-path: polygon(0 0, calc(100% - 6px) 0, 100% 6px, 100% 100%, 6px 100%, 0 calc(100% - 6px));
}

.history-profile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  background: rgba(0, 255, 255, 0.04);
  margin-bottom: 14px;
}

.history-name {
  color: #00ffff;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}

.history-meta {
  color: var(--text-secondary);
  font-size: 12px;
  margin-top: 4px;
}

.history-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.history-summary-item {
  padding: 12px;
  border: 1px solid var(--border-subtle);
  background: rgba(255, 255, 255, 0.02);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-summary-item strong {
  color: #00ffff;
  font-size: 24px;
  line-height: 1;
}

.history-summary-item span {
  color: var(--text-secondary);
  font-size: 12px;
}

.history-summary-item.active strong {
  color: #39ff14;
}

.history-summary-item.left strong {
  color: #ff9800;
}

.history-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
  max-height: 460px;
  overflow-y: auto;
  padding-right: 4px;
}

.history-timeline-item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  column-gap: 12px;
  position: relative;
}

.history-timeline-item::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 18px;
  bottom: -8px;
  width: 1px;
  background: var(--border);
}

.history-timeline-item:last-child::before {
  display: none;
}

.history-dot {
  width: 11px;
  height: 11px;
  margin-top: 15px;
  border: 2px solid #ff9800;
  background: var(--bg-surface);
  box-shadow: 0 0 10px rgba(255, 152, 0, 0.45);
  z-index: 1;
}

.history-timeline-item.current .history-dot {
  border-color: #39ff14;
  box-shadow: 0 0 12px rgba(57, 255, 20, 0.55);
}

.history-card {
  border: 1px solid var(--border-subtle);
  background: rgba(255, 255, 255, 0.025);
  padding: 12px;
  margin-bottom: 10px;
}

.history-timeline-item.current .history-card {
  border-color: rgba(57, 255, 20, 0.55);
  background: rgba(57, 255, 20, 0.045);
}

.history-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.history-card-head strong {
  color: var(--text-primary);
  font-size: 15px;
}

.history-current-mark {
  margin-left: 8px;
  color: #39ff14;
  font-size: 12px;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.history-time-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.history-time-grid div {
  padding: 8px;
  border: 1px solid var(--border-subtle);
  background: rgba(0, 0, 0, 0.12);
}

.history-time-grid span {
  display: block;
  color: var(--text-secondary);
  font-size: 11px;
  margin-bottom: 4px;
}

.history-time-grid strong {
  display: block;
  color: var(--text-primary);
  font-size: 12px;
  line-height: 1.3;
  word-break: break-word;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.cyber-pagination {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pagination__total) {
  color: var(--text-primary) !important;
}

.cyber-pagination :deep(.el-pagination__sizes) {
  color: var(--text-primary) !important;
}

.cyber-pagination :deep(.el-pager li) {
  background: transparent !important;
  color: var(--text-primary) !important;
  border: 1px solid var(--border) !important;
  font-family: 'JetBrains Mono', monospace !important;
  border-radius: 0 !important;
  margin: 0 2px;
}

.cyber-pagination :deep(.el-pager li:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

.cyber-pagination :deep(.el-pager li.is-active) {
  background: rgba(0, 255, 255, 0.2) !important;
  color: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.5) !important;
}

.cyber-pagination :deep(.el-pagination__jump) {
  color: var(--text-primary) !important;
}

.cyber-pagination :deep(.el-pagination__editor) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  border-radius: 0 !important;
}

.cyber-pagination :deep(.btn-prev),
.cyber-pagination :deep(.btn-next) {
  background: transparent !important;
  border: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  border-radius: 0 !important;
}

.cyber-pagination :deep(.btn-prev:hover),
.cyber-pagination :deep(.btn-next:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

/* Cyberpunk Dialog */
.cyber-dialog {
  font-family: 'JetBrains Mono', monospace !important;
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  clip-path: polygon(0 0, calc(100% - 24px) 0, 100% 24px, 100% 100%, 24px 100%, 0 calc(100% - 24px));
  box-shadow: 0 0 40px rgba(0, 255, 255, 0.2), 0 0 80px rgba(255, 16, 240, 0.1), inset 0 0 40px rgba(0, 255, 255, 0.02) !important;
}

.cyber-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1), rgba(255, 16, 240, 0.05)) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 16px 20px !important;
  margin-right: 0 !important;
}

.cyber-dialog :deep(.el-dialog__title) {
  color: #00ffff !important;
  font-weight: 700 !important;
  font-size: 16px !important;
  text-transform: uppercase;
  letter-spacing: 2px;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5), 0 0 20px rgba(0, 255, 255, 0.3);
}

.cyber-dialog :deep(.el-dialog__headerbtn) {
  top: 16px !important;
  right: 16px !important;
}

.cyber-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: var(--text-primary) !important;
  font-size: 18px !important;
}

.cyber-dialog :deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.8) !important;
}

.cyber-dialog :deep(.el-dialog__body) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  padding: 24px 20px !important;
}

.cyber-dialog :deep(.el-dialog__footer) {
  background: linear-gradient(135deg, rgba(255, 16, 240, 0.05), rgba(0, 255, 255, 0.02)) !important;
  border-top: 1px solid var(--border) !important;
  padding: 16px 20px !important;
}

/* Cyberpunk Form */
.cyber-form :deep(.el-form-item__label) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
  text-transform: uppercase;
  letter-spacing: 1px;
  font-size: 12px !important;
}

.cyber-form :deep(.el-form-item) {
  margin-bottom: 20px !important;
}

.cyber-form :deep(.el-form-item__error) {
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace !important;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5);
}

/* Cyberpunk Input & Select */
.cyber-input :deep(.el-input__wrapper) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  box-shadow: inset 0 0 10px rgba(0, 255, 255, 0.05) !important;
  padding: 4px 12px !important;
}

.cyber-input :deep(.el-input__wrapper:hover) {
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.2), inset 0 0 10px rgba(0, 255, 255, 0.05) !important;
}

.cyber-input :deep(.el-input__wrapper.is-focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.4), inset 0 0 15px rgba(0, 255, 255, 0.1) !important;
}

.cyber-input :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-input :deep(.el-input__inner::placeholder) {
  color: rgba(224, 224, 224, 0.4) !important;
}

.cyber-select :deep(.el-select__wrapper) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  box-shadow: inset 0 0 10px rgba(0, 255, 255, 0.05) !important;
  min-height: 32px !important;
}

.cyber-select :deep(.el-select__wrapper:hover) {
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.2), inset 0 0 10px rgba(0, 255, 255, 0.05) !important;
}

.cyber-select :deep(.el-select__wrapper.is-focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.4), inset 0 0 15px rgba(0, 255, 255, 0.1) !important;
}

.cyber-select :deep(.el-select__selected-item) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select__placeholder) {
  color: rgba(224, 224, 224, 0.4) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select-dropdown) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  border-radius: 0 !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.2), 0 0 40px rgba(255, 16, 240, 0.1) !important;
}

.cyber-select :deep(.el-select-dropdown__item) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
  border-left: 2px solid transparent !important;
}

.cyber-select :deep(.el-select-dropdown__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
  border-left-color: #00ffff !important;
}

.cyber-select :deep(.el-select-dropdown__item.is-selected) {
  background: rgba(0, 255, 255, 0.15) !important;
  color: #00ffff !important;
  font-weight: 600 !important;
  border-left-color: #00ffff !important;
}

.cyber-select :deep(.el-select-dropdown__empty) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

/* Cyberpunk Upload */
.cyber-upload :deep(.el-upload-dragger) {
  background: rgba(0, 255, 255, 0.02) !important;
  border: 2px dashed var(--border) !important;
  border-radius: 0 !important;
  transition: all 0.3s ease !important;
}

.cyber-upload :deep(.el-upload-dragger:hover) {
  border-color: #00ffff !important;
  background: rgba(0, 255, 255, 0.05) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.2), inset 0 0 20px rgba(0, 255, 255, 0.05) !important;
}

.cyber-upload :deep(.el-upload__text) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-upload :deep(.el-upload__tip) {
  color: rgba(224, 224, 224, 0.6) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

/* Cyberpunk Descriptions */
.cyber-descriptions :deep(.el-descriptions__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 700 !important;
  text-transform: uppercase;
  letter-spacing: 1px;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5);
}

.cyber-descriptions :deep(.el-descriptions-item__label) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
  background: rgba(0, 255, 255, 0.05) !important;
  border-color: var(--border-subtle) !important;
}

.cyber-descriptions :deep(.el-descriptions-item__content) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border-color: var(--border-subtle) !important;
}

.cyber-descriptions :deep(.el-descriptions__body) {
  background: transparent !important;
  border-color: var(--border-subtle) !important;
}

/* Cyberpunk Divider */
.cyber-divider {
  border-color: var(--border-subtle) !important;
  background: transparent !important;
}

.cyber-divider :deep(.el-divider__text) {
  background: var(--bg-surface) !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  border-color: var(--border-subtle) !important;
}

/* Cyberpunk Result */
.cyber-result :deep(.el-result__title) {
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.5), 0 0 20px rgba(57, 255, 20, 0.3);
}

.cyber-result :deep(.el-result__subtitle p) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-result :deep(.el-result__icon svg) {
  fill: #39ff14 !important;
  filter: drop-shadow(0 0 10px rgba(57, 255, 20, 0.5));
}

/* 初始密码展示 */
.init-pwd-box {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: var(--text-primary) !important;
}

.init-pwd-label {
  color: #00ffff !important;
  min-width: 70px;
  font-family: 'JetBrains Mono', monospace !important;
  text-transform: uppercase;
  font-size: 12px;
  letter-spacing: 1px;
}

/* 模板提示 */
.template-tip {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-primary) !important;
  margin-bottom: 12px;
  font-family: 'JetBrains Mono', monospace !important;
}

/* 导入错误列表 */
.error-list {
  margin-top: 12px;
}

.error-title {
  font-size: 13px;
  color: #ff10f0 !important;
  margin-bottom: 6px;
  font-weight: 600;
  font-family: 'JetBrains Mono', monospace !important;
  text-transform: uppercase;
  letter-spacing: 1px;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5);
}

.error-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-primary) !important;
  padding: 3px 0;
  font-family: 'JetBrains Mono', monospace !important;
}

/* Override Element Plus global styles */
:deep(.el-tag--success) {
  background: rgba(57, 255, 20, 0.1) !important;
  border-color: #39ff14 !important;
  color: #39ff14 !important;
  box-shadow: 0 0 8px rgba(57, 255, 20, 0.3) !important;
}

:deep(.el-tag--warning) {
  background: rgba(255, 152, 0, 0.1) !important;
  border-color: #ff9800 !important;
  color: #ff9800 !important;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.3) !important;
}

:deep(.el-tag--danger) {
  background: rgba(255, 16, 240, 0.1) !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 8px rgba(255, 16, 240, 0.3) !important;
}

:deep(.el-tag--info) {
  background: rgba(0, 255, 255, 0.1) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3) !important;
}

:deep(.el-text--primary) {
  color: #00ffff !important;
}

:deep(.el-text--success) {
  color: #39ff14 !important;
}

:deep(.el-text--danger) {
  color: #ff10f0 !important;
}

:deep(.el-text--warning) {
  color: #ff9800 !important;
}

:deep(.el-icon) {
  vertical-align: middle;
}

:deep(.el-scrollbar__wrap) {
  background: var(--bg-surface) !important;
}

:deep(.el-scrollbar__thumb) {
  background: rgba(0, 255, 255, 0.3) !important;
}

:deep(.el-scrollbar__thumb:hover) {
  background: rgba(0, 255, 255, 0.5) !important;
}
</style>
