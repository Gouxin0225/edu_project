<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-brand">
        <div class="brand-logo">
          <svg width="36" height="36" viewBox="0 0 64 64" fill="none" aria-hidden="true">
            <defs>
              <linearGradient id="layoutLogoGrad" x1="8" y1="8" x2="56" y2="56">
                <stop offset="0%" stop-color="#2563EB"/>
                <stop offset="58%" stop-color="#3B82F6"/>
                <stop offset="100%" stop-color="#12C7B7"/>
              </linearGradient>
              <filter id="layoutLogoGlow" x="-20%" y="-20%" width="140%" height="140%">
                <feGaussianBlur stdDeviation="2.2" result="blur"/>
                <feColorMatrix in="blur" type="matrix" values="0 0 0 0 0.1 0 0 0 0 0.45 0 0 0 0 1 0 0 0 .42 0"/>
                <feBlend in="SourceGraphic"/>
              </filter>
            </defs>
            <rect x="4" y="4" width="56" height="56" rx="16" fill="rgba(255,255,255,.08)"/>
            <rect x="4.5" y="4.5" width="55" height="55" rx="15.5" stroke="url(#layoutLogoGrad)" stroke-opacity=".5"/>
            <g filter="url(#layoutLogoGlow)">
              <path
                d="M13.8 48 25.2 18.8c2.5-6.3 11.1-6.3 13.6 0L50.2 48h-8.8l-3.8-9.6H25.4L21.6 48h-7.8Z"
                fill="url(#layoutLogoGrad)"
              />
              <path
                d="M26.8 31.7h10.4l-3.4-8.6c-.9-2.4-4.7-2.4-5.6 0l-3.4 8.6Z"
                fill="white"
                opacity=".92"
              />
              <path
                d="M16.8 48c6.7-11.2 16.4-16.9 30.4-17.4"
                stroke="#20D7C7"
                stroke-width="5.8"
                stroke-linecap="round"
              />
            </g>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-zh">慧学助手</span>
          <span class="brand-sub">SMART LEARNING ASSISTANT</span>
        </div>
      </div>

      <div class="header-right">
        <ThemeToggle variant="header" />
        <div class="user-pill">
          <div class="user-avatar">
            <el-icon><User /></el-icon>
          </div>
          <span class="user-name">{{ userStore.userInfo?.realName }}</span>
          <span class="role-badge" :class="getRoleClass(userStore.userInfo?.role)">
            {{ getRoleText(userStore.userInfo?.role) }}
          </span>
        </div>
        <el-button text class="hdr-btn" @click="showChangePwd = true">
          <el-icon><Lock /></el-icon>
          <span>密码</span>
        </el-button>
        <el-button text class="hdr-btn hdr-btn-logout" @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出</span>
        </el-button>
      </div>
    </el-header>

    <el-container class="body-container">
      <el-aside :width="isStudentRole ? '260px' : '200px'" class="layout-aside" :class="{ 'student-aside': isStudentRole }">
        <el-menu
          :default-active="activeMenu"
          router
          class="cyber-menu"
          :class="{ 'student-menu': isStudentRole }"
          background-color="transparent"
          text-color="rgba(255,255,255,0.50)"
          active-text-color="#FF10F0"
        >
          <template v-for="item in menuItems" :key="item.path">
            <el-menu-item :index="item.path" class="cyber-menu-item">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </el-menu-item>
          </template>
        </el-menu>
        <div class="aside-footer" :class="{ 'student-footer': isStudentRole }">
          <template v-if="isStudentRole">
            <div class="student-footer-visual">
              <div class="book-stack">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <div class="student-bot">
                <div class="bot-cap"></div>
                <div class="bot-face">AI</div>
              </div>
            </div>
          </template>
          <span v-else class="aside-ver">v2.0.0 // CYBER</span>
        </div>
      </el-aside>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <ChangePasswordDialog v-model:visible="showChangePwd" :force="forceChangePwd" />
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { useUserStore } from '@/stores/user'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isStudentRole = computed(() => userStore.userInfo?.role === 'STUDENT')
const showChangePwd = ref(false)
const serverRequiredChangePwd = ref(false)
const forceChangePwd = computed(() => !!userStore.userInfo?.mustChangePassword || serverRequiredChangePwd.value)

const activeMenu = computed(() => route.path)

interface MenuItem { path: string; title: string; icon: string }

