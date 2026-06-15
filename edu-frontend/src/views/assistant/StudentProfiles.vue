<template>
  <div class="assistant-profiles" v-loading="loading">
    <header class="page-header">
      <div>
        <h2>学员完整画像</h2>
        <p>集中查看基础信息、学习状态、风险原因、回访记录和任务明细。</p>
      </div>
      <el-button :icon="Refresh" @click="loadBoards">刷新</el-button>
    </header>

    <section class="profile-layout">
      <aside class="student-list">
        <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索姓名或手机号" />
        <button
          v-for="item in filteredStudents"
          :key="item.studentId"
          class="student-item"
          :class="{ active: selectedStudentId === item.studentId }"
          @click="selectStudent(item.studentId)"
        >
          <strong>{{ item.studentName }}</strong>
          <span>{{ item.className }} · {{ item.phone || item.username }}</span>
          <el-tag :type="item.riskScore ? 'danger' : 'success'" size="small" effect="plain">风险 {{ item.riskScore }}</el-tag>
        </button>
      </aside>

      <main class="profile-main" v-if="profile">
        <section class="hero-panel">
          <div>
            <h3>{{ profile.basicInfo.studentName }}</h3>
            <p>{{ profile.basicInfo.phone || profile.basicInfo.username }} · {{ profile.basicInfo.schoolName || '未设置学校' }}</p>
          </div>
          <el-tag :type="profile.risks.length ? 'danger' : 'success'" effect="plain">{{ profile.risks.length ? '需跟进' : '正常' }}</el-tag>
        </section>

        <section class="summary-grid">
          <div v-for="item in summaryCards" :key="item.label" class="summary-card">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </section>

        <section class="panel">
          <h4>基础信息</h4>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="当前班级">{{ profile.basicInfo.currentClassName }}</el-descriptions-item>
            <el-descriptions-item label="入班时间">{{ formatTime(profile.basicInfo.joinTime) }}</el-descriptions-item>
            <el-descriptions-item label="最近学习活动">{{ formatTime(profile.learningState.lastActivityTime) }}</el-descriptions-item>
            <el-descriptions-item label="考试均分">{{ profile.learningState.averageScore || 0 }}</el-descriptions-item>
            <el-descriptions-item label="AI 使用">{{ profile.learningState.aiQuestionCount }} 次</el-descriptions-item>
            <el-descriptions-item label="错题累计">{{ profile.learningState.mistakeCount }} 次</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="split-grid">
          <div class="panel">
            <h4>风险来源</h4>
            <el-table :data="profile.risks" size="small" max-height="300">
              <el-table-column prop="title" label="类型" width="100" />
              <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.level === 'HIGH' ? 'danger' : 'warning'" size="small">{{ levelText(row.level) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!profile.risks.length" description="暂无风险" :image-size="70" />
          </div>
          <div class="panel">
            <h4>最近回访</h4>
            <div v-for="visit in profile.visits.slice(0, 5)" :key="visit.id" class="visit-row">
              <strong>{{ visitResultText(visit.visitResult) }}</strong>
              <span>{{ formatTime(visit.visitTime) }} · {{ visit.problemCategory || '未分类' }} · 下次 {{ formatTime(visit.nextFollowTime) }}</span>
              <p>{{ visit.content }}</p>
              <p v-if="visit.conclusion">结论：{{ visit.conclusion }}</p>
            </div>
            <el-empty v-if="!profile.visits.length" description="暂无回访" :image-size="70" />
          </div>
        </section>

        <section class="panel">
          <h4>考试记录</h4>
          <el-table :data="profile.exams" size="small">
            <el-table-column prop="title" label="考试" min-width="180" show-overflow-tooltip />
            <el-table-column label="得分" width="90">
              <template #default="{ row }">{{ row.score ?? '-' }}/{{ row.totalScore || '-' }}</template>
            </el-table-column>
            <el-table-column label="切屏" width="80">
              <template #default="{ row }">{{ row.switchScreenCount || 0 }}</template>
            </el-table-column>
            <el-table-column label="提交时间" width="150">
              <template #default="{ row }">{{ formatTime(row.submitTime) }}</template>
            </el-table-column>
          </el-table>
        </section>

        <section class="split-grid">
          <div class="panel">
            <h4>作业记录</h4>
            <el-table :data="profile.homework" size="small" max-height="260">
              <el-table-column prop="title" label="作业" min-width="160" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="90" />
              <el-table-column label="提交" width="150">
                <template #default="{ row }">{{ formatTime(row.submitTime) }}</template>
              </el-table-column>
            </el-table>
          </div>
          <div class="panel">
            <h4>问卷记录</h4>
            <el-table :data="profile.surveys" size="small" max-height="260">
              <el-table-column prop="title" label="问卷" min-width="160" show-overflow-tooltip />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.submitted ? 'success' : 'warning'" size="small">{{ row.submitted ? '已填' : '未填' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="提交" width="150">
                <template #default="{ row }">{{ formatTime(row.submitTime) }}</template>
              </el-table-column>
            </el-table>
          </div>
        </section>
      </main>
      <main v-else class="profile-main empty-panel">
        <el-empty description="请选择学员" />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Refresh, Search } from '@element-plus/icons-vue'
