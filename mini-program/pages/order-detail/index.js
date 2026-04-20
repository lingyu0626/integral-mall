const { getOrderDetail, getOrderDelivery, cancelOrder, decideRejectedOrder } = require('../../services/orders')

const STATUS_TEXT_MAP = {
  INIT: '初始状态',
  PENDING_AUDIT: '待审核',
  WAIT_AUDIT: '待审核',
  PENDING_SHIP: '待发货',
  WAIT_SHIP: '待发货',
  SHIPPED: '已发货',
  DELIVERED: '已发货',
  FINISHED: '已完成',
  COMPLETED: '已完成',
  CLOSED: '已关闭',
  CANCELED: '已取消',
  REJECTED: '已驳回',
}

function mapStatusText(code) {
  const key = String(code || '').trim().toUpperCase()
  if (!key) return '-'
  return STATUS_TEXT_MAP[key] || key
}

function parseList(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.list)) return payload.list
  return []
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
      (statusCode === 'REJECTED' && buyerDecisionRequired ? '已驳回（待你确认）' : mapStatusText(statusCode)),
    total_point_amount: item.total_point_amount ?? item.point_amount ?? 0,
    total_item_count: item.total_item_count ?? item.quantity ?? 1,
    submit_at: item.submit_at || item.created_at || '',
    audit_at: item.audit_at || '',
    ship_at: item.ship_at || '',
    finish_at: item.finish_at || '',
    user_remark: item.user_remark || '',
    admin_remark: item.admin_remark || '',
    buyer_decision_required: buyerDecisionRequired,
  }
}

function normalizeItem(item = {}) {
  return {
    id: item.id || item.order_item_id || '',
    product_name_snapshot: item.product_name_snapshot || item.product_name || '兑换商品',
    main_image_snapshot: item.main_image_snapshot || item.main_image_url || '',
    unit_point_price: item.unit_point_price ?? item.point_price ?? 0,
    quantity: item.quantity ?? 1,
    total_point_amount: item.total_point_amount ?? item.point_amount ?? 0,
  }
}

function normalizeFlow(item = {}) {
  const fromStatusCode = item.from_status_code || '-'
  const toStatusCode = item.to_status_code || '-'
  return {
    id: item.id || item.flow_id || '',
    from_status_code: fromStatusCode,
    to_status_code: toStatusCode,
    from_status_text: mapStatusText(fromStatusCode),
    to_status_text: mapStatusText(toStatusCode),
    operated_at: item.operated_at || item.created_at || '',
    action_code: item.action_code || '',
    remark: item.remark || '',
  }
}

function normalizeAddress(item = {}) {
  if (!item || typeof item !== 'object') return null
  return {
    receiver_name: item.receiver_name || '',
    receiver_phone: item.receiver_phone || '',
    province_name: item.province_name || '',
    city_name: item.city_name || '',
    district_name: item.district_name || '',
    detail_address: item.detail_address || '',
  }
}

function parseOrderDetail(payload = {}) {
  if (payload.order || payload.items) {
    return {
      order: payload.order || {},
      items: payload.items || payload.order_items || payload.item_list || [],
      address: payload.address_snapshot || payload.order_address_snapshot || payload.address || null,
    }
  }
  return {
    order: payload || {},
    items: payload.items || payload.order_items || payload.item_list || [],
    address: payload.address_snapshot || payload.order_address_snapshot || payload.address || null,
  }
}

function canCancel(statusCode) {
  const status = String(statusCode || '').toUpperCase()
  return status === 'PENDING_AUDIT' || status === 'WAIT_AUDIT'
}

