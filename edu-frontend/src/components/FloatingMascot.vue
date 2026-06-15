<template>
  <div
    ref="orbRef"
    class="floating-orb"
    :class="{ 'is-raised': shouldRaise, 'is-dragging': dragging, 'is-open': panelOpen }"
    :style="orbStyle"
    role="button"
    :aria-expanded="panelOpen"
    aria-label="AI 快捷助手悬浮球"
    tabindex="0"
    @pointerdown="handlePointerDown"
    @click="handleOrbClick"
    @keydown.enter.prevent="togglePanel"
    @keydown.space.prevent="togglePanel"
  >
    <div class="orb-halo halo-one"></div>
    <div class="orb-halo halo-two"></div>
    <div class="orb-shell">
      <div class="orb-grid"></div>
      <div class="orb-core">
        <div class="core-ring"></div>
        <div class="core-face">
          <span class="eye eye-left"></span>
          <span class="eye eye-right"></span>
          <span class="signal-mouth"></span>
        </div>
        <span class="ai-mark">AI</span>
      </div>
      <span class="spark spark-a"></span>
      <span class="spark spark-b"></span>
      <span class="spark spark-c"></span>
      <span class="spark spark-d"></span>
    </div>

    <transition-group name="encourage-drop" tag="div" class="encourage-layer">
      <div
        v-for="item in encouragements"
        :key="item.id"
        class="encourage-chip"
        :style="{ '--drift': item.drift + 'px', '--rotate': item.rotate + 'deg' }"
      >
        {{ item.text }}
      </div>
    </transition-group>

    <transition name="assistant-panel">
      <section
        v-if="panelOpen"
        class="assistant-panel"
        aria-label="AI 快捷助手"
        @pointerdown.stop
        @click.stop
      >
        <div class="panel-header">
          <div>
            <strong>{{ panelTitle }}</strong>
            <span>{{ panelSubtitle }}</span>
          </div>
          <button class="panel-close" type="button" aria-label="关闭快捷助手" @click="panelOpen = false">×</button>
        </div>

        <div class="panel-status" :class="statusTone">
          <span class="status-dot"></span>
          <span>{{ statusText }}</span>
        </div>

        <div class="quick-actions">
          <button
            v-for="item in quickActions"
            :key="item.path"
            class="quick-action"
            type="button"
            @click="navigateTo(item.path)"
          >
            <span class="action-icon">{{ item.icon }}</span>
            <span class="action-copy">
              <strong>{{ item.title }}</strong>
              <small>{{ item.desc }}</small>
            </span>
            <span v-if="item.badge" class="action-badge">{{ item.badge }}</span>
          </button>
        </div>

        <div class="panel-tools">
          <button type="button" @click="dropEncouragement">掉落鼓励</button>
          <button type="button" @click="resetPosition">回到右下</button>
        </div>
      </section>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const orbRef = ref<HTMLElement | null>(null)
const dragging = ref(false)
const movedDuringPointer = ref(false)
const panelOpen = ref(false)
const encouragements = ref<Array<{ id: number; text: string; drift: number; rotate: number }>>([])

const ORB_SIZE = 92
const VIEWPORT_PADDING = 12
const STORAGE_KEY = 'edu-floating-orb-position'

const raisedRouteNames = new Set([
  'TeacherExamCreate',
  'TeacherExamEdit',
  'StudentExamTaking'
])

const shouldRaise = computed(() => raisedRouteNames.has(String(route.name || '')))
const isAuthenticated = computed(() => Boolean(userStore.token && userStore.userInfo?.role))
const role = computed(() => isAuthenticated.value ? userStore.userInfo?.role || 'STUDENT' : 'GUEST')
const realName = computed(() => isAuthenticated.value ? userStore.userInfo?.realName || '同学' : '访客')

