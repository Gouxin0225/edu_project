<template>
  <div class="page ai-chat-page">
    <aside class="session-panel cyber-card">
      <div class="session-header">
        <div>
          <h2>AI 问答助手</h2>
          <p>{{ roleLabel }}</p>
        </div>
        <el-button type="primary" :icon="Plus" circle :loading="creating" @click="handleCreateSession" />
      </div>

      <div class="session-list" v-loading="sessionLoading">
        <el-empty v-if="sessions.length === 0 && !sessionLoading" description="暂无会话" :image-size="90" />
        <button
          v-for="item in sessions"
          :key="item.id"
          class="session-item"
          :class="{ active: item.id === activeSessionId }"
          @click="selectSession(item.id)"
        >
          <div class="session-title">{{ item.title || '新会话' }}</div>
          <div class="session-meta">
            <span>{{ getCourseLabel(item.courseCategory) }}</span>
            <span v-if="item.knowledgePoint">{{ item.knowledgePoint }}</span>
          </div>
          <div class="session-time">{{ formatTime(item.lastMessageTime || item.createTime) }}</div>
        </button>
      </div>
    </aside>

    <section class="chat-panel cyber-card">
      <div class="chat-topbar">
        <div>
          <h3>{{ activeSession?.title || '新会话' }}</h3>
          <p>{{ roleHint }}</p>
        </div>
        <div class="topbar-actions">
          <el-tag class="cyber-tag" type="primary">{{ roleLabel }}</el-tag>
          <el-button
            v-if="canManageKnowledge"
            :icon="Files"
            @click="openKnowledgeDialog"
            class="cyber-btn"
          >
            知识库
          </el-button>
          <el-button
            v-if="canCreateReviewNoteAction"
            :icon="Files"
            @click="openReviewNotesDialog"
            class="cyber-btn"
          >
            复习笔记
          </el-button>
          <el-button
            :icon="Delete"
            :disabled="!activeSessionId"
            @click="handleDeleteSession"
            class="cyber-btn"
          >
            删除
          </el-button>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        class="chat-settings"
      >
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="课程方向" prop="courseCategory">
              <el-select v-model="form.courseCategory" placeholder="可选：选择课程方向" clearable style="width:100%" @change="handleCourseChange">
                <el-option v-for="item in COURSE_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="知识点" prop="knowledgePoint">
              <el-select
                v-model="form.knowledgePoint"
                placeholder="可选：请选择或输入知识点"
                filterable
                allow-create
                clearable
                default-first-option
                style="width:100%"
              >
                <el-option v-for="item in knowledgeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="回答来源" prop="responseSource">
              <el-select v-model="form.responseSource" style="width:100%">
                <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6">
            <el-form-item label="回答模式" prop="mode">
              <el-radio-group v-model="form.mode" class="mode-group">
                <el-radio-button v-for="item in modeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="quick-prompts">
        <el-button
          v-for="item in quickPrompts"
          :key="item"
          size="small"
          :disabled="sending"
          class="cyber-btn"
          @click="submitQuickPrompt(item)"
        >
          {{ item }}
        </el-button>
      </div>

      <div ref="messageWindowRef" class="message-window" v-loading="messageLoading">
        <el-empty
          v-if="activeSessionId && messages.length === 0 && !messageLoading && !sending"
          description="当前会话暂无消息"
        />
        <el-empty
          v-else-if="!activeSessionId && !sessionLoading"
          description="请选择或新建一个会话"
        />

        <div v-else class="message-list">
          <div
            v-for="item in messages"
            :key="item.localId || item.id"
            class="message-row"
            :class="item.messageRole === 'USER' ? 'is-user' : 'is-ai'"
          >
            <div class="message-bubble">
              <div class="message-role">{{ item.messageRole === 'USER' ? '我' : 'AI' }}</div>
              <div class="message-content">
                <template v-if="item.messageRole === 'ASSISTANT'">
                  <div class="markdown-body" v-html="renderMarkdown(messageDisplayText(item))"></div>
                </template>
                <template v-else>
                  {{ messageDisplayText(item) }}
                </template>
                <span v-if="item.status === 'generating'" class="stream-cursor">|</span>
              </div>
              <div class="message-time">
                {{ formatTime(item.createTime) }}
                <span v-if="item.status && item.status !== 'finished'" class="message-status">{{ getStatusLabel(item.status) }}</span>
              </div>
              <div v-if="item.messageRole === 'ASSISTANT' && item.sourceSummary" class="message-source">
                <span>{{ getIntentLabel(item.intent) }}</span>
                <span>{{ getSourceLabel(item.responseSource) }}</span>
                <span>{{ item.sourceSummary }}</span>
              </div>
              <div v-if="canUseMessageAction(item)" class="message-actions">
                <el-button
                  v-if="canCreateQuestionAction"
                  size="small"
                  text
                  :loading="actionLoading"
                  @click="openActionPreview(item, 'CREATE_QUESTION')"
                >
                  转为题目
                </el-button>
                <el-button
                  v-if="canCreateReviewNoteAction"
                  size="small"
                  text
                  :loading="actionLoading"
                  @click="openActionPreview(item, 'CREATE_REVIEW_NOTE')"
                >
                  加入笔记
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <el-form ref="questionFormRef" :model="form" :rules="questionRules" class="question-form">
        <el-form-item prop="question">
          <el-input
            v-model="form.question"
            type="textarea"
            :rows="4"
            maxlength="2000"
            show-word-limit
            placeholder="请输入你想继续咨询的问题"
            @keydown.ctrl.enter.prevent="handleSubmit"
          />
        </el-form-item>
        <div class="form-actions">
          <el-button :icon="Refresh" :disabled="sending" @click="fetchSessionList" class="cyber-btn">刷新</el-button>
          <el-button :icon="Refresh" :disabled="sending || !canRegenerate" @click="handleRegenerate" class="cyber-btn">
            重新生成
          </el-button>
          <el-button v-if="sending" type="warning" @click="handleCancelGeneration" class="cyber-btn">
            停止生成
          </el-button>
          <el-button v-else type="primary" :icon="Promotion" @click="handleSubmit" class="cyber-btn cyber-btn-primary">
            发送
          </el-button>
        </div>
      </el-form>
    </section>

    <el-dialog v-model="actionDialog" :title="actionDialogTitle" width="760px" class="action-dialog">
      <div v-if="actionWarnings.length" class="action-warnings">
        <el-alert
          v-for="warning in actionWarnings"
          :key="warning"
          :title="warning"
          type="warning"
          show-icon
          :closable="false"
        />
      </div>

      <el-form
        v-if="activeActionType === 'CREATE_QUESTION'"
        :model="questionActionForm"
        label-width="90px"
        class="action-form"
      >
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12">
            <el-form-item label="课程方向">
              <el-select v-model="questionActionForm.courseCategory" style="width: 100%">
                <el-option v-for="item in COURSE_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="知识点">
              <el-input v-model="questionActionForm.knowledgePoint" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="题型">
              <el-select v-model="questionActionForm.type" style="width: 100%">
                <el-option v-for="item in questionTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="难度">
              <el-select v-model="questionActionForm.difficulty" style="width: 100%">
                <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="题干">
          <el-input v-model="questionActionForm.content" type="textarea" :rows="5" maxlength="5000" show-word-limit />
        </el-form-item>
        <el-form-item label="选项JSON">
          <el-input v-model="questionActionForm.optionsJson" type="textarea" :rows="3" maxlength="3000" show-word-limit />
        </el-form-item>
        <el-form-item label="标准答案">
          <el-input v-model="questionActionForm.standardAnswer" type="textarea" :rows="3" maxlength="3000" show-word-limit />
        </el-form-item>
        <el-form-item label="解析">
          <el-input v-model="questionActionForm.analysis" type="textarea" :rows="5" maxlength="8000" show-word-limit />
        </el-form-item>
      </el-form>

      <el-form
        v-if="activeActionType === 'CREATE_REVIEW_NOTE'"
        :model="reviewNoteActionForm"
        label-width="90px"
        class="action-form"
      >
        <el-form-item label="标题">
          <el-input v-model="reviewNoteActionForm.title" maxlength="200" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12">
            <el-form-item label="课程方向">
              <el-select v-model="reviewNoteActionForm.courseCategory" style="width: 100%">
                <el-option v-for="item in COURSE_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="知识点">
              <el-input v-model="reviewNoteActionForm.knowledgePoint" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容">
          <el-input v-model="reviewNoteActionForm.content" type="textarea" :rows="10" maxlength="12000" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="actionDialog = false">取消</el-button>
        <el-button type="primary" :loading="actionSaving" @click="handleConfirmAction">确认保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewNotesDialog" title="复习笔记" width="760px" class="review-notes-dialog">
      <el-table :data="reviewNotes" v-loading="reviewNotesLoading" size="small" class="knowledge-table">
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column prop="courseCategory" label="课程" width="100" />
        <el-table-column prop="knowledgePoint" label="知识点" width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="note-content">{{ row.content }}</div>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="knowledgeDialog" title="AI 知识库" width="760px" class="knowledge-dialog">
      <el-tabs v-model="knowledgeTab">
        <el-tab-pane label="录入文档" name="text">
          <el-form :model="knowledgeForm" label-width="90px" class="knowledge-form">
            <el-form-item label="标题">
              <el-input v-model="knowledgeForm.title" maxlength="200" placeholder="如：Python 函数课程大纲" />
            </el-form-item>
            <el-form-item label="课程方向">
              <el-select v-model="knowledgeForm.courseCategory" style="width: 100%">
                <el-option v-for="item in COURSE_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="知识点">
              <el-input v-model="knowledgeForm.knowledgePoint" maxlength="100" placeholder="可选，如：函数" />
            </el-form-item>
            <el-form-item label="可见范围">
              <el-radio-group v-model="knowledgeForm.visibility">
                <el-radio-button value="PRIVATE">仅自己</el-radio-button>
                <el-radio-button value="PUBLIC">公开</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="引用范围">
              <el-radio-group v-model="knowledgeForm.accessScope">
                <el-radio-button value="TEACHER_ONLY">仅教师引用</el-radio-button>
                <el-radio-button value="STUDENT_SAFE">学生可引用</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="knowledgeForm.content" type="textarea" :rows="8" maxlength="200000" show-word-limit />
            </el-form-item>
            <div class="dialog-actions">
              <el-button type="primary" :loading="knowledgeSaving" @click="handleCreateKnowledge">保存并切片</el-button>
            </div>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="上传文档" name="upload">
          <el-form :model="knowledgeForm" label-width="90px" class="knowledge-form">
            <el-form-item label="标题">
              <el-input v-model="knowledgeForm.title" maxlength="200" placeholder="不填则使用文件名" />
            </el-form-item>
            <el-form-item label="课程方向">
              <el-select v-model="knowledgeForm.courseCategory" style="width: 100%">
                <el-option v-for="item in COURSE_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="知识点">
              <el-input v-model="knowledgeForm.knowledgePoint" maxlength="100" placeholder="可选" />
            </el-form-item>
            <el-form-item label="可见范围">
              <el-radio-group v-model="knowledgeForm.visibility">
                <el-radio-button value="PRIVATE">仅自己</el-radio-button>
                <el-radio-button value="PUBLIC">公开</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="引用范围">
              <el-radio-group v-model="knowledgeForm.accessScope">
                <el-radio-button value="TEACHER_ONLY">仅教师引用</el-radio-button>
                <el-radio-button value="STUDENT_SAFE">学生可引用</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="文件">
              <input class="knowledge-file-input" type="file" accept=".txt,.md,.csv" @change="handleKnowledgeFileChange" />
              <span class="upload-tip">支持 .txt / .md / .csv</span>
            </el-form-item>
            <div class="dialog-actions">
              <el-button type="primary" :loading="knowledgeSaving" :disabled="!knowledgeFile" @click="handleUploadKnowledge">上传并切片</el-button>
            </div>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="文档列表" name="list">
          <el-table :data="knowledgeDocs" v-loading="knowledgeLoading" size="small" class="knowledge-table">
            <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
            <el-table-column prop="courseCategory" label="课程" width="100" />
            <el-table-column prop="knowledgePoint" label="知识点" width="120" show-overflow-tooltip />
            <el-table-column prop="visibility" label="范围" width="90" />
            <el-table-column label="引用" width="110">
              <template #default="{ row }">
                {{ row.accessScope === 'STUDENT_SAFE' ? '学生可引用' : '仅教师' }}
              </template>
            </el-table-column>
            <el-table-column prop="chunkCount" label="切片" width="70" />
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button type="danger" text size="small" @click="handleDeleteKnowledge(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Files, Plus, Promotion, Refresh } from '@element-plus/icons-vue'
import { COURSE_CATEGORIES } from '@/api/question'
import {
  createKnowledgeDocument,
  deleteKnowledgeDocument,
  getKnowledgeDocuments,
  uploadKnowledgeDocument,
  type AiKnowledgeDocument
} from '@/api/aiKnowledge'
import {
  confirmAiResultAction,
  getStudentReviewNotes,
  previewAiResultAction,
  type AiResultActionType,
  type StudentReviewNote
} from '@/api/aiAction'
import {
  createAiChatSession,
  cancelAiChatStream,
  deleteAiChatSession,
  getAiChatSessionMessages,
  getAiChatSessions,
  regenerateAiChatSessionMessage,
  streamAiChatSessionMessage,
  type AiChatHistoryMessage,
  type AiChatMode,
  type AiChatResponseSource,
  type AiChatSession,
  type AiChatStreamEvent
} from '@/api/aiChat'
import { useUserStore } from '@/stores/user'

