import type { ApiPageData, PageQuery } from '../api/types'
import { normalizePageQuery } from '../api/pagination'

export type AdminOperatorStatus = 'ACTIVE' | 'FROZEN'

export type AdminOperator = {
  id: number
  username: string
  display_name: string
  phone: string
  status_code: AdminOperatorStatus
  roles: string[]
  last_login_at: string
}

export type RoleStatus = 'ACTIVE' | 'DISABLED'

export type RoleItem = {
  id: number
  role_name: string
  role_code: string
  status_code: RoleStatus
  permission_count: number
  remark: string
}

export type PermissionItem = {
  id: number
  permission_name: string
  permission_code: string
  module_name: string
  method: string
  path: string
  parent_id: number | null
}

export type PermissionTreeNode = {
  label: string
  key: number
  children?: PermissionTreeNode[]
}

const mockAdminOperators: AdminOperator[] = [
  {
    id: 1,
    username: 'admin',
    display_name: '系统管理员',
    phone: '13800001111',
    status_code: 'ACTIVE',
    roles: ['超级管理员'],
    last_login_at: '2026-03-17 14:30:00',
  },
  {
    id: 2,
    username: 'ops_lead',
    display_name: '运营主管',
    phone: '13800002222',
    status_code: 'ACTIVE',
    roles: ['订单运营', '商品运营'],
    last_login_at: '2026-03-17 10:08:00',
  },
  {
    id: 3,
    username: 'auditor',
    display_name: '审核专员',
    phone: '13800003333',
    status_code: 'FROZEN',
    roles: ['订单审核'],
    last_login_at: '2026-03-16 18:21:00',
  },
]

const mockRoles: RoleItem[] = [
  { id: 10, role_name: '超级管理员', role_code: 'SUPER_ADMIN', status_code: 'ACTIVE', permission_count: 24, remark: '拥有全部权限' },
  { id: 11, role_name: '订单审核', role_code: 'ORDER_AUDIT', status_code: 'ACTIVE', permission_count: 7, remark: '处理审核与发货流程' },
  { id: 12, role_name: '商品运营', role_code: 'PRODUCT_OPS', status_code: 'ACTIVE', permission_count: 6, remark: '维护商品和推荐位' },
]

const mockPermissions: PermissionItem[] = [
  { id: 101, permission_name: '仪表盘查看', permission_code: 'dashboard:view', module_name: '仪表盘', method: 'GET', path: '/api/v1/admin/dashboard/overview', parent_id: null },
  { id: 201, permission_name: '用户列表查看', permission_code: 'users:view', module_name: '用户管理', method: 'GET', path: '/api/v1/admin/users', parent_id: null },
  { id: 202, permission_name: '用户冻结/解冻', permission_code: 'users:status', module_name: '用户管理', method: 'PUT', path: '/api/v1/admin/users/{userId}/status', parent_id: 201 },
  { id: 203, permission_name: '碎片调整', permission_code: 'users:points_adjust', module_name: '用户管理', method: 'POST', path: '/api/v1/admin/users/{userId}/points/adjust', parent_id: 201 },
  { id: 301, permission_name: '商品列表查看', permission_code: 'products:view', module_name: '商品中心', method: 'GET', path: '/api/v1/admin/products/spu', parent_id: null },
  { id: 302, permission_name: '商品新增', permission_code: 'products:create', module_name: '商品中心', method: 'POST', path: '/api/v1/admin/products/spu', parent_id: 301 },
  { id: 401, permission_name: '订单列表查看', permission_code: 'orders:view', module_name: '订单中心', method: 'GET', path: '/api/v1/admin/orders', parent_id: null },
  { id: 402, permission_name: '订单审核通过', permission_code: 'orders:approve', module_name: '订单中心', method: 'POST', path: '/api/v1/admin/orders/{orderId}/approve', parent_id: 401 },
  { id: 403, permission_name: '订单发货', permission_code: 'orders:ship', module_name: '订单中心', method: 'POST', path: '/api/v1/admin/orders/{orderId}/ship', parent_id: 401 },
  { id: 501, permission_name: '角色列表查看', permission_code: 'roles:view', module_name: '权限管理', method: 'GET', path: '/api/v1/admin/roles', parent_id: null },
  { id: 502, permission_name: '角色新增', permission_code: 'roles:create', module_name: '权限管理', method: 'POST', path: '/api/v1/admin/roles', parent_id: 501 },
  { id: 503, permission_name: '角色授权', permission_code: 'roles:grant', module_name: '权限管理', method: 'PUT', path: '/api/v1/admin/roles/{roleId}/permissions', parent_id: 501 },
]

