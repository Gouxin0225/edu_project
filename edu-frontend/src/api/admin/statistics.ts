import request from '@/utils/request'

export interface AdminStatsOverview {
  classCount: number
  activeClassCount: number
  finishedClassCount: number
  studentCount: number
  homeworkCount: number
  examCount: number
  surveyCount: number
  homeworkAverageScore: number
  examAverageScore: number
  surveyAverageScore: number
  homeworkScoreSampleCount: number
  examScoreSampleCount: number
  surveyScoreSampleCount: number
}

export interface AdminStatsPerson {
  id: number
  name: string
}

export interface AdminWorkSummary {
  taskCount: number
  submittedCount: number
  totalRequired: number
  pendingCount: number
  gradedCount: number
  pendingGradeCount: number
  scoreSampleCount: number
  completionRate: number
  averageScore: number
}

export interface AdminWorkItem {
  taskId: number
  title: string
  deadline: string | null
  totalStudents: number
  submittedCount: number
  pendingCount: number
  gradedCount: number
  returnedCount: number
  pendingGradeCount: number
  scoreSampleCount: number
  completionRate: number
  averageScore: number
}

export interface AdminSurveySummary {
  surveyCount: number
  responseCount: number
  totalRequired: number
  scoreSampleCount: number
  completionRate: number
  averageScore: number
}

export interface AdminSurveyItem {
  surveyId: number
  title: string
  deadline: string | null
  status: 0 | 1
  statusName: string
  targetTeacherId?: number | null
  targetTeacherName?: string | null
  totalStudents: number
  responseCount: number
  pendingCount: number
  scoreSampleCount: number
  completionRate: number
  averageScore: number
}

export interface AdminClassStatsModule {
  classId: number
  className: string
  status: 0 | 1
  statusName: string
  studentCount: number
  teachers: AdminStatsPerson[]
  assistants: AdminStatsPerson[]
  homeworkSummary: AdminWorkSummary
  homeworks: AdminWorkItem[]
  examSummary: AdminWorkSummary
  exams: AdminWorkItem[]
  surveySummary: AdminSurveySummary
  surveys: AdminSurveyItem[]
}

export interface AdminClassStatistics {
  overview: AdminStatsOverview
  classes: AdminClassStatsModule[]
}

export const getAdminClassStatistics = () =>
  request.get<any, { code: number; data: AdminClassStatistics }>('/api/admin/statistics/classes')