const panelTitle = computed(() => `${realName.value}的 AI 助手`)
const panelSubtitle = computed(() => roleSubtitleMap[role.value] || '快捷进入常用功能')
const statusTone = computed(() => {
  if (!isAuthenticated.value) return 'tone-guest'
  return role.value === 'STUDENT' ? 'tone-blue' : role.value === 'ASSISTANT' ? 'tone-pink' : 'tone-cyan'
})
const statusText = computed(() => {
  if (!isAuthenticated.value) return '请先登录后使用平台功能'
  if (pendingSummary.loading) return '正在同步你的待办状态'
  if (pendingSummary.error) return '待办同步失败，核心功能仍可使用'
  if (pendingSummary.count !== null && pendingSummary.count > 0) {
    return `${pendingSummary.label} ${pendingSummary.count} 项，建议优先处理`
  }
  if (role.value === 'STUDENT') return '学习状态正常，可继续推进'
  if (role.value === 'ASSISTANT') return '已开启班级运营辅助模式'
  if (role.value === 'TEACHER') return '已开启教学辅助模式'
  return '已开启平台管理辅助模式'
})

interface QuickAction {
  title: string
  desc: string
  path: string
  icon: string
  badge?: string
}

const roleSubtitleMap: Record<string, string> = {
  GUEST: '登录后开启完整功能',
  ADMIN: '平台管理快捷入口',
  TEACHER: '教学工作快捷入口',
  ASSISTANT: '班级运营快捷入口',
  STUDENT: '学习任务快捷入口'
}

const actionMap: Record<string, QuickAction[]> = {
  GUEST: [
    { title: '登录平台', desc: '进入后使用 AI 问答和任务功能', path: '/login', icon: '登' },
    { title: '学生注册', desc: '没有账号时先完成注册', path: '/register', icon: '注' }
  ],
  ADMIN: [
    { title: 'AI 问答', desc: '查询平台数据和辅助分析', path: '/admin/ai-chat', icon: 'AI' },
    { title: '用户管理', desc: '账号、角色和班级关系', path: '/admin/users', icon: '人' },
    { title: '班级管理', desc: '查看班级与教师配置', path: '/admin/classes', icon: '班' },
    { title: '数据统计', desc: '查看平台运行概览', path: '/admin/statistics', icon: '图' }
  ],
  TEACHER: [
    { title: 'AI 问答', desc: '带平台数据辅助教学', path: '/teacher/ai-chat', icon: 'AI' },
    { title: '考试管理', desc: '查看考试、成绩与批改', path: '/teacher/exams', icon: '考' },
    { title: 'AI 大纲出卷', desc: '按大纲生成试题', path: '/teacher/outline-exam', icon: '卷' },
    { title: '作业管理', desc: '发布和查看作业反馈', path: '/teacher/homework', icon: '业' }
  ],
  ASSISTANT: [
    { title: 'AI 问答', desc: '查询班级、考试和学生', path: '/assistant/ai-chat', icon: 'AI' },
    { title: '待办中心', desc: '处理催交和回访任务', path: '/assistant/todo-center', icon: '办' },
    { title: '风险中心', desc: '查看低分和异常学生', path: '/assistant/risk-center', icon: '警' },
    { title: '考试跟踪', desc: '跟进考试参加与评分', path: '/assistant/exams', icon: '考' }
  ],
  STUDENT: [
    { title: 'AI 问答', desc: '提问知识点和平台任务', path: '/student/ai-chat', icon: 'AI' },
    { title: '我的作业', desc: '查看待完成和反馈', path: '/student/homework', icon: '业' },
    { title: '我的考试', desc: '进入考试和查看安排', path: '/student/exams', icon: '考' },
    { title: '错题复习', desc: '针对错题查漏补缺', path: '/student/mistakes', icon: '错' }
  ]
}

const pendingSummary = reactive({
  count: null as number | null,
  label: '',
  loading: false,
  error: false
})

const quickActions = computed(() => {
  const actions = actionMap[role.value] || actionMap.GUEST
  if (!isAuthenticated.value || pendingSummary.count === null || pendingSummary.count <= 0) {
    return actions
  }
  const badge = pendingSummary.count > 99 ? '99+' : String(pendingSummary.count)
  return actions.map((item, index) => {
    if (index !== 0 && !isTodoAction(item.path)) {
      return item
    }
    return { ...item, badge }
  })
})

const position = reactive({
  left: 0,
  top: 0
})

const dragState = reactive({
  pointerId: 0,
  startX: 0,
  startY: 0,
  startLeft: 0,
  startTop: 0
})

