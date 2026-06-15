<template>
  <div class="page">
    <el-row :gutter="16">
      <!-- 左侧：班级列表 -->
      <el-col :span="12">
        <el-card shadow="never" class="cyber-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">我的班级</span>
              <el-button type="primary" :icon="Plus" @click="openAddStudentDialog" :disabled="!selectedClass" class="cyber-btn">
                添加学员
              </el-button>
            </div>
          </template>

          <el-table
            :data="classList"
            v-loading="classLoading"
            highlight-current-row
            @row-click="handleClassRowClick"
            :row-class-name="getRowClassName"
            stripe
            border
            style="width:100%"
            class="cyber-table"
          >
            <el-table-column prop="className" label="班级名称" min-width="150" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small" class="cyber-tag">
                  {{ row.status === 1 ? '进行中' : '已结课' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="班级人数" width="100" align="center">
              <template #default="{ row }">
                <el-badge :value="getStudentCount(row.id)" type="primary" :max="99" class="cyber-badge" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 右侧：班级学员 -->
      <el-col :span="12">
        <el-card shadow="never" class="cyber-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                {{ selectedClass ? `${selectedClass.className} - 学员列表` : '请选择班级' }}
              </span>
              <el-button
                v-if="selectedClass"
                type="success"
                size="small"
                @click="showBatchImportDialog = true"
                class="cyber-btn cyber-btn-success"
              >
                批量导入
              </el-button>
            </div>
          </template>

          <div v-if="selectedClass" class="student-toolbar">
            <el-input
              v-model="studentKeyword"
              placeholder="搜索用户名、姓名、手机号或学校"
              clearable
              class="cyber-input student-search"
              @keyup.enter="searchStudents"
              @clear="searchStudents"
            />
            <el-button type="primary" size="small" :loading="studentLoading" class="cyber-btn" @click="searchStudents">
              查找
            </el-button>
          </div>

          <el-table
            v-if="selectedClass"
            :data="filteredStudentList"
            v-loading="studentLoading"
            stripe
            border
            style="width:100%"
            class="cyber-table"
          >
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="realName" label="姓名" min-width="100" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column label="操作" width="210" align="center">
              <template #default="{ row }">
                <el-button type="primary" size="small" text class="cyber-btn-text cyber-btn-edit" @click="openEditStudent(row)">编辑</el-button>
                <el-popconfirm title="确定重置该学员密码？" @confirm="resetPassword(row)">
                  <template #reference>
                    <el-button type="warning" size="small" text class="cyber-btn-text">重置密码</el-button>
                  </template>
                </el-popconfirm>
                <el-popconfirm title="确定将该学员移出当前班级？学员账号不会被删除。" @confirm="removeStudent(row)">
                  <template #reference>
                    <el-button type="danger" size="small" text class="cyber-btn-text cyber-btn-danger">移出班级</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-else description="请从左侧选择班级" :image-size="80" class="cyber-empty" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加学员弹窗 -->
    <el-dialog v-model="showAddStudentDialog" title="添加学员" width="420px" class="cyber-dialog">
      <el-form ref="studentFormRef" :model="studentForm" :rules="studentRules" label-width="80px" class="cyber-form">
        <el-form-item label="已有账号">
          <el-select
            v-model="existingStudentId"
            filterable
            remote
            clearable
            reserve-keyword
            :remote-method="searchExistingStudentOptions"
            :loading="existingStudentLoading"
            placeholder="输入姓名、手机号或用户名搜索"
            style="width:100%"
            class="cyber-select"
            @change="applyExistingStudent"
          >
            <el-option
              v-for="student in existingStudentOptions"
              :key="student.id"
              :label="`${student.realName} / ${student.username}${student.phone ? ' / ' + student.phone : ''}`"
              :value="student.id"
            >
              <div class="existing-student-option">
                <span>{{ student.realName }} / {{ student.username }}</span>
                <small>{{ student.phone || '无手机号' }}{{ student.schoolName ? ` / ${student.schoolName}` : '' }}</small>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="studentForm.username" placeholder="学号/用户名；已有手机号可复用账号" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="studentForm.realName" placeholder="真实姓名" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="studentForm.phone" placeholder="手机号唯一；存在则直接加入班级" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="学校" prop="schoolName">
          <el-input v-model="studentForm.schoolName" placeholder="所属学校（可选）" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <el-select v-model="studentForm.classId" placeholder="请选择班级" style="width:100%" class="cyber-select">
            <el-option
              v-for="c in classList"
              :key="c.id"
              :label="c.className"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddStudentDialog = false" class="cyber-btn cyber-btn-default">取消</el-button>
        <el-button type="primary" :loading="addStudentLoading" @click="submitAddStudent" class="cyber-btn">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="showBatchImportDialog" title="批量导入学员" width="500px" class="cyber-dialog">
      <div class="template-tip" style="margin-bottom: 16px; display: flex; align-items: center; gap: 8px;">
        <el-icon color="#00ffff"><InfoFilled /></el-icon>
        <span style="color: rgba(255,255,255,0.7); font-size: 13px;">请按模板格式填写后上传，</span>
        <el-button type="primary" link :icon="Download" @click="downloadTemplate" class="cyber-link">下载导入模板</el-button>
      </div>

      <el-form label-width="90px" class="cyber-form">
        <el-form-item label="目标班级">
          <el-input :value="selectedClass?.className" disabled class="cyber-input" />
        </el-form-item>
        <el-form-item label="Excel文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
            class="cyber-upload"
          >
            <el-button type="primary" class="cyber-btn">选择Excel文件</el-button>
            <template #tip>
              <div class="el-upload__tip cyber-tip">
                Excel格式：手机号、真实姓名、所属学校。手机号将作为登录账号，姓名必填；手机号已存在时会复用账号并加入本班。
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <div v-if="batchImportResult?.credentials?.length" class="batch-result">
        <div class="cyber-tip">本次导入初始密码：</div>
        <el-scrollbar max-height="180px">
          <div v-for="item in batchImportResult.credentials" :key="item.username" class="credential-row">
            {{ item.username }} / {{ item.realName }}：{{ item.initialPassword }}
            <el-button size="small" text type="primary" :icon="CopyDocument" @click="copyText(item.initialPassword)">复制</el-button>
          </div>
        </el-scrollbar>
      </div>
      <template #footer>
        <el-button @click="showBatchImportDialog = false" class="cyber-btn cyber-btn-default">取消</el-button>
        <el-button type="primary" :loading="batchImportLoading" :disabled="!importFile" @click="submitBatchImport" class="cyber-btn">
          开始导入
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showEditStudentDialog" title="编辑学员资料" width="420px" class="cyber-dialog">
      <el-form ref="editStudentFormRef" :model="editStudentForm" :rules="editStudentRules" label-width="80px" class="cyber-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editStudentForm.username" placeholder="登录用户名" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="editStudentForm.realName" placeholder="真实姓名" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editStudentForm.phone" placeholder="11位手机号，可同步作为用户名" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="学校" prop="schoolName">
          <el-input v-model="editStudentForm.schoolName" placeholder="所属学校（可选）" clearable class="cyber-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditStudentDialog = false" class="cyber-btn cyber-btn-default">取消</el-button>
        <el-button type="primary" :loading="editStudentLoading" @click="submitEditStudent" class="cyber-btn">保存修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showPasswordDialog" title="密码已生成" width="420px" class="cyber-dialog">
      <div class="init-pwd-box" style="margin-bottom:12px">
        <span>用户名：</span>
        <el-text type="primary" tag="b">{{ passwordResult.username }}</el-text>
      </div>
      <div class="init-pwd-box">
        <span>密码：</span>
        <el-text type="danger" tag="b" style="font-size:18px;letter-spacing:2px">
          {{ passwordResult.password }}
        </el-text>
        <el-button size="small" text type="primary" :icon="CopyDocument" @click="copyPassword">复制</el-button>
      </div>
      <template #footer>
        <el-button type="primary" @click="showPasswordDialog = false" class="cyber-btn">我已记录，关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { CopyDocument, Plus, Download, InfoFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadInstance } from 'element-plus'
import { getMyClasses, createStudent, searchExistingStudents, batchImportStudents, getStudentList, resetStudentPassword, updateStudent, removeStudentFromClass } from '@/api/teacher'
import type { TeacherClass, StudentRecord } from '@/api/teacher'

const classList = ref<TeacherClass[]>([])
const classLoading = ref(false)
const selectedClass = ref<TeacherClass | null>(null)

const studentList = ref<StudentRecord[]>([])
const studentLoading = ref(false)
const studentMap = ref<Map<number, StudentRecord[]>>(new Map())
const studentKeyword = ref('')
const filteredStudentList = computed(() => {
  const keyword = studentKeyword.value.trim().toLowerCase()
  if (!keyword) return studentList.value
  return studentList.value.filter((student) => {
    return [
      student.username,
      student.realName,
      student.phone,
      student.schoolName,
      student.className
    ].some((value) => String(value || '').toLowerCase().includes(keyword))
  })
})

const showAddStudentDialog = ref(false)
const addStudentLoading = ref(false)
const existingStudentId = ref<number | null>(null)
const existingStudentLoading = ref(false)
const existingStudentOptions = ref<StudentRecord[]>([])
const studentFormRef = ref<FormInstance>()
const studentForm = reactive({
  username: '',
  realName: '',
  phone: '',
  schoolName: '',
  classId: null as number | null
})
const studentRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度3-50字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度2-50字符', trigger: 'blur' }
  ],
  classId: [
    { required: true, message: '请选择班级', trigger: 'change' }
  ]
}

