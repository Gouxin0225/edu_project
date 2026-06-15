import request from '@/utils/request'
import type { ExamParticipation, SubmissionDetail, SubmissionItem } from './exam'
import type { SurveyListItem } from './survey'

export interface AssistantExamItem {
  id: number
  title: string
  startTime: string
  endTime: string
  totalScore: number
  status: string
  targetClassNames: string[]
  targetStudentCount: number
  submittedCount: number
  inProgressCount: number
  notSubmittedCount: number
  averageScore: number
  passRate: number
}

export const getAssistantExamList = () =>
  request.get<any, { code: number; data: AssistantExamItem[] }>('/api/assistant/exams')

export const getAssistantExamParticipation = (examId: number) =>
  request.get<any, { code: number; data: ExamParticipation }>(`/api/assistant/exam/${examId}/participation`)

export const getAssistantExamSubmissions = (examId: number) =>
  request.get<any, { code: number; data: SubmissionItem[] }>(`/api/assistant/exam/${examId}/submissions`)

export const getAssistantExamSubmissionDetail = (examId: number, submissionId: number) =>
  request.get<any, { code: number; data: SubmissionDetail }>(`/api/assistant/exam/${examId}/submission/${submissionId}`)

export const remindAssistantExamStudents = (examId: number, studentIds: number[]) =>
  request.post<any, { code: number; data: null }>(`/api/assistant/exam/${examId}/remind`, { studentIds })

export const getAssistantSurveyList = () =>
  request.get<any, { code: number; data: SurveyListItem[] }>('/api/assistant/surveys')

export interface AssistantSurveyResponses {
  surveyId: number
  title: string
  isAnonymousRequired: number
  totalStudents: number
  submittedCount: number
  unsubmittedCount: number
  responses: AssistantSurveyResponseItem[]
  unsubmittedStudents: AssistantSurveyStudentItem[]
}

export interface AssistantSurveyResponseItem {
  recordId: number
  studentId?: number
  studentName: string
  username?: string
  className: string
  submitTime: string
  answers: AssistantSurveyAnswerItem[]
}

export interface AssistantSurveyAnswerItem {
  questionId: number
  title: string
  type: string
  answerValue: string
}

export interface AssistantSurveyStudentItem {
  studentId: number
  studentName: string
  username: string
  className: string
}

export const getAssistantSurveyResponses = (surveyId: number) =>
  request.get<any, { code: number; data: AssistantSurveyResponses }>(`/api/assistant/survey/${surveyId}/responses`)

export const remindAssistantSurveyStudents = (surveyId: number, studentIds: number[]) =>
  request.post<any, { code: number; data: null }>(`/api/assistant/survey/${surveyId}/remind`, { studentIds })
