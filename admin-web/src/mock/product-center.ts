import type { ApiPageData, PageQuery } from '../api/types'
import { normalizePageQuery } from '../api/pagination'

export type ProductStatus = 'ON_SHELF' | 'OFF_SHELF'
export type SkuStatus = 'ENABLED' | 'DISABLED'
export type AttrValueType = 'TEXT' | 'NUMBER' | 'ENUM'

export type AdminSpu = {
  id: number
  skc_code?: string
  sku_count?: number
  spu_name: string
  category_name: string
  category_names?: string[]
  point_price_min: number
  point_price_max: number
  total_stock: number
  status_code: ProductStatus
  recommend_flag: boolean
  updated_at: string
  detail_html?: string
}

export type AdminSku = {
  id: number
  sku_code?: string
  spu_id: number
  sku_name: string
  spec_text?: string
  point_price: number
  stock_available: number
  status_code: SkuStatus
}

export type AdminProductMedia = {
  id: number
  spu_id: number
  media_type: 'IMAGE' | 'VIDEO'
  media_url: string
  sort_no: number
}

export type AdminAttrDef = {
  id: number
  attr_name: string
  attr_code: string
  value_type: AttrValueType
  required_flag: boolean
  status_code: 'ENABLED' | 'DISABLED'
  updated_at: string
}

const mockSpus: AdminSpu[] = [
  {
    id: 101,
    skc_code: 'SKC000000000101',
    spu_name: '飞天茅台53度 500ML',
    category_name: '白酒',
    point_price_min: 1888,
    point_price_max: 2388,
    total_stock: 106,
    status_code: 'ON_SHELF',
    recommend_flag: true,
    updated_at: '2026-03-17 15:12:00',
  },
  {
    id: 102,
    skc_code: 'SKC000000000102',
    spu_name: '苹果 Watch Ultra3',
    category_name: '数码',
    point_price_min: 5988,
    point_price_max: 6988,
    total_stock: 47,
    status_code: 'ON_SHELF',
    recommend_flag: true,
    updated_at: '2026-03-17 15:09:00',
  },
  {
    id: 103,
    skc_code: 'SKC000000000103',
    spu_name: '至臻玩家微信群入群资格',
    category_name: '生活权益',
    point_price_min: 8888,
    point_price_max: 8888,
    total_stock: 9999,
    status_code: 'OFF_SHELF',
    recommend_flag: false,
    updated_at: '2026-03-16 20:30:00',
  },
]

const mockSkus: AdminSku[] = [
  { id: 5001, sku_code: 'SKU000000005001', spu_id: 101, sku_name: '标准装', spec_text: '500ml x 1', point_price: 1888, stock_available: 58, status_code: 'ENABLED' },
  { id: 5002, sku_code: 'SKU000000005002', spu_id: 101, sku_name: '礼盒装', spec_text: '500ml x 1 + 礼袋', point_price: 2388, stock_available: 48, status_code: 'ENABLED' },
  { id: 5003, sku_code: 'SKU000000005003', spu_id: 102, sku_name: '49mm 钛金属', spec_text: '黑色海洋表带', point_price: 5988, stock_available: 27, status_code: 'ENABLED' },
  { id: 5004, sku_code: 'SKU000000005004', spu_id: 102, sku_name: '49mm 钛金属', spec_text: '橙色高山回环表带', point_price: 6988, stock_available: 20, status_code: 'ENABLED' },
  { id: 5005, sku_code: 'SKU000000005005', spu_id: 103, sku_name: '入群名额', spec_text: '有效期一年', point_price: 8888, stock_available: 9999, status_code: 'DISABLED' },
]

const mockMedia: AdminProductMedia[] = [
  { id: 8001, spu_id: 101, media_type: 'IMAGE', media_url: 'https://picsum.photos/id/1060/800/500', sort_no: 100 },
  { id: 8002, spu_id: 101, media_type: 'IMAGE', media_url: 'https://picsum.photos/id/292/800/500', sort_no: 90 },
  { id: 8003, spu_id: 102, media_type: 'IMAGE', media_url: 'https://picsum.photos/id/3/800/500', sort_no: 100 },
]