interface UiMessage extends AiChatHistoryMessage {
  localId?: number
}

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const questionFormRef = ref<FormInstance>()
const sessionLoading = ref(false)
const messageLoading = ref(false)
const sending = ref(false)
const creating = ref(false)
const messageWindowRef = ref<HTMLElement>()
const knowledgeDialog = ref(false)
const knowledgeTab = ref('text')
const knowledgeSaving = ref(false)
const knowledgeLoading = ref(false)
const knowledgeFile = ref<File | null>(null)
const reviewNotesDialog = ref(false)
const reviewNotesLoading = ref(false)
const actionDialog = ref(false)
const actionLoading = ref(false)
const actionSaving = ref(false)
const activeActionType = ref<AiResultActionType | null>(null)
const actionSourceMessageId = ref<number | undefined>()
const actionWarnings = ref<string[]>([])
const knowledgeDocs = ref<AiKnowledgeDocument[]>([])
const reviewNotes = ref<StudentReviewNote[]>([])
const sessions = ref<AiChatSession[]>([])
const messages = ref<UiMessage[]>([])
const activeSessionId = ref<number | null>(null)
const currentAbortController = ref<AbortController | null>(null)
const streamAssistantLocalId = ref<number | null>(null)

const form = reactive({
  courseCategory: '',
  knowledgePoint: '',
  question: '',
  mode: 'DETAILED' as AiChatMode,
  responseSource: 'AUTO' as AiChatResponseSource
})

