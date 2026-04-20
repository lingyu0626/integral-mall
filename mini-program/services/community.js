const { request } = require('../utils/request')
const { compilePath } = require('../utils/path')
const { APP_ENDPOINTS } = require('./endpoints')

function getGroupResources(params = {}) {
  const endpoint = APP_ENDPOINTS.GET_GROUP_RESOURCES
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: params,
    requireAuth: false,
  })
}

function getGroupResourceDetail(resourceId) {
  const endpoint = APP_ENDPOINTS.GET_GROUP_RESOURCE_DETAIL
  return request({
    url: compilePath(endpoint.path, { resourceId }),
    method: endpoint.method,
    requireAuth: false,
  })
}

function getCustomerServiceContact() {
  const endpoint = APP_ENDPOINTS.GET_CUSTOMER_SERVICE_CONTACT
  return request({
    url: endpoint.path,
    method: endpoint.method,
    requireAuth: false,
  })
}

function getDictItems(dictTypeCode) {
  const endpoint = APP_ENDPOINTS.GET_DICT_ITEMS
  return request({
    url: compilePath(endpoint.path, { dictTypeCode }),
    method: endpoint.method,
    requireAuth: false,
  })
}

function getPublicSystemConfigs(params = {}) {
  const endpoint = APP_ENDPOINTS.GET_PUBLIC_SYSTEM_CONFIGS
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: params,
    requireAuth: false,
  })
}

module.exports = {
  getGroupResources,
  getGroupResourceDetail,
  getCustomerServiceContact,
  getDictItems,
  getPublicSystemConfigs,
}
