import request from '@/utils/request'

export type AiChatMode = 'SIMPLE' | 'DETAILED' | 'LESSON_PLAN' | 'QUESTION'
export type AiChatResponseSource = 'AUTO' | 'PLATFORM_ONLY' | 'KNOWLEDGE_ONLY' | 'GENERAL_AI'
export type AiChatIntent = 'LEARN' | 'PLATFORM_QUERY' | 'KNOWLEDGE_QA' | 'QUESTION_GEN' | 'QUESTION_REVIEW' | 'GENERAL'

export interface AiChatMessagePayload {
  courseCategory?: string
  knowledgePoint?: string
  question: string
  mode: AiChatMode
  responseSource?: AiChatResponseSource
  intent?: AiChatIntent
}

export interface AiChatMessageResult {
  sessionId?: number
  userMessageId?: number
  assistantMessageId?: number
  sessionTitle?: string
  courseCategory: string
  knowledgePoint: string
  question: string
  answer: string
  mode: AiChatMode
  intent?: AiChatIntent
  responseSource?: AiChatResponseSource
  sourceSummary?: string
  role: 'ADMIN' | 'TEACHER' | 'ASSISTANT' | 'STUDENT'
  model: string
  status?: 'generating' | 'finished' | 'interrupted' | 'failed'
  createTime: string
}

export interface AiChatSessionPayload {
  title?: string
  courseCategory?: string
  knowledgePoint?: string
}

export interface AiChatSession {
  id: number
  title: string
  courseCategory?: string
  knowledgePoint?: string
  userRole: 'ADMIN' | 'TEACHER' | 'ASSISTANT' | 'STUDENT'
  isTop: boolean
  lastMessageTime: string
  createTime: string
  updateTime: string
}

export interface AiChatHistoryMessage {
  id: number
  sessionId: number
  messageRole: 'USER' | 'ASSISTANT'
  content: string
  courseCategory?: string
  knowledgePoint?: string
  mode?: AiChatMode
  intent?: AiChatIntent
  responseSource?: AiChatResponseSource
  sourceSummary?: string
  model?: string
  status?: 'generating' | 'finished' | 'interrupted' | 'failed'
  createTime: string
}

export const sendAiChatMessage = (data: AiChatMessagePayload) =>
  request.post<any, { code: number; data: AiChatMessageResult }>(
    '/api/ai-chat/message',
    data,
    { timeout: 120000 }
  )

export const createAiChatSession = (data: AiChatSessionPayload = {}) =>
  request.post<any, { code: number; data: AiChatSession }>('/api/ai-chat/sessions', data)

export const getAiChatSessions = () =>
  request.get<any, { code: number; data: AiChatSession[] }>('/api/ai-chat/sessions')

export const getAiChatSessionMessages = (sessionId: number) =>
  request.get<any, { code: number; data: AiChatHistoryMessage[] }>(`/api/ai-chat/sessions/${sessionId}/messages`)

export const sendAiChatSessionMessage = (sessionId: number, data: AiChatMessagePayload) =>
  request.post<any, { code: number; data: AiChatMessageResult }>(
    `/api/ai-chat/sessions/${sessionId}/message`,
    data,
    { timeout: 120000 }
  )

export const deleteAiChatSession = (sessionId: number) =>
  request.delete<any, { code: number }>(`/api/ai-chat/sessions/${sessionId}`)

export const updateAiChatSession = (sessionId: number, data: { title?: string; isTop?: boolean }) =>
  request.put<any, { code: number; data: AiChatSession }>(`/api/ai-chat/sessions/${sessionId}`, data)

export interface AiChatStreamEvent {
  event: 'start' | 'delta' | 'done' | 'error' | 'interrupted'
  data: any
}

export async function streamAiChatSessionMessage(
  sessionId: number,
  data: AiChatMessagePayload,
  onEvent: (event: AiChatStreamEvent) => void,
  signal?: AbortSignal
) {
  return postSse(`/api/ai-chat/sessions/${sessionId}/message/stream`, data, onEvent, signal)
}

export async function regenerateAiChatSessionMessage(
  sessionId: number,
  onEvent: (event: AiChatStreamEvent) => void,
  signal?: AbortSignal
) {
  return postSse(`/api/ai-chat/sessions/${sessionId}/message/regenerate/stream`, {}, onEvent, signal)
}

export const cancelAiChatStream = (sessionId: number) =>
  request.post<any, { code: number }>(`/api/ai-chat/sessions/${sessionId}/message/stream/cancel`)

async function postSse(
  url: string,
  data: any,
  onEvent: (event: AiChatStreamEvent) => void,
  signal?: AbortSignal
) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(localStorage.getItem('token') ? { Authorization: `Bearer ${localStorage.getItem('token')}` } : {})
    },
    body: JSON.stringify(data),
    signal
  })

  if (!response.ok || !response.body) {
    const text = await response.text().catch(() => '')
    throw new Error(extractErrorMessage(text) || 'AI流式请求失败')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split(/\n\n/)
    buffer = chunks.pop() || ''
    for (const chunk of chunks) {
      const parsed = parseSseChunk(chunk)
      if (parsed) onEvent(parsed)
    }
  }

  if (buffer.trim()) {
    const parsed = parseSseChunk(buffer)
    if (parsed) onEvent(parsed)
  }
}

function extractErrorMessage(text: string) {
  if (!text) return ''
  try {
    const parsed = JSON.parse(text)
    return parsed?.message || parsed?.data?.message || text
  } catch {
    return text
  }
}

function parseSseChunk(chunk: string): AiChatStreamEvent | null {
  let event = 'message'
  const dataLines: string[] = []
  for (const line of chunk.split(/\n/)) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }
  if (!dataLines.length) return null
  try {
    return { event: event as AiChatStreamEvent['event'], data: JSON.parse(dataLines.join('\n')) }
  } catch {
    return { event: event as AiChatStreamEvent['event'], data: dataLines.join('\n') }
  }
}
