import { API_ENDPOINTS } from '../endpoints'
import { del, get, post, put } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type { AdminSystemConfig, ConfigStatus, ConfigValueType } from '../../mock/platform-center'

export type SystemConfigListQuery = Partial<PageQuery> & {
  keyword?: string
}

export type SaveSystemConfigPayload = {
  config_key: string
  config_name: string
  config_value: string
  value_type_code: ConfigValueType
  group_code: string
  status_code: ConfigStatus
  remark: string
}

export async function fetchSystemConfigs(query: SystemConfigListQuery): Promise<ApiPageData<AdminSystemConfig>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_SYSTEM_CONFIGS
  return await get<ApiPageData<AdminSystemConfig>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function createSystemConfig(payload: SaveSystemConfigPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_SYSTEM_CONFIGS
  await post(endpoint.path, payload, { scope: 'admin' })
}

export async function updateSystemConfig(configId: number, payload: SaveSystemConfigPayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_SYSTEM_CONFIGS_BY_CONFIG_ID
  await put(compilePath(endpoint.path, { configId }), payload, { scope: 'admin' })
}

export async function deleteSystemConfig(configId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_SYSTEM_CONFIGS_BY_CONFIG_ID
  await del(compilePath(endpoint.path, { configId }), { scope: 'admin' })
}
