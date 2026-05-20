import request from '@/utils/request'

export type VisitMethod = 'PHONE' | 'WECHAT' | 'OFFLINE' | 'ONLINE' | 'OTHER'
export type VisitResult = 'REACHED' | 'UNREACHED' | 'NEED_FOLLOW' | 'RESOLVED'

export interface StudentVisitRecord {
  id: number
  studentId: number
  studentName: string
  studentUsername: string
  classId: number
  className: string
  visitorId: number
  visitorName: string
  visitorRole: 'TEACHER' | 'ASSISTANT'
  visitMethod: VisitMethod
  visitResult: VisitResult
  content: string
  nextFollowTime: string | null
  visitTime: string
  createTime: string
}

export interface CreateStudentVisitRecordDTO {
  studentId: number
  visitMethod: VisitMethod
  visitResult: VisitResult
  content: string
  nextFollowTime?: string | null
  visitTime?: string | null
}

export const getStudentVisitRecords = (params?: { classId?: number; studentId?: number }) =>
  request.get<any, { code: number; data: StudentVisitRecord[] }>('/api/student-visits', { params })

export const createStudentVisitRecord = (data: CreateStudentVisitRecordDTO) =>
  request.post<any, { code: number; data: number }>('/api/student-visits', data)
