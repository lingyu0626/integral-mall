import type { ApiPageData, PageQuery } from '../api/types'
import { normalizePageQuery } from '../api/pagination'

export type AdminMe = {
  id: number
  username: string
  display_name: string
  roles: string[]
}

export type DashboardOverview = {
  user_total: number
  order_total: number
  pending_audit_count: number
  today_exchange_point: number
  pending_writeoff_point?: number
  product_total: number
}

export type UserStatus = 'ACTIVE' | 'FROZEN'

export type AdminUser = {
  id: number
  backpack_id?: string
  nick_name: string
  phone?: string
  phone_masked: string
  user_status_code: UserStatus
  point_balance: number
  total_consume_amount?: number
  profit_amount?: number
  order_count: number
  admin_remark?: string
  created_at: string
}

export type AdminProduct = {
  id: number
  product_name: string
  category_name: string
  point_price: number
  stock_available: number
  status_code: 'ON_SHELF' | 'OFF_SHELF'
}

export type AdminOrder = {
  id: number
  order_no: string
  user_name: string
  order_status_code: 'PENDING_AUDIT' | 'PENDING_SHIP' | 'SHIPPED' | 'FINISHED'
  total_point_amount: number
  submit_at: string
}

export type AdminPointLedger = {
  id: number
  user_name: string
  biz_type_code: string
  change_amount: number
  balance_after: number
  note?: string
  occurred_at: string
}

export type CategoryStatus = 'ENABLED' | 'DISABLED'

export type AdminCategory = {
  id: number
  category_name: string
  sort_no: number
  status_code: CategoryStatus
  product_count: number
  updated_at: string
}

export type RecommendSlotStatus = 'ENABLED' | 'DISABLED'

export type AdminRecommendSlot = {
  id: number
  slot_name: string
  slot_code: string
  status_code: RecommendSlotStatus
  item_count: number
  updated_at: string
}

export type AdminRecommendItem = {
  id: number
  slot_id: number
  spu_id: number
  product_name: string
  point_price: number
  sort_no: number
  status_code: 'ENABLED' | 'DISABLED'
  start_at: string
  end_at: string
  banner_image_url?: string
  image_url?: string
}

const mockAdminMe: AdminMe = {
  id: 1,
  username: 'admin',
  display_name: '系统管理员',
  roles: ['超级管理员'],
}

const mockUsers: AdminUser[] = [
  {
    id: 1001,
    nick_name: '碎片用户A',
    phone_masked: '138****5678',
    user_status_code: 'ACTIVE',
    point_balance: 16888,
    order_count: 6,
    created_at: '2026-03-01 11:20:00',
  },
  {
    id: 1002,
    nick_name: '潮玩用户B',
    phone_masked: '139****8899',
    user_status_code: 'ACTIVE',
    point_balance: 3200,
    order_count: 2,
    created_at: '2026-03-06 09:40:00',
  },
  {
    id: 1003,
    nick_name: '薅羊毛用户C',
    phone_masked: '137****2233',
    user_status_code: 'FROZEN',
    point_balance: 120,
    order_count: 1,
    created_at: '2026-03-08 16:30:00',
  },
  {
    id: 1004,
    nick_name: '茅台爱好者',
    phone_masked: '186****7755',
    user_status_code: 'ACTIVE',
    point_balance: 4280,
    order_count: 4,
    created_at: '2026-03-10 10:08:00',
  },
]

const mockProducts: AdminProduct[] = [
  { id: 101, product_name: '飞天茅台53度 500ML×1', category_name: '白酒', point_price: 1888, stock_available: 35, status_code: 'ON_SHELF' },
  { id: 102, product_name: '苹果 Watch Ultra3', category_name: '数码', point_price: 5988, stock_available: 18, status_code: 'ON_SHELF' },
  { id: 103, product_name: '至臻玩家微信群入群资格', category_name: '生活权益', point_price: 8888, stock_available: 9999, status_code: 'ON_SHELF' },
  { id: 104, product_name: '每天发红包牛票', category_name: '生活权益', point_price: 888, stock_available: 9999, status_code: 'OFF_SHELF' },
]

