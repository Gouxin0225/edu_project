import request from '@/utils/request'

export interface CreateHomeworkDTO {
  title: string
  content: string
  deadline: string
  targetClassIds: number[]
  attachments?: string
}

export interface HomeworkListItem {
  homeworkId: number
  title: string
  content: string
  deadline: string
  totalSubmissions: number
  targetStudentCount: number
  gradedSubmissions: number
  pendingSubmissions: number
}

export const getTeacherHomeworkList = () =>
  request.get<any, { code: number; data: HomeworkListItem[] }>('/api/homework')

export const getAssistantHomeworkList = () =>
  request.get<any, { code: number; data: HomeworkListItem[] }>('/api/assistant/homework')

export const createHomework = (data: CreateHomeworkDTO) =>
  request.post<any, { code: number; data: number }>('/api/homework', data)

export const deleteHomework = (homeworkId: number) =>
  request.delete<any, { code: number; message: string }>(`/api/homework/${homeworkId}`)

export interface StudentHomeworkItem {
  homeworkId: number
  submissionId: number | null
  title: string
  content: string
  deadline: string
  status: string
  scoreGained: number | null
  submitTime: string | null
  version: number
}

export const getStudentHomeworkList = () =>
  request.get<any, { code: number; data: StudentHomeworkItem[] }>('/api/student/homework')

export interface SubmitHomeworkDTO {
  content: string
  gitLink?: string
  fileUrl?: string
}

export const submitHomework = (homeworkId: number, data: SubmitHomeworkDTO) =>
  request.post<any, { code: number; message: string }>(`/api/homework/${homeworkId}/submit`, data)

export interface SubmissionHistoryItem {
  submissionId: number
  version: number
  status: string
  content: string
  gitLink: string
  fileUrl: string
  scoreGained: number | null
  teacherComment: string | null
  assistantComment: string | null
  submitTime: string
}

export const getSubmissionHistory = (homeworkId: number, submissionId: number) =>
  request.get<any, { code: number; data: SubmissionHistoryItem[] }>(
    `/api/homework/${homeworkId}/submission/${submissionId}/history`
  )

export interface GradeHomeworkDTO {
  scoreGained: number
  teacherComment?: string
}

export const gradeHomework = (homeworkId: number, submissionId: number, data: GradeHomeworkDTO) =>
  request.put<any, { code: number; message: string }>(
    `/api/homework/${homeworkId}/submission/${submissionId}/grade`,
    data
  )

export interface ReturnHomeworkDTO {
  teacherComment: string
}

export const returnHomework = (homeworkId: number, submissionId: number, data: ReturnHomeworkDTO) =>
  request.put<any, { code: number; message: string }>(
    `/api/homework/${homeworkId}/submission/${submissionId}/return`,
    data
  )

export interface HomeworkSubmissionItem {
  submissionId: number
  studentId: number
  studentName: string
  status: string
  submitTime: string
  scoreGained: number | null
  switchScreenCount: number
  assistantComment?: string | null
}

export const getHomeworkSubmissions = (homeworkId: number) =>
  request.get<any, { code: number; data: HomeworkSubmissionItem[] }>(
    `/api/homework/${homeworkId}/submissions`
  )

export interface SubmissionContentItem {
  questionId: number
  type: string
  content: string
  studentAnswer: string
  standardAnswer: string
  isCorrect: boolean | null
  scoreGained: number | null
  questionScore: number
}

export interface HomeworkSubmissionDetail {
  submissionId: number
  studentId: number
  studentName: string
  status: string
  totalScoreGained: number | null
  submitTime: string
  content: string
  gitLink: string
  fileUrl: string
  teacherComment: string | null
  assistantComment?: string | null
}

export const getHomeworkSubmissionDetail = (homeworkId: number, submissionId: number) =>
  request.get<any, { code: number; data: HomeworkSubmissionDetail }>(
    `/api/homework/${homeworkId}/submission/${submissionId}`
  )

export interface UnsubmittedStudentItem {
  studentId: number
  studentName: string
  username: string
}

export interface HomeworkProgress {
  homeworkId: number
  title: string
  totalStudents: number
  submittedCount: number
  pendingCount: number
  unsubmittedStudents: UnsubmittedStudentItem[]
}

export const getAssistantHomeworkProgress = (homeworkId: number) =>
  request.get<any, { code: number; data: HomeworkProgress }>(
    `/api/assistant/homework/${homeworkId}/progress`
  )

export const getAssistantHomeworkSubmissions = (homeworkId: number) =>
  request.get<any, { code: number; data: HomeworkSubmissionItem[] }>(
    `/api/assistant/homework/${homeworkId}/submissions`
  )

export const getAssistantHomeworkSubmissionDetail = (homeworkId: number, submissionId: number) =>
  request.get<any, { code: number; data: HomeworkSubmissionDetail }>(
    `/api/assistant/homework/${homeworkId}/submission/${submissionId}`
  )

export const remindAssistantStudents = (homeworkId: number, studentIds: number[]) =>
  request.post<any, { code: number; message: string }>(
    `/api/assistant/homework/${homeworkId}/remind`,
    { studentIds }
  )

export const addAssistantSubmissionComment = (submissionId: number, comment: string) =>
  request.post<any, { code: number; message: string }>(
    `/api/assistant/submission/${submissionId}/comment`,
    { comment }
  )
