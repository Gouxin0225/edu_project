<template>
  <div class="ai-chat-layout" :class="{ 'sidebar-collapsed': navCollapsed }">
    <!-- Sidebar -->
    <aside class="ai-sidebar" :class="{ 'is-hidden': navCollapsed }">
      <div class="sidebar-header">
        <div class="brand-text">
          <h3>智能问答</h3>
          <span>{{ roleLabel }}</span>
        </div>
        <el-button
          class="cyber-add-btn"
          type="primary"
          :icon="Plus"
          :loading="creating"
          @click="handleCreateSession"
        >
          新建对话
        </el-button>
      </div>

      <div class="session-nav" v-loading="sessionLoading">
        <div class="nav-label">最近会话</div>
        <el-empty v-if="sessions.length === 0 && !sessionLoading" description="无对话记录" :image-size="60" />
        
        <div class="session-list-scroll">
          <div
            v-for="item in sessions"
            :key="item.id"
            class="session-nav-item"
            :class="{ active: item.id === activeSessionId }"
            @click="selectSession(item.id)"
          >
            <el-icon class="item-icon"><ChatDotRound /></el-icon>
            <div class="item-main">
              <div class="item-title-row">
                <el-icon v-if="item.isTop" class="top-icon"><PriceTag /></el-icon>
                <span class="item-title">{{ item.title || '新会话' }}</span>
              </div>
              <span class="item-time">{{ formatTimeShort(item.lastMessageTime || item.createTime) }}</span>
            </div>
            
            <div @click.stop>
              <el-dropdown trigger="click" @command="(cmd: any) => handleSessionCommand(cmd, item)">
                <span class="item-more">
                  <el-icon><MoreFilled /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="'top'" :icon="item.isTop ? Close : Top">
                      {{ item.isTop ? '取消置顶' : '会话置顶' }}
                    </el-dropdown-item>
                    <el-dropdown-item :command="'rename'" :icon="EditPen">
                      重命名
                    </el-dropdown-item>
                    <el-dropdown-item :command="'export'" :icon="Download">
                      导出 Markdown
                    </el-dropdown-item>
                    <el-dropdown-item :command="'delete'" divided :icon="Delete" style="color: #f56c6c">
                      删除会话
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>

      <div class="sidebar-footer">
        <el-button class="view-all-btn" text @click="fetchSessionList">
          <span>查看全部会话</span>
          <el-icon><ArrowRight /></el-icon>
        </el-button>
        <el-button class="collapse-btn" text :icon="ArrowLeft" @click="navCollapsed = true" />
      </div>
    </aside>

    <!-- Main Content -->
    <main class="ai-main-container">
      <button v-if="navCollapsed" class="expand-trigger" @click="navCollapsed = false">
        <el-icon><ArrowRight /></el-icon>
      </button>

      <header class="chat-topbar">
        <div class="current-session-info">
          <h2 class="session-name">{{ activeSession?.title || '新会话' }}</h2>
          <p class="session-desc">AI 助手为你解答学习中的问题</p>
        </div>
        
        <div class="topbar-tools">
          <el-button v-if="canManageKnowledge" :icon="Files" @click="openKnowledgeDialog">
            知识库
          </el-button>
          <el-button v-if="canCreateReviewNoteAction" :icon="Files" @click="openReviewNotesDialog">
            收藏
          </el-button>
          <el-button :icon="Delete" :disabled="!activeSessionId" @click="handleDeleteSession" class="danger-tool">
            删除
          </el-button>
        </div>
      </header>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="chat-config-bar"
      >
        <div class="config-item wide">
          <label>课程方向</label>
          <el-form-item prop="courseCategory">
            <el-select
              v-model="form.courseCategory"
              placeholder="可选"
              clearable
              size="small"
              @change="handleCourseChange"
            >
              <el-option v-for="item in COURSE_CATEGORIES" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <div class="config-item wide">
          <label>知识点</label>
          <el-form-item prop="knowledgePoint">
            <el-select
              v-model="form.knowledgePoint"
              placeholder="可选"
              filterable
              allow-create
              clearable
              size="small"
            >
              <el-option v-for="item in knowledgeOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </div>
        <div class="config-divider"></div>
        <div class="config-item">
          <label>模式</label>
          <el-form-item prop="mode">
            <el-radio-group v-model="form.mode" size="small">
              <el-radio-button v-for="item in modeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>
      </el-form>

      <div ref="messageWindowRef" class="messages-viewport" v-loading="messageLoading">
        <div class="message-inner-container">
          <!-- Empty State -->
          <el-empty
            v-if="!activeSessionId && !sessionLoading"
            description="开启一段新的对话"
            class="centered-empty"
          >
            <template #image>
              <div class="welcome-icon">AI</div>
            </template>
            <div class="welcome-prompts">
              <button
                v-for="item in quickPrompts"
                :key="item"
                class="quick-start-card"
                @click="submitQuickPrompt(item)"
              >
                {{ item }}
              </button>
            </div>
          </el-empty>

          <!-- Chat History -->
          <div v-else class="message-stream">
            <div
              v-for="item in messages"
              :key="item.localId || item.id"
              class="message-bundle"
              :class="item.messageRole === 'USER' ? 'user-bundle' : 'ai-bundle'"
            >
              <div class="avatar-col">
                <div class="user-avatar" v-if="item.messageRole === 'USER'">我</div>
                <div class="ai-avatar" v-else>
                  <img :src="AI_AVATAR_URL" alt="AI 助手" @error="handleAiAvatarError" />
                </div>
              </div>
              
              <div class="content-col">
                <div class="bubble-header">
                  <span class="role-name">{{ item.messageRole === 'USER' ? '你' : 'AI 助手' }}</span>
                  <span class="bubble-time">{{ formatTimeShort(item.createTime) }}</span>
                </div>
                
                <div class="message-bubble">
                  <template v-if="item.messageRole === 'ASSISTANT'">
                    <div class="markdown-body" v-html="renderMarkdown(messageDisplayText(item))"></div>
                  </template>
                  <template v-else>
                    <div class="text-content">{{ messageDisplayText(item) }}</div>
                  </template>
                  <span v-if="item.status === 'generating'" class="generating-cursor"></span>
                </div>

                <div v-if="item.messageRole === 'ASSISTANT' && item.sourceSummary" class="source-metadata">
                  <el-icon><InfoFilled /></el-icon>
                  <span>基于 {{ getIntentLabel(item.intent) }}：{{ item.sourceSummary }}</span>
                </div>

                <div v-if="canUseMessageAction(item)" class="bubble-actions">
                  <el-button v-if="canCreateQuestionAction" size="small" link @click="openActionPreview(item, 'CREATE_QUESTION')">存入题库</el-button>
                  <el-button v-if="canCreateReviewNoteAction" size="small" link @click="openActionPreview(item, 'CREATE_REVIEW_NOTE')">加入笔记</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <footer class="ai-input-zone">
        <div class="input-wrapper">
          <el-form ref="questionFormRef" :model="form" :rules="questionRules">
            <div class="main-input-box">
              <el-form-item prop="question">
                <el-input
                  v-model="form.question"
                  type="textarea"
                  :autosize="{ minRows: 2, maxRows: 8 }"
                  placeholder="询问任何问题 (Ctrl + Enter 发送)..."
                  @keydown.ctrl.enter.prevent="handleSubmit"
                />
              </el-form-item>
              <div class="input-actions">
                <el-button v-if="sending" type="danger" @click="handleCancelGeneration">停止生成</el-button>
                <el-button v-else type="primary" :disabled="!form.question.trim()" @click="handleSubmit" :icon="Promotion">发送</el-button>
              </div>
            </div>
          </el-form>
        </div>
      </footer>
    </main>

    <!-- Dialogs -->
    <el-dialog v-model="actionDialog" :title="actionDialogTitle" width="760px">
      <!-- ... Action Preview Form ... -->
      <template #footer>
        <el-button @click="actionDialog = false">取消</el-button>
        <el-button type="primary" :loading="actionSaving" @click="handleConfirmAction">确认保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="knowledgeDialog" title="知识库管理" width="800px">
      <!-- ... Knowledge Base Management ... -->
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, ArrowRight, ChatDotRound, Close, Delete, Download,
  EditPen, Files, InfoFilled, MoreFilled, Plus, PriceTag, Promotion, Refresh, Top
} from '@element-plus/icons-vue'
import { COURSE_CATEGORIES } from '@/api/question'
import {
  createAiChatSession, cancelAiChatStream, deleteAiChatSession,
  getAiChatSessionMessages, getAiChatSessions, regenerateAiChatSessionMessage,
  streamAiChatSessionMessage, updateAiChatSession,
  type AiChatHistoryMessage, type AiChatMode, type AiChatResponseSource,
  type AiChatSession, type AiChatStreamEvent
} from '@/api/aiChat'
import {
  createKnowledgeDocument, deleteKnowledgeDocument, getKnowledgeDocuments, uploadKnowledgeDocument, type AiKnowledgeDocument
} from '@/api/aiKnowledge'
import {
  confirmAiResultAction, getStudentReviewNotes, previewAiResultAction, type AiResultActionType, type StudentReviewNote
} from '@/api/aiAction'
import { useUserStore } from '@/stores/user'
import MarkdownIt from 'markdown-it'

