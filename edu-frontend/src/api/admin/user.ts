import request from '@/utils/request'

export interface StudentClassHistory {
  classId: number
  className: string
  status: string
  joinStatus: string
  joinTime: string
  leaveTime?: string
}

export interface UserRecord {
  id: number
  username: string
  phone?: string
  schoolName?: string
  realName: string
  role: 'TEACHER' | 'ASSISTANT' | 'STUDENT' | 'ADMIN'
  classId?: number
  className?: string
  createdBy?: number
  createTime: string
  classHistory?: StudentClassHistory[]
}

export interface UserListResult {
  records: UserRecord[]
  total: number
  size: number
  current: number
}

export interface UserStats {
  total: number
  teachers: number
  assistants: number
  students: number
}

export interface CreateUserParams {
  username: string
  realName: string
  role: 'TEACHER' | 'ASSISTANT'
}

export interface CreateUserResult {
  id: number
  username: string
  realName: string
  role: string
  initialPassword?: string | null
}

export interface BatchImportResult {
  success: number
  failed: number
  errors: string[]
  credentials?: Array<{
    username: string
    realName: string
    initialPassword: string
  }>
}

export interface ResetPasswordResult {
  userId: number
  username: string
  newPassword: string
  message: string
}

export const getUserList = (params: { page: number; size: number; role?: string; keyword?: string }) =>
  request.get<any, { code: number; data: UserListResult }>('/admin/user/list', { params })

export const getUserStats = () =>
  request.get<any, { code: number; data: UserStats }>('/admin/user/stats')

export const createUser = (data: CreateUserParams) =>
  request.post<any, { code: number; data: CreateUserResult }>('/admin/user', data)

export const batchImportUsers = (file: File) => {
  const form = new FormData()
  form.append('file', file)
  return request.post<any, { code: number; data: BatchImportResult }>('/admin/user/batch', form)
}

export const resetPassword = (id: number) =>
  request.put<any, { code: number; data: ResetPasswordResult }>(`/admin/user/${id}/reset-password`)

export const deleteUser = (id: number) =>
  request.delete<any, { code: number }>(`/admin/user/${id}`)
