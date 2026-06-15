<template>
  <div class="assistant-todos" v-loading="loading">
    <header class="page-header">
      <div>
        <h2>待办任务中心</h2>
        <p>集中处理催交、回访、考试未参加、问卷未填和高风险学员事项。</p>
      </div>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </header>

    <section class="toolbar">
      <el-select v-model="classId" placeholder="全部班级" clearable @change="loadData">
        <el-option v-for="item in classOptions" :key="item.classId" :label="item.className" :value="item.classId" />
      </el-select>
      <el-select v-model="typeFilter" placeholder="待办类型">
        <el-option label="全部待办" value="ALL" />
        <el-option label="今日待催交" value="HOMEWORK_MISSING" />
        <el-option label="今日待回访" value="VISIT_TODAY" />
        <el-option label="考试未参加" value="EXAM_MISSING" />
        <el-option label="问卷未填写" value="SURVEY_MISSING" />
        <el-option label="高风险待处理" value="HIGH_RISK" />
        <el-option label="回访逾期" value="VISIT_OVERDUE" />
      </el-select>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="summary-card">
        <strong>{{ item.value }}</strong>
        <span>{{ item.label }}</span>
      </div>
    </section>

    <section class="todo-list">
      <article v-for="item in filteredTodos" :key="item.id" class="todo-card">
        <div class="todo-main">
          <el-tag :type="item.priority === 'HIGH' ? 'danger' : 'warning'" effect="plain">{{ typeText(item.type) }}</el-tag>
          <div>
            <strong>{{ item.studentName }}</strong>
            <span>{{ item.className }} · {{ item.reason }}</span>
          </div>
        </div>
        <div class="todo-side">
          <span>{{ formatTime(item.deadline) }}</span>
          <el-button type="primary" text @click="goHandle(item)">处理</el-button>
        </div>
      </article>
      <el-empty v-if="!filteredTodos.length" description="暂无待办" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Refresh } from '@element-plus/icons-vue'
import {
  getAssistantClassBoards,
  getAssistantTodos,
  type AssistantClassBoard,
  type AssistantTodoItem
} from '@/api/assistantOperations'

const router = useRouter()
const loading = ref(false)
const classId = ref<number | null>(null)
const typeFilter = ref('ALL')
const classOptions = ref<AssistantClassBoard[]>([])
const todos = ref<AssistantTodoItem[]>([])

const filteredTodos = computed(() =>
  todos.value.filter(item => typeFilter.value === 'ALL' || item.type === typeFilter.value)
)

const summaryCards = computed(() => [
  { label: '全部待办', value: filteredTodos.value.length },
  { label: '高优先级', value: filteredTodos.value.filter(item => item.priority === 'HIGH').length },
  { label: '待催交/考试', value: filteredTodos.value.filter(item => item.type === 'HOMEWORK_MISSING' || item.type === 'EXAM_MISSING').length },
  { label: '回访相关', value: filteredTodos.value.filter(item => item.type === 'VISIT_TODAY' || item.type === 'VISIT_OVERDUE').length }
])

async function loadData() {
  loading.value = true
  try {
    const [classRes, todoRes] = await Promise.all([
      getAssistantClassBoards(),
      getAssistantTodos(classId.value || undefined)
    ])
    classOptions.value = classRes.data || []
    todos.value = todoRes.data || []
  } catch (error: any) {
    ElMessage.error(error?.message || '加载待办失败')
  } finally {
    loading.value = false
  }
}

function goHandle(item: AssistantTodoItem) {
  if (item.type.includes('VISIT') || item.type === 'HIGH_RISK') {
    router.push('/assistant/risk-center')
  } else if (item.sourceType === 'EXAM') {
    router.push('/assistant/exams')
  } else if (item.sourceType === 'HOMEWORK') {
    router.push('/assistant/homework')
  } else if (item.sourceType === 'SURVEY') {
    router.push('/assistant/surveys')
  }
}

function typeText(type: string) {
  const map: Record<string, string> = {
    HOMEWORK_MISSING: '催交',
    VISIT_TODAY: '回访',
    EXAM_MISSING: '考试',
    SURVEY_MISSING: '问卷',
    HIGH_RISK: '高风险',
    VISIT_OVERDUE: '逾期'
  }
  return map[type] || type
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

onMounted(loadData)
</script>

<style scoped>
.assistant-todos {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
}

.page-header,
.toolbar,
.summary-grid,
.todo-card,
.todo-main,
.todo-side {
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
.todo-main span,
.todo-side span {
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
.todo-card {
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

.todo-list {
  display: grid;
  gap: 10px;
}

.todo-main {
  justify-content: flex-start;
  min-width: 0;
}

.todo-main div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.todo-main strong {
  color: var(--text-primary);
}

.todo-side {
  flex-shrink: 0;
}

@media (max-width: 960px) {
  .toolbar,
  .summary-grid,
  .todo-card {
    display: grid;
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .toolbar .el-select {
    width: 100%;
  }
}
</style>