// Markdown and Logic
const md = new MarkdownIt({ html: false, breaks: true, linkify: true })
const AI_AVATAR_URL = '/opt/ai.png'
const userStore = useUserStore()
const sessions = ref<AiChatSession[]>([])
const messages = ref<any[]>([])
const activeSessionId = ref<number | null>(null)
const sessionLoading = ref(false)
const messageLoading = ref(false)
const sending = ref(false)
const creating = ref(false)
const navCollapsed = ref(false)
const messageWindowRef = ref<HTMLElement>()

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
  { label: '教案', value: 'LESSON_PLAN' },
  { label: '出题', value: 'QUESTION' }
]

const rules = { mode: [{ required: true }] }
const questionRules = { question: [{ required: true, message: '请输入内容' }] }

const roleLabel = computed(() => userStore.userInfo?.role === 'STUDENT' ? '学生模式' : '教师模式')
const activeSession = computed(() => sessions.value.find(s => s.id === activeSessionId.value))
const knowledgeOptions = computed(() => []) // Simplified for this implementation
const canManageKnowledge = computed(() => userStore.userInfo?.role !== 'STUDENT')
const canCreateReviewNoteAction = computed(() => userStore.userInfo?.role === 'STUDENT')
const canCreateQuestionAction = computed(() => userStore.userInfo?.role !== 'STUDENT')
const quickPrompts = computed(() => ['帮我总结今天的学习重点', '如何高效复习错题', '给我出一个Python练习题'])