const phrases = [
  '稳住节奏，答案会越来越清晰',
  '先拆小步，再解决大题',
  '今天多理解一点，明天就轻松一点',
  '保持专注，你正在靠近目标',
  '思路卡住时，换个角度再试一次',
  '把疑问问出来，就是进步的开始',
  '认真复盘，比盲目刷题更有效',
  '每一次修正，都会让能力更扎实',
  '先抓关键概念，再处理细节',
  '继续推进，复杂问题会被拆开'
]

let phraseId = 0
let autoDropTimer: number | undefined

const orbStyle = computed(() => ({
  left: `${position.left}px`,
  top: `${position.top}px`
}))

onMounted(() => {
  restorePosition()
  window.addEventListener('resize', handleResize)
  autoDropTimer = window.setInterval(() => {
    if (!dragging.value && document.visibilityState === 'visible') {
      dropEncouragement()
    }
  }, 18000)
})

watch([role, isAuthenticated], () => {
  pendingSummary.count = null
  pendingSummary.label = ''
  pendingSummary.error = false
  if (panelOpen.value) {
    fetchPendingSummary()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', handlePointerUp)
  if (autoDropTimer) {
    window.clearInterval(autoDropTimer)
  }
})

function handlePointerDown(event: PointerEvent) {
  if (event.button !== 0 && event.pointerType === 'mouse') {
    return
  }

  dragging.value = true
  movedDuringPointer.value = false
  dragState.pointerId = event.pointerId
  dragState.startX = event.clientX
  dragState.startY = event.clientY
  dragState.startLeft = position.left
  dragState.startTop = position.top
  orbRef.value?.setPointerCapture(event.pointerId)
  window.addEventListener('pointermove', handlePointerMove)
  window.addEventListener('pointerup', handlePointerUp)
}

function handlePointerMove(event: PointerEvent) {
  if (!dragging.value || event.pointerId !== dragState.pointerId) {
    return
  }

  const deltaX = event.clientX - dragState.startX
  const deltaY = event.clientY - dragState.startY
  if (Math.abs(deltaX) > 4 || Math.abs(deltaY) > 4) {
    movedDuringPointer.value = true
  }

  const nextLeft = dragState.startLeft + deltaX
  const nextTop = dragState.startTop + deltaY
  setClampedPosition(nextLeft, nextTop)
}

function handlePointerUp(event: PointerEvent) {
  if (event.pointerId !== dragState.pointerId) {
    return
  }

  dragging.value = false
  orbRef.value?.releasePointerCapture(event.pointerId)
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerup', handlePointerUp)
  savePosition()

  nextTick(() => {
    window.setTimeout(() => {
      movedDuringPointer.value = false
    }, 0)
  })
}

function handleOrbClick() {
  if (movedDuringPointer.value) {
    return
  }
  togglePanel()
}

function togglePanel() {
  panelOpen.value = !panelOpen.value
  if (panelOpen.value) {
    dropEncouragement()
    fetchPendingSummary()
  }
}

function navigateTo(path: string) {
  panelOpen.value = false
  if (!isAuthenticated.value && !isPublicPath(path)) {
    router.push('/login')
    return
  }
  router.push(path)
}

function isPublicPath(path: string) {
  return path === '/login' || path === '/register'
}

function isTodoAction(path: string) {
  return path.includes('todo') || path.includes('homework') || path.includes('exam') || path.includes('statistics')
}

async function fetchPendingSummary() {
  if (!isAuthenticated.value || pendingSummary.loading) {
    return
  }
  pendingSummary.loading = true
  pendingSummary.error = false
  try {
    if (role.value === 'STUDENT') {
      const res = await request.get<any, { code: number; data: { pendingTasks?: unknown[] } }>('/api/student/dashboard')
      pendingSummary.count = res.data?.pendingTasks?.length || 0
      pendingSummary.label = '待完成任务'
    } else if (role.value === 'ASSISTANT') {
      const res = await request.get<any, { code: number; data: unknown[] }>('/api/assistant/operations/todos')
      pendingSummary.count = res.data?.length || 0
      pendingSummary.label = '运营待办'
    } else if (role.value === 'TEACHER') {
      const res = await request.get<any, { code: number; data: { overview?: { pendingGradeCount?: number } } }>('/api/teacher/statistics')
      pendingSummary.count = Number(res.data?.overview?.pendingGradeCount || 0)
      pendingSummary.label = '待批改'
    } else if (role.value === 'ADMIN') {
      const res = await request.get<any, { code: number; data: { classes?: Array<{ homeworkSummary?: { pendingCount?: number; pendingGradeCount?: number }; examSummary?: { pendingCount?: number; pendingGradeCount?: number }; surveys?: Array<{ pendingCount?: number }> }> } }>('/api/admin/statistics/classes')
      pendingSummary.count = (res.data?.classes || []).reduce((sum, item) => {
        const homework = Number(item.homeworkSummary?.pendingCount || 0) + Number(item.homeworkSummary?.pendingGradeCount || 0)
        const exam = Number(item.examSummary?.pendingCount || 0) + Number(item.examSummary?.pendingGradeCount || 0)
        const survey = (item.surveys || []).reduce((total, surveyItem) => total + Number(surveyItem.pendingCount || 0), 0)
        return sum + homework + exam + survey
      }, 0)
      pendingSummary.label = '平台待处理'
    } else {
      pendingSummary.count = 0
      pendingSummary.label = ''
    }
  } catch {
    pendingSummary.count = null
    pendingSummary.error = true
  } finally {
    pendingSummary.loading = false
  }
}

function dropEncouragement() {
  const text = phrases[Math.floor(Math.random() * phrases.length)]
  const item = {
    id: ++phraseId,
    text,
    drift: Math.round(Math.random() * 72 - 36),
    rotate: Math.round(Math.random() * 14 - 7)
  }
  encouragements.value = [...encouragements.value.slice(-3), item]
  window.setTimeout(() => {
    encouragements.value = encouragements.value.filter(current => current.id !== item.id)
  }, 3600)
}

function restorePosition() {
  const fallbackLeft = window.innerWidth - ORB_SIZE - 22
  const fallbackTop = window.innerHeight - ORB_SIZE - (shouldRaise.value ? 106 : 24)
  try {
    const saved = window.localStorage.getItem(STORAGE_KEY)
    if (saved) {
      const parsed = JSON.parse(saved) as { left?: number; top?: number }
      if (typeof parsed.left === 'number' && typeof parsed.top === 'number') {
        setClampedPosition(parsed.left, parsed.top)
        return
      }
    }
  } catch {
    // 本地存储不可用时使用默认位置。
  }
  setClampedPosition(fallbackLeft, fallbackTop)
}

function savePosition() {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ left: position.left, top: position.top }))
  } catch {
    // 本地存储不可用不影响悬浮球使用。
  }
}

