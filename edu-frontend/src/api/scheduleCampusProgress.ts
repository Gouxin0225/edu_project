import request from '@/utils/request'

export type ProgressStatus = 'NOT_STARTED' | 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'NEED_MAKEUP' | 'CANCELED'

export interface CampusCourseProgressRecord {
  id: number
  campusGradeId: number
  schoolName?: string
  gradeName?: string
  courseId: number
  courseName?: string
  courseCode?: string
  expectedStudentCount: number
  progressStatus: ProgressStatus
  progressPercent?: number
  plannedStartDate?: string
  plannedEndDate?: string
  actualStartDate?: string
  actualEndDate?: string
  currentTeacherId?: number
  currentTeacherName?: string
  lastPlanId?: number
  remark?: string
  canSchedule?: boolean
  blockedReasons?: string[]
  createTime?: string
  updateTime?: string
}

export interface CampusCourseProgressListResult {
  records: CampusCourseProgressRecord[]
  total: number
  size: number
  current: number
}

export interface CampusProgressQuery {
  pageNum: number
  pageSize: number
  schoolName?: string
  campusGradeId?: number
  courseId?: number
  progressStatus?: ProgressStatus | ''
  keyword?: string
}

export interface InitCampusProgressPayload {
  schoolName: string
  gradeName: string
  classId?: number
  majorDirection?: string
  studentCount?: number
  courses: Array<{
    courseId: number
    expectedStudentCount?: number
    progressStatus?: ProgressStatus
    remark?: string
  }>
}

export const getAllCampusProgress = (params: CampusProgressQuery) =>
  request.get<any, { code: number; data: CampusCourseProgressListResult }>('/admin/schedule/campus-progress', {
    params: normalizeQuery(params)
  })

export const getMyCampusProgress = (params: Omit<CampusProgressQuery, 'schoolName'>) =>
  request.get<any, { code: number; data: CampusCourseProgressListResult }>('/admin/schedule/campus-progress/my', {
    params: normalizeQuery(params)
  })

export const initCampusProgress = (data: InitCampusProgressPayload) =>
  request.post<any, { code: number; data: CampusCourseProgressRecord[] }>('/admin/schedule/campus-progress/init', data)

export const updateExpectedStudentCount = (id: number, expectedStudentCount: number) =>
  request.patch<any, { code: number; data: CampusCourseProgressRecord }>(
    `/admin/schedule/campus-progress/${id}/expected-student-count`,
    { expectedStudentCount }
  )

export const updateProgressRemark = (id: number, remark: string) =>
  request.patch<any, { code: number; data: CampusCourseProgressRecord }>(
    `/admin/schedule/campus-progress/${id}/remark`,
    { remark }
  )

function normalizeQuery(params: CampusProgressQuery | Omit<CampusProgressQuery, 'schoolName'>) {
  return {
    pageNum: params.pageNum,
    pageSize: params.pageSize,
    schoolName: 'schoolName' in params ? params.schoolName || undefined : undefined,
    campusGradeId: params.campusGradeId || undefined,
    courseId: params.courseId || undefined,
    progressStatus: params.progressStatus || undefined,
    keyword: params.keyword || undefined
  }
}
