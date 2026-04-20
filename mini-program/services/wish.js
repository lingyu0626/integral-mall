const { request } = require('../utils/request')
const { APP_ENDPOINTS } = require('./endpoints')

function createWishDemand(payload = {}) {
  const endpoint = APP_ENDPOINTS.CREATE_WISH_DEMAND
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: payload,
    requireAuth: true,
  })
}

function getWishDemands(params = {}) {
  const endpoint = APP_ENDPOINTS.GET_WISH_DEMANDS
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: params,
    requireAuth: true,
  })
}

module.exports = {
  createWishDemand,
  getWishDemands,
}
