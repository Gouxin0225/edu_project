<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-brand">
        <div class="brand-logo">
          <svg width="32" height="32" viewBox="0 0 80 80" fill="none">
            <defs>
              <linearGradient id="logoGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#4080FF"/>
                <stop offset="100%" stop-color="#8060E8"/>
              </linearGradient>
            </defs>
            <rect x="8" y="8" width="64" height="64" rx="14"
              fill="url(#logoGrad)" opacity="0.15"/>
            <rect x="8" y="8" width="64" height="64" rx="14"
              fill="none" stroke="url(#logoGrad)" stroke-width="1.5"/>
            <text x="50%" y="55%" dominant-baseline="middle" text-anchor="middle"
              fill="url(#logoGrad)" font-size="26" font-weight="800" font-family="Outfit,sans-serif">OP</text>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-zh">智能教育管理系统</span>
          <span class="brand-sub">SMART EDUCATION SYSTEM</span>
        </div>
      </div>

      <div class="header-right">
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
      <el-aside width="200px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          router
          class="cyber-menu"
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
        <div class="aside-footer">
          <span class="aside-ver">v2.0.0 // CYBER</span>
        </div>
      </el-aside>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <ChangePasswordDialog v-model:visible="showChangePwd" :force="forceChangePassword" />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const forceChangePassword = computed(() => !!userStore.userInfo?.mustChangePassword)
const showChangePwd = ref(forceChangePassword.value)

watch(forceChangePassword, (required) => {
  if (required) {
    showChangePwd.value = true
  }
}, { immediate: true })

const activeMenu = computed(() => route.path)

interface MenuItem { path: string; title: string; icon: string }

const adminMenus: MenuItem[] = [
  { path: '/admin/users',      title: '用户管理',   icon: 'User' },
  { path: '/admin/classes',    title: '班级管理',   icon: 'School' },
  { path: '/admin/questions',  title: '题库管理',   icon: 'Collection' },
  { path: '/admin/ai-chat',    title: 'AI 问答',    icon: 'ChatLineRound' },
  { path: '/admin/statistics', title: '数据统计',   icon: 'DataLine' }
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
  { path: '/assistant/join-applications', title: '入班审核', icon: 'Checked' },
  { path: '/assistant/students', title: '学员跟进', icon: 'User' },
  { path: '/assistant/ai-chat',   title: 'AI 问答',  icon: 'ChatLineRound' },
  { path: '/assistant/homework',   title: '作业跟踪', icon: 'Notebook' },
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
  { path: '/student/scsa-s',       title: 'SCSA-S练习', icon: 'Reading' },
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
  router.push('/login')
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
  background: rgba(6, 11, 24, 0.88) !important;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
  position: relative;
  z-index: 10;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 1px 0 rgba(64, 128, 255, 0.1), 0 4px 24px rgba(0, 0, 0, 0.4);
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 0 0 auto;
}

.brand-logo {
  flex-shrink: 0;
  opacity: 0.95;
  transition: opacity 0.2s ease;
}
.brand-logo:hover { opacity: 1; }

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-zh {
  font-size: 15px;
  font-weight: 700;
  color: #E2E8F0;
  line-height: 1;
  letter-spacing: 0.5px;
}

.brand-sub {
  font-size: 9px;
  letter-spacing: 2.5px;
  color: rgba(64, 128, 255, 0.55);
  font-weight: 500;
  text-transform: uppercase;
}

/* ── Header Right ── */
.header-right {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 5px 14px 5px 6px;
  transition: border-color 0.2s ease;
}
.user-pill:hover {
  border-color: rgba(64, 128, 255, 0.3);
}

.user-avatar {
  width: 26px;
  height: 26px;
  background: rgba(64, 128, 255, 0.15);
  border: 1px solid rgba(64, 128, 255, 0.3);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6499FF;
  font-size: 13px;
}

.user-name {
  font-size: 13px;
  color: #C8D5E8;
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
  color: rgba(148, 163, 184, 0.7) !important;
  font-size: 12px !important;
  gap: 5px;
  padding: 5px 12px !important;
  border-radius: 8px !important;
  transition: all 0.18s ease !important;
  font-weight: 500 !important;
}
.hdr-btn:hover {
  color: #E2E8F0 !important;
  background: rgba(255, 255, 255, 0.06) !important;
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
  background: rgba(6, 11, 24, 0.75) !important;
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.cyber-menu {
  border-right: none !important;
  flex: 1;
  padding: 10px 8px;
}

.cyber-menu :deep(.el-menu-item) {
  height: 40px;
  line-height: 40px;
  margin: 2px 0;
  color: rgba(148, 163, 184, 0.7);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.2px;
  border-radius: 8px;
  transition: all 0.18s ease;
  position: relative;
}

.cyber-menu :deep(.el-menu-item:hover) {
  background: rgba(64, 128, 255, 0.08) !important;
  color: #94A3B8 !important;
}

.cyber-menu :deep(.el-menu-item.is-active) {
  background: rgba(64, 128, 255, 0.14) !important;
  color: #6499FF !important;
  font-weight: 600;
}

.cyber-menu :deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0; top: 8px; bottom: 8px;
  width: 3px;
  background: #4080FF;
  border-radius: 0 3px 3px 0;
}

.cyber-menu :deep(.el-menu-item .el-icon) {
  font-size: 15px;
  margin-right: 9px;
  color: inherit;
}

.aside-footer {
  padding: 14px 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.04);
}
.aside-ver {
  font-size: 11px;
  color: rgba(64, 128, 255, 0.3);
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
</style>
