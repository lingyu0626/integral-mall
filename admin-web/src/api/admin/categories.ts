import { API_ENDPOINTS } from '../endpoints'
import { del, get, post, put } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type { AdminCategory, CategoryStatus } from '../../mock/admin'

export type AdminCategoryListQuery = Partial<PageQuery> & {
  keyword?: string
}

export type SaveCategoryPayload = {
  category_name: string
  sort_no: number
}

export async function fetchAdminCategories(query: AdminCategoryListQuery): Promise<ApiPageData<AdminCategory>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_CATEGORIES
  return await get<ApiPageData<AdminCategory>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function createAdminCategory(payload: SaveCategoryPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_CATEGORIES
  await post(endpoint.path, payload, { scope: 'admin' })
}

export async function updateAdminCategory(categoryId: number, payload: SaveCategoryPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_CATEGORIES_BY_CATEGORY_ID
  await put(compilePath(endpoint.path, { categoryId }), payload, { scope: 'admin' })
}

export async function updateAdminCategoryStatus(categoryId: number, status: CategoryStatus): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_CATEGORIES_BY_CATEGORY_ID_STATUS
  await put(compilePath(endpoint.path, { categoryId }), { status_code: status }, { scope: 'admin' })
}

export async function deleteAdminCategory(categoryId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_CATEGORIES_BY_CATEGORY_ID
  await del(compilePath(endpoint.path, { categoryId }), { scope: 'admin' })
}

export async function sortAdminCategories(sortPairs: Array<{ category_id: number; sort_no: number }>): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_CATEGORIES_SORT
  await put(endpoint.path, { categories: sortPairs }, { scope: 'admin' })
}
