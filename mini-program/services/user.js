const { APP_ENDPOINTS } = require('./endpoints')
const { request } = require('../utils/request')

function getMe() {
  const endpoint = APP_ENDPOINTS.GET_ME
  return request({
    url: endpoint.path,
    method: endpoint.method,
  })
}

function updateMe(payload) {
  const endpoint = APP_ENDPOINTS.UPDATE_ME
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: payload,
  })
}

function getMeSummary() {
  const endpoint = APP_ENDPOINTS.GET_ME_SUMMARY
  return request({
    url: endpoint.path,
    method: endpoint.method,
  })
}

module.exports = {
  getMe,
  updateMe,
  getMeSummary,
}
