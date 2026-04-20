import type { ApiPageData, PageQuery } from '../api/types'
import { normalizePageQuery } from '../api/pagination'

export type UserStats = {
  new_user_today: number
  active_user_7d: number
  frozen_user_count: number
  conversion_rate: number
}

export type OrderStats = {
  pending_audit_count: number
  pending_ship_count: number
  shipped_count: number
  finished_count: number
  close_count: number
}

export type ProductStats = {
  on_shelf_count: number
  off_shelf_count: number
  recommend_count: number
  low_stock_count: number
}

export type ConversionStats = {
  visit_uv_today: number
  exchange_uv_today: number
  conversion_rate: number
  avg_exchange_point: number
}

export type AdminOperationLogStatus = 'SUCCESS' | 'FAILED'

export type AdminOperationLog = {
  id: number
  operator_name: string
  module_name: string
  action_name: string
  request_method: 'GET' | 'POST' | 'PUT' | 'DELETE'
  request_path: string
  client_ip: string
  status_code: AdminOperationLogStatus
  request_id: string
  occurred_at: string
  detail_text: string
}

export type OutboxEventStatus = 'PENDING' | 'SUCCESS' | 'FAILED'

export type AdminOutboxEvent = {
  id: number
  event_type_code: string
  aggregate_type_code: string
  aggregate_id: number
  status_code: OutboxEventStatus
  retry_count: number
  next_retry_at: string
  last_error_msg: string
  created_at: string
  updated_at: string
}

const mockUserStats: UserStats = {
  new_user_today: 92,
  active_user_7d: 1836,
  frozen_user_count: 13,
  conversion_rate: 0.124,
}

const mockOrderStats: OrderStats = {
  pending_audit_count: 6,
  pending_ship_count: 11,
  shipped_count: 28,
  finished_count: 245,
  close_count: 7,
}

const mockProductStats: ProductStats = {
  on_shelf_count: 108,
  off_shelf_count: 21,
  recommend_count: 29,
  low_stock_count: 14,
}

const mockConversionStats: ConversionStats = {
  visit_uv_today: 5620,
  exchange_uv_today: 693,
  conversion_rate: 0.1233,
  avg_exchange_point: 2248,
}

const mockOperationLogs: AdminOperationLog[] = [
  {
    id: 50001,
    operator_name: '系统管理员',
    module_name: '订单中心',
    action_name: '审核通过',
    request_method: 'POST',
    request_path: '/api/v1/admin/orders/30001/approve',
    client_ip: '10.0.0.23',
    status_code: 'SUCCESS',
    request_id: 'req_20260317_001',
    occurred_at: '2026-03-17 15:30:00',
    detail_text: '订单 30001 审核通过，进入待发货状态',
  },
  {
    id: 50002,
    operator_name: '运营主管',
    module_name: '商品中心',
    action_name: '更新SKU库存',
    request_method: 'PUT',
    request_path: '/api/v1/admin/products/skus/5001/stock',
    client_ip: '10.0.0.17',
    status_code: 'SUCCESS',
    request_id: 'req_20260317_002',
    occurred_at: '2026-03-17 15:26:00',
    detail_text: 'SKU 5001 库存 +100',
  },
  {
    id: 50003,
    operator_name: '审核专员',
    module_name: '权限管理',
    action_name: '角色授权',
    request_method: 'PUT',
    request_path: '/api/v1/admin/roles/11/permissions',
    client_ip: '10.0.0.29',
    status_code: 'FAILED',
    request_id: 'req_20260317_003',
    occurred_at: '2026-03-17 15:22:00',
    detail_text: '权限不足，授权失败',
  },
  {
    id: 50004,
    operator_name: '系统管理员',
    module_name: '群资源管理',
    action_name: '新增群资源',
    request_method: 'POST',
    request_path: '/api/v1/admin/group-resources',
    client_ip: '10.0.0.23',
    status_code: 'SUCCESS',
    request_id: 'req_20260317_004',
    occurred_at: '2026-03-17 15:18:00',
    detail_text: '新增群资源：茅台玩家交流群',
  },
  {
    id: 50005,
    operator_name: '系统管理员',
    module_name: '系统配置',
    action_name: '更新配置',
    request_method: 'PUT',
    request_path: '/api/v1/admin/system-configs/3002',
    client_ip: '10.0.0.23',
    status_code: 'SUCCESS',
    request_id: 'req_20260317_005',
    occurred_at: '2026-03-17 15:10:00',
    detail_text: '更新默认兑换限购为 1',
  },
]

