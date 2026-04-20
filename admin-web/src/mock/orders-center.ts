import type { ApiPageData, PageQuery } from '../api/types'
import { normalizePageQuery } from '../api/pagination'

export type AdminOrderStatus = 'PENDING_AUDIT' | 'REJECTED' | 'PENDING_SHIP' | 'SHIPPED' | 'FINISHED' | 'CLOSED'

export type AdminOrderListItem = {
  id: number
  order_no: string
  user_id?: number
  user_name: string
  buyer_name?: string
  buyer_phone_tail?: string
  buyer_display?: string
  goods_summary?: string
  order_status_code: AdminOrderStatus
  procurement_status?: 'PENDING_PROCURE' | 'PROCURED'
  procurement_status_text?: string
  procured_at?: string
  total_point_amount: number
  submit_at: string
  remark: string
}

export type ProcurementExportRow = {
  order_no: string
  user_id: number
  order_status_code?: AdminOrderStatus
  order_status_text?: string
  procurement_status?: 'PENDING_PROCURE' | 'PROCURED'
  procurement_status_text?: string
  product_sku_qty?: string
  point_price?: number
  user_remark?: string
  sku_name: string
  quantity: number
  buyer_name?: string
  buyer_full_info?: string
  province_name?: string
  city_name?: string
  district_name?: string
  receiver_phone: string
  receiver_address: string
  submit_at?: string
}

export type AdminOrderItem = {
  id: number
  order_id: number
  spu_name: string
  sku_name: string
  quantity: number
  point_price: number
  total_point_amount: number
}

export type AdminOrderFlow = {
  id: number
  order_id: number
  from_status: AdminOrderStatus | 'INIT'
  to_status: AdminOrderStatus
  action_text: string
  note: string
  operator_name: string
  occurred_at: string
}

export type AdminOrderDelivery = {
  order_id: number
  receiver_name: string
  receiver_phone: string
  receiver_address: string
  express_company: string
  express_no: string
  shipper_code?: string
  delivery_status_code?: string
  delivery_status_text?: string
  ship_at: string
  signed_at?: string
  latest_trace_time?: string
  latest_trace_station?: string
}

type OrderListQuery = Partial<PageQuery> & {
  keyword?: string
  status_code?: AdminOrderStatus | ''
}

const mockOrders: AdminOrderListItem[] = [
  {
    id: 30001,
    order_no: 'EO100001',
    user_name: '碎片用户A',
    order_status_code: 'PENDING_AUDIT',
    total_point_amount: 1888,
    submit_at: '2026-03-17 09:18:00',
    remark: '',
  },
  {
    id: 30002,
    order_no: 'EO100002',
    user_name: '潮玩用户B',
    order_status_code: 'PENDING_SHIP',
    total_point_amount: 5988,
    submit_at: '2026-03-17 10:23:00',
    remark: '优先发货',
  },
  {
    id: 30003,
    order_no: 'EO100003',
    user_name: '碎片用户A',
    order_status_code: 'SHIPPED',
    total_point_amount: 888,
    submit_at: '2026-03-16 21:42:00',
    remark: '',
  },
  {
    id: 30004,
    order_no: 'EO100004',
    user_name: '茅台爱好者',
    order_status_code: 'FINISHED',
    total_point_amount: 3300,
    submit_at: '2026-03-14 12:11:00',
    remark: 'VIP用户',
  },
]

const mockOrderItems: AdminOrderItem[] = [
  { id: 1, order_id: 30001, spu_name: '飞天茅台53度 500ML', sku_name: '标准装', quantity: 1, point_price: 1888, total_point_amount: 1888 },
  { id: 2, order_id: 30002, spu_name: '苹果 Watch Ultra3', sku_name: '49mm 钛金属 黑色表带', quantity: 1, point_price: 5988, total_point_amount: 5988 },
  { id: 3, order_id: 30003, spu_name: '每天发红包牛票', sku_name: '权益券', quantity: 1, point_price: 888, total_point_amount: 888 },
  { id: 4, order_id: 30004, spu_name: '茅台酒厂纪念礼盒', sku_name: '礼盒版', quantity: 1, point_price: 3300, total_point_amount: 3300 },
]

