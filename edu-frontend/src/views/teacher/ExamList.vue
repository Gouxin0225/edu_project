<template>
  <div class="page">
    <el-card shadow="never" class="cyber-card">
      <template #header>
        <div class="header-bar">
          <span class="cyber-title">考试列表</span>
          <div class="header-actions">
            <el-button type="default" @click="goTemplates" class="cyber-btn-outline">试卷模板</el-button>
            <el-button type="primary" :icon="Plus" @click="goCreate" class="cyber-btn">创建考试</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="tableLoading" stripe border class="cyber-table">
        <el-table-column prop="title" label="考试标题" min-width="150" show-overflow-tooltip />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'EXAM' ? 'primary' : 'success'" size="small" class="cyber-tag">
              {{ row.type === 'EXAM' ? '考试' : '作业' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间范围" width="180">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }} ~ {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="时长(分钟)" width="100" align="center" />
        <el-table-column prop="totalScore" label="总分" width="80" align="center" />
        <el-table-column prop="passScore" label="及格分" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" class="cyber-tag">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.editable"
              size="small"
              type="primary"
              text
              @click="goEdit(row)"
              class="cyber-btn-text"
            >编辑</el-button>
            <el-button size="small" type="info" text @click="viewDetail(row)" class="cyber-btn-text">查看</el-button>
            <el-button
              v-if="row.status === 'PUBLISHED' || row.status === 'ENDED'"
              size="small"
              type="success"
              text
              @click="goGrade(row)"
              class="cyber-btn-text"
            >批改</el-button>
            <el-button
              size="small"
              type="warning"
              text
              :icon="Download"
              :loading="exportingId === row.id"
              @click="handleExport(row)"
              class="cyber-btn-text"
            >下载答题成绩</el-button>
            <el-button
              v-if="row.status === 'PUBLISHED' || row.status === 'ENDED'"
              size="small"
              type="primary"
              text
              @click="viewParticipation(row)"
              class="cyber-btn-text"
            >参考情况</el-button>
            <el-button
              size="small"
              type="danger"
              text
              @click="handleDelete(row)"
              class="cyber-btn-text"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchList"
          @size-change="(s) => { pagination.size = s; pagination.page = 1; fetchList() }"
          class="cyber-pagination"
        />
      </div>
    </el-card>

    <el-dialog v-model="showDetail" title="考试详情" width="600px" class="cyber-dialog">
      <el-descriptions :column="2" border v-if="currentExam" class="cyber-descriptions">
        <el-descriptions-item label="考试标题">{{ currentExam.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag :type="currentExam.type === 'EXAM' ? 'primary' : 'success'" size="small" class="cyber-tag">
            {{ currentExam.type === 'EXAM' ? '考试' : '作业' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatTime(currentExam.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="截止时间">{{ formatTime(currentExam.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="考试时长">{{ currentExam.duration }} 分钟</el-descriptions-item>
        <el-descriptions-item label="总分">{{ currentExam.totalScore }}</el-descriptions-item>
        <el-descriptions-item label="及格分数">{{ currentExam.passScore }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(currentExam.status)" size="small" class="cyber-tag">{{ statusLabel(currentExam.status) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="showDetail = false" class="cyber-btn">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showParticipation" :title="`参考情况 - ${participationExam?.examTitle}`" width="1000px" class="cyber-dialog">
      <el-row :gutter="16" v-if="participationExam">
        <el-col :span="8">
          <el-card shadow="never" header="已交卷" class="cyber-card-inner">
            <div v-if="participationExam.submittedStudents.length === 0" class="empty-tip">
              暂无已交卷学生
            </div>
            <el-table v-else :data="participationExam.submittedStudents" max-height="400" size="small" class="cyber-table">
              <el-table-column prop="studentName" label="姓名" />
              <el-table-column prop="status" label="状态">
                <template #default="{ row }">
                  <el-tag :type="participationStatusType(row.status)" size="small" class="cyber-tag">
                    {{ participationStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="submitTime" label="交卷时间" />
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never" header="答题中" class="cyber-card-inner">
            <div v-if="participationExam.inProgressStudents.length === 0" class="empty-tip">
              暂无答题中学生
            </div>
            <el-table v-else :data="participationExam.inProgressStudents" max-height="400" size="small" class="cyber-table">
              <el-table-column prop="studentName" label="姓名" />
              <el-table-column prop="status" label="状态">
                <template #default="{ row }">
                  <el-tag :type="participationStatusType(row.status)" size="small" class="cyber-tag">
                    {{ participationStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="startTime" label="开始时间" />
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never" header="未参加" class="cyber-card-inner">
            <div v-if="participationExam.notSubmittedStudents.length === 0" class="empty-tip">
              暂无未参加学生
            </div>
            <el-table v-else :data="participationExam.notSubmittedStudents" max-height="400" size="small" class="cyber-table">
              <el-table-column prop="studentName" label="姓名" />
              <el-table-column prop="status" label="状态">
                <template #default>
                  <el-tag type="danger" size="small" class="cyber-tag">未参加</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
      <template #footer>
        <el-button
          :icon="Download"
          :disabled="!participationExam?.notSubmittedStudents.length"
          @click="exportNotSubmittedStudents"
          class="cyber-btn-outline"
        >
          导出未参加名单
        </el-button>
        <el-button @click="showParticipation = false" class="cyber-btn">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Download, Plus } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { deleteExam, getExamList, getExamParticipation } from '@/api/exam'
import type { ExamRecord, ExamParticipation } from '@/api/exam'
import { downloadAuthorizedFile } from '@/utils/download'
import { exportRowsToXlsx } from '@/utils/exportRows'

const router = useRouter()

const tableData = ref<ExamRecord[]>([])
const tableLoading = ref(false)
const pagination = reactive({ page: 1, size: 10, total: 0 })

const showDetail = ref(false)
const currentExam = ref<ExamRecord | null>(null)

const showParticipation = ref(false)
const participationExam = ref<ExamParticipation | null>(null)
const exportingId = ref<number | null>(null)

async function fetchList() {
  tableLoading.value = true
  try {
    const res = await getExamList({ page: pagination.page, size: pagination.size })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch {
    ElMessage.error('获取考试列表失败')
  } finally {
    tableLoading.value = false
  }
}

function goCreate() {
  router.push('/teacher/exams/create')
}

function goTemplates() {
  router.push('/teacher/templates')
}

function goEdit(row: ExamRecord) {
  router.push(`/teacher/exams/edit/${row.id}`)
}

function viewDetail(row: ExamRecord) {
  currentExam.value = row
  showDetail.value = true
}

async function handleExport(row: ExamRecord) {
  exportingId.value = row.id
  try {
    await downloadAuthorizedFile(`/api/exam/${row.id}/export`, `${row.title || '考试'}-答题成绩.xlsx`)
  } catch {
    ElMessage.error('下载答题成绩失败')
  } finally {
    exportingId.value = null
  }
}

function goGrade(row: ExamRecord) {
  router.push(`/teacher/exams/grade/${row.id}`)
}

async function viewParticipation(row: ExamRecord) {
  try {
    const res = await getExamParticipation(row.id)
    participationExam.value = res.data
    showParticipation.value = true
  } catch {
    ElMessage.error('获取参考情况失败')
  }
}

async function exportNotSubmittedStudents() {
  if (!participationExam.value?.notSubmittedStudents.length) return
  await exportRowsToXlsx(
    `${participationExam.value.examTitle}-未参加名单.xlsx`,
    [
      { label: '考试标题', prop: () => participationExam.value?.examTitle || '' },
      { label: '学生姓名', prop: 'studentName' },
      { label: '状态', prop: () => '未参加' }
    ],
    participationExam.value.notSubmittedStudents
  )
}

async function handleDelete(row: ExamRecord) {
  try {
    await ElMessageBox.confirm(`确认删除考试「${row.title}」？删除后学生端将不再显示该考试。`, '删除考试', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteExam(row.id)
    ElMessage.success('考试已删除')
    fetchList()
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除考试失败')
    }
  }
}

function formatTime(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    SCHEDULED: '待开始',
    PUBLISHED: '已发布',
    ENDED: '已结束'
  }
  return map[status] || status
}

function statusType(status: string) {
  const map: Record<string, string> = {
    DRAFT: 'info',
    SCHEDULED: 'warning',
    PUBLISHED: 'success',
    ENDED: 'warning'
  }
  return map[status] || ''
}

function participationStatusLabel(status: string) {
  const map: Record<string, string> = {
    UN: '答题中',
    SUBMITTED: '待批改',
    GRADED: '已批改',
    RETURNED: '已打回',
    NOT_SUBMITTED: '未参加'
  }
  return map[status] || status
}

function participationStatusType(status: string) {
  const map: Record<string, string> = {
    UN: 'info',
    SUBMITTED: 'warning',
    GRADED: 'success',
    RETURNED: 'danger',
    NOT_SUBMITTED: 'danger'
  }
  return map[status] || 'info'
}

onMounted(fetchList)
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600;700&display=swap');

.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--bg-surface) !important;
  min-height: 100vh;
  padding: 16px;
  font-family: 'JetBrains Mono', monospace !important;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.cyber-title {
  font-family: 'JetBrains Mono', monospace !important;
  font-size: 20px !important;
  font-weight: 700 !important;
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
  letter-spacing: 2px !important;
}

.cyber-card {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1), inset 0 0 60px rgba(0, 0, 0, 0.5) !important;
  clip-path: polygon(0 0, calc(100% - 20px) 0, 100% 20px, 100% 100%, 20px 100%, 0 calc(100% - 20px)) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1) 0%, transparent 100%) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 16px 20px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-card__body) {
  background: var(--bg-surface) !important;
  padding: 20px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card-inner {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.1) !important;
  clip-path: polygon(0 0, calc(100% - 15px) 0, 100% 15px, 100% 100%, 15px 100%, 0 calc(100% - 15px)) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card-inner :deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(255, 16, 240, 0.1) 0%, transparent 100%) !important;
  border-bottom: 1px solid var(--border) !important;
  color: #ff10f0 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card-inner :deep(.el-card__body) {
  background: var(--bg-surface) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table {
  background: var(--bg-surface) !important;
  font-family: 'JetBrains Mono', monospace !important;
  color: var(--text-primary) !important;
}

.cyber-table :deep(.el-table__header-wrapper th) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.15) 0%, rgba(255, 16, 240, 0.05) 100%) !important;
  color: #00ffff !important;
  border-bottom: 1px solid var(--border) !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
  text-transform: uppercase !important;
  font-size: 12px !important;
}

.cyber-table :deep(.el-table__body-wrapper tr) {
  background: var(--bg-surface) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table :deep(.el-table__body-wrapper td) {
  background: var(--bg-surface) !important;
  border-bottom: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-table :deep(.el-table__body-wrapper tr:hover > td) {
  background: rgba(0, 255, 255, 0.05) !important;
}

.cyber-table :deep(.el-table__body-wrapper .el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.3) !important;
}

.cyber-table :deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(26, 26, 46, 0.3) !important;
}

.cyber-table :deep(.el-table__empty-text) {
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-tag {
  font-family: 'JetBrains Mono', monospace !important;
  background: rgba(0, 0, 0, 0.5) !important;
  border: 1px solid currentColor !important;
  box-shadow: 0 0 8px currentColor !important;
}

.cyber-btn {
  font-family: 'JetBrains Mono', monospace !important;
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  border: none !important;
  color: #0a0a0a !important;
  font-weight: 700 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.4) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn:hover {
  box-shadow: 0 0 25px rgba(0, 255, 255, 0.6) !important;
  transform: translateY(-2px) !important;
}

.cyber-btn-outline {
  font-family: 'JetBrains Mono', monospace !important;
  background: transparent !important;
  border: 1px solid #ff10f0 !important;
  color: #ff10f0 !important;
  font-weight: 700 !important;
  clip-path: polygon(8px 0, 100% 0, 100% calc(100% - 8px), calc(100% - 8px) 100%, 0 100%, 0 8px) !important;
  box-shadow: 0 0 15px rgba(255, 16, 240, 0.4) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-outline:hover {
  background: rgba(255, 16, 240, 0.1) !important;
  box-shadow: 0 0 25px rgba(255, 16, 240, 0.6) !important;
  transform: translateY(-2px) !important;
}

.cyber-btn-text {
  font-family: 'JetBrains Mono', monospace !important;
  color: #ff10f0 !important;
  text-shadow: 0 0 5px rgba(255, 16, 240, 0.5) !important;
  transition: all 0.3s ease !important;
}

.cyber-btn-text:hover {
  color: #00ffff !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.8) !important;
}

.cyber-pagination {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pagination__total) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pager li) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-pagination :deep(.el-pager li:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

.cyber-pagination :deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  color: #0a0a0a !important;
  font-weight: 700 !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.5) !important;
}

.cyber-pagination :deep(.btn-prev), .cyber-pagination :deep(.btn-next) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  color: var(--text-primary) !important;
}

.cyber-pagination :deep(.btn-prev:hover), .cyber-pagination :deep(.btn-next:hover) {
  color: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.3) !important;
}

.cyber-dialog {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-dialog :deep(.el-dialog) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.2), 0 0 60px rgba(255, 16, 240, 0.1) !important;
  clip-path: polygon(0 0, calc(100% - 25px) 0, 100% 25px, 100% 100%, 25px 100%, 0 calc(100% - 25px)) !important;
}

.cyber-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, rgba(0, 255, 255, 0.1) 0%, rgba(255, 16, 240, 0.05) 100%) !important;
  border-bottom: 1px solid var(--border) !important;
  padding: 20px !important;
}

.cyber-dialog :deep(.el-dialog__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 700 !important;
  text-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
  letter-spacing: 2px !important;
}

.cyber-dialog :deep(.el-dialog__body) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  padding: 24px !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-dialog :deep(.el-dialog__footer) {
  background: var(--bg-surface) !important;
  border-top: 1px solid var(--border) !important;
  padding: 16px 20px !important;
}

.cyber-descriptions :deep(.el-descriptions__label) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
  border: 1px solid var(--border) !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
}

.cyber-descriptions :deep(.el-descriptions__content) {
  background: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  border: 1px solid var(--border) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.empty-tip {
  color: #39ff14 !important;
  font-family: 'JetBrains Mono', monospace !important;
  text-align: center;
  padding: 20px;
  text-shadow: 0 0 10px rgba(57, 255, 20, 0.5) !important;
}

.cyber-dialog :deep(.el-select .el-input__wrapper),
.cyber-dialog :deep(.el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) !important;
}

.cyber-dialog :deep(.el-select .el-input__wrapper:hover),
.cyber-dialog :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff !important;
}

