const USER_TOKEN_KEY = 'pm_user_token'

function getToken() {
  return wx.getStorageSync(USER_TOKEN_KEY) || ''
}

function setToken(token) {
  wx.setStorageSync(USER_TOKEN_KEY, token)
}

function clearToken() {
  wx.removeStorageSync(USER_TOKEN_KEY)
}

module.exports = {
  USER_TOKEN_KEY,
  getToken,
  setToken,
  clearToken,
}
