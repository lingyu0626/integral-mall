const { loadCartItems, removeCartProduct } = require('../../utils/cart')
const { getPointAccount } = require('../../services/points')

function normalizeCartItem(item = {}) {
  const quantity = Number(item.quantity || 1)
  const pointPrice = Number(item.point_price || 0)
  return {
    product_id: item.product_id || item.id || item.spu_id || '',
    sku_id: item.sku_id || '',
    product_name: item.product_name || item.name || '未命名商品',
    main_image_url: item.main_image_url || item.image_url || '',
    point_price: pointPrice,
    quantity,
    total_point_amount: pointPrice * quantity,
    added_at: item.added_at || '-',
    added_ts: Number(item.added_ts || 0),
  }
}

Page({
  data: {
    list: [],
    point_balance: 0,
    sort_options: [
      { label: '碎片高到低', value: 'POINT_DESC' },
      { label: '碎片低到高', value: 'POINT_ASC' },
    ],
    sort_index: 0,
    temp_sort_index: 0,
    show_sort_sheet: false,
  },

  async onShow() {
    this.loadPointBalance()
    this.refreshList()
  },

  async loadPointBalance() {
    try {
      const data = await getPointAccount()
      this.setData({ point_balance: Number(data?.point_balance || 0) })
    } catch (_) {
      this.setData({ point_balance: 0 })
    }
  },

  refreshList() {
    const list = loadCartItems().map(normalizeCartItem)

    const sortValue = this.data.sort_options[this.data.sort_index]?.value || 'POINT_DESC'
    list.sort((a, b) => {
      const pointDiff = Number(b.total_point_amount || 0) - Number(a.total_point_amount || 0)
      const timeDiff = Number(b.added_ts || 0) - Number(a.added_ts || 0)
      if (sortValue === 'POINT_ASC') {
        if (pointDiff !== 0) return -pointDiff
        return -timeDiff
      }
      if (pointDiff !== 0) return pointDiff
      return timeDiff
    })

    this.setData({ list })
  },

  onTapFilterLevel() {
    this.setData({
      show_sort_sheet: true,
      temp_sort_index: this.data.sort_index,
    })
  },

  onSortPickerChange(event) {
    const idx = Number(event?.detail?.value?.[0] ?? 0)
    this.setData({ temp_sort_index: idx })
  },

  onCancelSortSheet() {
    this.setData({ show_sort_sheet: false })
  },

  onConfirmSortSheet() {
    this.setData({
      show_sort_sheet: false,
      sort_index: this.data.temp_sort_index,
    })
    this.refreshList()
  },

  onTapDetail(event) {
    const productId = event.currentTarget.dataset.id
    if (!productId) return
    wx.navigateTo({ url: `/pages/product-detail/index?productId=${productId}` })
  },

  onTapExchange(event) {
    const productId = event.currentTarget.dataset.id
    const skuId = String(event.currentTarget.dataset.skuId || '').trim()
    if (!productId) return
    const skuPart = skuId ? `&skuId=${encodeURIComponent(skuId)}` : ''
    wx.navigateTo({ url: `/pages/exchange-confirm/index?productId=${productId}${skuPart}` })
  },

  onTapRemove(event) {
    const productId = event.currentTarget.dataset.id
    if (!productId) return
    wx.showModal({
      title: '移除商品',
      content: '确认从购物车移除该商品吗？',
      success: ({ confirm }) => {
        if (!confirm) return
        removeCartProduct(productId)
        this.refreshList()
      },
    })
  },

  noop() {},
})
