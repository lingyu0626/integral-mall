const { getBackpackAssetDetail, getBackpackAssetFlows, useBackpackAsset } = require('../../services/backpack')

function parseList(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.list)) return payload.list
  return []
}

function normalizeAsset(item = {}) {
  return {
    id: item.id || item.asset_id || '',
    asset_name: item.asset_name || item.asset_title || item.product_name || '未命名资产',
    asset_type_code: item.asset_type_code || item.type_code || '-',
    asset_status_code: item.asset_status_code || item.status_code || '-',
    quantity: item.quantity ?? item.available_qty ?? 1,
    cover_url: item.cover_url || item.main_image_url || '',
    expire_at: item.expire_at || item.invalid_at || '',
    remark: item.remark || '',
  }
}

function normalizeFlow(item = {}) {
  return {
    id: item.id || item.flow_id || '',
    flow_type_code: item.flow_type_code || item.action_code || '-',
    delta_qty: item.delta_qty ?? item.change_qty ?? 0,
    occurred_at: item.occurred_at || item.created_at || '',
    remark: item.remark || '',
  }
}

function canUse(statusCode) {
  const status = String(statusCode || '').toUpperCase()
  return status === 'ACTIVE' || status === 'AVAILABLE'
}

Page({
  data: {
    loading: false,
    using: false,
    asset_id: '',
    asset: null,
    flows: [],
  },

  onLoad(options) {
    const assetId = options.assetId || ''
    this.setData({ asset_id: assetId })
    this.loadPage()
  },

  async loadPage() {
    if (!this.data.asset_id) {
      wx.showToast({ title: '缺少 assetId', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const [assetData, flowData] = await Promise.all([
        getBackpackAssetDetail(this.data.asset_id),
        getBackpackAssetFlows(this.data.asset_id, { pageNo: 1, pageSize: 50 }).catch(() => []),
      ])
      this.setData({
        asset: normalizeAsset(assetData),
        flows: parseList(flowData).map(normalizeFlow),
      })
    } catch (error) {
      wx.showToast({ title: error.message || '资产详情加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  async onTapUse() {
    if (!this.data.asset?.id || this.data.using) return
    if (!canUse(this.data.asset.asset_status_code)) {
      wx.showToast({ title: '当前状态不可使用', icon: 'none' })
      return
    }
    this.setData({ using: true })
    try {
      await useBackpackAsset(this.data.asset.id)
      wx.showToast({ title: '使用成功', icon: 'success' })
      this.loadPage()
    } catch (error) {
      wx.showToast({ title: error.message || '使用失败', icon: 'none' })
    } finally {
      this.setData({ using: false })
    }
  },
})
