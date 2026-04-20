import { describe, expect, it } from 'vitest'
import { compilePath } from '../src/api/path'
import { normalizePageQuery } from '../src/api/pagination'
import { shouldAttachRequestId } from '../src/api/request-id'

describe('api utils', () => {
  it('compilePath 可以替换路径参数', () => {
    expect(compilePath('/api/v1/admin/users/{userId}/orders/{orderId}', { userId: 8, orderId: 22 })).toBe(
      '/api/v1/admin/users/8/orders/22',
    )
  })

  it('缺少路径参数时抛错', () => {
    expect(() => compilePath('/api/v1/admin/users/{userId}', {})).toThrowError('缺少路径参数: userId')
  })

  it('分页参数会自动归一化', () => {
    expect(normalizePageQuery({ pageNo: 0, pageSize: -2 })).toEqual({ pageNo: 1, pageSize: 20 })
    expect(normalizePageQuery({ pageNo: 3, pageSize: 50 })).toEqual({ pageNo: 3, pageSize: 50 })
  })

  it('写操作自动启用 X-Request-Id', () => {
    expect(shouldAttachRequestId('GET')).toBe(false)
    expect(shouldAttachRequestId('POST')).toBe(true)
    expect(shouldAttachRequestId('PUT')).toBe(true)
    expect(shouldAttachRequestId('DELETE')).toBe(true)
  })
})