const rolePermissionMap: Record<number, number[]> = {
  10: mockPermissions.map((item) => item.id),
  11: [401, 402, 403, 201, 202, 203, 101],
  12: [301, 302, 101, 501, 502, 503],
}

let roleIdSeed = 100

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

export function listMockAdminOperators(query?: Partial<PageQuery>, keyword = ''): ApiPageData<AdminOperator> {
  const key = keyword.trim()
  const source = key
    ? mockAdminOperators.filter((item) =>
        [item.username, item.display_name, item.phone].some((field) => field.includes(key)),
      )
    : mockAdminOperators
  return paginate(source, query)
}

export function updateMockAdminOperatorStatus(adminUserId: number, status: AdminOperatorStatus): void {
  const target = mockAdminOperators.find((item) => item.id === adminUserId)
  if (!target) return
  target.status_code = status
}

export function resetMockAdminOperatorPassword(adminUserId: number): string {
  const target = mockAdminOperators.find((item) => item.id === adminUserId)
  if (!target) return 'Temp@123456'
  return `${target.username}@123`
}

export function listMockRoles(query?: Partial<PageQuery>, keyword = ''): ApiPageData<RoleItem> {
  const key = keyword.trim()
  const source = key
    ? mockRoles.filter((item) =>
        [item.role_name, item.role_code, item.remark].some((field) => field.includes(key)),
      )
    : mockRoles
  return paginate(source, query)
}

export function createMockRole(payload: Pick<RoleItem, 'role_name' | 'role_code' | 'remark'>): RoleItem {
  const role: RoleItem = {
    id: ++roleIdSeed,
    role_name: payload.role_name,
    role_code: payload.role_code,
    remark: payload.remark,
    status_code: 'ACTIVE',
    permission_count: 0,
  }
  mockRoles.unshift(role)
  rolePermissionMap[role.id] = []
  return role
}

export function updateMockRole(roleId: number, payload: Partial<Pick<RoleItem, 'role_name' | 'role_code' | 'remark' | 'status_code'>>): void {
  const role = mockRoles.find((item) => item.id === roleId)
  if (!role) return
  if (payload.role_name !== undefined) role.role_name = payload.role_name
  if (payload.role_code !== undefined) role.role_code = payload.role_code
  if (payload.remark !== undefined) role.remark = payload.remark
  if (payload.status_code !== undefined) role.status_code = payload.status_code
}

export function deleteMockRole(roleId: number): void {
  const idx = mockRoles.findIndex((item) => item.id === roleId)
  if (idx < 0) return
  mockRoles.splice(idx, 1)
  delete rolePermissionMap[roleId]
}

export function listMockPermissions(): PermissionItem[] {
  return mockPermissions.slice()
}

export function getMockPermissionTree(): PermissionTreeNode[] {
  const roots = mockPermissions.filter((item) => item.parent_id === null)
  return roots.map((root) => ({
    label: `${root.permission_name}（${root.permission_code}）`,
    key: root.id,
    children: mockPermissions
      .filter((item) => item.parent_id === root.id)
      .map((item) => ({
        label: `${item.permission_name}（${item.permission_code}）`,
        key: item.id,
      })),
  }))
}

export function listRolePermissionIds(roleId: number): number[] {
  return rolePermissionMap[roleId]?.slice() ?? []
}

export function assignMockRolePermissions(roleId: number, permissionIds: number[]): void {
  const role = mockRoles.find((item) => item.id === roleId)
  if (!role) return
  rolePermissionMap[roleId] = permissionIds.slice()
  role.permission_count = permissionIds.length
}
