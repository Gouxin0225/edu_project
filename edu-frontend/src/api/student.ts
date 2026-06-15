import request from '@/utils/request'

export interface TaskVO {
  taskId: number
  taskType: 'EXAM' | 'HOMEWORK'
  title: string
  deadline: string
  status: 'UPCOMING' | 'PENDING' | 'RETURNED' | 'SUBMITTED' | 'COMPLETED'
  scoreGained: number | null
}

export interface DashboardVO {
  upcomingTasks: TaskVO[]
  todoTasks?: TaskVO[]
  pendingTasks: TaskVO[]
  submittedTasks?: TaskVO[]
  completedTasks: TaskVO[]
}

export interface MistakeVO {
  mistakeId: number
  questionId: number
  courseCategory: string
  knowledgePoint: string
  questionType: string
  difficulty: string
  content: string
  optionsJson: string | null
  standardAnswer: string
  analysis: string
  wrongCount: number
  lastWrongTime: string
  isMastered: number
}

export interface ChallengeQuestionVO {
  questionId: number
  courseCategory: string
  knowledgePoint: string
  type: string
  difficulty: string
  content: string
  optionsJson: string | null
  standardAnswer: string
  analysis: string
}

export interface ChallengeVO {
  challengeId: string
  questions: ChallengeQuestionVO[]
}

export const getDashboard = () =>
  request.get<any, { code: number; data: DashboardVO }>('/api/student/dashboard')

export const getMistakeList = (params?: { knowledgePoint?: string; questionType?: string }) =>
  request.get<any, { code: number; data: MistakeVO[] }>('/api/student/mistakes', { params })

export const markMistakeMastered = (questionId: number) =>
  request.put<any, { code: number; message: string }>(`/api/student/mistakes/${questionId}/master`)

export const startChallenge = (questionCount: number) =>
  request.post<any, { code: number; data: ChallengeVO }>('/api/student/mistakes/challenge', { questionCount })

export interface StudentClassItem {
  classId: number
  className: string
  status: number
  schoolName: string | null
  allowStudentApply: boolean
  joined: boolean
  applicationStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELED' | null
  applyTime: string | null
  auditTime: string | null
  rejectReason: string | null
  teacherNames: string[]
  assistantNames: string[]
}

export const getAvailableClasses = () =>
  request.get<any, { code: number; data: StudentClassItem[] }>('/api/student/classes/available')

export const getMyClasses = () =>
  request.get<any, { code: number; data: StudentClassItem[] }>('/api/student/classes/my')

export const applyJoinClass = (classId: number) =>
  request.post<any, { code: number; message: string }>(`/api/student/classes/${classId}/apply`)
