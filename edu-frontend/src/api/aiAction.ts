import request from '@/utils/request'

export type AiResultActionType = 'CREATE_QUESTION' | 'CREATE_REVIEW_NOTE'

export interface AiResultActionPreviewPayload {
  actionType: AiResultActionType
  sourceMessageId?: number
  sourceContent?: string
  courseCategory?: string
  knowledgePoint?: string
}

export interface AiResultActionConfirmPayload {
  actionType: AiResultActionType
  sourceMessageId?: number
  data: Record<string, any>
}

export interface AiResultActionPreview {
  actionType: AiResultActionType
  sourceMessageId?: number
  objectType: 'QUESTION' | 'REVIEW_NOTE'
  data: Record<string, any>
  warnings?: string[]
}

export interface AiResultActionConfirmResult {
  actionType: AiResultActionType
  objectType: 'QUESTION' | 'REVIEW_NOTE'
  objectId: number
  message: string
}

export interface StudentReviewNote {
  id: number
  studentId: number
  sourceMessageId?: number
  courseCategory: string
  knowledgePoint: string
  title: string
  content: string
  sourceType: string
  createTime: string
  updateTime: string
}

export const previewAiResultAction = (data: AiResultActionPreviewPayload) =>
  request.post<any, { code: number; data: AiResultActionPreview }>('/api/ai-actions/preview', data)

export const confirmAiResultAction = (data: AiResultActionConfirmPayload) =>
  request.post<any, { code: number; data: AiResultActionConfirmResult }>('/api/ai-actions/confirm', data)

export const getStudentReviewNotes = () =>
  request.get<any, { code: number; data: StudentReviewNote[] }>('/api/ai-actions/review-notes')
