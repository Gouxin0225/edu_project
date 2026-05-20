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
    <el-dialog v-model="showHistoryDialog" title="学员课程历史" width="600px" class="cyber-dialog">
      <div v-if="currentUser" class="history-header">
        <strong>{{ currentUser.realName }}</strong> ({{ currentUser.username }})
      </div>
      <el-table :data="currentUser?.classHistory" border stripe size="small" class="cyber-table">
        <el-table-column prop="className" label="班级名称" min-width="150" />
        <el-table-column label="课程状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'info'" size="small">
              {{ row.status === '1' ? '进行中' : '已结束' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入班状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.joinStatus === 'ACTIVE' ? 'primary' : 'warning'" size="small">
              {{ row.joinStatus === 'ACTIVE' ? '学习中' : '已退出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="joinTime" label="加入时间" width="160">
          <template #default="{ row }">{{ formatTime(row.joinTime) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!currentUser?.classHistory?.length" description="暂无历史记录" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules, UploadInstance, UploadFile } from 'element-plus'
import {
  Search, Plus, Upload, CopyDocument, UploadFilled, Warning,
  User, UserFilled, Avatar, Reading, InfoFilled, Download
} from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'
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
const filterRole = ref('')
const pagination = reactive({ page: 1, size: 10, total: 0 })
const showHistoryDialog = ref(false)
const currentUser = ref<UserRecord | null>(null)

async function fetchList() {
  tableLoading.value = true
  try {
    const res = await getUserList({
      page: pagination.page,
      size: pagination.size,
      role: filterRole.value || undefined
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
function downloadTemplate() {
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
  background: #0a0a0a;
  min-height: 100vh;
  padding: 16px;
}

/* 统计卡片 */
.stat-row {
  margin-bottom: 0;
}

.stat-card {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
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
  color: #e0e0e0 !important;
  margin-top: 4px;
  opacity: 0.8;
}

/* 主卡片 */
.main-card {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
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
  --el-table-border-color: #1a1a2e !important;
  --el-table-header-text-color: #00ffff !important;
  --el-table-text-color: #e0e0e0 !important;
  border: 1px solid #1a1a2e !important;
}

.cyber-table :deep(.el-table__header-wrapper th) {
  background: rgba(0, 255, 255, 0.08) !important;
  border-bottom: 1px solid #1a1a2e !important;
  font-weight: 600 !important;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.cyber-table :deep(.el-table__body-wrapper tr) {
  background: transparent !important;
}

.cyber-table :deep(.el-table__body-wrapper td) {
  border-bottom: 1px solid #1a1a2e !important;
  background: transparent !important;
}

.cyber-table :deep(.el-table__body-wrapper tr:hover td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.cyber-table :deep(.el-table__empty-block) {
  background: transparent !important;
}

.cyber-table :deep(.el-table__empty-text) {
  color: #e0e0e0 !important;
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
  color: #e0e0e0 !important;
}

.cyber-pagination :deep(.el-pagination__sizes) {
  color: #e0e0e0 !important;
}

.cyber-pagination :deep(.el-pager li) {
  background: transparent !important;
  color: #e0e0e0 !important;
  border: 1px solid #1a1a2e !important;
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
  color: #e0e0e0 !important;
}

.cyber-pagination :deep(.el-pagination__editor) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  border-radius: 0 !important;
}

.cyber-pagination :deep(.btn-prev),
.cyber-pagination :deep(.btn-next) {
  background: transparent !important;
  border: 1px solid #1a1a2e !important;
  color: #e0e0e0 !important;
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
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  border-radius: 0 !important;
  clip-path: polygon(0 0, calc(100% - 24px) 0, 100% 24px, 100% 100%, 24px 100%, 0 calc(100% - 24px));
  box-shadow: 0 0 40px rgba(0, 255, 255, 0.2), 0 0 80px rgba(255, 16, 240, 0.1), inset 0 0 40px rgba(0, 255, 255, 0.02) !important;
}

.cyber-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1), rgba(255, 16, 240, 0.05)) !important;
  border-bottom: 1px solid #1a1a2e !important;
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
  color: #e0e0e0 !important;
  font-size: 18px !important;
}

.cyber-dialog :deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.8) !important;
}

.cyber-dialog :deep(.el-dialog__body) {
  background: #0a0a0a !important;
  color: #e0e0e0 !important;
  padding: 24px 20px !important;
}

.cyber-dialog :deep(.el-dialog__footer) {
  background: linear-gradient(135deg, rgba(255, 16, 240, 0.05), rgba(0, 255, 255, 0.02)) !important;
  border-top: 1px solid #1a1a2e !important;
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
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
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
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-input :deep(.el-input__inner::placeholder) {
  color: rgba(224, 224, 224, 0.4) !important;
}

.cyber-select :deep(.el-select__wrapper) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
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
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select__placeholder) {
  color: rgba(224, 224, 224, 0.4) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select-dropdown) {
  background: #0a0a0a !important;
  border: 1px solid #1a1a2e !important;
  border-radius: 0 !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.2), 0 0 40px rgba(255, 16, 240, 0.1) !important;
}

.cyber-select :deep(.el-select-dropdown__item) {
  color: #e0e0e0 !important;
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
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

/* Cyberpunk Upload */
.cyber-upload :deep(.el-upload-dragger) {
  background: rgba(0, 255, 255, 0.02) !important;
  border: 2px dashed #1a1a2e !important;
  border-radius: 0 !important;
  transition: all 0.3s ease !important;
}

.cyber-upload :deep(.el-upload-dragger:hover) {
  border-color: #00ffff !important;
  background: rgba(0, 255, 255, 0.05) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.2), inset 0 0 20px rgba(0, 255, 255, 0.05) !important;
}

.cyber-upload :deep(.el-upload__text) {
  color: #e0e0e0 !important;
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
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
  background: rgba(0, 255, 255, 0.05) !important;
  border-color: #1a1a2e !important;
}

.cyber-descriptions :deep(.el-descriptions-item__content) {
  color: #e0e0e0 !important;
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border-color: #1a1a2e !important;
}

.cyber-descriptions :deep(.el-descriptions__body) {
  background: transparent !important;
  border-color: #1a1a2e !important;
}

/* Cyberpunk Divider */
.cyber-divider {
  border-color: #1a1a2e !important;
  background: transparent !important;
}

.cyber-divider :deep(.el-divider__text) {
  background: #0a0a0a !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  border-color: #1a1a2e !important;
}

/* Cyberpunk Result */
.cyber-result :deep(.el-result__title) {
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace !important;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.5), 0 0 20px rgba(57, 255, 20, 0.3);
}

.cyber-result :deep(.el-result__subtitle p) {
  color: #e0e0e0 !important;
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
  color: #e0e0e0 !important;
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
  color: #e0e0e0 !important;
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
  color: #e0e0e0 !important;
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
  background: #0a0a0a !important;
}

:deep(.el-scrollbar__thumb) {
  background: rgba(0, 255, 255, 0.3) !important;
}

:deep(.el-scrollbar__thumb:hover) {
  background: rgba(0, 255, 255, 0.5) !important;
}
</style>
