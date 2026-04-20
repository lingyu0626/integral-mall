import type { ApiPageData, PageQuery } from '../api/types'
import { normalizePageQuery } from '../api/pagination'

export type DictStatus = 'ENABLED' | 'DISABLED'
export type ConfigStatus = 'ENABLED' | 'DISABLED'
export type ConfigValueType = 'STRING' | 'NUMBER' | 'BOOLEAN' | 'JSON'

export type AdminDictType = {
  id: number
  dict_type_code: string
  dict_type_name: string
  status_code: DictStatus
  remark: string
  updated_at: string
}

export type AdminDictItem = {
  id: number
  dict_type_code: string
  item_code: string
  item_name: string
  item_value: string
  sort_no: number
  status_code: DictStatus
  updated_at: string
}

export type AdminSystemConfig = {
  id: number
  config_key: string
  config_name: string
  config_value: string
  value_type_code: ConfigValueType
  group_code: string
  status_code: ConfigStatus
  remark: string
  updated_at: string
}

export type AdminUploadedFile = {
  id: number
  file_name: string
  file_url: string
  mime_type: string
  file_size_kb: number
  uploaded_at: string
}

const mockDictTypes: AdminDictType[] = [
  { id: 1001, dict_type_code: 'ORDER_STATUS', dict_type_name: '订单状态', status_code: 'ENABLED', remark: '订单状态字典', updated_at: '2026-03-17 10:10:00' },
  { id: 1002, dict_type_code: 'ASSET_STATUS', dict_type_name: '资产状态', status_code: 'ENABLED', remark: '背包资产状态', updated_at: '2026-03-17 10:09:00' },
  { id: 1003, dict_type_code: 'USER_LEVEL', dict_type_name: '用户等级', status_code: 'DISABLED', remark: '会员等级字典', updated_at: '2026-03-16 19:30:00' },
]

const mockDictItems: AdminDictItem[] = [
  { id: 2001, dict_type_code: 'ORDER_STATUS', item_code: 'PENDING_AUDIT', item_name: '待审核', item_value: 'PENDING_AUDIT', sort_no: 100, status_code: 'ENABLED', updated_at: '2026-03-17 10:00:00' },
  { id: 2002, dict_type_code: 'ORDER_STATUS', item_code: 'PENDING_SHIP', item_name: '待发货', item_value: 'PENDING_SHIP', sort_no: 90, status_code: 'ENABLED', updated_at: '2026-03-17 10:00:00' },
  { id: 2003, dict_type_code: 'ORDER_STATUS', item_code: 'SHIPPED', item_name: '已发货', item_value: 'SHIPPED', sort_no: 80, status_code: 'ENABLED', updated_at: '2026-03-17 10:00:00' },
  { id: 2101, dict_type_code: 'ASSET_STATUS', item_code: 'ACTIVE', item_name: '有效', item_value: 'ACTIVE', sort_no: 100, status_code: 'ENABLED', updated_at: '2026-03-17 09:58:00' },
  { id: 2102, dict_type_code: 'ASSET_STATUS', item_code: 'INVALID', item_name: '失效', item_value: 'INVALID', sort_no: 90, status_code: 'ENABLED', updated_at: '2026-03-17 09:58:00' },
]

const mockSystemConfigs: AdminSystemConfig[] = [
  {
    id: 3001,
    config_key: 'mall.home.banner_autoplay',
    config_name: '首页轮播自动播放',
    config_value: 'true',
    value_type_code: 'BOOLEAN',
    group_code: 'MALL_HOME',
    status_code: 'ENABLED',
    remark: '控制首页 banner 自动轮播',
    updated_at: '2026-03-17 12:00:00',
  },
  {
    id: 3002,
    config_key: 'mall.exchange.default_limit',
    config_name: '默认兑换限购',
    config_value: '1',
    value_type_code: 'NUMBER',
    group_code: 'MALL_EXCHANGE',
    status_code: 'ENABLED',
    remark: '未单独配置时的默认限购数',
    updated_at: '2026-03-17 11:58:00',
  },
  {
    id: 3003,
    config_key: 'mall.customer_service.contact',
    config_name: '客服联系方式',
    config_value: '抖店客服：18888888888',
    value_type_code: 'STRING',
    group_code: 'MALL_SERVICE',
    status_code: 'DISABLED',
    remark: '前台客服联系文案',
    updated_at: '2026-03-16 14:22:00',
  },
]