onMounted(() => fetchSessionList())

function handleAiAvatarError(event: Event) {
  const image = event.target
  if (image instanceof HTMLImageElement) {
    image.style.display = 'none'
  }
}

async function fetchSessionList() {
  sessionLoading.value = true
  try {
    const res = await getAiChatSessions()
    sessions.value = res.data
    if (!activeSessionId.value && sessions.value.length > 0) selectSession(sessions.value[0].id)
  } finally { sessionLoading.value = false }
}

async function handleCreateSession() {
  creating.value = true
  try {
    const res = await createAiChatSession()
    sessions.value.unshift(res.data)
    selectSession(res.data.id)
  } finally { creating.value = false }
}

async function selectSession(id: number) {
  if (activeSessionId.value === id) return
  activeSessionId.value = id
  const session = sessions.value.find(s => s.id === id)
  if (session) {
    form.courseCategory = session.courseCategory || ''
    form.knowledgePoint = session.knowledgePoint || ''
  }
  fetchMessages(id)
}

async function fetchMessages(id: number) {
  messageLoading.value = true
  try {
    const res = await getAiChatSessionMessages(id)
    messages.value = res.data
    scrollToBottom()
  } finally { messageLoading.value = false }
}

function handleSessionCommand(command: string, item: AiChatSession) {
  if (command === 'top') handleToggleTop(item)
  else if (command === 'rename') handleRenameSessionById(item)
  else if (command === 'export') handleExportMarkdownById(item)
  else if (command === 'delete') handleDeleteSessionById(item.id)
}

async function handleToggleTop(session: AiChatSession) {
  await updateAiChatSession(session.id, { isTop: !session.isTop })
  ElMessage.success(session.isTop ? '已取消置顶' : '已置顶')
  fetchSessionList()
}

async function handleRenameSessionById(session: AiChatSession) {
  try {
    const { value } = await ElMessageBox.prompt('请输入新标题', '重命名', { inputValue: session.title })
    if (value) {
      await updateAiChatSession(session.id, { title: value })
      session.title = value
      ElMessage.success('已修改')
    }
  } catch {}
}

