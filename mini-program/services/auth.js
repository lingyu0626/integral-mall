const { APP_ENDPOINTS } = require('./endpoints')
const { request } = require('../utils/request')
const { setToken, clearToken } = require('../utils/auth')

async function wxLogin(code) {
  const endpoint = APP_ENDPOINTS.WX_LOGIN
  const data = await request({
    url: endpoint.path,
    method: endpoint.method,
    data: { code },
    requireAuth: false,
  })

  if (data && data.access_token) {
    setToken(data.access_token)
  }

  return data
}

function bindPhone(payload) {
  const endpoint = APP_ENDPOINTS.BIND_PHONE
  return request({
    url: endpoint.path,
    method: endpoint.method,
    data: payload,
  })
}

async function refreshToken(refresh_token) {
  const endpoint = APP_ENDPOINTS.REFRESH_TOKEN
  const data = await request({
    url: endpoint.path,
    method: endpoint.method,
    data: { refresh_token },
    requireAuth: false,
  })

  if (data && data.access_token) {
    setToken(data.access_token)
  }

  return data
}

async function logout() {
  const endpoint = APP_ENDPOINTS.LOGOUT
  await request({
    url: endpoint.path,
    method: endpoint.method,
  })
  clearToken()
}

module.exports = {
  wxLogin,
  bindPhone,
  refreshToken,
  logout,
}