Page({
  data: {
    loading: false,
    order_id: '',
    order: null,
    items: [],
    delivery: null,
    address_snapshot: null,
    can_cancel: false,
    can_review_reject: false,
    reject_prompt_shown: false,
  },

  onLoad(options) {
    const orderId = options.orderId || ''
    this.setData({ order_id: orderId })
    this.loadPage()
  },

  async loadPage() {
    if (!this.data.order_id) {
      wx.showToast({ title: '缺少 orderId', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    try {
      const [detailData, deliveryData] = await Promise.all([
        getOrderDetail(this.data.order_id),
        getOrderDelivery(this.data.order_id).catch(() => null),
      ])

      const parsed = parseOrderDetail(detailData)
      const order = normalizeOrder(parsed.order)
      this.setData({
        order,
        items: parseList(parsed.items).map(normalizeItem),
        delivery: deliveryData || null,
        address_snapshot: normalizeAddress(parsed.address),
        can_cancel: canCancel(order.order_status_code),
        can_review_reject: order.order_status_code === 'REJECTED' && !!order.buyer_decision_required,
      })
      this.maybePromptRejectDecision()
    } catch (error) {
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  maybePromptRejectDecision() {
    if (!this.data.can_review_reject || this.data.reject_prompt_shown) return
    const order = this.data.order
    if (!order?.id) return
    this.setData({ reject_prompt_shown: true })
    wx.showActionSheet({
      itemList: ['接受（订单进入待发货）', '拒绝（碎片退回背包）'],
      success: async ({ tapIndex }) => {
        try {
          if (tapIndex === 0) {
            await decideRejectedOrder(order.id, { decision: 'ACCEPT' })
            wx.showToast({ title: '已提交重审', icon: 'success' })
            this.loadPage()
            return
          }
          if (tapIndex === 1) {
            await decideRejectedOrder(order.id, { decision: 'REFUND' })
            wx.showToast({ title: '碎片已退回', icon: 'success' })
            this.loadPage()
          }
        } catch (error) {
          wx.showToast({ title: error.message || '操作失败', icon: 'none' })
          this.setData({ reject_prompt_shown: false })
        }
      },
      fail: () => {
        // 用户取消则保留页面按钮可手动处理
      },
    })
  },

  onTapCancel() {
    if (!this.data.order?.id || !this.data.can_cancel) return
    wx.showModal({
      title: '取消订单',
      content: '确认取消该订单吗？',
      success: async ({ confirm }) => {
        if (!confirm) return
        try {
          await cancelOrder(this.data.order.id, { cancel_reason: 'USER_CANCEL' })
          wx.showToast({ title: '订单已取消', icon: 'success' })
          this.loadPage()
        } catch (error) {
          wx.showToast({ title: error.message || '取消失败', icon: 'none' })
        }
      },
    })
  },

  onTapAcceptReject() {
    const order = this.data.order
    if (!order?.id || !this.data.can_review_reject) return
    wx.showModal({
      title: '确认接受驳回',
      content: '接受后，订单会自动进入待发货。',
      success: async ({ confirm }) => {
        if (!confirm) return
        try {
          await decideRejectedOrder(order.id, { decision: 'ACCEPT' })
          wx.showToast({ title: '已提交重审', icon: 'success' })
          this.loadPage()
        } catch (error) {
          wx.showToast({ title: error.message || '操作失败', icon: 'none' })
        }
      },
    })
  },

  onTapRefundReject() {
    const order = this.data.order
    if (!order?.id || !this.data.can_review_reject) return
    wx.showModal({
      title: '确认拒绝并退回碎片',
      content: '拒绝后将返还碎片并关闭该订单，无法恢复。',
      success: async ({ confirm }) => {
        if (!confirm) return
        try {
          await decideRejectedOrder(order.id, { decision: 'REFUND' })
          wx.showToast({ title: '碎片已退回', icon: 'success' })
          this.loadPage()
        } catch (error) {
          wx.showToast({ title: error.message || '操作失败', icon: 'none' })
        }
      },
    })
  },

  onTapLogistics() {
    if (!this.data.order?.id) return
    wx.navigateTo({ url: `/pages/logistics/index?orderId=${this.data.order.id}` })
  },
})
