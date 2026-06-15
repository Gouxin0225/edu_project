<template>
  <div class="assistant-risk" v-loading="loading">
    <header class="page-header">
      <div>
        <h2>风险预警中心</h2>
        <p>自动归类缺交、低分、切屏、无学习活动和回访逾期，并展示具体来源。</p>
      </div>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </header>

    <section class="toolbar">
      <el-select v-model="classId" placeholder="全部班级" clearable @change="loadData">
        <el-option v-for="item in classOptions" :key="item.classId" :label="item.className" :value="item.classId" />
      </el-select>
      <el-select v-model="typeFilter" placeholder="风险类型">
        <el-option label="全部风险" value="ALL" />
        <el-option label="多次缺交/未完成" value="MISSING_TASK" />
        <el-option label="低分" value="LOW_SCORE" />
        <el-option label="切屏异常" value="SWITCH_SCREEN" />
        <el-option label="长时间无学习活动" value="INACTIVE" />
        <el-option label="问卷未填" value="SURVEY_MISSING" />
        <el-option label="回访逾期" value="VISIT_OVERDUE" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="处理状态">
        <el-option label="全部状态" value="ALL" />
        <el-option label="待处理" value="PENDING" />
        <el-option label="已联系" value="CONTACTED" />
        <el-option label="持续跟进" value="FOLLOWING" />
        <el-option label="已解决" value="RESOLVED" />
      </el-select>
      <el-button :icon="Download" :disabled="!filteredRisks.length" @click="exportRiskStudents">
        导出风险名单
      </el-button>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <strong>{{ item.value }}</strong>
        <span>{{ item.label }}</span>
      </div>
    </section>

    <section class="panel">
      <el-table :data="filteredRisks" stripe>
        <el-table-column label="风险" width="130">
          <template #default="{ row }">
            <el-tag :type="row.level === 'HIGH' ? 'danger' : 'warning'" effect="plain">{{ row.title }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学员" min-width="140">
          <template #default="{ row }">
            <strong>{{ row.studentName }}</strong>
            <span class="muted"> · {{ row.className }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="风险原因" min-width="260" show-overflow-tooltip />
        <el-table-column label="来源" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ sourceText(row) }}</template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ formatTime(row.sourceTime) }}</template>
        </el-table-column>
        <el-table-column label="处理状态" width="150">
          <template #default="{ row }">
            <el-select :model-value="row.status" size="small" @change="value => setStatus(row.id, value)">
              <el-option label="待处理" value="PENDING" />
              <el-option label="已联系" value="CONTACTED" />
              <el-option label="持续跟进" value="FOLLOWING" />
              <el-option label="已解决" value="RESOLVED" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Download, Refresh } from '@element-plus/icons-vue'
import {
  getAssistantClassBoards,
  getAssistantRisks,
  type AssistantClassBoard,
  type AssistantRiskItem
} from '@/api/assistantOperations'
import { exportRowsToXlsx } from '@/utils/exportRows'

const STORAGE_KEY = 'assistant-risk-status'

const loading = ref(false)
const classId = ref<number | null>(null)
const typeFilter = ref('ALL')
const statusFilter = ref('ALL')
const classOptions = ref<AssistantClassBoard[]>([])
const risks = ref<AssistantRiskItem[]>([])
const statusMap = ref<Record<string, AssistantRiskItem['status']>>(JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}'))

const filteredRisks = computed(() => risks.value
  .map(item => ({ ...item, status: statusMap.value[item.id] || item.status || 'PENDING' }))
  .filter(item => typeFilter.value === 'ALL' || item.type === typeFilter.value)
  .filter(item => statusFilter.value === 'ALL' || item.status === statusFilter.value))

const summaryCards = computed(() => [
  { label: '全部风险', value: filteredRisks.value.length },
  { label: '高风险', value: filteredRisks.value.filter(item => item.level === 'HIGH').length },
  { label: '待处理', value: filteredRisks.value.filter(item => item.status === 'PENDING').length },
  { label: '已解决', value: filteredRisks.value.filter(item => item.status === 'RESOLVED').length }
])

async function loadData() {
  loading.value = true
  try {
    const [classRes, riskRes] = await Promise.all([
      getAssistantClassBoards(),
      getAssistantRisks(classId.value || undefined)
    ])
    classOptions.value = classRes.data || []
    risks.value = riskRes.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载风险预警失败')
  } finally {
    loading.value = false
  }
}

function setStatus(id: string, status: AssistantRiskItem['status']) {
  statusMap.value = { ...statusMap.value, [id]: status }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(statusMap.value))
}

async function exportRiskStudents() {
  await exportRowsToXlsx(
    '风险学生名单.xlsx',
    [
      { label: '学生姓名', prop: 'studentName' },
      { label: '班级', prop: 'className' },
      { label: '风险类型', prop: row => riskTypeText(row.type) },
      { label: '风险等级', prop: row => levelText(row.level) },
      { label: '风险原因', prop: 'reason' },
      { label: '来源', prop: sourceText },
      { label: '发生时间', prop: row => formatTime(row.sourceTime) },
      { label: '处理状态', prop: row => statusText(row.status) }
    ],
    filteredRisks.value
  )
}

function riskTypeText(type: string) {
  return {
    MISSING_TASK: '多次缺交/未完成',
    LOW_SCORE: '低分',
    SWITCH_SCREEN: '切屏异常',
    INACTIVE: '长时间无学习活动',
    SURVEY_MISSING: '问卷未填',
    VISIT_OVERDUE: '回访逾期'
  }[type] || type
}

function levelText(level: string) {
  return { HIGH: '高', MEDIUM: '中', LOW: '低' }[level] || level
}

function statusText(status: string) {
  return { PENDING: '待处理', CONTACTED: '已联系', FOLLOWING: '持续跟进', RESOLVED: '已解决' }[status] || status
}

function sourceText(row: AssistantRiskItem) {
  return `${row.sourceType}${row.sourceTitle ? ` · ${row.sourceTitle}` : ''}`
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(loadData)
</script>

<style scoped>
.assistant-risk {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
}

.page-header,
.toolbar,
.summary-grid {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  color: var(--text-primary);
}

.page-header p,
.summary-card span,
.muted {
  color: var(--text-secondary);
  font-size: 13px;
}

.toolbar {
  justify-content: flex-start;
}

.toolbar .el-select {
  width: 220px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-card,
.panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-card);
  box-shadow: var(--shadow-sm);
  padding: 14px;
}

.summary-card strong {
  display: block;
  color: var(--text-primary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 24px;
}

@media (max-width: 960px) {
  .toolbar,
  .summary-grid {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .toolbar,
  .summary-grid {
    display: grid;
  }

  .toolbar .el-select {
    width: 100%;
  }
}
</style>
