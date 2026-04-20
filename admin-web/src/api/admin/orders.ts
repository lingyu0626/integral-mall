import { API_ENDPOINTS } from '../endpoints'
import { del, get, post, put } from '../http'
import { getToken } from '../auth-storage'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type {
  AdminOrderDelivery,
  AdminOrderFlow,
  AdminOrderListItem,
  AdminOrderStatus,
  AdminOrderItem,
  ProcurementExportRow,
} from '../../mock/orders-center'

export type OrderListQuery = Partial<PageQuery> & {
  keyword?: string
  status_code?: AdminOrderStatus | ''
  procurement_status?: ProcurementStatus | ''
}

export type RejectOrderPayload = {
  reject_reason: string
  refund_point: boolean
}

export type ShipOrderPayload = {
  express_company: string
  express_no: string
  shipper_code?: string
}

export type LogisticsDetectItem = {
  shipper_code: string
  express_company: string
  score?: number
}

export type ProcurementExportQuery = {
  status_code?: AdminOrderStatus | ''
  submit_date?: string
  procurement_status?: ProcurementStatus | ''
}

export type ProcurementStatus = 'PENDING_PROCURE' | 'PROCURED'

export async function fetchAdminOrders(query: OrderListQuery): Promise<ApiPageData<AdminOrderListItem>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_ORDERS
  return await get<ApiPageData<AdminOrderListItem>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function updateAdminOrderProcurement(orderId: number, procurementStatus: ProcurementStatus): Promise<void> {
  await put(`/api/v1/admin/orders/${orderId}/procurement`, { procurement_status: procurementStatus }, { scope: 'admin' })
}

export async function fetchAdminOrderDetail(orderId: number): Promise<AdminOrderListItem | null> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_ORDERS_BY_ORDER_ID
  return await get<AdminOrderListItem>(compilePath(endpoint.path, { orderId }), {
    scope: 'admin',
  })
}

export async function fetchAdminOrderItems(orderId: number): Promise<AdminOrderItem[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_ORDERS_BY_ORDER_ID_ITEMS
  const data = await get<{ list?: AdminOrderItem[] } | AdminOrderItem[]>(compilePath(endpoint.path, { orderId }), {
    scope: 'admin',
  })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function fetchAdminOrderFlows(orderId: number): Promise<AdminOrderFlow[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_ORDERS_BY_ORDER_ID_FLOWS
  const data = await get<{ list?: AdminOrderFlow[] } | AdminOrderFlow[]>(compilePath(endpoint.path, { orderId }), {
    scope: 'admin',
  })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function fetchAdminOrderDelivery(orderId: number): Promise<AdminOrderDelivery | null> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_ORDERS_BY_ORDER_ID_DELIVERY
  return await get<AdminOrderDelivery>(compilePath(endpoint.path, { orderId }), {
    scope: 'admin',
  })
}

export async function approveAdminOrder(orderId: number, auditRemark: string): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_ORDERS_BY_ORDER_ID_APPROVE
  await post(compilePath(endpoint.path, { orderId }), { audit_remark: auditRemark }, { scope: 'admin' })
}

export async function rejectAdminOrder(orderId: number, payload: RejectOrderPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_ORDERS_BY_ORDER_ID_REJECT
  await post(compilePath(endpoint.path, { orderId }), payload, { scope: 'admin' })
}

export async function shipAdminOrder(orderId: number, payload: ShipOrderPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_ORDERS_BY_ORDER_ID_SHIP
  await post(compilePath(endpoint.path, { orderId }), payload, { scope: 'admin' })
}

export async function detectAdminLogistics(expressNo: string): Promise<LogisticsDetectItem[]> {
  const data = await post<{ list?: LogisticsDetectItem[] } | LogisticsDetectItem[]>(
    '/api/v1/admin/logistics/detect',
    { express_no: expressNo },
    { scope: 'admin' },
  )
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function completeAdminOrder(orderId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_ORDERS_BY_ORDER_ID_COMPLETE
  await post(compilePath(endpoint.path, { orderId }), {}, { scope: 'admin' })
}

export async function closeAdminOrder(orderId: number, closeReason: string): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_ORDERS_BY_ORDER_ID_CLOSE
  await post(compilePath(endpoint.path, { orderId }), { close_reason: closeReason }, { scope: 'admin' })
}

export async function updateAdminOrderRemark(orderId: number, remark: string): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_ORDERS_BY_ORDER_ID_REMARK
  await put(compilePath(endpoint.path, { orderId }), { remark }, { scope: 'admin' })
}

export async function deleteAdminOrder(orderId: number): Promise<void> {
  await del(`/api/v1/admin/orders/${orderId}`, { scope: 'admin' })
}

export async function fetchProcurementExportRows(query: ProcurementExportQuery = {}): Promise<ProcurementExportRow[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_ORDERS_PROCUREMENT_EXPORT
  const data = await get<{ list?: ProcurementExportRow[] } | ProcurementExportRow[]>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function downloadProcurementExportCsv(query: ProcurementExportQuery = {}): Promise<void> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_ORDERS_PROCUREMENT_EXPORT_CSV
  const baseURL = import.meta.env.VITE_API_BASE_URL?.trim() || ''
  const queryString = new URLSearchParams()
  if (query.status_code) queryString.set('status_code', query.status_code)
  if (query.submit_date) queryString.set('submit_date', query.submit_date)
  if (query.procurement_status) queryString.set('procurement_status', query.procurement_status)
  const url = `${baseURL}${endpoint.path}${queryString.toString() ? `?${queryString.toString()}` : ''}`

  const token = getToken('admin')
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
    },
  })
  if (!response.ok) {
    throw new Error('导出失败，请稍后重试')
  }
  const blob = await response.blob()
  const fileName =
    response.headers.get('content-disposition')?.match(/filename=([^;]+)/)?.[1] ||
    `procurement-orders-${new Date().toISOString().slice(0, 10)}.csv`
  const objectUrl = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = objectUrl
  a.download = fileName.replace(/"/g, '')
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(objectUrl)
}
