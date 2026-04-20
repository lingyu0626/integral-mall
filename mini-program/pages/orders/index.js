const { getOrders, cancelOrder, getOrderStatusCounts } = require('../../services/orders')

const STATUS_TABS = [
  { key: 'ALL', label: '全部', status_code: '' },
  { key: 'PENDING_AUDIT', label: '待审核', status_code: 'PENDING_AUDIT' },
  { key: 'PENDING_SHIP', label: '待发货', status_code: 'PENDING_SHIP' },
  { key: 'SHIPPED', label: '已发货', status_code: 'SHIPPED' },
  { key: 'FINISHED', label: '已完成', status_code: 'FINISHED' },
]

const STATUS_TEXT_MAP = {
  PENDING_AUDIT: '待审核',
  PENDING_SHIP: '待发货',
  SHIPPED: '已发货',
  FINISHED: '已完成',
  CLOSED: '已关闭',
  CANCELED: '已取消',
  REJECTED: '已驳回',
}

function parsePage(payload) {
  if (Array.isArray(payload)) {
    return {
      list: payload,
      pageNo: 1,
      pageSize: payload.length || 20,
      total: payload.length,
    }
  }
  if (payload && Array.isArray(payload.list)) {
    return {
      list: payload.list,
      pageNo: Number(payload.pageNo || payload.page_no || 1),
      pageSize: Number(payload.pageSize || payload.page_size || 20),
      total: Number(payload.total || payload.total_count || payload.list.length || 0),
    }
  }
  return { list: [], pageNo: 1, pageSize: 20, total: 0 }
}

function normalizeOrder(item = {}) {
  const statusCode = item.order_status_code || item.status_code || ''
  const buyerDecisionRequired =
    item.buyer_decision_required === true ||
    item.buyer_decision_required === 'true' ||
    item.buyer_decision_required === 1
  return {
    id: item.id || item.order_id || '',
    order_no: item.order_no || '-',
    order_status_code: statusCode,
    order_status_text:
      item.order_status_text ||
      (statusCode === 'REJECTED' && buyerDecisionRequired
        ? '已驳回（待你确认）'
        : STATUS_TEXT_MAP[statusCode] || statusCode || '-'),
    total_point_amount: item.total_point_amount ?? item.point_amount ?? 0,
    total_item_count: item.total_item_count ?? item.quantity ?? 1,
    submit_at: item.submit_at || item.created_at || '',
    product_name_snapshot: item.product_name_snapshot || item.product_name || '兑换商品',
    main_image_snapshot: item.main_image_snapshot || item.main_image_url || '',
    can_cancel: canCancel(statusCode),
    buyer_decision_required: buyerDecisionRequired,
  }
}

function resolveCount(counts = {}, key) {
  const c = counts || {}
  if (key === 'ALL') {
    return Number(c.total || c.total_count || c.all_count || 0)
  }
  if (key === 'PENDING_AUDIT') {
    return Number(c.pending_audit_count || c.pendingAuditCount || c.pending_count || 0)
  }
  if (key === 'PENDING_SHIP') {
    return Number(c.pending_ship_count || c.pendingShipCount || c.to_ship_count || 0)
  }
  if (key === 'SHIPPED') {
    return Number(c.shipped_count || c.shipCount || 0)
  }
  if (key === 'FINISHED') {
    return Number(c.finished_count || c.finish_count || 0)
  }
  return 0
}

function canCancel(statusCode) {
  const status = String(statusCode || '').toUpperCase()
  return status === 'PENDING_AUDIT' || status === 'WAIT_AUDIT'
}

