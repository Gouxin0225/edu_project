import request from '@/utils/request'
import type { EduRequestConfig } from '@/utils/request'
import type { QuestionRecord } from './question'

export interface ExamFormData {
  title: string
  startTime: string
  endTime: string
  targetClassIds: number[]
  type?: string
  duration?: number
  passScore?: number
  totalScore?: number
  requireSurveyBeforeSubmit?: boolean
  requiredSurveyId?: number | null
}

export interface ExamRecord {
  id: number
  title: string
  type: string
  startTime: string
  endTime: string
  totalScore: number
  duration?: number
  passScore?: number
  requireSurveyBeforeSubmit?: boolean
  requiredSurveyId?: number | null
  status?: string
  editable?: boolean
  targetClassIds: number[]
  creatorId: number
  createTime?: string
}

export interface ExamQuestionItem extends QuestionRecord {
  questionId?: number
  score?: number
}

export interface ClassInfo {
  id: number
  className: string
  status: number
  createTime: string
}

export const getMyClasses = () =>
  request.get<any, { code: number; data: ClassInfo[] }>('/teacher/my-classes')

export const getQuestionList = (params: {
  page: number; size: number
  courseCategory?: string; type?: string; difficulty?: string; creatorId?: string
}) => request.get<any, { code: number; data: { records: QuestionRecord[]; total: number; size: number; current: number } }>('/api/question', { params })

export const createExam = (data: ExamFormData) =>
  request.post<any, { code: number; data: ExamRecord }>('/api/exam', data)

export const updateExam = (examId: number, data: ExamFormData) =>
  request.put<any, { code: number; data: ExamRecord }>(`/api/exam/${examId}`, data)

export const addExamQuestions = (examId: number, questionIds: number[]) =>
  request.post<any, { code: number }>(`/api/exam/${examId}/questions`, { questionIds })

export const removeExamQuestion = (examId: number, questionId: number) =>
  request.delete<any, { code: number }>(`/api/exam/${examId}/questions/${questionId}`)

export const autoScore = (examId: number) =>
  request.post<any, { code: number; message: string }>(`/api/exam/${examId}/auto-score`)

export const updateQuestionScore = (examId: number, questionId: number, score: number) =>
  request.put<any, { code: number; message: string }>(`/api/exam/${examId}/question/${questionId}/score`, { score })

export const publishExam = (examId: number) =>
  request.post<any, { code: number; message: string }>(`/api/exam/${examId}/publish`)

export const deleteExam = (examId: number) =>
  request.delete<any, { code: number; message: string }>(`/api/exam/${examId}`)

export const getExamDetail = (examId: number) =>
  request.get<any, { code: number; data: ExamRecord }>(`/api/exam/${examId}`)

export const getExamQuestions = (examId: number) =>
  request.get<any, { code: number; data: ExamQuestionItem[] }>(`/api/exam/${examId}/questions`)

export const getExamList = (params: { page: number; size: number }) =>
  request.get<any, { code: number; data: { records: ExamRecord[]; total: number; size: number; current: number } }>('/api/exam', { params })

export interface ExamStartData {
  submissionId: number
  questions: ExamQuestionItem[]
  totalScore: number
  duration: number
  remainingSeconds: number
  switchScreenLimit: number
  deadline?: string
}

export interface StudentExamListItem {
  examId: number
  examTitle: string
  startTime: string
  endTime: string
  totalScore: number
  status: string
  submissionId?: number
  scoreGained?: number
}

export const getStudentExamList = () =>
  request.get<any, { code: number; data: StudentExamListItem[] }>('/api/student/exam-list')

export interface ExamRecordItem {
  examId: number
  title: string
  deadline: string
  submitTime: string | null
  status: string
  scoreGained: number | null
}

export const getStudentExamRecords = () =>
  request.get<any, { code: number; data: ExamRecordItem[] }>('/api/student/exam-records')

export interface QuestionResult {
  questionId: number
  type: string
  difficulty: string
  content: string
  optionsJson: string | null
  score: number
  standardAnswer: string
  studentAnswer: string
  isCorrect: boolean | null
  scoreGained: number | null
  analysis: string | null
  aiSuggestScore?: number | null
  aiSuggestDetail?: string | null
}

export interface ExamResultData {
  examId: number
  examTitle: string
  totalScore: number
  scoreGained: number | null
  passed: boolean
  rank: number | null
  totalStudents: number | null
  submitTime: string | null
  showAnalysis: boolean
  answers: QuestionResult[]
}

export const getMyExamResult = (examId: number) =>
  request.get<any, { code: number; data: ExamResultData }>(`/api/exam/${examId}/my-result`)

