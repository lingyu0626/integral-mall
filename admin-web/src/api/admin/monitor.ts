import { API_ENDPOINTS } from '../endpoints'
import { get, post } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type {
  AdminOperationLog,
  AdminOperationLogStatus,
  AdminOutboxEvent,
  ConversionStats,
  OrderStats,
  OutboxEventStatus,
  ProductStats,
  UserStats,
} from '../../mock/monitor-center'

export type OperationLogListQuery = Partial<PageQuery> & {
  keyword?: string
  status_code?: AdminOperationLogStatus | ''
}

export type OutboxEventListQuery = Partial<PageQuery> & {
  keyword?: string
  status_code?: OutboxEventStatus | ''
}

export async function fetchUserStats(): Promise<UserStats> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_STATS_USERS
  return await get<UserStats>(endpoint.path, { scope: 'admin' })
}

export async function fetchOrderStats(): Promise<OrderStats> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_STATS_ORDERS
  return await get<OrderStats>(endpoint.path, { scope: 'admin' })
}

export async function fetchProductStats(): Promise<ProductStats> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_STATS_PRODUCTS
  return await get<ProductStats>(endpoint.path, { scope: 'admin' })
}

export async function fetchConversionStats(): Promise<ConversionStats> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_STATS_CONVERSIONS
  return await get<ConversionStats>(endpoint.path, { scope: 'admin' })
}

export async function fetchOperationLogs(query: OperationLogListQuery): Promise<ApiPageData<AdminOperationLog>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_OPERATION_LOGS
  return await get<ApiPageData<AdminOperationLog>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function fetchOperationLogDetail(logId: number): Promise<AdminOperationLog> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_OPERATION_LOGS_BY_LOG_ID
  return await get<AdminOperationLog>(compilePath(endpoint.path, { logId }), {
    scope: 'admin',
  })
}

export async function fetchOutboxEvents(query: OutboxEventListQuery): Promise<ApiPageData<AdminOutboxEvent>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_OUTBOX_EVENTS
  return await get<ApiPageData<AdminOutboxEvent>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function retryOutboxEvent(eventId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_OUTBOX_EVENTS_BY_EVENT_ID_RETRY
  await post(compilePath(endpoint.path, { eventId }), {}, { scope: 'admin' })
}
