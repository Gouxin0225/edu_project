import request from '@/utils/request'

// ─── 枚举常量 ───────────────────────────────────────────
export const QUESTION_TYPES = [
  { value: 'SINGLE',   label: '单选题' },
  { value: 'MULTIPLE', label: '多选题' },
  { value: 'JUDGE',    label: '判断题' },
  { value: 'SHORT',    label: '简答题' },
  { value: 'CODE',     label: '代码题' }
] as const

export const COURSE_CATEGORIES = [
  { value: 'python', label: 'Python' },
  { value: 'mysql', label: 'MySQL' },
  { value: 'csa', label: 'CSA' },
  { value: 'hcia', label: 'HCIA' },
  { value: 'hcip', label: 'HCIP' },
  { value: 'web', label: 'Web' },
  { value: 'rhce', label: 'RHCE' }
] as const

export const DIFFICULTIES = [
  { value: 'EASY',   label: '简单' },
  { value: 'MEDIUM', label: '中等' },
  { value: 'HARD',   label: '困难' }
] as const

export type QuestionType = typeof QUESTION_TYPES[number]['value']
export type CourseCategory = typeof COURSE_CATEGORIES[number]['value']
export type Difficulty    = typeof DIFFICULTIES[number]['value']

// ─── 数据类型 ────────────────────────────────────────────
export interface QuestionRecord {
  id: number
  courseCategory: string
  knowledgePoint: string
  type: QuestionType
  difficulty: Difficulty
  content: string
  optionsJson?: string
  standardAnswer: string
  analysis?: string
  creatorId: number
  isAiGenerated?: 0 | 1
  isPublic?: 0 | 1
  createTime: string
}

export interface QuestionListResult {
  records: QuestionRecord[]
  total: number
  size: number
  current: number
}

export interface QuestionPayload {
  courseCategory: string
  knowledgePoint: string
  type: QuestionType
  difficulty: Difficulty
  content: string
  optionsJson?: string
  standardAnswer: string
  analysis?: string
}

export interface ImportResult {
  success: number
  failed: number
  errors: string[]
}

export interface AiGenerateParams {
  courseCategory: CourseCategory
  knowledgePoint?: string
  types: QuestionType[]
  difficulty: Difficulty
  count: number
}

// ─── 接口 ────────────────────────────────────────────────
export const getQuestionList = (params: {
  page: number; size: number
  courseCategory?: string; type?: string; difficulty?: string; creatorId?: number
}) => request.get<any, { code: number; data: QuestionListResult }>('/api/question', { params })

export const createQuestion = (data: QuestionPayload) =>
  request.post<any, { code: number; data: QuestionRecord }>('/api/question', data)

export const updateQuestion = (id: number, data: QuestionPayload) =>
  request.put<any, { code: number; data: QuestionRecord }>(`/api/question/${id}`, data)

export const deleteQuestion = (id: number) =>
  request.delete<any, { code: number }>(`/api/question/${id}`)

export const importQuestions = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<any, { code: number; data: ImportResult }>('/api/question/import', form)
}

export const aiGenerateQuestions = (data: AiGenerateParams) =>
  request.post<any, { code: number; data: { message: string; count: number } }>(
    '/api/question/ai-generate', data, { timeout: 90000 }
  )

export const publishQuestion = (id: number) =>
  request.post<any, { code: number }>(`/api/admin/question/${id}/publish`)

export const getQuestionCreators = () =>
  request.get<any, { code: number; data: { id: number; realName: string; role: string }[] }>('/api/question/creators')
