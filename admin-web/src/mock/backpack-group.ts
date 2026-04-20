import type { ApiPageData, PageQuery } from '../api/types'
import { normalizePageQuery } from '../api/pagination'

export type BackpackAssetStatus = 'ACTIVE' | 'USED' | 'EXPIRED' | 'INVALID'
export type BackpackAssetType = 'GROUP_QR' | 'COUPON' | 'PHYSICAL'

export type AdminBackpackAsset = {
  id: number
  asset_no: string
  user_name: string
  asset_name: string
  asset_type_code: BackpackAssetType
  status_code: BackpackAssetStatus
  obtained_at: string
  expire_at: string
}

export type AdminBackpackAssetFlow = {
  id: number
  asset_id: number
  action_type_code: 'GRANT' | 'INVALIDATE' | 'EXPIRE' | 'USE'
  action_text: string
  note: string
  operator_name: string
  occurred_at: string
}

export type GroupResourceStatus = 'ENABLED' | 'DISABLED'

export type AdminGroupResource = {
  id: number
  group_name: string
  qr_image_url: string
  intro_text: string
  max_member_count: number
  current_member_count: number
  expire_at: string
  status_code: GroupResourceStatus
  updated_at: string
}

const mockBackpackAssets: AdminBackpackAsset[] = [
  {
    id: 7001,
    asset_no: 'BA202603170001',
    user_name: '碎片用户A',
    asset_name: '至臻玩家微信群入群资格',
    asset_type_code: 'GROUP_QR',
    status_code: 'ACTIVE',
    obtained_at: '2026-03-17 09:20:00',
    expire_at: '2026-12-31 23:59:59',
  },
  {
    id: 7002,
    asset_no: 'BA202603160002',
    user_name: '潮玩用户B',
    asset_name: '潮玩盲盒兑换券',
    asset_type_code: 'COUPON',
    status_code: 'USED',
    obtained_at: '2026-03-16 10:30:00',
    expire_at: '2026-10-01 00:00:00',
  },
  {
    id: 7003,
    asset_no: 'BA202603150003',
    user_name: '茅台爱好者',
    asset_name: '飞天茅台优先购买资格',
    asset_type_code: 'PHYSICAL',
    status_code: 'EXPIRED',
    obtained_at: '2026-03-15 08:00:00',
    expire_at: '2026-03-16 23:59:59',
  },
]

const mockBackpackFlows: AdminBackpackAssetFlow[] = [
  {
    id: 90001,
    asset_id: 7001,
    action_type_code: 'GRANT',
    action_text: '发放资产',
    note: '运营活动赠送',
    operator_name: '系统管理员',
    occurred_at: '2026-03-17 09:20:00',
  },
  {
    id: 90002,
    asset_id: 7002,
    action_type_code: 'GRANT',
    action_text: '发放资产',
    note: '碎片兑换',
    operator_name: '系统',
    occurred_at: '2026-03-16 10:30:00',
  },
  {
    id: 90003,
    asset_id: 7002,
    action_type_code: 'USE',
    action_text: '核销使用',
    note: '用户兑换成功',
    operator_name: '系统',
    occurred_at: '2026-03-16 12:20:00',
  },
  {
    id: 90004,
    asset_id: 7003,
    action_type_code: 'GRANT',
    action_text: '发放资产',
    note: '后台补发',
    operator_name: '系统管理员',
    occurred_at: '2026-03-15 08:00:00',
  },
  {
    id: 90005,
    asset_id: 7003,
    action_type_code: 'EXPIRE',
    action_text: '资产过期',
    note: '系统自动过期',
    operator_name: '系统',
    occurred_at: '2026-03-17 00:00:00',
  },
]

const mockGroupResources: AdminGroupResource[] = [
  {
    id: 8101,
    group_name: '茅台玩家交流群',
    qr_image_url: 'https://picsum.photos/id/180/300/300',
    intro_text: '每日行情、酒友交流、官方活动第一时间通知',
    max_member_count: 500,
    current_member_count: 289,
    expire_at: '2026-12-31 23:59:59',
    status_code: 'ENABLED',
    updated_at: '2026-03-17 14:00:00',
  },
  {
    id: 8102,
    group_name: '潮玩福利群',
    qr_image_url: 'https://picsum.photos/id/201/300/300',
    intro_text: '每周掉落新品兑换福利',
    max_member_count: 300,
    current_member_count: 198,
    expire_at: '2026-09-30 23:59:59',
    status_code: 'ENABLED',
    updated_at: '2026-03-17 13:00:00',
  },
  {
    id: 8103,
    group_name: '高阶会员内测群',
    qr_image_url: 'https://picsum.photos/id/433/300/300',
    intro_text: '功能内测与意见反馈',
    max_member_count: 200,
    current_member_count: 56,
    expire_at: '2026-06-30 23:59:59',
    status_code: 'DISABLED',
    updated_at: '2026-03-16 16:00:00',
  },
]

let assetIdSeed = 9000
let flowIdSeed = 99000
let groupResourceIdSeed = 9000

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

