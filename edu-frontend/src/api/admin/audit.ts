import request from '@/utils/request'

export interface AuditLogRecord {
  id: number
  operatorId: number | null
  operatorUsername: string | null
  operatorRealName: string | null
  operatorRole: string | null
  action: string
  targetType: string
  targetId: number | null
  detail: string | null
  clientIp: string | null
  createTime: string
}

export interface AuditLogPage {
  records: AuditLogRecord[]
  total: number
  current: number
  size: number
}

export interface AuditLogQuery {
  page: number
  size: number
  action?: string
  keyword?: string
}

export const getAuditLogs = (params: AuditLogQuery) =>
  request.get<any, { code: number; data: AuditLogPage }>('/admin/audit-logs', { params })