Page({
  data: {
    loading: false,
    loading_more: false,
    tabs: STATUS_TABS,
    active_tab: 'ALL',
    list: [],
    page_no: 1,
    page_size: 20,
    total: 0,
    has_more: true,
    reject_prompted_order_id: '',
  },

  onLoad(options) {
    const status = String(options?.status || '').toUpperCase()
    const target = STATUS_TABS.find((item) => item.key === status)
    if (target) {
      this.setData({ active_tab: target.key })
    }
  },

  onShow() {
    this.refreshPage()
  },

  onPullDownRefresh() {
    Promise.resolve(this.refreshPage()).finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.loadList({ reset: false })
  },

  async refreshPage() {
    await Promise.all([this.loadStatusCounts(), this.loadList({ reset: true })])
  },

  async loadStatusCounts() {
    try {
      const counts = await getOrderStatusCounts()
      const tabs = STATUS_TABS.map((tab) => ({
        ...tab,
        count: resolveCount(counts, tab.key),
      }))
      this.setData({ tabs })
    } catch (error) {
      // 统计失败不阻断列表
    }
  },

  async loadList({ reset = false } = {}) {
    const nextPage = reset ? 1 : this.data.page_no + 1
    if (!reset && (!this.data.has_more || this.data.loading_more || this.data.loading)) return

    this.setData(reset ? { loading: true } : { loading_more: true })

    try {
      const active = this.data.tabs.find((tab) => tab.key === this.data.active_tab)
      const params = {
        pageNo: nextPage,
        pageSize: this.data.page_size,
      }
      if (active?.status_code) {
        params.order_status_code = active.status_code
      }

      const pageData = parsePage(await getOrders(params))
      const list = pageData.list.map(normalizeOrder)
      const mergedList = reset ? list : this.data.list.concat(list)
      const hasMore = mergedList.length < pageData.total

      this.setData({
        list: mergedList,
        page_no: pageData.pageNo,
        total: pageData.total,
        has_more: hasMore,
      })
      if (reset) {
        this.maybePromptRejectedOrder(mergedList)
      }
    } catch (error) {
      wx.showToast({ title: error.message || '订单加载失败', icon: 'none' })
      if (reset) this.setData({ list: [], has_more: false, total: 0 })
    } finally {
      this.setData({ loading: false, loading_more: false })
    }
  },

  onTapTab(event) {
    const { key } = event.currentTarget.dataset
    if (!key || key === this.data.active_tab) return
    this.setData({ active_tab: key })
    this.loadList({ reset: true })
  },

  onTapDetail(event) {
    const { id } = event.currentTarget.dataset
    if (!id) return
    wx.navigateTo({ url: `/pages/order-detail/index?orderId=${id}` })
  },

  onTapLogistics(event) {
    const { id } = event.currentTarget.dataset
    if (!id) return
    wx.navigateTo({ url: `/pages/logistics/index?orderId=${id}` })
  },

  onTapCancel(event) {
    const { id, status } = event.currentTarget.dataset
    if (!id || !canCancel(status)) return

    wx.showModal({
      title: '取消订单',
      content: '确认取消该订单吗？',
      success: async ({ confirm }) => {
        if (!confirm) return
        try {
          await cancelOrder(id, { cancel_reason: 'USER_CANCEL' })
          wx.showToast({ title: '已取消', icon: 'success' })
          this.refreshPage()
        } catch (error) {
          wx.showToast({ title: error.message || '取消失败', icon: 'none' })
        }
      },
    })
  },

  maybePromptRejectedOrder(list = []) {
    if (!Array.isArray(list) || !list.length) return
    const target = list.find((item) => item.order_status_code === 'REJECTED' && item.buyer_decision_required)
    if (!target || !target.id) return
    if (String(this.data.reject_prompted_order_id || '') === String(target.id)) return

    this.setData({ reject_prompted_order_id: target.id })
    wx.showModal({
      title: '订单待你确认',
      content: '你有一笔驳回订单，请选择“接受重审”或“拒绝退回碎片”。',
      confirmText: '立即处理',
      cancelText: '稍后',
      success: ({ confirm }) => {
        if (!confirm) return
        wx.navigateTo({ url: `/pages/order-detail/index?orderId=${target.id}` })
      },
    })
  },
})
