const { request } = require('../utils/request')
const { compilePath } = require('../utils/path')
const { APP_ENDPOINTS } = require('./endpoints')

function getPointAccount() {
  const endpoint = APP_ENDPOINTS.GET_POINT_ACCOUNT
  return request({
    url: endpoint.path,
    method: endpoint.method,
    requireAuth: true,
  })
}

function getPointLedger(params = {}) {
  const endpoint = APP_ENDPOINTS.GET_POINT_LEDGER
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: params,
    requireAuth: true,
  })
}

function getPointLedgerDetail(ledgerId) {
  const endpoint = APP_ENDPOINTS.GET_POINT_LEDGER_DETAIL
  return request({
    url: compilePath(endpoint.path, { ledgerId }),
    method: endpoint.method,
    requireAuth: true,
  })
}

module.exports = {
  getPointAccount,
  getPointLedger,
  getPointLedgerDetail,
}