const mockOrders: AdminOrder[] = [
  { id: 30001, order_no: 'EO100001', user_name: '碎片用户A', order_status_code: 'PENDING_AUDIT', total_point_amount: 1888, submit_at: '2026-03-17 09:18:00' },
  { id: 30002, order_no: 'EO100002', user_name: '潮玩用户B', order_status_code: 'PENDING_SHIP', total_point_amount: 5988, submit_at: '2026-03-17 10:23:00' },
  { id: 30003, order_no: 'EO100003', user_name: '碎片用户A', order_status_code: 'SHIPPED', total_point_amount: 888, submit_at: '2026-03-16 21:42:00' },
  { id: 30004, order_no: 'EO100004', user_name: '茅台爱好者', order_status_code: 'FINISHED', total_point_amount: 3300, submit_at: '2026-03-14 12:11:00' },
]

const mockPointLedger: AdminPointLedger[] = [
  { id: 50001, user_name: '碎片用户A', biz_type_code: 'EXCHANGE_ORDER', change_amount: -1888, balance_after: 16888, occurred_at: '2026-03-17 09:18:00' },
  { id: 50002, user_name: '潮玩用户B', biz_type_code: 'MANUAL_ADJUST', change_amount: 500, balance_after: 3200, occurred_at: '2026-03-17 10:05:00' },
  { id: 50003, user_name: '茅台爱好者', biz_type_code: 'ORDER_CANCEL_REFUND', change_amount: 3300, balance_after: 4280, occurred_at: '2026-03-16 13:25:00' },
]

const mockCategories: AdminCategory[] = [
  { id: 11, category_name: '白酒', sort_no: 100, status_code: 'ENABLED', product_count: 18, updated_at: '2026-03-17 10:10:00' },
  { id: 12, category_name: '数码', sort_no: 90, status_code: 'ENABLED', product_count: 24, updated_at: '2026-03-17 10:09:00' },
  { id: 13, category_name: '生活权益', sort_no: 80, status_code: 'ENABLED', product_count: 36, updated_at: '2026-03-17 10:08:00' },
  { id: 14, category_name: '潮流周边', sort_no: 70, status_code: 'DISABLED', product_count: 6, updated_at: '2026-03-16 20:40:00' },
]

const mockRecommendSlots: AdminRecommendSlot[] = [
  { id: 201, slot_name: '首页推荐', slot_code: 'HOME_RECOMMEND', status_code: 'ENABLED', item_count: 3, updated_at: '2026-03-17 11:20:00' },
  { id: 202, slot_name: '推荐上新', slot_code: 'HOME_NEW_ARRIVAL', status_code: 'ENABLED', item_count: 2, updated_at: '2026-03-17 11:15:00' },
  { id: 203, slot_name: '猜你喜欢', slot_code: 'HOME_GUESS', status_code: 'DISABLED', item_count: 1, updated_at: '2026-03-15 09:15:00' },
]

const mockRecommendItems: AdminRecommendItem[] = [
  {
    id: 9001,
    slot_id: 201,
    spu_id: 101,
    product_name: '飞天茅台53度 500ML×1',
    point_price: 1888,
    sort_no: 100,
    status_code: 'ENABLED',
    start_at: '2026-03-01 00:00:00',
    end_at: '2026-12-31 23:59:59',
  },
  {
    id: 9002,
    slot_id: 201,
    spu_id: 102,
    product_name: '苹果 Watch Ultra3',
    point_price: 5988,
    sort_no: 90,
    status_code: 'ENABLED',
    start_at: '2026-03-01 00:00:00',
    end_at: '2026-12-31 23:59:59',
  },
  {
    id: 9003,
    slot_id: 201,
    spu_id: 103,
    product_name: '至臻玩家微信群入群资格',
    point_price: 8888,
    sort_no: 80,
    status_code: 'DISABLED',
    start_at: '2026-03-01 00:00:00',
    end_at: '2026-12-31 23:59:59',
  },
  {
    id: 9004,
    slot_id: 202,
    spu_id: 104,
    product_name: '每天发红包牛票',
    point_price: 888,
    sort_no: 100,
    status_code: 'ENABLED',
    start_at: '2026-03-10 00:00:00',
    end_at: '2026-12-31 23:59:59',
  },
  {
    id: 9005,
    slot_id: 202,
    spu_id: 105,
    product_name: '茅台酒厂纪念礼盒',
    point_price: 2388,
    sort_no: 90,
    status_code: 'ENABLED',
    start_at: '2026-03-10 00:00:00',
    end_at: '2026-12-31 23:59:59',
  },
  {
    id: 9006,
    slot_id: 203,
    spu_id: 106,
    product_name: '新品权益试用券',
    point_price: 288,
    sort_no: 100,
    status_code: 'ENABLED',
    start_at: '2026-03-10 00:00:00',
    end_at: '2026-12-31 23:59:59',
  },
]