export const startExam = (examId: number) =>
  request.post<any, { code: number; data: ExamStartData }>(`/api/exam/${examId}/start`)

export interface ExamProgressData {
  submissionId: number
  questions: ExamQuestionItem[]
  switchScreenCount: number
  duration: number
  remainingSeconds: number
  switchScreenLimit: number
  deadline?: string
  status: string
}

export const resumeExam = (examId: number) =>
  request.get<any, { code: number; data: ExamProgressData }>(
    `/api/exam/${examId}/resume`,
    { silentError: true } as EduRequestConfig
  )

export interface SaveAnswerItem {
  questionId: number
  answerValue: string
}

export const saveExamAnswers = (examId: number, answers: SaveAnswerItem[]) =>
  request.post<any, { code: number }>(`/api/exam/${examId}/save`, { answers })

export const submitExam = (examId: number) =>
  request.post<any, { code: number; message: string }>(`/api/exam/${examId}/submit`)

export interface ExamSubmitRequirement {
  requireSurvey: boolean
  surveySubmitted: boolean
  surveyExpired?: boolean
  surveyId: number | null
  surveyTitle: string | null
  message?: string | null
}

export const getExamSubmitRequirement = (examId: number) =>
  request.get<any, { code: number; data: ExamSubmitRequirement }>(`/api/exam/${examId}/submit-requirement`)

export const reportScreenSwitch = (examId: number) =>
  request.post<any, { code: number; data: number }>(`/api/exam/${examId}/screen-switch`)

export interface SubmissionItem {
  submissionId: number
  studentId: number
  studentName: string
  status: string
  submitTime: string
  scoreGained: number | null
  switchScreenCount: number
}

export const getExamSubmissions = (examId: number) =>
  request.get<any, { code: number; data: SubmissionItem[] }>(`/api/exam/${examId}/submissions`)

export interface AnswerItem {
  questionId: number
  type: string
  content: string
  optionsJson: string | null
  studentAnswer: string
  standardAnswer: string
  isCorrect: boolean | null
  scoreGained: number | null
  questionScore: number
  aiSuggestScore?: number | null
  aiSuggestDetail?: string | null
}

export interface SubmissionDetail {
  submissionId: number
  studentId: number
  studentName: string
  status: string
  totalScoreGained: number | null
  submitTime: string
  gradeTime: string | null
  answers: AnswerItem[]
}

export const getSubmissionDetail = (examId: number, submissionId: number) =>
  request.get<any, { code: number; data: SubmissionDetail }>(`/api/exam/${examId}/submission/${submissionId}`)

export interface GradeItem {
  questionId: number
  scoreGained: number
  aiSuggestScore?: number | null
  aiSuggestDetail?: string | null
}

export const submitGrade = (examId: number, submissionId: number, grades: GradeItem[]) =>
  request.put<any, { code: number; message: string }>(`/api/exam/${examId}/submission/${submissionId}/grade`, grades)

export const publishScore = (examId: number) =>
  request.post<any, { code: number; message: string }>(`/api/exam/${examId}/publish-score`)

export interface AiGradeSuggest {
  suggestedScore: number
  maxScore: number
  reasoning: string
  suggestion?: string
  keyPoints: string[]
  matchedPoints: string[]
  missingPoints: string[]
  confidence: 'HIGH' | 'MEDIUM' | 'LOW' | string
}

export const getAiGradeSuggest = (data: {
  questionId?: number
  questionType?: string
  questionContent: string
  standardAnswer: string
  studentAnswer: string
  maxScore: number
  scoringRubric?: string
}) => request.post<any, { code: number; data: AiGradeSuggest }>('/api/question/ai-grade-suggest', data, { timeout: 90000 })

export interface ScoreDistributionItem {
  range: string
  count: number
  rate: number
}

export interface ExamStatistics {
  examId: number
  title: string
  totalStudents: number
  submittedCount: number
  gradedCount: number
  averageScore: number
  highestScore: number
  lowestScore: number
  passRate: number
  scoreDistribution: ScoreDistributionItem[]
}

export const getExamStatistics = (examId: number) =>
  request.get<any, { code: number; data: ExamStatistics }>(`/api/exam/${examId}/statistics`)

export interface ExamParticipation {
  examId: number
  examTitle: string
  submittedStudents: ParticipationStudent[]
  inProgressStudents: ParticipationStudent[]
  notSubmittedStudents: ParticipationStudent[]
}

export interface ParticipationStudent {
  studentId: number
  studentName: string
  startTime: string | null
  submitTime: string | null
  status: string
}

export const getExamParticipation = (examId: number) =>
  request.get<any, { code: number; data: ExamParticipation }>(`/api/exam/${examId}/participation`)
