import { API_ENDPOINTS } from '../endpoints'
import { del, get, post, put } from '../http'
import { compilePath } from '../path'
import type { ApiPageData, PageQuery } from '../types'
import type {
  AdminOperator,
  AdminOperatorStatus,
  PermissionItem,
  PermissionTreeNode,
  RoleItem,
  RoleStatus,
} from '../../mock/rbac'

export type AdminOperatorListQuery = Partial<PageQuery> & {
  keyword?: string
}

export type RoleListQuery = Partial<PageQuery> & {
  keyword?: string
}

export type SaveRolePayload = {
  role_name: string
  role_code: string
  remark: string
  status_code?: RoleStatus
}

export async function fetchAdminOperators(query: AdminOperatorListQuery): Promise<ApiPageData<AdminOperator>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_ADMIN_USERS
  return await get<ApiPageData<AdminOperator>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function updateAdminOperatorStatus(adminUserId: number, status: AdminOperatorStatus): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_ADMIN_USERS_BY_ADMIN_USER_ID_STATUS
  await put(compilePath(endpoint.path, { adminUserId }), { status_code: status }, { scope: 'admin' })
}

export async function resetAdminOperatorPassword(adminUserId: number): Promise<string> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_ADMIN_USERS_BY_ADMIN_USER_ID_RESET_PASSWORD
  const result = await post<{ temp_password?: string }>(compilePath(endpoint.path, { adminUserId }), {}, { scope: 'admin' })
  return result?.temp_password ?? ''
}

export async function fetchAdminOperatorPassword(adminUserId: number): Promise<string> {
  const result = await get<{ password?: string }>(`/api/v1/admin/admin-users/${adminUserId}/password`, { scope: 'admin' })
  return result?.password ?? ''
}

export async function fetchRoles(query: RoleListQuery): Promise<ApiPageData<RoleItem>> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_ROLES
  return await get<ApiPageData<RoleItem>>(endpoint.path, {
    scope: 'admin',
    params: query,
  })
}

export async function createRole(payload: SaveRolePayload): Promise<void> {
  const endpoint = API_ENDPOINTS.POST_ADMIN_ROLES
  await post(endpoint.path, payload, { scope: 'admin' })
}

export async function updateRole(roleId: number, payload: SaveRolePayload): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_ROLES_BY_ROLE_ID
  await put(compilePath(endpoint.path, { roleId }), payload, { scope: 'admin' })
}

export async function deleteRole(roleId: number): Promise<void> {
  const endpoint = API_ENDPOINTS.DELETE_ADMIN_ROLES_BY_ROLE_ID
  await del(compilePath(endpoint.path, { roleId }), { scope: 'admin' })
}

export async function fetchPermissions(): Promise<PermissionItem[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_PERMISSIONS
  const data = await get<{ list?: PermissionItem[] } | PermissionItem[]>(endpoint.path, { scope: 'admin' })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function fetchPermissionTree(): Promise<PermissionTreeNode[]> {
  const endpoint = API_ENDPOINTS.GET_ADMIN_PERMISSIONS_TREE
  const data = await get<{ list?: PermissionTreeNode[] } | PermissionTreeNode[]>(endpoint.path, { scope: 'admin' })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export async function fetchRolePermissionIds(roleId: number): Promise<number[]> {
  const role = await get<{ permission_ids?: number[] }>(compilePath(API_ENDPOINTS.GET_ADMIN_ROLES_BY_ROLE_ID.path, { roleId }), {
    scope: 'admin',
  })
  return role.permission_ids ?? []
}

export async function assignRolePermissions(roleId: number, permissionIds: number[]): Promise<void> {
  const endpoint = API_ENDPOINTS.PUT_ADMIN_ROLES_BY_ROLE_ID_PERMISSIONS
  await put(compilePath(endpoint.path, { roleId }), { permission_ids: permissionIds }, { scope: 'admin' })
}