const adminMenus: MenuItem[] = [
  { path: '/admin/users',      title: '用户管理',   icon: 'User' },
  { path: '/admin/classes',    title: '班级管理',   icon: 'School' },
  { path: '/admin/schedule-courses', title: '课程管理', icon: 'Reading' },
  { path: '/admin/schedule-teachers', title: '讲师能力', icon: 'UserFilled' },
  { path: '/admin/campus-progress', title: '课程进度', icon: 'DataBoard' },
  { path: '/admin/course-pool', title: '未上课程池', icon: 'Grid' },
  { path: '/admin/questions',  title: '题库管理',   icon: 'Collection' },
  { path: '/admin/ai-chat',    title: 'AI 问答',    icon: 'ChatLineRound' },
  { path: '/admin/statistics', title: '数据统计',   icon: 'DataLine' },
  { path: '/admin/audit-logs', title: '审计日志',   icon: 'Tickets' }
]

const teacherMenus: MenuItem[] = [
  { path: '/teacher/classes',    title: '我的班级',  icon: 'School' },
  { path: '/teacher/student-visits', title: '回访记录', icon: 'ChatDotRound' },
  { path: '/teacher/questions',  title: '题库管理',  icon: 'Collection' },
  { path: '/teacher/outline-exam', title: 'AI 大纲出卷', icon: 'DocumentAdd' },
  { path: '/teacher/ai-chat',    title: 'AI 问答',   icon: 'ChatLineRound' },
  { path: '/teacher/exams',      title: '考试管理',  icon: 'EditPen' },
  { path: '/teacher/homework',   title: '作业管理',  icon: 'Notebook' },
  { path: '/teacher/surveys',    title: '问卷管理',  icon: 'ChatDotRound' },
  { path: '/teacher/statistics', title: '数据统计',  icon: 'DataLine' }
]

const assistantMenus: MenuItem[] = [
  { path: '/assistant/dashboard', title: '工作台', icon: 'HomeFilled' },
  { path: '/assistant/profiles', title: '学员画像', icon: 'UserFilled' },
  { path: '/assistant/class-board', title: '班级看板', icon: 'DataBoard' },
  { path: '/assistant/campus-progress', title: '课程进度', icon: 'Reading' },
  { path: '/assistant/risk-center', title: '风险预警', icon: 'WarningFilled' },
  { path: '/assistant/todo-center', title: '待办中心', icon: 'List' },
  { path: '/assistant/join-applications', title: '入班审核', icon: 'Checked' },
  { path: '/assistant/students', title: '学员跟进', icon: 'User' },
  { path: '/assistant/ai-chat',   title: 'AI 问答',  icon: 'ChatLineRound' },
  { path: '/assistant/exams',   title: '考试跟踪', icon: 'EditPen' },
  { path: '/assistant/homework',   title: '作业跟踪', icon: 'Notebook' },
  { path: '/assistant/surveys', title: '问卷反馈', icon: 'ChatDotRound' },
  { path: '/assistant/student-visits', title: '回访记录', icon: 'ChatDotRound' },
  { path: '/assistant/statistics', title: '数据统计', icon: 'DataLine' }
]

const studentMenus: MenuItem[] = [
  { path: '/student/dashboard',    title: '任务看板', icon: 'HomeFilled' },
  { path: '/student/classes',      title: '加入班级', icon: 'School' },
  { path: '/student/ai-chat',      title: 'AI 问答',  icon: 'ChatLineRound' },
  { path: '/student/homework',     title: '我的作业', icon: 'Notebook' },
  { path: '/student/exams',        title: '我的考试', icon: 'EditPen' },
  { path: '/student/exam-records', title: '考试记录', icon: 'List' },
  { path: '/student/surveys',      title: '我的问卷', icon: 'ChatDotRound' },
  { path: '/student/mistakes',     title: '错题本',   icon: 'Warning' },
  { path: '/student/profile',      title: '个人中心', icon: 'User' }
]

const menuMap: Record<string, MenuItem[]> = {
  ADMIN: adminMenus,
  TEACHER: teacherMenus,
  ASSISTANT: assistantMenus,
  STUDENT: studentMenus
}

const menuItems = computed<MenuItem[]>(() =>
  menuMap[userStore.userInfo?.role ?? ''] ?? []
)

watch(forceChangePwd, (required) => {
  if (required) {
    showChangePwd.value = true
  }
}, { immediate: true })

function handlePasswordChangeRequired() {
  serverRequiredChangePwd.value = true
  showChangePwd.value = true
}

