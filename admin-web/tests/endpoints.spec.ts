import { describe, expect, it } from 'vitest'
import {
  ADMIN_ENDPOINT_COUNT,
  API_ENDPOINT_COUNT,
  API_ENDPOINT_ENTRIES,
  API_ENDPOINTS,
  APP_ENDPOINT_COUNT,
} from '../src/api/endpoints'

describe('endpoints constants', () => {
  it('接口总数应与规范一致（156）', () => {
    expect(API_ENDPOINT_COUNT).toBe(156)
    expect(APP_ENDPOINT_COUNT + ADMIN_ENDPOINT_COUNT).toBe(156)
  })

  it('每个接口都以 /api/v1 开头', () => {
    expect(API_ENDPOINT_ENTRIES.every(([, item]) => item.path.startsWith('/api/v1/'))).toBe(true)
  })

  it('包含关键幂等接口常量', () => {
    expect(API_ENDPOINTS.POST_APP_EXCHANGES_ORDERS.path).toBe('/api/v1/app/exchanges/orders')
    expect(API_ENDPOINTS.POST_ADMIN_USERS_BY_USER_ID_POINTS_ADJUST.path).toBe('/api/v1/admin/users/{userId}/points/adjust')
  })
})
