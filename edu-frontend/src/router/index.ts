import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/scsa-s',
    name: 'ScsaPractice',
    component: () => import('@/views/public/ScsaPractice.vue'),
    meta: { requiresAuth: false }
  },
  // ADMIN
  {
    path: '/admin',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/users' },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/UserManage.vue') },
      { path: 'classes', name: 'AdminClasses', component: () => import('@/views/admin/ClassManage.vue') },
      { path: 'questions', name: 'AdminQuestions', component: () => import('@/views/teacher/QuestionBank.vue') },
      { path: 'ai-chat', name: 'AdminAiChat', component: () => import('@/views/common/AiChat.vue') },
      { path: 'statistics', name: 'AdminStatistics', component: () => import('@/views/admin/Statistics.vue') }
    ]
  },
  // TEACHER
  {
    path: '/teacher',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true, role: 'TEACHER' },
    children: [
      { path: '', redirect: '/teacher/classes' },
      { path: 'classes', name: 'TeacherClasses', component: () => import('@/views/teacher/Classes.vue') },
      { path: 'questions', name: 'TeacherQuestions', component: () => import('@/views/teacher/QuestionBank.vue') },
      { path: 'outline-exam', name: 'TeacherOutlineExam', component: () => import('@/views/teacher/OutlineExam.vue') },
      { path: 'ai-chat', name: 'TeacherAiChat', component: () => import('@/views/common/AiChat.vue') },
      { path: 'student-visits', name: 'TeacherStudentVisits', component: () => import('@/views/common/StudentVisits.vue') },
      { path: 'exams', name: 'TeacherExams', component: () => import('@/views/teacher/ExamList.vue') },
      { path: 'exams/create', name: 'TeacherExamCreate', component: () => import('@/views/teacher/ExamCreate.vue') },
      { path: 'exams/grade/:id', name: 'TeacherExamGrade', component: () => import('@/views/teacher/ExamGrade.vue') },
      { path: 'templates', name: 'TeacherTemplates', component: () => import('@/views/teacher/TemplateList.vue') },
      { path: 'homework', name: 'TeacherHomework', component: () => import('@/views/teacher/HomeworkManage.vue') },
      { path: 'surveys', name: 'TeacherSurveys', component: () => import('@/views/teacher/Surveys.vue') },
      { path: 'statistics', name: 'TeacherStatistics', component: () => import('@/views/teacher/Statistics.vue') }
    ]
  },
  // ASSISTANT
  {
    path: '/assistant',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true, role: 'ASSISTANT' },
    children: [
      { path: '', redirect: '/assistant/dashboard' },
      { path: 'dashboard', name: 'AssistantDashboard', component: () => import('@/views/assistant/Dashboard.vue') },
      { path: 'join-applications', name: 'AssistantJoinApplications', component: () => import('@/views/assistant/JoinApplications.vue') },
      { path: 'students', name: 'AssistantStudents', component: () => import('@/views/assistant/Students.vue') },
      { path: 'ai-chat', name: 'AssistantAiChat', component: () => import('@/views/common/AiChat.vue') },
      { path: 'homework', name: 'AssistantHomework', component: () => import('@/views/assistant/Homework.vue') },
      { path: 'student-visits', name: 'AssistantStudentVisits', component: () => import('@/views/common/StudentVisits.vue') },
      { path: 'statistics', name: 'AssistantStatistics', component: () => import('@/views/assistant/Statistics.vue') }
    ]
  },
  // STUDENT
  {
    path: '/student',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true, role: 'STUDENT' },
    children: [
      { path: '', redirect: '/student/dashboard' },
      { path: 'dashboard', name: 'StudentDashboard', component: () => import('@/views/student/Dashboard.vue') },
      { path: 'classes', name: 'StudentClasses', component: () => import('@/views/student/Classes.vue') },
      { path: 'ai-chat', name: 'StudentAiChat', component: () => import('@/views/common/AiChat.vue') },
      { path: 'homework', name: 'StudentHomework', component: () => import('@/views/student/MyHomework.vue') },
      { path: 'exams', name: 'StudentExams', component: () => import('@/views/student/Exams.vue') },
      { path: 'exam-records', name: 'StudentExamRecords', component: () => import('@/views/student/ExamRecords.vue') },
      { path: 'exam-result/:id', name: 'StudentExamResult', component: () => import('@/views/student/ExamResult.vue') },
      { path: 'exam/:id', name: 'StudentExamTaking', component: () => import('@/views/student/ExamTaking.vue') },
      { path: 'surveys', name: 'StudentSurveys', component: () => import('@/views/student/Surveys.vue') },
      { path: 'survey/:id', name: 'StudentSurveyFill', component: () => import('@/views/student/SurveyFill.vue') },
      { path: 'scsa-s', name: 'StudentScsaPractice', component: () => import('@/views/public/ScsaPractice.vue') },
      { path: 'mistakes', name: 'StudentMistakes', component: () => import('@/views/student/Mistakes.vue') },
      { path: 'profile', name: 'StudentProfile', component: () => import('@/views/student/Profile.vue') }
    ]
  },
  // 根路径重定向
  { path: '/', redirect: '/login' },
  // 404
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')

  if (to.meta.requiresAuth === false) {
    return next()
  }

  // 未登录跳转登录页
  if (!token || !userInfo) {
    return next('/login')
  }

  // 角色权限校验
  const requiredRole = to.meta.role as string | undefined
  if (requiredRole && userInfo.role !== requiredRole) {
    return next(getRoleHome(userInfo.role))
  }

  next()
})

function getRoleHome(role: string): string {
  const map: Record<string, string> = {
    ADMIN: '/admin',
    TEACHER: '/teacher',
    ASSISTANT: '/assistant',
    STUDENT: '/student'
  }
  return map[role] || '/login'
}

export { getRoleHome }
export default router