const mockFiles: AdminUploadedFile[] = [
  {
    id: 4001,
    file_name: 'banner-home-01.png',
    file_url: 'https://picsum.photos/id/20/960/420',
    mime_type: 'image/png',
    file_size_kb: 512,
    uploaded_at: '2026-03-17 08:30:00',
  },
  {
    id: 4002,
    file_name: 'group-qr-01.jpg',
    file_url: 'https://picsum.photos/id/37/300/300',
    mime_type: 'image/jpeg',
    file_size_kb: 182,
    uploaded_at: '2026-03-17 09:12:00',
  },
]

let dictTypeIdSeed = 5000
let dictItemIdSeed = 6000
let configIdSeed = 7000
let fileIdSeed = 8000

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

export function listMockDictTypes(query?: Partial<PageQuery>, keyword = ''): ApiPageData<AdminDictType> {
  const key = keyword.trim().toLowerCase()
  const source = key
    ? mockDictTypes.filter((item) =>
        [item.dict_type_code, item.dict_type_name, item.remark].some((field) => field.toLowerCase().includes(key)),
      )
    : mockDictTypes
  source.sort((a, b) => b.updated_at.localeCompare(a.updated_at))
  return paginate(source, query)
}

export function createMockDictType(payload: Pick<AdminDictType, 'dict_type_code' | 'dict_type_name' | 'remark' | 'status_code'>): AdminDictType {
  const dictType: AdminDictType = {
    id: ++dictTypeIdSeed,
    dict_type_code: payload.dict_type_code,
    dict_type_name: payload.dict_type_name,
    remark: payload.remark,
    status_code: payload.status_code,
    updated_at: nowText(),
  }
  mockDictTypes.unshift(dictType)
  return dictType
}

export function updateMockDictType(typeId: number, payload: Partial<Pick<AdminDictType, 'dict_type_code' | 'dict_type_name' | 'remark' | 'status_code'>>): void {
  const target = mockDictTypes.find((item) => item.id === typeId)
  if (!target) return
  if (payload.dict_type_code !== undefined) target.dict_type_code = payload.dict_type_code
  if (payload.dict_type_name !== undefined) target.dict_type_name = payload.dict_type_name
  if (payload.remark !== undefined) target.remark = payload.remark
  if (payload.status_code !== undefined) target.status_code = payload.status_code
  target.updated_at = nowText()
}

export function deleteMockDictType(typeId: number): void {
  const index = mockDictTypes.findIndex((item) => item.id === typeId)
  if (index < 0) return
  const [type] = mockDictTypes.splice(index, 1)
  for (let i = mockDictItems.length - 1; i >= 0; i -= 1) {
    if (mockDictItems[i].dict_type_code === type.dict_type_code) {
      mockDictItems.splice(i, 1)
    }
  }
}

export function listMockDictItems(typeCode: string): AdminDictItem[] {
  return mockDictItems
    .filter((item) => item.dict_type_code === typeCode)
    .sort((a, b) => b.sort_no - a.sort_no || b.id - a.id)
}

export function createMockDictItem(typeCode: string, payload: Omit<AdminDictItem, 'id' | 'dict_type_code' | 'updated_at'>): AdminDictItem {
  const dictItem: AdminDictItem = {
    id: ++dictItemIdSeed,
    dict_type_code: typeCode,
    item_code: payload.item_code,
    item_name: payload.item_name,
    item_value: payload.item_value,
    sort_no: payload.sort_no,
    status_code: payload.status_code,
    updated_at: nowText(),
  }
  mockDictItems.unshift(dictItem)
  return dictItem
}

