<template>
  <div class="assistant-operation" v-loading="loading">
    <header class="page-header">
      <div>
        <h2>班级看板</h2>
        <p>按班级查看学生、活跃、风险和任务完成情况，并下钻到学员明细。</p>
      </div>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </header>

    <section class="board-grid">
      <button
        v-for="item in boards"
        :key="item.classId"
        class="class-card"
        :class="{ active: currentClass?.classId === item.classId }"
        @click="currentClass = item"
      >
        <div class="card-head">
          <strong>{{ item.className }}</strong>
          <el-tag :type="item.riskCount ? 'danger' : 'success'" effect="plain">{{ item.riskCount }} 风险</el-tag>
        </div>
        <span>{{ item.schoolName || '未设置学校' }}</span>
        <div class="metric-row">
          <div><b>{{ item.studentCount }}</b><em>学生</em></div>
          <div><b>{{ item.activeCount }}</b><em>活跃</em></div>
          <div><b>{{ formatRate(item.homeworkCompletionRate) }}</b><em>作业</em></div>
          <div><b>{{ formatRate(item.examCompletionRate) }}</b><em>考试</em></div>
          <div><b>{{ formatRate(item.surveyCompletionRate) }}</b><em>问卷</em></div>
        </div>
      </button>
    </section>

    <section class="panel" v-if="currentClass">
      <div class="panel-header">
        <div>
          <h3>{{ currentClass.className }} 明细</h3>
          <span>{{ currentClass.studentCount }} 名学生，{{ currentClass.activeCount }} 名近 7 天有学习活动</span>
        </div>
        <el-button type="primary" text @click="router.push('/assistant/profiles')">查看完整画像</el-button>
      </div>
      <el-table :data="currentClass.students" stripe>
        <el-table-column label="学员" min-width="160">
          <template #default="{ row }">
            <div class="student-cell">
              <strong>{{ row.studentName }}</strong>
              <span>{{ row.phone || row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="schoolName" label="学校" min-width="130" show-overflow-tooltip />
        <el-table-column label="入班时间" width="150">
          <template #default="{ row }">{{ formatTime(row.joinTime) }}</template>
        </el-table-column>
        <el-table-column label="最近学习活动" width="150">
          <template #default="{ row }">{{ formatTime(row.lastActivityTime) }}</template>
        </el-table-column>
        <el-table-column label="风险" width="90">
          <template #default="{ row }">
            <el-tag :type="row.riskScore ? 'danger' : 'success'" effect="plain">{{ row.riskScore }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下次跟进" width="150">
          <template #default="{ row }">{{ formatTime(row.nextFollowTime) }}</template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Refresh } from '@element-plus/icons-vue'
import { getAssistantClassBoards, type AssistantClassBoard } from '@/api/assistantOperations'

const router = useRouter()
const loading = ref(false)
const boards = ref<AssistantClassBoard[]>([])
const selectedClassId = ref<number | null>(null)

const currentClass = computed({
  get: () => boards.value.find(item => item.classId === selectedClassId.value) || boards.value[0] || null,
  set: value => { selectedClassId.value = value?.classId || null }
})

async function loadData() {
  loading.value = true
  try {
    const res = await getAssistantClassBoards()
    boards.value = res.data || []
    if (!selectedClassId.value && boards.value.length) selectedClassId.value = boards.value[0].classId
  } catch (error: any) {
    ElMessage.error(error?.message || '加载班级看板失败')
  } finally {
    loading.value = false
  }
}

function formatRate(value?: number) {
  return `${Number(value || 0).toFixed(1)}%`
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(loadData)
</script>

<style scoped>
.assistant-operation {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
}

.page-header,
.panel-header,
.card-head,
.metric-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-header h2,
.panel-header h3 {
  margin: 0;
  color: var(--text-primary);
}

.page-header p,
.panel-header span,
.class-card > span,
.student-cell span {
  color: var(--text-secondary);
  font-size: 13px;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.class-card,
.panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-card);
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
}

.class-card {
  cursor: pointer;
  padding: 14px;
  text-align: left;
}

.class-card.active,
.class-card:hover {
  border-color: var(--primary-border);
  background: var(--surface-hover);
}

.card-head strong,
.student-cell strong {
  color: var(--text-primary);
}

.metric-row {
  margin-top: 14px;
}

.metric-row div {
  display: grid;
  gap: 3px;
}

.metric-row b {
  color: var(--text-primary);
  font-family: 'JetBrains Mono', monospace;
}

.metric-row em {
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
}

.panel {
  padding: 16px;
}

.panel-header {
  margin-bottom: 12px;
}

.student-cell {
  display: grid;
  gap: 4px;
}
</style>
