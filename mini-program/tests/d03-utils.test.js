const test = require('node:test')
const assert = require('node:assert/strict')

const { assertApiSuccess, ApiBusinessError } = require('../utils/api-response')
const { maskPhone } = require('../utils/format')
const { shouldAttachRequestId } = require('../utils/request-id')
const { compilePath } = require('../utils/path')
const { APP_ENDPOINTS } = require('../services/endpoints')

test('assertApiSuccess: code=0 返回 data', () => {
  const data = assertApiSuccess({ code: 0, message: 'ok', data: { user_id: 1 } })
  assert.deepEqual(data, { user_id: 1 })
})

test('assertApiSuccess: code!=0 抛业务异常', () => {
  assert.throws(
    () => assertApiSuccess({ code: 4001, message: '碎片不足', data: null }),
    (error) => error instanceof ApiBusinessError && error.code === 4001,
  )
})

test('maskPhone 脱敏', () => {
  assert.equal(maskPhone('13812345678'), '138****5678')
})

test('写操作需要 X-Request-Id', () => {
  assert.equal(shouldAttachRequestId('GET'), false)
  assert.equal(shouldAttachRequestId('POST'), true)
  assert.equal(shouldAttachRequestId('PUT'), true)
  assert.equal(shouldAttachRequestId('DELETE'), true)
})

test('compilePath 可替换参数', () => {
  assert.equal(
    compilePath('/api/v1/app/categories/{categoryId}/products', { categoryId: 18 }),
    '/api/v1/app/categories/18/products',
  )
})

test('D03 + D07 app endpoints 覆盖 38 条且路径前缀正确', () => {
  const values = Object.values(APP_ENDPOINTS)
  assert.equal(values.length, 38)
  assert.equal(values.every((item) => item.path.startsWith('/api/v1/app/')), true)

  assert.equal(APP_ENDPOINTS.HOME_RECOMMENDS.path, '/api/v1/app/home/recommends')
  assert.equal(APP_ENDPOINTS.GET_CATEGORY_PRODUCTS.path, '/api/v1/app/categories/{categoryId}/products')
  assert.equal(APP_ENDPOINTS.GET_PRODUCT_DETAIL.path, '/api/v1/app/products/{productId}')
  assert.equal(APP_ENDPOINTS.GET_ADDRESSES.path, '/api/v1/app/addresses')
  assert.equal(APP_ENDPOINTS.SET_DEFAULT_ADDRESS.path, '/api/v1/app/addresses/{addressId}/default')
  assert.equal(APP_ENDPOINTS.SUBMIT_EXCHANGE_ORDER.path, '/api/v1/app/exchanges/orders')
  assert.equal(APP_ENDPOINTS.GET_ORDERS.path, '/api/v1/app/orders')
  assert.equal(APP_ENDPOINTS.GET_ORDER_DETAIL.path, '/api/v1/app/orders/{orderId}')
  assert.equal(APP_ENDPOINTS.CANCEL_ORDER.path, '/api/v1/app/orders/{orderId}/cancel')
  assert.equal(APP_ENDPOINTS.GET_ORDER_STATUS_COUNTS.path, '/api/v1/app/orders/status-counts')
  assert.equal(APP_ENDPOINTS.GET_POINT_LEDGER.path, '/api/v1/app/points/ledger')
  assert.equal(APP_ENDPOINTS.GET_BACKPACK_ASSETS.path, '/api/v1/app/backpack/assets')
  assert.equal(APP_ENDPOINTS.USE_BACKPACK_ASSET.path, '/api/v1/app/backpack/assets/{assetId}/use')
  assert.equal(APP_ENDPOINTS.GET_GROUP_RESOURCES.path, '/api/v1/app/group-resources')
  assert.equal(APP_ENDPOINTS.GET_CUSTOMER_SERVICE_CONTACT.path, '/api/v1/app/customer-service/contact')
  assert.equal(APP_ENDPOINTS.GET_DICT_ITEMS.path, '/api/v1/app/dict/{dictTypeCode}/items')
  assert.equal(APP_ENDPOINTS.GET_PUBLIC_SYSTEM_CONFIGS.path, '/api/v1/app/system-configs/public')
})
