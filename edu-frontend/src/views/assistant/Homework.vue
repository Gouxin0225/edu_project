<template>
  <div class="assistant-homework" v-loading="loading">
    <header class="page-header">
      <div>
        <h2>作业跟踪</h2>
        <p>按负责班级查看作业提交进度、未交名单和班主任批注。</p>
      </div>
      <el-button :icon="Refresh" @click="fetchHomeworkList">刷新</el-button>
    </header>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <el-icon><component :is="item.icon" /></el-icon>
        <div>
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <section class="workbench">
      <aside class="homework-list">
        <button
          v-for="item in homeworkList"
          :key="item.homeworkId"
          class="homework-item"
          :class="{ active: currentHomework?.homeworkId === item.homeworkId }"
          @click="selectHomework(item)"
        >
          <span class="item-title">{{ item.title }}</span>
          <span class="item-meta">截止 {{ formatTime(item.deadline) }}</span>
          <span class="item-stats">
            已交 {{ item.totalSubmissions || 0 }}
            <em>待处理 {{ item.pendingSubmissions || 0 }}</em>
          </span>
        </button>
        <el-empty v-if="!homeworkList.length" description="暂无可跟踪作业" :image-size="80" />
      </aside>

      <main class="detail-panel" v-if="currentHomework">
        <div class="detail-head">
          <div>
            <h3>{{ currentHomework.title }}</h3>
            <p>{{ currentHomework.content || '暂无作业说明' }}</p>
          </div>
          <el-tag type="warning" effect="plain">截止 {{ formatTime(currentHomework.deadline) }}</el-tag>
        </div>

        <div class="progress-block" v-if="progress">
          <div class="progress-top">
            <span>提交进度</span>
            <strong>{{ progress.submittedCount }}/{{ progress.totalStudents }}</strong>
          </div>
          <el-progress :percentage="completionRate" :stroke-width="10" />
          <div class="progress-actions">
            <span>未交 {{ progress.pendingCount }} 人</span>
            <el-button
              type="primary"
              size="small"
              :disabled="!progress.unsubmittedStudents.length"
              @click="remindAll"
            >
              一键催交
            </el-button>
          </div>
        </div>

        <div class="split-grid">
          <section class="panel">
            <div class="panel-header">
              <h4>未交学生</h4>
              <span>{{ progress?.unsubmittedStudents.length || 0 }} 人</span>
            </div>
            <el-table :data="progress?.unsubmittedStudents || []" size="small" max-height="300">
              <el-table-column prop="studentName" label="姓名" min-width="90" />
              <el-table-column prop="username" label="账号" min-width="120" show-overflow-tooltip />
              <el-table-column label="操作" width="90" align="right">
                <template #default="{ row }">
                  <el-button size="small" type="primary" text @click="remindOne(row.studentId)">催交</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>

          <section class="panel">
            <div class="panel-header">
              <h4>已提交学生</h4>
              <span>{{ submissions.length }} 条</span>
            </div>
            <el-table :data="submissions" size="small" max-height="300">
              <el-table-column prop="studentName" label="姓名" min-width="90" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="得分" width="70">
                <template #default="{ row }">{{ row.scoreGained ?? '-' }}</template>
              </el-table-column>
              <el-table-column label="批注" width="70">
                <template #default="{ row }">
                  <el-tag v-if="row.assistantComment" size="small" type="success">已批注</el-tag>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" type="primary" text @click="viewSubmission(row)">查看</el-button>
                  <el-button size="small" type="success" text @click="openComment(row)">批注</el-button>
                </template>
              </el-table-column>
            </el-table>
          </section>
        </div>
      </main>

      <main class="detail-panel empty-panel" v-else>
        <el-empty description="请选择左侧作业" />
      </main>
    </section>

    <el-dialog v-model="detailVisible" title="提交详情" width="720px">
      <div v-if="currentSubmission" class="submission-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学生">{{ currentSubmission.studentName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentSubmission.status)" size="small">
              {{ statusText(currentSubmission.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatTime(currentSubmission.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="得分">{{ currentSubmission.totalScoreGained ?? '-' }}</el-descriptions-item>
        </el-descriptions>

        <section class="content-section">
          <h4>提交内容</h4>
          <div class="text-box">{{ currentSubmission.content || '无文本内容' }}</div>
          <a v-if="currentSubmission.gitLink" :href="currentSubmission.gitLink" target="_blank">{{ currentSubmission.gitLink }}</a>
          <a v-if="currentSubmission.fileUrl" :href="currentSubmission.fileUrl" target="_blank">{{ currentSubmission.fileUrl }}</a>
        </section>

        <section class="content-section" v-if="currentSubmission.teacherComment">
          <h4>教师评语</h4>
          <div class="text-box">{{ currentSubmission.teacherComment }}</div>
        </section>

        <section class="content-section" v-if="currentSubmission.assistantComment">
          <h4>班主任批注</h4>
          <div class="text-box">{{ currentSubmission.assistantComment }}</div>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="commentVisible" title="添加批注" width="520px">
      <el-form :model="commentForm" :rules="commentRules" ref="commentFormRef" label-width="72px">
        <el-form-item label="学生">
          <span>{{ commentTarget?.studentName || '-' }}</span>
        </el-form-item>
        <el-form-item label="批注" prop="comment">
          <el-input
            v-model="commentForm.comment"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="记录跟进情况、家校沟通或学习建议"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="commentVisible = false">取消</el-button>
        <el-button type="primary" :loading="commenting" @click="submitComment">保存批注</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Finished, Refresh, Timer, User } from '@element-plus/icons-vue'
import {
  addAssistantSubmissionComment,
  getAssistantHomeworkList,
  getAssistantHomeworkProgress,
  getAssistantHomeworkSubmissionDetail,
  getAssistantHomeworkSubmissions,
  remindAssistantStudents,
  type HomeworkListItem,
  type HomeworkProgress,
  type HomeworkSubmissionDetail,
  type HomeworkSubmissionItem
} from '@/api/homework'

const loading = ref(false)
const detailLoading = ref(false)
const commenting = ref(false)
const homeworkList = ref<HomeworkListItem[]>([])
const currentHomework = ref<HomeworkListItem | null>(null)
const progress = ref<HomeworkProgress | null>(null)
const submissions = ref<HomeworkSubmissionItem[]>([])
const currentSubmission = ref<HomeworkSubmissionDetail | null>(null)
const commentTarget = ref<HomeworkSubmissionItem | null>(null)
const detailVisible = ref(false)
const commentVisible = ref(false)
const commentFormRef = ref()

const commentForm = reactive({ comment: '' })
const commentRules = {
  comment: [{ required: true, message: '请输入批注内容', trigger: 'blur' }]
}

const completionRate = computed(() => {
  if (!progress.value?.totalStudents) return 0
  return Math.round((progress.value.submittedCount / progress.value.totalStudents) * 1000) / 10
})

const summaryCards = computed(() => {
  const totalSubmitted = homeworkList.value.reduce((sum, item) => sum + Number(item.totalSubmissions || 0), 0)
  const totalPending = homeworkList.value.reduce((sum, item) => sum + Number(item.pendingSubmissions || 0), 0)
  return [
    { label: '跟踪作业', value: homeworkList.value.length, icon: Document },
    { label: '已提交', value: totalSubmitted, icon: Finished },
    { label: '待跟进', value: totalPending, icon: Timer },
    { label: '未交学生', value: progress.value?.pendingCount || 0, icon: User }
  ]
})

async function fetchHomeworkList() {
  loading.value = true
  try {
    const res = await getAssistantHomeworkList()
    homeworkList.value = res.data || []
    if (!currentHomework.value && homeworkList.value.length) {
      await selectHomework(homeworkList.value[0])
    } else if (currentHomework.value) {
      const latest = homeworkList.value.find(item => item.homeworkId === currentHomework.value?.homeworkId)
      currentHomework.value = latest || null
      if (latest) await loadHomeworkDetail(latest.homeworkId)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载作业列表失败')
  } finally {
    loading.value = false
  }
}

async function selectHomework(homework: HomeworkListItem) {
  currentHomework.value = homework
  await loadHomeworkDetail(homework.homeworkId)
}

async function loadHomeworkDetail(homeworkId: number) {
  detailLoading.value = true
  try {
    const [progressRes, submissionsRes] = await Promise.all([
      getAssistantHomeworkProgress(homeworkId),
      getAssistantHomeworkSubmissions(homeworkId)
    ])
    progress.value = progressRes.data
    submissions.value = submissionsRes.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载作业进度失败')
  } finally {
    detailLoading.value = false
  }
}

async function remindOne(studentId: number) {
  if (!currentHomework.value) return
  await remindAssistantStudents(currentHomework.value.homeworkId, [studentId])
  ElMessage.success('已记录催交')
}

async function remindAll() {
  if (!currentHomework.value || !progress.value?.unsubmittedStudents.length) return
  await ElMessageBox.confirm(`确认催交 ${progress.value.unsubmittedStudents.length} 名未交学生？`, '一键催交', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await remindAssistantStudents(
    currentHomework.value.homeworkId,
    progress.value.unsubmittedStudents.map(item => item.studentId)
  )
  ElMessage.success('已记录催交')
}

async function viewSubmission(row: HomeworkSubmissionItem) {
  if (!currentHomework.value) return
  const res = await getAssistantHomeworkSubmissionDetail(currentHomework.value.homeworkId, row.submissionId)
  currentSubmission.value = res.data
  detailVisible.value = true
}

function openComment(row: HomeworkSubmissionItem) {
  commentTarget.value = row
  commentForm.comment = ''
  commentVisible.value = true
}

async function submitComment() {
  const valid = await commentFormRef.value?.validate().catch(() => false)
  if (!valid || !commentTarget.value || !currentHomework.value) return
  commenting.value = true
  try {
    await addAssistantSubmissionComment(commentTarget.value.submissionId, commentForm.comment)
    ElMessage.success('批注已保存')
    commentVisible.value = false
    await loadHomeworkDetail(currentHomework.value.homeworkId)
  } catch (error: any) {
    ElMessage.error(error?.message || '保存批注失败')
  } finally {
    commenting.value = false
  }
}

function statusType(status: string) {
  const map: Record<string, string> = { SUBMITTED: 'warning', GRADED: 'success', RETURNED: 'danger' }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { SUBMITTED: '待批改', GRADED: '已批改', RETURNED: '已打回' }
  return map[status] || status
}

function formatTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(fetchHomeworkList)
</script>

<style scoped>
.assistant-homework {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
}

.page-header,
.detail-head,
.progress-top,
.progress-actions,
.panel-header,
.item-stats {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-header h2,
.detail-head h3,
.panel-header h4 {
  margin: 0;
  color: #e2e8f0;
  letter-spacing: 0;
}

.page-header p,
.detail-head p {
  margin: 6px 0 0;
  color: rgba(226, 232, 240, 0.58);
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 12px;
}

.summary-card,
.homework-list,
.detail-panel,
.panel {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(12, 20, 40, 0.74);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(14px);
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 82px;
  padding: 14px;
}

.summary-card .el-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: rgba(64, 128, 255, 0.12);
  color: #8bb5ff;
}

.summary-card strong {
  display: block;
  color: #f8fafc;
  font-family: 'JetBrains Mono', monospace;
  font-size: 24px;
}

.summary-card span,
.panel-header span {
  color: rgba(226, 232, 240, 0.54);
  font-size: 12px;
}

.workbench {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
  min-height: 560px;
}

.homework-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
}

.homework-item {
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  color: inherit;
  cursor: pointer;
  padding: 12px;
  text-align: left;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.homework-item:hover,
.homework-item.active {
  border-color: rgba(64, 128, 255, 0.38);
  background: rgba(64, 128, 255, 0.1);
}

.item-title {
  display: block;
  overflow: hidden;
  color: #e2e8f0;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta,
.item-stats {
  margin-top: 8px;
  color: rgba(226, 232, 240, 0.52);
  font-size: 12px;
}

.item-stats em {
  color: #fbbf24;
  font-style: normal;
}

.detail-panel {
  min-width: 0;
  padding: 16px;
}

.empty-panel {
  display: grid;
  place-items: center;
}

.progress-block {
  margin-top: 16px;
  padding: 14px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.progress-top strong {
  color: #bfdbfe;
  font-family: 'JetBrains Mono', monospace;
}

.progress-actions {
  margin-top: 12px;
  color: rgba(226, 232, 240, 0.62);
  font-size: 13px;
}

.split-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.85fr) minmax(0, 1.15fr);
  gap: 16px;
  margin-top: 16px;
}

.panel {
  min-width: 0;
  padding: 14px;
}

.content-section {
  margin-top: 18px;
}

.content-section h4 {
  margin: 0 0 10px;
  color: #e2e8f0;
}

.text-box {
  white-space: pre-wrap;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(226, 232, 240, 0.82);
  padding: 12px;
}

.content-section a {
  display: block;
  margin-top: 8px;
  color: #8bb5ff;
}

@media (max-width: 1180px) {
  .workbench,
  .split-grid {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