let categoryIdSeed = 100
let slotIdSeed = 300
let recommendItemIdSeed = 10000

function paginate<T>(source: T[], query?: Partial<PageQuery>): ApiPageData<T> {
  const { pageNo, pageSize } = normalizePageQuery(query)
  const from = (pageNo - 1) * pageSize
  const list = source.slice(from, from + pageSize)
  return {
    pageNo,
    pageSize,
    total: source.length,
    list,
  }
}

export function getMockAdminMe(): AdminMe {
  return { ...mockAdminMe }
}

export function getMockDashboardOverview(): DashboardOverview {
  return {
    user_total: mockUsers.length,
    order_total: mockOrders.length,
    pending_audit_count: mockOrders.filter((item) => item.order_status_code === 'PENDING_AUDIT').length,
    today_exchange_point: mockOrders.reduce((sum, item) => sum + item.total_point_amount, 0),
    product_total: mockProducts.length,
  }
}

export function listMockUsers(query?: Partial<PageQuery>, keyword = ''): ApiPageData<AdminUser> {
  const normalizedKeyword = keyword.trim()
  const source = normalizedKeyword
    ? mockUsers.filter((item) =>
        [item.nick_name, item.phone_masked, String(item.id)].some((field) => field.includes(normalizedKeyword)),
      )
    : mockUsers
  return paginate(source, query)
}

export function updateMockUserStatus(userId: number, status: UserStatus): void {
  const target = mockUsers.find((item) => item.id === userId)
  if (!target) return
  target.user_status_code = status
}

export function adjustMockUserPoints(userId: number, delta: number): void {
  const target = mockUsers.find((item) => item.id === userId)
  if (!target) return
  target.point_balance += delta
  mockPointLedger.unshift({
    id: Date.now(),
    user_name: target.nick_name,
    biz_type_code: 'MANUAL_ADJUST',
    change_amount: delta,
    balance_after: target.point_balance,
    occurred_at: new Date().toISOString().slice(0, 19).replace('T', ' '),
  })
}

export function listMockProducts(query?: Partial<PageQuery>): ApiPageData<AdminProduct> {
  return paginate(mockProducts, query)
}

export function listMockOrders(query?: Partial<PageQuery>): ApiPageData<AdminOrder> {
  return paginate(mockOrders, query)
}

export function listMockPointLedger(query?: Partial<PageQuery>): ApiPageData<AdminPointLedger> {
  return paginate(mockPointLedger, query)
}

export function listMockCategories(query?: Partial<PageQuery>, keyword = ''): ApiPageData<AdminCategory> {
  const key = keyword.trim().toLowerCase()
  const source = key
    ? mockCategories.filter((item) => [item.category_name, String(item.id)].some((field) => field.toLowerCase().includes(key)))
    : mockCategories
  source.sort((a, b) => b.sort_no - a.sort_no || b.id - a.id)
  return paginate(source, query)
}

export function createMockCategory(payload: Pick<AdminCategory, 'category_name' | 'sort_no'>): AdminCategory {
  const category: AdminCategory = {
    id: ++categoryIdSeed,
    category_name: payload.category_name,
    sort_no: payload.sort_no,
    status_code: 'ENABLED',
    product_count: 0,
    updated_at: new Date().toISOString().slice(0, 19).replace('T', ' '),
  }
  mockCategories.unshift(category)
  return category
}

export function updateMockCategory(categoryId: number, payload: Partial<Pick<AdminCategory, 'category_name' | 'sort_no'>>): void {
  const target = mockCategories.find((item) => item.id === categoryId)
  if (!target) return
  if (payload.category_name !== undefined) target.category_name = payload.category_name
  if (payload.sort_no !== undefined) target.sort_no = payload.sort_no
  target.updated_at = new Date().toISOString().slice(0, 19).replace('T', ' ')
}

export function deleteMockCategory(categoryId: number): void {
  const index = mockCategories.findIndex((item) => item.id === categoryId)
  if (index < 0) return
  mockCategories.splice(index, 1)
}

export function updateMockCategoryStatus(categoryId: number, status: CategoryStatus): void {
  const target = mockCategories.find((item) => item.id === categoryId)
  if (!target) return
  target.status_code = status
  target.updated_at = new Date().toISOString().slice(0, 19).replace('T', ' ')
}

