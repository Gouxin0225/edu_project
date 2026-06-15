import request from '@/utils/request'
import type { CourseLevel } from './scheduleCourse'
import type { ProgressStatus } from './scheduleCampusProgress'

export interface SchedulingCoursePoolRecord {
  progressId: number
  campusGradeId: number
  schoolName?: string
  gradeName?: string
  courseId: number
  courseCode?: string
  courseName?: string
  courseDirection?: string
  courseLevel?: CourseLevel
  expectedStudentCount?: number
  progressStatus: ProgressStatus
  canSchedule: boolean
  blockedReasons: string[]
  teacherCapacity?: number
  suggestedTeacherCount: number
}

export interface SchedulingCoursePoolResult {
  records: SchedulingCoursePoolRecord[]
  total: number
  size: number
  current: number
}

export interface DispatchPlanCreatePayload {
  campusCourseProgressId: number
  plannedStartDate: string
  plannedEndDate: string
  teacherIds: number[]
  remark?: string
}

export interface DispatchPlanRecord {
  id: number
  planNo: string
  planStatus: string
  warning?: string
  warnings?: string[]
}

export const getSchedulingCoursePool = (params: {
  pageNum: number
  pageSize: number
  schoolName?: string
  campusGradeId?: number
  courseId?: number
  keyword?: string
}) =>
  request.get<any, { code: number; data: SchedulingCoursePoolResult }>('/api/scheduling/course-pool', {
    params: {
      pageNum: params.pageNum,
      pageSize: params.pageSize,
      schoolName: params.schoolName || undefined,
      campusGradeId: params.campusGradeId || undefined,
      courseId: params.courseId || undefined,
      keyword: params.keyword || undefined
    }
  })

export const createDispatchPlan = (data: DispatchPlanCreatePayload) =>
  request.post<any, { code: number; data: DispatchPlanRecord }>('/api/scheduling/dispatch-plans', data)