onMounted(() => {
  window.addEventListener('password-change-required', handlePasswordChangeRequired)
  if (forceChangePwd.value) {
    showChangePwd.value = true
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('password-change-required', handlePasswordChangeRequired)
})


function getRoleText(role: string | undefined): string {
  const map: Record<string, string> = {
    ADMIN: '管理员', TEACHER: '教师', ASSISTANT: '班主任', STUDENT: '学生'
  }
  return map[role ?? ''] ?? ''
}

function getRoleClass(role: string | undefined): string {
  const map: Record<string, string> = {
    ADMIN: 'role-admin', TEACHER: 'role-teacher', ASSISTANT: 'role-assistant', STUDENT: 'role-student'
  }
  return map[role ?? ''] ?? ''
}

async function handleLogout() {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await userStore.logout()
  await router.replace('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
  background: transparent;
  position: relative;
  z-index: 1;
}

/* ── Header ── */
.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 58px !important;
  padding: 0 24px;
  background: var(--header-bg) !important;
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  position: relative;
  z-index: 10;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: var(--header-shadow);
}

.layout-header::before,
.layout-header::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.layout-header::before {
  inset: 0;
  background:
    linear-gradient(90deg, transparent, var(--circuit-line), transparent),
    linear-gradient(135deg, transparent 0 46%, var(--circuit-line-strong) 47% 49%, transparent 50%);
  background-size: 180px 100%, 220px 220px;
  opacity: 0.55;
}

.layout-header::after {
  left: 0;
  right: 0;
  bottom: -1px;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--primary), var(--panel-edge), transparent);
  animation: edgePulse 4s ease-in-out infinite;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 0 0 auto;
  position: relative;
  z-index: 1;
}

.brand-logo {
  flex-shrink: 0;
  opacity: 0.95;
  filter: drop-shadow(0 0 10px var(--primary-border));
  transition: opacity 0.2s ease, filter 0.2s ease;
}
.brand-logo:hover {
  opacity: 1;
  filter: drop-shadow(0 0 14px var(--panel-edge));
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-zh {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 9px;
  letter-spacing: 2.5px;
  color: var(--brand-sub);
  font-weight: 500;
  text-transform: uppercase;
}

/* ── Header Right ── */
.header-right {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  z-index: 1;
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--surface-muted);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 5px 14px 5px 6px;
  transition: border-color 0.2s ease;
}
.user-pill:hover {
  border-color: var(--primary-border);
}

.user-avatar {
  width: 26px;
  height: 26px;
  background: var(--primary-dim);
  border: 1px solid var(--primary-border);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-light);
  font-size: 13px;
}

.user-name {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 600;
  letter-spacing: 0.3px;
}

.role-badge {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  letter-spacing: 0.5px;
}

.role-admin {
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #F87171;
}
.role-teacher {
  background: rgba(64, 128, 255, 0.12);
  border: 1px solid rgba(64, 128, 255, 0.3);
  color: #6499FF;
}
.role-assistant {
  background: rgba(34, 197, 94, 0.12);
  border: 1px solid rgba(34, 197, 94, 0.3);
  color: #4ADE80;
}
.role-student {
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.3);
  color: #FBB040;
}

.hdr-btn {
  color: var(--text-secondary) !important;
  font-size: 12px !important;
  gap: 5px;
  padding: 5px 12px !important;
  border-radius: 8px !important;
  transition: all 0.18s ease !important;
  font-weight: 500 !important;
}
.hdr-btn:hover {
  color: var(--text-primary) !important;
  background: var(--surface-hover) !important;
}
.hdr-btn-logout:hover {
  color: #F87171 !important;
  background: rgba(239, 68, 68, 0.08) !important;
}

/* ── Body ── */
.body-container {
  height: calc(100vh - 58px);
  overflow: hidden;
}

/* ── Sidebar ── */
.layout-aside {
  background: var(--aside-bg) !important;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  position: relative;
}

.layout-aside::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, var(--circuit-line) 1px, transparent 1px),
    linear-gradient(0deg, var(--circuit-line) 1px, transparent 1px);
  background-size: 72px 72px;
  opacity: 0.38;
  pointer-events: none;
  animation: circuitDrift 42s linear infinite;
}