.cyber-dialog :deep(.el-select .el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #00ffff, 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-dialog :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-dialog :deep(.el-input__inner::placeholder) {
  color: rgba(224, 224, 224, 0.4) !important;
}

.cyber-dialog :deep(.el-textarea__inner) {
  background-color: var(--bg-surface) !important;
  color: var(--text-primary) !important;
  border: 1px solid var(--border) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-dialog :deep(.el-textarea__inner:hover) {
  border-color: #00ffff !important;
}

.cyber-dialog :deep(.el-textarea__inner:focus) {
  border-color: #00ffff !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-dialog :deep(.el-button) {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-select .el-input__wrapper),
.cyber-card :deep(.el-input-number .el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) !important;
}

.cyber-card :deep(.el-select .el-input__wrapper:hover),
.cyber-card :deep(.el-input-number .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff !important;
}

.cyber-card :deep(.el-select .el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #00ffff, 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-card :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-input-number__decrease),
.cyber-card :deep(.el-input-number__increase) {
  background: var(--bg-surface) !important;
  color: #00ffff !important;
  border-color: var(--border-subtle) !important;
}

.cyber-card :deep(.el-input-number__decrease:hover),
.cyber-card :deep(.el-input-number__increase:hover) {
  color: #ff10f0 !important;
}

.cyber-card :deep(.el-select-dropdown) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
}

.cyber-card :deep(.el-select-dropdown__item) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-select-dropdown__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
}

