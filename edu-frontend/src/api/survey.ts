import request from '@/utils/request'

export interface QuestionConfig {
  type: string
  title: string
  optionsJson?: string
  isRequired?: number
  sortOrder?: number
}

export interface CreateSurveyDTO {
  title: string
  endTime: string
  targetClassIds: number[]
  isAnonymousRequired: number
  targetTeacherId: number
  questions?: QuestionConfig[]
}

export interface SurveyListItem {
  surveyId: number
  title: string
  endTime: string
  targetTeacherName: string
  isAnonymousRequired: number
  status: number
  totalQuestions: number
  totalSubmissions: number
}

export const getSurveyList = () =>
  request.get<any, { code: number; data: SurveyListItem[] }>('/api/survey')

export const createSurvey = (data: CreateSurveyDTO) =>
  request.post<any, { code: number; data: number }>('/api/survey', data)

export const publishSurvey = (id: number) =>
  request.post<any, { code: number; message: string }>(`/api/survey/${id}/publish`)

export const deleteSurvey = (id: number) =>
  request.delete<any, { code: number; message: string }>(`/api/survey/${id}`)

export interface QuestionStatistics {
  questionId: number
  title: string
  type: string
  avgScore: number | null
  distribution: { value: number; count: number; rate: number }[]
  textAnswers: string[]
}

export interface NpsData {
  score: number
  promoterRate: number
  detractorRate: number
}

export interface RadarDataItem {
  questionTitle: string
  avgScore: number
}

export interface SurveyStatistics {
  surveyId: number
  title: string
  totalCount: number
  nps: NpsData
  radarData: RadarDataItem[]
  questions: QuestionStatistics[]
}

export const getSurveyStatistics = (id: number) =>
  request.get<any, { code: number; data: SurveyStatistics }>(`/api/survey/${id}/statistics`)

export interface StudentSurveyItem {
  surveyId: number
  title: string
  endTime: string
  targetTeacherName: string
  isAnonymousRequired: number
  status: string
  alreadySubmitted: boolean
  totalQuestions: number
}

export const getStudentSurveyList = () =>
  request.get<any, { code: number; data: StudentSurveyItem[] }>('/api/student/survey')

export interface SurveyQuestion {
  questionId: number
  type: string
  title: string
  optionsJson: string
  isRequired: number
  sortOrder: number
}

export interface SurveyDetail {
  surveyId: number
  title: string
  targetTeacherName: string
  isAnonymousRequired: number
  questions: SurveyQuestion[]
}

export const getSurveyDetail = (id: number) =>
  request.get<any, { code: number; data: SurveyDetail }>(`/api/survey/${id}/detail`)

export interface SurveyAnswer {
  questionId: number
  answerValue: string
}

export interface SubmitSurveyDTO {
  surveyId: number
  answers: SurveyAnswer[]
}

export const submitSurvey = (id: number, data: SubmitSurveyDTO) =>
  request.post<any, { code: number; message: string }>(`/api/survey/${id}/submit`, data)

export interface TeacherOption {
  id: number
  username: string
  realName: string
  role: 'TEACHER'
}

export const getTeacherOptions = () =>
  request.get<any, { code: number; data: TeacherOption[] }>('/api/teachers')