const showBatchImportDialog = ref(false)
const batchImportLoading = ref(false)
const uploadRef = ref<UploadInstance>()
const importFile = ref<File | null>(null)
const batchImportResult = ref<any>(null)
const showPasswordDialog = ref(false)
const passwordResult = reactive({ username: '', password: '' })
const showEditStudentDialog = ref(false)
const editStudentLoading = ref(false)
const editStudentFormRef = ref<FormInstance>()
const editingStudentId = ref<number | null>(null)
const editStudentForm = reactive({
  username: '',
  realName: '',
  phone: '',
  schoolName: ''
})
const editStudentRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度3-50字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 50, message: '姓名长度2-50字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1\d{10}$/, message: '请输入11位手机号', trigger: 'blur' }
  ]
}

// ---------- 下载导入模板 ----------
async function downloadTemplate() {
  const XLSX = await import('xlsx')
  const headers = [['手机号', '真实姓名', '所属学校']]
  const samples = [
    ['13812345678', '学员小明', '某某学校']
  ]
  const ws = XLSX.utils.aoa_to_sheet([...headers, ...samples])

  // 列宽
  ws['!cols'] = [{ wch: 18 }, { wch: 14 }, { wch: 20 }]

  // 备注行
  const remark = [['说明：1.手机号必填且必须为11位手机号，系统将手机号作为登录账号；2.真实姓名必填；3.密码统一默认为 openlab@123']]
  XLSX.utils.sheet_add_aoa(ws, remark, { origin: { r: headers.length + samples.length + 1, c: 0 } })

  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '学员导入模板')
  XLSX.writeFile(wb, '学员批量导入模板.xlsx')
}

