import request from '@/utils/request'

export type CourseLevel = 'BASIC' | 'INTERMEDIATE' | 'ADVANCED'
export type CourseType = 'FIXED' | 'CUSTOM'
export type CourseStatus = 'ACTIVE' | 'DISABLED'

export interface ScheduleCourseRecord {
  id: number
  courseCode: string
  courseName: string
  courseDirection: string
  courseLevel: CourseLevel
  courseType: CourseType
  teacherCapacity: number
  sortOrder?: number
  status: CourseStatus
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ScheduleCourseListResult {
  records: ScheduleCourseRecord[]
  total: number
  size: number
  current: number
}

export interface ScheduleCoursePayload {
  courseCode: string
  courseName: string
  courseDirection: string
  courseLevel: CourseLevel
  courseType: CourseType
  teacherCapacity: number
  sortOrder?: number
  status?: CourseStatus
  remark?: string
}

export const getScheduleCourseList = (params: {
  pageNum: number
  pageSize: number
  keyword?: string
  courseLevel?: CourseLevel | ''
  courseType?: CourseType | ''
  status?: CourseStatus | ''
}) =>
  request.get<any, { code: number; data: ScheduleCourseListResult }>('/admin/schedule/courses', {
    params: {
      pageNum: params.pageNum,
      pageSize: params.pageSize,
      keyword: params.keyword || undefined,
      courseLevel: params.courseLevel || undefined,
      courseType: params.courseType || undefined,
      status: params.status || undefined
    }
  })

export const createScheduleCourse = (data: ScheduleCoursePayload) =>
  request.post<any, { code: number; data: ScheduleCourseRecord }>('/admin/schedule/courses', data)

export const updateScheduleCourse = (id: number, data: ScheduleCoursePayload) =>
  request.put<any, { code: number; data: ScheduleCourseRecord }>(`/admin/schedule/courses/${id}`, data)

export const updateScheduleCourseStatus = (id: number, status: CourseStatus) =>
  request.patch<any, { code: number; message: string }>(`/admin/schedule/courses/${id}/status`, null, {
    params: { status }
  })
