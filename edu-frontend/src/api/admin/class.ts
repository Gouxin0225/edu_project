import request from '@/utils/request'
import { getUserList } from './user'
import type { UserRecord } from './user'

export interface ClassRecord {
  id: number
  className: string
  status: 0 | 1          // 1-进行中  0-已结课
  teacherNames?: string[] // 已分配讲师姓名列表（详情接口返回）
  teacherIds?: number[]
  assistantNames?: string[]
  assistantIds?: number[]
  createTime?: string
}

export interface ClassListResult {
  records: ClassRecord[]
  total: number
  size: number
  current: number
}

// ──────────────────────────────────────────
// 已有接口
// ──────────────────────────────────────────

/** 创建班级 */
export const createClass = (className: string) =>
  request.post<any, { code: number; data: ClassRecord }>('/admin/class', { className })

/** 为班级分配讲师或班主任 */
export const assignTeacher = (classId: number, teacherId: number) =>
  request.post<any, { code: number; message: string }>(`/admin/class/${classId}/teacher`, { teacherId })

/** 移除班级讲师或班主任 */
export const removeTeacher = (classId: number, teacherId: number) =>
  request.delete<any, { code: number; message: string }>(`/admin/class/${classId}/teacher/${teacherId}`)

/** 获取可分配到班级的人员列表（讲师 + 班主任） */
export const getAssignableUserOptions = async (): Promise<UserRecord[]> => {
  const [teacherRes, assistantRes] = await Promise.all([
    getUserList({ page: 1, size: 500, role: 'TEACHER' }),
    getUserList({ page: 1, size: 500, role: 'ASSISTANT' })
  ])
  return [...teacherRes.data.records, ...assistantRes.data.records]
}

export const getClassList = async (params: {
  page: number
  size: number
  keyword?: string
  status?: 0 | 1 | ''
  teacherId?: number
}): Promise<{ data: ClassListResult }> => {
  return request.get<any, { code: number; data: ClassListResult }>('/admin/class', {
    params: {
      pageNum: params.page,
      pageSize: params.size,
      keyword: params.keyword,
      status: params.status === '' ? undefined : params.status,
      teacherId: params.teacherId
    }
  })
}

export const getClassDetail = (id: number): Promise<{ data: ClassRecord }> => {
  return getClassList({ page: 1, size: 1000 }).then(res => {
    const found = res.data.records.find(c => c.id === id)
    return { data: found ?? { id, className: '', status: 1, teacherNames: [], teacherIds: [], assistantNames: [], assistantIds: [] } }
  })
}
