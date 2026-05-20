import request from '@/utils/request'
import type { CourseCategory, Difficulty, QuestionType } from './question'

export interface OutlineDocument {
  id: number
  title: string
  originalFilename: string
  courseCategory: CourseCategory
  outlineText: string
  status: string
  createTime: string
}

export interface OutlineQuestion {
  id: number
  documentId: number
  courseCategory: CourseCategory
  knowledgePoint: string
  type: QuestionType
  difficulty: Difficulty
  content: string
  optionsJson?: string | null
  standardAnswer: string
  analysis?: string | null
  selected: boolean
  questionId?: number | null
}

export interface OutlineConfirmParams {
  createExam: boolean
  createTemplate?: boolean
  examTitle?: string
  templateName?: string
  startTime?: string
  endTime?: string
  targetClassIds?: number[]
}

export interface OutlineConfirmResult {
  savedCount: number
  questionIds: number[]
  examId?: number | null
  templateId?: number | null
}

export const uploadOutline = (data: { file: File; title?: string; courseCategory: CourseCategory }) => {
  const form = new FormData()
  form.append('file', data.file)
  form.append('courseCategory', data.courseCategory)
  if (data.title) form.append('title', data.title)
  return request.post<any, { code: number; data: OutlineDocument }>('/api/outline/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

export const generateOutlineQuestions = (id: number) =>
  request.post<any, { code: number; data: OutlineQuestion[] }>(`/api/outline/${id}/generate-questions`, {}, { timeout: 180000 })

export const deleteOutline = (id: number) =>
  request.delete<any, { code: number; message: string }>(`/api/outline/${id}`)

export const updateOutlineQuestion = (id: number, data: Partial<OutlineQuestion>) =>
  request.put<any, { code: number; data: OutlineQuestion }>(`/api/outline/questions/${id}`, data)

export const confirmOutline = (id: number, data: OutlineConfirmParams) =>
  request.post<any, { code: number; data: OutlineConfirmResult }>(`/api/outline/${id}/confirm`, data, { timeout: 60000 })