function handleResize() {
  setClampedPosition(position.left, position.top)
}

function resetPosition() {
  const fallbackLeft = window.innerWidth - ORB_SIZE - 22
  const fallbackTop = window.innerHeight - ORB_SIZE - (shouldRaise.value ? 106 : 24)
  setClampedPosition(fallbackLeft, fallbackTop)
  savePosition()
}

function setClampedPosition(left: number, top: number) {
  const maxLeft = Math.max(VIEWPORT_PADDING, window.innerWidth - ORB_SIZE - VIEWPORT_PADDING)
  const maxTop = Math.max(VIEWPORT_PADDING, window.innerHeight - ORB_SIZE - VIEWPORT_PADDING)
  position.left = Math.min(Math.max(VIEWPORT_PADDING, left), maxLeft)
  position.top = Math.min(Math.max(VIEWPORT_PADDING, top), maxTop)
}
</script>

<style scoped>
.floating-orb {
  position: fixed;
  width: 92px;
  height: 92px;
  z-index: 100;
  cursor: grab;
  touch-action: none;
  user-select: none;
  transform: translateZ(0);
  transition: filter 0.18s ease;
}

.floating-orb:focus-visible {
  outline: 2px solid rgba(103, 232, 249, 0.95);
  outline-offset: 6px;
  border-radius: 50%;
}

.floating-orb.is-dragging {
  cursor: grabbing;
  filter: saturate(1.2) brightness(1.08);
}

.orb-shell,
.orb-halo,
.encourage-layer {
  position: absolute;
  inset: 0;
}

