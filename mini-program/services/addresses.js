const { request } = require('../utils/request')
const { compilePath } = require('../utils/path')
const { APP_ENDPOINTS } = require('./endpoints')

function getAddresses(params = {}) {
  const endpoint = APP_ENDPOINTS.GET_ADDRESSES
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: params,
    requireAuth: true,
  })
}

function createAddress(payload) {
  const endpoint = APP_ENDPOINTS.CREATE_ADDRESS
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: payload,
    requireAuth: true,
  })
}

function getAddressDetail(addressId) {
  const endpoint = APP_ENDPOINTS.GET_ADDRESS_DETAIL
  return request({
    url: compilePath(endpoint.path, { addressId }),
    method: endpoint.method,
    requireAuth: true,
  })
}

function updateAddress(addressId, payload) {
  const endpoint = APP_ENDPOINTS.UPDATE_ADDRESS
  return request({
    url: compilePath(endpoint.path, { addressId }),
    method: endpoint.method,
    data: payload,
    requireAuth: true,
  })
}

function deleteAddress(addressId) {
  const endpoint = APP_ENDPOINTS.DELETE_ADDRESS
  return request({
    url: compilePath(endpoint.path, { addressId }),
    method: endpoint.method,
    requireAuth: true,
  })
}

function setDefaultAddress(addressId) {
  const endpoint = APP_ENDPOINTS.SET_DEFAULT_ADDRESS
  return request({
    url: compilePath(endpoint.path, { addressId }),
    method: endpoint.method,
    requireAuth: true,
  })
}

module.exports = {
  getAddresses,
  createAddress,
  getAddressDetail,
  updateAddress,
  deleteAddress,
  setDefaultAddress,
}