function appendAssetFlow(
  assetId: number,
  actionTypeCode: AdminBackpackAssetFlow['action_type_code'],
  actionText: string,
  note: string,
  operatorName = '系统管理员',
): void {
  mockBackpackFlows.unshift({
    id: ++flowIdSeed,
    asset_id: assetId,
    action_type_code: actionTypeCode,
    action_text: actionText,
    note,
    operator_name: operatorName,
    occurred_at: nowText(),
  })
}

export function listMockBackpackAssets(query?: Partial<PageQuery>, keyword = '', statusCode: BackpackAssetStatus | '' = ''): ApiPageData<AdminBackpackAsset> {
  const key = keyword.trim().toLowerCase()
  const source = mockBackpackAssets.filter((item) => {
    const passKeyword = key
      ? [item.asset_no, item.user_name, item.asset_name].some((field) => field.toLowerCase().includes(key))
      : true
    const passStatus = statusCode ? item.status_code === statusCode : true
    return passKeyword && passStatus
  })
  source.sort((a, b) => b.obtained_at.localeCompare(a.obtained_at))
  return paginate(source, query)
}

export function listMockBackpackAssetFlows(assetId: number): AdminBackpackAssetFlow[] {
  return mockBackpackFlows
    .filter((item) => item.asset_id === assetId)
    .sort((a, b) => b.occurred_at.localeCompare(a.occurred_at))
}

export function grantMockBackpackAsset(payload: Pick<AdminBackpackAsset, 'user_name' | 'asset_name' | 'asset_type_code' | 'expire_at'>): AdminBackpackAsset {
  const asset: AdminBackpackAsset = {
    id: ++assetIdSeed,
    asset_no: `BA${new Date().toISOString().slice(0, 10).replace(/-/g, '')}${String(assetIdSeed).slice(-4)}`,
    user_name: payload.user_name,
    asset_name: payload.asset_name,
    asset_type_code: payload.asset_type_code,
    status_code: 'ACTIVE',
    obtained_at: nowText(),
    expire_at: payload.expire_at,
  }
  mockBackpackAssets.unshift(asset)
  appendAssetFlow(asset.id, 'GRANT', '发放资产', '后台人工发放')
  return asset
}

export function invalidateMockBackpackAsset(assetId: number, note = ''): void {
  const target = mockBackpackAssets.find((item) => item.id === assetId)
  if (!target || target.status_code !== 'ACTIVE') return
  target.status_code = 'INVALID'
  appendAssetFlow(assetId, 'INVALIDATE', '资产失效', note || '后台手工失效')
}

export function expireMockBackpackAsset(assetId: number, note = ''): void {
  const target = mockBackpackAssets.find((item) => item.id === assetId)
  if (!target || target.status_code !== 'ACTIVE') return
  target.status_code = 'EXPIRED'
  appendAssetFlow(assetId, 'EXPIRE', '资产过期', note || '后台手工过期')
}

export function listMockGroupResources(query?: Partial<PageQuery>, keyword = ''): ApiPageData<AdminGroupResource> {
  const key = keyword.trim().toLowerCase()
  const source = key
    ? mockGroupResources.filter((item) => [item.group_name, item.intro_text].some((field) => field.toLowerCase().includes(key)))
    : mockGroupResources
  source.sort((a, b) => b.updated_at.localeCompare(a.updated_at))
  return paginate(source, query)
}

export function createMockGroupResource(payload: Omit<AdminGroupResource, 'id' | 'updated_at'>): AdminGroupResource {
  const resource: AdminGroupResource = {
    ...payload,
    id: ++groupResourceIdSeed,
    updated_at: nowText(),
  }
  mockGroupResources.unshift(resource)
  return resource
}

export function updateMockGroupResource(resourceId: number, payload: Partial<Omit<AdminGroupResource, 'id' | 'updated_at'>>): void {
  const target = mockGroupResources.find((item) => item.id === resourceId)
  if (!target) return
  if (payload.group_name !== undefined) target.group_name = payload.group_name
  if (payload.qr_image_url !== undefined) target.qr_image_url = payload.qr_image_url
  if (payload.intro_text !== undefined) target.intro_text = payload.intro_text
  if (payload.max_member_count !== undefined) target.max_member_count = payload.max_member_count
  if (payload.current_member_count !== undefined) target.current_member_count = payload.current_member_count
  if (payload.expire_at !== undefined) target.expire_at = payload.expire_at
  if (payload.status_code !== undefined) target.status_code = payload.status_code
  target.updated_at = nowText()
}

export function updateMockGroupResourceStatus(resourceId: number, status: GroupResourceStatus): void {
  const target = mockGroupResources.find((item) => item.id === resourceId)
  if (!target) return
  target.status_code = status
  target.updated_at = nowText()
}

export function deleteMockGroupResource(resourceId: number): void {
  const index = mockGroupResources.findIndex((item) => item.id === resourceId)
  if (index < 0) return
  mockGroupResources.splice(index, 1)
}
