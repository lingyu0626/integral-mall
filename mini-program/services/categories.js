const { request } = require('../utils/request')
const { compilePath } = require('../utils/path')
const { APP_ENDPOINTS } = require('./endpoints')

function getCategories() {
  const endpoint = APP_ENDPOINTS.GET_CATEGORIES
  return request({
    url: endpoint.path,
    method: endpoint.method,
    requireAuth: false,
  })
}

function getCategoryProducts(categoryId, params = {}) {
  const endpoint = APP_ENDPOINTS.GET_CATEGORY_PRODUCTS
  return request({
    url: compilePath(endpoint.path, { categoryId }),
    method: endpoint.method,
    data: params,
    requireAuth: false,
  })
}

module.exports = {
  getCategories,
  getCategoryProducts,
}
