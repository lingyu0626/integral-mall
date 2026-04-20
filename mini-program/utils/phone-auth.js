const { getMe } = require('../services/user')

function hasPhone(user = {}) {
  return !!String(user.phone || user.phone_masked || '').trim()
}

async function ensurePhoneAuthorized() {
  try {
    const app = typeof getApp === 'function' ? getApp() : null
    const loginOk =
      app && typeof app.silentLogin === 'function'
        ? await app.silentLogin(true)
        : false
    if (!loginOk) return false
    const user = await getMe()
    return hasPhone(user)
  } catch (_) {
    return false
  }
}

module.exports = {
  ensurePhoneAuthorized,
}

