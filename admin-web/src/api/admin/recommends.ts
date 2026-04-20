import { API_ENDPOINTS } from '../endpoints'
import { del, get, post, put } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type { AdminRecommendItem, AdminRecommendSlot, RecommendSlotStatus } from '../../mock/admin'

export type RecommendSlotListQuery = Partial<PageQuery> & {
  keyword?: string
}

export type SaveRecommendSlotPayload = {
  slot_name: string
  slot_code: string
}

export type SaveRecommendItemPayload = {
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

export async function fetchRecommendSlots(query: RecommendSlotListQuery): Promise<ApiPageData<AdminRecommendSlot>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_RECOMMEND_SLOTS
  return await get<ApiPageData<AdminRecommendSlot>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function createRecommendSlot(payload: SaveRecommendSlotPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_RECOMMEND_SLOTS
  await post(endpoint.path, payload, { scope: 'admin' })
}

export async function updateRecommendSlot(slotId: number, payload: SaveRecommendSlotPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_RECOMMEND_SLOTS_BY_SLOT_ID
  await put(compilePath(endpoint.path, { slotId }), payload, { scope: 'admin' })
}

export async function updateRecommendSlotStatus(slotId: number, status: RecommendSlotStatus): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_RECOMMEND_SLOTS_BY_SLOT_ID_STATUS
  await put(compilePath(endpoint.path, { slotId }), { status_code: status }, { scope: 'admin' })
}

export async function fetchRecommendItems(slotId: number): Promise<AdminRecommendItem[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_RECOMMEND_SLOTS_BY_SLOT_ID_ITEMS
  const data = await get<{ list?: AdminRecommendItem[] } | AdminRecommendItem[]>(compilePath(endpoint.path, { slotId }), {
    scope: 'admin',
  })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function createRecommendItem(slotId: number, payload: SaveRecommendItemPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_RECOMMEND_SLOTS_BY_SLOT_ID_ITEMS
  await post(compilePath(endpoint.path, { slotId }), payload, { scope: 'admin' })
}

export async function updateRecommendItem(itemId: number, payload: Partial<SaveRecommendItemPayload>): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_RECOMMEND_ITEMS_BY_ITEM_ID
  await put(compilePath(endpoint.path, { itemId }), payload, { scope: 'admin' })
}

export async function deleteRecommendItem(itemId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_RECOMMEND_ITEMS_BY_ITEM_ID
  await del(compilePath(endpoint.path, { itemId }), { scope: 'admin' })
}

export async function sortRecommendItems(slotId: number, sortPairs: Array<{ item_id: number; sort_no: number }>): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_RECOMMEND_SLOTS_BY_SLOT_ID_ITEMS_SORT
  await put(compilePath(endpoint.path, { slotId }), { items: sortPairs }, { scope: 'admin' })
}
