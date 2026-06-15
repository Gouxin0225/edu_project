import request from '@/utils/request'
import type { CreateUserResult, ResetPasswordResult } from './admin/user'

export interface TeacherClass {
  id: number
  className: string
  status: number
  createTime: string
}

export interface StudentRecord {
  id: number
  username: string
  phone?: string
  schoolName?: string
  realName: string
  role: string
  classId: number
  className?: string
  createTime?: string
  classHistory?: StudentClassHistory[]
}

export interface StudentClassHistory {
  classId: number
  className: string
  status: string
  joinStatus: string
  joinTime: string
  leaveTime?: string
}

export interface CreateStudentDTO {
  username?: string
  realName: string
  phone?: string
  schoolName?: string
  classId: number
  role: 'STUDENT'
}

export interface UpdateStudentDTO {
  username: string
  realName: string
  phone?: string
  schoolName?: string
}

export const getMyClasses = () =>
  request.get<any, { code: number; data: TeacherClass[] }>('/teacher/my-classes')

export const createStudent = (data: CreateStudentDTO) =>
  request.post<any, { code: number; data: CreateUserResult }>('/teacher/student', data)

export const searchExistingStudents = (params: { targetClassId: number; keyword: string; limit?: number }) =>
  request.get<any, { code: number; data: StudentRecord[] }>('/teacher/students/search-existing', {
    params: {
      targetClassId: params.targetClassId,
      keyword: params.keyword,
      limit: params.limit || 20
    }
  })

export const resetStudentPassword = (id: number) =>
  request.put<any, { code: number; data: ResetPasswordResult }>(`/teacher/student/${id}/reset-password`)

export const updateStudent = (id: number, data: UpdateStudentDTO) =>
  request.put<any, { code: number; data: StudentRecord }>(`/teacher/student/${id}`, data)

export const removeStudentFromClass = (studentId: number, classId: number) =>
  request.delete<any, { code: number }>(`/teacher/student/${studentId}/class/${classId}`)

export const batchImportStudents = (classId: number, file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('classId', String(classId))
  return request.post<any, { code: number; data: any }>('/teacher/student/batch', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getStudentList = (classId?: number, keyword?: string) => {
  const params: { classId?: number; keyword?: string } = {}
  if (classId) params.classId = classId
  if (keyword?.trim()) params.keyword = keyword.trim()
  return request.get<any, { code: number; data: StudentRecord[] }>('/teacher/students', { params })
}
export interface TeacherStatisticsOverview {
  classCount: number
  studentCount: number
  examCount: number
  homeworkCount: number
  activeTaskCount: number
  pendingGradeCount: number
  averageScore: number
  passRate: number
}

export interface TeacherStatisticsClassOption {
  classId: number
  className: string
  studentCount: number
}

export interface TeacherTaskProgress {
  taskId: number
  title: string
  type: 'EXAM' | 'HOMEWORK'
  deadline: string
  totalStudents: number
  submittedCount: number
  gradedCount: number
  pendingCount: number
  pendingGradeCount: number
  completionRate: number
  averageScore: number
  targetClassNames: string[]
}

export interface TeacherClassComparison {
  classId: number
  className: string
  studentCount: number
  taskCount: number
  submissionRate: number
  averageScore: number
  riskCount: number
}

export interface TeacherRiskStudent {
  studentId: number
  studentName: string
  className: string
  missingCount: number
  lowScoreCount: number
  switchScreenCount: number
  averageScore: number
  riskScore: number
}

export interface TeacherScoreDistribution {
  range: string
  count: number
  rate: number
}

export interface TeacherScoreTrend {
  taskId: number
  title: string
  deadline: string
  averageScore: number
  completionRate: number
  submittedCount: number
  totalStudents: number
}

export interface TeacherWeakKnowledgePoint {
  courseCategory: string
  knowledgePoint: string
  attempts: number
  wrongCount: number
  wrongRate: number
  averageScoreRate: number
}

export interface TeacherAiUsageSummary {
  studentQuestionCount: number
  teacherQuestionCount: number
  activeStudentCount: number
  knowledgeDocumentCount: number
  knowledgeChunkCount: number
  reviewNoteCount: number
  topTopics: TeacherAiTopic[]
}

export interface TeacherAiTopic {
  label: string
  count: number
}

export interface TeacherSurveySummary {
  surveyCount: number
  publishedCount: number
  totalResponses: number
  recentSurveys: TeacherSurveyItem[]
}

export interface TeacherSurveyItem {
  surveyId: number
  title: string
  deadline: string
  status: number
  totalResponses: number
}

export interface TeacherStatistics {
  selectedClassId: number | null
  selectedClassName: string
  overview: TeacherStatisticsOverview
  classes: TeacherStatisticsClassOption[]
  recentTasks: TeacherTaskProgress[]
  classComparisons: TeacherClassComparison[]
  riskStudents: TeacherRiskStudent[]
  scoreDistribution: TeacherScoreDistribution[]
  scoreTrends: TeacherScoreTrend[]
  weakKnowledgePoints: TeacherWeakKnowledgePoint[]
  aiUsageSummary: TeacherAiUsageSummary
  surveySummary: TeacherSurveySummary
}

export const getTeacherStatistics = (classId?: number) =>
  request.get<any, { code: number; data: TeacherStatistics }>('/api/teacher/statistics', {
    params: classId ? { classId } : {}
  })

export interface ClassJoinApplication {
  id: number
  classId: number
  className: string
  studentId: number
  studentName: string
  username: string
  phone: string
  schoolName: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELED'
  applyTime: string
  auditTime: string | null
  auditorId: number | null
  auditorName: string | null
  rejectReason: string | null
}

export const getJoinApplications = (params?: { classId?: number; status?: string }) =>
  request.get<any, { code: number; data: ClassJoinApplication[] }>('/api/class-join-applications', { params })

export const approveJoinApplication = (id: number) =>
  request.put<any, { code: number; message: string }>(`/api/class-join-applications/${id}/approve`)

export const rejectJoinApplication = (id: number, rejectReason?: string) =>
  request.put<any, { code: number; message: string }>(`/api/class-join-applications/${id}/reject`, { rejectReason })