.cyber-menu {
  border-right: none !important;
  flex: 1;
  padding: 10px 8px;
  position: relative;
  z-index: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* Custom scrollbar for cyber-menu */
.cyber-menu::-webkit-scrollbar {
  width: 4px;
}
.cyber-menu::-webkit-scrollbar-thumb {
  background: var(--primary-dim);
  border-radius: 4px;
}
.cyber-menu::-webkit-scrollbar-track {
  background: transparent;
}

.cyber-menu :deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  margin: 2px 0;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.2px;
  border-radius: 8px;
  transition: all 0.18s ease;
  position: relative;
  overflow: hidden;
}

.cyber-menu :deep(.el-menu-item::after) {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 7px;
  border: 1px solid transparent;
  background: linear-gradient(90deg, transparent, var(--panel-sheen), transparent);
  opacity: 0;
  transform: translateX(-60%);
  transition: opacity 0.18s ease, transform 0.18s ease;
  pointer-events: none;
}

.cyber-menu :deep(.el-menu-item:hover) {
  background: var(--surface-hover) !important;
  color: var(--text-primary) !important;
}

.cyber-menu :deep(.el-menu-item:hover::after) {
  opacity: 1;
  transform: translateX(0);
}

.cyber-menu :deep(.el-menu-item.is-active) {
  background: var(--primary-dim) !important;
  color: var(--primary-light) !important;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px var(--primary-border), var(--glow-primary);
}

.cyber-menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0; top: 8px; bottom: 8px;
  width: 3px;
  background: var(--primary);
  border-radius: 0 3px 3px 0;
}

.cyber-menu :deep(.el-menu-item .el-icon) {
  font-size: 15px;
  margin-right: 9px;
  color: inherit;
  filter: drop-shadow(0 0 8px color-mix(in srgb, currentColor 40%, transparent));
}

.aside-footer {
  padding: 14px 16px;
  border-top: 1px solid var(--border-subtle);
  position: relative;
  z-index: 1;
  flex-shrink: 0;
}

.student-menu::-webkit-scrollbar-thumb {
  background: rgba(75, 114, 255, 0.2) !important;
}
.student-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(75, 114, 255, 0.4) !important;
}
.aside-ver {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 1.5px;
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
}

/* ── Main ── */
.layout-main {
  background: transparent;
  overflow-y: auto;
  padding: 0;
}

.main-inner {
  padding: 24px;
  min-height: 100%;
}

/* ── Student Sidebar ── */
.layout-aside.student-aside {
  --student-blue: #4b72ff;
  --student-purple: #7662ff;
  --student-ink: #25334f;
  --student-muted: #7d8ba6;
  --student-line: #dfe8ff;
  width: 260px;
  padding: 18px 14px 0;
  border-right: 1px solid rgba(208, 220, 248, 0.92);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(243, 248, 255, 0.95)),
    #f8fbff !important;
  box-shadow: 12px 0 34px rgba(62, 94, 171, 0.1);
}

.layout-aside.student-aside::before {
  display: none;
}

.student-menu {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  gap: 8px;
  padding: 0 0 32px 0 !important;
  background: transparent !important;
}

.student-menu :deep(.el-menu-item) {
  height: 52px;
  min-height: 52px;
  margin: 0;
  padding: 0 14px !important;
  border-radius: 8px;
  color: var(--student-ink) !important;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 52px;
  transition: background 0.18s ease, box-shadow 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

.student-menu :deep(.el-menu-item::after),
.student-menu :deep(.el-menu-item.is-active::before) {
  display: none;
}

.student-menu :deep(.el-menu-item:hover) {
  background: rgba(235, 241, 255, 0.82) !important;
  color: var(--student-blue) !important;
  transform: translateX(2px);
}

.student-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, var(--student-blue), var(--student-purple)) !important;
  color: #fff !important;
  box-shadow: 0 12px 24px rgba(85, 104, 255, 0.28);
  transform: none;
}

.student-menu :deep(.el-menu-item.is-active)::after {
  display: block;
  content: '';
  position: absolute;
  right: 18px;
  top: 50%;
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: #fff;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.75);
  transform: translateY(-50%) rotate(45deg);
}