.cyber-card :deep(.el-select-dropdown__item.is-selected) {
  background: rgba(255, 16, 240, 0.2) !important;
  color: #ff10f0 !important;
}

.cyber-card :deep(.el-date-editor .el-input__wrapper) {
  background-color: var(--bg-surface) !important;
  box-shadow: 0 0 0 1px var(--border) !important;
}

.cyber-card :deep(.el-date-editor .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #00ffff !important;
}

.cyber-card :deep(.el-picker-panel) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-date-picker__header-label) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-date-table th) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-date-table td.available:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
}

.cyber-card :deep(.el-date-table td.current:not(.disabled) .el-date-table-cell__text) {
  background: #00ffff !important;
  color: #0a0a0a !important;
}

.cyber-card :deep(.el-picker-panel__icon-btn) {
  color: #00ffff !important;
}

.cyber-card :deep(.el-time-panel) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-time-spinner__item) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-time-spinner__item:hover) {
  background: rgba(0, 255, 255, 0.1) !important;
  color: #00ffff !important;
}

.cyber-card :deep(.el-time-spinner__item.is-active) {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

.cyber-card :deep(.el-form-item__label) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
  letter-spacing: 1px !important;
}

.cyber-card :deep(.el-form-item) {
  --el-form-label-font-size: 14px !important;
}

