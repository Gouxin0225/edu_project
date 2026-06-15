import request from '@/utils/request'

export type DispatchStatus = 'AVAILABLE' | 'PAUSED' | 'UNAVAILABLE'
export type SkillLevel = 'MAIN' | 'TEACHABLE' | 'ASSIST'
export type SkillStatus = 'ACTIVE' | 'DISABLED'

export interface TeacherSkill {
  id?: number
  teacherId: number
  courseId: number
  courseName?: string
  skillLevel: SkillLevel
  certified?: 0 | 1
  status: SkillStatus
  remark?: string
}

export interface TeacherDispatchRecord {
  teacherId: number
  username: string
  teacherName: string
  phone?: string
  schoolName?: string
  profileId?: number
  homeSchoolName?: string
  dispatchStatus: DispatchStatus
  canTravel: 0 | 1
  isCenterReserve: 0 | 1
  maxParallelPlans?: number
  maxMonthlyDispatchDays?: number
  priority?: number
  availableRemark?: string
  createTime?: string
  updateTime?: string
  skills: TeacherSkill[]
}

export interface TeacherDispatchListResult {
  records: TeacherDispatchRecord[]
  total: number
  size: number
  current: number
}

export interface TeacherProfilePayload {
  homeSchoolName?: string
  dispatchStatus: DispatchStatus
  canTravel: 0 | 1
  isCenterReserve: 0 | 1
  maxParallelPlans?: number
  maxMonthlyDispatchDays?: number
  priority?: number
  availableRemark?: string
}

export interface TeacherSkillPayload {
  courses: Array<{
    courseId: number
    skillLevel: SkillLevel
    certified?: 0 | 1
    status?: SkillStatus
    remark?: string
  }>
}

export const getTeacherCapabilityList = (params: {
  pageNum: number
  pageSize: number
  keyword?: string
  dispatchStatus?: DispatchStatus | ''
  courseId?: number
  canTravel?: 0 | 1 | ''
  isCenterReserve?: 0 | 1 | ''
}) =>
  request.get<any, { code: number; data: TeacherDispatchListResult }>('/admin/schedule/teachers', {
    params: {
      pageNum: params.pageNum,
      pageSize: params.pageSize,
      keyword: params.keyword || undefined,
      dispatchStatus: params.dispatchStatus || undefined,
      courseId: params.courseId || undefined,
      canTravel: params.canTravel === '' ? undefined : params.canTravel,
      isCenterReserve: params.isCenterReserve === '' ? undefined : params.isCenterReserve
    }
  })

export const saveTeacherProfile = (teacherId: number, data: TeacherProfilePayload) =>
  request.put<any, { code: number; data: TeacherDispatchRecord }>(`/admin/schedule/teachers/${teacherId}/profile`, data)

export const saveTeacherSkills = (teacherId: number, data: TeacherSkillPayload) =>
  request.put<any, { code: number; data: TeacherDispatchRecord }>(`/admin/schedule/teachers/${teacherId}/skills`, data)