const modeOptions = [
  { label: '详细', value: 'DETAILED' },
  { label: '简洁', value: 'SIMPLE' },
  { label: '教案式', value: 'LESSON_PLAN' },
  { label: '出题式', value: 'QUESTION' }
]

const sourceOptions = [
  { label: '自动', value: 'AUTO' },
  { label: '仅平台数据', value: 'PLATFORM_ONLY' },
  { label: '仅知识库', value: 'KNOWLEDGE_ONLY' },
  { label: '通用 AI', value: 'GENERAL_AI' }
]

const questionTypeOptions = [
  { label: '单选题', value: 'SINGLE' },
  { label: '多选题', value: 'MULTIPLE' },
  { label: '判断题', value: 'JUDGE' },
  { label: '简答题', value: 'SHORT' },
  { label: '编程题', value: 'CODE' }
]

const difficultyOptions = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' }
]

const knowledgeMap: Record<string, string[]> = {
  python: ['变量与数据类型', '条件语句', '循环', '函数', '面向对象', '异常处理', '文件操作'],
  mysql: ['SQL基础', '表设计', '索引', '事务', '多表查询', '视图', '存储过程'],
  csa: ['Linux基础', '用户权限', '文件系统', '网络配置', '服务管理'],
  hcia: ['网络基础', '路由交换', 'VLAN', 'OSPF', 'ACL', 'NAT'],
  hcip: ['高级路由', 'BGP', 'MPLS', '网络可靠性', '网络安全'],
  web: ['HTML', 'CSS', 'JavaScript', 'Vue', 'HTTP', '前后端交互'],
  rhce: ['Shell脚本', 'Ansible', '系统服务', '容器基础', '安全加固']
}

const rules: FormRules = {
  mode: [{ required: true, message: '请选择回答模式', trigger: 'change' }],
  responseSource: [{ required: true, message: '请选择回答来源', trigger: 'change' }]
}

const questionRules: FormRules = {
  question: [
    { required: true, message: '请输入问题', trigger: 'blur' },
    { min: 2, max: 2000, message: '问题长度需在2到2000个字符之间', trigger: 'blur' }
  ]
}

const roleLabel = computed(() => {
  const role = userStore.userInfo?.role
  if (role === 'ADMIN') return '管理员模式'
  if (role === 'TEACHER' || role === 'ASSISTANT') return '教师模式'
  return '学生模式'
})
const roleHint = computed(() =>
  ['ADMIN', 'TEACHER', 'ASSISTANT'].includes(userStore.userInfo?.role || '')
    ? '当前会话会偏向教学辅助、结构化讲解和课堂设计。'
    : '当前会话会偏向概念讲解、示例演示和学习引导。'
)
const knowledgeOptions = computed(() => knowledgeMap[form.courseCategory] ?? [])
const activeSession = computed(() => sessions.value.find(item => item.id === activeSessionId.value))
const canRegenerate = computed(() => !!activeSessionId.value && messages.value.some(item => item.messageRole === 'USER'))
const canManageKnowledge = computed(() => userStore.userInfo?.role === 'TEACHER' || userStore.userInfo?.role === 'ADMIN')
const canCreateQuestionAction = computed(() => ['ADMIN', 'TEACHER', 'ASSISTANT'].includes(userStore.userInfo?.role || ''))
const canCreateReviewNoteAction = computed(() => userStore.userInfo?.role === 'STUDENT')
const actionDialogTitle = computed(() => activeActionType.value === 'CREATE_QUESTION' ? '预览并保存为题目' : '预览并保存为复习笔记')
const quickPrompts = computed(() => {
  const role = userStore.userInfo?.role || ''
  if (['ADMIN', 'TEACHER', 'ASSISTANT'].includes(role)) {
    return [
      '查看我管理的班级概览',
      '列出各班学生数量和学生名单',
      '查看近期考试/作业提交情况',
      '根据班级情况给出教学跟进建议'
    ]
  }
  return [
    '查看我的班级和个人信息',
    '查看我的待完成任务',
    '查看我的最近成绩和反馈',
    '根据我的错题给出复习建议'
  ]
})

const knowledgeForm = reactive({
  title: '',
  courseCategory: 'python',
  knowledgePoint: '',
  content: '',
  visibility: 'PRIVATE' as 'PRIVATE' | 'PUBLIC',
  accessScope: 'TEACHER_ONLY' as 'STUDENT_SAFE' | 'TEACHER_ONLY'
})

const questionActionForm = reactive({
  courseCategory: 'python',
  knowledgePoint: '',
  type: 'SHORT',
  difficulty: 'MEDIUM',
  content: '',
  optionsJson: '',
  standardAnswer: '',
  analysis: ''
})

const reviewNoteActionForm = reactive({
  title: '',
  courseCategory: 'python',
  knowledgePoint: '',
  content: '',
  sourceType: 'AI_CHAT'
})

onMounted(() => {
  fetchSessionList()
})

onBeforeUnmount(() => {
  currentAbortController.value?.abort()
})

async function fetchSessionList() {
  sessionLoading.value = true
  try {
    const res = await getAiChatSessions()
    sessions.value = res.data
    if (!activeSessionId.value && sessions.value.length > 0) {
      await selectSession(sessions.value[0].id)
    } else if (activeSessionId.value && !sessions.value.some(item => item.id === activeSessionId.value)) {
      activeSessionId.value = null
      messages.value = []
    }
  } finally {
    sessionLoading.value = false
  }
}

async function handleCreateSession() {
  creating.value = true
  try {
    const res = await createAiChatSession({
      courseCategory: form.courseCategory || undefined,
      knowledgePoint: form.knowledgePoint || undefined
    })
    sessions.value.unshift(res.data)
    await selectSession(res.data.id)
  } finally {
    creating.value = false
  }
}

async function selectSession(id: number) {
  if (sending.value) {
    ElMessage.warning('当前回答生成中，请先停止生成')
    return
  }
  if (activeSessionId.value === id && messages.value.length > 0) return
  activeSessionId.value = id
  const session = sessions.value.find(item => item.id === id)
  form.courseCategory = session?.courseCategory || ''
  form.knowledgePoint = session?.knowledgePoint || ''
  await fetchMessages(id)
}

async function fetchMessages(id: number) {
  messageLoading.value = true
  try {
    const res = await getAiChatSessionMessages(id)
    messages.value = res.data
  } finally {
    messageLoading.value = false
  }
}