.cyber-card :deep(.el-form-item__error) {
  color: #ff9800 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-dialog :deep(.el-form-item__label) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  font-weight: 600 !important;
}

.cyber-dialog :deep(.el-form-item__error) {
  color: #ff9800 !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-radio__label) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-radio__input.is-checked .el-radio__inner) {
  background: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-card :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #00ffff !important;
}

.cyber-card :deep(.el-checkbox__label) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #00ffff !important;
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-card :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #00ffff !important;
}

.cyber-card :deep(.el-switch.is-checked .el-switch__core) {
  background: linear-gradient(135deg, #00ffff 0%, #00cccc 100%) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-card :deep(.el-switch__label) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-switch__label.is-active) {
  color: #00ffff !important;
}

.cyber-card :deep(.el-slider__runway) {
  background: var(--bg-surface) !important;
}

.cyber-card :deep(.el-slider__bar) {
  background: linear-gradient(90deg, #00ffff 0%, #ff10f0 100%) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-card :deep(.el-slider__button) {
  border-color: #00ffff !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-card :deep(.el-rate__icon) {
  color: #1a1a2e !important;
  font-size: 20px !important;
}

.cyber-card :deep(.el-rate__icon.hover) {
  color: #ff10f0 !important;
}

.cyber-card :deep(.el-rate__icon.is-active) {
  color: #ff10f0 !important;
  text-shadow: 0 0 10px rgba(255, 16, 240, 0.5) !important;
}

.cyber-card :deep(.el-color-picker__trigger) {
  border-color: var(--border-subtle) !important;
  background: var(--bg-surface) !important;
}

.cyber-card :deep(.el-color-picker__color) {
  border-color: var(--border-subtle) !important;
}

.cyber-card :deep(.el-message-box) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 30px rgba(0, 255, 255, 0.2) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-message-box__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-message-box__content) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-overlay) {
  background: rgba(3, 3, 3, 0.85) !important;
}

