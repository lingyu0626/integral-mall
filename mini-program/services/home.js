const { request } = require('../utils/request')
const { APP_ENDPOINTS } = require('./endpoints')

function getHomeRecommends(params = {}) {
  const endpoint = APP_ENDPOINTS.HOME_RECOMMENDS
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: params,
    requireAuth: false,
  })
}

module.exports = {
  getHomeRecommends,
}