const mockOutboxEvents: AdminOutboxEvent[] = [
  {
    id: 70001,
    event_type_code: 'ORDER_APPROVED',
    aggregate_type_code: 'ORDER',
    aggregate_id: 30001,
    status_code: 'SUCCESS',
    retry_count: 0,
    next_retry_at: '',
    last_error_msg: '',
    created_at: '2026-03-17 15:30:00',
    updated_at: '2026-03-17 15:30:00',
  },
  {
    id: 70002,
    event_type_code: 'ORDER_SHIPPED',
    aggregate_type_code: 'ORDER',
    aggregate_id: 30003,
    status_code: 'FAILED',
    retry_count: 2,
    next_retry_at: '2026-03-17 15:45:00',
    last_error_msg: 'Kafka timeout',
    created_at: '2026-03-17 15:12:00',
    updated_at: '2026-03-17 15:32:00',
  },
  {
    id: 70003,
    event_type_code: 'POINT_LEDGER_SYNC',
    aggregate_type_code: 'POINT_LEDGER',
    aggregate_id: 50002,
    status_code: 'PENDING',
    retry_count: 0,
    next_retry_at: '2026-03-17 15:40:00',
    last_error_msg: '',
    created_at: '2026-03-17 15:34:00',
    updated_at: '2026-03-17 15:34:00',
  },
  {
    id: 70004,
    event_type_code: 'ASSET_GRANTED',
    aggregate_type_code: 'BACKPACK_ASSET',
    aggregate_id: 7001,
    status_code: 'SUCCESS',
    retry_count: 0,
    next_retry_at: '',
    last_error_msg: '',
    created_at: '2026-03-17 15:21:00',
    updated_at: '2026-03-17 15:21:00',
  },
]

function nowText() {
  return new Date().toISOString().slice(0, 19).replace('T', ' ')
}

function paginate<T>(source: T[], query?: Partial<PageQuery>): ApiPageData<T> {
  const { pageNo, pageSize } = normalizePageQuery(query)
  const from = (pageNo - 1) * pageSize
  return {
    pageNo,
    pageSize,
    total: source.length,
    list: source.slice(from, from + pageSize),
  }
}

export function getMockUserStats(): UserStats {
  return { ...mockUserStats }
}

export function getMockOrderStats(): OrderStats {
  return { ...mockOrderStats }
}

export function getMockProductStats(): ProductStats {
  return { ...mockProductStats }
}

export function getMockConversionStats(): ConversionStats {
  return { ...mockConversionStats }
}

export function listMockOperationLogs(query?: Partial<PageQuery>, keyword = '', statusCode: AdminOperationLogStatus | '' = ''): ApiPageData<AdminOperationLog> {
  const key = keyword.trim().toLowerCase()
  const source = mockOperationLogs.filter((item) => {
    const passKeyword = key
      ? [item.operator_name, item.module_name, item.action_name, item.request_path, item.request_id].some((field) => field.toLowerCase().includes(key))
      : true
    const passStatus = statusCode ? item.status_code === statusCode : true
    return passKeyword && passStatus
  })
  source.sort((a, b) => b.occurred_at.localeCompare(a.occurred_at))
  return paginate(source, query)
}

export function getMockOperationLogDetail(logId: number): AdminOperationLog | null {
  const log = mockOperationLogs.find((item) => item.id === logId)
  return log ? { ...log } : null
}

export function listMockOutboxEvents(query?: Partial<PageQuery>, keyword = '', statusCode: OutboxEventStatus | '' = ''): ApiPageData<AdminOutboxEvent> {
  const key = keyword.trim().toLowerCase()
  const source = mockOutboxEvents.filter((item) => {
    const passKeyword = key
      ? [item.event_type_code, item.aggregate_type_code, String(item.aggregate_id), item.last_error_msg].some((field) => field.toLowerCase().includes(key))
      : true
    const passStatus = statusCode ? item.status_code === statusCode : true
    return passKeyword && passStatus
  })
  source.sort((a, b) => b.updated_at.localeCompare(a.updated_at))
  return paginate(source, query)
}

export function retryMockOutboxEvent(eventId: number): void {
  const target = mockOutboxEvents.find((item) => item.id === eventId)
  if (!target) return
  target.retry_count += 1
  target.status_code = 'SUCCESS'
  target.last_error_msg = ''
  target.next_retry_at = ''
  target.updated_at = nowText()
}