.cyber-card :deep(.el-popover.el-popper) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
  box-shadow: 0 0 20px rgba(0, 255, 255, 0.1) !important;
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-popover.el-popper.is-light) {
  background: var(--bg-surface) !important;
  border: 1px solid var(--border) !important;
}

.cyber-card :deep(.el-popover__title) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-tooltip__popper.is-dark) {
  background: linear-gradient(135deg, var(--bg-surface) 0%, var(--bg-surface) 100%) !important;
  border: 1px solid #00ffff !important;
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
  box-shadow: 0 0 15px rgba(0, 255, 255, 0.3) !important;
}

.cyber-card :deep(.el-loading-mask) {
  background: rgba(3, 3, 3, 0.9) !important;
}

.cyber-card :deep(.el-loading-spinner .el-loading-text) {
  color: #00ffff !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-loading-spinner .path) {
  stroke: #00ffff !important;
}

.cyber-card :deep(.el-loading-spinner .el-loading-logo) {
  filter: drop-shadow(0 0 10px rgba(0, 255, 255, 0.5)) !important;
}

.cyber-card :deep(.el-progress__text) {
  color: var(--text-primary) !important;
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-progress-bar__outer) {
  background: var(--bg-surface) !important;
}

.cyber-card :deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, #00ffff 0%, #ff10f0 100%) !important;
  box-shadow: 0 0 10px rgba(0, 255, 255, 0.5) !important;
}

.cyber-card :deep(.el-progress-bar__inner::after) {
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent) !important;
}

.cyber-card :deep(.el-tag) {
  font-family: 'JetBrains Mono', monospace !important;
}

.cyber-card :deep(.el-tag--primary) {
  background: rgba(0, 255, 255, 0.1) !important;
  border-color: #00ffff !important;
  color: #00ffff !important;
  box-shadow: 0 0 8px rgba(0, 255, 255, 0.3) !important;
}

.cyber-card :deep(.el-tag--success) {
  background: rgba(57, 255, 20, 0.1) !important;
  border-color: #39ff14 !important;
  color: #39ff14 !important;
  box-shadow: 0 0 8px rgba(57, 255, 20, 0.3) !important;
}

.cyber-card :deep(.el-tag--warning) {
  background: rgba(255, 152, 0, 0.1) !important;
  border-color: #ff9800 !important;
  color: #ff9800 !important;
  box-shadow: 0 0 8px rgba(255, 152, 0, 0.3) !important;
}

.cyber-card :deep(.el-tag--danger) {
  background: rgba(255, 16, 240, 0.1) !important;
  border-color: #ff10f0 !important;
  color: #ff10f0 !important;
  box-shadow: 0 0 8px rgba(255, 16, 240, 0.3) !important;
}

.cyber-card :deep(.el-tag--info) {
  background: rgba(224, 224, 224, 0.1) !important;
  border-color: var(--text-primary) !important;
  color: var(--text-primary) !important;
  box-shadow: 0 0 8px rgba(224, 224, 224, 0.2) !important;
}
</style>