async function fetchMyClasses() {
  classLoading.value = true
  try {
    const res = await getMyClasses()
    classList.value = res.data || []
    if (classList.value.length > 0 && !selectedClass.value) {
      selectedClass.value = classList.value[0]
      fetchStudentsForClass(selectedClass.value.id)
    }
  } catch {
    ElMessage.error('获取班级列表失败')
  } finally {
    classLoading.value = false
  }
}

async function fetchStudentsForClass(classId: number) {
  studentLoading.value = true
  try {
    const res = await getStudentList(classId, studentKeyword.value)
    studentList.value = res.data || []
    if (!studentKeyword.value.trim()) {
      studentMap.value.set(classId, studentList.value)
    }
  } catch {
    ElMessage.error('获取学员列表失败')
  } finally {
    studentLoading.value = false
  }
}

function getStudentCount(classId: number): number {
  return studentMap.value.get(classId)?.length || 0
}

function handleClassRowClick(row: TeacherClass) {
  selectedClass.value = row
  studentKeyword.value = ''
  if (!studentMap.value.has(row.id)) {
    fetchStudentsForClass(row.id)
  } else {
    studentList.value = studentMap.value.get(row.id) || []
  }
}

function getRowClassName({ row }: { row: TeacherClass }) {
  return selectedClass.value?.id === row.id ? 'selected-row' : ''
}

function searchStudents() {
  if (!selectedClass.value) return
  fetchStudentsForClass(selectedClass.value.id)
}

function openAddStudentDialog() {
  existingStudentId.value = null
  existingStudentOptions.value = []
  studentForm.username = ''
  studentForm.realName = ''
  studentForm.phone = ''
  studentForm.schoolName = ''
  studentForm.classId = selectedClass.value?.id || null
  showAddStudentDialog.value = true
}

