const {
  getAddresses,
  createAddress,
  deleteAddress,
  setDefaultAddress,
} = require('../../services/addresses')

function parseList(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.list)) return payload.list
  return []
}

function normalizeAddress(item = {}) {
  return {
    id: item.id || item.address_id || '',
    receiver_name: item.receiver_name || '',
    receiver_phone: item.receiver_phone || '',
    province_name: item.province_name || '',
    city_name: item.city_name || '',
    district_name: item.district_name || '',
    detail_address: item.detail_address || '',
    is_default: Number(item.is_default || 0) === 1,
  }
}

function chooseWechatAddress() {
  return new Promise((resolve, reject) => {
    wx.chooseAddress({
      success: resolve,
      fail: reject,
    })
  })
}

Page({
  data: {
    loading: false,
    list: [],
    choose_mode: false,
    selected_address_id: '',
  },

  onLoad(options) {
    const chooseMode = String(options?.mode || '') === 'select'
    const selectedAddressId = options?.selectedAddressId ? decodeURIComponent(options.selectedAddressId) : ''
    this.setData({
      choose_mode: chooseMode,
      selected_address_id: selectedAddressId,
    })
    if (chooseMode) {
      wx.setNavigationBarTitle({ title: '选择收货地址' })
    }
  },

  onShow() {
    this.loadList()
  },

  async loadList() {
    this.setData({ loading: true })
    try {
      const data = await getAddresses({ pageNo: 1, pageSize: 100 })
      const list = parseList(data).map(normalizeAddress)
      this.setData({ list })
    } catch (error) {
      wx.showToast({ title: error.message || '地址加载失败', icon: 'none' })
      this.setData({ list: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  onTapAdd() {
    wx.navigateTo({ url: '/pages/address-edit/index' })
  },

  onTapEdit(event) {
    const { id } = event.currentTarget.dataset
    if (!id) return
    wx.navigateTo({ url: `/pages/address-edit/index?addressId=${id}` })
  },

  async onTapWechatAdd() {
    try {
      const wxAddress = await chooseWechatAddress()
      const payload = {
        receiver_name: wxAddress.userName || '',
        receiver_phone: wxAddress.telNumber || '',
        province_name: wxAddress.provinceName || '',
        city_name: wxAddress.cityName || '',
        district_name: wxAddress.countyName || '',
        detail_address: wxAddress.detailInfo || '',
        country_code: 'CN',
        is_default: this.data.list.length ? 0 : 1,
        status_code: 'ACTIVE',
      }
      await createAddress(payload)
      wx.showToast({ title: '已导入微信地址', icon: 'success' })
      this.loadList()
    } catch (error) {
      const msg = String(error?.errMsg || error?.message || '')
      if (msg.includes('cancel')) return
      if (msg.includes('auth deny') || msg.includes('authorize')) {
        wx.showModal({
          title: '需要地址权限',
          content: '请在设置中开启通讯地址权限后重试',
          confirmText: '去设置',
          success: ({ confirm }) => {
            if (confirm) wx.openSetting()
          },
        })
        return
      }
      wx.showToast({ title: '获取微信地址失败', icon: 'none' })
    }
  },

  async onTapSetDefault(event) {
    const { id } = event.currentTarget.dataset
    if (!id) return
    try {
      await setDefaultAddress(id)
      wx.showToast({ title: '已设为默认', icon: 'success' })
      this.loadList()
    } catch (error) {
      wx.showToast({ title: error.message || '设置失败', icon: 'none' })
    }
  },

  onTapDelete(event) {
    const { id } = event.currentTarget.dataset
    if (!id) return

    wx.showModal({
      title: '删除地址',
      content: '确认删除这条地址吗？',
      success: async ({ confirm }) => {
        if (!confirm) return
        try {
          await deleteAddress(id)
          wx.showToast({ title: '删除成功', icon: 'success' })
          this.loadList()
        } catch (error) {
          wx.showToast({ title: error.message || '删除失败', icon: 'none' })
        }
      },
    })
  },

  onTapChoose(event) {
    if (!this.data.choose_mode) return
    const { id } = event.currentTarget.dataset
    if (!id) return
    const target = (this.data.list || []).find((item) => String(item.id) === String(id))
    if (!target) return

    this.setData({ selected_address_id: target.id })

    const eventChannel = this.getOpenerEventChannel?.()
    if (eventChannel && eventChannel.emit) {
      eventChannel.emit('addressSelected', target)
    }

    wx.navigateBack()
  },

  noop() {},
})
