const { wxLogin } = require('./services/auth')
const { getToken } = require('./utils/auth')

function getWxCode() {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res?.code) {
          resolve(res.code)
          return
        }
        reject(new Error('微信登录 code 获取失败'))
      },
      fail: () => reject(new Error('微信登录失败')),
    })
  })
}

App({
  globalData: {
    project_name: '碎片商城',
    version: 'd03',
  },
  _silentLoginPromise: null,

  onLaunch() {
    this.silentLogin()
  },

  async silentLogin(force = false) {
    if (!force && getToken()) return true
    if (this._silentLoginPromise) return this._silentLoginPromise

    this._silentLoginPromise = (async () => {
      try {
        const code = await getWxCode()
        await wxLogin(code)
        return true
      } catch (error) {
        // 静默登录失败不阻塞页面渲染
        console.warn('[app] silentLogin failed:', error?.message || error)
        return false
      } finally {
        this._silentLoginPromise = null
      }
    })()

    return this._silentLoginPromise
  },
})