async function searchExistingStudentOptions(keyword: string) {
  const targetClassId = studentForm.classId || selectedClass.value?.id
  const searchKeyword = keyword.trim()
  if (!targetClassId || !searchKeyword) {
    existingStudentOptions.value = []
    return
  }

  existingStudentLoading.value = true
  try {
    const res = await searchExistingStudents({ targetClassId, keyword: searchKeyword, limit: 20 })
    existingStudentOptions.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e.message || '搜索已有学员失败')
  } finally {
    existingStudentLoading.value = false
  }
}

function applyExistingStudent(studentId: number | null) {
  if (!studentId) return
  const student = existingStudentOptions.value.find((item) => item.id === studentId)
  if (!student) return
  studentForm.username = student.username || ''
  studentForm.realName = student.realName || ''
  studentForm.phone = student.phone || ''
  studentForm.schoolName = student.schoolName || ''
}

async function submitAddStudent() {
  const valid = await studentFormRef.value?.validate().catch(() => false)
  if (!valid) return

  addStudentLoading.value = true
  try {
    const classId = studentForm.classId!
    const res = await createStudent({
      username: studentForm.username,
      realName: studentForm.realName,
      phone: studentForm.phone || undefined,
      schoolName: studentForm.schoolName || undefined,
      classId,
      role: 'STUDENT'
    })
    ElMessage.success(res.data.initialPassword ? '学员账号已创建并加入班级' : '已有学员账号已加入班级')
    if (res.data.initialPassword) {
      passwordResult.username = res.data.username
      passwordResult.password = res.data.initialPassword
      showPasswordDialog.value = true
    }
    showAddStudentDialog.value = false
    existingStudentId.value = null
    existingStudentOptions.value = []
    studentFormRef.value?.resetFields()

    if (selectedClass.value?.id === classId) {
      fetchStudentsForClass(classId)
    }
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  } finally {
    addStudentLoading.value = false
  }
}

function handleFileChange(file: any) {
  importFile.value = file.raw
  batchImportResult.value = null
}

async function submitBatchImport() {
  if (!importFile.value || !selectedClass.value) return

  batchImportLoading.value = true
  try {
    const res = await batchImportStudents(selectedClass.value.id, importFile.value)
    const data = res.data
    batchImportResult.value = data
    ElMessage.success(`导入完成：成功 ${data.success} 条，失败 ${data.failed} 条`)
    if (data.errors?.length) {
      console.warn('导入错误:', data.errors)
    }
    fetchStudentsForClass(selectedClass.value.id)
    if (!data.credentials?.length) {
      showBatchImportDialog.value = false
      uploadRef.value?.clearFiles()
      importFile.value = null
    }
  } catch (e: any) {
    ElMessage.error(e.message || '导入失败')
  } finally {
    batchImportLoading.value = false
  }
}

async function resetPassword(row: StudentRecord) {
  try {
    const res = await resetStudentPassword(row.id)
    passwordResult.username = res.data.username
    passwordResult.password = res.data.newPassword
    showPasswordDialog.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '重置失败')
  }
}

function openEditStudent(row: StudentRecord) {
  editingStudentId.value = row.id
  editStudentForm.username = row.username || ''
  editStudentForm.realName = row.realName || ''
  editStudentForm.phone = row.phone || ''
  editStudentForm.schoolName = row.schoolName || ''
  showEditStudentDialog.value = true
}

async function submitEditStudent() {
  const valid = await editStudentFormRef.value?.validate().catch(() => false)
  if (!valid || editingStudentId.value == null) return

  editStudentLoading.value = true
  try {
    await updateStudent(editingStudentId.value, {
      username: editStudentForm.username,
      realName: editStudentForm.realName,
      phone: editStudentForm.phone || undefined,
      schoolName: editStudentForm.schoolName || undefined
    })
    ElMessage.success('学员资料已更新')
    showEditStudentDialog.value = false
    if (selectedClass.value) {
      studentMap.value.delete(selectedClass.value.id)
      await fetchStudentsForClass(selectedClass.value.id)
    }
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    editStudentLoading.value = false
  }
}

async function removeStudent(row: StudentRecord) {
  if (!selectedClass.value) return
  try {
    await removeStudentFromClass(row.id, selectedClass.value.id)
    ElMessage.success('已将学员移出当前班级')
    if (selectedClass.value) {
      studentMap.value.delete(selectedClass.value.id)
      await fetchStudentsForClass(selectedClass.value.id)
    }
  } catch (e: any) {
    ElMessage.error(e.message || '移出班级失败')
  }
}