.orb-shell {
  border-radius: 50%;
  overflow: hidden;
  background:
    radial-gradient(circle at 38% 30%, rgba(255, 255, 255, 0.95) 0 5%, rgba(103, 232, 249, 0.68) 12%, transparent 28%),
    radial-gradient(circle at 60% 64%, rgba(255, 16, 240, 0.52), transparent 38%),
    linear-gradient(135deg, rgba(12, 22, 44, 0.94), rgba(18, 36, 70, 0.92) 46%, rgba(8, 12, 26, 0.96));
  border: 1px solid rgba(137, 247, 255, 0.72);
  box-shadow:
    0 0 20px rgba(103, 232, 249, 0.55),
    0 0 42px rgba(255, 16, 240, 0.34),
    inset 0 0 22px rgba(103, 232, 249, 0.28),
    inset 0 -20px 36px rgba(2, 6, 23, 0.78);
  animation: orbPulse 3.2s ease-in-out infinite;
}

.orb-shell::before,
.orb-shell::after {
  content: '';
  position: absolute;
  inset: 9px;
  border-radius: 50%;
  pointer-events: none;
}

.orb-shell::before {
  border: 1px dashed rgba(190, 242, 255, 0.68);
  animation: orbitSpin 8s linear infinite;
}

.orb-shell::after {
  inset: 16px;
  border-top: 2px solid rgba(255, 255, 255, 0.88);
  border-right: 2px solid rgba(103, 232, 249, 0.38);
  transform: rotate(-22deg);
  opacity: 0.86;
}

.orb-grid {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(103, 232, 249, 0.13) 1px, transparent 1px),
    linear-gradient(0deg, rgba(255, 16, 240, 0.1) 1px, transparent 1px);
  background-size: 14px 14px;
  mask-image: radial-gradient(circle, black 58%, transparent 72%);
  animation: gridDrift 4.8s linear infinite;
}

.orb-core {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  background:
    radial-gradient(circle at 50% 38%, rgba(255, 255, 255, 0.9), rgba(103, 232, 249, 0.8) 23%, rgba(79, 70, 229, 0.5) 52%, rgba(2, 6, 23, 0.82) 100%);
  box-shadow: 0 0 22px rgba(103, 232, 249, 0.7);
}

.core-ring {
  position: absolute;
  inset: -8px;
  border-radius: 50%;
  border: 2px solid transparent;
  border-top-color: rgba(103, 232, 249, 0.95);
  border-left-color: rgba(255, 16, 240, 0.82);
  animation: orbitSpin 2.8s linear infinite;
}

.core-face {
  position: absolute;
  inset: 12px 10px;
}

.eye {
  position: absolute;
  top: 7px;
  width: 6px;
  height: 9px;
  border-radius: 999px;
  background: #07111f;
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.72);
  animation: blink 4s ease-in-out infinite;
}

.eye-left {
  left: 6px;
}

.eye-right {
  right: 6px;
}

.signal-mouth {
  position: absolute;
  left: 50%;
  bottom: 5px;
  width: 17px;
  height: 7px;
  border-bottom: 2px solid rgba(7, 17, 31, 0.95);
  border-radius: 0 0 999px 999px;
  transform: translateX(-50%);
}

.ai-mark {
  position: absolute;
  left: 50%;
  bottom: -21px;
  padding: 2px 8px;
  border: 1px solid rgba(103, 232, 249, 0.74);
  border-radius: 999px;
  background: rgba(2, 6, 23, 0.62);
  color: #dffbff;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0;
  transform: translateX(-50%);
  box-shadow: 0 0 12px rgba(103, 232, 249, 0.44);
}

.orb-halo {
  border-radius: 50%;
  pointer-events: none;
}

.halo-one {
  inset: -13px;
  border: 1px solid rgba(103, 232, 249, 0.32);
  box-shadow: 0 0 28px rgba(103, 232, 249, 0.28);
  animation: haloBreath 2.8s ease-in-out infinite;
}

.halo-two {
  inset: -25px;
  border: 1px solid rgba(255, 16, 240, 0.22);
  clip-path: polygon(50% 0, 62% 36%, 100% 50%, 62% 64%, 50% 100%, 38% 64%, 0 50%, 38% 36%);
  animation: orbitSpin 9s linear infinite reverse;
}

.spark {
  position: absolute;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #dffbff;
  box-shadow: 0 0 12px rgba(103, 232, 249, 0.95);
}

