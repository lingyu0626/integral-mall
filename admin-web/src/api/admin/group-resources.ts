import { API_ENDPOINTS } from '../endpoints'
import { del, get, post, put } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type { AdminGroupResource, GroupResourceStatus } from '../../mock/backpack-group'

export type GroupResourceListQuery = Partial<PageQuery> & {
  keyword?: string
}

export type SaveGroupResourcePayload = {
  group_name: string
  qr_image_url: string
  intro_text: string
  max_member_count: number
  current_member_count: number
  expire_at: string
  status_code: GroupResourceStatus
}

export async function fetchGroupResources(query: GroupResourceListQuery): Promise<ApiPageData<AdminGroupResource>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_GROUP_RESOURCES
  return await get<ApiPageData<AdminGroupResource>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function createGroupResource(payload: SaveGroupResourcePayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_GROUP_RESOURCES
  await post(endpoint.path, payload, { scope: 'admin' })
}

export async function updateGroupResource(resourceId: number, payload: SaveGroupResourcePayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_GROUP_RESOURCES_BY_RESOURCE_ID
  await put(compilePath(endpoint.path, { resourceId }), payload, { scope: 'admin' })
}

export async function updateGroupResourceStatus(resourceId: number, status: GroupResourceStatus): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_GROUP_RESOURCES_BY_RESOURCE_ID_STATUS
  await put(compilePath(endpoint.path, { resourceId }), { status_code: status }, { scope: 'admin' })
}

export async function deleteGroupResource(resourceId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_GROUP_RESOURCES_BY_RESOURCE_ID
  await del(compilePath(endpoint.path, { resourceId }), { scope: 'admin' })
}
