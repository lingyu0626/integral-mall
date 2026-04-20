const { request } = require('../utils/request')
const { compilePath } = require('../utils/path')
const { APP_ENDPOINTS } = require('./endpoints')

function searchProducts(params = {}) {
  const endpoint = APP_ENDPOINTS.SEARCH_PRODUCTS
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: params,
    requireAuth: false,
  })
}

function getProductDetail(productId) {
  const endpoint = APP_ENDPOINTS.GET_PRODUCT_DETAIL
  return request({
    url: compilePath(endpoint.path, { productId }),
    method: endpoint.method,
    requireAuth: false,
  })
}

function getExchangePreview(productId, params = {}) {
  const endpoint = APP_ENDPOINTS.GET_EXCHANGE_PREVIEW
  const data = {}
  if (params && params.sku_id) {
    data.sku_id = params.sku_id
  }
  return request({
    url: compilePath(endpoint.path, { productId }),
    method: endpoint.method,
    data,
    requireAuth: false,
  })
}

module.exports = {
  searchProducts,
  getProductDetail,
  getExchangePreview,
}
