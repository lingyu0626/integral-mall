const { getOrderDelivery, getOrderLogisticsTraces } = require('../../services/orders')

function parseTraces(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.traces)) return payload.traces
  return []
}

function normalizeTrace(item = {}) {
  return {
    accept_time: item.accept_time || item.AcceptTime || item.time || '',
    accept_station: item.accept_station || item.AcceptStation || item.context || '-',
    status: item.status || item.Status || '',
    location: item.location || item.Location || '',
  }
}

Page({
  data: {
    order_id: '',
    loading: false,
    provider_tip: '本数据由快递公司提供',
    delivery: null,
    traces: [],
  },

  onLoad(options) {
    this.setData({ order_id: options?.orderId || '' })
    this.loadPage()
  },

  async loadPage() {
    if (!this.data.order_id) return
    this.setData({ loading: true })
    try {
      const [logisticsData, delivery] = await Promise.all([
        getOrderLogisticsTraces(this.data.order_id).catch(() => null),
        getOrderDelivery(this.data.order_id).catch(() => null),
      ])
      const traces = parseTraces(logisticsData).map(normalizeTrace)
      const mergedDelivery = {
        ...(delivery || {}),
        ...(logisticsData || {}),
      }
      this.setData({
        provider_tip: logisticsData?.provider_tip || '本数据由快递公司提供',
        delivery: mergedDelivery,
        traces,
      })
    } catch (error) {
      wx.showToast({ title: error.message || '加载物流失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onTapCopyNo() {
    const no = String(this.data.delivery?.express_no || this.data.delivery?.tracking_no || this.data.delivery?.delivery_no || '').trim()
    if (!no) {
      wx.showToast({ title: '暂无单号', icon: 'none' })
      return
    }
    wx.setClipboardData({ data: no })
  },

  onTapCall() {
    const phone = String(this.data.delivery?.shipper_phone || '').trim()
    if (!phone) {
      wx.showToast({ title: '暂无电话', icon: 'none' })
      return
    }
    wx.makePhoneCall({ phoneNumber: phone })
  },
})
