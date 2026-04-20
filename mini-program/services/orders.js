const { request } = require('../utils/request')
const { compilePath } = require('../utils/path')
const { APP_ENDPOINTS } = require('./endpoints')

function getOrders(params = {}) {
  const endpoint = APP_ENDPOINTS.GET_ORDERS
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: params,
    requireAuth: true,
  })
}

function getOrderDetail(orderId) {
  const endpoint = APP_ENDPOINTS.GET_ORDER_DETAIL
  return request({
    url: compilePath(endpoint.path, { orderId }),
    method: endpoint.method,
    requireAuth: true,
  })
}

function getOrderFlows(orderId) {
  const endpoint = APP_ENDPOINTS.GET_ORDER_FLOWS
  return request({
    url: compilePath(endpoint.path, { orderId }),
    method: endpoint.method,
    requireAuth: true,
  })
}

function getOrderDelivery(orderId) {
  const endpoint = APP_ENDPOINTS.GET_ORDER_DELIVERY
  return request({
    url: compilePath(endpoint.path, { orderId }),
    method: endpoint.method,
    requireAuth: true,
  })
}

function getOrderLogisticsTraces(orderId) {
  const endpoint = APP_ENDPOINTS.GET_ORDER_LOGISTICS_TRACES
  return request({
    url: compilePath(endpoint.path, { orderId }),
    method: endpoint.method,
    requireAuth: true,
  })
}

function cancelOrder(orderId, payload = {}) {
  const endpoint = APP_ENDPOINTS.CANCEL_ORDER
  return request({
    url: compilePath(endpoint.path, { orderId }),
    method: endpoint.method,
    data: payload,
    requireAuth: true,
  })
}

function decideRejectedOrder(orderId, payload = {}) {
  const endpoint = APP_ENDPOINTS.DECIDE_REJECTED_ORDER
  return request({
    url: compilePath(endpoint.path, { orderId }),
    method: endpoint.method,
    data: payload,
    requireAuth: true,
  })
}

function getOrderStatusCounts() {
  const endpoint = APP_ENDPOINTS.GET_ORDER_STATUS_COUNTS
  return request({
    url: endpoint.path,
    method: endpoint.method,
    requireAuth: true,
  })
}

module.exports = {
  getOrders,
  getOrderDetail,
  getOrderFlows,
  getOrderDelivery,
  getOrderLogisticsTraces,
  cancelOrder,
  decideRejectedOrder,
  getOrderStatusCounts,
}
