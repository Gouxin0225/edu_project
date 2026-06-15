import request from '@/utils/request'

export interface AssistantStudentBrief {
  studentId: number
  studentName: string
  username: string
  phone?: string
  schoolName?: string
  classId: number
  className: string
  joinTime?: string
  lastActivityTime?: string
  lastVisitTime?: string
  nextFollowTime?: string
  riskScore: number
}

export interface AssistantClassBoard {
  classId: number
  className: string
  schoolName?: string
  studentCount: number
  activeCount: number
  riskCount: number
  homeworkCompletionRate: number
  examCompletionRate: number
  surveyCompletionRate: number
  students: AssistantStudentBrief[]
}

export interface AssistantRiskItem {
  id: string
  type: string
  title: string
  level: 'HIGH' | 'MEDIUM' | 'LOW'
  studentId: number
  studentName: string
  classId: number
  className: string
  sourceType: string
  sourceId: number
  sourceTitle?: string
  sourceTime?: string
  reason: string
  status: 'PENDING' | 'CONTACTED' | 'FOLLOWING' | 'RESOLVED'
}

export interface AssistantTodoItem {
  id: string
  type: string
  title: string
  studentId: number
  studentName: string
  classId: number
  className: string
  sourceType: string
  sourceId: number
  deadline?: string
  reason: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
}

export interface AssistantVisitItem {
  id: number
  visitMethod: string
  visitResult: string
  content: string
  problemCategory?: string
  conclusion?: string
  attachmentUrl?: string
  resolved?: boolean
  visitTime?: string
  nextFollowTime?: string
}

export interface AssistantTaskRecord {
  taskId: number
  title: string
  type: 'EXAM' | 'HOMEWORK'
  deadline?: string
  status: string
  score?: number
  totalScore?: number
  switchScreenCount: number
  startTime?: string
  submitTime?: string
  assistantComment?: string
}

export interface AssistantSurveyRecord {
  surveyId: number
  title: string
  deadline?: string
  submitted: boolean
  submitTime?: string
}

export interface AssistantStudentProfile {
  basicInfo: {
    studentId: number
    studentName: string
    username: string
    phone?: string
    schoolName?: string
    currentClassId: number
    currentClassName: string
    joinTime?: string
    createTime?: string
  }
  learningState: {
    examCount: number
    homeworkCount: number
    submittedHomeworkCount: number
    submittedExamCount: number
    missingTaskCount: number
    lowScoreCount: number
    switchScreenCount: number
    surveySubmittedCount: number
    surveyMissingCount: number
    aiQuestionCount: number
    mistakeCount: number
    averageScore: number
    lastActivityTime?: string
  }
  classHistory: Array<{
    classId: number
    className: string
    status: string
    joinStatus: string
    joinSource?: string
    joinTime?: string
    leaveTime?: string
  }>
  risks: AssistantRiskItem[]
  visits: AssistantVisitItem[]
  exams: AssistantTaskRecord[]
  homework: AssistantTaskRecord[]
  surveys: AssistantSurveyRecord[]
  weakPoints: string[]
}

export const getAssistantClassBoards = () =>
  request.get<any, { code: number; data: AssistantClassBoard[] }>('/api/assistant/operations/classes')

export const getAssistantStudentProfile = (studentId: number) =>
  request.get<any, { code: number; data: AssistantStudentProfile }>(`/api/assistant/operations/students/${studentId}/profile`)

export const getAssistantRisks = (classId?: number) =>
  request.get<any, { code: number; data: AssistantRiskItem[] }>('/api/assistant/operations/risks', {
    params: classId ? { classId } : {}
  })

export const getAssistantTodos = (classId?: number) =>
  request.get<any, { code: number; data: AssistantTodoItem[] }>('/api/assistant/operations/todos', {
    params: classId ? { classId } : {}
  })
