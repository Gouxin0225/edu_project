<template>
  <div class="student-follow" v-loading="loading">
    <header class="page-header">
      <div>
        <h2>学员跟进</h2>
        <p>按班级查看学员风险、最近回访和下一步跟进状态。</p>
      </div>
      <div class="header-actions">
        <el-select v-model="selectedClassId" placeholder="全部班级" clearable @change="loadData">
          <el-option v-for="item in classList" :key="item.id" :label="item.className" :value="item.id" />
        </el-select>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </header>

    <section class="filter-panel">
      <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索姓名或账号" />
      <el-select v-model="riskFilter" placeholder="风险等级">
        <el-option label="全部" value="ALL" />
        <el-option label="高风险" value="HIGH" />
        <el-option label="中风险" value="MEDIUM" />
        <el-option label="正常" value="NORMAL" />
      </el-select>
      <el-select v-model="visitFilter" placeholder="回访状态">
        <el-option label="全部" value="ALL" />
        <el-option label="未回访" value="NONE" />
        <el-option label="需跟进" value="NEED_FOLLOW" />
        <el-option label="已联系" value="REACHED" />
        <el-option label="已解决" value="RESOLVED" />
      </el-select>
      <el-select v-model="followFilter" placeholder="跟进计划">
        <el-option label="全部" value="ALL" />
        <el-option label="今日到期" value="DUE_TODAY" />
        <el-option label="已逾期" value="OVERDUE" />
        <el-option label="未安排" value="NO_PLAN" />
      </el-select>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <strong>{{ item.value }}</strong>
        <span>{{ item.label }}</span>
      </div>
    </section>

    <section class="table-panel">
      <el-table :data="filteredStudents" stripe>
        <el-table-column label="学员" min-width="150">
          <template #default="{ row }">
            <div class="student-cell">
              <strong>{{ row.realName }}</strong>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" min-width="140" show-overflow-tooltip />
        <el-table-column label="风险" width="110">
          <template #default="{ row }">
            <el-tag :type="riskTagType(row.riskLevel)" effect="plain">{{ riskLabel(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险原因" min-width="220">
          <template #default="{ row }">
            <div class="reason-list">
              <el-tag v-if="row.risk?.missingCount" size="small" type="warning" effect="plain">缺交 {{ row.risk.missingCount }}</el-tag>
              <el-tag v-if="row.risk?.lowScoreCount" size="small" type="danger" effect="plain">低分 {{ row.risk.lowScoreCount }}</el-tag>
              <el-tag v-if="row.risk?.switchScreenCount" size="small" type="info" effect="plain">切屏 {{ row.risk.switchScreenCount }}</el-tag>
              <span v-if="!row.risk">暂无异常</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="最近回访" min-width="220">
          <template #default="{ row }">
            <div v-if="row.lastVisit" class="visit-cell">
              <strong>{{ resultLabel(row.lastVisit.visitResult) }}</strong>
              <span>{{ formatTime(row.lastVisit.visitTime) }} · {{ row.lastVisit.visitorName }}</span>
            </div>
            <span v-else class="muted">未回访</span>
          </template>
        </el-table-column>
        <el-table-column label="下次跟进" min-width="150">
          <template #default="{ row }">
            <el-tag v-if="row.lastVisit?.nextFollowTime" :type="followTagType(row.lastVisit)" effect="plain">
              {{ followLabel(row.lastVisit) }}
            </el-tag>
            <span v-else class="muted">未安排</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" text @click="openVisit(row)">新增回访</el-button>
            <el-button text @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="detailVisible" size="560px" :title="detailStudent?.realName || '学员详情'">
      <div v-if="detailStudent" class="detail-content">
        <section class="detail-card">
          <h3>{{ detailStudent.realName }}</h3>
          <p>{{ detailStudent.username }} · {{ detailStudent.className }}</p>
          <el-tag :type="riskTagType(detailStudent.riskLevel)" effect="plain">{{ riskLabel(detailStudent.riskLevel) }}</el-tag>
        </section>
        <section class="detail-card">
          <h4>风险指标</h4>
          <div class="metric-row">
            <span>缺交</span><strong>{{ detailStudent.risk?.missingCount || 0 }}</strong>
            <span>低分</span><strong>{{ detailStudent.risk?.lowScoreCount || 0 }}</strong>
            <span>切屏</span><strong>{{ detailStudent.risk?.switchScreenCount || 0 }}</strong>
          </div>
        </section>
        <section class="detail-card">
          <h4>班级历史</h4>
          <el-table :data="detailStudent.classHistory" size="small" border stripe>
            <el-table-column prop="className" label="班级名称" min-width="120" />
            <el-table-column label="课程状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === '1' ? 'success' : 'info'" size="small">
                  {{ row.status === '1' ? '进行中' : '已结束' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="入班状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.joinStatus === 'ACTIVE' ? 'primary' : 'warning'" size="small">
                  {{ row.joinStatus === 'ACTIVE' ? '学习中' : '已退出' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="加入时间" width="140">
              <template #default="{ row }">
                {{ formatTime(row.joinTime) }}
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!detailStudent.classHistory?.length" description="暂无班级记录" :image-size="60" />
        </section>
        <section class="detail-card">
          <h4>回访记录</h4>
          <div v-for="visit in detailStudent.visits" :key="visit.id" class="detail-visit">
            <div>
              <strong>{{ resultLabel(visit.visitResult) }}</strong>
              <span>{{ formatTime(visit.visitTime) }} · {{ visit.visitorName }}</span>
            </div>
            <el-tag v-if="visit.nextFollowTime" :type="followTagType(visit)" effect="plain" size="small">
              下次跟进 {{ formatTime(visit.nextFollowTime) }}
            </el-tag>
            <p>{{ visit.content }}</p>
          </div>
          <el-empty v-if="!detailStudent.visits.length" description="暂无回访记录" :image-size="80" />
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="visitDialogVisible" title="新增回访" width="560px">
      <el-form ref="visitFormRef" :model="visitForm" :rules="visitRules" label-width="96px">
        <el-form-item label="学员">
          <span>{{ visitTarget?.realName }}（{{ visitTarget?.username }}）</span>
        </el-form-item>
        <el-form-item label="回访方式" prop="visitMethod">
          <el-select v-model="visitForm.visitMethod" style="width: 100%">
            <el-option label="电话" value="PHONE" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="线下" value="OFFLINE" />
            <el-option label="线上会议" value="ONLINE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="回访结果" prop="visitResult">
          <el-select v-model="visitForm.visitResult" style="width: 100%">
            <el-option label="已联系" value="REACHED" />
            <el-option label="未联系上" value="UNREACHED" />
            <el-option label="需继续跟进" value="NEED_FOLLOW" />
            <el-option label="已解决" value="RESOLVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker v-model="visitForm.nextFollowTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="可选" style="width: 100%" />
        </el-form-item>
        <el-form-item label="回访内容" prop="content">
          <el-input v-model="visitForm.content" type="textarea" :rows="5" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visitDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingVisit" @click="submitVisit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { getMyClasses, getStudentList, getTeacherStatistics, type StudentRecord, type TeacherClass, type TeacherRiskStudent } from '@/api/teacher'
import { createStudentVisitRecord, getStudentVisitRecords, type StudentVisitRecord, type VisitMethod, type VisitResult } from '@/api/studentVisit'

type RiskLevel = 'HIGH' | 'MEDIUM' | 'NORMAL'
type RiskFilter = 'ALL' | RiskLevel
type VisitFilter = 'ALL' | 'NONE' | VisitResult
type FollowFilter = 'ALL' | 'DUE_TODAY' | 'OVERDUE' | 'NO_PLAN'

interface FollowStudent extends StudentRecord {
  className: string
  risk?: TeacherRiskStudent
  riskLevel: RiskLevel
  visits: StudentVisitRecord[]
  lastVisit?: StudentVisitRecord
}

const loading = ref(false)
const savingVisit = ref(false)
const keyword = ref('')
const selectedClassId = ref<number | null>(null)
const riskFilter = ref<RiskFilter>('ALL')
const visitFilter = ref<VisitFilter>('ALL')
const followFilter = ref<FollowFilter>('ALL')
const classList = ref<TeacherClass[]>([])
const students = ref<StudentRecord[]>([])
const risks = ref<TeacherRiskStudent[]>([])
const visits = ref<StudentVisitRecord[]>([])
const detailVisible = ref(false)
const detailStudent = ref<FollowStudent | null>(null)
const visitDialogVisible = ref(false)
const visitTarget = ref<FollowStudent | null>(null)
const visitFormRef = ref<FormInstance>()

const visitForm = reactive({
  visitMethod: 'PHONE' as VisitMethod,
  visitResult: 'REACHED' as VisitResult,
  content: '',
  nextFollowTime: null as string | null
})

const visitRules: FormRules = {
  visitMethod: [{ required: true, message: '请选择回访方式', trigger: 'change' }],
  visitResult: [{ required: true, message: '请选择回访结果', trigger: 'change' }],
  content: [{ required: true, message: '请输入回访内容', trigger: 'blur' }]
}

const followStudents = computed<FollowStudent[]>(() => {
  const classNameMap = new Map(classList.value.map(item => [item.id, item.className]))
  const riskMap = new Map(risks.value.map(item => [item.studentId, item]))
  const visitsByStudent = new Map<number, StudentVisitRecord[]>()
  for (const visit of visits.value) {
    const list = visitsByStudent.get(visit.studentId) || []
    list.push(visit)
    visitsByStudent.set(visit.studentId, list)
  }
  return students.value.map(student => {
    const risk = riskMap.get(student.id)
    const studentVisits = (visitsByStudent.get(student.id) || []).sort((a, b) => String(b.visitTime).localeCompare(String(a.visitTime)))
    return {
      ...student,
      className: student.className || classNameMap.get(student.classId) || '-',
      risk,
      riskLevel: calcRiskLevel(risk),
      visits: studentVisits,
      lastVisit: studentVisits[0]
    }
  })
})

const filteredStudents = computed(() => followStudents.value.filter(item => {
  const query = keyword.value.trim().toLowerCase()
  if (query && !item.realName.toLowerCase().includes(query) && !item.username.toLowerCase().includes(query)) return false
  if (riskFilter.value !== 'ALL' && item.riskLevel !== riskFilter.value) return false
  if (visitFilter.value === 'NONE' && item.lastVisit) return false
  if (visitFilter.value !== 'ALL' && visitFilter.value !== 'NONE' && item.lastVisit?.visitResult !== visitFilter.value) return false
  if (followFilter.value === 'NO_PLAN' && item.lastVisit?.nextFollowTime) return false
  if (followFilter.value === 'DUE_TODAY' && !isToday(item.lastVisit?.nextFollowTime)) return false
  if (followFilter.value === 'OVERDUE' && !isOverdue(item.lastVisit)) return false
  return true
}))

const summaryCards = computed(() => [
  { label: '学员总数', value: followStudents.value.length },
  { label: '高风险', value: followStudents.value.filter(item => item.riskLevel === 'HIGH').length },
  { label: '中风险', value: followStudents.value.filter(item => item.riskLevel === 'MEDIUM').length },
  { label: '未回访', value: followStudents.value.filter(item => !item.lastVisit).length },
  { label: '需跟进', value: followStudents.value.filter(item => item.lastVisit?.visitResult === 'NEED_FOLLOW').length },
  { label: '今日到期', value: followStudents.value.filter(item => isToday(item.lastVisit?.nextFollowTime)).length },
  { label: '已逾期', value: followStudents.value.filter(item => isOverdue(item.lastVisit)).length }
])

async function loadData() {
  loading.value = true
  try {
    if (!classList.value.length) {
      const classRes = await getMyClasses()
      classList.value = classRes.data || []
    }
    const [studentRes, statRes, visitRes] = await Promise.all([
      getStudentList(selectedClassId.value || undefined),
      getTeacherStatistics(selectedClassId.value || undefined),
      getStudentVisitRecords({ classId: selectedClassId.value || undefined })
    ])
    students.value = studentRes.data || []
    risks.value = statRes.data.riskStudents || []
    visits.value = visitRes.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载学员跟进失败')
  } finally {
    loading.value = false
  }
}

function calcRiskLevel(risk?: TeacherRiskStudent): RiskLevel {
  if (!risk) return 'NORMAL'
  if (risk.riskScore >= 8 || risk.missingCount >= 2 || risk.lowScoreCount >= 2) return 'HIGH'
  if (risk.riskScore > 0) return 'MEDIUM'
  return 'NORMAL'
}

function riskLabel(level: RiskLevel) {
  return { HIGH: '高风险', MEDIUM: '中风险', NORMAL: '正常' }[level]
}

function riskTagType(level: RiskLevel) {
  return { HIGH: 'danger', MEDIUM: 'warning', NORMAL: 'success' }[level]
}

function resultLabel(value: VisitResult) {
  return { REACHED: '已联系', UNREACHED: '未联系上', NEED_FOLLOW: '需跟进', RESOLVED: '已解决' }[value] || value
}

function followLabel(visit: StudentVisitRecord) {
  if (isOverdue(visit)) return `已逾期 ${formatTime(visit.nextFollowTime)}`
  if (isToday(visit.nextFollowTime)) return `今日 ${formatTime(visit.nextFollowTime).slice(11)}`
  return formatTime(visit.nextFollowTime)
}

function followTagType(visit: StudentVisitRecord) {
  if (isOverdue(visit)) return 'danger'
  if (isToday(visit.nextFollowTime)) return 'warning'
  return 'info'
}

function isOverdue(visit?: StudentVisitRecord) {
  if (!visit?.nextFollowTime || visit.visitResult === 'RESOLVED') return false
  return new Date(visit.nextFollowTime).getTime() < Date.now() && !isToday(visit.nextFollowTime)
}

function isToday(value?: string | null) {
  if (!value) return false
  const date = new Date(value)
  const now = new Date()
  return date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth() && date.getDate() === now.getDate()
}

function openDetail(row: FollowStudent) {
  detailStudent.value = row
  detailVisible.value = true
}

function openVisit(row: FollowStudent) {
  visitTarget.value = row
  visitForm.visitMethod = 'PHONE'
  visitForm.visitResult = 'REACHED'
  visitForm.content = ''
  visitForm.nextFollowTime = null
  visitDialogVisible.value = true
}

async function submitVisit() {
  const valid = await visitFormRef.value?.validate().catch(() => false)
  if (!valid || !visitTarget.value) return
  savingVisit.value = true
  try {
    await createStudentVisitRecord({
      studentId: visitTarget.value.id,
      visitMethod: visitForm.visitMethod,
      visitResult: visitForm.visitResult,
      content: visitForm.content,
      nextFollowTime: visitForm.nextFollowTime
    })
    ElMessage.success('回访记录已保存')
    visitDialogVisible.value = false
    await loadData()
  } catch (error: any) {
    ElMessage.error(error?.message || '保存回访记录失败')
  } finally {
    savingVisit.value = false
  }
}

function formatTime(value?: string | null) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(loadData)
</script>

<style scoped>
.student-follow {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
}

.page-header,
.header-actions,
.filter-panel,
.summary-grid,
.reason-list,
.metric-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header {
  justify-content: space-between;
  align-items: flex-start;
}

.page-header h2 {
  margin: 0;
  color: #e2e8f0;
  font-size: 24px;
  letter-spacing: 0;
}

.page-header p {
  margin: 6px 0 0;
  color: rgba(226, 232, 240, 0.6);
  font-size: 13px;
}

.header-actions .el-select,
.filter-panel .el-input,
.filter-panel .el-select {
  width: 220px;
}

.filter-panel,
.summary-card,
.table-panel,
.detail-card {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(12, 20, 40, 0.74);
  box-shadow: 0 10px 32px rgba(0, 0, 0, 0.28);
}

.filter-panel,
.table-panel {
  padding: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
}

.summary-card {
  padding: 14px;
}

.summary-card strong {
  display: block;
  color: #f8fafc;
  font-family: 'JetBrains Mono', monospace;
  font-size: 24px;
}

.summary-card span,
.student-cell span,
.visit-cell span,
.muted {
  color: rgba(226, 232, 240, 0.54);
  font-size: 12px;
}

.student-cell,
.visit-cell {
  display: grid;
  gap: 4px;
}

.student-cell strong,
.visit-cell strong {
  color: #f8fafc;
}

.reason-list {
  flex-wrap: wrap;
}

.detail-content {
  display: grid;
  gap: 12px;
}

.detail-card {
  padding: 14px;
}

.detail-card h3,
.detail-card h4 {
  margin: 0 0 10px;
  color: #e2e8f0;
}

.detail-card p,
.detail-visit span {
  color: rgba(226, 232, 240, 0.62);
}

.metric-row {
  flex-wrap: wrap;
}

.metric-row strong {
  color: #fbbf24;
  margin-right: 12px;
}

.detail-visit {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding: 10px 0;
  display: grid;
  gap: 8px;
}

.student-follow :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(15, 23, 42, 0.92);
  --el-table-header-text-color: #cbd5e1;
  --el-table-text-color: #e5e7eb;
  --el-table-border-color: rgba(148, 163, 184, 0.14);
  --el-table-row-hover-bg-color: rgba(64, 128, 255, 0.08);
}

.student-follow :deep(.el-table th.el-table__cell),
.student-follow :deep(.el-table td.el-table__cell) {
  background: transparent;
}
</style>
