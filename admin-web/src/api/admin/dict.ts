import { API_ENDPOINTS } from '../endpoints'
import { del, get, post, put } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type { AdminDictItem, AdminDictType, DictStatus } from '../../mock/platform-center'

export type DictTypeListQuery = Partial<PageQuery> & {
  keyword?: string
}

export type SaveDictTypePayload = {
  dict_type_code: string
  dict_type_name: string
  status_code: DictStatus
  remark: string
}

export type SaveDictItemPayload = {
  item_code: string
  item_name: string
  item_value: string
  sort_no: number
  status_code: DictStatus
}

export async function fetchDictTypes(query: DictTypeListQuery): Promise<ApiPageData<AdminDictType>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_DICT_TYPES
  return await get<ApiPageData<AdminDictType>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function createDictType(payload: SaveDictTypePayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_DICT_TYPES
  await post(endpoint.path, payload, { scope: 'admin' })
}

export async function updateDictType(typeId: number, payload: SaveDictTypePayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_DICT_TYPES_BY_TYPE_ID
  await put(compilePath(endpoint.path, { typeId }), payload, { scope: 'admin' })
}

export async function deleteDictType(typeId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_DICT_TYPES_BY_TYPE_ID
  await del(compilePath(endpoint.path, { typeId }), { scope: 'admin' })
}

export async function fetchDictItems(typeCode: string): Promise<AdminDictItem[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_DICT_TYPES_BY_TYPE_CODE_ITEMS
  const data = await get<{ list?: AdminDictItem[] } | AdminDictItem[]>(compilePath(endpoint.path, { typeCode }), {
    scope: 'admin',
  })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function createDictItem(typeCode: string, payload: SaveDictItemPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_DICT_TYPES_BY_TYPE_CODE_ITEMS
  await post(compilePath(endpoint.path, { typeCode }), payload, { scope: 'admin' })
}

export async function updateDictItem(itemId: number, payload: SaveDictItemPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_DICT_ITEMS_BY_ITEM_ID
  await put(compilePath(endpoint.path, { itemId }), payload, { scope: 'admin' })
}

export async function deleteDictItem(itemId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_DICT_ITEMS_BY_ITEM_ID
  await del(compilePath(endpoint.path, { itemId }), { scope: 'admin' })
}