async function handleDeleteSession() {
  if (!activeSessionId.value) return
  if (sending.value) {
    await handleCancelGeneration()
  }
  await ElMessageBox.confirm('确定要删除当前会话及其历史消息吗？', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })

  const deleteId = activeSessionId.value
  await deleteAiChatSession(deleteId)
  ElMessage.success('会话已删除')
  sessions.value = sessions.value.filter(item => item.id !== deleteId)
  activeSessionId.value = null
  messages.value = []
  if (sessions.value.length > 0) {
    await selectSession(sessions.value[0].id)
  }
}

function handleCourseChange() {
  form.knowledgePoint = ''
}

async function submitQuickPrompt(question: string) {
  form.question = question
  await nextTick()
  await handleSubmit()
}

function canUseMessageAction(item: UiMessage) {
  return item.messageRole === 'ASSISTANT' && item.status !== 'generating' && !!item.content?.trim()
}

async function openActionPreview(item: UiMessage, actionType: AiResultActionType) {
  if (!item.id) return
  actionLoading.value = true
  try {
    const res = await previewAiResultAction({
      actionType,
      sourceMessageId: item.id,
      sourceContent: item.content,
      courseCategory: item.courseCategory || form.courseCategory,
      knowledgePoint: item.knowledgePoint || form.knowledgePoint
    })
    activeActionType.value = actionType
    actionSourceMessageId.value = res.data.sourceMessageId || item.id
    actionWarnings.value = res.data.warnings || []
    if (actionType === 'CREATE_QUESTION') {
      Object.assign(questionActionForm, {
        courseCategory: res.data.data.courseCategory || form.courseCategory,
        knowledgePoint: res.data.data.knowledgePoint || form.knowledgePoint,
        type: res.data.data.type || 'SHORT',
        difficulty: res.data.data.difficulty || 'MEDIUM',
        content: res.data.data.content || '',
        optionsJson: res.data.data.optionsJson || '',
        standardAnswer: res.data.data.standardAnswer || '',
        analysis: res.data.data.analysis || ''
      })
    } else {
      Object.assign(reviewNoteActionForm, {
        title: res.data.data.title || '',
        courseCategory: res.data.data.courseCategory || form.courseCategory,
        knowledgePoint: res.data.data.knowledgePoint || form.knowledgePoint,
        content: res.data.data.content || '',
        sourceType: res.data.data.sourceType || 'AI_CHAT'
      })
    }
    actionDialog.value = true
  } finally {
    actionLoading.value = false
  }
}

async function handleConfirmAction() {
  if (!activeActionType.value) return
  const data = activeActionType.value === 'CREATE_QUESTION'
    ? { ...questionActionForm }
    : { ...reviewNoteActionForm }
  actionSaving.value = true
  try {
    const res = await confirmAiResultAction({
      actionType: activeActionType.value,
      sourceMessageId: actionSourceMessageId.value,
      data
    })
    ElMessage.success(res.data.message || '保存成功')
    actionDialog.value = false
  } finally {
    actionSaving.value = false
  }
}

async function openKnowledgeDialog() {
  knowledgeDialog.value = true
  knowledgeForm.courseCategory = form.courseCategory
  knowledgeForm.knowledgePoint = form.knowledgePoint
  await fetchKnowledgeDocs()
}

async function openReviewNotesDialog() {
  reviewNotesDialog.value = true
  reviewNotesLoading.value = true
  try {
    const res = await getStudentReviewNotes()
    reviewNotes.value = res.data
  } finally {
    reviewNotesLoading.value = false
  }
}

async function fetchKnowledgeDocs() {
  knowledgeLoading.value = true
  try {
    const res = await getKnowledgeDocuments({ courseCategory: knowledgeForm.courseCategory })
    knowledgeDocs.value = res.data
  } finally {
    knowledgeLoading.value = false
  }
}

async function handleCreateKnowledge() {
  if (!knowledgeForm.title || !knowledgeForm.courseCategory || !knowledgeForm.content.trim()) {
    ElMessage.warning('请填写标题、课程方向和内容')
    return
  }
  knowledgeSaving.value = true
  try {
    await createKnowledgeDocument({
      title: knowledgeForm.title,
      courseCategory: knowledgeForm.courseCategory,
      knowledgePoint: knowledgeForm.knowledgePoint || undefined,
      content: knowledgeForm.content,
      visibility: knowledgeForm.visibility,
      accessScope: knowledgeForm.accessScope
    })
    ElMessage.success('知识文档已保存')
    knowledgeForm.content = ''
    await fetchKnowledgeDocs()
    knowledgeTab.value = 'list'
  } finally {
    knowledgeSaving.value = false
  }
}

function handleKnowledgeFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  knowledgeFile.value = input.files?.[0] || null
}

async function handleUploadKnowledge() {
  if (!knowledgeFile.value) return
  knowledgeSaving.value = true
  try {
    await uploadKnowledgeDocument({
      file: knowledgeFile.value,
      title: knowledgeForm.title || undefined,
      courseCategory: knowledgeForm.courseCategory,
      knowledgePoint: knowledgeForm.knowledgePoint || undefined,
      visibility: knowledgeForm.visibility,
      accessScope: knowledgeForm.accessScope
    })
    ElMessage.success('知识文档已上传')
    knowledgeFile.value = null
    await fetchKnowledgeDocs()
    knowledgeTab.value = 'list'
  } finally {
    knowledgeSaving.value = false
  }
}

