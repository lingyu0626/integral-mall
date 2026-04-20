const { request } = require('../utils/request')
const { APP_ENDPOINTS } = require('./endpoints')

function submitExchangeOrder(payload) {
  const endpoint = APP_ENDPOINTS.SUBMIT_EXCHANGE_ORDER
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: payload,
    requireAuth: true,
  })
}

module.exports = {
  submitExchangeOrder,
}