async function copyPassword() {
  await copyText(passwordResult.password)
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动复制')
  }
}

onMounted(() => {
  fetchMyClasses()
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.page {
  padding: 0;
  background: var(--bg-surface);
  min-height: 100vh;
  font-family: 'JetBrains Mono', monospace;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #00ffff;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.3);
  letter-spacing: 1px;
}

.cyber-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px));
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1), inset 0 0 30px rgba(0, 0, 0, 0.5) !important;
}

.cyber-card :deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.8), rgba(10, 10, 10, 0.9)) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 16px 20px !important;
}

.cyber-card :deep(.el-card__body) {
  background: var(--bg-surface) !important;
  padding: 20px !important;
}

.cyber-btn {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, var(--bg-surface), var(--bg-surface)) !important;
  border: 1px solid #ff10f0 !important;
  color: #ff10f0 !important;
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
  box-shadow: 0 0 10px rgba(255, 16, 240, 0.3) !important;
  transition: all 0.3s ease !important;
  font-size: 12px !important;
  padding: 8px 16px !important;
}

.cyber-btn:hover {
  background: linear-gradient(135deg, #ff10f0, var(--bg-surface)) !important;
  color: #0a0a0a !important;
  box-shadow: 0 0 20px rgba(255, 16, 240, 0.6) !important;
}

.cyber-btn:active {
  transform: scale(0.98) !important;
}

.cyber-btn-success {
  border-color: #39ff14 !important;
  color: #39ff14 !important;
  box-shadow: 0 0 10px rgba(57, 255, 20, 0.3) !important;
}

.cyber-btn-success:hover {
  background: linear-gradient(135deg, #39ff14, var(--bg-surface)) !important;
  color: #0a0a0a !important;
  box-shadow: 0 0 20px rgba(57, 255, 20, 0.6) !important;
}

.cyber-btn-default {
  border-color: var(--text-primary) !important;
  color: var(--text-primary) !important;
  box-shadow: 0 0 10px rgba(224, 224, 224, 0.1) !important;
}

.cyber-btn-default:hover {
  background: linear-gradient(135deg, var(--text-primary), var(--bg-surface)) !important;
  color: #0a0a0a !important;
  box-shadow: 0 0 15px rgba(224, 224, 224, 0.3) !important;
}

.cyber-btn-text {
  font-family: 'JetBrains Mono', monospace !important;
  color: #ff9800 !important;
  border: none !important;
  background: transparent !important;
  font-size: 12px !important;
  padding: 4px 8px !important;
}

.cyber-btn-text:hover {
  color: #ff9800 !important;
  text-shadow: 0 0 10px rgba(255, 152, 0, 0.5) !important;
}

.cyber-btn-edit {
  color: #00ffff !important;
}

.cyber-btn-edit:hover {
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-btn-danger {
  color: #ff4d4f !important;
}

.cyber-btn-danger:hover {
  color: #ff4d4f !important;
  text-shadow: 0 0 10px rgba(255, 77, 79, 0.5) !important;
}

.cyber-table {
  background: var(--bg-surface) !important;
  font-family: 'JetBrains Mono', monospace !important;
  color: var(--text-primary) !important;
}

.student-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.student-search {
  flex: 1;
  min-width: 0;
}

.existing-student-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.3;
}

.existing-student-option small {
  color: var(--text-secondary);
  font-size: 11px;
}

.cyber-table :deep(.el-table__header-wrapper th) {
  background: var(--bg-surface) !important;
  color: #00ffff !important;
  border-bottom: 1px solid var(--border) !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
}

.cyber-table :deep(.el-table__body-wrapper tr) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
}

.cyber-table :deep(.el-table__body-wrapper td) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  border-bottom: 1px solid var(--border) !important;
}

.cyber-table :deep(.el-table__body-wrapper tr:hover td) {
  background: rgba(26, 26, 46, 0.5) !important;
}

.cyber-table :deep(.el-table__body-wrapper .el-table__row:hover) {
  background: rgba(26, 26, 46, 0.5) !important;
}

.cyber-table :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.3) !important;
}

.cyber-table :deep(.el-table--border .el-table__cell) {
  border-right: 1px solid var(--border) !important;
}

.cyber-table :deep(.el-table--border) {
  border: 1px solid var(--border) !important;
}

.cyber-table :deep(.el-table__header-wrapper th .cell) {
  color: #00ffff !important;
}

