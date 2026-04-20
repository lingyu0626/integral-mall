const { request } = require('../utils/request')
const { compilePath } = require('../utils/path')
const { APP_ENDPOINTS } = require('./endpoints')

function getBackpackAssets(params = {}) {
  const endpoint = APP_ENDPOINTS.GET_BACKPACK_ASSETS
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: params,
    requireAuth: true,
  })
}

function getBackpackAssetDetail(assetId) {
  const endpoint = APP_ENDPOINTS.GET_BACKPACK_ASSET_DETAIL
  return request({
    url: compilePath(endpoint.path, { assetId }),
    method: endpoint.method,
    requireAuth: true,
  })
}

function getBackpackAssetFlows(assetId, params = {}) {
  const endpoint = APP_ENDPOINTS.GET_BACKPACK_ASSET_FLOWS
  return request({
    url: compilePath(endpoint.path, { assetId }),
    method: endpoint.method,
    data: params,
    requireAuth: true,
  })
}

function useBackpackAsset(assetId, payload = {}) {
  const endpoint = APP_ENDPOINTS.USE_BACKPACK_ASSET
  return request({
    url: compilePath(endpoint.path, { assetId }),
    method: endpoint.method,
    data: payload,
    requireAuth: true,
  })
}

module.exports = {
  getBackpackAssets,
  getBackpackAssetDetail,
  getBackpackAssetFlows,
  useBackpackAsset,
}
