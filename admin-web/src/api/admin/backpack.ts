import { API_ENDPOINTS } from '../endpoints'
import { get, post } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type {
  AdminBackpackAsset,
  AdminBackpackAssetFlow,
  BackpackAssetStatus,
  BackpackAssetType,
} from '../../mock/backpack-group'

export type BackpackAssetListQuery = Partial<PageQuery> & {
  keyword?: string
  status_code?: BackpackAssetStatus | ''
}

export type GrantAssetPayload = {
  user_name: string
  asset_name: string
  asset_type_code: BackpackAssetType
  expire_at: string
}

export async function fetchBackpackAssets(query: BackpackAssetListQuery): Promise<ApiPageData<AdminBackpackAsset>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_BACKPACK_ASSETS
  return await get<ApiPageData<AdminBackpackAsset>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function fetchBackpackAssetFlows(assetId: number): Promise<AdminBackpackAssetFlow[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_BACKPACK_ASSET_FLOWS
  const data = await get<{ list?: AdminBackpackAssetFlow[] } | AdminBackpackAssetFlow[]>(endpoint.path, {
    scope: 'admin',
    params: { asset_id: assetId },
  })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function grantBackpackAsset(payload: GrantAssetPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_BACKPACK_ASSETS_GRANT
  await post(endpoint.path, payload, { scope: 'admin' })
}

export async function invalidateBackpackAsset(assetId: number, reason: string): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_BACKPACK_ASSETS_BY_ASSET_ID_INVALIDATE
  await post(compilePath(endpoint.path, { assetId }), { reason }, { scope: 'admin' })
}

export async function expireBackpackAsset(assetId: number, reason: string): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_BACKPACK_ASSETS_BY_ASSET_ID_EXPIRE
  await post(compilePath(endpoint.path, { assetId }), { reason }, { scope: 'admin' })
}
