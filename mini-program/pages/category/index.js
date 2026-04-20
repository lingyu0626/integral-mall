const { getCategories, getCategoryProducts } = require('../../services/categories')
const { getPointAccount } = require('../../services/points')

function parseList(payload) {
  if (Array.isArray(payload)) return payload
  if (payload && Array.isArray(payload.list)) return payload.list
  return []
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
      pageSize: Number(payload.pageSize || payload.page_size || payload.list.length || 20),
      total: Number(payload.total || payload.total_count || payload.list.length || 0),
    }
  }
  return { list: [], pageNo: 1, pageSize: 20, total: 0 }
}

const PAGE_SIZE = 100
const MAX_AUTO_FETCH_PAGES = 50

function normalizeCategory(item = {}) {
  return {
    id: item.id || item.category_id || '',
    category_name: item.category_name || item.name || '未命名分类',
  }
}

function normalizeProduct(item = {}) {
  return {
    id: item.id || item.product_id || item.spu_id || '',
    product_name: item.product_name || item.name || '未命名商品',
    main_image_url: item.main_image_url || item.image_url || '',
    point_price: item.point_price || item.exchange_point || item.need_point || 0,
    stock_available: item.stock_available ?? item.stock ?? '-',
  }
}

Page({
  data: {
    loading: false,
    categories: [],
    active_category_id: '',
    keyword: '',
    sort_by: '',
    products: [],
    point_balance: 0,
    status_bar_height: 20,
    nav_content_height: 44,
    layout_height: 420,
    base_layout_height: 420,
  },

  onLoad() {
    this.computeLayoutMetrics()
  },

  async onShow() {
    this.loadPointBalance()
    if (!this._initialized || !this.data.categories.length) {
      this._initialized = true
      await this.initPage()
    }
  },

  async loadPointBalance() {
    try {
      const data = await getPointAccount()
      this.setData({ point_balance: Number(data?.point_balance || 0) })
    } catch (error) {
      this.setData({ point_balance: 0 })
    }
  },

  async initPage() {
    this.setData({ loading: true })
    try {
      const data = await getCategories()
      const categories = parseList(data).map(normalizeCategory)
      const active = categories[0]?.id || ''
      this.setData({ categories, active_category_id: active })
      if (active) {
        await this.loadProducts(active)
      } else {
        this.setData({ products: [] })
        this.scheduleAdjustLayoutHeight()
      }
    } catch (error) {
      wx.showToast({ title: error.message || '加载分类失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onInputKeyword(event) {
    this.setData({ keyword: event.detail.value })
  },

  async onTapSearch() {
    if (!this.data.active_category_id) return
    await this.loadProducts(this.data.active_category_id)
  },

  async onTapSort(event) {
    const { sort } = event.currentTarget.dataset
    const next = String(sort || '')
    if (next === this.data.sort_by) return
    this.setData({ sort_by: next })
    if (this.data.active_category_id) {
      await this.loadProducts(this.data.active_category_id)
    }
  },

  onTapPoints() {
    wx.navigateTo({ url: '/pages/points/index' })
  },

  onTapMyExchange() {
    wx.navigateTo({ url: '/pages/orders/index' })
  },

  async onTapCategory(event) {
    const { id } = event.currentTarget.dataset
    if (!id || id === this.data.active_category_id) return

    this.setData({ active_category_id: id, keyword: '' })
    await this.loadProducts(id)
  },

  async loadProducts(categoryId) {
    this.setData({ loading: true })
    try {
      const params = {}
      if (this.data.keyword) params.keyword = this.data.keyword
      if (this.data.sort_by) params.sort_by = this.data.sort_by
      const products = await this.loadAllCategoryProducts(categoryId, params)
      this.setData({ products })
      this.scheduleAdjustLayoutHeight()
    } catch (error) {
      wx.showToast({ title: error.message || '加载商品失败', icon: 'none' })
      this.setData({ products: [] })
      this.scheduleAdjustLayoutHeight()
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadAllCategoryProducts(categoryId, baseParams = {}) {
    const merged = []
    let nextPageNo = 1
    let total = 0

    while (nextPageNo <= MAX_AUTO_FETCH_PAGES) {
      const pageData = parsePage(
        await getCategoryProducts(categoryId, {
          ...baseParams,
          pageNo: nextPageNo,
          pageSize: PAGE_SIZE,
        }),
      )
      const pageList = parseList(pageData).map(normalizeProduct)
      merged.push(...pageList)
      total = Number(pageData.total || merged.length)

      const reachedTotal = merged.length >= total
      const reachedLastPage = pageList.length < Number(pageData.pageSize || PAGE_SIZE)
      if (reachedTotal || reachedLastPage) break

      nextPageNo += 1
    }

    if (total > 0 && merged.length > total) {
      return merged.slice(0, total)
    }
    return merged
  },

  onTapDetail(event) {
    const { id } = event.currentTarget.dataset
    if (!id) {
      wx.showToast({ title: '商品ID缺失', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/product-detail/index?productId=${id}` })
  },

  computeLayoutMetrics() {
    try {
      const sys = wx.getSystemInfoSync()
      const menu = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null
      const statusBar = Number(sys.statusBarHeight || 20)
      let navContent = 44
      if (menu && menu.top && menu.bottom) {
        navContent = Math.round((menu.bottom - menu.top) + (menu.top - statusBar) * 2)
      }
      const rpx2px = Number(sys.windowWidth || 375) / 750
      const topCardHeight = 176 * rpx2px
      const searchBlock = (64 + 22 - 32) * rpx2px
      const layoutHeight = Math.max(
        240,
        Math.floor(Number(sys.windowHeight || 667) - statusBar - navContent - topCardHeight - searchBlock),
      )
      this.setData({
        status_bar_height: statusBar,
        nav_content_height: navContent,
        layout_height: layoutHeight,
        base_layout_height: layoutHeight,
      })
    } catch (_) {}
  },

  scheduleAdjustLayoutHeight() {
    if (this._layoutTimer) clearTimeout(this._layoutTimer)
    this._layoutTimer = setTimeout(() => {
      this.adjustLayoutHeight()
    }, 30)
  },

  adjustLayoutHeight() {
    const base = Number(this.data.base_layout_height || this.data.layout_height || 420)
    const next = Math.max(240, Math.floor(base))
    if (next !== this.data.layout_height) {
      this.setData({ layout_height: next })
    }
  },

  onUnload() {
    if (this._layoutTimer) {
      clearTimeout(this._layoutTimer)
      this._layoutTimer = null
    }
  },
})