import {
  getAssistantClassBoards,
  getAssistantStudentProfile,
  type AssistantStudentBrief,
  type AssistantStudentProfile
} from '@/api/assistantOperations'

const loading = ref(false)
const keyword = ref('')
const students = ref<AssistantStudentBrief[]>([])
const selectedStudentId = ref<number | null>(null)
const profile = ref<AssistantStudentProfile | null>(null)

const filteredStudents = computed(() => {
  const key = keyword.value.trim()
  if (!key) return students.value
  return students.value.filter(item =>
    item.studentName?.includes(key) ||
    item.phone?.includes(key) ||
    item.username?.includes(key)
  )
})

const summaryCards = computed(() => {
  const state = profile.value?.learningState
  if (!state) return []
  return [
    { label: '缺交任务', value: state.missingTaskCount },
    { label: '低分次数', value: state.lowScoreCount },
    { label: '切屏次数', value: state.switchScreenCount },
    { label: '未填问卷', value: state.surveyMissingCount }
  ]
})

async function loadBoards() {
  loading.value = true
  try {
    const res = await getAssistantClassBoards()
    students.value = (res.data || []).flatMap(item => item.students)
    if (!selectedStudentId.value && students.value.length) await selectStudent(students.value[0].studentId)
  } catch (error: any) {
    ElMessage.error(error?.message || '加载学员列表失败')
  } finally {
    loading.value = false
  }
}

async function selectStudent(studentId: number) {
  selectedStudentId.value = studentId
  const res = await getAssistantStudentProfile(studentId)
  profile.value = res.data
}

function formatTime(value?: string) {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

function levelText(value: string) {
  return value === 'HIGH' ? '高' : value === 'MEDIUM' ? '中' : '低'
}

function visitResultText(value: string) {
  const map: Record<string, string> = { REACHED: '已联系', UNREACHED: '未联系上', NEED_FOLLOW: '需跟进', RESOLVED: '已解决' }
  return map[value] || value
}

onMounted(loadBoards)
</script>

<style scoped>
.assistant-profiles {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 18px;
}

.page-header,
.hero-panel,
.summary-grid {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-header h2,
.hero-panel h3,
.panel h4 {
  margin: 0;
  color: var(--text-primary);
}

.page-header p,
.hero-panel p,
.student-item span,
.summary-card span,
.visit-row span,
.visit-row p {
  color: var(--text-secondary);
  font-size: 13px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
}

.student-list,
.profile-main {
  min-width: 0;
}

.student-list,
.hero-panel,
.panel,
.summary-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface-card);
  box-shadow: var(--shadow-sm);
}

.student-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
}

.student-item {
  display: grid;
  gap: 6px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  padding: 10px;
  text-align: left;
}

.student-item.active,
.student-item:hover {
  border-color: var(--primary-border);
  background: var(--surface-hover);
}

.profile-main {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero-panel,
.panel,
.summary-card {
  padding: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-card strong {
  display: block;
  color: var(--text-primary);
  font-family: 'JetBrains Mono', monospace;
  font-size: 24px;
}

.split-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.visit-row {
  border-bottom: 1px solid var(--border-subtle);
  padding: 10px 0;
}

.visit-row strong {
  color: var(--text-primary);
}

.empty-panel {
  display: grid;
  min-height: 420px;
  place-items: center;
}

@media (max-width: 1180px) {
  .profile-layout,
  .split-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
