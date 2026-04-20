const APP_ENDPOINTS = {
  // D03 auth & user
  WX_LOGIN: { method: 'POST', path: '/api/v1/app/auth/wx-login' },
  BIND_PHONE: { method: 'POST', path: '/api/v1/app/auth/bind-phone' },
  REFRESH_TOKEN: { method: 'POST', path: '/api/v1/app/auth/refresh-token' },
  LOGOUT: { method: 'POST', path: '/api/v1/app/auth/logout' },
  GET_ME: { method: 'GET', path: '/api/v1/app/users/me' },
  UPDATE_ME: { method: 'PUT', path: '/api/v1/app/users/me' },
  GET_ME_SUMMARY: { method: 'GET', path: '/api/v1/app/users/me/summary' },

  // D04 home/category/product
  HOME_RECOMMENDS: { method: 'GET', path: '/api/v1/app/home/recommends' },
  GET_CATEGORIES: { method: 'GET', path: '/api/v1/app/categories' },
  GET_CATEGORY_PRODUCTS: { method: 'GET', path: '/api/v1/app/categories/{categoryId}/products' },
  SEARCH_PRODUCTS: { method: 'GET', path: '/api/v1/app/products' },
  GET_PRODUCT_DETAIL: { method: 'GET', path: '/api/v1/app/products/{productId}' },
  GET_EXCHANGE_PREVIEW: { method: 'GET', path: '/api/v1/app/products/{productId}/exchange-preview' },

  // D05 addresses/exchange
  GET_ADDRESSES: { method: 'GET', path: '/api/v1/app/addresses' },
  CREATE_ADDRESS: { method: 'POST', path: '/api/v1/app/addresses' },
  GET_ADDRESS_DETAIL: { method: 'GET', path: '/api/v1/app/addresses/{addressId}' },
  UPDATE_ADDRESS: { method: 'PUT', path: '/api/v1/app/addresses/{addressId}' },
  DELETE_ADDRESS: { method: 'DELETE', path: '/api/v1/app/addresses/{addressId}' },
  SET_DEFAULT_ADDRESS: { method: 'PUT', path: '/api/v1/app/addresses/{addressId}/default' },
  SUBMIT_EXCHANGE_ORDER: { method: 'POST', path: '/api/v1/app/exchanges/orders' },

  // D06 orders
  GET_ORDERS: { method: 'GET', path: '/api/v1/app/orders' },
  GET_ORDER_DETAIL: { method: 'GET', path: '/api/v1/app/orders/{orderId}' },
  GET_ORDER_FLOWS: { method: 'GET', path: '/api/v1/app/orders/{orderId}/flows' },
  GET_ORDER_DELIVERY: { method: 'GET', path: '/api/v1/app/orders/{orderId}/delivery' },
  GET_ORDER_LOGISTICS_TRACES: { method: 'GET', path: '/api/v1/app/orders/{orderId}/logistics-traces' },
  CANCEL_ORDER: { method: 'POST', path: '/api/v1/app/orders/{orderId}/cancel' },
  DECIDE_REJECTED_ORDER: { method: 'POST', path: '/api/v1/app/orders/{orderId}/reject-decision' },
  GET_ORDER_STATUS_COUNTS: { method: 'GET', path: '/api/v1/app/orders/status-counts' },

  // D07 points/backpack/group/service/dict/config
  GET_POINT_ACCOUNT: { method: 'GET', path: '/api/v1/app/points/account' },
  GET_POINT_LEDGER: { method: 'GET', path: '/api/v1/app/points/ledger' },
  GET_POINT_LEDGER_DETAIL: { method: 'GET', path: '/api/v1/app/points/ledger/{ledgerId}' },
  GET_BACKPACK_ASSETS: { method: 'GET', path: '/api/v1/app/backpack/assets' },
  GET_BACKPACK_ASSET_DETAIL: { method: 'GET', path: '/api/v1/app/backpack/assets/{assetId}' },
  GET_BACKPACK_ASSET_FLOWS: { method: 'GET', path: '/api/v1/app/backpack/assets/{assetId}/flows' },
  USE_BACKPACK_ASSET: { method: 'POST', path: '/api/v1/app/backpack/assets/{assetId}/use' },
  GET_GROUP_RESOURCES: { method: 'GET', path: '/api/v1/app/group-resources' },
  GET_GROUP_RESOURCE_DETAIL: { method: 'GET', path: '/api/v1/app/group-resources/{resourceId}' },
  GET_CUSTOMER_SERVICE_CONTACT: { method: 'GET', path: '/api/v1/app/customer-service/contact' },
  GET_DICT_ITEMS: { method: 'GET', path: '/api/v1/app/dict/{dictTypeCode}/items' },
  GET_PUBLIC_SYSTEM_CONFIGS: { method: 'GET', path: '/api/v1/app/system-configs/public' },
  CREATE_WISH_DEMAND: { method: 'POST', path: '/api/v1/app/wish-demands' },
  GET_WISH_DEMANDS: { method: 'GET', path: '/api/v1/app/wish-demands' },
}

module.exports = {
  APP_ENDPOINTS,
}