const mockOrderFlows: AdminOrderFlow[] = [
  {
    id: 1001,
    order_id: 30001,
    from_status: 'INIT',
    to_status: 'PENDING_AUDIT',
    action_text: '用户提交订单',
    note: '',
    operator_name: '系统',
    occurred_at: '2026-03-17 09:18:00',
  },
  {
    id: 1002,
    order_id: 30002,
    from_status: 'INIT',
    to_status: 'PENDING_AUDIT',
    action_text: '用户提交订单',
    note: '',
    operator_name: '系统',
    occurred_at: '2026-03-17 10:23:00',
  },
  {
    id: 1003,
    order_id: 30002,
    from_status: 'PENDING_AUDIT',
    to_status: 'PENDING_SHIP',
    action_text: '审核通过',
    note: '库存充足',
    operator_name: '系统管理员',
    occurred_at: '2026-03-17 10:35:00',
  },
  {
    id: 1004,
    order_id: 30003,
    from_status: 'INIT',
    to_status: 'PENDING_AUDIT',
    action_text: '用户提交订单',
    note: '',
    operator_name: '系统',
    occurred_at: '2026-03-16 21:42:00',
  },
  {
    id: 1005,
    order_id: 30003,
    from_status: 'PENDING_AUDIT',
    to_status: 'PENDING_SHIP',
    action_text: '审核通过',
    note: '',
    operator_name: '系统管理员',
    occurred_at: '2026-03-16 22:10:00',
  },
  {
    id: 1006,
    order_id: 30003,
    from_status: 'PENDING_SHIP',
    to_status: 'SHIPPED',
    action_text: '发货',
    note: '顺丰 SF000003',
    operator_name: '仓库管理员',
    occurred_at: '2026-03-17 08:00:00',
  },
  {
    id: 1007,
    order_id: 30004,
    from_status: 'INIT',
    to_status: 'PENDING_AUDIT',
    action_text: '用户提交订单',
    note: '',
    operator_name: '系统',
    occurred_at: '2026-03-14 12:11:00',
  },
  {
    id: 1008,
    order_id: 30004,
    from_status: 'PENDING_AUDIT',
    to_status: 'PENDING_SHIP',
    action_text: '审核通过',
    note: '',
    operator_name: '系统管理员',
    occurred_at: '2026-03-14 12:30:00',
  },
  {
    id: 1009,
    order_id: 30004,
    from_status: 'PENDING_SHIP',
    to_status: 'SHIPPED',
    action_text: '发货',
    note: '京东 JD000004',
    operator_name: '仓库管理员',
    occurred_at: '2026-03-14 14:00:00',
  },
  {
    id: 1010,
    order_id: 30004,
    from_status: 'SHIPPED',
    to_status: 'FINISHED',
    action_text: '完成',
    note: '用户已签收',
    operator_name: '系统管理员',
    occurred_at: '2026-03-16 15:20:00',
  },
]

const mockOrderDeliveries: AdminOrderDelivery[] = [
  {
    order_id: 30003,
    receiver_name: '张三',
    receiver_phone: '13800001111',
    receiver_address: '上海市浦东新区世纪大道100号',
    express_company: '顺丰',
    express_no: 'SF000003',
    ship_at: '2026-03-17 08:00:00',
  },
  {
    order_id: 30004,
    receiver_name: '李四',
    receiver_phone: '13900002222',
    receiver_address: '北京市朝阳区建国路88号',
    express_company: '京东',
    express_no: 'JD000004',
    ship_at: '2026-03-14 14:00:00',
  },
]

let flowIdSeed = 2000

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

function getOrderById(orderId: number): AdminOrderListItem | undefined {
  return mockOrders.find((item) => item.id === orderId)
}

