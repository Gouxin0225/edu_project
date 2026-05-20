import request from '@/utils/request'

export interface AiKnowledgeDocument {
  id: number
  title: string
  courseCategory: string
  knowledgePoint?: string
  sourceType: 'TEXT' | 'FILE'
  sourceName?: string
  visibility: 'PRIVATE' | 'PUBLIC'
  accessScope: 'STUDENT_SAFE' | 'TEACHER_ONLY'
  ownerId: number
  ownerRole: string
  chunkCount: number
  createTime: string
  updateTime: string
}

export interface AiKnowledgeCreatePayload {
  title: string
  courseCategory: string
  knowledgePoint?: string
  content: string
  visibility: 'PRIVATE' | 'PUBLIC'
  accessScope: 'STUDENT_SAFE' | 'TEACHER_ONLY'
}

export const createKnowledgeDocument = (data: AiKnowledgeCreatePayload) =>
  request.post<any, { code: number; data: AiKnowledgeDocument }>('/api/ai-knowledge/documents', data)

export const uploadKnowledgeDocument = (data: {
  file: File
  title?: string
  courseCategory: string
  knowledgePoint?: string
  visibility: 'PRIVATE' | 'PUBLIC'
  accessScope: 'STUDENT_SAFE' | 'TEACHER_ONLY'
}) => {
  const form = new FormData()
  form.append('file', data.file)
  if (data.title) form.append('title', data.title)
  form.append('courseCategory', data.courseCategory)
  if (data.knowledgePoint) form.append('knowledgePoint', data.knowledgePoint)
  form.append('visibility', data.visibility)
  form.append('accessScope', data.accessScope)
  return request.post<any, { code: number; data: AiKnowledgeDocument }>('/api/ai-knowledge/documents/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export const getKnowledgeDocuments = (params?: { courseCategory?: string }) =>
  request.get<any, { code: number; data: AiKnowledgeDocument[] }>('/api/ai-knowledge/documents', { params })

export const deleteKnowledgeDocument = (id: number) =>
  request.delete<any, { code: number }>(`/api/ai-knowledge/documents/${id}`)