.cyber-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid !important;
}

.cyber-tag.el-tag--success {
  border-color: #39ff14 !important;
  color: #39ff14 !important;
  box-shadow: 0 0 8px rgba(57, 255, 20, 0.3) !important;
}

.cyber-tag.el-tag--info {
  border-color: var(--text-primary) !important;
  color: var(--text-primary) !important;
}

.cyber-badge :deep(.el-badge__content) {
  background: linear-gradient(135deg, #ff10f0, var(--bg-surface)) !important;
  border: 1px solid #ff10f0 !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
  box-shadow: 0 0 8px rgba(255, 16, 240, 0.3) !important;
}

.cyber-empty :deep(.el-empty__description) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-empty :deep(.el-empty__image svg) {
  fill: #1a1a2e !important;
}

.cyber-dialog {
  font-family: 'JetBrains Mono', monospace !important;
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px));
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.2), 0 0 60px rgba(255, 16, 240, 0.1) !important;
}

.cyber-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.9), rgba(10, 10, 10, 0.95)) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 20px !important;
}

.cyber-dialog :deep(.el-dialog__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
  letter-spacing: 1px !important;
}

.cyber-dialog :deep(.el-dialog__body) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  padding: 30px 20px !important;
}

.cyber-dialog :deep(.el-dialog__footer) {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.8), rgba(10, 10, 10, 0.9)) !important;
  border-top: 1px solid var(--border) !important;
  padding: 16px 20px !important;
}

.cyber-form :deep(.el-form-item__label) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 500 !important;
  letter-spacing: 0.5px !important;
}

.cyber-input {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-input :deep(.el-input__wrapper) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: none !important;
  clip-path: polygon(0 0, calc(100% - 6px) 0, 100% 6px, 100% 100%, 6px 100%, 0 calc(100% - 6px)) !important;
}

.cyber-input :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-input :deep(.el-input__inner::placeholder) {
  color: rgba(224, 224, 224, 0.3) !important;
}

.cyber-input :deep(.el-input__wrapper:hover) {
  border-color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.2) !important;
}

.cyber-input :deep(.el-input__wrapper.is-focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-select :deep(.el-select__wrapper) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: none !important;
  clip-path: polygon(0 0, calc(100% - 6px) 0, 100% 6px, 100% 100%, 6px 100%, 0 calc(100% - 6px)) !important;
}

.cyber-select :deep(.el-select__placeholder) {
  color: rgba(224, 224, 224, 0.3) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select__selected-item) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select-dropdown) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
}

.cyber-select :deep(.el-select-dropdown__item) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-select :deep(.el-select-dropdown__item.hover),
.cyber-select :deep(.el-select-dropdown__item:hover) {
  background: rgba(26, 26, 46, 0.8) !important;
}

.cyber-select :deep(.el-select-dropdown__item.selected) {
  color: #00ffff !important;
  background: rgba(0, 255, 255, 0.1) !important;
}

.cyber-upload :deep(.el-upload__tip) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-tip {
  color: rgba(224, 224, 224, 0.5) !important;
  margin-top: 8px !important;
}

:deep(.selected-row) {
  background-color: rgba(0, 255, 255, 0.1) !important;
}

:deep(.el-popconfirm) {
  font-family: 'JetBrains Mono', monospace !important;
}

:deep(.el-popconfirm .el-popconfirm__main) {
  color: var(--text-primary) !important;
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
}

:deep(.el-message) {
  font-family: 'JetBrains Mono', monospace !important;
}

:deep(.el-message--success) {
  background: var(--bg-surface) !important;
  border-color: #39ff14 !important;
  color: #39ff14 !important;
}

:deep(.el-message--error) {
  background: var(--bg-surface) !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
}

:deep(.el-message--warning) {
  background: var(--bg-surface) !important;
  border-color: #ff9800 !important;
  color: #ff9800 !important;
}

:deep(.el-message--info) {
  background: var(--bg-surface) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
}

:deep(.el-loading-mask) {
  background: rgba(3, 3, 3, 0.9) !important;
}

:deep(.el-loading-spinner .circular) {
  stroke: #00ffff !important;
}

:deep(.el-loading-text) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

:deep(.el-icon) {
  color: inherit !important;
}

:deep(.el-table__row.current-row) {
  background: rgba(0, 255, 255, 0.1) !important;
}

:deep(.el-table__row:hover) {
  background: rgba(26, 26, 46, 0.5) !important;
}
</style>