async function handleDeleteKnowledge(id: number) {
  await ElMessageBox.confirm('确定删除该知识文档及其切片吗？', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteKnowledgeDocument(id)
  ElMessage.success('知识文档已删除')
  await fetchKnowledgeDocs()
}

async function handleSubmit() {
  if (sending.value) return
  const settingsValid = await formRef.value?.validate().catch(() => false)
  const questionValid = await questionFormRef.value?.validate().catch(() => false)
  if (!settingsValid || !questionValid) return

  if (!activeSessionId.value) {
    await handleCreateSession()
  }
  if (!activeSessionId.value) return

  const question = form.question
  sending.value = true
  const assistantLocalId = Date.now() + 1
  const localUserMessage: UiMessage = {
    id: Date.now(),
    localId: Date.now(),
    sessionId: activeSessionId.value,
    messageRole: 'USER',
    content: question,
    courseCategory: form.courseCategory,
    knowledgePoint: form.knowledgePoint,
    mode: form.mode,
    responseSource: form.responseSource,
    createTime: new Date().toISOString()
  }
  messages.value.push(localUserMessage)
  messages.value.push({
    id: assistantLocalId,
    localId: assistantLocalId,
    sessionId: activeSessionId.value,
    messageRole: 'ASSISTANT',
    content: '',
    courseCategory: form.courseCategory,
    knowledgePoint: form.knowledgePoint,
    mode: form.mode,
    responseSource: form.responseSource,
    status: 'generating',
    createTime: new Date().toISOString()
  })
  streamAssistantLocalId.value = assistantLocalId
  scrollToBottom()
  form.question = ''

  try {
    const controller = new AbortController()
    currentAbortController.value = controller
    await streamAiChatSessionMessage(
      activeSessionId.value,
      {
        courseCategory: form.courseCategory || undefined,
        knowledgePoint: form.knowledgePoint || undefined,
        question,
        mode: form.mode,
        responseSource: form.responseSource
      },
      event => handleStreamEvent(event, localUserMessage, assistantLocalId),
      controller.signal
    )
  } catch (error: any) {
    if (error?.name !== 'AbortError') {
      if (localUserMessage.id !== localUserMessage.localId) {
        markStreamMessage(assistantLocalId, { status: 'failed' })
      } else {
        messages.value = messages.value.filter(item => item.localId !== localUserMessage.localId && item.localId !== assistantLocalId)
        form.question = question
      }
      ElMessage.error(error?.message || 'AI问答请求失败')
    }
  } finally {
    sending.value = false
    currentAbortController.value = null
    streamAssistantLocalId.value = null
  }
}

async function handleRegenerate() {
  if (sending.value || !activeSessionId.value) return
  const assistantLocalId = Date.now()
  messages.value.push({
    id: assistantLocalId,
    localId: assistantLocalId,
    sessionId: activeSessionId.value,
    messageRole: 'ASSISTANT',
    content: '',
    courseCategory: form.courseCategory,
    knowledgePoint: form.knowledgePoint,
    mode: form.mode,
    responseSource: form.responseSource,
    status: 'generating',
    createTime: new Date().toISOString()
  })
  sending.value = true
  streamAssistantLocalId.value = assistantLocalId
  scrollToBottom()

  try {
    const controller = new AbortController()
    currentAbortController.value = controller
    await regenerateAiChatSessionMessage(
      activeSessionId.value,
      event => handleStreamEvent(event, null, assistantLocalId),
      controller.signal
    )
  } catch (error: any) {
    if (error?.name !== 'AbortError') {
      markStreamMessage(assistantLocalId, { status: 'failed' })
      ElMessage.error(error?.message || '重新生成失败')
    }
  } finally {
    sending.value = false
    currentAbortController.value = null
    streamAssistantLocalId.value = null
  }
}

async function handleCancelGeneration() {
  if (!activeSessionId.value) return
  try {
    await cancelAiChatStream(activeSessionId.value)
  } catch {
    // 取消生成是尽力而为，后端任务可能已经自然结束。
  } finally {
    currentAbortController.value?.abort()
    if (streamAssistantLocalId.value) {
      markStreamMessage(streamAssistantLocalId.value, { status: 'interrupted' })
    }
    sending.value = false
  }
}

function handleStreamEvent(event: AiChatStreamEvent, localUserMessage: UiMessage | null, assistantLocalId: number) {
  const data = event.data
  if (event.event === 'start') {
    if (localUserMessage && data.userMessageId) {
      localUserMessage.id = data.userMessageId
    }
    markStreamMessage(assistantLocalId, {
      id: data.assistantMessageId || assistantLocalId,
      intent: data.intent,
      responseSource: data.responseSource,
      sourceSummary: data.sourceSummary,
      status: data.status || 'generating'
    })
    return
  }

  if (event.event === 'delta') {
    const item = findStreamMessage(assistantLocalId)
    if (item) {
      item.content += data.content || ''
      scrollToBottom()
    }
    return
  }

  if (event.event === 'done') {
    markStreamMessage(assistantLocalId, {
      id: data.assistantMessageId || assistantLocalId,
      content: data.answer || findStreamMessage(assistantLocalId)?.content || '',
      model: data.model,
      intent: data.intent,
      responseSource: data.responseSource,
      sourceSummary: data.sourceSummary,
      status: data.status || 'finished',
      createTime: data.createTime || new Date().toISOString()
    })
    updateActiveSession(data)
    return
  }

  if (event.event === 'interrupted') {
    markStreamMessage(assistantLocalId, {
      id: data.assistantMessageId || assistantLocalId,
      content: data.answer || findStreamMessage(assistantLocalId)?.content || '',
      status: 'interrupted'
    })
    ElMessage.info('已停止生成')
    return
  }

  if (event.event === 'error') {
    markStreamMessage(assistantLocalId, { status: data.status || 'failed' })
    ElMessage.error(data.message || 'AI问答请求失败')
  }
}

function findStreamMessage(localId: number) {
  return messages.value.find(item => item.localId === localId || item.id === localId)
}

function markStreamMessage(localId: number, patch: Partial<UiMessage>) {
  const item = findStreamMessage(localId)
  if (item) Object.assign(item, patch)
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageWindowRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function updateActiveSession(data: any) {
  const session = sessions.value.find(item => item.id === activeSessionId.value)
  if (!session) return
  session.title = data.sessionTitle || session.title
  session.lastMessageTime = data.createTime || session.lastMessageTime
  sessions.value = [...sessions.value].sort((a, b) => {
    const ta = new Date(a.lastMessageTime || a.createTime).getTime()
    const tb = new Date(b.lastMessageTime || b.createTime).getTime()
    return tb - ta
  })
}

function messageDisplayText(item: UiMessage) {
  return item.content || (item.status === 'generating' ? '正在生成...' : '')
}

type TableAlignment = 'left' | 'center' | 'right' | ''

interface MarkdownBlockResult {
  html: string
  nextIndex: number
}

interface ListMarkerMatch {
  indent: number
  ordered: boolean
  marker: string
  content: string
}

function renderMarkdown(markdown: string) {
  const source = normalizeMarkdown(markdown)
  if (!source.trim()) return ''
  const lines = source.split('\n')
  return renderMarkdownBlocks(lines).html
}

function normalizeMarkdown(markdown: string) {
  return markdown.replace(/\r\n/g, '\n').replace(/\r/g, '\n').replace(/\t/g, '  ')
}

function renderMarkdownBlocks(lines: string[], startIndex = 0): MarkdownBlockResult {
  const html: string[] = []
  let index = startIndex

  while (index < lines.length) {
    if (!lines[index].trim()) {
      index += 1
      continue
    }

    const fenced = parseFencedCodeBlock(lines, index)
    if (fenced) {
      html.push(fenced.html)
      index = fenced.nextIndex
      continue
    }

    const heading = parseHeadingBlock(lines, index)
    if (heading) {
      html.push(heading.html)
      index = heading.nextIndex
      continue
    }

    if (isThematicBreak(lines[index])) {
      html.push('<hr>')
      index += 1
      continue
    }

    const table = parseTableBlock(lines, index)
    if (table) {
      html.push(table.html)
      index = table.nextIndex
      continue
    }

    const blockquote = parseBlockquoteBlock(lines, index)
    if (blockquote) {
      html.push(blockquote.html)
      index = blockquote.nextIndex
      continue
    }

    const list = parseListBlock(lines, index)
    if (list) {
      html.push(list.html)
      index = list.nextIndex
      continue
    }

    const paragraph = parseParagraphBlock(lines, index)
    html.push(paragraph.html)
    index = paragraph.nextIndex
  }

  return { html: html.join(''), nextIndex: index }
}

function parseFencedCodeBlock(lines: string[], startIndex: number): MarkdownBlockResult | null {
  const fenceMatch = lines[startIndex].trim().match(/^(`{3,}|~{3,})([\w-]*)\s*$/)
  if (!fenceMatch) return null

  const fence = fenceMatch[1]
  const language = fenceMatch[2]
  const fenceChar = fence[0]
  const fenceLength = fence.length
  const codeLines: string[] = []
  let index = startIndex + 1

  while (index < lines.length) {
    const trimmed = lines[index].trim()
    if (trimmed && new RegExp(`^${escapeFenceForRegex(fenceChar)}{${fenceLength},}\\s*$`).test(trimmed)) {
      index += 1
      break
    }
    codeLines.push(lines[index])
    index += 1
  }

  const className = language ? ` class="language-${escapeAttribute(language)}"` : ''
  return {
    html: `<pre><code${className}>${escapeHtml(codeLines.join('\n'))}</code></pre>`,
    nextIndex: index
  }
}

function parseHeadingBlock(lines: string[], startIndex: number): MarkdownBlockResult | null {
  const line = lines[startIndex]
  const atxMatch = line.trim().match(/^(#{1,6})[ \t]+(.+?)(?:\s+#+\s*)?$/)
  if (atxMatch) {
    const level = atxMatch[1].length
    return {
      html: `<h${level}>${renderInline(atxMatch[2])}</h${level}>`,
      nextIndex: startIndex + 1
    }
  }

  const nextLine = lines[startIndex + 1]
  if (!nextLine || !line.trim()) return null

  const setextMatch = nextLine.trim().match(/^(=+|-+)\s*$/)
  if (!setextMatch || isTableStart(lines, startIndex)) return null

  const level = setextMatch[1][0] === '=' ? 1 : 2
  return {
    html: `<h${level}>${renderInline(line.trim())}</h${level}>`,
    nextIndex: startIndex + 2
  }
}

function parseBlockquoteBlock(lines: string[], startIndex: number): MarkdownBlockResult | null {
  if (!lines[startIndex].trimStart().startsWith('>')) return null

  const quoteLines: string[] = []
  let index = startIndex

  while (index < lines.length) {
    const line = lines[index]
    if (line.trimStart().startsWith('>')) {
      quoteLines.push(line.replace(/^\s*>\s?/, ''))
      index += 1
      continue
    }

    if (!line.trim()) {
      const nextNonEmpty = findNextNonEmptyLine(lines, index + 1)
      if (nextNonEmpty !== -1 && lines[nextNonEmpty].trimStart().startsWith('>')) {
        quoteLines.push('')
        index += 1
        continue
      }
    }
    break
  }

  return {
    html: `<blockquote>${renderMarkdown(quoteLines.join('\n'))}</blockquote>`,
    nextIndex: index
  }
}

function parseListBlock(lines: string[], startIndex: number): MarkdownBlockResult | null {
  const firstItem = matchListMarker(lines[startIndex])
  if (!firstItem) return null

  const tag = firstItem.ordered ? 'ol' : 'ul'
  const items: string[] = []
  let index = startIndex

  while (index < lines.length) {
    const marker = matchListMarker(lines[index])
    if (!marker || marker.indent !== firstItem.indent || marker.ordered !== firstItem.ordered) break

    const itemLines = [marker.content]
    index += 1

    while (index < lines.length) {
      const line = lines[index]
      if (!line.trim()) {
        const nextNonEmpty = findNextNonEmptyLine(lines, index + 1)
        if (nextNonEmpty === -1) {
          index = lines.length
          break
        }

        const nextMarker = matchListMarker(lines[nextNonEmpty])
        const nextIndent = countIndent(lines[nextNonEmpty])
        if (nextMarker && nextMarker.indent === firstItem.indent && nextMarker.ordered === firstItem.ordered) {
          index = nextNonEmpty
          break
        }
        if (nextIndent <= firstItem.indent && !(nextMarker && nextMarker.indent > firstItem.indent)) {
          index = nextNonEmpty
          break
        }

        itemLines.push('')
        index += 1
        continue
      }

      const nextMarker = matchListMarker(line)
      const indent = countIndent(line)

      if (nextMarker && nextMarker.indent === firstItem.indent && nextMarker.ordered === firstItem.ordered) break
      if (nextMarker && nextMarker.indent < firstItem.indent) break

      if (indent > firstItem.indent || (nextMarker && nextMarker.indent > firstItem.indent)) {
        itemLines.push(stripIndent(line, Math.min(indent, firstItem.indent + 2)))
        index += 1
        continue
      }

      break
    }

    items.push(renderListItem(itemLines))

    while (index < lines.length && !lines[index].trim()) {
      index += 1
    }
  }

  return {
    html: `<${tag}>${items.join('')}</${tag}>`,
    nextIndex: index
  }
}

function parseParagraphBlock(lines: string[], startIndex: number): MarkdownBlockResult {
  const paragraph: string[] = []
  let index = startIndex

  while (index < lines.length) {
    if (!lines[index].trim()) break
    if (index > startIndex && isBlockStart(lines, index)) break
    paragraph.push(lines[index].trimEnd())
    index += 1
  }

  return {
    html: `<p>${renderInline(paragraph.join('\n')).replace(/\n/g, '<br>')}</p>`,
    nextIndex: index
  }
}

function isBlockStart(lines: string[], index: number) {
  const line = lines[index]
  if (!line?.trim()) return false
  return Boolean(
    parseFencedCodeBlock(lines, index) ||
    parseHeadingBlock(lines, index) ||
    isThematicBreak(line) ||
    parseTableBlock(lines, index) ||
    parseBlockquoteBlock(lines, index) ||
    parseListBlock(lines, index)
  )
}

function matchListMarker(line: string): ListMarkerMatch | null {
  const match = line.match(/^(\s*)([-+*]|\d+[.)])\s+(.*)$/)
  if (!match) return null
  return {
    indent: match[1].length,
    ordered: /\d+[.)]/.test(match[2]),
    marker: match[2],
    content: match[3]
  }
}

function renderListItem(lines: string[]) {
  const itemLines = trimBlankEdges(lines)
  if (!itemLines.length) return '<li></li>'

  const taskMatch = itemLines[0].match(/^\[( |x|X)\]\s+(.*)$/)
  if (taskMatch) {
    itemLines[0] = taskMatch[2]
  }

  const contentHtml = unwrapSingleParagraph(renderMarkdown(itemLines.join('\n')).trim())
  if (!taskMatch) {
    return `<li>${contentHtml}</li>`
  }

  const checked = taskMatch[1].toLowerCase() === 'x' ? ' checked' : ''
  return `
    <li class="task-list-item">
      <div class="task-list-row">
        <input type="checkbox" disabled${checked}>
        <div class="task-list-content">${contentHtml}</div>
      </div>
    </li>
  `.trim()
}

function trimBlankEdges(lines: string[]) {
  const result = [...lines]
  while (result.length && !result[0].trim()) result.shift()
  while (result.length && !result[result.length - 1].trim()) result.pop()
  return result
}

function unwrapSingleParagraph(html: string) {
  const trimmed = html.trim()
  const match = trimmed.match(/^<p>([\s\S]*)<\/p>$/)
  return match ? match[1] : trimmed
}

function findNextNonEmptyLine(lines: string[], startIndex: number) {
  for (let index = startIndex; index < lines.length; index += 1) {
    if (lines[index].trim()) return index
  }
  return -1
}

function countIndent(line: string) {
  const match = line.match(/^ */)
  return match ? match[0].length : 0
}

function stripIndent(line: string, count: number) {
  let removed = 0
  let index = 0
  while (index < line.length && removed < count && line[index] === ' ') {
    index += 1
    removed += 1
  }
  return line.slice(index)
}

function isThematicBreak(line: string) {
  return /^\s{0,3}([-*_])(?:\s*\1){2,}\s*$/.test(line)
}

function isTableSeparator(line: string) {
  const cells = splitTableRow(line)
  return cells.length > 1 && cells.every(cell => /^:?-{3,}:?$/.test(cell))
}

function isTableStart(lines: string[], index: number) {
  const current = lines[index]
  const next = lines[index + 1]
  return Boolean(current && next && current.includes('|') && isTableSeparator(next))
}

function parseTableBlock(lines: string[], startIndex: number): MarkdownBlockResult | null {
  if (!isTableStart(lines, startIndex)) return null

  const rows: string[][] = [splitTableRow(lines[startIndex])]
  const aligns = parseTableAlignments(lines[startIndex + 1])
  let index = startIndex + 2

  while (index < lines.length) {
    if (!lines[index].trim() || !lines[index].includes('|')) break
    rows.push(splitTableRow(lines[index]))
    index += 1
  }

  return {
    html: renderTable(rows, aligns),
    nextIndex: index
  }
}

function splitTableRow(line: string) {
  const source = line.trim().replace(/^\|/, '').replace(/\|$/, '')
  const cells: string[] = []
  let current = ''
  let inCode = false

  for (let index = 0; index < source.length; index += 1) {
    const char = source[index]
    const next = source[index + 1]

    if (char === '\\' && next) {
      current += next
      index += 1
      continue
    }

    if (char === '`') {
      inCode = !inCode
      current += char
      continue
    }

    if (char === '|' && !inCode) {
      cells.push(current.trim())
      current = ''
      continue
    }

    current += char
  }

  cells.push(current.trim())
  return cells
}

function parseTableAlignments(line: string): TableAlignment[] {
  return splitTableRow(line).map(cell => {
    if (/^:-+:$/.test(cell)) return 'center'
    if (/^-+:$/.test(cell)) return 'right'
    if (/^:-+$/.test(cell)) return 'left'
    return ''
  })
}

function renderTable(rows: string[][], aligns: TableAlignment[] = []) {
  if (!rows.length) return ''
  const header = rows[0]
  const body = rows.slice(1)
  const getAlignAttr = (index: number) => aligns[index] ? ` style="text-align:${aligns[index]}"` : ''
  const thead = `<thead><tr>${header.map((cell, index) => `<th${getAlignAttr(index)}>${renderInline(cell)}</th>`).join('')}</tr></thead>`
  const tbody = body.length
    ? `<tbody>${body.map(row => `<tr>${header.map((_, column) => `<td${getAlignAttr(column)}>${renderInline(row[column] || '')}</td>`).join('')}</tr>`).join('')}</tbody>`
    : ''
  return `<div class="markdown-table-wrap"><table>${thead}${tbody}</table></div>`
}

function renderInline(value: string) {
  const slots: string[] = []
  const stash = (html: string) => {
    const token = `\uE000${slots.length}\uE001`
    slots.push(html)
    return token
  }

  let html = escapeHtml(value)
  html = html.replace(/(`+)([\s\S]*?[^`])\1/g, (_, ticks, code) => ticks.length ? stash(`<code>${code}</code>`) : code)
  html = html.replace(/!\[([^\]]*)\]\(([^)\s]+)(?:\s+"[^"]*")?\)/g, (_, alt, src) => {
    const safeSrc = sanitizeUrl(src)
    return safeSrc ? stash(`<img src="${safeSrc}" alt="${alt}">`) : alt
  })
  html = html.replace(/\[([^\]]+)]\(([^)\s]+)(?:\s+"[^"]*")?\)/g, (_, label, href) => {
    const safeHref = sanitizeUrl(href)
    return safeHref ? stash(`<a href="${safeHref}" target="_blank" rel="noopener noreferrer">${label}</a>`) : label
  })
  html = html.replace(/&lt;((?:https?:\/\/|mailto:)[^&]+)&gt;/g, (_, href) => {
    const safeHref = sanitizeUrl(href)
    return safeHref ? stash(`<a href="${safeHref}" target="_blank" rel="noopener noreferrer">${href}</a>`) : href
  })
  html = html.replace(/(^|[\s(（])((?:https?:\/\/|mailto:)[^\s<]+)/g, (_, prefix, href) => {
    const safeHref = sanitizeUrl(href)
    return safeHref ? `${prefix}${stash(`<a href="${safeHref}" target="_blank" rel="noopener noreferrer">${href}</a>`)}` : `${prefix}${href}`
  })
  html = html.replace(/~~(.+?)~~/g, '<del>$1</del>')
  html = html.replace(/(\*\*\*|___)(?=\S)([\s\S]*?\S)\1/g, '<strong><em>$2</em></strong>')
  html = html.replace(/(\*\*|__)(.+?)\1/g, '<strong>$2</strong>')
  html = html.replace(/(^|[^\w\\])\*(?=\S)([\s\S]*?\S)\*(?!\*)/g, '$1<em>$2</em>')
  html = html.replace(/(^|[^\w\\])_(?=\S)([\s\S]*?\S)_(?![\w_])/g, '$1<em>$2</em>')
  html = html.replace(/\\([\\`*_[\]{}()#+\-.!|>])/g, '$1')

  return html.replace(/\uE000(\d+)\uE001/g, (_, slot) => slots[Number(slot)] || '')
}

function sanitizeUrl(value: string) {
  const url = value.trim().replace(/^&quot;|&quot;$/g, '')
  if (/^(https?:\/\/|mailto:|#|\/(?!\/))/i.test(url)) {
    return escapeAttribute(url)
  }
  return ''
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function escapeAttribute(value: string) {
  return escapeHtml(value).replace(/`/g, '&#96;')
}

function escapeFenceForRegex(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function getCourseLabel(value?: string) {
  if (!value) return '未指定'
  return COURSE_CATEGORIES.find(item => item.value === value)?.label ?? value
}

function getSourceLabel(value?: string) {
  if (!value) return '自动'
  return sourceOptions.find(item => item.value === value)?.label ?? value
}

function getIntentLabel(value?: string) {
  const labels: Record<string, string> = {
    LEARN: '学习答疑',
    PLATFORM_QUERY: '平台查询',
    KNOWLEDGE_QA: '知识库问答',
    QUESTION_GEN: '生成题目',
    QUESTION_REVIEW: '改题优化',
    GENERAL: '普通对话'
  }
  return value ? labels[value] || value : '自动识别'
}

function formatTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 19)
}

function getStatusLabel(status?: string) {
  if (status === 'generating') return '生成中'
  if (status === 'interrupted') return '已中断'
  if (status === 'failed') return '失败'
  return ''
}
</script>

<style scoped>
.ai-chat-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  height: calc(100vh - 96px);
  min-height: 620px;
}

