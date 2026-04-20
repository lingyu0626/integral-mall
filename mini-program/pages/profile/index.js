const { getMe, getMeSummary } = require('../../services/user')
const { bindPhone } = require('../../services/auth')
const { getPublicSystemConfigs } = require('../../services/community')

const BASE_MENU_LIST = [
  { key: 'address', title: '收货地址', icon: '../../assets/profile/menu-address.png' },
  { key: 'points', title: '碎片收支', icon: '../../assets/profile/menu-ledger.png' },
]

function formatBackpackId(value) {
  const id = Number(value)
  if (!Number.isFinite(id) || id <= 0) return '01000'
  return String(Math.floor(id)).padStart(5, '0')
}

function parseList(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.list)) return payload.list
  return []
}

function parseSwitchValue(value) {
  const raw = String(value == null ? '' : value).trim().toLowerCase()
  return ['1', 'true', 'yes', 'on', 'enabled'].includes(raw)
}

Page({
  data: {
    loading: false,
    login_ing: false,
    binding_phone: false,
    user: null,
    summary: null,
    brand_avatar_url: '../../assets/profile/avatar-brand.png',
    menu_list: BASE_MENU_LIST,
    backpack_id_text: '01000',
    group_entry_enabled: false,
    group_entry_text: '加入群聊',
    group_entry_qrcode_url: '',
    group_desc_visible: false,
    group_desc_text: '',
    group_qr_popup_visible: false,
    group_qr_popup_title: '加入群聊',
    group_qr_popup_url: '',
  },

  async onShow() {
    this.loadProfile()
    this.loadProfileMenuConfigs()
  },

  async loadProfile() {
    this.setData({ loading: true })
    try {
      const [user, summary] = await Promise.all([getMe(), getMeSummary()])
      this.setData({
        user,
        summary,
        backpack_id_text: formatBackpackId(user?.id),
      })
    } catch (_) {
      this.setData({
        user: null,
        summary: null,
        backpack_id_text: '01000',
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadProfileMenuConfigs() {
    try {
      const list = parseList(await getPublicSystemConfigs({ pageNo: 1, pageSize: 200 }))
      let groupEnabled = false
      let groupText = '加入群聊'
      let groupQrUrl = ''
      let groupDescEnabled = false
      let groupDescText = ''
      list.forEach((item = {}) => {
        const key = String(item.config_key || '').trim()
        const value = String(item.config_value == null ? '' : item.config_value).trim()
        if (!key) return
        if (key === 'mall.profile.group_entry_enabled') {
          groupEnabled = parseSwitchValue(value)
        } else if (key === 'mall.profile.group_entry_text' && value) {
          groupText = value
        } else if (key === 'mall.profile.group_entry_qrcode_url' && value) {
          groupQrUrl = value
        } else if (key === 'mall.profile.group_entry_desc_enabled') {
          groupDescEnabled = parseSwitchValue(value)
        } else if (key === 'mall.profile.group_entry_desc_text' && value) {
          groupDescText = value
        }
      })

      const menu = BASE_MENU_LIST.slice()
      if (groupEnabled) {
        menu.push({
          key: 'group',
          title: groupText || '加入群聊',
          icon: '../../assets/profile/menu-group.png',
          qrcode_url: groupQrUrl,
        })
      }
      this.setData({
        menu_list: menu,
        group_entry_enabled: groupEnabled,
        group_entry_text: groupText || '加入群聊',
        group_entry_qrcode_url: groupQrUrl || '',
        group_desc_visible: !!(groupDescEnabled && groupDescText),
        group_desc_text: groupDescText || '',
      })
    } catch (_) {
      this.setData({
        menu_list: BASE_MENU_LIST,
        group_entry_enabled: false,
        group_entry_text: '加入群聊',
        group_entry_qrcode_url: '',
        group_desc_visible: false,
        group_desc_text: '',
      })
    }
  },

  async onTapWxLogin() {
    if (this.data.login_ing) return
    this.setData({ login_ing: true })
    try {
      const app = typeof getApp === 'function' ? getApp() : null
      if (!app || typeof app.silentLogin !== 'function') {
        wx.showToast({ title: '登录能力不可用', icon: 'none' })
        return
      }
      const ok = await app.silentLogin(true)
      if (!ok) {
        wx.showToast({ title: '登录失败，请重试', icon: 'none' })
        return
      }
      await this.loadProfile()
      wx.showToast({ title: '登录成功', icon: 'success' })
    } catch (_) {
      wx.showToast({ title: '登录失败，请重试', icon: 'none' })
    } finally {
      this.setData({ login_ing: false })
    }
  },

  async onGetPhoneNumber(event) {
    const detail = event?.detail || {}
    if (detail.errMsg && !String(detail.errMsg).includes(':ok')) {
      wx.showToast({ title: '你已取消手机号授权', icon: 'none' })
      return
    }
    const phoneCode = detail.code || ''
    if (!phoneCode) {
      wx.showToast({ title: '手机号授权失败，请重试', icon: 'none' })
      return
    }

    this.setData({ binding_phone: true })
    try {
      await bindPhone({ phone_code: phoneCode })
      wx.showToast({ title: '手机号绑定成功', icon: 'success' })
      await this.loadProfile()
    } catch (error) {
      wx.showToast({ title: error.message || '手机号绑定失败', icon: 'none' })
    } finally {
      this.setData({ binding_phone: false })
    }
  },

  onTapOrderMore() {
    wx.navigateTo({ url: '/pages/orders/index' })
  },

  onTapOrderStatus(event) {
    const { status } = event.currentTarget.dataset
    if (!status) {
      this.onTapOrderMore()
      return
    }
    wx.navigateTo({ url: `/pages/orders/index?status=${status}` })
  },

  onTapMenu(event) {
    const { key, title, qrcodeUrl } = event.currentTarget.dataset
    if (key === 'address') {
      wx.navigateTo({ url: '/pages/address/index' })
      return
    }
    if (key === 'points') {
      wx.navigateTo({ url: '/pages/points/index' })
      return
    }
    if (key === 'group') {
      if (!qrcodeUrl) {
        wx.showToast({ title: '管理员暂未配置群二维码', icon: 'none' })
        return
      }
      this.setData({
        group_qr_popup_visible: true,
        group_qr_popup_title: String(title || '加入群聊'),
        group_qr_popup_url: String(qrcodeUrl || ''),
      })
      return
    }
    if (key === 'service') {
      wx.navigateTo({ url: '/pages/community/index' })
    }
  },

  onCloseGroupQrPopup() {
    this.setData({ group_qr_popup_visible: false })
  },

  onPreviewGroupQr() {
    const url = String(this.data.group_qr_popup_url || '').trim()
    if (!url) return
    wx.previewImage({
      urls: [url],
      current: url,
    })
  },

  noop() {},
})
