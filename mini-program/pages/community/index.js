const {
  getGroupResources,
  getGroupResourceDetail,
  getCustomerServiceContact,
  getDictItems,
  getPublicSystemConfigs,
} = require('../../services/community')

function parseList(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.list)) return payload.list
  return []
}

function normalizeResource(item = {}) {
  return {
    id: item.id || item.resource_id || '',
    resource_name: item.resource_name || item.name || '社群资源',
    qr_code_url: item.qr_code_url || item.qrcode_url || item.image_url || '',
    point_price: item.point_price ?? item.exchange_point ?? 0,
    status_code: item.status_code || '-',
  }
}

Page({
  data: {
    loading: false,
    group_resources: [],
    service_contact: null,
    service_hours: [],
    public_configs: [],
  },

  onShow() {
    this.loadPage()
  },

  async loadPage() {
    this.setData({ loading: true })
    try {
      const [groupData, serviceData, hoursData, configData] = await Promise.all([
        getGroupResources({ pageNo: 1, pageSize: 20 }).catch(() => []),
        getCustomerServiceContact().catch(() => null),
        getDictItems('SERVICE_HOURS').catch(() => []),
        getPublicSystemConfigs().catch(() => []),
      ])
      this.setData({
        group_resources: parseList(groupData).map(normalizeResource),
        service_contact: serviceData,
        service_hours: parseList(hoursData),
        public_configs: parseList(configData),
      })
    } catch (error) {
      wx.showToast({ title: error.message || '社群信息加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async onTapPreviewResource(event) {
    const { id, url } = event.currentTarget.dataset
    let imageUrl = url || ''

    if (!imageUrl && id) {
      try {
        const detail = await getGroupResourceDetail(id)
        imageUrl = detail?.qr_code_url || detail?.qrcode_url || detail?.image_url || ''
      } catch (error) {
        wx.showToast({ title: error.message || '获取资源详情失败', icon: 'none' })
        return
      }
    }

    if (!imageUrl) {
      wx.showToast({ title: '暂无二维码', icon: 'none' })
      return
    }
    wx.previewImage({ urls: [imageUrl], current: imageUrl })
  },

  onTapCallService() {
    const phone =
      this.data.service_contact?.service_phone ||
      this.data.service_contact?.phone ||
      this.data.service_contact?.mobile ||
      ''
    if (!phone) {
      wx.showToast({ title: '暂无客服电话', icon: 'none' })
      return
    }
    wx.makePhoneCall({ phoneNumber: String(phone) })
  },

  onTapCopyServiceWx() {
    const wxCode =
      this.data.service_contact?.wechat ||
      this.data.service_contact?.wx_code ||
      this.data.service_contact?.contact_wechat ||
      ''
    if (!wxCode) {
      wx.showToast({ title: '暂无客服微信', icon: 'none' })
      return
    }
    wx.setClipboardData({ data: String(wxCode) })
  },
})