.spark-a { left: 12px; top: 18px; animation: sparkFloat 2.4s ease-in-out infinite; }
.spark-b { right: 10px; top: 30px; animation: sparkFloat 2.8s ease-in-out infinite 0.3s; }
.spark-c { left: 18px; bottom: 16px; animation: sparkFloat 3s ease-in-out infinite 0.6s; }
.spark-d { right: 20px; bottom: 10px; animation: sparkFloat 2.6s ease-in-out infinite 0.9s; }

.encourage-layer {
  pointer-events: none;
  overflow: visible;
}

.assistant-panel {
  position: absolute;
  right: 104px;
  bottom: 8px;
  width: 286px;
  padding: 13px;
  border: 1px solid rgba(103, 232, 249, 0.42);
  border-radius: 10px;
  background:
    linear-gradient(135deg, rgba(7, 16, 34, 0.94), rgba(17, 24, 48, 0.9)),
    radial-gradient(circle at 18% 12%, rgba(103, 232, 249, 0.18), transparent 42%);
  box-shadow:
    0 18px 42px rgba(2, 6, 23, 0.52),
    0 0 34px rgba(103, 232, 249, 0.24),
    inset 0 0 0 1px rgba(255, 255, 255, 0.04);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
  cursor: default;
}

.assistant-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background:
    linear-gradient(90deg, rgba(103, 232, 249, 0.1) 1px, transparent 1px),
    linear-gradient(0deg, rgba(255, 16, 240, 0.08) 1px, transparent 1px);
  background-size: 18px 18px;
  opacity: 0.6;
  pointer-events: none;
  mask-image: linear-gradient(180deg, black, transparent 86%);
}

.panel-header,
.panel-status,
.quick-actions,
.panel-tools {
  position: relative;
  z-index: 1;
}

.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.panel-header strong {
  display: block;
  margin-bottom: 3px;
  color: #f8fdff;
  font-size: 14px;
  line-height: 1.3;
}

.panel-header span {
  display: block;
  color: rgba(226, 248, 255, 0.62);
  font-size: 12px;
  line-height: 1.4;
}

.panel-close {
  width: 26px;
  height: 26px;
  border: 1px solid rgba(103, 232, 249, 0.2);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(226, 248, 255, 0.8);
  cursor: pointer;
  font-size: 17px;
  line-height: 1;
}

.panel-close:hover {
  border-color: rgba(103, 232, 249, 0.55);
  color: #ffffff;
  background: rgba(103, 232, 249, 0.12);
}

.panel-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 11px;
  padding: 8px 10px;
  border-radius: 8px;
  color: #dffbff;
  font-size: 12px;
  line-height: 1.4;
}

.tone-blue {
  background: rgba(64, 128, 255, 0.14);
  border: 1px solid rgba(64, 128, 255, 0.28);
}

.tone-cyan {
  background: rgba(34, 211, 238, 0.13);
  border: 1px solid rgba(34, 211, 238, 0.28);
}

.tone-pink {
  background: rgba(255, 16, 240, 0.12);
  border: 1px solid rgba(255, 16, 240, 0.25);
}

.tone-guest {
  background: rgba(245, 158, 11, 0.13);
  border: 1px solid rgba(245, 158, 11, 0.28);
}

.status-dot {
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #67e8f9;
  box-shadow: 0 0 12px rgba(103, 232, 249, 0.9);
}

.quick-actions {
  display: grid;
  gap: 8px;
}

.quick-action {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 54px;
  padding: 9px 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.045);
  color: #e8fdff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.16s ease, border-color 0.16s ease, background 0.16s ease;
}

.quick-action:hover {
  transform: translateX(-2px);
  border-color: rgba(103, 232, 249, 0.44);
  background: rgba(103, 232, 249, 0.1);
}

.action-icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  border-radius: 8px;
  background:
    radial-gradient(circle at 35% 25%, rgba(255, 255, 255, 0.65), transparent 28%),
    linear-gradient(135deg, rgba(103, 232, 249, 0.34), rgba(255, 16, 240, 0.22));
  border: 1px solid rgba(103, 232, 249, 0.32);
  color: #ffffff;
  font-size: 12px;
  font-weight: 900;
  box-shadow: 0 0 16px rgba(103, 232, 249, 0.18);
}

.action-copy {
  min-width: 0;
  flex: 1 1 auto;
  display: grid;
  gap: 2px;
}

.action-copy strong {
  color: #f8fdff;
  font-size: 13px;
  line-height: 1.25;
}