function appendOrderFlow(orderId: number, toStatus: AdminOrderStatus, actionText: string, note: string, operatorName: string) {
  const order = getOrderById(orderId)
  if (!order) return
  const flow: AdminOrderFlow = {
    id: ++flowIdSeed,
    order_id: orderId,
    from_status: order.order_status_code,
    to_status: toStatus,
    action_text: actionText,
    note,
    operator_name: operatorName,
    occurred_at: nowText(),
  }
  mockOrderFlows.unshift(flow)
  order.order_status_code = toStatus
}

export function listMockOrders(query: OrderListQuery): ApiPageData<AdminOrderListItem> {
  const key = query.keyword?.trim().toLowerCase() ?? ''
  const status = query.status_code ?? ''
  const source = mockOrders.filter((item) => {
    const passKeyword = key
      ? [item.order_no, item.user_name, String(item.id)].some((field) => field.toLowerCase().includes(key))
      : true
    const passStatus = status ? item.order_status_code === status : true
    return passKeyword && passStatus
  })
  source.sort((a, b) => b.submit_at.localeCompare(a.submit_at))
  return paginate(source, query)
}

export function getMockOrderDetail(orderId: number): AdminOrderListItem | null {
  const order = getOrderById(orderId)
  return order ? { ...order } : null
}

export function listMockOrderItems(orderId: number): AdminOrderItem[] {
  return mockOrderItems.filter((item) => item.order_id === orderId)
}

export function listMockOrderFlows(orderId: number): AdminOrderFlow[] {
  return mockOrderFlows
    .filter((item) => item.order_id === orderId)
    .sort((a, b) => b.occurred_at.localeCompare(a.occurred_at))
}

export function getMockOrderDelivery(orderId: number): AdminOrderDelivery | null {
  const delivery = mockOrderDeliveries.find((item) => item.order_id === orderId)
  return delivery ? { ...delivery } : null
}

export function approveMockOrder(orderId: number, auditRemark = ''): void {
  const order = getOrderById(orderId)
  if (!order || order.order_status_code !== 'PENDING_AUDIT') return
  appendOrderFlow(orderId, 'PENDING_SHIP', '审核通过', auditRemark, '系统管理员')
}

export function rejectMockOrder(orderId: number, rejectReason = '', refundPoint = true): void {
  const order = getOrderById(orderId)
  if (!order || order.order_status_code !== 'PENDING_SHIP') return
  const note = refundPoint ? `驳回原因：${rejectReason}（已回补碎片）` : `驳回原因：${rejectReason}`
  appendOrderFlow(orderId, 'REJECTED', '发货驳回', note, '系统管理员')
}

export function shipMockOrder(orderId: number, expressCompany: string, expressNo: string): void {
  const order = getOrderById(orderId)
  if (!order || order.order_status_code !== 'PENDING_SHIP') return
  appendOrderFlow(orderId, 'SHIPPED', '发货', `${expressCompany} ${expressNo}`, '仓库管理员')
  const current = mockOrderDeliveries.find((item) => item.order_id === orderId)
  if (current) {
    current.express_company = expressCompany
    current.express_no = expressNo
    current.ship_at = nowText()
  } else {
    mockOrderDeliveries.unshift({
      order_id: orderId,
      receiver_name: order.user_name,
      receiver_phone: '138****0000',
      receiver_address: '上海市静安区测试路88号',
      express_company: expressCompany,
      express_no: expressNo,
      ship_at: nowText(),
    })
  }
}

export function completeMockOrder(orderId: number): void {
  const order = getOrderById(orderId)
  if (!order || order.order_status_code !== 'SHIPPED') return
  appendOrderFlow(orderId, 'FINISHED', '完成', '订单已完成', '系统管理员')
}

export function closeMockOrder(orderId: number, closeReason = ''): void {
  const order = getOrderById(orderId)
  if (!order) return
  if (!['PENDING_AUDIT', 'PENDING_SHIP', 'SHIPPED'].includes(order.order_status_code)) return
  appendOrderFlow(orderId, 'CLOSED', '关闭', closeReason || '管理员关闭订单', '系统管理员')
}

export function updateMockOrderRemark(orderId: number, remark: string): void {
  const order = getOrderById(orderId)
  if (!order) return
  order.remark = remark
}