async function handleExportMarkdownById(session: AiChatSession) {
  const res = await getAiChatSessionMessages(session.id)
  let content = `# ${session.title}\n\n`
  res.data.forEach((m: any) => {
    content += `### ${m.messageRole === 'USER' ? '用户' : 'AI'}\n${m.content}\n\n`
  })
  const blob = new Blob([content], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${session.title}.md`
  a.click()
}

async function handleDeleteSessionById(id: number) {
  try {
    await ElMessageBox.confirm('确定删除？')
    await deleteAiChatSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (activeSessionId.value === id) {
      activeSessionId.value = null
      messages.value = []
    }
    ElMessage.success('已删除')
  } catch {}
}

async function handleDeleteSession() {
  if (activeSessionId.value) handleDeleteSessionById(activeSessionId.value)
}

async function handleSubmit() {
  if (!form.question.trim() || sending.value) return
  if (!activeSessionId.value) await handleCreateSession()
  if (!activeSessionId.value) return

  const question = form.question
  form.question = ''
  const now = Date.now()
  const userLocalId = `user-${now}`
  const aiLocalId = `assistant-${now}`
  const userMsg = { localId: userLocalId, messageRole: 'USER', content: question, createTime: new Date().toISOString() }
  const aiMsg = { localId: aiLocalId, messageRole: 'ASSISTANT', content: '', status: 'generating', createTime: new Date().toISOString() }
  messages.value.push(userMsg, aiMsg)
  scrollToBottom()
  
  sending.value = true
  try {
    await streamAiChatSessionMessage(activeSessionId.value, { question, mode: form.mode }, event => {
      if (event.event === 'start') {
        updateLocalMessage(userLocalId, {
          id: event.data.userMessageId,
          sessionId: event.data.sessionId
        })
        updateLocalMessage(aiLocalId, {
          id: event.data.assistantMessageId,
          sessionId: event.data.sessionId,
          intent: event.data.intent,
          responseSource: event.data.responseSource,
          sourceSummary: event.data.sourceSummary,
          status: event.data.status || 'generating'
        })
      } else if (event.event === 'delta') {
        appendLocalMessageContent(aiLocalId, event.data.content || '')
        scrollToBottom()
      } else if (event.event === 'done') {
        updateLocalMessage(aiLocalId, {
          id: event.data.assistantMessageId,
          content: event.data.answer || getLocalMessageContent(aiLocalId),
          status: event.data.status || 'finished',
          createTime: event.data.createTime || new Date().toISOString(),
          mode: event.data.mode,
          intent: event.data.intent,
          responseSource: event.data.responseSource,
          sourceSummary: event.data.sourceSummary,
          model: event.data.model
        })
        updateActiveSessionFromStream(event.data)
      } else if (event.event === 'interrupted') {
        updateLocalMessage(aiLocalId, {
          id: event.data.assistantMessageId,
          content: event.data.answer || getLocalMessageContent(aiLocalId),
          status: event.data.status || 'interrupted'
        })
      } else if (event.event === 'error') {
        updateLocalMessage(aiLocalId, {
          id: event.data.assistantMessageId,
          status: event.data.status || 'failed'
        })
        ElMessage.error(event.data.message || '发送失败')
      }
    })
  } catch (err) {
    ElMessage.error('发送失败')
    updateLocalMessage(aiLocalId, { status: 'failed' })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function findLocalMessageIndex(localId: string) {
  return messages.value.findIndex(item => item.localId === localId)
}

function getLocalMessageContent(localId: string) {
  const index = findLocalMessageIndex(localId)
  return index >= 0 ? messages.value[index].content || '' : ''
}

function updateLocalMessage(localId: string, patch: Record<string, any>) {
  const index = findLocalMessageIndex(localId)
  if (index < 0) return
  messages.value[index] = { ...messages.value[index], ...patch }
}

function appendLocalMessageContent(localId: string, content: string) {
  if (!content) return
  const index = findLocalMessageIndex(localId)
  if (index < 0) return
  const current = messages.value[index]
  messages.value[index] = { ...current, content: `${current.content || ''}${content}` }
}

function updateActiveSessionFromStream(data: any) {
  if (!activeSessionId.value) return
  const index = sessions.value.findIndex(item => item.id === activeSessionId.value)
  if (index < 0) return
  sessions.value[index] = {
    ...sessions.value[index],
    title: data.sessionTitle || sessions.value[index].title,
    courseCategory: data.courseCategory || sessions.value[index].courseCategory,
    knowledgePoint: data.knowledgePoint || sessions.value[index].knowledgePoint,
    lastMessageTime: data.createTime || new Date().toISOString()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messageWindowRef.value) messageWindowRef.value.scrollTop = messageWindowRef.value.scrollHeight
  })
}

function renderMarkdown(c: string) { return md.render(c || '') }
function messageDisplayText(m: any) { return m.content || (m.status === 'generating' ? '思考中...' : '') }
function formatTimeShort(t: string) { return t ? new Date(t).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '' }
function getIntentLabel(i: any) { return i || '常规对话' }
function handleCourseChange() {}
function canUseMessageAction(m: any) { return m.messageRole === 'ASSISTANT' && m.status !== 'generating' }
function submitQuickPrompt(p: string) { form.question = p; handleSubmit() }

// Placeholder for missing dialog logics - can be expanded as needed
const actionDialog = ref(false)
const knowledgeDialog = ref(false)
const actionDialogTitle = ref('')
const canManageKnowledgeLogic = () => {}
</script>

<style scoped>
.ai-chat-layout {
  display: flex;
  height: calc(100vh - 64px);
  background: var(--bg-base);
  color: var(--text-primary);
  overflow: hidden;
}

.ai-sidebar {
  width: 300px;
  background: var(--bg-surface);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
}

.ai-sidebar.is-hidden { width: 0; overflow: hidden; }

.sidebar-header { padding: 20px; border-bottom: 1px solid var(--border); }
.brand-text h3 { margin: 0; font-size: 18px; color: var(--text-primary); }
.brand-text span { font-size: 12px; color: var(--text-secondary); }
.cyber-add-btn { width: 100%; margin-top: 15px; }

.session-nav { flex: 1; overflow: hidden; display: flex; flex-direction: column; }
.nav-label { padding: 15px 20px 10px; font-size: 12px; color: var(--text-muted); font-weight: bold; }
.session-list-scroll { flex: 1; overflow-y: auto; padding: 0 10px; }

.session-nav-item {
  display: flex; align-items: center; gap: 12px; padding: 12px;
  margin-bottom: 5px; border-radius: 8px; cursor: pointer; transition: 0.2s;
  color: var(--text-secondary);
}
.session-nav-item:hover { background: var(--surface-hover); }
.session-nav-item.active { background: var(--primary-dim); color: var(--primary-light); }

.item-main { flex: 1; min-width: 0; }
.item-title { font-size: 14px; font-weight: 500; color: var(--text-primary); display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-time { font-size: 11px; color: var(--text-muted); }

.item-more { opacity: 0; transition: 0.2s; padding: 4px; border-radius: 4px; }
.session-nav-item:hover .item-more, .session-nav-item.active .item-more { opacity: 1; }
.item-more:hover { background: var(--surface-hover); }

.ai-main-container { flex: 1; display: flex; flex-direction: column; min-width: 0; background: var(--bg-surface); }
.chat-topbar { height: 64px; padding: 0 25px; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; }
.session-name { margin: 0; font-size: 18px; }
.session-desc { margin: 0; font-size: 12px; color: var(--text-secondary); }

.chat-config-bar { padding: 10px 25px; display: flex; gap: 20px; border-bottom: 1px solid var(--border); background: var(--bg-elevated); }
.config-item { display: flex; align-items: center; gap: 8px; font-size: 13px; }

.messages-viewport { flex: 1; overflow-y: auto; padding: 30px 20px; background: var(--bg-base); }
.message-inner-container { max-width: 800px; margin: 0 auto; }
.message-stream { display: flex; flex-direction: column; gap: 25px; }

.message-bundle { display: flex; gap: 15px; }
.user-bundle { flex-direction: row-reverse; }
.user-avatar, .ai-avatar { width: 35px; height: 35px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #fff; flex-shrink: 0; overflow: hidden; }
.user-avatar { background: var(--primary); }
.ai-avatar { background: var(--surface-muted); border: 1px solid var(--border); }
.ai-avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }

.message-bubble { padding: 12px 16px; border-radius: 12px; font-size: 14px; line-height: 1.6; max-width: 85%; }
.user-bundle .message-bubble { background: var(--primary); color: #fff; }
.ai-bundle .message-bubble { background: var(--bg-elevated); color: var(--text-primary); border: 1px solid var(--border); }

.ai-input-zone { padding: 20px; border-top: 1px solid var(--border); background: var(--bg-surface); }
.main-input-box { border: 1px solid var(--border); border-radius: 8px; padding: 10px; display: flex; flex-direction: column; gap: 10px; background: var(--bg-elevated); }
.input-actions { display: flex; justify-content: flex-end; }

.top-icon { color: var(--color-warning); font-size: 12px; margin-right: 4px; }
</style>