export function sortMockCategories(sortPairs: Array<{ category_id: number; sort_no: number }>): void {
  sortPairs.forEach((pair) => {
    const target = mockCategories.find((item) => item.id === pair.category_id)
    if (!target) return
    target.sort_no = pair.sort_no
    target.updated_at = new Date().toISOString().slice(0, 19).replace('T', ' ')
  })
}

export function listMockRecommendSlots(query?: Partial<PageQuery>, keyword = ''): ApiPageData<AdminRecommendSlot> {
  const key = keyword.trim().toLowerCase()
  const source = key
    ? mockRecommendSlots.filter((item) => [item.slot_name, item.slot_code, String(item.id)].some((field) => field.toLowerCase().includes(key)))
    : mockRecommendSlots
  source.forEach((slot) => {
    slot.item_count = mockRecommendItems.filter((item) => item.slot_id === slot.id).length
  })
  source.sort((a, b) => b.updated_at.localeCompare(a.updated_at))
  return paginate(source, query)
}

export function createMockRecommendSlot(payload: Pick<AdminRecommendSlot, 'slot_name' | 'slot_code'>): AdminRecommendSlot {
  const slot: AdminRecommendSlot = {
    id: ++slotIdSeed,
    slot_name: payload.slot_name,
    slot_code: payload.slot_code,
    status_code: 'ENABLED',
    item_count: 0,
    updated_at: new Date().toISOString().slice(0, 19).replace('T', ' '),
  }
  mockRecommendSlots.unshift(slot)
  return slot
}

export function updateMockRecommendSlot(slotId: number, payload: Partial<Pick<AdminRecommendSlot, 'slot_name' | 'slot_code'>>): void {
  const target = mockRecommendSlots.find((item) => item.id === slotId)
  if (!target) return
  if (payload.slot_name !== undefined) target.slot_name = payload.slot_name
  if (payload.slot_code !== undefined) target.slot_code = payload.slot_code
  target.updated_at = new Date().toISOString().slice(0, 19).replace('T', ' ')
}

export function updateMockRecommendSlotStatus(slotId: number, status: RecommendSlotStatus): void {
  const target = mockRecommendSlots.find((item) => item.id === slotId)
  if (!target) return
  target.status_code = status
  target.updated_at = new Date().toISOString().slice(0, 19).replace('T', ' ')
}

export function listMockRecommendItems(slotId: number): AdminRecommendItem[] {
  return mockRecommendItems
    .filter((item) => item.slot_id === slotId)
    .sort((a, b) => b.sort_no - a.sort_no || b.id - a.id)
}

export function createMockRecommendItem(payload: Omit<AdminRecommendItem, 'id'>): AdminRecommendItem {
  const item: AdminRecommendItem = {
    ...payload,
    id: ++recommendItemIdSeed,
  }
  mockRecommendItems.unshift(item)
  const slot = mockRecommendSlots.find((current) => current.id === payload.slot_id)
  if (slot) {
    slot.item_count += 1
    slot.updated_at = new Date().toISOString().slice(0, 19).replace('T', ' ')
  }
  return item
}

export function updateMockRecommendItem(itemId: number, payload: Partial<Omit<AdminRecommendItem, 'id' | 'slot_id'>>): void {
  const target = mockRecommendItems.find((item) => item.id === itemId)
  if (!target) return
  if (payload.spu_id !== undefined) target.spu_id = payload.spu_id
  if (payload.product_name !== undefined) target.product_name = payload.product_name
  if (payload.point_price !== undefined) target.point_price = payload.point_price
  if (payload.sort_no !== undefined) target.sort_no = payload.sort_no
  if (payload.status_code !== undefined) target.status_code = payload.status_code
  if (payload.start_at !== undefined) target.start_at = payload.start_at
  if (payload.end_at !== undefined) target.end_at = payload.end_at
}

export function deleteMockRecommendItem(itemId: number): void {
  const index = mockRecommendItems.findIndex((item) => item.id === itemId)
  if (index < 0) return
  const [target] = mockRecommendItems.splice(index, 1)
  const slot = mockRecommendSlots.find((current) => current.id === target.slot_id)
  if (slot) {
    slot.item_count = Math.max(0, slot.item_count - 1)
    slot.updated_at = new Date().toISOString().slice(0, 19).replace('T', ' ')
  }
}

export function sortMockRecommendItems(slotId: number, sortPairs: Array<{ item_id: number; sort_no: number }>): void {
  sortPairs.forEach((pair) => {
    const target = mockRecommendItems.find((item) => item.slot_id === slotId && item.id === pair.item_id)
    if (!target) return
    target.sort_no = pair.sort_no
  })
}