const mockAttrDefs: AdminAttrDef[] = [
  { id: 9001, attr_name: '容量', attr_code: 'CAPACITY', value_type: 'TEXT', required_flag: true, status_code: 'ENABLED', updated_at: '2026-03-17 11:10:00' },
  { id: 9002, attr_name: '酒精度', attr_code: 'ABV', value_type: 'NUMBER', required_flag: true, status_code: 'ENABLED', updated_at: '2026-03-17 11:09:00' },
  { id: 9003, attr_name: '版本', attr_code: 'EDITION', value_type: 'ENUM', required_flag: false, status_code: 'ENABLED', updated_at: '2026-03-17 11:08:00' },
]

let spuIdSeed = 1000
let skuIdSeed = 10000
let mediaIdSeed = 12000
let attrIdSeed = 15000

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

function syncSpuStock(spuId: number): void {
  const target = mockSpus.find((item) => item.id === spuId)
  if (!target) return
  const stock = mockSkus
    .filter((item) => item.spu_id === spuId && item.status_code === 'ENABLED')
    .reduce((sum, item) => sum + item.stock_available, 0)
  target.total_stock = stock
  target.updated_at = nowText()
}

function syncSpuPriceRange(spuId: number): void {
  const target = mockSpus.find((item) => item.id === spuId)
  if (!target) return
  const prices = mockSkus
    .filter((item) => item.spu_id === spuId && item.status_code === 'ENABLED')
    .map((item) => item.point_price)
  if (prices.length === 0) {
    target.point_price_min = 0
    target.point_price_max = 0
  } else {
    target.point_price_min = Math.min(...prices)
    target.point_price_max = Math.max(...prices)
  }
  target.updated_at = nowText()
}

export function listMockSpus(query?: Partial<PageQuery>, keyword = '', status = ''): ApiPageData<AdminSpu> {
  const key = keyword.trim().toLowerCase()
  const source = mockSpus
    .map((item) => ({
      ...item,
      sku_count: mockSkus.filter((sku) => sku.spu_id === item.id).length,
    }))
    .filter((item) => {
    const passKeyword = key
      ? [item.spu_name, item.category_name, String(item.id)].some((field) => field.toLowerCase().includes(key))
      : true
    const passStatus = status ? item.status_code === status : true
    return passKeyword && passStatus
  })
  source.sort((a, b) => b.updated_at.localeCompare(a.updated_at))
  return paginate(source, query)
}

export function createMockSpu(payload: Pick<AdminSpu, 'spu_name' | 'category_name' | 'point_price_min' | 'point_price_max'>): AdminSpu {
  const spu: AdminSpu = {
    id: ++spuIdSeed,
    skc_code: `SKC${String(spuIdSeed).padStart(12, '0')}`,
    spu_name: payload.spu_name,
    category_name: payload.category_name,
    point_price_min: payload.point_price_min,
    point_price_max: payload.point_price_max,
    total_stock: 0,
    status_code: 'OFF_SHELF',
    recommend_flag: false,
    updated_at: nowText(),
  }
  mockSpus.unshift(spu)
  return spu
}

export function updateMockSpu(spuId: number, payload: Partial<Pick<AdminSpu, 'spu_name' | 'category_name' | 'point_price_min' | 'point_price_max'>>): void {
  const target = mockSpus.find((item) => item.id === spuId)
  if (!target) return
  if (payload.spu_name !== undefined) target.spu_name = payload.spu_name
  if (payload.category_name !== undefined) target.category_name = payload.category_name
  if (payload.point_price_min !== undefined) target.point_price_min = payload.point_price_min
  if (payload.point_price_max !== undefined) target.point_price_max = payload.point_price_max
  target.updated_at = nowText()
}

export function updateMockSpuStatus(spuId: number, status: ProductStatus): void {
  const target = mockSpus.find((item) => item.id === spuId)
  if (!target) return
  target.status_code = status
  target.updated_at = nowText()
}

export function updateMockSpuRecommend(spuId: number, recommendFlag: boolean): void {
  const target = mockSpus.find((item) => item.id === spuId)
  if (!target) return
  target.recommend_flag = recommendFlag
  target.updated_at = nowText()
}

export function listMockSkus(spuId: number): AdminSku[] {
  return mockSkus
    .filter((item) => item.spu_id === spuId)
    .sort((a, b) => b.id - a.id)
}