.student-menu :deep(.el-menu-item .el-icon) {
  display: inline-flex;
  width: 34px;
  height: 34px;
  margin-right: 14px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(211, 222, 249, 0.95);
  border-radius: 8px;
  background: linear-gradient(180deg, #f7faff, #eef4ff);
  color: var(--student-blue);
  font-size: 20px;
  filter: none;
  box-shadow: 0 8px 18px rgba(58, 98, 186, 0.08);
}

.student-menu :deep(.el-menu-item.is-active .el-icon) {
  border-color: rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  box-shadow: none;
}

.student-menu :deep(.el-menu-item:nth-child(1) .el-icon) {
  color: #5478ff;
}

.student-menu :deep(.el-menu-item:nth-child(2) .el-icon) {
  color: #5f73ff;
}

.student-menu :deep(.el-menu-item:nth-child(3) .el-icon) {
  color: #5c7bff;
}

.student-menu :deep(.el-menu-item:nth-child(4) .el-icon) {
  color: #4488f7;
}

.student-menu :deep(.el-menu-item:nth-child(5) .el-icon) {
  color: #7563ff;
}

.student-menu :deep(.el-menu-item:nth-child(6) .el-icon) {
  color: #8066ff;
}

.student-menu :deep(.el-menu-item:nth-child(7) .el-icon) {
  color: #36c5b6;
}

.student-menu :deep(.el-menu-item:nth-child(8) .el-icon) {
  color: #ff785f;
}

.student-menu :deep(.el-menu-item:nth-child(9) .el-icon) {
  color: #4c74ff;
}

.student-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #fff;
}

.student-footer {
  min-height: 138px;
  margin: 10px -14px 0;
  padding: 0;
  overflow: hidden;
  border-top: 1px solid rgba(216, 226, 250, 0.9);
  background:
    radial-gradient(circle at 83% 26%, rgba(255, 208, 87, 0.58) 0 2px, transparent 3px),
    radial-gradient(circle at 49% 28%, rgba(127, 112, 255, 0.28) 0 4px, transparent 5px),
    linear-gradient(180deg, rgba(255, 255, 255, 0.2), rgba(223, 233, 255, 0.86));
}

.student-footer-visual {
  position: relative;
  height: 138px;
}

.student-footer-visual::before {
  content: '';
  position: absolute;
  left: -8px;
  bottom: -16px;
  width: 120px;
  height: 110px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(83, 210, 190, 0.36), transparent 66%);
}

.book-stack {
  position: absolute;
  left: 22px;
  bottom: 18px;
  width: 82px;
  height: 44px;
}

.book-stack span {
  position: absolute;
  left: 0;
  width: 74px;
  height: 14px;
  border-radius: 4px;
  box-shadow: 0 6px 14px rgba(75, 114, 255, 0.16);
}

.book-stack span:nth-child(1) {
  bottom: 0;
  background: #f6b15d;
  transform: rotate(2deg);
}

.book-stack span:nth-child(2) {
  bottom: 12px;
  left: 8px;
  background: #7f98ff;
  transform: rotate(-3deg);
}

.book-stack span:nth-child(3) {
  bottom: 24px;
  left: 2px;
  background: #c8d8ff;
  transform: rotate(2deg);
}

.student-bot {
  position: absolute;
  right: 28px;
  bottom: 15px;
  width: 82px;
  height: 82px;
  border-radius: 50%;
  background:
    radial-gradient(circle at 50% 32%, #fff 0 28%, transparent 29%),
    linear-gradient(180deg, #eaf2ff, #cbdcff);
  box-shadow: 0 14px 26px rgba(68, 108, 210, 0.2);
}

.bot-face {
  position: absolute;
  left: 16px;
  top: 28px;
  display: grid;
  width: 50px;
  height: 32px;
  place-items: center;
  border-radius: 12px;
  background: #445bd9;
  color: #9effff;
  font-size: 13px;
  font-weight: 900;
}

.bot-cap {
  position: absolute;
  left: 13px;
  top: -7px;
  width: 58px;
  height: 16px;
  border-radius: 5px;
  background: linear-gradient(135deg, #506fff, #7b67ff);
  transform: rotate(9deg);
}

.bot-cap::after {
  content: '';
  position: absolute;
  right: -6px;
  top: 12px;
  width: 2px;
  height: 20px;
  background: #f6b15d;
  transform: rotate(-10deg);
}

@media (max-width: 960px) {
  .layout-aside.student-aside {
    width: 220px !important;
    padding: 14px 10px 0;
  }

  .student-menu :deep(.el-menu-item) {
    height: 48px;
    min-height: 48px;
    font-size: 14px;
  }

  .student-menu :deep(.el-menu-item .el-icon) {
    width: 30px;
    height: 30px;
    margin-right: 10px;
    font-size: 17px;
  }
}
</style>