export function updateMockDictItem(itemId: number, payload: Partial<Omit<AdminDictItem, 'id' | 'dict_type_code' | 'updated_at'>>): void {
  const target = mockDictItems.find((item) => item.id === itemId)
  if (!target) return
  if (payload.item_code !== undefined) target.item_code = payload.item_code
  if (payload.item_name !== undefined) target.item_name = payload.item_name
  if (payload.item_value !== undefined) target.item_value = payload.item_value
  if (payload.sort_no !== undefined) target.sort_no = payload.sort_no
  if (payload.status_code !== undefined) target.status_code = payload.status_code
  target.updated_at = nowText()
}

export function deleteMockDictItem(itemId: number): void {
  const index = mockDictItems.findIndex((item) => item.id === itemId)
  if (index < 0) return
  mockDictItems.splice(index, 1)
}

export function listMockSystemConfigs(query?: Partial<PageQuery>, keyword = ''): ApiPageData<AdminSystemConfig> {
  const key = keyword.trim().toLowerCase()
  const source = key
    ? mockSystemConfigs.filter((item) =>
        [item.config_key, item.config_name, item.group_code, item.remark].some((field) => field.toLowerCase().includes(key)),
      )
    : mockSystemConfigs
  source.sort((a, b) => b.updated_at.localeCompare(a.updated_at))
  return paginate(source, query)
}

export function createMockSystemConfig(payload: Omit<AdminSystemConfig, 'id' | 'updated_at'>): AdminSystemConfig {
  const config: AdminSystemConfig = {
    ...payload,
    id: ++configIdSeed,
    updated_at: nowText(),
  }
  mockSystemConfigs.unshift(config)
  return config
}

export function updateMockSystemConfig(configId: number, payload: Partial<Omit<AdminSystemConfig, 'id' | 'updated_at'>>): void {
  const target = mockSystemConfigs.find((item) => item.id === configId)
  if (!target) return
  if (payload.config_key !== undefined) target.config_key = payload.config_key
  if (payload.config_name !== undefined) target.config_name = payload.config_name
  if (payload.config_value !== undefined) target.config_value = payload.config_value
  if (payload.value_type_code !== undefined) target.value_type_code = payload.value_type_code
  if (payload.group_code !== undefined) target.group_code = payload.group_code
  if (payload.status_code !== undefined) target.status_code = payload.status_code
  if (payload.remark !== undefined) target.remark = payload.remark
  target.updated_at = nowText()
}

export function deleteMockSystemConfig(configId: number): void {
  const index = mockSystemConfigs.findIndex((item) => item.id === configId)
  if (index < 0) return
  mockSystemConfigs.splice(index, 1)
}

export function listMockFiles(query?: Partial<PageQuery>, keyword = ''): ApiPageData<AdminUploadedFile> {
  const key = keyword.trim().toLowerCase()
  const source = key
    ? mockFiles.filter((item) => [item.file_name, item.file_url, item.mime_type].some((field) => field.toLowerCase().includes(key)))
    : mockFiles
  source.sort((a, b) => b.uploaded_at.localeCompare(a.uploaded_at))
  return paginate(source, query)
}

export function uploadMockFile(payload: Pick<AdminUploadedFile, 'file_name' | 'file_url' | 'mime_type' | 'file_size_kb'>): AdminUploadedFile {
  const file: AdminUploadedFile = {
    id: ++fileIdSeed,
    file_name: payload.file_name,
    file_url: payload.file_url,
    mime_type: payload.mime_type,
    file_size_kb: payload.file_size_kb,
    uploaded_at: nowText(),
  }
  mockFiles.unshift(file)
  return file
}

export function getMockFile(fileId: number): AdminUploadedFile | null {
  const file = mockFiles.find((item) => item.id === fileId)
  return file ? { ...file } : null
}

export function deleteMockFile(fileId: number): void {
  const index = mockFiles.findIndex((item) => item.id === fileId)
  if (index < 0) return
  mockFiles.splice(index, 1)
}