export function createMockSku(spuId: number, payload: Omit<AdminSku, 'id' | 'spu_id'>): AdminSku {
  const nextId = ++skuIdSeed
  const sku: AdminSku = {
    ...payload,
    id: nextId,
    sku_code: payload.sku_code || `SKU${String(nextId).padStart(12, '0')}`,
    spu_id: spuId,
  }
  mockSkus.unshift(sku)
  syncSpuStock(spuId)
  syncSpuPriceRange(spuId)
  return sku
}

export function updateMockSku(skuId: number, payload: Partial<Omit<AdminSku, 'id' | 'spu_id'>>): void {
  const target = mockSkus.find((item) => item.id === skuId)
  if (!target) return
  if (payload.sku_name !== undefined) target.sku_name = payload.sku_name
  if (payload.spec_text !== undefined) target.spec_text = payload.spec_text
  if (payload.point_price !== undefined) target.point_price = payload.point_price
  if (payload.stock_available !== undefined) target.stock_available = payload.stock_available
  if (payload.status_code !== undefined) target.status_code = payload.status_code
  syncSpuStock(target.spu_id)
  syncSpuPriceRange(target.spu_id)
}

export function adjustMockSkuStock(skuId: number, deltaStock: number): void {
  const target = mockSkus.find((item) => item.id === skuId)
  if (!target) return
  target.stock_available = Math.max(0, target.stock_available + deltaStock)
  syncSpuStock(target.spu_id)
}

export function listMockSpuMedia(spuId: number): AdminProductMedia[] {
  return mockMedia
    .filter((item) => item.spu_id === spuId)
    .sort((a, b) => b.sort_no - a.sort_no || b.id - a.id)
}

export function createMockSpuMedia(spuId: number, payload: Omit<AdminProductMedia, 'id' | 'spu_id'>): AdminProductMedia {
  const media: AdminProductMedia = {
    ...payload,
    id: ++mediaIdSeed,
    spu_id: spuId,
  }
  mockMedia.unshift(media)
  return media
}

export function deleteMockSpuMedia(mediaId: number): void {
  const index = mockMedia.findIndex((item) => item.id === mediaId)
  if (index < 0) return
  mockMedia.splice(index, 1)
}

export function listMockAttrDefs(query?: Partial<PageQuery>, keyword = ''): ApiPageData<AdminAttrDef> {
  const key = keyword.trim().toLowerCase()
  const source = key
    ? mockAttrDefs.filter((item) => [item.attr_name, item.attr_code].some((field) => field.toLowerCase().includes(key)))
    : mockAttrDefs
  source.sort((a, b) => b.updated_at.localeCompare(a.updated_at))
  return paginate(source, query)
}

export function createMockAttrDef(payload: Pick<AdminAttrDef, 'attr_name' | 'attr_code' | 'value_type' | 'required_flag'>): AdminAttrDef {
  const attr: AdminAttrDef = {
    id: ++attrIdSeed,
    attr_name: payload.attr_name,
    attr_code: payload.attr_code,
    value_type: payload.value_type,
    required_flag: payload.required_flag,
    status_code: 'ENABLED',
    updated_at: nowText(),
  }
  mockAttrDefs.unshift(attr)
  return attr
}

export function updateMockAttrDef(attrDefId: number, payload: Partial<Pick<AdminAttrDef, 'attr_name' | 'attr_code' | 'value_type' | 'required_flag' | 'status_code'>>): void {
  const target = mockAttrDefs.find((item) => item.id === attrDefId)
  if (!target) return
  if (payload.attr_name !== undefined) target.attr_name = payload.attr_name
  if (payload.attr_code !== undefined) target.attr_code = payload.attr_code
  if (payload.value_type !== undefined) target.value_type = payload.value_type
  if (payload.required_flag !== undefined) target.required_flag = payload.required_flag
  if (payload.status_code !== undefined) target.status_code = payload.status_code
  target.updated_at = nowText()
}

export function deleteMockAttrDef(attrDefId: number): void {
  const index = mockAttrDefs.findIndex((item) => item.id === attrDefId)
  if (index < 0) return
  mockAttrDefs.splice(index, 1)
}