.action-copy small {
  color: rgba(226, 248, 255, 0.58);
  font-size: 11px;
  line-height: 1.35;
}

.action-badge {
  display: grid;
  place-items: center;
  min-width: 24px;
  height: 24px;
  flex: 0 0 auto;
  padding: 0 7px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.48);
  background: linear-gradient(135deg, rgba(255, 16, 240, 0.72), rgba(34, 211, 238, 0.68));
  color: #ffffff;
  font-size: 11px;
  font-weight: 900;
  box-shadow: 0 0 16px rgba(255, 16, 240, 0.36);
}

.panel-tools {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 10px;
}

.panel-tools button {
  min-height: 32px;
  border: 1px solid rgba(103, 232, 249, 0.24);
  border-radius: 8px;
  background: rgba(2, 6, 23, 0.46);
  color: rgba(226, 248, 255, 0.76);
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}

.panel-tools button:hover {
  color: #ffffff;
  border-color: rgba(103, 232, 249, 0.52);
  background: rgba(103, 232, 249, 0.1);
}

.assistant-panel-enter-active,
.assistant-panel-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.assistant-panel-enter-from,
.assistant-panel-leave-to {
  opacity: 0;
  transform: translate(10px, 8px) scale(0.96);
}

.encourage-chip {
  position: absolute;
  left: 50%;
  top: 70px;
  width: max-content;
  max-width: min(280px, calc(100vw - 40px));
  padding: 8px 12px;
  border: 1px solid rgba(103, 232, 249, 0.55);
  border-radius: 8px;
  background: rgba(4, 10, 24, 0.88);
  color: #e8fdff;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
  white-space: normal;
  overflow-wrap: anywhere;
  box-shadow:
    0 10px 24px rgba(2, 6, 23, 0.36),
    0 0 22px rgba(103, 232, 249, 0.28);
  transform: translateX(-50%);
}

.encourage-drop-enter-active {
  animation: chipDrop 3.4s cubic-bezier(0.2, 0.8, 0.22, 1) forwards;
}

.encourage-drop-leave-active {
  transition: opacity 0.2s ease;
}

.encourage-drop-leave-to {
  opacity: 0;
}

@keyframes chipDrop {
  0% {
    opacity: 0;
    transform: translate(-50%, -16px) scale(0.84) rotate(0deg);
  }
  12% {
    opacity: 1;
  }
  74% {
    opacity: 1;
    transform: translate(calc(-50% + var(--drift)), 78px) scale(1) rotate(var(--rotate));
  }
  100% {
    opacity: 0;
    transform: translate(calc(-50% + var(--drift)), 112px) scale(0.96) rotate(var(--rotate));
  }
}

@keyframes orbPulse {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-5px) scale(1.025);
  }
}

@keyframes orbitSpin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes gridDrift {
  to {
    background-position: 28px 28px;
  }
}

@keyframes haloBreath {
  0%, 100% {
    opacity: 0.62;
    transform: scale(0.94);
  }
  50% {
    opacity: 1;
    transform: scale(1.08);
  }
}

@keyframes sparkFloat {
  0%, 100% {
    opacity: 0.58;
    transform: translateY(0) scale(0.84);
  }
  50% {
    opacity: 1;
    transform: translateY(-8px) scale(1.1);
  }
}

@keyframes blink {
  0%, 92%, 100% {
    transform: scaleY(1);
  }
  95% {
    transform: scaleY(0.16);
  }
}

@media (max-width: 768px) {
  .floating-orb {
    width: 78px;
    height: 78px;
  }

  .orb-core {
    width: 42px;
    height: 42px;
  }

  .encourage-chip {
    font-size: 12px;
    max-width: min(240px, calc(100vw - 30px));
  }

  .assistant-panel {
    right: auto;
    left: 50%;
    bottom: 90px;
    width: min(286px, calc(100vw - 28px));
    transform: translateX(-50%);
  }

  .assistant-panel-enter-from,
  .assistant-panel-leave-to {
    transform: translateX(-50%) translateY(8px) scale(0.96);
  }
}

@media (prefers-reduced-motion: reduce) {
  .orb-shell,
  .orb-shell::before,
  .core-ring,
  .halo-one,
  .halo-two,
  .spark,
  .eye {
    animation: none;
  }
}
</style>
