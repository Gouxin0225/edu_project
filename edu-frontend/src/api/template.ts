import request from '@/utils/request'

export interface TemplateQuestion {
  questionId: number
  courseCategory?: string
  knowledgePoint?: string
  type: string
  difficulty: string
  content: string
  optionsJson: string | null
  standardAnswer?: string
  analysis?: string | null
  score: number
}

export interface Template {
  id: number
  name: string
  description: string
  courseId: number | null
  courseName: string | null
  totalScore: number
  questionCount: number
  creatorId: number
  creatorName: string
  createdAt: string
  questions?: TemplateQuestion[]
}

export interface CreateTemplateData {
  name: string
  description?: string
  courseId?: number
  courseName?: string
  questionIds: number[]
  scores?: number[]
  totalScore?: number
}

export interface CreateExamFromTemplateData {
  templateId: number
  title: string
  startTime?: string
  endTime: string
  targetClassIds?: number[]
  type?: string
  settingJson?: string
  duration?: number
  totalScore?: number
  passScore?: number
}

export const getTemplateList = (params: { page: number; size: number }) =>
  request.get<any, { code: number; data: { records: Template[]; total: number; size: number; current: number } }>('/api/template', { params })

export const getTemplateDetail = (id: number) =>
  request.get<any, { code: number; data: Template }>(`/api/template/${id}`)

export const getTemplateQuestions = (id: number) =>
  request.get<any, { code: number; data: TemplateQuestion[] }>(`/api/template/${id}/questions`)

export const createTemplate = (data: CreateTemplateData) =>
  request.post<any, { code: number; data: Template }>('/api/template', data)

export const deleteTemplate = (id: number) =>
  request.delete<any, { code: number; message: string }>(`/api/template/${id}`)

export const createExamFromTemplate = (data: CreateExamFromTemplateData) =>
  request.post<any, { code: number; data: { id: number; title: string } }>('/api/template/create-exam', data)