.session-panel,
.chat-panel {
  padding: 16px;
  overflow: hidden;
}

.session-panel {
  display: flex;
  flex-direction: column;
}

.session-header,
.chat-topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.session-header h2,
.chat-topbar h3 {
  margin: 0 0 6px;
  color: #e2e8f0;
  letter-spacing: 0;
}

.session-header h2 {
  font-size: 20px;
}

.chat-topbar h3 {
  font-size: 18px;
}

.session-header p,
.chat-topbar p {
  margin: 0;
  color: rgba(226, 232, 240, 0.6);
  font-size: 12px;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 2px;
}

.session-item {
  width: 100%;
  text-align: left;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.session-item:hover,
.session-item.active {
  border-color: rgba(64, 128, 255, 0.55);
  background: rgba(64, 128, 255, 0.12);
}

.session-title {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta,
.session-time {
  margin-top: 6px;
  color: rgba(226, 232, 240, 0.52);
  font-size: 12px;
}

.session-meta {
  display: flex;
  gap: 8px;
  overflow: hidden;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-settings {
  flex-shrink: 0;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0 0 12px;
}

.mode-group {
  width: 100%;
}

.message-window {
  flex: 1;
  min-height: 260px;
  overflow-y: auto;
  padding: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.message-row {
  display: flex;
}

.message-row.is-user {
  justify-content: flex-end;
}

.message-row.is-ai {
  justify-content: flex-start;
}

.message-bubble {
  width: min(78%, 820px);
  padding: 14px 16px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
}

.is-user .message-bubble {
  background: rgba(64, 128, 255, 0.16);
  border-color: rgba(64, 128, 255, 0.28);
}

.message-role {
  margin-bottom: 8px;
  color: #91b4ff;
  font-size: 12px;
  font-weight: 700;
}

.message-content {
  color: #e2e8f0;
  font-size: 14px;
  line-height: 1.8;
  white-space: normal;
  word-break: break-word;
}

.is-user .message-content {
  white-space: pre-wrap;
}

.markdown-body {
  color: #e2e8f0;
  overflow-wrap: anywhere;
}

.markdown-body :deep(*) {
  box-sizing: border-box;
}

.markdown-body :deep(p) {
  margin: 0 0 10px;
}

.markdown-body :deep(p:last-child),
.markdown-body :deep(ul:last-child),
.markdown-body :deep(ol:last-child),
.markdown-body :deep(pre:last-child),
.markdown-body :deep(blockquote:last-child),
.markdown-body :deep(.markdown-table-wrap:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  margin: 14px 0 8px;
  color: #f8fafc;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 0;
}

.markdown-body :deep(h1) {
  font-size: 22px;
}

.markdown-body :deep(h2) {
  font-size: 20px;
}

.markdown-body :deep(h3) {
  font-size: 18px;
}

.markdown-body :deep(h4),
.markdown-body :deep(h5),
.markdown-body :deep(h6) {
  font-size: 15px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0 0 10px;
  padding-left: 22px;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

.markdown-body :deep(.task-list-item) {
  list-style: none;
}

.markdown-body :deep(.task-list-row) {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.markdown-body :deep(.task-list-item input[type='checkbox']) {
  margin-top: 5px;
  accent-color: #4080ff;
}

.markdown-body :deep(.task-list-content) {
  min-width: 0;
  flex: 1;
}

.markdown-body :deep(.task-list-content > :first-child) {
  margin-top: 0;
}

.markdown-body :deep(.task-list-content > :last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(blockquote) {
  margin: 0 0 10px;
  padding: 8px 12px;
  border-left: 3px solid rgba(145, 180, 255, 0.75);
  background: rgba(145, 180, 255, 0.08);
  color: rgba(226, 232, 240, 0.86);
}

.markdown-body :deep(pre) {
  margin: 0 0 10px;
  padding: 12px;
  overflow-x: auto;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 8px;
  background: rgba(2, 6, 23, 0.72);
  line-height: 1.65;
}

.markdown-body :deep(code) {
  padding: 2px 5px;
  border-radius: 5px;
  background: rgba(15, 23, 42, 0.72);
  color: #bfdbfe;
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  white-space: break-spaces;
}

.markdown-body :deep(pre code) {
  display: block;
  padding: 0;
  background: transparent;
  color: #dbeafe;
  white-space: pre;
}

.markdown-body :deep(a) {
  color: #93c5fd;
  text-decoration: none;
}

.markdown-body :deep(a:hover) {
  text-decoration: underline;
}

.markdown-body :deep(img) {
  display: block;
  max-width: 100%;
  margin: 10px 0;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 10px;
}

.markdown-body :deep(hr) {
  height: 1px;
  margin: 14px 0;
  border: 0;
  background: rgba(148, 163, 184, 0.24);
}

.markdown-body :deep(.markdown-table-wrap) {
  max-width: 100%;
  margin: 0 0 10px;
  overflow-x: auto;
}

.markdown-body :deep(table) {
  width: max-content;
  min-width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  line-height: 1.55;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 8px 10px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  text-align: left;
  vertical-align: top;
}

.markdown-body :deep(th) {
  background: rgba(64, 128, 255, 0.16);
  color: #f8fafc;
  font-weight: 700;
}

.markdown-body :deep(td) {
  background: rgba(15, 23, 42, 0.26);
}

.message-time {
  margin-top: 8px;
  color: rgba(226, 232, 240, 0.45);
  font-size: 12px;
}

.message-status {
  margin-left: 8px;
  color: #91b4ff;
}

.message-source {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  color: rgba(226, 232, 240, 0.58);
  font-size: 12px;
  line-height: 1.5;
}

.message-source span {
  padding: 2px 6px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.06);
}

.message-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.stream-cursor {
  display: inline-block;
  margin-left: 2px;
  color: #91b4ff;
  animation: blink 1s steps(2, start) infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.question-form {
  flex-shrink: 0;
  margin-top: 14px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.knowledge-form {
  padding-top: 8px;
}

.action-form {
  padding-top: 8px;
}

.action-warnings {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
}

.upload-tip {
  margin-left: 12px;
  color: rgba(226, 232, 240, 0.55);
  font-size: 12px;
}

.knowledge-file-input {
  max-width: 100%;
  color: #dbeafe;
  font-size: 13px;
}

.knowledge-file-input::file-selector-button {
  margin-right: 12px;
  padding: 8px 14px;
  border: 1px solid rgba(64, 128, 255, 0.22);
  border-radius: 8px;
  background: rgba(64, 128, 255, 0.12);
  color: #bfdbfe;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.knowledge-file-input::file-selector-button:hover {
  background: rgba(64, 128, 255, 0.2);
  border-color: rgba(64, 128, 255, 0.38);
  color: #e2e8f0;
}

.knowledge-table {
  width: 100%;
}

.note-content {
  padding: 8px 12px;
  color: #e2e8f0;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 960px) {
  .ai-chat-page {
    grid-template-columns: 1fr;
    height: auto;
  }

  .session-panel {
    max-height: 320px;
  }

  .message-bubble {
    width: 100%;
  }
}
</style>
